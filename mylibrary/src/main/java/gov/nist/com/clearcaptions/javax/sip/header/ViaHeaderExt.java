/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package gov.nist.com.clearcaptions.javax.sip.header;


/**
 * @author jean.deruelle@gmail.com
 *
 */
public interface ViaHeaderExt {
	/**
	 * Returns hostname:port as a string equivalent to the "sent-by" field 
	 * @return "sent-by" field
	 * @since 2.0
	 */
	public String getSentByField();
	/**
	 * Returns transport to the "sent-protocol" field 
	 * @return "sent-protocol" field
	 * @since 2.0
	 */
	public String getSentProtocolField();
}
