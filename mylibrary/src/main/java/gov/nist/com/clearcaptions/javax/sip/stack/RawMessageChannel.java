package gov.nist.com.clearcaptions.javax.sip.stack;

import gov.nist.com.clearcaptions.javax.sip.message.SIPMessage;

public interface RawMessageChannel {
	
	public abstract void processMessage(SIPMessage sipMessage) throws Exception ;

}
