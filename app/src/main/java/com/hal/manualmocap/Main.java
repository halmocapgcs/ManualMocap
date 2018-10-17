package com.hal.manualmocap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
//package com.hal.manualmocap;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;

import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.R.attr.delay;
import static android.R.attr.right;

public class Main extends Activity implements IVideoPlayer {

    private static final String TAG = Main.class.getSimpleName();
    //telemetry variables
    public static final String SERVER_IP_ADDRESS = "server_ip_adress_text";
    public static final String SERVER_PORT_ADDRESS = "server_port_number_text";
    public static final String LOCAL_PORT_ADDRESS = "local_port_number_text";
    public static final int DRONE_LENGTH = 5;
    public static final int DISTANCE_FROM_WALL = 12;
    private static final int MAX_USER_ID = 42;
    private static final LatLng LAB_ORIGIN = new LatLng(36.005417, -78.940984);

    private int mVideoHeight;
    private int mVideoWidth;
    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;
    private float multiplier;
    private double rawAltitude;

	public int percent = 100;
    public int AcId = 31;

    private int click_count = 0;
    private SurfaceView mSurfaceView1, mSurfaceView2 ;
    private FrameLayout mSurfaceFrame1, mSurfaceFrame2;
    private SurfaceHolder mSurfaceHolder1, mSurfaceHolder2;
    private Surface mSurface1 = null, mSurface2 = null;

    private LibVLC mLibVLC1, mLibVLC2;

    private String mMediaUrl1, mMediaUrl2;
    private String[] temp_options;
    private String[] new_options;

    boolean DEBUG=true;
	boolean lowBatteryUnread = true;
	boolean emptyBatteryUnread = true;
    String AppPassword;

    private GoogleMap mMap1, mMap2;
    private GroundOverlay trueMap1, trueMap2;
    private int mapIndex = 0;
    private int[] mapImages = {R.drawable.modulezone_manual, R.drawable.trainingzone_manual, R.drawable.croppedexperimentzone};
    public Point currentPosition;
    public Telemetry AC_DATA;

    SharedPreferences AppSettings;

    private ReadTelemetry TelemetryAsyncTask;
    boolean isTaskRunning = false;
    private Thread mTCPthread;

    static DatagramSocket sSocket = null;
    private EventLogger logger;

