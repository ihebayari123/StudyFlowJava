"""
main.py — StudyFlow WhatsApp Bot (Français)
============================================
Fonctionnalités :
  1. Chatbot WhatsApp — répond aux questions sur les quiz en BDD
  2. Surveille la BDD et notifie une liste FIXE de numéros dès qu'un quiz est ajouté
  3. Endpoint /quiz/notify/{id} appelable depuis Java

═══════════════════════════════════════════════════════
 ▶ LISTE DES NUMÉROS À NOTIFIER — modifiez ici :
═══════════════════════════════════════════════════════
"""

# ══════════════════════════════════════════════════════════════════════════════
# 📋  NUMÉROS À NOTIFIER — MODIFIEZ CETTE LISTE
#     Format : "whatsapp:+INDICATIF_NUMÉRO"
#     Exemple Tunisie : "whatsapp:+21612345678"
# ══════════════════════════════════════════════════════════════════════════════

NUMEROS_A_NOTIFIER = [
    "whatsapp:+21694675039",   # ← remplacez par votre numéro

]

# ══════════════════════════════════════════════════════════════════════════════

from dotenv import load_dotenv
from datetime import datetime
import asyncio, os, json
import pymysql, pymysql.cursors

from groq import Groq
from twilio.rest import Client as TwilioClient
from fastapi import FastAPI, Request, Form, HTTPException
from fastapi.responses import PlainTextResponse
from fastapi.middleware.cors import CORSMiddleware

load_dotenv()

# ── Clients ───────────────────────────────────────────────────────────────────
app = FastAPI(title="StudyFlow WhatsApp Bot")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

groq_client   = Groq(api_key=os.getenv("GROQ_API_KEY"))
twilio_client = TwilioClient(os.getenv("TWILIO_ACCOUNT_SID"), os.getenv("TWILIO_AUTH_TOKEN"))
TWILIO_NUMBER = os.getenv("TWILIO_WHATSAPP_NUMBER", "whatsapp:+14155238886")

# ── BDD ───────────────────────────────────────────────────────────────────────
DB_CONFIG = {
    "host":        os.getenv("DB_HOST",     "127.0.0.1"),
    "port":        int(os.getenv("DB_PORT", "3306")),
    "db":          os.getenv("DB_NAME",     "studyflow"),
    "user":        os.getenv("DB_USER",     "root"),
    "password":    os.getenv("DB_PASSWORD", ""),
    "charset":     "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor,
}

# ══════════════════════════════════════════════════════════════════════════════
# 🗄️  FONCTIONS BASE DE DONNÉES
# ══════════════════════════════════════════════════════════════════════════════

def db():
    return pymysql.connect(**DB_CONFIG)

def get_all_quizzes() -> list:
    try:
        conn = db()
        with conn.cursor() as cur:
            cur.execute("""
                        SELECT q.id, q.titre, q.duree, q.date_creation, q.course_id,
                               c.titre AS cours_titre,
                               (SELECT COUNT(*) FROM question WHERE quiz_id = q.id) AS nb_questions
                        FROM quiz q
                                 LEFT JOIN cours c ON q.course_id = c.id
                        ORDER BY q.date_creation DESC
                        """)
            rows = cur.fetchall()
        conn.close()
        return rows
    except Exception as e:
        print(f"[db] {e}")
        return []

def get_quiz_by_id(quiz_id: int):
    try:
        conn = db()
        with conn.cursor() as cur:
            cur.execute("""
                        SELECT q.id, q.titre, q.duree, q.date_creation, q.course_id,
                               c.titre AS cours_titre, c.description AS cours_description,
                               (SELECT COUNT(*) FROM question WHERE quiz_id = q.id) AS nb_questions
                        FROM quiz q
                                 LEFT JOIN cours c ON q.course_id = c.id
                        WHERE q.id = %s
                        """, (quiz_id,))
            row = cur.fetchone()
        conn.close()
        return row
    except Exception as e:
        print(f"[db] {e}")
        return None

def format_date(d):
    if isinstance(d, datetime):
        return d.strftime("%d/%m/%Y à %H:%M")
    return str(d) if d else "—"

# ══════════════════════════════════════════════════════════════════════════════
# 🔔  NOTIFICATION D'UN NOUVEAU QUIZ
# ══════════════════════════════════════════════════════════════════════════════

