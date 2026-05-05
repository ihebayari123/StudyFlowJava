from flask import Flask, request, jsonify
import joblib
import pymysql
from scipy.sparse import hstack, csr_matrix

app = Flask(__name__)

# ─── Charger le modèle ────────────────────────────────────────
model = joblib.load('model.pkl')
tfidf = joblib.load('tfidf.pkl')
le    = joblib.load('label_encoder.pkl')
print("✅ Modèle chargé, serveur prêt !")

# ─── Connexion DB ─────────────────────────────────────────────
def get_conn():
    return pymysql.connect(
        host='localhost',
        user='root',
        password='',
        database='studyflow',
        cursorclass=pymysql.cursors.DictCursor
    )

# ─── Route 1 : Prédire le prix ────────────────────────────────
@app.route('/predict', methods=['POST'])
def predict():
    data        = request.get_json()
    nom         = data.get('nom', '')
    description = data.get('description', '')
    categorie   = data.get('categorie', '')

    texte   = nom + ' ' + description
    X_tfidf = tfidf.transform([texte])

    if categorie in le.classes_:
        cat_enc = le.transform([categorie])[0]
    else:
        cat_enc = 0

    X_final = hstack([X_tfidf, csr_matrix([[cat_enc]])])

    # Prédiction + intervalle de confiance
    prix_predit = model.predict(X_final)[0]
    preds_trees = [tree.predict(X_final)[0] for tree in model.estimators_]
    prix_min    = round(min(preds_trees), 2)
    prix_max    = round(max(preds_trees), 2)

    return jsonify({
        'prix'     : round(float(prix_predit), 2),
        'prix_min' : prix_min,
        'prix_max' : prix_max
    })

# ─── Route 2 : Sauvegarder une prédiction ─────────────────────
@app.route('/log', methods=['POST'])
def log_prediction():
    data        = request.get_json()
    nom         = data.get('nom', '')
    description = data.get('description', '')
    categorie   = data.get('categorie', '')
    prix_predit = data.get('prix_predit', 0)
    prix_reel   = data.get('prix_reel', 0)
    ecart       = prix_reel - prix_predit

    conn = get_conn()
    try:
        with conn.cursor() as cursor:
            cursor.execute("""
                INSERT INTO prediction_log
                (nom_produit, description, categorie, prix_predit, prix_reel, ecart)
                VALUES (%s, %s, %s, %s, %s, %s)
            """, (nom, description, categorie, prix_predit, prix_reel, ecart))
        conn.commit()
    finally:
        conn.close()

    return jsonify({'status': 'ok'})

# ─── Route 3 : Stats pour le dashboard ────────────────────────
@app.route('/stats', methods=['GET'])
def stats():
    conn = get_conn()
    try:
        with conn.cursor() as cursor:

            # Nombre total de produits
            cursor.execute("SELECT COUNT(*) as total FROM produit")
            total_produits = cursor.fetchone()['total']

            # Catégories
            cursor.execute("SELECT COUNT(DISTINCT nom_categorie) as total FROM type_categorie")
            total_categories = cursor.fetchone()['total']

            # Nombre de prédictions
            cursor.execute("SELECT COUNT(*) as total FROM prediction_log")
            total_predictions = cursor.fetchone()['total']

            # MAE (erreur moyenne absolue)
            cursor.execute("SELECT AVG(ABS(ecart)) as mae FROM prediction_log WHERE prix_reel > 0")
            mae_result = cursor.fetchone()['mae']
            mae = round(float(mae_result), 2) if mae_result else 0

            # Dernières 5 prédictions
            cursor.execute("""
                SELECT nom_produit, prix_predit, prix_reel, ecart, date_prediction
                FROM prediction_log
                ORDER BY date_prediction DESC
                LIMIT 5
            """)
            derniers = cursor.fetchall()
            for d in derniers:
                d['date_prediction'] = str(d['date_prediction'])

            # Produits par catégorie
            cursor.execute("""
                SELECT tc.nom_categorie, COUNT(p.id) as total
                FROM type_categorie tc
                LEFT JOIN produit p ON p.type_categorie_id = tc.id
                GROUP BY tc.nom_categorie
            """)
            par_categorie = cursor.fetchall()

    finally:
        conn.close()

    return jsonify({
        'total_produits'   : total_produits,
        'total_categories' : total_categories,
        'total_predictions': total_predictions,
        'mae'              : mae,
        'derniers'         : derniers,
        'par_categorie'    : par_categorie
    })

if __name__ == '__main__':
    app.run(host='localhost', port=5000, debug=False)