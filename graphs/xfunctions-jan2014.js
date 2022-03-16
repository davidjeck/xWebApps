"use strict";

if (! xfunctions) {
    var xfunctions = {};
}

//----------------------------------- from xfunctions-core.js --------------------------------

xfunctions.sameCases = function(cases1,cases2) {
    if (cases1.length != cases2.length)
       return false;
    for (var i = 0; i < cases1.length; i++) {
        if (cases1[i] != cases2[i])
            return false;
    }
    return true;
}

xfunctions.MathObject= function(name) {
    if (  !(typeof name == "string"))
        throw "name required, must be a string";
    this.getName = function() { return name; };
}

xfunctions.Constant = function(name, value) {
    xfunctions.MathObject.call(this, name);
    if ( !(typeof value == "number"))
        throw "numeric value required for constant";
    this.getValue = function() { return value; };
}
xfunctions.Constant.prototype = new xfunctions.MathObject("");
xfunctions.Constant.prototype.constructor = xfunctions.Constant;

xfunctions.Variable = function(name, initialValue) {
    xfunctions.MathObject.call(this,name);
    if (initialValue) {
        this.setValue(initialValue);
    }
    else {
        this.value = 0;
    }
}
xfunctions.Variable.prototype = new xfunctions.MathObject("");
xfunctions.Variable.prototype.constructor = xfunctions.Variable;
xfunctions.Variable.prototype.getValue = function() {
    return this.value;
}
xfunctions.Variable.prototype.setValue = function(x) {
    if ( !(typeof x == "number"))
        throw "numeric value required";
    this.value = x;
}
xfunctions.Variable.prototype.toString = function() {
    return this.getName();
}

xfunctions.Function = function(name,domainDimension,rangeDimension) { // abstract class to represent different
    xfunctions.MathObject.call(this,name);
    if ( !domainDimension )
        domainDimension = 1;
    if ( !rangeDimension )
        rangeDimension = 1;
    this.domainDimension = domainDimension;
    this.rangeDimension = rangeDimension;
    this.params = new Array(domainDimension);
}
xfunctions.Function.prototype = new xfunctions.MathObject("");
xfunctions.Function.prototype.constructor = xfunctions.Function;

xfunctions.ExpressionFunction = function(name,arg,def) {
        // second parameter is either a xfunctions.Variable or an array of them.
        // third parameter is either a xfunctions.Expression or an array of them;
    var parmArray = [];
    var defArray = [];
    if (arg instanceof xfunctions.Variable)
        parmArray.push(arg);
    else {
        for (var i = 0; i < arg.length; i++)
           parmArray.push(arg[i]);
    }
    if (def instanceof xfunctions.Expression) {
        defArray.push(def);
    }
    else {
        for (var i = 0; i < def.length; i++)
            defArray.push(def[i]);
    }
    xfunctions.Function.call(this,name,parmArray.length,defArray.length);
    this.param = parmArray;
    this.def = defArray;
}
xfunctions.ExpressionFunction.prototype = new xfunctions.Function("",0,0);
xfunctions.ExpressionFunction.prototype.constructor = xfunctions.ExpressionFunction;
xfunctions.ExpressionFunction.prototype.eval = function(x,casesArray) {
    if ( typeof x == "number")
        x = arguments;
    if ( x.length != this.param.length )
        throw "wrong number of arguments";
    var ans = new Array(this.def.length);
    for (var i = 0; i < this.param.length; i++)
        this.param[i].setValue(x[i]);
    if (this.def.length == 1)
        return this.def[0].value(casesArray);
    for (var i = 0; i < this.def.length; i++)
        ans[i] = this.def[i].value(casesArray);
    return ans;
}
xfunctions.ExpressionFunction.prototype.derivative = function(wrtParamNum) {
    if (!wrtParamNum)
        wrtParamNum = 1;
    var name;
    if (this.domainDimension == 1)
        name = this.getName() + "'";
    else {
        name = "D" + wrtParamNum + "_" + this.getName();
    }
    var wrtVar = this.param[wrtParamNum-1];
    var def = [];
    for (var i = 0; i < this.def.length; i++) {
        def.push( this.def[i].derivative(wrtVar));
    }
    return new xfunctions.ExpressionFunction(name,this.param,def);
}
xfunctions.ExpressionFunction.prototype.toString = function() {
    var s = this.getName() + "(";
    for (var i = 0; i < this.param.length; i++) {
        if (i > 0)
            s += ",";
        s += this.param[i].getName();
    }
    s += ") = ";
    if (this.def.length > 1) {
        s += "(";
    }
    for (var i = 0; i < this.def.length; i++) {
        if (i > 0)
            s += ",";
        s += this.def[i];
    }
    if (this.def.length > 1) {
        s += ")";
    }
    return s;
}

xfunctions.SIN = 1;  // constants for standard functions
xfunctions.COS = 2;
xfunctions.TAN = 3;
xfunctions.COT = 4;
xfunctions.SEC = 5;
xfunctions.CSC = 6;
xfunctions.ARCSIN = 7;
xfunctions.ARCCOS = 8;
xfunctions.ARCTAN = 9;
xfunctions.EXP = 10;
xfunctions.LN = 11;
xfunctions.LOG2 = 12;
xfunctions.LOG10 = 13;
xfunctions.ABS = 14;
xfunctions.SQRT = 15;
xfunctions.ROUND = 16;
xfunctions.TRUNC = 17;
xfunctions.FLOOR = 18;
xfunctions.CEILING = 19;
xfunctions.CUBERT = 20;
xfunctions.standardFunctionName = [ "sin", "cos", "tan", "cot", "sec", "csc",
                                   "arcsin", "arccos", "arctan", "exp", "ln", "log2", "log10","abs", 
                                   "sqrt", "round", "trunc", "floor", "ceiling", "cubert" ];

xfunctions.PLUS = 1;   // constants for nodes in expression trees
xfunctions.MINUS = 2;
xfunctions.TIMES = 3;
xfunctions.DIVIDE = 4;
xfunctions.POWER = 5;
xfunctions.UNARYMINUS = 6;
xfunctions.AND = 7;
xfunctions.OR = 8;
xfunctions.EQ = 9;
xfunctions.NE = 10;
xfunctions.LT = 11;
xfunctions.GT = 12;
xfunctions.LE = 13;
xfunctions.GE = 14;
xfunctions.NOT = 15;


xfunctions.StandardFunction = function(name,id) {
    xfunctions.MathObject.call(this,name);
    var ID;
    if ( !(typeof id == "number") || id < 1 || id > 20 )
        throw "unknown standard function id " + id;
    ID = Math.round(id);
    this.getID = function() { return ID; };
}
xfunctions.StandardFunction.prototype = new xfunctions.MathObject("");
xfunctions.StandardFunction.prototype.constructor = xfunctions.StandardFunction;
xfunctions.StandardFunction.prototype.eval = function(x,casesArray) {
    var ans, casecode = null;
    switch (this.getID()) {
        case xfunctions.SIN: ans = Math.sin(x); break;
        case xfunctions.COS: ans = Math.cos(x); break;
        case xfunctions.TAN: casecode = Math.floor((x-Math.PI/2.0)/Math.PI); ans = Math.tan(x); break;
        case xfunctions.COT: casecode = Math.floor(x/Math.PI); ans = Math.cos(x) / Math.sin(x); break;
        case xfunctions.SEC: casecode = Math.floor((x-Math.PI/2.0)/Math.PI); ans = 1.0 / Math.cos(x); break;
        case xfunctions.CSC: casecode = Math.floor(x/Math.PI); ans = 1.0 / Math.sin(x); break;
        case xfunctions.ARCSIN: ans = Math.asin(x); break;
        case xfunctions.ARCCOS: ans = Math.acos(x); break;
        case xfunctions.ARCTAN: ans = Math.atan(x); break;
        case xfunctions.EXP: ans = Math.exp(x); break;
        case xfunctions.LN:  ans = Math.log(x); break;
        case xfunctions.LOG2: ans = Math.log(x) / Math.log(2); break;
        case xfunctions.LOG10: ans = Math.log(x) / Math.log(10); break;
        case xfunctions.ABS: casecode = (x > 0)? 1 : -1; ans = Math.abs(x); break;
        case xfunctions.SQRT: ans = Math.sqrt(x); break;
        case xfunctions.ROUND: var y = Math.round(x); casecode = y; ans = y;  break;
        case xfunctions.TRUNC: var y = ((x >= 0)? Math.floor(x) : Math.ceil(x)); casecode = y; ans = y;  break;
        case xfunctions.FLOOR: var y = Math.floor(x); casecode = y; ans = y;  break;
        case xfunctions.CEILING: var y = Math.ceil(x); casecode = y; ans = y; break;
        case xfunctions.CUBERT: casecode = (x > 0)? 1 : -1; ans = (x >= 0)? Math.pow(x,1.0/3.0) : -Math.pow(-x,1.0/3.0); break;
    }
    if (casesArray && casecode != null) {
        casesArray.push(casecode);
    }
    return ans;
}

xfunctions.standardFunction = [
    null,
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.SIN-1], xfunctions.SIN),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.COS-1], xfunctions.COS),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.TAN-1], xfunctions.TAN),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.COT-1], xfunctions.COT),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.SEC-1], xfunctions.SEC),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.CSC-1], xfunctions.CSC),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.ARCSIN-1], xfunctions.ARCSIN),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.ARCCOS-1], xfunctions.ARCCOS),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.ARCTAN-1], xfunctions.ARCTAN),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.EXP-1], xfunctions.EXP),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.LN-1], xfunctions.LN),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.LOG2-1], xfunctions.LOG2),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.LOG10-1], xfunctions.LOG10),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.ABS-1], xfunctions.ABS),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.SQRT-1], xfunctions.SQRT),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.ROUND-1], xfunctions.ROUND),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.TRUNC-1], xfunctions.TRUNC),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.FLOOR-1], xfunctions.FLOOR),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.CEILING-1], xfunctions.CEILING),
    new xfunctions.StandardFunction(xfunctions.standardFunctionName[xfunctions.CUBERT-1], xfunctions.CUBERT)
];


