package com.hal.manualmocap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

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

    private GoogleMap mMap;

    public Telemetry AC_DATA;
    public int AcId = 31;
    SharedPreferences AppSettings;

    private ReadTelemetry TelemetryAsyncTask;
    boolean isTaskRunning = false;
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

        //initialize map
        mMap = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();

        //initialize map options
        GoogleMapOptions mMapOptions = new GoogleMapOptions();

        mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        LatLng labOrigin = new LatLng(36.005417, -78.940984);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(labOrigin, 50));
        CameraPosition rotated = new CameraPosition.Builder()
                .target(labOrigin)
                .zoom(50)
                .bearing(90.0f)
                .build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(rotated));

        BitmapDescriptor labImage = BitmapDescriptorFactory.fromResource(R.drawable.fullroomoriginmanual);
        GroundOverlay trueMap = mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(labImage)
                .position(labOrigin, (float) 35)
                .bearing(90.0f));



        //Disable zoom and gestures to lock the image in place
        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(false);
        mMap.getUiSettings().setTiltGesturesEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d("coord", latLng.toString());
            }
        });

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
                    if(js1.getX()>0) AC_DATA.yaw = 15;      //right button for yaw
                    if(js1.getX()<0) AC_DATA.yaw = -15;     //left button for yaw
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

    public void refresh_map_data(){
        if (null == AC_DATA.AircraftData.AC_Marker) {
            add_AC_to_map();
        }

        if (AC_DATA.AircraftData.AC_Enabled && AC_DATA.AircraftData.AC_Position_Changed) {
            AC_DATA.AircraftData.AC_Marker.setPosition(convert_to_lab(AC_DATA.AircraftData.Position));
            AC_DATA.AircraftData.AC_Marker.setRotation(Float.parseFloat(AC_DATA.AircraftData.Heading));
            AC_DATA.AircraftData.AC_Position_Changed = false;
        }


    }

    public void add_AC_to_map(){
        if (AC_DATA.AircraftData.AC_Enabled) {
            AC_DATA.AircraftData.AC_Logo = create_ac_icon(Color.RED, AC_DATA.GraphicsScaleFactor);

            AC_DATA.AircraftData.AC_Marker = mMap.addMarker(new MarkerOptions()
                    .position(convert_to_lab(AC_DATA.AircraftData.Position))
                    .anchor((float) 0.5, (float) 0.5)
                    .flat(true)
                    .rotation(Float.parseFloat(AC_DATA.AircraftData.Heading))
                    .draggable(false)
                    .icon(BitmapDescriptorFactory.fromBitmap(AC_DATA.AircraftData.AC_Logo))
            );
        }
        else{
            AC_DATA.get_new_aircraft_data(AcId);
        }
    }

    public Bitmap create_ac_icon(int ColorType, float GraphicsScaleFactor) {

        int AcColor = ColorType;

        int w = (int) (34 * GraphicsScaleFactor);
        int h = (int) (34 * GraphicsScaleFactor);
        Bitmap.Config conf = Bitmap.Config.ARGB_4444; // see other conf types
        Bitmap bmp = Bitmap.createBitmap(w, h, conf); // this creates a MUTABLE bitmapAircraftData[IndexOfAc].AC_Color
        Canvas canvas = new Canvas(bmp);

        canvas = create_selected_canvas(canvas, AcColor, GraphicsScaleFactor);


        //Create rotorcraft logo
        Paint p = new Paint();

        p.setColor(AcColor);


        p.setStyle(Paint.Style.STROKE);
        //p.setStrokeWidth(2f);
        p.setAntiAlias(true);

        Path ACpath = new Path();
        ACpath.moveTo((3 * w / 16), (h / 2));
        ACpath.addCircle(((3 * w / 16) + 1), (h / 2), ((3 * w / 16) - 2), Path.Direction.CW);
        ACpath.moveTo((3 * w / 16), (h / 2));
        ACpath.lineTo((13 * w / 16), (h / 2));
        ACpath.addCircle((13 * w / 16), (h / 2), ((3 * w / 16) - 2), Path.Direction.CW);
        ACpath.addCircle((w / 2), (13 * h / 16), ((3 * w / 16) - 2), Path.Direction.CW);
        ACpath.moveTo((w / 2), (13 * h / 16));
        ACpath.lineTo((w / 2), (5 * h / 16));
        ACpath.lineTo((6 * w / 16), (5 * h / 16));
        ACpath.lineTo((w / 2), (2 * h / 16));
        ACpath.lineTo((10 * w / 16), (5 * h / 16));
        ACpath.lineTo((w / 2), (5 * h / 16));

        canvas.drawPath(ACpath, p);

        Paint black = new Paint();
        black.setColor(Color.BLACK);
        black.setStyle(Paint.Style.STROKE);
        black.setStrokeWidth(6f);
        black.setAntiAlias(true);

        canvas.drawPath(ACpath, black);
        p.setStrokeWidth(3.5f);
        canvas.drawPath(ACpath, p);
        return bmp;
    }

    private Canvas create_selected_canvas(Canvas CanvIn, int AcColor, float GraphicsScaleFactor) {

        int w = CanvIn.getWidth();
        int h = CanvIn.getHeight();

        float SelLineLeng = 4 * GraphicsScaleFactor;

        Path SelPath = new Path();
        SelPath.moveTo(0, 0); //1
        SelPath.lineTo(SelLineLeng, 0);
        SelPath.moveTo(0, 0);
        SelPath.lineTo(0, SelLineLeng);
        SelPath.moveTo(w, 0); //2
        SelPath.lineTo(w - SelLineLeng, 0);
        SelPath.moveTo(w, 0);
        SelPath.lineTo(w, SelLineLeng);
        SelPath.moveTo(w, h);   //3
        SelPath.lineTo(w, h - SelLineLeng);
        SelPath.moveTo(w, h);
        SelPath.lineTo(w - SelLineLeng, h);
        SelPath.moveTo(0, h);
        SelPath.lineTo(0, h - SelLineLeng);
        SelPath.moveTo(0, h);
        SelPath.lineTo(SelLineLeng, h);

        Paint p = new Paint();
        //p.setColor(AcColor);
        p.setColor(Color.YELLOW);
        p.setAntiAlias(true);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3 * GraphicsScaleFactor);
        CanvIn.drawPath(SelPath, p);
        return CanvIn;
    }

    public LatLng convert_to_lab(LatLng position){
        double oldLat = position.latitude;
        double oldLong = position.longitude;

        double newLat = 1.75*oldLat - 27.00401175;
        double newLong = 1.777778*oldLong + 61.39861865;

        LatLng newPosition = new LatLng(newLat, newLong);
        return newPosition;
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

            refresh_map_data();


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
                    //update AC position
                    AC_DATA.parse_tcp_string(message);

                }
            });
            AC_DATA.mTcpClient.SERVERIP = AC_DATA.ServerIp;
            AC_DATA.mTcpClient.SERVERPORT= AC_DATA.ServerTcpPort;
            AC_DATA.mTcpClient.run();

        }

    }

}
