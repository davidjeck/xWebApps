package tmcm;
import java.awt.*;
import tmcm.xSortLab.xSortLabPanel;

public class xSortLabApplet extends java.applet.Applet {

   xSortLabPanel sortLab;

   public String getAppletInfo() {
      return "xSortLabApplet, by David J. Eck (eck@hws.edu), August 1997";
   }
   
   public void init() {
      setBackground(Color.lightGray);
      setLayout(new BorderLayout());
      sortLab = new xSortLabPanel();
      add("Center",sortLab);
   }
   
   public Insets insets() {
      return new Insets(5,5,5,5);
   }
   
   public void start() {
      sortLab.start();
   }
   
   public void stop() {
      sortLab.stop();
   }
   
   public void destroy() {
      sortLab.destroy();
   }


}
