
package tmcm.xComputer;
import java.awt.*;
import java.util.Date;

class ComputerGUI extends Panel implements Runnable {

   MemoryPanel memory;
   Panel controlsAndRegisters;
   
   Button clearMemBttn, disassembleBttn;
   Button runBttn,stepBttn,cycleBttn;
   Button loadBttn, loadPCBttn, zeroPCBttn;
   Button loadFileBttn, newProgramBttn;
   
   Choice registerDisplayChoice;
   int currentRegDisplay;
   Choice speedChoice;
   int currentSpeed;
   int speedDelay;
   int flashCount;
   
   boolean running;
   
   boolean computerHalted;
   
   TextField addrInput, dataInput;
   
   Register[] registers;
   
   Label errorMessage;
   boolean errorMessageShown;
   
   int memoryDumpCount;
   Parser parser = new Parser();
   
   Thread runner;
   Converter convert = new Converter();
   
   xComputerPanel owner;
   boolean canLoad = true;
   
   final static int    // registers
         _x = 0, _y = 1, _flag = 2, _ac = 3,
         _count = 4, _ir = 5, _pc = 6, _addr = 7;

   public ComputerGUI(xComputerPanel owner) {
   
      Panel temp;
   
      this.owner = owner;
      setBackground(Color.lightGray);
      setLayout(null);
      memory = new MemoryPanel();
      add(memory);
      controlsAndRegisters = new Panel();
      add(controlsAndRegisters);
      
      controlsAndRegisters.setLayout(new GridLayout(2,1,15,15));
      Panel controls = new Panel();
      controls.setLayout(new GridLayout(5,2,3,3));
      controlsAndRegisters.add(controls);
      Panel registerPanel = new Panel();
      registerPanel.setLayout(new GridLayout(5,2,6,6));
      controlsAndRegisters.add(registerPanel);
      
      Font bigf = getFont();
      if (bigf == null)
          bigf = new Font("TimesRoman",Font.BOLD,16);
      else
          bigf = new Font("TimesRoman",bigf.BOLD,(int)(1.6*bigf.getSize()));
      Label label;
      
      label = new Label("Controls",Label.CENTER);
      label.setForeground(Color.red);
    //  label.setBackground(new Color(255,225,225));
      label.setFont(bigf);
      controls.add(label);
      
      label = new Label("Registers",Label.CENTER);
      label.setForeground(Color.red);
    //  label.setBackground(new Color(255,225,225));
      label.setFont(bigf);
      registerPanel.add(label);
      
      registerDisplayChoice = new Choice();
      registerDisplayChoice.addItem("Default display");
      registerDisplayChoice.addItem("Integer display");
      registerDisplayChoice.addItem("Unsigned display");
      registerDisplayChoice.addItem("Binary display");
      temp = new Panel();
      temp.add(registerDisplayChoice);
      registerPanel.add(temp);
      
      registers = new Register[8];
      registers[_x] = new Register("X",16,Register.integerView);
      registers[_y] = new Register("Y",16,Register.integerView);
      registers[_flag] = new Register("FLAG",1,Register.integerView);
      registers[_ac] = new Register("AC",16,Register.integerView);
      registers[_count] = new Register("COUNT",4,Register.integerView);
      registers[_ir] = new Register("IR",16,Register.assemblyView);
      registers[_pc] = new Register("PC",10,Register.integerView);
      registers[_addr] = new Register("ADDR",10,Register.integerView);
      
      registerPanel.add(registers[_x]);
      registerPanel.add(registers[_count]);
      registerPanel.add(registers[_y]);
      registerPanel.add(registers[_ir]);
      registerPanel.add(registers[_flag]);
      registerPanel.add(registers[_pc]);
      registerPanel.add(registers[_ac]);
      registerPanel.add(registers[_addr]);


      runBttn = new Button("Run");
      stepBttn = new Button("Step");
      cycleBttn = new Button("Cycle");
      clearMemBttn = new Button("Clear Mem");
      disassembleBttn = new Button("Disassemble");
      newProgramBttn = new Button("New Prgm");
      loadFileBttn = new Button("Load File");
      loadBttn = new Button("Data to Memory");
      loadPCBttn = new Button("Addr to PC");
      zeroPCBttn = new Button("Set PC = 0");
      
      speedChoice = new Choice();
      speedChoice.addItem("Fastest Speed");
      speedChoice.addItem("Fast Speed");
      speedChoice.addItem("Moderate Speed");
      speedChoice.addItem("Slow Speed");
      speedChoice.addItem("Slower Speed");
      speedChoice.select(2);
      setCurrentSpeed(2);

      errorMessage = new Label("",Label.CENTER);
      errorMessage.setForeground(Color.red);
      
      addrInput = new TextField();
      addrInput.setText("0");
      dataInput = new TextField();
      
      Label addrLabel = new Label("addr:");
      addrLabel.setForeground(Color.blue);
      Label dataLabel = new Label("data:");
      dataLabel.setForeground(Color.blue);
            
      controls.add(errorMessage);
      
      temp = new Panel();
      temp.setLayout(new GridLayout(1,3,3,3));
      temp.add(runBttn);
      temp.add(stepBttn);
      temp.add(cycleBttn);
      controls.add(temp);
      
      temp = new Panel();
      temp.setLayout(new BorderLayout(3,3));
      temp.add("West",addrLabel);
      temp.add("Center",addrInput);
      controls.add(temp);

      temp = new Panel();
      temp.add(speedChoice);
      controls.add(temp);
      
      temp = new Panel();
      temp.setLayout(new BorderLayout(3,3));
      temp.add("West",dataLabel);
      temp.add("Center",dataInput);
      controls.add(temp);

      temp = new Panel();
      temp.setLayout(new GridLayout(1,2,3,3));
      temp.add(newProgramBttn);
      temp.add(loadFileBttn);
      controls.add(temp);

      controls.add(loadBttn);      

      temp = new Panel();
      temp.setLayout(new GridLayout(1,2,3,3));
      temp.add(disassembleBttn);
      temp.add(clearMemBttn);
      controls.add(temp);

      temp = new Panel();
      temp.setLayout(new GridLayout(1,2,3,3));
      temp.add(loadPCBttn);
      temp.add(zeroPCBttn);
      controls.add(temp);
      

   }
   
