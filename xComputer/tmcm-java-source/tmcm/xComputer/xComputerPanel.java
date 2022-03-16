
package tmcm.xComputer;
import java.awt.*;
import java.io.*;
import java.net.URL;

public class xComputerPanel extends Panel {

   ComputerGUI computer;
   ProgramPanel programs;
   Choice panelChoice;
   Panel cardPanel;
   CardLayout cardLayout;
   Button loadButton,saveButton,clearButton,assembleButton;
   
   int untitledCount;
   boolean canSave = true, canLoad = true;
   
   boolean computerVisible = true;
   
   Parser parser = new Parser();
   
   public xComputerPanel(URL[] urlList, String[] nameList) {
      this();
      for (int i = 0; i < urlList.length; i++) {
         panelChoice.addItem(nameList[i]);
         programs.loadURL(urlList[i]);
      }
   }

   public xComputerPanel(String[] nameList) {
      this();
      for (int i = 0; i < nameList.length; i++) {
         panelChoice.addItem(nameList[i]);
         programs.loadFile(null,nameList[i],true);
      }
   }

   public xComputerPanel() {
      
      setLayout(new BorderLayout(4,4));
      
      panelChoice = new Choice();
      panelChoice.addItem("Computer");
      panelChoice.addItem("[New]");
      add("North",panelChoice);
      
      cardPanel = new Panel();
      add("Center", cardPanel);
      
      cardLayout = new CardLayout();
      cardPanel.setLayout(cardLayout);

      computer = new ComputerGUI(this);
      cardPanel.add("C",computer);

      Panel temp = new Panel();
      cardPanel.add("P",temp);
      temp.setLayout(new BorderLayout(4,4));
      programs = new ProgramPanel();
      temp.add("Center",programs);
      Panel bottom = new Panel();
      temp.add("South",bottom);
      
      assembleButton = new Button("Translate");
      loadButton = new Button("Load File");
      saveButton = new Button("Save to File");
      clearButton = new Button("Clear");
      bottom.add(assembleButton);
      bottom.add(saveButton);
      bottom.add(loadButton);
      bottom.add(clearButton);
      
   }
   
   public Insets insets() {
      return new Insets(4,4,4,4);
   }
   
   public void start() {
      computer.start();
   }
   
   public void stop() {
   }
   
   public void destroy() {
   }
   
   void doPanelChoice() {
      int item = panelChoice.getSelectedIndex();
      if ((item == 0 && computerVisible) || (item > 1 && item == programs.getCurrentProgramNumber()))
         return;
      else if (item == 0)
         showComputer();
      else if (item > 1)
         showProgramPanel(item-2);
      else
         doNewProgram(null,null);
   }
   
   void doNewProgram(String title, String text) {
      if (title == null) {
         untitledCount++;
         panelChoice.addItem("Untitled " + untitledCount);
      }
      else
         panelChoice.addItem(title);
      panelChoice.select(panelChoice.countItems() - 1);
      programs.newProgram(text);
      showProgramPanel(panelChoice.getSelectedIndex() - 2);
   }
   
   
   void showProgramPanel(int programNumber) {
      if (programNumber != programs.getCurrentProgramNumber())
         programs.selectProgram(programNumber);
      if (computerVisible) {
         computer.stopRunning();
         computer.removeErrorMessage();
         cardLayout.show(cardPanel,"P");
         computerVisible = false;
      }
//      programs.text.requestFocus();
   }
   
   void showComputer() {
       if (computerVisible)
          return;
       cardLayout.show(cardPanel,"C");
       computerVisible = true;
       programs.removeErrorMessage();
   }
   
