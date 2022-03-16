package tmcm.xTuringMachine;
import java.awt.*;

class RuleTable extends Panel {

   Scrollbar scroll;
   RuleCanvas canvas;
   
   MachineData data;
   MachinePanel owner;
   
   RuleTable(MachinePanel owner) {
      setLayout(new BorderLayout());
      scroll = new Scrollbar(Scrollbar.VERTICAL);
      add("East",scroll);
      canvas = new RuleCanvas(this);
      add("Center",canvas);
      this.owner = owner;
   }
      
   void setMachineData(MachineData data) {
      this.data = data;
      int ct = (data == null)? 0 : data.getRuleCount();
      canvas.topRule = 0;
      if (data == null || canvas.visibleRules >= ct) {
          scroll.disable();
          scroll.setValues(0,1,0,1);
      }
      else {
          scroll.setValues(0,canvas.visibleRules,0,ct);
          scroll.enable();
      }
      canvas.selectedRule = -1;
      owner.deleteRuleButton.disable();
      canvas.repaint(2,canvas.lineHeight+4,canvas.width-4,canvas.height-canvas.lineHeight-6);       
   }
   
   void doDeleteRule() {
      if (canvas.selectedRule < 0 || data == null)
         return;
      Rule rule = data.getRule(canvas.selectedRule);
      data.deleteRule(rule.state,rule.symbol);
      if (rule.state == owner.ruleMaker.state && rule.symbol == owner.ruleMaker.symbol)
         owner.makeButton.setLabel("Make Rule");
      canvas.selectedRule = -1;
      canvas.resetScroll();
      owner.deleteRuleButton.disable();
      owner.dropFocus(MachinePanel.RULETABLEFOCUS);
   }
   
   void ruleAdded(int state, char symbol) {   
      int index = data.findRule(state,symbol);
      canvas.selectedRule = index;
      canvas.selectedColumn = 0;
      owner.deleteRuleButton.enable();
      canvas.resetScroll();
   }
   
   void ruleChanged(int state, char symbol) {
      int index = data.findRule(state,symbol);
      if (index == canvas.selectedRule)
         canvas.selectedRule = -1;  // this will force canvas.select to repaint
      canvas.select(index,0);
   }

   public boolean handleEvent(Event evt) {
      if (evt.id == Event.SCROLL_LINE_DOWN || evt.id == Event.SCROLL_LINE_UP || 
          evt.id == Event.SCROLL_PAGE_DOWN || evt.id == Event.SCROLL_PAGE_UP || 
          evt.id == Event.SCROLL_ABSOLUTE) {
         owner.dropFocus(MachinePanel.RULETABLEFOCUS);
         canvas.setStart();
         return true;
      }
      return super.handleEvent(evt);
   }
      

}


class RuleCanvas extends Canvas {

   int lineHeight = -1;

   int width;
   int height;
   int[] columnLoc = new int[11];
   
   RuleTable owner;

   int topRule;
   int visibleRules;
   
   int selectedRule = -1;
   int selectedColumn;
   boolean hasFocus;
   
   Font font;
   FontMetrics fm;

   RuleCanvas(RuleTable owner) {
      setBackground(Color.white);
      this.owner = owner;
   }
   
