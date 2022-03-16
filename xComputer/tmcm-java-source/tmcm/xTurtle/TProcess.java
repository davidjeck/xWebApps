


package tmcm.xTurtle;

import java.util.*;
import java.awt.Color;

class TStack {

   double turtleX, turtleY, turtleHeading;
   boolean turtleIsVisible = true, turtleIsDrawing = true;
   int forkNumber;
   int programCounter;
   int stackStart, stackTop, stackRef;
   TStack next,prev;
   TStack parent;
   Vector children;
   Color color = Color.red;
   Object turtleRef = null;
   int recursionDepth;
   double[] stack;
   
   static int maxRecursionDepth = 2000;
   
   TStack() {
      this(0,null);
   }
   
   TStack(int start, TStack parent) {
      programCounter = start;
      next = this;
      prev = this;
      this.parent = parent;
      children = null;
      stack = new double[20];
   }
   
   void reinit(int globalCt) {
      stackTop = globalCt;
      next = this;
      prev = this;
   }
   
   void push(double x) {
      int loc = stackTop - stackStart;
      if (loc >= stack.length) {
         double[] temp = new double[stack.length + 100];
         System.arraycopy(stack,0,temp,0,stack.length);
         stack = temp;
      }
      stack[loc] = x;
      stackTop++;
   }
   
   double pop() {
      stackTop--;
      int loc = stackTop - stackStart;
      if (loc < 0)
         throw new TError("Internal xTurtle error! Attempt to pop past process start.",0);
      return stack[loc];
   }
   
   void store(int offset, double x) {
     if (offset <= 0)
       offset = -offset;
     else
       offset = stackRef + offset;
     TStack proc = this;
     while (offset < proc.stackStart)
       proc = proc.parent;
     int loc = offset - proc.stackStart;
     proc.stack[loc] = x;
   }
   
   double fetch(int offset) {
     if (offset <= 0)
       offset = -offset;
     else
       offset = stackRef + offset;
     TStack proc = this;
     while (offset < proc.stackStart)
       proc = proc.parent;
     int loc = offset - proc.stackStart;
     return proc.stack[loc];
   }
   
}

class TProcess extends Thread {

   TProgram prog;
   TTurtleHandler TH;
   BitSet Semaphore;
   int loc;
   private int delay = 0;  // milliseconds between steps
   private boolean randomScheduling = true;
   private boolean running;
   private boolean paused = false;
   private double userInput;
   
   TurtleNotification notify;
   
   TStack proc, topProc, activeProcessList;
   int insSinceSleep;
   
   int totalNumberOfProcesses;
   static int maxProcessCount = 1000;
   
   TProcess(TProgram prog, TStack proc, TTurtleHandler h, int startLoc, TurtleNotification notify) {
      super("TurtleRunner");
      this.prog = prog;
      this.TH = h;
      this.proc = proc;
      proc.programCounter = startLoc;
      Semaphore = new BitSet(prog.GrabCount + 1);
      this.notify = notify;  // can be null
   }
   
   synchronized boolean getRandomScheduling() {
      return randomScheduling;
   }
   
   synchronized void setRandomScheduling(boolean on) {
      randomScheduling = on;
   }
   
   synchronized int getDelay() {
      return delay;
   }
   
   synchronized void setDelay(int milli) {
      delay = milli;
   }
   
   synchronized void pause(boolean stop) {
      paused = stop;
      if (!paused)
         notify();
   }
   
   synchronized void doneInput(double userInput) {
      this.userInput = userInput;
      notify();
   }
   
   synchronized void waitForInput() {
      while (TH.getWaitingForIO() >= 0 && running)
         try {
            wait();
         }
         catch (InterruptedException e) {
         }
      doDelay(250);
   }
   
   synchronized boolean getRunning() {
      while (paused && running)
         try {
            wait();
         }
         catch (InterruptedException e) {
         }
      return running;
   }
   
   synchronized void setRunning(boolean run) {
      running = run;
      if (TH.getWaitingForIO() >= 0)
         TH.abortInput();
      if (!running)
         notify();
   }
   
