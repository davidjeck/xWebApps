package tmcm.xComputer;
import java.util.Vector;

class ParseError extends Exception {
   int pos;
   ParseError(String message, int pos) {
      super(message);
      this.pos = pos;
   }
}

class LabelData {
   String name;
   short numericalValue;
}

class Parser {
   
   short[] parse(String str) throws ParseError{
      data = new short[1024];
      dataCt = 0;
      input = str;
      length = str.length();
      pos = 0;
      labelNames = new Vector();
      buffer = new StringBuffer(50);
      unfinishedLabelData = new int[1024];
      for (int i = 0; i < 1024; i++)
         unfinishedLabelData[i] = -1;
      skip();
      if (length == pos)
         doError("The program does not contain any assembly language instructions.");
      while (pos < length) {
          char ch = input.charAt(pos);
          if ((Character.isLetter(ch) || ch == '_') && readLabelDefinition()) {
             skip();
             continue;
          }
          if (ch == 'B' || ch == 'b')
             addData(readLabelOrBinary(false));
          else if (Character.isLetter(ch) || ch == '_')
             addData(readInstruction(true));
          else if (Character.isDigit(ch)) { // number or rep count
             int n = 0;
             while (pos < length && n < 100000 && Character.isDigit(input.charAt(pos))) {
                n = 10*n + Character.digit(input.charAt(pos),10);
                pos++;
             }
             skipSpaces();
             if (pos < length && input.charAt(pos) == '#') {
                pos++;
                readRepeatedValue(n);
             }
             else if (n > 65535)
                doError("Integer larger than maximum permitted value of 65535.");
             else
                addData(n);
          }
          else if (ch == '+' || ch == '-')
             addData(readInt(false));
          else if (ch == '$')
             addData(readHex(false));
          else if (ch == '\'')
             addData(readAscii(false));
          else if (ch == '\"')
             readString();
          else if (ch == '@') {
             readDataCt();
             skip();
             continue;
          }
          else
             doError("Unexpected Character ('" + ch + "') found in program.");
          skipSpaces();
          if (pos < length && input.charAt(pos) != ';' && input.charAt(pos) != '\r'
                     && input.charAt(pos) != '\n')
             doError("Extra stuff found on line after data item. Only one item is allowed per line.");
          skip();
      }
      for (int i = 0; i < labelNames.size(); i++) {
         LabelData info = (LabelData)labelNames.elementAt(i);
         if (info.numericalValue < 0)
            doError("The label \"" + info.name + "\", which was used in the program, is undefined.");
      }
      for (int i = 0; i < 1024; i++)
         if (unfinishedLabelData[i] >= 0)
            data[i] |= ((LabelData)labelNames.elementAt(unfinishedLabelData[i])).numericalValue;
      short[] temp = data;
      data = null;
      labelNames = null;
      input = null;
      buffer = null;
      unfinishedLabelData = null;
      return temp;
   }
   
   short parseOneInstruction(String str) throws ParseError {
      input = str;
      length = str.length();
      pos = 0;
      buffer = new StringBuffer(20);
      skip();
      if (pos == length)
         doError("No Data.");
      int n = 0;
      if (input.charAt(pos) == 'b' || input.charAt(pos) == 'B')
         n = readBinary(false);
      else if (input.charAt(pos) == '$')
         n = readHex(false);
      else if (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '-' || input.charAt(pos) == '+')
         n = readInt(false);
      else if (input.charAt(pos) == '\'')
         n = readAscii(false);
      else if (Character.isLetter(input.charAt(pos)))
         n = readInstruction(false);
      else
         doError("Illegal data.");
      skip();
      if (pos != length)
         doError("Illegal data.");
      short temp = (short)n;
      input = null;
      buffer = null;
      return temp;
   }
   
   //  The rest is private
   
   private String input;
   private int length;
   private int pos;
      
   private short[] data;
   private int dataCt;
   
   private int[] unfinishedLabelData; 
   
   private StringBuffer buffer;

   private Vector labelNames;
   
   private void doError(String message) throws ParseError{
      data = null;
      input = null;
      labelNames = null;
      throw new ParseError(message,pos);
   }
   
   private void skipSpaces() {
      while (pos < length && (input.charAt(pos) == ' ' || input.charAt(pos) == '\t'))
         pos++;
   }

