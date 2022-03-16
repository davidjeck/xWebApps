package tmcm.xTuringMachine;
import java.awt.*;

class RuleMakerCanvas extends Canvas {

   int width = -1, height;
   int baseLine;
   int[] columnLoc = new int[11];
   Font font;
   FontMetrics fm;

   int state;
   char symbol;
   int newState;
   char newSymbol;
   boolean direction;
   
   int selectedColumn;
   boolean hasFocus;
   
   MachineData data;
   MachinePanel owner;

   RuleMakerCanvas(MachinePanel owner) {
      setBackground(Color.white);
      state = MachineData.UNSPECIFIED;
      selectedColumn = -1;
      this.owner = owner;
   }

   public Dimension preferredSize() {
      return new Dimension(400,28);
   }
   
   public Dimension mimimumSize() {
      return new Dimension(100,28);
   }
   
   public void paint(Graphics g) {
      if (size().width != width || size().height != height)
         setUp();
      g.setFont(font);
      g.drawRect(0,0,width,height);
      g.drawRect(1,1,width-2,height-2);
      g.drawLine(0,height-2,width,height-2);
      g.drawLine(width-2,0,width-2,height);
      g.drawLine(columnLoc[2], 0, columnLoc[2], height);
      g.drawLine(columnLoc[4], 0, columnLoc[4], height);
      g.drawLine(columnLoc[4] - 1, 0, columnLoc[4] - 1, height);
      g.drawLine(columnLoc[6], 0, columnLoc[6], height);
      g.drawLine(columnLoc[8], 0, columnLoc[8], height);
      if (state == MachineData.UNSPECIFIED)
         return;
      String str = String.valueOf(state);
      g.drawString(str,columnLoc[1] - fm.stringWidth(str)/2, baseLine);
      if (symbol == '*')
         str = "other";
      else
         str = String.valueOf(symbol);
      g.drawString(str,columnLoc[3] - fm.stringWidth(str)/2, baseLine);
      if (newSymbol == '*')
         str = "same";
      else
         str = String.valueOf(newSymbol);
      g.drawString(str,columnLoc[5] - fm.stringWidth(str)/2, baseLine);
      if (direction)
         str = "R";
      else
         str = "L";
      g.drawString(str,columnLoc[7] - fm.stringWidth(str)/2, baseLine);
      if (newState == MachineData.HALTSTATE)
         str = "h";
      else
         str = String.valueOf(newState);
      g.drawString(str,columnLoc[9] - fm.stringWidth(str)/2, baseLine);
      if (hasFocus && selectedColumn >= 0) {
        g.setColor(Palette.hiliteColor);
        int x = columnLoc[2*selectedColumn]+4;
        int wd = columnLoc[2+2*selectedColumn] - 3 - x;
        int y = 4;
        int ht = height-8;
        g.drawRect(x,y,wd,ht);
        g.drawRect(x+1,y+1,wd-2,ht-2);
      }
   }
   
   void setUp() {
      width = size().width;
      height = size().height;
      for (int i = 1; i < 10; i++)
         columnLoc[i] = 2 + (i*(width-3)+5)/10;
      columnLoc[10] = width-1;
      if (font == null) {
         font = getFont();
         fm = getFontMetrics(font);
      }
      baseLine = (height + fm.getAscent() - fm.getDescent())/2;
   }
   
   synchronized void setRule(int st, char sym, boolean grabFocus) {
      if (!data.ruleDefined(st,sym))
         owner.makeButton.setLabel("Make Rule");
      else
         owner.makeButton.setLabel("Replace");
      if (selectedColumn == 2 && sym == '*' && symbol != '*')
         owner.requestFocus(MachinePanel.RULEMAKERFOCUS,Palette.SYMBOLSANDDEFAULT);
      else if (selectedColumn == 2 && sym != '*' && symbol == '*')
         owner.requestFocus(MachinePanel.RULEMAKERFOCUS,Palette.SYMBOLS);
      if (state != st || symbol != sym) {
        if (data.getNewState(st,sym) == MachineData.UNSPECIFIED) {
           newState = st;
           newSymbol = sym;
           direction = true;  
        }
        else {
           newState = data.getNewState(st,sym);
           newSymbol = data.getNewSymbol(st,sym);
           direction = data.getDirection(st,sym);
        }
        state = st;
        symbol = sym;
      }
      if (grabFocus && !hasFocus) {
         selectedColumn = 2;
         if (sym == '*')
            owner.requestFocus(MachinePanel.RULEMAKERFOCUS,Palette.SYMBOLSANDDEFAULT);
         else
            owner.requestFocus(MachinePanel.RULEMAKERFOCUS,Palette.SYMBOLS);
      }
      repaint(2,2,width-4,height-4);
         
   }
   
