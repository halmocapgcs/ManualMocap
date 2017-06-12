package com.hal.manualmocap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.net.DatagramSocket;

public class Main extends ActionBarActivity {

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

    static DatagramSocket sSocket = null;

    //joystick variables
    RelativeLayout layout_joystick_left, layout_joystick_right;
    Button power_button, Button_LaunchInspectionMode;

    JStick js1, js2;
    TextView xView1, xView2, yView1, yView2, battery_level;

    private void setup_telemetry_class() {

        //Create com.hal.manualmocap.Telemetry class
        AC_DATA = new Telemetry();

        //sub in values
        AC_DATA.ServerIp = "192.168.50.10";
        AC_DATA.ServerTcpPort = 5010;
        AC_DATA.UdpListenPort = 5005;
        AC_DATA.DEBUG=DEBUG;
        AC_DATA.context = getApplicationContext();

        AC_DATA.prepare_class();
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

        //setup battery
        battery_level = (TextView)findViewById(R.id.battery_level);
        battery_level.setText("??? v");

        layout_joystick_left = (RelativeLayout)findViewById(R.id.layout_joystick_left);
        layout_joystick_right = (RelativeLayout)findViewById(R.id.layout_joystick_right);
        power_button = (Button)findViewById(R.id.power_button);

        js1 = new JStick(getApplicationContext(), layout_joystick_left, R.drawable.image_button, "YAW");
        js2 = new JStick(getApplicationContext(), layout_joystick_right, R.drawable.image_button, "PITCH");

        Button_LaunchInspectionMode = (Button) findViewById(R.id.InspectionMode);
        Button_LaunchInspectionMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = "file:///sdcard/DCIM/video.sdp";
                Intent inspect = new Intent(getApplicationContext(), InspectionMode.class);
                inspect.putExtra("videoUrl", url);
                startActivity(inspect);
            }
        });

        //joystick for throttle and yaw
        layout_joystick_left.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                js1.drawStick(arg1);
                //checks to see if the joystick is in the throttle region
                if((arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE) && Math.abs(js1.getX()) < 60) {
                    //js1.stored_throttle = js1.getY(); had been used for a throttle that doesn't snap back to center
                    if(js1.getY()>30) AC_DATA.throttle = 84;
                    else if(js1.getY()<-30) AC_DATA.throttle = 42;
                    else AC_DATA.throttle = 63;
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
					AC_DATA.throttle = 63;
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
                        || arg1.getAction() == MotionEvent.ACTION_MOVE)) {
                    AC_DATA.roll = (int) (js2.getX()/4.0f);
                    AC_DATA.pitch = (int) -(js2.getY()/4.0f);
                    xView2.setText("X : " + String.valueOf(AC_DATA.roll));
                    yView2.setText("Y : " + String.valueOf(AC_DATA.pitch));
                }
                //reset both values to zero when lifting up or in central zone
                else if(arg1.getAction() == MotionEvent.ACTION_UP) {
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
                    AC_DATA.throttle = 63;
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
		AC_DATA.setup_udp();
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
                AC_DATA.read_udp_data(Main.sSocket);

                //4 check ui changes
                if (AC_DATA.ViewChanged) {
                    publishProgress("ee");
                    AC_DATA.ViewChanged = false;
                }
            }
            if (DEBUG) Log.d("PPRZ_info", "Stopping AsyncTask ..");
            return null;
        }

        @Override
        protected void onProgressUpdate(String... value) {
            super.onProgressUpdate(value);

            if (AC_DATA.BatteryChanged) {
                battery_level.setText(AC_DATA.AircraftData.Battery + " v");
                if(Double.parseDouble(AC_DATA.AircraftData.Battery) < 10.2){
                    battery_level.setTextColor(Color.RED);
                }
            }
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
