package com.siemens.plm.dc;

public class Timer {
	public Timer()
	{
	}
	
	public static final long start()
	{		
		return System.currentTimeMillis();
	}
	
	public static final long stop(long start)
	{
		return System.currentTimeMillis() - start;
	}
}
