package tmcm.xTuringMachine;
import java.awt.*;

class Machine extends Panel implements Runnable {

   static final Color backgroundColor = new Color(255,240,220);
   static final Color tapeColor = new Color(60,50,40);
   static final Color machineColor = tapeColor;
   static final Color insideMachineColor = Color.white;
   static final Color symbolColor = Color.blue;
   static final Color stateColor = Color.red;
   
   int squareSize;
   int machineWidth,machineHeight;
   Font tapeFont;
   int charWidth;
   int baseLine;
   int machineBaseline;
   int tapeTop;
   Font machineFont;
   FontMetrics machineFM,tapeFM;
   
   Rectangle changedRect = new Rectangle();
   
   static final int NOSELECTION = 1000000000;
   static final int MACHINESELECTED = -NOSELECTION;
   int selectedItem = NOSELECTION;  // if a square is selected, this is the square number
   
   int width = -1;
   int height;
   
   Image OSC;
   Graphics OSG;

   MachineData data;
   
   String[] message = new String[3];
   boolean temporaryMessage;
   
   int centerSquare;
   double squaresVisible;
   int currentSquare;
   
   int machineState;
   int speed;  // initialized by MachinePanel.class
   int[] speedDelay = { 20, 100, 300, 500, 1500 };
   boolean blink;
   
   Thread runner;
   
   final static int IDLE = 0,
                    NORULE = 1,
                    STOPPING = 2,
                    RUNNING = 3,
                    STEPPING = 4,
                    MESSAGEDISPLAY = 5;
   private int status = IDLE;
                    
   
   
   MachinePanel owner;
   
   boolean focused;
   
   boolean changed;
   
   Machine(MachinePanel owner) {
      setBackground(backgroundColor);
      this.owner = owner;
   }
   
   public Dimension preferredSize() {
      return new Dimension(500,76);
   }
   
   public Dimension minimumSize() {
      return new Dimension(100,76);
   }
   
   synchronized void setMachineData(MachineData data, int currentSquare) {
      if (status == RUNNING)
          stopRunning();
      this.data = data;
      centerSquare = currentSquare;
      machineState = 0;
      this.currentSquare = currentSquare;
      selectedItem = NOSELECTION;
      status = IDLE;
      changed = true;
      clearMessage();
      setChangedAll();
      repaint();
   }
   
   synchronized void setMessage(String m1, String m2, String m3) {
      stopRunning();
      message[0] = m1;
      message[1] = m2;
      message[2] = m3;  
      temporaryMessage = false;
      setStatus(MESSAGEDISPLAY);
      repaint();
   }
   
   synchronized void clearMessage() {
      if (status == MESSAGEDISPLAY) {
        message[0] = message[1] = message[2] = null;
        temporaryMessage = false;
        setStatus(IDLE);
        repaint();
      }
   }
   
   synchronized void clearTemporaryMessage() {
      if (temporaryMessage && status == MESSAGEDISPLAY) {
        message[0] = message[1] = message[2] = null;
        temporaryMessage = false;
        setStatus(IDLE);
        repaint();
      }
   }
   
   synchronized void setTemporaryMessage(String m1, String m2, String m3) {
      stopRunning();
      selectedItem = NOSELECTION;
      owner.dropFocus(MachinePanel.MACHINEFOCUS);
      message[0] = m1;
      message[1] = m2;
      message[2] = m3;  
      temporaryMessage = true;
      setStatus(MESSAGEDISPLAY);
      repaint();
   }
   
   
   synchronized public void paint(Graphics g) {
      if (tapeFont == null || width != size().width || height != size().height)
         setUp();
      if (status == MESSAGEDISPLAY) {
         g.setColor(backgroundColor);
         g.fillRect(0,0,size().width,size().height);
         g.setColor(tapeColor);
         g.drawString(message[0],10,10+tapeFM.getAscent());
         if (message[1] != null)
            g.drawString(message[1],10,10+tapeFM.getAscent()+tapeFM.getHeight());
         if (message[2] != null)
            g.drawString(message[2],10,10+tapeFM.getAscent()+2*tapeFM.getHeight());            
         return;
      }
      if (OSC == null) {
         setChangedAll();
         doDraw(g);
      }
      else {
         if (changed)
            doDraw(OSG);
         g.drawImage(OSC,0,0,this);
      }  
   }
   
