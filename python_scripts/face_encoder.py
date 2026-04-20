import sys
import json
import face_recognition
import cv2
import numpy as np

def encode_face(image_path):
    try:
        bgr_image = cv2.imread(image_path, cv2.IMREAD_COLOR)
        if bgr_image is None:
            print(json.dumps({"success": False, "error": "Impossible de lire l'image"}))
            return

        rgb_image = cv2.cvtColor(bgr_image, cv2.COLOR_BGR2RGB)
        rgb_image = np.ascontiguousarray(rgb_image, dtype=np.uint8)

        encodings = face_recognition.face_encodings(rgb_image)

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
