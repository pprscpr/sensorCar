# Sensor/Camera/Remote Controlled Car
This project was created as project work during my studies at the STB-Berlin. With the help of my fellow students Stefan Riedl and Tobias Buhn. It is a remote controlled car via Android App.
<br />There are available 3 modes :
* remote Controlled  
* follow a tracking object with the help of a camera,
* follow a black line on the ground with the help of IR-sensors 

   ### AndroidApp
    * Android Studio
    * Remote control
    * Choose mode
    * Live camera Stream from flask server on static IP-Adress
   ### RaspberryiPi
    * Python / Flask
    * Raspberryi Camera 
    * USB connection to Arduino
    * Generates live stream via Flask Server
   ### Arduino Mega
    * Bluetooth Modul HC05
    * controls the steering
    * change mode
    * Connection to Arduino uno
   ### Arduino Uno
    * Ultrasonic / Laser Sensor
    * Led Indicator
    * Connection to Arduino Mega
 
 
 #### The full documentation in german:
https://github.com/pprscpr/sensorCar/blob/master/presentation/Abschlussdokumentation_SensorAuto.pdf

## the car
![GitHub Logo](https://raw.githubusercontent.com/pprscpr/sensorCar/master/presentation/carv2.png)

## the app
![GitHub Logo](https://raw.githubusercontent.com/pprscpr/sensorCar/master/presentation/Screenshot_2020-03-19-15-11-09.png)


