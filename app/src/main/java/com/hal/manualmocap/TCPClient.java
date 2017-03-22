package com.hal.manualmocap;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by benwelton on 3/5/17.
 */

public class TCPClient {
    private String serverMessage;
    public String SERVERIP = "192.168.50.10"; //server ip
    public int SERVERPORT = 5010;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;

    PrintWriter out;
    BufferedReader in;

    /**
     *  Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public TCPClient(OnMessageReceived listener) {
        mMessageListener = listener;
    }

    /**
     * Sends the message entered by client to the server
     * @param message text entered by client
     */
    public void sendMessage(String message){
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
            Log.d("PPRZ_info", "tcp msgsend (tcpclient):" +message);
        }
    }

    public interface OnMessageReceived {
        public void messageReceived(String message);
    }

    public void run() {

        mRun = true;

        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(SERVERIP);

            Log.e("TCP Client", "C: Connecting...");

            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, SERVERPORT);

            try {

                //send the message to the server
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                 Log.e("TCP Client", "C: Sent.");

                //receive the message which the server sends back
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //in this while the client listens for the messages sent by the server
                while (mRun) {

                    serverMessage = in.readLine();

                    if (serverMessage != null && mMessageListener != null) {
                        //call the method messageReceived from MyActivity class
                        Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");
                        mMessageListener.messageReceived(serverMessage);

                    }
                    serverMessage = null;
                    //connection alive?
                    /*
                    if (!socket.isBound()) {
                        Log.e("TCP", "Socket is closed");
                    }*/

                }

                Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + serverMessage + "'");

            } catch (Exception e) {

                 Log.e("TCP", "S: Error", e);

            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                socket.close();
            }

        } catch (Exception e) {

            Log.e("TCP", "C: Error", e);

        }

    }


    public void stopClient(){
        mRun = false;
    }
}