xfunctions.SymbolTable = function(parent,caseSensative) {
    var parentContext = null;
    var symbolTable = {};
    var ignorecase = false;
    if (parent) {
        if ( ! (parent instanceof xfunctions.SymbolTable) ) {
            throw "Argument to SymbolTable constructor must be another SymbolTable.";
        }
        parentContext = parent;
    }
    else {
        symbolTable["pi"] = new xfunctions.Constant("pi", Math.PI);
        symbolTable["e"] = new xfunctions.Constant("e", Math.E);
        for (var i = i ; i <= 20; i++) {
            var f = xfunctions.standardFunction(i);
            symbolTable[f.getName()] = f;
        }
    }
    if (caseSensative) {
        ignorecase = true;
    }
    this.get = function(name) {
        if ( !name || !(typeof name == "string"))
            throw "Get requires a string as its argument";
        if (ignorecase) {
            name = name.toLowerCase();
        }
        if (symbolTable[name] != null)
            return symbolTable[name];
        else if (parentContext != null)
            return parentContext.get(name);
        else
            return null;
    }
    this.put = function(name, value) {
        if ( !name || !(typeof name == "string"))
            throw "Put requires a string as its first argument";
        if (ignorecase) {
            name = name.toLowerCase();
        }
        if (! value)
            throw "Put requires a non-null value as second argument";
        symbolTable[name] = value;
    }
    this.addMathObject = function(mthObj) { 
        if (!mthObj || !(mthObj instanceof xfunctions.MathObject))
            throw "requierd argument missing or of wrong type";
        this.put(mthObj.getName(), mthObj);
    }
    this.remove = function(name) {
        if ( !name || !(typeof name == "string"))
           return;
        if (ignorecase) {
        }
        if (name in symbolTable)
            delete symbolTable[name];
    }
    if (parent == null) {
        this.addMathObject(new xfunctions.Constant("pi",Math.PI));
        this.addMathObject(new xfunctions.Constant("e",Math.E));
        for (var i = 0; i < 20; i++) {
            this.addMathObject(new xfunctions.StandardFunction(xfunctions.standardFunctionName[i],i+1));
        }
    }
}

//---------------------------- Expressions ------------------------------------------

xfunctions.Expression = function() {
}
xfunctions.Expression.prototype.isLogical = function() {
    return false;
}

xfunctions.ConstantNode = function(val) {
    this.mathobj = null;
    var value;
    if (typeof val == "number")
        value = val;
    else if (val instanceof xfunctions.Constant) {
        value = val.getValue();
        this.mathobj = val;
    }
    else {
        throw "illegal parameter for ConstantNode constructor";
    }
    this.value = function() {
        return value;
    }
    this.toString = function() {
        if (this.mathobj)
           return this.mathobj.getName();
        else
           return "" + value;
    }
}
xfunctions.ConstantNode.prototype = new xfunctions.Expression();
xfunctions.ConstantNode.prototype.constructor = xfunctions.ConstantNode;
xfunctions.ConstantNode.prototype.isConstant = function() {
    return true;
}
xfunctions.ConstantNode.prototype.copy = function() {
    return new xfunctions.ConstantNode( this.mathobj? this.mathobj : this.value() );
}
xfunctions.ConstantNode.prototype.derivative = function(wrtVariable) {
    return new xfunctions.ConstantNode(0);
}

xfunctions.VariableNode = function(variable) {
    if ( !variable || !(variable instanceof xfunctions.Variable) )
        throw "illegal parameter for VariableNode constructor";
    this.variable = variable;
}
xfunctions.VariableNode.prototype = new xfunctions.Expression();
xfunctions.VariableNode.prototype.constructor = xfunctions.VariableNode;
xfunctions.VariableNode.prototype.toString = function() {
    return this.variable.getName();
}
xfunctions.VariableNode.prototype.value = function() {
    return this.variable.getValue();
}
xfunctions.VariableNode.prototype.isConstant = function(wrtVariable) {
    return wrtVariable ? (this.variable != wrtVariable) : false;
}
xfunctions.VariableNode.prototype.copy = function() {
    return new xfunctions.VariableNode(this.variable);
}
xfunctions.VariableNode.prototype.derivative = function(wrtVariable) {
    return new xfunctions.ConstantNode(wrtVariable == this.variable ? 1 : 0);
}

xfunctions.StandardFunctionNode = function(standardFunction, arg) {  
    if (typeof standardFunction == "number")
       standardFunction = new xfunctions.StandardFunction(xfunctions.standardFunctionName[standardFunction-1],standardFunction);
    else if (!standardFunction || !(standardFunction instanceof xfunctions.StandardFunction)
            || !arg || ! (arg instanceof xfunctions.Expression) ) {
        throw "illegal argument for StandardFunctionNode constructor";
    }
    this.standardFunction = standardFunction;
    this.arg = arg;
}
xfunctions.StandardFunctionNode.prototype = new xfunctions.Expression();
xfunctions.StandardFunctionNode.prototype.constructor = xfunctions.StandardFunctionNode;
xfunctions.StandardFunctionNode.prototype.toString = function() {
    return this.standardFunction.getName() + "(" + this.arg.toString() + ")";
}
xfunctions.StandardFunctionNode.prototype.value = function(casesArray) {
    var v = this.arg.value(casesArray);
    return this.standardFunction.eval(v,casesArray);
}
xfunctions.StandardFunctionNode.prototype.isConstant = function(wrtVariable) {
    return this.arg.isConstant(wrtVariable);
}
xfunctions.StandardFunctionNode.prototype.copy = function() {
    return new xfunctions.StandardFunctionNode(this.standardFunction, this.arg.copy());
}
xfunctions.StandardFunctionNode.prototype.derivative = function(wrtVariable) {
    if (this.arg.isConstant(wrtVariable))
        return new xfunctions.ConstantNode(0);
    var id = this.standardFunction.getID();
    if (id == xfunctions.ROUND || id == xfunctions.TRUNC || id == xfunctions.FLOOR || id == xfunctions.CEILING)
        return new xfunctions.ConstantNode(0);
    var d;
    switch (id) {
         case xfunctions.SIN: d = new xfunctions.StandardFunctionNode(xfunctions.COS,this.arg.copy()); break;
         case xfunctions.COS: d = new xfunctions.UnaryNode(xfunctions.UNARYMINUS, new xfunctions.StandardFunctionNode(xfunctions.SIN,this.arg.copy())); break;
         case xfunctions.TAN: d = new xfunctions.BinaryNode(xfunctions.POWER,new xfunctions.StandardFunctionNode(xfunctions.SEC,this.arg.copy()),new xfunctions.ConstantNode(2)); break;
         case xfunctions.COT: d = new xfunctions.UnaryNode(xfunctions.UNARYMINUS, 
                                new xfunctions.BinaryNode(xfunctions.POWER,new xfunctions.StandardFunctionNode(xfunctions.CSC,this.arg.copy()),new xfunctions.ConstantNode(2)));
                   break;
         case xfunctions.SEC: d = new xfunctions.BinaryNode(xfunctions.TIMES,
                                new xfunctions.StandardFunctionNode(xfunctions.SEC,this.arg.copy()),
                                new xfunctions.StandardFunctionNode(xfunctions.TAN,this.arg.copy())); 
                   break;
         case xfunctions.CSC: d = new xfunctions.UnaryNode(xfunctions.UNARYMINUS, new xfunctions.BinaryNode(xfunctions.TIMES,
                                new xfunctions.StandardFunctionNode(xfunctions.CSC,this.arg.copy()),
                                new xfunctions.StandardFunctionNode(xfunctions.COT,this.arg.copy()))); 
                    break;
         case xfunctions.ARCSIN: d = new xfunctions.BinaryNode(xfunctions.DIVIDE,
                                 new xfunctions.ConstantNode(1),
                                 new xfunctions.StandardFunctionNode(xfunctions.SQRT,
                                        new xfunctions.BinaryNode(xfunctions.MINUS,
                                            new xfunctions.ConstantNode(1),
                                            new xfunctions.BinaryNode(xfunctions.POWER,
                                                  this.arg.copy(),
                                                  new xfunctions.ConstantNode(2)))));
                    break;
         case xfunctions.ARCCOS: d = new xfunctions.BinaryNode(xfunctions.DIVIDE,
                                 new xfunctions.ConstantNode(-1),
                                 new xfunctions.StandardFunctionNode(xfunctions.SQRT,
                                        new xfunctions.BinaryNode(xfunctions.MINUS,
                                            new xfunctions.ConstantNode(1),
                                            new xfunctions.BinaryNode(xfunctions.POWER,
                                                  this.arg.copy(),
                                                  new xfunctions.ConstantNode(2))))); 
                    break;
         case xfunctions.ARCTAN: d = new xfunctions.BinaryNode(xfunctions.DIVIDE,
                               new xfunctions.ConstantNode(1),
                               new xfunctions.BinaryNode(xfunctions.PLUS,
                                      new xfunctions.ConstantNode(1),
                                      new xfunctions.BinaryNode(xfunctions.POWER,
                                                     this.arg.copy(),
                                                     new xfunctions.ConstantNode(2))));
                    break;
         case xfunctions.EXP: d = new xfunctions.StandardFunctionNode(xfunctions.EXP,this.arg.copy()); break;
         case xfunctions.LN: d =  new xfunctions.ConditionalNode(
                         new xfunctions.BinaryNode(xfunctions.GT,this.arg.copy(),new xfunctions.ConstantNode(0)),
                         new xfunctions.BinaryNode(xfunctions.DIVIDE,
                            new xfunctions.ConstantNode(1),
                            this.arg.copy()));
                  break;
         case xfunctions.LOG2: d = new xfunctions.ConditionalNode(
                         new xfunctions.BinaryNode(xfunctions.GT,this.arg.copy(),new xfunctions.ConstantNode(0)),
                         new xfunctions.BinaryNode(xfunctions.DIVIDE,
                            new xfunctions.ConstantNode(1),
                            new xfunctions.BinaryNode(xfunctions.TIMES,
                                   new xfunctions.StandardFunctionNode(xfunctions.LN,new xfunctions.ConstantNode(2)), 
                                   this.arg.copy())));
                  break;
         case xfunctions.LOG10: d =  new xfunctions.ConditionalNode(
                         new xfunctions.BinaryNode(xfunctions.GT,this.arg.copy(),new xfunctions.ConstantNode(0)),
                         new xfunctions.BinaryNode(xfunctions.DIVIDE,
                            new xfunctions.ConstantNode(1),
                            new xfunctions.BinaryNode(xfunctions.TIMES,
                                   new xfunctions.StandardFunctionNode(xfunctions.LN,new xfunctions.ConstantNode(10)), 
                                   this.arg.copy())));
                  break;
         case xfunctions.ABS: d = new xfunctions.BinaryNode(xfunctions.DIVIDE,
                                new xfunctions.StandardFunctionNode(xfunctions.ABS,this.arg.copy()),
                                this.arg.copy());
                       break;
         case xfunctions.SQRT: d = new xfunctions.BinaryNode(xfunctions.DIVIDE,
                              new xfunctions.ConstantNode(1),
                              new xfunctions.BinaryNode(xfunctions.TIMES, new xfunctions.ConstantNode(2),
                                   new xfunctions.StandardFunctionNode(xfunctions.SQRT,this.arg.copy())));
                   break;
         case xfunctions.CUBERT: d = new xfunctions.BinaryNode(xfunctions.DIVIDE,
                              new xfunctions.ConstantNode(1),
                              new xfunctions.BinaryNode(xfunctions.TIMES, new xfunctions.ConstantNode(3),
                                   new xfunctions.StandardFunctionNode(xfunctions.CUBERT,
                                           new xfunctions.BinaryNode(xfunctions.POWER,this.arg.copy(),new xfunctions.ConstantNode(2)))));
                   break;
        default: throw "unhandled case!";
    }
    if (this.arg instanceof xfunctions.VariableNode && this.arg.variable == wrtVariable)
        return d;
    else {
        var dd = this.arg.derivative(wrtVariable);
        if (dd instanceof xfunctions.ConstantNode && dd.value() == 0)
            return new xfunctions.ConstantNode(0);
        else
            return new xfunctions.BinaryNode(xfunctions.TIMES,d,dd);
    }
}

