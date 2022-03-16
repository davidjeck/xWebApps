package tmcm.xTuringMachine;
import java.util.Vector;
import java.io.*;
import java.net.*;

class Rule {
   int state;
   char symbol;
   int newState;
   char newSymbol;
   boolean direction;  // left = false, right = true
}

class MachineData {
  
   static final int STATES = 25;
   static final int SYMBOLS = 8;  //  #  x  y  z  0  1  $ *  (* = default, # = blank)
   
   static final String symbolNames = "#$01xyz*";  // ASCII ordering, except for *
                                                  // '#' and '*' can't be changed
   
   static final int UNSPECIFIED = 999;
   static final int HALTSTATE = -1;
   static final int DEFAULT = 7;
   
   private Rule[] ruleList = new Rule[STATES*SYMBOLS];
   private int ruleCt = 0;

   private char[][] newSymbol = new char[STATES][SYMBOLS];
   private boolean[][] moveDirection  = new boolean[STATES][SYMBOLS];  // left or right
   private int[][] newState = new int[STATES][SYMBOLS];
   
   private StringBuffer tape_pos = new StringBuffer();
   private StringBuffer tape_neg = new StringBuffer();
   
   int saveCurrentSquare;  // when this machine is not active!
   
   MachineData() {
      clearRules();
      tape_neg.append(' ');
      for (int i = 0; i < STATES*SYMBOLS; i++)
         ruleList[i] = new Rule();
   }
   
   void clearRules() {
      for (int i = 0; i < STATES; i++)
         for (int j = 0; j < SYMBOLS; j++) {
            newState[i][j] = UNSPECIFIED;
            newSymbol[i][j] = '#';
            moveDirection[i][j] = false;
         }
      ruleCt = 0;
   }
   
   void clearTape() {
      tape_pos.setLength(0);
      tape_neg.setLength(1);
   }
   
   int getNewState(int state, char symbol) {  // 0 <= state <= STATES; symbol in symbolNames
      int sym = symbolNames.indexOf(symbol);
      if (sym == -1)  // shouldn't be allowed to happen
         return HALTSTATE;
      int st = newState[state][sym];
      if (st != UNSPECIFIED)
         return st;
      else
         return newState[state][DEFAULT];
   }
   
   char getNewSymbol(int state, char symbol) {
      int sym = symbolNames.indexOf(symbol);
      if (sym == -1)  // shouldn't be allowed to happen
         return '?';
      if (newState[state][sym] == UNSPECIFIED) {
         char s = newSymbol[state][DEFAULT];
         if (s == '*')
            return symbol;
         else
            return newSymbol[state][DEFAULT];
      }
      else
         return newSymbol[state][sym];
   }
   
   boolean getDirection(int state, char symbol) {
      int sym = symbolNames.indexOf(symbol);
      if (sym == -1)  // shouldn't be allowed to happen
         return true;
      if (newState[state][sym] == UNSPECIFIED)
         return moveDirection[state][DEFAULT];
      else
         return moveDirection[state][sym];
   }
   
   void setActionData(int state, char symbol, char symbolToWrite, boolean directionToMove, int stateToChangeTo) {
      int sym;
      if (symbol == '*')
         sym = DEFAULT;
      else {
         sym = symbolNames.indexOf(symbol);
         if (sym == -1)  // shouldn't happen!
            return;
      }
      setRuleListData(state,symbol,symbolToWrite,directionToMove,stateToChangeTo);
      newState[state][sym] = stateToChangeTo;
      newSymbol[state][sym] = symbolToWrite;
      moveDirection[state][sym] = directionToMove;
   }
   
   void deleteRule(int state, char symbol) {
      int sym;
      if (symbol == '*')
         sym = DEFAULT;
      else {
         sym = symbolNames.indexOf(symbol);
         if (sym == -1)  // shouldn't happen!
            return;
      }
      removeFromRuleList(state,symbol);
      newState[state][sym] = UNSPECIFIED;
   }
   
