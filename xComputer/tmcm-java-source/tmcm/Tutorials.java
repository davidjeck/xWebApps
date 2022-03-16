package tmcm;
import java.awt.*;

public class Tutorials extends Frame {

   public static void main(String[] args) {
      new Tutorials();
   }

   String[] buttonNames = { "DataReps", 
                            "xLogicCircuits", 
                            "xComputer", 
                            "xTuringMachine", 
                            "xTurtle", "xTurtle Tutorial",
                            "xSortLab", 
                            "xModels", "xModels Tutorial" };
                            
   String[]
      xCStrings = {  "xComputerBasics.txt","Graphics.txt", "Labels.txt", "Three_N_Plus_One.txt" },
      xLCStrings = { "SampleCircuits.txt" },
      xMStrings = { "NestedSquares3D.txt", "WheelInSquare.txt", "Goblet.txt" },
      xTMStrings = { "CopyXYZ.txt", "GatherDollars.txt", "CountInBinary.txt", "BinaryAddition.txt" },
      xTStrings = { "ColoredCircles.txt", "RecursiveBush.txt", "ParallelSnowflake.txt", "PersianTiling.txt" },
      xMTutorialStrings = { "Tutorial1_xModelsBasics.txt", "Tutorial2_Animation.txt", 
                           "Tutorial3_3D.txt", "Tutorial4_ComplexObjects.txt", 
                           "Tutorial5_PolygonsEtc.txt", "Tutorial6_Segments.txt" },
      xTTutorialStrings = { "Tutorial1_xTurtleBasics.txt", "Tutorial2_variables.txt", 
                           "Tutorial3_io.txt", "Tutorial4_loop.txt", 
                           "Tutorial5_if.txt", "Tutorial6_subroutines.txt", 
                           "Tutorial7_recursion.txt", "Tutorial8_multitasking.txt" };
   
   public Tutorials() {
      super("TMCM Tutorials");
      setLayout(new GridLayout(9,1));
      for (int i = 0; i < 9; i++)
         add (new Button(buttonNames[i]));
      reshape(100,100,225,270);
      setResizable(false);
      show();
   }
   
   public boolean action(Event evt, Object arg) {
      if (evt.target instanceof Button) {
         String buttonName = (String)arg;
         if (buttonName.equals("DataReps"))
            new DataRepsFrame();
         else if (buttonName.equals("xLogicCircuits"))
            xLogicCircuitsFrame.main(xLCStrings);
         else if (buttonName.equals("xComputer"))
            xComputerFrame.main(xCStrings);
         else if (buttonName.equals("xTuringMachine"))
            xTuringMachineFrame.main(xTMStrings);
         else if (buttonName.equals("xTurtle"))
            xTurtleFrame.main(xTStrings);
         else if (buttonName.equals("xSortLab"))
            new xSortLabFrame();
         else if (buttonName.equals("xModels"))
            xModelsFrame.main(xMStrings);
         else if (buttonName.equals("xModels Tutorial"))
            xModelsFrame.main(xMTutorialStrings);
         else if (buttonName.equals("xTurtle Tutorial"))
            xTurtleFrame.main(xTTutorialStrings);
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