xfunctions.FunctionNode = function(func, args) { 
    if ( !args || !func || !(func instanceof xfunctions.Function) )
        throw "illegal parameter in FunctionNode constructor";
    if (args instanceof xfunctions.Expression)
        args = [args];
    else {
        for (var i = 0; i < args.length; i++) {
            if ( ! (args[i] instanceof xfunctions.Expression) )
                throw "illegal parameter in FunctionNode constructor";
        }
    }
    this.func = func;
    this.args = args;
}
xfunctions.FunctionNode.prototype = new xfunctions.Expression();
xfunctions.FunctionNode.prototype.constructor = xfunctions.FunctionNode;
xfunctions.FunctionNode.prototype.toString = function() {
    var s = this.func.getName() + "(";
    for (var i = 0; i < this.args.length; i++) {
        if (i > 0)
           s += ",";
        s += this.args[i].toString();
    }
    s += ")";
    return s;
}
xfunctions.FunctionNode.prototype.value = function(casesArray) {
    var a = new Array(this.args.length);
    for (var i = 0; i < a.length; i++) {
        a[i] = (this.args[i].value(casesArray));
    }
    return this.func.eval(a,casesArray);
}
xfunctions.FunctionNode.prototype.isConstant = function(wrtVariable) {
    for (var i = 0; i < this.args.length; i++) {
        if ( ! this.args[i].isConstant(wrtVariable))
            return false;
    }
    return true;
}
xfunctions.FunctionNode.prototype.copy = function() {
    var args = new Array(this.args.length);
    for (var j = 0; j < args.length; j++)
        args[j] = this.args[j].copy();
    return new xfunctions.FunctionNode(this.func,args);    
}
xfunctions.FunctionNode.prototype.derivative = function(wrtVariable) {
    var e  = null;
    for (var i = 0; i < this.args.length; i++) {
        if ( ! this.args[i].isConstant(wrtVariable) ) {
            var args = new Array(this.args.length);
            for (var j = 0; j < args.length; j++)
                args[j] = this.args[j].copy();
            var a = new xfunctions.FunctionNode(this.func.derivative(i+1),args);
            var b = this.args[i].derivative(wrtVariable);
            if (! (b instanceof xfunctions.ConstantNode && b.value() == 1) )
                a = new xfunctions.BinaryNode(xfunctions.TIMES, a, b);
            if (e == null)
                e = a;
            else
                e = new xfunctions.BinaryNode(xfunctions.PLUS,e,a);
        }
    }
    return e || new xfunctions.ConstantNode(0);
}


xfunctions.UnaryNode = function(opcode, arg) {
    if ( (opcode != xfunctions.UNARYMINUS && opcode != xfunctions.NOT) || !arg ||
        !(arg instanceof xfunctions.Expression)) {
        throw "illegal parameter in UnaryNode constructor";
    }
    this.opcode = opcode;
    this.arg = arg;
}
xfunctions.UnaryNode.prototype = new xfunctions.Expression();
xfunctions.UnaryNode.prototype.constructor = xfunctions.UnaryNode;
xfunctions.UnaryNode.prototype.isLogical = function() {
    return this.opcode == xfunctions.NOT;
}
xfunctions.UnaryNode.prototype.toString = function() {
    if (this.opcode == xfunctions.NOT) {
        return "NOT (" + this.arg.toString() + ")";
    }
    else if (this.arg instanceof xfunctions.ConstantNode || this.arg instanceof xfunctions.VariableNode ||
             this.arg instanceof xfunctions.StandardFunctionNode /*|| this.arg instanceof xfunctions.FunctionNode*/ ) {
        return "-" + this.arg.toString();
    }
    else {
        return "-(" + this.arg.toString() + ")";
    }
}
xfunctions.UnaryNode.prototype.value = function(casesArray) {
    if (this.opcode == xfunctions.UNARYMINUS)
        return -this.arg.value(casesArray);
    else {
        var v = this.arg.value() == 1 ? 0 : 1;
        if (casesArray)
            casesArray.push(v);
        return v;
    }
}
xfunctions.UnaryNode.prototype.isConstant = function(wrtVariable) {
    return this.arg.isConstant(wrtVariable);
}
xfunctions.UnaryNode.prototype.copy = function() {
    return new xfunctions.UnaryNode(this.opcode, this.arg.copy());
}
xfunctions.UnaryNode.prototype.derivative = function(wrtVariable) {
    if (this.opcode == xfunctions.NOT)
        throw "can't take derivative of boolean expression";
    if (this.arg.isConstant(wrtVariable))
        return new xfunctions.ConstantNode(0);
    return new xfunctions.UnaryNode(this.opcode, this.arg.derivative(wrtVariable));
}