   void setTape(int pos, char ch) {
      if (pos >= 0) {
         if (pos < tape_pos.length())
            tape_pos.setCharAt(pos,ch);
         else if (ch != '#') {
            for (int i = tape_pos.length(); i < pos; i++)
               tape_pos.append('#');
            tape_pos.append(ch);
         }
      }
      else {
         pos = -pos;
         if (pos < tape_neg.length())
            tape_neg.setCharAt(pos,ch);
         else if (ch != '#') {
            for (int i = tape_neg.length(); i < pos; i++)
               tape_neg.append('#');
            tape_neg.append(ch);
         }
      }   
   }
   
   char getTape(int pos) {
      if (pos >= 0) {
         if (pos >= tape_pos.length())
            return '#';
         else
            return tape_pos.charAt(pos);
      }
      else {
         pos = -pos;
         if (pos >= tape_neg.length())
            return '#';
         else
            return tape_neg.charAt(pos);
      }
   }
   
   int firstFilledSquare() {
      int pos = tape_neg.length() - 1;
      while (pos > 0 && tape_neg.charAt(pos) == '#')
         pos--;
      if (pos == 0) {
         pos = 0;
         while (pos < tape_pos.length() && tape_pos.charAt(pos) == '#')
            pos++;
         if (pos < tape_pos.length())
            return pos;
         else
            return 0;
      }
      else
         return -pos;
   }
   
   int lastFilledSquare() {
      int pos = tape_pos.length() - 1;
      while (pos >= 0 && tape_pos.charAt(pos) == '#')
         pos--;
      if (pos < 0) {
         pos = 1;
         while (pos < tape_neg.length() && tape_neg.charAt(pos) == '#')
            pos++;
         if (pos < tape_neg.length())
            return -pos;
         else
            return 0;
      }
      else
         return pos;
   }
   
   //
   
   private void setRuleListData(int state, char symbol, char symbolToWrite, boolean directionToMove, int stateToChangeTo) {
      int index = 0;
      if (symbol == '*')
         while (index < ruleCt && (ruleList[index].state < state || (ruleList[index].state == state && ruleList[index].symbol != symbol)))
            index++;      
      else
         while (index < ruleCt && (ruleList[index].state < state || (ruleList[index].state == state && ruleList[index].symbol != '*' && ruleList[index].symbol < symbol)))
            index++;
      if (index == ruleCt) {
         ruleCt++;
         ruleList[index].state = state;
         ruleList[index].symbol = symbol;
      }
      else if (ruleList[index].state != state || ruleList[index].symbol != symbol) {
         Rule temp = ruleList[ruleCt];
         for (int i = ruleCt; i > index; i--)
            ruleList[i] = ruleList[i-1];
         ruleCt++;
         ruleList[index] = temp;
         ruleList[index].state = state;
         ruleList[index].symbol = symbol;
      }      
      ruleList[index].newState = stateToChangeTo;
      ruleList[index].newSymbol = symbolToWrite;
      ruleList[index].direction = directionToMove;
   }
   
   private void removeFromRuleList(int state, char symbol) {
      int index = 0;
      while (index < ruleCt && (ruleList[index].state < state || (ruleList[index].state == state && ruleList[index].symbol != symbol)))
         index++;
      if (index < ruleCt && ruleList[index].state == state && ruleList[index].symbol == symbol) {
         Rule temp = ruleList[index];
         for (int i = index; i < ruleCt-1; i++)
            ruleList[i] = ruleList[i+1];
         ruleList[ruleCt-1] = temp;
         ruleCt--;
      }
   }
   
   int getRuleCount() {
      return ruleCt;
   }
   
   Rule getRule(int index) {
      if (index < 0 || index >= ruleCt)
         return null;
      else
         return ruleList[index];
   }
   
   int findRule(int state, char symbol) {
      int index = 0;
      while (index < ruleCt && (ruleList[index].state < state || (ruleList[index].state == state && ruleList[index].symbol != symbol)))
         index++;
      if (index < ruleCt && ruleList[index].state == state && ruleList[index].symbol == symbol)
         return index;
      else
         return -1;
   }
   
