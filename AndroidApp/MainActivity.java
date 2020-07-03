package com.example.android2car;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.ted.androidtoarduino.R;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.UUID;


/*Die Main Klasse erbt die Eigenschaften der Klasse AppCompatActivity*/
public class MainActivity extends AppCompatActivity {

    /*Initialisiern der Objekte*/
    private TextView                tv_steering;
    private TextView                tv_camera;
    private TextView                tv_BluetoothStatus;
    private TextView                tv_ReadBuffer;
    private Button                  btn_BTScan;
    private Button                  btn_BTOff;
    private Button                  btn_PairedDev;
    private Button                  btn_Discover;
    private Button                  btn_up;
    private Button                  btn_right;
    private Button                  btn_down;
    private Button                  btn_left;
    private Button                  btn_Cam_Up;
    private Button                  btn_Cam_Right;
    private Button                  btn_Cam_Down;
    private Button                  btn_Cam_Left;
    private Button                  btn_refresh;
    private WebView                 webView;

    /*Der BluetoothAdapter ist der Eintritts-Punkt für jede Bluetooth Interaktion*/
    private BluetoothAdapter        BTAdapter;
    private Set<BluetoothDevice>    PairedDevices;
    /*Der ArrayAdapter wird nachher die Namen und Adressen der bekannten BT-Geräte beinhalten */
    private ArrayAdapter<String>    BTArrayAdapter;
    private ListView                DevicesListView;

    /*Ein Handler der die Nachrichten zwischen Main und Connected Thread handelt */
    private Handler                 Handler;
    /* Ein Thread, der wenn die Bluetooth Verbindung hergestellt wird, Daten sendet und empfängt */
    private ConnectedThread         ConnectedThread;    // bluetooth background worker thread to send and receive data
    /*Eine bi-direktionaler client to client Datenerbindung */
    private BluetoothSocket         BTSocket = null;    // bi-directional client-to-client data path
    /*User-Uniqe-ID um den Nutzer zu Identifizieren*/
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    /*#defines für die Kommunikation zwischen Prozessen*/
    private final static int REQUEST_ENABLE_BT = 1;     // wird benutzt um BT-Namen hinzuzufügen
    private final static int MESSAGE_READ = 2;          // wird im BT-Handler benutzt um festzustellen ob eine Nachricht bereit ist.
    private final static int CONNECTING_STATUS = 3;     // wird im BT-Handler benutzt um festzustellen in welchem Status sich die Nachricht befindet.

    int stateOfPairedList;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*Bindung der vorher festgelegten Objekte an die grafischen Obejekte. 
        die Verbdingung funktioniert mit der Methode findViewByID. 
        Zu finden sind diese in der main_menu.xml*/
        tv_steering =           findViewById(R.id.tv_steering);
        tv_camera =             findViewById(R.id.tv_camera);
        tv_BluetoothStatus =    findViewById(R.id.tv_bluetoothStatus);
        tv_ReadBuffer =         findViewById(R.id.tv_readBuffer);
        btn_BTScan =            findViewById(R.id.btn_scan);
        btn_BTOff =             findViewById(R.id.btn_off);
        btn_Discover =          findViewById(R.id.dtn_discover);
        btn_PairedDev =         findViewById(R.id.btn_paired_dev);
        DevicesListView =       findViewById(R.id.devicesListView);
        btn_up =                findViewById(R.id.btn_up);
        btn_right =             findViewById(R.id.btn_right);
        btn_down =              findViewById(R.id.btn_down);
        btn_left =              findViewById(R.id.btn_left);
        btn_Cam_Up =            findViewById(R.id.btn_Cam_up);
        btn_Cam_Right =         findViewById(R.id.btn_Cam_right);
        btn_Cam_Down =          findViewById(R.id.btn_Cam_down);
        btn_Cam_Left =          findViewById(R.id.btn_Cam_left);
        webView =               findViewById(R.id.webView);
        btn_refresh =           findViewById(R.id.btn_refresh);
        /*Der ArrayAdapter holt sich die Eigenschaften der BT-Geräte*/
        BTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        /*Eigenschaften aus BTArrayAdapter werden nun an ein ListView gebunden*/
        DevicesListView.setAdapter(BTArrayAdapter);
        /*Einzelne Items im ListView werden klickbar gemacht*/
        DevicesListView.setOnItemClickListener(mDeviceClickListener);
        /*Hilfsvariable für Anzeige der Geräte-ListView*/
        stateOfPairedList = 0;