   public void update(Graphics g) {
      paint(g);
   }
   
   void setUp() {
      width = size().width;
      height = size().height;
      tapeFont = getFont();
      tapeFM = getFontMetrics(tapeFont);
      charWidth = tapeFM.charWidth('9');
      squareSize = tapeFM.getAscent() + tapeFM.getDescent() + 10;
      tapeTop = height - squareSize - 3;
      baseLine = tapeTop + (squareSize + tapeFM.getAscent() - tapeFM.getDescent()) / 2;
      machineHeight = tapeTop - 12;
      machineFont = new Font(tapeFont.getName(),Font.PLAIN,24);
      machineFM = getFontMetrics(machineFont);
      if (machineFM.getAscent() + machineFM.getDescent() + 14 > machineHeight) {
        machineFont = new Font(tapeFont.getName(),Font.PLAIN,18);
        machineFM = getFontMetrics(machineFont);
        if (machineFM.getAscent() + machineFM.getDescent() + 14 > machineHeight) {
          machineFont = new Font(tapeFont.getName(),Font.PLAIN,14);
          machineFM = getFontMetrics(machineFont);
          if (machineFM.getAscent() + machineFM.getDescent() + 14 > machineHeight) {
             machineFont = new Font(tapeFont.getName(),Font.PLAIN,12);
             machineFM = getFontMetrics(machineFont);
          }          
        }
      }
      machineWidth = machineFM.stringWidth("99") + 18;
      if (machineWidth < 4*machineHeight/5)
         machineWidth = 4*machineHeight/5;
      machineBaseline = 6 + (machineHeight + machineFM.getAscent() - machineFM.getDescent())/2;
      try { 
        OSC = createImage(width,height);
        OSG = OSC.getGraphics();
      }
      catch (OutOfMemoryError e) {
        OSC = null;
        OSG = null;
      }
      setChangedAll();
   }   
   
   void doDraw(Graphics g) {
      g.setColor(backgroundColor);
      g.fillRect(changedRect.x, changedRect.y, changedRect.width, changedRect.height);
      g.setColor(tapeColor);
      g.drawLine(0,tapeTop,width,tapeTop);
      g.drawLine(0,height-3,width,height-3);
      if (changedRect.y + changedRect.height > tapeTop) {
         int leftSquare = centerSquare - (width/2 - changedRect.x)/squareSize - 1;
         int rightSquare = centerSquare + (changedRect.x + changedRect.width - width/2)/squareSize + 1;
         int x = width/2 - (centerSquare - leftSquare)*squareSize - (squareSize+1)/2;
         g.drawLine(x,tapeTop,x,height-4);
         g.setFont(tapeFont);
         for (int square = leftSquare; square <= rightSquare; square++) {
            char sym = (data == null)? '#' : data.getTape(square);
            if (blink && square == currentSquare)
                g.fillRect(x,tapeTop,squareSize,squareSize);
            if (sym != '#') {
               g.setColor(symbolColor);
               g.drawString(String.valueOf(sym),x + (squareSize-charWidth)/2,baseLine);
               g.setColor(tapeColor);
            }               
            x += squareSize;
            g.drawLine(x,tapeTop,x,height-4);
         }
         if (focused && selectedItem != NOSELECTION && selectedItem != MACHINESELECTED) {
            g.setColor(Palette.hiliteColor);
            x = (width-squareSize)/2 - (centerSquare - selectedItem)*squareSize;
            g.drawRect(x,tapeTop,squareSize+1,squareSize+1);
            g.drawRect(x+1,tapeTop+1, squareSize-1,squareSize-1);
         }
      }
      if (status == RUNNING && speed == 0) {
         g.setColor(machineColor);
         int center = width/2 - (centerSquare - currentSquare)*squareSize;
         g.fillRect(center-squareSize/2+1,tapeTop-3,squareSize-1,3);
      }      
      else if (changedRect.y < tapeTop) {
         int pos = (width-machineWidth)/2 - (centerSquare - currentSquare)*squareSize;
         int center = width/2 - (centerSquare - currentSquare)*squareSize;
         if (changedRect.x <= pos + machineWidth || changedRect.x + changedRect.width >= pos) {
            g.setColor(machineColor);
            g.fillRect(center-squareSize/2+1,tapeTop-3,squareSize-1,3);
            g.fillRect(center-2,tapeTop-6,4,4);
            g.fillRoundRect(pos,6,machineWidth,machineHeight,12,12);
            if (focused && selectedItem == MACHINESELECTED) {
               g.setColor(Palette.hiliteColor);
               g.fillRoundRect(pos+3,9,machineWidth-6,machineHeight-6,12,12);
               g.setColor(insideMachineColor);
               g.fillRoundRect(pos+5,11,machineWidth-10,machineHeight-10,12,12);
            }
            else {
               g.setColor(insideMachineColor);
               g.fillRoundRect(pos+3,9,machineWidth-6,machineHeight-6,12,12);
            }
            String str = (machineState == MachineData.HALTSTATE)? "h" : String.valueOf(machineState);
            g.setColor(stateColor);
            g.setFont(machineFont);
            g.drawString(str,center-machineFM.stringWidth(str)/2,machineBaseline);
         }
      }
      if (status == NORULE) {
         String str = "No Rule Defined!";
         int wd = machineFM.stringWidth(str);
         if (wd + 10 <= (width-machineWidth/2))
            g.setFont(machineFont);
         else {
            g.setFont(tapeFont);
            wd = tapeFM.stringWidth(str);
         }
         int pos;
         if (currentSquare <= centerSquare)
            pos = (width+machineWidth)/2 - (centerSquare-currentSquare)*squareSize + 10;
         else
            pos = (width-machineWidth)/2 - (centerSquare-currentSquare)*squareSize - wd - 10;
         g.setColor(stateColor);
         g.drawString(str,pos,machineBaseline);
      }
      changed = false;
      changedRect.reshape(0,0,0,0);
   }
   
