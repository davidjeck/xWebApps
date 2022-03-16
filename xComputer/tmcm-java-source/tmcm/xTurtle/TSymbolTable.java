
package tmcm.xTurtle;

import java.util.*;

abstract class TSymbol {
    String name;
    int kind;   // one of the symbol kinds
}

class TKeyWordSym extends TSymbol {
   int token;
}

class TVarSym extends TSymbol {
   int offset;
   int grabNum;  // for variables
   int trueSymbolLoc;  // for imported variables
}

class TSubSym extends TSymbol {
   int start;
   int paramCount; // 0 to 30
   BitSet paramTypes = new BitSet(32);
}


class TSymbolTable {

   static final int            // symbol kinds
      undefinedSymb = 0, 
      keyWordSymb = 1, 
      subSymb = 2, 
      functionSymb = 3, 
      variableSymb = 4, 
      paramSymb = 5, 
      refParamSymb = 6;
      
   Vector symbols = new Vector(100);
   
   int symbolSearchStart = 0;
   
   TSymbolTable() {
      Add("red",TTokenizer.red_);
      Add("green",TTokenizer.green_);
      Add("blue",TTokenizer.blue_);
      Add("cyan",TTokenizer.cyan_);
      Add("magenta",TTokenizer.magenta_);
      Add("yellow",TTokenizer.yellow_);
      Add("black",TTokenizer.black_);
      Add("white",TTokenizer.white_);
      Add("gray",TTokenizer.gray_);
      Add("lightgray",TTokenizer.lightGray_);
      Add("darkgray",TTokenizer.darkGray_);
      Add("rgb",TTokenizer.rgb_);
      Add("hsb",TTokenizer.hsb_);
	  Add("hideturtle",TTokenizer.HideTurtle_);
	  Add("showturtle",TTokenizer.ShowTurtle_);
	  Add("clear",TTokenizer.Clear_);
	  Add("home",TTokenizer.Home_);
	  Add("penup",TTokenizer.PenUp_);
	  Add("pendown",TTokenizer.PenDown_);
	//  Add("fill",TTokenizer.Fill_);   Fill is not supported!
	  Add("telluser",TTokenizer.TellUser_);
	  Add("askuser",TTokenizer.AskUser_);
	  Add("yesorno",TTokenizer.YesOrNo_);
	  Add("drawtext",TTokenizer.DrawText_);
	  Add("fork",TTokenizer.Fork_);
	  Add("halt",TTokenizer.Halt_);
	  Add("killprocess",TTokenizer.Die_);
	  Add("forward",TTokenizer.Forward_);
	  Add("back",TTokenizer.Back_);
	  Add("turn",TTokenizer.Turn_);
	  Add("face",TTokenizer.Face_);
	  Add("circle",TTokenizer.Circle_);
	  Add("move",TTokenizer.Move_);
	  Add("moveto",TTokenizer.MoveTo_);
	  Add("arc",TTokenizer.Arc_);
	  Add("random",TTokenizer.Random_);
	  Add("xcoord",TTokenizer.turtleX_);
	  Add("ycoord",TTokenizer.turtleY_);
	  Add("heading",TTokenizer.turtleHeading_);
	  Add("isvisible",TTokenizer.TurtleIsVisible_);
	  Add("isdrawing",TTokenizer.TurtleIsDrawing_);
	  Add("forknumber",TTokenizer.forkNumber_);
	  Add("sin",TTokenizer.sin_);
	  Add("cos",TTokenizer.cos_);
	  Add("tan",TTokenizer.tan_);
	  Add("sec",TTokenizer.sec_);
	  Add("cot",TTokenizer.cot_);
	  Add("csc",TTokenizer.csc_);
	  Add("arctan",TTokenizer.arctan_);
	  Add("arcsin",TTokenizer.arcsin_);
	  Add("arccos",TTokenizer.arccos_);
	  Add("abs",TTokenizer.abs_);
	  Add("sqrt",TTokenizer.sqrt_);
	  Add("exp",TTokenizer.exp_);
	  Add("ln",TTokenizer.ln_);
	  Add("round",TTokenizer.round_);
	  Add("trunc",TTokenizer.trunc_);
	  Add("randomint",TTokenizer.RandomInt_);
	  Add("and",TTokenizer.and_);
	  Add("or",TTokenizer.or_);
	  Add("not",TTokenizer.not_);
	  Add("if",TTokenizer.if_);
	  Add("then",TTokenizer.then_);
	  Add("else",TTokenizer.else_);
	  Add("orif",TTokenizer.elseif_);
	  Add("endif",TTokenizer.endif_);
	  Add("end",TTokenizer.end_);
	  Add("loop",TTokenizer.loop_);
	  Add("endloop",TTokenizer.endloop_);
	  Add("exit",TTokenizer.exit_);
	  Add("exitif",TTokenizer.exitif_);
	  Add("exitunless",TTokenizer.exitunless_);
	  Add("unless",TTokenizer.unless_);
	  Add("sub",TTokenizer.sub_);
	  Add("endsub",TTokenizer.endsub_);
	  Add("function",TTokenizer.function_);
	  Add("endfunction",TTokenizer.endfunction_);
	  Add("return",TTokenizer.return_);
	  Add("ref",TTokenizer.ref_);
	  Add("declare",TTokenizer.declare_);
	  Add("predeclare",TTokenizer.predeclare_);
	  Add("import",TTokenizer.import_);
	  Add("grab",TTokenizer.Grab_);
	  Add("endgrab",TTokenizer.endGrab_);
   }
   
