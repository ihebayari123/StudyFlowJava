# Stress Detection Model — Modèle Unifié

## Fichiers
- stress_model.pkl         : RandomForestClassifier entraîné
- stress_scaler.pkl        : StandardScaler
- stress_label_encoder.pkl : LabelEncoder (emploi_du_temps)

## Features d'entrée (ordre exact)
['eye_opening', 'blink_rate', 'head_angle', 'face_tension', 'source', 'heures_sommeil', 'niveau_fatigue', 'temps_travail', 'activite_physique', 'pauses', 'emploi_encoded']

## Classes de sortie
  0 → Low Stress
  1 → Medium Stress
  2 → High Stress

## Métriques (Test Set)
  Accuracy  : 0.8880
  Precision : 0.8992
  Recall    : 0.8880
  F1-Score  : 0.8899
  R² Score  : 0.9290
  AUC-ROC   : 0.9816
  CV F1     : 0.8856 ± 0.0191

## Utilisation
```python
import pickle, numpy as np

with open('stress_model.pkl', 'rb') as f:
    model = pickle.load(f)
with open('stress_scaler.pkl', 'rb') as f:
    scaler = pickle.load(f)

sample = np.array([[...]])          # features dans l'ordre ci-dessus
sample_sc = scaler.transform(sample)
prediction = model.predict(sample_sc)
proba      = model.predict_proba(sample_sc)
classes    = ['Low', 'Medium', 'High']
print(f"Stress Level : {classes[prediction[0]]}")
print(f"Probabilités : {dict(zip(classes, proba[0]))}")
```
