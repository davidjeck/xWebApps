
package tmcm.xTurtle;

class TToken {
   int position;
   String name;
   int kind;  // one of the token constants defined in TTokenizer, below
}

class TSymbolToken extends TToken {
   int symbolLocation;
}

class TNumberToken extends TToken {
   double value;
}

class TStringToken extends TToken {
   String str;
}

class TTokenizer {

   static final int
   
        Jump_ = 1,   // subs with zero params
        JumpToSubroutine_ = 2, 
        SubroutineSetup_ = 3, 
        FunctionSetup_ = 4, 
        AdjustSavedPC_ = 5, 
        SetStackRef_ = 6, 
        Reserve_ = 7 ,
        TellUser_ = 8, 
        DrawText_ = 9,
        HideTurtle_ = 10, 
        ShowTurtle_ = 11, 
        Clear_ = 12,
        Home_ = 13,
        PenUp_ = 14, 
        PenDown_ = 15, 
        Fill_ = 16, 
        Halt_ = 17, 
        Grab_ = 18, 
        endGrab_ = 19,
        return_ = 20, 
        returnFromFunction_ = 21,
        red_ = 22,
        blue_ = 23,
        green_ = 24,
        cyan_ = 25,
        magenta_ = 26,
        yellow_ = 27,
        gray_ = 28,
        lightGray_ = 29,
        darkGray_ = 30,
        black_ = 31,
        white_ = 32,
        Die_ = 33,
        
        Fork_ = 34,  // subs with one param
        JumpIf_ = 35, 
        AskUser_ = 36, 
        YesOrNo_ = 37, 
        Pop_ = 38, 
        PopRefParam_ = 39, 
        PopFunctionValue_ = 40,
        Forward_ = 41, 
        Back_ = 42, 
        Turn_ = 43, 
        Face_ = 44, 
        Circle_ = 45,

        Move_ = 46,  // subs with two params 
        MoveTo_ = 47, 
        Arc_ = 48,
        rgb_ = 49,  // with three params
        hsb_ = 50, 

        Push_ = 51, // functions with 0 params
        PushRefParam_ = 52, 
        PushAbsoluteReference_ = 53, 
        PushConstant_ = 54, 
        PushDummy_ = 55, 
        Random_ = 56, 
        
        turtleX_ = 57, 
        turtleY_ = 58, 
        turtleHeading_ = 59, 
        TurtleIsVisible_ = 60, 
        TurtleIsDrawing_ = 61,
        forkNumber_ = 62,

        sin_ = 63, // functions with one param
        cos_ = 64, 
        tan_ = 65, 
        sec_ = 66, 
        cot_ = 67, 
        csc_ = 68, 
        arctan_ = 69, 
        arcsin_ = 70, 
        arccos_ = 71, 
        not_ = 72,
        abs_ = 73, 
        sqrt_ = 74, 
        exp_ = 75, 
        ln_ = 76, 
        round_ = 77, 
        trunc_ = 78, 
        RandomInt_ = 79, 
        UnaryMinus_ = 80,

        plus_ = 81, // functions with two params
        minus_ = 82, 
        times_ = 83, 
        divide_ = 84, 
        power_ = 85,
        and_ = 86, 
        or_ = 87, 
        LT_ = 88, 
        GT_ = 89, 
        LE_ = 90, 
        GE_ = 91, 
        NE_ = 92, 
        EQ_ = 93, 

        leftParen_ = 94, // Other tokens, not commands 
        rightParen_ = 95, 
        colon_ = 96, 
        assign_ = 97, 
        comma_ = 98,
        if_ = 99, 
        then_ = 100, 
        else_ = 101, 
        elseif_ = 102, 
        endif_ = 103, 
        end_ = 104,
        loop_ = 105, 
        endloop_ = 106, 
        exit_ = 107, 
        exitif_ = 108, 
        unless_ = 109, 
        exitunless_ = 110,
        sub_ = 111, 
        endsub_ = 112,
        function_ = 113, 
        endfunction_ = 114, 
        ref_ = 115,
        declare_ = 116, 
        predeclare_ = 117, 
        import_ = 118,

        variableName_ = 119, // tokens with values 
        paramName_ = 120, 
        refParamName_ = 121, 
        subName_ = 122, 
        functionName_ = 123, 
        undeclaredName_ = 124, 
        number_ = 125, 
        string_ = 126, 

        endOfData_ = 127;


   String data;
   int pos;
   TToken NextTok;
   boolean tokenReady;
   
   TSymbolTable ST;
   
   private final static char endOfData = (char)0;
   
   TTokenizer(String theData, TSymbolTable symbolTable) {
      data = theData;
      pos = 0;
      NextTok = null;
      ST = symbolTable;
      tokenReady = false;
   }
   
