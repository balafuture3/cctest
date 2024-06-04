package com.clearcaptions.transport.network;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import com.clearcaptions.transport.CaptionSessionState;
import com.clearcaptions.transport.CCLog;
import com.clearcaptions.transport.protocol.*;

public class ReadThread extends Thread implements Runnable
{
	InputStream mInputStream;
	ProtocolWizard  mWizard;
	boolean run;
	ArrayList<String> packets;

	public ReadThread(ProtocolWizard pw)
	{
		packets = new ArrayList<String>();
		mInputStream = null;
		mWizard = pw;
		run = true;
	}

	public void run()
	{
		int newSize = 1024;
		try {
			while (!done()) {
				byte[] buffer = new byte[newSize];
				int count = mInputStream.read(buffer, 0, newSize);
				if (count >= 0) {
					insert(buffer, count);
				}
				else {
					if (mInputStream != null) {
						CCLog.error("ProtocolWizard.ReadThread("+getName()+") run() : " + buffer);
						mWizard.handleState(CaptionSessionState.STATE_CONNECTION_LOST, "");
					}
				}
				buffer = null;
			}
		}
		/*
		catch (InterruptedException foo) {
			CCLog.trace("ProtocolWizard.WriteThread("+getName()+")run(): sleep interrupted: "+ foo.getMessage());
		}
		*/
		catch (Exception exc) {
			CCLog.error("ProtocolWizard.ReadThread("+getName()+") run(): " + exc.getMessage());
			if (mInputStream != null) {
				mWizard.handleState(CaptionSessionState.STATE_CONNECTION_LOST, "");
			}
		}

		mInputStream = null;
		CCLog.trace("ProtocolWizard.ReadThread("+getName()+") done");
	}
	
	public boolean done(){
		return ((mInputStream == null) || (run==false));
	}
	
	public void quit(){
		run = false;
	}
	
	public synchronized void insert(byte[] buffer, int size)
	{
		try {
			String data = new String(buffer, "UTF-8").trim();
			String[] pieces = data.split("\0");
			for (int i=0;i<pieces.length;i++) {
				if (pieces[i].length() > 0) {
					packets.add(pieces[i]);
				}
			}
		} 
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void SetInputStream(InputStream is)
	{
		if (is == null && mInputStream != null) {
			try {
				if (mWizard.writeQueueSize() == 0)
				{
					mInputStream.close();
				}
			}
			catch (Exception e) {
			}
		}
		mInputStream = is;
	}
	
	public synchronized int available()
	{
		return packets.size();
	}

	public synchronized String read()
	{
		return packets.remove(0);
		/*
		byte[] newBuffer;
		int    newSize;

		for (int i=0; i<dest.length; i++)
			dest[i] = buffer[i];

		newSize = buffer.length-dest.length;
		newBuffer = new byte[newSize];

		for (int i=0; i<newSize; i++)
			newBuffer[i] = buffer[dest.length+i];

		buffer = newBuffer;

		return dest.length;
		*/
	}
	
}
