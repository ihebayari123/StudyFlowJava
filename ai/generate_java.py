"""
generate_java.py — StudyFlow
=============================
PDF  →  PyMuPDF (extraction)
      →  Chunking
      →  Groq / LLaMA-3 (génération rapide — ~2s par chunk)
      →  JSON  →  stdout  (lu par Java QuizAIController)

Avantage Groq vs Ollama :
  - Ollama/Mistral local : 30-90s par chunk
  - Groq/LLaMA-3        :  2-5s  par chunk  ← 15x plus rapide

Usage :
  py generate_java.py <pdf_path> <quiz_id>

Prérequis :
  pip install pymupdf requests
  GROQ_API_KEY dans ai/.env ou variable d'environnement

Output :
  JSON sur stdout : {"questions": [...]}
  Logs  sur stderr
"""

import sys
import json
import re
import os

# ── Encoding Windows ──────────────────────────────────────────────────────────
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

# ── Dépendances ───────────────────────────────────────────────────────────────
try:
    import fitz  # PyMuPDF
except ImportError:
    print(json.dumps({"error": "PyMuPDF manquant: pip install pymupdf"}))
    sys.exit(1)

try:
    import requests
except ImportError:
    print(json.dumps({"error": "requests manquant: pip install requests"}))
    sys.exit(1)

# ── Arguments ─────────────────────────────────────────────────────────────────
if len(sys.argv) < 3:
    print(json.dumps({"error": "Usage: py generate_java.py <pdf_path> <quiz_id>"}))
    sys.exit(1)

pdf_path = sys.argv[1]
quiz_id  = int(sys.argv[2])

# ── Config Groq ───────────────────────────────────────────────────────────────
GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
MODEL    = "llama-3.3-70b-versatile"

