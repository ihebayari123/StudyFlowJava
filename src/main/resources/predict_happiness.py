#!/usr/bin/env python3
"""
Script de prédiction du bonheur basé sur best_happiness_model.pkl
Usage: python predict_happiness.py <heures_sommeil> <heures_etude> <age> <cafes_par_jour>
"""
import sys
import os
import warnings
warnings.filterwarnings('ignore')

def find_model():
    """Cherche le fichier modèle dans les répertoires parents."""
    script_dir = os.path.dirname(os.path.abspath(__file__))
    current = script_dir
    for _ in range(8):
        candidate = os.path.join(current, 'best_happiness_model.pkl')
        if os.path.exists(candidate):
            return candidate
        current = os.path.dirname(current)
    raise FileNotFoundError("best_happiness_model.pkl introuvable")

def predict(heures_sommeil, heures_etude, age, cafes_par_jour):
    import joblib
    model_path = find_model()
    model = joblib.load(model_path)
    features = [[float(heures_sommeil), float(heures_etude), float(age), float(cafes_par_jour)]]
    prediction = model.predict(features)[0]
    score = float(prediction)
    # Si le score est entre 0 et 1, convertir en pourcentage
    if 0.0 <= score <= 1.0:
        score = score * 100.0
    print(f"{score:.1f}")

if __name__ == '__main__':
    if len(sys.argv) != 5:
        print("ERROR: Usage: predict_happiness.py <heures_sommeil> <heures_etude> <age> <cafes_par_jour>")
        sys.exit(1)
    try:
        predict(float(sys.argv[1]), float(sys.argv[2]), float(sys.argv[3]), float(sys.argv[4]))
    except Exception as ex:
        print(f"ERROR: {ex}")
        sys.exit(1)
