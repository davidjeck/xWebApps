
package tmcm;

import java.awt.*;
import java.applet.Applet;

public class DataRepsLauncher extends Applet {

   private DataRepsFrame frame;
   private Button launchButton;
   
   String[][] parameterInfo = {
         { "none", "", "" }
      };
   
   public String getAppletInfo() {
      return "DataRepsLauncher, by David J. Eck (eck@hws.edu), July 1997";
   }
   
   public String[][] getParameterInfo() {
      return parameterInfo;
   }
   
   public void init() {
      setLayout(new BorderLayout());
      launchButton = new Button("Launch DataReps");
      add("Center",launchButton);
   }
   
   private void launch() {
     if (frame != null) {
        if (frame.isClosed())
           frame = new DataRepsFrame();
        else
           frame.show();
     }
     else {
        frame = new DataRepsFrame();
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