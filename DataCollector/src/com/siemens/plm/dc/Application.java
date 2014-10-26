package com.siemens.plm.dc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

@XmlRootElement(name = "application")
@XmlAccessorType(XmlAccessType.NONE)
public class Application {
	private static final Timer timer = new Timer();
	private static final Logger log = Logger.getLogger(Application.class);

	@XmlElementWrapper(name = "settings")
	@XmlElement(name = "setting")
	public List<Setting> settingsList;

	@XmlElement(name = "modules")
	public Modules modules;
	// public static boolean rereadConfig = false;
	public static HashMap<String, String> globalSettings;
	public static String appPath;
	public static List<String> graphPrefixes = new ArrayList<String>();
	public static Map<String, Interval> sampleIntervals = new LinkedHashMap<String, Interval>() {
		private static final long serialVersionUID = 1L;
		{
			put("1hour", new Interval("1 Hour", 3600));
			put("4hour", new Interval("4 Hours", 14400));
			put("12hour", new Interval("12 Hours", 43200));
			put("1day", new Interval("1 Day", 86400));
			put("1week", new Interval("1 Week", 604800));
			put("1month", new Interval("1 Month", 2592000));
			put("1year", new Interval("1 Year", 31536000));
			put("3year", new Interval("3 Years", 94608000));
		}
	};
	public static Map<String, Interval> graphIntervals = new LinkedHashMap<String, Interval>() {
		private static final long serialVersionUID = 1L;
		{
			put("1hour", new Interval("1 Hour", 3600));
			put("4hour", new Interval("4 Hours", 14400));
			put("12hour", new Interval("12 Hour", 43200));
			put("1day", new Interval("1 Day", 86400));
			put("1week", new Interval("1 Week", 604800));
			put("1month", new Interval("1 Month", 2592000));
			put("1year", new Interval("1 Year", 31536000));
			put("3year", new Interval("3 Years", 94608000));
		}
	};

	public Application() throws Exception {
		Thread.currentThread().setName("main");
		Thread.currentThread().setPriority(1);
	}

	public final void initialize() throws Exception {
		globalSettings = new HashMap<String, String>();
		for (Setting setting : this.settingsList) {
			globalSettings.put(setting.name.toLowerCase().trim(),
					setting.value.trim());
		}

		List<String> toRemove = new ArrayList<String>();
		for (String interval : sampleIntervals.keySet()) {
			if (!((String) globalSettings.get("sampleintervals"))
					.contains(interval)) {
				toRemove.add(interval);
			}
		}
		for (String interval : toRemove) {
			sampleIntervals.remove(interval);
		}
		toRemove = new ArrayList<String>();
		for (String interval : graphIntervals.keySet()) {
			if (!((String) globalSettings.get("graphintervals"))
					.contains(interval)) {
				toRemove.add(interval);
			}
		}
		for (String interval : toRemove) {
			graphIntervals.remove(interval);
		}

		if ((sampleIntervals.isEmpty()) || (graphIntervals.isEmpty())) {
			throw new Exception("No valid time intervals has been defined.");
		}
	}

	public static final void main() {
		File configFile;
		long configLastMod;
		// Application.rereadConfig = false;

		try {
			Application.appPath = URLDecoder.decode(Start.class
					.getProtectionDomain().getCodeSource().getLocation()
					.getPath(), "UTF-8");
			if (!new File(Application.appPath + "log4j.xml").canRead())
				throw new Exception("Cannot open logging configuration file.");
			DOMConfigurator.configure(Application.appPath + "log4j.xml");

			configFile = new File(Application.appPath + "datacollector.xml");
			if (!configFile.canRead())
				throw new Exception(
						"Cannot open application configuration file.");
			configLastMod = configFile.lastModified();

			Configuration config = new Configuration();
			Application app = config.readConfig();
			app.initialize();

			List<Module> toRemove = new ArrayList<Module>();
			for (Module mod : app.modules.getModules()) {
				if (mod.initialize())
					mod.start();
				else
					toRemove.add(mod);
			}
			app.modules.rmModules(toRemove);

			app.generateHTML();

			long workerSleep = Integer
					.parseInt((String) Application.globalSettings
							.get("graphinterval")) * 1000;
			while (true) {
				Thread.sleep(workerSleep);
				for (Module mod : app.modules.getModules()) {
					timer.start();
					mod.buildGraph();
					log.info("Generated graphs in " + timer.delta() + " ms");
				}
				if (configFile.lastModified() > configLastMod) {
					log.warn("Rereading application configuration file.");
					configLastMod = configFile.lastModified();
					// Application.rereadConfig = true;

					log.debug("Waiting for application threads to exit.");
					for (Module m : app.modules.getModules()) {
						if (m.thread.isAlive()) {
							m.thread.interrupt();
							m.thread.join();
							log.debug("   Thread \"" + m.thread.getName()
									+ "\" exited");
						}
					}
					log.warn("All threads exited. Rereading...");
					// Application.rereadConfig = false;

					Application.main();
				}
			}
		} catch (Exception ex) {
			log.error(ex.getMessage());
			log.error(Application.stackTraceToString(ex));
		}
	}

	public static final String stackTraceToString(Exception ex) {
		Writer result = new StringWriter();
		PrintWriter printWriter = new PrintWriter(result);
		ex.printStackTrace(printWriter);
		return result.toString();
	}

	public final void generateHTML() throws Exception {
		String htmlDir = (String) globalSettings.get("htmlpath");
		String graphDir = (String) globalSettings.get("rrdgraphpath");
		File outputDir = new File(appPath + File.separator + htmlDir);
		String header = "<html>\n<head>\n<title>DataCollector Graphs</title>\n<meta http-equiv=\"CACHE-CONTROL\" content=\"NO-CACHE\">\n<meta http-equiv=\"PRAGMA\" content=\"NO-CACHE\">\n<meta http-equiv=\"REFRESH\" content=\""
				+ (String) globalSettings.get("htmlrefresh")
				+ "\">\n"
				+ "</head>\n<body>\n" + "<table>\n";
		String footer = "</table>\n</body>\n</html>\n";
		String bodyAll = "";

		if (!outputDir.exists()) {
			outputDir.mkdir();
		}
		for (String path : graphPrefixes) {
			bodyAll = bodyAll + "<tr>\n";
			for (String interval : graphIntervals.keySet()) {
				bodyAll = bodyAll + "<td>\n<img src=\"../" + graphDir + "/"
						+ path + "_" + interval + ".png\"/>\n</td>\n";
			}
			bodyAll = bodyAll + "</tr>\n";
		}

		BufferedWriter out = new BufferedWriter(new FileWriter(
				outputDir.getAbsolutePath() + "/All.html"));
		out.write(header + bodyAll + footer);
		out.close();

		for (String prefix : graphPrefixes) {
			String bodyModule = "<tr>\n";
			int i = 0;
			for (String interval : graphIntervals.keySet()) {
				i++;
				bodyModule = bodyModule + "<td><img src=\"../" + graphDir + "/"
						+ prefix + "_" + interval + ".png\"/>\n</td>\n";
				if (i % 3 == 0) {
					bodyModule = bodyModule + "</tr>\n<tr>\n";
				}
			}
			bodyModule = bodyModule + "</tr>\n";
			out = new BufferedWriter(new FileWriter(outputDir.getAbsolutePath()
					+ "/" + prefix + ".html"));
			out.write(header + bodyModule + footer);
			out.close();
		}
	}
}
