
package tmcm;

import tmcm.xLogicCircuits.*;
import java.awt.*;
import java.net.*;

public class xLogicCircuitsApplet extends java.applet.Applet {

   xLogicCircuitsPanel circuit;
   
   URL urlToLoad;

   String[][] parameterInfo = {
         { "LOAD", "url", "URL of file to be loaded at startup (can be relative to document base)" },
      };
   
   public String getAppletInfo() {
      return "xLogicCircuitsApplet, by David J. Eck (eck@hws.edu), August 1997";
   }
   
   public String[][] getParameterInfo() {
      return parameterInfo;
   }
   
   public void init() {
      getURLParam();
      circuit = new xLogicCircuitsPanel();
      setLayout(new BorderLayout());
      setBackground(Color.black);
      add("Center",circuit);
   }

   void getURLParam() {
      String urlName = getParameter("LOAD");
      if (urlName == null) {
         urlToLoad = null;
         return;
      }
      try {
         urlToLoad = new URL(getDocumentBase(),urlName);
      }
      catch (MalformedURLException e) {
         urlToLoad =  null;
      }
   }

   public Insets insets() {
      return new Insets(1,1,1,1);
   }
   
   public void start() {
      if (urlToLoad != null) {
         circuit.loadURL(urlToLoad);
         urlToLoad = null;
      }
      circuit.start();
   }
   
   public void stop() {
      circuit.stop();
   }
   
   public void destroy() {
      circuit.destroy();
   }
   
   public Dimension minimumSize() {
      return new Dimension(300,230);
   }
   
}