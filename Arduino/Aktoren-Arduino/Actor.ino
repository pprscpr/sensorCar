//=========================================================//
//                                                         //
//  Arduino-ToRaspi-Car-                                   //
//  15.05.2020                                             //
//  Jonas Wuensch, Tobias Buhn, Stefan Riedl               //
//                                                         //
//  RaspiUSBConection,Motor-Treiber = l289n                //
//  Servo-Motoren                                          //
//  Infrarot-Strecken-Erkennung                            //
//=========================================================//

#include <Servo.h>                         // Library für die Servo-Motoren

#define       LICHT_AUS         1          // Festlegung für Öffner oder Schliesser Kontakte
#define       LICHT_AN          0          // Festlegung für Öffner oder Schliesser Kontakte
#define       LINKS_Str         29         // Infrarot-Strecken-Sensor Links PIN
#define       MIT_LI_Str        31         // Infrarot-Strecken-Sensor Mitte Links PIN
#define       MIT_TE_Str        33         // Infrarot-Strecken-Sensor Mitte PIN
#define       MIT_RE_Str        35         // Infrarot-Strecken-Sensor Mitte Rechts PIN
#define       RECHTS_Str        37         // Infrarot-Strecken-Sensor Rechts PIN
#define       motorRightForward  2         // Motor-Treiber rechts Vorwärts PIN 
#define       motorRightBackward 3         // Motor-Treiber rechts Rückwärts PIN 
#define       motorLeftForward   4         // Motor-Treiber links Vorwärts PIN 
#define       motorLeftBackward  5         // Motor-Treiber links Rückswärts PIN
#define       pwm1               6         // Motor-Treiber Pwm1 PIN
#define       pwm2               7         // Motor-Treiber pwm2 PIN
#define       inputSignal        15        // Eingangssignal ob ein Sensor am SensorArduino ausgelöst hat(Kommunikationsbrücke zwischen beiden Arduinos)
#define       maxInput           999       // Maxiamler Pegel der als noch LOW gwertet wird       
Servo         servoX;                      // Initialisierung des Servomotors X-Achse
Servo         servoY;                      // Initialisierung des Servomotors Y-Achse
int           speeed = 220;                // Variable für die zulässige PWM-Geschwindigkeit
char          val;                         // Variable für eingelesene Strings oder Chars aus Serial2 ( Bluetooth-Stick-Verbindung )
char          mode = '9';                  // hier wird der aktuell gewählte Modus ausgewählt, Default Modus ist '9' Selbstfahrer
byte          raspiReceived;               // Variable für eingelesene bytes aus Serial ( RaspberryPi-USB-Verbindung )
char          charBuffer[9];               // Buffer Variable für die Ausgabe nach Serial2 ( Bluetooth-Stick-Verbindung )
                                           // Für die Ausgabe von erfassten Werten an die Aussenstelle
int           posX = 108;                  // Position des X-Achsen-Servo-Motors, Default=108
int           posY = 148;                  // Position des Y-Achsen-Servo-Motors, Default=148
int           signalState = 0;             // Variable um die eingehende Spannung vom SensorArduino zu speichern


void setup() {
  // put your setup code here, to run once:
  // Pins zum Motortreiber werden als Ausgang deklariert
     pinMode(motorRightForward,OUTPUT);
     pinMode(motorRightBackward,OUTPUT);
     pinMode(motorLeftForward,OUTPUT);
     pinMode(motorLeftBackward,OUTPUT);
     pinMode(pwm1,OUTPUT);
     pinMode(pwm2,OUTPUT);
     // Eingangssignal vom Sensorarduino ist ein Eingang
     pinMode(inputSignal,INPUT);
    
  // Die Pin Ausgänge werden auf LOW (nicht fahren) initialisiert
     digitalWrite(motorRightBackward,LOW);
     digitalWrite(motorRightForward,LOW);
     digitalWrite(motorLeftBackward,LOW);
     digitalWrite(motorLeftForward,LOW);
     analogWrite(pwm1,speeed);
     analogWrite(pwm2,speeed);
  
  // Servos initialisieren
     servoX.attach(12);    // X-Achsen-Servo Pin=12
     servoY.attach(11);    // Y-Achsen-Servo Pin=1
     servoX.write(posX);   // Initialisierung mit den Wert aus posX
     servoY.write(posY);   // Initialisierung mit den Wert aus posy


  // start serial communication
     Serial.begin(9600);  // UART/USB Verbindung zum raspberryPI
     Serial2.begin(9600); // UART Verbindung zum BluetoothStick


}

