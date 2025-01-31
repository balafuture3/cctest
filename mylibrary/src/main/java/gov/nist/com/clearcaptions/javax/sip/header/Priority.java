/*
* Conditions Of Use 
* 
* This software was developed by employees of the National Institute of
* Standards and Technology (NIST), an agency of the Federal Government.
* Pursuant to title 15 Untied States Code Section 105, works of NIST
* employees are not subject to copyright protection in the United States
* and are considered to be in the public domain.  As a result, a formal
* license is not needed to use the software.
* 
* This software is provided by NIST as a service and is expressly
* provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
* OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
* AND DATA ACCURACY.  NIST does not warrant or make any representations
* regarding the use of the software or the results thereof, including but
* not limited to the correctness, accuracy, reliability or usefulness of
* the software.
* 
* Permission to use this software is contingent upon your acceptance
* of the terms of this agreement
*  
* .
* 
*/
/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.com.clearcaptions.javax.sip.header;

import com.clearcaptions.javax.sip.header.*;

import java.text.ParseException;

/**
 * the Priority header. 
 *
 * @author Olivier Deruelle <br/>
 * @version 1.2 $Revision: 1.5 $ $Date: 2006/11/16 16:17:10 $
 *
 * 
 *
 */
public class Priority extends SIPHeader implements PriorityHeader {

	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3837543366074322106L;

	/** constant EMERGENCY field
	*/
	public static final String EMERGENCY = ParameterNames.EMERGENCY;

	/** constant URGENT field
	 */
	public static final String URGENT = ParameterNames.URGENT;

	/** constant NORMAL field
	 */
	public static final String NORMAL = ParameterNames.NORMAL;

	/** constant NON_URGENT field
	 */
	public static final String NON_URGENT = ParameterNames.NON_URGENT;
	/** priority field
	 */
	protected String priority;

	/** Default constructor
	 */
	public Priority() {
		super(NAME);
	}

	/**
	 * Encode into canonical form.
	 * @return String
	 */
	public String encodeBody() {
		return priority;
	}

	/**
	 * get the priority value.
	 * @return String
	 */
	public String getPriority() {
		return priority;
	}

	/**
	 * Set the priority member
	 * @param p String to set
	 */
	public void setPriority(String p) throws ParseException {
		if (p == null)
			throw new NullPointerException(
				"JAIN-SIP Exception,"
					+ "Priority, setPriority(), the priority parameter is null");
		priority = p;
	}
}
