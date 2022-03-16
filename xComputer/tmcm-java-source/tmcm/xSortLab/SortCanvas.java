package tmcm.xSortLab;
import java.awt.*;

class SortCanvas extends Canvas implements Runnable {

   Thread runner;
   
   final static int APPLETSTOPPED = -1, IDLE = 0,
                            STARTING = 1,
                            STOPPED = 2,
                            STEP = 3,
                            RUN = 4,
                            STOPPING = 5;

   private int state = STARTING;
   private int oldState = STARTING;

   Image OSC;
   Graphics OSG;
   boolean changed;
   int changed_x, changed_y, changed_width, changed_height;
   
   int width = -1, height;
   
   static final Color backgroundColor = new Color(230,230,255);
   static final Color borderColor = Color.blue;
   static final Color barColor = Color.gray;
   static final Color finishedBarColor = Color.black;
   static final Color movingBarColor = Color.lightGray;
   static final Color movingBarOutlineColor = Color.gray;
   static final Color boxColor = Color.magenta;
   static final Color multiBoxColor = new Color(0,200,0);
   static final Color maxColor = Color.red;
   

   int[] item = new int[33];
   boolean tempOn;
   int[] mergeBox = new int[3];
   Point multiBoxLoc = new Point(-1,-1);
   Point movingItemLoc = new Point(-1,-1);
   int maxLoc, hiLoc, loLoc, box1Loc, box2Loc, movingItem;
   
   int copyCt;
   int comparisonCt;
   Label comparisons, copies;
   MessageCanvas comment1, comment2;
   VisualSortPanel owner;
   
   static final int barGap = 8;
   int barWidth, barHeight, minBarHeight, barIncrement,
       leftOffset, firstRow_y, secondRow_y, textAscent;
   Font font;
   FontMetrics fm;
      
   boolean fast;
   
   SortCanvas(VisualSortPanel owner, Label comparisons, Label copies, MessageCanvas comment1, MessageCanvas comment2) {
      this.owner = owner;
      this.comparisons = comparisons;
      this.copies = copies;
      this.comment1 = comment1;
      this.comment2 = comment2;
      setUpSortData();
   }
   
   public void reshape(int x, int y, int width, int height) {
      super.reshape(x,y,width,height);
      if (width != this.width || height != this.height) {
         OSC = null;
         OSG = null;
      }
   }

   synchronized int getState() {
      return state;
   }
   
   synchronized void setState(int state) {
      this.state = state;
      notify();
   }
   
   void setUpSortData() {
      maxLoc = -1;
      hiLoc = -1;
      loLoc = -1;
      box1Loc = -1;
      box2Loc = -1;
      multiBoxLoc.x = -1;
      mergeBox[0] = -1;
      movingItem = -1;
      tempOn = false;
      for (int i = 1; i <= 16; i++)
         item[i] = i;
      for (int i = 16; i >= 2; i--) {
         int j = 1 + (int)(Math.random()*i);
         int temp = item[i];
         item[i] = item[j];
         item[j] = temp;
      }
      item[0] = -1;
      for (int i = 17; i < 33; i++)
         item[i] = -1;
   }
   
   synchronized void newSort(int sortMethod) {
      if (state == RUN)
         stopRunning();
      state = STARTING;
      setUpSortData();
      comparisons.setText("0");
      copies.setText("0");
      comparisonCt = 0;
      copyCt = 0;
      method = sortMethod;
      valid = false;
      comment1.changeMessage("Click \"Go\" or \"Step\" to begin sorting.");
      comment2.changeMessage("");
      setChangedAll();
      repaint();
   }
   
   synchronized public void paint(Graphics g) {
      if (OSC == null || size().width != width || size().height != height) {
         try {
            OSC = createImage(size().width,size().height);
            OSG = OSC.getGraphics();
         }
         catch (OutOfMemoryError e) {
            OSC = null;
            OSG = null;
         }
         font = new Font("Helvetica",Font.PLAIN,10);
         fm = g.getFontMetrics(font);
         if (OSG != null)
            OSG.setFont(font);
         textAscent = fm.getAscent();
         setSizeData(size().width,size().height);
         setChanged(0,0,width,height);
      }
      if (OSC == null) {
         g.setFont(font);
         draw(g,0,0,width,height);
      }
      else {
         if (changed)
            draw(OSG,changed_x,changed_y,changed_width,changed_height);
         g.drawImage(OSC,0,0,this);
      }
   }
   
