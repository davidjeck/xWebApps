
package tmcm;

import java.awt.*;
import java.applet.Applet;

public class xSortLabLauncher extends Applet {

   private xSortLabFrame frame;
   private Button launchButton;
      
   public String getAppletInfo() {
      return "xSortLabLauncher, by David J. Eck (eck@hws.edu), August 1997";
   }
   
   
   public void init() {
      setLayout(new BorderLayout());
      launchButton = new Button("Launch xSortLab");
      add("Center",launchButton);
   }
   
   private void launch() {
     if (frame != null) {
        if (frame.isClosed())
           frame = new xSortLabFrame();
        else
           frame.show();
     }
     else {
        frame = new xSortLabFrame();
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