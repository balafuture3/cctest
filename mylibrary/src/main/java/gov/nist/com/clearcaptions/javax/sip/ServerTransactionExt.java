package gov.nist.com.clearcaptions.javax.sip;

import com.clearcaptions.javax.sip.ServerTransaction;


public interface ServerTransactionExt {
	/**
	 * Return the canceled Invite transaction corresponding to an
	 * incoming CANCEL server transaction.
	 * 
	 * @return -- the canceled Invite transaction.
	 * 
	 */
	public ServerTransaction getCanceledInviteTransaction();
}
