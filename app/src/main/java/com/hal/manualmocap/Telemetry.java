package com.hal.manualmocap;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

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
    private DatagramSocket socket = null;
    int AcId = 1;

    public void setup_udp() {
        try {
            socket = new DatagramSocket(UdpListenPort);
            socket.setSoTimeout(150); //This is needed to prevent udp read lock
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
            Toast.makeText(context, "UCP reading properly", Toast.LENGTH_SHORT).show();
            get_new_aircraft_data(AcId);
        }

        if (LastTelemetryString.matches("(^ground DL_VALUES .*)")) {

        }
    }

    public void parse_tcp_string(String LastTelemetryString) {
        if (LastTelemetryString.matches("(^AppServer ACd .*)")) {
            Toast.makeText(context, "TCP tester", Toast.LENGTH_SHORT).show();
        }
    }

    private void get_new_aircraft_data(int AcId) {
        SendToTcp = ("getac " + AcId);
    }

}


