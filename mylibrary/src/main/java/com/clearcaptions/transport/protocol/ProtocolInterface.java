package com.clearcaptions.transport.protocol;

import com.clearcaptions.transport.CaptionSessionState;

public interface ProtocolInterface {
	
	/*
	 * send information about the result of an attempted call setup
	 *
	public void processCall(int type, String name, String address, int action, int ct);
	*/
	
	/**
	 * sends both state and data associated with the current call/connection
	 */
	public void processState(CaptionSessionState state, String msg);

	/**
	 * sends both errorCode and errorMsg associated with an error
	 */
	public void processError(String errorCode, String errorMsg);
	
}
