
package tmcm.xModels;

import java.util.Hashtable;
import java.util.Vector;
import java.awt.Color;

class ParseError extends Exception {
   int errorPosition;
   ParseError(String message, int pos) {
      super(message);
      errorPosition = pos;
   }
}

class Parser {

   // variable declarations and static initializer at end!

   
   Parser(String str) throws ParseError {
      data = str;
      pos = 0;
      userdef = new Hashtable(10);
      model = new Model();
      objects = new Vector(10);
      nextToken = null;
      model.frames = 1;  // default, to be changed if an animate command is found
      doParse();
      model.scene = new ComplexObject(objects);
      if (model.viewDistance == null)
         model.viewDistance = new ParamVal(20.0);
      if (model.bgColor == null)
        model.bgColor = new RGBParam(Color.white);
      objects = null;
      data = null;
      userdef = null;
      nextToken = null;
   }
   
   Model getModel() {
      return model;
   }
   
   // everything else is private
   
   
   private void doError(String message) throws ParseError{
      throw new ParseError(message,pos);
   }
   
   private Object lookToken() throws ParseError {
      if (nextToken == null)
         getToken();
      return nextToken;
   }
   
   private Object readToken() throws ParseError {
      if (nextToken == null)
         getToken();
      Object t = nextToken;
      nextToken = null;
      return t;
   }
   
   private void rescanToken() {   // force recanning of lookahead token
      if (nextToken != null) {
         nextToken = null;
         pos = tokenStart;
      }
   }
   
   private void getToken()  throws ParseError {
      while (true) {  // skip whitespace and comments
         if (pos >= data.length())
            break;
         if (data.charAt(pos) == ';') {
            do {
               pos++;
            } while (pos < data.length() && data.charAt(pos) != '\n' && data.charAt(pos) != '\r');
            if (pos >= data.length())
               break;
         }
         if (data.charAt(pos) == ',' || Character.isSpace(data.charAt(pos)))
            pos++;
         else
            break;
      }
      tokenStart = pos;  // used for rescanning (which is done by doDefineObject()
      if (pos >= data.length()) {
         nextToken = eofToken;
         tokenString = "End-of-data";
         return;
      }
      char ch = data.charAt(pos);
      if (ch == '[') {
         nextToken = leftBracket;
         tokenString = "[";
         pos++;
      }
      else if (ch == ']') {
         nextToken = rightBracket;
         tokenString = "]";
         pos++;
      }
      else if (ch == ':') {
         nextToken = colon;
         tokenString = ":";
         pos++;
      }
      else if (Character.isLetter(ch) || ch == '_')
         doWord();
      else if (Character.isDigit(ch) || ch == '-' || ch == '+' || ch == '.')
         doNumber();
      else
         doError("Illegal character, " + ch + ", found in scene description."); 
   }
   
   private void doNumber()  throws ParseError {
      buffer.setLength(0);
      char ch = data.charAt(pos);
      if (ch == '+' || ch == '-') {
         buffer.append(ch);
         pos++;
      }
      while (true) {
         if (pos >= data.length())
            break;
         ch = data.charAt(pos);
         if (Character.isDigit(ch)) {
            buffer.append(ch);
            pos++;
         }
         else
            break;
      }
      if (pos < data.length() && data.charAt(pos) == '.') {
         buffer.append(data.charAt(pos));
         pos++;
         while (true) {
            if (pos >= data.length())
               break;
            ch = data.charAt(pos);
            if (Character.isDigit(ch)) {
               buffer.append(ch);
               pos++;
            }
            else
               break;
         }
      }
      if (pos < data.length() && (data.charAt(pos) == 'e' || data.charAt(pos) == 'E')) {
         buffer.append(data.charAt(pos));
         pos++;
         if (pos < data.length() && (data.charAt(pos) == '-' || data.charAt(pos) == '+')) {
            buffer.append(data.charAt(pos));
            pos++;
         }
         while (true) {
            if (pos >= data.length())
               break;
            ch = data.charAt(pos);
            if (Character.isDigit(ch)) {
               buffer.append(ch);
               pos++;
            }
            else
               break;
         }
      }
      Double d = null;
      tokenString = buffer.toString();
      try {
         d = new Double(tokenString);
      }
      catch (NumberFormatException e) {
         d = null;
      }
      if (d == null || d.isInfinite() || d.isNaN())
         doError("Illegal number, \"" + tokenString + "\", found in scene description.");
      nextToken = d;
   }
   