   synchronized void setBlinking(boolean blink) {
      this.blink = blink;
   }
   
   synchronized void setChangedAll() {
      changedRect.reshape(0,0,width,height);
      changed = true;
   }
   
   synchronized void setChanged(int x, int y, int wd, int ht) {
      if (changedRect.isEmpty())
         changedRect.reshape(x,y,wd,ht);
      else {
         changedRect.add(x,y);
         changedRect.add(x+wd,y+ht);
      }
      changed = true;
   }
   
   synchronized void setStatus(int status) {
      this.status = status;
      notify();
   }
   
   synchronized int getStatus() {
      return status;
   }
   
   // ----------------------------------------------------------------------------------
   
   
   
   
   // ------------------------------ focus stuff ----------------------------------------
   
   boolean firstKeyDownInSelection;
   
   synchronized void focusIsOn() {
      focused = true;
      repaintSelection();
      firstKeyDownInSelection = true;
   }
   
   synchronized void focusIsOff() {
       focused = false;
       repaintSelection();
   }
   
   synchronized void processKey(int key) {
      if (key >= ' ' && key <= 127) {
        if (key == ' ')
           key = '#';
        else
           key = Character.toLowerCase((char)key);
        if (selectedItem == MACHINESELECTED) {
           if (key == 'h') {
              if (machineState != MachineData.HALTSTATE)
                 owner.stepButton.setLabel("Reset");
              machineState = MachineData.HALTSTATE;
              repaintSelection();
           }
           else if (key <= '9' && key >= '0') {
              if (machineState == MachineData.HALTSTATE)
                 owner.stepButton.setLabel("Step");
              if (machineState == MachineData.HALTSTATE || firstKeyDownInSelection)
                 machineState = key - '0';
              else
                 machineState = 10*machineState + key - '0';
              if (machineState >= MachineData.STATES)
                 machineState = key - '0';
              repaintSelection();
              firstKeyDownInSelection = false;
           }
        }
        else if (selectedItem != NOSELECTION) {
          if (key == 'i')
             key = '1';
          else if (key == 'o')
             key = '0';
          if (key != '*' && MachineData.symbolNames.indexOf((char)key) >= 0 && data != null) {
             data.setTape(selectedItem,(char)key);
             selectItem(selectedItem + 1);
          }
        }
      }
      else if (key == Event.LEFT) {
         if (Math.abs(selectedItem) < NOSELECTION)
            selectItem(selectedItem - 1);
      }
      else if (key == Event.RIGHT) {
         if (Math.abs(selectedItem) < NOSELECTION)
            selectItem(selectedItem + 1);
      }
      else if (key == Event.UP || key == Event.DOWN) {
         if (selectedItem == MACHINESELECTED)
            selectItem(currentSquare);
         else
            selectItem(MACHINESELECTED);
      }
      else if (key == Event.HOME) {
         if (data != null)
            selectItem(data.firstFilledSquare());
      }
      else if (key == Event.END) {
         if (data != null)
            selectItem(data.lastFilledSquare());
      }
      
   }
   