   public void paint(Graphics g) {
      if (lineHeight == -1 || width != size().width || height != size().height)
         setUp();
      g.setFont(font);
      g.drawRect(0,0,width+1,height);
      g.drawRect(1,1,width-1,height-2);
      g.drawLine(0,height-2,width,height-2);
      g.drawLine(width-1,0,width-1,height);
      g.drawLine(0,lineHeight+2,width,lineHeight+2);
      g.drawLine(0,lineHeight+3,width,lineHeight+3);
      g.drawLine(columnLoc[2], 0, columnLoc[2], height);
      g.drawLine(columnLoc[4], 0, columnLoc[4], height);
      g.drawLine(columnLoc[4] - 1, 0, columnLoc[4] - 1, height);
      g.drawLine(columnLoc[6], 0, columnLoc[6], height);
      g.drawLine(columnLoc[8], 0, columnLoc[8], height);
      int h = 2 + (lineHeight + fm.getAscent() - fm.getDescent())/2;
      g.setColor(Color.blue);
      g.drawString("In State", columnLoc[1] - fm.stringWidth("In State")/2, h);
      g.drawString("Reading", columnLoc[3] - fm.stringWidth("Reading")/2, h);
      g.drawString("Write", columnLoc[5] - fm.stringWidth("Write")/2, h);
      g.drawString("Move", columnLoc[7] - fm.stringWidth("Move")/2, h);
      g.drawString("New State", columnLoc[9] - fm.stringWidth("New State")/2, h);
      g.setColor(Color.black);
      if (owner.data == null)
         return;
      h += lineHeight + 3;
      int i = topRule;
      while ( h < height - 4) {
         Rule rule = owner.data.getRule(i);
         if (rule == null)
            return;
         if (i == selectedRule)
            g.setColor(Color.red);
         else
            g.setColor(Color.black);
         String str = String.valueOf(rule.state);
         g.drawString(str,columnLoc[1] - fm.stringWidth(str)/2,h);
         if (rule.symbol == '*')
            str = "other";
         else
            str = String.valueOf(rule.symbol);
         g.drawString(str,columnLoc[3] - fm.stringWidth(str)/2,h);
         if (rule.newSymbol == '*')
            str = "same";
         else
            str = String.valueOf(rule.newSymbol);
         g.drawString(str,columnLoc[5] - fm.stringWidth(str)/2,h);
         if (rule.direction)
            str = "R";
         else
            str = "L";
         g.drawString(str,columnLoc[7] - fm.stringWidth(str)/2,h);
         if (rule.newState == MachineData.HALTSTATE)
            str = "h";
         else
            str = String.valueOf(rule.newState);
         g.drawString(str,columnLoc[9] - fm.stringWidth(str)/2,h);
         if (hasFocus && i == selectedRule)
            putHilite(g,i-topRule,selectedColumn);
         h += lineHeight;
         i += 1;
      }      
   }
   
   void putHilite(Graphics g, int line, int column) {
      g.setColor(Palette.hiliteColor);
      int x = columnLoc[4+2*column]+4;
      int width = columnLoc[6+2*column] - 3 - x;
      int y = (line+1)*lineHeight + 6;
      int height = lineHeight;
      g.drawRect(x,y,width,height);
      g.drawRect(x+1,y+1,width-2,height-2);
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
      lineHeight = fm.getHeight() + 5;
      visibleRules = (height - lineHeight - 6) / lineHeight;
      if (owner.data == null || visibleRules >= owner.data.getRuleCount()) {
         topRule = 0;
         owner.scroll.disable();
         owner.scroll.setValues(0,1,0,1);
      }
      else if (topRule + visibleRules >= owner.data.getRuleCount()) {
         topRule = owner.data.getRuleCount() - visibleRules;
         owner.scroll.setValues(topRule,visibleRules,0,topRule+visibleRules);
         owner.scroll.enable();
      }
      else {
         owner.scroll.setValues(topRule,visibleRules,0,owner.data.getRuleCount());
         owner.scroll.enable();
      }
   }
   
   void select(int index, int column) {
      if (selectedRule == index && selectedColumn == column && (index == -1 || (index >= topRule && index < topRule + visibleRules)))
         return;
      if (index == -1 && (selectedRule < topRule || selectedRule >= topRule + visibleRules)) {
         selectedRule = -1;
         return;
      }
      firstKeyDownInSelection = true;
      int oldIndex = selectedRule;
      selectedRule = index;
      if (column >= 0)
         selectedColumn = column;
      if (index == -1)
         owner.owner.deleteRuleButton.disable();
      else
         owner.owner.deleteRuleButton.enable();
      if (index >= 0 && (index < topRule || index >= topRule+visibleRules)) {
          int newTop;
          if (index < topRule) 
             newTop = index;
          else
             newTop = index - visibleRules + 1;
          owner.scroll.setValue(newTop);
          setStart();
      }
      else {
         if (oldIndex != -1)
            repaint(2,(oldIndex-topRule+1)*lineHeight+5,width-3,lineHeight+3);
         if (selectedRule != -1 && selectedRule != oldIndex)
            repaint(2,(selectedRule-topRule+1)*lineHeight+5,width-3,lineHeight+3);
      }
   }
   
