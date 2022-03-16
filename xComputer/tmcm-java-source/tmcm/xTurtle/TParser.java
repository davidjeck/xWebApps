
package tmcm.xTurtle;

import java.util.*;

class TParser {  // retains no state between parse() and parseAppend();

  private TTokenizer tokenizer;
  private TProgram prog;
  
  private TSymbolTable ST;
  private int saveGlobalOffsetCt;
  private int saveSymbolCt;
  private int saveInsCt;

  
  private boolean parsingSubroutine, parsingFunction, returnFound;
  private int offsetCt;
  private int currentLoopLoc;
  private int grabBeingParsed;

  
  TProgram parse(String str, TSymbolTable ST) {
     if (str == null || str.trim().equals(""))
        throw new TError("Data is empty; no program available for compilation.",-1);
     this.ST = ST;
     tokenizer = new TTokenizer(str,ST);
     prog = new TProgram( (str.length() < 400)? 100 : str.length()/4 );
     doParse();
     TProgram temp = prog;
     prog = null;
     tokenizer = null;
     return temp;
  }
  
  void parseAppend(String str, TSymbolTable ST, TProgram prog) {
     if (str == null || str.trim().equals(""))
        throw new TError("Data is empty; no program available for compilation.",-1);
     saveInsCt = prog.insCt;
     tokenizer = new TTokenizer(str,ST);
     this.prog = prog;
     this.ST = ST;
     doParse();
     tokenizer = null;
     prog = null;
     ST = null;
  }
  
  private void Error(String message) {
     ST.symbols.setSize(saveSymbolCt);
     prog.globalOffsetCt = saveGlobalOffsetCt;
     prog.insCt = saveInsCt;
     int pos = tokenizer.pos;
     tokenizer = null;
     prog = null;
     ST = null;
     throw new TError(message, pos);
  }
  
  private void doParse() {
      TToken tok;
      int i;
      int undeclared;
      saveGlobalOffsetCt = prog.globalOffsetCt;
      saveSymbolCt = ST.symbols.size();
      ST.symbolSearchStart = 0;
	  currentLoopLoc = -1;
	  parsingSubroutine = false;
	  parsingFunction = false;
	  grabBeingParsed = 0;
//	  if (saveGlobalOffsetCt > 0)
//    	   prog.addInstruction(TTokenizer.Reserve_, saveGlobalOffsetCt, tokenizer.pos);
	  do {
	   tok = tokenizer.LookToken();
	   if (tok.kind == TTokenizer.sub_ || tok.kind == TTokenizer.function_)
 	    ParseSubroutine();
	   else if (tok.kind == TTokenizer.endloop_ ||
	            tok.kind ==  TTokenizer.endif_ ||
	            tok.kind ==  TTokenizer.endsub_ ||
	            tok.kind ==  TTokenizer.endfunction_ ||
	            tok.kind ==  TTokenizer.else_ || 
	            tok.kind == TTokenizer.elseif_)
	    Error("Expecting a statement or declaration.  (CheckNesting)");
	   else if (tok.kind == TTokenizer.predeclare_)
	    ParseForwardDeclaration();
	   else if (tok.kind != TTokenizer.endOfData_)
	    ParseStatement();
	  } while (tok.kind != TTokenizer.endOfData_);
	  prog.addInstruction(TTokenizer.Die_, 0, tokenizer.pos);
	  undeclared = ST.checkPredeclarations();
	  if (undeclared != -1) {
	   TSymbol sym = (TSymbol)ST.symbols.elementAt(undeclared);
	   if (sym.kind == TSymbolTable.subSymb)
	    Error("The predeclared subroutine, '" + sym.name+ "', has never actually been declared.");
	   else
	    Error("The predeclared function, '" + sym.name+ "', has never actually been declared.");
	  }
	  prog.appendStart = saveInsCt;
  }
  
  private void ParseDeclaration() {
	   TToken firstTok, tok;
	   boolean done;
	   int loc, offset;
	   int skind; // SymbolKinds
	   String name;
	   int trueSymbolLoc;
	   if (grabBeingParsed > 0)
 	     Error("Declarations cannot occur inside GRAB statements.");
	   firstTok = tokenizer.GetToken();
	   if (firstTok.kind == TTokenizer.import_ && !(parsingSubroutine || parsingFunction))
	    Error("An import statement can only occur in a subroutine or function definition.");
	   done = false;
	   do {
	    tok = tokenizer.GetToken();
	    if (tok.kind == TTokenizer.endOfData_)
	     Error("Enexpected end of data while reading the list of items for a declaration statemet.");
	    else if (tok.kind == TTokenizer.string_ || tok.kind == TTokenizer.number_ || (tok.kind >= TTokenizer.leftParen_ && tok.kind <= TTokenizer.comma_))
	     Error("An unexpected illegal item '" + tok.name + "' found in list of items being declared.");
	    else if (tok.kind < TTokenizer.variableName_)
	     Error("'" +  tok.name + "' is a reserved word.  A reserved word can't be redefined.");
	    else if ((tok.kind == TTokenizer.subName_ || tok.kind == TTokenizer.functionName_) && !(parsingSubroutine || parsingFunction))
	     Error("The identifier '" + tok.name + "' has already been defined as a subroutine or function name.");
	    else if ((tok.kind == TTokenizer.subName_ || tok.kind == TTokenizer.functionName_) && (firstTok.kind == TTokenizer.import_))
	     Error("Subroutines and functions do not need to be imported to be used.");
	    else if (tok.kind == TTokenizer.paramName_ || tok.kind == TTokenizer.variableName_ || tok.kind == TTokenizer.refParamName_) {
	       if (parsingSubroutine || parsingFunction)
	         Error("The Identifier '" + tok.name + "' is already defined in this subroutine.");
	       else
	         Error("The Identifier '" + tok.name + "' is already defined.");
	    }
	    else if (tok.kind == TTokenizer.endOfData_)
	     Error("Unexpected end of data while reading the list of items for a declaration statement.");
	    else if (!(tok.kind == TTokenizer.undeclaredName_ || tok.kind == TTokenizer.subName_ || tok.kind == TTokenizer.functionName_))
	     Error("An unexpected illegal item '" + tok.name + "' found in list of items being declared.");
	    if (firstTok.kind == TTokenizer.declare_) {
		      if (parsingSubroutine || parsingFunction) {
		       offsetCt++;
		       offset = offsetCt;
		      }
		      else {
		       offset = -prog.globalOffsetCt;
		       prog.globalOffsetCt++;
		      }
		      if (tok.kind == TTokenizer.undeclaredName_) 
		       loc = ST.addSymbol(TSymbolTable.variableSymb, tok.name);
		      else {
		       loc = ((TSymbolToken)tok).symbolLocation;
		       name = ((TSymbol)ST.symbols.elementAt(loc)).name;
		       loc = ST.addSymbol(TSymbolTable.variableSymb, name);
		      }
		      ST.setVariableData(loc, offset);
		      prog.addInstruction(TTokenizer.PushDummy_, 0, tokenizer.pos);
	    }
	    else {
	      loc = ST.findGlobalSymbol(tok.name);
	      if (loc == -1) 
	        Error("'" + tok.name + "' cannot be imported because it is undefined.  Only global variables can be imported.");
	      skind = ST.get(loc).kind;
	      if (skind != TSymbolTable.variableSymb)
	       Error("The identifier, '" + tok.name + "', is not a global variable.  Only global variables can be imported.");
	      offset = ((TVarSym)ST.get(loc)).offset;
	      trueSymbolLoc = loc;
	      loc = ST.addSymbol(TSymbolTable.variableSymb, tok.name);
	      ST.setVariableData(loc, offset);
	      ((TVarSym)ST.get(loc)).trueSymbolLoc = trueSymbolLoc;
	    }
	    tok = tokenizer.LookToken();
	    if (tok.kind != TTokenizer.comma_)
	     done = true;
	    else
	     tok = tokenizer.GetToken();
	   } while (!done);
  }