   boolean ruleDefined(int state, char symbol) {
      int index = symbolNames.indexOf(symbol);
      if (index < 0)
         return false;
      else
         return newState[state][index] != UNSPECIFIED;
   }
   
   //------------------------------ filing ------------------------------------------

   
   void write(PrintStream out, int currentSquare) {
      out.println("xTuringMachine File Format 1.0");
      if (out.checkError())
         return;
      out.println("#$01xyz* 25");
      int first = firstFilledSquare();
      int last = lastFilledSquare();
      out.println("" + first + ' ' + last + ' ' + currentSquare + ';');
      int symCt = 0;
      for (int i = first; i <= last; i++) {
         if (symCt == 50) {
            out.println();
            symCt = 0;
         }
         out.print(getTape(i));
         symCt++;
      }
      out.println();
      out.println("" + ruleCt + ';');
      for (int r = 0; r < ruleCt; r++) {
         out.println("" + ruleList[r].state + ' ' + ruleList[r].symbol + ' ' +
                             ruleList[r].newSymbol + ' ' + ( ruleList[r].direction? 'R' : 'L' ) + ' ' +
                                 ruleList[r].newState + ';');
      }                         
   }
   
   void read(InputStream in) throws MachineInputException {
      clearRules();
      try {
         DataInputStream data = new DataInputStream(in);
         int lineCt = 1;
         String line;
         line = data.readLine();
         if (!line.trim().equalsIgnoreCase("xTuringMachine File Format 1.0"))
            throw new MachineInputException("Not a legal input file (missing header on line 1)");
         line = data.readLine();
         if (!line.trim().equalsIgnoreCase("#$01xyz* 25"))
            throw new MachineInputException("Not a legal input file (illegal list of symbols or number of states in line 2)");
         int first = getInt(data,3);
         int last = getInt(data,3);
         if (first > last)
            throw new MachineInputException("Illegal data.  (First tape square comes after last tape square.)");
         setTape(first,'#');  // force allocation of space
         setTape(last,'#');
         saveCurrentSquare = getInt(data,3);
         data.readLine();
         for (int i = first; i <= last; i++) {
            int ch;
            do { ch = data.read(); }
            while (ch == '\r' || ch == '\n');
            if (ch == -1)
               throw new MachineInputException("Illegal input.  (Number of tape symbols provided is less than number specified.)");
            int sym = symbolNames.indexOf(ch);
            if (sym < 0 || ch == '*')
               throw new MachineInputException("Illegal input.  Illegal tape symbol specified: " + (char)ch + '.');
            setTape(i,(char)ch);
         }
         data.readLine();
         int numberOfRules = getInt(data,4);
         if (numberOfRules < 0)
            throw new MachineInputException("Illegal input.  The number of rules specified is less than zero.");
         if (numberOfRules > STATES*SYMBOLS)
            throw new MachineInputException("Illegal input.  The number of rules specified is larger than the maximum.");
         data.readLine();
         for (int r = 0; r < numberOfRules; r++) {
            int state = getState(data,r+5);
            if (state == HALTSTATE)
               throw new MachineInputException("Illegal input.  Illegal rule found on line " + (r + 5) + '.');
            char symbol = getSymbol(data,r+5);
            char newSymbol = getSymbol(data,r+5);
            if (newSymbol == '*' && symbol != '*')
               throw new MachineInputException("Illegal input.  Illegal rule found on line " + (r + 5) + '.');
            boolean direction = getDirection(data,r+5);
            int newState = getState(data,r+5);
            data.readLine();
            setActionData(state,symbol,newSymbol,direction,newState);
         }
      }
      catch (IOException e) {
         throw new MachineInputException("Input error occured while reading from file. (" + e + ")");
      }
   }
   
   int getInt(DataInputStream data, int lineNum) throws MachineInputException, IOException {
      int ch;
      boolean neg = false;
      do { ch = data.read(); }
      while (ch == ' ' || ch == '\t');
      if (ch == '-') {
         neg = true;
         ch = data.read();
      }
      if (ch == -1)
         throw new MachineInputException("Unexpected end of file encountered while reading rules from file.");
      if (ch > '9' || ch < '0')
         throw new MachineInputException("Illegal data found while looking for integer on line " + lineNum + ".");
      int n = 0;
      do { 
         n = 10*n + ch - '0';
         ch = data.read();
      } while (ch >= '0' && ch <= '9');
      if (neg)
         return -n;
      else
         return n;
   }
   
