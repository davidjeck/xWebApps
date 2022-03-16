

package tmcm.xModels;

import java.awt.*;
import java.net.URL;
import java.io.*;

public class xModelsPanel extends Panel {
   
   ProgramPanel programPanel;
   Panel graphicsPanel;
   xModelsCanvas canvas;
   Button newBttn, showProgramBttn;
   Choice panelChoice, speedChoice, loopChoice;
   Panel mainPanel;
   CardLayout mainLayout;
   Button renderButton, loadButton, clearButton, saveButton;
   Label frameNumber;
   
   Model saveModel;  // for saving model between stopApplet and startApplet
   
   int untitledCount;
   boolean graphicsVisible;
   
   boolean canLoad = true, canSave = true;
   
   public xModelsPanel() {
      init();
      panelChoice.addItem("Untitled 1");
      panelChoice.select(2);
      programPanel.newProgram();
      untitledCount = 1;
   }
   
   public xModelsPanel(URL[] urlList, String[] nameList) {
      init();
      for (int i = 0; i < urlList.length; i++) {
         panelChoice.addItem(nameList[i]);
         programPanel.loadURL(urlList[i]);
      }
      panelChoice.select(2);
   }
   
   public xModelsPanel(String[] nameList) {
      init();
      for (int i = 0; i < nameList.length; i++) {
         panelChoice.addItem(nameList[i]);
         programPanel.loadFile(null,nameList[i],true);
      }
      panelChoice.select(2);
   }
   
   private void init() {
   
      setBackground(Color.lightGray);
      graphicsVisible = false;
      
      setLayout(new BorderLayout(6,6));
      
      panelChoice = new Choice();
      panelChoice.addItem("Graphics");
      panelChoice.addItem("[New]");
      add("North",panelChoice);
      mainPanel = new Panel();
      add("Center",mainPanel);
      
      mainLayout = new CardLayout();
      mainPanel.setLayout( mainLayout );
      
      programPanel = new ProgramPanel();
      Panel tempPanel = new Panel();
      tempPanel.setLayout( new BorderLayout(6,6));
      tempPanel.add("Center",programPanel);
      Panel bot = new Panel();
      tempPanel.add("South",bot);
      
      renderButton = new Button("RENDER!");
      bot.add(renderButton);
      loadButton = new Button("Load File");
      bot.add(loadButton);
      saveButton = new Button("Save To File");
      bot.add(saveButton);
      clearButton = new Button("Clear");
      bot.add(clearButton);
            
      mainPanel.add("P", tempPanel);
      graphicsPanel = new Panel();
      mainPanel.add("G", graphicsPanel);
      
      graphicsPanel.setLayout( new BorderLayout(6,6) );
      canvas = new xModelsCanvas();
      graphicsPanel.add("Center",canvas);      
      Panel right = new Panel();
      graphicsPanel.add("East",right);
      
      right.setLayout( new GridLayout(9,1,5,5) );
      canvas.frameNumber = new Label("xModels");
      frameNumber = canvas.frameNumber;
      right.add(canvas.frameNumber);
      newBttn = new Button("New Program");
      right.add(newBttn);
      showProgramBttn = new Button("Show Program");
      right.add(showProgramBttn);
      canvas.goBttn = new Button("Go");
      right.add(canvas.goBttn);
      canvas.pauseBttn = new Button("Pause");
      right.add(canvas.pauseBttn);
      canvas.nextBttn = new Button("Next Frame");
      right.add(canvas.nextBttn);
      canvas.previousBttn = new Button("Previous Frame");
      right.add(canvas.previousBttn);      
      
      speedChoice = new Choice();
      right.add(speedChoice);
      speedChoice.addItem("30 frames/second");
      speedChoice.addItem("20 frames/second");
      speedChoice.addItem("10 frames/second");
      speedChoice.addItem(" 5 frames/second");
      speedChoice.addItem(" 2 frames/second");
      speedChoice.addItem(" 1 frame/second");
      speedChoice.select(2);
      
      loopChoice = new Choice();
      right.add(loopChoice);
      loopChoice.addItem("Loop");
      loopChoice.addItem("Back-and-Forth");
      loopChoice.addItem("Once Through");
      loopChoice.select(0);
      
   }
   
   public void stopApplet() {
      if (graphicsVisible) {
         saveModel = canvas.model;
         canvas.setModel(null);
      }
   }
   
   public void startApplet() {
      if (graphicsVisible && saveModel != null) {
         canvas.setModel(saveModel);
         saveModel = null;
      }
   }
   
   public Insets insets() {
      return new Insets(6,6,6,6);
   }

/*   public boolean gotFocus(Event evt, Object what) {
      programPanel.text.requestFocus();
      return true;
   }
 */
   
   void showProgramPanel(int programNumber) {
      if (programNumber != programPanel.getCurrentProgramNumber())
         programPanel.selectProgram(programNumber);
      if (graphicsVisible) {
         canvas.setModel(null);
         mainLayout.show(mainPanel,"P");
         graphicsVisible = false;
      }
   //   programPanel.text.requestFocus();
   }
   
   void showGraphics() {
       if (graphicsVisible)
          return;
       mainLayout.show(mainPanel,"G");
       graphicsVisible = true;
       programPanel.removeErrorMessage();
   }
   
   void doPanelChoice() {
      int item = panelChoice.getSelectedIndex();
      if (item == 0) {
         if (!doRender())
            panelChoice.select(programPanel.getCurrentProgramNumber() + 2);
      }
      else if (item > 1)
         showProgramPanel(item-2);
      else
         doNewProgram();
   }
   