  private void ParseFunctionCall (int symbLoc) {
    int paramCt;
    BitSet paramTypes;
    String name;
    prog.addInstruction(TTokenizer.FunctionSetup_, 0, tokenizer.pos);
    paramCt = ((TSubSym)ST.get(symbLoc)).paramCount;
    paramTypes = ((TSubSym)ST.get(symbLoc)).paramTypes;
    name = ST.get(symbLoc).name;
    ParseActualParams(paramCt, paramTypes, name);
    prog.addInstruction(TTokenizer.AdjustSavedPC_, paramCt, tokenizer.pos);
    prog.addInstruction(TTokenizer.JumpToSubroutine_, ((TSubSym)ST.get(symbLoc)).start, tokenizer.pos);
  }

  private boolean ParsePrimary() {
    boolean logical;
    TToken tok;
    tok = tokenizer.GetToken();
    logical = false;
    switch (tok.kind) {
     case TTokenizer.leftParen_:
       logical = ParseExpression();
       tok = tokenizer.GetToken();
       if (tok.kind != TTokenizer.rightParen_)
         Error("Found '" +  tok.name + "' while expecting right parenthesis to match previous left parenthesis.");
       break;
     case TTokenizer.functionName_: 
       ParseFunctionCall(((TSymbolToken)tok).symbolLocation);
       break;
     case TTokenizer.variableName_:
     case TTokenizer.paramName_: 
       prog.addInstruction(TTokenizer.Push_, ((TVarSym)ST.get(((TSymbolToken)tok).symbolLocation)).offset, tok.position);
       break;
     case TTokenizer.refParamName_: 
       prog.addInstruction(TTokenizer.PushRefParam_, ((TVarSym)ST.get(((TSymbolToken)tok).symbolLocation)).offset, tok.position);
       break;
     case TTokenizer.number_:
       prog.addInstruction(TTokenizer.PushConstant_, prog.addConstant(((TNumberToken)tok).value), tok.position);
       break;
     case TTokenizer.Random_:
     case TTokenizer.turtleX_:
     case TTokenizer.turtleY_:
     case TTokenizer.turtleHeading_:
     case TTokenizer.TurtleIsVisible_:
     case TTokenizer.TurtleIsDrawing_:
     case TTokenizer.forkNumber_:
       ParseActualParams(0, null, tok.name);
       prog.addInstruction(tok.kind, 0, tok.position);
       break;
     case TTokenizer.sin_:
     case TTokenizer.cos_:
     case TTokenizer.tan_:
     case TTokenizer.sec_:
     case TTokenizer.cot_:
     case TTokenizer.csc_:
     case TTokenizer.arctan_:
     case TTokenizer.arcsin_:
     case TTokenizer.arccos_:
     case TTokenizer.abs_:
     case TTokenizer.sqrt_:
     case TTokenizer.exp_:
     case TTokenizer.ln_:
     case TTokenizer.round_:
     case TTokenizer.trunc_:
     case TTokenizer.RandomInt_:
       ParseActualParams(1, null, tok.name);
       prog.addInstruction(tok.kind, 0, tok.position);
       break;
     case TTokenizer.subName_: 
       Error("Subroutine '" + tok.name + "' found while reading an expression.  (Subroutine cannot be used like a function or variable.)");
       break;
     case TTokenizer.undeclaredName_: 
       Error("Undeclared identifier '" + tok.name + "' encountered in an expression.");
       break;
     case TTokenizer.endOfData_: 
       Error("Unexpected end of data encountered while reading an expression.");
       break;
     default:
       Error("Unexpected item '" + tok.name + "' found while reading an expression.");
    }
    return logical;
  }

  private boolean ParseFactor (){
    TToken tok;
    boolean log2,logical;
    logical = ParsePrimary();
    tok = tokenizer.LookToken();
    while (tok.kind == TTokenizer.power_) {
      if (logical)
       Error("Power operator cannot be applied to logical values.");
      tok = tokenizer.GetToken();
      log2 = ParsePrimary();
      if (log2)
       Error("Power operator  cannot be applied to logical values.");
      prog.addInstruction(tok.kind, 0, tok.position);
      tok = tokenizer.LookToken();
    }
    return logical;
  }

