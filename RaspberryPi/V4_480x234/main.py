##################################################################################################
#   Titel:   AutoProjekt-main.py                                                                 #
#   Author:  Jonas Wuensch                                                                       #
#                                                                                                #
#   Version: 1.0                                                                                 #
#   Stand  : 24.04.2020                                                                          #
##################################################################################################

###################################################################################################
# Zur automatischen Ausführung:                                                                   #
# put me in autostart                                                                             #
# python3 /../../main.python                                                                      #
# last line in --> /home/pi/.bashrc                                                               #
###################################################################################################

import time
from image_app_core import start_server_process, get_control_instruction, put_output_image
import pi_camera_stream
import cv2
from flask import Flask, render_template, Response
import numpy as np
import functions


#controlled_image_server_behavior ist die Hauptfunktion die den Ablauf des Programms steuert
def controlled_image_server_behavior():

    #in last_state steht immer die Richtung in die zuletzt gefahren wurde 
    global last_state
    #centre x und y beschreibt nachher die Bildmitte ,Default 0
    global centre_x
    global centre_y
    #Default Wert "nach rechts" wird last_state zugewiesen
    last_state = "right"
    centre_x = 0.
    centre_y = 0.
    #elapsed_time ist die verstrichene Zeitspanne solange kein Objekt gefunden wurde. 
    elapsed_time = 4.0
    #correction_factor x und y ist eine Hilfe für Auflösungsänderung, in diesem Fall nicht benutzt
    correction_factor_x = 3.0
    correction_factor_y = 2.5
    #Die Verbindung zum Arduino Mega wird aufgebaut
    functions.open_connection_arduino()
    #Motoren werden auf LOW initialisiert
    functions.stop()

    #Hilfstimer start und end
    #start ist die Startzeit des Timers
    #in start wird eine aktuelle Zeitangabe geschrieben
    start = float(time.perf_counter())
    #end ist das Ende des Timers 
    #Startzeit + die gewünschte Zeitspanne die vorher in elapsed_time festgelegt wurde
    end = start + elapsed_time
    #Initalisierung der Kamera
    camera = pi_camera_stream.setup_camera()
	#kruzer sleep damit die Kamera sich "aufwärmen" kann
    time.sleep(0.1)
	

    #in der for Schleife werden einzelne Frames (Bilder) ausgewertet, verarbeitet
    #und im Nachhinein wieder ausgegeben. In diesem werden sie an den Flask-Server
    #weitergeleitet. Der über den Port 8000 aufrufbar ist.
    #Ebenfalls anhand der erfassten Positionswerte des aktuellen Bildes reagiert
    #das heißt es werden Funktionen aufgerufen um das Auto zu steuern
    for frame in pi_camera_stream.start_stream(camera):
		#Hue Saturation Value
        hsv1 = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
        #das Bild wird maskiert und nur noch der Teil mit den voreingestellten Farben
        #in diesem Fall Pink,Rot wird zu sehen sein
        mask_red = pi_camera_stream.segment_colour(frame)
        #In diesem Teil werden die Kooardinaten des Bildes erfasst und in 
        #die Hilfsvariablen  geschrieben
        loct,area = pi_camera_stream.find_blob(mask_red)
        #die einzelnen Werte aus loct werden aufgesplittet um diese nachher zu verwenden
        x,y,w,h = loct
        #print Funktionen zu Debug-Zwecken
        ##print("x+w")
        ##print(x+w)

        #Der folgende Fall beschreibt wenn kein Objekt gefunden wurde
        if (w*h) < 10: ####<NO OBJECT FOUND>#####
            found = 0
            #ein Timer wird gestartet um kurz zu warten ob das Objekt noch gefunden wird
            start = float(time.perf_counter())
            #wenn die vorher eingestellte Zeit, in Default-Fall 4 Sekunden, die jetzt in end steht vertrichen ist,
            #wird in die zuletzt gefahrene Richtung gefahren.
            if end <= start:
                if last_state == "left":
                    #Funktion kommuniziert mit dem Arduino um nach links zu fahren
                    functions.turnleft()
                    time.sleep(0.1)
                   
                elif last_state == "right":
                    #Funktion kommuniziert mit dem Arduino um nach rechts zu fahren
                    functions.turnright()
                    time.sleep(0.1)
        
        #Der folgende Fall beschreibt wenn ein Objekt gefunden wurde    
        else:           ####<OBJECT FOUND>####
            found = 1
            #Timer wird bei jedem Durchlauf neu gesetzt und das Ende wird durch die aufaddierte Zeit gesetzt.
            start = float(time.perf_counter())
            end = float(time.perf_counter()) + elapsed_time
            #Die Mittelpunkte des Bildes werden ermittelt und in die Variablen centre x und y geschrieben.
            centre_x = x + ((w)/2)
            centre_y = y + ((h)/2)
            #Anhand der nun beschriebenen Variablen wird ein kleiner Kreis,
            #zur Visualisierung, in das Bild gezeichnet.
            cv2.circle(frame,(int(centre_x),int(centre_y)),10,(240,240,240),-1)
            #centre_x wird nocheinmal in 2 Häflten geteilt um nachher einfacher festzustellen in welcher
            #Hälfte sich das gefundene Objekt befindet.
            centre_x -= 240

        #Der Bewegungsablauf wenn ein Objekt gefunden wurde
        if(found == 1 ):
            #wenn sich das Objekt in der äußeren linken Hälfte befindet,
            #wird nachjustiert, also nach links gefahren um die Objekt-Mitte in die Bild-Mitte zu bringen
            if(centre_x <= -100):
                functions.turnleft()
                last_state = "left"
            #wenn sich das Objekt in der äußeren rechten Hälfte befindet,
            #wird nachjustiert, also nach rechts gefahren um die Objekt-Mitte in die Bild-Mitte zu bringen
            elif(centre_x >= 100):
                functions.turnright()
                last_state = "right"
            #Solange die Außenkanten des Objektes die festgelegten Werte nicht überschreitet wird
            #geradeaus in Richtung des Objektes gefahren.
            elif(x >= 18 and x+w <=330):
                functions.forward()
            #wenn keiner der vorhergegangen Fälle eintritt wird angehalten (Stop)
            else:
                functions.stop()
         #wenn keiner der vorhergegangen Fälle eintritt wird angehalten (Stop)
        else:
            functions.stop()

        #Das nun erfasste Bild wird nun in die Variable encoded_bytes geschrieben
        encoded_bytes = pi_camera_stream.get_encoded_bytes_for_frame(frame)
        #das erfasste Bild wird nun in die Schlange (Queue) eingereiht
        #um nachher abgerufen zu werden und auf den Server als Stream ausgegeben zu werden
        put_output_image(encoded_bytes)
        #wenn eine Kontrollanweisung gesetzt wurde wie zum Beispiel "exit" wird darauf reagiert
        #in Falle von "exit" wird das Programm angehalten
        instruction = get_control_instruction()
        if instruction == "exit":
            print("Stopping")
            return

#Ein Neuer Prozess wird aufgerufen um den Server zu starten,
#auf den nachher die Bildausgabe stattfindet .
#Als Parameter wird Die Html Seite verwendet die nachher ausgegeben werden soll.
process = start_server_process('camView.html')
try:
    #die Funktion "controlled_image_server_behavior" wird nun gestartet
	controlled_image_server_behavior()
finally:
    #im Fehlerfall oder bei Abruch, Ende, etc. wird der Server Prozess beendet.
	process.terminate()

	