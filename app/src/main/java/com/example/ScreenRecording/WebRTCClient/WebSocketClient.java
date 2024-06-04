package com.example.ScreenRecording.WebRTCClient;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class WebSocketClient {

  private final WebRTCClient webRTC;
  public Socket webSocket;

  public WebSocketClient(String server, WebRTCClient webRTC) {
    this.webRTC = webRTC;
    try {
      webSocket = IO.socket(server);
      webSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
         @Override
         public void call(Object... args) {
           Log.i("PLUGIN:Socket.IO", "Connected");
         }
       }).on("offer", new Emitter.Listener() {
         @Override
         public void call(Object... args) {
           String message = (String) args[0];
             try {
                 JSONObject jsonObject = new JSONObject(message);
                 assert jsonObject.getString("type").equals("offer");
                 String sdp = jsonObject.getString("sdp");
                 webRTC.peerConnection.setRemoteDescription(new SdpObserver() {
                     @Override
                     public void onCreateSuccess(SessionDescription sessionDescription) {

                     }

                     @Override
                     public void onSetSuccess() {
                         webRTC.peerConnection.createAnswer(new SdpObserver() {
                             @Override
                             public void onCreateSuccess(SessionDescription sessionDescription) {
                                 // Log the SDP offer or answer
                                 Log.i("WebRTCClient", "SDP: " + sessionDescription.description);
                                 webRTC.peerConnection.setLocalDescription(new SdpObserver() {
                                     @Override
                                     public void onCreateSuccess(SessionDescription sessionDescription) {

                                     }

                                     @Override
                                     public void onSetSuccess() {
                                         try {
                                             JSONObject answer = new JSONObject();
                                             answer.put("type", "answer");
                                             answer.put("sdp", sessionDescription.description);
                                             webSocket.emit("answer", answer.toString(), "C1");
                                             // Log.i("WebRTC", )
                                         } catch (JSONException e) {
                                             throw new RuntimeException(e);
                                         }
                                     }

                                     @Override
                                     public void onCreateFailure(String s) {

                                     }

                                     @Override
                                     public void onSetFailure(String s) {

                                     }
                                 }, sessionDescription);
                             }

                             @Override
                             public void onSetSuccess() {

                             }

                             @Override
                             public void onCreateFailure(String s) {

                             }

                             @Override
                             public void onSetFailure(String s) {

                             }
                         }, webRTC.sdpConstraits);
                     }

                     @Override
                     public void onCreateFailure(String s) {
                         Log.e("PLUGIN:WebRTC", "Failed to create remote description");
                     }

                     @Override
                     public void onSetFailure(String s) {
                         Log.e("PLUGIN:WebRTC", "Failed to set remote description");
                     }
                 }, new SessionDescription(SessionDescription.Type.OFFER, sdp));
             } catch (JSONException e) {
                 throw new RuntimeException(e);
             }
         }
       }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
         @Override
         public void call(Object... args) {
           Log.i("PLUGIN:Socket.IO", "Disconnected");
         }
       });
      webSocket.connect();
    } catch (URISyntaxException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}