  private boolean ParseTerm () {
    TToken tok;
    boolean log2,logical;
    logical = ParseFactor();
    tok = tokenizer.LookToken();
    while (tok.kind == TTokenizer.times_ || tok.kind == TTokenizer.divide_) {
      if (logical)
       Error("Operation cannot be applied to logical values.");
      tok = tokenizer.GetToken();
      log2 = ParseFactor();
      if (log2)
       Error("Operation cannot be applied to logical values.");
      prog.addInstruction(tok.kind, 0, tok.position);
      tok = tokenizer.LookToken();
    }
    return logical;
  }

  private boolean ParseExp () {
    TToken tok;
    boolean leadingMinus;
    boolean log2, logical;
    tok = tokenizer.LookToken();
    leadingMinus = (tok.kind == TTokenizer.minus_);
    if (tok.kind == TTokenizer.minus_ || tok.kind == TTokenizer.plus_)
     tok = tokenizer.GetToken();
    logical = ParseTerm();
    if (leadingMinus) {
      if (logical)
       Error("A minus sign cannot be applied to a logical value.");
      prog.addInstruction(TTokenizer.UnaryMinus_, 0, tok.position);
    }
    tok = tokenizer.LookToken();
    while (tok.kind == TTokenizer.minus_ || tok.kind == TTokenizer.plus_) {
      if (logical) 
       Error("Operation cannot be applied to logical values.");
      tok = tokenizer.GetToken();
      log2 = ParseTerm();
      if (log2) 
       Error("Operation cannot be applied to logical values.");
      prog.addInstruction(tok.kind, 0, tok.position);
      tok = tokenizer.LookToken();
    }
    return logical;
  }

  private boolean ParseComparison () {
    TToken tok;
    boolean log2, logical;
    logical = ParseExp();
    tok = tokenizer.LookToken();
    if (tok.kind >= TTokenizer.LT_ && tok.kind <=TTokenizer.EQ_) {
      if (logical)
       Error("Comparison operator cannot be applied to a logical value.");
      tok = tokenizer.GetToken();
      log2 = ParseExp();
      if (log2)
       Error("Comparison operator cannot be applied to a logical value.");
      prog.addInstruction(tok.kind, 0, tok.position);
      tok = tokenizer.LookToken();
      if (tok.kind >= TTokenizer.LT_ && tok.kind <=TTokenizer.EQ_)
       Error("Sequences of comparison operators are not allowed.");
      logical = true;
    }
    return logical;
  }

  private boolean ParseLFactor () {
    int notCt;
    TToken tok;
    boolean logical;
    notCt = 0;
    do {
     tok = tokenizer.LookToken();
     if (tok.kind == TTokenizer.not_) {
       notCt++;
       tok = tokenizer.GetToken();
     }
    } while (tok.kind == TTokenizer.not_);
    logical = ParseComparison();
    if (notCt > 0) {
      if (!logical) 
        Error("The operator NOT can only be applied to logical values.");
      if (notCt % 2 == 1)
        prog.addInstruction(TTokenizer.not_, 0, tok.position);
    }
    return logical;
  }

  private boolean ParseLTerm () {
    TToken tok;
    boolean log2,logical;
    logical = ParseLFactor();
    tok = tokenizer.LookToken();
    while (tok.kind == TTokenizer.and_) {
      tok = tokenizer.GetToken();
      if (!logical)
       Error("The operator AND can only be applied to logical values.");
      log2 = ParseLTerm();
      if (!log2)
       Error("The operator AND can only be applied to logical values.");
      prog.addInstruction(TTokenizer.and_, 0, tok.position);
      tok = tokenizer.LookToken();
    }
    return logical;
  }

 private boolean ParseExpression () {
   TToken tok;
   boolean logical,log2;
   logical = ParseLTerm();
   tok = tokenizer.LookToken();
   while (tok.kind == TTokenizer.or_) {
     tok = tokenizer.GetToken();
     if (!logical)
      Error("The operator OR can only be applied to logical values.");
     log2 = ParseLTerm();
     if (!log2)
      Error("The operator OR can only be applied to logical values.");
     prog.addInstruction(TTokenizer.or_, 0, tok.position);
     tok = tokenizer.LookToken();
   }
   return logical;
 }

 private void ParseComputation() {
   boolean logical;
   logical = ParseExpression();
   if (logical)
    Error("Logical value not allowed here; numeric value or expression required.");
 }

 private void ParseCondition() {
   boolean logical;
   logical = ParseExpression();
   if (!logical)
    Error("Numeric value not allowed here; logical value or expression required.");
 }

 private void ParseAssignmentStatement() {
   TToken tok, asg;
   tok = tokenizer.GetToken();
   asg = tokenizer.GetToken();
   if (asg.kind != TTokenizer.assign_) {
     if (asg.kind == TTokenizer.leftParen_)
      Error("'" + tok.name + "' is a variable.  It looks like you are trying to use it as a subroutine.");
     else
      Error("Expected the assignment operator, :=, after variable name '" + tok.name + "'");
   }
   ParseComputation();
   if (tok.kind == TTokenizer.refParamName_)
     prog.addInstruction(TTokenizer.PopRefParam_, ((TVarSym)ST.get(((TSymbolToken)tok).symbolLocation)).offset, tok.position);
   else
     prog.addInstruction(TTokenizer.Pop_, ((TVarSym)ST.get(((TSymbolToken)tok).symbolLocation)).offset, tok.position);
 }

 private void ParseSubroutineCall() {
   TToken tok;
   int paramCt;
   BitSet paramTypes;
   String name;
   tok = tokenizer.GetToken();
   prog.addInstruction(TTokenizer.SubroutineSetup_, 0, tokenizer.pos);
   TSubSym sym = ((TSubSym)ST.get(((TSymbolToken)tok).symbolLocation));
   paramCt = sym.paramCount;
   paramTypes = sym.paramTypes;
   name = sym.name;
   ParseActualParams(paramCt, paramTypes, name);
   prog.addInstruction(TTokenizer.AdjustSavedPC_, paramCt, tokenizer.pos);
   prog.addInstruction(TTokenizer.JumpToSubroutine_, sym.start, tokenizer.pos);
 }

