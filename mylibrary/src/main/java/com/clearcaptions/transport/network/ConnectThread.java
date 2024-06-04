
package com.clearcaptions.transport.network;

import com.clearcaptions.transport.CaptionSessionState;
import com.clearcaptions.transport.CCLog;
import com.clearcaptions.transport.protocol.*;

public class ConnectThread extends Thread implements Runnable
{
	public ConnectThread(ProtocolWizard pw, String h, int p)
	{
		mWizard = pw;
		host = h;
		port = p;
	}

	public void run()
	{
		CCLog.trace("ConnectThread : (" + getName() + ") Constructed ["+host+":"+port+"]");
		try {
			mWizard.netConnect();
		} 
		catch (Exception ex) {
			CCLog.error("ConnectThread: ("+getName()+") Failure: " + ex.toString());
			mWizard.handleState(CaptionSessionState.STATE_CONNECT_FAILED, ex.getMessage());
		}

		CCLog.trace("ConnectThread: ("+getName()+") Finished");
	}
	
	ProtocolWizard  mWizard;
	String host;
	int    port;
}
