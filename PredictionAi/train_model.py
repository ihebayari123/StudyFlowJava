import pandas as pd
import numpy as np
import pymysql
import joblib
from scipy.sparse import hstack, csr_matrix
from sklearn.ensemble import RandomForestRegressor
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error

# ─── 1. Connexion DB ───────────────────────────────────────────
conn = pymysql.connect(
    host='localhost',
    user='root',
    password='',          # pas de mot de passe
    database='studyflow'
)

query = """
    SELECT p.nom, p.description, p.prix, tc.nom_categorie
    FROM produit p
    JOIN type_categorie tc ON p.type_categorie_id = tc.id
"""

df = pd.read_sql(query, conn)
conn.close()

print(f"✅ {len(df)} produits chargés")
print(df.head())

# ─── 2. Vérification minimum de données ───────────────────────
if len(df) < 5:
    print("❌ Pas assez de produits en base pour entraîner le modèle.")
    print("   Ajoute au moins 5 produits avec des prix variés et relance.")
    exit()

# ─── 3. Nettoyage ─────────────────────────────────────────────
df = df.dropna()
df = df[df['prix'] > 0]

# Combiner nom + description en un seul texte
df['texte'] = df['nom'] + ' ' + df['description']

# Encoder la catégorie en nombre
le = LabelEncoder()
df['categorie_enc'] = le.fit_transform(df['nom_categorie'])

print(f"📂 Catégories trouvées : {list(le.classes_)}")

# ─── 4. Features / Target ─────────────────────────────────────
X_text = df['texte']
X_cat  = df['categorie_enc'].values.reshape(-1, 1)
y      = df['prix'].values

# ─── 5. TF-IDF sur le texte ───────────────────────────────────
tfidf = TfidfVectorizer(max_features=500, ngram_range=(1, 2))
X_tfidf = tfidf.fit_transform(X_text)

# Combiner TF-IDF + catégorie
X_final = hstack([X_tfidf, csr_matrix(X_cat)])

# ─── 6. Entraînement ──────────────────────────────────────────
# Si moins de 10 produits, pas de split train/test
if len(df) < 10:
    print("⚠️  Peu de données : entraînement sans split test")
    model = RandomForestRegressor(
        n_estimators=100,
        max_depth=5,
        random_state=42,
        n_jobs=-1
    )
    model.fit(X_final, y)
    print("✅ Modèle entraîné (mode données limitées)")
else:
    X_train, X_test, y_train, y_test = train_test_split(
        X_final, y, test_size=0.2, random_state=42
    )
    model = RandomForestRegressor(
        n_estimators=200,
        max_depth=15,
        min_samples_split=3,
        random_state=42,
        n_jobs=-1
    )
    model.fit(X_train, y_train)
    y_pred = model.predict(X_test)
    mae = mean_absolute_error(y_test, y_pred)
    print(f"📊 MAE (erreur moyenne) : {mae:.2f} DT")

# ─── 7. Sauvegarde ────────────────────────────────────────────
joblib.dump(model, 'model.pkl')
joblib.dump(tfidf, 'tfidf.pkl')
joblib.dump(le,    'label_encoder.pkl')
print("✅ Modèle sauvegardé : model.pkl  tfidf.pkl  label_encoder.pkl")