 private void ParseIfStatement() {
   TToken tok;
   int firstJump, prevJump, runner;
   int prevJumpIf;
   tok = tokenizer.GetToken();
   prevJump = -1;
   firstJump = -1;
   prevJumpIf = -1;
   do {
    ParseCondition();
    tok = tokenizer.GetToken();
    if (tok.kind != TTokenizer.then_)
     Error("Expected 'THEN' here as part of an IF statemen.");
    prog.addInstruction(TTokenizer.not_, 0, tokenizer.pos);
    prevJumpIf = prog.insCt;
    prog.addInstruction(TTokenizer.JumpIf_, 0, tokenizer.pos);
    do {
     tok = tokenizer.LookToken();
     if (tok.kind == TTokenizer.endOfData_)
      Error("End of program encountered in the middle of an IF statement.");
     else if (tok.kind == TTokenizer.declare_ || tok.kind == TTokenizer.import_)
      Error("Declarations cannot occur inside IF statements; check for missing END IF.");
     else if (!(tok.kind == TTokenizer.else_ || tok.kind == TTokenizer.elseif_ || tok.kind == TTokenizer.endif_))
      ParseStatement();
    }while (!(tok.kind == TTokenizer.else_ || tok.kind == TTokenizer.elseif_ || tok.kind == TTokenizer.endif_));
    tok = tokenizer.GetToken();
    if (tok.kind == TTokenizer.elseif_ || tok.kind == TTokenizer.else_) {
      if (firstJump == -1)
       firstJump = prog.insCt;
      else
       prog.data[prevJump] = prog.insCt;
      prevJump = prog.insCt;
      prog.addInstruction(TTokenizer.Jump_, 0, tokenizer.pos);
      prog.data[prevJumpIf] = prog.insCt;
      prevJumpIf = -1;
    }
   } while (!(tok.kind == TTokenizer.endif_ || tok.kind == TTokenizer.else_));
   if (tok.kind == TTokenizer.else_) {
     do {
      tok = tokenizer.LookToken();
      if (tok.kind == TTokenizer.endOfData_)
       Error("No matching END IF for IF statement;  end of program encountered inside an IF statement.");
      else if (tok.kind == TTokenizer.elseif_)
       Error("In an IF statement, an OR IF clause cannot follow an ELSE clause.");
      else if (tok.kind == TTokenizer.declare_ || tok.kind == TTokenizer.import_) 
       Error("Declarations cannot occur inside IF statements; check for missing END IF.");
      else if (tok.kind != TTokenizer.endif_)
       ParseStatement();
     } while (tok.kind != TTokenizer.endif_);
     tok = tokenizer.GetToken();
   }
   if (prevJumpIf != -1)
     prog.data[prevJumpIf] = prog.insCt;
   if (firstJump != -1) {
     runner = firstJump;
     do {
      firstJump = prog.data[runner];
      prog.data[runner] = prog.insCt;
      runner = firstJump;
     } while (runner != 0);
   }
 }

 private void ParseLoopStatement() {
   int i, saveLoopLoc;
   TToken tok;
   boolean exitexists;
   saveLoopLoc = currentLoopLoc;
   currentLoopLoc = prog.insCt;
   tok = tokenizer.GetToken();
   do {
    tok = tokenizer.LookToken();
    if (tok.kind == TTokenizer.endOfData_)
      Error("Encountered end of program in the middle of a LOOP.");
    else if (tok.kind  == TTokenizer.declare_ || tok.kind == TTokenizer.import_)
     Error("Declarations cannot occur inside of loops.");
    else if (tok.kind != TTokenizer.endloop_)
      ParseStatement();
   } while (tok.kind != TTokenizer.endloop_);
   tok = tokenizer.GetToken();
   prog.addInstruction(TTokenizer.Jump_, currentLoopLoc, tokenizer.pos);
   exitexists = false;
   for (i = currentLoopLoc; i < prog.insCt; i++)
    if ((prog.ins[i] == TTokenizer.Jump_ || prog.ins[i] == TTokenizer.JumpIf_) && prog.data[i] == -currentLoopLoc - 1) {
      prog.data[i] = prog.insCt;
      exitexists = true;
    }
    else if (prog.ins[i] == TTokenizer.Halt_ || prog.ins[i] == TTokenizer.Die_ || prog.ins[i] == TTokenizer.return_)
      exitexists = true;
   if (!exitexists)
     Error("No way is provided to ever exit from LOOP.");
   currentLoopLoc = saveLoopLoc;
 }

 private void ParseExitStatement() {
   TToken tok;
   if (currentLoopLoc == -1)
     Error("EXIT statements can only occur inside loops.");
   if (grabBeingParsed > 1)
     Error("An EXIT statement cannnot occur inside a GRAB statement,  except in the ELSE part.");
   tok = tokenizer.GetToken();
   if (tok.kind != TTokenizer.exit_)
     ParseCondition();
   if (tok.kind == TTokenizer.exitunless_)
      prog.addInstruction(TTokenizer.not_, 0, tok.position);
   if (tok.kind == TTokenizer.exit_)
      prog.addInstruction(TTokenizer.Jump_, -currentLoopLoc - 1, tok.position);
   else
      prog.addInstruction(TTokenizer.JumpIf_, -currentLoopLoc - 1, tok.position);
 }

 private void ParseReturnStatement() {
   TToken tok;
   if (!(parsingSubroutine || parsingFunction))
      Error("A RETURN statement can occur only in a subroutine or function definition.");
   if (grabBeingParsed > 1)
      Error("A RETURN statement cannot occur inside a GRAB statement, except in the ELSE part.");
   tok = tokenizer.GetToken();
   if (parsingFunction) {
       ParseComputation();
       prog.addInstruction(TTokenizer.PopFunctionValue_, 0, tokenizer.pos);
       prog.addInstruction(TTokenizer.returnFromFunction_, offsetCt, tok.position);
       returnFound = true;
   }
   else
    prog.addInstruction(TTokenizer.return_, offsetCt, tok.position);
 }