   public void run() {
      topProc = proc;
      activeProcessList = proc;
      proc.recursionDepth = 0;
      insSinceSleep = 0;
      totalNumberOfProcesses = 0;
      setRunning(true);
      if (notify != null)
         notify.startRunning(this);
      try {
        while (getRunning()) {
          if (getRandomScheduling()) {
             int num = 3 + (int)(Math.random()*7);
             for (int i = 0; i < num; i++) {
                loc = proc.programCounter;
                proc.programCounter++;
                DoInstruction();
                insSinceSleep++;
                if (!getRunning())
                   break;
             }
             proc = proc.next;
          }
          else {
             loc = proc.programCounter;
             proc.programCounter++;
             DoInstruction();
             proc = proc.next;
             insSinceSleep++;
          }
          if (insSinceSleep >= 1000)
             doDelay(10);
        }
        KillAllProcesses();
     }
     catch (TError te) {
        KillAllProcesses();
        if (notify != null) 
           notify.errorReport("Program terminated because of runtime error: " + te.getMessage(), te.pos);
        setRunning(false);
     }
     catch (OutOfMemoryError oe) {
        KillAllProcesses();
        if (notify != null) {
           notify.errorReport("Program terminated because of runtime error: Ran out of memory.", -1);
        }
        setRunning(false);
     }
     catch (RuntimeException re) {
        KillAllProcesses();
        if (notify != null)
           notify.errorReport("Program terminated because an internal error in xTurtle was detected: " + re.getMessage() + "  (This is not your fault!)", -1);
        setRunning(false);
     }
     finally {
        if (notify != null)
           notify.doneRunning();
        proc.reinit(prog.globalOffsetCt);
        TH = null;
        proc = null;
        activeProcessList = null;
        prog = null;
     }
   }
   
   final synchronized void doDelay(int sleeptime) {
      try { wait(sleeptime); }
      catch (InterruptedException e) { }
      insSinceSleep = 0;
   }
   
   void KillAllProcesses() {
      proc = topProc;
      totalNumberOfProcesses = 0;
      proc.recursionDepth = 0;
      if (proc.children == null)
         return;
      TH.removeAllTurtles();
      proc.turtleRef = null;
      DoDrawTurtle(true);
   }
   
   void doError(String message) {
      throw new TError(message,prog.pos[loc]);
   }
   
   void InsertProcess (TStack newproc) {
     newproc.next = proc;
     newproc.prev = proc.prev;
     proc.prev = newproc;
     newproc.prev.next = newproc;
   }
   
   void DoDrawTurtle(boolean turning) {
      if ((proc.turtleRef == null) && proc.turtleIsVisible) {
         proc.turtleRef = TH.DrawTurtle(proc.turtleX, proc.turtleY, proc.turtleHeading);
      }
      if (!turning || (proc.turtleIsVisible && !TH.getAlwaysHideTurtles())) {
         int d = getDelay();
         if (d > 0)
            doDelay(d);
      }
   }
   
