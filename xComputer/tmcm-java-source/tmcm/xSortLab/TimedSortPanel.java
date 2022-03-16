package tmcm.xSortLab;
import java.awt.*;

class TimedSortPanel extends Panel {

   final static String[] sortName = { "Bubble Sort", "Selection Sort",
                                         "Insertion Sort", "Merge Sort", "QuickSort" };

   Choice methodChoice;
   Button goButton;
   TextField arrayCountInput;
   TextField arraySizeInput;
   boolean running;
      
   TimedSort sorter;

   TimedSortPanel(LogPanel log) {
      setLayout(new BorderLayout(5,5));
      sorter = new TimedSort(this,log);
      add("Center",sorter);
      Panel bottom = new Panel();
      methodChoice = new Choice();
      for (int i = 0; i < sortName.length; i++)
         methodChoice.addItem(sortName[i]);
      bottom.add(methodChoice);
      goButton = new Button("Start Sorting");
      bottom.add(goButton);
      add("South",bottom);
      Panel top = new Panel();
      top.add(new Label("Array size:"));
      arraySizeInput = new TextField("1000",6);
      top.add(arraySizeInput);
      top.add(new Label("    Number of arrays:"));
      arrayCountInput = new TextField("1",6);
      top.add(arrayCountInput);
      add("North",top);
   }


   void aboutToShow() {
      sorter.sortMethod = -1;
      goButton.setLabel("Start Sorting");
      goButton.enable();
      arraySizeInput.requestFocus();
      running = false;
   }
   
   void aboutToHide() {
      if (sorter.getState() == TimedSort.RUN)
         sorter.setState(TimedSort.ABORT);
   }
   

   void doneRunning() {  // from sorter
      goButton.setLabel("Start Sorting");
      goButton.enable();
      arraySizeInput.requestFocus();
      running = false;
   }

   void readyToStart() { // called from sorter
      goButton.enable();
   }
   
   void startSorter() {
      int method = methodChoice.getSelectedIndex();
      int arrayCt = 0;
      int arraySize = 0;
      String str;
      str = arraySizeInput.getText();
      if (str == null || str.trim().length() == 0) {
         sorter.setError("Please enter an array size!");
         arraySizeInput.requestFocus();
         return;
      }
      try {
         arraySize = Integer.parseInt(str);
      }
      catch (NumberFormatException e) {
         sorter.setError("The array size must be an integer!");
         arraySizeInput.requestFocus();
         return;
      }
      if (arraySize <= 0) {
         if (arraySize == 0)
            sorter.setError("The array size can't be zero!");
         else
            sorter.setError("The array size can't be negative!");
         arraySizeInput.requestFocus();
         return;
      }
      str = arrayCountInput.getText();
      if (str == null || str.trim().length() == 0)
         arrayCt = 1;
      else {
        try {
           arrayCt = Integer.parseInt(str);
        }
        catch (NumberFormatException e) {
           sorter.setError("The number of arrays must be an integer!");
           arrayCountInput.requestFocus();
           return;
        }
        if (arrayCt <= 0) {
           if (arrayCt == 0)
              sorter.setError("The number of arrays can't be zero!");
           else
              sorter.setError("The number of arrays can't be negative!");
           arrayCountInput.requestFocus();
           return;
        }
      }
      if (arraySize > 10000000 || arrayCt > 10000000 || (long)arraySize*(long)arrayCt > 10000000L) {
         sorter.setError("No more than ten million items, total, please!");
         arraySizeInput.requestFocus();
         return;
      }
      running = true;
      goButton.disable();
      goButton.setLabel("Abort");
      sorter.start(method,arraySize,arrayCt);
   }
   
   public boolean action(Event evt, Object arg) {
      if (evt.target == goButton) {
         if (running) {
            goButton.disable();
            sorter.setState(TimedSort.ABORT);
         }
         else
            startSorter();
         return true;
      }
      else
         return super.action(evt,arg);
   }

}