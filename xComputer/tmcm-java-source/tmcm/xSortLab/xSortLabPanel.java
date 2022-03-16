package tmcm.xSortLab;
import java.awt.*;

public class xSortLabPanel extends Panel {

   Choice panelChoice;
   LogPanel log;
   VisualSortPanel visual;
   TimedSortPanel timed;
   int currentPanel;
   
   Panel mainPanel;
   CardLayout mainLayout;

   public xSortLabPanel() {
      setBackground(Color.lightGray);
      setLayout(new BorderLayout(5,5));
      
      panelChoice = new Choice();
      panelChoice.addItem("Visual Sort");
      panelChoice.addItem("Timed Sort");
      panelChoice.addItem("Log");
      
      add("North",panelChoice);
      
      mainPanel = new Panel();
      mainLayout = new CardLayout();
      mainPanel.setLayout(mainLayout);
      
      log = new LogPanel();
      visual = new VisualSortPanel(log);
      timed = new TimedSortPanel(log);
      
      mainPanel.add("visual",visual);
      mainPanel.add("timed",timed);
      mainPanel.add("log",log);
      currentPanel = 0;
      
      add("Center",mainPanel);
   }
   

   public void start() {
      if (currentPanel == 0)
         visual.sorter.doAppletStart();
      else if (currentPanel == 1)
         timed.sorter.doAppletStart();
   }
   
   public void stop() {
      if (currentPanel == 0)
         visual.sorter.doAppletStop();
      else if (currentPanel == 1)
         timed.sorter.doAppletStop();
   }
   
   public void destroy() {
      if (visual.sorter.runner != null && visual.sorter.runner.isAlive())
         visual.sorter.runner.stop();
      if (timed.sorter.runner != null && timed.sorter.runner.isAlive())
         timed.sorter.runner.stop();
   }

   public boolean action(Event evt, Object arg) {
      if (evt.target == panelChoice) {
         doPanelChoice();
         return true;
      }
      else
         return super.action(evt,arg);   
   }
   
   void doPanelChoice() {
     int item = panelChoice.getSelectedIndex();
     if (item == currentPanel)
        return;
     if (currentPanel == 0)
        visual.aboutToHide();
     else if (currentPanel == 1)
        timed.aboutToHide();
     else if (currentPanel == 2)
        log.aboutToHide();
     if (item == 0) {
        visual.aboutToShow();
        mainLayout.show(mainPanel,"visual");
     }
     else if (item == 1) {
        timed.aboutToShow();
        mainLayout.show(mainPanel,"timed");
     }
     else {
        log.aboutToShow();
        mainLayout.show(mainPanel,"log");
        log.shown();
     }
     currentPanel = item;  
   }

}