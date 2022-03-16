package tmcm.xLogicCircuits;

import java.awt.*;
import java.util.Vector;

class ScrollHandler {

   static final String[] standardItemName = 
          { "NOT Gate", "OR Gate", "AND Gate", "Input", "Output", "\"Tack\"" };
   static final int standardItemCt = 6;

   CircuitCanvas owner;
   Scrollbar scroll;

   int width = -1, height = -1;

   Vector items;
   int visibleItems;
   int topItem;
   
   Gate[][] gates;
   int[] currentGate;
   
   int selectedItemNum = -1;
   
   int deletedItemPos;
   
   ScrollHandler(CircuitCanvas owner, Scrollbar scroll) {
      this.owner = owner;
      this.scroll = scroll;
      items = new Vector();
      topItem = 0;
    
      gates = new Gate[3][4];
      for (int kind = 0; kind < 3; kind ++)
         for (int facing = 0; facing < 4; facing ++)
            gates[kind][facing] = new Gate(kind,facing);
      gates[0][0].reshape(0,0,35,25);
      gates[1][0].reshape(0,0,40,35);
      gates[2][0].reshape(0,0,40,35);
      currentGate = new int[3];
      
      items.addElement(gates[0][0]);
      items.addElement(gates[1][0]);
      items.addElement(gates[2][0]);
      
      CircuitIONub in = new CircuitIONub(CircuitIONub.ONLEFT,0.0,true);
      in.boundingBox.reshape(0,0,13,10);
      items.addElement(in);
      
      CircuitIONub out = new CircuitIONub(CircuitIONub.ONLEFT,0.0,false);
      out.boundingBox.reshape(0,0,13,10);
      items.addElement(out);
      
      Tack tack = new Tack();
      tack.boundingBox.reshape(0,0,5,5);
      items.addElement(tack);
            
   }
   
   CircuitItem checkMouse(int x, int y) {  // respond to mouse click; return item hit, if any
      int itemNum = topItem + y/width;
      if (itemNum >= items.size())
         return null;
      CircuitItem item = (CircuitItem)items.elementAt(itemNum);
      if (itemNum < 3 && x < 16 && y < (itemNum-topItem)*width + 17) {
         currentGate[itemNum]++;
         if (currentGate[itemNum] > 3)
            currentGate[itemNum] = 0;
         FloatRect r = ((Gate)items.elementAt(itemNum)).boundingBox;
         Gate newItem = gates[itemNum][currentGate[itemNum]];
         synchronized(owner) {
            boolean selected = ((Gate)items.elementAt(itemNum)).selected;
            newItem.selected = selected;
            if (selected)
               owner.selectedItem = newItem;
         }
         items.setElementAt(newItem,itemNum);
         if (newItem.facing == Gate.FACEDOWN || newItem.facing == Gate.FACEUP)
            newItem.reshape((width/2 - r.height/2),((itemNum-topItem)*width + width/2 - r.width/2 - 4),r.height,r.width);
         else
            newItem.reshape((width/2 - r.height/2),((itemNum-topItem)*width + width/2 - r.width/2),r.height,r.width);
         owner.repaint(0,(itemNum-topItem)*width,width-1,width-1);
         return null;
      }
      if (item.hit(x,y))
         return item;
      else
         return null;
   }
    
   void setSelection(CircuitItem selectedItem) {
       selectedItemNum = items.indexOf(selectedItem);
   }
   
   synchronized void reshape(int width, int height) {
      if (width == this.width && height == this.height)
         return;
      this.width = width;
      this.height = height;
      visibleItems = (height-5) / width;
      int max = items.size() - visibleItems;
      if (max < 0)
         max = 0;
      if (topItem > max)
         topItem = max;
      for (int i = 0; i < items.size(); i++) {
         CircuitItem it = (CircuitItem)items.elementAt(i);
         it.reshape((width/2 - it.boundingBox.width/2),(it.boundingBox.y),it.boundingBox.width,it.boundingBox.height);
      }
      scroll.setValues(topItem,visibleItems,0,max+visibleItems);
      updateScroll();
   }
   
   void draw(Graphics g) {
      g.setColor(Color.white);
      g.fillRect(0,0,width-1,height);
      g.setColor(Color.black);
      g.drawLine(0,0,width-1,0);
      int max = Math.min(items.size(), topItem + visibleItems + 1);
      for (int i = topItem; i < max; i++) {
         CircuitItem it = (CircuitItem)items.elementAt(i);
         it.draw(g);
         g.setColor(Color.black);
         g.drawLine(0,width*(i-topItem+1)-1,width-1,width*(i-topItem+1)-1);
         if (i < standardItemCt)
            g.drawString(standardItemName[i],5,width*(i-topItem+1)-5);
         if (i < 3) {
            int x = 3;
            int y = width*(i-topItem)+4;
            g.setColor(Color.red);
            g.drawArc(x,y,12,12,90,320);
            g.drawLine(x+9,y+2,x+9,y+6);
            g.drawLine(x+9,y+2,x+13,y+2);
         }
      }
      int top = width*(max-topItem);
      if (top < height-1) {
         g.setColor(Color.lightGray);
         g.fillRect(0,top,width-1,height-top);
      }
   }
   
