
package tmcm.xTuringMachine;
import java.awt.*;
import java.net.*;
import java.util.Vector;
import java.io.*;

class MachinePanel extends Panel implements Runnable {

   Machine TM;
   Button runButton, stepButton, clearTapeButton, deleteRuleButton,
            loadButton, saveButton, makeButton;
   boolean canLoad = true;
   boolean canSave = true;
   Choice speedChoice;
   int currentSpeed;
   Vector machineData;
   MachineData currentData = null;
   int currentMachineNumber = 0;
   RuleTable ruleTable;
   RuleMakerCanvas ruleMaker;
   Palette palette;
   
   boolean running;
   boolean halted;
   
   Thread timer;
   int timeToAlarm = 0;
   
   MachineLoader[] loaders;
   
   xTuringMachinePanel owner;
   
   MachinePanel(xTuringMachinePanel owner) {
      this.owner = owner;
      setUp();
      currentData = new MachineData();
      machineData.addElement(currentData);
      TM.setMachineData(currentData,0);
      ruleTable.setMachineData(currentData);
      ruleMaker.setMachineData(currentData);
   }
   
   MachinePanel(URL[] urlList, xTuringMachinePanel owner) {
      this.owner = owner;
      setUp();
      loaders = new MachineLoader[urlList.length];
      for (int loaderNumber = 0; loaderNumber < urlList.length; loaderNumber++) {
         machineData.addElement(null);
         loaders[loaderNumber] = new MachineLoader(urlList[loaderNumber],this,loaderNumber);
      }
      setControlsForLoading();
      TM.setMachineData(null,0);
      TM.setMessage("Loading from URL:", urlList[0].toString(), null);
      ruleTable.setMachineData(null);
      ruleMaker.setMachineData(null);
   }
   
   MachinePanel(String[] fileNameList, xTuringMachinePanel owner) {
      this.owner = owner;
      setUp();
      loaders = new MachineLoader[fileNameList.length];
      for (int loaderNumber = 0; loaderNumber < fileNameList.length; loaderNumber++) {
         machineData.addElement(null);
         loaders[loaderNumber] = new MachineLoader(null,fileNameList[loaderNumber],this,loaderNumber);
      }
      setControlsForLoading();
      TM.setMachineData(null,0);
      TM.setMessage("Loading from file:", fileNameList[0], null);
      ruleTable.setMachineData(null);
      ruleMaker.setMachineData(null);
   }
   
   private void setUp() {
      setLayout(new BorderLayout(5,5));
      setBackground(Color.lightGray);

      machineData = new Vector();
      runButton = new Button("Run");
      stepButton = new Button("Step");
      clearTapeButton = new Button("Clear Tape");
      deleteRuleButton = new Button("Delete Rule");
      deleteRuleButton.disable();
      loadButton = new Button("Load File");
      saveButton = new Button("Save");
      makeButton = new Button("Make Rule");
      speedChoice = new Choice();
      speedChoice.addItem("Fastest");
      speedChoice.addItem("Fast");
      speedChoice.addItem("Moderate");
      speedChoice.addItem("Slow");
      speedChoice.addItem("Slowest");
      speedChoice.select("Moderate");
      currentSpeed = 2;
      TM = new Machine(this);
      TM.speed = currentSpeed;
      machineData = new Vector();
      palette = new Palette(this);
      ruleTable = new RuleTable(this);
      ruleMaker = new RuleMakerCanvas(this);
      
      Panel controls = new Panel();
      controls.setLayout(new GridLayout(7,1,5,5));
      controls.add(speedChoice);
      controls.add(runButton);
      controls.add(stepButton);
      controls.add(clearTapeButton);
      controls.add(deleteRuleButton);
      controls.add(loadButton);
      controls.add(saveButton);
      
      Panel rm = new Panel();
      rm.setLayout(new BorderLayout(5,5));
      rm.add("Center",ruleMaker);
      rm.add("East",makeButton);
      Panel io = new Panel();
      io.setLayout(new GridLayout(2,1,5,5));
      io.add(palette);
      io.add(rm);
      Panel rules = new Panel();
      rules.setLayout(new BorderLayout(5,5));
      rules.add("North",io);
      rules.add("Center",ruleTable);
      
      add("North",TM);
      add("Center",rules);
      add("West",controls);
            
   }
   
