package com.siemens.plm.dc;

import java.io.FileInputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

class Configuration
{
	public Application readConfig() throws Exception
	{
		JAXBContext context = JAXBContext.newInstance(new Class[] { Application.class });
		Unmarshaller um = context.createUnmarshaller();

		Application app = (Application)um.unmarshal(new FileInputStream(Application.appPath + "datacollector.xml"));

		return app;
	}

	public void writeConfig() throws Exception
	{
	}
}