 private void ParseGrab() {
   TToken tok, varTok;
   int num, jumpLoc;
   boolean elseOccurred;
   int loc;
   tokenizer.GetToken();
   varTok = tokenizer.GetToken();
   if (varTok.kind == TTokenizer.undeclaredName_)
      Error("The identifier '" + varTok.name + "' has not been declared.");
   if (varTok.kind == TTokenizer.variableName_ && ((TVarSym)ST.get(((TSymbolToken)varTok).symbolLocation)).offset > 0)
      Error("The reserved work GRAB must be followed by the name of a global variable.  '" + varTok.name + "' is a local variable.");
   if (varTok.kind != TTokenizer.variableName_)
      Error("The reserved word GRAB must be followed by the name of a global variable.");
   loc = ((TVarSym)ST.get(((TSymbolToken)varTok).symbolLocation)).trueSymbolLoc;
   num = ((TVarSym)ST.get(loc)).grabNum;
   if (num == -1) {
     num = prog.GrabCount;
     prog.GrabCount++;
     ((TVarSym)ST.get(loc)).grabNum = num;
   }
   prog.addInstruction(TTokenizer.Grab_, num, tokenizer.pos);
   jumpLoc = prog.insCt;
   prog.addInstruction(TTokenizer.Jump_, jumpLoc - 1, tokenizer.pos);
   tok = tokenizer.GetToken();
   if (tok.kind != TTokenizer.then_)
      Error("Expected to find the reserved word THEN, which is required in a GRAB command.");
   elseOccurred = false;
   grabBeingParsed = 2;
   do {
    tok = tokenizer.LookToken();
    if (tok.kind == TTokenizer.endOfData_)
     Error("Missing ENDGRAB.  End of program occurred inside a GRAB statement.");
    else if (tok.kind == TTokenizer.Fork_ && grabBeingParsed == 2)
     Error("A FORK statement cannot occur inside a GRAB statement, except in the ELSE part.");
    else if (tok.kind == TTokenizer.Die_ && grabBeingParsed == 2)
     Error("A KILLPROCESS statement cannot occur inside a GRAB statement, except in the ELSE part.");
    else if (tok.kind == TTokenizer.else_) {
      grabBeingParsed = 1;
      tok = tokenizer.GetToken();
      elseOccurred = true;
      prog.addInstruction(TTokenizer.endGrab_, num, tokenizer.pos);
      prog.addInstruction(TTokenizer.Jump_, 0, tokenizer.pos);
      prog.data[jumpLoc] = prog.insCt;
      jumpLoc = prog.insCt - 1;
    }
    else if (tok.kind != TTokenizer.endGrab_)
     ParseStatement();
   } while (tok.kind != TTokenizer.endGrab_);
   tok = tokenizer.GetToken();
   if (elseOccurred)
    prog.data[jumpLoc] = prog.insCt;
   else
    prog.addInstruction(TTokenizer.endGrab_, num, tokenizer.pos);
   grabBeingParsed = 0;
 }
 
 
  private int DoVarForIOStatement (String str, int start) {
    int i = start; 
    StringBuffer name = new StringBuffer();
    boolean done;
    int loc;
    int kind; // SymbolKinds
    done = false;
    i = i + 1;
    if (i >= str.length() || !Character.isLetter(str.charAt(i)))
      Error("Expected a variable name following # in string.");
    do {
      name.append(str.charAt(i));
      i = i + 1;
    } while ( i < str.length() && (Character.isLetter(str.charAt(i)) || Character.isDigit(str.charAt(i)) || str.charAt(i) == '_'));
    loc = ST.findSymbol(name.toString().toLowerCase());
    if (loc == -1)
     Error("Expected variable name after # in string; '" + name.toString() + "' is an undeclared identifier.");
    kind = ST.get(loc).kind;
    if (kind == TSymbolTable.refParamSymb)
     prog.addInstruction(TTokenizer.PushRefParam_, ((TVarSym)ST.get(loc)).offset, tokenizer.pos);
    else if (kind == TSymbolTable.paramSymb || kind == TSymbolTable.variableSymb)
     prog.addInstruction(TTokenizer.Push_, ((TVarSym)ST.get(loc)).offset, tokenizer.pos);
    else if (kind == TSymbolTable.keyWordSymb && (((TKeyWordSym)ST.get(loc)).token >= TTokenizer.turtleX_ && ((TKeyWordSym)ST.get(loc)).token <= TTokenizer.forkNumber_))
     prog.addInstruction(((TKeyWordSym)ST.get(loc)).token, 0, tokenizer.pos);
    else
     Error("Expected variable name after # in string; '" + name + "' is not a variable name.");
    return i;
  }


