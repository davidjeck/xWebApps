package tmcm;
import tmcm.*;
import java.awt.*;

public class tmcmLabsFrame extends Frame {

   String[] buttonNames = { "DataReps Lab",
                            "xTurtle Lab 1", 
                            "xLogicCircuits Lab 1", 
                            "xTurtle Lab 2",
                            "xLogicCircuits Lab 2",
                            "xTurtle Lab 3", 
                            "xComputer Lab 1", 
                            "xTurtle Lab 4",
                            "xComputer Lab 2",
                            "xSortLab",
                            "xComputer Lab 3", 
                            "xModels Lab 1",
                            "xTuringMachineLab",
                            "xModels Lab 2"
                          };
                          
   String[][] fileNames = {
          { }, // DataReps Lab
          { "Necklace.txt", "RandomWalk.txt", "InputOutputExample.txt" }, // xTurtle Lab 1 
          { "FirstExamples.txt" }, // xLogicCircuits Lab 1 
          { "NestedSquares.txt", "Quadratic.txt", "SpiralsSubroutine.txt" }, // xTurtle Lab 2
          { "MemoryCircuits.txt" }, // xLogicCircuits Lab 2
          { "SymmetrySubs.txt", "BinaryTrees.txt", "KochCurves.txt" }, // xTurtle Lab 3 
          { "CountAndStore.txt" }, // xComputer Lab 1 
          { "Bugs.txt", "ParallelSpectrum.txt", "TwoTasks.txt",
            "ParallelSnowflake.txt", "Circles.txt", "SynchronizedRandomWalk.txt",
            "ThreeNPlusOneMax.txt", "TwoGraphs.txt", "SumOfSquares.txt" }, // xTurtle Lab 4
          { "SimpleCounter.txt", "MultiplyByAdding.txt", "ThreeNPlusOne.txt", 
            "ListSum.txt", "PowersOfThree.txt", "CountAndStore.txt" }, // xComputer Lab 2
          { }, // xSortLab
          { "MultiplyBySeven.txt", "MultiplyTwoNumbers.txt", "ListSumSubroutine.txt", 
            "PrimesAndRemainders.txt" }, // xComputer Lab 3 
          { "Pinwheel.txt", "SimpleObjects.txt", "FirstAnimation.txt", 
            "Wagon.txt", "Houses.txt", "Bounce.txt" }, // xModels Lab 1
          { "Change01toXY.txt", "FindDoubleX.txt", "CopyXYZ.txt", 
            "Increment.txt", "AddBinaryNumbers.txt", "MultiplyByAddingTM.txt" }, // xTuringMachineLab
          { "Flaps.txt", "NestedSquares3D.txt", "LatheAndExtrude.txt", 
            "Wagon.txt" }  // xModels Lab 2
      };
                          
   Button[] buttons = new Button[14];
                            
   public tmcmLabsFrame() {
      super("TMCM Labs");
      setLayout(new GridLayout(7,2));
      for (int i = 0; i < 14; i++) {
         buttons[i] = new Button(buttonNames[i]);
         add(buttons[i]);
      }
      reshape(100,100,370,220);
      setResizable(false);
      show();
   }
   
   public boolean action(Event evt, Object arg) {
      if (evt.target instanceof Button) {
         for (int i = 0; i < 14; i++)
            if (evt.target == buttons[i])
               switch (i) {
                  case 0: DataRepsFrame.main(null); return true;
                  case 1: xTurtleFrame.main(fileNames[1]); return true;
                  case 2: xLogicCircuitsFrame.main(fileNames[2]); return true;
                  case 3: xTurtleFrame.main(fileNames[3]); return true;
                  case 4: xLogicCircuitsFrame.main(fileNames[4]); return true;
                  case 5: xTurtleFrame.main(fileNames[5]); return true;
                  case 6: xComputerFrame.main(fileNames[6]); return true;
                  case 7: xTurtleFrame.main(fileNames[7]); return true;
                  case 8: xComputerFrame.main(fileNames[8]); return true;
                  case 9: xSortLabFrame.main(null); return true;
                  case 10: xComputerFrame.main(fileNames[10]); return true;
                  case 11: xModelsFrame.main(fileNames[11]); return true;
                  case 12: xTuringMachineFrame.main(fileNames[12]); return true;
                  case 13: xModelsFrame.main(fileNames[13]); return true;
               }
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