xfunctions.BinaryNode = function(opcode, arg1, arg2) {
    if ( !arg1 || !arg2 || !(arg1 instanceof xfunctions.Expression) ||
            !(arg2 instanceof xfunctions.Expression)) {
        throw "illegal parameter in BinaryNode constructor";
    }
    if (opcode != xfunctions.PLUS && opcode != xfunctions.MINUS && opcode != xfunctions.TIMES &&
           opcode != xfunctions.DIVIDE && opcode != xfunctions.POWER && opcode != xfunctions.AND &&
           opcode != xfunctions.OR && opcode != xfunctions.EQ && opcode != xfunctions.NE &&
           opcode != xfunctions.LT && opcode != xfunctions.GT && opcode != xfunctions.LE &&
           opcode != xfunctions.GE ) {
        throw "illegal opcode in BinaryNode constructor";
    }
    this.opcode = opcode;
    this.param1 = arg1;
    this.param2 = arg2;
}
xfunctions.BinaryNode.prototype = new xfunctions.Expression();
xfunctions.BinaryNode.prototype.constructor = xfunctions.BinaryNode;
xfunctions.BinaryNode.prototype.isLogical = function() {
    return this.opcode >= xfunctions.AND;
}
xfunctions.BinaryNode.prototype.toString = function() {
    var a, b, c;
    switch(this.opcode) {
         case xfunctions.PLUS: b = (" + "); break;
         case xfunctions.MINUS: b = (" - "); break;
         case xfunctions.TIMES: b = (" * "); break;
         case xfunctions.DIVIDE: b = (" / "); break;
         case xfunctions.POWER: b = ("^"); break;
         case xfunctions.AND: b = (" AND "); break;
         case xfunctions.OR: b = (" OR "); break;
         case xfunctions.EQ: b = (" = "); break;
         case xfunctions.NE: b = (" != "); break;
         case xfunctions.LT: b = (" < "); break;
         case xfunctions.GT: b = (" > "); break;
         case xfunctions.LE: b = (" <= "); break;
         case xfunctions.GE: b = (" >= "); break;
    }
    if (this.opcode > xfunctions.OR && !(this.param1 instanceof xfunctions.ConditionalNode))
        a = this.param1.toString();
    else if (this.opcode >= xfunctions.AND || (this.param1 instanceof xfunctions.BinaryNode && ( this.param1.opcode < this.opcode || 
                                                  (this.param1.opcode == this.opcode && this.opcode == xfunctions.POWER) ) )
                                        || this.param1 instanceof xfunctions.UnaryNode
                                        || this.param1 instanceof xfunctions.ConditionalNode) {
        a = "(" + this.param1.toString() +")";
    }
    else
        a = this.param1.toString();
    if (this.opcode > xfunctions.OR && !(this.param2 instanceof xfunctions.ConditionalNode))
        c = this.param2.toString();
    else if ( this.opcode >= xfunctions.AND || ( (this.param2 instanceof xfunctions.BinaryNode && 
                (this.param2.opcode < this.opcode ||
                      (this.param2.opcode == this.opcode && (this.opcode == xfunctions.MINUS || this.opcode == xfunctions.DIVIDE || this.opcode == xfunctions.POWER))))
                               || (this.param2 instanceof xfunctions.UnaryNode)
                               || (this.param2 instanceof xfunctions.ConditionalNode)) ) {
        c = "(" + this.param2.toString() + ")";
    }
    else
        c = this.param2.toString();
    return a + b + c;
}
xfunctions.BinaryNode.prototype.value = function(casesArray) {
    if (this.opcode > xfunctions.POWER) {
        var ans;
        switch(this.opcode) {
            case xfunctions.AND:ans = this.param1.value() == 1 && this.param2.value() == 1;  break;
            case xfunctions.OR: ans = this.param1.value() == 1 || this.param2.value() == 1; break;
            case xfunctions.EQ: ans = this.param1.value() == this.param2.value(); break;
            case xfunctions.NE: ans = this.param1.value() != this.param2.value(); break;
            case xfunctions.LT: ans = this.param1.value() < this.param2.value(); break;
            case xfunctions.GT: ans = this.param1.value() > this.param2.value(); break;
            case xfunctions.LE: ans = this.param1.value() <= this.param2.value(); break;
            case xfunctions.GE: ans = this.param1.value() >= this.param2.value(); break;
            default: throw "Internal error???  illegal opcode in BinaryNode";
        }
        ans = ans? 1 : 0; 
        if (casesArray)
            casesArray.push(ans);
        return ans;
    }
    else {
        var ans;
        var x = this.param1.value(casesArray);
        var y = this.param2.value(casesArray);
        switch(this.opcode) {
            case xfunctions.PLUS: ans = x + y; break;
            case xfunctions.MINUS: ans = x - y; break;
            case xfunctions.TIMES: ans = x * y; break;
            case xfunctions.DIVIDE:
                if (casesArray)
                    casesArray.push( y > 0? 1 : 0 );
                ans = x / y;
                break;
            case xfunctions.POWER: ans = Math.pow(x,y); break;
        }
        return ans;
    }
}
xfunctions.BinaryNode.prototype.copy = function () {
    return new xfunctions.BinaryNode(this.opcode, this.param1.copy(), this.param2.copy());
}
xfunctions.BinaryNode.prototype.isConstant = function(wrtVariable) {
    return this.param1.isConstant(wrtVariable) && this.param2.isConstant(wrtVariable);
}
xfunctions.BinaryNode.prototype.derivative = function(wrtVariable) {
    var v = wrtVariable;
    var d1,d2;
    d1 = this.param1.derivative(v);
    d2 = this.param2.derivative(v);
    if (d1 instanceof xfunctions.ConstantNode && d1.value() == 0)
        d1 = null;
    if (d2 instanceof xfunctions.ConstantNode && d2.value() == 0)
        d2 = null;
    //if (this.param1.isConstant(v))
    //   d1 = null;
    //else
    //   d1 = this.param1.derivative(v);
    //if (this.param2.isConstant(v))
    //   d2 = null;
    //else
    //   d2 = this.param2.derivative(v);
    if (d1 == null && d2 == null)
       return new xfunctions.ConstantNode(0);
    switch (this.opcode) {
         case xfunctions.PLUS:
            if (d1 == null) 
               return d2;
            else if (d2 == null)
               return d1;
            else
               return new xfunctions.BinaryNode(xfunctions.PLUS,d1,d2);
         case xfunctions.MINUS:
            if (d1 == null)
               return new xfunctions.UnaryNode(xfunctions.UNARYMINUS,d2);
            else if (d2 == null)
               return d1;
            else
               return new xfunctions.BinaryNode(xfunctions.MINUS,d1,d2);
         case xfunctions.TIMES:
            if (d1 == null) {
               if (d2 instanceof xfunctions.ConstantNode && d2.value() == 1)
                  return this.param1.copy();
               else 
                  return new xfunctions.BinaryNode(xfunctions.TIMES,this.param1.copy(),d2);
            }
            else if (d2 == null) {
               if (d1 instanceof xfunctions.ConstantNode && d1.value() == 1)
                  return this.param2.copy();
               else
                 return new xfunctions.BinaryNode(xfunctions.TIMES,this.param2.copy(),d1);
            }
            else {
                var left = (d2 instanceof xfunctions.ConstantNode && d2.value() == 1) ? 
                                      this.param1.copy() :
                                         new xfunctions.BinaryNode(xfunctions.TIMES,this.param1.copy(),d2);
                var right = (d1 instanceof xfunctions.ConstantNode && d1.value() == 1) ? 
                                      this.param2.copy() :
                                         new xfunctions.BinaryNode(xfunctions.TIMES,d1,this.param2.copy());
                return new xfunctions.BinaryNode(xfunctions.PLUS,left,right);
            }
         case xfunctions.DIVIDE:
            if (d2 == null)
               return new xfunctions.BinaryNode(xfunctions.DIVIDE,d1,this.param2.copy());
            else if (d1 == null) {
               var d = new xfunctions.BinaryNode(xfunctions.DIVIDE,
                                      new xfunctions.UnaryNode(xfunctions.UNARYMINUS,this.param1.copy()),
                                      new xfunctions.BinaryNode(xfunctions.POWER,this.param2.copy(), new xfunctions.ConstantNode(2)));
               if (d2 instanceof xfunctions.ConstantNode && d2.value() == 1)
                  return d;
               else
                  return new xfunctions.BinaryNode(xfunctions.TIMES,d,d2);                                        
            }
            else {
               return new xfunctions.BinaryNode(xfunctions.DIVIDE,
                       new xfunctions.BinaryNode(xfunctions.MINUS,
                             new xfunctions.BinaryNode(xfunctions.TIMES,this.param2.copy(),d1),
                             new xfunctions.BinaryNode(xfunctions.TIMES,this.param1.copy(),d2)),
                       new xfunctions.BinaryNode(xfunctions.POWER,this.param2.copy(),new xfunctions.ConstantNode(2)));
            }
         case xfunctions.POWER:
            if (d1 == null) {
               var d;
               if (this.param1 instanceof xfunctions.ConstantNode && this.param1.value() == Math.E)
                  d = this.copy();
               else
                  d = new xfunctions.BinaryNode(xfunctions.TIMES,this.copy(),
                             new xfunctions.StandardFunctionNode(xfunctions.LN,this.param1.copy()));
               if (d2 instanceof xfunctions.ConstantNode && d2.value() == 1)
                  return d;
               else
                  return new xfunctions.BinaryNode(xfunctions.TIMES,d,d2);
            }
            else if (d2 == null) {
               var d;
               if (this.param2 instanceof xfunctions.ConstantNode) {
                  if (this.param2.value() == 0)
                     return new xfunctions.ConstantNode(0);
                  else if (this.param2.value() == 1) 
                     return d1;
                  else if (this.param2.value() == 2) 
                     d = new xfunctions.BinaryNode(xfunctions.TIMES,this.param2.copy(),this.param1.copy());
                  else
                     d = new xfunctions.BinaryNode(xfunctions.TIMES, this.param2.copy(),
                           new xfunctions.BinaryNode(xfunctions.POWER, this.param1.copy(), new xfunctions.ConstantNode(this.param2.value() - 1)));
               }
               else
                  d = new xfunctions.BinaryNode(xfunctions.TIMES, this.param2.copy(),
                           new xfunctions.BinaryNode(xfunctions.POWER, this.param1.copy(),
                           new xfunctions.BinaryNode(xfunctions.MINUS, this.param2.copy(), new xfunctions.ConstantNode(1))));
               if (d1 instanceof xfunctions.ConstantNode && d1.value() == 1)
                  return d;
               else
                  return new xfunctions.BinaryNode(xfunctions.TIMES,d,d1);
            }
            else {
               return new xfunctions.BinaryNode(xfunctions.TIMES, this.copy(),
                  new xfunctions.BinaryNode(xfunctions.PLUS,
                        new xfunctions.BinaryNode(xfunctions.TIMES,
                              this.param2.copy(),
                              new xfunctions.BinaryNode(xfunctions.DIVIDE,d1,this.param1.copy())),
                        new xfunctions.BinaryNode(xfunctions.TIMES,
                               new xfunctions.StandardFunctionNode(xfunctions.LN,this.param1.copy()),
                               d2)));
            }
         default:
            throw "Attempt to take the derivative of a logical condition.";
      }
}

xfunctions.ConditionalNode = function(cond, trueCase, falseCase) {
    if (! cond || !(cond instanceof xfunctions.Expression) || !trueCase || !(trueCase instanceof xfunctions.Expression) || 
           (falseCase != null && !(falseCase instanceof xfunctions.Expression))) {
        throw "illegal parameter to ConditionalNode constructor";
    }
    this.cond = cond;
    this.trueCase = trueCase;
    this.falseCase = falseCase;
}
xfunctions.ConditionalNode.prototype = new xfunctions.Expression();
xfunctions.ConditionalNode.prototype.constructor = xfunctions.ConditionalNode;
xfunctions.ConditionalNode.prototype.toString = function() {
    var s = "(" + this.cond.toString() + ") ? " ;
    if (this.trueCase instanceof xfunctions.ConditionalNode)
        s += "(" + this.trueCase.toString() + ")";
    else
        s += this.trueCase.toString();
    if (this.falseCase)
        s += " : " + this.falseCase.toString();
    return s;
}
xfunctions.ConditionalNode.prototype.value = function(casesArray) {
    var c;
    if ( this.cond.value() != 0 ) {
        c = 1;
    }
    else if (this.falseCase) {
        c = 2;
    }
    else {
        c = 3;
    }
    if (casesArray)
        casesArray.push(c);
    if (c == 1)
        return this.trueCase.value(casesArray);
    else if (c == 2)
        return this.falseCase.value(casesArray);
    else
        return NaN;
}
xfunctions.ConditionalNode.prototype.isConstant = function(wrtVariable) {
    return this.cond.isConstant(wrtVariable) && this.trueCase.isConstant(wrtVariable) &&
                (this.falseCase == null || this.falseCase.isConstant(wrtVariable));
}
xfunctions.ConditionalNode.prototype.copy = function() {
    return new xfunctions.ConditionalNode(this.cond.copy(), this.trueCase.copy(), this.falseCase == null ? null : this.falseCase.copy());
}
xfunctions.ConditionalNode.prototype.derivative = function(wrtVariable) {
    if (this.isConstant(wrtVariable))
        return new xfunctions.ConstantNode(0);
    return new xfunctions.ConditionalNode(this.cond.copy(), this.trueCase.derivative(wrtVariable),
                                          this.falseCase == null ? null : this.falseCase.derivative(wrtVariable));
}



//----------------- PARSER ----------------------------------------------------

xfunctions.ParseError = function(message,position,dataString) {
    this.message = message;
    this.position = position;
    this.dotaString = dataString;
}
xfunctions.ParseError.prototype.toString = function() {
    return "ParseError at position " + this.position + ": " + this.message;
}

