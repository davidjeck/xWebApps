
package tmcm.xComputer;
import java.awt.*;
import java.awt.image.*;


class GraphicsMemoryView extends Panel {

   boolean visible;
   int[] pixels = new int[1024*16];
   int[] pix1 = new int[16];
   MemoryImageSource memSrc;
   MemoryImageSource oneLocSrc;
   Image mem;
   Image oneLoc;
   int topOffset = -1, leftOffset = -1;
   
   int black = Color.black.getRGB();
   int white = Color.white.getRGB();
   
   
   GraphicsMemoryView() {
      setBackground(new Color(180,180,255));
      memSrc = new MemoryImageSource(64,256,pixels,0,64);
      mem = createImage(memSrc);
      oneLocSrc = new MemoryImageSource(16,1,pix1,0,16);
      oneLoc = createImage(oneLocSrc);
   }
   
   public void paint(Graphics g) {
      if (leftOffset == -1) {
         leftOffset = Math.max(0,(size().width - 64) / 2);
         topOffset = Math.max(0,(size().height - 256) / 2);
      }
      mem.flush();
      g.drawImage(mem,leftOffset,topOffset,null);
   }
   
   
   void reset(short[] newData) {
      int loc = 0;
      for (int i = 0; i < 1024; i++) {
         int d = newData[i];
         pixels[loc++] = (d < 0)? black : white;
         d &= 0x7FFF;
         for (int j = 0x4000; j != 0; j >>= 1)
            pixels[loc++] = ((d & j) == 0)? white : black;
      }
      if (visible)
         repaint();
   }
   
   void set(int location, int val) {
      int loc = location*16;
      int i = 1;
      pix1[0] = pixels[loc++] = (val < 0)? black : white;
      val &= 0x7FFF; 
      for (int j = 0x4000; j != 0; j >>= 1)
         pix1[i++] = pixels[loc++] = ((val & j) == 0)? white : black;
      if (visible) {
        oneLoc.flush();
        Graphics g = getGraphics();
        if (leftOffset == -1) {
           leftOffset = Math.max(0,(size().width - 64) / 2);
           topOffset = Math.max(0,(size().height - 256) / 2);
        }
        int y = (location >> 2) + topOffset;
        int x = 16*(location & 3) + leftOffset;
        g.drawImage(oneLoc,x,y,null);
        g.dispose();
      }
   }
   
   
   
   
}