 private void ParseIOStatement() {
   TToken firstTok, tok;
   int i, len;
   int loc;
   StringBuffer buf = new StringBuffer();
   firstTok = tokenizer.GetToken();
   tok = tokenizer.GetToken();
   if (tok.kind != TTokenizer.leftParen_)
     Error("Expected left parenthesis to begin parameter list.");
   tok = tokenizer.GetToken();
   if (tok.kind != TTokenizer.string_)
     Error("The first parameter for this subroutine must be a string.");
   loc = prog.stringCt;
   i = 0;
   len = ((TStringToken)tok).str.length();
   String str = ((TStringToken)tok).str;
   while (i < len) {
     if (str.charAt(i) == '#') {
       if (i < len-1 && str.charAt(i + 1) == '#') {
         buf.append('#');
         i = i + 2;
       }
       else {
         buf.append((char)255);
         i = DoVarForIOStatement(str,i);
       }
     }
     else {
       buf.append(str.charAt(i));
       i = i + 1;
     }
   }
   prog.addString(buf.toString());
   if (firstTok.kind == TTokenizer.AskUser_ || firstTok.kind == TTokenizer.YesOrNo_) {
     tok = tokenizer.GetToken();
     if (tok.kind != TTokenizer.comma_)
      Error("Expected a comman here.  (Two parameters are required.)");
     tok = tokenizer.GetToken();
     if (tok.kind == TTokenizer.refParamName_)
      prog.addInstruction(TTokenizer.Push_, ((TVarSym)ST.get(((TSymbolToken)tok).symbolLocation)).offset, tok.position);
     else if (tok.kind == TTokenizer.variableName_ || tok.kind == TTokenizer.paramName_)
      prog.addInstruction(TTokenizer.PushAbsoluteReference_, ((TVarSym)ST.get(((TSymbolToken)tok).symbolLocation)).offset, tok.position);
     else if (tok.kind == TTokenizer.undeclaredName_)
      Error("Undeclared identifier '" + tok.name + "'.");
     else
      Error("Unexpected item found.  This is a REF parameter;  actual parameter must be a variable name.");
   }
   tok = tokenizer.GetToken();
   if (tok.kind == TTokenizer.comma_)
    Error("Too many parameters provided.");
   else if (tok.kind != TTokenizer.rightParen_)
    Error("Expected right parenthesis to end parameter list.");
   prog.addInstruction(firstTok.kind, loc, firstTok.position);
  }

  private void ParseStatement() {
   TToken tok;
   tok = tokenizer.LookToken();
   if ((tok.kind >= TTokenizer.HideTurtle_ && tok.kind <= TTokenizer.Halt_) 
            || (tok.kind >= TTokenizer.red_ && tok.kind <= TTokenizer.white_)) {
      tok = tokenizer.GetToken();
      ParseActualParams(0, null, tok.name);
      prog.addInstruction(tok.kind, 0, tok.position);
   }
   else if (tok.kind == TTokenizer.Fork_ || (tok.kind >= TTokenizer.Forward_ && tok.kind <= TTokenizer.Circle_)) {
      tok = tokenizer.GetToken();
      ParseActualParams(1, null, tok.name);
      prog.addInstruction(tok.kind, 0, tok.position);
   }
   else if (tok.kind == TTokenizer.functionName_ ||
                (tok.kind >= TTokenizer.Random_ && tok.kind <= TTokenizer.forkNumber_) ||
                (tok.kind >= TTokenizer.sin_ && tok.kind <= TTokenizer.arcsin_) ||
                (tok.kind >= TTokenizer.abs_ && tok.kind <= TTokenizer.RandomInt_))
     Error("Function name '" + tok.name + "' not legal here; functions cannot be used like subroutines or assigned values.");
   else
   switch (tok.kind) {
    case TTokenizer.variableName_:
    case TTokenizer.refParamName_:
    case TTokenizer.paramName_: 
       ParseAssignmentStatement();
       break;
    case TTokenizer.subName_: 
      ParseSubroutineCall();
      break;
    case TTokenizer.if_: 
      ParseIfStatement();
      break;
    case TTokenizer.loop_: 
      ParseLoopStatement();
      break;
    case TTokenizer.exit_:
    case TTokenizer.exitif_:
    case TTokenizer.exitunless_: 
      ParseExitStatement();
      break;
    case TTokenizer.return_: 
      ParseReturnStatement();
      break;
    case TTokenizer.Grab_: 
      ParseGrab();
      break;
    case TTokenizer.Die_:
      tok = tokenizer.GetToken();
      if (grabBeingParsed == 2)
         Error("A KillProcess command cannot occur in a GRAB statement, except in the ELSE part.");
      if (parsingSubroutine || parsingFunction) {
        prog.addInstruction(TTokenizer.return_, offsetCt, tok.position);
        returnFound = true;
      }
      else
        prog.addInstruction(TTokenizer.Die_, 0, tok.position);
      break;
    case TTokenizer.Move_:
    case TTokenizer.MoveTo_:
    case TTokenizer.Arc_:
       tok = tokenizer.GetToken();
       ParseActualParams(2, null, tok.name);
       prog.addInstruction(tok.kind, 0, tok.position);
       break;
    case TTokenizer.rgb_:
    case TTokenizer.hsb_:
       tok = tokenizer.GetToken();
       ParseActualParams(3, null, tok.name);
       prog.addInstruction(tok.kind, 0, tok.position);
       break;
    case TTokenizer.declare_:
    case TTokenizer.import_: 
      ParseDeclaration();
      break;
    case TTokenizer.AskUser_:
    case TTokenizer.YesOrNo_:
    case TTokenizer.TellUser_:
    case TTokenizer.DrawText_: 
       ParseIOStatement();
       break;
    case TTokenizer.sub_:
    case TTokenizer.function_:
    case TTokenizer.predeclare_: 
       Error("Subroutine and function declarations cannot be nested inside each other or inside statements.");
       break;
    case TTokenizer.endloop_: 
       Error("'END LOOP' found without matching 'LOOP'.  Check nesting of statements.");
       break;
    case TTokenizer.else_:
    case TTokenizer.elseif_:
    case TTokenizer.endif_: 
       Error("'" + tok.name + "' found without matching 'IF'.  Check nesting of statements.");
       break;
    case TTokenizer.endGrab_: 
       Error("'ENDGRAB' found without matching 'GRAB'.  Check nesting of statements.");
       break;
    case TTokenizer.endsub_: 
       Error("'ENDSUB' found without matching 'SUB'.  Check nesting.");
       break;
    case TTokenizer.endfunction_: 
       Error("'ENDFUNCTION found without matching 'FUNCTION'.  Check nesting.");
       break;
    case TTokenizer.undeclaredName_: 
      if (parsingSubroutine)
        Error("The identifier '" + tok.name + "' has not been declared in this subroutine.");
      else if (parsingFunction)
        Error("The identifier '" + tok.name + "' has not been declared in this function.");
      else
        Error("The identifier '" + tok.name + "' has not been declared.");
      break;
    default:
      Error("Unexpected item found; expected a statement or declaration.");
   }
 }

