package com.siemens.plm.dc.modules;

import com.siemens.plm.dc.Application;
import com.siemens.plm.dc.Module;

import java.awt.Color;
import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDef;
import org.rrd4j.graph.RrdGraphDef;

public class TcEnterpriseNet extends Module
{
	private double processMax = 0.0D;
	private double processWarm = 0.0D;
	private double processTarget = 0.0D;
	private double assignedServers = 0.0D;

	private double warmServers = 0.0D;
	private String tcPoolName;
	private File rrdTcEnterprise;
	private File graphTcEnterprise;
	
	private static final Logger log = Logger.getLogger(TcEnterpriseNet.class);

	protected final void rrdCreate()
			throws Exception
			{
		super.rrdCreate();

		if (!this.rrdTcEnterprise.exists())
		{
			RrdDef rrdDef = new RrdDef(this.rrdTcEnterprise.getAbsolutePath());
			rrdDef.addDatasource("processMax", DsType.GAUGE, this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
			rrdDef.addDatasource("processWarm", DsType.GAUGE, this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
			rrdDef.addDatasource("processTarget", DsType.GAUGE, this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
			rrdDef.addDatasource("assignedServers", DsType.GAUGE, this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
			rrdDef.addDatasource("warmServers", DsType.GAUGE, this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
			rrdDef.addDatasource("coldServers", DsType.GAUGE, this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
			rrdDef.addDatasource("startingServers", DsType.GAUGE, this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
			super.rrdCreate(rrdDef);
		}
			}

	private final void getSamples() throws Exception
	{
		this.processMax = Double.NaN;
		this.processWarm = Double.NaN;
		this.processTarget = Double.NaN;
		this.assignedServers = Double.NaN;
		this.warmServers = Double.NaN;

		String output = getUrlContent("");

		Pattern p = Pattern.compile(
				"Server Manager ID.+?>[0-9]+?@(.+?)</span>.+?Available Servers.+?>([0-9]+?)</span>.+?Busy Level.+?>([0-9]+?) %</span>.+?Pool ID.+?span id=.+?>(.+?)</span>.+?Process Warm.+?value=\"([0-9]+?)\".+?Process Max.+?value=\"([0-9]+?)\".+?Process Target.+?value=\"(.+?)\"", 
				32);

		Matcher m = p.matcher(output);

		double busyLevel = 0.0D;
		if (m.find())
		{
			this.server = m.group(1);
			this.warmServers = Double.parseDouble(m.group(2));
			busyLevel = Double.parseDouble(m.group(3));
			this.tcPoolName = m.group(4);
			this.processWarm = Double.parseDouble(m.group(5));
			this.processMax = Double.parseDouble(m.group(6));
			this.processTarget = findProcessTarget(m.group(7));
			this.assignedServers = (this.processMax * busyLevel / 100.0D);
		}
	}

	private double findProcessTarget(String processTarget)
	{
		String[] processTargets = processTarget.split(",");
		Format formatter = new SimpleDateFormat("HHmm");
		int currentTime = Integer.parseInt(formatter.format(new Date()));

		int lastTimeDiff = 2400;
		int targetToUse = 0;
		for (String targetPair : processTargets)
		{
			targetPair = targetPair.trim();
			String[] target = targetPair.split(" ");
			int targetTime = Integer.parseInt(target[0]);
			int timeDiff = currentTime - targetTime;
			if ((timeDiff < lastTimeDiff) && (timeDiff >= 0))
			{
				lastTimeDiff = timeDiff;
				targetToUse = Integer.parseInt(target[1]);
			}
		}

		return targetToUse;
	}

	private final void addSamples() throws Exception
	{
		super.addSamples(this.rrdTcEnterprise, this.processMax, this.processWarm, this.processTarget, this.assignedServers, this.warmServers, (0.0D / 0.0D), (0.0D / 0.0D));
	}

	protected final void buildGraph() throws Exception
	{
		super.buildGraph();

		RrdGraphDef graphDef = new RrdGraphDef();
		graphDef.setVerticalLabel("(#)");
		graphDef.datasource("assignedServersave", this.rrdTcEnterprise.getAbsolutePath(), "assignedServers", ConsolFun.AVERAGE);
		graphDef.datasource("assignedServersmax", this.rrdTcEnterprise.getAbsolutePath(), "assignedServers", ConsolFun.MAX);
		graphDef.datasource("warmServersave", this.rrdTcEnterprise.getAbsolutePath(), "warmServers", ConsolFun.AVERAGE);
		graphDef.datasource("processTargetave", this.rrdTcEnterprise.getAbsolutePath(), "processTarget", ConsolFun.AVERAGE);
		graphDef.datasource("processWarmave", this.rrdTcEnterprise.getAbsolutePath(), "processWarm", ConsolFun.AVERAGE);
		graphDef.datasource("processMaxave", this.rrdTcEnterprise.getAbsolutePath(), "processMax", ConsolFun.AVERAGE);
		graphDef.datasource("processTargetWarmave", "processTargetave,processWarmave,+");
		graphDef.area("assignedServersave", new Color(100, 255, 100), "Assigned Servers");
		graphDef.stack("warmServersave", new Color(255, 255, 100), "Warm Servers");
		graphDef.line("processTargetave", new Color(0, 0, 0), "Process Target", 1.0F);
		graphDef.line("processTargetWarmave", new Color(0, 0, 0), "Process Target+Warm", 1.0F);
		graphDef.line("processMaxave", new Color(0, 0, 0), "Process Max", 1.5F);
		graphDef.setMinValue(0.0D);
		graphDef.comment("\\l");
		graphDef.comment("\\l");
		graphDef.gprint("assignedServersave", ConsolFun.AVERAGE, "Assigned Servers    : %3.0f");
		graphDef.gprint("assignedServersmax", ConsolFun.MAX, "Assigned Servers Max: %3.0f\\l");
		graphDef.comment("\\l");
		graphDef.gprint("processMaxave", ConsolFun.AVERAGE, "Process Max   : %3.0f");
		graphDef.gprint("processTargetave", ConsolFun.AVERAGE, "Process Target: %3.0f");
		graphDef.gprint("processWarmave", ConsolFun.AVERAGE, "Process Warm  : %3.0f\\l");
		super.buildGraph(this.url.replaceFirst("https*://", "").replaceFirst(":.+$", "") + ":" + this.tcPoolName, this.graphTcEnterprise, graphDef);
	}

	public final boolean initialize() throws Exception
	{
		if (!super.initialize())
		{
			return false;
		}

		this.rrdTcEnterprise = new File(this.rrdFilePath + File.separator + this.id + "_tcent_net.rrd");
		this.graphTcEnterprise = new File(this.graphFilePath + File.separator + this.id + "_tcent_net");
		Application.graphPrefixes.add(this.graphTcEnterprise.getName());

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