   private void doWord() {
      buffer.setLength(0);
      while (true) {
         if (pos >= data.length())
            break;
         char ch = data.charAt(pos);
         if (Character.isLetterOrDigit(ch) || ch == '_') {
            buffer.append(Character.toLowerCase(ch));
            pos++;
         }
         else
            break;
      }
      tokenString = buffer.toString();
      Object val = userdef.get(tokenString);
      if (val == null)
         val = predef.get(tokenString);
      if (val == null)
         nextToken = tokenString;
      else
         nextToken = val;
   }
   
   private void doParse() throws ParseError {
      Object token = lookToken();
      if (token == eofToken)
         doError("There is no data in the program.");
      if (token == animate) {
         doAnimationData();
         token = lookToken();
      }
      while (token != eofToken) {
         if (token == define)
            doDefineObject();
         else if (token == background)
            doBackgroundData();
         else if (token == viewdistance)
            doViewdistance();
         else if (token instanceof SceneMaker || token == leftBracket)
            objects.addElement(doObject());
         else if (token == rgb || token == hsb || token instanceof Color)
            objects.addElement(doColor());
         else if (token instanceof Integer) {
            if (token == polygon || token == polygon_3d || token == lathe || token == extrude)
               objects.addElement( doPolyThing(((Integer)token).intValue()) );
            else if (token == animate)
               doError("The command \"animate\" can only be used as the first item in a scene description.");
            else if (token == infinity)
               doError("The word \"infinity\" is reserved for use with the ViewDistance command.");
            else if (token == aboutline)
               doError("The word \"aboutLine\" is meant for used with a rotation transformation.");
            else if (token == about)
               doError("The word \"about\" is meant for used with a rotation transformation.");
            else
               doError("Found a transformation when an object or command was expected.");
         }
         else if (token instanceof String)
            doError("Unrecognized word, " + token + ", found in scene description.");
         else if (token instanceof Character)
            doError("Misplaced character, " + token + ", found while expecting an object, a color, etc.");
         else if (token instanceof Double)
            doError("Number (" + ((Double)token) + ") found in illegal position while expecting a transformation, object, color, etc.");
         else
            doError("Unexpected token found in input.");
         token = lookToken();
      }
   }
   
   private void doAnimationData()  throws ParseError {
      readToken();  // skip "animate"
      Object token = lookToken();
      if (!(token instanceof Double))
         doError("Expecting a number here; the \"animate\" command must be followd by one or more frame counts.");
      int[] frameCounts = new int[10];
      int ct = 0;
      while (token instanceof Double) {
         readToken();
         if (ct >= frameCounts.length) {
            int[] temp = new int[frameCounts.length + 10];
            for (int i = 0; i < frameCounts.length; i++)
               temp[i] = frameCounts[i];
               frameCounts = temp;
         }
         frameCounts[ct] = ((Double)token).intValue();
         if (frameCounts[ct] <= 0)
            doError("The frame counts for the animate command must be positive integers.");
         ct++;
         token = lookToken();
      }
      model.frameCounts = new int[ct + 1];  // model.frameCounts are cumulative
      model.frameCounts[0] = 0;
      for (int i = 1; i <= ct; i++)
         model.frameCounts[i] = frameCounts[i-1] + model.frameCounts[i-1];
      model.frames = model.frameCounts[ct] + 1;
      if (model.frames > 100000)
         doError("Too many frames specified in animation. An absolute limit of 100000 is imposed.");
   }
   
