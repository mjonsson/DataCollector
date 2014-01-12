package com.siemens.plm.dc.modules;
//
//import com.siemens.plm.dc.Application;
import com.siemens.plm.dc.Module;
//import java.awt.Color;
//import java.io.File;
//import java.util.Arrays;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import org.rrd4j.ConsolFun;
//import org.rrd4j.DsType;
//import org.rrd4j.core.RrdDef;
//import org.rrd4j.graph.RrdGraphDef;
//
public class MemoryWinServer extends Module
{
//	private final List<String> perfCounters = Arrays.asList(new String[] { 
//			"\\Memory\\Available Bytes", 
//			"\\Memory\\Committed Bytes",
//			"\\Memory\\Commit Limit" });
//
//	private double memPhysUse = 0.0D;
//	private double memPageUse = 0.0D;
//	private double memTotal = 0.0D;
//	private File rrdMemory;
//	private File graphMemory;
//
//	protected final void rrdCreate() throws Exception
//	{
//		super.rrdCreate();
//
//		if (!this.rrdMemory.exists())
//		{
//			RrdDef rrdDef = new RrdDef(this.rrdMemory.getAbsolutePath());
//			rrdDef.addDatasource("memavail", DsType.GAUGE, this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
//			rrdDef.addDatasource("memcache", DsType.GAUGE, this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
//			rrdDef.addDatasource("memuse", DsType.GAUGE, this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
//			super.rrdCreate(rrdDef);
//		}
//	}
//
//	private final void getSamples() throws Exception
//	{
//		String cmd = "typeperf";
//		String args = "";
//		for (String counter : this.perfCounters)
//		{
//			args += "\"\\\\" + this.server + counter + "\" ";
//		}
//		args += "-si " + Integer.toString(this.deltaSample) +
//				" -sc " + Integer.toString(this.sampleInterval / this.deltaSample);
//
//		String output = runCommand(cmd, args, null, null);
//
//		String[] splitOutput = output.split("\\n");
//		Pattern p = Pattern.compile("^\"[0-9]+?");
//
//		int lines = 0;
//		this.memAvail = 0.0D;
//		this.memCache = 0.0D;
//		this.memUse = 0.0D;
//		for (String line : splitOutput)
//		{
//			Matcher m = p.matcher(line);
//
//			if (m.find())
//			{
//				lines++;
//				String[] cols = line.split(",");
//				this.memAvail += Double.parseDouble(cols[1].replaceAll("\"", ""));
//				this.memCache += Double.parseDouble(cols[2].replaceAll("\"", ""));
//				this.memUse += Double.parseDouble(cols[3].replaceAll("\"", ""));
//			}
//		}
//		this.memAvail /= (lines * 1048576.0D);
//		this.memCache /= (lines * 1048576.0D);
//		this.memUse /= (lines * 1048576.0D);
//	}
//
//	private final void addSamples() throws Exception
//	{
//		super.addSamples(this.rrdMemory, this.memAvail, this.memCache, this.memUse);
//	}
//
//	protected final void buildGraph() throws Exception
//	{
//		super.buildGraph();
//
//		RrdGraphDef graphDef = new RrdGraphDef();
//		graphDef.setVerticalLabel("(MB)");
//		graphDef.datasource("memavailave", this.rrdMemory.getAbsolutePath(), "memavail", ConsolFun.AVERAGE);
//		graphDef.datasource("memcacheave", this.rrdMemory.getAbsolutePath(), "memcache", ConsolFun.AVERAGE);
//		graphDef.datasource("memuseave", this.rrdMemory.getAbsolutePath(), "memuse", ConsolFun.AVERAGE);
//		graphDef.datasource("memavailmax", this.rrdMemory.getAbsolutePath(), "memavail", ConsolFun.MAX);
//		graphDef.datasource("memcachemax", this.rrdMemory.getAbsolutePath(), "memcache", ConsolFun.MAX);
//		graphDef.datasource("memusemax", this.rrdMemory.getAbsolutePath(), "memuse", ConsolFun.MAX);
//		graphDef.datasource("memtotalave", "memavailave,memcacheave,+");
//		graphDef.area("memuseave", new Color(100, 255, 100), "In Use");
//		graphDef.stack("memcacheave", new Color(100, 100, 255), "Cache");
//		graphDef.stack("memavailave", new Color(255, 100, 100), "Available");
//		graphDef.setMinValue(0.0D);
//		//graphDef.setMaxValue(100.0D);
//		/*
//    graphDef.comment("\\l");
//    graphDef.comment("\\l");
//    graphDef.gprint("procusertimeave", ConsolFun.AVERAGE, "User      : %3.0f %%");
//    graphDef.gprint("procprivtimeave", ConsolFun.AVERAGE, "System    : %3.0f %%");
//    graphDef.gprint("procinttimeave", ConsolFun.AVERAGE, "Inter.    : %3.0f %%\\l");
//    graphDef.gprint("procusertimemax", ConsolFun.MAX, "User Max  : %3.0f %%");
//    graphDef.gprint("procprivtimemax", ConsolFun.MAX, "System Max: %3.0f %%");
//    graphDef.gprint("procinttimemax", ConsolFun.MAX, "Inter. Max: %3.0f %%\\l");
//		 */
//		super.buildGraph(this.server, this.graphMemory, graphDef);
//	}
//
//	public final boolean initialize() throws Exception
//	{
//		if (!super.initialize())
//		{
//			return false;
//		}
//
//		if (this.sampleInterval % this.deltaSample > 0)
//		{
//			throw new Exception("Sample interval must be evenly dividable by delta sample interval.");
//		}
//		this.rrdMemory = new File(this.rrdFilePath + File.separator + this.id + "_memory.rrd");
//		this.graphMemory = new File(this.graphFilePath + File.separator + this.id + "_memory");
//
//		Application.graphPrefixes.add(this.graphMemory.getName());
//
//		return true;
//	}
//
//	public final void run()
//	{
//		super.run();
//		try
//		{
//			while (true)
//			{
//				rrdCreate();
//				getSamples();
//				addSamples();
//			}
//		}
//		catch (Exception ex)
//		{
//			System.err.println(ex.getMessage());
//			ex.printStackTrace();
//		}
//	}
}