   synchronized void doNewMachineCommand(int index) {
      TM.stopRunning();
      dropFocus(currentFocus);
      if (currentData != null)
         currentData.saveCurrentSquare = TM.currentSquare;
      else
         setNormalControls();
      currentData = new MachineData();
      machineData.addElement(currentData);
      currentMachineNumber = machineData.size() - 1;
      TM.setMachineData(currentData,0);
      ruleMaker.setMachineData(currentData);
      ruleTable.setMachineData(currentData);
   }
   
   synchronized void selectMachineNumber(int index) {
      TM.clearTemporaryMessage();
      if (index < 0 || index >= machineData.size() || index == currentMachineNumber)
         return;
      dropFocus(currentFocus);
      TM.stopRunning();
      if (currentData != null)
         currentData.saveCurrentSquare = TM.currentSquare;
      currentData = (MachineData)machineData.elementAt(index);
      if (currentData == null)
         setControlsForLoading();
      else
         setNormalControls();
      currentMachineNumber = index;
      if (currentData == null) {
         TM.setMachineData(null,0);
         ruleMaker.setMachineData(null);
         ruleTable.setMachineData(null);
         if (loaders[index].url != null)
             TM.setMessage("Error while oading from URL:",loaders[index].url.toString(),loaders[index].errorMessage);
         else
             TM.setMessage("Error while loading from file:",loaders[index].fileName,loaders[index].errorMessage);
      }
      else {
         TM.setMachineData(currentData,currentData.saveCurrentSquare);
         ruleMaker.setMachineData(currentData);
         ruleTable.setMachineData(currentData);
      }
   }
      
   // -------------------------------- focus and key-press processing ---------------
   
   
   static final int NOFOCUS = 0,
                    RULETABLEFOCUS = 1,
                    RULEMAKERFOCUS = 2,
                    MACHINEFOCUS = 3;
   int currentFocus = NOFOCUS;
   int currentPaletteDisplay = Palette.NONE;
   boolean focused;
   
   void requestFocus(int objectToFocus, int paletteDisplay) {
      TM.clearTemporaryMessage();
      if (objectToFocus != currentFocus) {
        switch (currentFocus) {
           case RULETABLEFOCUS:
              ruleTable.canvas.focusIsOff();
              break;
           case MACHINEFOCUS:
              TM.focusIsOff();
              break;
           case RULEMAKERFOCUS:
              ruleMaker.focusIsOff();
              break;
        }
      }
      currentFocus = objectToFocus;
      currentPaletteDisplay = paletteDisplay;
      if (focused && currentFocus != NOFOCUS) {
        palette.setDisplay(paletteDisplay);
        switch (currentFocus) {
           case RULETABLEFOCUS:
              ruleTable.canvas.focusIsOn();
              break;
           case MACHINEFOCUS:
              TM.focusIsOn();
              break;
           case RULEMAKERFOCUS:
              ruleMaker.focusIsOn();
              break;
        }
      }
      if (!focused)
         requestFocus();
   }
   
   void dropFocus(int objectToFocus) {
      if (objectToFocus == currentFocus) {
         currentPaletteDisplay = Palette.NONE;
         palette.setDisplay(Palette.NONE);
         switch (currentFocus) {
            case RULETABLEFOCUS:
               ruleTable.canvas.focusIsOff();
               break;
            case MACHINEFOCUS:
               TM.focusIsOff();
               break;
            case RULEMAKERFOCUS:
               ruleMaker.focusIsOff();
               break;
         }
         currentFocus = NOFOCUS;
      }
   }
   
   synchronized public boolean gotFocus(Event evt, Object what) {
      if (timeToAlarm > 0)
         setAlarm(0);
      else {
        focused = true;
        switch (currentFocus) {
           case RULETABLEFOCUS:
              ruleTable.canvas.focusIsOn();
              break;
           case MACHINEFOCUS:
              TM.focusIsOn();
              break;
           case RULEMAKERFOCUS:
              ruleMaker.focusIsOn();
              break;
        }
        if (currentFocus != NOFOCUS)
           palette.setDisplay(currentPaletteDisplay);
      }
      return true;
   }
   
