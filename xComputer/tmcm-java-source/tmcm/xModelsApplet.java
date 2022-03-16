package tmcm;
import tmcm.xModels.xModelsPanel;
import java.awt.*;
import java.util.*;
import java.net.*;
import java.applet.Applet;

public class xModelsApplet extends Applet {

   xModelsPanel modelsPanel;
   
   String[][] parameterInfo = {
         { "URL", "url", "absolute or relative url of a text file containing a sample xModels program" },
         { "URL1,URL2,...", "url", "additional URLs of xTurtle programs" },
         { "BASE", "url", "base url for interpreting URL, URL1, ...; if not given, document base is used" }
      };
   
   public String getAppletInfo() {
      return "xModels, by David J. Eck (eck@hws.edu), Version 1.0 June 1997.";
   }
   
   public String[][] getParameterInfo() {
      return parameterInfo;
   }
   
   public void init() {
      setBackground(Color.lightGray);
      setLayout(new BorderLayout());
      Object[] urls = getURLs();
      if (urls == null)
         modelsPanel = new xModelsPanel();
      else
         modelsPanel = new xModelsPanel((URL[])urls[0], (String[])urls[1]);
      add("Center",modelsPanel);
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
  
  public void start() {
     modelsPanel.startApplet();
  }
      
  public void stop() {
     modelsPanel.stopApplet();
  }
  
/*  public boolean gotFocus() {
     modelsPanel.requestFocus();
     return true;
  }
*/
      
}