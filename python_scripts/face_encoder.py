# face_encoder.py
# Prend une photo, encode le visage, retourne le vecteur en JSON

import sys
import json
import face_recognition
import numpy as np

def encode_face(image_path):
    try:
        image = face_recognition.load_image_file(image_path)
        encodings = face_recognition.face_encodings(image)
        
        if len(encodings) == 0:
            print(json.dumps({"success": False, "error": "Aucun visage détecté"}))
            return
        
        encoding = encodings[0].tolist()
        print(json.dumps({"success": True, "encoding": encoding}))
        
    except Exception as e:
        print(json.dumps({"success": False, "error": str(e)}))

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({"success": False, "error": "Chemin image manquant"}))
    else:
        encode_face(sys.argv[1])