   private void doDefineObject()  throws ParseError {
      readToken();  // skip "define"
      Object token = readToken();
      if (token instanceof String) {
         String name = (String)token;
         token = lookToken();
         if (token == rgb || token == hsb || token instanceof Color)
            userdef.put(name, doColor());
         else
            userdef.put(name, doObject());
         rescanToken();  // necessary because putting a new string into userdef might have invalidated the lookahead token
      }
      else if (token instanceof SceneMaker || token instanceof Integer)
         doError("You can't redefine the word \"" + tokenString + "\".  It already has a definition.");
      else if (token == eofToken)
         doError("Unexpected end-of-data in  the middle of a \"define\" command.");
      else if (token instanceof Character)
         doError("Found an unexpected character, " + tokenString + ", in a \"define\" command where the word being defined should have been.");
      else if (token instanceof Double)
         doError("Found a number in a \"define\" command where the word being defined should have been.");
      else
         doError("Error in \"define\" command.");
   }
   
   private void doBackgroundData()  throws ParseError {
      if (model.bgColor != null)
         doError("The \"background\" command can only be used once in a scene description.");
      readToken();  //skip "background"
      model.bgColor = doColor();
   }
   
   private void doViewdistance()  throws ParseError {
      if (model.viewDistance != null)
         doError("The \"viewdistance\" command can only be used once in a scene description.");
      readToken();  //skip "viewdistance"
      Object token = lookToken();
      if (token == infinity) {
         model.viewDistance = new ParamVal(Double.POSITIVE_INFINITY);
         readToken();
      }
      else if (token instanceof Double) {
         ParamVal v = doParam();
         for (int i = 0; i < v.values.length; i++)
            if (!Double.isNaN(v.values[i]) && v.values[i] < 1.0e-10)
               doError("Negative number or zero found in viewDistance specification.  View distance must be positive.");
         model.viewDistance = v;
      }
      else
         doError("The \"viewdistance\" command must be followed by a number, a number range, or the word \"infinity\".");
   }
   
   private SceneMaker doObject()  throws ParseError {
      Object token = lookToken();
      SceneMaker obj = null;
      if (token == leftBracket) {
         readToken();
         Vector objlist = new Vector(10);
         while (true) {
            token = lookToken();
            if (token == eofToken)
               doError("Missing right bracket; end-of-data encountered in the middle of a complex object.");
            if (token == rightBracket)
               break;
            if (token instanceof SceneMaker || token == leftBracket)
               objlist.addElement(doObject());
            else if (token == rgb || token == hsb || token instanceof Color)
               objlist.addElement(doColor());
            else if (token instanceof Integer) {
               if (token == polygon || token == polygon_3d || token == lathe || token == extrude)
                  objlist.addElement( doPolyThing(((Integer)token).intValue()) );
               else if (token == infinity)
                  doError("The word \"infinity\" is reserved for use with the ViewDistance command.");
               else if (token == about)
                  doError("The word \"about\" is meant for used with a rotation transformation.");
               else if (token == aboutline)
                  doError("The word \"aboutLine\" is meant for used with a rotation transformation.");
               else if (token == define || token == background || token == viewdistance || token == animate)
                  doError("The command \"" + tokenString + "\" cannot be used in a complex object or as an object definition.");
               else
                  doError("Found a transformation when an object or command was expected.");
            }
            else if (token instanceof String)
               doError("Unrecognized word, " + token + ", found in complex object or as an object definition.");
            else if (token instanceof Character)
               doError("Misplaced character, " + token + ", found while expecting an object, a color, etc.");
            else
               doError("Error in object.");
         }
         readToken();
         obj = new ComplexObject(objlist);
      }
      else if (token instanceof SceneMaker)
         obj = (SceneMaker)readToken();
      else if (token == polygon || token == polygon_3d || token == lathe || token == extrude)
         obj = doPolyThing( ((Integer)token).intValue() );
      else
         doError("Expected an object, but found \"" + tokenString + "\".");
      token = lookToken();
      if (token instanceof Integer && ((Integer)token).intValue() > 100)
         obj = new TransformedObject(obj,doTransformList());
      return obj;
   }
   
