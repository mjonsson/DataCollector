package com.siemens.plm.dc.modules;

import com.siemens.plm.dc.Application;
import com.siemens.plm.dc.Module;

import java.awt.Color;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDef;
import org.rrd4j.graph.RrdGraphDef;

public class CpuServerWin extends Module {
	private final List<String> perfCounters = Arrays.asList(new String[] {
			"\\Processor(_Total)\\% User Time",
			"\\Processor(_Total)\\% Privileged Time",
			"\\Processor(_Total)\\% Interrupt Time",
			"\\System\\Processor Queue Length" });

	private double procUserTime = 0.0D;
	private double procPrivTime = 0.0D;
	private double procIntTime = 0.0D;
	private double procQueueLen = 0.0D;
	private File rrdProcessor;
	private File graphProcessor;
	// private File graphQueue;

	private static final Logger log = Logger.getLogger(CpuServerWin.class);

	protected final void rrdCreate() throws Exception {
		super.rrdCreate();

		if (!this.rrdProcessor.exists()) {
			RrdDef rrdDef = new RrdDef(this.rrdProcessor.getAbsolutePath());
			rrdDef.addDatasource("procusertime", DsType.GAUGE,
					this.sampleInterval * 2, 0.0D, 100.0D);
			rrdDef.addDatasource("procprivtime", DsType.GAUGE,
					this.sampleInterval * 2, 0.0D, 100.0D);
			rrdDef.addDatasource("procinttime", DsType.GAUGE,
					this.sampleInterval * 2, 0.0D, 100.0D);
			rrdDef.addDatasource("queuelen", DsType.GAUGE,
					this.sampleInterval * 2, 0.0D, (0.0D / 0.0D));
			super.rrdCreate(rrdDef);
		}
	}

	private final void getSamples() throws Exception {
		this.procUserTime = 0.0D;
		this.procPrivTime = 0.0D;
		this.procIntTime = 0.0D;
		this.procQueueLen = 0.0D;

		String cmd = "typeperf";
		String args = "";
		for (String counter : this.perfCounters) {
			args += "\"\\\\" + this.server + counter + "\" ";
		}
		args += "-si " + Integer.toString(this.deltaSample) + " -sc "
				+ Integer.toString(this.sampleInterval / this.deltaSample - 1);

		String output = runCommand(cmd, args, null, null);

		String[] splitOutput = output.split("\\n");
		Pattern p = Pattern.compile("^\"[0-9]+?");

		int lines = 0;
		for (String line : splitOutput) {
			Matcher m = p.matcher(line);

			if (m.find()) {
				lines++;
				String[] cols = line.split(",");
				this.procUserTime += Double.parseDouble(cols[1].replaceAll(
						"\"", ""));
				this.procPrivTime += Double.parseDouble(cols[2].replaceAll(
						"\"", ""));
				this.procIntTime += Double.parseDouble(cols[3].replaceAll("\"",
						""));
				this.procQueueLen += Double.parseDouble(cols[4].replaceAll(
						"\"", ""));
			}
		}
		if (lines == 0) {
			this.procUserTime = Double.NaN;
			this.procPrivTime = Double.NaN;
			this.procIntTime = Double.NaN;
			this.procQueueLen = Double.NaN;
			return;
		}
		this.procUserTime /= lines;
		this.procPrivTime /= lines;
		this.procIntTime /= lines;
		this.procQueueLen /= lines;
	}

	private final void addSamples() throws Exception {
		super.addSamples(this.rrdProcessor, this.procUserTime,
				this.procPrivTime, this.procIntTime, this.procQueueLen);
	}

	protected final void buildGraph() throws Exception {
		super.buildGraph();

		RrdGraphDef graphDef = new RrdGraphDef();
		graphDef.setVerticalLabel("(%/#)");
		graphDef.datasource("procusertimeave",
				this.rrdProcessor.getAbsolutePath(), "procusertime",
				ConsolFun.AVERAGE);
		graphDef.datasource("procprivtimeave",
				this.rrdProcessor.getAbsolutePath(), "procprivtime",
				ConsolFun.AVERAGE);
		graphDef.datasource("procinttimeave",
				this.rrdProcessor.getAbsolutePath(), "procinttime",
				ConsolFun.AVERAGE);
		graphDef.datasource("queuelenave", this.rrdProcessor.getAbsolutePath(),
				"queuelen", ConsolFun.AVERAGE);
		graphDef.datasource("procusertimemax",
				this.rrdProcessor.getAbsolutePath(), "procusertime",
				ConsolFun.MAX);
		graphDef.datasource("procprivtimemax",
				this.rrdProcessor.getAbsolutePath(), "procprivtime",
				ConsolFun.MAX);
		graphDef.datasource("procinttimemax",
				this.rrdProcessor.getAbsolutePath(), "procinttime",
				ConsolFun.MAX);
		graphDef.datasource("queuelenmax", this.rrdProcessor.getAbsolutePath(),
				"queuelen", ConsolFun.MAX);
		graphDef.area("procusertimeave", new Color(100, 255, 100), "User");
		graphDef.stack("procprivtimeave", new Color(100, 100, 255), "System");
		graphDef.stack("procinttimeave", new Color(255, 100, 100), "Interrupts");
		graphDef.line("queuelenave", new Color(0, 0, 0), "Processor Queue",
				1.5F);
		graphDef.setMinValue(0.0D);
		graphDef.setMaxValue(100.0D);
		graphDef.comment("\\l");
		graphDef.comment("\\l");
		graphDef.gprint("procusertimeave", ConsolFun.AVERAGE,
				"User Avg: %3.0f %%");
		graphDef.gprint("procprivtimeave", ConsolFun.AVERAGE,
				"Sys Avg: %3.0f %%");
		graphDef.gprint("procinttimeave", ConsolFun.AVERAGE,
				"Inter. Avg: %3.0f %%");
		graphDef.gprint("queuelenave", ConsolFun.AVERAGE, "Queue Avg: %3.0f\\l");
		graphDef.gprint("procusertimemax", ConsolFun.MAX, "User Max: %3.0f %%");
		graphDef.gprint("procprivtimemax", ConsolFun.MAX, "Sys Max: %3.0f %%");
		graphDef.gprint("procinttimemax", ConsolFun.MAX, "Inter. Max: %3.0f %%");
		graphDef.gprint("queuelenmax", ConsolFun.MAX, "Queue Max: %3.0f\\l");

		super.buildGraph(this.server, this.graphProcessor, graphDef);
	}

	public final boolean initialize() throws Exception {
		if (!super.initialize()) {
			return false;
		}

		if (this.sampleInterval % this.deltaSample > 0) {
			throw new Exception(
					"Sample interval must be evenly dividable by delta sample interval.");
		}
		this.rrdProcessor = new File(this.rrdFilePath + File.separator
				+ this.id + "_processor.rrd");
		this.graphProcessor = new File(this.graphFilePath + File.separator
				+ this.id + "_processor");
		// this.graphQueue = new File(this.graphFilePath + File.separator +
		// this.id + "_queue");

		// Application.graphPrefixes.add(this.graphQueue.getName());
		Application.graphPrefixes.add(this.graphProcessor.getName());

		return true;
	}

	public final void run() {
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