xfunctions.Parser = function(parent) {
    if (!parent) {
        this.symbols = new xfunctions.SymbolTable();
    }
    else {
        this.symbols = new xfunctions.SymbolTable(parent.symbols);
    }
}
xfunctions.Parser.prototype.defineVariable = function(name) {
    var x = new xfunctions.Variable(name,0);
    this.symbols.addMathObject(x); 
    return x;
}
xfunctions.Parser.prototype.defineFunction = function (name,arg,def) {
    if (typeof arg == "string") {
        arg = [arg];        
    }
    var v = new Array(arg.length);
    var parser = new xfunctions.Parser(this);
    for (var i = 0; i < arg.length; i++) {
        v[i] = parser.defineVariable(arg[i]);
    }
    var d =  parser.parse(def);
    var f = new xfunctions.ExpressionFunction(name,v,d);
    this.symbols.addMathObject( f );
    return f;
}
xfunctions.Parser.prototype.parseLogicalExpression = function(string) {
    var exp = this.parse(string);
    if (!exp.isLogical())
       throw new xfunctions.ParseError("Found a numeric expression while looking for a logical expression.",string.length,string);
    return exp;
}
xfunctions.Parser.prototype.parseNumericExpression = function(string) {
    var exp = this.parse(string);
    if (exp.isLogical())
       throw new xfunctions.ParseError("Found a logical expression while looking for a numeric expression.",string.length,string);
    return exp;
}
xfunctions.Parser.prototype.parse = function(string) {
    if ( !string || typeof string != "string")
        throw new xfunctions.ParseError("The input to a parser must be a string.", 0, "");
    var pos = 0;
    var wordRegex = /^[A-Za-z_][A-Za-z0-9_]*/g;
    var numberRegex = /^(\d+\.?\d*|\.\d+)((e|E)(\+|\-)?\d+)?/g;
    var parser = this;
    var token;
    var ttype = null;
    var EOS = 1;
    var NUMBER = 2;
    var WORD = 3;
    var UNKNOWNWORD = 4;
    var OPERATOR = 5;
    var CHAR = 6;
    function peekCh() {
        if (pos >= string.length)
            return null;
        else
            return string.charAt(pos);
    }
    function getCh() { 
        if (pos >= string.length)
            return null;
        else
            return string.charAt(pos++);
    }
    function peek() {
        if (ttype == null)
            readToken();
        return ttype;
    }
    function get() {
        if (ttype == null)
            readToken();
        var t = ttype;
        ttype = null;
        return t;
    }
    function readToken() {
        while (peekCh() == " " || peekCh() == "\t") {
           getCh();
        }
        var ch = peekCh();
        wordRegex.lastIndex = 0;
        numberRegex.lastIndex = 0;
        var word = wordRegex.exec(string.substring(pos));
        var num = numberRegex.exec(string.substring(pos));
        if (ch == null) {
            ttype = EOS;
            token = "";
            getCh();
        }
        else if (word) {
            token = word[0];
            pos += wordRegex.lastIndex;
            if (parser.symbols.get(token) != null)
                ttype = WORD;
            else if (token.toLowerCase() == "or") {
                ttype = OPERATOR;
                token = "|";
            }
            else if (token.toLowerCase() == "and") {
                ttype = OPERATOR;
                token = "&";
            }
            else if (token.toLowerCase() == "not") {
                ttype = OPERATOR;
                token = "!";
            }
            else {
                ttype = UNKNOWNWORD;
            }
        }
        else if (num) {
            ttype = NUMBER;
            token = num[0];
            pos += numberRegex.lastIndex;
        }
        else if (ch.match(/[+*/=<>^&|!?:-]/)) {
            ttype = OPERATOR;
            token = ch;
            getCh();
            if (ch == "!" && peekCh() == "=" ) {
                token += getCh();
            }
            else if ( (ch == "<" || ch == '>') && peekCh() == "=" ) {
                token += getCh();
            }
            else if ( ch == "=" || ch == "&" || ch == "|" ) {
                if (peekCh() == ch)
                    getCh();
            }
        }
        else {
            token = getCh();
            ttype = CHAR;
        }
        //console.log("read token " + ttype + " " + token);
    }
    function parseExpression() {
        var cond = parseLogicalExpression();
        var tok = peek();
        if (ttype == OPERATOR && token == "?") {
            if (!cond.isLogical())
                throw new xfunctions.ParseError("'?' operator can only be used after a logical expression.", pos, string);
            get();
            var trueCase, falseCase;
            trueCase = parseNumericExpression();
            if (trueCase.isLogical())
                throw new xfunctions.ParseError("'?' operator must be followed by a numeric expression, not a logical expression.", pos, string);
            tok = peek();
            if (ttype == OPERATOR && token == ":") {
                get();
                falseCase = parseExpression();
                if (falseCase.isLogical())
                    throw new xfunctions.ParseError("':' operator must be followed by a numeric expression, not a logical expression.", pos, string);
            }
            else {
                falseCase = null;
            }
            return new xfunctions.ConditionalNode(cond, trueCase, falseCase);
        }
        else {
            return cond;
        }
    }
    function parseLogicalExpression() {
       var term = parseLogicalTerm();
       var tok = peek();
       while (ttype == OPERATOR && token == ("|")) {
          if (!term.isLogical())
             throw new xfunctions.ParseError("'OR' operator requires logical operands.");
          get();
          var next = parseLogicalTerm();
          if (!next.isLogical())
             throw new xfunctions.ParseError("'OR' operator requires logical operands.");
          term = new xfunctions.BinaryNode(xfunctions.OR,term,next);
          tok = peek();
       }
       return term;
    }
    function parseLogicalTerm() {
       var factor = parseLogicalFactor();
       var tok = peek();
       while (ttype == OPERATOR && token == ("&")) {
          if (!factor.isLogical())
             throw new xfunctions.ParseError("'AND' operator requires logical operands.");
          get();
          var next = parseLogicalFactor();
          if (!next.isLogical())
             throw new xfunctions.ParseError("'AND' operator requires logical operands.");
          factor = new xfunctions.BinaryNode(xfunctions.AND,factor,next);
          tok = peek();
       }
       return factor;
    }
    function parseLogicalFactor() {
        var tok = peek();
        var notCt = 0;
        while (ttype == OPERATOR && token == ("!")) {
           get();
           tok = peek();
           notCt++;
        }
        var rel = parseRelation();
        if (notCt > 0 && !rel.isLogical())
             throw new xfunctions.ParseError("'NOT' operator requires logical operands.");
        if (notCt % 2 == 1)
           rel =  new xfunctions.UnaryNode(xfunctions.NOT,rel);
        return rel;
    }
    function parseRelation() {
        var tok = peek();
        var left = parseNumericExpression();
        tok = peek();
        if (ttype == OPERATOR &&  (token == ("=") || token == ("<") || token == (">") || 
                                          token == ("<=") || token == (">=") || token == ("!=")) ) {
            if (left.isLogical())
                throw new xfunctions.ParseError(token + " requires numeric operands", pos, string);
            get();
            var rel = 0;
            if (token == ("="))
               rel = xfunctions.EQ;
            else if (token == ("<"))
               rel = xfunctions.LT;
            else if (token == (">"))
               rel = xfunctions.GT;
            else if (token == ("<="))
               rel = xfunctions.LE;
            else if (token == (">="))
               rel = xfunctions.GE;
            else if (token == ("!="))
               rel = xfunctions.NE;
            var right = parseNumericExpression();
            if (right.isLogical())
                throw new xfunctions.ParseError(token + " requires numeric operands", pos, string);
            tok = peek();
            if (ttype == OPERATOR && (token == ("=") || token == ("<") || token == (">") || 
                                              token == ("<=") || token == (">=") || token == ("!=")))
               throw new xfunctions.ParseError("It is illegal to string together relations operators; use \"AND\" instead.",pos,string);
            return new xfunctions.BinaryNode(rel,left,right);
        }
        else
            return left;
    }
    function parseNumericExpression() {
       var neg = false;
       var tok = peek();
       if (ttype == OPERATOR && (token == ("+") || token == ("-"))) {
          get();
          neg = (token == ("-"));
       }
       var term = parseTerm();
       if (neg) {
          if (term.isLogical())
             throw new xfunctions.ParseError("'-' operator cannot be used with logical operand", pos, string);
          term = new xfunctions.UnaryNode(xfunctions.UNARYMINUS,term);
       }
       tok = peek();
       while (ttype == OPERATOR && (token == ("+") || token == ("-"))) {
          if (term.isLogical())
             throw new xfunctions.ParseError("'" + token + "' operator cannot be used with logical operand", pos, string);
          get();
          var opcode = (token == ("+")? xfunctions.PLUS : xfunctions.MINUS);
          var next = parseTerm();
          if (next.isLogical())
             throw new xfunctions.ParseError("'" + token + "' operator cannot be used with logical operand", pos, string);
          term = new xfunctions.BinaryNode(opcode,term,next);
          tok = peek();
       }
       return term;
    }
    function parseTerm() {
       var primary = parsePrimary();
       var tok = peek();
       while (ttype == OPERATOR && (token == ("*") || token == ("/"))) {
          if (primary.isLogical())
             throw new xfunctions.ParseError("'" + token + "' operator cannot be used with logical operand", pos, string);
          get();
          var opcode = (token == ("*")? xfunctions.TIMES : xfunctions.DIVIDE);
          var next = parsePrimary();
          if (next.isLogical())
             throw new xfunctions.ParseError("'" + token + "' operator cannot be used with logical operand", pos, string);
          primary = new xfunctions.BinaryNode(opcode,primary,next);
          tok = peek();
       }
       return primary;
    }
    function parsePrimary() {
       var factor = parseFactor();
       var tok = peek();
       while (tok == OPERATOR && token == ("^")) {
          if (factor.isLogical())
             throw new xfunctions.ParseError("'" + token + "' operator cannot be used with logical operand", pos, string);
          get();
          var next = parseFactor();
          if (next.isLogical())
             throw new xfunctions.ParseError("'" + token + "' operator cannot be used with logical operand", pos, string);
          factor = new xfunctions.BinaryNode(xfunctions.POWER,factor,next);
          tok = peek();
       }
       return factor;
    }
    function parseFactor() {
       var tok = get();
       if (tok == NUMBER)
          return parseNumber();
       else if (tok == WORD)
          return parseWord();
       else if (tok == UNKNOWNWORD)
          throw new xfunctions.ParseError("Unknown name (" + token + ") encountered.",pos,string);      
       else if (tok == CHAR && token == ("(")) {
          var e = parseExpression();
          tok = peek();
          if (tok == CHAR && token == (")"))
             get();
          else
             throw new xfunctions.ParseError("Missing right parenthesis.",pos,string);
          return e;
       }
       else if (tok == EOS)
          throw new xfunctions.ParseError("Data ended in the middle of an incomplete expression.",pos,string);
       else if (tok == OPERATOR)
          throw new xfunctions.ParseError("Misplaced operator.", pos, string);
       else if (tok == CHAR && token == (")"))
          throw new xfunctions.ParseError("Misplaced right parenthesis with no matching left parenthesis.", pos, string);
       else if (tok == CHAR)
          throw new xfunctions.ParseError("Unexpected character \"" + token + "\"", pos, string);
       else
          throw new xfunctions.ParseError("Internal program error??? Unknown token type.", pos, string);
    }
    function parseNumber() {
          var d = Number(token);
          if (isNaN(d))
             throw new xfunctions.ParseError("Illegal number \"" + token + "\"",pos,string);
          return new xfunctions.ConstantNode(d);
    }
    function parseWord() {
       var s = parser.symbols.get(token);
       if (s == null)
          throw new xfunctions.ParseError("Internal program error??? null symbol.",pos,string);
       else if (s instanceof xfunctions.Variable)
          return new xfunctions.VariableNode(s);
       else if (s instanceof xfunctions.Constant)
          return new xfunctions.ConstantNode(s);
       else if (s instanceof xfunctions.Function)
          return parseFunction(s);
       else if (s instanceof xfunctions.StandardFunction)
          return parseStandardFunction(s); 
       else
          throw new xfunctions.ParseError("Internal program error??? Unknown type of symbol.",pos,string);
    }
    function parseFunction(f) {
       if (f.rangeDimension > 1)
           throw new xfunctions.ParseError("Functions with range dimension greater than 1 not allowed in expressions.", pos, string);
       var params = parseParameterList(f.domainDimension);
       return new xfunctions.FunctionNode(f,params);
    }
    function parseStandardFunction(f) {
       var params = parseParameterList(1);
       return new xfunctions.StandardFunctionNode(f.getID(),params[0]);
    }
    function parseParameterList(arity) {
       var params = new Array(arity);
       var tok = get();
       if (tok != CHAR || token != ("("))
          throw new xfunctions.ParseError("Missing left parenthesis after function name.",pos,string);
       var i = 0;
       while(i < arity) {
          params[i] = parseExpression();
          if (params[i].isLogical())
              throw new xfunctions.ParseError("Function argument cannot be a logical expression.", pos, string);
          tok = get();
          if (i < arity - 1) {
             if (tok != CHAR || token != (","))
                throw new xfunctions.ParseError("Expected a comma followed by another parameter.",pos,string);
          }
          else {
             if (tok != CHAR || token != (")"))
                throw new xfunctions.ParseError("Expected right parenthesis to end parameter list.",pos,string);
          }
          i++;
       }
       return params;
    }
    var expr = parseExpression();
    if (peek() != EOS)
        throw new xfunctions.ParseError("Input contains extra characters after a complete expression.",pos,string);
    return expr;
}

