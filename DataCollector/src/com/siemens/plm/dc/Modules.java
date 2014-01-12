package com.siemens.plm.dc;

import com.siemens.plm.dc.modules.CpuServerWin;
import com.siemens.plm.dc.modules.DiskServerWin;
//import com.siemens.plm.dc.modules.MemoryWinServer;
import com.siemens.plm.dc.modules.NetTcpPing;
import com.siemens.plm.dc.modules.TcEnterpriseJ2EE;
import com.siemens.plm.dc.modules.TcEnterpriseNet;
import com.siemens.plm.dc.modules.TcFsc;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class Modules
{

  @XmlElement(name="tc_fms")
  private List<TcFsc> tcfms;

  @XmlElement(name="tc_enterprise_net")
  private List<TcEnterpriseNet> tcenterprisenet;

  @XmlElement(name="tc_enterprise_j2ee")
  private List<TcEnterpriseJ2EE> tcenterprisej2ee;

  @XmlElement(name="cpu_server_win")
  private List<CpuServerWin> cpuserverwin;

  //@XmlElement(name="mem_server_win")
  //private List<MemoryWinServer> memserverwin;

  @XmlElement(name="disk_server_win")
  private List<DiskServerWin> diskserverwin;

  @XmlElement(name="net_tcpping_win")
  private List<NetTcpPing> tcpping;
  private List<Module> allModules = null;

  public List<Module> getModules()
  {
    if (this.allModules == null)
    {
      this.allModules = new ArrayList<Module>();
      if (this.tcpping != null) this.allModules.addAll(this.tcpping);
      if (this.cpuserverwin != null) this.allModules.addAll(this.cpuserverwin);
      //if (this.memserverwin != null) this.allModules.addAll(this.memserverwin);
      if (this.diskserverwin != null) this.allModules.addAll(this.diskserverwin);
      if (this.tcenterprisenet != null) this.allModules.addAll(this.tcenterprisenet);
      if (this.tcenterprisej2ee != null) this.allModules.addAll(this.tcenterprisej2ee);
      if (this.tcfms != null) this.allModules.addAll(this.tcfms);

      SortedMap<Integer, Module> sortedModules = new TreeMap<Integer, Module>();
      for (Module mod : this.allModules)
      {
        sortedModules.put(new Integer(mod.order), mod);
      }
      this.allModules = new ArrayList<Module>();
      this.allModules.addAll(sortedModules.values());
    }

    return this.allModules;
  }

  public void rmModules(List<Module> modules)
  {
    for (Module mod : modules)
    {
      if (this.allModules.contains(mod))
      {
        this.allModules.remove(mod);
      }
    }
  }
}