   private RGBParam doColor()  throws ParseError {
      Object token = readToken();
      RGBParam p = null;
      if (token == rgb || token == hsb) {
         Vector params = doParamList(0);
         if (params.size() != 3)
            if (token == rgb)
               doError("Exactly three parameters are required for an rgb specification.");
            else
               doError("Exactly three parameters are required for an hsb specification.");
         for (int i=0; i<3; i++) {
            ParamVal v = (ParamVal)params.elementAt(i);
            for (int j=0; j < v.values.length; j++)
               if (!Double.isNaN(v.values[j]) && (v.values[j] < 0 ||v.values[j] > 1))
                  if (token == rgb)
                     doError("All parameter values for \"rgb\" must be between 0.0 and 1.0, inclusize.");
                  else
                     doError("All parameter values for \"hsb\" must be between 0.0 and 1.0, inclusize.");
         }
         if (token == rgb)
            p = new RGBParam( (ParamVal)params.elementAt(0), (ParamVal)params.elementAt(1), (ParamVal)params.elementAt(2) );
         else
            p = new HSBParam( (ParamVal)params.elementAt(0), (ParamVal)params.elementAt(1), (ParamVal)params.elementAt(2) );
      }
      else if (token instanceof Color)
         p = new RGBParam((Color)token);
      else
         doError("Expected a color specification, but found \"" + tokenString + "\".");
      return p;
   }
   
   private Vector doParamList(int max) throws ParseError {
      Vector list = new Vector();
      list.addElement(doParam());
      while (lookToken() instanceof Double) {
         if (max > 0 && list.size() == max)
            doError("Found too many parameters, when expecting a maximum of " + max + ".");
         list.addElement(doParam());
      }
      return list;
   }
   
   private ParamVal doParam() throws ParseError {
      Object token = readToken();
      if (!(token instanceof Double))
         if (model.frames > 1)
            doError("Expected a number or range of numbers, but found \"" + tokenString + "\" instead");
         else
            doError("Expected a number, but found \"" + tokenString + "\" instead");
      Object next = lookToken();
      if (next != colon)
         return new ParamVal(((Double)token).doubleValue());
      if (model.frames == 1)
         doError("Found an illegal \":\".  This scene was not specified to be an animation; ranges of numbers not allowed.");
      double[] values = new double[model.frameCounts.length];
      values[0] = ((Double)token).doubleValue();
      int ct = 1;
      while (next == colon) {
         if (ct >= values.length)
            if (values.length == 2)
               doError("Too many values.  This is not a segmented animation; a number range must give exactly two values.");
            else
               doError("Too many values.  A number range must contain exactly as many colons as there are segments in the animation.");
         readToken();
         token = lookToken();
         if (token == colon) {
            values[ct] = Double.NaN;
            ct++;
            next = token;
         }
         else if (token instanceof Double) {
            values[ct] = ((Double)token).doubleValue();
            ct++;
            readToken();
            next = lookToken();
         }
         else
            doError("Error in number range; a \":\" must be followed by a number or by another \":\".");
      }
      if (ct < values.length)
          doError("Too few values.  A number range must contain exactly as many colons as there are segments in the animation.");
      return new ParamVal(model.frameCounts, values);
   }
   
   
   private Vector doTransformList() throws ParseError {
      Vector list = new Vector(10);
      Object token = lookToken();
      while (token instanceof Integer && ((Integer)token).intValue() > 100) {
         list.addElement(doTransform());
         token = lookToken();
      }
      return list;
   }
   