   synchronized void addErrorMessage(String message) {
      if (errorMessageShown) {
        errorMessage.setText("");
        try { Thread.sleep(200); }
        catch (InterruptedException e) { }      
      }
      errorMessage.setText(message);
      try { Thread.sleep(150); }
      catch (InterruptedException e) { }
      errorMessage.setText("");
      try { Thread.sleep(150); }
      catch (InterruptedException e) { }
      errorMessage.setText(message);
      try { Thread.sleep(150); }
      catch (InterruptedException e) { }
      errorMessage.setText("");
      try { Thread.sleep(150); }
      catch (InterruptedException e) { }
      errorMessage.setText(message);
      errorMessageShown = true;
   }
   
   synchronized void removeErrorMessage() {
      if (errorMessageShown) {
         errorMessage.setText("");
         errorMessageShown = false;
      }
   }
   
   public void reshape(int x, int y, int width, int height) {
      super.reshape(x,y,width,height);
      int memWidth = (width - 12) / 3;
      memory.reshape(width-memWidth-4,4,memWidth,height-8);
      controlsAndRegisters.reshape(4,4,width-memWidth-20,height-8);
   }
   
   public void start() {
      dataInput.requestFocus();
   }
   
   public void run() {
      long lastStepTime = System.currentTimeMillis();  // for speeds < fastest
      int consecStepCt = 0;  // for speed = fastest
      try {
       while (getRunning()) {
          doStep();
          if (computerHalted) {
             runBttn.disable();
             addErrorMessage("Clock Stopped");
             setRunning(false);
             cleanUpAfterRun();
             return;
          }
          long delay = getSpeedDelay();
          if (delay == 0) {
             consecStepCt++;
             if (consecStepCt >= 100) {
                 synchronized(this) {
                    try { wait(10); }
                    catch (InterruptedException e) { }
                 }
                 consecStepCt = 0; 
             }
          }
          else {
             long time = System.currentTimeMillis();
             delay = delay - (time - lastStepTime);
             lastStepTime = time;
             if (delay < 10)
                delay = 10;
             synchronized(this) {
                try { wait(delay); }
                catch (InterruptedException e) { }
             }
            lastStepTime = System.currentTimeMillis();
          }
       }
      }
      finally {
         if (getRunning() == true)
            cleanUpAfterRun();  // shouldn't happen unless there is some funny error
      }
   }
        
