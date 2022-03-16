
package tmcm;

import java.awt.*;

public class DataRepsFrame extends Frame{

   private DataRepsApplet applet;
   private boolean closed = false;

   public static void main(String[] args) {
      DataRepsFrame frame = new DataRepsFrame();
   }
   
   DataRepsFrame() {
      super("Data Representation");
      applet = new DataRepsApplet();
      add("Center",applet);
      reshape(20,30,370,370);
      setResizable(false);
      setBackground(Color.gray);
      show();
      applet.init();
      applet.validate();
      applet.start();
   }
   
   void close() {
     applet.stop();
     closed = true;
     dispose();
   }
   
   boolean isClosed() {
     return closed;
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
