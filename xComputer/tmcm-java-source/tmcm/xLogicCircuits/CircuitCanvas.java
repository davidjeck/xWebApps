package tmcm.xLogicCircuits;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.Vector;

class CircuitCanvas extends Canvas implements Runnable {

   xLogicCircuitsPanel owner;
   
   int width = -1;
   int height = -1;
   int circuitWidth;
   int circuitHeight;
   static final int LEFT = 74;
   Image OSC;
   Graphics OSG;
   Color boardColor = new Color(230,230,200);
   Color borderColor = new Color(120,110,90);
   
   int untitledCt = 0;
   
   ScrollHandler scroller;
   Undoer undoer;
   IOManager loader;
   
   Vector circuitStack = new Vector();  // other circuits besides current one
   Circuit currentCircuit;
   
   final static int IDLE = 0, RUNNING = 1, DRAGGING = 2, DRAWING = 3, STOPPED = 4, LOADING = 5, MESSAGE = 6;
   int state = IDLE;   // STOPPED state is only for an applet whose stop() method has been called, at a time when the state was RUNNING
   Thread runner;
   
   
   boolean circuitChanged;
   int computeDelay = 5;

   CircuitCanvas(xLogicCircuitsPanel owner, Scrollbar scr, Button undoButton) {
      this.owner = owner;
      scroller = new ScrollHandler(this,scr);
      undoer = new Undoer(this,undoButton);
      loader = new IOManager(this);
      setBackground(Color.white);
      currentCircuit = new Circuit();
   }
   
   // --------------------------------- shared state variables --------------------------
   
   final synchronized void setState(int state) {
      this.state = state;
      if (state == RUNNING)
         notify();
   }
   
   final synchronized int getState() {
      return state;
   }
   
   final synchronized void circuitHasChanged() {
      circuitChanged = true;
   }
   
   final synchronized int getComputeDelay() {
      return computeDelay;
   }
   
   // --------------------------------------- start/stop -------------------------------------
   
   void start() {
      if (getState() == STOPPED)
         setState(RUNNING);
      undoer.cancel();
      owner.undoButton.disable();  // only here because Netscape didin't properly disable the buttons at startup
      if (selectedItem == null) {
        owner.deleteButton.disable();
        owner.deIconifyButton.disable();
      }
   }
   
   void stop() {
      if (getState() == RUNNING)
        setState(STOPPED);
   }
   
   //  -----------------------------------------------------------------------------

   public void reshape(int x, int y, int width, int height) {
      super.reshape(x,y,width,height);
      if (this.width != width || this.height != height) {
         undoer.cancel();
         this.width = width;
         this.height = height;
         circuitWidth = width - LEFT;
         circuitHeight = height - 1;
         OSC = null;
         scroller.reshape(LEFT,height);
         currentCircuit.reshape(5,5,circuitWidth-10,circuitHeight-10);
         if (selectedItem != null && !resizer.hidden) {
            if (selectionInScroller)
               resizer.show(selectedItem.boundingBox.x, selectedItem.boundingBox.y, 
                             selectedItem.boundingBox.width, selectedItem.boundingBox.height);
            else
               resizer.show(selectedItem.boundingBox.x+LEFT, selectedItem.boundingBox.y, 
                             selectedItem.boundingBox.width, selectedItem.boundingBox.height);
         }
      }
   }
   
   public void update(Graphics g) {
      paint(g);
   }
   
   synchronized public void paint(Graphics g) {
      if (state == LOADING || state == MESSAGE) {
         putMessage(g);
         return;
      }
      Rectangle clip = g.getClipRect();
      if (clip.x + clip.width > LEFT) {
        if (OSC == null) {
           circuitChanged = true;
           try {
              OSC = createImage(circuitWidth,circuitHeight);
              OSG = OSC.getGraphics();
              OSG.setFont(this.getFont());
           }
           catch (OutOfMemoryError e) {
              OSC = null;
              OSG = null;
              circuitChanged = false;
           }
        }
        if (OSG == null) {
           setMessage("Out of memory.  Try using a smaller window.");
           return;
        }
        if (circuitChanged) {
            OSG.setColor(boardColor);
            OSG.fillRect(0,0,width-LEFT,height-1);
            OSG.setColor(borderColor);
            OSG.drawRoundRect(5,5,circuitWidth-10,circuitHeight-10,5,5);
            currentCircuit.draw(OSG);
            OSG.setColor(Color.black);
            OSG.drawLine(0,0,circuitWidth,0);
            if (getState() == DRAWING) {
               OSG.setColor(Color.green);
               OSG.drawLine(lineSourceNub.connect_x,lineSourceNub.connect_y,newline_x,newline_y);
            }
            circuitChanged = false;
        }
        g.drawImage(OSC,LEFT,0,this);
      }
      if (clip.x < LEFT)
         scroller.draw(g);
      g.setColor(Color.black);
      g.drawLine(0,height-1,width,height-1);
      g.drawLine(LEFT-1,0,LEFT-1,height);
      if (selectedItem != null && selectionInScroller && getState() == DRAGGING)
         selectedItem.draw(g);
      if (selectedItem != null)
         resizer.draw(g);
   }
   
