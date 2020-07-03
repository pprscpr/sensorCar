##################################################################################################
#   Titel:   AutoProjekt-functions.py                                                            #
#   Author:  Jonas Wuensch                                                                       #
#                                                                                                #
#   Version: 1.0                                                                                 #
#   Stand  : 24.04.2020                                                                          #
##################################################################################################

import serial
import time

#Es wird ein String über die bestehende Verbindung "connection" gesendet.
#connection ist die Verbindung zum Arduino
#String L steht für Left
#R für Right
#B für Backwards
#F für Forward
#S für Stop
def turnleft():
    connection.write(str.encode('L'))

def turnright():
    connection.write(str.encode('R'))

def reverse():
    connection.write(str.encode('B'))

def forward():
    connection.write(str.encode('F'))

def stop():
    connection.write(str.encode('S'))


#Eine USB verbindung zum Arduino Mega wird auf gebaut 5 sekunden pause um Verbindung aufzubauen
def open_connection_arduino():
    global connection
    #/dev/serial/by-id/usb-1a86_USB2.0-Serial-if00-port0 beschreibt die USB-Schnittstelle zum Arduino Mega
    connection = serial.Serial('/dev/serial/by-id/usb-1a86_USB2.0-Serial-if00-port0', 9600)
    connection.isOpen()
    print("connection is being built up")
    time.sleep(5)