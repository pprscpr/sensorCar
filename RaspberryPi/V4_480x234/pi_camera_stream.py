##################################################################################################
#   Titel:   AutoProjekt-pi_camera_stream.py                                                     #
#   Author:  Jonas Wuensch  ,Funktion find_blob() und segment_colour() von junejarohan(GitHub)   #
#   benutzt: OpenCV for Python                                                                   #
#   Version: 1.0                                                                                 #
#   Stand  : 24.04.2020                                                                          #
##################################################################################################

from picamera.array import PiRGBArray
from picamera import PiCamera
import numpy as numpy
import cv2
import numpy as np

#size beschreibt die gewünschte Auflösung des Bildes
size=(480,320)
encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 90]

#Funktion zur Initilisierung der Kamera
def setup_camera():
	camera = PiCamera()
	camera.resolution = size
	camera.rotation = 180
	return camera
#Funktion zur Erfassung des Bildes der Kamera
#Nachher wird das Bild via generator "yield" zurückgegeben.
def start_stream(camera):
	image_storage = PiRGBArray(camera, size=size)
	cam_stream = camera.capture_continuous(image_storage, format="bgr", use_video_port=True)
	for raw_frame in cam_stream:
		yield raw_frame.array
		image_storage.truncate(0)

#Der erfasste Frame welcher als Parameter übergeben wird,
#wird  byte-codiert als String wieder zurückgegeben.
def get_encoded_bytes_for_frame(frame):
	result, encoded_image = cv2.imencode('.jpg', frame, encode_param)
	return encoded_image.tostring()


#Grundfunktion von junejarohan
#Bildanalyse
#Ein vorher maskiertes Bild wird als Parameter übergeben.
#Die Funktion "findContours" zeichnet Outlines um die gefundenen Objekte.
#Jetzt wird um die Objekte ein Kreis gezeichnet.
#Danach wird analysiert welcher Kreis am größten ist.
#Am Schluss werden die Koordinaten und Positionen erfasst und zurückgegeben.
def find_blob(blob):
    largest_contour = 0
    cont_index = 0
    contours, hierarchy = cv2.findContours(blob, cv2.RETR_CCOMP, cv2.CHAIN_APPROX_SIMPLE)
    for idx, contour in enumerate(contours):
        area = cv2.contourArea(contour)
        if (area >largest_contour) :
            largest_contour = area

            cont_index = idx

    r=(0,0,2,2)
    if len(contours) > 0:
        r = cv2.boundingRect(contours[cont_index])
    return r,largest_contour


#Grundfunktion von junejarohan
#Bildanalyse
#Diese Funktion findet die Pinken/roten Pixel im Bild und gibt diese zurück
#Der Rest des Bildes, also der umliegende Teil wird ausgeschnitten.
#Zwei Masken werden gesetzt
#Maske 1 = HSV Hue Saturation Value siehe OpenCV Doku
#Maske 2 = YCrCb siehe OpenCV Doku , Wikipedia
#Die Maske im return ist eine Veroderung beider Masken.
def segment_colour(frame)
    hsv_roi =  cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
    mask_1 = cv2.inRange(hsv_roi, np.array([167, 112, 149]), np.array([190,255,255]))#PINK
    ycr_roi = cv2.cvtColor(frame,cv2.COLOR_BGR2YCrCb)
    mask_2 = cv2.inRange(ycr_roi, np.array((0.,170.,0.)), np.array((255.,255.,255.)))
    mask = mask_1 | mask_2
    kern_dilate = np.ones((8,8),np.uint8)
    kern_erode  = np.ones((3,3),np.uint8)
    mask = cv2.erode(mask,kern_erode)      #Eroding
    mask = cv2.dilate(mask,kern_dilate)     #Dilating
    #cv2.imshow('mask',mask_1)###mask #wenn unkommentiert = Anzeige der Maske in SW
    #cv2.imshow('mask2',mask_2)###mask #wenn unkommentiert = Anzeige der Maske in SW
    return mask