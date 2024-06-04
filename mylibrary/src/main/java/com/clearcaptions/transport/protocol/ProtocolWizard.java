/**
 * @author Wes Rosenberg
 * 
 * The ProtocolWizard allows communication with the ClearCaptions system
 * to start and stop captioning sessions.
 * 
 */
package com.clearcaptions.transport.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.*;

import com.clearcaptions.cloud.captions.Captions;
import com.clearcaptions.javawi.jstun.util.CallTypeUtil;
import com.clearcaptions.transport.CCEnvironment;
import com.clearcaptions.transport.CCTransportType;
import com.clearcaptions.transport.CaptionSessionState;
import com.clearcaptions.transport.PhoneEvent;
import com.clearcaptions.transport.CCLog;
import com.clearcaptions.transport.network.*;

import static com.clearcaptions.transport.CaptionSessionState.STATE_REGISTERING;
import static com.clearcaptions.transport.CaptionSessionState.STATE_SIPREMOTE;

public class ProtocolWizard
{
	private ProtocolInterface	mPI;
	private Socket			mSocket;
	private CaptionClient	webClient;
	private CaptionSessionState	mState;
	private String			mWebSocketHost;
	private String			mHost;
	private int				mPort;
	private int 			connectTimeout;
	private String			mIncomingHost;
	private int				mIncomingPort;
	private String			pushToken;
	private String			deviceID;
	private String			deviceType;
	private String			enterprise;
	private String			ipAddress;
	private String			userAgent;
	private String			callType;
	private String			appVersion;
	private String			deviceCaps;
	private String			instructions;
	private String			userid;
	private String			callGroup;
	private boolean			mRegistered;
	private OutputStream	mOutputStream;
	private InputStream		mInputStream;
	private String			mNumberToCall;
	private String			callbackNumber;
	private StreamIOThread  streamIOInstance;
	private WriteThread		mWriteInstance;
	private ReadThread		mReadInstance;
	private ConnectThread	mConnectInstance;
	private Thread			streamIOThread;
	private Thread			mReadThread;
	private Thread			mConnectThread;
	private Thread			mWriteThread;
	private String			myID;
	private String			opID;
	private int				firstContact;
	private boolean			firstPoll;
	private boolean			waitFirstTime;
	private boolean			dontPoll;
	private String			m_useThisToken;
	private CCEnvironment	env;
	private CCTransportType transportType;
	private int				logstuff;
    private boolean  		inCall;
    private boolean  		attemptingRetry;
    private long 			retryInterval;
    private int socketTimeout;
	private long attempts;
	private long maxAttempts = 3;

	private Timer wanLossTimer;
	private TimerTask timerTask;

	static final char nl = 13;
	static final String FAILURE_MSG = "Sorry, we're experiencing technical difficulties, please try your ClearCaptions call again";
	static final String LOST_MSG = "Captioner lost...";

	public static final String KEEP_ALIVE_TIMEOUT_CODE = "900";
	public static final String KEEP_ALIVE_TIMEOUT_MSG = "WAN loss warning - 5 second keepAlive Timeout exceeded";

