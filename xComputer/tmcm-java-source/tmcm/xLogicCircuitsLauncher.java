
package tmcm;

import java.awt.*;
import java.util.*;
import java.net.*;
import java.applet.Applet;

public class xLogicCircuitsLauncher extends Applet {

   private xLogicCircuitsFrame frame;
   private Button launchButton;
   private URL url;
   
   String[][] parameterInfo = {
         { "LOAD", "url", "url, relative to document base, of an xLogicCircuits data file" },
      };
   
   public String getAppletInfo() {
      return "xLogicCircuitsLauncher, by David J. Eck (eck@hws.edu), August 1997";
   }
   
   public String[][] getParameterInfo() {
      return parameterInfo;
   }
   
   public void init() {
      setLayout(new BorderLayout());
      launchButton = new Button("Launch xLogicCircuits");
      add("Center",launchButton);
   }
   
   private void launch() {
     if (frame != null) {
        if (frame.isClosed())
           frame = new xLogicCircuitsFrame(url);
        else
           frame.show();
     }
     else {
        getURL();
        frame = new xLogicCircuitsFrame(url);
     }
   }
   
   private void getURL() {
      String urlName = getParameter("LOAD");
      if (urlName == null) {
         url = null;
         return;
      }
      try {
         url = new URL(getDocumentBase(),urlName);
      }
      catch (MalformedURLException e) {
         url =  null;
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