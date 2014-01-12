package com.siemens.plm.dc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamReader
implements Runnable
{
	private InputStream is = null;
	private String data = "";
	private BufferedReader br = null;

	public StreamReader(InputStream is) {
		this.is = is;
	}

	public final String getData()
	{
		return this.data;
	}
	
	public final BufferedReader getBufferedReader() {
		return this.br;
	}

	public final Thread start() {
		Thread thread = new Thread(this, "StreamReader");
		thread.setDaemon(true);
		thread.start();

		return thread;
	}

	public final void run() {
		synchronized (this) {
			InputStreamReader isr = null;
			try
			{
				isr = new InputStreamReader(this.is);
				br = new BufferedReader(isr);

				String s;
				while ((s = br.readLine()) != null)
				{
					this.data = (this.data + s + "\n");
				}
			}
			catch (Exception ex) { }
			finally
			{
				try
				{
					br.close();
					isr.close();
					this.is.close();
				}
				catch (Exception localException2)
				{
				}
			}
		}
	}

}