   private void skip() {
      while (pos < length && (Character.isSpace(input.charAt(pos)) || input.charAt(pos) == ';')) {
         if (input.charAt(pos) == ';') 
            do {
               pos++;
            } while (pos < length && input.charAt(pos) != '\r' && input.charAt(pos) != '\n');
         else
            pos++;
      }
   }
   
   private String readWord() {  // assumes next char is a letter or underscore
      buffer.setLength(0);
      while (pos < length && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
         buffer.append(input.charAt(pos));
         pos++;
      }
      return buffer.toString().toUpperCase();
   }
   
   private int readHex(boolean TenBit) throws ParseError {   // assumes next char is a $
      pos++;
      int n = 0;
      boolean found = false;
      while (pos < length && n < 100000 &&
                (Character.isDigit(input.charAt(pos)) ||
                    (input.charAt(pos) >= 'a' && input.charAt(pos) <= 'f') ||
                         (input.charAt(pos) >= 'A' && input.charAt(pos) <= 'F'))) {
         n = 16*n + Character.digit(input.charAt(pos),16);
         pos++;
         found = true;
      }
      if (!found)
         doError("Illegal hexadecimal number; no digits found after '$'.");
      if (TenBit) {
         if (n >1023)
            doError("Hexadecimal number larger than maximum legal value of $3FF used as instruction data.");
      }
      else {
         if (n > 0xFFFF)
            doError("Hexadecimal number larger than maximum permitted value of $FFFF.");
      }
      return n;
   }
   
   private int readInt(boolean TenBit)  throws ParseError{   // assumes next char is a digit or '-' or '+'
      boolean neg = false;
      if (input.charAt(pos) == '+' || input.charAt(pos) == '-') {
         neg = (input.charAt(pos) == '-');
         if (neg && TenBit)
            doError("Negative numbers not allowed as data for instructions; value must be between 0 ans 1023.");
         pos++;
         if (pos == length || !Character.isDigit(input.charAt(pos)))
            doError("Illegal number; no digits found after '" + input.charAt(pos-1) + "'.");
      }
      int n = 0;
      while (pos < length && n < 100000 && Character.isDigit(input.charAt(pos))) {
         n = 10*n + Character.digit(input.charAt(pos),10);
         pos++;
      }
      if (neg)
         n = -n;
      if (TenBit) {
         if (n >1023)
            doError("Integer larger than maximum legal value of 1023 used as instruction data.");
      }
      else {
         if (n > 65535)
            doError("Integer larger than maximum permitted value of 65535.");
         if (n < -32768)
            doError("Integer less than the mimimum permitted value of -32768.");
      }
      return n;
   }
   
   private int readBinary(boolean TenBit)  throws ParseError{  // assumes next char is a 'b' or 'B'
      pos++;
      int n = 0;
      boolean found = false;
      while (pos < length && (input.charAt(pos) == '0' || input.charAt(pos) == '1')) {
         n = 2*n + Character.digit(input.charAt(pos),2);
         pos++;
         found = true;
      }
      if (!found)
         doError("No digits provided in binary number.");
      if (TenBit) {
         if (n >1023)
            doError("Binary number with value larger than 1023 used as instruction data.");
      }
      else {
         if (n > 65535)
            doError("Binary number with value larger than maximum permitted value of 65535.");
      }
      return n;
   }
   
   private int readAscii(boolean TenBit)  throws ParseError{ // assumes next char is a '
      pos++;
      if (pos == length || input.charAt(pos) == '\n' || input.charAt(pos) == '\r')
         doError("Illegal ASCII value; no characters found on line after the \"'\".");
      int n = (int)input.charAt(pos++);
      if (TenBit)
         return n;
      if (pos < length && input.charAt(pos) != '\n' && input.charAt(pos) != '\r')
         n = 256*n + (int)input.charAt(pos++);
      return n;
   }
   
   private int readLabelOrBinary(boolean TenBit) throws ParseError {  // assumes next char is a letter or underscore
     int data = 0;
     int savePos = pos;
     String label = readWord();
     boolean binary = true;
     if (label.charAt(0) != 'b' && label.charAt(0) != 'B')
        binary = false;
     else if (label.length() == 1)
        binary = false;
     else {
        for (int j = 1; j < label.length(); j++)
           if (label.charAt(j) != '0' && label.charAt(j) != '1') {
              binary = false;
              break;
           }
     }
     if (binary) {
        pos = savePos;
        data = readBinary(TenBit);
     }
     else if (label.equals("DATA")) {
        if (TenBit)
           doError("The word \"DATA\" has a special meaning.  It cannot be used as a label.");
        else 
           return getValueForData();
     }
     else {
        int infoLoc = findLabel(label);
        if (infoLoc == -1) {
           LabelData info = new LabelData();
           info.name = label;
           info.numericalValue = -1;
           labelNames.addElement(info);
           if (dataCt <= 1023)
              unfinishedLabelData[dataCt] = labelNames.size() - 1;
        }
        else {
           LabelData info = (LabelData)labelNames.elementAt(infoLoc);
           if (info.numericalValue >= 0)
              data = info.numericalValue;
           else if (dataCt < 1024)
              unfinishedLabelData[dataCt] = infoLoc;
        }
     }
     return data;  // this is zero if it's an undefined label
   }
   