void loop() {
 // MODUS wird anhand der empfangenen Signale
  // von Serial2 ( Bluetooth-Stick-Verbindung ) ausgewertet.
  // 8 = automatisches fahren nach Kamera-Steuerung
  // 9 = Selbstfahrer Modus ( fahren mit der Android-App )
  // 7 = Infrarot-Strecken-Erkennung  

  // Die Signale die über Serial2( Bluetooth-Stick-Verbindung ) kommen
  // werden ausgelesen und die Variable val geschrieben
     if(Serial2.available() > 0){
           val = Serial2.read();
     }
     
  // Anhand der nun beschriebenen Variable val wird ausgewertet 
  // in welchem Modus nun gefahren werden soll      
   if(val == '8'){      //Kamera Steuerung
         mode = '8';
         speeed = 180;
   }
   else if(val == '9'){  //Bluetooth Steuerung
         mode = '9';
         speeed = 220;
   }
   else if(val == '7'){  //Boon Steuerung
         mode = '7';
         speeed = 220;
   }
   // ankommenden Wert am  SensorArduino einlesen 
   signalState = analogRead(inputSignal);
   //Serial.println(signalState);
   // Übertragung von signalState an die Bluetooth Schnittstelle
   sprintf(charBuffer,"%u \n",signalState);
   Serial2.write(charBuffer);

   // Selbstfahrer Modus ( fahren mit der Android-App ) über Bluetooth
     if(mode == '9'){ 

      
       // Anhand Variable val wird nun ausgewertet 
       // welche Funktion bzw Reaktion ausgeführt wird
       if( val == 'F') {      // Es wird geradeaus gefahren
             forward();
       }
       else if(val == 'B'){  // Es wird rückwärts gefahren
             backward();  
       }
       else if(val == 'L'){  // Es wird nach links gefahren
             left();
       }
       else if(val == 'R'){  // Es wird nach rechts gefahren
             right();
       }
       else if(val == 'S'){  // Es wird angehalten
             stoppp();
       }  
        else if(val=='j'){
           if(posX<135){       // Wenn der vorgegebene Bereich nicht überschritten wird
              posX++;          // wird der X-Achsen-Servo nach links gedreht
              servoX.write(posX);
              delay(5);
           }
        }
        else if(val=='l'){  
           if(posX>75){    // Wenn der vorgegebene Bereich nicht unterschritten wird
              posX--;       // wird der X-Achsen-Servo nach rechts gedreht
              servoX.write(posX);
              delay(5);
           }
        }
        else if(val=='k'){
           if(posY>110){   // Wenn der vorgegebene Bereich nicht überschritten wird
              posY--;      // wird der Y-Achsen-Servo nach links gedreht
              servoY.write(posY);
              delay(5);
           }
        }
        else if(val=='i'){
           if(posY<170){   // Wenn der vorgegebene Bereich nicht unterschritten wird
              posY++;      // wird der Y-Achsen-Servo nach rechts gedreht
              servoY.write(posY);
              delay(5);
           }
        }
          
     }//mode9


  // automatisches fahren nach Kamera-Steuerung anhand der empfangen Werte vom RaspberryPi
     else if(mode == '8'){ 
        
        // Die Signale die über Serial( RaspberryPi-USB-Verbindung ) kommen
        // werden ausgelesen und die Variable raspiReceived geschrieben.
        if (Serial.available() ){
           raspiReceived = Serial.read();  
        }
        // Die Servomotoren werden nochmal auf ihr Default-Positionen gefahren
        // Damit die Kamera wieder mittig ist
        servoX.write(108);
        servoY.write(148);
          

        // Anhand Variable raspiReceived wird nun ausgewertet 
        // welche Funktion bzw Reaktion ausgeführt wird
        if(raspiReceived == 'F') {     // Es wird geradeaus gefahren
              forward();
        }
        else if(raspiReceived == 'B'){ // Es wird nach rückwärts gefahren
              backward();  
        }
        else if(raspiReceived == 'L'){  // Es wird nach links gefahren
              left();
        }
        else if(raspiReceived == 'R'){  // Es wird nach rechts gefahren
              right();
        }
        else{                           // Es wird angehalten
              stoppp();
        }
     
     }//mode 8

  // Infrarot-Strecken-Erkennung
     else if (mode == '7'){ 
        if ( val != 'S' ) {
   
            // Diese Funktion liest aus welche kombination die Infrarot Sensoren erfassen
            // und ruft damit die richtige bewegung auf
            // sollten die Spur falsch oder nicht erkannt werden wird gestoppt.
                IRstrecke();
          }
        else {
            // wenn Stop gedrückt wird, wird angehalten!
                stoppp();
          }
     }//mode 7
     
}//loop

  
  
/******************************************
 * Funktionen                             *
 ******************************************/

        
