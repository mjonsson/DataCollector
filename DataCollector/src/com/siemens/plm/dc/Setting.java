package com.siemens.plm.dc;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class Setting
{

  @XmlAttribute
  public String name = null;

  @XmlAttribute
  public String value = null;
}