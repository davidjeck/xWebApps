
package tmcm;
import java.awt.*;
import java.util.Vector;
import java.net.*;
import java.applet.Applet;
import tmcm.xTuringMachine.xTuringMachinePanel;

public class xTuringMachineApplet extends Applet {

   xTuringMachinePanel TMpanel;

   String[][] parameterInfo = {
         { "URL", "url", "absolute or relative url of a text file containing a sample xTuringMachine program" },
         { "URL1,URL2,...", "url", "additional URLs of xTTuringMachine programs" },
         { "BASE", "url", "base url for interpreting URL, URL1, ...; if not given, document base is used" }
      };
   
   public String getAppletInfo() {
      return "xTuringMachine, by David J. Eck (eck@hws.edu), Version 1.0 August 1997.";
   }
   
   public String[][] getParameterInfo() {
      return parameterInfo;
   }
   
   
   public void init() {
      setBackground(Color.lightGray);
      setLayout(new BorderLayout());
      Object[] urls = getURLs();
      if (urls == null)
         TMpanel = new xTuringMachinePanel(null,null);
      else
         TMpanel = new xTuringMachinePanel((URL[])urls[0], (String[])urls[1]);
      add("Center",TMpanel);
   }
   
   public void start() {
      TMpanel.start();
   }
   
   public void stop() {
      TMpanel.stop();
   }
   
   public void destroy() {
      TMpanel.destroy();
   }
   
  Object[] getURLs() {
     int urlCt = 0;
     Vector urlList = new Vector();
     Vector names = new Vector();
     String baseStr = getParameter("BASE");
     URL base;
     if (baseStr == null)
        base = getDocumentBase();
     else {
        try {
           base = new URL(getDocumentBase(),baseStr);
        }
        catch (MalformedURLException e) {
           return null;
        }
     }
     String urlString = getParameter("URL");
     URL url;
     do {
        if (urlString != null) {
           try {
              url = new URL(base,urlString);
           }
           catch (MalformedURLException e) {
              continue;
           }
           urlList.addElement(url);
           names.addElement(urlString);
        }
        urlCt++;
        urlString = getParameter("URL" + urlCt);
     } while (urlString != null);
     if (urlList.size() > 0) {
        URL[] u = new URL[urlList.size()];
        String[] nameList = new String[urlList.size()];
        for (int i = 0; i < u.length; i++) {
           u[i] = (URL)urlList.elementAt(i);
           nameList[i] = (String)names.elementAt(i);  
        }
        Object[] temp = new Object[2];
        temp[0] = u;
        temp[1] = nameList;
        return temp;
     }
     else
        return null;
  }
  
}