package tmcm.xSortLab;
import java.awt.*;

class VisualSortPanel extends Panel {

   final static String[] sortName = { "Bubble Sort", "Selection Sort",
                                         "Insertion Sort", "Merge Sort", "QuickSort" };
   
   Choice sortMethodChoice;
   int currentSortMethod = 0;
   Checkbox fastBox;
   Button goButton,stepButton,newButton;
   
   Label comparisons, copies;
   
   SortCanvas sorter;
   MessageCanvas comment1,comment2;
   
   Panel messagePanel, controlPanel, statPanel;
   LogPanel log;
   
   VisualSortPanel(LogPanel log) {
   
      this.log = log;
   
      setBackground(Color.lightGray);
      setLayout(null);
   
      messagePanel = new Panel();
      messagePanel.setBackground(Color.lightGray);
      messagePanel.setLayout(new GridLayout(2,1,5,5));
      add(messagePanel);
      
      comment1 = new MessageCanvas("Click \"Go\" or \"Step\" to begin sorting.");
      comment1.setBackground(Color.white);
      comment1.setForeground(Color.red);
      messagePanel.add(comment1);
      comment2 = new MessageCanvas();
      comment2.setBackground(Color.white);
      comment2.setForeground(Color.red);
      messagePanel.add(comment2);
      
      controlPanel = new Panel();
      controlPanel.setLayout(new GridLayout(5,1,3,3));
      add(controlPanel);
      
      sortMethodChoice = new Choice();
      for (int i = 0; i < sortName.length; i++)
         sortMethodChoice.addItem(sortName[i]);
      controlPanel.add(sortMethodChoice);
      fastBox = new Checkbox("Fast");
      Panel temp = new Panel();
      temp.add(fastBox);
      controlPanel.add(temp);
      goButton = new Button("Go");
      controlPanel.add(goButton);
      stepButton = new Button("Step");
      controlPanel.add(stepButton);
      newButton = new Button("Start Again");
      controlPanel.add(newButton);
      
      statPanel = new Panel();
      statPanel.setLayout(new GridLayout(4,1,5,5));
      statPanel.setBackground(Color.white);
      add(statPanel);
      
      statPanel.add(new Label("  Comparisons:"));
      comparisons = new Label("0", Label.CENTER);
      comparisons.setForeground(Color.red);
      statPanel.add(comparisons);
      statPanel.add(new Label("  Copies:"));
      copies = new Label("0", Label.CENTER);
      copies.setForeground(Color.red);
      statPanel.add(copies);

      sorter = new SortCanvas(this,comparisons,copies,comment1,comment2);
      add(sorter);
      
   }
   
   public void reshape(int x, int y, int width, int height) {
      super.reshape(x,y,width,height);
      if (width < 300)
         width = 300;
      if (height < 200)
         height = 200;
      messagePanel.reshape(0,height - 45,width,45);
      sorter.reshape(0,0,width-125,height-50);
      int h = ((height-55)*5)/9;
      controlPanel.reshape(width-120,0,120,h);
      statPanel.reshape(width-120,h+5,120,height-55-h);
   }
   
   void aboutToShow() {
       goButton.setLabel("Go");
       stepButton.enable();
       sorter.newSort(currentSortMethod+1);
   }
   
   void aboutToHide() {
       sorter.stopRunning();
   }
   
   void doneRunning(int method, int comparisonCt, int copyCt) {  // callback from sorter
      goButton.enable();
      goButton.setLabel("Go");
      stepButton.disable();
      log.addLine(sortName[method-1] + " applied to 1 array containing 16 items:");
      log.addLine("   Number of comparisons: " + comparisonCt);
      log.addLine("   Number of copies: " + copyCt);
      log.addEoln();
   }
   
   void runnerStopped() {  // callback from sorter
      goButton.enable();
      goButton.setLabel("Go");
      stepButton.enable();
   }
   
  public boolean action(Event evt, Object arg) {
      if (evt.target == sortMethodChoice) {
         int choice = sortMethodChoice.getSelectedIndex();
         if (choice == currentSortMethod)
            return true;
         currentSortMethod = choice;
         sorter.newSort(currentSortMethod+1);
         return true;
      }
      else if (evt.target == fastBox) {
         sorter.setFast(((Boolean)arg).booleanValue());
         return true;
      }
      else if (evt.target == goButton) {
         synchronized(sorter) {
           if (sorter.getState() == SortCanvas.RUN) {
             goButton.disable();
             sorter.stopRunning();
           }
           else {
             goButton.setLabel("Stop");
             stepButton.disable();
             sorter.startRunning();
           }
         }
         return true;
      }
      else if (evt.target == stepButton) {
         sorter.doStep();
         synchronized(sorter) {
            int st = sorter.getState();
            if (st == SortCanvas.STEP) {
               stepButton.disable();
               goButton.disable();
            }
         }
         return true;
      }
      else if (evt.target == newButton) {
         sorter.newSort(currentSortMethod+1);
         goButton.setLabel("Go");
         stepButton.enable();
         return true;
      }
      else
         return super.action(evt,arg);
   }

}