   void reset(short[] data) {
      memory.resetMem(data);
      if (registers[_count].get() != 0) {
         registers[_count].set((short)0,0);
         memory.setControlWires(Globals.step[0][0][0]);
      }
      if (registers[_pc].get() != 0)
         registers[_pc].set((short)0,0);
   }
   
   synchronized void setRegister(int whichReg, short val) {
      registers[whichReg].set(val,flashCount);
      if (whichReg == _addr)
         memory.setAddress(val);
      else if (whichReg == _count || whichReg == _ir) {
         int ir = registers[_ir].get();
         int count = registers[_count].get();
         int ins = (ir >> 10) & 15;
         int mode = (ir & 0xC000) >> 14;
         if (ins <= Globals._jmp)  
            memory.setControlWires(Globals.step[ins][mode][count]);
         else {
            if ( (mode == Globals.direct && count == 4) || (mode == Globals.indirect && count == 5) ) {
               if ( (ins == Globals._jmz && registers[_ac].get() != 0) ||
                        (ins == Globals._jmn && registers[_ac].get() >= 0) ||
                            (ins == Globals._jmf && registers[_flag].get() == 0) )
                  memory.setControlWires(Globals.step[0][0][0]);
               else
                  memory.setControlWires(Globals.step[ins][mode][count]);
            }
            else
               memory.setControlWires(Globals.step[ins][mode][count]);
         }
      }
   }
   
   int getAddr() {  // return -1 to indicate error in contents of addrInput
      String data = addrInput.getText().trim();
      if (data.length() == 0) {
         addrInput.selectAll();
         addrInput.requestFocus();
         addErrorMessage("No Address!");
         return -1;
      }
      for (int i = 0; i < data.length(); i++)
         if (!Character.isDigit(data.charAt(i))) {
            addrInput.selectAll();
            addrInput.requestFocus();
            addErrorMessage("Addr is not a number!");
            return -1;
         }
      int n = 0;
      for (int i = 0; i < data.length(); i++) {
         n = 10*n + Character.digit(data.charAt(i),10);
         if (n > 1023) {
            addrInput.selectAll();
            addrInput.requestFocus();
            addErrorMessage("Addr too large!");
            return -1;
         }
      }
      return n;
   }
   
   void doDataToMem() {
      String data = dataInput.getText();
      short val = 0;
      try {
         val = parser.parseOneInstruction(data);
      }
      catch (ParseError e) {
         dataInput.selectAll();
         dataInput.requestFocus();
         addErrorMessage("Illegal Data!");
         return;
      }
      int n = getAddr();
      if (n == -1)
        return;  // error in addr
      setRegister(_addr,(short)n);
      memory.set(n,val,flashCount);
      if (registers[_count].get() != 0)
         setRegister(_count,(short)0);
      if (n == 1023)
         addrInput.setText("0");
      else
         addrInput.setText(String.valueOf(n+1));
      dataInput.selectAll();
      dataInput.requestFocus();
   }
   
   public boolean action(Event evt, Object arg) {
      removeErrorMessage();
      if (evt.target instanceof TextField) {
         if (evt.target == dataInput) {
            doDataToMem();
         }
         else {
            dataInput.selectAll();
            dataInput.requestFocus();
         }
         return true;
      }
      if (evt.target instanceof Button) {
         if (evt.target == runBttn) {
               if (getRunning())
                  stopRunning();
               else
                  doRun();
         }
         else if (evt.target == newProgramBttn) {
               owner.doNewProgram(null,null);
         }
         else if (evt.target == loadFileBttn) {
               if (canLoad)
                  owner.doLoadCommandFromComputer();
         }
         else if (evt.target == stepBttn) {
               computerHalted = false;
               doStep();
               if (computerHalted)
                  addErrorMessage("Clock Stopped");
         }
         else if (evt.target == cycleBttn) {
               doCycle();
         }
         else if (evt.target == loadBttn) {
               doDataToMem();
         }
         else if (evt.target == loadPCBttn) {
                int n = getAddr();
                if (n != -1) {
                    setRegister(_pc,(short)n);
                    setRegister(_count,(short)0);
                }
         }
         else if (evt.target == zeroPCBttn) {
               setRegister(_pc,(short)0);
               setRegister(_count,(short)0);
         }
         else if (evt.target == clearMemBttn) {
               memory.clear();
         }
         else if (evt.target == disassembleBttn) {
               doDisassemble();
         }
         return true;
      }
      if (evt.target instanceof Choice) {
         if (evt.target == speedChoice) {
            setCurrentSpeed(speedChoice.getSelectedIndex());
         }
         else if (evt.target == registerDisplayChoice) {
            int s = registerDisplayChoice.getSelectedIndex();
            if (s != currentRegDisplay) {
               for (int i = 0; i < 8; i++)
                  registers[i].setDisplayStyle(s);
               currentRegDisplay = s;
            }
         }
         return true;
      }   
      return super.action(evt,arg);
   }
   
