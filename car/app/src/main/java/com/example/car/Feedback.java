package com.example.car;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;


import okhttp3.WebSocket;


import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

public class Feedback extends AppCompatActivity {
    private WebSocket ws;
    private ImageView im;
    private WebSocketClient webSocketClient;
    private Bitmap bitmap;
    private static final String TAG = "YOLOv8Android";
    private static final String MODEL_NAME = "yolov8n_float32.tflite";
    private static final String LABELS_FILE = "labels.txt";
    private static final int INPUT_SIZE = 640; // Adjust based on your model
    private static final boolean IS_MODEL_QUANTIZED = false;
    private Interpreter tflite;
    private List<String> labels;
    private ImageView imageView;
    private TextView resultView;
    private SeekBar sks,skt,skf,skp;
    private TextView ts,tt,tf,tp,text,text3 ;

    private float s, p, t,f;
    private Timer timer = new Timer();
    String op,ad;









    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feedback);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        skt=findViewById(R.id.engineTemp);
        skp=findViewById(R.id.tyrePressure);
        sks=findViewById(R.id.speed);
        skf=findViewById(R.id.fuel);

        tt=findViewById(R.id.engineTempLabel);
        tp=findViewById(R.id.tyrePressureLabel);
        ts=findViewById(R.id.speedLabel);
        tf=findViewById(R.id.fuelLabel);
        text=findViewById(R.id.textView2);
        text3=findViewById(R.id.textView3);

        connectWebSocket();


        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                manualSensorInput();
                loadModel();
                runOnUiThread(() -> {
                    runMl( s, p , t,  f);
                    text.setText(op+"\n"+ad);

                });
            }
        }, 0, 1000);





//        String output = CameraMl.processFrame(bitmap);
//        Log.d("DriverAssist", output);

    }


    void runMl(float s,float p ,float t, float f) {

        float[] input = {s, p, f, t};

        float[][] output = new float[1][5];


        tflite.run(input, output);
        Log.d("DriverAssist", Arrays.toString(output));

    }

    private void loadModel() {
        try {
            AssetFileDescriptor fileDescriptor = getAssets().openFd("driver_feedback_model.tflite");
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer model = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            tflite = new Interpreter(model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    void manualSensorInput(){

        skt.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tt.setText("Engine Temp -> "+ progress);
                t=progress;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sks.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ts.setText("Speed -> "+ progress);
                s=progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        skp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tp.setText("Tyre Presure -> "+ progress);
                p=progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        skf.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tf.setText("Fuel capcity -> "+ progress);
                f=progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }


    private void connectWebSocket() {
        im= findViewById(R.id.imageView);
        URI uri;
        try {
            uri = new URI("ws://192.168.56.92:8765"); // Replace with your Raspberry Pi's IP and port
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshake) {

            }

            @Override
            public void onMessage(String message) {
                try {
                    JSONObject json = new JSONObject(message);
                    runOnUiThread(() -> text3.setText(String.valueOf(json)));

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                // Connection closed
            }

            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        };
        webSocketClient.connect();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

}



