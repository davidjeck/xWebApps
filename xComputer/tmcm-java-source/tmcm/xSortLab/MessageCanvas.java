package tmcm.xSortLab;
import java.awt.*;

class MessageCanvas extends Canvas {

   String message;
   
   Font font12, font10, font9;
   FontMetrics fm12, fm10, fm9;
   int height12, height10, height9;
   int offset12, offset10, offset9;
   
   MessageCanvas(String str) {
     message = str;
   }
   
   MessageCanvas() {
     message = "";
   }
   
   void changeMessage(String message) {
      this.message = message;
      repaint();
   }
   
   void changeMessageNow(String message) {
      this.message = message;
      Graphics g = getGraphics();
      update(g);
      g.dispose();
   }

   public void paint(Graphics g) {
      if (message == null || message.length() == 0)
         return;
      if (font12 == null) {
         Font font = g.getFont();
         int defSize = font.getSize();
         if (defSize > 12) {
            font12 = font;
            fm12 = g.getFontMetrics(font);
            font10 = new Font(font.getName(), Font.PLAIN,Math.round((defSize*5.0F)/6.0F));
            fm10 = g.getFontMetrics(font10);         
            font9 = new Font(font.getName(), Font.PLAIN,Math.round((defSize*3.0F)/4.0F));
            fm9 = g.getFontMetrics(font9);
         }
         else {
            font12 = new Font(font.getName(), Font.PLAIN,12);
            fm12 = g.getFontMetrics(font12);
            font10 = new Font(font.getName(), Font.PLAIN,10);  
            fm10 = g.getFontMetrics(font10);         
            font9 = new Font(font.getName(), Font.PLAIN,9);
            fm9 = g.getFontMetrics(font9);
         }
         height12 = fm12.getAscent() + fm12.getDescent();
         height10 = fm10.getAscent() + fm10.getDescent();
         height9 = fm9.getAscent() + fm9.getDescent();
         offset12 = fm12.getAscent();
         offset10 = fm10.getAscent();
         offset9 = fm9.getAscent();
      }
      int width = size().width - 7;
      int height = size().height;
      int stringWidth = fm12.stringWidth(message);
      int stringHeight = height12;
      int offset = offset12;
      if (stringHeight <= height && stringWidth <= width)
         g.setFont(font12);
      else {
         stringWidth = fm10.stringWidth(message);
         stringHeight = height10;
         offset = offset10;
         if (stringWidth <= width && stringHeight <= height)
            g.setFont(font10);
         else {
            stringHeight = height9;
            offset = offset9;
            g.setFont(font9);
         }
      }
      g.drawString(message,7,(height-stringHeight)/2+offset);
   }

}