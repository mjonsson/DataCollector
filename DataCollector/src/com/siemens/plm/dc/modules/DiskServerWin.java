package com.siemens.plm.dc.modules;

import com.siemens.plm.dc.Application;
import com.siemens.plm.dc.Module;
import com.siemens.plm.dc.Timer;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDef;
import org.rrd4j.graph.RrdGraphDef;

public class DiskServerWin extends Module
{
	private List<String> perfCounters = new ArrayList<String>();

	private Double diskReadPercent = Double.valueOf(0.0D);
	private Double diskWritePercent = Double.valueOf(0.0D);
	private Double diskReadTime = Double.valueOf(0.0D);
	private Double diskWriteTime = Double.valueOf(0.0D);
	private File rrdDisk;
	private File graphActivity;
	private File graphAccess;
	private String diskToMonitor;
	
	private static final Logger log = Logger.getLogger(DiskServerWin.class);

	protected final void rrdCreate() throws Exception
	{
		super.rrdCreate();

		if (!this.rrdDisk.exists())
		{
			RrdDef rrdDef = new RrdDef(this.rrdDisk.getAbsolutePath());
			rrdDef.addDatasource("diskreadpercent", DsType.GAUGE, this.sampleInterval * 2, 0.0D, 100.0D);
			rrdDef.addDatasource("diskwritepercent", DsType.GAUGE, this.sampleInterval * 2, 0.0D, 100.0D);
			rrdDef.addDatasource("diskreadtime", DsType.GAUGE, this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
			rrdDef.addDatasource("diskwritetime", DsType.GAUGE, this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
			super.rrdCreate(rrdDef);
		}
	}

	private final void getSamples() throws Exception
	{
		this.diskReadPercent = 0.0D;
		this.diskWritePercent = 0.0D;
		this.diskReadTime = 0.0D;
		this.diskWriteTime = 0.0D;

		String cmd = "typeperf";
		String args = "";
		for (String counter : this.perfCounters)
		{
			args += "\"\\\\" + this.server + counter + "\" ";
		}
		args += "-si " + Integer.toString(this.deltaSample) +
				" -sc " + Integer.toString(this.sampleInterval / this.deltaSample - 1);

		String output = runCommand(cmd, args, null, null);

		String[] splitOutput = output.split("\\n");
		Pattern p = Pattern.compile("^\"[0-9]+?");

		int lines = 0;
		for (String line : splitOutput)
		{
			Matcher m = p.matcher(line);

			if (m.find())
			{
				lines++;
				String[] cols = line.split(",");
				this.diskReadPercent = Double.valueOf(this.diskReadPercent.doubleValue() + Double.parseDouble(cols[1].replaceAll("\"", "")));
				this.diskWritePercent = Double.valueOf(this.diskWritePercent.doubleValue() + Double.parseDouble(cols[2].replaceAll("\"", "")));
				this.diskReadTime = Double.valueOf(this.diskReadTime.doubleValue() + Double.parseDouble(cols[3].replaceAll("\"", "")));
				this.diskWriteTime = Double.valueOf(this.diskWriteTime.doubleValue() + Double.parseDouble(cols[4].replaceAll("\"", "")));
			}
		}
		if (lines == 0)
		{
			this.diskReadPercent = Double.NaN;
			this.diskWritePercent = Double.NaN;
			this.diskReadTime = Double.NaN;
			this.diskWriteTime = Double.NaN;
			return;
		}
		this.diskReadPercent = Double.valueOf(this.diskReadPercent.doubleValue() / lines);
		this.diskWritePercent = Double.valueOf(this.diskWritePercent.doubleValue() / lines);
		this.diskReadTime = Double.valueOf(this.diskReadTime.doubleValue() / lines * 1000.0D);
		this.diskWriteTime = Double.valueOf(this.diskWriteTime.doubleValue() / lines * 1000.0D);
		
		// Wait until interval complete until adding samles
		sleepDelta();
	}

	private final void addSamples() throws Exception
	{
		super.addSamples(this.rrdDisk, this.diskReadPercent, this.diskWritePercent, this.diskReadTime, this.diskWriteTime);
	}

	protected final void buildGraph() throws Exception
	{
		super.buildGraph();

		RrdGraphDef graphDef = new RrdGraphDef();
		graphDef.setVerticalLabel("(%)");
		graphDef.datasource("diskreadpercentave", this.rrdDisk.getAbsolutePath(), "diskreadpercent", ConsolFun.AVERAGE);
		graphDef.datasource("diskwritepercentave", this.rrdDisk.getAbsolutePath(), "diskwritepercent", ConsolFun.AVERAGE);
		graphDef.datasource("diskreadpercentmax", this.rrdDisk.getAbsolutePath(), "diskreadpercent", ConsolFun.MAX);
		graphDef.datasource("diskwritepercentmax", this.rrdDisk.getAbsolutePath(), "diskwritepercent", ConsolFun.MAX);
		graphDef.line("diskreadpercentave", new Color(100, 255, 100), "Read Activity", 1.5F);
		graphDef.line("diskwritepercentave", new Color(255, 100, 100), "Write Activity", 1.5F);
		graphDef.setMinValue(0.0D);
		graphDef.setMaxValue(100.0D);
		graphDef.comment("\\l");
		graphDef.comment("\\l");
		graphDef.gprint("diskreadpercentave", ConsolFun.AVERAGE, "Read Activity Avg : %4.1f %%");
		graphDef.gprint("diskreadpercentmax", ConsolFun.MAX, "Read Activity Max : %4.1f %%\\l");
		graphDef.gprint("diskwritepercentave", ConsolFun.AVERAGE, "Write Activity Avg: %4.1f %%");
		graphDef.gprint("diskwritepercentmax", ConsolFun.MAX, "Write Activity Max: %4.1f %%\\l");
		super.buildGraph(this.server, this.graphActivity, graphDef);

		graphDef = new RrdGraphDef();
		graphDef.setVerticalLabel("(ms)");
		graphDef.datasource("diskreadtimeave", this.rrdDisk.getAbsolutePath(), "diskreadtime", ConsolFun.AVERAGE);
		graphDef.datasource("diskwritetimeave", this.rrdDisk.getAbsolutePath(), "diskwritetime", ConsolFun.AVERAGE);
		graphDef.datasource("diskreadtimemax", this.rrdDisk.getAbsolutePath(), "diskreadtime", ConsolFun.MAX);
		graphDef.datasource("diskwritetimemax", this.rrdDisk.getAbsolutePath(), "diskwritetime", ConsolFun.MAX);
		graphDef.line("diskreadtimeave", new Color(100, 255, 100), "Read Access Time", 1.5F);
		graphDef.line("diskwritetimeave", new Color(255, 100, 100), "Write Access Time", 1.5F);
		graphDef.setMinValue(0.0D);
		graphDef.comment("\\l");
		graphDef.comment("\\l");
		graphDef.gprint("diskreadtimeave", ConsolFun.AVERAGE, "Read Access Avg : %4.1f ms");
		graphDef.gprint("diskreadtimemax", ConsolFun.MAX, "Read Access Max : %4.1f ms\\l");
		graphDef.gprint("diskwritetimeave", ConsolFun.AVERAGE, "Write Access Avg: %4.1f ms");
		graphDef.gprint("diskwritetimemax", ConsolFun.MAX, "Write Access Max: %4.1f ms\\l");
		super.buildGraph(this.server, this.graphAccess, graphDef);
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

		this.diskToMonitor = ((String)this.settings.get("disktomonitor"));
		this.perfCounters.add("\\LogicalDisk(" + this.diskToMonitor + ":)\\% Disk Read Time");
		this.perfCounters.add("\\LogicalDisk(" + this.diskToMonitor + ":)\\% Disk Write Time");
		this.perfCounters.add("\\LogicalDisk(" + this.diskToMonitor + ":)\\Avg. Disk sec/Read");
		this.perfCounters.add("\\LogicalDisk(" + this.diskToMonitor + ":)\\Avg. Disk sec/Write");

		this.rrdDisk = new File(this.rrdFilePath + File.separator + this.id + "_disk.rrd");
		this.graphActivity = new File(this.graphFilePath + File.separator + this.id + "_activity");
		this.graphAccess = new File(this.graphFilePath + File.separator + this.id + "_access");

		Application.graphPrefixes.add(this.graphActivity.getName());
		Application.graphPrefixes.add(this.graphAccess.getName());

		return true;
	}

	public final void run()
	{
		super.run();
		try
		{
			while (true)
			{
				timer = Timer.start();
				rrdCreate();
				getSamples();
				addSamples();
				if (Thread.interrupted()) throw new InterruptedException();
				if (Application.rereadConfig) break;
			}
		}
		catch (Exception ex)
		{
			log.error(ex.getMessage());
			log.error(Application.stackTraceToString(ex));
		}
	}
}