        /*Aufruf der Funktion webViewInit() um den WebView ui initialisieren 
        * indem nachher die Html-Seite/Html-Template mit den Live-Stream angezeigt wird*/
        webViewInit();


        /*Ein Handler der die Nachrichten zwischen Main und Connected Thread handelt 
          Vom Arduino übertragene Daten werden nachher im TextView tv_ReadBuffer zu lesen sein. 
          Der Status ob ein Gerät verbunden ,welches Gerät verbunden wurde oder ob die Verbindung fehlgeschlagen ist 
          wird im TextView tv_BluetoothStatus zu lesen sein.*/
        Handler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String( (byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    tv_ReadBuffer.setText((String)readMessage + "\n");
                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1)
                        tv_BluetoothStatus.setText("Connected to Device: " + (String)(msg.obj));
                    else
                        tv_BluetoothStatus.setText("Connection Failed");
                }
            }
        };


        /*Auch wenn man davon ausgehen kann, dass die allermeisten (Android-)Smartphones über Bluetooth verfügen, 
         sollte man jedoch als guter Entwickler immer testen, ob ein zu benutzender Dienst auch existiert. 
         Das in Android zu überprüfen.*/
        if (BTArrayAdapter == null) {
            // Device does not support Bluetooth
            tv_BluetoothStatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {
            /*Ein OnTouchListener hört darauf ob ein Objekt berührt wurde 
             in diesem Fall wird beschrieben was passieren soll, wenn die "Kamera nach oben Taste" 
             gedrückt wird und was passieren soll , wenn sie wieder losgelassen wird. 
             In diesem Fall also, wenn ein Button berührt ist "MotionEvent.ACTION_DOWN" wird 
             ein String mit dem Inhalt "i" geschrieben. Dieses "i" erhält nun das andere Ende der Bluetooth-Verbindung, 
             also unser Bluetooth-Stick am Arduino. Jenes "i" wird jetzt vom Arduino ausgewertet und es wird 
             die Funktion Kamera nach oben aufgerufen. 
             Gleich verhät es sich mit den anderen Funktionen. Als weiteres Beispiel: Vorwärts fahren sendet bei Betätigung 
             des Buttons ein "F" und bei loslassen ein "S". Damit wird sichergestellt das bei loslassen des Buttons wieder angehalten wird. 
             Um sicherzugehen das im Fehlerfall unser Programm nicht abstürzt, wurde ein Exception Handling eingeführt(try, catch). 
             Ein Fehler könnte Beispielsweise sein das eine Taste betätigt wird und noch keine Verbindung besteht. 
             Die Methode "setOnTouchListener(new View.OnTouchListener()" ist eine Schnittstellendefinition für einen Callback, 
             der aufgerufen wird, wenn ein Berührungsereignis an diese View gesendet wird. 
             Der Callback wird aufgerufen, bevor das Touch-Ereignis an die View übergeben wird. 
 
             public  boolean onTouch (View v, MotionEvent event) 
             Wird aufgerufen, wenn ein Berührungsereignis an einen View gesendet wird. 
             v      : View: Der View, an den das Berührungsereignis gesendet wurde. 
             event   : MotionEvent: Das MotionEvent-Objekt mit vollständigen Informationen zum Ereignis. 
             Returns : boolean :    True wenn der Listener meldet das er "betätigt", ansonten false. 
             */

            /*Kamera nach oben Button*/
            btn_Cam_Up.setOnTouchListener(new View.OnTouchListener() {

                @Override // eine Methode wird überschrieben
                public boolean onTouch(View v, MotionEvent event) {
                        try {
                            // was soll passieren wenn die Taste berührt ist ? (ACTION_DOWN)
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                // ist der Kommunikationsthread noch aktiv ?
                                if (ConnectedThread != null)
                                    // schreibe ein "i" über den Kommunikationsthread an das Endgerät. Kamera nach oben!
                                    ConnectedThread.write("i");
                            }
                            // was soll passieren wenn die Taste unberührt ist ? (ACTION_UP)
                            if (event.getAction() == MotionEvent.ACTION_UP) {
                                // ist der Kommunikationsthread noch aktiv ?
                                if (ConnectedThread != null) ;
                                // schreibe ein "x" über den Kommunikationsthread an das Endgerät.
                                ConnectedThread.write("x");
                            }
                            return true;
                            // Exceptionhandling
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return false;

                }
            });
            /*Kamera nach links Button*/
            btn_Cam_Left.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    try {
                        // was soll passieren wenn die Taste berührt ist ? (ACTION_DOWN)
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            // ist der Kommunikationsthread noch aktiv ?
                            if (ConnectedThread != null)
                                // schreibe ein "j" über den Kommunikationsthread an das Endgerät. Kamera nach links!
                                ConnectedThread.write("j");
                        }
                        // was soll passieren wenn die Taste unberührt ist ? (ACTION_UP)
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            // ist der Kommunikationsthread noch aktiv ?
                            if (ConnectedThread != null) ;
                                // schreibe ein "x" über den Kommunikationsthread an das Endgerät.
                                ConnectedThread.write("x");
                        }
                        return true;
                        // Exceptionhandling
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });
            /*Kamera nach rechts Button*/
            btn_Cam_Right.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    try{
                        // was soll passieren wenn die Taste berührt ist ? (ACTION_DOWN)
                        if(event.getAction() == MotionEvent.ACTION_DOWN){
                            // ist der Kommunikationsthread noch aktiv ?
                            if(ConnectedThread != null)
                                // schreibe ein "l" über den Kommunikationsthread an das Endgerät. Kamera nach rechts!
                                ConnectedThread.write("l");
                        }
                        if(event.getAction() == MotionEvent.ACTION_UP){
                            // ist der Kommunikationsthread noch aktiv ?
                            if(ConnectedThread != null);
                                // schreibe ein "x" über den Kommunikationsthread an das Endgerät.
                                ConnectedThread.write("x");
                        }
                        return true;
                        // Exceptionhandling
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });
            /*Kamera nach unten Button*/
            btn_Cam_Down.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    try{
                        // was soll passieren wenn die Taste berührt ist ? (ACTION_DOWN)
                        if(event.getAction() == MotionEvent.ACTION_DOWN){
                            // ist der Kommunikationsthread noch aktiv ?
                            if(ConnectedThread != null)
                                // schreibe ein "k" über den Kommunikationsthread an das Endgerät. Kamera nach unten!
                                ConnectedThread.write("k");
                            }
                            if(event.getAction() == MotionEvent.ACTION_UP){
                                // ist der Kommunikationsthread noch aktiv ?
                                if(ConnectedThread != null);
                                    // schreibe ein "x" über den Kommunikationsthread an das Endgerät.
                                    ConnectedThread.write("x");
                        }
                        return true;
                        // Exceptionhandling
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });
            /*Vorwärts fahren Button*/
            btn_up.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    try{
                        // was soll passieren wenn die Taste berührt ist ? (ACTION_DOWN)
                        if(event.getAction() == MotionEvent.ACTION_DOWN){
                            // ist der Kommunikationsthread noch aktiv ?
                            if(ConnectedThread != null)
                                // schreibe ein "F" über den Kommunikationsthread an das Endgerät. Vorwärts fahren!
                                ConnectedThread.write("F");
                        }
                        if(event.getAction() == MotionEvent.ACTION_UP){
                            // ist der Kommunikationsthread noch aktiv ?
                            if(ConnectedThread != null)
                                // schreibe ein "S" über den Kommunikationsthread an das Endgerät. Anhalten!
                                ConnectedThread.write("S");
                        }
                        return true;
                        // Exceptionhandling
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    return false;
                }

            });
            /*Nach rechts fahren Button*/
            btn_right.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                     try{
                        // was soll passieren wenn die Taste berührt ist ? (ACTION_DOWN)
                        if(event.getAction() == MotionEvent.ACTION_DOWN){
                            // ist der Kommunikationsthread noch aktiv ?
                            if(ConnectedThread != null)
                                // schreibe ein "R" über den Kommunikationsthread an das Endgerät. Nach rechts fahren!
                                ConnectedThread.write("R");
                        }
                        if(event.getAction() == MotionEvent.ACTION_UP){
                            // ist der Kommunikationsthread noch aktiv ?
                            if(ConnectedThread != null)
                                // schreibe ein "S" über den Kommunikationsthread an das Endgerät. Anhalten!
                                ConnectedThread.write("S");
                        }
                        return true;
                         // Exceptionhandling
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    return false;
                }
            });
            /*Rückwärts fahren Button*/
            btn_down.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    try{
                        // was soll passieren wenn die Taste berührt ist ? (ACTION_DOWN)
                        if(event.getAction() == MotionEvent.ACTION_DOWN){
                            // ist der Kommunikationsthread noch aktiv ?
                            if(ConnectedThread != null)
                                // schreibe ein "B" über den Kommunikationsthread an das Endgerät. Rückwärts fahren!
                                ConnectedThread.write("B");
                        }
                        if(event.getAction() == MotionEvent.ACTION_UP){
                            // ist der Kommunikationsthread noch aktiv ?
                            if(ConnectedThread != null)
                                // schreibe ein "S" über den Kommunikationsthread an das Endgerät. Anhalten!
                                ConnectedThread.write("S");
                        }
                        return true;
                        // Exceptionhandling
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    return false;
                }
            });
            /*nach links fahren Button*/
            btn_left.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    try{
                        // was soll passieren wenn die Taste berührt ist ? (ACTION_DOWN)
                        if(event.getAction() == MotionEvent.ACTION_DOWN){
                            // ist der Kommunikationsthread noch aktiv ?
                            if(ConnectedThread != null)
                                // schreibe ein "L" über den Kommunikationsthread an das Endgerät. Nach links fahren!
                                ConnectedThread.write("L");
                        }
                        if(event.getAction() == MotionEvent.ACTION_UP){
                            // ist der Kommunikationsthread noch aktiv ?
                            if(ConnectedThread != null)
                                // schreibe ein "S" über den Kommunikationsthread an das Endgerät. Anhalten!
                                ConnectedThread.write("S");
                        }
                        return true;
                        // Exceptionhandling
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    return false;
                }
            });
            /*Bluetooth einschalten Button*/
            btn_BTScan.setOnClickListener(new View.OnClickListener() {
                @Override // Methode überschreiben
                public void onClick(View v) {
                    bluetoothOn(v);
                }
            });
            /*Bluetooth ausschalten Button*/
            btn_BTOff.setOnClickListener(new View.OnClickListener(){
                @Override // Methode überschreiben
                public void onClick(View v){
                    bluetoothOff(v);
                }
            });
            /*Bekannte BT-Geräte auflisten Button*/
            btn_PairedDev.setOnClickListener(new View.OnClickListener() {
                @Override // Methode überschreiben
                public void onClick(View v){
                    listPairedDevices();
                }
            });
            /*Nach neuen BT-Geräte suchen Button*/
            btn_Discover.setOnClickListener(new View.OnClickListener(){
                @Override // Methode überschreiben
                public void onClick(View v){
                    discover(v);
                }
            });
            /*WebView (Streamanzeige) aktuellisieren Button*/
            btn_refresh.setOnClickListener(new View.OnClickListener(){
                @Override // Methode überschreiben
                public void onClick(View v){
                    webView.reload();
                }
            });
        }
    }//onCreate




    /* Um zu testen, ob das Smartphone über Bluetooth verfügt, holen wir uns vom System den BluetoothAdapter über die Methode getDefaultAdapter(). 
    Dieser BluetoothAdapter stellt die Repräsentation des eigenen Bluetooth-Gerätes dar. 
    Wenn man als Rückgabewert ein NULL erhält, besitzt das Gerät kein Bluetooth. 
    Wenn das Gerät über Bluetooth verfügt, muss sichergestellt werden, 
    dass es auch zum Zeitpunkt der Nutzung aktiviert ist. 
    Die Aktivierung muss aber nochmals explizit durch den Nutzer bestätigt werden. 
    Wenn Bluetooth noch nicht aktiv ist, dann wird ein Intent mit der BluetoothAdapter. 
    ACTION_REQUEST_ENABLE-Aktion erzeugt, welche per startActivityForResult()-Methode ins System abgesetzt wird. 
    Dadurch öffnet sich ein Dialog, welcher den Nutzer fragt, ob Bluetooth aktiviert werden soll oder nicht. 
    Durch startActivityForResult() wird nach Abarbeitung des Intents bzw. 
    die Antwort des Nutzers, die Callback-Methode onActivityResult() aufgerufen. 
    Dieser ResultCode muss allerdings zunächst definiert werden.*/
    private void bluetoothOn(View view){
        if (!BTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            tv_BluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    /*Durch Auslesen des ResultCodes kann abgelesen werden, ob die Aktivierung erfolgreich war oder nicht.*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        /*Überprüfen um welchen Request es sich handelt*/
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == REQUEST_ENABLE_BT) {
            /*Sicherstellen des der Request erfolgreich war*/
            if (resultCode == RESULT_OK) {
                /*der User hat einen Kontakt ausgewählt*/
                /*Die Uri des Intents übergibt welcher Kontakt ausgewählt wurde*/
                tv_BluetoothStatus.setText("Enabled");
            } else
                tv_BluetoothStatus.setText("Disabled");
        }
    }
    /*Um die Bluetooth-Verbindung auf dem Gerät wieder zu deaktivieren benutzen wir die Methode disble().*/
    private void bluetoothOff(View view){
        BTAdapter.disable(); // turn off
        tv_BluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    /*Neue Geräte suchen 
    Wenn man eine Verbindung zu einem dem eigenem Gerät noch unbekanntem Gerät herstellen will, 
    muss man zunächst nach diesem Gerät suchen. Dies erreicht man durch den Aufruf der Methode startDiscovery(). 
    Der Aufruf dieser Methode löst eine asynchrone Suche aus. Nach Beendigung dieser Suche, 
    wird durch das Aussenden der Aktion ACTION_FOUND in das System das Ergebnis dem System zugänglich gemacht und kann abgefragt werden. 
    Zu diesem Zweck benötigt man einen BroadcastReceiver, 
    der auf die Beendigung der Suche reagiert. 
    Wird in unserer allerdings nicht benutzt*/
    private void discover(View view){
        if(BTAdapter.isDiscovering()){
            BTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else{
            if(BTAdapter.isEnabled()) {
                BTArrayAdapter.clear();
                BTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*Neue Geräte suchen 
    Wenn man eine Verbindung zu einem dem eigenem Gerät noch unbekanntem Gerät herstellen will, 
    muss man zunächst nach diesem Gerät suchen. Dies erreicht man durch den Aufruf der Methode startDiscovery(). 
    Der Aufruf dieser Methode löst eine asynchrone Suche aus. Nach Beendigung dieser Suche, 
    wird durch das Aussenden der Aktion ACTION_FOUND in das System das Ergebnis dem System zugänglich gemacht und kann abgefragt werden. 
    Zu diesem Zweck benötigt man einen BroadcastReceiver, der auf die Beendigung der Suche reagiert. */
    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                BTArrayAdapter.notifyDataSetChanged();
            }
        }
    };


    /*Gepaarte Geräte 
    Wenn man eine Verbindung zu einem bereits bekannten Gerät aufbauen möchte, 
    muss man keine neue Suche nach anderen Geräten starten, da man alle Informationen auf dem eigenen Gerät finden kann. 
    Durch die Methode getBondedDevices() wird die Liste aller gespeicherten und gepaarten Geräte abgerufen. */
    private void listPairedDevices(){
        /*Wenn die Geräte aufgelistet werden sollen, werden die umliegenden Elemente unsichtbar. Dies geschieht mit der Methode setVisibility().*/
        if(stateOfPairedList==0) {
            tv_steering.setVisibility(View.INVISIBLE);
            tv_camera.setVisibility(View.INVISIBLE);
            btn_up.setVisibility(View.INVISIBLE);
            btn_down.setVisibility(View.INVISIBLE);
            btn_left.setVisibility(View.INVISIBLE);
            btn_right.setVisibility(View.INVISIBLE);
            btn_Cam_Left.setVisibility(View.INVISIBLE);
            btn_Cam_Right.setVisibility(View.INVISIBLE);
            btn_Cam_Up.setVisibility(View.INVISIBLE);
            btn_Cam_Down.setVisibility(View.INVISIBLE);
            webView.setVisibility(View.INVISIBLE);
            DevicesListView.setVisibility(View.VISIBLE);

            PairedDevices = BTAdapter.getBondedDevices();

            if (BTAdapter.isEnabled()) {
                /* Geräte Name und Adresse werden dem BTArrayAdapter übergeben */
                for (BluetoothDevice device : PairedDevices) {
                    BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
            stateOfPairedList=1;
        }else if(stateOfPairedList==1){
            /*Wenn die Geräte nicht mehr aufgelistet werden sollen, werden die umliegenden Elemente sichtbar gemacht.*/
            DevicesListView.setVisibility(View.INVISIBLE);
            tv_steering.setVisibility(View.VISIBLE);
            tv_camera.setVisibility(View.VISIBLE);
            btn_up.setVisibility(View.VISIBLE);
            btn_down.setVisibility(View.VISIBLE);
            btn_left.setVisibility(View.VISIBLE);
            btn_right.setVisibility(View.VISIBLE);
            btn_Cam_Left.setVisibility(View.VISIBLE);
            btn_Cam_Right.setVisibility(View.VISIBLE);
            btn_Cam_Up.setVisibility(View.VISIBLE);
            btn_Cam_Down.setVisibility(View.VISIBLE);
            webView.setVisibility(View.VISIBLE);

            stateOfPairedList=0;
        }
    }


    /*Wenn ein BT-Gerät ausgewählt werden soll, muss dazu eine Liste mit den Namen der schon bekannten BT-Geräte 
      gefüllt werden. Ebenfalls muss definiert werden was passieren soll wenn eines dieser Geräte ausgewählt (geklickt) wird. */
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            if(!BTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }
            /*Wenn ein Gerät ausgewählt wurde, wird die Liste der Geräte wieder unsichtbar und die anderen Steuerelemente 
              werden wieder angezeigt.*/
            DevicesListView.setVisibility(View.INVISIBLE);
            tv_steering.setVisibility(View.VISIBLE);
            tv_camera.setVisibility(View.VISIBLE);
            btn_up.setVisibility(View.VISIBLE);
            btn_down.setVisibility(View.VISIBLE);
            btn_left.setVisibility(View.VISIBLE);
            btn_right.setVisibility(View.VISIBLE);
            btn_Cam_Left.setVisibility(View.VISIBLE);
            btn_Cam_Right.setVisibility(View.VISIBLE);
            btn_Cam_Up.setVisibility(View.VISIBLE);
            btn_Cam_Down.setVisibility(View.VISIBLE);
            webView.setVisibility(View.VISIBLE);

            stateOfPairedList=0;

            tv_BluetoothStatus.setText("Connecting...");
            /*Erfassen der Mac-Adresse des Gerätes. Die Mac-Adresse finden wir in den letzten 17 Zeichen des Strings*/
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            /*Einen neuer wird erstellt um die anderen laufenden Anwendungen nicht zu blockieren*/
            new Thread()
            {
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = BTAdapter.getRemoteDevice(address);

                    try {
                        /*Hier wird eine neue Bluetooth-Schnittstelle(BTSocket) erstellt (ähnlich wie ein TCP Socket). 
                          Dieser Verbindungspunkt erlaubt den Austausch von Daten mit anderen BT-Geräten 
                          die einen Input- oder Output-Stream benutzen.*/
                        BTSocket = createBluetoothSocket(device);

                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    /*Durch den Aufruf von connect() auf dem BluetoothSocket versucht das System, 
                      eine Verbindung zum Socket auf dem Server herzustellen. Ist dies von Erfolg gekrönt, 
                      kann zwischen beiden eine Kommunikation stattfinden. */
                    try {
                        BTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            BTSocket.close();
                            Handler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(fail == false) {
                        ConnectedThread = new ConnectedThread(BTSocket);
                        ConnectedThread.start();

                        Handler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();
        }
    };


    /*Durch den Aufruf von createRfcommSocketToServiceRecord(UUID) auf dem gewählten Gerät, 
      erhalten wir ein BluetoothSocket-Objekt. 
      Stellt eine sichere Ausgangsverbindung zum BT-Gerät mittles der UUID her.*/
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }


    /*Daten-Handhabung 
    *Der Aufbau einer Verbindung zwischen zwei Bluetooth-Geräten ist nur die eine Seite der Medaille. 
    *Nach dem Aufbau dieser Verbindung muss dann auch noch der Austausch der Daten bewältigt werden. 
    *Bei der Übertragung der Daten kommen Input- und Outputstreams zum Einsatz. 
    *Beispiel-Implementierung für die Handhabung der Daten: */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;


        /*Es wird ein Thread erstellt um eine Verbindung mit den Geräten einzugehen um den 
          Input- und den Output-Stream zu erfassen.*/
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            /*Neuer Buffer für den Stream wird erstellt*/
            byte[] buffer = new byte[1024];
            /*Variable zum erfassen des read() Inhaltes.*/
            int bytes;
            /*Solange kein Fehler passiert wird der InputStream ausgelesen*/
            while (true) {
                try {
                    /*Input Stream auslesen*/
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        /*Kurze Pause um auf den Datenstrom zu warten, variabel.*/
                        SystemClock.sleep(500);
                        /*Wie viele bytes sind zum einlesen vorhande ?*/
                        bytes = mmInStream.available();
                        /*Hier wird noch einmal festgehalten ie viele bytes nun wirklich eingelesen wurden*/
                        bytes = mmInStream.read(buffer, 0, bytes);
                        /*Versenden der erfassten Daten an das UserInterface.*/
                        Handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        /* Mit dieser Methode kann an einen verbundenen Stream geschrieben werden */
        public void write(String input) {
            /*Wandelt einen empfangenen String in bytes um.*/
            byte[] bytes = input.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Mit dieser Methode kann eine Stream-Verbindung unterbrochen werden */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


    /*In userer App soll nachher der Live Stream der Kamera, des RaspberryPi´s, angezeigt werden. Mit der Methode loadUrl() 
    können wir nun unsere gewünschte Webseite, wie in einem Browser, aufrufen. 
    In unserem Fall tragen wir die IP-Adresse des RaspberryPi´s und den Port auf welchen er den Stream weiterleitet ein. 
    Falls ein Fehler auftritt, wie zum Beispiel wenn die Webseite nicht erreicht werden kann, wird eine Errorpage ausgegeben. */
    public void webViewInit(){
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        try {
            webView.loadUrl("http://192.168.178.24:8000");

        }catch (Exception e){
            e.printStackTrace();
        }
        webView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                webView.loadData("<html><body><h1>SORRY, NO STREAM AVAILABLE ATM</h1></body></html>","text/html","utf-8");

            }
        });
    }


    /*Wenn das OptionsMenu, rechts oben geklickt wird, kann der gewünschte Modus ausgewählt werden. 
    In unserem Fall klickt nun der User beispielsweise auf "mode Camera" und es wird über unsere Bluetooth-Verbindung "ConnectedThead" 
    eine String mit dem Inhalt 8 an unseren Bluetooth-Stick am Arduino gesendet. Dieser liest nun den String aus und schaltet in den Kamera-Modus um. 
    Das gleiche passiert bei "mode Self" = "9" oder "mode Boon" = "7".*/
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId()){
            case R.id.modeSelf:

                try {
                    if (ConnectedThread != null) {
                        ConnectedThread.write("9");
                        Toast.makeText(getBaseContext(), "mode Self", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(getBaseContext(), "mode not changed", Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                return true;
            case R.id.modeCamera:

                try {
                    if (ConnectedThread != null) {
                        ConnectedThread.write("8");
                        Toast.makeText(getBaseContext(), "mode Camera", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(getBaseContext(), "mode not changed", Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                return true;
            case R.id.modeBoon:

                try {
                    if (ConnectedThread != null) {
                        ConnectedThread.write("7");
                        Toast.makeText(getBaseContext(), "mode Boon", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(getBaseContext(), "mode not changed", Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                return true;
            default:
                return false;

        }

    }


    /*Um ein OptionsMenu aufzurufen muss es vorher noch initialisiert werden. 
    Dies geschieht mit einem MenuInflater welcher nun unser Menu "main_menu" "aufbläst". */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater =getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }
}// Main Activity