
package tmcm.xTurtle;

import java.awt.*;

public class TestXTurtle {
   public static void main(String[] args) {
   
      Frame frame = new Frame("Turtle Test");
      xTurtleMainPanel tp = new xTurtleMainPanel();
      frame.add("Center",tp);
      frame.resize(550,430);
      frame.setResizable(false);
      frame.show();
      tp.start();
   
/*      Console console = new Console();
      TParser parser = new TParser();
      TSymbolTable ST = new TSymbolTable();
      TProgram prog = parser.parse("declare x,y,z", ST);
      TStack proc = new TStack();
      
      TTurtleHandler TH = new TTurtleHandler();
      frame.add("Center",TH);
      frame.resize(300,250);
      frame.setResizable(false);
      frame.show();
      
      TProcess runner = new TProcess(prog,proc,TH,0,TH);
      runner.start();
      try {
         runner.join();
      }
      catch (InterruptedException e) {
      }
      do {
         console.putln();
         console.put("? ");
         String str = console.getln();
         if (str.equals(""))
            break;
         try {
            parser.parseAppend(str,ST,prog);
         }
         catch (TError e) {
           console.putln('^', e.pos + 2);
           console.putln("Compilation error at position " + e.pos + ": " + e.getMessage());
           continue;
         }
         try {
            runner = new TProcess(prog,proc,TH,prog.appendStart,TH);
//            runner.setDelay(250);
//            runner.setRandomScheduling(false);
            runner.start();
            runner.join();
         }
         catch (InterruptedException e) {
         }
         catch (TError e) {
           console.putln('^', e.pos + 2);
           console.putln("Runtime error at position " + e.pos + ": " + e.getMessage());
           continue;
         }
      } while (true);
      console.close();
      frame.dispose();
*/
   }
}