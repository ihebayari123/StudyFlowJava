"""
main.py — StudyFlow WhatsApp Notifier (Français)
=================================================
Intégré avec le projet Java StudyFlow (base de données MySQL `studyflow`).

Fonctionnalités :
  1. Chatbot WhatsApp en FRANÇAIS via Twilio + Groq (LLaMA)
  2. Surveillance de la table `quiz` (date_creation) → notifie les abonnés
     WhatsApp dès qu'un nouveau quiz est détecté en BDD
  3. Endpoint REST pour forcer une vérification manuelle
  4. Endpoint REST pour envoyer une notification depuis Java (optionnel)

Démarrage :
    venv\\Scripts\\activate
    uvicorn main:app --reload --port 8000

Variables .env requises :
    GROQ_API_KEY
    TWILIO_ACCOUNT_SID
    TWILIO_AUTH_TOKEN
    TWILIO_WHATSAPP_NUMBER   (ex: whatsapp:+14155238886)
    DB_HOST                  (ex: 127.0.0.1)
    DB_PORT                  (ex: 3306)
    DB_NAME                  (ex: studyflow)
    DB_USER                  (ex: root)
    DB_PASSWORD              (ex: )
"""

from dotenv import load_dotenv
from datetime import datetime
import asyncio
import os
import json
import pymysql
import pymysql.cursors

from groq import Groq
from pydantic import BaseModel
from twilio.request_validator import RequestValidator
from twilio.rest import Client as TwilioClient
from fastapi import FastAPI, Request, Form, HTTPException
from fastapi.responses import PlainTextResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware

load_dotenv()

# ══════════════════════════════════════════════════════════════════════════════
# ⚙️  CONFIG
# ══════════════════════════════════════════════════════════════════════════════

app = FastAPI(title="StudyFlow WhatsApp Notifier — Français")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_methods=["*"], allow_headers=["*"]
)

groq_client   = Groq(api_key=os.getenv("GROQ_API_KEY"))
twilio_client = TwilioClient(os.getenv("TWILIO_ACCOUNT_SID"), os.getenv("TWILIO_AUTH_TOKEN"))
twilio_validator = RequestValidator(os.getenv("TWILIO_AUTH_TOKEN"))
TWILIO_NUMBER = os.getenv("TWILIO_WHATSAPP_NUMBER", "whatsapp:+14155238886")

# ── Base de données ───────────────────────────────────────────────────────────
DB_CONFIG = {
    "host":     os.getenv("DB_HOST",     "127.0.0.1"),
    "port":     int(os.getenv("DB_PORT", "3306")),
    "db":       os.getenv("DB_NAME",     "studyflow"),
    "user":     os.getenv("DB_USER",     "root"),
    "password": os.getenv("DB_PASSWORD", ""),
    "charset":  "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor,
}

# ══════════════════════════════════════════════════════════════════════════════
# 👥  ABONNÉS (fichier JSON sur disque)
# ══════════════════════════════════════════════════════════════════════════════

SUBSCRIBERS_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "subscribers.json")


