package tmcm.xSortLab;
import java.awt.*;
import java.io.*;

class LogPanel extends Panel {

   String saveContents;  // while displaying a special message
   
   TextArea text;
   Button saveButton, clearButton;
   boolean hidden = true;
   String saveText = "";
   
   boolean canSave = true;
   boolean firstTime = true;
   
   String eoln = System.getProperty("line.separator");;
   
   LogPanel() {
      setBackground(Color.lightGray);
      setLayout(new BorderLayout(5,5));
      text = new TextArea();
      text.setEditable(false);
      add("Center", text);
      Panel bottom = new Panel();
      bottom.setBackground(Color.lightGray);
      add("South",bottom);
      clearButton = new Button("Clear Log");
      bottom.add(clearButton);
      saveButton = new Button("Save to File");
      bottom.add(saveButton);
   }
   
   void addLine(String str) {
      if (hidden)
         saveText += str + eoln;
      else {
         clearMessage();
         text.appendText(str + eoln);
         text.select(32000,32000);
      }
   }
   
   void addEoln() {
      if (hidden)
         saveText +=  eoln;
      else {
         clearMessage();
         text.appendText(eoln);
         text.select(32000,32000);
      }
   }
   
   void setMessage(String message) {
      saveContents = text.getText();
      text.setText(message + eoln + eoln + "Click \"Clear Log\" button to dismiss this message.");
   }
   
   void clearMessage() {
      if (saveContents != null) {
         text.setText(saveContents);
         saveContents = null;
      }
   }

   void aboutToShow() {
      if (!firstTime) {
        text.setText(saveText);
        saveText = "";
      }
   }
   
   void shown() {  // firstTime is a fudge to deal with the fact that the panel's peer doesn't seem to exist before it is shown for the first time.
      if (firstTime) {
         text.setText(saveText);
         firstTime = false;
      }
   }
   
   void aboutToHide() {
      clearMessage();
      saveText = text.getText();
   }
   
   void doSave() {
      if (!canSave)
         return;
      String fileName = null,directory = null;
      try {
          FileDialog fd = null;
          try {
            Container c = this;  // find frame that contains this panel
            do {
               Container p = c.getParent();
               if (p == null)
                  break;
               c = p; 
            } while (true);
            if (! (c instanceof Frame) )
               c = null;
            fd = new FileDialog((Frame)c,"Save as:",FileDialog.SAVE);
            fd.show();
          }
          catch (AWTError e) {  // thrown by Netscape 3.0 on attempt to use file dialog
            setMessage("ERROR while trying to create a file dialog box." + eoln + "It will not be possible to save files.");
            canSave = false;
            saveButton.disable();
            return;
          }
          catch (RuntimeException re) {
            setMessage("ERROR while trying to create a file dialog box." + eoln + "It will not be possible to save files.");
            canSave = false;
            saveButton.disable();
            return;
          }
          fileName = fd.getFile();
          if (fileName == null)
             return;
          directory = fd.getDirectory();
          PrintStream out = new PrintStream(new FileOutputStream(new File(directory,fileName)));
          String contents = text.getText();
          out.print(contents);
          out.close();
      }
      catch (IOException e) {
          setMessage("OUTPUT ERROR while trying to save to the file '"  + 
                      fileName + "':  " + eoln + eoln + e.getMessage());
      }
      catch (SecurityException e) {
          setMessage("SECURITY ERROR while trying to save to the file '"  +
                      fileName + "':  " + eoln + eoln + e.getMessage());
      }
   }
   
   public boolean action(Event evt, Object arg) {
      if (evt.target == saveButton) {
         clearMessage();
         doSave();
         return true;
      }
      else if (evt.target == clearButton) {
         if (saveContents != null)
            clearMessage();
         else
           text.setText("");
         return true;
      }
      else
         return super.action(evt,arg);
   }

}