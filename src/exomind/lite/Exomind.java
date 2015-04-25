package exomind.lite;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent; 
import android.net.Uri; 
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream; 
import java.lang.reflect.Method;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Enumeration;

public class Exomind extends Activity {
    /**
     * Called when the activity is first created.
     */
    private static String TAG = "FileClient";

    private String SERVER = "xx.xx.xx.xx";
    private static final int PORT = 8016;
    EditText ipbox;

    public  byte [] concatAll(byte[] first, byte[]... rest) {
        int totalLength = first.length;
        for (byte[] array : rest) {
            totalLength += array.length;
        }
        byte [] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte [] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Context context = getApplicationContext();

        final Intent i = new Intent(context, FilePickerActivity.class);

        ipbox = (EditText) findViewById(R.id.ipText);

        ((TextView) findViewById(R.id.ipview)).setText("Your IP: " + getLocalIpAddress());

        findViewById(R.id.uploadButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(i, 0); 
            }
        });

        startListen();
    }

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


    void sendData(final String uri, final String fname) {
        Thread send = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Connecting to " + SERVER + ":" + PORT);
                    final InetAddress serverAddress = InetAddress.getByName(ipbox.getText().toString());

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                // For JellyBean and above
                ClipData clip = data.getClipData();

                if (clip != null) {
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        Uri uri = clip.getItemAt(i).getUri();
                    }
                }
            } else {
                Uri uri = data.getData();
                sendData(uri.getPath(), uri.getLastPathSegment());
            }
        }
    }

    //http://stackoverflow.com/questions/21898456/get-android-wifi-net-hostname-from-code
    public static String getHostName() {
        try {
            Method getString = Build.class.getDeclaredMethod("getString", String.class);
            getString.setAccessible(true);
            return getString.invoke(null, "net.hostname").toString();
        } catch (Exception ex) {
            return "";
        }
    }

    //http://android-er.blogspot.com/2014/02/android-sercerclient-example-server.html
    private String getLocalIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();

                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip = inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip = "Something Wrong! " + e.toString() + "\n";
        }
        return ip;
    }
}