def load_subscribers() -> dict:
    if os.path.exists(SUBSCRIBERS_FILE):
        try:
            with open(SUBSCRIBERS_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            print(f"[subscribers] Erreur lecture : {e}")
    return {}


def save_subscribers(data: dict):
    try:
        tmp = SUBSCRIBERS_FILE + ".tmp"
        with open(tmp, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        os.replace(tmp, SUBSCRIBERS_FILE)
    except Exception as e:
        print(f"[subscribers] Erreur écriture : {e}")


_initial = load_subscribers()
print(f"[subscribers] {len(_initial)} enregistrement(s) trouvé(s)")

# ══════════════════════════════════════════════════════════════════════════════
# 🗄️  ACCÈS BASE DE DONNÉES
# ══════════════════════════════════════════════════════════════════════════════

def get_db_connection():
    """Ouvre une connexion PyMySQL à la BDD StudyFlow."""
    return pymysql.connect(**DB_CONFIG)


def get_all_quizzes() -> list[dict]:
    """
    Récupère tous les quiz de la BDD avec un maximum d'informations.
    Retourne une liste de dict avec id, titre, duree, date_creation, course_id,
    et le titre du cours associé si disponible.
    """
    try:
        conn = get_db_connection()
        with conn.cursor() as cur:
            cur.execute("""
                SELECT
                    q.id,
                    q.titre,
                    q.duree,
                    q.date_creation,
                    q.course_id,
                    c.titre   AS cours_titre,
                    c.description AS cours_description
                FROM quiz q
                LEFT JOIN cours c ON q.course_id = c.id
                ORDER BY q.date_creation DESC
            """)
            rows = cur.fetchall()
        conn.close()
        return rows
    except Exception as e:
        print(f"[db] Erreur récupération quiz : {e}")
        return []


def get_quiz_by_id(quiz_id: int) -> dict | None:
    """Récupère un quiz spécifique avec toutes ses infos."""
    try:
        conn = get_db_connection()
        with conn.cursor() as cur:
            cur.execute("""
                SELECT
                    q.id,
                    q.titre,
                    q.duree,
                    q.date_creation,
                    q.course_id,
                    c.titre       AS cours_titre,
                    c.description AS cours_description,
                    (SELECT COUNT(*) FROM question WHERE quiz_id = q.id) AS nb_questions
                FROM quiz q
                LEFT JOIN cours c ON q.course_id = c.id
                WHERE q.id = %s
            """, (quiz_id,))
            row = cur.fetchone()
        conn.close()
        return row
    except Exception as e:
        print(f"[db] Erreur récupération quiz {quiz_id} : {e}")
        return None


def get_questions_count_by_quiz(quiz_id: int) -> int:
    """Compte les questions d'un quiz."""
    try:
        conn = get_db_connection()
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) AS cnt FROM question WHERE quiz_id = %s", (quiz_id,))
            row = cur.fetchone()
        conn.close()
        return row["cnt"] if row else 0
    except Exception:
        return 0

# ══════════════════════════════════════════════════════════════════════════════
# 🔔  SURVEILLANCE QUIZ — détection des nouveaux quiz
# ══════════════════════════════════════════════════════════════════════════════

# On mémorise les IDs déjà connus pour ne pas re-notifier
_known_quiz_ids: set[int] = set()


def format_quiz_notification(quiz: dict) -> str:
    """
    Formate un message WhatsApp complet pour un nouveau quiz.
    Inclut toutes les informations disponibles depuis la BDD.
    """
    date_str = ""
    if isinstance(quiz.get("date_creation"), datetime):
        date_str = quiz["date_creation"].strftime("%d/%m/%Y à %H:%M")
    else:
        date_str = str(quiz.get("date_creation", "—"))

    nb_questions = get_questions_count_by_quiz(quiz["id"])

    cours_label = quiz.get("cours_titre") or ("ID " + str(quiz.get("course_id", "—")))
    lines = [
        "🎓 *Nouveau Quiz disponible sur StudyFlow !*",
        "━━━━━━━━━━━━━━━━━━━━",
        f"📌 *Titre :* {quiz.get('titre', '—')}",
        f"🆔 *ID Quiz :* {quiz.get('id', '—')}",
        f"⏱️ *Durée :* {quiz.get('duree', '—')} minutes",
        f"📅 *Créé le :* {date_str}",
        f"📚 *Cours :* {cours_label}",
    ]

    if quiz.get("cours_description"):
        lines.append(f"📝 *Description du cours :* {quiz['cours_description']}")

    if nb_questions > 0:
        lines.append(f"❓ *Nombre de questions :* {nb_questions}")
    else:
        lines.append("❓ *Questions :* aucune encore (quiz vide)")

    lines += [
        "━━━━━━━━━━━━━━━━━━━━",
        "_StudyFlow — Plateforme d'apprentissage_ 📖",
    ]

    return "\n".join(lines)


async def send_whatsapp(to: str, body: str):
    """Envoie un message WhatsApp via Twilio."""
    twilio_client.messages.create(from_=TWILIO_NUMBER, to=to, body=body)


async def check_new_quizzes():
    """
    Vérifie les nouveaux quiz dans la BDD et notifie les abonnés.
    Appelé périodiquement par le scheduler.
    """
    global _known_quiz_ids

    quizzes = get_all_quizzes()
    if not quizzes:
        return

    # Initialisation silencieuse au premier lancement
    if not _known_quiz_ids:
        _known_quiz_ids = {q["id"] for q in quizzes}
        print(f"[quiz-watcher] Initialisation : {len(_known_quiz_ids)} quiz connus en BDD")
        return

    # Détection des nouveaux
    new_quizzes = [q for q in quizzes if q["id"] not in _known_quiz_ids]

    for quiz in new_quizzes:
        _known_quiz_ids.add(quiz["id"])
        msg = format_quiz_notification(quiz)
        print(f"[quiz-watcher] Nouveau quiz détecté : #{quiz['id']} — {quiz['titre']}")

        # Notifier tous les abonnés actifs
        subs = load_subscribers()
        active = [uid for uid, info in subs.items() if info.get("subscribed")]
        print(f"[quiz-watcher] Envoi à {len(active)} abonné(s)")
        for uid in active:
            try:
                await send_whatsapp(uid, msg)
                print(f"[quiz-watcher] ✅ Envoyé à {uid}")
            except Exception as e:
                print(f"[quiz-watcher] ❌ Erreur envoi à {uid} : {e}")

# ══════════════════════════════════════════════════════════════════════════════
# ⏰  SCHEDULER
# ══════════════════════════════════════════════════════════════════════════════

QUIZ_CHECK_INTERVAL_SECONDS = 30  # Vérification toutes les 30 secondes


async def scheduler():
    """Boucle de fond : surveille les nouveaux quiz en permanence."""
    while True:
        try:
            await check_new_quizzes()
        except Exception as e:
            print(f"[scheduler] Erreur : {e}")
        await asyncio.sleep(QUIZ_CHECK_INTERVAL_SECONDS)


@app.on_event("startup")
async def startup_event():
    asyncio.create_task(scheduler())
    subs = load_subscribers()
    count = sum(1 for v in subs.values() if v.get("subscribed"))
    quizzes = get_all_quizzes()
    print(f"✅ StudyFlow WhatsApp démarré | {count} abonné(s) | {len(quizzes)} quiz en BDD")

# ══════════════════════════════════════════════════════════════════════════════
# 💬  CHATBOT — Système en FRANÇAIS
# ══════════════════════════════════════════════════════════════════════════════

SYSTEM_PROMPT_FR = """Tu es l'assistant officiel de la plateforme *StudyFlow* 🎓

Tu parles TOUJOURS en français, peu importe la langue utilisée par l'utilisateur.

📚 Tu aides les utilisateurs à :
- Connaître les quiz disponibles sur la plateforme
- Comprendre les notifications reçues sur WhatsApp
- S'abonner ou se désabonner aux alertes de nouveaux quiz
- Obtenir des informations générales sur StudyFlow

🎯 Style de réponse :
- Clair, concis, adapté à WhatsApp (pas trop long)
- Professionnel mais chaleureux
- Utilise des emojis avec modération
- Réponds toujours en français

⚠️ Règles :
- Ne demande pas d'informations personnelles sensibles
- Si tu ne sais pas quelque chose, dis-le honnêtement
- Pour s'abonner : taper *ABONNER*
- Pour se désabonner : taper *STOP*
- Pour voir les quiz : taper *QUIZ*
"""

conversation_store: dict[str, list[dict]] = {}
MAX_HISTORY = 20


def chat_fr(user_id: str, user_message: str) -> str:
    """Génère une réponse en français via Groq/LLaMA."""
    history = conversation_store.setdefault(user_id, [])
    history.append({"role": "user", "content": user_message})
    if len(history) > MAX_HISTORY:
        history[:] = history[-MAX_HISTORY:]

    resp = groq_client.chat.completions.create(
        model="llama-3.3-70b-versatile",
        messages=[{"role": "system", "content": SYSTEM_PROMPT_FR}] + history,
        temperature=0.4,
        max_tokens=500,
    )
    reply = resp.choices[0].message.content.strip()
    history.append({"role": "assistant", "content": reply})
    return reply

# ══════════════════════════════════════════════════════════════════════════════
# 🌐  ENDPOINTS REST
# ══════════════════════════════════════════════════════════════════════════════

@app.get("/")
def root():
    subs = load_subscribers()
    count = sum(1 for v in subs.values() if v.get("subscribed"))
    quizzes = get_all_quizzes()
    return {
        "status": "✅ StudyFlow WhatsApp Notifier actif",
        "abonnés_actifs": count,
        "quiz_en_bdd": len(quizzes),
        "quiz_connus": len(_known_quiz_ids),
        "vérification_toutes": f"{QUIZ_CHECK_INTERVAL_SECONDS}s",
    }


@app.get("/quiz")
def list_quizzes():
    """Liste tous les quiz disponibles en BDD avec leurs infos complètes."""
    quizzes = get_all_quizzes()
    result = []
    for q in quizzes:
        nb = get_questions_count_by_quiz(q["id"])
        result.append({
            "id": q["id"],
            "titre": q["titre"],
            "duree_min": q["duree"],
            "date_creation": str(q["date_creation"]),
            "course_id": q["course_id"],
            "cours_titre": q.get("cours_titre"),
            "nb_questions": nb,
        })
    return {"quiz": result, "total": len(result)}


@app.post("/quiz/notify/{quiz_id}")
async def notify_quiz_manually(quiz_id: int):
    """
    Endpoint optionnel : appelable depuis Java pour forcer la notification
    d'un quiz spécifique dès son insertion en BDD.

    Exemple depuis Java (QuizService) :
        HttpClient.newHttpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8000/quiz/notify/" + quizId))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    """
    quiz = get_quiz_by_id(quiz_id)
    if not quiz:
        raise HTTPException(status_code=404, detail=f"Quiz #{quiz_id} introuvable en BDD")

    msg = format_quiz_notification(quiz)
    subs = load_subscribers()
    active = [uid for uid, info in subs.items() if info.get("subscribed")]

    sent, errors = 0, []
    for uid in active:
        try:
            await send_whatsapp(uid, msg)
            sent += 1
        except Exception as e:
            errors.append(f"{uid}: {str(e)}")

    return {
        "quiz_id": quiz_id,
        "titre": quiz["titre"],
        "notifiés": sent,
        "erreurs": errors,
    }


@app.post("/quiz/check")
async def force_check():
    """Force une vérification immédiate des nouveaux quiz (pour tests)."""
    await check_new_quizzes()
    return {"status": "Vérification effectuée", "quiz_connus": len(_known_quiz_ids)}


@app.get("/subscribers")
def list_subscribers():
    subs = load_subscribers()
    return {
        "abonnés": subs,
        "total_actifs": sum(1 for v in subs.values() if v.get("subscribed")),
    }

# ══════════════════════════════════════════════════════════════════════════════
# 📱  WEBHOOK WHATSAPP
# ══════════════════════════════════════════════════════════════════════════════

SUBSCRIBE_KEYWORDS   = ["abonner", "abonne", "subscribe", "notification", "notifier", "alert"]
UNSUBSCRIBE_KEYWORDS = ["stop", "désabonner", "desabonner", "unsubscribe", "arrêt", "arret"]
QUIZ_KEYWORDS        = ["quiz", "quizzes", "liste", "disponible", "cours"]
RESET_KEYWORDS       = ["reset", "recommencer", "effacer", "restart"]


@app.post("/whatsapp", response_class=PlainTextResponse)
async def whatsapp_webhook(
    request: Request,
    Body: str = Form(default=""),
    From: str = Form(...),
    NumMedia: str = Form(default="0"),
):
    # ── Validation signature Twilio ───────────────────────────────────────────
    auth_token = os.getenv("TWILIO_AUTH_TOKEN", "")
    if auth_token:
        signature  = request.headers.get("X-Twilio-Signature", "")
        form_data  = dict(await request.form())
        if not twilio_validator.validate(str(request.url), form_data, signature):
            return PlainTextResponse("Forbidden", status_code=403)

    user_id     = From
    user_message = Body.strip()
    lower        = user_message.lower()

    # ── Message vide ─────────────────────────────────────────────────────────
    if not user_message:
        return _twiml(
            "👋 Bienvenue sur *StudyFlow* ! 🎓\n\n"
            "Je suis votre assistant en français.\n\n"
            "📌 *Commandes disponibles :*\n"
            "🔔 *ABONNER* — recevoir les alertes de nouveaux quiz\n"
            "🔕 *STOP* — se désabonner\n"
            "📋 *QUIZ* — voir les quiz disponibles\n\n"
            "Ou posez-moi directement votre question !"
        )

    # ── Reset ─────────────────────────────────────────────────────────────────
    if any(k in lower for k in RESET_KEYWORDS):
        conversation_store.pop(user_id, None)
        return _twiml("✅ Conversation effacée. Comment puis-je vous aider ?")

    # ── S'abonner ─────────────────────────────────────────────────────────────
    if any(k in lower for k in SUBSCRIBE_KEYWORDS):
        subs = load_subscribers()
        subs[user_id] = {"subscribed": True, "joined": datetime.now().isoformat()}
        save_subscribers(subs)
        total = sum(1 for v in subs.values() if v.get("subscribed"))
        print(f"[subscribe] {user_id} abonné. Total actifs : {total}")
        return _twiml(
            "✅ *Vous êtes abonné(e) aux notifications StudyFlow !*\n\n"
            "Vous recevrez une alerte WhatsApp dès qu'un nouveau quiz\n"
            "est ajouté sur la plateforme, avec toutes ses informations.\n\n"
            "Pour vous désabonner, tapez *STOP*."
        )

    # ── Se désabonner ─────────────────────────────────────────────────────────
    if any(k in lower for k in UNSUBSCRIBE_KEYWORDS):
        subs = load_subscribers()
        if user_id in subs:
            subs[user_id]["subscribed"] = False
            save_subscribers(subs)
        return _twiml(
            "✅ Vous avez été désabonné(e) des notifications.\n"
            "Tapez *ABONNER* à tout moment pour vous réinscrire."
        )

    # ── Liste des quiz ────────────────────────────────────────────────────────
    if any(k in lower for k in QUIZ_KEYWORDS):
        quizzes = get_all_quizzes()
        if not quizzes:
            return _twiml("❌ Aucun quiz disponible pour le moment.")

        lines = [f"📋 *Quiz disponibles sur StudyFlow* ({len(quizzes)} au total)\n━━━━━━━━━━━━"]
        for q in quizzes[:8]:   # Max 8 pour ne pas surcharger WhatsApp
            date_str = ""
            if isinstance(q.get("date_creation"), datetime):
                date_str = q["date_creation"].strftime("%d/%m/%Y")
            nb = get_questions_count_by_quiz(q["id"])
            cours = q.get("cours_titre") or f"Cours #{q.get('course_id')}"
            lines.append(
                f"\n🎯 *{q['titre']}*\n"
                f"   ⏱️ {q['duree']} min | ❓ {nb} questions\n"
                f"   📚 {cours} | 📅 {date_str}"
            )
        if len(quizzes) > 8:
            lines.append(f"\n... et {len(quizzes) - 8} autres.")
        lines.append("\n━━━━━━━━━━━━\n_StudyFlow_ 📖")
        return _twiml("\n".join(lines))

    # ── Chat libre (Groq / LLaMA) ─────────────────────────────────────────────
    try:
        reply = chat_fr(user_id, user_message)
    except Exception as e:
        reply = f"⚠️ Erreur du service IA : {str(e)}\nVeuillez réessayer."

    return _twiml(reply)


def _twiml(message: str) -> PlainTextResponse:
    safe = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    xml = (
        '<?xml version="1.0" encoding="UTF-8"?>'
        f"<Response><Message>{safe}</Message></Response>"
    )
    return PlainTextResponse(xml, media_type="application/xml")
