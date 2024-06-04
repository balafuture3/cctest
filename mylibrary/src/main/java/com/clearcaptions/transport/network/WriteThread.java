package com.clearcaptions.transport.network;

import java.io.IOException;
import java.io.OutputStream;

import com.clearcaptions.transport.CaptionSessionState;
import com.clearcaptions.transport.CCLog;
import com.clearcaptions.transport.protocol.*;

public class WriteThread extends Thread implements Runnable
{
	OutputStream mOutputStream;
	ProtocolWizard  mWizard;
	byte[]	queue;
	byte[]	buffer;
	boolean run;

	public WriteThread(ProtocolWizard pw)
	{
		queue = null;
		buffer = null;
		mOutputStream = null;
		mWizard = pw;
		run = true;
	}

	public synchronized void SetOutputStream(OutputStream os)
	{
		mOutputStream = os;
	}

	public synchronized void write(byte[] data)
	{
		byte[] newData;
		int newSize = data.length;
		int start = 0;

		if (queue != null)
			newSize = queue.length+data.length;

		newData = new byte[newSize];

		if (queue != null)
		{
			for (int i=0; i<queue.length; i++)
				newData[i] = queue[i];
			start = queue.length;
		}

		for (int i=0; i<data.length; i++)
			newData[start+i] = data[i];

		queue = newData;
	}

	public synchronized void copyQueue()
	{
		buffer = queue;
		queue = null;
	}
	
	public synchronized int queueSize()
	{
		if (queue != null)
			return queue.length;
		else
			return 0;
	}
	
	public void send() throws IOException
	{
		try {
			mOutputStream.write(buffer);
			mOutputStream.flush();			
			CCLog.trace("ProtocolWizard.send("+getName()+") ==> [" + new String(buffer).length() + "]");
			buffer = null;
		}
		catch (IOException esp) {
			queue = null;
			buffer = null;
			mOutputStream = null;
			CCLog.trace("ProtocolWizard.send("+getName()+")send(): exception...");
			throw esp;
		}
	}
	
	public void run()
	{
		while (!done())
		{
			copyQueue();
			if (mOutputStream != null && buffer != null)
			{
				try {
					send();
				} 
				catch (IOException e) {
					CCLog.error("ProtocolWizard.WriteThread("+getName()+") run() : " + e.getMessage());
					mWizard.handleState(CaptionSessionState.STATE_CONNECTION_LOST, e.getMessage());
					return;
				}
			}
			else
			{
				if (done())
				{
					try {
						if ( mOutputStream != null )
							mOutputStream.flush();
					} 
					catch (Exception e) {
						CCLog.trace("ProtocolWizard.WriteThread("+getName()+")run(): flushing: "+e.getMessage());
					}
					mWizard.handleState(CaptionSessionState.STATE_CALL_ENDED, "");
					quit();
					return;
				}
				
				try {
					sleep(10); // don't hog the cpu
				} 
				catch (InterruptedException foo) {
					CCLog.trace("ProtocolWizard.WriteThread("+getName()+")run(): sleep interrupted: "+foo);
				}
			}
		}
		CCLog.trace("ProtocolWizard.WriteThread("+getName()+")run():  exit");
		
	}

	public boolean done(){
		return !run;
	}
	
	public void quit(){
		run = false;
	}
	
}