   void resetScroll() { // number of items has changed
      if (owner.data == null || owner.data.getRuleCount() <= visibleRules) {
         topRule = 0;
         owner.scroll.disable();
         owner.scroll.setValues(0,1,0,1);
      }
      else {
         if (selectedRule >= 0 && selectedRule < topRule)
            topRule = selectedRule;
         else if (selectedRule >= topRule + visibleRules)
            topRule = selectedRule - visibleRules + 1;
         int max = owner.data.getRuleCount() - visibleRules;
         if (topRule > max)
            topRule = max;
         owner.scroll.setValues(topRule,visibleRules,0,max+visibleRules);
         owner.scroll.enable();
      }
      setStart();
   }
   
   void setStart() {
      topRule = owner.scroll.getValue();
      if (topRule > owner.data.getRuleCount() - visibleRules) {
         topRule = Math.max(0,owner.data.getRuleCount() - visibleRules);
         owner.scroll.setValue(topRule);
      }
      repaint(2,lineHeight+4,width-3,height-lineHeight-6);
   }   
   
   // ------------------------------ focus stuff ----------------------------------------
   
   void repaintSelection() {
      if (selectedRule == -1)
         return;
      int line = selectedRule - topRule;
      int x = columnLoc[4+2*selectedColumn]+1;
      int width = columnLoc[6+2*selectedColumn] - x;
      int y = (line+1)*lineHeight + 5;
      int height = lineHeight+3;
      repaint(x,y,width,height);
   }
   
   
   boolean firstKeyDownInSelection;
   
   void focusIsOn() {
      hasFocus = true;
      repaintSelection();
      firstKeyDownInSelection = true;
   }
   
   void focusIsOff() {
      hasFocus = false;
      repaintSelection();
   }
   
   void processKey(int key) {
      if (owner.data == null)
         return;
      Rule rule = owner.data.getRule(selectedRule);
      if (rule == null)
         return;
      if (key >= ' ' && key <= 127) {
        if (key == ' ')
           key = '#';
        else
           key = Character.toLowerCase((char)key);
        switch (selectedColumn) {
           case 0:
              if (key == 'i')
                 key = '1';
              else if (key == 'o')
                 key = '0';
              if (rule.newSymbol != key && MachineData.symbolNames.indexOf((char)key) >= 0 && (key != '*' || rule.symbol == '*')) {
                 owner.data.setActionData(rule.state,rule.symbol,(char)key,rule.direction,rule.newState);
                 repaintSelection();
              }
              break;
           case 1:
              if (key == 'l' && rule.direction) {
                 owner.data.setActionData(rule.state,rule.symbol,rule.newSymbol,false,rule.newState);
                 repaintSelection();
              }
              else if (key == 'r' && !rule.direction) {
                 owner.data.setActionData(rule.state,rule.symbol,rule.newSymbol,true,rule.newState);
                 repaintSelection();
              }
              break;
           case 2:
              if (key == 'h') {
                 if (rule.newState != MachineData.HALTSTATE) {
                    owner.data.setActionData(rule.state,rule.symbol,rule.newSymbol,rule.direction,MachineData.HALTSTATE);
                    repaintSelection();
                 }
              }
              else if (key >= '0' && key <= '9') {
                 int newState;
                 if (rule.newState != MachineData.HALTSTATE)
                    newState = rule.newState * 10 + key - '0';
                 else 
                    newState = key - '0';
                 if (newState >= MachineData.STATES || firstKeyDownInSelection)
                    newState = key - '0';
                 if (newState != rule.newState) {
                    owner.data.setActionData(rule.state,rule.symbol,rule.newSymbol,rule.direction,newState);
                    repaintSelection();
                 }
                 firstKeyDownInSelection = false;
              }
              break;
        }
      }
      else if (key == Event.LEFT || key == Event.RIGHT) {
         selectedColumn = (key == Event.LEFT)? selectedColumn - 1 : selectedColumn + 1;
         if (selectedColumn < 0)
            selectedColumn = 2;
         else if (selectedColumn > 2)
            selectedColumn = 0;
         if (selectedColumn == 0)
            if (rule.symbol == '*')
               owner.owner.requestFocus(MachinePanel.RULETABLEFOCUS,Palette.SYMBOLSANDDEFAULT);
            else
               owner.owner.requestFocus(MachinePanel.RULETABLEFOCUS,Palette.SYMBOLS);
         else if (selectedColumn == 1)
            owner.owner.requestFocus(MachinePanel.RULETABLEFOCUS,Palette.DIRECTIONS);
         else
            owner.owner.requestFocus(MachinePanel.RULETABLEFOCUS,Palette.STATESANDHALT);
         if (selectedRule != -1)
            repaint(columnLoc[4]+1,(selectedRule-topRule+1)*lineHeight+5,width-2-columnLoc[4],lineHeight+3);
      }
      else if (key == Event.UP) {
         int i = selectedRule - 1;
         if (i >= 0)
            select(i,-1);
      }
      else if (key == Event.DOWN) {
         int i = selectedRule + 1;
         if (i < owner.data.getRuleCount())
            select(i,-1);
      }
      else if (key == Event.HOME) {
         select(0,-1);
      }
      else if (key == Event.END) {
         select(owner.data.getRuleCount()-1, -1);
      }
      else if (key == Event.PGUP) {
         int i = selectedRule - visibleRules;
         if (i < 0)
            i = 0;
         select(i,-1);
      }
      else if (key == Event.PGDN) {
         int i = selectedRule + visibleRules;
         if (i >= owner.data.getRuleCount())
            i = owner.data.getRuleCount() - 1;
         select(i,-1);
      }
   }
   
