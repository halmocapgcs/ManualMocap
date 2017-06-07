/*
	Inspection mode here has been created to allow for a video stream and nudge controls when the
	drone is close to an object and needs to get a better view

	Currently, launching inspection mode restarts the telemetry processes since a broadcast reciever
	or more complex intent has not been implemented. As soon as the buttons are pressed for the first
	time, the app will begin to send RC_4CH commands through mode 1 -- auto1 which has been set to
	AC_MODE_ATTITUDE_RC_CLIMB
		(NOTE: this is not the same mode that paparazzi documentation discusses. We made a slight
		modification such that this mode now also has hover horizontal guidance rather than direct
		horizontal control so that the drone will stay in place and respond to all types of directional
		commands)

	Pressing the return button drops a pin at the current location, waits a little bit to let that
	command register, and then switches mode to auto 2. A timer must then wait a little before ending the
	RC_4CH commands so that the drone does not trigger safe mode and touch down.

	There are some areas here that need to be fixed. The button UI is not very obvious, the timers
	used in the return button cause a large delay after pressing it, there is some lag in the buttons,
	there is a drop in altitude when switching from main to inspection, the pause pin drop is imperfect,
	and the TCP implementation is sloppy and potentially causing some of the lag. Some of these issues,
	such as the button lag, likely cannot be fixed and are problems from paparazzi.

	--6/1/17


 */

package com.hal.manualmocap;

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
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class InspectionMode extends ActionBarActivity {

	private ReadTelemetry TelemetryAsyncTask;
	boolean isTaskRunning;
	private Thread mTCPthread;

	RelativeLayout joystick_left, joystick_right;
	TextView battery_level;
	ThumbPad leftPad, rightPad;
	public Telemetry AC_DATA;

	boolean DEBUG=false;
	boolean newLocationSet = false;
	boolean TcpSettingsChanged;
	boolean UdpSettingsChanged;
	String AppPassword;

	public int AcId, yaw, pitch, roll = 0;
	public int throttle = 63;
	int JoyMessageLimiter = 0;

	int RIGHT = 1;
	int UP = 2;
	int LEFT = 3;
	int DOWN = 4;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_inspection_mode);

		setup_app();
		setup_telemetry_class();

		TelemetryAsyncTask = new ReadTelemetry();
		TelemetryAsyncTask.execute();

		//send paparazzi method to indicate switching to hover mode

	}

	private void setup_telemetry_class() {

		//Create com.hal.manualmocap.Telemetry class
		AC_DATA = new Telemetry();

		//sub in values
		AC_DATA.inspecting = true;
		AC_DATA.ServerIp = "192.168.50.10";
		AC_DATA.ServerTcpPort = 5010;
		AC_DATA.UdpListenPort = 5005;

		AC_DATA.prepare_class();
		AC_DATA.unopened = false;
		AC_DATA.setup_udp();
	}

	public void setup_app(){
		AppPassword = "1234";

		//setup battery
		battery_level = (TextView)findViewById(R.id.battery_level_inspection);
		battery_level.setText("??? v");

		joystick_left = (RelativeLayout)findViewById(R.id.joystick_left);
		joystick_right = (RelativeLayout)findViewById(R.id.joystick_right);

		leftPad = new ThumbPad(joystick_left);
		rightPad = new ThumbPad(joystick_right);

		joystick_left.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_DOWN){
					if(leftPad.getRegion(event) == RIGHT){
						yaw = 15;
					}
					else if(leftPad.getRegion(event) == LEFT){
						yaw = -15;
					}
					else if(leftPad.getRegion(event) == UP){
						throttle = 84;
					}
					else if(leftPad.getRegion(event) == DOWN){
						throttle = 42;
					}
				}
				else if(event.getAction()== MotionEvent.ACTION_UP) {
					yaw = 0;
					throttle = 63;
				}
				return true;
			}
		});

		joystick_right.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == MotionEvent.ACTION_DOWN){
					if(rightPad.getRegion(event) == RIGHT){
						roll = 15;
					}
					else if(rightPad.getRegion(event) == LEFT){
						roll = -15;
					}
					else if(rightPad.getRegion(event) == UP){
						pitch = -15;
					}
					else if(rightPad.getRegion(event) == DOWN){
						pitch = 15;
					}
				}
				else if(event.getAction()== MotionEvent.ACTION_UP){
					pitch = 0;
					roll = 0;
				}
				return true;
			}
		});

		Button returnButton = (Button) findViewById(R.id.Return);
		returnButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AC_DATA.inspecting = false;
				isTaskRunning = false;
				AC_DATA.mTcpClient.sendMessage("removeme");
				AC_DATA.mTcpClient.stopClient();
				finish();
			}
		});
	}

	public void publish_joystick_info(int AcId, int yaw, int throttle, int roll, int pitch) {
		AC_DATA.SendToTcp = ("joyinfo" + " " + 1 + " " + throttle + " " + roll + " " + pitch + " " + yaw);
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

				if(JoyMessageLimiter >= 8) {
					AC_DATA.mTcpClient.sendMessage("joyinfo" + " " + 1 + " " + throttle + " "
							+ roll + " " + pitch + " " + yaw);
					JoyMessageLimiter = 0;
				}
				else{
					JoyMessageLimiter++;
				}

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