   private TransformInfo doTransform() throws ParseError {
      Object token=readToken();
      TransformInfo trans = null;
      Vector params, params2;
      Object temp;
      int savePos = pos;
      switch (((Integer)token).intValue()) {
         case 105: // scale
           params = doParamList(3);
           if (params.size() == 0)
              doError("Missing parameter for \"scale\" transformation.  One, two, or three numbers or number ranges must be provided.");
           trans = new TransformInfo(TransformInfo.scale, params);
           break;
         case 106: // xscale
           params = doParamList(1);
           if (params.size() == 0)
              doError("Missing parameter for \"xscale\" transformation.  A number or number range must be provided.");
           params.addElement(new ParamVal(1));
           params.addElement(new ParamVal(1));
           trans = new TransformInfo(TransformInfo.scale, params);
           break;
         case 107: // yscale
           params = doParamList(1);
           if (params.size() == 0)
              doError("Missing parameter for \"yscale\" transformation.  A number or number range must be provided.");
           temp = params.elementAt(0);
           params.setSize(0);
           params.addElement(new ParamVal(1));
           params.addElement(temp);
           params.addElement(new ParamVal(1));
           trans = new TransformInfo(TransformInfo.scale, params);
           break;
         case 108: // zscale
           params = doParamList(1);
           if (params.size() == 0)
              doError("Missing parameter for \"zscale\" transformation.  A number or number range must be provided.");
           temp = params.elementAt(0);
           params.setSize(0);
           params.addElement(new ParamVal(1));
           params.addElement(new ParamVal(1));
           params.addElement(temp);
           trans = new TransformInfo(TransformInfo.scale, params);
           break;
         case 109: // translate
           params = doParamList(3);
           if (params.size() == 0)
              doError("Missing parameter for \"translate\" transformation.  One, two, or three numbers or number ranges must be provided.");
           trans = new TransformInfo(TransformInfo.translate, params);
           break;
         case 110: // xtranslate
           params = doParamList(1);
           if (params.size() == 0)
              doError("Missing parameter for \"xtranslate\" transformation.  A number or number range must be provided.");
           params.addElement(new ParamVal(0));
           params.addElement(new ParamVal(0));
           trans = new TransformInfo(TransformInfo.translate, params);
           break;
         case 111: // ytranslate
           params = doParamList(1);
           if (params.size() == 0)
              doError("Missing parameter for \"ytranslate\" transformation.  A number or number range must be provided.");
           temp = params.elementAt(0);
           params.setSize(0);
           params.addElement(new ParamVal(0));
           params.addElement(temp);
           params.addElement(new ParamVal(0));
           trans = new TransformInfo(TransformInfo.translate, params);
           break;
         case 112: // ztranslate
           params = doParamList(1);
           if (params.size() == 0)
              doError("Missing parameter for \"ztranslate\" transformation.  A number or number range must be provided.");
           temp = params.elementAt(0);
           params.setSize(0);
           params.addElement(new ParamVal(0));
           params.addElement(new ParamVal(0));
           params.addElement(temp);
           trans = new TransformInfo(TransformInfo.translate, params);
           break;
         case 113: // rotate
           params = doParamList(1);
           if (params.size() == 0)
              doError("Missing parameter for \"rotate\" transformation.  A number or number range must be provided.");
           token = lookToken();
           if (token == about || token == aboutline) {
              readToken();
              lookToken();
              if (tokenString.equals("line")) {
                 readToken();
                 token = aboutline;
              }
              if (token == about) {
                 params2 = doParamList(0);
                if (params2.size() == 0) 
                     doError("Missing parameters.  \"About\" requires two parameters to specifiy the pivot point for the rotation.");
                 if (params2.size() != 2)
                     doError("\"About\" requires exactly two parameters to specifiy the pivot point for the rotation.");
                 params.addElement(params2.elementAt(0));
                 params.addElement(params2.elementAt(1));
                 trans = new TransformInfo(TransformInfo.rotateAboutPoint, params);
              }
              else if (token == aboutline) {
                 params2 = doParamList(0);
                 if (params2.size() == 0) 
                     doError("Missing parameters.  \"AboutLine\" requires either 3 or 6 parameters to specify the line.");
                 if (params2.size() != 3 && params2.size() != 6)
                     doError("\"AboutLine\" requires either 3 or 6 parameters to specify the line.");
                 for (int i = 0; i < params2.size(); i++)
                    params.addElement(params2.elementAt(i));
                 trans = new TransformInfo(TransformInfo.rotateAboutLine, params);
              }
           }
           else
              trans = new TransformInfo(TransformInfo.zrotate, params);
           break;
         case 114: // xrorate
           params = doParamList(1);
           if (params.size() == 0)
              doError("Missing parameter for \"xrotate\" transformation.  A number or number range must be provided.");
           trans = new TransformInfo(TransformInfo.xrotate, params);
           break;
         case 115: // yrorate
           params = doParamList(1);
           if (params.size() == 0)
              doError("Missing parameter for \"yrotate\" transformation.  A number or number range must be provided.");
           trans = new TransformInfo(TransformInfo.yrotate, params);
           break;
         case 116: // zrorate
           params = doParamList(1);
           if (params.size() == 0)
              doError("Missing parameter for \"zrotate\" transformation.  A number or number range must be provided.");
           trans = new TransformInfo(TransformInfo.zrotate, params);
           break;
         case 117:  // xskew
           params = doParamList(1);
           if (params.size() == 0)
              doError("Missing parameter for \"xskew\" transformation.  A number or number range must be provided.");
           params.addElement(new ParamVal(0));
           trans = new TransformInfo(TransformInfo.xskew, params);
           break;
         case 118:  // yskew
           params = doParamList(1);
           if (params.size() == 0)
              doError("Missing parameter for \"yskew\" transformation.  A number or number range must be provided.");
           trans = new TransformInfo(TransformInfo.yskew, params);
           break;
         case 119:  // xyshear
           params = doParamList(0);
           if (params.size() == 0)
              doError("Missing parameters for \"xyshear\" transformation.  Two numbers or number ranges must be provided.");
           if (params.size() != 2)
              doError("Wrong number of parameters for \"xyshear\" transformation.  Exactly two numbers or number ranges must be provided.");
           trans = new TransformInfo(TransformInfo.xyshear, params);
           break;
         default:  // shouldn't happen!
           doError("Internal Error: Unimplemented transformation type.");
      }
      return trans;      
   }
   
   
   
