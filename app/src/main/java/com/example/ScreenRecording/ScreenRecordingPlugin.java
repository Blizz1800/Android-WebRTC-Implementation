package com.example.ScreenRecording;

// import android.app.Activity;
// import android.content.Context;
// import android.content.Intent;
// import android.media.projection.MediaProjectionManager;
// import android.util.DisplayMetrics;
// import android.util.Log;

// import androidx.activity.result.ActivityResult;

// import com.getcapacitor.Plugin;
// import com.getcapacitor.PluginCall;
// import com.getcapacitor.PluginMethod;
// import com.getcapacitor.annotation.ActivityCallback;
// import com.getcapacitor.annotation.CapacitorPlugin;

// @CapacitorPlugin(name = "ScreenRecording")
// public class ScreenRecordingPlugin extends Plugin {
public class ScreenRecordingPlugin {

  // protected static final int START_CAPTURE = 1000;
  // private ScreenRecording sr;
  // private PluginCall savedCall;

  // @Override
  // public void load() {
  //   DisplayMetrics metrics = getActivity().getApplicationContext().getResources().getDisplayMetrics();
  //   int width = metrics.widthPixels;
  //   int height = metrics.heightPixels;

  //   MediaProjectionManager projectionManager = (MediaProjectionManager) getActivity().getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
  //   this.sr = new ScreenRecording(getActivity().getApplicationContext(), projectionManager, width, height);
  // }

  // @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
  // public void startCapture(PluginCall call) {
  //   call.setKeepAlive(true);
  //   savedCall = call;
  //   Log.i("Plugin", "Starting screen capture");

  //   MediaProjectionManager projectionManager = (MediaProjectionManager) getActivity().getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
  //   Intent captureIntent = projectionManager.createScreenCaptureIntent();
  //   startActivityForResult(call, captureIntent, "handleActivityResult");
  //   Log.i("Plugin", "Start Recording");
  // }

  // @PluginMethod()
  // public void stopCapture(PluginCall call) {
  //   Log.i("Plugin", "Stopping screen capture");
  //   sr.stopCapturing();
  //   call.resolve();
  // }

  // @ActivityCallback
  // private void handleActivityResult(PluginCall call, ActivityResult result) {
  //   if (result.getResultCode() == Activity.RESULT_OK) {
  //     sr.startMediaProjection(result, savedCall);
  //   } else {
  //     call.reject("Screen capture permission denied");
  //   }
  // }
}
