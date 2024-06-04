package com.clearcaptions.transport;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.*;

public class CCLog {
	
	private static Logger logger;
	
	CCLog()
	{
		logger = Logger.getLogger("CCTransport");
	}
	
	public static void trace(Object msg)
	{
		System.out.println(getDateString() + " TRACE [" + Thread.currentThread().getId() + "] " + msg);
		if (logger != null)
			logger.trace(msg);
	}
	
	public static void info(String TAG, Object msg)
	{
		info(TAG + ": " + msg);
	}
	
	public static void info(Object msg)
	{
		System.out.println(getDateString() + " INFO [" + Thread.currentThread().getId() + "] " + msg);
		if (logger != null)
			logger.info(msg);
	}

	public static void debug(String TAG, Object msg)
	{
		debug(TAG + ": " + msg);
	}
	
	public static void debug(Object msg)
	{
		System.out.println(getDateString() + " DEBUG [" + Thread.currentThread().getId() + "] " + msg);
		if (logger != null)
			logger.debug(msg);
	}

	public static void error(String TAG, Object msg)
	{
		error(TAG + ": " + msg);
	}
	
	public static void error(Object msg)
	{
		System.out.println(getDateString() + " ERROR [" + Thread.currentThread().getId() + "] " + msg);
		if (logger != null)
			logger.error(msg);
	}
	
	private static String getDateString()
	{
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat formatter2 = new SimpleDateFormat("hh:mm:ss,SSS");
		Date currentTime_1 = new Date(System.currentTimeMillis());
		String dateString = formatter.format(currentTime_1);
		String dateString2 = formatter2.format(currentTime_1);
		return dateString + " " + dateString2;
	}
}
