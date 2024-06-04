package com.example.ScreenRecording.WebRTCClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.util.Base64;
import android.util.Log;
import android.view.Surface;

import org.json.JSONException;
import org.json.JSONObject;

import org.webrtc.CapturerObserver;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WebRTCClient {

    private final Context context;
    private final WebRTCClientCallbacks callbacks;
    public PeerConnectionFactory peerConnectionFactory;
    public VideoSource videoSource;
    public VideoTrack videoTrack;
    public PeerConnection peerConnection;
    public PeerConnection.Observer pcObserver;
    public SurfaceViewRenderer surfaceViewRenderer;
    public EglBase rootEglBase;

    public MediaConstraints sdpConstraits;

    WebSocketClient webSocketClient;
    protected MediaProjection mediaProjection;
    private int dpi;
    private int height;
    private int width;

    public WebRTCClient(Context ctx, String SignalingServerIP, WebRTCClientCallbacks callbacks) {
        this.context = ctx;
        rootEglBase = EglBase.create();
        webSocketClient = new WebSocketClient(SignalingServerIP, this);
        // webSocketClient = new WebSocketClient("10.0.2.2:8000", this);
        this.callbacks = callbacks;
        this.sdpConstraits = new MediaConstraints();
    }

    public void initPeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();
    }

    private void sendIceCandidateToRemotePeer(IceCandidate iceCandidate) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("type", "candidate");
        message.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
        message.put("sdpMid", iceCandidate.sdpMid);
        message.put("candidate", iceCandidate.sdp);

        // Assume you have a WebSocket connection named 'webSocket'
        webSocketClient.webSocket.send(message.toString());
    }

    public void initPeerConnection(String uri) {
        // Create a list of STUN and TURN servers
        List<PeerConnection.IceServer> iceServers = Arrays.asList(
                PeerConnection.IceServer.builder(uri).createIceServer(),
                PeerConnection.IceServer.builder("stun:stun.relay.metered.ca:80").createIceServer(),
                PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80")
                        .setUsername("ef20ea7fb58f60a104894329")
                        .setPassword("KccuwUQetvqKrtJB")
                        .createIceServer(),
                PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80?transport=tcp")
                        .setUsername("ef20ea7fb58f60a104894329")
                        .setPassword("KccuwUQetvqKrtJB")
                        .createIceServer(),
                PeerConnection.IceServer.builder("turn:global.relay.metered.ca:443")
                        .setUsername("ef20ea7fb58f60a104894329")
                        .setPassword("KccuwUQetvqKrtJB")
                        .createIceServer(),
                PeerConnection.IceServer.builder("turns:global.relay.metered.ca:443?transport=tcp")
                        .setUsername("ef20ea7fb58f60a104894329")
                        .setPassword("KccuwUQetvqKrtJB")
                        .createIceServer()
        );

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);

        // Create a PeerConnection observer
        pcObserver = new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {}

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {}

            @Override
            public void onIceConnectionReceivingChange(boolean b) {}

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {}

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                // Send the ICE candidate to the remote peer
                // You need to implement this part based on how you plan to communicate with the
                // remote peer
                try {
                    sendIceCandidateToRemotePeer(iceCandidate);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}

            @Override
            public void onAddStream(MediaStream mediaStream) {
                // Get the video track from the MediaStream
                if (!mediaStream.videoTracks.isEmpty()) {
                    VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);

                    // Attach a VideoSink to the remote video track
                    remoteVideoTrack.addSink(videoFrame -> {
                        // Log the video frame
                        Log.i("WebRTCClient", "VideoFrame: " + videoFrame.toString());

                        // Convert the video frame to a Bitmap
                        //Bitmap bitmap = videoFrameToBitmap(videoFrame);

                        // Convert the Bitmap to Base64
                        //String base64 = bitmapToBase64(bitmap);

                        // Send the Base64 string to the WebView using Capacitor
                        //callbacks.onAddStream(base64);
                    });
                }
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {}

            @Override
            public void onDataChannel(DataChannel dataChannel) {}

            @Override
            public void onRenegotiationNeeded() {}

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {}
        };

        // Create a PeerConnection
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, pcObserver);

    }

    public void initMediaStreamTrack(String id) {
        // Create a VideoSource
        videoSource = peerConnectionFactory.createVideoSource(true);

        // Create a VideoTrack
        videoTrack = peerConnectionFactory.createVideoTrack(id, videoSource);
    }

    public void initMediaStream(String label) {
        // Create a MediaStream and add the VideoTrack
        MediaStream mediaStream = peerConnectionFactory.createLocalMediaStream(label);
        mediaStream.addTrack(videoTrack);

        // Add the MediaStream to the PeerConnection
        boolean added = peerConnection.addStream(mediaStream);
        // Log the MediaStream and VideoTrack
        Log.i("WebRTCClient", "MediaStream: " + mediaStream.toString());
        Log.i("WebRTCClient", "Added Stream: " + added);
        Log.i("WebRTCClient", "VideoTrack: " + videoTrack.toString());
    }

    public void initSurfaceViewRenderer() {
//        this.width = width;
//        this.height = height;
//        this.dpi = dpi;
//        this.mediaProjection = mediaProjection;
        surfaceViewRenderer = new SurfaceViewRenderer(context);
        surfaceViewRenderer.init(rootEglBase.getEglBaseContext(), null);
        surfaceViewRenderer.setEnableHardwareScaler(true);
        surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
    }

    private void sendOfferToRemotePeer(SessionDescription sessionDescription) {
        JSONObject message = new JSONObject();
        try {
            message.put("type", "offer");
            message.put("sdp", sessionDescription.description);
            // Assume you have a WebSocket connection named 'webSocket'
            webSocketClient.webSocket.send(message.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void createOffer() {
        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                // Set the local description
                peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        // Local description set successfully
                    }

                    @Override
                    public void onSetSuccess() {
                        // Local description set successfully
                    }

                    @Override
                    public void onCreateFailure(String s) {
                        // Handle error
                    }

                    @Override
                    public void onSetFailure(String s) {
                        // Handle error
                    }
                }, sessionDescription);

                // Send the offer to the remote peer
                // You need to implement this part based on how you plan to communicate with the remote peer
                sendOfferToRemotePeer(sessionDescription);
            }

            @Override
            public void onSetSuccess() {
                // Offer was set successfully
            }

            @Override
            public void onCreateFailure(String s) {
                // Handle error
            }

            @Override
            public void onSetFailure(String s) {
                // Handle error
            }
        }, this.sdpConstraits);
    }

    public void addIceCandidate(String sdpMid, int sdpMLineIndex, String candidate) {
        IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, candidate);
        peerConnection.addIceCandidate(iceCandidate);
    }

    private Bitmap videoFrameToBitmap(VideoFrame videoFrame) {
        // The video frame must be in I420 format
        if (!(videoFrame.getBuffer() instanceof VideoFrame.I420Buffer)) {
            throw new IllegalArgumentException("Video frame must be in I420 format");
        }

        VideoFrame.I420Buffer i420Buffer = (VideoFrame.I420Buffer) videoFrame.getBuffer();

        // Get the YUV planes
        ByteBuffer yPlane = i420Buffer.getDataY();
        ByteBuffer uPlane = i420Buffer.getDataU();
        ByteBuffer vPlane = i420Buffer.getDataV();

        // Get the YUV strides
        // int yStride = i420Buffer.getStrideY();
        // int uStride = i420Buffer.getStrideU();
        // int vStride = i420Buffer.getStrideV();

        // Create a byte array for NV21 data
        byte[] nv21Data = new byte[videoFrame.getRotatedWidth() * videoFrame.getRotatedHeight() * 3 / 2];

        // Convert I420 to NV21
        YuvHelper.I420ToNV21(
                yPlane.array(),
                uPlane.array(),
                vPlane.array(),
                nv21Data,
                videoFrame.getRotatedWidth(), videoFrame.getRotatedHeight()
        );

        // Create a YuvImage
        YuvImage yuvImage = new YuvImage(nv21Data, ImageFormat.NV21, videoFrame.getRotatedWidth(), videoFrame.getRotatedHeight(), null);

        // Convert YuvImage to JPEG
        ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, videoFrame.getRotatedWidth(), videoFrame.getRotatedHeight()), 100, jpegOutputStream);

        // Convert JPEG to Bitmap
        byte[] jpegByteArray = jpegOutputStream.toByteArray();
        return BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        // Convert Bitmap to JPEG
        ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, jpegOutputStream);

        // Convert JPEG to Base64
        byte[] jpegByteArray = jpegOutputStream.toByteArray();
        return Base64.encodeToString(jpegByteArray, Base64.DEFAULT);
    }

    public static class YuvHelper {
        public static void I420ToNV21(byte[] yData, byte[] uData, byte[] vData, byte[] output, int width, int height) {
            int frameSize = width * height;
            int quarter = frameSize / 4;
            int tempFrameSize = frameSize - 1;

            System.arraycopy(yData, 0, output, 0, frameSize); // copy y

            for (int i = 0; i < quarter; i++) {
                output[tempFrameSize + 2 * i + 2] = vData[i]; // copy v
                output[tempFrameSize + 2 * i + 1] = uData[i]; // copy u
            }
        }
    }

    public interface WebRTCClientCallbacks {
        void onAddStream(String bitmap);
    }

}