   private SceneMaker doPolyThing(int code)  throws ParseError {
         // codes: polygon=1, polygon_3d=2, lathe=3, extrude=4
      String command = tokenString;
      readToken();
      Vector params;
      int reps = 0;
      int points,lines;
      ParamLineGroup g;
      SceneMaker obj = null;
      if (code == 3 || code == 4) { // read rep count for lathe/extrude
         Object n = readToken();
         if (!(n instanceof Double))
            doError("Expected an integer to specify the repetition count for \"" + command + "\" command.");
         Object next = lookToken();
         if (next == colon)
            doError("The \"" + command + "\" command must be followed by a single integer to specify the repetition count, not a number range.");
         reps = ((Double)n).intValue();
         if (reps < 2)
            doError("The repetition count for the \"" + command + "\" command must be at least 2");
         if (reps > 100)
            doError("The repetition count for the \"" + command + "\" command is limited to 100.");
      }
      params = doParamList(0);
      if (params.size() == 0)
         doError("Missing parameters. A list of points for the \"" + command + "\" command is required.");
      if (code == 2) {
         if (params.size() % 3 != 0)
            doError("Polygon_3d requires a list of (x,y,z) points; the number of parameters must be a multiple of 3.");
         if (params.size() < 6)
            doError("Polygon_3d requires at least 6 parameters, specifying at least two (x,y,z) points.");
      }
      else if (code == 1) {
         if (params.size() % 2 != 0)
            doError("Polygon requires a list of (x,y) points; the number of parameters must be an even number.");
         if (params.size() < 4)
            doError("Polygon requires at least 4 parameters, specifying at least two (x,y) points.");
      }
      else {
         if (params.size() % 2 != 0)
            doError(command + " requires a repetition count, followed by an even number of paramaters to specify a list of points.");
      }
      switch (code) {
         case 1:  // polygon
            points = params.size() / 2;
            lines = points;
            g = new ParamLineGroup(points,lines);
            for (int i = 0; i < lines; i++) {
               g.x[i] = (ParamVal)params.elementAt(2*i);
               g.y[i] = (ParamVal)params.elementAt(2*i+1);
               g.z[i] = new ParamVal(0);
               g.v1[i] = i;
               g.v2[i] = (i < lines-1)? i+1 : 0;
            }
            obj = g;
            break;
         case 2:  // polygon_3d
            points = params.size() / 3;
            points = params.size() / 2;
            lines = points;
            g = new ParamLineGroup(points,lines);
            for (int i = 0; i < lines; i++) {
               g.x[i] = (ParamVal)params.elementAt(3*i);
               g.y[i] = (ParamVal)params.elementAt(3*i+1);
               g.z[i] = (ParamVal)params.elementAt(3*i+2);
               g.v1[i] = i;
               g.v2[i] = (i < lines-1)? i+1 : 0;
            }
            obj = g;
            break;
         case 3:  // lathe
            points = params.size() / 2;
            obj = new LatheObject(reps,params);
            break;
         case 4:  // extrude
            points = params.size() / 2;
            obj = new ExtrudeObject(reps,params);
            break;
      }
      Object token = lookToken();
      if (token instanceof Integer && ((Integer)token).intValue() > 100)
         obj = new TransformedObject(obj,doTransformList());
      return obj;
   }
      