   synchronized public boolean lostFocus(Event evt, Object what) {
      setAlarm(200);
      return true;
   }
   
   synchronized void alarm() {
      focused = false;
      switch (currentFocus) {
         case RULETABLEFOCUS:
            ruleTable.canvas.focusIsOff();
            break;
         case MACHINEFOCUS:
            TM.focusIsOff();
            break;
         case RULEMAKERFOCUS:
            ruleMaker.focusIsOff();
            break;
      }
      if (currentFocus != NOFOCUS)
         palette.setDisplay(Palette.NONE);
   }
   
   
   
   public boolean keyDown(Event evt, int key) {
      TM.clearTemporaryMessage();
      if (key == '\t') {
         switch (currentFocus) {
            case NOFOCUS:
            case MACHINEFOCUS:
               ruleMaker.selectedColumn = 0;
               requestFocus(RULEMAKERFOCUS,Palette.STATES);
               break;
            case RULETABLEFOCUS:
               if (running) {
                  ruleMaker.selectedColumn = 0;
                  requestFocus(RULEMAKERFOCUS,Palette.STATES);
               }
               else {
                  TM.selectItem(TM.currentSquare);
               }
               break;
            case RULEMAKERFOCUS:
               if (currentData != null && currentData.getRuleCount() > 0) {
                   if (ruleTable.canvas.selectedRule < 0)
                      ruleTable.canvas.select(0,2);
                   else
                      ruleTable.canvas.select(ruleTable.canvas.selectedRule,2);
                   requestFocus(RULETABLEFOCUS,Palette.STATESANDHALT);
               }
               else if (!running) {
                  TM.selectItem(TM.currentSquare);
               }
               break;
         }
      }
      else {
         switch (currentFocus) {
            case RULETABLEFOCUS:
               ruleTable.canvas.processKey(key);
               break;
            case MACHINEFOCUS:
               TM.processKey(key);
               break;
            case RULEMAKERFOCUS:
               ruleMaker.processKey(key);
               break;
         }
      }
      return true;
   }
   
   
   void doPaletteClick(int clickedItem) {
      switch (currentFocus) {
         case RULETABLEFOCUS:
            ruleTable.canvas.processPaletteClick(clickedItem,palette.display);
            break;
         case MACHINEFOCUS:
            TM.processPaletteClick(clickedItem,palette.display);
            break;
         case RULEMAKERFOCUS:
            ruleMaker.processPaletteClick(clickedItem,palette.display);
            break;
      }
   }
   
   synchronized void setAlarm(int time) {
      timeToAlarm = time;
      if (timer == null || !timer.isAlive() && time > 0) {
         timer = new Thread(this);
         timer.start();
      }
      else
         notify();
   }
         
   public void run() {
      while (true) {
         synchronized(this) {
            while (timeToAlarm <= 0)
               try { wait(); }
               catch (InterruptedException e) { }
         }
         synchronized(this) {
            try { wait(timeToAlarm); }
            catch (InterruptedException e) { }
         }
         synchronized(this) {
            if (timeToAlarm > 0) {
               timeToAlarm = 0;
               alarm();
            }
         }
      }
   }
   
   // -------------------------------------------------------------------------------
   
   void setControlsForLoading() {
      runButton.disable();
      stepButton.disable();
      clearTapeButton.setLabel("Clear");
      clearTapeButton.enable();
      deleteRuleButton.disable();
      saveButton.disable();
   }
   
   void setNormalControls() {
      runButton.enable();
      stepButton.enable();
      clearTapeButton.setLabel("Clear Tape");
      clearTapeButton.enable();
      if (canSave)
         saveButton.enable();
   }
   