void IRstrecke (void){
  // Diese Funktion liest aus welche kombination die Infrarot Sensoren erfassen
  // und ruft damit die richtige Bewegung auf
  // sollten die Spur falsch oder nicht erkannt werden wird gestoppt.
  
  if      ( // spur ist genau mittig, dementsprechen gehts vorwärts
           digitalRead(LINKS_Str) == LICHT_AN && 
           digitalRead(MIT_LI_Str)== LICHT_AN && 
           digitalRead(MIT_TE_Str)== LICHT_AUS && 
           digitalRead(MIT_RE_Str)== LICHT_AN && 
           digitalRead(RECHTS_Str)== LICHT_AN )
           forward(); 
      
  else if ( // Spur ist breit und mittig, dementsprechend gehts vorwärts
           digitalRead(LINKS_Str) == LICHT_AN && 
           digitalRead(MIT_LI_Str)== LICHT_AUS && 
           digitalRead(MIT_TE_Str)== LICHT_AUS && 
           digitalRead(MIT_RE_Str)== LICHT_AUS && 
           digitalRead(RECHTS_Str)== LICHT_AN )
           forward();
           
  else if ( // Spur ist mittig links aber noch mittig, dementsprechend gehts vorwärts
           digitalRead(LINKS_Str) == LICHT_AN && 
           digitalRead(MIT_LI_Str)== LICHT_AUS && 
           digitalRead(MIT_TE_Str)== LICHT_AUS && 
           digitalRead(MIT_RE_Str)== LICHT_AN && 
           digitalRead(RECHTS_Str)== LICHT_AN )
           forward();
      
  else if ( // Spur ist mittig rechts aber noch mittig, dementsprechend gehts vorwärts
           digitalRead(LINKS_Str) == LICHT_AN && 
           digitalRead(MIT_LI_Str)== LICHT_AN && 
           digitalRead(MIT_TE_Str)== LICHT_AUS && 
           digitalRead(MIT_RE_Str)== LICHT_AUS && 
           digitalRead(RECHTS_Str)== LICHT_AN )
           forward();
           
        // Rechts fahren
  else if ( // Spur ist rechts geneigt aber noch in Toleranz, dementsprechend gehts vorwärts
           digitalRead(LINKS_Str) == LICHT_AN && 
           digitalRead(MIT_LI_Str)== LICHT_AN && 
           digitalRead(MIT_TE_Str)== LICHT_AN && 
           digitalRead(MIT_RE_Str)== LICHT_AUS && 
           digitalRead(RECHTS_Str)== LICHT_AN )
           rechts();
  else if ( // Spur ist am rechten Rand, dementsprechend gehts nach rechts
           digitalRead(LINKS_Str) == LICHT_AN && 
           digitalRead(MIT_LI_Str)== LICHT_AN && 
           digitalRead(MIT_TE_Str)== LICHT_AN && 
           digitalRead(MIT_RE_Str)== LICHT_AN && 
           digitalRead(RECHTS_Str)== LICHT_AUS )
           rechts(); 
        
  else if ( // Spur ist am rechten Rand, dementsprechend gehts nach rechts
           digitalRead(LINKS_Str) == LICHT_AN && 
           digitalRead(MIT_LI_Str)== LICHT_AN && 
           digitalRead(MIT_TE_Str)== LICHT_AN && 
           digitalRead(MIT_RE_Str)== LICHT_AUS && 
           digitalRead(RECHTS_Str)== LICHT_AUS )
           rechts();
  else if ( // Spur ist am rechten Rand, dementsprechend gehts nach rechts
           digitalRead(LINKS_Str) == LICHT_AN && 
           digitalRead(MIT_LI_Str)== LICHT_AN && 
           digitalRead(MIT_TE_Str)== LICHT_AUS && 
           digitalRead(MIT_RE_Str)== LICHT_AUS && 
           digitalRead(RECHTS_Str)== LICHT_AUS )
           rechts();
      
  else if (// Spur ist weit am rechten Rand, dementsprechend gehts nach rechts
           digitalRead(LINKS_Str) == LICHT_AN && 
           digitalRead(MIT_LI_Str)== LICHT_AUS && 
           digitalRead(MIT_TE_Str)== LICHT_AUS && 
           digitalRead(MIT_RE_Str)== LICHT_AUS && 
           digitalRead(RECHTS_Str)== LICHT_AUS )
           rechts();
        
        // Links fahren 
  else if ( // Spur ist links geneigt, dementsprechend gehts nach links
           digitalRead(LINKS_Str) == LICHT_AN && 
           digitalRead(MIT_LI_Str)== LICHT_AUS && 
           digitalRead(MIT_TE_Str)== LICHT_AN && 
           digitalRead(MIT_RE_Str)== LICHT_AN && 
           digitalRead(RECHTS_Str)== LICHT_AN )
           links();
           
  else if ( // Spur ist am linken Rand, dementsprechend gehts nach links
           digitalRead(LINKS_Str) == LICHT_AUS && 
           digitalRead(MIT_LI_Str)== LICHT_AN && 
           digitalRead(MIT_TE_Str)== LICHT_AN && 
           digitalRead(MIT_RE_Str)== LICHT_AN && 
           digitalRead(RECHTS_Str)== LICHT_AN )
           links();
      
  else if ( // Spur ist am linken Rand, dementsprechend gehts nach links
           digitalRead(LINKS_Str) == LICHT_AUS && 
           digitalRead(MIT_LI_Str)== LICHT_AUS && 
           digitalRead(MIT_TE_Str)== LICHT_AN && 
           digitalRead(MIT_RE_Str)== LICHT_AN && 
           digitalRead(RECHTS_Str)== LICHT_AN )
           links(); 
      
  else if ( //Spur ist am linken Rand, dementsprechend gehts nach links
           digitalRead(LINKS_Str) == LICHT_AUS && 
           digitalRead(MIT_LI_Str)== LICHT_AUS && 
           digitalRead(MIT_TE_Str)== LICHT_AUS && 
           digitalRead(MIT_RE_Str)== LICHT_AN && 
           digitalRead(RECHTS_Str)== LICHT_AN )
           links(); 
      
  else if ( //Spur ist weit am linken Rand, dementsprechend gehts nach links
           digitalRead(LINKS_Str) == LICHT_AUS && 
           digitalRead(MIT_LI_Str)== LICHT_AUS && 
           digitalRead(MIT_TE_Str)== LICHT_AUS && 
           digitalRead(MIT_RE_Str)== LICHT_AUS && 
           digitalRead(RECHTS_Str)== LICHT_AN )
           links(); 
  
  else stoppp(); // wenn die Spur nicht richtig erkannt wird, wird gestoppt
  
}// ende IRstrecke


