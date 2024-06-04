package com.clearcaptions.transport.network;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import com.clearcaptions.transport.protocol.ProtocolInterface;
import okhttp3.*;

import com.clearcaptions.transport.CCEnvironment;
import com.clearcaptions.transport.CaptionSessionState;
import com.clearcaptions.transport.CCLog;
import com.clearcaptions.transport.protocol.ProtocolWizard;

/**
 * A barebones chat client that uses the WebSocket protocol.
 */
public class CaptionClient extends OkHttpClient  {
	ProtocolWizard gandolf;
	CCWebSocketListener listener;
	WebSocket ws;

	public CaptionClient(String uri, ProtocolWizard gandolf) {
		super();
		listener = new CCWebSocketListener();
		Request request = new Request.Builder().url(uri).build();
		ws = this.newWebSocket(request, listener);
		this.gandolf = gandolf;
	}

    public void onMessage(String message) {
    	CCLog.trace("CaptionClient::onMessage <== [" + message + "]");
    	try {
			gandolf.processData(message);
		} 
    	catch (IOException e) {
			e.printStackTrace();
		}
    }

	public void send(String outData) {
		ws.send(outData);
	}

	public void close() {
		ws.close(1000,null);
	}

	public final class CCWebSocketListener extends WebSocketListener {
		private static final int NORMAL_CLOSURE_STATUS = 1000;

		@Override
		public void onOpen(WebSocket webSocket, Response response) {
			CCLog.trace("CaptionClient::onOpen [" + response + "]");
			try {
				gandolf.netConnect();
			}
			catch (Exception ex) {
				CCLog.error("CaptionClient: ("+response+") Failure: " + ex.toString());
				gandolf.handleState(CaptionSessionState.STATE_CONNECT_FAILED, ex.getMessage());
			}
		}

		@Override
		public void onMessage(WebSocket webSocket, String message) {
			CCLog.trace("CaptionClient::onMessage <== [" + message + "]");
			try {
				gandolf.processData(message);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onClosing(WebSocket webSocket, int code, String reason) {
			webSocket.close(NORMAL_CLOSURE_STATUS, null);
			//output("Closing : " + code + " / " + reason);
			CCLog.trace("CaptionClient::onClose [" + code + "] [" + reason + "]");

			gandolf.handleState(CaptionSessionState.STATE_CALL_ENDED, reason);
		}

		@Override
		public void onFailure(WebSocket webSocket, Throwable t, Response response) {
			//output("Error : " + t.getMessage());
			CCLog.trace("CaptionClient::onIOError [" + t.getMessage() + "]");

			super.onFailure(webSocket, t, response);
			CCLog.trace( "onFailure: "
					+ (t != null ? t.toString() : "t==null")
					+ (response != null ? response.toString() : "response==null"));
			t.printStackTrace();

			// if connection down while in call, we need to reconnect...
			gandolf.handleState(CaptionSessionState.STATE_CONNECTION_LOST, t.getMessage());
			if(gandolf.callback() != null)
				gandolf.callback().processError("CaptionClient.onFailure:", t.getMessage());
		}
	}
}
