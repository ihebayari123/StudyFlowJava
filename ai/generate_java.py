"""
generate_java.py
================
PDF  →  PyMuPDF (extraction)
      →  Chunking
      →  Ollama / Mistral (génération)
      →  JSON  →  stdout  (lu par Java QuizAIController)

Usage:
  py generate_java.py <pdf_path> <quiz_id>

Output:
  JSON sur stdout : {"questions": [...]}
  Erreurs sur stderr
"""

import sys
import json
import re
import os

# ── Fix encoding Windows ──────────────────────────────────────────────────────
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

# ── Dépendances ───────────────────────────────────────────────────────────────
try:
    import fitz          # PyMuPDF
except ImportError:
    print(json.dumps({"error": "PyMuPDF manquant: py -m pip install pymupdf"}))
    sys.exit(1)

try:
    import requests
except ImportError:
    print(json.dumps({"error": "requests manquant: py -m pip install requests"}))
    sys.exit(1)

# ── Arguments ─────────────────────────────────────────────────────────────────
if len(sys.argv) < 3:
    print(json.dumps({"error": "Usage: py generate_java.py <pdf_path> <quiz_id>"}))
    sys.exit(1)

pdf_path  = sys.argv[1]
quiz_id   = int(sys.argv[2])

# ── Ollama config ─────────────────────────────────────────────────────────────
OLLAMA_URL = "http://localhost:11434/api/chat"
MODEL      = "mistral"

# ── 1. Extraction texte PDF ───────────────────────────────────────────────────
if not os.path.exists(pdf_path):
    print(json.dumps({"error": f"PDF introuvable: {pdf_path}"}))
    sys.exit(1)

try:
    doc = fitz.open(pdf_path)
    raw_text = ""
    for page in doc:
        raw_text += page.get_text()
    doc.close()
except Exception as e:
    print(json.dumps({"error": f"Erreur lecture PDF: {e}"}))
    sys.exit(1)

if not raw_text.strip():
    print(json.dumps({"error": "Le PDF ne contient pas de texte extractible"}))
    sys.exit(1)

# ── 2. Chunking ───────────────────────────────────────────────────────────────
def split_chunks(text, size=500, overlap=50):
    words = text.split()
    chunks = []
    for i in range(0, len(words), size - overlap):
        chunk = " ".join(words[i : i + size])
        if len(chunk) > 80:
            chunks.append(chunk)
    return chunks

chunks = split_chunks(raw_text)
print(f"[INFO] {len(chunks)} chunk(s) extraits du PDF", file=sys.stderr)

# ── 3. Génération via Ollama (Mistral) ────────────────────────────────────────
def generate_questions(chunk: str, quiz_id: int):
    prompt = f"""Tu es un expert en création de quiz pédagogiques.
À partir du texte ci-dessous, génère exactement 3 questions en JSON :
- 1 de type "vrai_faux"
- 1 de type "choix_multiple" (4 choix A/B/C/D)
- 1 de type "texte" (réponse libre)

Pour chaque question, le champ "niveau" doit être "facile", "moyen" ou "difficile".

TEXTE : {chunk[:1200]}

Réponds UNIQUEMENT avec ce JSON valide (sans markdown, sans explication) :
{{
  "questions": [
    {{
      "texte": "...",
      "niveau": "facile",
      "indice": "...",
      "quiz_id": {quiz_id},
      "type": "vrai_faux",
      "choix_a": null,
      "choix_b": null,
      "choix_c": null,
      "choix_d": null,
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
      "choix_a": "...",
      "choix_b": "...",
      "choix_c": "...",
      "choix_d": "...",
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
      "choix_a": null,
      "choix_b": null,
      "choix_c": null,
      "choix_d": null,
      "bonne_reponse_choix": null,
      "bonne_reponse_bool": null,
      "reponse_attendue": "..."
    }}
  ]
}}"""

    try:
        resp = requests.post(
            OLLAMA_URL,
            json={
                "model": MODEL,
                "messages": [{"role": "user", "content": prompt}],
                "stream": False,
            },
            timeout=90,
        )
        raw = resp.json()["message"]["content"]
        # Nettoyer les balises markdown
        raw = re.sub(r"```json|```", "", raw).strip()
        match = re.search(r"\{.*\}", raw, re.DOTALL)
        if match:
            return json.loads(match.group())
    except Exception as e:
        print(f"[WARN] chunk ignoré: {e}", file=sys.stderr)
    return None

# ── 4. Agrégation ─────────────────────────────────────────────────────────────
all_questions = []
MAX_CHUNKS = 4   # Limiter pour éviter une attente trop longue

for i, chunk in enumerate(chunks[:MAX_CHUNKS]):
    print(f"[INFO] Traitement chunk {i+1}/{min(len(chunks), MAX_CHUNKS)}...", file=sys.stderr)
    result = generate_questions(chunk, quiz_id)
    if result and "questions" in result:
        all_questions.extend(result["questions"])

# ── 5. Sortie JSON sur stdout ─────────────────────────────────────────────────
print(json.dumps({"questions": all_questions}, ensure_ascii=False))
print(f"[INFO] {len(all_questions)} questions generees avec succes", file=sys.stderr)
