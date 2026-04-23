"""
analyze_eyes.py — Analyse des yeux en temps reel via webcam.
- Points verts sur les yeux en temps reel
- Prediction live toutes les 1.5s
- Son pip x3 a la fin + arret camera
- Sauvegarde image resume dans TEMP/sleep_result.png
- Output JSON sur stdout (une seule ligne)

Usage : python analyze_eyes.py [duration_seconds]
"""
import sys, json, os, time, warnings, tempfile, datetime
warnings.filterwarnings("ignore")

try:
    import cv2
    import numpy as np
    import joblib
    import pandas as pd
except ImportError as e:
    print(json.dumps({"error": f"Dependance manquante: {e}. pip install opencv-python numpy joblib pandas scikit-learn"}))
    sys.exit(1)

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
IMAGE_OUT  = os.path.join(tempfile.gettempdir(), "sleep_result.png")

try:
    model    = joblib.load(os.path.join(SCRIPT_DIR, "best_sleep_model.pkl"))
    scaler   = joblib.load(os.path.join(SCRIPT_DIR, "scaler.pkl"))
    features = joblib.load(os.path.join(SCRIPT_DIR, "features_list.pkl"))
except Exception as e:
    print(json.dumps({"error": f"Impossible de charger le modele: {e}"}))
    sys.exit(1)

DURATION = int(sys.argv[1]) if len(sys.argv) > 1 else 8

eye_cascade  = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_eye.xml")
face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")

GREEN  = (0, 255, 0)
CYAN   = (255, 255, 0)
WHITE  = (255, 255, 255)
RED    = (0, 0, 255)
YELLOW = (0, 255, 255)
BLUE   = (255, 100, 0)


def beep_triple():
    try:
        import winsound
        for _ in range(3):
            winsound.Beep(1000, 250)
            time.sleep(0.18)
    except Exception:
        try:
            import subprocess
            for _ in range(3):
                subprocess.run(["powershell", "-c", "[console]::beep(1000,250)"],
                               capture_output=True, timeout=2)
                time.sleep(0.18)
        except Exception:
            pass


def compute_ear(eye_gray):
    """
    Vrai EAR dynamique basé sur le profil vertical de luminosité.
    Un œil ouvert a un profil bright→dark(pupil)→bright.
    Un œil fermé a un profil uniforme sombre.
    Retourne une valeur entre 0.05 (fermé) et 0.45 (grand ouvert).
    """
    if eye_gray is None or eye_gray.size == 0:
        return 0.28
    h, w = eye_gray.shape[:2]
    if h < 4 or w < 4:
        return 0.28

    # Normaliser pour être robuste aux variations d'éclairage
    eye_norm = cv2.normalize(eye_gray, None, 0, 255, cv2.NORM_MINMAX).astype(np.float32)

    # Profil vertical : luminosité moyenne par ligne
    col_profile = np.mean(eye_norm, axis=1)

    # Trouver la zone sombre centrale (pupille/iris)
    threshold = float(np.mean(col_profile)) * 0.75
    dark_mask = col_profile < threshold
    dark_rows = int(np.sum(dark_mask))

    # EAR = proportion de lignes sombres (pupille visible = œil ouvert)
    ear = dark_rows / h

    # Variance verticale : œil ouvert = grande variance (bright-dark-bright)
    vert_var = float(np.var(col_profile)) / (128.0 ** 2)

    # Combiner les deux indicateurs
    ear_combined = ear * 0.6 + min(vert_var * 2.0, 0.4) * 0.4

    return round(float(np.clip(ear_combined, 0.05, 0.45)), 4)


def compute_redness(eye_bgr):
    """Score de rougeur dynamique basé sur le ratio R/G dans la région sclérale."""
    if eye_bgr is None or eye_bgr.size == 0:
        return 0.3
    b, g, r = cv2.split(eye_bgr)
    # Utiliser uniquement les pixels clairs (sclère, pas la pupille)
    bright_mask = (r.astype(np.float32) + g.astype(np.float32)) > 80
    if not np.any(bright_mask):
        return 0.3
    r_bright = float(np.mean(r[bright_mask]))
    g_bright = float(np.mean(g[bright_mask]))
    redness = r_bright / (g_bright + 1e-6)
    # Normaliser : 1.0 = neutre, >1.2 = rouge
    return round(float(np.clip((redness - 0.9) / 0.6, 0.0, 1.0)), 4)


