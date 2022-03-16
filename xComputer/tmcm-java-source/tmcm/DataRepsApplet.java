
package tmcm;

import java.awt.*;
import java.util.Random;

public class DataRepsApplet extends java.applet.Applet {

   Button zeroButton, onesButton, randomButton;
   Button setButton;
   TextField input;
   Checkbox asciiChoice, binaryChoice, intChoice, hexChoice, floatChoice;
   CheckboxGroup choices;
   DataRepsPixelCanvas pixels;
   
   Label asciiDisplay, binaryDisplay, intDisplay, hexDisplay, floatDisplay;
   Label directions;
   StringBuffer asciiTemp = new StringBuffer(25);
   
   Random randGen = new Random();


   String[][] parameterInfo = {
         { "none", "", "" }
      };
   
   public String getAppletInfo() {
      return "DataRepsApplet, by David J. Eck (eck@hws.edu), July 1997";
   }
   
   public String[][] getParameterInfo() {
      return parameterInfo;
   }
   
   
   public void init() {
   
      setLayout(null);
      setBackground(Color.gray);
      
      int width = size().width;
      int height = size().height;
      int lineheight = (height - 49 - 44 - 15) / 11;
      
      pixels = new DataRepsPixelCanvas(this);
      add(pixels);
      pixels.reshape(width/2 - 42, (height)/2 + lineheight/2 - 12, 84, 44);
      
      Color lightBlue = new Color(200,200,255);
      
      Panel display = new Panel();
      display.setLayout(new GridLayout(5,1,2,2));
      display.setBackground(lightBlue);

      binaryDisplay = new Label(" Binary:  00000000000000000000000000000000");
      binaryDisplay.setBackground(lightBlue);
      binaryDisplay.setForeground(Color.blue);
      display.add(binaryDisplay);
      
      intDisplay = new Label(" Base-ten Integer:  0");
      intDisplay.setBackground(lightBlue);
      intDisplay.setForeground(Color.blue);
      display.add(intDisplay);
      
      hexDisplay = new Label(" Hexadecimal:  0");
      hexDisplay.setBackground(lightBlue);
      hexDisplay.setForeground(Color.blue);
      display.add(hexDisplay);
      
      floatDisplay = new Label(" Real Number:  0");
      floatDisplay.setBackground(lightBlue);
      floatDisplay.setForeground(Color.blue);
      display.add(floatDisplay);
      
      asciiDisplay = new Label(" ASCII Text:  <#0><#0><#0><#0>");
      asciiDisplay.setBackground(lightBlue);
      asciiDisplay.setForeground(Color.blue);
      display.add(asciiDisplay);
      
      add(display);
      display.reshape(10, height - 10 - 5*lineheight, width - 20, 5*lineheight);
      
      Panel labelPanel = new Panel();
      labelPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
      directions = new Label(" Input a binary number (up to 32 bits):");
      directions.setBackground(Color.gray);
      labelPanel.add(directions);
      add(labelPanel);
      labelPanel.reshape(10,10,width-20,lineheight+2);
      
      Panel inputPanel = new Panel();
      inputPanel.setBackground(Color.white);
      inputPanel.setLayout(new BorderLayout());
      input = new TextField();
      input.setBackground(Color.white);
      inputPanel.add("Center", input);
      add(inputPanel);
      inputPanel.reshape(10,10+lineheight+4,width-20,lineheight+7);
      
      Panel choicePanel = new Panel();
      choicePanel.setLayout(new GridLayout(3,2));
      Label lab = new Label(" Select Input Type:");
      lab.setForeground(Color.blue);
      choicePanel.setBackground(lightBlue);
      choicePanel.add(lab);
      choices = new CheckboxGroup();
      binaryChoice = new Checkbox("Binary Number", choices, true);
      choicePanel.add(binaryChoice);
      intChoice = new Checkbox("Base-ten Integer", choices, false);
      choicePanel.add(intChoice);
      hexChoice = new Checkbox("Hexadecimal Number", choices, false);
      choicePanel.add(hexChoice);
      floatChoice = new Checkbox("Real Number", choices, false);
      choicePanel.add(floatChoice);
      asciiChoice = new Checkbox("ASCII Text", choices, false);
      choicePanel.add(asciiChoice);
      add(choicePanel);
      choicePanel.reshape(20,2*lineheight+25,width-40,3*lineheight);
      
      Panel buttons = new Panel();
      add(buttons);
      zeroButton = new Button("All Zeros");
      buttons.add(zeroButton);
      onesButton = new Button("All Ones");
      buttons.add(onesButton);
      randomButton = new Button("Random");
      buttons.add(randomButton);
      setButton = new Button("Use Input");
      buttons.add(setButton);
      buttons.setLayout(new GridLayout(1,4));
      buttons.reshape(10, 5*lineheight + 30, width-20, lineheight);
      
   }
   
