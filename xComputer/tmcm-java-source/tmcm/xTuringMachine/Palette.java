package tmcm.xTuringMachine;
import java.awt.*;

class Palette extends Canvas {

   static final Color backgroundColor = new Color(220,220,255);
   static final Color textColor = Color.blue;
   static final Color itemColor = Color.red;
   static final Color hitColor = Color.pink;
   
   static final Color hiliteColor = new Color(150,255,255);  // for use in other files
   
   static final int NONE = 0,
                     STATES = 1,
                     SYMBOLS = 2,
                     DIRECTIONS = 3,
                     SYMBOLSANDDEFAULT = 4,
                     STATESANDHALT = 5;
   int display = NONE;
   
   int width = -1;
   int height = -1;
   int leftOffset;
   int baseLine;
   int itemWidth;
   boolean twoRowsOfStates;
   
   int selectedItem = -1;
   Rectangle selectRect = new Rectangle();
   boolean inSelection;
   
   MachinePanel owner;
   
   Font font;
   FontMetrics fm;
   Font smallFont;
   FontMetrics smallFm;

   Palette(MachinePanel owner) {
      this.owner = owner;
      setBackground(backgroundColor);
   }

   public Dimension preferredSize() {
      return new Dimension(400,28);
   }
   
   public Dimension mimimumSize() {
      return new Dimension(100,24);
   }
   
   public void paint(Graphics g) {
      if (size().width != width || size().height != height)
         setUp();
      g.setColor(textColor);
      g.drawRect(0,0,width,height);
      g.drawRect(1,1,width-2,height-2);
      g.drawLine(width-2,0,width-2,height);
      g.drawLine(0,height-2,width,height-2);
      g.setFont(font);
      if (inSelection) {
         g.setColor(hitColor);
         g.fillRect(selectRect.x,selectRect.y,selectRect.width,selectRect.height);
         g.setColor(textColor);
      }
      int x;
      switch (display) {
         case NONE:
            return;
         case STATESANDHALT:
            leftOffset = fm.stringWidth("States:") + 16;
            g.drawString("States:",9,baseLine);
            g.setColor(itemColor);
            x = fm.stringWidth("00")+6;
            if (leftOffset + (MachineData.STATES+1)*x >= width) {
               g.setFont(smallFont);
               x = smallFm.stringWidth("00")+6;
            }
            if (leftOffset + (MachineData.STATES+1)*x < width) {
               twoRowsOfStates = false;
               itemWidth = (width - leftOffset) / (MachineData.STATES+1);
               g.drawString("h",leftOffset + (itemWidth - fm.stringWidth("h"))/2,baseLine);
               for (int i = 0; i < MachineData.STATES; i++) {
                  String str = String.valueOf(i);
                  g.drawString(str,leftOffset + (i+1)*itemWidth + (itemWidth - fm.stringWidth(str))/2,baseLine); 
               }
            }
            else {
               twoRowsOfStates = true;
               itemWidth = (width - leftOffset) / ((MachineData.STATES+1) / 2);
               int b1 = height/2 - 1;
               int b2 = height - 4;
               g.drawString("h",leftOffset + (itemWidth - smallFm.stringWidth("h"))/2,b1);
               boolean top = false;
               for (int i = 0; i < MachineData.STATES; i++) {
                  String str = String.valueOf(i);
                  int b = top? b1 : b2;
                  g.drawString(str,leftOffset + ((i+1)/2)*itemWidth + (itemWidth - fm.stringWidth(str))/2,b); 
                  top = !top;
               }
            }
            return;
         case STATES:
            leftOffset = fm.stringWidth("States:") + 16;
            g.drawString("States:",9,baseLine);
            g.setColor(itemColor);
            x = fm.stringWidth("00")+6;
            if (leftOffset + MachineData.STATES*x >= width) {
               g.setFont(smallFont);
               x = smallFm.stringWidth("00")+6;
            }
            if (leftOffset + MachineData.STATES*x < width) {
               twoRowsOfStates = false;
               itemWidth = (width - leftOffset) / MachineData.STATES;
               for (int i = 0; i < MachineData.STATES; i++) {
                  String str = String.valueOf(i);
                  g.drawString(str,leftOffset + i*itemWidth + (itemWidth - fm.stringWidth(str))/2,baseLine); 
               }
            }
            else {
               twoRowsOfStates = true;
               itemWidth = (width - leftOffset) / ((MachineData.STATES+1) / 2);
               int b1 = height/2 - 1;
               int b2 = height - 4;
               boolean top = true;
               for (int i = 0; i < MachineData.STATES; i++) {
                  String str = String.valueOf(i);
                  int b = top? b1 : b2;
                  g.drawString(str,leftOffset + (i/2)*itemWidth + (itemWidth - fm.stringWidth(str))/2,b); 
                  top = !top;
               }
            }
            return;
         case SYMBOLSANDDEFAULT:
            leftOffset = fm.stringWidth("Symbols:") + 16;
            g.drawString("Symbols:",9,baseLine);
            x = width - smallFm.stringWidth("(* = default)") - 5;
            itemWidth = (x - leftOffset - 5) / MachineData.SYMBOLS;
            g.setColor(itemColor);
            for (int i = 0; i < MachineData.symbolNames.length(); i++) {
               String str = String.valueOf(MachineData.symbolNames.charAt(i));
               g.drawString(str,leftOffset + i*itemWidth + itemWidth/2 - 3,baseLine);
            }
            g.setFont(smallFont);
            g.setColor(textColor);
            g.drawString("(# = blank)",x,height/2-1);
            g.drawString("(* = default)",x,height - 4);
            return;
         case SYMBOLS:
            leftOffset = fm.stringWidth("Symbols:") + 16;
            g.drawString("Symbols:",9,baseLine);
            x = width - fm.stringWidth("(* = blank)") - 5;
            itemWidth = (x - leftOffset - 5) / (MachineData.SYMBOLS - 1);
            g.setColor(itemColor);
            for (int i = 0; i < MachineData.symbolNames.length() - 1; i++) {
               String str = String.valueOf(MachineData.symbolNames.charAt(i));
               g.drawString(str,leftOffset + i*itemWidth + itemWidth/2 - 3,baseLine);
            }
            g.setColor(textColor);
            g.drawString("(# = blank)",x,baseLine);
            return;
         case DIRECTIONS:
            leftOffset = fm.stringWidth("Directions:") + 16;
            g.drawString("Directions:",9,baseLine);
            itemWidth = 40;
            g.setColor(itemColor);
            g.drawString("L",leftOffset + 17,baseLine);
            g.drawString("R",leftOffset + 57,baseLine);
            return;
      }
   }
   
