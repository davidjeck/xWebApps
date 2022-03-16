
package tmcm.xComputer;
import java.awt.*;


class ScrollMemoryView extends Panel {

   
   boolean visible;
   
   int currentStyle;
   
   Scrollbar bar;
   MemoryScrollCanvas canvas;
   
   ScrollMemoryView() {
      canvas = new MemoryScrollCanvas(this);
      bar = new Scrollbar();
      setLayout(new BorderLayout());
      add("Center",canvas);
      add("East",bar);
   }   
   
   
   void reset(short[] newData) {
      System.arraycopy(newData,0,canvas.data,0,1024);
      canvas.redrawOSC();
      if (visible)
         canvas.repaint();
   }
   
   void setScrollPos(int scrollTo) {
      canvas.setStart(scrollTo);
      if (visible) {
         Graphics g = canvas.getGraphics();
         canvas.paint(g);
         g.dispose();
      }
      bar.setValue(scrollTo);
   }
   
   void setViewStyle(int style) {
      canvas.currentStyle = style;
      canvas.redrawOSC();
      if (visible)
         canvas.repaint();
   }
   
   void set(int location, short data,int flashCount) {
       canvas.set(location,data,visible,flashCount);
   }
   
   public boolean handleEvent(Event evt) {
      if (evt.id == Event.SCROLL_LINE_DOWN || evt.id == Event.SCROLL_LINE_UP || 
          evt.id == Event.SCROLL_PAGE_DOWN || evt.id == Event.SCROLL_PAGE_UP || 
          evt.id == Event.SCROLL_ABSOLUTE) {
         int val = bar.getValue();
         if (val > 1023) {
            val = 1023;
            bar.setValue(val);
         }
         else if (val < 0) {
            val = 0;
            bar.setValue(0);
         }
         canvas.setStart(val);
         if (visible)
            canvas.repaint();
         return true;
      }
      return super.handleEvent(evt);
   }
      
}  // end of class ScrollMemoryView



class MemoryScrollCanvas extends Canvas {

   final static int assemblerView = 0,       // viewStyles
                    signedView = 1, 
                    unsignedView = 2,
                    binaryView = 3, 
                    asciiView = 4;

   short[] data;
   int lineHeight;
   int baseOffset;
   int dataOffset;     // x-value where data starts
   int topOffset;
   int lines;
   int startLine;
   int currentStyle;
   
   ScrollMemoryView owner;
   int width = -1;
   
   Converter convert = new Converter();
   
   char[] binaryChars;
   StringBuffer buffer;


   Image OSC;
   Graphics OSG;
   
   MemoryScrollCanvas(ScrollMemoryView owner) {
      this.owner = owner;
      data = new short[1024];
      setBackground(Color.white);
      binaryChars = new char[16];
      buffer = new StringBuffer(16);
      buffer.setLength(16);
      currentStyle=1;
   }
   
   synchronized void set(int location, short val, boolean visible, int flashCount) {
      data[location] = val;
      if (location >= startLine && location < startLine + lines) {
         int line = location - startLine;
         int x = dataOffset - 2;
         int y = line*lineHeight + topOffset;
         int w = size().width - dataOffset + 2;
         int h = lineHeight - 2;
         OSG.setColor(Color.white);
         OSG.fillRect(x,y,w,h);
         drawItemOnCanvas(line,-1,data[location]);
         if (visible) {
           if (flashCount == 0)
              repaint(x,y,w,h);
           else if (flashCount == 1) {
              String str = makeValString(val);
              int str_y = line*lineHeight + baseOffset + topOffset;
              Graphics g = getGraphics();
              g.setColor(Globals.flashColor);
              g.fillRect(x,y,w,h);
              g.setColor(Color.black);
              g.drawString(str,dataOffset,str_y);
              try { Thread.sleep(100); }
              catch (InterruptedException e) { }
              g.setColor(Color.white);
              g.fillRect(x,y,w,h);
              g.setColor(Color.black);
              g.drawString(str,dataOffset,str_y);
              g.dispose();
           }
           else {
              String str = makeValString(val);
              int str_y = line*lineHeight + baseOffset + topOffset;
              Graphics g = getGraphics();
              g.setColor(Globals.flashColor);
              g.fillRect(x,y,w,h);
              g.setColor(Color.black);
              g.drawString(str,dataOffset,str_y);
              try { Thread.sleep(80); }
              catch (InterruptedException e) { }
              g.setColor(Color.white);
              g.fillRect(x,y,w,h);
              g.setColor(Color.black);
              g.drawString(str,dataOffset,str_y);
              try { Thread.sleep(80); }
              catch (InterruptedException e) { }
              g.setColor(Globals.flashColor);
              g.fillRect(x,y,w,h);
              g.setColor(Color.black);
              g.drawString(str,dataOffset,str_y);
              try { Thread.sleep(80); }
              catch (InterruptedException e) { }
              g.setColor(Color.white);
              g.fillRect(x,y,w,h);
              g.setColor(Color.black);
              g.drawString(str,dataOffset,str_y);
              g.dispose();
           }
         }
      }
   }
   
   
   void redrawOSC() {
      int w = size().width;
      int h = size().height;
      OSG.setColor(Color.white);
      OSG.fillRect(0,0,w,h);
      OSG.setColor(Color.black);
      OSG.drawLine(0,0,w,0);
      OSG.drawLine(0,0,0,h);
      OSG.drawLine(0,h-1,w,h-1);
      for (int i = (startLine < 0)? 0 : startLine; i < startLine + lines; i++)
         drawItemOnCanvas(i-startLine,i,data[i]);
   }
      
