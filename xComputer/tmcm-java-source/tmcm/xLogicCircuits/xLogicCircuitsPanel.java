package tmcm.xLogicCircuits;
import java.awt.*;


public class xLogicCircuitsPanel extends Panel {

   Button deleteButton, undoButton, iconifyButton, deIconifyButton, loadButton, saveButton, clearButton; //newButton;
   Checkbox powerCheckbox;
   CircuitCanvas canvas;
   Scrollbar scroll;
   TextField nameInput;
   Choice speedChoice;
   
   boolean canSave = true;
   boolean canLoad = true;

   public xLogicCircuitsPanel() {
   
       setBackground(Color.white);

       Panel bottom = new Panel();
       Panel top = new Panel();
       
       deleteButton = new Button("Delete");
       undoButton = new Button("Undo");
       iconifyButton = new Button("Iconify");
       deIconifyButton = new Button("Enlarge");
       loadButton = new Button("Load");
       saveButton = new Button("Save");
       clearButton = new Button("Clear");
 //      newButton = new Button("New");
       powerCheckbox = new Checkbox("Power ");
       deleteButton.disable();
       undoButton.disable();
       deIconifyButton.disable();
       
       nameInput = new TextField("Untitled",12);
       
       speedChoice = new Choice();
       speedChoice.addItem("Fast");
       speedChoice.addItem("Moderate");
       speedChoice.addItem("Slow");
       
       top.add(clearButton);
 //      top.add(newButton);
       top.add(loadButton);
       top.add(saveButton);
       top.add(new Label("  Title:"));
       top.add(nameInput);

       bottom.add(speedChoice);
       bottom.add(powerCheckbox);
       bottom.add(deleteButton);
       bottom.add(undoButton);
       bottom.add(iconifyButton);
       bottom.add(deIconifyButton);
       
       Panel circuit = new Panel();
       scroll = new Scrollbar(Scrollbar.VERTICAL);
       canvas = new CircuitCanvas(this,scroll,undoButton);
       circuit.setBackground(Color.white);
       circuit.setLayout(new BorderLayout());
       circuit.add("Center",canvas);
       circuit.add("West",scroll);
       
       
       setLayout(new BorderLayout());
       add("North",top);
       add("South",bottom);
       add("Center",circuit);
       
   }
   
   public void loadURL(java.net.URL url) {  // called from applet during first start()
      canvas.loadURL(url);
   }
   
   public void loadFile(String fileName) {
      canvas.loadFile(fileName,null);
   }

   public void start() {
      canvas.start();
   }
   
   public void stop() {
      canvas.OSC = null;
      canvas.OSG = null;
      canvas.stop();
   }
   
   public void destroy() {
      if (canvas.runner != null && canvas.runner.isAlive())
         canvas.runner.stop();
      if (canvas.loader.runner != null && canvas.loader.runner.isAlive())
         canvas.loader.runner.stop();
   }
   
   void doLoad() {
      if (!canLoad)
         return;
      FileDialog fd = null;
      try {
        Container c = this;  // find frame that contains this panel
        do {
           Container p = c.getParent();
           if (p == null)
              break;
           c = p; 
        } while (true);
        if (!(c instanceof Frame))
           c = null;
        fd = new FileDialog((Frame)c,"Select File to Load",FileDialog.LOAD);
        fd.show();
      }
      catch (AWTError e) {  // thrown by Netscape 3.0 on attempt to use file dialog
        canvas.setMessage("ERROR while trying to create a file dialog box.+It will not be possible to save files.");
        canLoad = false;
        loadButton.disable();
        return;
      }
      catch (RuntimeException re) { // illegal typecast, maybe?
        canvas.setMessage("ERROR while trying to create a file dialog box.+It will not be possible to save files.");
        canLoad = false;
        loadButton.disable();
        return;
      }
      String fileName = fd.getFile();
      if (fileName == null)
         return;
      String dir = fd.getDirectory();
      canvas.loadFile(fileName,dir);
   }
   
   void doSave() {
      if (!canSave)
         return;
      String fileName = null,directory = null;
      FileDialog fd = null;
      try {
         Container c = this;  // find frame that contains this panel
         do {
            Container p = c.getParent();
            if (p == null)
               break;
            c = p; 
         } while (true);
         if (!(c instanceof Frame))
            c = null;
         fd = new FileDialog((Frame)c,"Save as:",FileDialog.SAVE);
         fd.show();
      }
      catch (AWTError e) {  // thrown by Netscape 3.0 on attempt to use file dialog
          canvas.setMessage("ERROR while trying to create a file dialog box.+It will not be possible to save files.");
          canSave = false;
          saveButton.disable();
          return;
      }
      catch (RuntimeException re) {
          canvas.setMessage("ERROR while trying to create a file dialog box.+It will not be possible to save files.");
          canSave = false;
          saveButton.disable();
          return;
      }
      fileName = fd.getFile();
      if (fileName == null)
         return;
      directory = fd.getDirectory();
      canvas.saveToFile(fileName,directory);
   }
      
   public boolean action(Event evt, Object arg) {
      if (evt.target == powerCheckbox)
         canvas.doPower( ((Boolean)arg).booleanValue() );
  //    else if (evt.target == newButton)
  //       canvas.doNew();
      else if (evt.target == saveButton)
         doSave();
      else if (evt.target == loadButton)
         doLoad();
      else if (evt.target == clearButton)
         canvas.doClear();
      else if (evt.target == iconifyButton)
         canvas.doIconify();
      else if (evt.target == deleteButton)
         canvas.doDelete();
      else if (evt.target == undoButton)
         canvas.undoer.click();
      else if (evt.target == deIconifyButton)
         canvas.doDeIconify();
      else if (evt.target == speedChoice)
         canvas.doSpeedChoice( speedChoice.getSelectedIndex() );
      else
         return super.action(evt,arg);
      return true;
   }

   public boolean handleEvent(Event evt) {
      if (evt.id == Event.SCROLL_LINE_DOWN || evt.id == Event.SCROLL_LINE_UP || 
          evt.id == Event.SCROLL_PAGE_DOWN || evt.id == Event.SCROLL_PAGE_UP || 
          evt.id == Event.SCROLL_ABSOLUTE) {
         canvas.scroller.doScroll();
         return true;
      }
      return super.handleEvent(evt);
   }


}