   private int readInstruction(boolean doingProgram) throws ParseError {  // assumes next char is a letter
      String ins = readWord();
      int insNum = -1;
      if (ins.length() == 3) {
         for (int i = 0; i < 16; i++)
            if (ins.equals(Globals.InstructionName[i])) {
               insNum = i;
               break;
            }
      }
      else if (ins.equals("OR"))
         insNum = Globals._or;
      if (insNum == -1) {        
         if (!doingProgram)
            doError("Unknown Instruction, \"" + ins + "\".");
         else {
            if (ins.equals("DATA"))
               return getValueForData();
            skipSpaces();
            if (pos < length && input.charAt(pos) != ';' &&
                        input.charAt(pos) != '\n' && input.charAt(pos) != '\r')
                doError("Extra stuff found on line.  (Possibly a missing ':' after a label name.)");
            int loc = findLabel(ins);
            if (loc == -1) {
               LabelData info = new LabelData();
               info.name = ins;
               info.numericalValue = -1;
               labelNames.addElement(info);
               if (dataCt < 1024)
                   unfinishedLabelData[dataCt] = labelNames.size() - 1;
               return 0;
            }
            else {
               LabelData info = (LabelData)labelNames.elementAt(loc);
               if (info.numericalValue == -1) {
                  if (dataCt < 1024)
                     unfinishedLabelData[dataCt] = loc;
                  return 0;
               }
               else
                  return info.numericalValue;
            }
         }
      }
      boolean requiresData = Globals.hasData.get(insNum);
      int n = insNum << 10;
      if (pos < length && input.charAt(pos) == '-') {
         pos++;
         if (pos == length)
            doError("Missing mode specification for \"" + ins + "-\".");
         char mode = input.charAt(pos);
         if (mode == '?') {
            if (!doingProgram)
               doError("Illegal Mode.");
            n |= 0xC000;
         }
         else if (mode == 'c' || mode == 'C') {
            if (!doingProgram && !Globals.hasConstantMode.get(insNum))
               doError("Illegal Mode.");
            n |= 0x4000;
         }
         else if (mode == 'i' || mode == 'I') {
            if (!doingProgram && !Globals.hasIndirectMode.get(insNum))
               doError("Illegal Mode.");
            n |= 0x8000;
         }
         else
            doError("Illegal mode specification for \"" + ins + "-\".  The mode must be C, I, or ?.");
         pos++;
      }
      skipSpaces();
      int data = 0;
      if (pos == length && requiresData)
         doError("Missing data for instruction (\"" + ins + "\") that requires data.");
      if (pos == length)
         data = 0;
      else if (doingProgram && (Character.isLetter(input.charAt(pos)) || input.charAt(pos) == '_'))
         data = readLabelOrBinary(true);
      else if (input.charAt(pos) == '$')
         data = readHex(true);
      else if (input.charAt(pos) == '\'')
         data = readAscii(true);
      else if (input.charAt(pos) == 'b' || input.charAt(pos) == 'B')
         data = readBinary(true);
      else if (input.charAt(pos) == '+' || Character.isDigit(input.charAt(pos)))
         data = readInt(true);
      else if (requiresData)
         doError("Missing data for instruction (\"" + ins + "\") that requires data.");
      skipSpaces();
      if (pos < length && input.charAt(pos) != ';' &&
                input.charAt(pos) != '\r' && input.charAt(pos) != '\n')
          doError("Extra stuff found on line after a legal instruction.");
      return (n | data);
   }
   
   private int findLabel(String name) {
      for (int i = 0; i < labelNames.size(); i++)
         if ( ((LabelData)labelNames.elementAt(i)).name.equals(name) )
            return i;
      return -1;
   }
   
