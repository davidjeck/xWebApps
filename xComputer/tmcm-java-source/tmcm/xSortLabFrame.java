package tmcm;
import tmcm.xSortLab.xSortLabPanel;
import java.awt.*;

public class xSortLabFrame extends Frame {

   public static void main(String[] args) {
      xSortLabFrame frame = new xSortLabFrame();   
   }
   
   private xSortLabPanel sortPanel;
   private boolean closed = false;
   
   xSortLabFrame() {
      super("xSortLab");
      setBackground(Color.lightGray);
      sortPanel = new xSortLabPanel();
      add("Center",sortPanel);
      reshape(20,30,500,380);
      setResizable(false);
      show();
   }
   
   public Insets insets() {
      Insets ins = (Insets)super.insets().clone();
      ins.bottom += 5;
      ins.top += 5;
      ins.left += 5;
      ins.right += 5;
      return ins;
   }
   
   boolean isClosed() {
      return closed;
   }
   
   void close() {
     sortPanel.stop();
     sortPanel.destroy();
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