   void DoEraseTurtle() {
     if (proc.turtleRef != null) {
        TH.RemoveTurtle(proc.turtleRef);
        proc.turtleRef = null;
     }
   }

void DoClear() {
  TH.ClearScreen();
  proc.turtleRef = null;
  DoDrawTurtle(false);
}

void DoHome() {
  DoEraseTurtle();
  proc.turtleX = 0;
  proc.turtleY = 0;
  proc.turtleHeading = 0;
  TH.ScrollToHome();
  DoDrawTurtle(false);
}

void DoKillProcess() {
  TStack parent, runner, This, newproc;
  if (activeProcessList == proc && proc.next == proc) {
    setRunning(false);
    while (proc != topProc) {
      DoEraseTurtle();
      proc = proc.parent;
    }
    DoDrawTurtle(true);
    return;
  }
  This = proc;
  DoEraseTurtle();
  newproc = proc.prev;
  if (activeProcessList == This)
    activeProcessList = newproc;
  This.next.prev = This.prev;
  This.prev.next = This.next;
  do {
   parent = This.parent;
   int size = parent.children.size();
   for (int i = 0; i < size; i++)
      if (parent.children.elementAt(i) == This) {
         totalNumberOfProcesses--;
         if (size == 1)
            parent.children = null;
         else
            parent.children.removeElementAt(i);
         break;
      }
   proc = This;
   DoEraseTurtle();
   This = parent;
  } while (This.children == null);
  proc = newproc;
  DoDrawTurtle(true);
}

void DoFace(double angle) {
  DoEraseTurtle();
  if (Math.abs(angle) > 180) {
    angle = angle - 360 * (int)(angle / 360);
    if (angle > 180)
     angle = angle - 360;
    else if (angle < -180)
     angle = angle + 360;
  }
  proc.turtleHeading = angle;
  DoDrawTurtle(true);
}

void DoMoveTo (double x, double y) {
  int a, b, c, d;
  DoEraseTurtle();
  if (proc.turtleIsDrawing)
    TH.PutLine(proc.color,proc.turtleX, proc.turtleY, x, y);
  proc.turtleX = x;
  proc.turtleY = y;
  DoDrawTurtle(false);
}

void DoForward (double dist) {
  double dx, dy;
  dx = dist * Math.cos(proc.turtleHeading / 180 * Math.PI);
  dy = dist * Math.sin(proc.turtleHeading / 180 * Math.PI);
  DoMoveTo(proc.turtleX + dx, proc.turtleY + dy);
}

void DoArc (double radius, double angle) {
  double centerX, centerY, x, y, t;
  double top, bottom, left, right;
  double theta1;
  DoEraseTurtle();
  centerX = proc.turtleX - radius * Math.sin(proc.turtleHeading / 180 * Math.PI);
  centerY = proc.turtleY + radius * Math.cos(proc.turtleHeading / 180 * Math.PI);
  x = proc.turtleX;
  y = proc.turtleY;
  if (radius > 0)
   theta1 = (proc.turtleHeading - 90);
  else
   theta1 = (proc.turtleHeading + 90);
  if (radius > 0)
   t = proc.turtleHeading + angle;
  else
   t = proc.turtleHeading - angle;
  if (Math.abs(t) > 180) {
    t = t - 360 * (int)(t / 360);
    if (t > 180)
     t = t - 360;
    else if (t < -180)
     t = t + 360;
  }
  proc.turtleHeading = t;
  if (radius > 0) {
    proc.turtleX = centerX + radius * Math.cos((theta1 + angle) / 180 * Math.PI);
    proc.turtleY = centerY + radius * Math.sin((theta1 + angle) / 180 * Math.PI);
  }
  else {
    proc.turtleX = centerX - radius * Math.cos((theta1 - angle) / 180 * Math.PI);
    proc.turtleY = centerY - radius * Math.sin((theta1 - angle) / 180 * Math.PI);
  }
  if (proc.turtleIsDrawing) {
      double r = Math.abs(radius);
      left = centerX - r;
      right = centerX + r;
      top = centerY + r;
      bottom = centerY - r;
      if (radius > 0)
        TH.PutArc(proc.color, left, top, right, bottom, theta1, angle);
      else
        TH.PutArc(proc.color, left, top, right, bottom, theta1, -angle);
  }
  DoDrawTurtle(false);
}

void DoDrawText (String str) {
  double x, y;
  DoEraseTurtle();
  x = proc.turtleX;
  y = proc.turtleY;
  TH.PutText(proc.color, x, y, str);
  y = y - 15*TH.pixelSize;
  proc.turtleY = y;
  DoDrawTurtle(false);
}

void DoFork (int processCt) {
  int i;
  TStack newProc = null;
  if (processCt < 1)
    doError("The number of processes in a fork command must be positive.");
  if (processCt > 100)
    doError("The number of processes created by a fork command is limited to 100.");
  if (processCt == 1)
    return;
  totalNumberOfProcesses += processCt;
  if (totalNumberOfProcesses > maxProcessCount)
     doError("Your program has created more than the permitted maximum of " + maxProcessCount + " processes.");
  proc.children = new Vector(processCt);
  for (i = 1; i<= processCt; i++) {
    newProc = new TStack(proc.programCounter, proc);
    proc.children.addElement(newProc);
    newProc.programCounter = proc.programCounter;
    newProc.stackTop = proc.stackTop;
    newProc.stackStart = proc.stackTop;
    newProc.stackRef = proc.stackRef;
    newProc.turtleX = proc.turtleX;
    newProc.turtleY = proc.turtleY;
    newProc.turtleHeading = proc.turtleHeading;
    newProc.turtleIsVisible = proc.turtleIsVisible;
    newProc.turtleIsDrawing = proc.turtleIsDrawing;
    newProc.recursionDepth = proc.recursionDepth;
    newProc.color = proc.color;
    newProc.forkNumber = i;
    InsertProcess(newProc);
  }
  if (activeProcessList == proc)
    activeProcessList = proc.next;
  proc.next.prev = proc.prev;
  proc.prev.next = proc.next;
  proc.prev = proc;
  proc.next = proc;
  TStack next = (TStack)proc.children.elementAt(0);
  next.turtleRef = proc.turtleRef;
  proc.turtleRef = null;
  proc = next.prev;
}


void DoReturn (int stackItems) {
  int newTop, itemsRemaining;
  TStack This, parent, runner;
  double PC, ref;
  newTop = proc.stackTop - stackItems;
  while (newTop <= proc.stackStart) {
    parent = proc.parent;
    int size = parent.children.size();
    for (int i = 0; i < size; i++) {
       if (parent.children.elementAt(i) == proc) {
          totalNumberOfProcesses--;
          if (size == 1)
             parent.children = null;
          else
             parent.children.removeElementAt(i);
          break;
       }
    }
    if (parent.children != null) {
      This = proc;
      DoEraseTurtle();
      proc = proc.prev;
      DoDrawTurtle(true);
      if (activeProcessList == This) {
         activeProcessList = proc;
         // if activeProcessList = topProc   ?????
      }
      This.next.prev = This.prev;
      This.prev.next = This.next;
      return;
    }
    InsertProcess(parent);
    proc.next.prev = proc.prev;
    proc.prev.next = proc.next;
    if (activeProcessList == proc)
      activeProcessList = parent;
    This = proc;
    DoEraseTurtle();
    proc = parent;
    DoDrawTurtle(true);
  }
  proc.stackTop = newTop;
  PC = proc.pop();
  proc.programCounter = (int)PC;
  ref = proc.pop();
  proc.stackRef = (int)ref;
  proc.recursionDepth--;
}


String MakeString (int loc) {
  int i, ct;
  char ch;
  double x;
  StringBuffer s = new StringBuffer();
  String str = prog.stringStore[loc];
  int len = str.length();
  int numCt = 0;
  for (i = 0; i< len; i++)
    if (str.charAt(i) == (char)255)
       numCt++;
  double[] nums = new double[numCt];
  for (i = numCt-1; i>= 0; i--)
     nums[i] = proc.pop();
  numCt = 0;
  for (i = 0; i < len; i++) {
    ch = str.charAt(i);
    if (ch == (char)255) {
      if (Double.isNaN(nums[numCt]) || Double.isInfinite(nums[numCt]))
         s.append("(undefined)");
      else
         s.append(nums[numCt]);
      numCt++;
    }
    else
      s.append(ch);
  }
  return s.toString();
}


double DoAskUser(String str) {
   TH.DoAskUser(this,str);
   waitForInput();
   return userInput;
}

void DoTellUser(String str) {
   TH.DoTellUser(this,str);
   waitForInput();
}

double DoYesOrNo(String str) {
   TH.DoYesOrNo(this,str);
   waitForInput();
   return userInput;
}


void DoComputation(int kind) {
  double p1=0, p2=0, ans=0;
  p1 = proc.pop();
  if (Double.isNaN(p1))
    doError("Uninitialized value used as subroutine parameter.");
  if (kind >= TTokenizer.plus_) {
    p2 = proc.pop();
    if (Double.isNaN(p2))
        doError("Uninitialized value used as subroutine parameter.");
  }
  switch (kind) {
   case TTokenizer.not_: 
     ans = (p1 == 0)? 1 : 0;
     break;
   case TTokenizer.sin_: 
     ans = Math.sin(p1 / 180 * Math.PI);
     break;
   case TTokenizer.cos_: 
     ans = Math.cos(p1 / 180 * Math.PI);
     break;
   case TTokenizer.tan_:
     ans = Math.tan(p1 / 180 * Math.PI);
     break;
   case TTokenizer.sec_:
     ans = 1 / Math.cos(p1 / 180 * Math.PI);
     break;
   case TTokenizer.cot_:
     ans = Math.cos(p1 / 180 * Math.PI) / Math.sin(p1 / 180 * Math.PI);
     break;
   case TTokenizer.csc_:
     ans = 1 / Math.sin(p1 / 180 * Math.PI);
     break;
   case TTokenizer.arctan_: 
     ans = Math.atan(p1) * 180 / Math.PI;
     break;
   case TTokenizer.arcsin_:
     ans = Math.asin(p1) * 180 / Math.PI;
     break;
   case TTokenizer.arccos_: 
     ans = Math.acos(p1) * 180 / Math.PI;
     break;
   case TTokenizer.abs_: 
     ans = Math.abs(p1);
     break;
   case TTokenizer.sqrt_: 
     if (p1 < 0)
       doError("Illegal attempt to take the square root of a negative number.");
     else
       ans = Math.sqrt(p1);
     break;
   case TTokenizer.exp_: 
     ans = Math.exp(p1);
     break;
   case TTokenizer.ln_: 
     if (p1 < 0)
       doError("Illegal attempt to take the logartihm of a negative number.");
     else
       ans = Math.log(p1);
     break;
   case TTokenizer.round_: 
     ans = Math.round(p1);
     break;
   case TTokenizer.trunc_: 
     ans = (int)p1;
     break;
   case TTokenizer.RandomInt_: 
     ans = (int)(Math.random()*p1) + 1;
     break;
   case TTokenizer.UnaryMinus_: 
     ans = -p1;
     break;
   case TTokenizer.plus_: 
     ans = p1 + p2;
     break;
   case TTokenizer.minus_: 
     ans = p2 - p1;
     break;
   case TTokenizer.times_: 
     ans = p1 * p2;
     break;
   case TTokenizer.divide_: 
     if (p1 == 0)
       doError("Illegal attempt to divide by zero.");
     else
       ans = p2 / p1;
     break;
   case TTokenizer.power_:
      try { 
        ans = Math.pow(p2, p1);
      }
      catch (ArithmeticException e) {
        ans = Double.NaN;
      }
      break;
   case TTokenizer.and_: 
     ans = (p1 == 1 && p2 == 1)? 1 : 0;
     break;
   case TTokenizer.or_: 
     ans = (p1 == 1 || p2 == 1)? 1 : 0;
     break;
   case TTokenizer.LT_: 
     ans = (p2 < p1)? 1: 0;
     break;
   case TTokenizer.GT_: 
     ans = (p2 > p1)? 1: 0;
     break;
   case TTokenizer.LE_: 
     ans = (p2 <= p1)? 1: 0;
     break;
   case TTokenizer.GE_: 
     ans = (p2 >= p1)? 1: 0;
     break;
   case TTokenizer.NE_: 
     ans = (Math.abs(p2 - p1) > 5e-12 * (Math.abs(p2) + Math.abs(p1)))? 1: 0;
     break;
   case TTokenizer.EQ_: 
     ans = (Math.abs(p2 - p1) <= 5e-12 * (Math.abs(p2) + Math.abs(p1)))? 1: 0;
     break;
  }
  if (Double.isInfinite(ans) || Double.isNaN(ans))
     doError("Undefined computation result, or number too big.");
   proc.push(ans);
}

void DoInstruction() {
  int kind = prog.ins[loc];
  if (kind >= TTokenizer.sin_ && kind <= TTokenizer.EQ_) {
    DoComputation(kind);
    return;
  }
  double p1 = 0, p2 = 0, p3 = 0, addr = 0, ans;
  int i;
  String str;
  boolean killed;
  if (kind >= TTokenizer.Fork_ && kind <= TTokenizer.hsb_) {
    p1 = proc.pop();
    if (Double.isNaN(p1))
      doError("Uninitialized value used as subroutine parameter.");
    if (kind >= TTokenizer.Move_) {
      p2 = proc.pop();
      if (Double.isNaN(p2))
          doError("Uninitialized value used as subroutine parameter.");
      if (kind >= TTokenizer.rgb_) {
         p3 = proc.pop();
         if (Double.isNaN(p3))
            doError("Uninitialized value used as subroutine parameter.");
      }
    }
  }
  switch (kind) {
   case TTokenizer.Jump_: 
     proc.programCounter = prog.data[loc];
     break;
   case TTokenizer.JumpToSubroutine_:
     proc.recursionDepth++;
     if (proc.recursionDepth > TStack.maxRecursionDepth)
        doError("Your program has exceeded the maximum allowed subroutine call depth of " + TStack.maxRecursionDepth + ".  Check for 'infinite recursion.'");
     proc.programCounter = prog.data[loc];
     proc.stackRef = proc.stackTop - 1;
     break;
   case TTokenizer.SubroutineSetup_:
   case TTokenizer.FunctionSetup_:
     if (kind == TTokenizer.FunctionSetup_)
       proc.push(Double.NaN);
     proc.push(proc.stackRef);
     proc.push(proc.programCounter);
     break;
   case TTokenizer.AdjustSavedPC_: 
     proc.stack[proc.stackTop - proc.stackStart - prog.data[loc] - 1] = proc.programCounter + 1;
     break;
   case TTokenizer.SetStackRef_:
     proc.stackRef = proc.stackTop - prog.data[loc] - 1;
     break;
   case TTokenizer.Reserve_: 
     for (i = 1; i<= prog.data[loc] - topProc.stackTop; i++)
       proc.push(Double.NaN);
     break;
   case TTokenizer.HideTurtle_:
     DoEraseTurtle();
     proc.turtleIsVisible = false;
     break;
   case TTokenizer.ShowTurtle_:
     proc.turtleIsVisible = true;
     DoDrawTurtle(true);
     break;
   case TTokenizer.Clear_: 
     DoClear();
     break;
   case TTokenizer.Home_: 
     DoHome();
     break;
   case TTokenizer.PenUp_: 
     proc.turtleIsDrawing = false;
     break;
   case TTokenizer.PenDown_: 
     proc.turtleIsDrawing = true;
     break;
   case TTokenizer.red_:
     proc.color = Color.red;
     break;
   case TTokenizer.blue_:
     proc.color = Color.blue;
     break;
   case TTokenizer.green_:
     proc.color = Color.green;
     break;
   case TTokenizer.cyan_:
     proc.color = Color.cyan;
     break;
   case TTokenizer.magenta_:
     proc.color = Color.magenta;
     break;
   case TTokenizer.yellow_:
     proc.color = Color.yellow;
     break;
   case TTokenizer.gray_:
     proc.color = Color.gray;
     break;
   case TTokenizer.darkGray_:
     proc.color = Color.darkGray;
     break;
   case TTokenizer.lightGray_:
     proc.color = Color.lightGray;
     break;
   case TTokenizer.black_:
     proc.color = Color.black;
     break;
   case TTokenizer.white_:
     proc.color = Color.white;
     break;
   case TTokenizer.Die_: 
     DoKillProcess();
     break;
   case TTokenizer.Halt_:
     KillAllProcesses();
     setRunning(false);
     break;
   case TTokenizer.return_:
   case TTokenizer.returnFromFunction_: 
     DoReturn(prog.data[loc]);
     break;
   case TTokenizer.Fork_: 
     DoFork((int)Math.round(p1));
     break;
   case TTokenizer.JumpIf_: 
     if (p1 == 1)
       proc.programCounter = prog.data[loc];
     break;
   case TTokenizer.Pop_: 
     proc.store(prog.data[loc], p1);
     break;
   case TTokenizer.PopRefParam_:
     addr = proc.fetch((int)prog.data[loc]);
     proc.store((int)Math.round(addr), p1);
     break;
   case TTokenizer.PopFunctionValue_: 
     proc.store(-(proc.stackRef - 2), p1);
     break;
   case TTokenizer.Grab_: 
     if (!Semaphore.get((int)prog.data[loc])) {
       Semaphore.set((int)prog.data[loc]);
       proc.programCounter++; // bypass jump instruction
     }
     break;
   case TTokenizer.endGrab_: 
     Semaphore.clear(prog.data[loc]);
     break;
   case TTokenizer.Forward_: 
     DoForward(p1);
     break;
   case TTokenizer.Back_: 
     DoForward(-p1);
     break;
   case TTokenizer.Turn_: 
     DoFace(p1 + proc.turtleHeading);
     break;
   case TTokenizer.Face_: 
     DoFace(p1);
     break;
   case TTokenizer.Circle_: 
     DoArc(p1, 360);
     break;
   case TTokenizer.Move_: 
     DoMoveTo(p2 + proc.turtleX, p1 + proc.turtleY);
     break;
   case TTokenizer.MoveTo_: 
     DoMoveTo(p2, p1);
     break;
   case TTokenizer.Arc_:
     if (Math.abs(p2) > 360)
        p2 = 360;
     DoArc(p2, p1);
     break;
   case TTokenizer.rgb_:
   case TTokenizer.hsb_:
     float a1 = (float)Math.max(0.0,Math.min(p1,1.0));
     float a2 = (float)Math.max(0.0,Math.min(p2,1.0));
     float a3 = (float)Math.max(0.0,Math.min(p3,0.95));
     proc.color = (kind == TTokenizer.rgb_)? new Color(a3,a2,a1) : Color.getHSBColor(a3,a2,a1);
     break;
   case TTokenizer.Push_:
     p1 = proc.fetch((int)prog.data[loc]);
     proc.push(p1);
     break;
   case TTokenizer.PushRefParam_:
     addr = proc.fetch((int)prog.data[loc]);
     p1 = proc.fetch((int)Math.round(addr));
     proc.push(p1);
     break;
   case TTokenizer.PushAbsoluteReference_: 
     if (prog.data[loc] < 0)
       proc.push(prog.data[loc]);
     else
       proc.push(-(proc.stackRef + prog.data[loc]));
     break;
   case TTokenizer.PushConstant_: 
     proc.push(prog.constantStore[(int)prog.data[loc]]);
     break;
   case TTokenizer.PushDummy_: 
     proc.push(Double.NaN);
     break;
   case TTokenizer.AskUser_:
     str = MakeString((int)prog.data[loc]);
     ans = DoAskUser(str);
     if (getRunning())
       proc.store((int)p1, ans);
     break;
   case TTokenizer.YesOrNo_:
     str = MakeString((int)prog.data[loc]);
     ans = DoYesOrNo(str);
     proc.store((int)p1, ans);
     break;
   case TTokenizer.TellUser_:
     str = MakeString((int)prog.data[loc]);
     DoTellUser(str);
     break;
   case TTokenizer.DrawText_:
     str = MakeString((int)prog.data[loc]);
     DoDrawText(str);
     break;
   case TTokenizer.Random_: 
     proc.push(Math.random());
     break;
   case TTokenizer.turtleX_: 
     proc.push(Math.round(10000 * proc.turtleX) / 10000.0);
     break;
   case TTokenizer.turtleY_: 
     proc.push(Math.round(10000 * proc.turtleY) / 10000.0);
     break;
   case TTokenizer.turtleHeading_: 
     proc.push(Math.round(10000 * proc.turtleHeading) / 10000);
     break;
   case TTokenizer.TurtleIsVisible_: 
     proc.push( proc.turtleIsVisible? 1 : 0 );
     break;
   case TTokenizer.TurtleIsDrawing_: 
     proc.push( proc.turtleIsDrawing? 1 : 0 );
     break;
   case TTokenizer.forkNumber_: 
     proc.push(proc.forkNumber);
 }
}


}