   void forceCircuitRedraw() {
      circuitHasChanged();
      repaint(LEFT,0,circuitWidth,circuitHeight);
   }

   
   //------------------------------------- Respond to buttons --------------------------
   
   synchronized void doSpeedChoice(int selectedIndex) {
      if (selectedIndex == 0)
         computeDelay = 5;
      else if (selectedIndex == 1)
         computeDelay = 100;
      else
         computeDelay = 1000;
      if (state == RUNNING)
         notify();
   }
   
   synchronized void doDeIconify() {
      undoer.cancel();
      if (selectedItem == null || !(selectedItem instanceof Circuit))
         return;
      currentCircuit.name = owner.nameInput.getText();
      Circuit source;
      Circuit oldCircuit = currentCircuit;
      Circuit newCircuit = (Circuit)selectedItem;
      if (selectionInScroller) {
         scroller.deleteItem((Circuit)selectedItem); // can set selectedItem = null
         source = null;
         owner.iconifyButton.setLabel("Iconify");
         circuitStack.setSize(0);
      }
      else {
         source = currentCircuit;
         owner.iconifyButton.setLabel("Shrink");
         circuitStack.addElement(currentCircuit);
      }
      newCircuit.selected = false;
      selectedItem = null;
      owner.deleteButton.disable();
      owner.deIconifyButton.disable();
      newCircuit.deiconify(5,5,circuitWidth-10,circuitHeight-10,source); 
      currentCircuit = newCircuit;
      owner.nameInput.setText(currentCircuit.name);
      if ( source == null ) {
          while (oldCircuit.saveContainerWhileEnlarged != null) {
              Circuit next = oldCircuit.saveContainerWhileEnlarged;
              Rectangle r = oldCircuit.boundingBoxInContainer.getIntRect();
              oldCircuit.iconify(r.x,r.y,r.width,r.height);
              oldCircuit = next;
          }
          if ( oldCircuit.items.size() != 0 || oldCircuit.lines.size() != 0 
                                 || oldCircuit.inputs.size() != 0 || oldCircuit.outputs.size() != 0 )  {
             oldCircuit.iconify(0,0,LEFT-20,LEFT-20);
             scroller.addItem(oldCircuit);
          }
      }
      forceCircuitRedraw();
   }
   
/* synchronized void doDeIconify() {  previous version that allowed multiple mani circuits on circuit stack
      undoer.cancel();
      if (selectedItem == null || !(selectedItem instanceof Circuit))
         return;
      Circuit source;
      Circuit newCircuit = (Circuit)selectedItem;
      if (selectionInScroller) {
         scroller.deleteItem((Circuit)selectedItem); // can set selectedItem = null
         source = null;
         owner.iconifyButton.setLabel("Iconify");
      }
      else {
         source = currentCircuit;
         owner.iconifyButton.setLabel("Shrink");
      }
      newCircuit.selected = false;
      selectedItem = null;
      owner.nameInput.setText(currentCircuit.name);
      owner.deleteButton.disable();
      owner.deIconifyButton.disable();
      circuitStack.addElement(currentCircuit);
      newCircuit.deiconify(5,5,circuitWidth-10,circuitHeight-10,source); 
      currentCircuit = newCircuit;
      owner.nameInput.setText(currentCircuit.name);
      forceCircuitRedraw();
   }
   
synchronized void doIconify() {  previous version allowed multiple main circuits on circuit stack
      undoer.cancel();
      Circuit container = currentCircuit.saveContainerWhileEnlarged;
      if (selectedItem != null && !selectionInScroller) {
         selectedItem.selected = false;
         selectedItem = null;
      }
      currentCircuit.name = owner.nameInput.getText();
      if (container == null)
         currentCircuit.iconify(0,0,LEFT-20,LEFT-20);
      else {
         Rectangle r = currentCircuit.boundingBoxInContainer.getIntRect();
         currentCircuit.iconify(r.x,r.y,r.width,r.height);
      }
      Circuit oldCircuit = currentCircuit;
      if (circuitStack.size() == 0) {
         currentCircuit = new Circuit();
         owner.nameInput.setText("Untitled");
         owner.iconifyButton.setLabel("Iconify");
      }
      else {
         currentCircuit = (Circuit)circuitStack.lastElement();
         circuitStack.setSize(circuitStack.size() - 1);
         owner.nameInput.setText(currentCircuit.name);
         if (currentCircuit.saveContainerWhileEnlarged == null)
            owner.iconifyButton.setLabel("Iconify");
         else
            owner.iconifyButton.setLabel("Shrink");
      }
      currentCircuit.reshape(5,5,circuitWidth-10,circuitHeight-10);
      if (container == null)
         scroller.addItem(oldCircuit);
      selectItem(oldCircuit, container == null);
      forceCircuitRedraw();
   }  */
   
