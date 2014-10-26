package com.siemens.plm.dc;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.URL;
import java.net.URLConnection;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.log4j.Logger;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;

@XmlType
public abstract class Module
implements Runnable
{

	@XmlAttribute(name="id")
	protected String id = "";

	@XmlAttribute(name="title")
	protected String title = "";

	@XmlAttribute(name="order")
	protected String order = "0";

	@XmlAttribute(name="enabled")
	protected String enabled = "false";

	@XmlElement(name="setting")
	protected List<Setting> settingsList;
	//private final double sampleFactor = 1.1D;
	protected String url = null;
	protected long timeOut;
	protected Map<String, String> settings = null;
	protected File rrdFilePath = null;
	protected File graphFilePath = null;
	protected int sampleInterval;
	protected double spinCheckFactor = 1.2D;
	protected int deltaSample;
	protected int graphXSize;
	protected int graphYSize;
	protected String server = null;
	protected String port = null;
	protected boolean collectorStarted = false;
	protected boolean grapherStarted = false;
	protected Thread thread = null;
	protected Timer timer = new Timer();
	
	private static final Logger log = Logger.getLogger(Module.class);
	
	public Module()
	{
	}

	protected final String runCommand(String command, String arguments, Map<String,String> env, String dir)
	{
		Process proc = null;
		Thread tOut = null;
	
		try
		{
			List<String> cmd = new ArrayList<String>();
			cmd.add(command);
			cmd.addAll(Arrays.asList(arguments.replace(" +", " ").split(" ")));
			ProcessBuilder pb = new ProcessBuilder(cmd);

			Map<String,String> pbEnv = pb.environment();
			if (env != null && env.size() > 0) pbEnv.putAll(env);
			if (dir != null && dir.length() > 0) pb.directory(new File(dir));

			log.info("Running operating system command:");
			log.info("Name        : " + command);
			log.debug("Args        : " + arguments);
			log.debug("Directory   : " + dir);

			timer.start();
			proc = pb.start();
			StreamReader srOut = new StreamReader(proc.getInputStream());
			StreamReader srErr = new StreamReader(proc.getErrorStream());
			tOut = srOut.start();
			srErr.start();

			synchronized (tOut)
			{
				tOut.wait(this.timeOut * 1000);
			}

			try
			{
				proc.exitValue();
			}
			catch (IllegalThreadStateException ex)
			{
				log.error("Command \"" + command + "\" did not complete within " + this.timeOut + " s.");
				proc.getOutputStream().flush();
				log.error("Command output on stdout:");
				log.error(srOut.getData());
				log.error("Command output on stderr:");
				log.error(srOut.getData());
				log.error("Killing streamreader threads...");
				try
				{
					proc.getInputStream().close();
				}
				catch (IOException e1) { }
				try
				{
					proc.getErrorStream().close();
				}
				catch (IOException e1) { }
				log.error("Killing process...");
				proc.destroy();
				return "";
			}

			log.info("Command took " + timer.delta() + " ms to complete.");
			log.debug("Command output on stdout:");
			log.debug(srOut.getData());

			return srOut.getData();
		}
		catch (InterruptedException e)
		{
			try
			{
				try
				{
					proc.exitValue();
				}
				catch (IllegalThreadStateException ex)
				{
					try
					{
						proc.getInputStream().close();
					}
					catch (IOException e1) { }
					try
					{
						proc.getErrorStream().close();
					}
					catch (IOException e1) { }
					proc.destroy();
				}
			}
			catch (Exception e1) { }
		}
		catch (Exception e)	{ }

		return "";
	}

	protected final String getUrlContent(String queryString)
	{
		URLConnection urlc = null;
		Thread t = null;
		
		try
		{
			log.info("Fetching data from URL:");
			log.info("Name        : " + this.url + queryString);
			
			timer.start();
			URL uri = new URL(this.url + queryString);
			urlc = uri.openConnection();
			urlc.setUseCaches(false);
			urlc.setConnectTimeout((int)this.timeOut);

			StreamReader sr = new StreamReader(urlc.getInputStream());
			t = sr.start();

			synchronized (t)
			{
				t.wait(this.timeOut * 1000);
			}

			if (t.isAlive())
			{
				log.error("Data fetch from  \"" + queryString + "\" did not complete within expected timeframe.");
				try
				{
					log.error("Killing streamreader thread...");
					urlc.getInputStream().close();
				}
				catch (IOException e1) { }
				return "";
			}

			log.info("Data fetch " + timer.delta() + " ms to complete.");
			log.debug("Fetch output:");
			log.debug(sr.getData());

			return sr.getData();
		}
		catch (InterruptedException e)
		{
			if (t.isAlive())
			{
				try
				{
					urlc.getInputStream().close();
				}
				catch (IOException e1) { }
			}
		}
		catch (Exception e) { }

		return "";
	}

	private final int calcSampleStep(long numberOfSecs)
	{
		double sampleStep = numberOfSecs / (this.sampleInterval * this.graphXSize);
		int sampleStepInt;

		if (sampleStep < 1.0D)
		{
			sampleStepInt = 1;
		}
		else
		{
			sampleStepInt = (int)Math.round(sampleStep);
		}

		return sampleStepInt;
	}

	private final int calcNumberOfSamples(long numberOfSecs, long sampleStep)
	{
		return (int)(numberOfSecs / (this.sampleInterval * sampleStep) * 1.1D);
	}

	private final long getUnixTimestamp()
	{
		return System.currentTimeMillis() / 1000L;
	}

	private final String getLocalTime()
	{
		Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return formatter.format(new Date());
	}

	protected void rrdCreate() throws Exception
	{
		if (!this.rrdFilePath.exists())
		{
			this.rrdFilePath.mkdir();
		}
	}

	protected final void rrdCreate(RrdDef rrdDef) throws Exception
	{
		int lastSampleStep = 0;
		rrdDef.setStartTime(getUnixTimestamp());
		rrdDef.setStep(this.sampleInterval);

		for (String key : Application.sampleIntervals.keySet())
		{
			long intervalValue = ((Interval)Application.sampleIntervals.get(key)).value;
			int sampleStep = calcSampleStep(intervalValue);
			if (sampleStep != lastSampleStep)
			{
				int numberOfSamples = calcNumberOfSamples(intervalValue, sampleStep);
				rrdDef.addArchive(ConsolFun.AVERAGE, 0.5D, sampleStep, numberOfSamples);
				rrdDef.addArchive(ConsolFun.MIN, 0.5D, sampleStep, numberOfSamples);
				rrdDef.addArchive(ConsolFun.MAX, 0.5D, sampleStep, numberOfSamples);
				lastSampleStep = sampleStep;
			}
		}

		RrdDb rrdDB = new RrdDb(rrdDef);
		rrdDB.close();

		Thread.sleep(1000L);
	}

	protected final void addSamples(File rrdFile, double val1) throws Exception
	{
		RrdDb rrdDB = new RrdDb(rrdFile.getAbsolutePath());
		Sample s = rrdDB.createSample();
		s.setAndUpdate(Long.toString(getUnixTimestamp()) + ":" + Double.toString(val1));
		rrdDB.close();
	}

	protected final void addSamples(File rrdFile, double val1, double val2) throws Exception
	{
		RrdDb rrdDB = new RrdDb(rrdFile.getAbsolutePath());
		Sample s = rrdDB.createSample();
		s.setAndUpdate(Long.toString(getUnixTimestamp()) + ":" + Double.toString(val1) + ":" + Double.toString(val2));
		rrdDB.close();
	}

	protected final void addSamples(File rrdFile, double val1, double val2, double val3) throws Exception
	{
		RrdDb rrdDB = new RrdDb(rrdFile.getAbsolutePath());
		Sample s = rrdDB.createSample();
		s.setAndUpdate(Long.toString(getUnixTimestamp()) + ":" + Double.toString(val1) + ":" + Double.toString(val2) + ":" + Double.toString(val3));
		rrdDB.close();
	}

	protected final void addSamples(File rrdFile, double val1, double val2, double val3, double val4) throws Exception
	{
		RrdDb rrdDB = new RrdDb(rrdFile.getAbsolutePath());
		Sample s = rrdDB.createSample();
		s.setAndUpdate(Long.toString(getUnixTimestamp()) + ":" + Double.toString(val1) + ":" + Double.toString(val2) + ":" + Double.toString(val3) + ":" + Double.toString(val4));
		rrdDB.close();
	}

	protected final void addSamples(File rrdFile, double val1, double val2, double val3, double val4, double val5) throws Exception
	{
		RrdDb rrdDB = new RrdDb(rrdFile.getAbsolutePath());
		Sample s = rrdDB.createSample();
		s.setAndUpdate(Long.toString(getUnixTimestamp()) + ":" + Double.toString(val1) + ":" + Double.toString(val2) + ":" + Double.toString(val3) + ":" + Double.toString(val4) + ":" + Double.toString(val5));
		rrdDB.close();
	}

	protected final void addSamples(File rrdFile, double val1, double val2, double val3, double val4, double val5, double val6) throws Exception
	{
		RrdDb rrdDB = new RrdDb(rrdFile.getAbsolutePath());
		Sample s = rrdDB.createSample();
		s.setAndUpdate(Long.toString(getUnixTimestamp()) + ":" + Double.toString(val1) + ":" + Double.toString(val2) + ":" + Double.toString(val3) + ":" + Double.toString(val4) + ":" + Double.toString(val5) + ":" + Double.toString(val6));
		rrdDB.close();
	}

	protected final void addSamples(File rrdFile, double val1, double val2, double val3, double val4, double val5, double val6, double val7) throws Exception
	{
		RrdDb rrdDB = new RrdDb(rrdFile.getAbsolutePath());
		Sample s = rrdDB.createSample();
		s.setAndUpdate(Long.toString(getUnixTimestamp()) + ":" + Double.toString(val1) + ":" + Double.toString(val2) + ":" + Double.toString(val3) + ":" + Double.toString(val4) + ":" + Double.toString(val5) + ":" + Double.toString(val6) + ":" + Double.toString(val7));
		rrdDB.close();
	}

	protected final void addSamples(File rrdFile, double val1, double val2, double val3, double val4, double val5, double val6, double val7, double val8) throws Exception
	{
		RrdDb rrdDB = new RrdDb(rrdFile.getAbsolutePath());
		Sample s = rrdDB.createSample();
		s.setAndUpdate(Long.toString(getUnixTimestamp()) + ":" + Double.toString(val1) + ":" + Double.toString(val2) + ":" + Double.toString(val3) + ":" + Double.toString(val4) + ":" + Double.toString(val5) + ":" + Double.toString(val6) + ":" + Double.toString(val7) + ":" + Double.toString(val8));
		rrdDB.close();
	}

	protected void buildGraph() throws Exception
	{
		if (!this.graphFilePath.exists())
		{
			this.graphFilePath.mkdir();
		}
	}

	protected final void buildGraph(String footer, File filePrefix, RrdGraphDef graphDef) throws Exception
	{
		long now = getUnixTimestamp();

		graphDef.setShowSignature(false);

		graphDef.setTextAntiAliasing(true);
		graphDef.setHeight(this.graphYSize);
		graphDef.setWidth(this.graphXSize);
		graphDef.setImageFormat("png");
		graphDef.comment("\\l");
		graphDef.comment("(" + footer + " / " + getLocalTime() + ")\\r");

		for (String key : Application.graphIntervals.keySet())
		{
			graphDef.setTitle(((Interval)Application.graphIntervals.get(key)).name + " - " + this.title);
			graphDef.setFilename(filePrefix.getAbsolutePath() + "_" + key + ".png");
			graphDef.setStartTime(now - ((Interval)Application.graphIntervals.get(key)).value);

			@SuppressWarnings("unused")
			RrdGraph localRrdGraph = new RrdGraph(graphDef);
		}
	}

	protected boolean initialize() throws Exception
	{
		if (this.enabled.toLowerCase().equals("true"))
		{
			this.settings = new HashMap<String, String>();

			for (Setting setting : this.settingsList)
			{
				this.settings.put(setting.name.toLowerCase(), setting.value);
			}
			for (String setting : Application.globalSettings.keySet())
			{
				if (!this.settings.containsKey(setting.toLowerCase()))
				{
					this.settings.put(setting, (String)Application.globalSettings.get(setting));
				}
			}
			this.sampleInterval = Integer.parseInt((String)this.settings.get("sampleinterval"));
			if (this.settings.containsKey("deltasample"))
			{
				this.deltaSample = Integer.parseInt((String)this.settings.get("deltasample"));
			}
			this.graphXSize = Integer.parseInt((String)this.settings.get("graphxsize"));
			this.graphYSize = Integer.parseInt((String)this.settings.get("graphysize"));
			this.server = ((String)this.settings.get("server"));
			if (this.settings.containsKey("port"))
			{
				this.port = ((String)this.settings.get("port"));
			}
			String login = (String)this.settings.get("login");
			String password = (String)this.settings.get("password");
			this.url = ((String)this.settings.get("url"));
			String timeOutStr = (String)this.settings.get("timeout");
			if (timeOutStr != null)
			{
				this.timeOut = Long.parseLong(timeOutStr);
			}
			else
			{
				this.timeOut = sampleInterval + sampleInterval / 2; 
			}
			String spinCheckFactorStr = (String)this.settings.get("spincheckfactor");
			if (spinCheckFactorStr != null && spinCheckFactorStr.length() > 0)
			{
				this.spinCheckFactor = Double.parseDouble(spinCheckFactorStr);
			}

			if ((this.url != null) && (login != null) && (password != null))
			{
				Authenticator.setDefault(new UrlAuthenticator(this.url, login, password));
			}

			this.rrdFilePath = new File(Application.appPath + File.separator + (String)this.settings.get("rrddatapath"));
			this.graphFilePath = new File(Application.appPath + File.separator + (String)this.settings.get("rrdgraphpath"));

			return true;
		}

		return false;
	}

	protected final void sleepDelta() throws Exception {
		long delta = this.sampleInterval * 1000 - timer.delta();
		
		log.debug(String.format("Sleeping for %d s", delta));
		
		if (delta > 0)
			Thread.sleep(delta);
	}
	
	protected final Thread start()
	{
		String threadName = this.id;
		thread = new Thread(this, threadName);
		thread.setDaemon(true);
		thread.start();

		return thread;
	}

	public void run()
	{
		log.info("Started module " + this.id);
	}
}