#!/usr/bin/env python3
"""
predict_happiness.py
Usage: python predict_happiness.py <sleep_hours> <study_hours> <age> <coffees_per_day>
Output: a single float (happiness score 0-100)

Features order expected by best_happiness_model.pkl:
  [sleep_hours, study_hours, age, coffees_per_day]

Uses joblib (not pickle) to handle cross-version sklearn compatibility.
"""

import sys
import os
import warnings
warnings.filterwarnings("ignore")


def find_model():
    """Search for best_happiness_model.pkl in multiple locations."""
    script_dir = os.path.dirname(os.path.abspath(__file__))

    candidates = [
        os.path.join(script_dir, "best_happiness_model.pkl"),
        os.path.join(script_dir, "..", "best_happiness_model.pkl"),
        os.path.join(script_dir, "..", "..", "best_happiness_model.pkl"),
        os.path.join(script_dir, "..", "..", "..", "best_happiness_model.pkl"),
        "best_happiness_model.pkl",
    ]

    # Also walk up from cwd
    cwd = os.getcwd()
    for _ in range(6):
        candidates.append(os.path.join(cwd, "best_happiness_model.pkl"))
        parent = os.path.dirname(cwd)
        if parent == cwd:
            break
        cwd = parent

    for path in candidates:
        normalized = os.path.normpath(path)
        if os.path.isfile(normalized):
            return normalized

    return None


def main():
    if len(sys.argv) != 5:
        print("ERROR: Usage: python predict_happiness.py "
              "<sleep_hours> <study_hours> <age> <coffees_per_day>")
        sys.exit(1)

    try:
        sleep_hours     = float(sys.argv[1])
        study_hours     = float(sys.argv[2])
        age             = float(sys.argv[3])
        coffees_per_day = float(sys.argv[4])
    except ValueError as e:
        print(f"ERROR: Invalid numeric argument: {e}")
        sys.exit(1)

    model_path = find_model()
    if model_path is None:
        print("ERROR: best_happiness_model.pkl not found")
        sys.exit(1)

    try:
        import joblib
        import numpy as np

        model = joblib.load(model_path)

        features   = np.array([[sleep_hours, study_hours, age, coffees_per_day]])
        prediction = model.predict(features)[0]

        # Clamp to [0, 100]
        score = max(0.0, min(100.0, float(prediction)))
        print(f"{score:.2f}")

    except ImportError as e:
        print(f"ERROR: Missing Python package: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"ERROR: Prediction failed: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
