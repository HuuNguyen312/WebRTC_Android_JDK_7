package org.appspot.apprtc;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

import static org.appspot.apprtc.CallActivity.EXTRA_ROOMID;
import static org.appspot.apprtc.CallActivity.EXTRA_URLPARAMETERS;

public class MsgActivity extends Activity
    implements AppRTCClient.SignalingEvents, MsgPcClient.Events {

  private TextView mTvMsg;
  private EditText mEtMsg;
  private Button mBtnSend;

  private AppRTCClient mAppRTCClient;
  private MsgPcClient mMsgPcClient;
  private AppRTCClient.SignalingParameters mSignalingParameters;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_msg);

    final Intent intent = getIntent();
    Uri roomUri = intent.getData();
    String roomId = intent.getStringExtra(EXTRA_ROOMID);
    String urlParameters = intent.getStringExtra(EXTRA_URLPARAMETERS);


    mTvMsg = findViewById(R.id.mTvMsg);
    mEtMsg = findViewById(R.id.mEtMsg);
    mBtnSend = findViewById(R.id.mBtnSend);

    mBtnSend.setOnClickListener(view -> {
      String message = mEtMsg.getText().toString();
      String text = "You: " + message;
      mTvMsg.setText(mTvMsg.getText().toString() + "\n" + text);
      mMsgPcClient.sendMessage(message);
      mEtMsg.setText("");
    });

    mAppRTCClient = new WebSocketRTCClient(this);
    mMsgPcClient = new MsgPcClient(getApplicationContext());
    mMsgPcClient.createPcFactory();

    AppRTCClient.RoomConnectionParameters params =
        new AppRTCClient.RoomConnectionParameters(roomUri.toString(), roomId, false, urlParameters);
    mAppRTCClient.connectToRoom(params);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    disconnect();
  }

  private void disconnect() {
    if (mAppRTCClient != null) {
      mAppRTCClient.disconnectFromRoom();
      mAppRTCClient = null;
    }
    if (mMsgPcClient != null) {
      mMsgPcClient.close();
      mMsgPcClient = null;
    }
  }

  @Override
  public void onConnectedToRoom(AppRTCClient.SignalingParameters params) {
    mSignalingParameters = params;
    mMsgPcClient.createPc(params, this);

    if (params.initiator) {
      mMsgPcClient.createOffer();
    } else {
      if (params.offerSdp != null) {
        mMsgPcClient.setRemoteDescription(params.offerSdp);
        // Create answer. Answer SDP will be sent to offering client in
        // PeerConnectionEvents.onLocalDescription event.
        mMsgPcClient.createAnswer();
      }
      if (params.iceCandidates != null) {
        // Add remote ICE candidates from room.
        for (IceCandidate iceCandidate : params.iceCandidates) {
          mMsgPcClient.addRemoteIceCandidate(iceCandidate);
        }
      }
    }
  }

  @Override
  public void onRemoteDescription(SessionDescription sdp) {
    mMsgPcClient.setRemoteDescription(sdp);
    if (!mSignalingParameters.initiator) {
      mMsgPcClient.createAnswer();
    }
  }

  @Override
  public void onRemoteIceCandidate(IceCandidate candidate) {
    mMsgPcClient.addRemoteIceCandidate(candidate);
  }

  @Override
  public void onRemoteIceCandidatesRemoved(IceCandidate[] candidates) {

  }

  @Override
  public void onChannelClose() {

  }

  @Override
  public void onChannelError(String description) {

  }

  @Override
  public void onLocalDescription(SessionDescription sdp) {
    if (mSignalingParameters.initiator) {
      mAppRTCClient.sendOfferSdp(sdp);
    } else {
      mAppRTCClient.sendAnswerSdp(sdp);
    }
  }

  @Override
  public void onIceCandidate(IceCandidate candidate) {
    mAppRTCClient.sendLocalIceCandidate(candidate);
  }

  @Override
  public void onIceCandidatesRemoved(IceCandidate[] candidates) {

  }

  @Override
  public void onIceConnected() {

  }

  @Override
  public void onIceDisconnected() {

  }

  @Override
  public void onPeerConnectionClosed() {

  }

  @Override
  public void onPeerConnectionStatsReady(StatsReport[] reports) {

  }

  @Override
  public void onPeerConnectionError(String description) {

  }

  @Override
  public void onMessage(String message) {
    mTvMsg.post(() -> {
      String text = "Other: " + message;
      mTvMsg.setText(mTvMsg.getText().toString() + "\n" + text);
    });
  }
}