//----------------------------------- from xfunctions-draw.js --------------------------------------

xfunctions.CoordinateRect = function(xmin, xmax, ymin, ymax) {
    this.setLimits(xmin,xmax,ymin,ymax);
    this.left = 0;
    this.top = 0;
    this.width = -1;
    this.height = -1;
    this.gap = 5;
    this.version = 1;
    this.backgroundColor = "white";
    this.borderWidth = 0;
    this.borderColor = "#666";
    this.drawItems = [];
    this.dragRect = null;
}
xfunctions.CoordinateRect.prototype.setLimits = function(xmin,xmax,ymin,ymax){
    if (xmin == undefined || isNaN(Number(xmin)))
        xmin = -5;
    if (xmax == undefined || isNaN(Number(xmax)))
        xmax = 5;
    if (ymin == undefined || isNaN(Number(ymin)))
        ymin = -5;
    if (ymax == undefined || isNaN(Number(ymax)))
        ymax = 5;
    if (xmin > xmax) {
        var t = xmin;
        xmin = xmax;
        xmax = t;
    }
    if (xmin == xmax) {
        xmin -= 1;
        xmax += 1;
    }
    if (ymin > ymax) {
        var t = ymin;
        ymin = ymax;
        xmax = t;
    }
    if (ymin == ymax) {
        ymin -= 1;
        ymax += 1;
    }
    if (this.xmin != xmin || this.xmax != xmax || this.ymin != ymin || this.ymax != ymax) {
        this.version++;
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
    }
}
xfunctions.CoordinateRect.prototype.getLimits = function() {
    return [this.xmin, this.xmax, this.ymin, this.ymax];
}
xfunctions.CoordinateRect.prototype.setGap = function(gap) {
    gap = gap || 5;
    if (this.gap != gap) {
        this.version++;
        this.gap = gap;
    }
}
xfunctions.CoordinateRect.prototype.zoom = function(factor,xCenter,yCenter) {
    xCenter = xCenter || (this.xmin + this.xmax)/2;
    yCenter = yCenter || (this.ymin + this.ymax)/2;
    factor = factor || 2;
    var newwidth = (this.xmax - this.xmin) / factor;
    var newheight = (this.ymax - this.ymin) / factor;
    this.xmin = xCenter - (xCenter - this.ymin) / (this.xmax - this.xmin);
    this.xmax = this.sxmin + newwidth;
    this.ymin = yCenter - (yCenter - this.ymin) / (this.ymax - this.ymin);
    this.ymax = this.ymin + newheight;
    this.version++;
}
xfunctions.CoordinateRect.prototype.equalizeAxes = function() {
      var w = this.xmax - this.xmin;
      var h = this.ymax - this.ymin;
      var pixelWidth = w / (this.width - 2*this.gap - 1);
      var pixelHeight = h / (this.height - 2*this.gap - 1);
      if (pixelWidth < pixelHeight) {
         var centerx = (this.xmax + this.xmin) / 2;
         var halfwidth = w/2 * pixelHeight/pixelWidth;
         this.xmax = centerx + halfwidth;
         this.xmin = centerx - halfwidth;
      }
      else if (pixelWidth > pixelHeight) {
         var centery = (this.ymax + this.ymin) / 2;
         var halfheight = h/2 * pixelWidth/pixelHeight;
         this.ymax = centery + halfheight;
         this.ymin = centery - halfheight;
      }
      else
         return null;
      this.version++;
      return this.getLimits();      
}
xfunctions.CoordinateRect.prototype.screenToCoordX = function(xint) {
    return this.xmin + (xint - (this.left + this.gap))/(this.width - 2*this.gap)*(this.xmax-this.xmin);
}
xfunctions.CoordinateRect.prototype.screenToCoordY = function(yint) {
    return this.ymax - ((this.top - this.gap) - yint)/(this.height - 2*this.gap)*(this.ymax-this.ymin);
}
xfunctions.CoordinateRect.prototype.coordToScreenX = function(x) {
    return this.left + this.gap + (x-this.xmin)/(this.xmax-this.xmin)*(this.width-2*this.gap);
}
xfunctions.CoordinateRect.prototype.coordToScreenY = function(y) {
    return this.top + this.gap + (this.ymax - y)/(this.ymax-this.ymin)*(this.height-2*this.gap);
}
xfunctions.CoordinateRect.prototype.pixelWidth = function() {
    return (this.xmax - this.xmin) /  (this.width - 2*this.gap - 1);
}
xfunctions.CoordinateRect.prototype.pixelHeight = function() {
    return (this.ymax - this.ymin) / (this.height - 2*this.gap - 1);
}
xfunctions.CoordinateRect.prototype.add = function(drawable) {
    this.drawItems.push(drawable);
}
xfunctions.CoordinateRect.prototype.clear = function() {
    this.drawItems = [];
}
xfunctions.CoordinateRect.prototype.remove = function(drawable) {
    for (var i = 0; i < this.drawItems.length; i++) {
        if (this.drawItems[i] == drawable) {
            this.drawItems.splice(i,1);
            return;
        }
    }
}
xfunctions.CoordinateRect.prototype.clear = function() {
    this.drawItems = [];
}
xfunctions.CoordinateRect.prototype.draw = function(graphics, left, top, width, height) {
    if (this.left != left || this.top != top || this.width != width || this.height != height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.version++;
    }
    graphics.fillStyle = this.backgroundColor;
    graphics.fillRect(left,top,width,height);
    if (this.borderWidth > 0) {
        graphics.fillStyle = this.borderColor;
        graphics.fillRect(this.left,this.top,this.width,this.borderWidth);
        graphics.fillRect(this.left,this.top,this.borderWidth,this.height);
        graphics.fillRect(this.left,this.top+this.height-this.borderWidth,this.width,this.borderWidth);
        graphics.fillRect(this.left+this.width-this.borderWidth,this.top,this.borderWidth,this.height);
    }
    for (var i = 0; i < this.drawItems.length; i++) {
        this.drawItems[i].prepare(graphics,this);
    }
    this.doDraw(graphics);
}
xfunctions.CoordinateRect.prototype.doDraw = function(graphics) {
    for (var i = 0; i < this.drawItems.length; i++) {
        this.drawItems[i].draw(graphics,this);
    }
}