   synchronized void loadingError(int loaderNumber) {
      if (loaders[loaderNumber] != null && loaderNumber == currentMachineNumber) {
         if (loaders[loaderNumber].url != null)
            TM.setMessage("Error while loading from URL:", loaders[loaderNumber].url.toString(), loaders[loaderNumber].errorMessage);
         else
            TM.setMessage("Error while loading from file:", loaders[loaderNumber].fileName, loaders[loaderNumber].errorMessage);
      }
   }
   
   synchronized void doneLoading(MachineData dataRead, int loaderNumber) {
      if (machineData.elementAt(loaderNumber) != null)
         return;
      machineData.setElementAt(dataRead,loaderNumber);
      if (loaderNumber == currentMachineNumber) {  // currentData must be null
         currentData = dataRead;
         TM.setMachineData(currentData,currentData.saveCurrentSquare);
         ruleTable.setMachineData(currentData);
         ruleMaker.setMachineData(currentData);
         setNormalControls();
      }
   }
   
   synchronized void abortLoad() {
      currentData = new MachineData();
      loaders[currentMachineNumber] = null;
      machineData.setElementAt(currentData,currentMachineNumber);
      TM.setMachineData(currentData,0);
      ruleMaker.setMachineData(currentData);
      ruleTable.setMachineData(currentData);
      setNormalControls();
   }
   
   
   void doLoad() {
      if (!canLoad)
         return;
      TM.stopRunning();
      FileDialog fd = null;
      try {
        Container c = this;  // find frame that contains this panel
        do {
           Container p = c.getParent();
           if (p == null)
              break;
           c = p; 
        } while (true);
        if (!(c instanceof Frame))
           c = null;
        fd = new FileDialog((Frame)c,"Select File to Load",FileDialog.LOAD);
        fd.show();
      }
      catch (AWTError e) {  // thrown by Netscape 3.0 on attempt to use file dialog
        TM.setTemporaryMessage("ERROR while trying to create a file dialog box.","It will not be possible to load files.",null);
        canLoad = false;
        loadButton.disable();
        return;
      }
      catch (RuntimeException re) { // illegal typecast, maybe?
        TM.setTemporaryMessage("ERROR while trying to create a file dialog box.","It will not be possible to load files.",null);
        canLoad = false;
        loadButton.disable();
        return;
      }
      String fileName = fd.getFile();
      if (fileName == null)
         return;
      String dir = fd.getDirectory();
      InputStream in = null;
      MachineData dataRead;
      try {
         in = new FileInputStream(new File(dir,fileName));
         dataRead = new MachineData();
         dataRead.read(in);
      }
      catch (MachineInputException e) {
         TM.setTemporaryMessage("LOAD FAILED:", e.getMessage(),null);
         dataRead = null;
      }
      catch (SecurityException e) {
         TM.setTemporaryMessage("LOAD FAILED, SECURITY ERROR:",e.getMessage(),null);
         dataRead = null;
      }
      catch (Exception e) {
         TM.setTemporaryMessage("LOAD FAILED, ERROR:", e.toString(), null);
         dataRead = null;
      }
      finally {
         if (in != null) {
            try { in.close(); }
            catch (IOException e) { }
         }
      }
      if (dataRead != null) 
        synchronized(this) {
           TM.stopRunning();
           dropFocus(currentFocus);
           owner.fileLoaded(fileName);
           machineData.addElement(dataRead);
           if (currentData == null)
              setNormalControls();
           else
              currentData.saveCurrentSquare = TM.currentSquare;
           currentData = dataRead;
           currentMachineNumber = machineData.size() - 1;
           TM.setMachineData(currentData,currentData.saveCurrentSquare);
           ruleMaker.setMachineData(currentData);
           ruleTable.setMachineData(currentData);
       }
   }
   
