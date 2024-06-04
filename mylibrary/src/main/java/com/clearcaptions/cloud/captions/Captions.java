package com.clearcaptions.cloud.captions;

import com.clearcaptions.cloud.sip.SIPHandler;
import com.clearcaptions.transport.CCEnvironment;
import com.clearcaptions.transport.CCLog;
import com.clearcaptions.transport.CaptionSessionState;
import com.clearcaptions.transport.protocol.ProtocolWizard;

public class Captions extends ProtocolWizard implements CaptionsInterface
{
	private static volatile Captions INSTANCE;
	private static String ERROR = "Whoops, something went wrong. Tap \"Captions\" to resume service.";
	private static boolean ISVOIP = false;
	
	private CaptionsInterface ci;
	private boolean bTest;
	private boolean inCall;
	private boolean callFailed;
	private SIPHandler sipHandler;

	public Captions()
	{
		super();
		callback(this);
		setTest(false);
		callStarted(false);
		callFailed(false);
		environment(CCEnvironment.LIVE);
	}
	
	public static Captions getInstance()
	{
		Captions instance = INSTANCE;
        if (instance == null)
		{
			synchronized (Captions.class) {
				instance = INSTANCE;
				if (instance == null)
				{
					INSTANCE = instance = new Captions();
				}
			}
		}
		return instance;
    }

	public static void setIsVoip(boolean isVoip)
	{
		ISVOIP = isVoip;
	}
	
	public void recordEvent(String state, String msg)
	{
		super.logEvent(state + ":" + msg);
		/*
		// Record when the user completes level 1
		 
		// Get the event client from insights instance
		EventClient eventClient = insightsInstance.getEventClient();
		 
		// Create a level completion event with a weaponUsed attribute.
		Event level1Event = eventClient.createEvent(thEvent)
		                               .withAttribute(key, value);
		 
		// Record the level completion event.
		eventClient.recordEvent(level1Event);
		//*/
	}

	public void endCall()
	{
		if (sipHandler != null) {
			sipHandler.endCall();
			sipHandler = null;
		}
		super.endCall();
		callStarted(false);
	}
	
	private void doCall(boolean test)
	{
		if (callStarted()) {
			return;
		}

		callStarted(true);

		// if we still have a sip handler, it means a previous call did not end well...
		if (sipHandler != null) {
			sipHandler.endCall();
			sipHandler = null;
		}

		if (test) {
			super.testCall();
		}
		else {
			super.startCall();
		}
	}
	
	public void startCall()
	{
		setTest(false);
		callFailed(false);
		doCall(false);
	}
	
	public void testCall()
	{
	    setTest(true);
	    doCall(true);
	}
	
	//@Override
	public void processState(CaptionSessionState state, String msg) {
		switch (state) {
			case STATE_OFFHOOK:
				break;
				
			/*
			 * make the sip calls, start the proxy thread
			 * 	this info does not need to be passed along
			 */
			case STATE_SIPREMOTE:
				CCLog.debug("Captions.processState::", state + "[" + msg + "]");
				if(!ISVOIP)
				{
					CCLog.debug("Captions.processState::", "Not a VoIP call, starting SIP connection.");
					if (callStarted()) {
						if (sipHandler == null) {
							recordEvent(""+state, msg);
							sipHandler = new SIPHandler(this);
						}
						sipHandler.initializeLocalProfile();
						sipHandler.initiateCall(msg);
					}
				}
				break;
			
			case STATE_SIPLOCAL:
				break;
			
			case STATE_SESSION_SIP_ESTABLISHED:
    			CCLog.debug("Captions.processState", ""+state);
				recordEvent(""+state, msg);
				ci.processState(state, msg);
        		if (isTest()) {
        			super.endTestCall();
        		}
        		else {
        			ci.processState(CaptionSessionState.STATE_ONLINE, opID());
        		}
				break;
			case STATE_SESSION_SIP_FAILED:
    			CCLog.debug("Captions.processState", ""+state);
    			callFailed(true);
				recordEvent(""+state, msg);
				ci.processState(state, ERROR);
        		if (isTest()) {
        			super.endTestCall();
        		}
        		else {
        			endCall();
        		}
				break;
				
			case STATE_CALL_ENDED:
				endCall();
				if (callFailed()) {
					return;
				}
			case STATE_CANCEL_CALL:
			case STATE_CONNECT_FAILED:
			case STATE_CONNECTION_LOST:
			case STATE_DEVICE_FAILED:
			case STATE_ERROR:
			case STATE_REGISTER_FAILED:
				callStarted(false);
				
			default:
				ci.processState(state, msg);

		}
	}

	public void processError(String errorCode, String msg) {

		CCLog.debug("Captions.processError::", errorCode + " [" + msg + "]");
		ci.processError(errorCode, msg);

	}

	public void callback(CaptionsInterface ci) {
		super.callback(this);
		this.ci = ci;
	}

	public boolean isTest() {
		return bTest;
	}

	public void setTest(boolean bTest) {
		this.bTest = bTest;
	}

	private boolean callStarted() {
		return inCall;
	}

	private void callStarted(boolean inCall) {
		this.inCall = inCall;
	}
	
	public String remoteRtpAddress() {
		return sipHandler.remoteAddress();
	}

	public int remoteRtpPort() {
		return sipHandler.remoteRtpPort();
	}

	public int remoteRtcpPort() {
		return sipHandler.remoteRtcpPort();
	}

	public void userAgent(String userAgent) {
		super.userAgent(userAgent + " CC/"+this.appVersion()+" ");
	}

	public boolean callFailed() {
		return callFailed;
	}

	public void callFailed(boolean callFailed) {
		this.callFailed = callFailed;
	}
	
}