   void doNewProgram() {
      untitledCount++;
      panelChoice.addItem("Untitled " + untitledCount);
      panelChoice.select(panelChoice.countItems() - 1);
      programPanel.newProgram();
      showProgramPanel(panelChoice.getSelectedIndex() - 2);
   }
   
   
   void doSpeedChoice() {
      int item = speedChoice.getSelectedIndex();
      switch (item) {
         case 0:
            canvas.setTimePerFrame(33);
            break;
         case 1:
            canvas.setTimePerFrame(50);
            break;
         case 2:
            canvas.setTimePerFrame(100);
            break;
         case 3:
            canvas.setTimePerFrame(200);
            break;
         case 4:
            canvas.setTimePerFrame(500);
            break;
         case 5:
            canvas.setTimePerFrame(1000);
            break;
      }
   }
   
   void doLoopChoice() {
       int item = loopChoice.getSelectedIndex();
      switch (item) {
         case 0:
            canvas.setLoopStyle(canvas.doLoop);
            break;
         case 1:
            canvas.setLoopStyle(canvas.doBackAndForth);
            break;
         case 2:
            canvas.setLoopStyle(canvas.doOnceThrough);
            break;
      }
   }
   
   boolean doRender() {  // returns success/failure
      String prog = programPanel.getCurrentContents();
      Parser parser;
      try {
         parser = new Parser(prog);
      }
      catch (ParseError e) {
         programPanel.addErrorMessage("Error found in program:  " + e.getMessage(), e.errorPosition);
//         programPanel.text.requestFocus();
         return false;
      }
      Model model = parser.getModel();
      panelChoice.select(0);
      showGraphics();
      canvas.setModel(model);
      return true;
   }
   
   void doLoad() {
      if (graphicsVisible || !canLoad || loadButton == null)
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
        programPanel.addErrorMessage(error,-1);
        canLoad = false;
        loadButton.disable();
        return;
      }
      catch (RuntimeException re) { // illegal typecast, maybe?
        String error = "ERROR while trying to create a file dialog box.  It will not be possible to save files.";
        programPanel.addErrorMessage(error,-1);
        canLoad = false;
        loadButton.disable();
        return;
      }
      String fileName = fd.getFile();
      if (fileName == null)
         return;
      String dir = fd.getDirectory();
      panelChoice.addItem(fileName);
      panelChoice.select(panelChoice.countItems() - 1);
      programPanel.loadFile(dir,fileName,false);
   }
   
   void doSave() {
      if (graphicsVisible || !canSave || saveButton == null)
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
            programPanel.addErrorMessage("ERROR while trying to create a file dialog box.  It will not be possible to save files.",-1);
            canSave = false;
            saveButton.disable();
            return;
          }
          catch (RuntimeException re) {
            programPanel.addErrorMessage("ERROR while trying to create a file dialog box.  It will not be possible to save files.",-1);
            canSave = false;
            saveButton.disable();
            return;
          }
          fileName = fd.getFile();
          if (fileName == null)
             return;
          directory = fd.getDirectory();
          PrintStream out = new PrintStream(new FileOutputStream(new File(directory,fileName)));
          String contents = programPanel.getCurrentContents();
          out.print(contents);
          out.close();
      }
      catch (IOException e) {
          programPanel.addErrorMessage("OUTPUT ERROR while trying to save to the file '" + fileName + "':  " + e.getMessage(), -1);
      }
      catch (SecurityException e) {
          programPanel.addErrorMessage("SECURITY ERROR while trying to save to the file '" + fileName + "':  " + e.getMessage(), -1);
      }
   }
   
   
   public boolean action(Event evt, Object obj) {
      if (evt.target instanceof Button) {
         if (evt.target == canvas.goBttn) {
             if (canvas.getState() == canvas.paused)
               canvas.setState(canvas.continueAnimation);
         }
         else if (evt.target == canvas.pauseBttn) {
             canvas.setState(canvas.stopAnimation);
         }
         else if (evt.target == canvas.nextBttn) {
             if (canvas.getState() == canvas.paused)
               canvas.setState(canvas.nextFrame);
         }
         else if (evt.target == canvas.previousBttn) {
             if (canvas.getState() == canvas.paused)
               canvas.setState(canvas.prevFrame);
         }
         else if (evt.target == newBttn)
            doNewProgram();
         else if (evt.target == showProgramBttn) {
            int prog = programPanel.getCurrentProgramNumber();
            panelChoice.select(prog+2);
            showProgramPanel(prog);
         }
         else if (evt.target == renderButton)
            doRender();
         else if (evt.target == loadButton)
            doLoad();
         else if (evt.target == saveButton)
            doSave();
         else if (evt.target == clearButton)
            programPanel.doClear();
//         if (!graphicsVisible)
//            programPanel.text.requestFocus();
         return true;
      }
      if (evt.target instanceof Choice)  {
         if (evt.target == panelChoice)
            doPanelChoice();
         else if (evt.target == speedChoice)
            doSpeedChoice();
         else if (evt.target == loopChoice)
            doLoopChoice();
         return true;
      }
      return super.action(evt,obj);
   }
   
   public boolean mouseDown(Event evt, int x, int y) {
      if (evt.target == frameNumber) {
         if (canvas.frameNumber == null) {
            frameNumber.setText("");
            canvas.frameNumber = frameNumber;
         }
         else {
            canvas.frameNumber = null;
            frameNumber.setText("xModels");
         }
         return true;
      }
      else
         return super.mouseDown(evt,x,y);
   }
   

}