   synchronized void doIconify() {
      if (currentCircuit.inputs.size() == 0 && currentCircuit.outputs.size() == 0 && currentCircuit.items.size() == 0)
         return;
      undoer.cancel();
      if (selectedItem != null && !selectionInScroller) {
         selectedItem.selected = false;
         selectedItem = null;
      }
      currentCircuit.name = owner.nameInput.getText();
      Circuit container = currentCircuit.saveContainerWhileEnlarged;
      Circuit oldCircuit = currentCircuit;
      if (container == null) {
         currentCircuit.iconify(0,0,LEFT-20,LEFT-20);
         currentCircuit = new Circuit();
         owner.nameInput.setText("Untitled " + (++untitledCt));
         scroller.addItem(oldCircuit);
         selectItem(oldCircuit,true);
      }
      else {
         Rectangle r = currentCircuit.boundingBoxInContainer.getIntRect();
         currentCircuit.iconify(r.x,r.y,r.width,r.height);
         currentCircuit = (Circuit)circuitStack.lastElement();  // same as container; I could get rid of circuitStack (but it's used in Undoer and IOManager
         circuitStack.setSize(circuitStack.size() - 1);
         owner.nameInput.setText(currentCircuit.name);
         selectItem(oldCircuit,false);
      }
      if (currentCircuit.saveContainerWhileEnlarged == null)
         owner.iconifyButton.setLabel("Iconify");
      currentCircuit.reshape(5,5,circuitWidth-10,circuitHeight-10);
      forceCircuitRedraw();
   }
   
/*   synchronized void doNew() {
      selectItem(null,false);
      currentCircuit.name = owner.nameInput.getText();
      Vector scrollerCircuits = scroller.deleteAllCircuits();
      undoer.setNewCircuitData(circuitStack,scrollerCircuits,currentCircuit);
      circuitStack = new Vector();
      currentCircuit = new Circuit();
      currentCircuit.reshape(5,5,circuitWidth-10,circuitHeight-10);
      owner.nameInput.setText("Untitled");
      owner.deIconifyButton.disable();
      owner.iconifyButton.setLabel("Iconify");
      forceCircuitRedraw();
  }
*/   
   synchronized void doDelete() {
      if (selectedItem == null)
        return;
      if (selectionInScroller) {
         selectedItem.selected = false;
         if (selectedItem instanceof Circuit) {  // should always pass
            undoer.setDeleteFromScrollData(selectedItem,scroller.topItem);
            scroller.deleteItem((Circuit)selectedItem);
         }
         selectedItem = null;
      }
      else {
         undoer.setDeleteData(selectedItem);
         selectedItem.delete(currentCircuit);
         selectedItem.selected = false;
         selectedItem = null;
         owner.deleteButton.disable();
         owner.deIconifyButton.disable();
         forceCircuitRedraw();
      }
   }
   
   synchronized void doClear() {
      if (state == MESSAGE || state == LOADING) {
         clearMessage();
         return;
      }
      selectItem(null,false);
      if (currentCircuit.saveContainerWhileEnlarged == null) {
          undoer.setClearMainCircuitData(currentCircuit.items,currentCircuit.lines,currentCircuit.inputs,currentCircuit.outputs);
          currentCircuit.inputs = new Vector();
          currentCircuit.outputs = new Vector();
      }
      else {
          undoer.setClearSubCircuitData(currentCircuit.items,currentCircuit.lines);
          for (int i = 0; i < currentCircuit.inputs.size(); i++) {
             CircuitIONub in = (CircuitIONub)currentCircuit.inputs.elementAt(i);
             in.destination.setSize(0);
          }
          for (int i = 0; i < currentCircuit.outputs.size(); i++) {
             CircuitIONub out = (CircuitIONub)currentCircuit.outputs.elementAt(i);
             out.source = null;
          }
      }
      currentCircuit.items = new Vector();
      currentCircuit.lines = new Vector();
      if (selectedItem != null && !selectionInScroller) {
         owner.deleteButton.disable();
         owner.deIconifyButton.disable();
         selectedItem.selected = false;
         selectedItem = null;
      }
      forceCircuitRedraw();
   }
   