   char NextCh() {
      if (pos >= data.length())
         return endOfData;
      else
         return data.charAt(pos);
   }
 
   char GetCh() {
      if (pos >= data.length())
         return endOfData;
      else {
         char ch = data.charAt(pos);
         pos++;
         return ch;
      }
   }

  void Skip() {
   int nesting = 0;
   int start = 0;
   char ch = NextCh();
   while (Character.isSpace(ch) || ch == '{') {
     pos++;
     if (ch == '{') {
       nesting = 1;
       start = pos;
       do {
          ch = GetCh();
          if (ch == '{')
             nesting++;
          else if (ch == '}')
             nesting--;
          else if (ch == endOfData)
             throw new TError("End of program occurred before matching '}' found for this comment.", start);
       } while (nesting != 0);
     }
     ch = NextCh();
   }
 }

 void ReadWord() {
   StringBuffer name = new StringBuffer();
   char ch = NextCh();
   while (Character.isLetter(ch) || Character.isDigit(ch) || ch == '_') {
     ch = GetCh();
     if (ch >='A' && ch <= 'Z')
       ch = Character.toLowerCase(ch);
     name.append(ch);
     ch = NextCh();
   }
   String tokname = name.toString();
   int loc = ST.findSymbol(tokname);
   if (loc == -1) {
     NextTok = new TToken();
     NextTok.name = tokname;
     NextTok.kind = undeclaredName_;
     return;
   }
   int kind = ST.get(loc).kind;
   if (kind == TSymbolTable.keyWordSymb) {
      NextTok = new TToken();
      NextTok.name = tokname;
      NextTok.kind = ((TKeyWordSym)ST.get(loc)).token;
      return;
   }
   NextTok = new TSymbolToken();
   NextTok.name = tokname;
   if (kind == TSymbolTable.variableSymb)
      NextTok.kind = variableName_;
   else if (kind == TSymbolTable.paramSymb)
      NextTok.kind = paramName_;
   else if (kind == TSymbolTable.subSymb)
      NextTok.kind = subName_;
   else if (kind == TSymbolTable.functionSymb)
      NextTok.kind = functionName_;
   else if (kind == TSymbolTable.refParamSymb)
      NextTok.kind = refParamName_;
   ((TSymbolToken)NextTok).symbolLocation = loc;
}


 void ReadString () {
   char ch = GetCh();
   StringBuffer str = new StringBuffer();
   ch = GetCh();
   boolean done = false;
   do {
    if (ch == '"' && NextCh() == '"')
     ch = GetCh();
    else if (ch == '"')
     done = true;
    if (!done)
     if (ch == '\n' || ch == '\r')
        throw new TError("Carriage return occured inside a string.  String must be closed before end-of-line.", pos);
     else if (ch == endOfData)
        throw new TError("End of data found in the middle of a string.", pos);
     else {
        str.append(ch);
        ch = GetCh();
     }
   } while (!done);
   NextTok = new TStringToken();
   NextTok.kind = string_;
   NextTok.name = "<a string>";
   ((TStringToken)NextTok).str = str.toString();
 }

void ReadNumber() {
   StringBuffer s = new StringBuffer();
   while (Character.isDigit(NextCh()))
      s.append(GetCh());
   if (NextCh() == '.') {
      s.append(GetCh());
      while (Character.isDigit(NextCh()))
        s.append(GetCh());
   }
   if (NextCh() == 'e' || NextCh() == 'E') {
     s.append(GetCh());
     if (NextCh() == '+' || NextCh() == '-') 
       s.append(GetCh());
     if (!Character.isDigit(NextCh()))
       throw new TError("Illegal number '" + s.toString() + "' encountered in program.", pos);
     while (Character.isDigit(NextCh()))
       s.append(GetCh());
   }
   Double d = null;
   try {
      d = new Double(s.toString());
   }
   catch (NumberFormatException e) {
      d = null;
   }
   if (d == null || d.isInfinite() || d.isNaN())
      throw new TError("Illegal number '" + s.toString() + "' encountered in program.", pos);
   NextTok = new TNumberToken();
   NextTok.kind = number_;
   NextTok.name = "<a constant>";
 //  int offset = AddConstant(d.doubleValue());
   ((TNumberToken)NextTok).value = d.doubleValue();
 }

