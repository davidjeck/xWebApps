
package tmcm;

import tmcm.xTurtle.xTurtleMainPanel;
import java.awt.*;
import java.util.*;
import java.net.*;
import java.applet.Applet;

public class xTurtleLauncher extends Applet {

   private xTurtleFrame frame;
   private Button launchButton;
   private URL[] urls;
   private String[] nameList;
   
   String[][] parameterInfo = {
         { "URL", "url", "absolute or relative url of a text file containing a sample xTrutle program" },
         { "URL1,URL2,...", "url", "additional URLs of xTurtle programs" },
         { "BASE", "url", "base url for interpreting URL, URL1, ...; if not given, document base is used" }
      };
   
   public String getAppletInfo() {
      return "xTurtleLauncher, by David J. Eck (eck@hws.edu), July 1997";
   }
   
   public String[][] getParameterInfo() {
      return parameterInfo;
   }
   
   public void init() {
      setLayout(new BorderLayout());
      launchButton = new Button("Launch xTurtle");
      add("Center",launchButton);
   }
   
   private void launch() {
     if (frame != null) {
        if (frame.isClosed())
           frame = new xTurtleFrame(urls,nameList);
        else
           frame.show();
     }
     else {
        getURLs();
        frame = new xTurtleFrame(urls,nameList);
     }
   }
   
   private void getURLs() {
     String baseStr = getParameter("BASE");
     URL base;
     if (baseStr == null)
        base = getDocumentBase();
     else {
        try {
           base = new URL(getDocumentBase(),baseStr);
        }
        catch (MalformedURLException e) {
           return;
        }
     }
     int urlCt = 0;
     Vector urlList = new Vector();
     Vector names = new Vector();
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
        urls = new URL[urlList.size()];
        nameList = new String[urlList.size()];
        for (int i = 0; i < urls.length; i++) {
           urls[i] = (URL)urlList.elementAt(i);
           nameList[i] = (String)names.elementAt(i);  
        }
     }
   }
   
   public void destroy() {
      if (frame != null && !frame.isClosed())
         frame.close();
   }
   
   public boolean action(Event evt, Object arg) {
      if (evt.target == launchButton) {
         launch();
         return true;
      }
      else
         return super.action(evt,arg);
   }
      
}