xfunctions.Graph1D = function(expression, variable) {
    this.expression = expression;
    this.variable = variable;
    this.lastVersion = 0;
    this.points = null;
    this.color = "#A0A";
    this.lineWidth = 1.5;
    var myExpression = expression;
    var myVariable = variable;
    this.getExpression = function() { return myExpression };
    this.setExpression = function(expression) { myExpression = expression; this.points = null; };
    this.getVariable = function() { return myVariable };
    this.setVariable = function(variable) { myVariable = variable; this.points = null; };
}
xfunctions.Graph1D.prototype.draw = function(graphics,coords) {
    if ( ! this.points || this.points.length < 2 )
        return;
    graphics.strokeStyle = this.color;
    graphics.lineWidth = this.lineWidth;
    graphics.beginPath();
    var jump = true;
    for (var i = 0; i < this.points.length; i++) {
        var pt = this.points[i];
        if ( pt ) {
            if (jump) {
               graphics.moveTo(pt[0],pt[1]);
            }
            else {
               graphics.lineTo(pt[0],pt[1]);
            }
        }
        jump = ! pt;
    }
    graphics.stroke();
}
xfunctions.Graph1D.prototype.prepare = function(graphics,coords) { 
    var points = [];
    var expression = this.getExpression();
    var variable = this.getVariable();
    if ( ! expression || ! variable )
        return;
    var onscreenymax, onscreenymin;
    var UNDEFINED = 0, ABOVE = 1, BELOW = 2, ONSCREEN = 3;
    if (this.points && this.lastVersion == coords.version)
        return;
    this.lastVersion = coords.version;
    var dy = coords.pixelHeight();
    onscreenymax = coords.ymax + dy*150;
    onscreenymin = coords.ymin - dy*150;
    var prevx, prevy, x, y, lastx, limitx, pixelx, pixely, cases1, cases2,
               status, prevstatus, xHoldOffscreen, yHoldOffscreen, statusHoldOffscreen;
    pixelx = coords.left;
    xHoldOffscreen = null;
    limitx = coords.left + coords.width;
    cases2 = [];
       x = coords.screenToCoordX(pixelx);
       variable.setValue(x);
       y = expression.value(cases2);
       status = getStatus(y);
       if (status == ONSCREEN) {
          points.push([coords.coordToScreenX(x),coords.coordToScreenY(y)]);
       }
       else if (status != UNDEFINED) {
          xHoldOffscreen = x;
          yHoldOffscreen = y;
          statusHoldOffscreen = status;
       }
       while (pixelx < limitx) {
          prevx = x;
          prevy = y;
          prevstatus = status;
          cases1 = cases2;
          cases2 = [];
          pixelx++;
          //pixelx += 4;
          //if (pixelx > limitx)
          //   pixelx = limitx;
          x = coords.screenToCoordX(pixelx);
          variable.setValue(x);
          y = expression.value(cases2);
          status = getStatus(y);
          if (status == UNDEFINED) {
             if (prevstatus != UNDEFINED) {
                if (prevstatus == ONSCREEN)
                   domainEndpoint(prevx,x,prevy,y,prevstatus,status,cases1,cases2,1);
                else if (xHoldOffscreen)
                   points.push([coords.coordToScreenX(xHoldOffscreen),coords.coordToScreenY(yHoldOffscreen)]);
                xHoldOffscreen = null;
                points.push(null);
             }
          }
          else if (prevstatus == UNDEFINED) {
             if (status == ONSCREEN) {
                domainEndpoint(prevx,x,prevy,y,prevstatus,status,cases1,cases2,1);
                points.push([coords.coordToScreenX(x),coords.coordToScreenY(y)]);
                xHoldOffscreen = null;
             }
             else {// note: status != UNDEFINED
                 xHoldOffscreen = x;
                 yHoldOffscreen = y;
                 statusHoldOffscreen = status;
             }
          }
          else if (xfunctions.sameCases(cases1,cases2)) {
             if (status == ONSCREEN) {
                if (xHoldOffscreen) {
                   points.push([coords.coordToScreenX(xHoldOffscreen),coords.coordToScreenY(yHoldOffscreen)]);
                   xHoldOffscreen = null;
                }
                points.push([coords.coordToScreenX(x),coords.coordToScreenY(y)]);                
             }
             else {
                if (xHoldOffscreen) {
                   if (status != statusHoldOffscreen) { // one ABOVE, one BELOW
                      points.push([coords.coordToScreenX(xHoldOffscreen),coords.coordToScreenY(yHoldOffscreen)]);
                      points.push([coords.coordToScreenX(x),coords.coordToScreenY(y)]);
                      points.push(null);
                   }
                }
                else
                   points.push([coords.coordToScreenX(x),coords.coordToScreenY(y)]); // first jump to offscreen
                xHoldOffscreen = x;
                yHoldOffscreen = y;
                statusHoldOffscreen = status;
             }
          }
          else {  // discontinuity
             if (prevstatus == ABOVE || prevstatus == BELOW) {
                if (status == prevstatus) {
                    if (xHoldOffscreen) { // should be false
                       points.push([coords.coordToScreenX(xHoldOffscreen),coords.coordToScreenY(yHoldOffscreen)]);
                       points.push(null);
                    }
                    xHoldOffscreen = x;          // don't worry about offscreen discontinuity
                    yHoldOffscreen = y;
                    statusHoldOffscreen = status;
                }
                else if (status == ONSCREEN) {  // possible visible discontinuity
                   if (xHoldOffscreen) {
                      points.push([coords.coordToScreenX(xHoldOffscreen),coords.coordToScreenY(yHoldOffscreen)]);
                      xHoldOffscreen = null;
                   }
                   discontinuity(prevx,x,prevy,y,prevstatus,status,cases1,cases2,1);
                   points.push([coords.coordToScreenX(x),coords.coordToScreenY(y)]);
                }
                else {  // status == ABOVE or BELOW, opposit to prevstatus; just do a jump
                   if (xHoldOffscreen)
                      points.push([coords.coordToScreenX(xHoldOffscreen),coords.coordToScreenY(yHoldOffscreen)]);
                   points.push(null);
                   xHoldOffscreen = x;
                   yHoldOffscreen = y;
                   statusHoldOffscreen = status;
                }
             }
             else {  // prevstatus is ONSCREEN; possible visible discontinuity
                 discontinuity(prevx,x,prevy,y,prevstatus,status,cases1,cases2,1);
                 if (status == ONSCREEN) {
                    points.push([coords.coordToScreenX(x),coords.coordToScreenY(y)]);
                    xHoldOffscreen = null;
                 }
                 else {
                    xHoldOffscreen = x;
                    yHoldOffscreen = y;
                    statusHoldOffscreen = status;
                 }
             }
          }
       }  // end while (pixel < limitx)
       if (points.length > 1)
           this.points = points;
       else 
           this.points = null;
    function getStatus(y) {
        if (isNaN(y) || y == Infinity || y == -Infinity)
           return UNDEFINED;
        else if (y > onscreenymax)
           return ABOVE;
        else if (y < onscreenymin)
           return BELOW;
        else
           return ONSCREEN;
    }
    function discontinuity(x1,x2,y1,y2,status1,status2,cases1,cases2,depth) {
         if (depth == 8) {
            points.push([coords.coordToScreenX(x1),coords.coordToScreenY(y1)]);
            points.push(null);
            points.push([coords.coordToScreenX(x2),coords.coordToScreenY(y2)]);
        }
        else {
           var xmid = (x1+x2)/2.0;
           var cases3 = [];
           variable.setValue(xmid);
           var ymid = expression.value(cases3);
           var samecases1 = xfunctions.sameCases(cases1,cases3);
           var samecases2 = xfunctions.sameCases(cases3,cases2);
           var statusmid = getStatus(ymid);
           if (statusmid == UNDEFINED) { // hope it doesn't happen
              if (status1 == ONSCREEN) 
                 domainEndpoint(x1,xmid,y1,ymid,status1,statusmid,cases1,cases3,1);
              points.push(null);
              if (status2 == ONSCREEN)
                 domainEndpoint(xmid,x2,ymid,y2,statusmid,status2,cases3,cases2,1);
           }
           else if ( ! samecases1 ) {
              discontinuity(x1,xmid,y1,ymid,status1,statusmid,cases1,cases3,depth+1);
              if ( ! samecases2 ) // double discontinuity
                 discontinuity(xmid,x2,ymid,y2,statusmid,status2,cases3,cases2,depth+1);
           }
           else if ( ! samecases2 )
              discontinuity(xmid,x2,ymid,y2,statusmid,status2,cases3,cases2,depth+1);
           else
              throw "Impossible error?  no discontinuity found in discontinuity for " + x1 + ',' + x2;
        }       
    }
    function domainEndpoint(x1,x2,y1,y2,status1,status2,cases1,cases2,depth) {
       if (depth == 15) {
           if (status1 == ONSCREEN)
              points.push([coords.coordToScreenX(x1),coords.coordToScreenY(y1)]);
           else  // status2 == ONSCREEN
              points.push([coords.coordToScreenX(x2),coords.coordToScreenY(y2)]);
       }
       else {
          var xmid = (x1+x2)/2.0;
          var cases3 = [];
          variable.setValue(xmid);
          var ymid = expression.value(cases3);
          var statusmid = getStatus(ymid);
          if (statusmid == ABOVE || statusmid == BELOW)
             points.push([coords.coordToScreenX(xmid),coords.coordToScreenY(ymid)]);
          else if (statusmid == status1) // statusmid is ONSCREEN or UNDEFINED
             domainEndpoint(xmid,x2,ymid,y2,statusmid,status2,cases3,cases2,depth+1);
          else
             domainEndpoint(x1,xmid,y1,ymid,status1,statusmid,cases1,cases3,depth+1);
       }        
    }
}


