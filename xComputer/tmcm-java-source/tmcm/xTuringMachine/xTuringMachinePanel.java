
package tmcm.xTuringMachine;
import java.awt.*;
import java.net.URL;

public class xTuringMachinePanel extends Panel {

   MachinePanel machine;
   Choice machineChoice;
   int currentChoice;
   int untitledCt;

   public xTuringMachinePanel(URL[] urlList, String[] nameList) {
      setBackground(Color.lightGray);
      setLayout(new BorderLayout(5,5));
      machineChoice = new Choice();
      machineChoice.addItem("[New]");
      if (urlList == null || urlList.length == 0) {
          machineChoice.addItem("Untitled 1");
          machine = new MachinePanel(this);
          untitledCt = 1;
      }
      else {
         for (int i = 0; i < nameList.length; i++)
            machineChoice.addItem(nameList[i]);
         machine = new MachinePanel(urlList,this);
         untitledCt = 0;
      }
      currentChoice = 1;
      machineChoice.select(1);
      add("North",machineChoice);
      add("Center",machine);
   }
   
   public xTuringMachinePanel(String[] fileNameList) {
      setBackground(Color.lightGray);
      setLayout(new BorderLayout(5,5));
      machineChoice = new Choice();
      machineChoice.addItem("[New]");
      if (fileNameList == null || fileNameList.length == 0) {
          machineChoice.addItem("Untitled 1");
          machine = new MachinePanel(this);
          untitledCt = 1;
      }
      else {
         for (int i = 0; i < fileNameList.length; i++)
            machineChoice.addItem(fileNameList[i]);
         machine = new MachinePanel(fileNameList,this);
         untitledCt = 0;
      }
      currentChoice = 1;
      machineChoice.select(1);
      add("North",machineChoice);
      add("Center",machine);
   }
   
   public Insets insets() {
      return new Insets(5,5,5,5);
   }
   
   public void start() {
   }
   
   public void stop() {
      machine.TM.stopRunning();
   }
   
   public void destroy() {
      if (machine.TM.runner != null && machine.TM.runner.isAlive())
         machine.TM.runner.stop();
      if (machine.timer != null && machine.timer.isAlive())
         machine.timer.stop();
      if (machine.loaders != null)
         for (int i = 0; i < machine.loaders.length; i++)
            if (machine.loaders[i] != null && machine.loaders[i].isAlive())
               machine.loaders[i].stop();
   }
   
   void fileLoaded(String fileName) {
      machineChoice.addItem(fileName);
      currentChoice = machineChoice.countItems() - 1;
      machineChoice.select(currentChoice);
   }
   
   void doMachineChoice() {
      int index = machineChoice.getSelectedIndex();
      if (index == currentChoice)
         return;
      if (index > 0) {
         machine.selectMachineNumber(index-1);
         currentChoice = index;
      }
      else {
         int ct = machineChoice.countItems();
         untitledCt++;
         machineChoice.addItem("Untitled " + untitledCt);
         machineChoice.select(ct);
         currentChoice = ct;
         machine.doNewMachineCommand(ct-1);
      }
   }
   
   public boolean action(Event evt, Object arg) {
      if (evt.target == machineChoice) {
         doMachineChoice();
         return true;
      }
      else
         return super.action(evt,arg);
   }

}