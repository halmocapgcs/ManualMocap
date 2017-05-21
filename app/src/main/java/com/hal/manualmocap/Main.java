package com.hal.manualmocap;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Main extends AppCompatActivity {

    //telemetry variables
    public static final String SERVER_IP_ADDRESS = "server_ip_adress_text";
    public static final String SERVER_PORT_ADDRESS = "server_port_number_text";
    public static final String LOCAL_PORT_ADDRESS = "local_port_number_text";

    boolean DEBUG=false;
    boolean TcpSettingsChanged;
    boolean UdpSettingsChanged;
    String AppPassword;

    public Telemetry AC_DATA;
    SharedPreferences AppSettings;

    private ReadTelemetry TelemetryAsyncTask;
    boolean isTaskRunning;
    private Thread mTCPthread;

    //joystick variables
    RelativeLayout layout_joystick_left, layout_joystick_right;
    Button power_button;

    JStick js1, js2;
    TextView xView1, xView2, yView1, yView2;

    public static final int pitchRoll = 21;

    private void setup_telemetry_class() {

        //Create com.hal.manualmocap.Telemetry class
        AC_DATA = new Telemetry();

        //sub in values
        AC_DATA.ServerIp = "192.168.50.10";//AppSettings.getString(SERVER_IP_ADDRESS, getString(R.string.pref_ip_address_default));
        AC_DATA.ServerTcpPort = 5010;//Integer.parseInt(AppSettings.getString(SERVER_PORT_ADDRESS, getString(R.string.pref_port_number_default)));
        AC_DATA.UdpListenPort = 5005;//nteger.parseInt(AppSettings.getString(LOCAL_PORT_ADDRESS, getString(R.string.pref_local_port_number_default)));
        AC_DATA.DEBUG=DEBUG;
        AC_DATA.context = getApplicationContext();

        //AC_DATA.prepare_class();
        AC_DATA.setup_udp();

    }

    private void set_up_app() {
        AppSettings = PreferenceManager.getDefaultSharedPreferences(this);
        AppPassword = "1234";

        //setup joysticks
        xView1 = (TextView)findViewById(R.id.x_position);
        yView1 = (TextView)findViewById(R.id.y_position);
        xView2 = (TextView)findViewById(R.id.x_position_right);
        yView2 = (TextView)findViewById(R.id.y_position_right);

        layout_joystick_left = (RelativeLayout)findViewById(R.id.layout_joystick_left);
        layout_joystick_right = (RelativeLayout)findViewById(R.id.layout_joystick_right);
        power_button = (Button)findViewById(R.id.power_button);

        js1 = new JStick(getApplicationContext(), layout_joystick_left, R.drawable.image_button, "YAW");
        js2 = new JStick(getApplicationContext(), layout_joystick_right, R.drawable.image_button, "PITCH");

        //joystick for throttle and yaw
        layout_joystick_left.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                js1.drawStick(arg1);
                //checks to see if the joystick is in the throttle region
                if((arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE) && Math.abs(js1.getX()) < 60) {
                    js1.stored_throttle = js1.getY();
                    if(js1.distance >= (js1.params.width/2 - js1.OFFSET)) AC_DATA.throttle = 0;  //edge case
                    else AC_DATA.throttle = (int) ((js1.getThrottle() + 127)/6.5) + 75;
                    xView1.setText("X : " + String.valueOf(AC_DATA.yaw));
                    yView1.setText("Y : " + String.valueOf(AC_DATA.throttle));
                }
                //checks to see if the joystick is in the yaw region
                else if((arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE) && Math.abs(js1.getX()) >= 72) {
                    if(js1.getX()>0) AC_DATA.yaw = 20;      //right button for yaw
                    if(js1.getX()<0) AC_DATA.yaw = -20;     //left button for yaw
                    xView1.setText("X : " + String.valueOf(AC_DATA.yaw));
                    yView1.setText("Y : " + String.valueOf(AC_DATA.throttle));
                }
                //reset value of yaw but not throttle when lifting up
                else if(arg1.getAction() == MotionEvent.ACTION_UP) {
                    AC_DATA.yaw = 0;
                    xView1.setText("X : 0");
                    yView1.setText("Y : " + String.valueOf(AC_DATA.throttle));
                }
                return true;
            }
        });

        //joystick for pitch and roll
        layout_joystick_right.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                js2.drawStick(arg1);
                if((arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE) &&
                        js2.distance > 8) {
                    //the offsets add a slightly larger region where values are (0,0) in order
                    //to avoid mistakenly pressing down near the center and activating the pitch/roll
                    double angle = js2.angle*(Math.PI/180);
                    double xoffset = 8*(Math.cos(angle));
                    double yoffset = 8*(Math.sin(angle));
                    AC_DATA.roll = (int) (js2.getX()/1.9 - xoffset/1.9);
                    AC_DATA.pitch = (int) -(js2.getY()/1.9 - yoffset/1.9);
                    //set limits for pitch and yaw
                    //note the integer steps for certain regions exist because our drone is back, right
                    //heavy
                    if(AC_DATA.roll >= pitchRoll) AC_DATA.roll = pitchRoll;
                    else if(AC_DATA.roll <= -pitchRoll-10) AC_DATA.roll = -pitchRoll-10;
                    if(AC_DATA.pitch >= pitchRoll) AC_DATA.pitch = pitchRoll;
                    else if(AC_DATA.pitch <= -pitchRoll + 4) AC_DATA.pitch = -pitchRoll + 4;
                    xView2.setText("X : " + String.valueOf(AC_DATA.roll));
                    yView2.setText("Y : " + String.valueOf(AC_DATA.pitch));
                }
                //reset both values to zero when lifting up or in central zone
                else if(arg1.getAction() == MotionEvent.ACTION_UP || (
                        (arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE) &&
                        js2.distance <= 8)) {
                    AC_DATA.roll = 0;
                    AC_DATA.pitch = 0;
                    xView2.setText("X : 0");
                    yView2.setText("Y : 0");
                }
                return true;
            }
        });

        //power button to start the rotors
        power_button.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                if(arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE){
                    AC_DATA.yaw = -127;
                    AC_DATA.throttle = 0;
                }
                else if(arg1.getAction() == MotionEvent.ACTION_UP){
                    AC_DATA.yaw = 0;
                    AC_DATA.throttle = 0;
                }
                return true;
            }
        });
    }

    /*private void send_to_server(String StrToSend, boolean ControlString) {
        //Is it a control string ? else ->Data request
        if (ControlString) {
            AC_DATA.SendToTcp = AppPassword + " " + StrToSend;
        } else {
            AC_DATA.SendToTcp = StrToSend;
        }
    }*/

        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joysticks_land);

        //setup telemetry data
        set_up_app();
        setup_telemetry_class();

        TelemetryAsyncTask = new ReadTelemetry();
        TelemetryAsyncTask.execute();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        //Force to reconnect
        //TcpSettingsChanged = true;
        TelemetryAsyncTask = new ReadTelemetry();
        TelemetryAsyncTask.execute();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context

        SharedPreferences.Editor editor = AppSettings.edit();

        AC_DATA.mTcpClient.sendMessage("removeme");
        //TelemetryAsyncTask.isCancelled();
        AC_DATA.mTcpClient.stopClient();
        isTaskRunning= false;

    }

    class ReadTelemetry extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isTaskRunning = true;
            if (DEBUG) Log.d("PPRZ_info", "ReadTelemetry() function started.");
        }

        @Override
        protected String doInBackground(String... strings) {
            mTCPthread =  new Thread(new ClientThread());
            mTCPthread.start();

            while (isTaskRunning) {

                //Check if settings changed
                if (TcpSettingsChanged) {
                    AC_DATA.mTcpClient.stopClient();
                    try {
                        Thread.sleep(200);
                        //AC_DATA.mTcpClient.SERVERIP= AC_DATA.ServerIp;
                        //AC_DATA.mTcpClient.SERVERPORT= AC_DATA.ServerTcpPort;
                        mTCPthread =  new Thread(new ClientThread());
                        mTCPthread.start();
                        TcpSettingsChanged=false;
                        if (DEBUG) Log.d("PPRZ_info", "TcpSettingsChanged applied");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (UdpSettingsChanged) {
                    AC_DATA.setup_udp();
                    //AC_DATA.tcp_connection();
                    UdpSettingsChanged = false;
                    if (DEBUG) Log.d("PPRZ_info", "UdpSettingsChanged applied");
                }

                // Log.e("PPRZ_info", "3");
                //1 check if any string waiting to be send to tcp
                if (!(null == AC_DATA.SendToTcp)) {
                    AC_DATA.mTcpClient.sendMessage(AC_DATA.SendToTcp);
                    AC_DATA.SendToTcp = null;

                }

                //3 try to read & parse udp data
                AC_DATA.read_udp_data();

                //4 check ui changes
                if (AC_DATA.ViewChanged) {
                    publishProgress("ee");
                    AC_DATA.ViewChanged = false;
                }
            }
            if (DEBUG) Log.d("PPRZ_info", "Stopping AsyncTask ..");
            return null;
        }
    }

    class ClientThread implements Runnable {


        @Override
        public void run() {

            if (DEBUG) Log.d("PPRZ_info", "ClientThread started");

            AC_DATA.mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    //publishProgress(message);
                    //Log.d("TCPParse", "Begin TCP parse");
                    AC_DATA.parse_tcp_string(message);

                }
            });
            AC_DATA.mTcpClient.SERVERIP = AC_DATA.ServerIp;
            AC_DATA.mTcpClient.SERVERPORT= AC_DATA.ServerTcpPort;
            AC_DATA.mTcpClient.run();

        }

    }

}