xfunctions.Axes = function(xLabel, yLabel) {
    this.axesColor = "#009";
    this.lightAxesColor = "#88E";
    this.labelColor = "red";
    this.xAxisPosition = xfunctions.Axes.SMART;
    this.yAxisPosition = xfunctions.Axes.SMART;
    this.xLabel = xLabel || null;
    this.yLabel = yLabel || null;
    this.data = null;
    this.lastVersion = 0;
}
xfunctions.Axes.TOP = 0;
xfunctions.Axes.BOTTOM = 1;
xfunctions.Axes.LEFT = 2;
xfunctions.Axes.RIGHT = 3;
xfunctions.Axes.CENTER = 4;
xfunctions.Axes.SMART = 5;
xfunctions.Axes.prototype.setXAxisPosition = function(p) {
    if ( this.xAxisPosition != p && (p == xfunctions.Axes.TOP || p == xfunctions.Axes.BOTTOM ||
           p == xfunctions.Axes.SMART || p == xfunctions.Axes.CENTER) ) {
        this.data = null;
        this.xAxisPosition = p;
    }
}
xfunctions.Axes.prototype.setYAxisPosition = function(p) {
    if ( this.yAxisPosition != p && (p == xfunctions.Axes.LEFT || p == xfunctions.Axes.RIGHT ||
           p == xfunctions.Axes.SMART || p == xfunctions.Axes.CENTER) ) {
        this.data = null;
        this.yAxisPosition = p;
    }
}
xfunctions.Axes.prototype.draw = function(graphics,coords) {
      if (this.data == null)
         return;
      var i,a,b;
      var data = this.data;
      graphics.lineWidth = 1;
      graphics.font = "10px serif";
      graphics.beginPath();
      if (this.xAxisPosition == xfunctions.Axes.SMART && (coords.ymax < 0 || coords.ymin > 0)) {
         graphics.strokeStyle = this.lightAxesColor;
         graphics.fillStyle = this.lightAxesColor;
      }
      else {
         graphics.strokeStyle = this.axesColor;
         graphics.fillStyle = this.axesColor;
      }
      graphics.moveTo(coords.left+coords.gap, data.xAxisPixelPosition);
      graphics.lineTo(coords.left+coords.width-coords.gap, data.xAxisPixelPosition);
      for (i = 0; i < data.xTicks.length; i++) {
         a = (data.xAxisPixelPosition - 2 < coords.top) ? data.xAxisPixelPosition : data.xAxisPixelPosition - 2;
         b = (data.xAxisPixelPosition + 2 >= coords.top + coords.height)? data.xAxisPixelPosition : data.xAxisPixelPosition + 2;
         graphics.moveTo(data.xTicks[i], a);
         graphics.lineTo(data.xTicks[i], b);
      }
      graphics.stroke();
      for (i = 0; i < data.xTickLabels.length; i++) {
         graphics.fillText(data.xTickLabels[i], data.xTickLabelPos[i][0], data.xTickLabelPos[i][1]);
      }
      if (this.yAxisPosition == xfunctions.Axes.SMART && (coords.xmax < 0 || coords.xmin > 0)) {
         graphics.strokeStyle = this.lightAxesColor;
         graphics.fillStyle = this.lightAxesColor;
      }
      else {
         graphics.strokeStyle = this.axesColor;
         graphics.fillStyle = this.axesColor;
      }
      graphics.beginPath();
      graphics.moveTo(data.yAxisPixelPosition, coords.top + coords.gap);
      graphics.lineTo(data.yAxisPixelPosition, coords.top + coords.height - coords.gap - 1);
      for (i = 0; i < data.yTicks.length; i++) {
         a = (data.yAxisPixelPosition - 2 < coords.left) ? data.yAxisPixelPosition : data.yAxisPixelPosition - 2;
         b = (data.yAxisPixelPosition + 2 >= coords.left + coords.width)? data.yAxisPixelPosition : data.yAxisPixelPosition + 2; 
         graphics.moveTo(a, data.yTicks[i]);
         graphics.lineTo(b, data.yTicks[i]);
      }
      graphics.stroke();
      for (i = 0; i < data.yTickLabels.length; i++)
         graphics.fillText(data.yTickLabels[i], data.yTickLabelPos[i][0], data.yTickLabelPos[i][1]);
      graphics.fillStyle = this.labelColor;
      graphics.font = "12px serif";
      if (this.xLabel != null)
         graphics.fillText(this.xLabel, data.xLabel_x, data.xLabel_y);
      if (this.yLabel != null)
         graphics.fillText(this.yLabel, data.yLabel_x, data.yLabel_y);
}
xfunctions.Axes.prototype.prepare = function(graphics,coords) {
    if (this.data != null && this.lastVersion == coords.version)
       return;
    this.lastVersion = coords.version;
    this.data = {};
    var data = this.data;
          graphics.font = "12px serif";
      var xLabelWidth = this.xLabel? graphics.measureText(this.xLabel).width : null;
      var yLabelWidth = this.yLabel? graphics.measureText(this.yLabel).width : null;
      graphics.font = "10px serif";
      var digitWidth = graphics.measureText("0").width;
      var ascent = 9;
      var descent = 2;
      switch (this.xAxisPosition) {
         case xfunctions.Axes.TOP: 
            data.xAxisPixelPosition = coords.top + coords.gap; 
            break;
         case xfunctions.Axes.BOTTOM: 
            data.xAxisPixelPosition = coords.top + coords.height - coords.gap - 1; 
            break;
         case xfunctions.Axes.CENTER: 
            data.xAxisPixelPosition = coords.top + coords.height/2; 
            break;
         case xfunctions.Axes.SMART:
            if (coords.ymax < 0)
               data.xAxisPixelPosition = coords.top + coords.gap;
            else if (coords.ymin > 0)
               data.xAxisPixelPosition = coords.top + coords.height - coords.gap - 1;
            else
               data.xAxisPixelPosition = coords.top + coords.gap + Math.round((coords.height-2*coords.gap - 1) * coords.ymax / (coords.ymax-coords.ymin));
            break;
      }
      switch (this.yAxisPosition) {
         case xfunctions.Axes.LEFT: 
            data.yAxisPixelPosition = coords.left + coords.gap; 
            break;
         case xfunctions.Axes.BOTTOM: 
            data.yAxisPixelPosition = coords.left + coords.width - coords.gap - 1; 
            break;
         case xfunctions.Axes.CENTER: 
            data.yAxisPixelPosition = coords.left + coords.width/2; 
            break;
         case xfunctions.Axes.SMART:
            if (coords.xmax < 0)
               data.yAxisPixelPosition = coords.left + coords.width - coords.gap - 1;
            else if (coords.xmin > 0)
               data.yAxisPixelPosition = coords.left + coords.gap;
            else
               data.yAxisPixelPosition = coords.left + coords.gap - Math.round((coords.width-2*coords.gap - 1) * coords.xmin / (coords.xmax-coords.xmin));
            break;
      }
      if (this.xLabel != null) {
         if (coords.left + coords.width - coords.gap - xLabelWidth <= data.yAxisPixelPosition)
            data.xLabel_x = coords.left + coords.gap;
         else
            data.xLabel_x = coords.left + coords.width - coords.gap - xLabelWidth;
         if (data.xAxisPixelPosition + 3 + ascent + descent + coords.gap >= coords.top + coords.height)
            data.xLabel_y = data.xAxisPixelPosition - 4;
         else
            data.xLabel_y = data.xAxisPixelPosition + 3 + ascent;
      }
      if (this.yLabel != null) {
         if (data.yAxisPixelPosition + 3 + yLabelWidth + coords.gap > coords.left + coords.width)
            data.yLabel_x = data.yAxisPixelPosition - yLabelWidth - 3;
         else
            data.yLabel_x = data.yAxisPixelPosition + 3;
         if (coords.top + ascent + descent + coords.gap > data.xAxisPixelPosition)
            data.yLabel_y = coords.top + coords.height - coords.gap - descent;
         else
            data.yLabel_y = coords.top + ascent + coords.gap;
      }
      var start;
      if (coords.xmin <= 0 && coords.xmax >= 0)
         start = 0;
      else
         start= fudgeStart( ((coords.xmax-coords.xmin)*(data.yAxisPixelPosition - (coords.left + coords.gap)))/(coords.width - 2*coords.gap)  + coords.xmin, 
                                                  0.05*(coords.xmax-coords.xmin) );
      var labelCt = (coords.width - 2*coords.gap) / (10*digitWidth);
      if (labelCt <= 2)
         labelCt = 3;
      else if (labelCt > 20)
         labelCt = 20;
      var interval = fudge( (coords.xmax - coords.xmin) / labelCt );
      for (var mul = 1.5; mul < 4; mul += 0.5) {
         var str = numtostring(interval+start);
         if (graphics.measureText(str).width + digitWidth > (interval/(coords.xmax-coords.xmin))*(coords.width-2*coords.gap))  // overlapping labels
             interval = fudge( mul*(coords.xmax - coords.xmin) / labelCt );
         else
            break;
      }
      var label = [];
      var x = start + interval;
      var limit = coords.left + coords.width;
      if (this.xLabel != null && coords.left + coords.width - coords.gap - graphics.measureText(this.xLabel).width > data.yAxisPixelPosition)  // avoid overlap with xLabel
         limit -= graphics.measureText(this.xLabel).width + coords.gap + digitWidth;
      while (x <= coords.xmax) {
         var str = numtostring(x);
         if (coords.left + coords.gap + (coords.width-2*coords.gap)*(x-coords.xmin)/(coords.xmax-coords.xmin) + graphics.measureText(str).width/2 > limit)
            break;
         label.push(x);
         x += interval;
      }
      x = start - interval;
      limit = coords.left;
      if (this.xLabel != null && coords.left + coords.width - coords.gap - xLabelWidth <= data.yAxisPixelPosition)  // avoid overlap with xLabel
         limit += xLabelWidth + digitWidth;
      while (x >= coords.xmin) {
         var str = numtostring(x);
         if (coords.left + coords.gap + (coords.width-2*coords.gap)*(x-coords.xmin)/(coords.xmax-coords.xmin) - graphics.measureText(str).width/2 < limit)
            break;
         label.push(x);
         x -= interval;
      }
      data.xTicks = [];
      data.xTickLabels = [];
      data.xTickLabelPos = [];
      for (var i = 0; i < label.length; i++) {
         data.xTicks.push(Math.round(coords.left + coords.gap + (coords.width-2*coords.gap)*(label[i]-coords.xmin)/(coords.xmax-coords.xmin)));
         var str = numtostring(label[i]);
         data.xTickLabels.push(str);
         var pos = [];
         pos[0] = data.xTicks[i] - graphics.measureText(str).width/2;
         if (data.xAxisPixelPosition - 4 - ascent >= coords.top)
            pos[1] = data.xAxisPixelPosition - 4;
         else
            pos[1] = data.xAxisPixelPosition + 4 + ascent;
         data.xTickLabelPos.push(pos);
      }
      if (coords.ymin <= 0 && coords.ymax >= 0)
         start = 0;
      else
         start = fudgeStart( coords.ymax - ((coords.ymax-coords.ymin)*(data.xAxisPixelPosition - (coords.top + coords.gap)))/(coords.height - 2*coords.gap), 
                                                  0.05*(coords.ymax-coords.ymin) );
      labelCt = (coords.height - 2*coords.gap) / (5*(ascent+descent));
      if (labelCt <= 2)
         labelCt = 3;
      else if (labelCt > 20)
         labelCt = 20;
      interval = fudge( (coords.ymax - coords.ymin) / labelCt );
      label = [];
      var y = start + interval;
      limit = coords.top + 8 + coords.gap;
      if (this.yLabel != null && coords.top + coords.gap + ascent + descent <= data.xAxisPixelPosition)  // avoid overlap with yLabel
          limit = coords.top + coords.gap + ascent + descent;
      while (y <= coords.ymax) {
         if (coords.top + coords.gap + (coords.height-2*coords.gap)*(coords.ymax-y)/(coords.ymax-coords.ymin) - ascent/2 < limit)
            break;
         label.push(y);
         y += interval;
      }
      y = start - interval;
      limit = coords.top + coords.height - coords.gap - 8;
      if (this.yLabel != null && coords.top + coords.gap + ascent + descent > data.xAxisPixelPosition)  // avoid overlap with yLabel
          limit = coords.top + coords.height - coords.gap - ascent - descent;
      while (y >= coords.ymin) {
         if (coords.top + coords.gap + (coords.height-2*coords.gap)*(coords.ymax-y)/(coords.ymax-coords.ymin) + ascent/2 > limit)
            break;
         label.push(y);
         y -= interval;
      }
      data.yTicks =[];
      data.yTickLabels = [];
      data.yTickLabelPos = [];
      var w = 0;  // width of tick mark
      for (var i = 0; i < label.length; i++) {
          str = numtostring(label[i]);
          data.yTickLabels.push(str);
          var s = graphics.measureText(str).width;
          if (s > w)
             w = s;  
      }
      for (var i = 0; i < label.length; i++) {
         data.yTicks[i] = Math.round(coords.top + coords.gap + (coords.height-2*coords.gap)*(coords.ymax-label[i])/(coords.ymax-coords.ymin));
         var pos = []
         pos[1] = data.yTicks[i] + ascent/2;
         if (data.yAxisPixelPosition - 4 - w < coords.left)
            pos[0] = data.yAxisPixelPosition + 4;
         else
            pos[0] = data.yAxisPixelPosition - 4 - graphics.measureText(data.yTickLabels[i]).width;
            data.yTickLabelPos.push(pos);
      }

    function numtostring(x) {
        var s = Number(x);
        if (Math.abs(x) < 0.0005 || Math.abs(x) >= 50000)
           return s.toExponential(3);
        var str;
        if (Math.abs(x) > 10000)
           return "" + Math.round(x);
        if (Math.abs(x) > 100)
           str = s.toFixed(2);
        else if (Math.abs(x) > 1)
           str = s.toFixed(3);
        else if (Math.abs(x) > 0.01)
           str = s.toFixed(4);
        else
           str = s.toFixed(5);
        while (str.charAt(str.length-1) == "0")
           str = str.substring(0,str.length-1);
        if (str.charAt(str.length-1) == ".")
           str = str.substring(0,str.length-1);
        return str;
    }
    function fudge(x) {  // carried over from Pascal via Java...
      var j,  y, digits;
      if (Math.abs(x) < 0.0005 || Math.abs(x) > 500000)
         return x;
      else if (Math.abs(x) < 0.1 || Math.abs(x) > 5000) {
            y = x;
            digits = 0;
            if (Math.abs(y) >= 1) {
               while (Math.abs(y) >= 8.75) {
                     y = y / 10;
                     digits = digits + 1;
               }
            }
            else {
               while (Math.abs(y) < 1) {
                     y = y * 10;
                     digits = digits - 1;
               }
            }
            y = Math.round(y * 4) / 4;
            if (digits > 0) {
               for (j = 0; j < digits; j++)
                  y = y * 10;
            }
            else if (digits < 0) {
               for (j = 0; j < -digits; j++)
                  y = y / 10;
            }
            return y;
      }
      else if (Math.abs(x) < 0.5)
         return Math.round(10 * x) / 10.0;
      else if (Math.abs(x) < 2.5)
         return Math.round(2 * x) / 2.0;
      else if (Math.abs(x) < 12)
         return Math.round(x);
      else if (Math.abs(x) < 120) 
         return Math.round(x / 10) * 10.0;
      else if (Math.abs(x) < 1200)
         return Math.round(x / 100) * 100.0;
      else
         return Math.round(x / 1000) * 1000.0;
    }
    function fudgeStart(a,diff) {  // carried over from Pascal via Java...
        if (Math.abs(Math.round(a) - a) < diff)
          return Math.round(a);
        for (var x = 10; x <= 100000; x *= 10) {
          var d = Math.round(a*x) / x;
          if (Math.abs(d - a) < diff)
             return d;
      }
      return a;
    }
}
