package tmcm;
import java.awt.*;

public class Apps extends Frame {

   public static void main(String[] args) {
      new Apps();
   }

   String[] buttonNames = { "Launch DataReps", "Launch xLogicCircuits", "Launch xComputer", 
                            "Launch xTuringMachine", "Launch xTurtle", "Launch xSortLab", 
                            "Launch xModels" };
                            
   public Apps() {
      super("TMCM Apps");
      setLayout(new GridLayout(7,1));
      for (int i = 0; i < 7; i++)
         add (new Button(buttonNames[i]));
      reshape(100,100,200,200);
      setResizable(false);
      show();
   }
   
   public boolean action(Event evt, Object arg) {
      if (evt.target instanceof Button) {
         String buttonName = (String)arg;
         if (buttonName.equals("Launch DataReps"))
            new DataRepsFrame();
         else if (buttonName.equals("Launch xLogicCircuits"))
            new xLogicCircuitsFrame(null);
         else if (buttonName.equals("Launch xComputer"))
            new xComputerFrame(null,null);
         else if (buttonName.equals("Launch xTuringMachine"))
            new xTuringMachineFrame(null,null);
         else if (buttonName.equals("Launch xTurtle"))
            new xTurtleFrame(null,null);
         else if (buttonName.equals("Launch xSortLab"))
            new xSortLabFrame();
         else if (buttonName.equals("Launch xModels"))
            new xModelsFrame(null,null);
      }
      return true;
   }
   
   public boolean handleEvent(Event evt) {
      if (evt.id == Event.WINDOW_DESTROY) {
         dispose();
         System.exit(0);
         return true;
      }
      else  
         return super.handleEvent(evt);
   }

}
