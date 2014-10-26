package com.siemens.plm.dc;

public class Timer {
	private long start;
	private long stop;
	
	public Timer()
	{
	}
	
	public final void start()
	{
		start = System.currentTimeMillis();
		stop = start;
	}
	
	public final void stop()
	{
		stop = System.currentTimeMillis();
	}
	
	public final long delta()
	{
		if (stop == start) { stop(); }
		return stop - start;
	}
}
