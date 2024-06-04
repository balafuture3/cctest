
package com.clearcaptions.cloud.captions;

import com.clearcaptions.transport.CCEnvironment;
import com.clearcaptions.transport.CCTransportType;

public class CaptionsMain {

    public static void main(String[] args) throws Exception {

    //properities for voip

/*  CaptionerHolder captionerHolder = new CaptionerHolder();
        captionerHolder.environment(CCEnvironment.STAGING);
        captionerHolder.callNumber("3108626176");
        captionerHolder.deviceID("B34049ec410ec");
        captionerHolder.callbackNumber("3108626176");
        String userAgentString = "ClearCaptions:Blue_VOIP 4.9.0.006: BSP SP31.2 : Android 4.4.3: 3.1.1:SIPLib 3.12.0-2617-g61e38e370";
        captionerHolder.userAgent(userAgentString);
        captionerHolder.callType("371");
        captionerHolder.startCall();*//*

       // captionerHolder.netConnect();
     //captions for pstn
       */
        /*Captions captions = new Captions();
        captions.callbackNumber("9163670200");
        captions.callNumber("9163670200");
        captions.deviceID("B34049ec3e291");
        captions.callType("70");
        captions.userAgent("App Sample 0.9.123123");

        captions.environment(CCEnvironment.TEST);
        captions.startCall();*/

        Captions captions = new Captions();
        captions.environment(CCEnvironment.STAGING);
        captions.callNumber("9166774956");
        captions.deviceID("B34049ec410ec");
        captions.callbackNumber("9163670200");
        String userAgentString = "ClearCaptions:Blue_VOIP 4.9.0.006: BSP SP31.2 : Android 4.4.3: 3.1.1:SIPLib 3.12.0-2617-g61e38e370";
        captions.userAgent(userAgentString);
        captions.callType("71");
        captions.startCall();
    }
}

