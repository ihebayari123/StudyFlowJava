#!/usr/bin/env python3
"""
Script de prédiction du bonheur — best_happiness_model.pkl
Usage: python predict_happiness.py <heures_sommeil> <heures_etude> <age> <cafes_par_jour>
Features attendues par le modèle : heures_sommeil, heures_etude, age, cafes_par_jour
"""
import sys
import os
import warnings
warnings.filterwarnings('ignore')


def find_model():
    """
    Cherche best_happiness_model.pkl dans :
      1. Le répertoire du script lui-même
      2. Le working directory (défini par Java via pb.directory())
      3. Les répertoires parents (jusqu'à 8 niveaux)
    """
    model_name = 'best_happiness_model.pkl'

    # 1. Répertoire du script
    script_dir = os.path.dirname(os.path.abspath(__file__))
    candidate = os.path.join(script_dir, model_name)
    if os.path.exists(candidate):
        return candidate

    # 2. Working directory courant (Java le définit via pb.directory())
    candidate = os.path.join(os.getcwd(), model_name)
    if os.path.exists(candidate):
        return candidate

    # 3. Remonter depuis le répertoire du script
    current = script_dir
    for _ in range(8):
        candidate = os.path.join(current, model_name)
        if os.path.exists(candidate):
            return candidate
        parent = os.path.dirname(current)
        if parent == current:
            break
        current = parent

    # 4. Remonter depuis le working directory
    current = os.getcwd()
    for _ in range(8):
        candidate = os.path.join(current, model_name)
        if os.path.exists(candidate):
            return candidate
        parent = os.path.dirname(current)
        if parent == current:
            break
        current = parent

    raise FileNotFoundError(
        f"ERROR: {model_name} introuvable. "
        f"Script dir: {script_dir}, CWD: {os.getcwd()}"
    )


def predict(heures_sommeil, heures_etude, age, cafes_par_jour):
    import joblib
    import numpy as np

    model_path = find_model()
    model = joblib.load(model_path)

    # Construire le vecteur de features dans le bon ordre
    features = np.array([[
        float(heures_sommeil),
        float(heures_etude),
        float(age),
        float(cafes_par_jour)
    ]])

    # Si le modèle attend des feature names, utiliser un DataFrame
    try:
        import pandas as pd
        df = pd.DataFrame(features, columns=['heures_sommeil', 'heures_etude', 'age', 'cafes_par_jour'])
        prediction = model.predict(df)[0]
    except Exception:
        prediction = model.predict(features)[0]

    score = float(prediction)

    # Normaliser si le modèle retourne une valeur entre 0 et 1
    if 0.0 <= score <= 1.0:
        score = score * 100.0

    # Clamp entre 0 et 100
    score = max(0.0, min(100.0, score))

    print(f"{score:.1f}")


if __name__ == '__main__':
    if len(sys.argv) != 5:
        print("ERROR: Usage: predict_happiness.py <heures_sommeil> <heures_etude> <age> <cafes_par_jour>")
        sys.exit(1)
    try:
        predict(
            float(sys.argv[1]),
            float(sys.argv[2]),
            float(sys.argv[3]),
            float(sys.argv[4])
        )
    except Exception as ex:
        print(f"ERROR: {ex}")
        sys.exit(1)
