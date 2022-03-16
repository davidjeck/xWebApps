package tmcm;

import tmcm.xTurtle.xTurtleMainPanel;
import java.awt.*;
import java.net.URL;

public class xTurtleFrame extends Frame {

   public static void main(String[] args) {
   
      int width = 540;
      int height = 420;
      if (args != null && args.length > 0) {
         String str = args[0].trim();
         boolean isInt = str.length() > 0;
         int val = 0;
         for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) < '0' || str.charAt(i) > '9') {
               isInt = false;
               break;
            }
            else
               val = 10*val + (int)(str.charAt(i)) - (int)('0');
         }
         if (isInt && val > 0) {
            width = val;
            if (width > 1000)
               width = 1000;
            else if (width < 300)
               width = 300;
            height = width - 120;
            if (height < 250)
               height = 250;
            if (args.length == 1)
               args = null;
            else
               args[0] = null;
         }
      }
      xTurtleFrame frame = new xTurtleFrame(width,height);
      if (args != null)
         frame.tp.loadFiles(args);
   
   }
   
   private xTurtleMainPanel tp;
   private boolean closed = false;
   
   public boolean isClosed() {
      return closed;
   }
   
   private xTurtleFrame(int width, int height) {
      super("xTurtle");
      tp = new xTurtleMainPanel(true);
      add("Center",tp);
      resize(width,height);
      setResizable(false);
      show();
      tp.start();
   }
   
   public xTurtleFrame(URL[] urlList, String[] nameList) {
      this(540,420);
      if (urlList != null) {
         tp.loadURLs(urlList,nameList);
      }
   }
   
   void close() {
     tp.stop();
     dispose();
     closed = true;
   }
   
   public boolean handleEvent(Event evt) {
      if (evt.id == Event.WINDOW_DESTROY) {
         close();
         return true;
      }
      else
         return super.handleEvent(evt);
   }
  

}