   public void update(Graphics g) {
      paint(g);
   }
   
   void setSizeData(int w, int h) {
      width = w;
      height = h;
      int x = (width - 20 + barGap)/16;
      barWidth =  x - barGap;
      leftOffset = (width - 16*barWidth - 15*barGap)/2;
      barHeight = (height - 40 - 2*textAscent) / 2;
      barIncrement = (barHeight-3)/17;
      minBarHeight = barHeight - 17*barIncrement;
      firstRow_y = barHeight + 10;
      secondRow_y = 2*barHeight + 25 + textAscent;
   }

   
   synchronized void setChanged(int x, int y, int w, int h) {
      if (changed) {
         int x1 = Math.min(x,changed_x);
         int y1 = Math.min(y,changed_y);
         int x2 = Math.max(x+w,changed_x+changed_width);
         int y2 = Math.max(y+h,changed_y+changed_height);
         changed_x = x1;
         changed_y = y1;
         changed_width = x2 - x1;
         changed_height = y2-y1;
      }
      else {
         changed_x = x;
         changed_y = y;
         changed_width = w;
         changed_height = h;
      }
      changed = true;
   }
   
   void setChangedAll() {
      setChanged(0,0,size().width,size().height);
   }
   
   void putItem(Graphics g, int i) {
      int h = item[i];
      if (h == -1)
         return;
      int x,y,ht;
      if (h > 16) {
         ht = (h-100)*barIncrement + minBarHeight;
         g.setColor(finishedBarColor);
      }
      else {
         ht = h*barIncrement + minBarHeight;
         g.setColor(barColor);
      }
      if (i == 0) {
         x = leftOffset + ((barWidth+barGap)*15)/2;
         y = secondRow_y - ht;
      }
      else if (i < 17) {
         x = leftOffset + (i-1)*(barWidth+barGap);
         y = firstRow_y - ht;
      }
      else {
         x = leftOffset + (i-17)*(barWidth+barGap);
         y = secondRow_y - ht;
      }
      g.fillRect(x,y,barWidth,ht);
      g.setColor(finishedBarColor);
      g.drawRect(x,y,barWidth,ht);
   }
   
   void drawMovingItem(Graphics g) {
      int ht = movingItem*barIncrement + minBarHeight;
      g.setColor(movingBarColor);
      g.fillRect(movingItemLoc.x,movingItemLoc.y-ht,barWidth,ht);
      g.setColor(movingBarOutlineColor);
      g.drawRect(movingItemLoc.x,movingItemLoc.y-ht,barWidth,ht);
   }
   
   void drawMax(Graphics g) {
      int sw = fm.stringWidth("Max");
      int x = leftOffset + (maxLoc-1)*(barWidth+barGap) + barWidth/2;
      int y = firstRow_y + 38 + textAscent;
      g.setColor(maxColor);
      g.drawString("Max",x-sw/2,y+textAscent);
      g.drawLine(x,y,x,y-30);
      g.drawLine(x,y-30,x+6,y-24);
      g.drawLine(x,y-30,x-6,y-24);
   }
   
   void drawBox(Graphics g, int boxLoc) {
      int x,y;
      if (boxLoc == 0) {
         x = leftOffset + ((barWidth+barGap)*15)/2;
         y = secondRow_y;
      }
      else if (boxLoc < 17) {
         x = leftOffset + (boxLoc-1)*(barWidth+barGap);
         y = firstRow_y;
      }
      else {
         x = leftOffset + (boxLoc-17)*(barWidth+barGap);
         y = secondRow_y;
      }
      g.setColor(boxColor);
      g.drawRect(x-2,y-barHeight-2,barWidth+4,barHeight+4);
   }
   
   void drawMultiBox(Graphics g) {
      int x,y,wd;
      if (multiBoxLoc.x < 17) {
         y = firstRow_y;
         x = leftOffset + (multiBoxLoc.x-1) * (barWidth + barGap);
      }
      else {
         y = secondRow_y;
         x = leftOffset + (multiBoxLoc.x-17) * (barWidth + barGap);
      }
      wd = (multiBoxLoc.y - multiBoxLoc.x)*(barGap + barWidth) + barWidth;
      g.setColor(multiBoxColor);
      g.drawRect(x-4,y-barHeight-4,wd+8,barHeight+8);
   }
   
