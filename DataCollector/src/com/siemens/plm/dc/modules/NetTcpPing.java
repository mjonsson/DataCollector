package com.siemens.plm.dc.modules;

import com.siemens.plm.dc.Application;
import com.siemens.plm.dc.Module;

import java.awt.Color;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDef;
import org.rrd4j.graph.RrdGraphDef;

public class NetTcpPing extends Module
{
	private double pingTime;
	private File rrdPing;
	private File graphPing;
	
	private static final Logger log = Logger.getLogger(NetTcpPing.class);

	protected final void rrdCreate()
			throws Exception
			{
		super.rrdCreate();

		if (!this.rrdPing.exists())
		{
			RrdDef rrdDef = new RrdDef(this.rrdPing.getAbsolutePath());
			rrdDef.addDatasource("pingtime", DsType.GAUGE, this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
			super.rrdCreate(rrdDef);
		}
			}

	private final void getSamples() throws Exception
	{
		String cmd = Application.appPath + "lib" + File.separator + "tcping.exe";
		String args = "-i " + Integer.toString(this.deltaSample) +
				" -n " + Integer.toString(this.sampleInterval / this.deltaSample) +
				" " + this.server + " " + this.port;

		String output = runCommand(cmd, args, null, null);

		//System.out.println(output);
		
		Pattern p = Pattern.compile("successful, ([0-9]+?) failed.+?Average = (.+?)ms", 32);
		Matcher m = p.matcher(output);

		this.pingTime = Double.NaN;
		if (m.find())
		{
			if (Integer.parseInt(m.group(1)) > 0)
			{
				this.pingTime = Double.NaN;
			}
			else
			{
				this.pingTime = Double.parseDouble(m.group(2));
			}
		}
	}

	private void addSamples() throws Exception
	{
		super.addSamples(this.rrdPing, this.pingTime);
	}

	protected final void buildGraph() throws Exception
	{
		super.buildGraph();

		RrdGraphDef graphDef = new RrdGraphDef();
		graphDef.setVerticalLabel("(ms)");
		graphDef.datasource("pingtimeave", this.rrdPing.getAbsolutePath(), "pingtime", ConsolFun.AVERAGE);
		graphDef.datasource("pingtimemin", this.rrdPing.getAbsolutePath(), "pingtime", ConsolFun.MIN);
		graphDef.datasource("pingtimemax", this.rrdPing.getAbsolutePath(), "pingtime", ConsolFun.MAX);
		graphDef.line("pingtimeave", new Color(Color.DARK_GRAY.getRGB()), "Time Average", 1.5F);
		graphDef.setMinValue(0.0D);
		graphDef.comment("\\l");
		graphDef.comment("\\l");
		graphDef.gprint("pingtimeave", ConsolFun.AVERAGE, "Time Avg: %4.1f ms");
		graphDef.gprint("pingtimemin", ConsolFun.MIN, "Time Min: %4.1f ms");
		graphDef.gprint("pingtimemax", ConsolFun.MAX, "Time Max: %4.1f ms\\l");

		super.buildGraph(this.server + ":" + this.port, this.graphPing, graphDef);
	}

	public final boolean initialize() throws Exception
	{
		if (!super.initialize())
		{
			return false;
		}

		if (this.sampleInterval % this.deltaSample > 0)
		{
			throw new Exception("Sample interval must be evenly dividable by delta sample interval.");
		}
		this.rrdPing = new File(this.rrdFilePath + File.separator + this.id + "_ping.rrd");
		this.graphPing = new File(this.graphFilePath + File.separator + this.id + "_ping");

		Application.graphPrefixes.add(this.graphPing.getName());

		return true;
	}

	public final void run()
	{
		super.run();
		try {
			while (true) {
				timer.start();
				rrdCreate();
				getSamples();
				timer.stop();
				sleepDelta();
				addSamples();
				// if (Thread.interrupted()) throw new InterruptedException();
				// if (Application.rereadConfig) break;
			}
		} catch (InterruptedException ex) {
			log.warn("Thread \"" + thread.getName() + "\" was interrupted. Exiting...");
		} catch (Exception ex) {
			log.error(ex.getMessage());
			log.error(Application.stackTraceToString(ex));
		}
	}
}