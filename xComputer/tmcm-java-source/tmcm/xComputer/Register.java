package tmcm.xComputer;
import java.awt.*;

class Register extends Canvas {

   String name, valString;
   int bits;
   short value, mask;
   
   int name_x, val_x, y;  // where to put the name and value
   Rectangle flashRect = new Rectangle();
   
   boolean dimmed;
   
   int displayStyle, defaultDisplayStyle;
   Converter convert = new Converter();
   StringBuffer binary = new StringBuffer(16);
   
   int width = -1;
   
   final static int   // display styles
      defaultView = 0,
      integerView = 1,
      unsignedView = 2,
      binaryView = 3,
      assemblyView = 4;
      
   

   Register(String name, int bits, int defaultDisplayStyle) {
      setBackground(Color.white);
      this.name = name + ": ";
      this.bits = bits;
      this.displayStyle = this.defaultDisplayStyle = defaultDisplayStyle;
      valString = makeValString();
      if (bits == 16)
         mask = (short)(-1);
      else {
         int m = 0;
         for (int i = 0; i < bits; i++)
            m = (m << 1) | 1;
         mask = (short)m;
      }
   }
   
   void setFontInfo(Graphics g) {
      width = size().width;
      Font f = new Font("Courier",Font.PLAIN,12);
      FontMetrics fm = getFontMetrics(f);
      int needed = fm.stringWidth("COUNT: 0000000000000000 ") + 7;
      if (needed > width) {
          f = new Font("Courier",Font.PLAIN,10);
          fm = getFontMetrics(f);
          needed = fm.stringWidth("COUNT: 0000000000000000 ") + 6;
          if (needed > width) {
             f = new Font("Courier",Font.PLAIN,9);
             fm = getFontMetrics(f);
             needed = fm.stringWidth("COUNT: 0000000000000000 ") + 5;
             if (needed > width) {
                f = new Font("Courier",Font.PLAIN,6);
                fm = getFontMetrics(f);
             }
          }
      }
      int w = 5 + fm.stringWidth("COUNT: ");
      name_x = w - fm.stringWidth(name);
      val_x = w;
      y = size().height / 2 + fm.getAscent() / 2;
      setFont(f);
      g.setFont(f);
      flashRect.x = val_x - 3;
      flashRect.y = 0;
      flashRect.width = size().width - flashRect.x;
      flashRect.height = size().height;
   }
   
   synchronized short get() {
      return value;
   }
   
   synchronized void set(short val, int flashCount) {  // speed setting is used to decide how long to flash
      if (mask == -1)
         value = val;
      else {
         value = (short) (val & mask);
      }
      if (!dimmed) {
         valString = makeValString();
         if (flashCount == 0)
            repaint(flashRect.x,flashRect.y,flashRect.width,flashRect.height);
         else if (flashCount == 1) {
            Graphics g = getGraphics();
            g.setColor(Globals.flashColor);
            g.fillRect(flashRect.x,flashRect.y,flashRect.width,flashRect.height);
            paint(g);
            try { Thread.sleep(100); }
            catch (InterruptedException e) { }
            g.setColor(Color.white);
            g.fillRect(flashRect.x,flashRect.y,flashRect.width,flashRect.height);
            paint(g);
            g.dispose();
         }
         else if (flashCount == 2) {
            Graphics g = getGraphics();
            g.setColor(Globals.flashColor);
            g.fillRect(flashRect.x,flashRect.y,flashRect.width,flashRect.height);
            paint(g);
            try { Thread.sleep(80); }
            catch (InterruptedException e) { }
            g.setColor(Color.white);
            g.fillRect(flashRect.x,flashRect.y,flashRect.width,flashRect.height);
            paint(g);
            try { Thread.sleep(80); }
            catch (InterruptedException e) { }
            g.setColor(Globals.flashColor);
            g.fillRect(flashRect.x,flashRect.y,flashRect.width,flashRect.height);
            paint(g);
            try { Thread.sleep(80); }
            catch (InterruptedException e) { }
            g.setColor(Color.white);
            g.fillRect(flashRect.x,flashRect.y,flashRect.width,flashRect.height);
            paint(g);
            g.dispose();
         }
         else if (flashCount == -1) {  // special value for "cycle" command
            Graphics g = getGraphics();
            g.clipRect(flashRect.x,flashRect.y,flashRect.width,flashRect.height);
            update(g);
            g.dispose();
         }
      }
   }
   
   synchronized void dim(boolean dimmed) {
      if (this.dimmed == dimmed)
         return;
      this.dimmed = dimmed;
      if (dimmed) {
         Graphics g = getGraphics();
         update(g);
         g.dispose();
      }
      else {
         valString = makeValString();
         repaint();
	  }
   }
   
   synchronized void setDisplayStyle(int style) {
      if (style == defaultView)
         style = defaultDisplayStyle;
      if (style == displayStyle)
         return;
      displayStyle = style;
      valString = makeValString();
      if (!dimmed)
         repaint();
   }
   
   synchronized public void paint(Graphics g) {
      if (size().width != width)
         setFontInfo(g);
      if (dimmed) {
         g.setColor(Color.lightGray);
         g.drawString(name,name_x,y);
      }
      else {
         g.setColor(Color.blue);
         g.drawString(name,name_x,y);
         g.setColor(Color.black);
         g.drawString(valString,val_x,y);
      }
   }
   
   String makeValString() {
      switch (displayStyle) {
         case integerView:
            return String.valueOf(get());
         case unsignedView:
            convert.set(get());
            return String.valueOf(convert.getUnsigned());
         case binaryView:
            int val = get();
            binary.setLength(0);
            for (int i = 1 << (bits-1); i > 0; i >>>= 1) 
               binary.append( ((val & i) == 0)? '0' : '1' );
            return binary.toString();
         case assemblyView:
            convert.set(get());
            return convert.getAssembly();
         default:
            return null;
      }
   }
   
  
}