package exomind.lite;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by sir.deenicus on 11/18/2015.
 */
public class TCPBridge {
    private static String TAG = "TCPClient";

    private String SERVER = "xx.xx.xx.xx";
    private static final int PORT = 8016;
    private DatagramSocket socket = null;

    private void startListen() {
        Thread listenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new DatagramSocket(PORT);
                    DatagramPacket packet;

                    byte[] buff = new byte[1000];
                    packet = new DatagramPacket(buff, buff.length);

                    Log.d(TAG, "created buffer of size: " + 1000);
                    while (true) {
                        packet.setLength(buff.length);
                        socket.receive(packet);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception: " + e);
                    if (socket != null) socket.close();
                }
            }
        });
        listenThread.start();
    }

    static void sendData(final String uri, final String ip, final String fname) {
        Thread send = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Connecting to " + ip + ":" + PORT);
                    final InetAddress serverAddress = InetAddress.getByName(ip);

                    Socket socket = new Socket(serverAddress, PORT);
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                    Log.i(TAG, "socket OK");

                    FileInputStream fileInputStream = null;
                    File file = new File(uri);
                    int flen = (int) file.length();
                    int BYTELEN = Math.min(1000, flen);
                    byte[] fbytes = new byte[BYTELEN];
                    byte[] fnb = fname.getBytes();

                    Log.d(TAG, "Reading file ");
                    fileInputStream = new FileInputStream(file);
                    int total = 0;
                    ByteBuffer bb = ByteBuffer.allocate(4);
                    bb.putInt(fnb.length);

                    dataOutputStream.write(bb.array(), 0, 4);
                    dataOutputStream.write(fnb, 0, fnb.length);

                    while (total < flen) {
                        int rlen = fileInputStream.read(fbytes);
                        dataOutputStream.write(fbytes, 0, rlen);
                        total+= rlen;
                    }

                    fileInputStream.close();

                    DataInputStream dataInput = new DataInputStream(socket.getInputStream());
                    int x = dataInput.readByte();

                    dataOutputStream.flush() ;
                    dataInput.close();
                    dataOutputStream.close();
                    socket.shutdownOutput();
                    socket.shutdownInput();
                    socket.close();

                } catch (Exception e) {
                    Log.e(TAG, "Exception: " + e);
                }
            }
        });
        send.start();
    }
}
