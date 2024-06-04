/*
 * This file is part of TinySip. 
 * http://code.google.com/p/de-tiny-sip/
 * 
 * Created 2011 by Sebastian R�sch <flowfire@sebastianroesch.de>
 * 
 * This software is licensed under the Apache License 2.0.
 */

package com.clearcaptions.cloud.sip;

/**
 * Implement this interface to listen to events raised by the SipManager.
 * 
 * @author Sebastian
 * 
 */
public interface SipManagerStatusListener {
	/**
	 * Send a SipManagerStatusChanged event to all registered listeners.
	 * 
	 * @param event
	 *            the event to send
	 */
	public void SipManagerStatusChanged(SipManagerStatusChangedEvent event);

	/**
	 * Send a SipManagerCallStatusChanged event to all registered listeners.
	 * 
	 * @param event
	 *            the event to send
	 */
	public void SipManagerCallStatusChanged(SipManagerCallStatusEvent event);

	/**
	 * Send a SipManagerSessionChanged event to all registered listeners.
	 * 
	 * @param event
	 *            the event to send
	 */
	public void SipManagerSessionChanged(SipManagerSessionEvent event);
}