   int getState(DataInputStream data, int lineNum) throws MachineInputException, IOException {
      int ch;
      boolean neg = false;
      do { ch = data.read(); }
      while (ch == ' ' || ch == '\t');
      if (ch == -1)
         throw new MachineInputException("Unexpected end of file encountered while reading rules from file.");
      if (ch == '-') {
         neg = true;
         ch = data.read();
      }
      if (ch > '9' || ch < '0')
         throw new MachineInputException("Illegal state specification found while reading rule on line  " + lineNum + ".");
      int n = 0;
      do { 
         n = 10*n + ch - '0';
         ch = data.read();
      } while (ch >= '0' && ch <= '9');
      if (neg)
         n = -n;
      if (n == -1)
         return MachineData.HALTSTATE;
      else if (n >= 0 && n < MachineData.STATES)
         return n;
      else
         throw new MachineInputException("Illegal state specification found while reading rule on line  " + lineNum + ".");
   }
   
   char getSymbol(DataInputStream data, int lineNum) throws MachineInputException, IOException {
      int ch;
      do { ch = data.read(); }
      while (ch == ' ' || ch == '\t');
      if (ch == -1)
         throw new MachineInputException("Unexpected end of file encountered while reading rules from file.");
      if (symbolNames.indexOf(ch) >= 0)
         return (char)ch;
      else 
         throw new MachineInputException("Illegal symbol found while reading rule on line " + lineNum + ".");
   }
   
   boolean getDirection(DataInputStream data, int lineNum) throws MachineInputException, IOException {
      int ch;
      do { ch = data.read(); }
      while (ch == ' ' || ch == '\t');
      if (ch == -1)
         throw new MachineInputException("Unexpected end of file encountered while reading rules from file.");
      if (ch == 'L' || ch == 'l')
         return false;
      else if (ch == 'R' || ch == 'r')
         return true;
      else 
         throw new MachineInputException("Illegal direction specification found while reading rule on line " + lineNum + ".");
   }
   
   
   
   
}


class MachineInputException extends Exception {
   MachineInputException(String message) {
      super(message);
   }
}



class MachineLoader extends Thread {
   MachinePanel owner;
   int ID;
   URL url;
   String fileName, directory;
   String errorMessage;
   MachineLoader(URL url, MachinePanel owner, int ID) {
      this.url = url;
      this.owner = owner;
      this.ID = ID;
      start();
   }
   MachineLoader(String directory, String fileName, MachinePanel owner, int ID) {
      this.fileName = fileName;
      this.directory = directory;
      this.owner = owner;
      this.ID = ID;
      start();
   }
   public void run() {
      try { setPriority(getPriority() - 1); }
      catch (RuntimeException e) { }
      try { Thread.sleep(500 + (int)(500*Math.random())); }
      catch (InterruptedException e) { }
      boolean success = false;
      InputStream in = null;
      MachineData dataRead;
      try {
         if (url != null)
            in = url.openConnection().getInputStream();
         else
            in = new FileInputStream(new File(directory,fileName));
         dataRead = new MachineData();
         dataRead.read(in);
         owner.doneLoading(dataRead,ID);
      }
      catch (MachineInputException e) {
         errorMessage = "LOAD FAILED:  " + e.getMessage();
         owner.loadingError(ID);
      }
      catch (SecurityException e) {
         errorMessage = "LOAD FAILED, SECURITY ERROR:  " + e.getMessage();
         owner.loadingError(ID);
      }
      catch (Exception e) {
         errorMessage = "LOAD FAILED, ERROR:  " + e.toString();
         owner.loadingError(ID);
      }
      finally {
         if (in != null) {
            try { in.close(); }
            catch (IOException e) { }
         }
      }
   }
}