   private int getValueForData() throws ParseError {
      skipSpaces();
      if (pos == length)
         return 0;
      else if ((Character.isDigit(input.charAt(pos)) ||
                         input.charAt(pos) == '-' || input.charAt(pos) == '+'))
          return readInt(false);
      else if (input.charAt(pos) == '$')
          return readHex(false);
      else if (input.charAt(pos) == '\'')
          return readAscii(false);
      else if (input.charAt(pos) == 'b' || input.charAt(pos) == 'B')
          return readBinary(false);
      else
          return 0;
   }

   private boolean readLabelDefinition() throws ParseError {
     int savePos = pos;
     String name = readWord();
     skipSpaces();
     if (pos < length && input.charAt(pos) == ':') {
        if (name.equals("DATA"))
           doError("The word \"DATA\" has a special meaning and cannot be redefined as a label.");
        pos++;
        int i = findLabel(name);
        if (i >= 0) {
           LabelData info = (LabelData)labelNames.elementAt(i);
           if (info.numericalValue < 0)
              info.numericalValue = (short)dataCt;
           else
             doError("A second definition was found for a label, \"" + name + "\", which was already defined earlier in the program");
        }
        else {
           boolean binary = true;
           if (name.charAt(0) != 'B' && name.charAt(0) != 'b')
              binary = false;
           else if (name.length() == 1)
              binary = false;
           else {
              for (int j = 1; j < name.length(); j++)
                 if (name.charAt(j) != '0' && name.charAt(j) != '1') {
                    binary = false;
                    break;
                 }
           }
           if (binary)
              doError("The binary number \"" + name + "\" cannot be used as a label.");
           LabelData info = new LabelData();
           info.name = name;
           info.numericalValue = (short)dataCt;
           labelNames.addElement(info);
        }
        return true;
     }
     else {
        pos = savePos;
        return false;
     }
   }
   
   private void readString() throws ParseError {
      pos++;
      while (true) {
        if (pos < length && input.charAt(pos) == '\"') {
           pos++;
           if (pos < length && input.charAt(pos) == '\"') {
              addData((int)'\"');
              pos++;
           }
           else return;
        }
        if (pos == length || input.charAt(pos) == '\n' || input.charAt(pos) == '\r')
           doError("Line ended inside a string.  (Strings cannot extend past and end-of-line.)");
        addData((int)input.charAt(pos));
        pos++;
     }
   }
   
   private void readDataCt() throws ParseError {
     pos++;
     skipSpaces();
     if (pos == length || !Character.isDigit(input.charAt(pos)))
        doError("Missing location number for '@' directive.");
     int n = 0;
     while (pos < length && n < 10000 && Character.isDigit(input.charAt(pos))) {
        n = 10*n + Character.digit(input.charAt(pos), 10);
        pos++;
     }
     if (n > 1023)
        doError("Illegal value in '@' directive; value must be between 0 and 1023.");
     dataCt = n;
   }
   
   private void readRepeatedValue(int repCount) throws ParseError {
      if (repCount > 1024 || repCount <= 0)
         doError("Illegal repetition count, " + repCount + ".");
      int saveDataCt = dataCt;
      int data  = 0;
      skip();
      char ch = input.charAt(pos);
      if (ch == 'B' || ch == 'b') {
         data = readLabelOrBinary(false);
         skipSpaces();
         if (pos < length && input.charAt(pos) == ':')
            doError("It is not legal to have a labeled value after a repetition count.");
      }
      else if (Character.isLetter(ch) || ch == '_')
         data = readInstruction(true);
      else if (Character.isDigit(ch) || ch == '+' || ch == '-') {
         data = readInt(false);
         skipSpaces();
         if (pos < length && input.charAt(pos) == '#')
            doError("It is not legal to have two repetition directives in a row, without a value between them.");
      }
      else if (ch == '$')
         data = readHex(false);
      else if (ch == '\'')
         data = readAscii(false);
      else
         doError("Unexpected character ('" + ch + "') found after '#' while looking for the value for a repitition directive.");
      for (int i = 0; i < repCount; i++)
         addData(data);
      if (unfinishedLabelData[saveDataCt] >= 0)
         for (int i = 1; i < repCount; i++)
            unfinishedLabelData[saveDataCt+i] = unfinishedLabelData[saveDataCt];
   }
   
   
   private void addData(int val) throws ParseError {
      if (dataCt >= 1024)
         doError("Attempt to store into non-existant memory location (location no. " + dataCt + ")");
      data[dataCt] = (short)val;
      dataCt++;
   }
      

}