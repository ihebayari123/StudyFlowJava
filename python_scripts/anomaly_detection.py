import sys
import json
import numpy as np
import mysql.connector
from sklearn.ensemble import IsolationForest
from datetime import datetime

# ═══════════════════════════════════════════
# CONNEXION MYSQL
# ═══════════════════════════════════════════
def get_connection():
    return mysql.connector.connect(
        host="localhost",
        user="root",
        password="",
        database="studyflow"
    )

# ═══════════════════════════════════════════
# EXTRAIRE LES FEATURES D'UN UTILISATEUR
# ═══════════════════════════════════════════
def extract_features(user):
    now = datetime.now()

    # Feature 1 : fréquence de connexion
    freq = user["login_frequency"] or 0

    # Feature 2 : jours depuis dernière connexion
    if user["last_login"]:
        delta = (now - user["last_login"]).days
        jours_inactif = min(delta, 365)
    else:
        jours_inactif = 365  # jamais connecté = suspect

    # Feature 3 : tentatives échouées
    failed = user["failed_login_attempts"] or 0

    # Feature 4 : ancienneté du compte (jours)
    if user["created_at"]:
        anciennete = (now - user["created_at"]).days
    else:
        anciennete = 0

    # Feature 5 : rôle (ADMIN = 3, FORMATEUR = 2, ETUDIANT = 1)
    role_map = {
    "ADMIN": 3, "ROLE_ADMIN": 3,
    "FORMATEUR": 2, "ENSEIGNANT": 2, "ROLE_ENSEIGNANT": 2,
    "ETUDIANT": 1, "ROLE_ETUDIANT": 1}
    role_score = role_map.get(user["role"], 1)

    # Feature 6 : statut (BLOQUE = 2, INACTIF = 1, ACTIF = 0)
    statut_map = {
    "BLOQUE": 2, "BLOQUÉ": 2,
    "INACTIF": 1,
    "ACTIF": 0}
    statut_score = statut_map.get(user["statut_compte"], 0)

    return [freq, jours_inactif, failed, anciennete, role_score, statut_score]

# ═══════════════════════════════════════════
# GÉNÉRER DONNÉES SIMULÉES POUR ENTRAÎNEMENT
# ═══════════════════════════════════════════
def generate_training_data():
    data = []

    # 35 utilisateurs NORMAUX
    for _ in range(35):
        freq          = np.random.randint(5, 50)      # connexions régulières
        jours_inactif = np.random.randint(0, 7)       # connecté récemment
        failed        = np.random.randint(0, 2)       # peu d'échecs
        anciennete    = np.random.randint(30, 500)    # compte ancien
        role_score    = np.random.choice([1, 2])      # étudiant ou formateur
        statut_score  = 0                             # actif
        data.append([freq, jours_inactif, failed, anciennete, role_score, statut_score])

    # 15 utilisateurs SUSPECTS
    for _ in range(15):
        freq          = np.random.randint(0, 3)       # presque jamais connecté
        jours_inactif = np.random.randint(60, 365)    # inactif longtemps
        failed        = np.random.randint(5, 20)      # beaucoup d'échecs
        anciennete    = np.random.randint(0, 10)      # compte très récent
        role_score    = np.random.choice([1, 3])
        statut_score  = np.random.choice([1, 2])      # inactif ou bloqué
        data.append([freq, jours_inactif, failed, anciennete, role_score, statut_score])

    return np.array(data)

# ═══════════════════════════════════════════
# CALCULER LES RAISONS
# ═══════════════════════════════════════════
def get_raisons(features, user):
    raisons = []
    freq, jours_inactif, failed, anciennete, role_score, statut_score = features

    if failed >= 5:
        raisons.append(f"{int(failed)} tentatives de connexion échouées")
    if jours_inactif >= 30:
        raisons.append(f"Inactif depuis {int(jours_inactif)} jours")
    if freq == 0:
        raisons.append("Jamais connecté")
    elif freq <= 2:
        raisons.append("Fréquence de connexion très faible")
    if anciennete <= 3:
        raisons.append("Compte créé il y a moins de 3 jours")
    if statut_score == 2:
        raisons.append("Compte bloqué")
    if statut_score == 1:
        raisons.append("Compte inactif")
    if role_score == 3 and failed >= 3:
        raisons.append("Compte ADMIN avec tentatives suspectes ⚠️")
    if not raisons:
        raisons.append("Comportement normal")

    return raisons

# ═══════════════════════════════════════════
# MAIN
# ═══════════════════════════════════════════
def calculate_rule_based_score(features, user):
    freq, jours_inactif, failed, anciennete, role_score, statut_score = features
    score = 0.0

    # Règle 1 — tentatives échouées (poids fort)
    if failed >= 10:
        score += 0.40
    elif failed >= 5:
        score += 0.25
    elif failed >= 3:
        score += 0.15

    # Règle 2 — statut bloqué ou inactif
    if statut_score == 2:  # BLOQUE
        score += 0.25
    elif statut_score == 1:  # INACTIF
        score += 0.10

    # Règle 3 — inactivité longue
    if jours_inactif >= 180:
        score += 0.15
    elif jours_inactif >= 60:
        score += 0.08

    # Règle 4 — jamais connecté
    if freq == 0:
        score += 0.10

    # Règle 5 — compte très récent avec activité suspecte
    if anciennete <= 3 and failed >= 2:
        score += 0.15

    # Règle 6 — ADMIN suspect (critique)
    if role_score == 3 and failed >= 3:
        score += 0.20

    return round(min(score, 1.0), 2)


def main():
    try:
        # Entraîner Isolation Forest (pour le score ML pur)
        training_data = generate_training_data()
        model = IsolationForest(
            n_estimators=200,
            contamination=0.25,
            random_state=42
        )
        model.fit(training_data)

        # Lire les vrais utilisateurs
        cnx = get_connection()
        cursor = cnx.cursor(dictionary=True)
        cursor.execute("""
            SELECT id, nom, prenom, email, role, statut_compte,
                   login_frequency, last_login,
                   failed_login_attempts, created_at
            FROM utilisateur
        """)
        users = cursor.fetchall()
        cursor.close()
        cnx.close()

        if not users:
            print(json.dumps({"error": "Aucun utilisateur trouvé"}))
            return

        results = []
        for user in users:
            features = extract_features(user)
            features_array = np.array(features).reshape(1, -1)

            # Score ML (Isolation Forest)
            raw_score = model.decision_function(features_array)[0]
            ml_score = float(1 - (raw_score + 0.5))
            ml_score = max(0.0, min(1.0, ml_score))

            # Score règles métier
            rule_score = calculate_rule_based_score(features, user)

            # Score final hybride : 40% ML + 60% règles métier
            final_score = round((0.4 * ml_score) + (0.6 * rule_score), 2)
            final_score = max(0.0, min(1.0, final_score))

            # Niveau
            if final_score >= 0.55:
                niveau = "HIGH"
            elif final_score >= 0.30:
                niveau = "MEDIUM"
            else:
                niveau = "LOW"

            raisons = get_raisons(features, user)

            results.append({
                "id":      int(user["id"]),
                "nom":     user["nom"],
                "prenom":  user["prenom"],
                "email":   user["email"],
                "role":    user["role"],
                "statut":  user["statut_compte"],
                "score":   final_score,
                "niveau":  niveau,
                "raisons": raisons
            })

        results.sort(key=lambda x: x["score"], reverse=True)
        print(json.dumps(results, ensure_ascii=False))

    except Exception as e:
        print(json.dumps({"error": str(e)}))

if __name__ == "__main__":
    main()