	TrustManager[] trustAllCerts = new TrustManager[]{
	    new X509TrustManager() {
	        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	            return null;
	        }
	        public void checkClientTrusted(
	            java.security.cert.X509Certificate[] certs, String authType) {
	        }
	        public void checkServerTrusted(
	            java.security.cert.X509Certificate[] certs, String authType) {
	        }
	    }
	};
	
	public ProtocolWizard()
	{
		mSocket = null;
		webClient = null;
		state(CaptionSessionState.STATE_ONHOOK);
		connectTimeout(1000);
		retryInterval(5000);
		mRegistered = false;
		mOutputStream = null;
		mInputStream = null;
		
		ID("EPAAKNVVBPHJ4P7YHDAB97VK24787V3");
		session("");
		deviceCaps("");
		instructions("");
		appVersion("");
		enterpriseID("");
		callGroup("");
		logEvents(0);
		transportType(CCTransportType.WEBSOCKET);
		inCall(false);
		attemptingRetry = false;
		
		firstContact = 0;
		firstPoll = true;
		dontPoll = false;

		waitFirstTime = true;
	}
	
	/**
	 * Default constructor for ProtocolWizard
	 * Takes ProtocolInterface as a parameter
	 */
	public ProtocolWizard(ProtocolInterface pi)
	{
		this();
		callback(pi);
	}

	public int logEvents() {
		return logstuff;
	}

	public void logEvents(int logstuff) {
		this.logstuff = logstuff;
	}

	public void environment(CCEnvironment env) {
		/* TODO: switch to port 443 for secure web sockets */
		port(10036);
		
		switch (env) {
			case LIVE:
		    	host("css.prod.clearcaptions.com");
		    	webSocketHost("websocket.clearcaptions.com");
		    	setSocketTimeout(5000);
				break;
			case STAGING:
		    	host("css.staging.clearcaptions.com");
		    	webSocketHost("websocket.staging.clearcaptions.com");
		    	setSocketTimeout(5000);
				break;
			case TEST:
		    	host("css.test.clearcaptions.com");
		    	webSocketHost("websocket.test.clearcaptions.com");
		    	setSocketTimeout(5000);
				break;
			case DEV:
		    	host("css.dev.clearcaptions.com");
		    	webSocketHost("websocket.dev.clearcaptions.com");
		    	setSocketTimeout(5000);
				break;
		}
		this.env = env;
	}

	public void customEnvironment(String hostURI, String websocketURI, int portNumber) {
		port(portNumber);
		host(hostURI);
		webSocketHost(websocketURI);
		setSocketTimeout(5000);

		this.env = CCEnvironment.CUSTOM;
	}

	public CCEnvironment environment() {
		return env;
	}

	public void callNumber(String val)
	{
		mNumberToCall = val;
	}
	
	public String callNumber()
	{
		return mNumberToCall;
	}
	
	public void callbackNumber(String val)
	{
		callbackNumber = val;
	}
	
	public String callbackNumber()
	{
		return callbackNumber;
	}
	
	public void connectTimeout(int val)
	{
		connectTimeout = val;
	}
	
	public int connectTimeout()
	{
		return connectTimeout;
	}
	
	private String webSocketHost() {
		return mWebSocketHost;
	}

	private void webSocketHost(String mWebSocketHost) {
		this.mWebSocketHost = mWebSocketHost;
	}

	private int webSocketPort() {
		return 443;
	}

	private void host(String val)
	{
		mHost = val;
	}
	
	private String host()
	{
		return mHost;
	}
	
	public int getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	private void port(int val)
	{
		mPort = val;
	}
	
	private int port()
	{
		return mPort;
	}
	
	private int ATAport()
	{
		return 10038;
	}
	
	public void incomingHost(String val)
	{
		mIncomingHost = val;
	}
	
	public String incomingHost()
	{
		return mIncomingHost;
	}
	
	public void incomingPort(int val)
	{
		mIncomingPort = val;
	}
	
	public int incomingPort()
	{
		return mIncomingPort;
	}
	
	private void state(CaptionSessionState val)
	{
		mState = val;
	}
	
	private CaptionSessionState state()
	{
		return mState;
	}
	
	public void callback(ProtocolInterface pi)
	{

		mPI = pi;
	}
	
	public ProtocolInterface callback()
	{
		return mPI;
	}
	
	public void overrideToken(String token)
	{
		m_useThisToken = token;
	}
	
	public String overrideToken()
	{
		return m_useThisToken;
	}
	
	public void pushToken(String val)
	{
		pushToken = val;
	}
	
	public String pushToken()
	{
		return pushToken;
	}

	public void deviceID(String val)
	{
		deviceID = val;
	}
	
	public String deviceID()
	{
		return deviceID;
	}

	public void deviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String deviceType() {
		return deviceType;
	}

	public String callGroup() {
		return callGroup;
	}

	public void callGroup(String callGroup) {
		this.callGroup = callGroup;
	}

	public void ip(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String ip() {
		return ipAddress;
	}

	public void userAgent(String val)
	{
		userAgent = val;
	}
	
	public String userAgent()
	{
		return userAgent + "(" + appVersion() + ")";
	}

	public void callType(String val)
	{
		callType = val;
		Captions.setIsVoip(CallTypeUtil.isVoipCallType(callType));
	}
	
	public String callType()
	{
		return callType;
	}

	public void appVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	public String appVersion() {

		// Blue-751
		// Ref: https://stackoverflow.com/questions/26551439/getting-maven-project-version-and-artifact-id-from-pom-while-running-in-eclipse/26573884#26573884
		final Properties properties = new Properties();
		try {
			properties.load(this.getClass().getClassLoader().getResourceAsStream("project.properties"));
			this.appVersion = properties.getProperty("version");
			return 	this.appVersion;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "not found";

	}

	public void deviceCaps(String val) {
		deviceCaps = val;
	}

	public String deviceCaps() {
		return deviceCaps;
	}

	public void instructions(String val)
	{
		instructions = val;
	}
	
	public String instructions()
	{
		return instructions;
	}
	
	public void ID(String val)
	{
		userid = val;
	}
	
	public String ID()
	{
		return userid;
	}
	
	public String opID() {
		return opID;
	}

	private void opID(String opID) {
		this.opID = opID;
	}

	public void session(String val)
	{
		myID = val;
	}
	
	public String session()
	{
		return myID;
	}

	/**
	 * Not used by instantiated classes
	 */
	public InputStream inputStream()
	{
		return mInputStream;
	}

	/**
	 * Not used by instantiated classes
	 */
	public int writeQueueSize()
	{
		return mWriteInstance.queueSize();
	}
	
	private long generateInterval(long attempt) {
		double maxInterval = (Math.pow(2, attempt) - 1) * 1000;

		if (maxInterval > 30*1000) {
			maxInterval = 30*1000; // If the generated interval is more than 30 seconds, truncate it down to 30 seconds.
		}
		  
		// generate the interval to a random number between 0 and the maxInterval determined from above
		return (long) (Math.random() * maxInterval); 
	}
	
	/**
	 * Not used by instantiated classes
	 */
	public void handleState(CaptionSessionState state, String msg)
	{
		CaptionSessionState localState;
		String localMsg;
		CCLog.trace("ProtocolWizard.handleState() got state="+state+" current state="+state());
		switch (state) {
			case STATE_CONNECTION_LOST:
				if (state() == CaptionSessionState.STATE_CALL_ENDED) {
					// call is over... just be done..
					stopCall(msg);
					return;
				}

				// try to reconnect...
				if (!attemptingRetry){
					CCLog.trace("ProtocolWizard.handleState() reconnect..");
					attemptingRetry = true;

					localState = state;
					localMsg = msg;

					if (inCall()) {
						CCLog.trace("ProtocolWizard.handleState() incall, connection retry " + attemptingRetry);
						try {
							if (attempts > maxAttempts) {
								// we're done here...
								stopCall(msg);
								return;
							}
							long time = generateInterval(attempts);
							try {
								attempts++;
								Thread.sleep(time);
							}
							catch (Exception e) {
								CCLog.trace("ProtocolWizard.handleState("+state+") got interrupted: " + e.getMessage());
								Thread.currentThread().interrupt(); // restore interrupted status
								stopCall(msg);
								return;
							}
							if (isWebSocketClient()) {
								connect();
							}
							else {
								mReadInstance.quit();
								mWriteInstance.quit();
								streamIOInstance.quit();

								netConnect();
							}
							localMsg = "";

							switch (state()) {
								case STATE_REGISTERING:
									localState = CaptionSessionState.STATE_REGISTERING;
									registerCall();
									break;

								default:
									//Don't resume
									break;
							}
						}
						catch (Exception e1) {
							CCLog.error("ProtocolWizard.handleState() retry connection failed: "+e1.getMessage());
							localState = CaptionSessionState.STATE_CONNECT_FAILED;
							localMsg = FAILURE_MSG;
						}
					}
					else{
						CCLog.trace("ProtocolWizard.handleState() NOT incall, DONE");
						localState = CaptionSessionState.STATE_CALL_ENDED;
						localMsg = LOST_MSG;
						try {
							mReadInstance.quit();
						}
						catch (Exception e) {

						}
						try {
							mWriteInstance.quit();
						}
						catch (Exception e) {

						}
						try {
							streamIOInstance.quit();
						}
						catch (Exception e) {

						}
					}

					if ( callback() != null )
						callback().processState(localState, localMsg);

					attemptingRetry=false;
				}
				break;
			case STATE_CONNECT_FAILED:
			case STATE_CALL_ENDED:
				// cleanup
				session("");
				shutdown(state);
				break;
			default:
				if (callback() != null) {
					callback().processState(state, msg);
				}
				break;
		}
	}

	public void answerCall(String sessionID)
	{
		// handle incoming call where we already have a sessionID to connect back to the bot with
		session(sessionID);
		dontPoll = true;
		connect(CaptionSessionState.STATE_ANSWER, incomingHost(), incomingPort());
	}
	
	public void ignoreCall(String sessionID)
	{
		// handle when a user declines to answer an incoming call...
		session(sessionID);
		connect(CaptionSessionState.STATE_IGNORE, incomingHost(), incomingPort());
	}
	
	/**
	 * sends a keep alive to the server
	 */
	public void keepAlive()
	{
		byte[]	packet;
		CCLog.trace("ProtocolWizard.keepAlive()");
		packet = Buildi711KeepAlive();
		SendCommand(packet, true, false);
	}

	/**
     * Ends a call in process when in stateless mode
     */
    public void cancelCall()
    {
		dontPoll = true;
		state(CaptionSessionState.STATE_CANCEL_CALL);
		if (callback() != null) {
			callback().processState(state(), "Please wait...");
		}

		connect(state(), host(), port());
    }

	/**
	 * Decline incoming call
	 */
	public void declineCall()
	{
		CCLog.trace("ProtocolWizard: declineCall : State ("+state()+") Registered ("+mRegistered+")");
		inCall(false);
		byte[] packet = Buildi711DeclineCallPacket();
		CCLog.trace("ProtocolWizard.declineCall() declineInboundCallPacket="+new String(packet));
		SendCommand(packet, true, false);
	}
	
	/**
	 * Ends a call in process
	 */
	public void endCall()
	{
		CCLog.trace("ProtocolWizard: Hangup : State ("+state()+") Registered ("+mRegistered+")");
		inCall(false);
		if (mRegistered) {
			byte[] packet = Buildi711HangupPacket();
			SendCommand(packet, true, false);
		}
	}
	
	public void logEvent(String event, boolean send)
	{
		if (send) {
			int index = 0;
			String data = "myMethod=logEvent||myID=" + session();
			data += "||userAgent=" + userAgent();
			data += "||callType=" + callType();
			data += "||myUID=||deviceID=" + deviceID();
			data += "||myIP=" + ip();
			data += "||deviceCaps=" + deviceCaps();
			data += "||trace=" + event;
	
			int len = data.length() + 1;
			byte[] packet = new byte[len];
			for (int i=0; i<data.length(); i++)
				packet[index++] = (byte)data.charAt(i); 
			packet[index++] = 0; 
	
			SendCommand(packet, true, false);
		}
	}
	
	public void logEvent(String event)
	{
		logEvent(event, logEvents() > 0);
	}
	
    /**
     *  single pass to start the call, for clients that are stateless
     */
	public void makeCall()
	{
		dontPoll = true;
		state(CaptionSessionState.STATE_CALL);

		if (callback() != null) {
			callback().processState(state(), "Connecting...");
		}

		connect(state(), host(), port());
	}

	/**
	 * Not used by instantiated classes
	 */
	public void processData() throws IOException
	{
		boolean notDone = true;
		while (mReadInstance.available() > 0 && notDone)
		{
			Handlei711InboundPacket(mReadInstance.read());
		}
	}

	public void processData(String data) throws IOException
	{
		if (data.length() > 0) {
			Handlei711InboundPacket(data);
		}
	}

	/**
	 * sends a message from the client
	 */
	public void sendString(String s)
	{
		byte[]	packet;

		packet = Buildi711MessagePacket(s);
		SendCommand(packet, true, false);
	}
	
	public void sendStatusUpdate(PhoneEvent event, String message)
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data = "myMethod=statusUpdate||myID="+session()+"||status="+event.toString()+"||message="+message;
		len = data.length() + 1;

		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 
		SendCommand(packet, true, false);
	}
	
	public void sessionCallActive()
	{
		dontPoll = true;
		state(CaptionSessionState.STATE_SESSION_CALL_ACTIVE);

		port(ATAport());
		connect(state(), host(), port());
	}
	
	public void sessionCallEnd()
	{
		dontPoll = true;
		state(CaptionSessionState.STATE_SESSION_CALL_END);

		port(ATAport());
		connect(state(), host(), port());
	}
	
	public void sessionSIPActive()
	{
		dontPoll = true;
		state(CaptionSessionState.STATE_SESSION_SIP_ESTABLISHED);

		port(ATAport());
		connect(state(), host(), port());
	}
	
	public void sessionSIPRestart()
	{
		dontPoll = true;
		state(CaptionSessionState.STATE_SESSION_SIP_RESTART);

		port(ATAport());
		connect(state(), host(), port());
	}

	public void sessionSIPFailed()
	{
		dontPoll = true;
		state(CaptionSessionState.STATE_SESSION_SIP_FAILED);

		port(ATAport());
		connect(state(), host(), port());
	}

	/**
     *  start the handshake of the call connection
     */
	public void startCall()
	{
		attempts = 1;
		state(STATE_REGISTERING);
		if (callback() != null) {
			callback().processState(state(), "Connecting...");
		}
		CCLog.trace("debug 001");
        CCLog.trace("state : "+state()+" host: "+host()+" port : "+port());
		connect(state(), host(), port());
	}
	
	public void testCall()
	{
    	CCLog.trace("ProtocolWizard.testCall()");
		state(CaptionSessionState.STATE_TEST);
		if (callback() != null) {
			callback().processState(state(), "Connecting...");
		}

		connect(state(), host(), port());
	}
	
	public void endTestCall()
	{
		CCLog.trace("ProtocolWizard: endTestCall : State ("+state()+")");
		byte[] packet = BuildEndTestCallPacket();
		SendCommand(packet, true, false);
	}
	
	public void pickUpCall()
	{
		dontPoll = true;
		state(CaptionSessionState.STATE_WAITING_FOR_INCOMING_CALL);
		connect(state(), host(), port());

	}
	
	public void startCaptions()
	{
		dontPoll = true;
		state(CaptionSessionState.STATE_START_CAPTIONS);
		connect(state(), host(), port());
	}

	/**
	 * Not used by instantiated classes
	 */
	public void stopCall(String msg)
	{
		inCall(false);
		mRegistered = false;
		state(CaptionSessionState.STATE_CALL_ENDED);
		callback().processState(state(), msg);
		closeSocket();
	}
	
	public void updateCall()
	{
		byte[] packet = BuildCallUpdatePacket();
		SendCommand(packet, true, false);
	}

	/** 
	 * Not used by instantiated classes
	 * common public method that allows external threads to start network connectivity
	 */
	public void netConnect() throws Exception
	{
		if (!isWebSocketClient()) {
			if (port() == 0) {
				try{
					mSocket = new Socket();
					InetSocketAddress ia = new InetSocketAddress(host(), port());
					mSocket.connect(ia, connectTimeout());
					if ( mSocket.isConnected() == false ) {
						throw new Exception("Socket not connected");
					}
				}
				catch (Exception e){
					CCLog.error("ProtocolWizard.netConnect() ["+host()+"] ["+port()+"] " + e.getMessage());
					throw e;
				}
			} else if (port() > 80) {
				
				//attempt to connect up to 3 times.
				//after the 3rd failure get out.
				boolean done=false;
				int connectAttemptCount = 0;
				
				while (!done){
					if (connectAttemptCount < 3){
						connectAttemptCount++;
						CCLog.trace("ProtocolWizard.netConnect() - SSL connectAttempt="+connectAttemptCount);
						try {
							SSLContext sc = SSLContext.getInstance("TLS");
						    sc.init(null, trustAllCerts, new java.security.SecureRandom());
				            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) sc.getSocketFactory();
				            mSocket = (SSLSocket) sslsocketfactory.createSocket(host(), port());
							CCLog.trace("ProtocolWizard.netConnect() - " + mSocket);
							if (mSocket != null){

								mOutputStream = mSocket.getOutputStream();
								mInputStream  = mSocket.getInputStream();
								mSocket.setSoTimeout(getSocketTimeout());
								
								if (state() == CaptionSessionState.STATE_ONHOOK || state() == CaptionSessionState.STATE_CALL_ENDED)
								{
									CCLog.trace("ProtocolWizard.netConnect(): Changed Mind [Hangup]");
									return;
								}
																	
								mReadInstance = new ReadThread(this);
								mReadInstance.SetInputStream(mInputStream);
								mReadThread = new Thread(mReadInstance);
								mReadThread.start();
						
								mWriteInstance = new WriteThread(this);
								mWriteInstance.SetOutputStream(mOutputStream);
								mWriteThread = new Thread(mWriteInstance);
								mWriteThread.start();
					
						        streamIOInstance = new StreamIOThread(this);
						        streamIOThread = new Thread(streamIOInstance);
						        streamIOThread.start();									
								done = true;
							}
						}
						catch (Exception e){
							CCLog.error("ProtocolWizard.netConnect(): "+e.getMessage());
							if (connectAttemptCount < 3) {
								Thread.sleep(retryInterval());
							}
						}
					}
					else{
						throw new Exception("Exceeded maximum connection attempts");
					}					
				}
			}
		}
		
        switch (state()) {
	        case STATE_WAITING_FOR_INCOMING_CALL:
	        	acceptInboundCall();
	        	break;
        	case STATE_ANSWER:
        		answerCall();
        		break;
        	case STATE_IGNORE:
        		ignoreCall();
        		break;
        	case STATE_REGISTERING:
    			registerCall();
    			break;
        	case STATE_CALL:
        		makeCallIn();
        		break;
        	case STATE_CANCEL_CALL:
        		cancelCallIn();
        		break;
        	case STATE_SESSION_CALL_ACTIVE:
        		callActive();
        		break;
        	case STATE_SESSION_CALL_END:
        		callEnd();
        		break;
        	case STATE_SESSION_SIP_ESTABLISHED:
        		sipActive();
        		break;
        	case STATE_SESSION_SIP_RESTART:
        		sipRestart();
        		break;
        	case STATE_SESSION_SIP_FAILED:
        		sipFailed();
        		break;
        	case STATE_START_CAPTIONS:
        		startCaptionsIn();
        		break;
        	case STATE_TEST:
        		testCallIn();
        		break;
        		
			default:
				break;
        }								
	}

	private void Handlei711State(String packet, int r)
	{
		String d;

		if (packet == null)
			return;
			
		r += 6;
		d = packet.substring(r);
		if (d.toUpperCase().startsWith("ONLINE"))
		{
			inCall(true);
			if (firstContact == 0)
			{
				firstContact = 1;
				if ( callback() != null )
					callback().processState(CaptionSessionState.STATE_ONLINE, "");
			}

			r = packet.toUpperCase().indexOf("RECEIVEDTEXT=");
			if (r > 0)
			{
				String rest = "";
				r += 13;
				d = packet.substring(r);
			
				r = 0;
				if (d != null)
					r = d.indexOf(":--:--:");
				while (r >= 0)
				{
					if (d.length() >= r+7)
						rest = d.substring(r+7);
					d = d.substring(0, r);
					d += nl + rest;
					r = d.indexOf(":--:--:");
				}
				CCLog.trace("ProtocolWizard.Handlei711State: Inbound ["+ d + "]");
				if ( callback() != null )
					callback().processState(CaptionSessionState.STATE_DATA, d);
			}
		}
		if (d.toUpperCase().startsWith("QUEUED"))
		{
				if (waitFirstTime)
				{
					if ( callback() != null )
						callback().processState(CaptionSessionState.STATE_QUEUED, "Waiting for an available captioner...");
				}
				waitFirstTime = false;
		}
		if (d.toUpperCase().startsWith("DISCONNECTED"))
		{
			endCall();
			if ( callback() != null )
				callback().processState(CaptionSessionState.STATE_OFFLINE, "");
			return;
		}
	}
	
	private void Handlei711InboundPacket(String packet)
	{
		int r = 0;

		CCLog.trace("ProtocolWizard.Handlei711InboundPacket(): Packet [" + packet + "]");
		logEvent(packet);

		if (packet.toUpperCase().indexOf("KEEPALIVE") >= 0) {
			stopWANLossWarningTimer();
			startWANLossWarningTimer();
			return;
		}

		// Blue-979 pass back error code with error message
		r = packet.toUpperCase().indexOf("ERRCODE=");
		if (r >= 0)
		{
			r += 8;
			String d = packet.substring(r);
			int y = d.indexOf("||");
			if (y > 0)
				d = d.substring(0, y);
			String errorCode = d;

			r = packet.toUpperCase().indexOf("ERRMSG=");
			if (r >= 0) {
				r += 7;
				String errorMsg = packet.substring(r);
				if (callback() != null)
					callback().processError(errorCode, errorMsg);
			} else {
				r = packet.toUpperCase().indexOf("ERRMESSAGE=");
				if (r >= 0) {
					r += 11;
					String errorMsg = packet.substring(r);
					if (callback() != null)
						callback().processError(errorCode, errorMsg);
				}
			}
		}

		if ((r = packet.toUpperCase().indexOf("POLLDATA=")) >=0)
			r += 9;
		
		if (r < 0 )
		{
			if ((r = packet.toUpperCase().indexOf("||ERRCODE=PROTOCOL ERROR")) >=0)
				r += 18;
		}
		if (r < 0 )
		{
			if ((r = packet.toUpperCase().indexOf("EXCHANGEDATA=")) >= 0)
				r += 13;
		}

		if (r > 0)
		{
			String d = packet.substring(r);
			int f= d.indexOf("||");
			int code = 0;

			try
			{
				code = Integer.parseInt(d.substring(0, f));
				CCLog.trace("ProtocolWizard.Handlei711InboundPacket(): Poll/Exchange Data code [" + code + "]");
			} 
			catch (Exception e)
			{
				CCLog.debug("ProtocolWizard.Handlei711InboundPacket(): Poll/Exchange Data [" + e + "]");
			}

			r = packet.toUpperCase().indexOf("STATE=");
			if (r > 0)
			{
				Handlei711State(packet, r);
			}
			r = packet.toUpperCase().indexOf("ERRMSG=");
			if (r >= 0)
			{
				r += 7;
				String errorMsg = packet.substring(r);
				if ( callback() != null )
					callback().processState(CaptionSessionState.STATE_ERROR, errorMsg);
			}
		}
		
		r = packet.toUpperCase().indexOf("RESUME=");
		if (r >= 0)
		{
			CCLog.trace("ProtocolWizard.Handlei711InboundPacket(): resume attempted, but resume is disabled");
			handleState(CaptionSessionState.STATE_CALL_ENDED, "resume is disabled");
		}

		r = packet.toUpperCase().indexOf("REGRETURN=");
		if (r >= 0)
		{
			r += 10;
			int code = 0;
			String d = packet.substring(r);
			int    e = d.indexOf("||");

			d = d.substring(0, e);

			try {
				code = Integer.parseInt(d);
			} 
			catch (Exception f) {
				code = 0;
			}
			CCLog.trace("ProtocolWizard: RegReturn [" + code + "]");
			r = packet.toUpperCase().indexOf("POLLING=0");
			if (r > 0 )
				dontPoll = true;

			r = packet.toUpperCase().indexOf("MYID=");
			if ( r > 0 && code == 1)
			{
				r += 5;
				d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				session(d);

				r = packet.toUpperCase().indexOf("LOG=");
				if (r > 0) {
					r += 4;
					d = packet.substring(r);
					y = d.indexOf("||");
					if (y > 0)
						d = d.substring(0, y);
					logEvents(Integer.parseInt(d));
				}
				
				r = packet.toUpperCase().indexOf("RETRYINTERVAL=");
				if (r > 0) {
					r += 14;
					d = packet.substring(r);
					y = d.indexOf("||");
					if (y > 0)
						d = d.substring(0, y);
					retryInterval(Integer.parseInt(d)*1000); // sent in s, store in ms
				}

				state(CaptionSessionState.STATE_ONHOOK);
				mRegistered = true;
				if ( callback() != null )
					callback().processState(state(), "");

				initiateCall();
			}
			else
			{
				String msg = "";
				r = packet.toUpperCase().indexOf("ERRMESSAGE=");
				if (r > 0) {
					r += 11;
					d = packet.substring(r);
					int y = d.indexOf("||");
					if (y > 0)
						d = d.substring(0, y);
					msg = d;
				}
				handleState(CaptionSessionState.STATE_CALL_ENDED, msg);
				return;
			}
		}
		
		r = packet.toUpperCase().indexOf("RELAYSTART=");
		if (r >= 0)
		{
			r += 11;
			String d = packet.substring(r);
			int idx = d.indexOf("||");
			if (idx > 0)
				d = d.substring(0, idx);
			int code = 0;
			try {
				code = Integer.parseInt(d);
			}
			catch (Exception e) {
				code = 0;
			}
			CCLog.trace("ProtocolWizard.Handlei711InboundPacket(): Relay Start [" + code + "]");

			r = packet.toUpperCase().indexOf("STATE=");
			if (r > 0)
			{
				Handlei711State(packet, r);
			}

			r = packet.toUpperCase().indexOf("MYID=");
			if (r > 0)
			{
				r += 5;
				d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				session(d);
			} 
			if (code > 0)
			{
				// check if we should poll or not 
				SendPoll();

				String msg = "Call answered";
				state(CaptionSessionState.STATE_CONNECTED);
				// just to handle the strange case....
				if (callback() != null) {
					callback().processState(state(), msg);
				}
				startWANLossWarningTimer();
				/*/ TODO: processCall obsolete????
				if ( mPI != null ) mPI.processCall(CALL_OUTGOING, mNumberToCall, mNumberToCall, CALL_ANSWERED, 0);
				/*/
			} 
			else
			{
				//r = packet.toUpperCase().indexOf("ERRMESSAGE=");
				String msg = "";
				String up = packet.toUpperCase();
				r = up.indexOf("ERRMESSAGE=");
				if (r >= 0)
				{
					r += 11;
					msg = packet.substring(r);
				}
				handleState(CaptionSessionState.STATE_CALL_ENDED, msg);
				return;
			}
		}
		
		if (packet.toUpperCase().startsWith("MYMETHOD=SWITCHOP"))
		{
			r = packet.toUpperCase().indexOf("SESSIONID=");
			if (r >= 0) {
				r += 10;
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				session(d);
			}
			r = packet.toUpperCase().indexOf("OPID=");
			if (r >= 0) {
				r += 5;
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				opID(d);
				if ( callback() != null ) callback().processState(CaptionSessionState.STATE_ONLINE, opID());
			}
		}
		
		/*
		 * myMethod=statusUpdate||type=firstCaption
		 * myMethod=statusUpdate||type=inProgress||message=
		 */
		if (packet.toUpperCase().startsWith("MYMETHOD=STATUSUPDATE"))
		{
			CaptionSessionState event = CaptionSessionState.STATE_IGNORE;
			String message = "";
			String cType = "TYPE=";
			String cMessage = "MESSAGE=";
			r = packet.toUpperCase().indexOf(cType);
			if (r >= 0)
			{
				r += cType.length();
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				if (d.toUpperCase().equals("FIRSTCAPTION")) {
					event = CaptionSessionState.STATE_DATA_FIRST;
				}
				else if (d.toUpperCase().equals("INPROGRESS")) {
					event = CaptionSessionState.STATE_INPROGRESS;
				}
			}
		
			r = packet.toUpperCase().indexOf(cMessage);
			if (r >= 0)
			{
				r += cMessage.length();
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				message = d;
			}
			
			if (callback() != null) callback().processState(event, message);
		}
		
		/*
		 * myMethod=StateChange||state=Online||OP=2007
		 */
		if (packet.toUpperCase().startsWith("MYMETHOD=STATECHANGE"))
		{
			boolean delayOnline = false;
			r = packet.toUpperCase().indexOf("OP=");
			if (r >= 0)
			{
				r += 3;
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				opID(d);
			}
			
			r = packet.toUpperCase().indexOf("OPNUMBER=");
			if (r >= 0)
			{
				r += 9;
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				if ( callback() != null ) callback().processState(CaptionSessionState.STATE_COMMAND, "AGENT=" + d);
			}
			
			r = packet.toUpperCase().indexOf("OPNUMBER2=");
			if (r >= 0)
			{
				r += 10;
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				if ( callback() != null ) callback().processState(CaptionSessionState.STATE_COMMAND, "AGENT2=" + d);
			}
			
			r = packet.toUpperCase().indexOf("SIPREMOTE=");
			if (r >= 0)
			{
				r += 10;
				String sipuri = packet.substring(r);
				int y = sipuri.indexOf("||");
				if (y > 0)
					sipuri = sipuri.substring(0, y);
				if ( callback() != null ) callback().processState(STATE_SIPREMOTE, sipuri);
				delayOnline = true;
			}
			
			r = packet.toUpperCase().indexOf("SIPLOCAL=");
			if (r >= 0)
			{
				r += 9;
				String sipuri = packet.substring(r);
				int y = sipuri.indexOf("||");
				if (y > 0)
					sipuri = sipuri.substring(0, y);
				if ( callback() != null ) callback().processState(CaptionSessionState.STATE_SIPLOCAL, sipuri);
				delayOnline = true;
			}

			r = packet.toUpperCase().indexOf("STATE=");
			if (r >= 0)
			{
				r += 6;
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				
				if (d.toUpperCase().equals("ONLINE"))
				{
					inCall(true);
					if (!delayOnline) {
						if (firstContact == 0)
						{
							firstContact = 1;
							if (callback() != null) {
								callback().processState(CaptionSessionState.STATE_ONLINE, opID());
							}
						}
					}
				}
				else if (d.toUpperCase().equals("QUEUED"))
				{
					if ( callback() != null ) callback().processState(CaptionSessionState.STATE_WAITING, opID());
				}
			}
		}
		
		if (packet.toUpperCase().startsWith("MYMETHOD=SENDTEXT"))
		{
			//myMethod=sendText||text= ** advertising ** ||type=macro
			String cType = "TYPE=";
			String cText = "TEXT=";
			String dataType = "";
			r = packet.toUpperCase().indexOf(cType);
			if (r >= 0)
			{
				r += cType.length();
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				dataType = d;
			}
					
			r = packet.toUpperCase().indexOf(cText);
			if (r >= 0)
			{
				r += cText.length();
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				String msg = d;
				if (callback() != null) {
					CaptionSessionState event = CaptionSessionState.STATE_DATA;
					if (dataType.toUpperCase().equals("MACRO")) {
						event = CaptionSessionState.STATE_DATA_MACRO;
					}
					else if (dataType.toUpperCase().equals("REVOICED")) {
						event = CaptionSessionState.STATE_DATA_CAPTION;						
					}
					callback().processState(event, msg);
				}
			}
		}
		
		if (packet.toUpperCase().startsWith("MYMETHOD=SENDCOMMAND"))
		{
			String theCommand = "";
			String theParty = "";

			/*
			 * "party type": called, assisted, agent?
			 */
			r = packet.toUpperCase().indexOf("TYPE=");
			if (r >= 0)
			{
				r += 5;
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				theParty = d;
			}

			r = packet.toUpperCase().indexOf("VOLUME=");
			if (r >= 0)
			{
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				theCommand = d;
			}

			if ( callback() != null ) 
				callback().processState(CaptionSessionState.STATE_COMMAND, theParty + "||" + theCommand);
		}

		/*
		 * mymethod=startSIPSession||opNumber=
		 */
		if (packet.toUpperCase().indexOf("MYMETHOD=STARTSIPSESSION") >= 0)
		{
			String sipLocal = "";
			String sipRemote = "";
			
			r = packet.toUpperCase().indexOf("SIPLOCAL=");
			if (r >= 0)
			{
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				sipLocal = d;
			}

			r = packet.toUpperCase().indexOf("SIPREMOTE=");
			if (r >= 0)
			{
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				sipRemote = d;
			}

			CCLog.trace("ProtocolWizard.Handlei711InboundPacket(): startSIPSession [" + sipLocal + "] [" + sipRemote + "]");
			if ( callback() != null ) {
				callback().processState(CaptionSessionState.STATE_SESSION_SIP_ESTABLISHED, sipLocal + "||" + sipRemote);
			}
		}
		
		/*
		 * mymethod=startSIPSession||opNumber=
		 */
		if (packet.toUpperCase().indexOf("MYMETHOD=RESTARTSIPSESSION") >= 0)
		{
			String sipLocal = "";
			String sipRemote = "";
			
			r = packet.toUpperCase().indexOf("SIPLOCAL=");
			if (r >= 0)
			{
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				sipLocal = d;
			}

			r = packet.toUpperCase().indexOf("SIPREMOTE=");
			if (r >= 0)
			{
				String d = packet.substring(r);
				int y = d.indexOf("||");
				if (y > 0)
					d = d.substring(0, y);
				sipRemote = d;
			}

			CCLog.trace("ProtocolWizard.Handlei711InboundPacket(): restartSIPSession [" + sipLocal + "] [" + sipRemote + "]");
			if ( callback() != null ) {
				callback().processState(CaptionSessionState.STATE_SESSION_SIP_RESTART, sipLocal + "||" + sipRemote);
			}
		}
		
		if (packet.toUpperCase().indexOf("MYMETHOD=STOPSIPSESSION") >= 0)
		{
			CCLog.trace("ProtocolWizard.Handlei711InboundPacket(): stopSession");
			if ( callback() != null ) {
				callback().processState(CaptionSessionState.STATE_SESSION_SIP_STOP, "");
			}
		}
		
		if (packet.toUpperCase().indexOf("STOPRELAY=") >= 0)
		{
			inCall(false);
			handleState(CaptionSessionState.STATE_CALL_ENDED, "");
		}
		
		if (packet.toUpperCase().indexOf("MYMETHOD=TESTCALL") >= 0)
		{
			r = packet.toUpperCase().indexOf("SIPDIAL=");
			if (r >= 0)
			{
				r += 8;
				String sipuri = packet.substring(r);
				int y = sipuri.indexOf("||");
				if (y > 0)
					sipuri = sipuri.substring(0, y);
				CCLog.trace("ProtocolWizard.Handlei711InboundPacket(): testSIPSession [" + sipuri + "]");
				if ( callback() != null ) callback().processState(STATE_SIPREMOTE, sipuri);
			}
		}		

		if (packet.toUpperCase().indexOf("MYMETHOD=ENDTESTCALL") >= 0)
		{
			inCall(false);
			handleState(CaptionSessionState.STATE_CALL_ENDED, "");
		}		
	}

	private void SendPoll()
	{
		byte[] packet;
		int i, len;
		String data;
		int index = 0;
		
		data = "";
		
		if (!dontPoll)
		{
			data = "myMethod=PollData||myID="+session();
			if (firstPoll)
			{
				firstPoll = false;
				data += "||firstPoll=1";
			}
			if (firstContact == 1)
			{
				firstContact = 2;
				data += "||firstOpContact=1";
			}
		}

		if (data.length() > 0)
		{
			len = data.length() + 1;

			packet = new byte[len];
		
			for (i=0; i<data.length(); i++)
				packet[index++] = (byte)data.charAt(i); 
			packet[index++] = 0; 

			SendCommand(packet, true, true);
		}
	}
	
	private void initiateCall()
	{
		byte[]	packet;

		CCLog.trace("ProtocolWizard.initiateCall(): InitiateCall()");
		state(CaptionSessionState.STATE_CALLING);
		if ( callback() != null ) callback().processState(state(), "Placing Call");

		packet = Buildi711CallPacket();
		SendCommand(packet, true, false);
	}
	
	public void connect()
	{
		String uri = "wss://" + webSocketHost() + ":" + webSocketPort();

		CCLog.trace("ProtocolWizard.startCall() websocket uri: " + uri);
		webClient = new CaptionClient(uri, this);

	}

    private void connect(CaptionSessionState state, String host, int port) {
        state(state);
		/*
		 * two paths now, the original custom socket and the newer websocket
		 *  
		 */
		if (isWebSocketClient()) {
			connect();
		} 
		else {
	        closeSocket();
			mConnectInstance = new ConnectThread(this, host(), port());
			mConnectThread = new Thread(mConnectInstance);
			mConnectThread.start();
		}
    }
 
	private Socket closeSocket()
	{
		Socket ret = mSocket;
		if (mSocket != null) {
			CCLog.trace("ProtocolWizard.closeSocket(): socket");
			if ( mWriteInstance != null )
				mWriteInstance.quit();
		}
		if (webClient != null) {
			CCLog.trace("ProtocolWizard.closeSocket(): websocket");
			webClient.close();
		}
		mOutputStream = null;
		mInputStream = null;
		if (mReadInstance != null) {
			mReadInstance.SetInputStream(mInputStream);
		}
		return ret;
	}

	private void shutdown(CaptionSessionState state)
	{
		CCLog.trace("ProtocolWizard.shutdown");
		stopWANLossWarningTimer();
		mNumberToCall = "";
		mRegistered = false;
		state(state);
		if (callback() != null){
			callback().processState(state(), "");
		}
		firstContact = 0;
		waitFirstTime = true;
		firstPoll = true;
		dontPoll = false;

		closeSocket();
	}
	
	private byte[] BuildCallActivePacket()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data = "myMethod=CallActive";
		data += "||deviceType=" + deviceType();
		data += "||deviceID=" + deviceID();
		data += "||myIP=" + ip();
		data += "||deviceCaps=" + deviceCaps();
		data += "||callType="+callType();
		data += "||number="+callNumber();
		len = data.length() + 1;
		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
	private byte[] BuildCallEndPacket()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data = "myMethod=CallEnd";
		data += "||deviceType=" + deviceType();
		data += "||deviceID=" + deviceID();
		data += "||myIP=" + ip();
		len = data.length() + 1;
		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
	private byte[] BuildSIPActivePacket()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data = "myMethod=SIPActive";
		data += "||deviceType=" + deviceType();
		data += "||deviceID=" + deviceID();
		data += "||myIP=" + ip();
		data += "||number=" + callNumber();
		len = data.length() + 1;
		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
	private byte[] BuildSIPRestartPacket()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data = "myMethod=SIPRestart";
		data += "||deviceType=" + deviceType();
		data += "||deviceID=" + deviceID();
		data += "||myIP=" + ip();
		len = data.length() + 1;
		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
	private byte[] BuildSIPFailedPacket()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data = "myMethod=SIPFailed";
		data += "||deviceType=" + deviceType();
		data += "||deviceID=" + deviceID();
		data += "||myIP=" + ip();
		len = data.length() + 1;
		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
    private byte[] Buildi711MessagePacket(String text)
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data = "myMethod=ExchangeData||myID="+session()+"||myText="+text;
		len = data.length() + 1;

		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}

	private byte[] Buildi711RegistrationRequest()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data = "myMethod=Register||userAgent="+userAgent()+"||callType="+callType()+"||myUID="+ID()+"||deviceID="+deviceID()+"||deviceToken="+pushToken()+"||useEncryption=0||enterprise="+enterpriseID()+"||";
		if (callGroup().length() > 0) {
			data += "callGroup=" + callGroup()  + "||";
		}
		
		len = data.length() + 1;

		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
	private byte[] BuildInboundCallPacket()
	{
		//myMethod=InboundCall||sessionID=%@||deviceID=%@||userAgent=%@
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data  = "myMethod=AcceptInboundCall||sessionID=" + session() + "||deviceID=" + deviceID();
		data += "||userAgent=" + userAgent() + "||myUID=" + ID() + "||callType=125";

		len = data.length() + 1;

		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
	private byte[] Buildi711CallPacket()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data  = "myMethod=StartRelay||myID=" + session() + "||preferredLanguage=English";
		data += "||number=" + callNumber() + "||callbacknumber=" + callbackNumber();
		data += "||instructions=" + instructions() + "||userAgent=" + userAgent();
		data += "||callType=" + callType() + "||myUID="+ID()+"||deviceID=" + deviceID();
		data += "||myIP=" + ip();
		data += "||deviceCaps=" + deviceCaps();

		CCLog.trace("the data is"+data);

		len = data.length() + 1;

		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
	private byte[] BuildEasyCallPacket()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data = "myMethod=StartRelay||myID=" + session();
		data += "||preferredLanguage=English";
		data += "||number="+callNumber();
		data += "||callbacknumber=" + callbackNumber();
		data += "||instructions=" + instructions();
		data += "||userAgent=" + userAgent();
		data += "||callType=" + callType();
		data += "||myUID=" + ID();
		data += "||deviceID=" + deviceID();
		data += "||myIP=" + ip();
		data += "||deviceCaps=" + deviceCaps();
		if (callGroup().length() > 0) {
			data += "||callGroup=" + callGroup();
		}
		len = data.length() + 1;
		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
	private byte[] BuildTestCallPacket()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data = "myMethod=TestCall||myID=" + session();
		data += "||userAgent=" + userAgent();
		data += "||callType=" + callType();
		data += "||myUID=||deviceID=" + deviceID();
		data += "||myIP=" + ip();
		data += "||deviceCaps=" + deviceCaps();
		len = data.length() + 1;
		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
	private byte[] BuildEndTestCallPacket()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data = "myMethod=EndTestCall";
		len = data.length() + 1;
		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
	private byte[] Buildi711KeepAlive()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data = "myMethod=KeepAlive||myID="+session();
		len = data.length() + 1;

		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}

	private byte[] Buildi711DeclineCallPacket()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		//myMethod=DeclineInboundCall||sessionID=||deviceID=||userAgent=
		data = "myMethod=DeclineInboundCall||sessionID="+session()+
				"||deviceID="+this.deviceID()+
				"||userAgent="+this.userAgent()+ 
				"||myUID=" + ID() + "||callType=125";
		len = data.length() + 1;

		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
	private byte[] Buildi711HangupPacket()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data = "myMethod=StopRelay||myID="+session();
		len = data.length() + 1;

		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	        	
	private byte[] Buildi711AnswerPacket()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data  = "myMethod=answerCall||fromBook="+callType()+"||myID="+session()+"||deviceID=";
		
		if (overrideToken() != null && overrideToken().length() > 0)
			data += overrideToken();
		else
			data += pushToken();

		len = data.length() + 1;

		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
	private byte[] Buildi711IgnorePacket()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data  = "myMethod=ignoreCall||fromBook="+callType()+"||myID="+session()+"||deviceID="+pushToken();

		len = data.length() + 1;

		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
	private byte[] BuildCallUpdatePacket()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data  = "myMethod=UpdateCall||myID=" + session();
		data += "||number=" + callNumber();
		data += "||callbacknumber=" + callbackNumber();
		data += "||instructions=" + instructions();
		data += "||userAgent=" + userAgent();
		data += "||callType=" + callType();
		data += "||myUID="+ID();
		data += "||deviceID=" + deviceID();
		data += "||myIP=" + ip();
		data += "||deviceCaps=" + deviceCaps();

		len = data.length() + 1;
		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
	private byte[] BuildStartCaptionsPacket()
	{
		byte[] packet;
		int i, len;
		String data;
		int 	index = 0;
		
		data = "myMethod=StartCaptions||myID=" + session();
		data += "||userAgent=" + userAgent();
		data += "||callType=" + callType();
		data += "||myUID=" + ID();
		data += "||deviceID=" + deviceID();
		data += "||myIP=" + ip();
		data += "||deviceCaps=" + deviceCaps();
		len = data.length() + 1;
		packet = new byte[len];
		
		for (i=0; i<data.length(); i++)
			packet[index++] = (byte)data.charAt(i); 
		packet[index++] = 0; 

		return packet;
	}
	
	private void acceptInboundCall()
	{
		byte[] packet = BuildInboundCallPacket();
		SendCommand(packet, true, false);
	}

	private void answerCall()
	{
		byte[] packet = Buildi711AnswerPacket();
		SendCommand(packet, true, false);
	}
	
	private void callActive()
	{
		byte[] packet = BuildCallActivePacket();
		SendCommand(packet, true, false);
	}
	
	private void callEnd()
	{
		byte[] packet = BuildCallEndPacket();
		SendCommand(packet, true, false);
	}
	
	private void cancelCallIn()
	{
		byte[] packet = Buildi711HangupPacket();
		SendCommand(packet, true, false);
	}

	private void ignoreCall()
	{
		byte[] packet = Buildi711IgnorePacket();
		SendCommand(packet, true, false);
	}

	private void makeCallIn()
	{
		byte[] packet = BuildEasyCallPacket();
		SendCommand(packet, true, false);
	}
	
	private void testCallIn()
	{
		byte[] packet = BuildTestCallPacket();
		SendCommand(packet, true, false);
	}
	
	public void registerCall()
	{
		byte[] packet = Buildi711RegistrationRequest();
		SendCommand(packet, true, false);
	}
	
	private void sipActive()
	{
		byte[]	packet = BuildSIPActivePacket();
		SendCommand(packet, true, false);
	}

	private void sipRestart()
	{
		byte[]	packet = BuildSIPRestartPacket();
		SendCommand(packet, true, false);
	}

	private void sipFailed()
	{
		byte[]	packet = BuildSIPFailedPacket();
		SendCommand(packet, true, false);
	}

	private void startCaptionsIn()
	{
		byte[]	packet = BuildStartCaptionsPacket();
		SendCommand(packet, true, false);
	}

	// send to server
	private void SendCommand(byte[] data, boolean wait, boolean justDoIt)
	{
		String outData = new String(data);
		if (webClient != null) {
			try {
				webClient.send(outData);
				CCLog.trace("ProtocolWizard.SendCommand() sent data length: "+outData.length());
			} 
			catch (Exception e) {
				CCLog.trace("ProtocolWizard.SendCommand(): "+e.getMessage());
				stopCall(e.getMessage());
				return;
			}
		}
		else {
			if (mOutputStream == null)
				return;
	
			mWriteInstance.SetOutputStream(mOutputStream);
	
			byte[] nullbyte = new byte[1];
			nullbyte[0] = 0;
	
			mWriteInstance.write(data);
			mWriteInstance.write(nullbyte);
		}
		CCLog.trace("ProtocolWizard.SendCommand(): ==> ["+outData.length()+"]");
	}

	private boolean isWebSocketClient()
	{
		return transportType() == CCTransportType.WEBSOCKET;
	}

	public String enterpriseID() {
		return enterprise;
	}

	public void enterpriseID(String enterprise) {
		this.enterprise = enterprise;
	}

	private CCTransportType transportType() {
		return transportType;
	}

	private void transportType(CCTransportType transportType) {
		this.transportType = transportType;
	}
	
	public void inCall(boolean b){
		inCall = b;
	}
		
	public boolean inCall(){
		return inCall;
	}

	public long retryInterval() {
		return retryInterval;
	}

	public void retryInterval(long retryInterval) {
		this.retryInterval = retryInterval;
	}

	private void startWANLossWarningTimer() {
		if(wanLossTimer == null) {
			wanLossTimer = new Timer("WANLossTimer");
		}
		TimerTask task = new WANLossWarning();

		wanLossTimer.schedule(task, 5000);
		//CCLog.trace("Started WAN loss warning timer." );
	}

	private void stopWANLossWarningTimer() {
		if(wanLossTimer != null) {
			wanLossTimer.cancel();
			wanLossTimer = null;
		}
		//CCLog.trace("Canceled wanLossTimer." );
	}

	class WANLossWarning extends TimerTask {
		public void run() {
			System.out.println("WAN loss warning ");
			CCLog.trace("WAN loss warning ");
			callback().processError(KEEP_ALIVE_TIMEOUT_CODE, KEEP_ALIVE_TIMEOUT_MSG);
		}
	}

}

class StreamIOThread extends Thread
{
    ProtocolWizard  mWizard;
    boolean run;

    public StreamIOThread(ProtocolWizard pw)
    {
        super();
        setPriority(Thread.MIN_PRIORITY);
        mWizard = pw;
        run = true;
    }  
    
    public void run()
    {
		try {
			while (!done()) {
				if ( mWizard.inputStream() == null ){
					CCLog.trace("ProtocolWizard.StreamIOThread.run() done");
			        quit();
					break;
				}
				
				mWizard.processData();
				
				try {
					sleep(10);
				} 
				catch (InterruptedException foo) {
				}                 
			}
		} 
		catch (Exception exc) {
		    CCLog.trace("ProtocolWizard.StreamIOThread.run() " + exc.getMessage());
		}
    }
    
    public void quit(){
    	run = false;
    }
    
    public boolean done() {
    	return !run;
    }
    
}