   void setUp() {
      width = size().width;
      height = size().height;
      font = getFont();
      fm = getFontMetrics(font);
      int pt = font.getSize();
   //   if (pt >= 10)
         smallFont = new Font(font.getName(),Font.PLAIN,9);
   //   else
   //      smallFont = new Font(font.getName(),Font.PLAIN,6);
      smallFm = getFontMetrics(smallFont);
      baseLine = (height + fm.getAscent() - fm.getDescent())/2;
   }
   
   public void reshape(int x, int y, int width, int height) {
      super.reshape(x,y,width,height);
      inSelection = false;
      selectedItem = -1;
   }
   
   void setDisplay(int display) {
      if (this.display == display && selectedItem == -1)
         return;
      this.display = display;
      inSelection = false;
      selectedItem = -1;
      repaint();
   }
   
   void checkPoint(int x, int y, boolean newEvent) {
      inSelection = false;
      if (display == NONE || x < leftOffset)
         return;
      int item = (x-leftOffset)/itemWidth;
      switch (display) {
         case STATES:
         case STATESANDHALT:
           if (twoRowsOfStates) {
              item = 2*item;
              if (y > height/2)
                 item++;
           }
           if (item >= ( (display == STATES)? MachineData.STATES : MachineData.STATES + 1))
              return;
           if (newEvent) {
              selectedItem = item;
              if (twoRowsOfStates) {
                 selectRect.x = leftOffset + (item/2)*itemWidth;
                 selectRect.width = itemWidth;
                 selectRect.y = ( (item & 1) == 0 )? 3 : height/2;
                 selectRect.height = height/2 - 3;
              }
              else {
                 selectRect.x = leftOffset + item*itemWidth;
                 selectRect.width = itemWidth;
                 selectRect.y = 4;
                 selectRect.height = height - 8;
              }
           }
           else if (item != selectedItem)
              return;
           break;
         case SYMBOLS:
         case SYMBOLSANDDEFAULT:
           if (item >= ( (display == SYMBOLS)? MachineData.SYMBOLS - 1 : MachineData.SYMBOLS ))
              return;
           if (newEvent) {
              selectedItem = item;
              selectRect.x = leftOffset + item*itemWidth;
              selectRect.y = 4;
              selectRect.width = itemWidth;
              selectRect.height = height - 8;
           }
           else if (item != selectedItem)
              return;
           break;
         case DIRECTIONS:
           if (item > 1)
              return;
           if (newEvent) {
              selectedItem = item;
              selectRect.x = leftOffset + item*itemWidth;
              selectRect.y = 4;
              selectRect.width = itemWidth;
              selectRect.height = height - 8;
           }
           else if (item != selectedItem)
              return;
           break;
      }
      inSelection = true;
      return;
   }
   
   public boolean mouseDown(Event evt, int x, int y) {
      checkPoint(x,y,true);
      if (inSelection)
         repaint(selectRect.x,selectRect.y,selectRect.width,selectRect.height);
      return true;
   }
   
   public boolean mouseDrag(Event evt, int x, int y) {
      if (selectedItem < 0)
         return true;
      boolean saveSelected = inSelection;
      checkPoint(x,y,false);
      if (saveSelected != inSelection)
         repaint(selectRect.x,selectRect.y,selectRect.width,selectRect.height);
      return true;
   }
   
   public boolean mouseUp(Event evt, int x, int y) {
       if (selectedItem < 0)
          return true;
       checkPoint(x,y,false);
       if (inSelection) {
          if (display == STATESANDHALT)
             owner.doPaletteClick(selectedItem-1);
          else
             owner.doPaletteClick(selectedItem);
       }
       inSelection = false;
       selectedItem = -1;
       repaint(selectRect.x,selectRect.y,selectRect.width,selectRect.height);
       return true;
   }
   
   
   

}