   synchronized void processPaletteClick(int itemClicked, int paletteDisplay) {
      firstKeyDownInSelection = true;
      if (paletteDisplay == Palette.SYMBOLS)
         processKey( MachineData.symbolNames.charAt(itemClicked) );
      else if (paletteDisplay == Palette.STATESANDHALT && selectedItem == MACHINESELECTED) {
         int state;
         if (itemClicked == -1) {
            if (machineState != MachineData.HALTSTATE)
               owner.stepButton.setLabel("Reset");
            state = MachineData.HALTSTATE;
         }
         else {
            if (machineState == MachineData.HALTSTATE)
               owner.stepButton.setLabel("Step");
            state = itemClicked;
         }
         if (state == machineState)
            return;
         machineState = state;
         repaintSelection();
      }
   }
   
   synchronized void selectItem(int item) {
      if (status == NORULE) {
         setStatus(IDLE);
         setChanged(0,0,width,tapeTop);
         repaint(0,0,width,tapeTop);
      }
      if (item != selectedItem && focused) {
         repaintSelection();
         firstKeyDownInSelection = true;
      }
      selectedItem = item;
      if (item == MACHINESELECTED) {
         owner.requestFocus(MachinePanel.MACHINEFOCUS,Palette.STATESANDHALT);
      }
      else if (item != NOSELECTION)
         owner.requestFocus(MachinePanel.MACHINEFOCUS,Palette.SYMBOLS);
   }
   
   void repaintSelection() {
      if (selectedItem == MACHINESELECTED) {
         int x = (width-squareSize)/2 - (centerSquare-currentSquare)*squareSize;
         if (x < 0 || x+squareSize > width)
            showSquare(currentSquare);
         else { 
           int machineLeft = (width-machineWidth)/2 - (centerSquare-currentSquare)*squareSize-2;
           setChanged(machineLeft,3,machineLeft+machineWidth+10, tapeTop-3);
           repaint(machineLeft,3,machineLeft+machineWidth+10, tapeTop-3);
         }
      }
      else if (selectedItem != NOSELECTION) {
         int x = (width-squareSize)/2 - (centerSquare-selectedItem)*squareSize;
         if (x < 0 || x+squareSize > width)
            showSquare(selectedItem);
         else {
            setChanged(x-1,tapeTop,squareSize+3,squareSize+2);
            repaint(x-1,tapeTop,squareSize+3,squareSize+2);
         }
      }
   }
   
   synchronized void showSquare(int square) {
      int x = (width - squareSize)/2 - (centerSquare-square)*squareSize;
      if (x < 0) {
         centerSquare = square + (width/squareSize)/4;
         setChangedAll();
         repaint();
      }
      else if (x + squareSize > width) {
         centerSquare = square - (width/squareSize)/4;
         setChangedAll();
         repaint();
      }
   }
   
   // -----------------------------------------------------------------------------------

   int start_x, start_y, startCurrentSquare, startCenterSquare;
   boolean shiftedClick;
   boolean dragging;