   synchronized void doPower(boolean on) {
      if (on) {
         setState(RUNNING);
         if (runner == null || !runner.isAlive()) {
            runner = new Thread(this,"Circuit Runner");
            runner.start();
         }
      }
      else {
         setState(IDLE);
         currentCircuit.powerOff();
         forceCircuitRedraw();
      }
   }
   
   
   //----------------------- File/URL IO ---------------------------------------
   
   
   synchronized void loadURL(URL url) {  // called from applet during first start()
      try {
         InputStream in = url.openConnection().getInputStream();
         if (!loader.startReading(in))
            throw new IOException("Internal Error:  Another file is being loaded.");
         loadMessage("the URL" + url.toString());
      }
      catch (IOException e) {
         setMessage("An input error occured while+opening the URL " + url.toString() + " +(" + e + ")");
      }
      catch (SecurityException e) {
         setMessage("A security error occured while+trying to open the URL " + url.toString() +".+(" + e + ")");
      }
      catch (Throwable e) {
         setMessage("An error occured while+trying to open the URL " + url.toString() +".+(" + e + ")");
      }
   }
   
   synchronized void loadFile(String fileName, String directoryName) {
      try {
         File file = new File(directoryName,fileName);
         InputStream in = new FileInputStream(file);
         if (!loader.startReading(in))
            throw new IOException("Internal Error:  Another file is being loaded.");
         loadMessage("the file " + fileName);
      }
      catch (IOException e) {
         setMessage("An input error occured while+opening the file " + fileName +".+(" + e + ")");
      }
      catch (SecurityException e) {
         setMessage("A security error occured while+trying to open the file " + fileName +".+(" + e + ")");
      }
      catch (Throwable e) {
         setMessage("An error occured while+trying to open the file " + fileName +".+(" + e + ")");
      }
   }
   
   synchronized void saveToFile(String fileName, String directoryName) {
      PrintStream out = null;
      try {
         File file = new File(directoryName,fileName);
         out = new PrintStream(new FileOutputStream(file));
      }
      catch (IOException e) {
         setMessage("An error occured while+opening the file " + fileName +".+(" + e + ")");
         return;
      }
      catch (SecurityException e) {
         setMessage("A security error occured while+trying to open the file " + fileName +".+(" + e + ")");
         return;
      }
      try {
         Vector scrollItems = new Vector();
         for (int i = scroller.standardItemCt; i < scroller.items.size(); i++)
            scrollItems.addElement(scroller.items.elementAt(i));
         currentCircuit.name = owner.nameInput.getText();
         loader.writeCircuitData(out,circuitStack,currentCircuit,scrollItems);
      }
      catch (IOException e) {
         setMessage("An output error occured while+writing to the file " + fileName +".+(" + e + ")");
     }
      catch (Throwable e) {
         setMessage("An error occured while+writing to the file " + fileName +".+(" + e + ")");
      }
      out.close();
   }
   
   synchronized void doneLoading(Vector circuitStackLoaded, Circuit currentCircuitLoaded, Vector scrollItemsLoaded) {
      selectItem(null,false);
      currentCircuit.name = owner.nameInput.getText();
      Vector scrollerCircuits = scroller.deleteAllCircuits();
      undoer.setLoadData(circuitStack,scrollerCircuits,currentCircuit);
      circuitStack = circuitStackLoaded;
      currentCircuit = currentCircuitLoaded;
      currentCircuit.reshape(5,5,circuitWidth-10,circuitHeight-10);
      owner.nameInput.setText(currentCircuit.name);
      owner.deIconifyButton.disable();
      if (currentCircuit.saveContainerWhileEnlarged != null)
         owner.iconifyButton.setLabel("Shrink");
      else
         owner.iconifyButton.setLabel("Iconify");
      if (scrollItemsLoaded.size() != 0)
         scroller.addItems(scrollItemsLoaded);
      circuitHasChanged();
      setState(IDLE);
      clearMessage();
   }
   
   synchronized void doneLoadingWithError(String errorMessage) {
      setMessage("An error occurred while reading data:+" + errorMessage);
   }
   
   String message;
   
   synchronized void setMessage(String message) {
      if (state != IDLE) {
         state = IDLE;
         currentCircuit.powerOff();
         circuitHasChanged();
         owner.powerCheckbox.setState(false);
      }
      state = MESSAGE;
      this.message = message;
      turnOffControls();
      repaint();
   }
   
   synchronized void loadMessage(String fileName) {
      if (state != IDLE) {
         state = IDLE;
         currentCircuit.powerOff();
         circuitHasChanged();
         owner.powerCheckbox.setState(false);
      }
      setMessage("LOADING new circuit data from+" + fileName);
      state = LOADING;
      turnOffControls();
      repaint();
   }
   
   synchronized void clearMessage() {
      if (state == LOADING)
         loader.cancelLoad();
      setState(IDLE);
      message = null;
      restoreControls();
      repaint();
   }
   
