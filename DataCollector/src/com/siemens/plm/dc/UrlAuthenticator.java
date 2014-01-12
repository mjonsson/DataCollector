package com.siemens.plm.dc;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.concurrent.ConcurrentHashMap;

public class UrlAuthenticator extends Authenticator
{
  private static ConcurrentHashMap<String, LoginInfo> loginData = new ConcurrentHashMap<String, LoginInfo>();

  public UrlAuthenticator(String url, String login, String password)
  {
    loginData.put(url, new LoginInfo(login, password));
  }

  protected PasswordAuthentication getPasswordAuthentication()
  {
    String url = getRequestingURL().toString();
    if (loginData.containsKey(url))
    {
      LoginInfo ld = (LoginInfo)loginData.get(url);
      return new PasswordAuthentication(ld.login, ld.password.toCharArray());
    }
    return null;
  }
}