   void doSave() {
      if (currentData == null || !canSave)
         return;
      TM.stopRunning();
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
            if (!(c instanceof Frame))
               c = null;
            fd = new FileDialog((Frame)c,"Save as:",FileDialog.SAVE);
            fd.show();
          }
          catch (AWTError e) {  // thrown by Netscape 3.0 on attempt to use file dialog
            TM.setTemporaryMessage("ERROR while trying to create a file dialog box.","It will not be possible to save files.",null);
            canSave = false;
            saveButton.disable();
            return;
          }
          catch (RuntimeException re) {
            TM.setTemporaryMessage("ERROR while trying to create a file dialog box.","It will not be possible to save files.",null);
            canSave = false;
            saveButton.disable();
            return;
          }
          fileName = fd.getFile();
          if (fileName == null)
             return;
          directory = fd.getDirectory();
          PrintStream out = new PrintStream(new FileOutputStream(new File(directory,fileName)));
          currentData.write(out, TM.currentSquare);
          out.close();
          if (out.checkError())
             throw new IOException("Error occurred while writing data.");
      }
      catch (IOException e) {
          TM.setTemporaryMessage("OUTPUT ERROR", "while trying to save to the file \"" + fileName + "\":", e.getMessage());
      }
      catch (SecurityException e) {
          TM.setTemporaryMessage("SECURITY ERROR", "while trying to save to the file \"" + fileName + "\":", e.getMessage());
      }
   }
   
   


   // --------------------------------------------------------------------------------
   
   
   

   void doMakeRule() {
     if (currentData == null)
        return;
     if (ruleMaker.state != MachineData.UNSPECIFIED) {
        boolean newRule = !(currentData.ruleDefined(ruleMaker.state,ruleMaker.symbol));
        currentData.setActionData(ruleMaker.state,ruleMaker.symbol,
                                    ruleMaker.newSymbol,ruleMaker.direction,ruleMaker.newState);
        if (newRule)
           ruleTable.ruleAdded(ruleMaker.state,ruleMaker.symbol);
        else
           ruleTable.ruleChanged(ruleMaker.state,ruleMaker.symbol);
        if (TM.getStatus() == Machine.NORULE && currentData.getNewState(TM.machineState,currentData.getTape(TM.machineState)) != MachineData.UNSPECIFIED) {
           TM.setChangedAll();
           TM.setStatus(Machine.IDLE);
           TM.repaint();
        }
        ruleMaker.ruleMade();
     }
   }
   

   synchronized void doneRunning(boolean inHaltState) {  // called from TM
      runButton.setLabel("Run");
      if (inHaltState)
         stepButton.setLabel("Reset");
      stepButton.enable();
      halted = inHaltState;
      runButton.enable();
      clearTapeButton.enable();
      running = false;
   }
   
   synchronized void doneStep(boolean inHaltState) {
      runButton.enable();
      if (inHaltState)
         stepButton.setLabel("Reset");
      stepButton.enable();
      clearTapeButton.enable();
      halted = inHaltState;
   }
   
   synchronized public boolean action(Event evt, Object arg) {
      TM.clearTemporaryMessage();
      if (evt.target == makeButton)
         doMakeRule();
      else if (evt.target == runButton) {
         if (currentData == null)
            return true;
         if (running) {
            runButton.disable();
            TM.stopRunning();
         }
         else {
            TM.startRunning();
            running = true;
            clearTapeButton.disable();
            stepButton.disable();
            if (halted) {
               halted = false;
               stepButton.setLabel("Step");
            }
            runButton.setLabel("Stop");
         }
      }
      else if (evt.target == stepButton) {
         if (currentData == null)
            return true;
         if (!running) {
            if (halted) {
               TM.reset();
               stepButton.setLabel("Step");
               halted = false;
            }
            else {
              stepButton.disable();
              runButton.disable();
              clearTapeButton.disable();
              TM.doStep();
            }
         }
      }
      else if (evt.target == clearTapeButton) {
          synchronized(this) {
            if (currentData != null)
               TM.clearTape();
            else
               abortLoad();
          }             
      }
      else if (evt.target == deleteRuleButton) {
         if (currentData != null)
            ruleTable.doDeleteRule();
      }
      else if (evt.target == loadButton) {
         doLoad();
      }
      else if (evt.target == saveButton) {
         doSave();
      }
      else if (evt.target == speedChoice) {
          int newSpeed = speedChoice.getSelectedIndex();
          if (newSpeed != currentSpeed) {
             TM.setSpeed(newSpeed);
             currentSpeed = newSpeed;
          }
      }
      else
         return super.action(evt,arg);
      return true;
   }
   
   
}
