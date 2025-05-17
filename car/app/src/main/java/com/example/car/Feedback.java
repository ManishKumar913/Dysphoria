package com.example.car;

import android.app.Activity;
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


import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
//import tech.gusavila92.websocketclient.WebSocketClient;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

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
    private TextView ts,tt,tf,tp;







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

        connectWebSocket();
        manualSensorInput();



//        // Run object detection
//        List<DetectionResult> results = detectObjects(bitmap);
//
//        // Display results
//        StringBuilder resultText = new StringBuilder();
//        for (DetectionResult result : results) {
//            resultText.append(String.format("%s: %.2f%%\n", result.label, result.confidence * 100));
//        }
//        resultView.setText(resultText.toString());
    }

    void manualSensorInput(){

        skt.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tt.setText("Engine Temp -> "+ progress);
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
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

//    private List<DetectionResult> detectObjects(Bitmap bitmap) {
//        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
//        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);
//
//        // Define output buffer
//        // Adjust the output shape and data type based on your model's output
//        float[][][] output = new float[1][25200][85]; // Example shape for YOLOv8n
//
//        tflite.run(inputBuffer, output);
//
//        // Post-processing to extract detection results
//        return parseDetectionResult(output[0]);
//    }
//
//    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap)     {
//        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4);
//        byteBuffer.order(ByteOrder.nativeOrder());
//        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
//        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
//        int pixel = 0;
//        for (int i = 0; i < INPUT_SIZE; ++i) {
//            for (int j = 0; j < INPUT_SIZE; ++j) {
//                final int val = intValues[pixel++];
//                byteBuffer.putFloat((((val >> 16) & 0xFF) / 255.0f));
//                byteBuffer.putFloat((((val >> 8) & 0xFF) / 255.0f));
//                byteBuffer.putFloat(((val & 0xFF) / 255.0f));
//            }
//        }
//        return byteBuffer;
//    }
//
//    private List<DetectionResult> parseDetectionResult(float[][] detections) {
//        List<DetectionResult> results = new ArrayList<>();
//        for (float[] detection : detections) {
//            float confidence = detection[4];
//            if (confidence > 0.5) { // Confidence threshold
//                float maxClass = 0;
//                int classId = -1;
//                for (int i = 5; i < detection.length; i++) {
//                    if (detection[i] > maxClass) {
//                        maxClass = detection[i];
//                        classId = i - 5;
//                    }
//                }
//                if (maxClass > 0.5) { // Class probability threshold
//                    float x = detection[0];
//                    float y = detection[1];
//                    float w = detection[2];
//                    float h = detection[3];
//                    RectF rect = new RectF(
//                            x - w / 2,
//                            y - h / 2,
//                            x + w / 2,
//                            y + h / 2
//                    );
//                    results.add(new DetectionResult(labels.get(classId), confidence * maxClass, rect));
//                }
//            }
//        }
//        return results;
//    }
//
//    private static class DetectionResult {
//        String label;
//        float confidence;
//        RectF location;
//
//        DetectionResult(String label, float confidence, RectF location) {
//            this.label = label;
//            this.confidence = confidence;
//            this.location = location;
//        }
//    }
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
                // Connection opened
            }

            @Override
            public void onMessage(String message) {

                byte[] decodedBytes = Base64.decode(message, Base64.DEFAULT);
                bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                runOnUiThread(() -> im.setImageBitmap(bitmap));
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



