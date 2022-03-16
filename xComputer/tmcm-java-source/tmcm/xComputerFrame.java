package tmcm;
import tmcm.xComputer.xComputerPanel;
import java.awt.*;
import java.net.URL;

public class xComputerFrame extends Frame {

   public static void main(String[] args) {
      xComputerFrame frame = new xComputerFrame(args);
   }
   
   private xComputerPanel compPanel;
   private boolean closed = false;
   
   private xComputerFrame(String[] args) {
      super("xComputer");
      reshape(20,30,540,400);
      setResizable(false);
      show();
      if (args == null || args.length == 0)
         compPanel = new xComputerPanel();
      else
         compPanel = new xComputerPanel(args);
      add("Center",compPanel);
      validate();
      compPanel.start();
   }
   
   xComputerFrame(URL[] urlList, String[] nameList) {
      super("xComputer");
      reshape(20,30,580,420);
      setResizable(false);
      show();
      if (urlList  == null)
         compPanel = new xComputerPanel();
      else
         compPanel = new xComputerPanel(urlList,nameList);
      add("Center",compPanel);
      validate();
      compPanel.start();
   }
   
   void close() {
     compPanel.stop();
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