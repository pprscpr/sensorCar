
//=========================================================//
//                                                         //
//  Arduino-ToRaspi-Car-                                   //
//  15.05.2020                                             //
//  Jonas Wuensch, Tobias Buhn, Stefan Riedl               //
//                                                         //
//                                                         //
//  Led-Indicator, Ultrasonic hr04                         //
//  ToF Laser-Distanz-Sensor                               //
//=========================================================//

#include <Wire.h>                          // Library für den I²C Schnittstelle
#include <VL53L1X.h>                       // Library für den Laser-Sensor VL53L1X
#include <NewPing.h>                       // Library für den Ultraschall-Sensor

#define       TRIGGER_PIN        8         // Ultraschallsensor Trigger PIN
#define       ECHO_PIN           9         // Ultraschallsensor Echo PIN 
#define       MAX_ECHO_DIST      50000     // Parameter für die Funktion sonar.Beschreibt die Länge des max Pings.
#define       outputSignal       11        // Ausgangssignal zu den AktorenArduino um zu signalisieren ob ein Sensor ausgelöst wurde 
#define       distLED            10        // Led die aufleuchtet wenn Distanz unterschritten wird PIN
#define       maxDist            30        // ist die max. Distanz in cm , gemessen vom US.sensor, die zulässig sein soll
#define       maxDist2           300       // ist die max. Distanz in mm , gemessen vom Laser-Sensor, die zulässig sein soll
unsigned int  uS;                          // Variable für gemessene Entfernung vom Ultraschallsensor in uSec
unsigned int  dist2;                       // Variable für gemessene Entfernung vom Laser-Distanz-Sensor in mm
unsigned int  dist;                        // Variable für gemessene Entfernung vom Ultraschallsensor in cm


// Neues Objekt sonar der Klasse NewPing zum erfassen der Werte des US.sensors
NewPing sonar(TRIGGER_PIN, ECHO_PIN, MAX_ECHO_DIST);  

// ein neues Objekt erstellen zum erfassen der Werte des Laser-Sensors
VL53L1X sensor;

void setup() {
// Laser Sensor initialisieren
     Wire.begin();                                // Start der I²C Schnittstelle 
     Wire.setClock(400000);                       // I²C Schnittstellen- Gescchwindigkeit einstellung 
     sensor.setTimeout(500);                      // setzen
     sensor.init();                               // Laser Sensor Initaliesierung
     sensor.setDistanceMode(VL53L1X::Short);      // Mode des Laser- Sensors initialisieren 
     sensor.setMeasurementTimingBudget(33000);    // wielang darf die Messung maximal dauern 
     sensor.startContinuous(100);                 // in welcher Perioden wird gemessen


     Serial.begin(9600);  // UART/USB Verbindung zum raspberryPI
}
 
void loop() {

  // Ultraschall-Sensor-Abfrage
          uS = sonar.ping();   // Ultraschall sensor schickt einen Ping und schreibt das Ergebnis in uS ( mikroSekunden )
          dist = (unsigned int)sonar.convert_cm(uS); // Das Ergebnis wird nun in cm umgewandelt und in dist geschrieben
          
  // Laser-Sensor-Abfrage
          dist2 = sensor.read();
        //Serial.println(dist);
        //Serial.println(dist2);
  
  //LED-Abtands-Anzeige
  // wenn der gewünschte Abstand ( maxDist oder maxdist2 ) unterschritten wird
  // soll die LED aufleuchten
  // ebenfalls soll ein Signal an den Aktoren Arduino gesendet werden damit dieser erkennt das ein Sensor ausgelöst hat
          if (dist <= maxDist || dist2 <= maxDist2){
            digitalWrite(distLED,HIGH);
            digitalWrite(outputSignal,LOW);
            delay(10);
            
          }
          else{
            digitalWrite(distLED,LOW);
            digitalWrite(outputSignal,HIGH);
            delay(10);
          }


        

}
