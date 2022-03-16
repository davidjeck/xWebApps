
package tmcm.xTurtle;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class xTurtleMainPanel extends Panel implements TurtleNotification {

   TTurtlePanel graphics;
   CardLayout cardManager;
   Panel cardPanel;
   Button newButton,runButton, pauseButton, clearButton, loadButton, saveButton, indentButton, stopButton;
   Choice panelChoice;
   Choice speedChoice;
   Checkbox showTurtleCheck;
   Checkbox randomSchedulingCheck;
   boolean running = false;
   boolean paused = false;
   
   boolean canLoad;
   boolean canSave;
   
   private boolean firstStart = true;
   
   boolean delayRunStart = false;
   
   int[] speedList = { 0, 20, 100, 300, 600 };
   int delay = 20;
   boolean randomScheduling = true;
   
   EditPanel[] editPanels = new EditPanel[10];
   int editPanelCt = 0;
   int currentPanel = 0;
   int compiledPanel = -1;
      
   TSymbolTable ST;
   TProcess proc;
   TProgram prog;
   TParser parser;
   
   public xTurtleMainPanel() {
      this(true);
   }

   public xTurtleMainPanel(boolean addSaveAndLoadButtons) {
   
      canSave = addSaveAndLoadButtons;
      canLoad = addSaveAndLoadButtons;
   
      parser = new TParser();
      ST = new TSymbolTable();
      graphics = new TTurtlePanel(ST,parser,null,this);
      new TProcess(parser.parse("forward(sin(0)+1)",ST), new TStack(), null, 0, null);  // force loading?
      
      setBackground(Color.lightGray);
      
      setLayout(new BorderLayout(5,5));
      
      panelChoice = new Choice();
      panelChoice.addItem("Graphics");
      panelChoice.addItem("[New]");
      add("North",panelChoice);
      
      Panel buttons = new Panel();
      buttons.setBackground(Color.lightGray);
      if (addSaveAndLoadButtons)
         buttons.setLayout(new GridLayout(11,1,5,5));
      else
         buttons.setLayout(new GridLayout(9,1,5,5));
      newButton = new Button("New Program");
      buttons.add(newButton);
      runButton = new Button("Run Program");
      buttons.add(runButton);
      pauseButton = new Button("Pause");
      buttons.add(pauseButton);
      stopButton = new Button("Stop");
      buttons.add(stopButton);
      clearButton = new Button("Clear");
      buttons.add(clearButton);
      indentButton = new Button("Indent");
      buttons.add(indentButton);
      if (addSaveAndLoadButtons) {
        loadButton = new Button("Load");
        buttons.add(loadButton);
        saveButton = new Button("Save");
        buttons.add(saveButton);
      }
      speedChoice = new Choice();
      speedChoice.addItem("Fastest");
      speedChoice.addItem("Fast");
      speedChoice.addItem("Moderate");
      speedChoice.addItem("Slow");
      speedChoice.addItem("Slowest");
      buttons.add(speedChoice);
      showTurtleCheck = new Checkbox("No Turtles");
      buttons.add(showTurtleCheck);
      randomSchedulingCheck = new Checkbox("Lock Step");
      buttons.add(randomSchedulingCheck);
      add("East",buttons);
      
      cardPanel = new Panel();
      cardManager = new CardLayout();
      cardPanel.setLayout(cardManager);
      cardPanel.add("Graphics",graphics);
      add("Center",cardPanel);
      
   }
   
   public void start() {
      if (firstStart) {
        stopButton.disable();
        pauseButton.disable();
        runButton.disable();
        indentButton.disable();
        if (saveButton != null)
          saveButton.disable();
        speedChoice.select(1);
        firstStart = false;
      }
      graphics.TH.start();
      graphics.command.requestFocus();
   }
   
   synchronized public void stop() {
      if (running)
         proc.setRunning(false);
      graphics.TH.stop();
   }
   
   public Insets insets() {
      return new Insets(5,5,5,5);
   }
   
   public void loadURLs(URL[] urlList, String[] nameList) { // assume lengths of arrays are equal
      if (editPanelCt + urlList.length > editPanels.length) {
         EditPanel[] temp = new EditPanel[editPanelCt + urlList.length + 5];
         for (int i = 0; i < editPanelCt; i++)
            temp[i] = editPanels[i];
         editPanels = temp;
      }
      for (int i = 0; i < urlList.length; i++) {
          String title = urlList[i].getFile();
          EditPanel panel = new EditPanel(nameList[i],getFont());
          editPanels[editPanelCt] = panel;
          String name = String.valueOf(panelChoice.countItems());
          cardPanel.add(name,panel);
          panelChoice.addItem(panel.title);
          editPanelCt++;
          panel.loadFromURL(urlList[i]);
      }
   }

   public void loadFiles(String[] fileNameList) {
      if (editPanelCt + fileNameList.length > editPanels.length) {
         EditPanel[] temp = new EditPanel[editPanelCt + fileNameList.length + 5];
         for (int i = 0; i < editPanelCt; i++)
            temp[i] = editPanels[i];
         editPanels = temp;
      }
      for (int i = 0; i < fileNameList.length; i++) 
        if (fileNameList[i] != null && !fileNameList[i].trim().equals("")) {
          EditPanel panel = new EditPanel(fileNameList[i].trim(),getFont());
          editPanels[editPanelCt] = panel;
          String name = String.valueOf(panelChoice.countItems());
          cardPanel.add(name,panel);
          panelChoice.addItem(panel.title);
          editPanelCt++;
          panel.loadFromFile(null,fileNameList[i]);
       }
   }

   synchronized public void startRunning(TProcess proc) {
      graphics.startRunning(proc);
      this.proc = proc;
      running = true;
      proc.setDelay(delay);
      proc.setRandomScheduling(randomScheduling);
      runButton.disable();
      if (loadButton != null)
        loadButton.disable();
      clearButton.disable();
      if (delayRunStart) {  // starting from pressing run button
         try { Thread.sleep(500); }
         catch (InterruptedException e) { }
         delayRunStart = false;
      }
      pauseButton.enable();
      stopButton.enable();
   }
   
   synchronized void finishUpRun() {
      running = false;
      if (prog != null)
         runButton.enable();
      if (paused)
         pauseButton.setLabel("Pause");
      paused = false;
      pauseButton.disable();
      stopButton.disable();
      clearButton.enable();
      if (canLoad)
        loadButton.enable();
      proc = null;
   }
   
   public void doneRunning() {
      graphics.doneRunning();
      finishUpRun();
   }
   
   public void errorReport(String errorMessage, int position) {
      graphics.errorReport(errorMessage,-1);
      finishUpRun();
      if (position >= 0 && compiledPanel >= 0)
         editPanels[compiledPanel].text.select(position,position);
   }
   
   void doShowEditPanel(int panelNum) {
       if (running)
          stopTheProgram();
       String name = String.valueOf(panelNum);
       cardManager.show(cardPanel,name);
//       editPanels[panelNum-2].text.requestFocus();
       if (canSave)
          saveButton.enable();
       indentButton.enable();
       runButton.setLabel("Run Program");
       runButton.enable();
       currentPanel = panelNum;
   }
   
   void doShowGraphics() {
       cardManager.show(cardPanel,"Graphics");
       if (saveButton != null)
          saveButton.disable();
       indentButton.disable();
       if (prog == null)
          runButton.disable();
       graphics.command.requestFocus();
       currentPanel = 0;
   }
   
   void doNew() {
      if (currentPanel == 0 && running)
         stopTheProgram();
      if (editPanelCt >= editPanels.length) {
         EditPanel[] temp = new EditPanel[editPanels.length + 10];
         for (int i = 0; i < editPanels.length; i++)
            temp[i] = editPanels[i];
         editPanels = temp;
      }
      EditPanel panel = new EditPanel(null,getFont());
      editPanels[editPanelCt] = panel;
      String name = String.valueOf(panelChoice.countItems());
      cardPanel.add(name,panel);
      panelChoice.addItem(panel.title);
      int panelNum = panelChoice.countItems() - 1;
      doShowEditPanel(panelNum);
      panelChoice.select(currentPanel);
      editPanelCt++;
   }
   
   void doChoosePanel() {
      if (currentPanel > 0)
         editPanels[currentPanel-2].removeErrorMessage();
      int choice = panelChoice.getSelectedIndex();
      if (choice == currentPanel)
         return;
      if (choice == 0) {  // Graphics
          if (currentPanel != 0)
             doShowGraphics();
      }
      else if (choice == 1) { // [New]
          doNew();
      }
      else { // an edit panel
          if (currentPanel == 0 && running)
             stopTheProgram();
          doShowEditPanel(choice);
      }
   }
   
   void stopTheProgram() {
      TProcess p;
      synchronized(this) {
         p = proc;
      }
      if (running && p != null) {
            p.setRunning(false);
            try { p.join(1000); }
            catch (InterruptedException e) { }
       }
   }
   
   void doRun() {
      if (running)
         return;
      if (currentPanel == 0) {
         graphics.set(prog,ST,this);
         delayRunStart = true;
         graphics.runTheProgram(0);
      }
      else {
         EditPanel panel = editPanels[currentPanel-2];
         String program = panel.text.getText();
         TSymbolTable newST = new TSymbolTable();
         TProgram newProg;
         try {
            newProg = parser.parse(program,newST);
         }
         catch (TError e) {
            prog = null;
            compiledPanel = -1;
            ST = null;
            panel.addErrorMessage("Compilation Error: " + e.getMessage(), e.pos);
            return;
         }
         panel.removeErrorMessage();
         ST = newST;
         prog = newProg;
         compiledPanel = currentPanel - 2;
         graphics.set(prog,ST,this);
         doShowGraphics();
         panelChoice.select(0);
         delayRunStart = true;
         graphics.runTheProgram(0);
      }
   }
   
   void doClear() {
     if (!running) {
        if (currentPanel == 0) {
           graphics.TH.ClearScreen();
           Object tr = graphics.TH.DrawTurtle(0,0,0);
           if (graphics.stack != null) {
              graphics.stack.turtleRef = tr;
              graphics.stack.turtleX = 0;
              graphics.stack.turtleY = 0;
              graphics.stack.turtleHeading = 0;
           }
        }
        else {
           EditPanel panel = editPanels[currentPanel-2];
           if (!panel.loading()) {
              panel.removeErrorMessage();
              panel.text.setText("");
           }
        }
     }
   }
   
   void doIndent() {
      if (currentPanel == 0)
         return;
      EditPanel panel = editPanels[currentPanel - 2];
      if (panel.loading())
         return;
      String old = panel.text.getText();
      if (old.trim().equals("")) {
         panel.text.setText("");
         return;
      }
      TTurtleIndent indent = new TTurtleIndent();
      String indented = indent.indent(old);
      panel.text.setText(indented);
   }
   
   void doLoad() {
      if (running || !canLoad || loadButton == null)
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
        if (currentPanel == 0)
           graphics.TH.errorReport(error,-1);
        else
           editPanels[currentPanel-2].addErrorMessage(error,-1);
        canLoad = false;
        loadButton.disable();
        return;
      }
      catch (RuntimeException re) { // illegal typecast, maybe?
        String error = "ERROR while trying to create a file dialog box.  It will not be possible to save files.";
        if (currentPanel == 0)
           graphics.TH.errorReport(error,-1);
        else
           editPanels[currentPanel-2].addErrorMessage(error,-1);
        canLoad = false;
        loadButton.disable();
        return;
      }
      String fileName = fd.getFile();
      if (fileName == null)
         return;
      String dir = fd.getDirectory();
      EditPanel panel = new EditPanel(fileName,getFont());
      if (editPanelCt >= editPanels.length) {
         EditPanel[] temp = new EditPanel[editPanels.length + 10];
         for (int i = 0; i < editPanels.length; i++)
            temp[i] = editPanels[i];
         editPanels = temp;
      }
      editPanels[editPanelCt] = panel;
      String name = String.valueOf(panelChoice.countItems());
      cardPanel.add(name,panel);
      panelChoice.addItem(fileName);
      int panelNum = panelChoice.countItems() - 1;
      doShowEditPanel(panelNum);
      panelChoice.select(currentPanel);
      editPanelCt++;
      panel.loadFromFile(dir,fileName);
   }
   
   void doSave() {
      if (running || currentPanel == 0 || saveButton == null)
         return;
      EditPanel panel = editPanels[currentPanel-2];
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
            panel.addErrorMessage("ERROR while trying to create a file dialog box.  It will not be possible to save files.",-1);
            canSave = false;
            saveButton.disable();
            return;
          }
          catch (RuntimeException re) {
            panel.addErrorMessage("ERROR while trying to create a file dialog box.  It will not be possible to save files.",-1);
            canSave = false;
            saveButton.disable();
            return;
          }
          fileName = fd.getFile();
          if (fileName == null)
             return;
          directory = fd.getDirectory();
          PrintStream out = new PrintStream(new FileOutputStream(new File(directory,fileName)));
          String contents = panel.text.getText();
          out.print(contents);
          out.close();
      }
      catch (IOException e) {
          panel.addErrorMessage("OUTPUT ERROR while trying to save to the file '" + fileName + "':  " + e.getMessage(), -1);
      }
      catch (SecurityException e) {
          panel.addErrorMessage("SECURITY ERROR while trying to save to the file '" + fileName + "':  " + e.getMessage(), -1);
      }
   }
   
   synchronized void doPause() {
         if (running && proc != null) {
            paused = !paused;
            if (paused)
               pauseButton.setLabel("Resume");
            else
               pauseButton.setLabel("Pause");
            proc.pause(paused);
         }
   }
   
   public boolean action(Event evt, Object arg) {
      if (evt.target == newButton)
         doNew();
      else if (evt.target == runButton)
         doRun();
      else if (evt.target == stopButton) {
         if (running)
            stopTheProgram();
      }
      else if (evt.target == pauseButton)
         doPause();
      else if (evt.target == clearButton)
         doClear();
      else if (evt.target == indentButton)
         doIndent();
      else if (loadButton != null && evt.target == loadButton)
         doLoad();
      else if (saveButton != null && evt.target == saveButton)
         doSave();
      else if (evt.target == panelChoice)
         doChoosePanel();
      else if (evt.target == speedChoice) {
         int i = speedChoice.getSelectedIndex();
         delay = speedList[i];
         synchronized(this) {
            if (proc != null)
               proc.setDelay(delay);
         }
      }
      else if (evt.target == showTurtleCheck) {
         boolean graphicsVisible = currentPanel == 0;
         graphics.TH.setAlwaysHideTurtles(showTurtleCheck.getState(), graphicsVisible);
      }
      else if (evt.target == randomSchedulingCheck) {
         randomScheduling = !randomSchedulingCheck.getState();
         synchronized(this) {
            if (proc != null)
               proc.setRandomScheduling(randomScheduling);
         }
      }
      else
         return super.handleEvent(evt);
/*      if (currentPanel == 0) {
         if (graphics.command.isEditable())
            graphics.command.requestFocus();
      }
      else {
         if (editPanels[currentPanel-2].text.isEditable())
            editPanels[currentPanel-2].text.requestFocus();
      } */
      return true;
   }
   
/*   public boolean gotFocus(Event evt, Object what) {
     if (currentPanel == 0) {
         if (graphics.TH.getWaitingForIO() >= 0)
            graphics.TH.requestFocus();
         else if (graphics.command.isEditable())
            graphics.command.requestFocus();
      }
      else {
         if (editPanels[currentPanel-2].text.isEditable())
            editPanels[currentPanel-2].text.requestFocus();
      }  
      return true;
  } */
   
}


