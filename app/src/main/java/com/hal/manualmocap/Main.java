package com.hal.manualmocap;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Main extends AppCompatActivity {

    //telemetry variables
    public static final String SERVER_IP_ADDRESS = "server_ip_adress_text";
    public static final String SERVER_PORT_ADDRESS = "server_port_number_text";
    public static final String LOCAL_PORT_ADDRESS = "local_port_number_text";
    //public static final String MIN_AIRSPEED = "minimum_air_speed";
    boolean DEBUG=true;
    boolean TcpSettingsChanged;
    boolean UdpSettingsChanged;

    public Telemetry AC_DATA;
    SharedPreferences AppSettings;

    private ReadTelemetry TelemetryAsyncTask;
    boolean isTaskRunning;
    private Thread mTCPthread;

    //joystick variables
    RelativeLayout layout_joystick_left, layout_joystick_right;

    JStick js1, js2;
    TextView xView1, xView2, yView1, yView2;

    private void setup_telemetry_class() {

        //Create com.hal.manualmocap.Telemetry class
        AC_DATA = new Telemetry();

        //sub in values
        AC_DATA.ServerIp = AppSettings.getString(SERVER_IP_ADDRESS, getString(R.string.pref_ip_address_default));
        AC_DATA.ServerTcpPort = Integer.parseInt(AppSettings.getString(SERVER_PORT_ADDRESS, getString(R.string.pref_port_number_default)));
        AC_DATA.UdpListenPort = Integer.parseInt(AppSettings.getString(LOCAL_PORT_ADDRESS, getString(R.string.pref_local_port_number_default)));
        AC_DATA.DEBUG=DEBUG;
        AC_DATA.context = getApplicationContext();
        //AC_DATA.AirSpeedMinSetting = parseDouble(AppSettings.getString(MIN_AIRSPEED, "10"));

        //AC_DATA.prepare_class();
        AC_DATA.setup_udp();

    }

    private void set_up_app() {
        AppSettings = PreferenceManager.getDefaultSharedPreferences(this);

        //setup joysticks
        xView1 = (TextView)findViewById(R.id.x_position);
        yView1 = (TextView)findViewById(R.id.y_position);
        xView2 = (TextView)findViewById(R.id.x_position_right);
        yView2 = (TextView)findViewById(R.id.y_position_right);

        layout_joystick_left = (RelativeLayout)findViewById(R.id.layout_joystick_left);
        layout_joystick_right = (RelativeLayout)findViewById(R.id.layout_joystick_right);

        js1 = new JStick(getApplicationContext(), layout_joystick_left, R.drawable.image_button);
        js2 = new JStick(getApplicationContext(), layout_joystick_right, R.drawable.image_button);

        layout_joystick_left.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                js1.drawStick(arg1);
                if(arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE) {
                    xView1.setText("X : " + String.valueOf(js1.getX()));
                    yView1.setText("Y : " + String.valueOf(js1.getY()));
                }
                else if(arg1.getAction() == MotionEvent.ACTION_UP) {
                    xView1.setText("X : 0");
                    yView1.setText("Y : 0");
                }
                return true;
            }
        });

        layout_joystick_right.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                js2.drawStick(arg1);
                if(arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE) {
                    xView2.setText("X : " + String.valueOf(js2.getX()));
                    yView2.setText("Y : " + String.valueOf(js2.getY()));
                }
                else if(arg1.getAction() == MotionEvent.ACTION_UP) {
                    xView2.setText("X : 0");
                    yView2.setText("Y : 0");
                }
                return true;
            }
        });
    }

    private void send_to_server(String StrToSend, boolean ControlString) {
        //Is it a control string ? else ->Data request
        //if (ControlString) {
        //    AC_DATA.SendToTcp = AppPassword + " " + StrToSend;
        //} else {
            AC_DATA.SendToTcp = StrToSend;
        //}
    }

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
        //AC_DATA.mTcpClient.stopClient();
        isTaskRunning= false;

    }

    //!!Todo connect_to_server method and listener

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
                    AC_DATA.parse_tcp_string(message);

                }
            });
            AC_DATA.mTcpClient.SERVERIP = AC_DATA.ServerIp;
            AC_DATA.mTcpClient.SERVERPORT= AC_DATA.ServerTcpPort;
            AC_DATA.mTcpClient.run();

        }

    }

}
