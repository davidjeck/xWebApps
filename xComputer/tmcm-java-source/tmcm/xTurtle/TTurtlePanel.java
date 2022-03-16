
package tmcm.xTurtle;

import java.awt.*;

public class TTurtlePanel extends Panel implements TurtleNotification {

   TSymbolTable ST;
   TParser parser;
   TProgram prog;
   TProcess proc;
   
   TTurtleHandler TH = null;   
   TStack stack = null;
   
   TextField command;
   Button goButton;
   
   boolean running = false;
   
   String errorString = null;
   String lastInput = null;
   
   TurtleNotification notify;
   
   TTurtlePanel(TSymbolTable ST, TParser parser, TProgram prog, TurtleNotification notify) {
      this.ST = ST;
      this.parser = parser;
      this.prog = prog;
      proc = null;
      this.notify = (notify == null)? this : notify;
      setLayout(new BorderLayout(5,5));
      setBackground(Color.lightGray);
      TH = new TTurtleHandler();
      add("Center", TH);
      Panel bottom = new Panel();
      bottom.setBackground(Color.lightGray);
      bottom.setLayout(new BorderLayout(5,5));
      command = new TextField();
      command.setBackground(Color.white);
      command.requestFocus();
      goButton = new Button("Do it!");
      bottom.add("Center",command);
      bottom.add("East",goButton);
      add("South",bottom);
   }
   
   public void startRunning(TProcess proc) {
     if (TH != null)
        TH.startRunning(proc);
   }
   
   public void doneRunning() {
      TH.doneRunning();
      goButton.setLabel("Do it!");
      command.setEditable(true);
      command.selectAll();
      command.requestFocus();
      running = false;
   }
   
   public void errorReport(String errorMessage, int position) {
      if (TH != null)
         TH.errorReport(errorMessage, position);
      goButton.setLabel("Do it!");
      command.setEditable(true);
      if (position < 0)
         command.selectAll();
      else
         command.select(position,position);
      command.requestFocus();
      running = false;
      errorString = lastInput;
   }

   void doCommand() {
      if (running)
         return;
      String str = command.getText();
      if (str == null || str.length() == 0)
         return;
      if (str.equals(errorString) && TH.turnOffError())
         return;
      errorString = null;
      if (parser == null)
         parser = new TParser();
      if (ST == null)
         ST = new TSymbolTable();
      int start;
      try {
        if (prog == null) {
            prog = parser.parse(str, ST);
            start = 0;
        }
        else {
            parser.parseAppend(str, ST, prog);
            start = prog.appendStart;
        }
      }
      catch (TError e) {
         errorReport("Compilation error: " + e.getMessage(), e.pos);
         errorString = str;
         return;
      }
      lastInput = str;
      runTheProgram(start);
   }
   
   void runTheProgram(int start) {
      if (stack == null) {
         stack = new TStack();
         TH.removeAllTurtles();
      }
      proc = new TProcess(prog,stack,TH,start,notify);
      command.setEditable(false);
      goButton.setLabel("Stop");
      running = true;
      proc.start();
   }
   
   void set(TProgram prog, TSymbolTable ST, TurtleNotification notify) {
      this.prog = prog;
      this.ST = ST;
      this.notify = notify;
      proc = null;
      stack = null;
      TH.ClearScreen();
      command.setText("");
      errorString = null;
   }
   
   public boolean action(Event evt, Object arg) {
      if (evt.target == command) {
         doCommand();
         return true;
      }
      else if (evt.target == goButton) {
         if (running)
            proc.setRunning(false);
         else
            doCommand();
         return true;
      }
      else
         return super.handleEvent(evt);
   }
   
//   public boolean gotFocus(Event evt, Object what) {
//      if (command.isEditable())
//         command.requestFocus();
//      return true;
//   }
   

}