 void ReadPunctuation() {
   char ch = GetCh();
   NextTok = new TToken();
   NextTok.name = "" + ch;
   switch (ch) {
    case '(': 
       NextTok.kind = leftParen_;
       break;
    case ')': 
       NextTok.kind = rightParen_;
       break;
    case ':': 
       if (NextCh() == '=') {
         NextTok.kind = assign_;
         ch = GetCh();
         NextTok.name = ":=";
       }
       else
         NextTok.kind = colon_;
       break;
    case ',': 
       NextTok.kind = comma_;
       break;
    case '+': 
       NextTok.kind = plus_;
       break;
    case '-': 
       NextTok.kind = minus_;
       break;
    case '*': 
       NextTok.kind = times_;
       break;
    case '/': 
       NextTok.kind = divide_;
       break;
    case '^': 
       NextTok.kind = power_;
       break;
    case '>':
       ch = NextCh();
       if (ch == '=' || ch == '>' || ch == '<') {
          ch = GetCh();
          if (ch == '=')
             NextTok.kind = GE_;
          else if (ch == '<')
             NextTok.kind = NE_;
          else
             throw new TError(">> is not a legal operator.",pos);
          NextTok.name = NextTok.name + ch;
       }
       else
          NextTok.kind = GT_;    
       break;
    case '<':
       ch = NextCh();
       if (ch == '=' || ch == '>' || ch == '<') {
          ch = GetCh();
          if (ch == '=')
             NextTok.kind = LE_;
          else if (ch == '>')
             NextTok.kind = NE_;
          else
             throw new TError("<< is not a legal operator.",pos);
          NextTok.name = NextTok.name + ch;
       }
       else
          NextTok.kind = LT_;    
       break;
    case '=':
       ch = NextCh();
       if (ch == '=' || ch == '>' || ch == '<') {
          ch = GetCh();
          if (ch == '<')
             NextTok.kind = LE_;
          else if (ch == '>')
             NextTok.kind = GE_;
          else
             throw new TError("== is not a legal operator.", pos);
          NextTok.name = NextTok.name + ch;
       }
       else
          NextTok.kind = EQ_;    
       break;
    case '&': 
       NextTok.kind = and_;
       break;
    case '|': 
       NextTok.kind = or_;
       break;
    case '~': 
       NextTok.kind = not_;
    default:
       throw new TError("Illegal character '" + ch + "' found in input.", pos);
   }
 }

 void ReadToken() {
   Skip();
   char ch = NextCh();
   if (ch == endOfData) {
      NextTok = new TToken();
      NextTok.kind = endOfData_;
   }
   else if (Character.isLetter(ch) || ch == '_') {
     ReadWord();
     if (NextTok.kind == end_) {
       Skip();
       if (Character.isLetter(NextCh())) {
         ReadWord();
         if (NextTok.kind == if_) {
            NextTok.kind = endif_;
            NextTok.name = "ENDIF";
         }
         else if (NextTok.kind == loop_) {
            NextTok.kind = endloop_;
            NextTok.name = "ENDLOOP";
         }
         else if (NextTok.kind == sub_) {
            NextTok.kind = endsub_;
            NextTok.name = "ENDSUB";
         }
         else if (NextTok.kind == function_) {
            NextTok.kind = endfunction_;
            NextTok.name = "ENDFUNCTION";
         }
         else if (NextTok.kind == Grab_) {
            NextTok.kind = endGrab_;
            NextTok.name = "ENDGRAB";
         }
         else
            throw new TError("The word 'END' can only be used in combinations like 'END IF' and 'END SUB'.", pos);
       }
       else
          throw new TError("The word 'END' can only be used in combinations like 'END IF' and 'END SUB'.", pos);
     }
     else if (NextTok.kind == exit_) {
        Skip();
        int savePos = pos;
        if (Character.isLetter(NextCh())) {
           ReadWord();
           if (NextTok.kind == if_) {
              NextTok.kind = exitif_;
              NextTok.name = "EXITIF";
           }
           else if (NextTok.kind == unless_) {
              NextTok.kind = exitunless_;
              NextTok.name = "EXITUNLESS";
          }
          else {
             NextTok.kind = exit_;
             NextTok.name = "EXIT";
             pos = savePos;
          }
       }
       else
          pos = savePos;
     }
     else if (NextTok.kind == or_) {
       int savePos = pos;
       Skip();
       if (Character.isLetter(NextCh())) {
          ReadWord();
          if (NextTok.kind == if_) {
             NextTok.kind = elseif_;
             NextTok.name = "ORIF";
          }
          else {
             NextTok.kind = or_;
             NextTok.name = "OR";
             pos = savePos;
          }
       }
       else
          pos = savePos;
     }
   }
   else if (Character.isDigit(ch) || ch == '.')
      ReadNumber();
   else if (ch == '"') 
      ReadString();
   else 
      ReadPunctuation();
   NextTok.position = pos;
   tokenReady = true;
 }

 TToken LookToken() {
   if (!tokenReady)
     ReadToken();
   return NextTok;
 }

 TToken GetToken() {
   TToken tok = LookToken();
   tokenReady = false;
   return tok;
 }
   
}