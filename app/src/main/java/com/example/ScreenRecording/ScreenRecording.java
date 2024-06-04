package com.example.ScreenRecording;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;

import androidx.activity.result.ActivityResult;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import com.example.ScreenRecording.WebRTCClient.WebRTCClient;

public class ScreenRecording {

  private static final int FPS = 15;

  private final MediaProjectionManager projectionManager;
  private final Context ctx;
  private final String socketServer;
  private final int dpi;
  private int width;
  private int height;
  private MediaProjection mediaProjection;
   private ImageReader imageReader;
  private final Handler mHandler;
  private boolean capturing;
  public ScreenRecording(Context ctx, MediaProjectionManager projectionManager, int width, int height, int dpi, String socketServer) {
    this.projectionManager = projectionManager;
    this.ctx = ctx;
    this.socketServer = socketServer;
    HandlerThread handlerThread = new HandlerThread("ScreenCaptureHandler");
    handlerThread.start();
    mHandler = new Handler(handlerThread.getLooper());
    this.width = width;
    this.height = height;
    this.dpi = dpi;
     imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
  }

  public void startMediaProjection(int result, Intent data) {
    Log.i("Plugin", "MEDIA PROJECTION");
    int resultCode = result;
    mediaProjection = projectionManager.getMediaProjection(resultCode, data);
//     if (imageReader == null)
//       imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
//     //mediaProjection.createVirtualDisplay("ScreenCapture", imageReader.getWidth(), imageReader.getHeight(), ctx.getResources().getDisplayMetrics().densityDpi, 0, imageReader.getSurface(), null, mHandler);
//    // Initialize all method of webRTC to do screencasting
    WebRTCClient webRTC = new WebRTCClient(this.ctx, socketServer, bitmap -> {

    });
    webRTC.initPeerConnectionFactory();
    webRTC.initPeerConnection("stun:stun.l.google.com");
    webRTC.initMediaStreamTrack("100");
    webRTC.initMediaStream("101");
    webRTC.initSurfaceViewRenderer();
    mediaProjection.createVirtualDisplay("ScreenCaptureWebRTC", width, height, ctx.getResources().getDisplayMetrics().densityDpi, 0, webRTC.surfaceViewRenderer.getHolder().getSurface(), null, mHandler);
    // webRTC.createOffer();


//     imageReader.setOnImageAvailableListener(reader -> {
//       Log.i("Plugin", "IMAGE LOADING");
//       Image image = null;
//       try {
//         image = reader.acquireLatestImage();
//         String base64Jpeg = "";
//         if (image != null) {
//           // Check image format
//           if (image.getFormat() == ImageFormat.JPEG) {
//             // Convert JPEG image to Base64
//             base64Jpeg = JpegToBase64(image);
//           } else if (image.getFormat() == ImageFormat.YUV_420_888) {
//             // Convert YUV_420_888 image to Base64
//             base64Jpeg = YuvToBase64(image);
//           } else if (image.getFormat() == PixelFormat.RGBA_8888) {
//             // Convert RGBA_888 image to Base64
//             base64Jpeg = Rgba8888ToBase64Jpeg(image);
//           } else {
//             Log.e("Plugin", "Unsupported image format: " + image.getFormat());
//           }
//
//           if (!base64Jpeg.isEmpty()) {
//
//           }
//         }
//       } finally {
//         if (image != null) {
//           image.close();
//         }
//       }
//     }, mHandler);
     capturing = true;
  }

  public void stopCapturing() {
    capturing = false;
    if (mediaProjection != null) { // && imageReader != null) {
      mediaProjection.stop();
      mediaProjection = null;
      // imageReader.close();
      // imageReader = null;
    }
  }



  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public MediaProjection getMediaProjection() {
    return mediaProjection;
  }

  private String Rgba8888ToBase64Jpeg(Image image) {
    int width = image.getWidth();
    int height = image.getHeight();

    // Obtener el stride (pitch) del buffer de la imagen
    Image.Plane[] planes = image.getPlanes();
    int pixelStride = planes[0].getPixelStride();
    int rowStride = planes[0].getRowStride();
    ByteBuffer buffer = planes[0].getBuffer();

    // Crear un bitmap con las mismas dimensiones
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

    // Crear un array para los datos de los píxeles
    byte[] pixelData = new byte[buffer.remaining()];
    buffer.get(pixelData);

    // Llenar el bitmap con los datos de los píxeles
    int[] pixels = new int[width * height];
    for (int y = 0; y < height; y++) {
      int rowOffset = y * rowStride;
      for (int x = 0; x < width; x++) {
        int pixelOffset = rowOffset + x * pixelStride;
        // ARGB_8888 format expects 4 bytes per pixel in the order of ARGB
        int r = pixelData[pixelOffset] & 0xFF; // Red
        int g = pixelData[pixelOffset + 1] & 0xFF; // Green
        int b = pixelData[pixelOffset + 2] & 0xFF; // Blue
        int a = pixelData[pixelOffset + 3] & 0xFF; // Alpha

        // ARGB format
        int pixel = (a << 24) | (r << 16) | (g << 8) | b;
        pixels[y * width + x] = pixel;
      }
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

    // Comprimir el bitmap a formato JPEG
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
    byte[] jpegBytes = outputStream.toByteArray();

    // Codificar el byte array JPEG en Base64
    return Base64.encodeToString(jpegBytes, Base64.DEFAULT);
  }


  private String JpegToBase64(Image image) {
    ByteBuffer buffer = image.getPlanes()[0].getBuffer(); // image is JPEG
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
    byte[] jpegBytes = outputStream.toByteArray();
    return Base64.encodeToString(jpegBytes, Base64.DEFAULT);
  }

  public String YuvToBase64(Image image) {
    if (image.getFormat() != ImageFormat.YUV_420_888) {
      throw new IllegalArgumentException("Image format must be YUV_420_888");
    }

    // Get the Y, U, and V buffers
    ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
    ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
    ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

    int ySize = yBuffer.remaining();
    int uSize = uBuffer.remaining();
    int vSize = vBuffer.remaining();

    // Create a new byte array to hold the YUV data
    byte[] nv21 = new byte[ySize + uSize + vSize];

    // Copy Y data
    yBuffer.get(nv21, 0, ySize);

    // Copy U and V data interleaved
    byte[] uByte = new byte[uSize];
    byte[] vByte = new byte[vSize];
    uBuffer.get(uByte);
    vBuffer.get(vByte);
    for (int i = 0; i < uSize; i++) {
      nv21[ySize + 2 * i] = vByte[i];
      nv21[ySize + 2 * i + 1] = uByte[i];
    }

    // Create YuvImage
    YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

    // Convert YuvImage to JPEG
    ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();
    yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, jpegOutputStream);

    // Get the JPEG byte array
    byte[] jpegByteArray = jpegOutputStream.toByteArray();

    // Encode the JPEG byte array into Base64
    return Base64.encodeToString(jpegByteArray, Base64.DEFAULT);
  }
}
