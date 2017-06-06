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
    boolean unopened = true;
    boolean inspecting = false;
    public String SendToTcp = null;

    //Visual change flags
    public boolean ViewChanged = false;  //Every function -willing to change UI needs to raise this flag
    public boolean BatteryChanged = false;

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

    public void publish_joystick_info(int AcId, int yaw, int throttle, int roll, int pitch) {
        SendToTcp = ("joyinfo" + " " + "1" + " " + throttle + " " + roll + " " + pitch + " " + yaw);
        //Log.d("Joy", SendToTcp);
    }

    public class AirCraft{
        String Battery;
    }
}