   public void start() {
      input.selectAll();
      input.requestFocus();
   }
      
   
   void setDisplays(int n) {  // also called from DataRepsPixelCanvas!
       intDisplay.setText(" Base-ten Integer:  " + String.valueOf(n));
       hexDisplay.setText(" Hexadecimal:  " + Integer.toHexString(n));
       floatDisplay.setText(" Real Number:  " + Float.intBitsToFloat(n));
       String bin = Integer.toBinaryString(n);
       if (bin.length() < 32)
          bin = "00000000000000000000000000000000".substring(0,32-bin.length()) + bin;
       binaryDisplay.setText(" Binary:  " + bin);
       long N = n;
       asciiTemp.setLength(0);
       long i = (N & 0xFF000000L) >> 24;
       if (i <= 32 || i >=127)
          asciiTemp.append("<#" + i + ">");
       else
          asciiTemp.append((char)i);
       i = (N & 0xFF0000L) >> 16;
       if (i <= 32 || i >=127)
          asciiTemp.append("<#" + i + ">");
       else
          asciiTemp.append((char)i);
       i = (N & 0xFF00L) >> 8;
       if (i <= 32 || i >=127)
          asciiTemp.append("<#" + i + ">");
       else
          asciiTemp.append((char)i);
       i = (N & 0xFFL);
       if (i <= 32 || i >=127)
          asciiTemp.append("<#" + i + ">");
       else
          asciiTemp.append((char)i);
       asciiDisplay.setText(" ASCII Text:  " + asciiTemp.toString());
   }
   
   void setAll(int n) {
      setDisplays(n);
      pixels.setAllPixels(n);
   }
   
   void doButton(Button bttn) {
      if (bttn == setButton)
         doInput();
      else if (bttn == zeroButton)
         setAll(0);
      else if (bttn == onesButton)
         setAll(0xFFFFFFFF);
      else
         setAll( (int)(randGen.nextLong()) );
      input.selectAll();
      input.requestFocus();
   }
   
   void doCheckbox(Checkbox box) {
      if (box == binaryChoice)
         directions.setText(" Input a binary number (up to 32 bits):");
      else if (box == intChoice)
         directions.setText(" Input an integer:");
      else if (box == floatChoice)
         directions.setText(" Input a real number:");
      else if (box == hexChoice)
         directions.setText(" Input up to 8 hexidecimal digits:");
      else // ascii
         directions.setText(" Input up to 4 ASCII characters:");
      input.selectAll();
      input.requestFocus();
   }
   