def format_notif(quiz: dict) -> str:
    cours = quiz.get("cours_titre") or ("Cours #" + str(quiz.get("course_id", "—")))
    nb    = quiz.get("nb_questions", 0)
    lines = [
        "🎓 *Nouveau Quiz — StudyFlow*",
        "━━━━━━━━━━━━━━━━━━━━",
        f"📌 *Titre :* {quiz.get('titre', '—')}",
        f"🆔 *ID :* {quiz.get('id', '—')}",
        f"⏱️ *Durée :* {quiz.get('duree', '—')} min",
        f"📅 *Créé le :* {format_date(quiz.get('date_creation'))}",
        f"📚 *Cours :* {cours}",
        f"❓ *Questions :* {nb if nb > 0 else 'aucune encore'}",
        "━━━━━━━━━━━━━━━━━━━━",
        "_StudyFlow — Plateforme d'apprentissage_ 📖",
    ]
    if quiz.get("cours_description"):
        lines.insert(7, f"📝 *Description :* {quiz['cours_description']}")
    return "\n".join(lines)

def send_sms(to: str, body: str):
    twilio_client.messages.create(from_=TWILIO_NUMBER, to=to, body=body)

# ══════════════════════════════════════════════════════════════════════════════
# ⏰  SURVEILLANCE AUTOMATIQUE (toutes les 30 secondes)
# ══════════════════════════════════════════════════════════════════════════════

_known_ids: set = set()

async def check_new_quizzes():
    global _known_ids
    quizzes = get_all_quizzes()
    if not quizzes:
        return

    # Premier lancement — mémoriser sans notifier
    if not _known_ids:
        _known_ids = {q["id"] for q in quizzes}
        print(f"[watcher] Init : {len(_known_ids)} quiz connus")
        return

    new = [q for q in quizzes if q["id"] not in _known_ids]
    for quiz in new:
        _known_ids.add(quiz["id"])
        msg = format_notif(quiz)
        print(f"[watcher] Nouveau quiz #{quiz['id']} — {quiz['titre']}")
        for numero in NUMEROS_A_NOTIFIER:
            try:
                send_sms(numero, msg)
                print(f"[watcher] ✅ Notifié : {numero}")
            except Exception as e:
                print(f"[watcher] ❌ {numero} : {e}")

async def scheduler():
    while True:
        try:
            await check_new_quizzes()
        except Exception as e:
            print(f"[scheduler] {e}")
        await asyncio.sleep(30)

@app.on_event("startup")
async def startup():
    asyncio.create_task(scheduler())
    quizzes = get_all_quizzes()
    print(f"✅ StudyFlow Bot démarré | {len(quizzes)} quiz en BDD")
    print(f"📱 Numéros notifiés : {NUMEROS_A_NOTIFIER}")

# ══════════════════════════════════════════════════════════════════════════════
# 💬  CHATBOT — QUESTIONS SUR LES QUIZ
# ══════════════════════════════════════════════════════════════════════════════

conversation_store: dict = {}
MAX_HISTORY = 14

SYSTEM_PROMPT = """Tu es l'assistant officiel de la plateforme *StudyFlow* 🎓
Tu parles TOUJOURS en français.

Tu peux répondre aux questions sur les quiz : nombre, noms, durées, cours associés, dates.
Tu as accès aux données réelles de la BDD qui te seront fournies dans le message.

Style : concis, adapté WhatsApp, professionnel mais chaleureux, emojis modérés.
Ne donne jamais d'informations personnelles.
"""

def build_context() -> str:
    """Construit un résumé textuel de tous les quiz pour le contexte IA."""
    quizzes = get_all_quizzes()
    if not quizzes:
        return "Aucun quiz disponible actuellement."
    lines = [f"Il y a {len(quizzes)} quiz dans la base de données StudyFlow :\n"]
    for q in quizzes:
        cours = q.get("cours_titre") or f"Cours #{q.get('course_id')}"
        nb    = q.get("nb_questions", 0)
        date  = format_date(q.get("date_creation"))
        lines.append(
            f"- [{q['id']}] *{q['titre']}* | {q['duree']} min | "
            f"{nb} question(s) | Cours: {cours} | Créé: {date}"
        )
    return "\n".join(lines)