   synchronized void drawItemOnCanvas(int line, int itemNum, short val) {
      int y = line*lineHeight + baseOffset + topOffset;
      if (itemNum >= 0) {
         OSG.setColor(Color.blue);
         String s;
         if (itemNum >= 1000)
            s = "" + itemNum + ": ";
         else if (itemNum >= 100)
            s = " " + itemNum + ": ";
         else if (itemNum >= 10)
            s = "  " + itemNum + ": ";
         else
            s = "   " + itemNum + ": ";
         OSG.drawString(s,3,y);
      }
      OSG.setColor(Color.black);
      String str = makeValString(val);
      OSG.drawString(str,dataOffset,y);
   }
   
   final String makeValString(short val) {
      convert.set(val);
      switch (currentStyle) {
          case assemblerView:
             return convert.getAssembly();
          case signedView:
             return String.valueOf(convert.getSigned());
          case unsignedView:
              return String.valueOf(convert.getUnsigned());
          case binaryView:
             int v = val;
             for (int i = 0; i < 16; i++) {
                buffer.setCharAt(15-i, ((v & 1) == 0)? '0' : '1');
                v >>>= 1;
             }
             return buffer.toString();
          case asciiView:
             return convert.getAscii();
          default:
             return null;
      }
   }
   
   synchronized void setStart(int newStartLine) {
      startLine = newStartLine - lines + 1;
      redrawOSC();
   }
   
   synchronized public void paint(Graphics g) {
      if (width != size().width) {
         checkFont(g);
         width = size().width;
      }
      if (OSC != null)
         g.drawImage(OSC,0,0,this);
   }
   
   public void update(Graphics g) {
      paint(g);
   }
   
   void checkFont(Graphics g) {
      int w = size().width;
      int h = size().height;
      OSC = createImage(w,h);
      OSG = OSC.getGraphics();
      
      Font f = new Font("Courier",Font.PLAIN,12);
      FontMetrics fm = getFontMetrics(f);
      int needed = fm.stringWidth("1024: 0000000000000000") + 9;
      if (needed > w) {
          f = new Font("Courier",Font.PLAIN,10);
          fm = getFontMetrics(f);
          needed = fm.stringWidth("1024: 0000000000000000") + 9;
          if (needed > w) {
             f = new Font("Courier",Font.PLAIN,9);
             fm = getFontMetrics(f);
             needed = fm.stringWidth("1024: 0000000000000000") + 8;
             if (needed > w) {
                f = new Font("Courier",Font.PLAIN,6);
                fm = getFontMetrics(f);
             }
          }
      }
      
      OSG.setFont(f);
      g.setFont(f);
      setFont(f);
      lineHeight = fm.getHeight() + 5;
      baseOffset = fm.getLeading() + fm.getAscent() + 3;
      lines = (h - 5 - fm.getDescent()) / lineHeight;
      dataOffset = fm.stringWidth("1024: ") + 3;
      topOffset = (h - lineHeight*lines)/2;
      owner.bar.setValues(lines - 1,lines,0,1023+lines);
      setStart(lines - 1);
   }
   
}