   void drawMergeListBoxes(Graphics g) {
      int x,y,wd1,wd2;
      y = firstRow_y;
      x = leftOffset + (mergeBox[0]-1) * (barWidth + barGap);
      wd1 = (mergeBox[1] - mergeBox[0])*(barGap + barWidth) + barWidth;
      wd2 = (mergeBox[2] - mergeBox[0])*(barGap + barWidth) + barWidth;
      g.setColor(multiBoxColor);
      g.drawRect(x-4,y-barHeight-4,wd1+8,barHeight+8);
      g.drawRect(x-4,y-barHeight-4,wd2+8,barHeight+8);
   }
   
   synchronized void draw(Graphics g, int x, int y, int w, int h) {
       g.setColor(backgroundColor);
       g.fillRect(x,y,w,h);
       g.setColor(borderColor);
       g.drawRect(0,0,width,height);
       g.drawRect(1,1,width-2,height-2);
       g.drawLine(0,height-2,width,height-2);
       g.drawLine(width-2,0,width-2,height);
       int firstBar = (x-10)/(barWidth+barGap) + 1;
       if (firstBar < 1)
          firstBar = 1;
       int lastBar = (x+w-10)/(barWidth+barGap) + 1;
       if (lastBar > 16)
          lastBar = 16;
       if (y <= firstRow_y) {
          for (int i = firstBar; i <= lastBar; i++)
             putItem(g,i);
       }
       if (y <= firstRow_y + 10 + textAscent && y+h > firstRow_y) {
          g.setColor(borderColor);
          for (int i = firstBar; i <= lastBar; i++) {
             String str = String.valueOf(i);
             int sw = fm.stringWidth(str);
             g.drawString(str,leftOffset+(i-1)*(barWidth+barGap)+(barWidth-sw)/2,firstRow_y+6+textAscent);
          }
       }
       if (y <= secondRow_y  && y+h >= secondRow_y - barHeight) {
          for (int i = 16 + firstBar; i <= 16 + lastBar; i++)
             putItem(g,i);
       }
      if (tempOn) {
         g.setColor(borderColor);
         int sw = fm.stringWidth("Temp");
         g.drawString("Temp",leftOffset + (16*barWidth+15*barGap - sw)/2, secondRow_y + 5 + textAscent);
         putItem(g,0);
      }
      if (maxLoc >= 0)
         drawMax(g);
      if (box1Loc >= 0) 
         drawBox(g,box1Loc);
      if (box2Loc >= 0)
         drawBox(g,box2Loc);
      if (multiBoxLoc.x > 0)
         drawMultiBox(g);
      if (mergeBox[0] > 0)
         drawMergeListBoxes(g);
      if (movingItem >= 0)
         drawMovingItem(g);
      changed = false;
   }
   
   synchronized void startRunning() {
      if (state == IDLE)
         newSort(method);
      state = RUN;
      if (fast)
         comment2.changeMessage("");
      if (runner == null || !runner.isAlive()) {
         runner = new Thread(this);
         runner.start();
      }
      else
         notify();
   }
   
   synchronized void stopRunning() {
      if (runner != null && runner.isAlive() && state == RUN) {
         state = STOPPING;
         notify();
         while (state == STOPPING) {
            try { wait(); }
            catch (InterruptedException e) { }
         }
      }
   }
   
   synchronized void setFast(boolean fast) {
      this.fast = fast;
      if (state == RUN && fast)
         comment2.changeMessage("");
      notify();
   }
   
   synchronized void doStep() {
      if (state == RUN || state == IDLE)
         return;
      state = STEP;
      if (runner == null || !runner.isAlive()) {
         runner = new Thread(this);
         runner.start();
      }
      else
         notify();
   }
   
   synchronized void doAppletStop() {
      oldState = state;
      stopRunning();
      state = APPLETSTOPPED;
      OSC = null;
      OSG = null;
   }
   
   synchronized void doAppletStart() {
      if (state != APPLETSTOPPED)
         return;
      state = oldState;
      if (state == RUN || state == STEP)
         notify();
   }
   
   synchronized void doWait(int millis) {
      try { wait(millis); }
      catch (InterruptedException e) { }
   }
   
