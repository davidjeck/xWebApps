package tmcm;
import tmcm.xLogicCircuits.xLogicCircuitsPanel;
import java.awt.*;
import java.net.URL;

public class xLogicCircuitsFrame extends Frame {

   public static void main(String[] args) {
      xLogicCircuitsFrame frame = new xLogicCircuitsFrame(args);   
   }
   
   private xLogicCircuitsPanel circuitPanel;
   private boolean closed = false;
   
   private xLogicCircuitsFrame(String[] args) {
      super("xLogicCircuits");
      circuitPanel = new xLogicCircuitsPanel();
      add("Center",circuitPanel);
      reshape(20,30,550,430);
      show();
      if (args != null && args.length > 0)
         circuitPanel.loadFile(args[0]);
      circuitPanel.start();
   }
   
   xLogicCircuitsFrame(URL url) {
      super("xLogicCircuits");
      circuitPanel = new xLogicCircuitsPanel();
      add("Center",circuitPanel);
      reshape(20,30,550,430);
      show();
      if (url != null)
         circuitPanel.loadURL(url);
      circuitPanel.start();
   }
   
   boolean isClosed() {
      return closed;
   }
   
   void close() {
     circuitPanel.stop();
     circuitPanel.destroy();
     closed = true;
     dispose();
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