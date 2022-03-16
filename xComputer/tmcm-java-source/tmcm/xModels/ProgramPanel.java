
package tmcm.xModels;

import java.awt.*;
import java.net.*;
import java.io.*;

class ProgramPanel extends Panel {

      // Uses a card Layout to show a set of EditPanels, with buttons
      // along the bottom for load, store, clear, and new.  Additional
      // buttons can be added, if they are handled by a container class.
   
   
   TextArea text;
   int topOffset = 0;
   Font messageFont;
   FontMetrics fm;
   String[] errorMessage;
   static Color dullRed = new Color(180,0,0);
   
   String[] contents;  // Strings giving list of contents for this Panel
   boolean[] isEditable;
   int currentProgram; // string for current program is in the TextArea; contents[currentProgram] is null
   int programCount;  // items in contents[]

   static String systemEOL = System.getProperty("line.separator");

   
   ProgramPanel() {
      setLayout(null);
      setBackground(Color.white);
      text = new TextArea();
      add(text);
      contents = new String[10];
      isEditable = new boolean[10];
      currentProgram = -1;
      programCount = 0;
   }
   
   synchronized String getContents(int i) {
      if (i < 0 || i >= programCount)
         return null;
      else if (i == currentProgram)
         return text.getText();
      else
         return contents[i];
   }
   
   synchronized String getCurrentContents() {
      return text.getText();
   }
   
   synchronized int getCurrentProgramNumber() {
      return currentProgram;
   }
   
   private void extendArrays() {
     String[] temp = new String[contents.length + 10];
     for (int i = 0; i < contents.length; i++)
        temp[i] = contents[i];
     contents = temp;
     boolean[] tempb = new boolean[isEditable.length + 10];
     for (int i = 0; i < isEditable.length; i++)
        tempb[i] = isEditable[i];
     isEditable = tempb;
   }
   
   synchronized void newProgram() {
      removeErrorMessage();
      if (currentProgram >= 0)
         contents[currentProgram] = text.getText();
      text.setText("");
      programCount++;
      if (programCount >= contents.length)
         extendArrays();
      currentProgram = programCount - 1;
      contents[currentProgram] = null;
      isEditable[currentProgram] = true;
      text.setEditable(true);
   }
   
   synchronized void selectProgram(int i) {
      if (i < 0 || i >= programCount || i == currentProgram)
         return;
      removeErrorMessage();
      contents[currentProgram] = text.getText();
      currentProgram = i;
      text.setText(contents[currentProgram]);
      text.setEditable(isEditable[i]);
   }
   
   synchronized void doClear() {
      if (currentProgram >= 0 && !isEditable[currentProgram]) {
         isEditable[currentProgram] = true;
         text.setEditable(true);
      }
      removeErrorMessage();
      text.setText("");
   }

   void loadURL(URL url) {
      if (programCount >= contents.length)
         extendArrays();
      String str = systemEOL + "LOADING from URL: " + url.toString() + systemEOL;
      programCount++;
      if (currentProgram < 0) {
         currentProgram = programCount - 1;
         text.setEditable(false);
         text.setText(str);
      }
      else
         contents[programCount - 1] = str;
      isEditable[programCount - 1] = false;
      new ProgramLoader(url,this,programCount - 1);
   }
   
   void loadFile(String directory, String name, boolean atStartup) {
      if (programCount >= contents.length)
         extendArrays();
      programCount++;
      String str = systemEOL + "LOADING from file: " + name + systemEOL;
      if (!atStartup) {
         removeErrorMessage();
         contents[currentProgram] = text.getText();
         isEditable[currentProgram] = text.isEditable();
         text.setEditable(false);
         text.setText(str);
         currentProgram = programCount - 1;
      }
      else if (currentProgram < 0) {
         currentProgram = programCount - 1;
         text.setEditable(false);
         text.setText(str);
      }
      else
         contents[programCount - 1] = str;
      isEditable[programCount - 1] = false;
      new ProgramLoader(directory,name,this,programCount - 1);
   }
   
   synchronized void doneLoading(int ID, boolean success, String txt) {
      if (!isEditable[ID]) {
         if (success) {
            if (currentProgram == ID)
               text.setText(txt);
            else
               contents[ID] = txt;
         }
         else {
            if (currentProgram == ID)
               text.appendText(txt);
            else
               contents[ID] = contents[ID] + txt;
         }
         isEditable[ID] = true;
         if (currentProgram == ID) {
            text.setEditable(true);
            text.select(0,0);
         }
      }
   }
   
   public void reshape(int x, int y, int width, int height) {
      super.reshape(x,y,width,height);
      Font f = getFont();
      if (f != null)
         text.setFont(new Font("Courier", f.getStyle(), f.getSize()));
      errorMessage = null;
      topOffset = 0;
      text.reshape(0,0,width,height);
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
      if (messageFont == null) {
         messageFont = getFont();
         messageFont = new Font("TimesRoman", messageFont.getStyle(), (int)(1.2*messageFont.getSize()));
         fm = getFontMetrics(messageFont);
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
   
   public boolean mouseDown(Event evt, int x, int y) {
      if (errorMessage != null && y < topOffset)
         removeErrorMessage();
      return true;
   }
   
   public void paint(Graphics g) {
      if (errorMessage != null) {
          g.setFont(messageFont);
          g.setColor(dullRed);
          g.drawRect(4,4,size().width-8,topOffset-8);
          for (int i = 0; i < errorMessage.length; i++)
             g.drawString(errorMessage[i],40,11+i*fm.getHeight()+fm.getAscent());
      }
   }
      
/*   public boolean gotFocus(Event evt, Object what) {
      text.requestFocus();
      return true;
   }
*/
   
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




class ProgramLoader extends Thread {

   String directory, fileName;
   URL url;
   ProgramPanel owner;
   int ID;

   ProgramLoader(URL url, ProgramPanel owner, int ID) {
      this.owner = owner;
      this.url = url;
      this.ID = ID;
      start();
   }
   
   ProgramLoader(String directory, String fileName, ProgramPanel owner, int ID) {
      this.owner = owner;
      this.directory = directory;
      this.fileName = fileName;
      this.ID = ID;
      start();
   }
   
   
   public void run() { // load from input stream in
      try { setPriority(getPriority() - 1); }
      catch (RuntimeException e) { }
      String systemEOL = ProgramPanel.systemEOL;
      String text = null;
      boolean success = false;
      InputStream in = null;
      try {
         int charCt = 0;
         int badCharCt = 0;
         if (url != null)
            in = url.openConnection().getInputStream();
         else
            in = new FileInputStream(new File(directory,fileName));
         StringBuffer str = new StringBuffer(10000);
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
         if (charCt >= 20000)
            str.append(systemEOL + systemEOL + "Input terminated after reading 20000 characters!" + systemEOL);
         text = str.toString();
         success = true;
      }
      catch (IOException e) {
         text = systemEOL + "LOAD FAILED; INPUT ERROR:  " + e.getMessage() + systemEOL;
      }
      catch (SecurityException e) {
         text = systemEOL + "LOAD FAILED; SECURITY ERROR:  " + e.getMessage() + systemEOL;
      }
      catch (Exception e) {
         text = systemEOL + "LOAD FAILED; ERROR:  " + e.toString() + systemEOL;
      }
      finally {
         owner.doneLoading(ID,success,text);
         if (in != null) {
            try { in.close(); }
            catch (IOException e) { }
         }
      }
   }
  
     
}