   public void run() {
      while (true) {
         int st;
         synchronized(this) {
            while (state <= STOPPED) {
               try { wait(); }
               catch (InterruptedException e) { }
            }
            st = state;
         }
         if (st == STOPPING) {
            setState(STOPPED);
            owner.runnerStopped();
         }
         else {  // st is RUN or STEP
            scriptStep();
            repaint();
            if (done) {
               owner.doneRunning(method,comparisonCt,copyCt);
               setState(IDLE);
               repaint();
            }
            else if (getState() == STOPPING) {
               setState(STOPPED);
               owner.runnerStopped();
            }
            else if (st == STEP && getState() != RUN) {  // state might have changed
               setState(STOPPED);
               owner.runnerStopped();
            }
            else if (fast)
               doWait(100);
            else
               doWait(1000);
         }
      }
   }
   
   // -------------------------------------------------------------------------------------
   
   void say1(String message) {
      comment1.changeMessage(message);
   }
   
   void say2(String message) {
      if (!fast || getState() != RUN)
         comment2.changeMessage(message);
   }
   
   void invalidate(int itemNumber, int expandedBy) {
      if (itemNumber < 0)
         return;
      int x,y;
      if (itemNumber == 0) {
         x = leftOffset + ((barWidth+barGap)*15)/2;
         y = secondRow_y - barHeight;
      }
      else if (itemNumber < 17) {
         x = leftOffset + (itemNumber-1)*(barWidth+barGap);
         y = firstRow_y - barHeight;
      }
      else {
         x = leftOffset + (itemNumber-17)*(barWidth+barGap);
         y = secondRow_y - barHeight;
      }
      setChanged(x-expandedBy,y-expandedBy,barWidth+1+2*expandedBy,barHeight+1+2*expandedBy);
   }
   
   void putTemp(boolean on) {
      if (tempOn == on)
         return;
      tempOn = on;
      setChanged(0,secondRow_y+1,width,height-secondRow_y);
   }
   
   void putMax(int itemNum) {
      int sw = fm.stringWidth("Max") + 4;
      if (maxLoc != -1)
         setChanged(leftOffset + (maxLoc-1)*(barWidth+barGap) + (barWidth-sw)/2,firstRow_y+1,sw,50+textAscent);
      maxLoc = itemNum;
      if (maxLoc != -1)
         setChanged(leftOffset + (maxLoc-1)*(barWidth+barGap) + (barWidth-sw)/2,firstRow_y+1,sw,50+textAscent);
   }
   
   void putMergeListBoxes(int a, int b, int c) {
      if (mergeBox[0] != -1) {
         invalidate(mergeBox[0],5);
         invalidate(mergeBox[2],5);
      }
      mergeBox[0] = a;
      mergeBox[1] = b;
      mergeBox[2] = c;
      if (mergeBox[0] != -1) {
         invalidate(mergeBox[0],5);
         invalidate(mergeBox[2],5);
      }
   }
   
   void putMultiBox(int a, int b) {
      if (multiBoxLoc.x != -1) {
         invalidate(multiBoxLoc.x,5);
         invalidate(multiBoxLoc.y,5);
      }
      multiBoxLoc.x = a;
      multiBoxLoc.y = b;
      if (multiBoxLoc.x != -1) {
         invalidate(multiBoxLoc.x,5);
         invalidate(multiBoxLoc.y,5);
      }
   }
   
   void putBoxes(int item1, int item2) {
      if (box1Loc != -1)
         invalidate(box1Loc,3);
      box1Loc = item1;
      if (box1Loc != -1)
         invalidate(box1Loc,3);
      if (box2Loc != -1)
         invalidate(box2Loc,3);
      box2Loc = item2;
      if (box2Loc != -1) {
         invalidate(box2Loc,3);
         repaint();
         if (fast)
           doWait(100);
         else
           doWait(200);
      }
   }
   
   void itemChanged(int itemNum) {
      invalidate(itemNum,0);
      repaint();
   }
   
   //-------------------------------------------------------------------------------------
   
   int method = 1;  // script data (yuch! should be in a record) copied from Pascal version
   boolean done;
   int i, j, k;
   int hi, lo;
   int [] stack = new int[33];
   int top;
   int sortLength, end_i, end_j;
   boolean valid = false;
   
   void copyFast(int toItem, int fromItem) {
     item[toItem] = item[fromItem];
     item[fromItem] = -1;
     invalidate(toItem,0);
     invalidate(fromItem,0);
     repaint();
     copyCt++;
     copies.setText(String.valueOf(copyCt));
     doWait(100);
   }
   