   synchronized void deleteItem(Circuit cir) {
      deletedItemPos = items.indexOf(cir);
      if (deletedItemPos < 0)
         return;  // shouldn't happen.
      items.removeElementAt(deletedItemPos);
      int max = items.size() - visibleItems;
      if (max < 0)
         max = 0;
      if (selectedItemNum < topItem)
         topItem--;
      else if (topItem + visibleItems - 1 >= items.size())
         topItem--;
      scroll.setValues(topItem,visibleItems,0,max+visibleItems);
      updateScroll();
      owner.repaint(0,0,width-1,height);
   }
   
   synchronized void unDeleteItem(Circuit cir, int oldTopItem) {  
      if (deletedItemPos < 0)
         return;
      items.insertElementAt(cir,deletedItemPos);
      int max = items.size() - visibleItems;
      if (max < 0)
         max = 0;
      topItem = oldTopItem;
      scroll.setValues(topItem,visibleItems,0,max+visibleItems);
      updateScroll();
      owner.repaint(0,0,width-1,height);
   }
   
   synchronized Vector deleteAllCircuits() {  // returns list of deleted circuits
      Vector circuits = new Vector();
      if (items.size() == standardItemCt)
         return circuits;
      for (int j = standardItemCt; j < items.size(); j++)
         circuits.addElement(items.elementAt(j));
      items.setSize(standardItemCt);
      int max = items.size() - visibleItems;
      if (max < 0)
         max = 0;
      scroll.setValues(0,visibleItems,0,max+visibleItems);
      updateScroll();
      owner.repaint(0,0,width-1,height);
      return circuits;
   }
   
   synchronized void addItem(Circuit item) {
      items.addElement(item);
      int posInList = items.size() - 1;
      topItem = posInList - visibleItems + 1;
      if (topItem < 0)
         topItem = 0;
      int max = items.size() - visibleItems;
      if (max < 0)
         max = 0;
      item.reshape((width/2 - item.boundingBox.width/2), item.boundingBox.y, 
                          item.boundingBox.width, item.boundingBox.height);
      scroll.setValues(topItem,visibleItems,0,max+visibleItems);
      updateScroll();
      owner.repaint(0,0,width-1,height);
   }
   
   synchronized void addItems(Vector list) {
      if (list.size() == 0)
         return;
      for (int i = 0; i < list.size(); i++) {
         Circuit cir = (Circuit)list.elementAt(i);
         items.addElement(cir);
         cir.reshape((width/2 - cir.boundingBox.width/2), cir.boundingBox.y, 
                          cir.boundingBox.width, cir.boundingBox.height);
      }
      int max =  items.size() - visibleItems;
      if (max < 0)
         max = 0;      
      scroll.setValues(topItem,visibleItems,0,max+visibleItems);
      updateScroll();
      owner.repaint(0,0,width-1,height);
   }
   
   void doScroll() {
      updateScroll();
      owner.repaint(0,0,width,height);
   }
   
   synchronized void updateScroll() {
      int newTopItem = scroll.getValue();
      if (newTopItem > items.size() - visibleItems) {
         newTopItem = Math.max(0,items.size() - visibleItems);
         scroll.setValue(newTopItem);
      }
      topItem = newTopItem;
      int max = Math.min(items.size(), topItem + visibleItems + 1);
      for (int i = topItem; i < max; i++) {
          CircuitItem it = (CircuitItem)items.elementAt(i);
          if (i < 3 && ( ((Gate)it).facing == Gate.FACEDOWN || ((Gate)it).facing == Gate.FACEUP ))
             it.reshape(it.boundingBox.x,  (((i-topItem)*width + width/2) - it.boundingBox.height/2 - 4), 
                          it.boundingBox.width, it.boundingBox.height);
          else
             it.reshape(it.boundingBox.x,  (((i-topItem)*width + width/2) - it.boundingBox.height/2), 
                          it.boundingBox.width, it.boundingBox.height);
      }
      if (owner.selectedItem != null && owner.selectionInScroller && selectedItemNum >= 0) {
         if (selectedItemNum < topItem || selectedItemNum >= topItem + visibleItems)
            owner.selectItem(null,false);
         else {
            if (selectedItemNum < 3 && ( ((Gate)owner.selectedItem).facing == Gate.FACEDOWN || ((Gate)owner.selectedItem).facing == Gate.FACEUP ))
               owner.selectedItem.reshape(owner.selectedItem.boundingBox.x,  (((selectedItemNum-topItem)*width + width/2)) - owner.selectedItem.boundingBox.height/2 - 4, 
                                           owner.selectedItem.boundingBox.width, owner.selectedItem.boundingBox.height);
            else
               owner.selectedItem.reshape(owner.selectedItem.boundingBox.x,  ((selectedItemNum-topItem)*width + width/2) - owner.selectedItem.boundingBox.height/2, 
                                           owner.selectedItem.boundingBox.width, owner.selectedItem.boundingBox.height);
            owner.resizer.reshape(owner.selectedItem.boundingBox.x, owner.selectedItem.boundingBox.y,
                     owner.selectedItem.boundingBox.width, owner.selectedItem.boundingBox.height);
         }
      }
   }

}