   public boolean mouseDown(Event evt, int x, int y) {
      if (temporaryMessage && message[0] != null) {
         clearTemporaryMessage();
         return true;
      }
      if (data == null)
         return true;
      if (getStatus() > NORULE) {
         dragging = false;
         return true;
      }
      else if (getStatus() == NORULE) {
         setStatus(IDLE);
         setChanged(0,0,width,tapeTop);
         repaint();
      }
      start_x = x; start_y = y;
      startCurrentSquare = currentSquare;
      startCenterSquare = centerSquare;
      shiftedClick = evt.metaDown() || evt.controlDown();
      int machineLeft = (width-machineWidth)/2 - (centerSquare-currentSquare)*squareSize;
      if (!shiftedClick) {
         if (y >= tapeTop) {
            int offset = x - width/2;
            if (offset > 0)
               selectItem( centerSquare + (offset+squareSize/2)/squareSize );
            else
               selectItem( centerSquare - (-offset+squareSize/2)/squareSize );
         }
         else if (x > machineLeft - 5 && x < machineLeft+machineWidth+5)
            selectItem(MACHINESELECTED);
      }
      dragging = (shiftedClick || y >= tapeTop || (x > machineLeft - 5 && x < machineLeft+machineWidth+5));
      return true;
   }
   
   public boolean mouseDrag(Event evt, int x, int y) {
      if (!dragging)
         return true;
      int newSquare;
      int offset = x - start_x;
      if (offset > 0)
         offset = (offset + squareSize/2)/squareSize;
      else
         offset = -((-offset + squareSize/2)/squareSize);
      if (shiftedClick) { // drag everything
         centerSquare = startCenterSquare - offset;
      }
      else if (start_y >= tapeTop) { // drag tape
         centerSquare = startCenterSquare - offset;
         currentSquare = startCurrentSquare - offset;
      }
      else { // drag machine
         currentSquare = startCurrentSquare + offset;
      }
      setChangedAll();
      repaint();
      return true;
   }

   // -----------------------------------------------------------------------------------



   synchronized void setSpeed(int speed) {
      if (status == RUNNING && (speed == 0 || this.speed == 0)) {
         setChanged(0,0,width,tapeTop);
         repaint();
      }
      this.speed = speed;
      notify();
   }
   
   synchronized int getSpeed() {
      return speed;
   }
   
   synchronized void startRunning() {
      if (status == NORULE) {
         setChanged(0,0,width,tapeTop);
         setStatus(IDLE);
         repaint();
         try { wait(100); }
         catch (InterruptedException e) { }
      }
      if (data == null || status == MESSAGEDISPLAY) {
         owner.doneRunning(false);
         return;
      }
      owner.dropFocus(MachinePanel.MACHINEFOCUS);
      status = RUNNING;
      if (speed == 0) {
         setChanged(0,0,width,tapeTop);
         repaint(0,0,width,tapeTop);
      }
      if (runner == null || !runner.isAlive()) {
         runner = new Thread(this);
         runner.start();
      }
      else
         notify();
   }
   
   synchronized void doStep() {
      if (data == null || status == MESSAGEDISPLAY) {
         owner.doneStep(false);
         return;
      }
      if (status == NORULE) {
         setChanged(0,0,width,tapeTop);
         repaint();
         setStatus(IDLE);
         try { wait(100); }
         catch (InterruptedException e) { }
      }
      owner.dropFocus(MachinePanel.MACHINEFOCUS);
      status = STEPPING;
      if (runner == null || !runner.isAlive()) {
         runner = new Thread(this);
         runner.start();
      }
      else
         notify();
   }
   
   synchronized void stopRunning() {
      if (status != RUNNING)
         return;
      setStatus(STOPPING);
      try { wait(1000); }
      catch (InterruptedException e) { }
      setStatus(IDLE);
      setChangedAll();
      repaint();
      owner.doneRunning(machineState == MachineData.HALTSTATE);
   }
   
   synchronized void reset() {
      if (status == NORULE) {
         setChanged(0,0,width,tapeTop);
         repaint();
         setStatus(IDLE);
      }
      machineState = 0;
      setChanged(0,0,width,tapeTop);
      repaint();
   }
   
