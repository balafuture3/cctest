package com.clearcaptions.cloud.sip;

import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.clearcaptions.cloud.captions.Captions;
import com.clearcaptions.javawi.jstun.util.CallTypeUtil;
import com.clearcaptions.javax.sdp.SdpConstants;
import com.clearcaptions.javax.sdp.SdpException;
import com.clearcaptions.javax.sip.*;
import com.clearcaptions.transport.CCLog;
import com.clearcaptions.transport.CaptionSessionState;

public class SIPHandler implements SipManagerStatusListener{

	private static String TAG = "ClearCaptionsSIP";

	private Captions captioner;
	private LocalSipProfile localSipProfile;
	private SipContact sipContact;
	private SipManager sipManager;
	private SipManagerState state;
	private Timer timer;
	private boolean bSent;

	private int remoteRtcpPort;

	private int remoteRtpPort;

	private String remoteAddress;

	public SIPHandler(Captions cap)
	{
		captioner = cap;
    	initialize();
	}
	
	private void initialize()
	{
		bSent = false;
	}
	
	public SipContact sipContact() {
		return sipContact;
	}

	public void sipContact(SipContact sipContact) {
		this.sipContact = sipContact;
	}

	public SipManagerState sipState() {
		return state;
	}

	private void sipState(SipManagerState state) {
		this.state = state;
	}

	private LocalSipProfile sipProfile() {
		return localSipProfile;
	}

	private void sipProfile(LocalSipProfile localSipProfile) {
		this.localSipProfile = localSipProfile;
	}

	private String getHostname(String uri)
	{
		String[] pieces = uri.split("@");
		return pieces[1];
	}
	
	private String getUsername(String uri)
	{
		String[] pieces = uri.split("@");
		return pieces[0];
	}
	
	private int getPort(String uri)
	{
		String[] pieces = uri.split(":");
		if (pieces.length > 1) {
			return Integer.parseInt(pieces[1]);
		}
		else {
			return 5060;
		}
	}
	
    private void closeLocalProfile() {
    	try {
    		SipManager.removeStatusListener(this);
			sipManager.unregisterProfile();
		} 
    	catch (Exception e) {
		
    	}
    }