   void doAssemble() {
      if (computerVisible)
         return;
      String s = programs.getCurrentContents();
      short[] data = null;
      try { 
         data = parser.parse(s); 
      }
      catch ( ParseError e ) {
         programs.addErrorMessage("ERROR in assembly language program:  " + e.getMessage(),e.pos);
         return;
      }
      programs.removeErrorMessage();
      panelChoice.select(0);
      showComputer();
      try { Thread.sleep(500); }
      catch (InterruptedException e) { }
      computer.reset(data);
   }
   
   
   void doLoad() {
      if (computerVisible || !canLoad)
         return;
      FileDialog fd = null;
      try {
        Container c = this;  // find frame that contains this panel
        do {
           Container p = c.getParent();
           if (p == null)
              break;
           c = p; 
        } while (true);
        fd = new FileDialog((Frame)c,"Select File to Load",FileDialog.LOAD);
        fd.show();
      }
      catch (AWTError e) {  // thrown by Netscape 3.0 on attempt to use file dialog
        String error = "ERROR while trying to create a file dialog box.  It will not be possible to save files.";
        programs.addErrorMessage(error,-1);
        canLoad = false;
        loadButton.disable();
        computer.loadFileBttn.disable();
        computer.canLoad = false;
        return;
      }
      catch (RuntimeException re) { // illegal typecast, maybe?
        String error = "ERROR while trying to create a file dialog box.  It will not be possible to save files.";
        programs.addErrorMessage(error,-1);
        canLoad = false;
        loadButton.disable();
        computer.loadFileBttn.disable();
        computer.canLoad = false;
        return;
      }
      String fileName = fd.getFile();
      if (fileName == null)
         return;
      String dir = fd.getDirectory();
      panelChoice.addItem(fileName);
      panelChoice.select(panelChoice.countItems() - 1);
      programs.loadFile(dir,fileName,false);
   }
   
   void doLoadCommandFromComputer() {
      if (!canLoad)
         return;
      FileDialog fd = null;
      try {
        Container c = this;  // find frame that contains this panel
        do {
           Container p = c.getParent();
           if (p == null)
              break;
           c = p; 
        } while (true);
        fd = new FileDialog((Frame)c,"Select File to Load",FileDialog.LOAD);
        fd.show();
      }
      catch (AWTError e) {  // thrown by Netscape 3.0 on attempt to use file dialog
        computer.addErrorMessage("Can't Load Files");
        canLoad = false;
        loadButton.disable();
        computer.loadFileBttn.disable();
        computer.canLoad = false;
        return;
      }
      catch (RuntimeException re) { // illegal typecast, maybe?
        computer.addErrorMessage("Can't Load Files");
        canLoad = false;
        loadButton.disable();
        computer.loadFileBttn.disable();
        computer.canLoad = false;
        return;
      }
      String fileName = fd.getFile();
      if (fileName == null)
         return;
      String dir = fd.getDirectory();
      panelChoice.addItem(fileName);
      panelChoice.select(panelChoice.countItems() - 1);
      programs.loadFile(dir,fileName,false);
      showProgramPanel(panelChoice.getSelectedIndex() - 2);
   }
   
   void doSave() {
      if (computerVisible || !canSave || saveButton == null)
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
            fd = new FileDialog((Frame)c,"Save as:",FileDialog.SAVE);
            fd.show();
          }
          catch (AWTError e) {  // thrown by Netscape 3.0 on attempt to use file dialog
            programs.addErrorMessage("ERROR while trying to create a file dialog box.  It will not be possible to save files.",-1);
            canSave = false;
            saveButton.disable();
            return;
          }
          catch (RuntimeException re) {
            programs.addErrorMessage("ERROR while trying to create a file dialog box.  It will not be possible to save files.",-1);
            canSave = false;
            saveButton.disable();
            return;
          }
          fileName = fd.getFile();
          if (fileName == null)
             return;
          directory = fd.getDirectory();
          PrintStream out = new PrintStream(new FileOutputStream(new File(directory,fileName)));
          String contents = programs.getCurrentContents();
          out.print(contents);
          out.close();
      }
      catch (IOException e) {
          programs.addErrorMessage("OUTPUT ERROR while trying to save to the file '" + fileName + "':  " + e.getMessage(), -1);
      }
      catch (SecurityException e) {
          programs.addErrorMessage("SECURITY ERROR while trying to save to the file '" + fileName + "':  " + e.getMessage(), -1);
      }
   }
   
   
   public boolean action(Event evt, Object obj) {
      if (evt.target == panelChoice) {
         doPanelChoice();
         return true;
      }
      else if (evt.target instanceof Button) {
         if (evt.target == loadButton)
            doLoad();
         else if (evt.target == saveButton)
            doSave();
         else if (evt.target == clearButton)
            programs.doClear();
         else if (evt.target == assembleButton)
            doAssemble();
/*         if (!computerVisible)
            programs.text.requestFocus();  */
         return true; 
      }
      else
         return super.action(evt,obj);
   }

}