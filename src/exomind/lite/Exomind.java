package exomind.lite;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class Exomind extends Activity {
    /**
     * Called when the activity is first created.
     */
    private static String TAG = "WSTCPClient";

    EditText ipbox;
    WebSocket ws;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Context context = getApplicationContext();

        final Intent i = new Intent(context, FilePickerActivity.class);

        ipbox = (EditText) findViewById(R.id.ipText);

        ((TextView) findViewById(R.id.ipview)).setText("Your IP: " + Util.getLocalIpAddress());

        findViewById(R.id.uploadButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(i, 0);
            }
        });

        findViewById(R.id.connectButton).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        connectWebSocket();
                    }
                });
    }

    private void connectWebSocket() {
        Thread webSocketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String ip = "ws://" + ipbox.getText().toString() + ":8083/websocket.android";
                    ws = new WebSocketFactory().createSocket(ip);

                    ws.addListener(new WebSocketAdapter() {
                        @Override
                        public void onTextMessage(WebSocket websocket, String message) throws Exception {
                            // Received a text message.
                            String path = Environment.getExternalStorageDirectory() + "/Documents/text.txt";
                            SaveTextFile(path, message);
                        }
                    });
                    ws.connect();

                } catch (Exception e) {
                    Log.e(TAG, "Exception: " + e);

                }
            }
        });
        webSocketThread.start();
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
                TCPBridge.sendData(uri.getPath(),ipbox.getText().toString(), uri.getLastPathSegment());
            }
        }
    }

    //http://stackoverflow.com/questions/14376807/how-to-read-write-string-from-a-file-in-android
     void SaveTextFile(String path, String txt) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter( openFileOutput(path, Context.MODE_PRIVATE));
            outputStreamWriter.write(txt);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
}