 private void ParseActualParams (int paramCt,
       BitSet paramTypes,
       String subName) {
   TToken tok;
   int i;
   if (paramCt == 0) {
     tok = tokenizer.LookToken();
     if (tok.kind == TTokenizer.leftParen_) {
       tokenizer.GetToken();
       tok = tokenizer.GetToken();
       if (tok.kind != TTokenizer.rightParen_)
         Error("Expected right parenthesis; '" + subName + "' has no parameters.");
     }
     return;
   }
   tok = tokenizer.GetToken();
   if (tok.kind != TTokenizer.leftParen_)
     Error("Expected left parenthesis to begin parameter list for '" + subName + "'.");
   for (i = 1; i <= paramCt; i++) {
     tok = tokenizer.LookToken();
     if (paramTypes != null && paramTypes.get(i - 1)) {
       tok = tokenizer.GetToken();
       if (tok.kind == TTokenizer.refParamName_)
         prog.addInstruction(TTokenizer.Push_, ((TVarSym)ST.get(((TSymbolToken)tok).symbolLocation)).offset, tok.position);
       else if (tok.kind == TTokenizer.variableName_ || tok.kind == TTokenizer.paramName_)
         prog.addInstruction(TTokenizer.PushAbsoluteReference_, ((TVarSym)ST.get(((TSymbolToken)tok).symbolLocation)).offset, tok.position);
       else if (tok.kind == TTokenizer.undeclaredName_)
         Error("Undeclared identifier, '" +  tok.name + "'.");
       else
         Error("Unexpected item found.  This is a REF parameter;  actual parameter must be a variable name.");
       tok = tokenizer.LookToken();
       if (tok.kind != TTokenizer.comma_ && tok.kind != TTokenizer.rightParen_) 
       Error("Unexpected item found.  This is a REF parameter;  actual parameter must be a variable name.");
     }
     else {
       ParseComputation();
       tok = tokenizer.LookToken();
     }
     tok = tokenizer.GetToken();
     if (i < paramCt) {
       if (tok.kind == TTokenizer.rightParen_)
         Error("Not enough parameters supplied for '" +  subName+  "'.  (" +  paramCt + " required.)");
       else if (tok.kind != TTokenizer.comma_)
         Error("Expected a comma here to separate parameters.");
     }
   }
   if (tok.kind != TTokenizer.rightParen_)
     Error("Expected right parenthesis here to end parameter list for '" + subName + "'.");
 }

 int ParseFormalParams (BitSet paramTypes) {
   int paramCt;
   TToken tok;
   int kind; // SymbolKinds
   int loc;
   String name;
   paramCt = 0;
   tokenizer.GetToken();  // "("
   tok = tokenizer.LookToken();
   if (tok.kind == TTokenizer.rightParen_) {
     tok = tokenizer.GetToken();
     return 0;
   }
   do {
    tok = tokenizer.GetToken();
    if (tok.kind == TTokenizer.ref_) {
      kind = TSymbolTable.refParamSymb;
      paramTypes.set(paramCt);
      tok = tokenizer.GetToken();
    }
    else
     kind = TSymbolTable.paramSymb;
    paramCt++;
    if (tok.kind == TTokenizer.undeclaredName_ || tok.kind == TTokenizer.functionName_ || tok.kind == TTokenizer.subName_) {
      if (paramCt > 30)
        Error("Too many parameters; there is a limit of 30.");
      if (tok.kind == TTokenizer.undeclaredName_)
        loc = ST.addSymbol(kind, tok.name);
      else {
       name = ST.get(((TSymbolToken)tok).symbolLocation).name;
       loc = ST.addSymbol(kind, name);
      }
      offsetCt++;
      ST.setVariableData(loc, offsetCt);
    }
    else if (tok.kind < TTokenizer.variableName_ && !(tok.kind >= TTokenizer.leftParen_ && tok.kind <= TTokenizer.comma_))
     Error("A reserved word can''t be redefined as a parameter name.");
    else if (tok.kind == TTokenizer.paramName_ || tok.kind == TTokenizer.refParamName_)
     Error("Duplicate parameter name.");
    else
     Error("Unexpected item found in formal parameter list.");
    tok = tokenizer.GetToken();
    if (tok.kind != TTokenizer.comma_ && tok.kind != TTokenizer.rightParen_)
     Error("Expected either a comma or a right parenthesis here, while reading a formal parameter list.");
   } while (tok.kind != TTokenizer.rightParen_);
   return paramCt;
 }


 private void ParseForwardDeclaration() {
   TToken firstTok, nameTok, tok;
   int nameLoc;
   int paramCt;
   BitSet paramTypes;
   tokenizer.GetToken();  // "predeclare"
   firstTok = tokenizer.GetToken();
   if (firstTok.kind != TTokenizer.sub_ && firstTok.kind != TTokenizer.function_)
    Error("The reserved word 'PREDECLARE' must be followed by either 'SUB' or 'FUNCTION'.");
   nameTok = tokenizer.GetToken();
   if (nameTok.kind != TTokenizer.undeclaredName_) {
    if (nameTok.kind < TTokenizer.variableName_)
     Error("You can't redefine a reserved word.");
    else
     Error("The identifier '" + nameTok.name + "' has already been defined.");
   }
   if (firstTok.kind == TTokenizer.sub_)
    nameLoc = ST.addSymbol(TSymbolTable.subSymb, nameTok.name);
   else
    nameLoc = ST.addSymbol(TSymbolTable.functionSymb, nameTok.name);
   prog.addInstruction(TTokenizer.Jump_, prog.insCt + 2, tokenizer.pos);
   prog.addInstruction(TTokenizer.Jump_, 0, tokenizer.pos);
   paramCt = 0;
   paramTypes = new BitSet(32);
   tok = tokenizer.LookToken();
   if (tok.kind == TTokenizer.leftParen_) {
     tok = tokenizer.GetToken();
     tok = tokenizer.LookToken();
     if (tok.kind != TTokenizer.rightParen_)
      do {
       if (paramCt == 30)
          Error("There is a maximum of 30 parameters for a subroutine or function.");
       paramCt++;
       tok = tokenizer.GetToken();
       if (tok.kind == TTokenizer.ref_) {
         paramTypes.set(paramCt-1);
         tok = tokenizer.GetToken();
       }
       if (tok.kind < TTokenizer.variableName_ || tok.kind == TTokenizer.string_ || tok.kind == TTokenizer.number_)
         Error("Expected an identifier here as the name for a dummy parameter.");
       tok = tokenizer.LookToken();
       if (tok.kind == TTokenizer.comma_)
          tok = tokenizer.GetToken();
      } while (tok.kind == TTokenizer.comma_);
     if (tok.kind != TTokenizer.rightParen_)
      Error("Expected a right parenthesis to end parameter list or a comma to separate parameters.");
     tok = tokenizer.GetToken();
   }
   paramTypes.set(31);  // this sub is predeclared
   ST.setSubroutineData(nameLoc, prog.insCt - 1, paramCt, paramTypes);
 }