// 

void forward(void){
        // sofern der gegebene Wert maxInput nicht überschritten werden
        // soll geradeaus gefahren werden , dementsprechend werden die 
        // Pins zum Motortreiber konfiguriert.
        // In allen anderen Fällen soll angehalten werden.
        // signalState wird vom Sensor Arduino empfangen und danach überpfüt ob der Grenzwerkt maxInput überschritten wurde
        // Wenn der Wert überschritten wurde bedeutet es dass am SensorArduino ein Sensor ausgelöst hat
        if( signalState > maxInput ){
          digitalWrite(motorRightBackward,LOW); 
          digitalWrite(motorRightForward,HIGH);
          digitalWrite(motorLeftBackward,LOW);
          digitalWrite(motorLeftForward,HIGH);
        }else{
          digitalWrite(motorRightBackward,LOW); 
          digitalWrite(motorRightForward,LOW);
          digitalWrite(motorLeftBackward,LOW);
          digitalWrite(motorLeftForward,LOW);
          
        }
 }

 void backward(void){
        // Es soll rückwärts gefahren werden , dementsprechend werden die 
        // Pins zum Motortreiber konfiguriert.
        digitalWrite(motorRightBackward,HIGH); 
        digitalWrite(motorRightForward,LOW);
        digitalWrite(motorLeftBackward,HIGH);
        digitalWrite(motorLeftForward,LOW);
 }
 void left(void){
        // soll nach links gefahren werden , dementsprechend werden die 
        // Pins zum Motortreiber konfiguriert.
        digitalWrite(motorRightBackward,LOW); 
        digitalWrite(motorRightForward,HIGH);
        digitalWrite(motorLeftBackward,LOW);
        digitalWrite(motorLeftForward,LOW);
 }
 void right(void){
        // soll nach rechts gefahren werden , dementsprechend werden die 
        // Pins zum Motortreiber konfiguriert.
        digitalWrite(motorRightBackward,LOW); 
        digitalWrite(motorRightForward,LOW);
        digitalWrite(motorLeftBackward,LOW);
        digitalWrite(motorLeftForward,HIGH);
 }
 void stoppp(void){
        // soll angehalten werden , dementsprechend werden die 
        // Pins zum Motortreiber konfiguriert.
        digitalWrite(motorRightBackward,LOW); 
        digitalWrite(motorRightForward,LOW);
        digitalWrite(motorLeftBackward,LOW);
        digitalWrite(motorLeftForward,LOW);
 }
 void links(void){
        // eine volle Linkskurve, dementsprechend werder die 
        // Pins konfiuriert das die Räderseiten sich entgegen drehen
        digitalWrite(motorRightBackward,LOW); 
        digitalWrite(motorRightForward,HIGH);
        digitalWrite(motorLeftBackward,HIGH);
        digitalWrite(motorLeftForward,LOW);
 }
 void rechts(void){
        // eine volle Rechtskurve, dementsprechend werder die 
        // Pins konfiuriert das die Räderseiten sich entgegen drehen
        digitalWrite(motorRightBackward,HIGH); 
        digitalWrite(motorRightForward,LOW);
        digitalWrite(motorLeftBackward,LOW);
        digitalWrite(motorLeftForward,HIGH);
 }