   void putMessage(Graphics g) {
      g.setColor(Color.white);
      g.fillRect(0,0,width,height);
      g.setColor(Color.black);
      g.drawLine(0,0,width,0);
      g.drawLine(0,height-1,width,height-1);
      FontMetrics fm = g.getFontMetrics();
      int y = 3*fm.getHeight();
      int a = 0;
      while (a < message.length()) {
         int b = message.indexOf("+",a);
         if (b == -1)
            b = message.length();
         String str = message.substring(a,b);
         g.drawString(str, 25, y);
         y += fm.getHeight() + 3;
         a = b+1;
      }
      y += fm.getHeight();
      g.setColor(Color.red);
      if (getState() == LOADING)
         g.drawString("Click \"Clear\" button to abort.",25,y);
      else
         g.drawString("Click mouse to continue.",25,y);
   }
   
   
   void turnOffControls() {
      owner.deleteButton.disable();
      owner.undoButton.disable();
      owner.iconifyButton.disable();
      owner.deIconifyButton.disable();
      owner.loadButton.disable();
      owner.saveButton.disable();
 //     owner.newButton.disable();
      owner.powerCheckbox.disable();
      owner.scroll.disable();
   }
   
   void restoreControls() {
      if (selectedItem != null) {
         owner.deleteButton.enable();
         if (selectedItem instanceof Circuit)
            owner.deIconifyButton.enable();
      }
      if (undoer.status != Undoer.NONE)
         owner.undoButton.enable();
      owner.iconifyButton.enable();
      if (owner.canLoad)
         owner.loadButton.enable();
      if (owner.canSave)
         owner.saveButton.enable();
//      owner.newButton.enable();
      owner.powerCheckbox.enable();
      owner.scroll.enable();
   }
   

   
   //----------------------- Selected Item Handling ----------------------------

   CircuitItem selectedItem = null;
   boolean selectionInScroller;
   ResizeBox resizer = new ResizeBox();

   
   synchronized void selectItem(CircuitItem it, boolean inScroller) {
      if (it == selectedItem)
         return;
      if (selectedItem != null) {
         if (!resizer.hidden)
            resizer.hide();
         selectedItem.selected = false;
         Rectangle r = selectedItem.getCopyOfBoundingBox(false);
         if (!selectionInScroller)
            r.x += LEFT;
         repaint(r.x,r.y,r.width,r.height);
         selectionInScroller = false;
      }
      selectedItem = it;
      if (it == null) {
         owner.deleteButton.disable();
         owner.deIconifyButton.disable();
      }
      else {
         it.selected = true;
         selectionInScroller = inScroller;
         if (selectionInScroller)
            scroller.setSelection(it);
         if (it instanceof Circuit || (!selectionInScroller && it instanceof Gate)) {
            if (selectionInScroller)
               resizer.show(it.boundingBox.x,it.boundingBox.y,
                                it.boundingBox.width,it.boundingBox.height);
            else
               resizer.show(it.boundingBox.x+LEFT,it.boundingBox.y,
                                it.boundingBox.width,it.boundingBox.height);
         }
         else
            resizer.hide();
         if ((it instanceof Circuit || !selectionInScroller) && getState() != DRAWING) {
            owner.deleteButton.enable();
            if (it instanceof Circuit)
               owner.deIconifyButton.enable();
            else
               owner.deIconifyButton.disable();
         }
         else {
            owner.deleteButton.disable();
            owner.deIconifyButton.disable();
         }
         Rectangle r = it.getCopyOfBoundingBox(false);
         if (!selectionInScroller)
            r.x += LEFT;
         repaint(r.x,r.y,r.width,r.height);
      }
      circuitHasChanged();
   }
   
   //--------------------------- Mouse Events ---------------------------------------
   
