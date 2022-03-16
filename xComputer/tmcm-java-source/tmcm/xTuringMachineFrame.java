package tmcm;
import tmcm.xTuringMachine.xTuringMachinePanel;
import java.awt.*;
import java.net.URL;

public class xTuringMachineFrame extends Frame {

   public static void main(String[] args) {
      xTuringMachineFrame frame = new xTuringMachineFrame(args);   
   }
   
   private xTuringMachinePanel machinePanel;
   private boolean closed = false;
   
   private xTuringMachineFrame(String[] args) {
      super("xTuringMachine");
      machinePanel = new xTuringMachinePanel(args);
      add("Center",machinePanel);
      reshape(20,30,500,380);
      show();
   }
   
   xTuringMachineFrame(URL[] urlList, String[] nameList) {
      super("xTuringMachine");
      machinePanel = new xTuringMachinePanel(urlList,nameList);
      add("Center",machinePanel);
      reshape(20,30,500,380);
      show();
   }
   
   boolean isClosed() {
      return closed;
   }
   
   void close() {
     machinePanel.destroy();
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