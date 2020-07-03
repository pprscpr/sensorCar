##################################################################################################
#	Titel:   AutoProjekt-image_app_core.py 														 #
#   Author:  Jonas Wuensch unter Verwendung von :Learn Robotics Programming, 				     #
#            Build and control autonomous robots using Raspberry Pi 3 and Python – Danny Staple  #
#	benutzt: pythonFlask WebTemplate/Server Tool 												 #
#   Version: 1.0  																				 #
#   Stand  : 24.04.2020         																 #
##################################################################################################
import time
from multiprocessing import Process, Queue
from flask import Flask, render_template, Response

#Ein Flask-Objekt "app" wird erstellt
app = Flask(__name__)
#Es wird eine Schlange (queue) zur Anreihung der Kontrollanweisungen erstellt.
control_queue = Queue()
#Es wird eine Schlange (queue) zur Anreihung der Bilder ersellt.
display_queue = Queue(maxsize=2)
#Eine Default template Html Datei wird angegeben
display_template = 'image_server.html'


#Beim starten von "app" wird automatisch hier angefangen
#hier wird das html gerendert zurückgegeben
@app.route('/')
def index():
	return render_template(display_template)
#Der frame_generator verarbeitet, sofern vorhanden, die Bilder aus der Schlange(display_queue).
#Danach wird über yield das Bild in Multipartdata generiert.
def frame_generator():
	while True:
		#Get (wait until we have data)
		encoded_bytes = display_queue.get()
		#Need to turn this into http multipart data
		yield (b'--frame\r\n'
				b'Content-Type: image/jpeg\r\n\r\n' + encoded_bytes + b'\r\n')

#Hier wird das generierte Bild aus dem frame_generator abgerufen und ausgegeben.
#Später wird es in der HTML_Template über <img src="{{ url_for('display') }}"> wieder zu finden sein.
@app.route('/display')
def display():
	return Response(frame_generator(), mimetype='multipart/x-mixed-replace; boundary=frame')


#Die nächste Route erlaubt es Nachrichten in die Kontroll-Schlange(control_queue) zu übergeben,
#um nachher darauf regieren zu können.
#Später wird es in der HTML_Template über<a href="#" onclick="$.get('/control/exit')">debug_exit</a><br> wieder zu finden sein.
@app.route('/display')
@app.route('/control/<control_name>')
def control(control_name):
	control_queue.put(control_name)
	return Response('queued')


#Diese Funktion startet einen neuen Prozess der den Flask-Server startet.
#Als Parameter wird die gewünschte Webpage übergeben,
#in unserem Fall die Seite die nachher in der Android-App angezeigt wird.
#Die Seite wird nachher auf der IP-Adresse des Raspberrys und dem Port 8000 aufzurufen sein.
def start_server_process(template_name):
	"""Start the process, call .terminate to close it """
	global display_template
	display_template = template_name
	server = Process(target=app.run, kwargs={"debug": False, "host": "0.0.0.0", "port": 8000})
	server.start()
	return server

#Diese Funktion übergibt die Bilder als encoded_bytes
#in die Schlange für Bilder (display_queue)
def put_output_image(encoded_bytes):
	if display_queue.empty():
		display_queue.put(encoded_bytes)


#Diese Funktion liest die Schlange Kontrollanweisungen nacheinander (control_queue) aus
#und gibt diese als return zurück.
def get_control_instruction():
	if control_queue.empty():
		#wenn keine Anweisungen anstehen wird None zurückgegeben
		return None
	else:
		return control_queue.get()