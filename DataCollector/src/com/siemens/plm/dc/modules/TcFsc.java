package com.siemens.plm.dc.modules;

import com.siemens.plm.dc.Application;
import com.siemens.plm.dc.Module;
import com.siemens.plm.dc.OSValidator;
import com.siemens.plm.dc.Timer;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.annotation.XmlType;

import org.apache.log4j.Logger;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDef;
import org.rrd4j.graph.RrdGraphDef;

@XmlType
public class TcFsc extends Module
{
	private String TCInstallPath;
	private double cacheHitRead;
	private double cacheSizeRead;
	private double cacheHitWrite;
	private double cacheSizeWrite;
	private File rrdCache;
	private File graphCacheHit;
	private File graphCacheSize;
	
	private static final Logger log = Logger.getLogger(TcFsc.class);

	protected final void rrdCreate() throws Exception
	{
		super.rrdCreate();

		if (!this.rrdCache.exists())
		{
			RrdDef rrdDef = new RrdDef(this.rrdCache.getAbsolutePath());
			rrdDef.addDatasource("cachereadhits", DsType.GAUGE, this.sampleInterval * 2, 0.0D, 100.0D);
			rrdDef.addDatasource("cachewritehits", DsType.GAUGE, this.sampleInterval * 2, 0.0D, 100.0D);
			rrdDef.addDatasource("cachereadsize", DsType.GAUGE, this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
			rrdDef.addDatasource("cachewritesize", DsType.GAUGE, this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
			super.rrdCreate(rrdDef);
		}
	}

	private final void getSamples() throws Exception
	{
		File fscDir = new File(this.TCInstallPath + File.separator + "fsc");
		File javaHome = new File(this.TCInstallPath + File.separator + "install" + File.separator + "install" + File.separator + "jre");
		Map<String,String> env = new HashMap<String,String>();
		env.put("FSC_HOME", fscDir.getCanonicalPath());
		env.put("JAVA_HOME", javaHome.getCanonicalPath());

		String fscAdminCmd = null;
		if (OSValidator.isWindows()) fscAdminCmd = "fscadmin.bat";
		else if ((OSValidator.isSolaris()) || (OSValidator.isUnix())) fscAdminCmd = "fscadmin.sh";

		String cmd = new File(this.TCInstallPath + File.separator + "fsc" + File.separator + fscAdminCmd).getCanonicalPath();
		String args = "-s " + this.url + " ./cachesummary";

		String output = runCommand(cmd, args, env, fscDir.getCanonicalPath());

		Pattern p = Pattern.compile("FSCReadMap\n Files: (.+?), Bytes: (.+?), Hits: (.+?), Misses: (.+?)\n.+?FSCWriteMap\n Files: (.+?), Bytes: (.+?), Hits: (.+?), Misses: (.+?)\n", 32);

		Matcher m = p.matcher(output);

		this.cacheHitRead = Double.NaN;
		this.cacheHitWrite = Double.NaN;
		this.cacheSizeRead = Double.NaN;
		this.cacheSizeWrite = Double.NaN;
		if (m.find())
		{
			double readHits = Double.parseDouble(m.group(3));
			double readMisses = Double.parseDouble(m.group(4));
			double writeHits = Double.parseDouble(m.group(7));
			double writeMisses = Double.parseDouble(m.group(8));

			if (readHits > 0) this.cacheHitRead = readHits / (readHits + readMisses) * 100.0D;
			else this.cacheHitRead = 0.0D;
			if (writeHits > 0) this.cacheHitWrite = writeHits / (writeHits + writeMisses) * 100.0D;
			else this.cacheHitWrite = 0.0D;
			this.cacheSizeRead = (Double.parseDouble(m.group(2)) / 1048576.0D);
			this.cacheSizeWrite = (Double.parseDouble(m.group(6)) / 1048576.0D);
		}
		
		// Wait until interval complete until adding samles
		sleepDelta();
	}

	private final void addSamples() throws Exception
	{
		super.addSamples(this.rrdCache, this.cacheHitRead, this.cacheHitWrite, this.cacheSizeRead, this.cacheSizeWrite);
	}

	protected final void buildGraph() throws Exception
	{
		super.buildGraph();

		RrdGraphDef graphDef = new RrdGraphDef();
		graphDef.setVerticalLabel("(%)");
		graphDef.datasource("cachereadhitsave", this.rrdCache.getAbsolutePath(), "cachereadhits", ConsolFun.AVERAGE);
		graphDef.datasource("cachewritehitsave", this.rrdCache.getAbsolutePath(), "cachewritehits", ConsolFun.AVERAGE);
		graphDef.datasource("cachereadhitsmin", this.rrdCache.getAbsolutePath(), "cachereadhits", ConsolFun.MIN);
		graphDef.datasource("cachewritehitsmin", this.rrdCache.getAbsolutePath(), "cachewritehits", ConsolFun.MIN);
		graphDef.datasource("cachereadhitsmax", this.rrdCache.getAbsolutePath(), "cachereadhits", ConsolFun.MAX);
		graphDef.datasource("cachewritehitsmax", this.rrdCache.getAbsolutePath(), "cachewritehits", ConsolFun.MAX);
		graphDef.line("cachereadhitsave", new Color(100, 255, 100), "Read", 1.5F);
		graphDef.line("cachewritehitsave", new Color(255, 100, 100), "Write", 1.5F);
		graphDef.setMinValue(0.0D);
		graphDef.setMaxValue(100.0D);
		graphDef.comment("\\l");
		graphDef.comment("\\l");
		graphDef.gprint("cachereadhitsave", ConsolFun.AVERAGE, "Read Avg : %3.0f %%");
		graphDef.gprint("cachereadhitsmin", ConsolFun.MIN, "Read Min : %3.0f %%");
		graphDef.gprint("cachereadhitsmax", ConsolFun.MAX, "Read Max : %3.0f %%\\l");
		graphDef.gprint("cachewritehitsave", ConsolFun.AVERAGE, "Write Avg: %3.0f %%");
		graphDef.gprint("cachewritehitsmin", ConsolFun.MIN, "Write Min: %3.0f %%");
		graphDef.gprint("cachewritehitsmax", ConsolFun.MAX, "Write Max: %3.0f %%\\l");
		super.buildGraph(this.url.replaceFirst("https*://", ""), this.graphCacheHit, graphDef);

		graphDef = new RrdGraphDef();
		graphDef.setVerticalLabel("(MB)");
		graphDef.datasource("cachereadsizeave", this.rrdCache.getAbsolutePath(), "cachereadsize", ConsolFun.AVERAGE);
		graphDef.datasource("cachewritesizeave", this.rrdCache.getAbsolutePath(), "cachewritesize", ConsolFun.AVERAGE);
		graphDef.datasource("cachereadsizemin", this.rrdCache.getAbsolutePath(), "cachereadsize", ConsolFun.MIN);
		graphDef.datasource("cachewritesizemin", this.rrdCache.getAbsolutePath(), "cachewritesize", ConsolFun.MIN);
		graphDef.datasource("cachereadsizemax", this.rrdCache.getAbsolutePath(), "cachereadsize", ConsolFun.MAX);
		graphDef.datasource("cachewritesizemax", this.rrdCache.getAbsolutePath(), "cachewritesize", ConsolFun.MAX);
		graphDef.line("cachereadsizeave", new Color(100, 255, 100), "Read", 1.5F);
		graphDef.line("cachewritesizeave", new Color(255, 100, 100), "Write", 1.5F);
		graphDef.setMinValue(0.0D);
		graphDef.comment("\\l");
		graphDef.comment("\\l");
		graphDef.gprint("cachereadsizeave", ConsolFun.AVERAGE, "Read Avg : %5.0f MB");
		graphDef.gprint("cachereadsizemin", ConsolFun.MIN, "Read Min : %5.0f MB");
		graphDef.gprint("cachereadsizemax", ConsolFun.MAX, "Read Max : %5.0f MB\\l");
		graphDef.gprint("cachewritesizeave", ConsolFun.AVERAGE, "Write Avg: %5.0f MB");
		graphDef.gprint("cachewritesizemin", ConsolFun.MIN, "Write Min: %5.0f MB");
		graphDef.gprint("cachewritesizemax", ConsolFun.MAX, "Write Max: %5.0f MB\\l");
		super.buildGraph(this.url.replaceFirst("https*://", ""), this.graphCacheSize, graphDef);
	}

	public final boolean initialize() throws Exception
	{
		if (!super.initialize())
		{
			return false;
		}

		this.TCInstallPath = ((String)this.settings.get("tcinstallpath"));
		this.rrdCache = new File(this.rrdFilePath + File.separator + this.id + "_fms.rrd");
		this.graphCacheHit = new File(this.graphFilePath + File.separator + this.id + "_fms_ratio");
		this.graphCacheSize = new File(this.graphFilePath + File.separator + this.id + "_fms_size");

		Application.graphPrefixes.add(this.graphCacheHit.getName());
		Application.graphPrefixes.add(this.graphCacheSize.getName());

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