   int saveState;
   
   
   public boolean mouseDown(Event evt, int x, int y) {
   
      int st = getState();
      
      if (st == MESSAGE) {
         clearMessage();
         return true;
      }
      
      if (st == LOADING)
         return true;
   
      if (st == DRAGGING || st == DRAWING) {  // can't happen, except with Netscspe's bug!
         setState(saveState);
         undoer.cancel();
         forceCircuitRedraw();
      }
   
      saveState = st;
      
      if (selectedItem != null && !resizer.hidden) {
         if (selectionInScroller)
            resizing = resizer.beginSymmetricDrag(x,y,LEFT-8,LEFT-8);
         else
            resizing = resizer.beginDrag(x,y,new Rectangle(LEFT+5,5,circuitWidth-10,circuitHeight-10));
         if (resizing) {
            beginResize(x,y);
            return true;
         }
      }
      
      resizing = false;
   
      CircuitItem hitItem = null;
      
      if (x < LEFT) {
         hitItem = scroller.checkMouse(x,y);
         if (hitItem != null)
            selectItem(hitItem,true);
         else
            selectItem(null,false);
      }
      else {
         for (int i = currentCircuit.items.size() - 1; i >= 0; i--) {
            CircuitItem it = (CircuitItem)currentCircuit.items.elementAt(i);
            if (it.hit(x-LEFT,y)) {
               hitItem = it;
               break;
            }
         }
         if (hitItem == null) for (int i = currentCircuit.inputs.size() - 1; i >= 0; i--) {
            CircuitItem it = (CircuitItem)currentCircuit.inputs.elementAt(i);
            if (it.hit(x-LEFT,y)) {
               hitItem = it;
               break;
            }
         }
         if (hitItem == null) for (int i = currentCircuit.outputs.size() - 1; i >= 0; i--) {
            CircuitItem it = (CircuitItem)currentCircuit.outputs.elementAt(i);
            if (it.hit(x-LEFT,y)) {
               hitItem = it;
               break;
            }
         }
         if (hitItem == null) for (int i = currentCircuit.lines.size() - 1; i >= 0; i--) {
            CircuitItem it = (CircuitItem)currentCircuit.lines.elementAt(i);
            if (it.hit(x-LEFT,y)) {
               hitItem = it;
               break;
            }
         }
         selectItem(hitItem,false);
      }

      if (selectedItem == null)
         return true;
         
      if (evt.metaDown() || evt.controlDown()) {
         beginDrag(x,y);
         return true;
      }
      
      if (evt.clickCount == 2 && selectedItem != null && hitItem == selectedItem) {
        if (selectedItem instanceof Circuit) {
           doDeIconify();
           return true;
        }
        else if (selectedItem instanceof Line) {
           insertTack((Line)selectedItem,x-LEFT,y);
           beginDrag(x,y);
           return true;
        }
      }
      
      if (selectionInScroller) {
         beginDrag(x,y);
         return true;
      }
      
      if (selectedItem instanceof CircuitIONub)
        doIOClick((CircuitIONub)selectedItem);

      beginDraw(x,y);
      
      return true;
   }

   public boolean mouseDrag(Event evt, int x, int y) {
      int state = getState();
      if (state == DRAGGING) {
         if (resizing)
            continueResize(x,y);
         else
            continueDrag(x,y);
      }
      else if (state == DRAWING)
         continueDraw(x,y);
      return true;
   }

   public boolean mouseUp(Event evt, int x, int y) {
      int state = getState();
      if (state == DRAGGING) {
         if (resizing)
            endResize(x,y);
         else
            endDrag(x,y);
         setState(saveState);
      }
      else if (state == DRAWING) {
         endDraw(x,y);
         setState(saveState);
      }
      return true;
   }
   
   synchronized void insertTack(Line line, int x, int y) {
      Rectangle r = line.getCopyOfBoundingBox(false);
      Tack tack = new Tack();
      tack.reshape(x-2,y-2,5,5);
      IONub source = line.source;
      IONub dest = line.destination;
      line.delete(currentCircuit);
      Line line1 = new Line(source,tack);
      Line line2 = new Line(tack,dest);
      currentCircuit.lines.addElement(line1);
      currentCircuit.lines.addElement(line2);
      currentCircuit.items.addElement(tack);
      line1.on = line2.on = tack.on = line.on;
      selectItem(tack,false);  // forces draw to OSC
      r.add(tack.getCopyOfBoundingBox(true));
      repaint(r.x,r.y,r.width,r.height);
      undoer.setAddTackData(tack,line);
   }
   
   synchronized void doIOClick(CircuitIONub io) {
      if (io.kind != IONub.INPUT)
         return;
      if (getState() == IDLE)
         return;
      io.on = !io.on;
      circuitHasChanged();
      repaint(Math.round(io.boundingBox.x)+LEFT,Math.round(io.boundingBox.y),
                                  Math.round(io.boundingBox.width),Math.round(io.boundingBox.height));
      notify();
   }
   
   //-------------------------- Manage drag and draw ------------------------------
   
   boolean resizing;
   int last_x, last_y, start_x, start_y;
   int offset_x, offset_y;
   Rectangle changedRect;
   boolean draggingFromScroller;
   CircuitItem saveScrollSelection;
   FloatRect saveOriginalBox;
   
   synchronized void beginDrag(int x, int y) {
      if (selectedItem == null || selectedItem instanceof Line)
         return;
      if (selectionInScroller)
         offset_x = Math.round(selectedItem.boundingBox.x) - x;
      else
         offset_x = Math.round(selectedItem.boundingBox.x) + LEFT - x;
      offset_y = Math.round(selectedItem.boundingBox.y) - y;
      draggingFromScroller = selectionInScroller;
      if (selectionInScroller) {
         CircuitItem it = selectedItem.copy();
         selectedItem.selected = false;
         saveScrollSelection = selectedItem;
         selectedItem = it;
      }
      else {
         selectedItem.selectConnectedLines(true);
         saveOriginalBox = new FloatRect(selectedItem.boundingBox.x, selectedItem.boundingBox.y,
                                    selectedItem.boundingBox.width, selectedItem.boundingBox.height); 
      }
      changedRect = selectedItem.getCopyOfBoundingBox(true);
      setState(DRAGGING);
      resizer.hide();
      start_x = x;
      start_y = y;
   }
   
