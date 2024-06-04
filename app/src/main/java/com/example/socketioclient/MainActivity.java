package com.example.socketioclient;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ScreenRecording.ScreenRecording;

public class MainActivity extends AppCompatActivity {

    EditText ip;
    Button connect;

    protected static final int START_CAPTURE = 1000;
    private ScreenRecording sr;

    private MediaProjectionManager projectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int dpi = metrics.densityDpi;

        ip = (EditText) findViewById(R.id.etIP);
        connect = (Button) findViewById(R.id.btnConn);
        connect.setOnClickListener(v -> {
            Log.i("BUTTON", "Pressed");
            String ipAddr = ip.getText().toString();

            this.projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            this.sr = new ScreenRecording(this, projectionManager, width, height, dpi, ipAddr);
            startCapture();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            sr.startMediaProjection(resultCode, data);
        } else {
            Toast.makeText(this, "ERROR WHILE LOADING onActivityResult", Toast.LENGTH_LONG).show();
        }
    }

    protected void startCapture() {
        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, START_CAPTURE);
    }

    protected void stopCapture() {
        sr.stopCapturing();
    }
}