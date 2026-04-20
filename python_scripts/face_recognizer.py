# face_recognizer.py
# Compare un visage capturé avec un encoding stocké en base

import sys
import json
import face_recognition
import numpy as np
import cv2 

def recognize_face(image_path, known_encoding_json):
    try:
        # Charger l'image à reconnaître
        bgr_image = cv2.imread(image_path)
        rgb_image = cv2.cvtColor(bgr_image, cv2.COLOR_BGR2RGB)
        encodings = face_recognition.face_encodings(rgb_image)
        
        if len(encodings) == 0:
            print(json.dumps({"success": False, "match": False, "error": "Aucun visage détecté"}))
            return
        
        # Encoding du visage capturé
        captured_encoding = encodings[0]
        
        # Encoding connu (depuis la base de données)
        known_encoding = np.array(json.loads(known_encoding_json))
        
        # Comparaison
        results = face_recognition.compare_faces([known_encoding], captured_encoding, tolerance=0.5)
        distance = face_recognition.face_distance([known_encoding], captured_encoding)[0]
        
        match = bool(results[0])
        confidence = round((1 - float(distance)) * 100, 2)
        
        print(json.dumps({
            "success": True,
            "match": match,
            "confidence": confidence
        }))
        
    except Exception as e:
        print(json.dumps({"success": False, "match": False, "error": str(e)}))

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print(json.dumps({"success": False, "match": False, "error": "Arguments manquants"}))
    else:
        recognize_face(sys.argv[1], sys.argv[2])
