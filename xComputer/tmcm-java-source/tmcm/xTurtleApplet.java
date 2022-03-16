package tmcm;

import tmcm.xTurtle.xTurtleMainPanel;
import java.awt.*;
import java.util.*;
import java.net.*;
import java.applet.Applet;

public class xTurtleApplet extends Applet {

   xTurtleMainPanel tp;
   private boolean firstStart;
   
   String[][] parameterInfo = {
         { "URL", "url", "absolute or relative url of a text file containing a sample xTrutle program" },
         { "URL1,URL2,...", "url", "additional URLs of xTurtle programs" },
         { "BASE", "url", "base url for interpreting URL, URL1, ...; if not given, document base is used" }
      };
   
   public String getAppletInfo() {
      return "xTurtle, by David J. Eck (eck@hws.edu), preliminary version December 1996.";
   }
   
   public String[][] getParameterInfo() {
      return parameterInfo;
   }
   
   public void init() {
      setBackground(Color.lightGray);
      tp = new xTurtleMainPanel(true);
      setLayout(new BorderLayout());
      add("Center",tp);
      firstStart = true;
   }
   
   public void start() {
      tp.start();
      if (firstStart) {
         firstStart = false;
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
               return;
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
            tp.loadURLs(u,nameList);
         }
      }
   }
   
   public void stop() {
      tp.stop();
   }
      
}