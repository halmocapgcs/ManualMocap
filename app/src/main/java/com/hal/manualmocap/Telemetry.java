package com.hal.manualmocap;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


/**
 * Created by benwelton on 3/5/17.
 */

public class Telemetry {
    boolean DEBUG;
    boolean unopened = true;
    boolean inspecting = false;
    public String SendToTcp = null;

    //Visual change flags
    public boolean ViewChanged = false;  //Every function -willing to change UI needs to raise this flag
    public boolean BatteryChanged = false;

    public float GraphicsScaleFactor = 1;

    public Context context;
    public AirCraft AircraftData;
    /*
    Comm variables
     */
    public String ServerIp;
    public int UdpListenPort;
    public TCPClient mTcpClient;
    public int ServerTcpPort;
    private DatagramPacket packet;
    private String String2parse;
    private String String2parse_buf = "";
    private DatagramSocket socket;
    int AcId = 31;
    int yaw, throttle, roll, pitch;

    public void prepare_class() {
        //Initial creation
        AircraftData = new AirCraft();
    }

    public void setup_udp() {
        if(unopened) {
            Log.d("PPRZ", "" + UdpListenPort);
            try {
                Main.sSocket = new DatagramSocket(UdpListenPort);
                Main.sSocket.setSoTimeout(150);//This is needed to prevent udp read lock
                unopened = false;
            } catch (SocketException e) {
                e.printStackTrace();
                if (DEBUG) Log.d("PPRZ_exception", "Udp SocketException");
            }
        }
        byte[] buf = new byte[1024];
        packet = new DatagramPacket(buf, buf.length);

    }

    public void read_udp_data(DatagramSocket socket) {

        try {

            socket.receive(packet);

            String2parse=  new String(packet.getData(), packet.getOffset(), packet.getLength());

            if ((String2parse != null) && (!String2parse.equals(String2parse_buf))) {
                String2parse_buf = String2parse;
                if (DEBUG) Log.d("PPRZ_exception", "Udp Package Received:" + String2parse);
                parse_udp_string(String2parse);
                String2parse=null;
            }

            //get_new_aircraft_data(AcId);
            if(!inspecting) publish_joystick_info(AcId, yaw, throttle, roll, pitch);

        } catch (Exception e) {
            //ignore java.net.SocketTimeoutException
            //if (DEBUG) Log.d("PPRZ_exception", "Error#3 .. Udp Package Read Error:" + e.toString());
        }

    }

    public void parse_udp_string(String LastTelemetryString) {
        //this is just code from the PPRZonDroid app, but only in relation to battery life
        //Parse ENGINE_STATUS messages
        if (LastTelemetryString.matches("(^ground ENGINE_STATUS .*)")) {
            String[] ParsedData = LastTelemetryString.split(" ");

            String bat = ParsedData[7].substring(0, (ParsedData[7].indexOf(".") + 2));

            //If battery is changed this will impact ui
            if (!(bat.equals(AircraftData.Battery))) {
                Log.d("PPRZ_info", "Old Battery=" + AircraftData.Battery + " New Battery:" + bat);
                AircraftData.Battery = bat;
                BatteryChanged = true;
                ViewChanged = true;
            }
        }

        if (LastTelemetryString.matches("(^ground FLIGHT_PARAM .*)")) {
            String[] ParsedData = LastTelemetryString.split(" ");

            AircraftData.Heading = ParsedData[5];
            AircraftData.Position = new LatLng(Double.parseDouble(ParsedData[6]), Double.parseDouble(ParsedData[7]));
			AircraftData.Altitude = ParsedData[10].substring(0, ParsedData[10].indexOf(".") + 1);

			if(Integer.parseInt(AircraftData.Altitude) <= 0) AircraftData.Altitude = "0.0";
			AircraftData.Altitude = AircraftData.Altitude + " m";

			if(AircraftData.AC_Enabled){
                AircraftData.AC_Position_Changed = true;
				AircraftData.Altitude_Changed = true;
                ViewChanged = true;
            }
        }

		if (LastTelemetryString.matches("(^ground AP_STATUS .*)")) {

			String[] ParsedData = LastTelemetryString.split(" ");
			Long FlightTime = Long.parseLong(ParsedData[9]);
			Long Hours, Minutes;

			Hours = TimeUnit.SECONDS.toHours(FlightTime);
			FlightTime = FlightTime - TimeUnit.HOURS.toSeconds(Hours);
			Minutes = TimeUnit.SECONDS.toMinutes(FlightTime);
			FlightTime = FlightTime - TimeUnit.MINUTES.toSeconds(Minutes);

			AircraftData.FlightTime = Long.toString(Hours) + ":" + Long.toString(Minutes) + ":" + Long.toString(FlightTime);

			AircraftData.ApStatusChanged = true;
			ViewChanged = true;

		}
    }

    public void parse_tcp_string(String LastTelemetryString) {
        if (LastTelemetryString.matches("(^AppServer ACd .*)")) {
            String[] ParsedData = LastTelemetryString.split(" ");

            if(Integer.parseInt((ParsedData[2])) == AcId) {
                AircraftData.AC_Enabled = true;
            }
        }
    }

    public void get_new_aircraft_data(int AcId) {
        //sends message to tcp
        SendToTcp = ("getac " + AcId);
    }

    public void publish_joystick_info(int AcId, int yaw, int throttle, int roll, int pitch) {
        SendToTcp = ("joyinfo" + " " + "1" + " " + throttle + " " + roll + " " + pitch + " " + yaw);
        //Log.d("Joy", SendToTcp);
    }

    public class AirCraft{
        boolean AC_Enabled = false;
        boolean AC_Position_Changed = false;
		boolean Altitude_Changed = false;
		boolean ApStatusChanged = false;

        String Battery;
        Marker AC_Marker1, AC_Marker2;
        Bitmap AC_Logo;
        LatLng Position;
		String Altitude;
        String Heading = "0";
		String FlightTime;
    }
}