   synchronized void clearTape() {
      if (data != null)
         data.clearTape();
      setChangedAll();
      currentSquare = 0;
      centerSquare = 0;
      if (status == NORULE)
         setStatus(IDLE);
      if (selectedItem != NOSELECTION && selectedItem != MACHINESELECTED)
         selectedItem = 0;
      repaint();
   }
   
   synchronized void doWait(int millis) {
      try { wait(millis); }
      catch (InterruptedException e) { }
   }
   
   void executeOneStep() {
      if (machineState == MachineData.HALTSTATE) {
        machineState = 0;
        setChanged(0,0,width,tapeTop);
        repaint();
        doWait(100);
      }
      char symbol = data.getTape(currentSquare);
      char newSymbol;
      boolean direction;
      int newState = data.getNewState(machineState,symbol);
      if (newState == MachineData.UNSPECIFIED) {
         setStatus(NORULE);
         setChangedAll();
         owner.ruleMaker.setRule(machineState,symbol,true);
         int x = (width - squareSize)/2 - (centerSquare-currentSquare)*squareSize;
         if (x < 0 || x + squareSize > width)
            showSquare(currentSquare);
         else
            repaint();
         return;
      }
      else {
         newSymbol = data.getNewSymbol(machineState,symbol);
         direction = data.getDirection(machineState,symbol);
      }
      int x = (width - squareSize)/2 - (centerSquare-currentSquare)*squareSize;
      if (getSpeed() > 1) {
         setBlinking(true);
         setChanged(x,tapeTop,squareSize,squareSize);
         repaint();
         if (getSpeed() == 2)
            doWait(100);
         else
            doWait(200);
         setBlinking(false);
         data.setTape(currentSquare,newSymbol);
         setChanged(x,tapeTop,squareSize,squareSize);
         if (getSpeed() > 2) {
            repaint();
            doWait(100);
         }
      }
      else if (newSymbol != symbol) {
         data.setTape(currentSquare,newSymbol);
         setChanged(x,tapeTop,squareSize,squareSize);
      }
      if (direction) {
         currentSquare++;
         setChanged(x + (squareSize-machineWidth)/2 - 2, 0, machineWidth*2 + 4,tapeTop);
      }
      else {
         currentSquare--;
         setChanged(x + (squareSize-3*machineWidth)/2 - 2, 0, machineWidth*2 + 4,tapeTop);
      }
      machineState = newState;
      if (machineState != MachineData.HALTSTATE) {
        symbol = data.getTape(currentSquare);
        if (data.getNewState(machineState,symbol) == MachineData.UNSPECIFIED) {
            setStatus(NORULE);
            setChangedAll();
            repaint();
            owner.ruleMaker.setRule(machineState,symbol,true);
        }
        else if (getSpeed() > 1) {
            if (data.ruleDefined(machineState,symbol))
               owner.ruleMaker.setRule(machineState,symbol,false);
            else
               owner.ruleMaker.setRule(machineState,'*',false);
        }
      }
      x = (width - squareSize)/2 - (centerSquare-currentSquare)*squareSize;
      if (x < 0 || x + squareSize > width)
         showSquare(currentSquare);
      else
         repaint();
   }

   public void run() {
      while (true) {
         int st;
         synchronized(this) {
            st = getStatus();
            while (st != RUNNING && st != STEPPING && st != STOPPING) {
               try { wait(); }
               catch (InterruptedException e) { }
               st = getStatus();
            }
         }
         if (st != STOPPING)
           executeOneStep();
         synchronized(this) {
           if (st == STEPPING) {
              if (status != NORULE)
                 setStatus(IDLE);
              owner.doneStep(machineState == MachineData.HALTSTATE);
           }
           else {
              if (machineState == MachineData.HALTSTATE) {
                 setStatus(IDLE);
                 if (getSpeed() == 0) {
                   setChanged(0,0,width,tapeTop);
                   repaint(0,0,width,tapeTop);
                 }
                 owner.doneRunning(true);
              }
              else if (getStatus() == STOPPING) {
                 notify();
              }
              else if (getStatus() == NORULE) {
                 owner.doneRunning(false);
              }
              else {
                 try { wait(speedDelay[speed]); }
                 catch (InterruptedException e) { }
              }
           }
         }
      }
   }
   
}