def chat(user_id: str, message: str) -> str:
    history = conversation_store.setdefault(user_id, [])

    # Injecter le contexte BDD dans chaque message utilisateur
    context = build_context()
    message_avec_contexte = f"[Données BDD en temps réel]\n{context}\n\n[Question de l'utilisateur]\n{message}"

    history.append({"role": "user", "content": message_avec_contexte})
    if len(history) > MAX_HISTORY:
        history[:] = history[-MAX_HISTORY:]

    resp = groq_client.chat.completions.create(
        model="llama-3.3-70b-versatile",
        messages=[{"role": "system", "content": SYSTEM_PROMPT}] + history,
        temperature=0.3,
        max_tokens=400,
    )
    reply = resp.choices[0].message.content.strip()

    # Sauvegarder la réponse sans le contexte BDD (pour ne pas polluer l'historique)
    history[-1] = {"role": "user", "content": message}
    history.append({"role": "assistant", "content": reply})
    return reply

# ══════════════════════════════════════════════════════════════════════════════
# 🌐  ENDPOINTS REST
# ══════════════════════════════════════════════════════════════════════════════

@app.get("/")
def root():
    quizzes = get_all_quizzes()
    return {
        "status": "✅ StudyFlow Bot actif",
        "quiz_en_bdd": len(quizzes),
        "numeros_notifies": NUMEROS_A_NOTIFIER,
    }

@app.get("/quiz")
def list_quizzes():
    quizzes = get_all_quizzes()
    return {"quiz": [
        {
            "id": q["id"],
            "titre": q["titre"],
            "duree_min": q["duree"],
            "date_creation": str(q["date_creation"]),
            "cours": q.get("cours_titre"),
            "nb_questions": q.get("nb_questions", 0),
        }
        for q in quizzes
    ], "total": len(quizzes)}

@app.post("/quiz/notify/{quiz_id}")
async def notify_java(quiz_id: int):
    """Appelé depuis Java après addEntity() pour notifier immédiatement."""
    quiz = get_quiz_by_id(quiz_id)
    if not quiz:
        raise HTTPException(404, f"Quiz #{quiz_id} introuvable")

    msg   = format_notif(quiz)
    sent, errors = 0, []
    for numero in NUMEROS_A_NOTIFIER:
        try:
            send_sms(numero, msg)
            sent += 1
        except Exception as e:
            errors.append(f"{numero}: {e}")

    return {"quiz_id": quiz_id, "titre": quiz["titre"], "notifiés": sent, "erreurs": errors}

@app.post("/quiz/check")
async def force_check():
    await check_new_quizzes()
    return {"status": "Vérification effectuée"}

# ══════════════════════════════════════════════════════════════════════════════
# 📱  WEBHOOK WHATSAPP
# ══════════════════════════════════════════════════════════════════════════════

@app.post("/whatsapp", response_class=PlainTextResponse)
async def webhook(
        request: Request,
        Body: str = Form(default=""),
        From: str = Form(...),
):
    msg   = Body.strip()
    lower = msg.lower()

    # Message vide → message d'accueil
    if not msg:
        return twiml(
            "👋 Bonjour ! Je suis l'assistant *StudyFlow* 🎓\n\n"
            "Posez-moi n'importe quelle question sur les quiz !\n\n"
            "Exemples :\n"
            "• _Combien de quiz y a-t-il ?_\n"
            "• _Quels sont les quiz disponibles ?_\n"
            "• _Parle-moi du quiz Java_\n"
            "• _Quel quiz dure le moins longtemps ?_"
        )

    # Reset conversation
    if lower in ["reset", "recommencer", "effacer"]:
        conversation_store.pop(From, None)
        return twiml("✅ Conversation effacée !")

    # Chat IA avec contexte BDD
    try:
        reply = chat(From, msg)
    except Exception as e:
        reply = f"⚠️ Erreur : {str(e)}"

    return twiml(reply)

def twiml(message: str) -> PlainTextResponse:
    safe = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return PlainTextResponse(
        '<?xml version="1.0" encoding="UTF-8"?>'
        f"<Response><Message>{safe}</Message></Response>",
        media_type="application/xml"
    )