   void processPaletteClick(int itemClicked, int paletteDisplay) {
      if (paletteDisplay == Palette.DIRECTIONS)
         processKey( (itemClicked == 0)?  'L'  :  'R' );
      else if (paletteDisplay == Palette.SYMBOLS || paletteDisplay == Palette.SYMBOLSANDDEFAULT)
         processKey( MachineData.symbolNames.charAt(itemClicked) );
      else if (paletteDisplay == Palette.STATESANDHALT && selectedColumn == 2) {
         if (owner.data == null)
            return;
         Rule rule = owner.data.getRule(selectedRule);
         if (rule == null)
            return;
         int state;
         if (itemClicked == -1)
            state = MachineData.HALTSTATE;
         else
            state = itemClicked;
         if (state == rule.newState)
            return;
         owner.data.setActionData(rule.state,rule.symbol,rule.newSymbol,rule.direction,state);
         repaintSelection();
      }
      firstKeyDownInSelection = true;
   }
   
   public boolean mouseDown(Event evt, int x, int y) {
      if (owner.data == null)
         return true;
      int row = (y - lineHeight - 5) / lineHeight;
      if (row < 0 || owner.data == null)
         return true;
      int index = topRule + row;
      if (index >= topRule + visibleRules || index >= owner.data.getRuleCount())
         return true;
      int col = x / (width/5) - 2;
      if (col < 0)
         col = 0;
      else if (col > 2)
         col = 2;
      if (index != selectedRule)
         select(index,col);
      else if (selectedColumn != col) {
         selectedColumn = col; 
         repaint(2,(index-topRule+1)*lineHeight+5,width-3,lineHeight+3);
      }
      if (col == 0) {
         Rule rule = owner.data.getRule(index);
         if (rule.symbol == '*')
            owner.owner.requestFocus(MachinePanel.RULETABLEFOCUS,Palette.SYMBOLSANDDEFAULT);
         else
            owner.owner.requestFocus(MachinePanel.RULETABLEFOCUS,Palette.SYMBOLS);
      }
      else if (col == 1)
         owner.owner.requestFocus(MachinePanel.RULETABLEFOCUS,Palette.DIRECTIONS);
      else if (col == 2)
         owner.owner.requestFocus(MachinePanel.RULETABLEFOCUS,Palette.STATESANDHALT);
      return true;
   }
   
   // -----------------------------------------------------------------------------------

}