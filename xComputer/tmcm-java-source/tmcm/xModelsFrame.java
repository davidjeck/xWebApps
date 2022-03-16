package tmcm;
import tmcm.xModels.xModelsPanel;
import java.awt.*;
import java.net.URL;

public class xModelsFrame extends Frame {

   public static void main(String[] args) {
      xModelsFrame frame = new xModelsFrame(args);   
   }
   
   private xModelsPanel modelsPanel;
   private boolean closed = false;
   
   private xModelsFrame(String[] args) {
      super("xModels");
      if (args == null || args.length == 0)
         modelsPanel = new xModelsPanel();
      else
         modelsPanel = new xModelsPanel(args);
      add("Center",modelsPanel);
      reshape(20,30,500,380);
      show();
   }
   
   xModelsFrame(URL[] urlList, String[] nameList) {
      super("xModels");
      if (urlList == null)
         modelsPanel = new xModelsPanel();
      else
         modelsPanel = new xModelsPanel(urlList,nameList);
      add("Center",modelsPanel);
      reshape(20,30,500,380);
      show();
   }
   
   boolean isClosed() {
      return closed;
   }
   
   void close() {
     modelsPanel.stopApplet();
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