def compute_dark_circles(face_gray, ex, ey, ew, eh):
    """Score de cernes : compare la luminosité sous l'œil vs la moyenne du visage."""
    under_y = ey + eh
    under_h = max(int(eh * 0.6), 5)
    if under_y + under_h > face_gray.shape[0]:
        return 0.3
    region = face_gray[under_y:under_y + under_h, ex:ex + ew]
    if region.size == 0:
        return 0.3
    face_mean = float(np.mean(face_gray))
    region_mean = float(np.mean(region))
    # Plus la région sous l'œil est sombre par rapport au visage = plus de cernes
    darkness_ratio = 1.0 - (region_mean / (face_mean + 1e-6))
    return round(float(np.clip(darkness_ratio, 0.0, 1.0)), 4)


def compute_pupil_dilation(eye_gray):
    """Dilatation pupillaire : ratio de la zone très sombre (pupille) sur l'œil total."""
    if eye_gray is None or eye_gray.size == 0:
        return 0.5
    eye_norm = cv2.normalize(eye_gray, None, 0, 255, cv2.NORM_MINMAX)
    # Seuil adaptatif : 30% des pixels les plus sombres
    threshold = float(np.percentile(eye_norm, 30))
    _, thresh = cv2.threshold(eye_norm, threshold, 255, cv2.THRESH_BINARY_INV)
    dark_ratio = float(np.sum(thresh > 0)) / (thresh.size + 1e-6)
    return round(float(np.clip(dark_ratio, 0.1, 0.9)), 4)


