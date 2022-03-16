
package tmcm.xModels;

import java.awt.*;

class xModelsCanvas extends Canvas implements Runnable {

   Image OSC;
   Model model;
   int currentFrame;
   
   int loopStyle = 0;
   final static int doLoop = 0, doBackAndForth = 1, doOnceThrough = 2;
   
   int timePerFrame = 100;  // milliseconds
   
   private int state;
   Thread runner;
   final static int  paused = 1, animating = 2, startAnimation = 3, stopAnimation = 4,
                                    continueAnimation = 5, nextFrame = 6, prevFrame = 7, die = 8;
   
   int OSCwidth = -1, OSCheight = -1;
   
   boolean OSCvalid = false;
                                     
   Label frameNumber;
   Button goBttn, pauseBttn, nextBttn, previousBttn;
         // if set, these are enabled/disabled by the run() method

   public void paint(Graphics g) {
      if (model == null) {
         g.setColor(Color.white);
         g.fillRect(0,0,size().width,size().height);
         return;
      }
      testOSC();
      if (OSC == null) {  // just in case of memory problem?
         synchronized(this) {
            model.drawFrame(g,currentFrame);
         }
         return;
      }
      putOSC(g);
   }
   
   synchronized void testOSC() {
      Dimension size = size();
      if (OSCwidth != size.width || OSCheight != size.height) {
         OSC = null;
         OSCwidth = size.width;
         OSCheight = size.height;
         OSCvalid = false;
         try { OSC = createImage(OSCwidth, OSCheight); }
         catch (OutOfMemoryError e) { OSC = null; }
         if (model != null)
            model.setSize(OSCwidth,OSCheight);
      }
   }
   
   private synchronized void putOSC(Graphics g) {
      if (!OSCvalid)
         makeOSC(currentFrame);
      g.drawImage(OSC,0,0,this);
   }
   
   private synchronized void makeOSC(int frame) {
      Graphics g = OSC.getGraphics();
      model.drawFrame(g,frame);
      g.dispose();
      OSCvalid = true;
   }
   
   public void update(Graphics g) {
      paint(g);
   }
   
   synchronized final void setState(int newstate) {
      state = newstate;
      notify();
   }
   
   synchronized final int getState() {
      return state;
   }
   
   synchronized final int getLoopStyle() {
      return loopStyle;
   }
   
   synchronized final void setLoopStyle(int val) {
      loopStyle = val;
   }
   
   synchronized final int getTimePerFrame() {
      return timePerFrame;
   }
   
   synchronized final void setTimePerFrame(int val) {
      timePerFrame = val;
   }
   
   synchronized final void setCurrentFrame(int val) {
      currentFrame = val;
   }
   
   synchronized final int getCurrentFrame() {
      return currentFrame;
   }
   
   void setModel(Model m) {
      if (runner != null && runner.isAlive()) {
         setState(die);
         try { runner.join(200); }
         catch (InterruptedException e) { }
         if (runner.isAlive())
            runner.stop();
      }
      runner = null;
      OSCvalid = false;
      model = m;
      if (m == null) {
         if (frameNumber != null)
            frameNumber.setText("");
         repaint();
         return;
      }
      m.setSize(size().width, size().height);
      if (m.frames == 1) {
         if (OSC != null) {
            currentFrame = 0;
            Graphics g = OSC.getGraphics();
            m.drawFrame(g,0);
            g.dispose();
         }
         repaint();
      }
      goBttn.disable();
      pauseBttn.disable();
      nextBttn.disable();
      previousBttn.disable();
      if (m.frames > 1) {
         setCurrentFrame(0);
         setState(startAnimation);
         runner = new Thread(this);
         runner.start();
      }
      else if (frameNumber != null)
         frameNumber.setText("Still Image");
   }
   
   public void run() {
      try  { Thread.sleep(500); }
      catch ( InterruptedException e ) { }
      long prevFrameTime = 0;
      boolean forward = true;
      int frame = 0;
      int loop = 0;
      while (true) {
         int state = getState();
         while (state == paused)
            synchronized(this) {
               try { wait(); }
               catch (InterruptedException e) { }
               state = getState();
            }
         switch (state) {
            case die:
               if (pauseBttn != null)
                  pauseBttn.disable();
               if (goBttn != null)
                  goBttn.disable();
               if (nextBttn != null)
                  nextBttn.disable();
               if (previousBttn != null)
                  previousBttn.disable();
               if (frameNumber != null)
                  frameNumber.setText("");
               return;
            case startAnimation:
               frame = 0;
               prevFrameTime = 0;
               setState(animating);
               forward = true;
               if (pauseBttn != null)
                  pauseBttn.enable();
               break;
            case stopAnimation:
               frame = -1;
               setState(paused);
               if (pauseBttn != null)
                  pauseBttn.disable();
               if (goBttn != null)
                  goBttn.enable();
               if (nextBttn != null)
                  nextBttn.enable();
               if (previousBttn != null)
                  previousBttn.enable();
               break;
            case continueAnimation:
               frame = getCurrentFrame();
               loop = getLoopStyle();
               if (loop != doBackAndForth) {
                  forward = true;  // loop might have changed!
                  if (frame >= model.frames - 1)
                     frame = 0;
                  else
                     frame++;
               }
               else
                  frame = -1;
               prevFrameTime = 0;
               setState(animating);
               if (pauseBttn != null)
                  pauseBttn.enable();
               if (goBttn != null)
                  goBttn.disable();
               if (nextBttn != null)
                  nextBttn.disable();
               if (previousBttn != null)
                  previousBttn.disable();
               break;
            case animating:
               frame = getCurrentFrame();
               loop = getLoopStyle();
               if (loop != doBackAndForth)
                  forward = true;
               if (forward)
                  frame++;
               else
                  frame--;
               if (frame < 0) {  // can only happen if loop == doBackAndForth
                  forward = true;
                  frame = 1;
               }
               else if (frame > model.frames-1) {
                  if (loop == doOnceThrough) {
                     setState(stopAnimation);
                     frame = -1;
                  }
                  else if (loop == doBackAndForth) {
                     forward = false;
                     frame = model.frames - 2;
                  }
                  else {
                     forward = true;
                     frame = (model.frames > 2)? 1 : 0;
                  }
               }
               break;
            case nextFrame:
               frame = getCurrentFrame();
               if (frame >= model.frames-1)
                  frame = 0;
               else
                  frame++;
               setState(paused);
               forward = true;
               break;
            case prevFrame:
               frame = getCurrentFrame();
               if (frame == 0)
                  frame = model.frames - 1;
               else
                  frame--;
               setCurrentFrame(frame);
               setState(paused);
               forward = true;
               break;
         }
         if (frame >= 0) {
            setCurrentFrame(frame);
            if (!OSCvalid && state == animating && OSC != null)
               try { Thread.sleep(333); }
               catch ( InterruptedException e ) { }
            testOSC();
            if (OSC != null) {
               makeOSC(frame);
               Graphics g = getGraphics();
               putOSC(g);
               g.dispose();
            }
            else {
               repaint();
            }
            if (frameNumber != null)
               frameNumber.setText("Frame #" + frame);
         }
         if (getState() == animating) {
            long time = System.currentTimeMillis();
            long sleepTime = getTimePerFrame() - (time - prevFrameTime);
            if (sleepTime < 10)
               sleepTime = 10;
            synchronized(this) {
               try { wait((int)sleepTime); }
               catch (InterruptedException e) { }
            }
            prevFrameTime = System.currentTimeMillis();
         }
      }
   }

}