   synchronized void setRunning(boolean r) {
      running = r;
      notify();
   }
   
   
   synchronized boolean getRunning() {
      return running;
   }
   
   synchronized void setCurrentSpeed(int speed) {
      if (speed == currentSpeed)
         return;
      if (currentSpeed == 0 && running) {
         for (int i = 0; i<8; i++)
            registers[i].dim(false);
      }
      if (speed == 0 && running) {
         for (int i = 0; i<8; i++)
            registers[i].dim(true);
      }
      currentSpeed = speed;
      switch (speed) {
          case 0:
             speedDelay = 0;
             break;
          case 1:
             speedDelay = 10;
             break;
          case 2:
             speedDelay = 200;
             break;
          case 3:
             speedDelay = 500;
             break;
          case 4:
             speedDelay = 1000;
             break;
      }
      if (running) {
         if (speed < 2)
            flashCount = 0;
         else if (speed == 2)
            flashCount = 1;
         else
            flashCount = 2;
         notify();
      }
      else
         flashCount = 2;
   }
   
   synchronized int getCurrentSpeed() {
      return currentSpeed;
   }
   
   synchronized int getSpeedDelay() {
      return speedDelay;
   }
      
   void doRun() {
      clearMemBttn.disable();
      disassembleBttn.disable();
      newProgramBttn.disable();
      loadFileBttn.disable();
      runBttn.setLabel("Stop");
      runBttn.disable();
      stepBttn.disable();
      cycleBttn.disable();
      loadBttn.disable();
      loadPCBttn.disable();
      zeroPCBttn.disable();
      dataInput.setEditable(false);
      addrInput.setEditable(false);
      if (getCurrentSpeed() == 0) {
         for (int i = 0; i < 8; i++)
            registers[i].dim(true);
      }
      if (getCurrentSpeed ()< 2)
         flashCount = 0;
      else if (getCurrentSpeed() == 2)
         flashCount = 1;
      else
         flashCount = 2;
      computerHalted = false;
      runner = new Thread(this);
      setRunning(true);
      runner.start();
      runBttn.enable();
   }
   
   void stopRunning() {
      runBttn.disable();
      if (runner != null && runner.isAlive()) {
         setRunning(false);
         try {
            runner.join(1000);
         }
         catch (InterruptedException e) { }
         if (runner.isAlive())
            runner.stop();
      }
      cleanUpAfterRun();
   }
   
   void cleanUpAfterRun() {
      runBttn.enable();
      runBttn.setLabel("Run");
      runner = null;
      flashCount = 2;
      clearMemBttn.enable();
      disassembleBttn.enable();
      stepBttn.enable();
      cycleBttn.enable();
      loadBttn.enable();
      loadPCBttn.enable();
      newProgramBttn.enable();
      if (canLoad)
         loadFileBttn.enable();
      zeroPCBttn.enable();
      dataInput.setEditable(true);
      addrInput.setEditable(true);
      for (int i = 0; i < 8; i++)
         registers[i].dim(false);
   }
   
