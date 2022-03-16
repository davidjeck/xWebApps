
package tmcm.xComputer;
import java.awt.*;
import java.util.BitSet;


class ControlWiresView extends Panel {

   boolean visible;
   BitSet wires = new BitSet(26);
   double lineHeight;
   int baseOffset;
   
   int width = -1;
   
   
   ControlWiresView() {
      setBackground(Color.white);
   }
      
   void putItem(Graphics g, int item) {
      int y = (int)(lineHeight*item) + 2 + baseOffset;
      if (wires.get(item))
         g.setColor(Color.red);
      else
         g.setColor(Color.black);
      g.drawString(Globals.WireName[item],4,y);
   }
   
   void setWires(BitSet newWires) {
      wires = newWires;
      if (visible) {
         Graphics g = getGraphics();
         paint(g);
         g.dispose();
         try { Thread.sleep(25); }
         catch (InterruptedException e) { }
      }
   }
   
   void checkFont(Graphics g) {
      int h = size().height;
      int w = size().width;
      Font f = new Font("Helvetica",Font.PLAIN,12);
      FontMetrics fm = getFontMetrics(f);
      int maxWidth = fm.stringWidth("Load-Data-into-Memory  ");
      int totalHeight = (fm.getAscent()+fm.getDescent())*26 + 18;
      
      if (maxWidth > w || totalHeight > h) {
         f = new Font("Helvetica",Font.PLAIN,10);
         fm = getFontMetrics(f);
         maxWidth = fm.stringWidth("Load-Data-into-Memory  ");
         totalHeight = (fm.getAscent()+fm.getDescent())*26 + 12;
         if (maxWidth > w || totalHeight > h) {
            f = new Font("Helvetica",Font.PLAIN,9);
            fm = getFontMetrics(f);
            maxWidth = fm.stringWidth("Load-Data-into-Memory  ");
            totalHeight = (fm.getAscent()+fm.getDescent())*26 + 8;
            if (maxWidth > w || totalHeight > h) {
               f = new Font("Helvetica",Font.PLAIN,8);
               fm = getFontMetrics(f);
               maxWidth = fm.stringWidth("Load-Data-into-Memory  ");
               totalHeight = (fm.getAscent()+fm.getDescent())*26 + 6;
               if (maxWidth > w || totalHeight > h) {
                  f = new Font("Helvetica",Font.PLAIN,8);
                  fm = getFontMetrics(f);
                  maxWidth = fm.stringWidth("Load-Data-into-Memory  ");
                  totalHeight = (fm.getAscent()+fm.getDescent())*26 + 2;
               }
            }
         }
      }
     
      setFont(f);
      g.setFont(f);
      lineHeight = (double)(size().height - 5) / 26.0;
      baseOffset = fm.getAscent();
   }
   
   public void paint(Graphics g) {
      if (width != size().width) {
         width = size().width;
         checkFont(g);
      }
      for (int i = 0; i < 26; i++)
         putItem(g,i);
   }
   
}