def load_api_key() -> str:
    """Cherche la clé Groq dans l'env ou dans ai/.env"""
    # 1. Variable d'environnement
    k = os.environ.get("GROQ_API_KEY", "").strip()
    if k:
        return k

    # 2. Fichier .env (cherche depuis le dossier du script vers la racine)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    candidates = [
        os.path.join(script_dir, ".env"),
        os.path.join(script_dir, "..", ".env"),
        os.path.join(script_dir, "..", "ai", ".env"),
    ]
    # Aussi depuis user.dir (racine projet)
    for base in [os.getcwd()]:
        candidates += [
            os.path.join(base, "ai", ".env"),
            os.path.join(base, ".env"),
        ]

    for path in candidates:
        path = os.path.normpath(path)
        if os.path.exists(path):
            with open(path, encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if line.startswith("GROQ_API_KEY="):
                        key = line[len("GROQ_API_KEY="):].strip()
                        if key:
                            print(f"[INFO] Clé Groq lue depuis : {path}", file=sys.stderr)
                            return key

    return ""

GROQ_API_KEY = load_api_key()
if not GROQ_API_KEY:
    print(json.dumps({"error": "GROQ_API_KEY introuvable. Ajoutez-la dans ai/.env"}))
    sys.exit(1)

print(f"[INFO] Groq configuré — modèle : {MODEL}", file=sys.stderr)

# ── 1. Extraction texte PDF ───────────────────────────────────────────────────
if not os.path.exists(pdf_path):
    print(json.dumps({"error": f"PDF introuvable: {pdf_path}"}))
    sys.exit(1)

try:
    doc      = fitz.open(pdf_path)
    raw_text = "".join(page.get_text() for page in doc)
    doc.close()
except Exception as e:
    print(json.dumps({"error": f"Erreur lecture PDF: {e}"}))
    sys.exit(1)

if not raw_text.strip():
    print(json.dumps({"error": "Le PDF ne contient pas de texte extractible"}))
    sys.exit(1)

print(f"[INFO] PDF lu — {len(raw_text)} caractères extraits", file=sys.stderr)

# ── 2. Chunking ───────────────────────────────────────────────────────────────
def split_chunks(text: str, size: int = 600, overlap: int = 60) -> list[str]:
    words  = text.split()
    chunks = []
    for i in range(0, len(words), size - overlap):
        chunk = " ".join(words[i: i + size])
        if len(chunk) > 80:
            chunks.append(chunk)
    return chunks

chunks = split_chunks(raw_text)
print(f"[INFO] {len(chunks)} chunk(s) extrait(s) du PDF", file=sys.stderr)

# ── 3. Génération via Groq ────────────────────────────────────────────────────
def generate_questions(chunk: str, quiz_id: int) -> dict | None:
    prompt = f"""Tu es un expert en création de quiz pédagogiques.
À partir du texte ci-dessous, génère exactement 3 questions en JSON :
- 1 de type "vrai_faux"
- 1 de type "choix_multiple" (4 choix A/B/C/D, indique laquelle est correcte)
- 1 de type "texte" (réponse libre courte)

Pour chaque question, le champ "niveau" doit être "facile", "moyen" ou "difficile".
Génère un indice utile mais qui ne révèle pas directement la réponse.

TEXTE :
{chunk[:1500]}

Réponds UNIQUEMENT avec ce JSON valide (sans markdown, sans explication) :
{{
  "questions": [
    {{
      "texte": "...",
      "niveau": "facile",
      "indice": "...",
      "quiz_id": {quiz_id},
      "type": "vrai_faux",
      "choix_a": null, "choix_b": null, "choix_c": null, "choix_d": null,
      "bonne_reponse_choix": null,
      "bonne_reponse_bool": true,
      "reponse_attendue": null
    }},
    {{
      "texte": "...",
      "niveau": "moyen",
      "indice": "...",
      "quiz_id": {quiz_id},
      "type": "choix_multiple",
      "choix_a": "...", "choix_b": "...", "choix_c": "...", "choix_d": "...",
      "bonne_reponse_choix": "a",
      "bonne_reponse_bool": null,
      "reponse_attendue": null
    }},
    {{
      "texte": "...",
      "niveau": "difficile",
      "indice": "...",
      "quiz_id": {quiz_id},
      "type": "texte",
      "choix_a": null, "choix_b": null, "choix_c": null, "choix_d": null,
      "bonne_reponse_choix": null,
      "bonne_reponse_bool": null,
      "reponse_attendue": "..."
    }}
  ]
}}"""

    headers = {
        "Authorization": f"Bearer {GROQ_API_KEY}",
        "Content-Type":  "application/json",
    }
    payload = {
        "model":       MODEL,
        "messages":    [{"role": "user", "content": prompt}],
        "temperature": 0.4,
        "max_tokens":  1200,
        "stream":      False,
    }

    try:
        resp = requests.post(GROQ_URL, headers=headers, json=payload, timeout=30)

        if resp.status_code != 200:
            print(f"[WARN] Groq HTTP {resp.status_code}: {resp.text[:200]}", file=sys.stderr)
            return None

        raw = resp.json()["choices"][0]["message"]["content"].strip()

        # Nettoyer les balises markdown éventuelles
        raw = re.sub(r"```json|```", "", raw).strip()

        # Extraire le JSON
        match = re.search(r"\{.*\}", raw, re.DOTALL)
        if match:
            result = json.loads(match.group())
            return result

    except requests.exceptions.Timeout:
        print("[WARN] Timeout Groq — chunk ignoré", file=sys.stderr)
    except json.JSONDecodeError as e:
        print(f"[WARN] JSON invalide : {e}", file=sys.stderr)
    except Exception as e:
        print(f"[WARN] Erreur : {e}", file=sys.stderr)

    return None

# ── 4. Agrégation ─────────────────────────────────────────────────────────────
MAX_CHUNKS    = 4   # 4 chunks × 3 questions = 12 questions max
all_questions = []

for i, chunk in enumerate(chunks[:MAX_CHUNKS]):
    print(f"[INFO] Chunk {i+1}/{min(len(chunks), MAX_CHUNKS)} → Groq...", file=sys.stderr)
    result = generate_questions(chunk, quiz_id)
    if result and "questions" in result:
        nb = len(result["questions"])
        all_questions.extend(result["questions"])
        print(f"[INFO] ✅ {nb} question(s) générée(s)", file=sys.stderr)
    else:
        print(f"[WARN] Chunk {i+1} ignoré", file=sys.stderr)

# ── 5. Sortie JSON ────────────────────────────────────────────────────────────
output = {"questions": all_questions}
print(json.dumps(output, ensure_ascii=False))
print(f"[INFO] Total : {len(all_questions)} questions générées avec succès", file=sys.stderr)