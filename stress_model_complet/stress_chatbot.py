#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
stress_chatbot.py  —  Pipeline complet stress + visage + PDF
Dépendances : opencv-python (inclus), reportlab (pip install reportlab)
Usage:
  python stress_chatbot.py predict  <score> <sleep> <study>
  python stress_chatbot.py capture  <output_image_path>
  python stress_chatbot.py pdf  <name> <score> <level> <photo_path> <output_pdf>
"""

import sys, os, json, warnings

# Supprimer les warnings sklearn (version mismatch) — Java reçoit du JSON propre
warnings.filterwarnings("ignore")
os.environ["PYTHONWARNINGS"] = "ignore"

import pickle
import numpy as np

MODEL_DIR = os.path.dirname(os.path.abspath(__file__))

# ─────────────────────────────────────────────────────────────────────────────
# 1. PREDICT  — prédiction stress en temps réel
# ─────────────────────────────────────────────────────────────────────────────
def predict(score_wb, sleep_hours, study_hours):
    try:
        with open(os.path.join(MODEL_DIR, 'stress_model.pkl'), 'rb') as f:
            model = pickle.load(f)
        with open(os.path.join(MODEL_DIR, 'stress_scaler.pkl'), 'rb') as f:
            scaler = pickle.load(f)
        with open(os.path.join(MODEL_DIR, 'stress_label_encoder.pkl'), 'rb') as f:
            le = pickle.load(f)

        score_wb   = float(score_wb)
        sleep_h    = float(sleep_hours)
        study_h    = float(study_hours)

        # Dériver les features à partir des données disponibles
        # ['eye_opening','blink_rate','head_angle','face_tension','source',
        #  'heures_sommeil','niveau_fatigue','temps_travail','activite_physique','pauses','emploi_encoded']
        eye_opening    = 0.35
        blink_rate     = 18.0
        head_angle     = 2.0
        face_tension   = max(0.0, 1.0 - score_wb / 100.0)
        source         = 1.0
        heures_sommeil = sleep_h
        niveau_fatigue = max(0.0, min(10.0, 10.0 - sleep_h))
        temps_travail  = study_h
        activite_phys  = 1.0 if score_wb > 60 else 0.0
        pauses         = max(1.0, study_h / 2.0)
        emploi_encoded = 0.0  # default schedule class

        features = np.array([[eye_opening, blink_rate, head_angle, face_tension,
                               source, heures_sommeil, niveau_fatigue, temps_travail,
                               activite_phys, pauses, emploi_encoded]])

        features_sc = scaler.transform(features)
        pred        = model.predict(features_sc)[0]
        proba       = model.predict_proba(features_sc)[0]
        classes     = ['Low', 'Medium', 'High']
        level       = classes[pred]
        confidence  = round(float(proba[pred]) * 100, 1)

        print(json.dumps({
            "success": True,
            "level": level,
            "confidence": confidence,
            "probabilities": {c: round(float(p)*100,1) for c,p in zip(classes, proba)}
        }))
    except Exception as ex:
        print(json.dumps({"success": False, "error": str(ex)}))


# ─────────────────────────────────────────────────────────────────────────────
# 2. CAPTURE  — webcam + Haar cascade face detection + landmark points (OpenCV only)
# ─────────────────────────────────────────────────────────────────────────────
def capture(output_path):
    """
    Détection de visage avec Haar Cascade (OpenCV intégré, aucune dépendance externe).
    Dessine :
      - Rectangle vert de centrage
      - Contour du visage (ellipse)
      - Points de repère simulés sur les zones clés (yeux, nez, bouche, mâchoire)
    Appuyer ESPACE pour capturer quand le visage est détecté.
    """
    try:
        import cv2

        # Chargement des cascades Haar (incluses dans OpenCV)
        face_cascade = cv2.CascadeClassifier(
            cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
        )
        eye_cascade = cv2.CascadeClassifier(
            cv2.data.haarcascades + 'haarcascade_eye.xml'
        )

        cap = cv2.VideoCapture(0)
        if not cap.isOpened():
            print(json.dumps({"success": False, "error": "Impossible d'ouvrir la caméra"}))
            return

        captured   = False
        saved_path = None

        while True:
            ret, frame = cap.read()
            if not ret:
                break

            h, w    = frame.shape[:2]
            display = frame.copy()
            gray    = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

            # ── Guide de centrage ──────────────────────────────────────────
            cx, cy = w // 2, h // 2
            cv2.rectangle(display, (cx - 130, cy - 170), (cx + 130, cy + 170),
                          (100, 100, 100), 1)

            # ── Détection visage ───────────────────────────────────────────
            faces = face_cascade.detectMultiScale(
                gray, scaleFactor=1.1, minNeighbors=5, minSize=(80, 80)
            )

            face_detected = len(faces) > 0

            for (fx, fy, fw, fh) in faces:
                # Ellipse contour visage (vert)
                cv2.ellipse(display,
                            (fx + fw // 2, fy + fh // 2),
                            (fw // 2, fh // 2),
                            0, 0, 360, (0, 255, 0), 2)

                # ── Points de repère faciaux ───────────────────────────────
                # Calculés proportionnellement à la bounding box du visage
                landmarks = _compute_landmarks(fx, fy, fw, fh)
                for (px, py) in landmarks:
                    cv2.circle(display, (px, py), 2, (0, 255, 0), -1)

                # ── Détection des yeux dans la région du visage ────────────
                roi_gray  = gray[fy:fy + fh, fx:fx + fw]
                roi_color = display[fy:fy + fh, fx:fx + fw]
                eyes = eye_cascade.detectMultiScale(
                    roi_gray, scaleFactor=1.1, minNeighbors=5, minSize=(20, 20)
                )
                for (ex, ey, ew, eh) in eyes[:2]:
                    cv2.ellipse(roi_color,
                                (ex + ew // 2, ey + eh // 2),
                                (ew // 2, eh // 2),
                                0, 0, 360, (255, 255, 0), 1)

                # ── Texte guide ────────────────────────────────────────────
                cv2.putText(display, "Visage detecte",
                            (fx, fy - 10), cv2.FONT_HERSHEY_SIMPLEX,
                            0.55, (0, 255, 0), 2)

            if face_detected:
                cv2.putText(display, "ESPACE = Capturer",
                            (10, h - 15), cv2.FONT_HERSHEY_SIMPLEX,
                            0.6, (0, 255, 0), 2)
            else:
                cv2.putText(display, "Aucun visage detecte — Centrez votre visage",
                            (10, 30), cv2.FONT_HERSHEY_SIMPLEX,
                            0.6, (0, 0, 255), 2)
                cv2.putText(display, "ESPACE = Capturer  |  Q = Quitter",
                            (10, h - 15), cv2.FONT_HERSHEY_SIMPLEX,
                            0.55, (200, 200, 200), 1)

            cv2.imshow("Capture Visage — StudyFlow", display)
            key = cv2.waitKey(1) & 0xFF

            if key == ord(' ') and face_detected:
                cv2.imwrite(output_path, display)
                saved_path = output_path
                captured   = True
                break
            elif key == ord('q') or key == 27:
                break

        cap.release()
        cv2.destroyAllWindows()

        if captured:
            print(json.dumps({"success": True, "path": saved_path}))
        else:
            print(json.dumps({"success": False, "error": "Capture annulée ou aucun visage détecté"}))

    except Exception as ex:
        print(json.dumps({"success": False, "error": str(ex)}))


def _compute_landmarks(fx, fy, fw, fh):
    """
    Génère ~68 points de repère faciaux proportionnels à la bounding box.
    Couvre : contour mâchoire, sourcils, nez, yeux, bouche.
    """
    pts = []

    # ── Contour mâchoire (17 points) ──────────────────────────────────────
    jaw_xs = [0.05, 0.10, 0.17, 0.25, 0.33, 0.42, 0.50,
              0.58, 0.67, 0.75, 0.83, 0.90, 0.95]
    jaw_ys = [0.55, 0.65, 0.75, 0.83, 0.88, 0.92, 0.95,
              0.92, 0.88, 0.83, 0.75, 0.65, 0.55]
    for xr, yr in zip(jaw_xs, jaw_ys):
        pts.append((int(fx + xr * fw), int(fy + yr * fh)))

    # ── Sourcil gauche (5 points) ──────────────────────────────────────────
    for i, xr in enumerate([0.18, 0.25, 0.32, 0.39, 0.46]):
        pts.append((int(fx + xr * fw), int(fy + (0.22 - i * 0.01) * fh)))

    # ── Sourcil droit (5 points) ───────────────────────────────────────────
    for i, xr in enumerate([0.54, 0.61, 0.68, 0.75, 0.82]):
        pts.append((int(fx + xr * fw), int(fy + (0.18 + i * 0.01) * fh)))

    # ── Nez (9 points) ────────────────────────────────────────────────────
    nose_pts = [(0.50, 0.30), (0.50, 0.38), (0.50, 0.46), (0.50, 0.54),
                (0.42, 0.58), (0.46, 0.60), (0.50, 0.62),
                (0.54, 0.60), (0.58, 0.58)]
    for xr, yr in nose_pts:
        pts.append((int(fx + xr * fw), int(fy + yr * fh)))

    # ── Œil gauche (6 points) ─────────────────────────────────────────────
    eye_l = [(0.22, 0.35), (0.28, 0.32), (0.34, 0.32),
             (0.40, 0.35), (0.34, 0.38), (0.28, 0.38)]
    for xr, yr in eye_l:
        pts.append((int(fx + xr * fw), int(fy + yr * fh)))

    # ── Œil droit (6 points) ──────────────────────────────────────────────
    eye_r = [(0.60, 0.35), (0.66, 0.32), (0.72, 0.32),
             (0.78, 0.35), (0.72, 0.38), (0.66, 0.38)]
    for xr, yr in eye_r:
        pts.append((int(fx + xr * fw), int(fy + yr * fh)))

    # ── Bouche (12 points) ────────────────────────────────────────────────
    mouth_pts = [
        (0.36, 0.72), (0.42, 0.69), (0.50, 0.68), (0.58, 0.69), (0.64, 0.72),
        (0.58, 0.77), (0.50, 0.79), (0.42, 0.77),
        (0.38, 0.72), (0.50, 0.71), (0.62, 0.72), (0.50, 0.78)
    ]
    for xr, yr in mouth_pts:
        pts.append((int(fx + xr * fw), int(fy + yr * fh)))

    return pts


# ─────────────────────────────────────────────────────────────────────────────
# 3. PDF  — génération emploi du temps + photo + score
# ─────────────────────────────────────────────────────────────────────────────
def generate_pdf(name, score, level, photo_path, output_pdf):
    try:
        from reportlab.lib.pagesizes import A4
        from reportlab.lib import colors
        from reportlab.lib.units import cm
        from reportlab.platypus import (SimpleDocTemplate, Paragraph, Spacer,
                                        Table, TableStyle, Image as RLImage,
                                        HRFlowable)
        from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
        from reportlab.lib.enums import TA_CENTER, TA_LEFT
        import datetime

        score = int(score)
        level = str(level)

        # Couleur selon niveau
        if level == "Low":
            accent = colors.HexColor("#2e7d32")
            level_fr = "Stress Faible"
            emoji_level = "✅"
        elif level == "Medium":
            accent = colors.HexColor("#f57c00")
            level_fr = "Stress Modéré"
            emoji_level = "⚠️"
        else:
            accent = colors.HexColor("#c62828")
            level_fr = "Stress Élevé"
            emoji_level = "🔴"

        # Emploi du temps selon niveau
        schedules = {
            "Low": [
                ("07:00", "Réveil & étirements (15 min)"),
                ("07:30", "Petit-déjeuner équilibré"),
                ("08:00", "Session d'étude intensive (2h)"),
                ("10:00", "Pause active — marche 10 min"),
                ("10:15", "Session d'étude (1h45)"),
                ("12:00", "Déjeuner & repos"),
                ("13:30", "Session d'étude (2h)"),
                ("15:30", "Sport ou activité physique (1h)"),
                ("16:30", "Session d'étude légère (1h30)"),
                ("18:00", "Loisirs & détente"),
                ("20:00", "Dîner"),
                ("21:00", "Révision légère (45 min)"),
                ("22:00", "Lecture & relaxation"),
                ("23:00", "Coucher"),
            ],
            "Medium": [
                ("07:30", "Réveil progressif — respiration 4-7-8"),
                ("08:00", "Petit-déjeuner & méditation (10 min)"),
                ("08:30", "Session d'étude (1h30)"),
                ("10:00", "Pause obligatoire — étirements (15 min)"),
                ("10:15", "Session d'étude (1h15)"),
                ("11:30", "Pause active — marche"),
                ("12:00", "Déjeuner calme (sans écrans)"),
                ("13:30", "Sieste courte (20 min)"),
                ("14:00", "Session d'étude (1h30)"),
                ("15:30", "Activité physique modérée (45 min)"),
                ("16:30", "Session légère (1h)"),
                ("17:30", "Loisirs & décompression"),
                ("19:30", "Dîner"),
                ("21:00", "Relaxation — musique / lecture"),
                ("22:30", "Coucher"),
            ],
            "High": [
                ("08:00", "Réveil doux — pas d'alarme stressante"),
                ("08:30", "Petit-déjeuner complet & respiration"),
                ("09:00", "Session d'étude courte (45 min MAX)"),
                ("09:45", "Pause obligatoire (20 min)"),
                ("10:05", "Session d'étude (45 min)"),
                ("10:50", "Exercice de relaxation (15 min)"),
                ("11:05", "Session légère (30 min)"),
                ("11:35", "Pause & hydratation"),
                ("12:00", "Déjeuner sans écrans"),
                ("13:00", "Sieste réparatrice (30 min)"),
                ("13:30", "Activité physique douce (yoga/marche 1h)"),
                ("14:30", "Session d'étude (1h)"),
                ("15:30", "Pause longue — sortie extérieure"),
                ("17:00", "Loisirs uniquement"),
                ("19:30", "Dîner"),
                ("20:30", "Méditation guidée (20 min)"),
                ("21:30", "Lecture légère"),
                ("22:00", "Coucher OBLIGATOIRE"),
            ]
        }

        schedule = schedules.get(level, schedules["Medium"])

        doc = SimpleDocTemplate(
            output_pdf,
            pagesize=A4,
            rightMargin=1.5*cm, leftMargin=1.5*cm,
            topMargin=1.5*cm, bottomMargin=1.5*cm
        )

        styles = getSampleStyleSheet()
        story  = []

        # ── En-tête ──────────────────────────────────────────────────
        title_style = ParagraphStyle(
            'Title', parent=styles['Title'],
            fontSize=22, textColor=accent,
            spaceAfter=4, alignment=TA_CENTER
        )
        sub_style = ParagraphStyle(
            'Sub', parent=styles['Normal'],
            fontSize=11, textColor=colors.HexColor("#555555"),
            alignment=TA_CENTER, spaceAfter=12
        )
        story.append(Paragraph("📊 Rapport de Bien-Être & Emploi du Temps", title_style))
        story.append(Paragraph(
            f"Généré le {datetime.datetime.now().strftime('%d/%m/%Y à %H:%M')}",
            sub_style
        ))
        story.append(HRFlowable(width="100%", thickness=2, color=accent, spaceAfter=12))

        # ── Bloc identité + photo ─────────────────────────────────────
        id_data = [[
            Paragraph(f"<b>Nom :</b> <font color='red'>{name}</font>", styles['Normal']),
            Paragraph(f"<b>Score Bien-Être :</b> {score}/100", styles['Normal']),
            Paragraph(f"<b>Niveau de Stress :</b> {emoji_level} {level_fr}", styles['Normal']),
        ]]

        if photo_path and os.path.exists(photo_path):
            try:
                img = RLImage(photo_path, width=3.5*cm, height=3.5*cm)
                id_data[0].append(img)
            except Exception:
                id_data[0].append(Paragraph("(photo)", styles['Normal']))
        else:
            id_data[0].append(Paragraph("(pas de photo)", styles['Normal']))

        id_table = Table(id_data, colWidths=[4.5*cm, 4.5*cm, 5*cm, 4*cm])
        id_table.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,-1), colors.HexColor("#f5f5f5")),
            ('BOX',        (0,0), (-1,-1), 1, accent),
            ('INNERGRID',  (0,0), (-1,-1), 0.5, colors.HexColor("#dddddd")),
            ('VALIGN',     (0,0), (-1,-1), 'MIDDLE'),
            ('PADDING',    (0,0), (-1,-1), 8),
            ('FONTSIZE',   (0,0), (-1,-1), 10),
        ]))
        story.append(id_table)
        story.append(Spacer(1, 0.5*cm))

        # ── Barre de score ────────────────────────────────────────────
        score_pct = score / 100.0
        bar_data = [[
            Paragraph("<b>Score de Bien-Être</b>", styles['Normal']),
            Paragraph(f"<b>{score}/100</b>", styles['Normal']),
        ]]
        bar_table = Table(bar_data, colWidths=[14*cm, 4*cm])
        bar_table.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (0,0), accent),
            ('BACKGROUND', (1,0), (1,0), colors.HexColor("#eeeeee")),
            ('TEXTCOLOR',  (0,0), (0,0), colors.white),
            ('TEXTCOLOR',  (1,0), (1,0), colors.black),
            ('FONTSIZE',   (0,0), (-1,-1), 11),
            ('PADDING',    (0,0), (-1,-1), 8),
            ('BOX',        (0,0), (-1,-1), 1, colors.HexColor("#cccccc")),
        ]))
        story.append(bar_table)
        story.append(Spacer(1, 0.4*cm))

        # ── Recommandations ───────────────────────────────────────────
        rec_style = ParagraphStyle(
            'Rec', parent=styles['Normal'],
            fontSize=10, textColor=colors.HexColor("#333333"),
            spaceAfter=4, leftIndent=10
        )
        rec_title = ParagraphStyle(
            'RecTitle', parent=styles['Heading2'],
            fontSize=13, textColor=accent, spaceAfter=6
        )

        recommendations = {
            "Low":    ["Maintenez votre rythme actuel",
                       "Continuez vos activités physiques régulières",
                       "Gardez un sommeil de 7-8h par nuit",
                       "Pratiquez la pleine conscience 10 min/jour"],
            "Medium": ["Réduisez les sessions d'étude à 90 min max",
                       "Intégrez 2 pauses de 15 min par session",
                       "Pratiquez la respiration 4-7-8 matin et soir",
                       "Limitez les écrans 1h avant le coucher",
                       "Faites 30 min d'activité physique quotidienne"],
            "High":   ["PRIORITÉ : réduisez immédiatement la charge de travail",
                       "Consultez un professionnel de santé si nécessaire",
                       "Sessions d'étude max 45 min avec pauses obligatoires",
                       "Pratiquez le yoga ou la méditation guidée",
                       "Dormez au moins 8h par nuit — coucher avant 22h",
                       "Évitez la caféine après 14h",
                       "Parlez à quelqu'un de confiance de votre stress"]
        }

        story.append(Paragraph("💡 Recommandations Personnalisées", rec_title))
        for rec in recommendations.get(level, []):
            story.append(Paragraph(f"• {rec}", rec_style))
        story.append(Spacer(1, 0.4*cm))

        # ── Emploi du temps ───────────────────────────────────────────
        story.append(HRFlowable(width="100%", thickness=1, color=accent, spaceAfter=8))
        story.append(Paragraph("📅 Emploi du Temps Recommandé", rec_title))

        sched_data = [["Heure", "Activité"]]
        for time_slot, activity in schedule:
            sched_data.append([time_slot, activity])

        sched_table = Table(sched_data, colWidths=[3*cm, 15*cm])
        sched_table.setStyle(TableStyle([
            ('BACKGROUND',  (0,0), (-1,0), accent),
            ('TEXTCOLOR',   (0,0), (-1,0), colors.white),
            ('FONTSIZE',    (0,0), (-1,0), 11),
            ('FONTNAME',    (0,0), (-1,0), 'Helvetica-Bold'),
            ('ROWBACKGROUNDS', (0,1), (-1,-1),
             [colors.HexColor("#ffffff"), colors.HexColor("#f9f9f9")]),
            ('FONTSIZE',    (0,1), (-1,-1), 10),
            ('GRID',        (0,0), (-1,-1), 0.5, colors.HexColor("#dddddd")),
            ('PADDING',     (0,0), (-1,-1), 7),
            ('VALIGN',      (0,0), (-1,-1), 'MIDDLE'),
            ('TEXTCOLOR',   (0,1), (0,-1), accent),
            ('FONTNAME',    (0,1), (0,-1), 'Helvetica-Bold'),
        ]))
        story.append(sched_table)

        # ── Pied de page ──────────────────────────────────────────────
        story.append(Spacer(1, 0.5*cm))
        story.append(HRFlowable(width="100%", thickness=1, color=colors.HexColor("#cccccc")))
        footer_style = ParagraphStyle(
            'Footer', parent=styles['Normal'],
            fontSize=8, textColor=colors.HexColor("#999999"),
            alignment=TA_CENTER, spaceBefore=6
        )
        story.append(Paragraph(
            "Rapport généré par StudyFlow — Système de Bien-Être Étudiant",
            footer_style
        ))

        doc.build(story)
        print(json.dumps({"success": True, "path": output_pdf}))

    except ImportError as ie:
        print(json.dumps({"success": False, "error": f"Module manquant (pip install reportlab): {ie}"}))
    except Exception as ex:
        print(json.dumps({"success": False, "error": str(ex)}))


# ─────────────────────────────────────────────────────────────────────────────
# MAIN
# ─────────────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({"success": False, "error": "Commande manquante"}))
        sys.exit(1)

    cmd = sys.argv[1]

    if cmd == "predict" and len(sys.argv) >= 5:
        predict(sys.argv[2], sys.argv[3], sys.argv[4])

    elif cmd == "capture" and len(sys.argv) >= 3:
        capture(sys.argv[2])

    elif cmd == "pdf" and len(sys.argv) >= 7:
        generate_pdf(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5], sys.argv[6])

    else:
        print(json.dumps({"success": False, "error": f"Commande inconnue ou arguments insuffisants: {sys.argv}"}))
