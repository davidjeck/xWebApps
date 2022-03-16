
package tmcm.xTurtle;

class TTurtleIndent {

int charCt;
String oldText;
StringBuffer newText;
int len;
int pos;
int indentCt;
int NewindentCt;
StringBuffer line;
String previousWord;

static String systemEOL = System.getProperty("line.separator");

StringBuffer w = new StringBuffer(50);

String indent(String str) {
  charCt = 0;
  oldText = str;
  len = oldText.length();
  newText = new StringBuffer((int)(len*1.5));
  pos = 0;
  indentCt = 0;
  NewindentCt = 0;
  previousWord = "";
  line = new StringBuffer(100);
  while (pos < len) {
    char ch = oldText.charAt(pos);
    if (ch == '"')
     CopyString();
    else if (ch == '{')
     CopyComment();
    else if (Character.isLetter(ch) || ch == '_')
     CopyWord();
    else {
      if (ch == '\n' || ch == '\r') {
         putEOL();
         if (ch == '\r' && pos+1 < len && oldText.charAt(pos+1) == '\n')
            pos++;
         pos++;
         while (pos < len && (oldText.charAt(pos) == ' ' || oldText.charAt(pos) == '\t'))
            pos++;
      }
      else {
         PutCh(ch);
         pos++;
      }
    }
  }
  if (line.length() != 0)
    Put(line.toString());
  return newText.toString();
}
   
   
 final void Put (String s) {
   newText.append(s);
   charCt += s.length();
 }
 
 void PutCh (char ch) {
   line.append(ch);
 }
 
 void putEOL() {
     line.append(systemEOL);
     if (line.charAt(0) != '\r' && line.charAt(0) != '\n')
      for (int i = 0; i < indentCt; i++)
         Put(" ");
     Put(line.toString());
     line.setLength(0);
     indentCt = NewindentCt;
 }
 
 void CopyString() {
   do {
    char ch = oldText.charAt(pos);
    if (ch == '\r')
       putEOL();
    else if (ch == '\n') {
       putEOL();
       if (pos+1 < len && oldText.charAt(pos) == '\r')
          pos++;
    }
    else
       PutCh(oldText.charAt(pos));
    pos = pos + 1;
   } while (pos < len && oldText.charAt(pos) != '"');
   if (pos < len) {
     PutCh(oldText.charAt(pos));
     pos = pos + 1;
   }
 }

 void CopyComment() {
   int nesting = 0;
   do {
    char ch = oldText.charAt(pos);
    if (ch == '{') {
       PutCh('{');
       nesting ++;
    }
    else if (ch == '}') {
       PutCh('}');
       nesting --;
    }
    else if (ch == '\n')
       putEOL();
    else if (ch == '\r') {
       putEOL();
       if (pos+1 < len && oldText.charAt(pos+1) == '\n')
          pos++;
    }
    else
      PutCh(ch);
    pos = pos + 1;
   } while (pos < len && nesting > 0);
 }
 
 boolean NextWordIsIf() {
   int p = pos;
   while (p < len && !Character.isLetter(oldText.charAt(p))) {
    if (oldText.charAt(p) == '{') {
      int nesting = 0;
      do {
        if (oldText.charAt(p) == '{')
           nesting++;
        else if (oldText.charAt(p) == '}')
           nesting--;
        p = p + 1;
      } while (p < len && nesting > 0);
      if (p < len)
       p = p + 1;
    }
    else
     p = p + 1;
   }
   return (p + 1 < len) && 
          (oldText.charAt(p) == 'i'|| oldText.charAt(p) == 'I') &&
          (oldText.charAt(p + 1) == 'f' || oldText.charAt(p + 1) == 'F') &&
          ((p + 2 == len) || 
                  !(oldText.charAt(p + 2) == '_' || Character.isLetter(oldText.charAt(p+2)) || Character.isDigit(oldText.charAt(p+2))));
 }
  
 void CopyWord() {
   int i;
   w.setLength(0);
   do {
      w.append(oldText.charAt(pos));
      pos++;
   } while (pos < len && 
              (oldText.charAt(pos) == '_' ||
               Character.isLetter(oldText.charAt(pos)) ||
               Character.isDigit(oldText.charAt(pos))));
   if (w.length() > 11) {
     for (i = 0; i < w.length(); i++)
        PutCh(w.charAt(i));
     previousWord = w.toString();
     return;
   }
   String lcw = w.toString().toLowerCase();
   if (lcw.equals("end")) {
     if (line.length() == 0)
      indentCt = indentCt - 3;
     NewindentCt = NewindentCt - 3;
   }
   else if (lcw.equals("orif") || lcw.equals("else")
                 || (lcw.equals("or") && NextWordIsIf())) {
     if (line.length() == 0)
       indentCt = indentCt - 3;
   }
   else {
     if (lcw.equals("if") && (previousWord.equals("or") || previousWord.equals("exit"))) { }
     else if ((lcw.equals("sub") || lcw.equals("function")) && previousWord.equals("predeclare")) { }
     else if (lcw.equals("sub") || lcw.equals("if") || lcw.equals("loop") || 
                  lcw.equals("grab") || lcw.equals("function")) {
       if (!(previousWord.equals("end")))
         NewindentCt = NewindentCt + 3;
     }
     else if (lcw.equals("endsub") || lcw.equals("endif") || lcw.equals("endloop") || 
                     lcw.equals("endgrab") || lcw.equals("endfunction")) {
       if (line.length() == 0)
         indentCt = indentCt - 3;
       NewindentCt = NewindentCt - 3;
     }
   }
   previousWord = lcw;
   for (i = 0; i < w.length(); i++)
     PutCh(w.charAt(i));
 }
  
  
   

}