package com.hal.manualmocap;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;


/**
 * Created by benwelton on 3/5/17.
 */

public class Telemetry {
    boolean DEBUG;
    public String SendToTcp = null;

    //Static values
    public int MaxNumbOfAC = 25;   //Max aircraft numb

    //Visual change flags
    public boolean ViewChanged = false;  //Every function -willing to change UI needs to raise this flag

    public Context context;
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
    int AcId = 1;

    public void setup_udp() {
        Log.d("PPRZ", "" + UdpListenPort);
        try {
            socket = new DatagramSocket(UdpListenPort);
            //Log.d("PPRZ", "" + socket.isClosed());
            socket.setSoTimeout(150);//This is needed to prevent udp read lock
        } catch (SocketException e) {
            e.printStackTrace();
            if (DEBUG) Log.d("PPRZ_exception", "Udp SocketException");
        }
        byte[] buf = new byte[1024];
        packet = new DatagramPacket(buf, buf.length);

    }

    public void read_udp_data() {

        try {

            socket.receive(packet);

            String2parse=  new String(packet.getData(), packet.getOffset(), packet.getLength());

            if ((String2parse != null) && (!String2parse.equals(String2parse_buf))) {
                String2parse_buf = String2parse;
                if (DEBUG) Log.d("PPRZ_exception", "Udp Package Received:" + String2parse);
                parse_udp_string(String2parse);
                String2parse=null;
            }


        } catch (Exception e) {
            //ignore java.net.SocketTimeoutException
            //if (DEBUG) Log.d("PPRZ_exception", "Error#3 .. Udp Package Read Error:" + e.toString());
        }

    }

    public void parse_udp_string(String LastTelemetryString) {
        //!!Todo
        if (LastTelemetryString.matches("(^ground AP_STATUS .*)")) {

        }

        if (LastTelemetryString.matches("(^ground NAV_STATUS .*)")) {

        }

        if (LastTelemetryString.matches("(^ground ENGINE_STATUS .*)")) {

        }

        if (LastTelemetryString.matches("(^ground FLIGHT_PARAM .*)")) {

        }

        if (LastTelemetryString.matches("(^ground WAYPOINT_MOVED .*)")) {
            //Log.d("UDP", "Parse begin");

            //sample method leads to send data to server via tcp
            get_new_aircraft_data(AcId);
        }

        if (LastTelemetryString.matches("(^ground DL_VALUES .*)")) {

        }
    }

    public void parse_tcp_string(String LastTelemetryString) {
        if (LastTelemetryString.matches("(^AppServer ACd .*)")) {
            //fill in here with respose to confirmation message from server
        }
    }

    private void get_new_aircraft_data(int AcId) {
        //sends message to tcp
        SendToTcp = ("getac " + AcId);
    }

    public class AirCraft {     //Class to hold aircraft data

        public boolean EngineStatusChanged = false;
        boolean isVisible = true;
        boolean AC_Enabled = false;     //Is AC being shown on ui? This also confirms that AC has all its data to shown in ui
        boolean MarkersEnabled = false;    //Markers data has been received and parsed
        boolean BlocksEnabled = false;  //Block data has been received and parsed
        boolean AcReady = false;   //AC has its all data to be ready to show in UI. After showing it AC_Enabled flag will be true.
        int AC_Id;
        String AC_Name;
        String AC_Color;
        String AC_Type;
        String AC_LaunchID;
        String AC_KillID;
        int AC_AltID;
        String AC_DlAlt;
       // Marker AC_Marker;
        Bitmap AC_Logo;
        Bitmap AC_Carrot_Logo;
        //Marker AC_Carrot_Marker;
        //LatLng AC_Carrot_Position;
        //Queue<LatLng> AC_Path;

        String Altitude;
        boolean Altitude_Changed = false;
        String AGL;

        String Heading = "0";
        String Speed;
        String Roll = "0";
        String Pitch = "0";
        String Throttle;
        String AirSpeed = "N/A";
        boolean ApStatusChanged = false;
        String FlightTime;
        String ApMode;
        String GpsMode;
        String StateFilterMode;
        String Battery;
        boolean AC_Position_Changed = false;

        int NumbOfWps = 1;

        boolean NewMarkerAdded = false;
        boolean MarkerModified = false;

        int SelectedBlock = 1;

        int BlockCount;
        boolean NewBlockAdded = false;

        boolean AirspeedEnabled = false;
        boolean AirspeedChanged = false;
    }
}