   synchronized void continueDrag(int x, int y) {
      int new_x = x + offset_x;
      int new_y = y + offset_y;
      if (selectionInScroller) {
         if (new_x < LEFT - 3)
            selectedItem.reshape(new_x,new_y,selectedItem.boundingBox.width,selectedItem.boundingBox.height);
         else {
            currentCircuit.addItem(selectedItem);
            selectionInScroller = false;
            changedRect.x -= LEFT;
            selectedItem.dragTo(new_x-LEFT,new_y,currentCircuit.boundingBox);
            owner.deleteButton.enable();
         }
      }
      else
         selectedItem.dragTo(new_x-LEFT,new_y,currentCircuit.boundingBox);
      Rectangle r = selectedItem.getCopyOfBoundingBox(true);
      changedRect.add(r);
      circuitHasChanged();
      if (selectionInScroller)
          repaint(changedRect.x,changedRect.y,changedRect.width,changedRect.height);
      else
          repaint(changedRect.x+LEFT,changedRect.y,changedRect.width,changedRect.height);
      changedRect = r;
   }
   
   synchronized void endDrag(int x, int y) {
      continueDrag(x,y);  // doess circuitHasChanged();
      selectedItem.selectConnectedLines(false);
      if (selectionInScroller) {
         selectedItem = saveScrollSelection;
         saveScrollSelection.selected = true;
         if (x != start_x || y != start_y)
            repaint();
      }
      if (selectedItem instanceof Circuit || (!selectionInScroller && selectedItem instanceof Gate)) {
         if (selectionInScroller)
            resizer.show(selectedItem.boundingBox.x, selectedItem.boundingBox.y,
                         selectedItem.boundingBox.width, selectedItem.boundingBox.height);
         else
            resizer.show(selectedItem.boundingBox.x+LEFT, selectedItem.boundingBox.y,
                         selectedItem.boundingBox.width, selectedItem.boundingBox.height);
         repaint(resizer.x,resizer.y,resizer.width+1,resizer.height+1);
      }
      if (selectionInScroller || (x == start_x && y == start_y))
         return;
      else if (draggingFromScroller)
         undoer.setAddItemData(selectedItem);
      else
         undoer.setDragData(selectedItem,saveOriginalBox);
   }
   
   synchronized void beginResize(int x, int y) {
      setState(DRAGGING);
      selectedItem.selectConnectedLines(true);
      saveOriginalBox = new FloatRect(selectedItem.boundingBox.x, selectedItem.boundingBox.y,
                                    selectedItem.boundingBox.width, selectedItem.boundingBox.height); 
      changedRect = selectedItem.getCopyOfBoundingBox(true);
      circuitHasChanged();
      start_x = x;
      start_y = y;
   }
   
   synchronized void continueResize(int x, int y) {
      resizer.continueDrag(x,y);
      if (selectionInScroller)
         selectedItem.reshape(resizer.x,resizer.y,resizer.width,resizer.height);
      else
         selectedItem.reshape(resizer.x-LEFT,resizer.y,resizer.width,resizer.height);
      Rectangle r = selectedItem.getCopyOfBoundingBox(true);
      changedRect.add(r);
      if (!selectionInScroller)
         changedRect.x += LEFT;
      circuitHasChanged();
      repaint(changedRect.x,changedRect.y,changedRect.width,changedRect.height);
      changedRect = r;
   }
   
   synchronized void endResize(int x, int y) {
      continueResize(x,y);  // doesCircuitHasChanged
      resizer.endDrag(x,y);
      selectedItem.selectConnectedLines(false);
      if (x != start_x || y != start_y)
         if (selectionInScroller)
            undoer.setResizeInScrollData(selectedItem,saveOriginalBox);         
         else
            undoer.setResizeData(selectedItem,saveOriginalBox);         
   }
   
   IONub lineSourceNub, lineDestinationNub;
   CircuitItem lineSourceItem, lineDestinationItem;
   int newline_x, newline_y;
   
   synchronized void beginDraw(int x, int y) {
      x = x - LEFT;
      lineSourceItem = currentCircuit.itemHitForLineSource(x,y);
      if (lineSourceItem == null)
         return;
      lineSourceNub = lineSourceItem.getLineSource(x,y);
      if (lineSourceNub == null)
         return;  // shouldn't be possible
      setState(DRAWING);
      last_x = newline_x = x;
      last_y = newline_y = y;
      changedRect = new Rectangle(x,y,1,1);
      changedRect.add(lineSourceNub.connect_x,lineSourceNub.connect_y);
      changedRect.grow(1,1);
      circuitHasChanged();
      repaint(changedRect.x,changedRect.y,changedRect.width,changedRect.height);
   }
   
