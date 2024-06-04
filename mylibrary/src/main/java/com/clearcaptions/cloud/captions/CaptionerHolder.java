package com.clearcaptions.cloud.captions;

import com.clearcaptions.cloud.sip.SIPHandler;
import com.clearcaptions.transport.CCEnvironment;
import com.clearcaptions.transport.CCLog;
import com.clearcaptions.transport.CaptionSessionState;
import com.clearcaptions.transport.protocol.ProtocolInterface;
import com.clearcaptions.transport.protocol.ProtocolWizard;

public class CaptionerHolder extends ProtocolWizard implements CaptionsInterface {


    private CaptionsInterface ci;
    private boolean bTest;
    private boolean inCall;
    private boolean callFailed;
    private SIPHandler sipHandler;

    private ProtocolInterface	mPI;

    public void callback(ProtocolInterface pi){
        mPI=pi;
    }
    public ProtocolInterface callback(){
        return mPI;
    }

    public CaptionerHolder() {
        super();
        callback(this);
        setTest(false);
        callStarted(false);
        callFailed(false);
        environment(CCEnvironment.LIVE);
    }

    private void callFailed(boolean b) {
        this.callFailed = b;
    }

    private void callStarted(boolean b) {
        this.inCall=b;
    }

    private void setTest(boolean bTest) {
        this.bTest=bTest;
    }

    @Override
    public void processState(CaptionSessionState state, String msg) {
        CaptionSessionState localState;
        ProtocolWizard protocolWizard = new ProtocolWizard();


        switch (state) {
            case STATE_REGISTERING:
              /*  localState = CaptionSessionState.STATE_REGISTERING;
                protocolWizard.registerCall();*/
                break;

            case STATE_CONNECTED:
                break;
            case STATE_QUEUED:
                break;
            case STATE_ONLINE:
                break;
            case STATE_ERROR:
                return;
            case STATE_CONNECT_FAILED:
                break;
            case STATE_COMMAND:
            case STATE_SESSION_SIP_ESTABLISHED:
                break;
            case STATE_SESSION_SIP_FAILED:
                break;
            case STATE_DATA:
            case STATE_DATA_MACRO:
            case STATE_DATA_CAPTION:
                break;
            case STATE_CALL_ENDED:
                break;
            case STATE_CALLING:
                return;
            default: break;
        }


    }


    @Override
    public void processError(String errorCode, String msg) {
       /* CCLog.debug("Captions.processError::", errorCode + " [" + msg + "]");
        ci.processError(errorCode, msg);
*/

    }
}