 private void ParseSubroutine() {
   TToken tok, nameTok, firstTok;
   int loc, nameLoc, i;
   int paramCt;
   int codeStart;
   BitSet paramTypes;
   boolean predeclared;
   offsetCt = 0;
   firstTok = tokenizer.GetToken();
   if (firstTok.kind == TTokenizer.sub_)
    parsingSubroutine = true;
   else {
     parsingFunction = true;
     returnFound = false;
   }
   nameTok = tokenizer.GetToken();
   predeclared = false;
   if (nameTok.kind == TTokenizer.subName_ || nameTok.kind == TTokenizer.functionName_) {
     TSubSym sym = (TSubSym)ST.get(((TSymbolToken)nameTok).symbolLocation);
     if (!sym.paramTypes.get(31))
      Error("The identifier '" + nameTok.name + "' has already been defined.");
     else if (nameTok.kind == TTokenizer.subName_ && firstTok.kind == TTokenizer.function_)
      Error("This was previously predeclared as a subroutine; it cannot be redefined as a function.");
     else if (nameTok.kind == TTokenizer.functionName_ && firstTok.kind == TTokenizer.sub_)
      Error("This was previously predeclared as a function; it cannot be redefined as a subroutine.");
     else
      predeclared = true;
   }
   else if (nameTok.kind != TTokenizer.undeclaredName_) {
    if (nameTok.kind < TTokenizer.variableName_)
     Error("You can't use a reserved word or a symbol as the name of a subroutine or function.");
    else
     Error("The identifier '" + nameTok.name + "' has already been defined.");
   }
   if (predeclared)
    nameLoc = ((TSymbolToken)nameTok).symbolLocation;
   else if (firstTok.kind == TTokenizer.sub_)
    nameLoc = ST.addSymbol(TSymbolTable.subSymb, nameTok.name);
   else
    nameLoc = ST.addSymbol(TSymbolTable.functionSymb, nameTok.name);
   ST.startSubroutineSymbols();
   codeStart = prog.insCt;
   prog.addInstruction(TTokenizer.Jump_, 0, tokenizer.pos);
   if (predeclared) {
     TSubSym sym = (TSubSym)ST.get(nameLoc);
     sym.paramTypes.clear(31);
     prog.data[sym.start] = prog.insCt;
   }
   tok = tokenizer.LookToken();
   if (tok.kind != TTokenizer.leftParen_) {
     paramCt = 0;
     paramTypes = new BitSet(32);
   }
   else {
     paramTypes = new BitSet(32);
     paramCt = ParseFormalParams(paramTypes);
     tok = tokenizer.LookToken();
   }
   if (predeclared) {
     TSubSym sym = (TSubSym)ST.get(nameLoc);
     if (paramCt != sym.paramCount)
      Error("The number of parameters given here does not agree with the number in the predeclaration of this subroutine.");
     for (i = 0; i < paramCt; i++)
       if (paramTypes.get(i) != sym.paramTypes.get(i))
         Error("The type of parameter number " + (i + 1) + "(REF or non-REF) does not agree with its type in the predeclaration of this subroutine.");
   }
   else
    ST.setSubroutineData(nameLoc, codeStart + 1, paramCt, paramTypes);
   prog.addInstruction(TTokenizer.SetStackRef_, paramCt, tokenizer.pos);
   do {
    if (tok.kind == TTokenizer.endfunction_ && parsingSubroutine)
     Error("Unexpected 'END FUNCTION' -- expecting END SUB or a statement or declaration.");
    else if (tok.kind == TTokenizer.endsub_ && parsingFunction)
     Error("Unexpected 'END SUB' -- expecting END FUNCTION or a statement or declaration.");
    else if (tok.kind == TTokenizer.endif_ || tok.kind == TTokenizer.endloop_ || tok.kind == TTokenizer.else_ || tok.kind == TTokenizer.elseif_) {
     if (parsingSubroutine)
      Error("Expecting END SUB or a statement or declaration.  (CheckNesting)");
     else
      Error("Expecting END FUNCTION or a statement or declaration.  (CheckNesting)");
    }
    ParseStatement();
    tok = tokenizer.LookToken();
   } while (tok.kind != TTokenizer.endsub_ && tok.kind != TTokenizer.endfunction_);
   if (parsingFunction && !returnFound)
    Error("No return statement in function; it has no way to return a value.");
   if (tok.kind == TTokenizer.endsub_) {
     if (firstTok.kind == TTokenizer.function_)
      Error("END FUNCTION is required to end a function definition.");
   }
   else if (firstTok.kind == TTokenizer.sub_)
    Error("END SUB is required to end a subroutine definition.");
   tok = tokenizer.GetToken();
   prog.addInstruction(TTokenizer.return_, offsetCt, tokenizer.pos);
   ST.endSubroutineSymbols();
   prog.data[codeStart] = prog.insCt;
   parsingSubroutine = false;
   parsingFunction = false;
 }


}