   void copyItem(int toItem, int fromItem) {
      if (fast) {
         copyFast(toItem,fromItem);
      }
      else {
         movingItem = item[fromItem];
         item[fromItem] = -1;
         invalidate(fromItem,0);
         int x1, y1, x2, y2;
         if (toItem == 0) {
            x2 = leftOffset + ((barWidth+barGap)*15)/2;
            y2 = secondRow_y;
         }
         else if (toItem < 17) {
            x2 = leftOffset + (toItem-1)*(barWidth+barGap);
            y2 = firstRow_y;
         }
         else {
            x2 = leftOffset + (toItem-17)*(barWidth+barGap);
            y2 = secondRow_y;
         }
         if (fromItem == 0) {
            x1 = leftOffset + ((barWidth+barGap)*15)/2;
            y1 = secondRow_y;
         }
         else if (fromItem < 17) {
            x1 = leftOffset + (fromItem-1)*(barWidth+barGap);
            y1 = firstRow_y;
         }
         else {
            x1 = leftOffset + (fromItem-17)*(barWidth+barGap);
            y1 = secondRow_y;
         }
         int dist = (int)Math.sqrt( (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2) );
         int ct = dist / 5;
         if (ct > 15)
           ct = 15;
         else if (ct < 5)
           ct = 5;
         int ht = minBarHeight + movingItem*barIncrement;
         for (int i = 0; i <= ct; i++) {
            if (i > 0)
               setChanged(movingItemLoc.x,movingItemLoc.y-ht,barWidth+1,ht+1);
            movingItemLoc.x = x1 + (int)(((x2-x1)*i)/ct);
            movingItemLoc.y = y1 + (int)(((y2-y1)*i)/ct);
            setChanged(movingItemLoc.x,movingItemLoc.y-ht,barWidth+1,ht+1);
            repaint();
            doWait(50);
         }
         item[toItem] = movingItem;
         movingItem = -1;
         invalidate(toItem,0);
         repaint();
         doWait(50);
         copyCt++;
         copies.setText(String.valueOf(copyCt));
      }
   }
   
boolean greaterThan(int itemA, int itemB) {
  putBoxes(itemA, itemB);
  comparisonCt++;
  comparisons.setText(String.valueOf(comparisonCt));
  return (item[itemA] > item[itemB]);
}
   
void swapItems (int a, int b) {
  copyItem(0, a);
  if (getState() == STARTING)
     return;
  copyItem(a, b);
  if (getState() == STARTING)
     return;
  copyItem(b, 0);
}

synchronized void scriptSetup() {
  comment2.changeMessage("");
   switch (method) {
    case 1:  {
      j = 16;
      i = 1;
      say1("Phase 1:  largest item \"bubbles\" up to position 16");
      putTemp(true);
      break;
     }
    case 2:  {
      j = 16;
      i = 2;
      say1("Phase 1:  Find the largest item and swap it with item 16");
      say2("Item 1 is the largest item seen so far during this phase");
      putMax(1);
      putTemp(true);
      break;
     }
    case 3:  {
      j = 0;
      putMultiBox(1, 1);
      say1("The sublist in the box -- just item 1 for now -- is correctly sorted");
      break;
     }
    case 4:  {
      sortLength = 1;
      i = 1;
      end_i = 1;
      j = 2;
      end_j = 2;
      k = 17;
      lo = 0;
      hi = 1;
      say1("Phase 1:  Merge lists of length 1 into lists of length 2");
      say2("First, merge item 1 with item 2.");
      putMultiBox(17, 18);
      putMergeListBoxes(1, 1, 2);
      break;
     }
    case 5:  {
      top = 0;
      hi = 16;
      lo = 1;
      k = 0;
      i = 1;     // i and j are starting valuse for lo and hi
      j = 16;
      say1("Apply \"QuickSortStep\" to items 1 through 16.");
      say2("The range of possible final positions for item 1 is boxed.");
      putMultiBox(1, 16);
      putTemp(true);
      break;
     }
   }
 }

   
synchronized void scriptStep() {
  if (!valid) {
     scriptSetup();
     valid = true;
     done = false;
     return;
  }
  switch (method) {
    case 1: 
     if (i==j)  {
       comment2.changeMessage("");
       putBoxes(-1, -1);
       if (j==2)  {
         say1("The sort is finished.");
         done = true;
         putTemp(false);
         item[1] = 100+item[1];
         itemChanged(1);
       }
       else {
         j = j - 1;
         i = 1;
         say1("Phase " + (17 - j) + ":  next largest item bubbles up to position " + j);
       }
      }
     else {
       if (greaterThan(i, i + 1))  {
         say2("Is item " + i + " bigger than item " + (i + 1) + "?  Yes, so swap them.");
         swapItems(i, i + 1);
       }
       else {
         say2("Is item " + i + " bigger than item " + (i + 1) + "?  No, so don't swap them.");
       }
       i = i + 1;
       if (i==j)  {
         item[j] = 100+item[j];
         itemChanged(j);
       }
      } // end case 1
      break;
    case 2: 
     if (j==1)  {
       say1("The sort is finished.");
       comment2.changeMessage("");
       done = true;
       item[1] = 100+item[1];
       itemChanged(1);
       putTemp(false);
      }
     else if (i == -1)  {
       say1("Phase " + (17 - j) + ":   Find the next largest item and move it to position " + j);
       say2("Item 1 is the largest item seen so far during this phase");
       i = 2;
       putMax(1);
      }
     else if (i > j)  {
       putBoxes(-1, -1);
       k = maxLoc;
       putMax(-1);
       if (k==j) 
         say2("Item " + j + " is already in its correct location.");
       else {
         if (j==2) 
          say2("Swap item 2 with item 1");
         else
          say2("Swap item " + j +  " with maximum among items 1 through " + (j - 1));
         swapItems(k, j);
       }
       item[j] = 100+item[j];
       itemChanged(j);
       j = j - 1;
       i = -1;
      }
     else if (greaterThan(i, maxLoc))  {
       say2("Item " +  i + " is bigger than item " +  maxLoc + ", so item " +  i + " is now the max seen.");
       putMax(i);
       i = i + 1;
      }
     else {
       say2("Item " +  i + " is smaller than item " + maxLoc + ", so item " + maxLoc + " is still the max seen.");
       i = i + 1;
      } // end case 2
      break;
    case 3: 
     if (j==0)  {
       say1("Phase 1: Insert item 2 into its correct position in the sorted list.");
       say2("Copy item 2 to Temp.");
       copyItem(0, 2);
       j = 2;
       i = 1;
       putTemp(true);
      }
     else if (j==17)  {
       putMultiBox(-1, -1);
       for (int i = 1; i <= 16; i++)
          item[i] += 100;
       setChangedAll();
       say1("The sort is finished.");
       done = true;
       comment2.changeMessage("");
       putTemp(false);
      }
     else if (i==0)  {
       say2("Temp is smaller than all items in the sorted list; copy it to position 1.");
       copyItem(1, 0);
       i = -1;
      }
     else if (i == -1)  {
       putBoxes(-1, -1);
       say1("Items 1 through " + j + " now form a sorted list.");
       comment2.changeMessage("");
       putMultiBox(1, j);
       j = j + 1;
       i = -2;
      }
     else if (i == -2)  {
       say1("Phase " + (j - 1) + ": Insert item " + j + "  into its correct position in the sorted list.");
       say2("Copy item " + j + " to Temp.");
       copyItem(0, j);
       i = j - 1;
      }
     else if (greaterThan(i, 0))  {
       say2("Is item " + i + " bigger than Temp?  Yes, so move it up to position " + (i + 1));
       copyItem(i + 1, i);
       i = i - 1;
      }
     else {
       say2("Is item " +  i + " bigger than Temp?  No, so Temp belongs in position " + (i + 1));
       copyItem(i + 1, 0);
       i = -1;
      }  // end case 3
      break;
    case 4: 
     if ((lo==1) && (sortLength==8))  {
       for (int i = 1; i <= 16; i++)
          item[i] += 100;
       setChangedAll();
       say1("The sort is finished.");
       comment2.changeMessage("");
       done = true;
      }
     else if (lo==1)  {
       hi = hi + 1;
       sortLength = sortLength * 2;
       say1("Phase " + hi + ":  Merge lists of length " + sortLength + " into lists of length " + (sortLength * 2));
       k = 17;
       i = 1;
       j = sortLength + 1;
       end_i = i + sortLength - 1;
       end_j = j + sortLength - 1;
       say2("First, merge items " + i + " through " + end_i + " with items " + j + " through " + end_j);
       putMultiBox(i + 16, end_j + 16);
       putMergeListBoxes(i, end_i, end_j);
       lo = 0;
      }
     else if ((end_i < i) && (end_j < j))  {
       if (k==33)  {
         putMultiBox(-1, -1);
         putMergeListBoxes(-1, -1, -1);
         say2("Copy merged items back to original list.");
         for (int n = 1; n < 17; n++) {
           copyFast(n, n + 16);
           if (getState() == STARTING)
              return;
         }
         lo = 1;
         }
       else {
         end_i = end_i + 2 * sortLength;
         end_j = end_j + 2 * sortLength;
         j = end_i + 1;
         i = j - sortLength;
         if (sortLength == 1) 
           say2("Next, merge item " + i + " with item " + j);
         else
           say2("Next, merge items " + i + " through " + end_i + " with items " + j + " through " + end_j);
         putMultiBox(i + 16, end_j + 16);
         putMergeListBoxes(i, end_i, end_j);
       }
      }
     else if (end_i < i)  {
       putBoxes(-1, -1);
       say2("List 1 is empty; move item " +  j + " to the merged list.");
       copyItem(k, j);
       j = j + 1;
       k = k + 1;
      }
     else if (end_j < j)  {
       putBoxes(-1, -1);
       say2("List 2 is empty; move item " +  i + " to the merged list.");
       copyItem(k, i);
       i = i + 1;
       k = k + 1;
      }
     else if (greaterThan(i, j))  {
       say2("Is item " +  j + " smaller than item " + i +  "?  Yes, so move item " + j + " to merged list");
       copyItem(k, j);
       j = j + 1;
       k = k + 1;
      }
     else {
       say2("Is item " + j + " smaller than item " + i + "?  No, so move item " + i + " to merged list");
       copyItem(k, i);
       i = i + 1;
       k = k + 1;
      }  // end case 4      
      break;
    case 5: 
     if (k==0)  {
       if (hi==lo)  {
         say2("There is only one item in the range; it is already in its final position.");
         item[hi] = 100+item[hi];
         itemChanged(hi);
         putMultiBox(-1, -1);
         k = 1;
       }
       else {
         say2("Copy item " + lo + " to Temp");
         copyItem(0, lo);
         k = -1;
       }
      }
     else if (k==1)  {
       if (top==0)  {
         say1("The sort is finished.");
         comment2.changeMessage("");
         putTemp(false);
         done = true;
       }
       else {
         hi = stack[top];
         lo = stack[top - 1];
         j = hi;
         i = lo;
         top = top - 2;
         say1("Apply \"QuickSortStep\" to items " + lo + " through " + hi);
         say2("The range of possible final positions for item " + lo + " is boxed");
         putMultiBox(lo, hi);
         k = 0;
       }
      }
     else if (k==2)  {
       say2("Item " + hi + " is in final position; smaller items below and bigger items above");
       putMultiBox(-1, -1);
       item[hi] = 100+item[hi];
       itemChanged(hi);
       if (hi < j)  {
         stack[top + 1] = hi + 1;
         stack[top + 2] = j;
         top = top + 2;
       }
       if (hi > i)  {
         stack[top + 1] = i;
         stack[top + 2] = hi - 1;
         top = top + 2;
       }
       k = 1;
      }
     else if (hi==lo)  {
       putBoxes(-1, -1);
       say2("Only one possible position left for Temp; copy Temp to position "  + hi);
       copyItem(hi, 0);
       k = 2;
      }
     else if (item[lo]==-1)  {
       if (greaterThan(0, hi))  {
         say2("Item " + hi + " is smaller than Temp, so move it; Temp will end up above it");
         copyItem(lo, hi);
         lo = lo + 1;
         putMultiBox(lo, hi);
       }
       else {
         say2("Item " + hi + " is bigger than Temp, so Temp will end up below it");
         hi = hi - 1;
         putMultiBox(lo, hi);
       }
      }
     else if (item[hi]==-1)  {
       if (greaterThan(lo, 0))  {
         say2("Item " +  lo + " is bigger than Temp, so move it; Temp will end up below it");
         copyItem(hi, lo);
         hi = hi - 1;
         putMultiBox(lo, hi);
       }
       else {
         say2("Item " + lo + " is smaller than Temp, so Temp will end up above it");
         lo = lo + 1;
         putMultiBox(lo, hi);
       }
      }  // end case 5
   } // end switch
 }  // end scriptStep()

}