    //joystick variables
    RelativeLayout layout_joystick_left, layout_joystick_right;
    Button power_button,toggle, map_swap, toggle_to_video;
    ViewFlipper vf_big,vf_small;
    JStick js1, js2;
    //TextView xView1, xView2, yView1, yView2;
    TextView battery_level, flight_time, altitude;
	ImageView mImageView;

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
        mMap1 = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();
        mMap2 = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map_small)).getMap();

        //initialize map options
        GoogleMapOptions mMapOptions = new GoogleMapOptions();

        mMap1.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap2.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap1.moveCamera(CameraUpdateFactory.newLatLngZoom(LAB_ORIGIN, 50));
        mMap2.moveCamera(CameraUpdateFactory.newLatLngZoom(LAB_ORIGIN, 50));
        CameraPosition rotated1 = new CameraPosition.Builder()
                .target(LAB_ORIGIN)
                .zoom(50)
                .bearing(90.0f)
                .build();
		CameraPosition rotated2 = new CameraPosition.Builder()
				.target(LAB_ORIGIN)
				.zoom(20)
				.bearing(90.0f)
				.build();
        mMap1.moveCamera(CameraUpdateFactory.newCameraPosition(rotated1));
        mMap2.moveCamera(CameraUpdateFactory.newCameraPosition(rotated2));

        Log.d("Maptime", "Begin overlay load");
        BitmapDescriptor labImage = BitmapDescriptorFactory.fromResource(mapImages[mapIndex]);
        trueMap1 = mMap1.addGroundOverlay(new GroundOverlayOptions()
                .image(labImage)
                .position(LAB_ORIGIN, (float) 35.05)
                .bearing(90.0f));
        trueMap2 = mMap2.addGroundOverlay(new GroundOverlayOptions()
                .image(labImage)
                .position(LAB_ORIGIN, (float) 35.05)
                .bearing(90.0f));

        blockMapInteraction();

        //setup health and status
		mImageView = (ImageView)findViewById(R.id.iv_battery);
        battery_level = (TextView)findViewById(R.id.Bat_Vol_On_Map);
		flight_time = (TextView)findViewById(R.id.Flight_Time_On_Map);
		altitude = (TextView)findViewById(R.id.Alt_On_Map);

        layout_joystick_left = (RelativeLayout)findViewById(R.id.layout_joystick_left);
        layout_joystick_right = (RelativeLayout)findViewById(R.id.layout_joystick_right);
        power_button = (Button)findViewById(R.id.power_button);
        toggle = (Button)findViewById(R.id.toggle);
        map_swap = (Button)findViewById(R.id.map_swap);

        js1 = new JStick(getApplicationContext(), layout_joystick_left, R.drawable.image_button, "YAW");
        js2 = new JStick(getApplicationContext(), layout_joystick_right, R.drawable.image_button, "PITCH");

        vf_big = (ViewFlipper)findViewById(R.id.vf_1);
        vf_small = (ViewFlipper)findViewById(R.id.vf_2);

        //joystick for throttle and yaw
        layout_joystick_left.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View arg0, MotionEvent arg1) {
                js1.drawStick(arg1);
                //checks to see if the joystick is in the throttle region
                if((arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE) && Math.abs(js1.getX()) < 60) {
                    if(arg1.getAction() == MotionEvent.ACTION_DOWN) logger.logEvent(AC_DATA.AircraftData, EventLogger.YAW_THROTTLE_START);
                    if(js1.getY()>30 && belowTakeoffAltitude()) AC_DATA.throttle = 84;
                    else if(js1.getY()>30 && belowAltitude()) AC_DATA.throttle = 81;
                    else if(js1.getY()<-30) AC_DATA.throttle = 40;
                    else AC_DATA.throttle = 63;
                }
                //checks to see if the joystick is in the yaw region
                else if((arg1.getAction() == MotionEvent.ACTION_DOWN
                        || arg1.getAction() == MotionEvent.ACTION_MOVE) && Math.abs(js1.getX()) >= 72) {
                    if(arg1.getAction() == MotionEvent.ACTION_DOWN) logger.logEvent(AC_DATA.AircraftData, EventLogger.YAW_THROTTLE_START);
                    if(js1.getX()>0) AC_DATA.yaw = 10;      //right button for yaw
                    if(js1.getX()<0) AC_DATA.yaw = -10;     //left button for yaw
                }
                //reset value of yaw but not throttle when lifting up
                else if(arg1.getAction() == MotionEvent.ACTION_UP) {
                    logger.logEvent(AC_DATA.AircraftData, EventLogger.YAW_THROTTLE_END);
                    AC_DATA.yaw = 0;
					AC_DATA.throttle = 63;
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
                    if(arg1.getAction() == MotionEvent.ACTION_DOWN) logger.logEvent(AC_DATA.AircraftData, EventLogger.ROLL_PITCH_START);
                    currentPosition = mMap1.getProjection().toScreenLocation(convert_to_lab(AC_DATA.AircraftData.Position));
                    if(nearWall(currentPosition, DRONE_LENGTH, DISTANCE_FROM_WALL)){
                        multiplier = 2.0f;
                    } else if(nearWall(currentPosition, DRONE_LENGTH, 1.5* DISTANCE_FROM_WALL)){
                        multiplier = 1.75f;
                    } else if(nearWall(currentPosition, DRONE_LENGTH, 2*DISTANCE_FROM_WALL)){
                        multiplier = 1.5f;
                    } else {
                        multiplier = 1.0f;
                    }
                    AC_DATA.roll = (int) (js2.getX()/(4.5f * multiplier));
                    AC_DATA.pitch = (int) -(js2.getY()/(4.5f * multiplier));
                }
                //reset both values to zero when lifting up or in central zone
                else if(arg1.getAction() == MotionEvent.ACTION_UP) {
                    logger.logEvent(AC_DATA.AircraftData, EventLogger.ROLL_PITCH_END);
                    AC_DATA.roll = 0;
                    AC_DATA.pitch = 0;
                }
                return true;
            }
        });

        initializePowerButton();
        initializeMapChangeButton();
        initializeToggleButton();


    }

    private void initializePowerButton() {
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


    private void initializeMapChangeButton() {
        map_swap.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(++mapIndex > 2) mapIndex = 0;
                BitmapDescriptor newLabImage = BitmapDescriptorFactory.fromResource(mapImages[mapIndex]);
                trueMap1.setImage(newLabImage);
                trueMap2.setImage(newLabImage);
                return false;
            }
        });
    }

    private void initializeToggleButton() {
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                click_count++;
                if(click_count%2==1){
                    mLibVLC2.stop();
                    try{
                        mLibVLC1 = new LibVLC();
                        mLibVLC1.setAout(mLibVLC1.AOUT_AUDIOTRACK);
                        mLibVLC1.setVout(mLibVLC1.VOUT_ANDROID_SURFACE);
                        mLibVLC1.setHardwareAcceleration(LibVLC.HW_ACCELERATION_AUTOMATIC);
                        mLibVLC1.setChroma("YV12");
                        mLibVLC1.init(getApplicationContext());

                    }
                    catch (LibVlcException e) {
                        Log.e(TAG, e.toString());
                    }
                    mSurface1 = mSurfaceHolder1.getSurface();
                    mLibVLC1.attachSurface(mSurface1, Main.this);
                    temp_options = mLibVLC1.getMediaOptions(0);
                    List<String> options_list = new ArrayList<String>(Arrays.asList(temp_options));


                    options_list.add(":file-caching=2000");
                    options_list.add(":network-caching=1");
                    options_list.add(":clock-jitter=0");
                    options_list.add("--clock-synchro=1");
                    new_options = options_list.toArray(new String[options_list.size()]);
                    mLibVLC1.playMRL(mMediaUrl1,new_options);

                }
                else {
                    mLibVLC1.stop();
                    try {

                        mLibVLC2 = new LibVLC();
                        mLibVLC2.setAout(mLibVLC2.AOUT_AUDIOTRACK);
                        mLibVLC2.setVout(mLibVLC2.VOUT_ANDROID_SURFACE);
                        mLibVLC2.setHardwareAcceleration(LibVLC.HW_ACCELERATION_AUTOMATIC);
                        mLibVLC2.setChroma("YV12");;
                        mLibVLC2.init(getApplicationContext());

                    } catch (LibVlcException e){
                        Log.e(TAG, e.toString());
                    }
                    mSurface2 = mSurfaceHolder2.getSurface();
                    mLibVLC2.attachSurface(mSurface2, Main.this);
                    temp_options = mLibVLC2.getMediaOptions(0);
                    List<String> options_list = new ArrayList<String>(Arrays.asList(temp_options));


                    options_list.add(":file-caching=2000");
                    options_list.add(":network-caching=1");
                    options_list.add(":clock-jitter=0");
                    options_list.add("--clock-synchro=1");
                    new_options = options_list.toArray(new String[options_list.size()]);
                    mLibVLC2.playMRL(mMediaUrl2,new_options);

                }

                vf_big.showNext();
                vf_small.showNext();


            }
        });
    }

    private void launch_file_dialog(){
        AlertDialog.Builder fileDialog = new AlertDialog.Builder(Main.this);
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.HORIZONTAL);
        dialogLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // Create a drop down menu of all possible user ids
        final Spinner userId = new Spinner(this);
        final List<String> userIdSelections = new ArrayList<>();
        for(int i = 1; i<MAX_USER_ID; i++){
            userIdSelections.add(Integer.toString(i));
        }

        ArrayAdapter<String> userIdDataAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        userIdSelections);
        userId.setAdapter(userIdDataAdapter);

        // Create a drop down menu of the experimental groups
        final Spinner experimentalGroups = new Spinner(this);
        final List<String> groupSelections = new ArrayList<>();
        groupSelections.add("Group_1");
        groupSelections.add("Group_2");
        groupSelections.add("Group_3");

        ArrayAdapter<String> groupDataAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        groupSelections);
        experimentalGroups.setAdapter(groupDataAdapter);

        // Create a drop down menu of the modules
        final Spinner modules = new Spinner(this);
        final List<String> moduleSelections = new ArrayList<>();
        moduleSelections.add("Module_3");
        moduleSelections.add("Module_4");
        moduleSelections.add("Module_5");
        moduleSelections.add("Module_6");
        moduleSelections.add("Checkride");
        moduleSelections.add("Experiment");

        ArrayAdapter<String> moduleDataAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        moduleSelections);
        modules.setAdapter(moduleDataAdapter);

        dialogLayout.addView(userId);
        dialogLayout.addView(experimentalGroups);
        dialogLayout.addView(modules);

        fileDialog.setTitle("Choose Experiment Parameters")
                .setMessage("Choose the user ID, the module being tested, and the experimental group.")
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        logger = new EventLogger(
                                "manual" + userId.getSelectedItem() + "_" +
                                        experimentalGroups.getSelectedItem() + "_" +
                                        modules.getSelectedItem() + ".csv");
                    }
                }).create();
        fileDialog.setView(dialogLayout);
        fileDialog.show();

    }

    private void blockMapInteraction() {
        mMap1.getUiSettings().setAllGesturesEnabled(false);
        mMap1.getUiSettings().setZoomGesturesEnabled(false);
        mMap1.getUiSettings().setTiltGesturesEnabled(false);
        mMap1.getUiSettings().setCompassEnabled(false);
        mMap2.getUiSettings().setAllGesturesEnabled(false);
        mMap2.getUiSettings().setZoomGesturesEnabled(false);
        mMap2.getUiSettings().setTiltGesturesEnabled(false);
        mMap2.getUiSettings().setCompassEnabled(false);
    }

    public void refresh_map_data(){
        if (AC_DATA.AircraftData.AC_Marker1 == null) {
            add_AC_to_map();
        }

        if(AC_DATA.AircraftData.AC_Marker2 == null){
			add_AC_to_map();
		}

        if (AC_DATA.AircraftData.AC_Enabled && AC_DATA.AircraftData.AC_Position_Changed) {
            AC_DATA.AircraftData.AC_Marker1.setPosition(convert_to_lab(AC_DATA.AircraftData.Position));
            AC_DATA.AircraftData.AC_Marker1.setRotation(Float.parseFloat(AC_DATA.AircraftData.Heading));

			AC_DATA.AircraftData.AC_Marker2.setPosition(convert_to_lab(AC_DATA.AircraftData.Position));
			AC_DATA.AircraftData.AC_Marker2.setRotation(Float.parseFloat(AC_DATA.AircraftData.Heading));
            AC_DATA.AircraftData.AC_Position_Changed = false;
        }


    }

    public void add_AC_to_map(){
        if (AC_DATA.AircraftData.AC_Enabled) {
            AC_DATA.AircraftData.AC_Logo = create_ac_icon(Color.RED, AC_DATA.GraphicsScaleFactor);

            AC_DATA.AircraftData.AC_Marker1 = mMap1.addMarker(new MarkerOptions()
                    .position(convert_to_lab(AC_DATA.AircraftData.Position))
                    .anchor((float) 0.5, (float) 0.5)
                    .flat(true)
                    .rotation(Float.parseFloat(AC_DATA.AircraftData.Heading))
                    .draggable(false)
                    .icon(BitmapDescriptorFactory.fromBitmap(AC_DATA.AircraftData.AC_Logo))
            );
            AC_DATA.AircraftData.AC_Marker2 = mMap2.addMarker(new MarkerOptions()
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

        double newLat = (1.75*oldLat - 27.00401175);
        double newLong = (1.777778*oldLong + 61.39861865);

        LatLng newPosition = new LatLng(newLat, newLong);
        return newPosition;
    }

	public LatLng convert_to_lab_small(LatLng position){
		double oldLat = position.latitude;
		double oldLong = position.longitude;

		double newLat = (1.75)/2.3*oldLat - 27.00401175;
		double newLong = (1.777778)/2.3*oldLong + 61.39861865;

		double newLatAdjusted = (newLat-oldLat)/2.3 +oldLat;
		double newLongAdjusted = (newLong-oldLong)/2.3 +oldLong;

		LatLng newPosition = new LatLng(newLatAdjusted, newLongAdjusted	);
		return newPosition;
	}

    public boolean belowAltitude(){
        return (Double.parseDouble(AC_DATA.AircraftData.RawAltitude) <= 2.0);
    }

    private boolean belowTakeoffAltitude() {
        return Double.parseDouble(AC_DATA.AircraftData.RawAltitude) <= 0.5;
    }

    public boolean nearWall(Point currentPosition, int droneSize, double distanceFromWall){
        int leftBound = currentPosition.x - droneSize;
        int rightBound = currentPosition.x + droneSize;
        int topBound = currentPosition.y - droneSize;
        int bottomBound = currentPosition.y + droneSize;

        //garage door
        if(rightBound >= 700-distanceFromWall) return true;

        //bottom wall
        if(bottomBound >= 580-distanceFromWall && leftBound >= 434) return true;

        //bottom barrels
        if(leftBound <= 434 + distanceFromWall && bottomBound >= 510 - distanceFromWall) return true;

        //wall by bottom barrels
        if(((leftBound >= 358 - distanceFromWall && leftBound <=358+distanceFromWall) ||
                (rightBound >= 358 - distanceFromWall && rightBound <=358+distanceFromWall))
                && bottomBound >= 332 - distanceFromWall) return true;

        //wall near climbing wall
        if(leftBound >= 67 && rightBound <= 358 && bottomBound >= 417 - distanceFromWall) return true;

        //wall next to common room
        if(leftBound <= 67 + distanceFromWall) return true;

        //upper wall
        if(topBound <= 59 + distanceFromWall) return true;

        //top barrels
        if(topBound <= 120 + distanceFromWall && leftBound >= 358 && rightBound <=454) return true;

        //control room
        if(topBound <= 332 + distanceFromWall && rightBound >=449-distanceFromWall) return true;

        //right tunnel wall
        if(rightBound >= 123 - distanceFromWall && rightBound <= 123 + distanceFromWall
                && topBound <= 338 && bottomBound >= 228) return true;

        //opposite side of right tunnel wall
        if(leftBound <= 123 + distanceFromWall && leftBound >= 123 + distanceFromWall
                && topBound <= 338 && bottomBound >= 228) return true;

        //middle horizontal wall, bottom
        if(topBound <= 228 + distanceFromWall && topBound >= 228 - distanceFromWall
                && leftBound >= 123 && leftBound <=358) return true;

        //middle horizontal wall, top
        if(bottomBound >= 220 -distanceFromWall && bottomBound <= 220 + distanceFromWall
                && leftBound >= 123 && rightBound <= 358) return true;

        //bottom portion of right wall, left
        if(rightBound >= 358 - distanceFromWall && rightBound <= 358 + distanceFromWall
                && bottomBound <= 228 && bottomBound >= 160) return true;

        //bottom portion of right wall, right
        if(leftBound >= 358 - distanceFromWall && leftBound <= 358 + distanceFromWall
                && bottomBound <= 228 && bottomBound >= 160) return true;

        //top portion of right wall, left
        if(rightBound >= 353 - distanceFromWall && rightBound <= 353 + distanceFromWall
                && topBound <= 120) return true;

        //doorway
        if(currentPosition.x>= 358 -distanceFromWall && currentPosition.x<=358 + distanceFromWall
                && (topBound<=120 || bottomBound>=160)) return true;

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_joysticks_land);
        mMediaUrl1 = "file:///sdcard/DCIM/video1.sdp";
        mMediaUrl2 = "file:///sdcard/DCIM/video2.sdp";

        mSurfaceView1 = (SurfaceView) findViewById(R.id.player_surface);
        mSurfaceView2 = (SurfaceView) findViewById(R.id.player_surface_small);
        mSurfaceHolder1 = mSurfaceView1.getHolder();
        mSurfaceHolder2 = mSurfaceView2.getHolder();

        mSurfaceFrame1 = (FrameLayout) findViewById(R.id.player_surface_frame);
        mSurfaceFrame2 = (FrameLayout) findViewById(R.id.player_surface_frame_small);
        //mMediaUrl = getIntent().getExtras().getString("videoUrl");
        try {
            mLibVLC2 = new LibVLC();
            mLibVLC2.setAout(mLibVLC2.AOUT_AUDIOTRACK);
            mLibVLC2.setVout(mLibVLC2.VOUT_ANDROID_SURFACE);
            mLibVLC2.setHardwareAcceleration(LibVLC.HW_ACCELERATION_AUTOMATIC);
            mLibVLC2.setChroma("YV12");
            mLibVLC2.init(getApplicationContext());

        } catch (LibVlcException e){
            Log.e(TAG, e.toString());
        }

        mSurface2 = mSurfaceHolder2.getSurface();
        //mSurface2 = mSurfaceHolder2.getSurface();
        mLibVLC2.attachSurface(mSurface2, Main.this);
        //mLibVLC2.attachSurface(mSurface2,Main.this);

        temp_options = mLibVLC2.getMediaOptions(0);
        List<String> options_list = new ArrayList<String>(Arrays.asList(temp_options));


        options_list.add(":file-caching=2000");
        options_list.add(":network-caching=1");
        options_list.add(":clock-jitter=0");
        options_list.add("--clock-synchro=1");
        new_options = options_list.toArray(new String[options_list.size()]);
        mLibVLC2.playMRL(mMediaUrl2,new_options);
        //mLibVLC2.playMRL(mMediaUrl2,new_options);

        //setup telemetry data
        set_up_app();
        setup_telemetry_class();

        TelemetryAsyncTask = new ReadTelemetry();
        TelemetryAsyncTask.execute();

        launch_file_dialog();
    }

    protected void onDestroy() {
        super.onDestroy();

         //MediaCodec opaque direct rendering should not be used anymore since there is no surface to attach.
    }

    @Override
    public void eventHardwareAccelerationError(){}

    @Override
    public void setSurfaceLayout(final int width, final int height, int visible_width, int visible_height, final int sar_num, int sar_den){
        Log.d(TAG, "setSurfaceSize -- START");
        if (width * height == 0)
            return;

        // store video size
        mVideoHeight = height;
        mVideoWidth = width;
        mVideoVisibleHeight = visible_height;
        mVideoVisibleWidth = visible_width;
        mSarNum = sar_num;
        mSarDen = sar_den;

        Log.d(TAG, "setSurfaceSize -- mMediaUrl: " + mMediaUrl1 + " mVideoHeight: " + mVideoHeight + " mVideoWidth: " + mVideoWidth + " mVideoVisibleHeight: " + mVideoVisibleHeight + " mVideoVisibleWidth: " + mVideoVisibleWidth + " mSarNum: " + mSarNum + " mSarDen: " + mSarDen);
    }
    @Override
    public int configureSurface(android.view.Surface surface, int i, int i1, int i2){
        return -1;
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

            refresh_map_data();

			if (AC_DATA.BatteryChanged) {

				Bitmap bitmap = Bitmap.createBitmap(
						55, // Width
						110, // Height
						Bitmap.Config.ARGB_8888 // Config
				);
				Canvas canvas = new Canvas(bitmap);
				//canvas.drawColor(Color.BLACK);
				Paint paint = new Paint();
				paint.setStyle(Paint.Style.FILL);
				paint.setAntiAlias(true);
				double battery_double = Double.parseDouble(AC_DATA.AircraftData.Battery);
				double battery_width = (12.5 - battery_double) / (.027);
				int val = (int) battery_width;


				int newPercent = (int) (((battery_double - 9.8)/(10.9-9.8)) * 100);
				if(newPercent >= 100 && percent >= 100){
					battery_level.setText("" + percent + " %");
				}
				if(newPercent < percent && newPercent >= 0) {
					battery_level.setText("" + newPercent + " %");
					percent = newPercent;
				}


				if (percent> 66) {
					paint.setColor(Color.parseColor("#18A347"));
				}
				if (66 >= percent && percent >= 33) {
					paint.setColor(Color.YELLOW);

				}
				if (33 > percent && percent > 10) {
					paint.setColor(Color.parseColor("#B0090E"));
					if(lowBatteryUnread) {
						Toast.makeText(getApplicationContext(), "Warning: Low Battery", Toast.LENGTH_SHORT).show();
						lowBatteryUnread = false;
					}
				}
				if (percent <= 10) {
					if(emptyBatteryUnread) {
						Toast.makeText(getApplicationContext(), "No battery remaining. Land immediately", Toast.LENGTH_SHORT).show();
						emptyBatteryUnread = false;
					}
				}
				int padding = 10;
				Rect rectangle = new Rect(
						padding, // Left
						100 - (int) (90*((double) percent *.01)), // Top
						canvas.getWidth() - padding , // Right
						canvas.getHeight() - padding // Bottom
				);

				canvas.drawRect(rectangle, paint);
				mImageView.setImageBitmap(bitmap);
				mImageView.setBackgroundResource(R.drawable.battery_image_empty);
				AC_DATA.BatteryChanged = false;
			}

			if (AC_DATA.AircraftData.Altitude_Changed) {
				altitude.setText(AC_DATA.AircraftData.Altitude);
				AC_DATA.AircraftData.Altitude_Changed = false;
			}

			if(AC_DATA.AircraftData.ApStatusChanged){
				flight_time.setText(AC_DATA.AircraftData.FlightTime + " s");

				AC_DATA.AircraftData.ApStatusChanged = false;
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