   private void Add(String name, int token) {
      TKeyWordSym sym = new TKeyWordSym();
      sym.name = name;
      sym.kind = keyWordSymb;
      sym.token = token;
      symbols.addElement(sym);
   }
   
   int addSymbol (int kind, String name) {
       int k;  // symbol kind
       int loc, newsize;
       TSymbol sym;
       switch (kind) {
          case keyWordSymb:
             sym = new TKeyWordSym();
             break;
          case subSymb:
          case functionSymb:
             sym = new TSubSym();
             break;
          case variableSymb:
          case paramSymb:
          case refParamSymb:
             sym = new TVarSym();
             ((TVarSym)sym).grabNum = -1;
             ((TVarSym)sym).trueSymbolLoc = symbols.size();
             break;
          default:
             sym = null;
       }
       sym.name = name;
       sym.kind = kind;
       symbols.addElement(sym);
       return symbols.size() - 1;
   }
       
   TSymbol get(int loc) {
      return (TSymbol)symbols.elementAt(loc);
   }


   void  setSubroutineData (int loc,
          int start,
          int paramCount,
          BitSet paramTypes) {
       TSubSym sym = (TSubSym)symbols.elementAt(loc);
       sym.start = start;
       sym.paramCount = paramCount;
       sym.paramTypes = paramTypes;
   }

   void setVariableData (int loc,
          int offset) {
       TVarSym sym = (TVarSym)symbols.elementAt(loc);
       sym.offset = offset;
   }


   int findSymbol (String name) {
       for (int i = symbols.size() - 1; i >= 0; i--) {
          TSymbol sym = (TSymbol)symbols.elementAt(i);
          if (sym.name.equals(name) && (i >= symbolSearchStart || sym.kind == keyWordSymb || sym.kind == subSymb || sym.kind == functionSymb)) {
             return i;
          }
       }
       return -1;
   }

   int findGlobalSymbol (String name) {
      for (int i = 0; i < symbols.size(); i++) {
          TSymbol sym = (TSymbol)symbols.elementAt(i);
          if (sym.name.equals(name)) {
             return i;
          }
      }
      return -1;
   }


   void startSubroutineSymbols() {
      symbolSearchStart = symbols.size();
   }

   void endSubroutineSymbols() {
      symbols.setSize(symbolSearchStart);
      symbolSearchStart = 0;
   }

   int checkPredeclarations() {
      for (int i = 0; i < symbols.size(); i++) {
         TSymbol sym = (TSymbol)symbols.elementAt(i);
         if ( (sym.kind == subSymb || sym.kind == functionSymb) && (((TSubSym)sym).paramTypes).get(31) )
            return i;
      }
      return -1;
   }

} // end of class TSymbolTable

