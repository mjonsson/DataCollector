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

public class TcEnterpriseJ2EE extends Module
{
	private double processMax = 0.0D;
	private double processWarm = 0.0D;
	private double processTarget = 0.0D;
	private double assignedServers = 0.0D;
	private double coldServers = 0.0D;
	private double warmServers = 0.0D;
	private double startingServers = 0.0D;
	private String tcPoolName;
	private File rrdTcEnterprise;
	private File graphTcEnterprise;
	
	private static final Logger log = Logger.getLogger(TcEnterpriseJ2EE.class);

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

	private final void getSamples()
			throws Exception
			{
		this.processMax = Double.NaN;
		this.processWarm = Double.NaN;
		this.processTarget = Double.NaN;
		this.assignedServers = Double.NaN;
		this.warmServers = Double.NaN;
		this.coldServers = Double.NaN;
		this.startingServers = Double.NaN;

		String output = getUrlContent("");

		Pattern p = Pattern.compile(
				"<LI><STRONG>.+? manager</STRONG>\n<UL type=disc>\n<LI><A HREF=\"(.+?)\">id=.+?</A>", 
				32);

		Matcher m = p.matcher(output);
		String queryString;
		if (m.find())
		{
			queryString = m.group(1);
		}
		else
			return;

		output = getUrlContent(queryString);

		p = Pattern.compile(
				"<TD><A HREF=\"javascript:alert\\('Name of the host this server pool is running on'\\);\"><B>Host</B></A></TD>\n<TD ALIGN=RIGHT>java\\.lang\\.String</TD>\n<TD ALIGN=center> RO </TD>\n<TD>(.+?)</TD>\n</TR>\n<TR>\n<TD><A HREF=\"javascript:alert\\('The number of assigned server processes currently in this server pool'\\);\"><B>Number of Assigned Servers</B></A></TD>\n<TD ALIGN=RIGHT>java\\.lang\\.Integer</TD>\n<TD ALIGN=center> RO </TD>\n<TD>([0-9]+?)</TD>\n</TR>\n<TR>\n<TD><A HREF=\"javascript:alert\\('The total number of server processes that were launched but never reported back in time'\\);\"><B>Number of Cold Servers</B></A></TD>\n<TD ALIGN=RIGHT>java\\.lang\\.Integer</TD>\n<TD ALIGN=center> RO </TD>\n<TD>([0-9]+?)</TD>\n</TR>\n<TR>\n<TD><A HREF=\"javascript:alert\\('The total number of server processes currently in this server pool'\\);\"><B>Number of Servers</B></A></TD>\n<TD ALIGN=RIGHT>java\\.lang\\.Integer</TD>\n<TD ALIGN=center> RO </TD>\n<TD>([0-9]+?)</TD>\n</TR>\n<TR>\n.*?<TD><A HREF=\"javascript:alert\\('The number of to-be-assigned server processes currently in this server pool'\\);\"><B>Number of Warm Servers</B></A></TD>\n<TD ALIGN=RIGHT>java\\.lang\\.Integer</TD>\n<TD ALIGN=center> RO </TD>\n<TD>([0-9]+?)</TD>\n</TR>\n<TR>\n<TD><A HREF=\"javascript:alert\\('The number of server processes that are currently initializing in this server pool'\\);\"><B>Number of Warming Up Servers</B></A></TD>\n<TD ALIGN=RIGHT>java\\.lang\\.Integer</TD>\n<TD ALIGN=center> RO </TD>\n<TD>([0-9]+?)</TD>\n</TR>\n<TR>\n<TD><A HREF=\"javascript:alert\\('The unique identifier of this server pool'\\);\"><B>Pool ID</B></A></TD>\n<TD ALIGN=RIGHT>java\\.lang\\.String</TD>\n<TD ALIGN=center> RO </TD>\n<TD>(.+?)</TD>\n</TR>\n<TR>\n<TD><A HREF=\"javascript:alert\\('Configuration parameters for this server pool.'\\);\"><B>Pool-Specific_Configuration</B></A></TD>\n<TD ALIGN=RIGHT>java\\.lang\\.String\\[\\]</TD>\n<TD ALIGN=center> RO </TD>\n<TD> <A HREF=\"(.+?)\">view the values of Pool-Specific_Configuration</A> </TD>", 
				32);

		m = p.matcher(output);

		if (m.find())
		{
			this.server = m.group(1);
			this.tcPoolName = m.group(7);
			this.assignedServers = Double.parseDouble(m.group(2));
			this.startingServers = Double.parseDouble(m.group(6));
			this.warmServers = Double.parseDouble(m.group(5));
			this.coldServers = Double.parseDouble(m.group(3));
			queryString = m.group(8);
		}
		else
			return;

		output = getUrlContent(queryString);

		p = Pattern.compile(
				"<TD>PROCESS_MAX=([0-9]+?)</TD>\n</TR>\n<TR>\n<TD ALIGN=center>.+?</TD><TD ALIGN=center>RO</TD>\n<TD>PROCESS_READY_TIMEOUT=[0-9]+?</TD>\n</TR>\n<TR>\n<TD ALIGN=center>.+?</TD><TD ALIGN=center>RO</TD>\n<TD>PROCESS_TARGET=(.+?)</TD>\n</TR>\n<TR>\n<TD ALIGN=center>.+?</TD><TD ALIGN=center>RO</TD>\n<TD>PROCESS_WARM=([0-9]+?)</TD>", 
				32);

		m = p.matcher(output);

		if (m.find())
		{
			this.processMax = Double.parseDouble(m.group(1));
			this.processTarget = findProcessTarget(m.group(2));
			this.processWarm = Double.parseDouble(m.group(3));
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
		super.addSamples(this.rrdTcEnterprise, this.processMax, this.processWarm, this.processTarget, this.assignedServers, this.warmServers, this.coldServers, this.startingServers);
	}

	protected final void buildGraph() throws Exception
	{
		super.buildGraph();

		RrdGraphDef graphDef = new RrdGraphDef();
		graphDef.setVerticalLabel("(#)");
		graphDef.datasource("assignedServersave", this.rrdTcEnterprise.getAbsolutePath(), "assignedServers", ConsolFun.AVERAGE);
		graphDef.datasource("assignedServersmax", this.rrdTcEnterprise.getAbsolutePath(), "assignedServers", ConsolFun.MAX);
		graphDef.datasource("warmServersave", this.rrdTcEnterprise.getAbsolutePath(), "warmServers", ConsolFun.AVERAGE);
		graphDef.datasource("coldServersave", this.rrdTcEnterprise.getAbsolutePath(), "coldServers", ConsolFun.AVERAGE);
		graphDef.datasource("startingServersave", this.rrdTcEnterprise.getAbsolutePath(), "startingServers", ConsolFun.AVERAGE);
		graphDef.datasource("processTargetave", this.rrdTcEnterprise.getAbsolutePath(), "processTarget", ConsolFun.AVERAGE);
		graphDef.datasource("processWarmave", this.rrdTcEnterprise.getAbsolutePath(), "processWarm", ConsolFun.AVERAGE);
		graphDef.datasource("processMaxave", this.rrdTcEnterprise.getAbsolutePath(), "processMax", ConsolFun.AVERAGE);
		graphDef.datasource("processTargetWarmave", "processTargetave,processWarmave,+");
		graphDef.area("assignedServersave", new Color(100, 255, 100), "Assigned Servers");
		graphDef.stack("warmServersave", new Color(255, 255, 100), "Warm Servers");
		graphDef.stack("coldServersave", new Color(100, 100, 255), "Cold Servers");
		graphDef.stack("startingServersave", new Color(255, 100, 100), "Starting Servers");
		graphDef.line("processTargetave", new Color(0, 0, 0), "Process Target", 1.0F);
		graphDef.line("processTargetWarmave", new Color(0, 0, 0), "Process Target+Warm", 1.0F);
		graphDef.line("processMaxave", new Color(0, 0, 0), "Process Max", 1.5F);
		graphDef.setMinValue(0.0D);
		graphDef.comment("\\l");
		graphDef.comment("\\l");
		graphDef.gprint("assignedServersave", ConsolFun.AVERAGE, "Assigned Servers    : %3.0f");
		graphDef.gprint("assignedServersmax", ConsolFun.MAX, "Assigned Servers Max: %3.0f\\l");
		graphDef.gprint("warmServersave", ConsolFun.AVERAGE, "Warm Servers        : %3.0f");
		graphDef.gprint("coldServersave", ConsolFun.AVERAGE, "Cold Servers        : %3.0f\\l");
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

		this.rrdTcEnterprise = new File(this.rrdFilePath + File.separator + this.id + "_tcent_j2ee.rrd");
		this.graphTcEnterprise = new File(this.graphFilePath + File.separator + this.id + "_tcent_j2ee");

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