   boolean isHex(String str) {
      for (int i = 0; i < str.length(); i++) {
         char ch = str.charAt(i);
         if ( !( (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F') || (ch >= 'a' && ch <= 'f') ) )
            return false;
      }
      return true;
   }
   
   boolean isBinary(String str) {
      for (int i = 0; i < str.length(); i++) {
         char ch = str.charAt(i);
         if ( ch != '0' && ch != '1' )
            return false;
      }
      return true;
   }
   
   boolean isInt(String str) {
      if (str.length() > 0 && str.charAt(0) != '+' && str.charAt(0) != '-'
                                && !(str.charAt(0) >= '0' && str.charAt(0) <= '9'))
         return false;
      for (int i = 1; i < str.length(); i++) {
         char ch = str.charAt(i);
         if ( ch < '0' || ch > '9' )
            return false;
      }
      return true;
   }
   
   boolean isFloat(String str) {
      int pos = 0;
      if (pos < str.length() && (str.charAt(pos) == '+' || str.charAt(pos) == '-'))
         pos++;
      while (pos < str.length() && (str.charAt(pos) >= '0' && str.charAt(pos) <= '9'))
         pos++;
      if (pos < str.length() && str.charAt(pos) == '.')
         pos++;
      while (pos < str.length() && (str.charAt(pos) >= '0' && str.charAt(pos) <= '9'))
         pos++;
      if (pos < str.length() && (str.charAt(pos) == 'e' || str.charAt(pos) == 'E'))
         pos++;
      if (pos < str.length() && (str.charAt(pos) == '+' || str.charAt(pos) == '-'))
         pos++;
      while (pos < str.length() && (str.charAt(pos) >= '0' && str.charAt(pos) <= '9'))
         pos++;
      return (pos == str.length());
   }
   
   void doInput() {
      String str = input.getText();
      Checkbox type = choices.getCurrent();
      if (type != asciiChoice)
         str = str.trim();
      if (type == null || str == null)
         return;  // shouldn't happen!
      if (type == binaryChoice) {
         if (str.length() == 0)
            setAll(0);
         else if (!isBinary(str))
            input.setText("Bad input!  The only legal bits are 0 and 1.");
         else if (str.length() > 32)
            input.setText("Too many bits; the limit is 32!");
         else {
            long N = Long.parseLong(str,2);
            setAll( (int)N );
         }
      }
      else if (type == intChoice) {
          if (str.length() > 0 && str.charAt(0) == '+')
             str = str.substring(1);
          if (str.length() == 0 || str.equals("-"))
            setAll(0);
          else if (!isInt(str))
             input.setText("Illegal integer!");
          else if (str.length() > 15)
             input.setText("Integer out of legal range!");
          else {
             long N = Long.parseLong(str);
             if (N > Integer.MAX_VALUE || N < Integer.MIN_VALUE)
                input.setText("Integer out of legal range!");
             else
                setAll( (int)N );
          }
      }
      else if (type == floatChoice) {
         if (str.length() == 0)
            setAll(0);
         else if (!isFloat(str))
            input.setText("Illegal real number!");
         else {
            Float N = new Float(Float.NaN);
            try { 
               N = Float.valueOf(str);
            }
            catch (NumberFormatException e) {
            }
            if (N.isNaN())
               input.setText("Illegal real number!");
            else
               setAll(Float.floatToIntBits(N.floatValue()));
         }
      }
      else if (type == hexChoice) {
         if (str.length() == 0)
            setAll(0);
         else if (!isHex(str))
            input.setText("Bad input!  Legal hex digits are 0-9 and A-F.");
         else if (str.length() > 8)
            input.setText("Too many hex digits; the limit is 8!");
         else {
            long N = Long.parseLong(str,16);
            setAll( (int)N );
         }
      }
      else {  // ascii
         if (str.length() > 4)
            input.setText("Too many characters; the limit is 4!");
         else {
            int N = 0;
            for (int i = 0; i < str.length(); i++)
                  N = (N << 8) | ((int)str.charAt(i) & 0xFF);
            setAll(N);
         }
      }
      input.selectAll();
      input.requestFocus();
   }
   
   public boolean action(Event evt, Object arg) {
      if (evt.target instanceof Button) {
         doButton( (Button)evt.target );
         return true;
      }
      if (evt.target instanceof Checkbox) {
         doCheckbox( (Checkbox)evt.target );
         return true;
      }
      if (evt.target == input) {
         doInput();
         return true;
      }
      return super.action(evt,arg);
   }
   

} // class DataRepsApplet

class DataRepsPixelCanvas extends Canvas {

   // Shows a grid of "big pixels" (each 10X10 normal pixels).
   
   // It is assumed that the size of the canvas is 84-by-44.
   //   (This is encoded in the following constants, which can be modified
   //    if a different size is desired.)
   
   final static int border = 2;     // border around big pixels
   final static int pixelWidth = 10; // size of pixel (including one-pixel grid line)
   final static int pixelHeight = 10;
   
   final static Color borderColor = Color.blue;
   final static Color gridLinesColor = new Color(200,200,255);  // light blue
   final static Color pixelOnColor = Color.black;
   final static Color pixelOffColor = Color.white;

   DataRepsApplet owner;  // the applet that contains this canvas; used in
                          // setPixel() to notify the applet that a pixel has
                          // been turned on or off
   
   boolean[] data = new boolean[32];  // represents the 32 big pixels
   
   boolean settingPixels;  // set to true or false in mouseDown() to indicate
                           // whether pixels are being turned on or off; this is
                           // used in mouseDrag().
   
   DataRepsPixelCanvas(DataRepsApplet owner) {
      this.owner = owner;
      setBackground(borderColor);
   }
   
   void setAllPixels(int newPixels) {
      int pos = 1;
      for (int i = 31; i >= 0; i--) {
         data[i] = ((newPixels & pos) != 0);
         pos <<= 1;
      }
      Graphics g = getGraphics();
      paint(g);
      g.dispose();
   }
   
   synchronized void setPixel(int row, int col, boolean on) {
      Graphics g = getGraphics();
      int left = border + col*pixelWidth;
      int top = border + row*pixelHeight;
      data[8*row+col] = on;
      g.setColor( on ? pixelOnColor : pixelOffColor );
      g.fillRect(left,top,pixelWidth-1,pixelHeight-1);
      g.dispose();
      int n = 0;
      for (int i = 0; i < 32; i++) {
         n = (n << 1);
         if (data[i])
            n |= 1;
      }
      owner.setDisplays(n);
   }
   
   synchronized public void paint(Graphics g) {
      g.setColor(borderColor);
      int width = 2*border + 8*pixelWidth - 1;
      int height = 2*border + 4*pixelHeight - 1;
      g.drawRect(0,0,width,height);
      g.drawRect(1,1,width-2,height-2);
      g.setColor(gridLinesColor);
      for (int i = 1; i < 4; i++)
         g.drawLine(border,border+i*pixelHeight-1,width-border-1,border+i*pixelHeight-1);
      for (int i = 1; i < 8; i++)
         g.drawLine(border+i*pixelWidth-1,border,border+i*pixelWidth-1,height-border-1);
      for (int pos = 0; pos < 32; pos++) {
         int row = pos / 8;
         int col = pos % 8;
         int left = border + col*pixelWidth;
         int top = border + row*pixelHeight;
         g.setColor( data[pos] ? pixelOnColor : pixelOffColor );
         g.fillRect(left,top,pixelWidth-1,pixelHeight-1);
      }
   }
   
   public void update(Graphics g) {
      paint(g);
   }
   
   public boolean mouseDown(Event evt, int x, int y) {
      int row = (y-border) / pixelHeight;
      int col = (x-border) / pixelWidth;
      if (row < 0 || row > 3 || col < 0 || col > 7)
         return true;
      int pos = 8*row + col;
      settingPixels = !data[pos];
      setPixel(row,col,settingPixels);
      return true;
   }
   
   public boolean mouseDrag(Event evt, int x, int y) {
      int row = (y-border) / pixelHeight;
      int col = (x-border) / pixelWidth;
      if (row < 0 || row > 3 || col < 0 || col > 7)
         return true;
      int pos = 8*row + col;
      if (settingPixels == data[pos])
         return true;
      setPixel(row,col,settingPixels);
      return true;
   }
   

}