   void setMachineData(MachineData data) {
      this.data = data;
      if (data == null) {
         state = MachineData.UNSPECIFIED;
         selectedColumn = -1;
         owner.makeButton.disable();
         repaint();
         return;
      }
      setRule(0,'#',false);
      owner.makeButton.enable();
      selectedColumn = 2;
      repaint();
   }
   
   void ruleMade() {  // called by owner when the rule has beed added/replaced
      int loc = MachineData.symbolNames.indexOf(symbol);
      loc++;
      int st = state;
      if (loc >= MachineData.SYMBOLS) {
         loc = 0;
         st++;
         if (st >= MachineData.STATES)
            st = 0;
      }
      char sym = MachineData.symbolNames.charAt(loc);
      setRule(st,sym,false);
   }
   
   final void repaintContents() {
      repaint(2,2,width-4,height-4);
   }
   
   final void repaintColumn(int col) {
     int x = columnLoc[2*col]+2;
     int wd = columnLoc[2+2*col] - 1 - x;
     repaint(x,2,wd,height-4);
   }

   // ------------------------------ focus stuff ----------------------------------------
   
   boolean firstKeyDownInSelection;
   
   void focusIsOn() {
      hasFocus = true;
      firstKeyDownInSelection = true;
      repaintContents();
   }
   
   void focusIsOff() {
      hasFocus = false;
      repaintContents();
   }
   
   void processKey(int key) {
      if (key >= ' ' && key <= 127) {
         if (key == ' ')
            key = '#';
         key = Character.toLowerCase((char)key);
         switch (selectedColumn) {
            case 0:
              if (key >= '0' && key <= '9') {
                 state = 10*state + key - '0';
                 if (state >= MachineData.STATES || firstKeyDownInSelection)
                    state = key - '0';
                 repaintColumn(0);
                 if (data.ruleDefined(state,symbol))
                    owner.makeButton.setLabel("Replace");
                 else
                    owner.makeButton.setLabel("Make Rule");
                 firstKeyDownInSelection = false;
              }
              break;
            case 1:
              if (key == 'i')
                 key = '1';
              else if (key == 'o')
                 key = '0';
              if (MachineData.symbolNames.indexOf((char)key) >= 0) {
                 if (symbol == '*' && (char)key != '*' && newSymbol == '*') {
                    newSymbol = (char)key;
                    repaintColumn(2);
                 }
                 symbol = (char)key;
                 repaintColumn(1);
                 if (data.ruleDefined(state,symbol))
                    owner.makeButton.setLabel("Replace");
                 else
                    owner.makeButton.setLabel("Make Rule");
              }
              break;
            case 2:
              if (key == 'i')
                 key = '1';
              else if (key == 'o')
                 key = '0';
              if (MachineData.symbolNames.indexOf((char)key) >= 0 && (key != '*' || symbol == '*')) {
                 newSymbol = (char)key;
                 repaintColumn(2);
              }
              break;
            case 3:
              if (key == 'l') {
                 direction = false;
                 repaintColumn(3);
              }
              else if (key == 'r') {
                 direction = true;
                 repaintColumn(3);
              }
              break;
            case 4:
              if (key == 'h') {
                 newState = MachineData.HALTSTATE;
                 repaintColumn(4);
              }
              else if (key >= '0' && key <= '9') {
                 int st;
                 if (newState == MachineData.HALTSTATE)
                    st = key - '0';
                 else {
                    st = 10*newState + key - '0';
                    if (st >= MachineData.STATES || firstKeyDownInSelection)
                        st = key - '0';
                 }
                 newState = st;
                 repaintColumn(4);
                 firstKeyDownInSelection = false;
              }
              break;
         }
      }
      else if (key == Event.LEFT || key == Event.RIGHT) {
         if (key == Event.LEFT)
            selectedColumn = (selectedColumn == 0)? 4 : selectedColumn - 1;
         else
            selectedColumn = (selectedColumn == 4)? 0 : selectedColumn + 1;
         if (selectedColumn == 0)
            owner.requestFocus(MachinePanel.RULEMAKERFOCUS,Palette.STATES);
         else if (selectedColumn == 1)
            owner.requestFocus(MachinePanel.RULEMAKERFOCUS,Palette.SYMBOLSANDDEFAULT);
         else if (selectedColumn == 2)
            if (symbol == '*')
               owner.requestFocus(MachinePanel.RULEMAKERFOCUS,Palette.SYMBOLSANDDEFAULT);
            else
               owner.requestFocus(MachinePanel.RULEMAKERFOCUS,Palette.SYMBOLS);
         else if (selectedColumn == 3)
            owner.requestFocus(MachinePanel.RULEMAKERFOCUS,Palette.DIRECTIONS);
         else if (selectedColumn == 4)
            owner.requestFocus(MachinePanel.RULEMAKERFOCUS,Palette.STATESANDHALT);
      }
      else if (key == Event.UP || key == Event.DOWN) {
         int index = MachineData.symbolNames.indexOf(symbol);
         if (key == Event.UP) {
            index--;
            if (index < 0) {
               index = MachineData.SYMBOLS - 1;
               state--;
               if (state < 0)
                  state = MachineData.STATES - 1;
            }
         }
         else {
            index++;
            if (index >= MachineData.SYMBOLS) {
               index = 0;
               state++;
               if (state >= MachineData.STATES)
                  state = 0;
            }
         }
         char sym = MachineData.symbolNames.charAt(index);
         if (selectedColumn == 2 && sym == '*' && symbol != '*')
            owner.requestFocus(MachinePanel.RULEMAKERFOCUS,Palette.SYMBOLSANDDEFAULT);
         else if (selectedColumn == 2 && sym != '*' && symbol == '*')
            owner.requestFocus(MachinePanel.RULEMAKERFOCUS,Palette.SYMBOLS);
         symbol = sym;
         if (!data.ruleDefined(state,symbol)) 
            owner.makeButton.setLabel("Make Rule");
         else
            owner.makeButton.setLabel("Replace");         
         if (data.getNewState(state,symbol) == MachineData.UNSPECIFIED) {
            newState = state;
            newSymbol = symbol;
            direction = true;
         }
         else {
            newState = data.getNewState(state,symbol);
            newSymbol = data.getNewSymbol(state,symbol);
            direction = data.getDirection(state,symbol);
         }
         repaintContents();
      }
      else if (key == '\r' || key == '\n') {
         owner.doMakeRule();
      }
   }
   