    /*
     * end call
     */
    public void endCall() {
		try {
			stopTimer();
			sipManager.endCall();
		} catch (SipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SdpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	closeLocalProfile();
		//sipManager.close();
    }
    
    /**
     * Make the outgoing call.
     */
    public void initiateCall(String uri) {
		CCLog.info(TAG, "INITIATE CALL: " + uri);

		sipContact(new SipContact(getUsername(uri), getHostname(uri), getPort(uri)));
		CCLog.info(TAG, "sipContact: " + sipContact().toString());
		CCLog.info(TAG, "userAgent: " + captioner.userAgent());

		startSipRegistration();
    }

    public void initializeLocalProfile() {
		InetAddress address = SipManager.getCurrentIP();
		sipProfile(new LocalSipProfile(captioner.deviceID(), address));

		// create a list of supported audio formats for the local user agent
		ArrayList<SipAudioFormat> audioFormats = new ArrayList<SipAudioFormat>();
		audioFormats.add(new SipAudioFormat(SdpConstants.PCMU, "PCMU", 8000));
		audioFormats.add(new SipAudioFormat(SdpConstants.GSM, "GSM", 8000));
		audioFormats.add(new SipAudioFormat(SdpConstants.PCMA, "PCMA", 8000));
		sipProfile().setAudioFormats(audioFormats);

		// set ports for rtp and rtcp for audio
		sipProfile().setLocalAudioRtpPort(5071);
		sipProfile().setLocalAudioRtcpPort(5072);

    }
    
    public void sendInvite()
    {
    	if (!bSent) {
			localSipProfile = sipManager.getLocalSipProfile();
			CCLog.info(TAG, "SIP READY: " + localSipProfile.toString());
			CCLog.info(TAG, "SIP INVITE:  " + sipContact.toString());
			try {
				sipManager.sendInvite(sipContact);
				bSent = true;
			} catch (Exception e) {
				CCLog.error(TAG, "SIP REGISTER: " + e.getMessage());
			}
    	}
    }



	private void startSipRegistration() 
	{
		CCLog.debug(TAG, "startSipRegistration()");
		sipManager = SipManager.createInstance(sipProfile(), sipProfile().getSipDomain(), sipProfile().getLocalSipPort());
		CCLog.debug(TAG, "startSipRegistration() sipManager.createInstance()");
		SipManager.addStatusListener(this);
		CCLog.debug(TAG, "startSipRegistration() sipManager.addStatusListener()");

		try {
			CCLog.debug(TAG, "startSipRegistration() before sipManager.registerProfile()");
			sipManager.setUserAgent(captioner.userAgent());
			sipManager.registerProfile();
		} catch (Exception e) {
			CCLog.error(TAG, "SIP REGISTER ERROR: " + e.getMessage());
		}
	}
	
	//@Override
	public void SipManagerStatusChanged(SipManagerStatusChangedEvent event) {
		sipState(event.getState());

		captioner.recordEvent(sipState().toString(), event.getInfo());
		switch (sipState()) {
			case IDLE:
				CCLog.debug(TAG, "SIP IDLE");
				break;
			case RINGING:
				break;
			case ESTABLISHED:
				stopTimer();
				break;
			case INCOMING:
				break;
			case ERROR:
				CCLog.debug(TAG, "SIP ERROR");
				captioner.logEvent("||CODE=EP2020||MSG="+event.getInfo(), true);
				captioner.processState(CaptionSessionState.STATE_SESSION_SIP_FAILED, "");
				break;
			case UNREGISTERING:
				CCLog.debug(TAG, "SIP UNREGISTER");
				break;
			case READY:
				captioner.processState(CaptionSessionState.STATE_OFFHOOK, "");
				sendInvite();
				startTimer();
				break;
			case REGISTERING:
				CCLog.debug(TAG, "SIP REGISTER");
				break;
			case BYE:
				CCLog.debug(TAG, "SIP BYE");
				captioner.processState(CaptionSessionState.STATE_CALL_ENDED, "");
				break;
			case TIMEOUT:
			case INVALID:
				// log error...
				captioner.logEvent("||CODE=EP2020||MSG="+event.getInfo(), true);
				// retry the call...
				//sipHandler.sendInvite();
				captioner.processState(CaptionSessionState.STATE_SESSION_SIP_FAILED, "");
				captioner.processError("EP2020", "SIP invite response timed out");
				break;
			case CANCELED:
				CCLog.debug(TAG, "SIP INVITE CANCELED");
				captioner.processState(CaptionSessionState.STATE_CANCEL_CALL, "");
				captioner.processError("EP2020", "SIP invite canceled");
				break;
			default:
				break;
		}
	}

	class InviteTimeoutTask extends TimerTask {
		public void run() {
			captioner.logEvent("||CODE=EP2020||MSG=SIP invite response timed out", true);
			//captioner.processState(CaptionSessionState.STATE_SESSION_SIP_FAILED, "");
			sipManager.setStatusChanged(SipManagerState.TIMEOUT);
		}
	}
	
	private void startTimer() {
		int seconds = 20;
		timer = new Timer();
		timer.schedule(new InviteTimeoutTask(), seconds * 1000);
	}
	
	private void stopTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	//@Override
	public void SipManagerCallStatusChanged(SipManagerCallStatusEvent event) {
		CCLog.debug(TAG, "SipManagerCallStatusChanged: " + event.getMessage());
		captioner.recordEvent("SipManagerCallStatusChanged", event.getMessage());
	}

	//@Override
	public void SipManagerSessionChanged(SipManagerSessionEvent event) {
		SipSession session = event.getSession();

		if (session != null) {
			/*
			 *  set remote address and port
			 */
			//data.remoteRTP(session.getToSipURI().toString(), session.getRemoteAddress(), session.getRemoteAudioRtpPort());
			
			/*
			 * send sip address and port back to caption interface..
			 */
			remoteAddress(session.getRemoteAddress().getHostAddress());
			remoteRtpPort(session.getRemoteAudioRtpPort());
			remoteRtcpPort(session.getRemoteAudioRtcpPort());
			CCLog.info(TAG, "SIP CONNECTED: " + session.getToSipURI() + "; address: " + remoteAddress() + "; rtp port: " + remoteRtpPort() + "; rtcp port: " + remoteRtcpPort());
			captioner.processState(CaptionSessionState.STATE_SESSION_SIP_ESTABLISHED, "");
		}
	}

	private void remoteRtcpPort(int remoteAudioRtcpPort) {
		remoteRtcpPort = remoteAudioRtcpPort;
	}
	
	public int remoteRtcpPort() {
		return (remoteRtcpPort == 0) ? remoteRtpPort() + 1 : remoteRtcpPort;
	}

	private void remoteRtpPort(int remoteAudioRtpPort) {
		remoteRtpPort = remoteAudioRtpPort;
	}

	public int remoteRtpPort() {
		return remoteRtpPort;
	}

	private void remoteAddress(String hostAddress) {
		remoteAddress = hostAddress;
	}
	
	public String remoteAddress() {
		return remoteAddress;
	}

}
