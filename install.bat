@echo off
echo ========================================
echo   StudyFlow - Installation Face Recognition
echo ========================================
echo.

echo [1/5] Installation cmake...
pip install cmake
echo.

echo [2/5] Installation dlib...
echo Telechargez dlib-19.24.1-cp311-cp311-win_amd64.whl
echo depuis : https://github.com/z-mahmud22/Dlib_Windows_Python3.x/releases
echo Placez le fichier a la racine C:\ puis appuyez sur une touche...
pause
pip install C:\dlib-19.24.1-cp311-cp311-win_amd64.whl
echo.

echo [3/5] Installation face_recognition...
pip install face_recognition
echo.

echo [4/5] Installation opencv-python...
pip install opencv-python
echo.

echo [5/5] Downgrade numpy pour compatibilite dlib...
pip install numpy==1.26.4
echo.

echo ========================================
echo   Test de l installation...
echo ========================================
python -c "import face_recognition; import cv2; print('OK tout est installe !')"
echo.

echo ========================================
echo IMPORTANT : OpenCV Java
echo ========================================
echo Telechargez OpenCV 4.9.0 Windows depuis : https://opencv.org/releases/
echo Extrayez dans C:\opencv
echo Le jar doit etre ici : C:\opencv\build\java\opencv-490.jar
echo.

echo Installation terminee !
pause