   private Hashtable userdef;
   private int pos;
   private String data;
   private Model model;
   private Vector objects;
   private Object nextToken;
   private int tokenStart;
   private String tokenString;
   private StringBuffer buffer = new StringBuffer(50);
   
   private final static Character eofToken = new Character((char)26);
   private final static Character leftBracket = new Character('[');
   private final static Character rightBracket = new Character(']');
   private final static Character colon = new Character(':');

   
   private static Hashtable predef;  // see static initializer below
   
   private static LineGroup square, circle, cube, line, cone, cylinder;
   
   private static final Integer
   
            polygon = new Integer(1),
            polygon_3d = new Integer(2),
            lathe = new Integer(3),
            extrude = new Integer(4),
            
            rgb = new Integer(18),
            hsb = new Integer(19),
            infinity = new Integer(20),
            
            define = new Integer(21),
            animate = new Integer(22),
            background = new Integer(23),
            viewdistance = new Integer(24),
 
            about = new Integer(90),
            aboutline = new Integer(91),
            
            scale = new Integer(105),   // transformation commands have intValue > 100
            xscale = new Integer(106),
            yscale = new Integer(107),
            zscale = new Integer(108),
            translate = new Integer(109),
            xtranslate = new Integer(110),
            ytranslate = new Integer(111),
            ztranslate = new Integer(112),
            rotate = new Integer(113),
            xrotate = new Integer(114),
            yrotate = new Integer(115),
            zrotate = new Integer(116),
            xskew = new Integer(117),
            yskew = new Integer(118),
            xyshear = new Integer(119);
            
           
            
            
   static {
   
      line = new LineGroup(2,1);
      line.x[0] = -0.5;
      line.x[1] = 0.5;
      line.v1[0] = 0;
      line.v2[0] = 1;
   
      square = new LineGroup(4,4);
      square.x[0] = -0.5;
      square.y[0] = -0.5;
      square.x[1] = 0.5;
      square.y[1] = -0.5;
      square.x[2] = 0.5;
      square.y[2] = 0.5;
      square.x[3] = -0.5;
      square.y[3] = 0.5;
      square.v1[0] = 0;
      square.v2[0] = 1;
      square.v1[1] = 1;
      square.v2[1] = 2;
      square.v1[2] = 2;
      square.v2[2] = 3;
      square.v1[3] = 3;
      square.v2[3] = 0;
      
      cube = new LineGroup(8,12);
      for (int i = 0; i < 4; i++) {
         cube.x[i] = cube.x[i+4] = square.x[i];
         cube.y[i] = cube.y[i+4] = square.y[i];
         cube.z[i] = -0.5;
         cube.z[i+4] = 0.5;
         cube.v1[i] = i;
         cube.v2[i] = (i<3)? i+1 : 0;
         cube.v1[i+4] = i+4;
         cube.v2[i+4] = (i<3)? i+5 : 4;
         cube.v1[i+8] = i;
         cube.v2[i+8] = i+4;
      }
   
      circle = new LineGroup(36,36);
      for (int i = 0; i < 36; i++) {
         circle.x[i] = 0.5*Math.cos(i*Math.PI/18.0);
         circle.y[i] = 0.5*Math.sin(i*Math.PI/18.0);
         circle.v1[i] = i;
         circle.v2[i] = (i<35)? i+1 : 0;
      }
      
      cone = new LineGroup(37,48);
      for (int i = 0; i < 36; i++) {
         cone.x[i] = circle.x[i];
         cone.y[i] = -0.5;
         cone.z[i] = circle.y[i];
         cone.v1[i] = circle.v1[i];
         cone.v2[i] = circle.v2[i];
      }
      cone.x[36] = 0;
      cone.y[36] = 0.5;
      cone.z[36] = 0;
      for (int i = 36; i < 48; i++) {
         cone.v1[i] = 36;
         cone.v2[i] = (i-36) * 3;
      }
      
      cylinder = new LineGroup(72,84);
      for (int i = 0; i < 36; i++) {
         cylinder.x[i] = cylinder.x[i+36] = circle.x[i];
         cylinder.y[i] = -0.5;
         cylinder.y[i+36] = 0.5;
         cylinder.z[i] = cylinder.z[i+36] = circle.y[i];
         cylinder.v1[i] = circle.v1[i];
         cylinder.v1[i+36] = circle.v1[i]+36;
         cylinder.v2[i] = circle.v2[i];
         cylinder.v2[i+36] = circle.v2[i]+36;
      }
      for (int i = 73; i < 84; i++) {
         cylinder.v1[i] = (i-72) * 3;
         cylinder.v2[i] = cylinder.v1[i] + 36;
      }
      
      predef = new Hashtable(75);      
      predef.put("polygon", polygon);
      predef.put("polygon_3d", polygon_3d);
      predef.put("lathe",lathe);
      predef.put("extrude",extrude);
      predef.put("scale",scale);
      predef.put("xscale",xscale);
      predef.put("yscale",yscale);
      predef.put("zscale",zscale);
      predef.put("translate",translate);
      predef.put("xtranslate",xtranslate);
      predef.put("ytranslate",ytranslate);
      predef.put("ztranslate",ztranslate);
      predef.put("rotate",rotate);
      predef.put("xrotate",xrotate);
      predef.put("yrotate",yrotate);
      predef.put("zrotate",zrotate);
      predef.put("xskew",xskew);
      predef.put("yskew",yskew);
      predef.put("xyshear",xyshear);
      predef.put("about",about);
      predef.put("aboutline",aboutline);
      predef.put("rgb",rgb);
      predef.put("hsb",hsb);
      predef.put("infinity",infinity);
      predef.put("define",define);
      predef.put("animate",animate);
      predef.put("background",background);
      predef.put("viewdistance",viewdistance);
      predef.put("line",line);
      predef.put("square",square);
      predef.put("circle",circle);
      predef.put("cube",cube);
      predef.put("cone",cone);
      predef.put("cylinder",cylinder);
      predef.put("red", Color.red);
      predef.put("green", Color.green);
      predef.put("blue", Color.blue);
      predef.put("black", Color.black);
      predef.put("white", Color.white);
      predef.put("gray", Color.gray);
      predef.put("darkgray", Color.darkGray);
      predef.put("lightgray", Color.lightGray);
      predef.put("cyan", Color.cyan);
      predef.put("magenta", Color.magenta);
      predef.put("yellow", Color.yellow);
   }  // end of static initialization


}  // end of class Parser