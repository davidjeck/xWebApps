
package tmcm.xTurtle;
import java.awt.*;
import java.io.*;
import java.net.*;

class EditPanel extends Panel implements Runnable {
   
   private boolean loading = false;
   private Thread loader;
   private String directory, fileName;
   private URL url;
   boolean error = false;
   TextArea text;
   
   Font f;
   FontMetrics fm;
   
   String title;   
   static int num = 0;
   
   static Color dullRed = new Color(180,0,0);
   
   String[] errorMessage;
   int topOffset = 0;
   
   static String systemEOL = System.getProperty("line.separator");
   
   EditPanel(String title, Font f) {
      setLayout(null);
      setBackground(Color.white);
      text = new TextArea();
      if (f != null)
         text.setFont(new Font("Courier", f.getStyle(), f.getSize()));
      add(text);
      if (title == null) {
         num++;
         this.title = "Untitled Program " + num;
      }
      else
         this.title = title;
   }
   
   public void reshape(int x, int y, int width, int height) {
      super.reshape(x,y,width,height);
      text.reshape(0,topOffset,width,height-topOffset);
   }
   
   void removeErrorMessage() {
      if (errorMessage != null) {
         errorMessage = null;
         topOffset = 0;
         repaint();
         text.reshape(0,0,size().width,size().height);
      }
   }
   
   void addErrorMessage(String message, int pos) {
      if (f == null) {
         f = getFont();
         f = new Font("TimesRoman", f.getStyle(), (int)(1.2*f.getSize()));
         fm = getFontMetrics(f);
      }
      errorMessage = makeStringList(message);
      int newTopOffset = errorMessage.length*fm.getHeight() - fm.getLeading() + 18;
      if (newTopOffset != topOffset) {
         topOffset = newTopOffset;
         text.reshape(0,topOffset,size().width,size().height - topOffset);
      }
      if (pos >= 0)
         text.select(pos,pos);
      repaint();
      text.requestFocus();
   }
   
   synchronized boolean loading() {
      return loading;
   }
   
   synchronized void loadFromURL(URL url) {
      text.setText(systemEOL + "LOADING from URL: " + url.toString() + systemEOL);
      text.setEditable(false);
      loading = true;
      error = false;
      directory = null;
      fileName = null;
      this.url = url;
      loader = new Thread(this);
      try { loader.setPriority(loader.getPriority() - 1); }
      catch (RuntimeException e) { }
      loader.start();
   }
   
   synchronized void loadFromFile(String directory, String fileName) {
      text.setText(systemEOL + "LOADING from file:  " + fileName + systemEOL);
      text.setEditable(false);
      loading = true;
      error = false;
      loader = new Thread(this);
      url = null;
      this.directory = directory;
      this.fileName = fileName;
      try { loader.setPriority(loader.getPriority() - 1); }
      catch (RuntimeException e) { }
      loader.start();
   }
   
  
 /*  public boolean gotFocus(Event evt, Object what) {
      text.requestFocus();
      return true;
   }
 */  
   public void run() { // load from input stream in
      InputStream in = null;
      try {
         int charCt = 0;
         int badCharCt = 0;
         if (url != null)
            in = url.openConnection().getInputStream();
         else
            in = new FileInputStream(new File(directory,fileName));
         StringBuffer str = new StringBuffer(5000);
         int ch = in.read();
         int ct = 0;
         while (ch >= 0 && charCt < 20000) {
            charCt++;
            ct++;
            if (ct == 500) {
               try { Thread.sleep(20); }
               catch (InterruptedException e) { }
               ct = 0;
            }
            char c = (char)ch;
            if (c < ' ' && !Character.isSpace(c))
               badCharCt++;
            else if (c == '\n') {
               str.append(systemEOL);
            }
            else if (c == '\r') {
               str.append(systemEOL);
               ch = in.read();
               if (ch == -1)
                  break;
               if (ch == '\r')
                  str.append(systemEOL);
               else if (ch != (int)'\n')
                  str.append((char)ch);
            }
            else
               str.append(c);
            if (badCharCt > 100 && badCharCt > (charCt / 100))
               throw new IOException("  The data does not seem to be text!  Input aborted.");
            ch = in.read();
         }
         text.setText(str.toString());
         if (charCt >= 20000)
            text.appendText(systemEOL + systemEOL + "Input terminated after reading 20000 characters!" + systemEOL);
      }
      catch (IOException e) {
         text.appendText(systemEOL + "LOAD FAILED; INPUT ERROR:  " + e.getMessage() + systemEOL);
         error = true;
      }
      catch (SecurityException e) {
         text.appendText(systemEOL + "LOAD FAILED; SECURITY ERROR:  " + e.getMessage() + systemEOL);
         error = true;
      }
      finally {
         loading = false;
         text.setEditable(true);
         if (in != null) {
            try { in.close(); }
            catch (IOException e) { }
         }
      }
   }
   
   public boolean mouseDown(Event evt, int x, int y) {
      if (errorMessage != null && y < topOffset)
         removeErrorMessage();
      return true;
   }
   
   public void paint(Graphics g) {
      if (errorMessage != null) {
          g.setFont(f);
          g.setColor(dullRed);
          g.drawRect(4,4,size().width-8,topOffset-8);
          for (int i = 0; i < errorMessage.length; i++)
             g.drawString(errorMessage[i],40,11+i*fm.getHeight()+fm.getAscent());
      }
   }
      
  private String[] makeStringList(String str) {
     int w = size().width - 80;
     char[] ch = str.toCharArray();
     int[] offsets = new int[10];
     offsets[0] = 0;
     int ct = 0;
     int pos = 0;
     int lastBlank = -1;
     StringBuffer buf = new StringBuffer();
     while (pos < str.length()) {
        buf.append(ch[pos]);
        int cw = fm.stringWidth(buf.toString());
        if (cw > w) {
           if (ct == offsets.length - 2)
              break;
           ct++;
           buf.setLength(0);
           if (lastBlank == -1)
              offsets[ct] = pos;
           else {
              offsets[ct] = lastBlank + 1;
              for (int i = lastBlank + 1; i <= pos; i++)
                 buf.append(ch[i]);
           }
           lastBlank = -1;
        }
        if (ch[pos] == ' ')
           lastBlank = pos;
        pos++;
     }
     if (pos > offsets[ct]) {
        ct++;
        offsets[ct] = pos;
     }
     String[] list = new String[ct];
     for (int i=0; i<ct; i++)
       list[i] = new String(ch,offsets[i],offsets[i+1]-offsets[i]);
     return list;
  }
  
   
}