   void processPaletteClick(int itemClicked, int paletteDisplay) {
      firstKeyDownInSelection = true;
      if (paletteDisplay == Palette.DIRECTIONS)
         processKey( (itemClicked == 0)?  'L'  :  'R' );
      else if (paletteDisplay == Palette.SYMBOLS || paletteDisplay == Palette.SYMBOLSANDDEFAULT)
         processKey( MachineData.symbolNames.charAt(itemClicked) );
      else if (paletteDisplay == Palette.STATES || paletteDisplay == Palette.STATESANDHALT) {
         if (itemClicked == -1)
            processKey('h');
         else if (selectedColumn == 0) {
            state = itemClicked;
            if (!data.ruleDefined(state,symbol))
               owner.makeButton.setLabel("Make Rule");
            else
               owner.makeButton.setLabel("Replace");
            repaintColumn(0);
         }
         else if (selectedColumn == 4) {
            newState = itemClicked;
            repaintColumn(4);
         }
      }
   }
   
   synchronized public boolean mouseDown(Event evt, int x, int y) {
      if (data == null)
         return true;
      int col = x / (width/5);
      if (col < 0)
         col = 0;
      else if (col > 4)
         col = 4;
      if (hasFocus && col == selectedColumn)
         return true;
      selectedColumn = col;
      int ds;
      if (col == 0)
         ds = Palette.STATES;
      else if (col == 1)
         ds = Palette.SYMBOLSANDDEFAULT;
      else if (col == 2)
         if (symbol == '*')
            ds = Palette.SYMBOLSANDDEFAULT;
         else
            ds = Palette.SYMBOLS;
      else if (col == 3)
         ds = Palette.DIRECTIONS;
      else
         ds = Palette.STATESANDHALT;
      owner.requestFocus(MachinePanel.RULEMAKERFOCUS,ds);
      return true;
   }
   
   // -----------------------------------------------------------------------------------

}