   synchronized void continueDraw(int x, int y) {
      x = x - LEFT;
      last_x = x;
      last_y = y;
      x = Math.min(Math.max(5,x),circuitWidth - 6);
      y = Math.min(Math.max(5,y),circuitHeight - 6);
      IONub checkForNewSource = lineSourceItem.getLineSource(x,y);
      if (selectedItem == lineSourceItem && !lineSourceItem.boundingBox.inside(x,y))
         selectItem(null,false);
      boolean needsRefresh = false;
      if (checkForNewSource != null && checkForNewSource != lineSourceNub) {
         needsRefresh = true;
         lineSourceNub = checkForNewSource;
         changedRect.add(lineSourceNub.connect_x,lineSourceNub.connect_y);
      }
      lineDestinationItem = currentCircuit.itemHitForLineDestination(x,y);
      if (selectedItem != null && lineDestinationItem != selectedItem)
         selectItem(null,false);
      int x0,y0;
      if (lineDestinationItem == null || lineDestinationItem == lineSourceItem) {
         lineDestinationNub = null;
         x0 = x;
         y0 = y;
      }
      else {
         lineDestinationNub = lineDestinationItem.getLineDestination(x,y);
         x0 = lineDestinationNub.connect_x;
         y0 = lineDestinationNub.connect_y;
         selectItem(lineDestinationItem,false);
         resizer.hide();
      }
      if (newline_x != x0 || newline_y != y0) {
         needsRefresh = true;
         changedRect.add(x0,y0);
         newline_x = x0;
         newline_y = y0;
      }
      if (needsRefresh) {
         circuitHasChanged();
         changedRect.grow(1,1);
         repaint(changedRect.x+LEFT,changedRect.y,changedRect.width,changedRect.height);
      }
      changedRect.reshape(newline_x,newline_y,1,1);
      changedRect.add(lineSourceNub.connect_x,lineSourceNub.connect_y);
   }
   
   synchronized void endDraw(int x, int y) {
      x = x - LEFT;
      if (last_x != x || last_y != y)
         continueDraw(x,y);
      setState(saveState);
      if (lineDestinationItem != null && lineDestinationNub != null) {
         Line line = new Line(lineSourceNub,lineDestinationNub);
         currentCircuit.addItem(line);
         undoer.setDrawData(line);
         circuitHasChanged();
      }
      if (selectedItem != lineSourceItem)
         selectItem(null,false);
      changedRect.grow(1,1);
      circuitHasChanged();
      repaint(changedRect.x+LEFT,changedRect.y,changedRect.width,changedRect.height);
      lineSourceNub = null;
      lineDestinationNub = null;
      lineSourceItem = null;
      lineDestinationItem = null;
   }
   
   public void run() {
      while (true) {
         synchronized(this) {
            while (state != RUNNING) {
               try { wait(); }
               catch (InterruptedException e) { }
            }
         }
         boolean needsRepaint = false;
         while (getState() == RUNNING) {
            synchronized(this) {
               if (currentCircuit.computeTopLevel()) {
                  circuitHasChanged();
                  if (state != RUNNING)
                     continue;
                  Graphics g = getGraphics();
                  g.translate(LEFT,0);
                  for (int i = 0; i < currentCircuit.outputs.size(); i++) {
                     CircuitIONub out = (CircuitIONub)currentCircuit.outputs.elementAt(i);
                     out.changed = false;
                  }
                  for (int i = 0; i < currentCircuit.lines.size(); i++) {
                     Line line = (Line)currentCircuit.lines.elementAt(i);
                     line.draw(g);
                     line.destination.changed = true;
                  }
                  for (int i = 0; i < currentCircuit.outputs.size(); i++) {
                     CircuitIONub out = (CircuitIONub)currentCircuit.outputs.elementAt(i);
                      if (out.changed)
                         out.drawCenter(g);
                  }
                  g.dispose();
                  needsRepaint = true;
               }
               else if (needsRepaint) {
                  repaint(LEFT,0,circuitWidth,circuitHeight);
                  needsRepaint = false;
               }
               if (needsRepaint && computeDelay > 200) {
                  repaint(LEFT,0,circuitWidth,circuitHeight);
                  needsRepaint = false;
               }
            }
            if (state == RUNNING) {
               try { Thread.sleep(getComputeDelay()); }
               catch (InterruptedException e) { }
            }
         }
         repaint(LEFT,0,circuitWidth,circuitHeight);
      }
   }

}
