
package tmcm.xComputer;

import java.awt.*;
import java.util.BitSet;

class MemoryPanel extends Panel {

   // for outside use

   void set(int location, short value, int flashCount) {
      data[location] = value;
      if (currentView < graphicsView)
         scroll.set(location,value,flashCount);
      else if (currentView == graphicsView)
         graphics.set(location,value);
   }
   
   short get(int location) {
      return data[location];
   }
   
   void setControlWires(BitSet wiresOn) {
      currentWires = wiresOn;
      if (currentView != controlWireView)
         return;
      wires.setWires(wiresOn);
   }
   
   void setAddress(int address) {
      currentAddress = address;
      if (autoscroll && currentView < graphicsView)
         scroll.setScrollPos(address);
   }

   void resetMem(short[] data) {
      this.data = data;
      if (currentView < graphicsView)
         scroll.reset(data);
      else if (currentView == graphicsView)
         graphics.reset(data);
   }
   
   void clear() {
      for (int i = 0; i < 1024; i++)
         data[i] = 0;
      if (currentView < graphicsView)
         scroll.reset(data);
      else if (currentView == graphicsView)
         graphics.reset(data);
   }
   
   // for internal use


   final static int assemblerView = 0,
                    signedView = 1, 
                    unsignedView = 2,
                    binaryView = 3, 
                    asciiView = 4, 
                    graphicsView = 5, 
                    controlWireView = 6;
   
   short[] data;
                    
   CardLayout cardLayout;
   Panel cardPanel;
   
   ScrollMemoryView scroll;
   GraphicsMemoryView graphics;
   ControlWiresView wires;
   
   Choice viewChoice;
   int currentView;
   
   Checkbox autoscrollBox;
   boolean autoscroll;
   
   int currentAddress;
   BitSet currentWires;
   
   
   MemoryPanel() {
   
      data = new short[1024];
      currentView = 1;
      autoscroll = false;
      currentAddress = 0;
      currentWires = new BitSet(26);
      
      setLayout(new BorderLayout(3,3));
            
      viewChoice = new Choice();
      viewChoice.addItem("Instructions");
      viewChoice.addItem("Integers");
      viewChoice.addItem("Unsigned Ints");
      viewChoice.addItem("Binary");
      viewChoice.addItem("ASCII");
      viewChoice.addItem("Graphics");
      viewChoice.addItem("Control Wires");
      viewChoice.select(1);
      Panel temp = new Panel();
      temp.add(viewChoice);
      add("North",temp);
      
      
      cardPanel = new Panel();
      cardLayout = new CardLayout();
      cardPanel.setLayout(cardLayout);
      add("Center",cardPanel);
      
      temp = new Panel();
      temp.setLayout(new BorderLayout(3,3));
      scroll = new ScrollMemoryView();
      temp.add("Center",scroll);
      autoscrollBox = new Checkbox("Autoscroll");
      Panel temp2 = new Panel();
      temp2.add(autoscrollBox);
      temp.add("South",temp2);
      cardPanel.add("S",temp);
      
      graphics = new GraphicsMemoryView();
      cardPanel.add("G",graphics);
      
      wires = new ControlWiresView();
      cardPanel.add("W",wires);
      
      scroll.visible = true;
      
   }
   
   public boolean action(Event evt, Object arg) {
      if (evt.target == autoscrollBox) {
         doAutoScroll(((Boolean)arg).booleanValue());
         return true;
      }
      else if (evt.target == viewChoice) {
         doViewChoice();
         return true;
      }
      return super.action(evt,arg);
   }
   
   synchronized void doAutoScroll(boolean a) {
      autoscroll = a;
      if (currentView < graphicsView && a)
         scroll.setScrollPos(currentAddress);
   }
   
   synchronized void doViewChoice() {
      int choice = viewChoice.getSelectedIndex();
      if (choice == currentView)
         return;
      if (choice < graphicsView)
         scroll.setViewStyle(choice);
      if (choice >= graphicsView || currentView >= graphicsView) {
         if (choice < graphicsView) {
            scroll.reset(data);
            if (autoscroll)
               scroll.setScrollPos(currentAddress);
            cardLayout.show(cardPanel,"S");
            scroll.visible = true;
            wires.visible = false;
            graphics.visible = false;
         }
         else if (choice == graphicsView) {
            graphics.reset(data);
            cardLayout.show(cardPanel,"G");
            scroll.visible = false;
            wires.visible = false;
            graphics.visible = true;
         }
         else {
            wires.setWires(currentWires);
            cardLayout.show(cardPanel,"W");
            scroll.visible = false;
            wires.visible = true;
            graphics.visible = false;
         }
      }
      currentView = choice;
   }

}