   void doDisassemble() {
      StringBuffer prog = new StringBuffer(10000);
      String eol = System.getProperty("line.separator");
      prog.append("; xComputer Memory Dump" + eol);
      prog.append("; " + (new Date()) + eol + eol);
      short[] data = memory.data;
      int top = 1023;
      while (top >= 0 && data[top] == 0)
         top--;
      if (top < 0) {
         prog.append("; All data in memory are 0" + eol + eol);
         prog.append("ADD 0" + eol);
      }
      else {
         for (int i = 0; i <= top; i++) {
            convert.set(data[i]);
            int mode = convert.getMode();
            if ( (mode == Globals.direct && Globals.hasData.get(convert.getInstruction())) ||
                 (mode != Globals.illegal && convert.getData() == 0) ||
                 (mode == Globals.indirect && Globals.hasIndirectMode.get(convert.getInstruction())) ||
                 (mode == Globals.constant && Globals.hasConstantMode.get(convert.getInstruction())) )
               prog.append(convert.getAssembly() + eol);
            else
               prog.append(data[i] + eol);
         }
      }
      memoryDumpCount++;
      owner.doNewProgram("Memory Dump " + memoryDumpCount, prog.toString());
   }
   
   void doCycle() {
      flashCount = -1;
      computerHalted = false;
      for (int i = 0; i < 10; i++) {  // i will never get to 10
         doStep();
         if (registers[_count].get() == 2 || computerHalted)
            break;
         try { Thread.sleep(50); }
         catch (InterruptedException e) { }
      }
      flashCount = -1;
      if (computerHalted)
         addErrorMessage("Clock Stopped");
   }
   
//---------- The following came more-or-less directly from the Pascal version!!-----

void doWire (int wr) {
  int a, b, ans, flag;
  boolean loadFlag;
  switch (wr) {
    case Globals.load_data_into_memory: 
       memory.set(registers[_addr].get(), registers[_ac].get(), flashCount);
       break;
    case Globals.load_x_from_ac: 
       RegToReg(_ac, _x);
       break;
    case Globals.load_y_from_memory: 
       setRegister(_y, memory.get(registers[_addr].get()));
       break;
    case Globals.load_y_from_ir: 
       RegToReg(_ir, _y);
       break;
    case Globals.select_add:
    case Globals.select_subtract:
    case Globals.select_and:
    case Globals.select_or:
    case Globals.select_not:
    case Globals.select_shift_right:
    case Globals.select_shift_left:
	     a = registers[_x].get();
	     b = registers[_y].get();
	     loadFlag = false;
	     flag = 0;
	     if (Globals.select_add == wr) {
	       ans = a + b;
	       loadFlag = true;
	     }
	     else if (Globals.select_subtract == wr) {
	       ans = a - b;
	       loadFlag = true;
	     }
	     else if (Globals.select_and == wr)
	       ans = a & b;
	     else if (Globals.select_or == wr)
 	       ans = a | b;
	     else if (Globals.select_not == wr)
	       ans = ~a;
	     else if (Globals.select_shift_right == wr) {
	       flag = (a & 1);
	       ans = ((a >>> 1) & 0x7FFF);
	       loadFlag = true;
	     }
	     else if (Globals.select_shift_left == wr) {
	       loadFlag = true;
	       ans = a << 1;
	     }
	     else
	       ans = 0;
	     setRegister(_ac, (short)ans);
	     if (loadFlag)
	        if (((ans & 0x10000) != 0) || (flag != 0))
	            setRegister(_flag, (short)1);
	        else
	            setRegister(_flag, (short)0);
         break;
    case Globals.load_ac_from_memory: 
       setRegister(_ac, memory.get(registers[_addr].get()));
       break;
    case Globals.load_ac_from_ir: 
       RegToReg(_ir, _ac);
       break;
    case Globals.increment_ac: 
       setRegister(_ac, (short)(registers[_ac].get() + 1));
       break;
    case Globals.decrement_ac: 
       setRegister(_ac, (short)(registers[_ac].get() - 1));
       break;
    case Globals.load_flag_from_alu: 
       break;
    case Globals.load_pc_from_memory: 
       setRegister(_pc, memory.get(registers[_addr].get()));
       break;
    case Globals.load_pc_from_ir: 
       RegToReg(_ir, _pc);
       break;
    case Globals.increment_pc: 
       setRegister(_pc, (short)(registers[_pc].get() + 1));
       break;
    case Globals.load_ir_from_memory: 
       setRegister(_ir, memory.get(registers[_addr].get()));
       break;
    case Globals.load_addr_from_ir: 
       RegToReg(_ir, _addr);
       break;
    case Globals.load_addr_from_pc: 
       RegToReg(_pc, _addr);
       break;
    case Globals.load_addr_from_y: 
       RegToReg(_y, _addr);
       break;
    case Globals.set_count_to_zero: 
       setRegister(_count,(short)0);
       break;
    case Globals.stop_clock: 
       computerHalted = true;
       break;
  }
}

final void RegToReg(int r1, int r2) {
   short val;
   if (r1 == _ir)
      val = (short)(registers[_ir].get() & 0x3FF);
   else
      val = registers[r1].get();
   setRegister(r2,val);
}

void doDirectStep(int ins, int ct) {
  switch (ct) {
   case 4: 
	  switch (ins) {
	    case Globals._add:
	    case Globals._sub:
	    case Globals._and:
	    case Globals._or:
	    case Globals._sto:
	    case Globals._lod: 
	         doWire(Globals.load_addr_from_ir);
	         break;
	    case Globals._not:
	    case Globals._shl:
	    case Globals._shr: 
  	         doWire(Globals.load_x_from_ac);
	         break;
	    case Globals._inc: 
	         doWire(Globals.increment_ac);
	         break;
	    case Globals._dec: 
	         doWire(Globals.decrement_ac);
	         break;
	    case Globals._hlt: 
	         doWire(Globals.stop_clock);
	         break;
	    case Globals._jmp: 
	         doWire(Globals.load_pc_from_ir);
	         break;
	    case Globals._jmf: 
	         if (registers[_flag].get() != 0)
	            doWire(Globals.load_pc_from_ir);
	         break;
	    case Globals._jmz: 
	         if (registers[_ac].get()  == 0)
	             doWire(Globals.load_pc_from_ir);
	         break;
	    case Globals._jmn: 
	         if (registers[_ac].get() < 0)
	             doWire(Globals.load_pc_from_ir);
	         break;
	    default:
	      doWire(Globals.set_count_to_zero);
      }
      break;
   case 5: 
    switch (ins) {
	    case Globals._add:
	    case Globals._sub:
	    case Globals._and:
	    case Globals._or:
	         doWire(Globals.load_x_from_ac);
	         doWire(Globals.load_y_from_memory);
	         break;
	    case Globals._not: 
	         doWire(Globals.select_not);
	         break;
	    case Globals._shl: 
	         doWire(Globals.select_shift_left);
	         break;
	    case Globals._shr: 
	         doWire(Globals.select_shift_right);
	         break;
	    case Globals._lod: 
	         doWire(Globals.load_ac_from_memory);
	         break;
	    case Globals._sto: 
	         doWire(Globals.load_data_into_memory);
	         break;
	    default:
	         doWire(Globals.set_count_to_zero);
    }
    break;
   case 6: 
    switch (ins) {
	    case Globals._add: 
	         doWire(Globals.select_add);
	         break;
	    case Globals._sub: 
	         doWire(Globals.select_subtract);
	         break;
	    case Globals._and: 
	         doWire(Globals.select_and);
	         break;
	    case Globals._or: 
	         doWire(Globals.select_or);
	    case Globals._not:
	    case Globals._shl:
	    case Globals._shr: 
	         break;
	    default:
	      doWire(Globals.set_count_to_zero);
    }
    break;
   case 7: 
    switch (ins) {
	    case Globals._add:
	    case Globals._sub:
	    case Globals._and:
	    case Globals._or: 
	         break;
	    default:
 	         doWire(Globals.set_count_to_zero);
	 }
	 break;
   default:
	    doWire(Globals.set_count_to_zero);
  }
}
 
void doIndirectStep(int ins, int ct) {
  switch (ct) {
   case 4: 
    if (Globals.hasIndirectMode.get(ins))
        doWire(Globals.load_addr_from_ir);
    else
        doWire(Globals.set_count_to_zero);
    break;
   case 5: 
    switch (ins) {
	    case Globals._add:
	    case Globals._sub:
	    case Globals._and:
	    case Globals._or:
	    case Globals._lod:
	    case Globals._sto: 
	         doWire(Globals.load_y_from_memory);
	         break;
	    case Globals._jmp: 
	         doWire(Globals.load_pc_from_memory);
	         break;
	    case Globals._jmf: 
	         if (registers[_flag].get()  != 0)
	            doWire(Globals.load_pc_from_memory);
	         break;
	    case Globals._jmz: 
	         if (registers[_ac].get()  == 0)
	            doWire(Globals.load_pc_from_memory);
	         break;
	    case Globals._jmn: 
	         if (registers[_ac].get() < 0)
	            doWire(Globals.load_pc_from_memory);
	         break;
	    default:
	         doWire(Globals.set_count_to_zero);
    }
    break;
   case 6:
      switch (ins) {
	    case Globals._add:
	    case Globals._sub:
	    case Globals._and:
	    case Globals._or:
	    case Globals._lod:
	    case Globals._sto:
           doWire(Globals.load_addr_from_y);
           break;
        default:
           doWire(Globals.set_count_to_zero);
      }
      break;
   case 7: 
    switch (ins) {
	    case Globals._add:
	    case Globals._sub:
	    case Globals._and:
	    case Globals._or:
	         doWire(Globals.load_x_from_ac);
	         doWire(Globals.load_y_from_memory);
	         break;
	    case Globals._lod: 
	         doWire(Globals.load_ac_from_memory);
	         break;
	    case Globals._sto: 
	         doWire(Globals.load_data_into_memory);
	         break;
	    default:
	         doWire(Globals.set_count_to_zero);
     }
     break;
   case 8: 
    switch (ins) {
	    case Globals._add: 
	         doWire(Globals.select_add);
	         break;
	    case Globals._sub: 
	         doWire(Globals.select_subtract);
	         break;
	    case Globals._and: 
	         doWire(Globals.select_and);
	         break;
	    case Globals._or: 
	         doWire(Globals.select_or);
	         break;
	    default:
	         doWire(Globals.set_count_to_zero);
    }
    break;
   case 9: 
       if (ins != Globals._add && ins != Globals._sub &&
                     ins != Globals._and && ins != Globals._or)
          doWire(Globals.set_count_to_zero);
  }
}

void doConstantStep(int ins, int ct) {
  switch (ct) {
   case 4: 
    switch (ins) {
	    case Globals._add:
	    case Globals._sub:
	    case Globals._and:
	    case Globals._or:
	         doWire(Globals.load_x_from_ac);
	         doWire(Globals.load_y_from_ir);
	         break;
	    case Globals._lod: 
	         doWire(Globals.load_ac_from_ir);
	         break;
	    default:
	         doWire(Globals.set_count_to_zero);
    }
    break;
   case 5: 
    switch (ins) {
	    case Globals._add: 
	         doWire(Globals.select_add);
	         break;
	    case Globals._sub: 
	         doWire(Globals.select_subtract);
	         break;
	    case Globals._and: 
	         doWire(Globals.select_and);
	         break;
	    case Globals._or: 
	         doWire(Globals.select_or);
	         break;
	    default:
	         doWire(Globals.set_count_to_zero);
    }
    break;
   case 6: 
       if (ins != Globals._add && ins != Globals._sub &&
                     ins != Globals._and && ins != Globals._or)
           doWire(Globals.set_count_to_zero);
        break;
   default:
       doWire(Globals.set_count_to_zero);
  }
}

void doStep(){
  int ct,ins;
  convert.set(registers[_ir].get());
  ins = convert.getInstruction();
  ct = registers[_count].get();
  ct++;
  setRegister(_count,(short)ct);
  switch (ct) {
   case 1: 
      doWire(Globals.load_addr_from_pc);
      break;
   case 2: 
      doWire(Globals.load_ir_from_memory);
      break;
   case 3: 
      doWire(Globals.increment_pc);
      break;
   case 4:
   case 5:
   case 6:
   case 7:
   case 8:
   case 9:
     switch (convert.getMode()) {
      case Globals.illegal: 
         doWire(Globals.set_count_to_zero);
         break;
      case Globals.direct: 
         doDirectStep(ins, ct);
         break;
      case Globals.indirect: 
         doIndirectStep(ins, ct);
         break;
      case Globals.constant: 
         doConstantStep(ins, ct);
     }
     break;
   default:
     doWire(Globals.set_count_to_zero);
  }
 }


//----------------------------------------------------------------------------------   


}  // end of class ComputerGUI