def draw_eye_landmarks(frame, fx, fy, ex, ey, ew, eh, ear):
    ax, ay = fx + ex, fy + ey
    color = GREEN if ear > 0.22 else YELLOW if ear > 0.15 else RED
    cv2.rectangle(frame, (ax, ay), (ax + ew, ay + eh), color, 2)
    pts = [
        (ax,           ay + eh // 2),
        (ax + ew,      ay + eh // 2),
        (ax + ew // 2, ay),
        (ax + ew // 2, ay + eh),
        (ax + ew // 4, ay + eh // 4),
        (ax + 3*ew//4, ay + eh // 4),
        (ax + ew // 4, ay + 3*eh//4),
        (ax + 3*ew//4, ay + 3*eh//4),
    ]
    for pt in pts:
        cv2.circle(frame, pt, 3, GREEN, -1)
    cv2.circle(frame, (ax + ew // 2, ay + eh // 2), 5, CYAN, -1)
    for a, b in [(0,4),(4,2),(2,5),(5,1),(1,7),(7,3),(3,6),(6,0)]:
        cv2.line(frame, pts[a], pts[b], GREEN, 1)
    cv2.putText(frame, f"EAR:{ear:.2f}", (ax, ay - 6),
                cv2.FONT_HERSHEY_SIMPLEX, 0.42, color, 1, cv2.LINE_AA)


def draw_hud(frame, elapsed, duration, metrics, live_pred, blink_count, face_detected):
    fh, fw = frame.shape[:2]
    progress = min(elapsed / duration, 1.0)
    bar_w = int(fw * progress)
    cv2.rectangle(frame, (0, 0), (fw, 8), (30, 30, 30), -1)
    bar_color = GREEN if progress < 0.7 else YELLOW if progress < 0.9 else RED
    cv2.rectangle(frame, (0, 0), (bar_w, 8), bar_color, -1)
    remaining = max(0, duration - int(elapsed))
    cv2.putText(frame, f"Analyse: {remaining}s restantes",
                (fw // 2 - 80, 22), cv2.FONT_HERSHEY_SIMPLEX, 0.5, WHITE, 1, cv2.LINE_AA)

    px, py, pw, ph = 10, 30, 220, 200
    ov = frame.copy()
    cv2.rectangle(ov, (px, py), (px+pw, py+ph), (10, 20, 35), -1)
    cv2.addWeighted(ov, 0.75, frame, 0.25, 0, frame)
    cv2.rectangle(frame, (px, py), (px+pw, py+ph), (0, 180, 0), 1)
    cv2.putText(frame, "METRIQUES OCULAIRES", (px+8, py+16),
                cv2.FONT_HERSHEY_SIMPLEX, 0.38, GREEN, 1, cv2.LINE_AA)
    rows = [
        ("EAR",         f"{metrics.get('ear_ratio',0):.3f}",          GREEN if metrics.get('ear_ratio',0)>0.22 else RED),
        ("Ouverture",   f"{metrics.get('eye_open_pct',74):.0f}%",  CYAN),
        ("Rougeur",     f"{metrics.get('redness_score',0):.3f}",       RED if metrics.get('redness_score',0)>0.5 else WHITE),
        ("Cernes",      f"{metrics.get('dark_circles_score',0):.3f}",  YELLOW),
        ("Clignements", f"{metrics.get('blink_rate_per_min',0):.0f}/min", WHITE),
        ("Ptosis",      f"{metrics.get('ptosis_score',0):.3f}",        RED if metrics.get('ptosis_score',0)>0.35 else WHITE),
        ("Stabilite",   f"{metrics.get('gaze_stability',0):.3f}",      GREEN),
    ]
    for i, (lbl, val, col) in enumerate(rows):
        y = py + 32 + i * 22
        cv2.putText(frame, f"{lbl}:", (px+8, y), cv2.FONT_HERSHEY_SIMPLEX, 0.36, (180,180,180), 1, cv2.LINE_AA)
        cv2.putText(frame, val,       (px+110, y), cv2.FONT_HERSHEY_SIMPLEX, 0.38, col, 1, cv2.LINE_AA)

    rx, ry, rw, rh = fw-200, 30, 190, 100
    ov2 = frame.copy()
    cv2.rectangle(ov2, (rx, ry), (rx+rw, ry+rh), (10, 20, 35), -1)
    cv2.addWeighted(ov2, 0.75, frame, 0.25, 0, frame)
    cv2.rectangle(frame, (rx, ry), (rx+rw, ry+rh), (124, 77, 255), 1)
    cv2.putText(frame, "PREDICTION LIVE", (rx+8, ry+16),
                cv2.FONT_HERSHEY_SIMPLEX, 0.38, (180,130,255), 1, cv2.LINE_AA)
    if live_pred:
        hv = live_pred["hours_slept"]
        st = live_pred["status"]
        sc = GREEN if st == "OK" else YELLOW if st == "insuffisant" else RED
        cv2.putText(frame, f"{hv:.1f}h", (rx+55, ry+55), cv2.FONT_HERSHEY_SIMPLEX, 1.2, sc, 2, cv2.LINE_AA)
        cv2.putText(frame, "de sommeil", (rx+35, ry+72), cv2.FONT_HERSHEY_SIMPLEX, 0.38, WHITE, 1, cv2.LINE_AA)
        cv2.putText(frame, st, (rx+50, ry+90), cv2.FONT_HERSHEY_SIMPLEX, 0.42, sc, 1, cv2.LINE_AA)
    else:
        cv2.putText(frame, "Calcul...", (rx+40, ry+55), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (180,180,180), 1, cv2.LINE_AA)

    stxt = "Visage detecte" if face_detected else "Cherche visage..."
    sclr = GREEN if face_detected else YELLOW
    cv2.putText(frame, stxt, (fw//2-70, fh-15), cv2.FONT_HERSHEY_SIMPLEX, 0.45, sclr, 1, cv2.LINE_AA)
    cv2.putText(frame, "SleepAI - Analyse en temps reel",
                (fw//2-130, fh-35), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (180,130,255), 1, cv2.LINE_AA)
    cv2.putText(frame, f"Clignements: {blink_count}",
                (px+8, py+ph+18), cv2.FONT_HERSHEY_SIMPLEX, 0.4, CYAN, 1, cv2.LINE_AA)


def predict_from_metrics(metrics_dict):
    try:
        df = pd.DataFrame([metrics_dict])[features]
        h = float(np.clip(model.predict(scaler.transform(df)), 0, 10)[0])
        h = round(h, 1)
        missing = round(max(0.0, 8.0 - h), 1)
        status = "OK" if h >= 7 else ("insuffisant" if h >= 5 else "CRITIQUE")
        return {"hours_slept": h, "missing_hours": missing, "status": status}
    except Exception:
        return None


def build_summary_image(metrics, result, last_frame):
    """
    Cree l'image resume avec:
    - Capture camera annotee (gauche) — prise au 3eme pip
    - Metriques + prediction (droite)
    """
    W, H = 800, 500
    img = np.zeros((H, W, 3), dtype=np.uint8)
    img[:] = (13, 27, 42)

    # ── Titre ────────────────────────────────────────────────────────
    cv2.rectangle(img, (0, 0), (W, 50), (20, 10, 60), -1)
    cv2.putText(img, "SleepAI - Rapport d'Analyse en Temps Reel", (W//2-195, 33),
                cv2.FONT_HERSHEY_SIMPLEX, 0.75, (180, 130, 255), 2, cv2.LINE_AA)

    # ── Capture camera annotee (gauche) ──────────────────────────────
    if last_frame is not None:
        cam = cv2.resize(last_frame, (360, 270))
        img[60:330, 10:370] = cam
        cv2.rectangle(img, (10, 60), (370, 330), (124, 77, 255), 2)
        # Badge "CAPTURE FINALE - 3eme PIP"
        cv2.rectangle(img, (10, 60), (200, 82), (124, 77, 255), -1)
        cv2.putText(img, "CAPTURE - 3eme PIP", (15, 76),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.38, WHITE, 1, cv2.LINE_AA)
        cv2.putText(img, "Capture en temps reel", (10, 348),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.40, (180, 130, 255), 1, cv2.LINE_AA)
    else:
        cv2.rectangle(img, (10, 60), (370, 330), (40, 40, 80), -1)
        cv2.putText(img, "Camera non disponible", (80, 200),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (120, 120, 180), 1, cv2.LINE_AA)

    # ── Panneau resultats (droite) ────────────────────────────────────
    h_val   = result["hours_slept"]
    missing = result["missing_hours"]
    status  = result["status"]
    sc = (0, 200, 80) if status == "OK" else (0, 200, 255) if status == "insuffisant" else (0, 80, 255)

    rx = 390
    cv2.rectangle(img, (rx, 60), (rx+390, 165), (20, 10, 50), -1)
    cv2.rectangle(img, (rx, 60), (rx+390, 165), sc, 2)
    cv2.putText(img, f"{h_val:.1f}h", (rx+20, 135),
                cv2.FONT_HERSHEY_SIMPLEX, 2.2, sc, 3, cv2.LINE_AA)
    cv2.putText(img, "de sommeil detectees", (rx+160, 100),
                cv2.FONT_HERSHEY_SIMPLEX, 0.48, WHITE, 1, cv2.LINE_AA)
    status_txt = {"OK": "SOMMEIL OPTIMAL", "insuffisant": "SOMMEIL INSUFFISANT"}.get(status, "MANQUE CRITIQUE")
    cv2.putText(img, status_txt, (rx+160, 128), cv2.FONT_HERSHEY_SIMPLEX, 0.52, sc, 1, cv2.LINE_AA)
    if missing > 0:
        cv2.putText(img, f"Manque: {missing:.1f}h", (rx+160, 155),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.44, (0, 200, 255), 1, cv2.LINE_AA)

    # Barre de sommeil
    bar_y = 178
    cv2.putText(img, f"Niveau: {h_val:.1f}h / 9h recommandees", (rx+10, bar_y),
                cv2.FONT_HERSHEY_SIMPLEX, 0.4, (180,180,180), 1, cv2.LINE_AA)
    cv2.rectangle(img, (rx+10, bar_y+8), (rx+370, bar_y+22), (30,30,60), -1)
    bar_fill = int(min(h_val / 9.0, 1.0) * 360)
    cv2.rectangle(img, (rx+10, bar_y+8), (rx+10+bar_fill, bar_y+22), sc, -1)

    # Metriques cles
    cv2.putText(img, "METRIQUES CLES (temps reel)", (rx+10, 222),
                cv2.FONT_HERSHEY_SIMPLEX, 0.42, (180,130,255), 1, cv2.LINE_AA)
    metric_display = [
        ("EAR (ouverture yeux)", f"{metrics.get('ear_ratio',0):.3f}"),
        ("Rougeur oculaire",     f"{metrics.get('redness_score',0):.3f}"),
        ("Cernes",               f"{metrics.get('dark_circles_score',0):.3f}"),
        ("Clignements/min",      f"{metrics.get('blink_rate_per_min',0):.1f}"),
        ("Ptosis (paupiere)",    f"{metrics.get('ptosis_score',0):.3f}"),
        ("Stabilite regard",     f"{metrics.get('gaze_stability',0):.3f}"),
    ]
    for i, (lbl, val) in enumerate(metric_display):
        y = 242 + i * 28
        cv2.rectangle(img, (rx+10, y-14), (rx+380, y+8), (20,20,45), -1)
        cv2.putText(img, lbl + ":", (rx+15, y), cv2.FONT_HERSHEY_SIMPLEX, 0.37, (180,180,180), 1, cv2.LINE_AA)
        cv2.putText(img, val,       (rx+280, y), cv2.FONT_HERSHEY_SIMPLEX, 0.42, CYAN, 1, cv2.LINE_AA)

    # Footer
    cv2.rectangle(img, (0, H-40), (W, H), (20, 10, 60), -1)
    ts = datetime.datetime.now().strftime("%d/%m/%Y %H:%M:%S")
    cv2.putText(img, f"Analyse realisee le {ts} | SleepAI powered by ML",
                (20, H-15), cv2.FONT_HERSHEY_SIMPLEX, 0.37, (120,120,180), 1, cv2.LINE_AA)

    cv2.imwrite(IMAGE_OUT, img)
    return IMAGE_OUT


def run_analysis():
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        return simulate_analysis(), None

    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
    cap.set(cv2.CAP_PROP_FPS, 30)
    cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)

    # Laisser la caméra se stabiliser
    for _ in range(5):
        cap.read()
        time.sleep(0.05)

    ret, _ = cap.read()
    if not ret:
        cap.release()
        return simulate_analysis(), None

    all_metrics = []
    blink_count = [0]
    prev_ear    = [0.28]
    prev_centers = []
    start_time  = time.time()
    live_pred   = None
    last_pred_t = 0
    last_frame  = None

    cv2.namedWindow("SleepAI - Analyse des yeux", cv2.WINDOW_NORMAL)
    cv2.resizeWindow("SleepAI - Analyse des yeux", 700, 540)

    while True:
        elapsed = time.time() - start_time
        if elapsed >= DURATION:
            break

        ret, frame = cap.read()
        if not ret:
            break

        # Égalisation histogramme pour meilleure détection
        gray     = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        gray_eq  = cv2.equalizeHist(gray)

        # Détection visage avec paramètres optimisés
        faces = face_cascade.detectMultiScale(gray_eq, 1.05, 3, minSize=(60, 60))
        face_detected = len(faces) > 0

        current_metrics = {
            "ear_ratio": 0.29, "eye_open_pct": 74.0, "redness_score": 0.36,
            "pupil_dilation": 0.47, "blink_rate_per_min": 16.0, "ptosis_score": 0.34,
            "eye_asymmetry": 0.10, "saccade_speed": 266.0, "dark_circles_score": 0.30,
            "lid_droop_score": 0.31, "gaze_stability": 0.75, "conjunctival_redness": 0.31
        }
        eyes_data = []

        if face_detected:
            fx, fy, fw, fh = faces[0]
            cv2.rectangle(frame, (fx, fy), (fx+fw, fy+fh), BLUE, 2)
            cv2.putText(frame, "Visage", (fx, fy-6), cv2.FONT_HERSHEY_SIMPLEX, 0.45, BLUE, 1, cv2.LINE_AA)

            # Utiliser uniquement la zone supérieure du visage (60%) pour les yeux
            eye_zone_h = int(fh * 0.60)
            face_gray_eq = gray_eq[fy:fy + eye_zone_h, fx:fx + fw]
            face_gray    = gray[fy:fy + fh, fx:fx + fw]
            face_color   = frame[fy:fy + fh, fx:fx + fw]

            eyes = eye_cascade.detectMultiScale(face_gray_eq, 1.05, 2, minSize=(18, 18))

            for (ex, ey, ew, eh) in eyes[:2]:
                # Récupérer la région dans le gray original (non équalisé) pour les métriques
                eye_gray  = gray[fy + ey:fy + ey + eh, fx + ex:fx + ex + ew]
                eye_color = frame[fy + ey:fy + ey + eh, fx + ex:fx + ex + ew]

                ear   = compute_ear(eye_gray)
                red   = compute_redness(eye_color)
                pupil = compute_pupil_dilation(eye_gray)
                dark  = compute_dark_circles(face_gray, ex, ey, ew, eh)
                center = (fx + ex + ew // 2, fy + ey + eh // 2)

                # Détection clignement : EAR chute sous 0.15
                if prev_ear[0] > 0.18 and ear < 0.15:
                    blink_count[0] += 1
                prev_ear[0] = ear

                draw_eye_landmarks(frame, fx, fy, ex, ey, ew, eh, ear)
                eyes_data.append({"ear": ear, "redness": red, "pupil": pupil,
                                   "dark": dark, "center": center, "h": eh, "w": ew})

            if eyes_data:
                e0 = eyes_data[0]
                # EAR → eye_open_pct : le modèle attend des valeurs en % (0-100)
                # Mapping EAR [0.05-0.45] → eye_open_pct [30-100]
                eye_open_pct = float(np.clip((e0["ear"] - 0.05) / 0.40 * 70 + 30, 30.0, 100.0))
                # ptosis : paupière tombante = EAR faible → ptosis élevé [0-1]
                ptosis = float(np.clip(1.0 - e0["ear"] / 0.35, 0.0, 1.0))
                # lid_droop similaire
                lid_droop = float(np.clip(0.8 - e0["ear"] * 2.0, 0.0, 0.8))

                current_metrics.update({
                    "ear_ratio":            e0["ear"],
                    "eye_open_pct":         round(eye_open_pct, 2),   # en %
                    "redness_score":        e0["redness"],
                    "pupil_dilation":       e0["pupil"],
                    "dark_circles_score":   e0["dark"],
                    "conjunctival_redness": round(e0["redness"] * 0.85, 4),
                    "ptosis_score":         round(ptosis, 4),
                    "lid_droop_score":      round(lid_droop, 4),
                })

            if len(eyes_data) >= 2:
                e0, e1 = eyes_data[0], eyes_data[1]
                current_metrics["eye_asymmetry"] = round(abs(e0["ear"] - e1["ear"]), 4)
                if prev_centers:
                    # saccade_speed : le modèle attend des pixels (mean=266, std=50)
                    dx = abs(e0["center"][0] - prev_centers[0][0])
                    dy = abs(e0["center"][1] - prev_centers[0][1])
                    saccade_px = float(dx + dy)  # en pixels, pas normalisé
                    current_metrics["saccade_speed"]  = round(saccade_px, 2)
                    # gaze_stability : inverse normalisé [0-1]
                    current_metrics["gaze_stability"] = round(float(np.clip(1.0 - saccade_px / 500.0, 0.0, 1.0)), 4)

            prev_centers = [e["center"] for e in eyes_data]

        elapsed_safe = max(elapsed, 0.1)
        current_metrics["blink_rate_per_min"] = round(blink_count[0] / elapsed_safe * 60, 1)
        all_metrics.append(current_metrics.copy())

        # Prédiction live toutes les 1.5s (dès qu'on a assez de données)
        if elapsed - last_pred_t >= 1.5 and len(all_metrics) >= 5:
            avg_so_far = {k: float(np.mean([m[k] for m in all_metrics])) for k in all_metrics[0]}
            live_pred = predict_from_metrics(avg_so_far)
            last_pred_t = elapsed

        draw_hud(frame, elapsed, DURATION, current_metrics, live_pred, blink_count[0], face_detected)
        last_frame = frame.copy()
        cv2.imshow("SleepAI - Analyse des yeux", frame)

        key = cv2.waitKey(1) & 0xFF
        if key in (ord('q'), 27):
            break

    # Son pip x3 puis fermeture caméra
    # La capture finale est prise AVANT les pips (dernier frame de l'analyse)
    # last_frame contient déjà le dernier frame avec les landmarks
    beep_triple()
    cap.release()
    cv2.destroyAllWindows()

    if not all_metrics:
        return simulate_analysis(), None

    # Moyenne finale sur toutes les métriques collectées
    avg = {k: round(float(np.mean([m[k] for m in all_metrics])), 4) for k in all_metrics[0]}
    # Blink rate final précis
    total_time = max(time.time() - start_time, 1)
    avg["blink_rate_per_min"] = round(blink_count[0] / total_time * 60, 1)
    return avg, last_frame


def simulate_analysis():
    import random
    rng = random.Random(int(time.time()) % 100)
    return {
        "ear_ratio": round(rng.uniform(0.15,0.35),4), "eye_open_pct": round(rng.uniform(0.3,0.8),4),
        "redness_score": round(rng.uniform(0.2,0.7),4), "pupil_dilation": round(rng.uniform(0.3,0.7),4),
        "blink_rate_per_min": round(rng.uniform(8,30),1), "ptosis_score": round(rng.uniform(0.0,0.5),4),
        "eye_asymmetry": round(rng.uniform(0.0,0.3),4), "saccade_speed": round(rng.uniform(0.3,0.9),4),
        "dark_circles_score": round(rng.uniform(0.1,0.6),4), "lid_droop_score": round(rng.uniform(0.0,0.4),4),
        "gaze_stability": round(rng.uniform(0.4,0.9),4), "conjunctival_redness": round(rng.uniform(0.1,0.5),4),
    }


def build_result(metrics, last_frame):
    result = predict_from_metrics(metrics)
    if result is None:
        result = {"hours_slept": 6.0, "missing_hours": 2.0, "status": "insuffisant"}
    h, missing, status = result["hours_slept"], result["missing_hours"], result["status"]

    if status == "OK":
        msg = f"Excellent ! Vous avez dormi environ {h}h cette nuit. Votre niveau de repos est optimal."
        rec = "Continuez sur cette lancee ! Un sommeil regulier de 7-9h ameliore la memoire et la concentration."
    elif status == "insuffisant":
        msg = f"Attention ! Votre analyse indique environ {h}h de sommeil, soit {missing}h de moins que recommande."
        rec = "Essayez de vous coucher 30 min plus tot ce soir. Evitez les ecrans 1h avant le coucher."
    else:
        msg = f"Alerte critique ! Seulement {h}h de sommeil detectees - il vous manque {missing}h."
        rec = "Faites une sieste de 20 min maintenant. Consultez un medecin si ce manque est chronique."

    details = []
    if metrics.get("redness_score",0) > 0.5:
        details.append("Yeux rouges detectes - signe de fatigue oculaire.")
    if metrics.get("dark_circles_score",0) > 0.4:
        details.append("Cernes prononces - indicateur de manque de sommeil.")
    if metrics.get("blink_rate_per_min",18) < 10:
        details.append("Taux de clignement faible - yeux secs, fatigue avancee.")
    if metrics.get("ptosis_score",0) > 0.35:
        details.append("Paupieres tombantes detectees - somnolence importante.")

    image_path = build_summary_image(metrics, result, last_frame)

    return {
        "hours_slept": h, "missing_hours": missing, "status": status,
        "message": msg, "recommendation": rec, "details": details,
        "metrics": metrics, "simulated": (last_frame is None),
        "image_path": image_path
    }


if __name__ == "__main__":
    try:
        result = run_analysis()
        if isinstance(result, tuple):
            metrics, last_frame = result
        else:
            metrics, last_frame = result, None
        output = build_result(metrics, last_frame)
        print(json.dumps(output))
    except Exception as e:
        print(json.dumps({"error": str(e)}))
        sys.exit(1)
