
package tmcm.xTurtle;

import java.awt.*;
import java.util.*;

class TTurtleRef {
   int center_x, center_y;
   int[] x = new int[4];
   int[] y = new int[4];
}

class TTurtleHandler extends Panel implements TurtleNotification {

  boolean alwaysHideTurtles = false;
  Vector turtles = new Vector();

  double pixelSize;
  
  int width=-1, height=-1;
  Image OSC;
  Graphics OSG;
  
  int basicSize = 10;
  
  private double xmin, xmax, ymin, ymax, scale;
  
  private int xint,yint;  // for internal communication in lieu of REF params
  
  private String[] errorMessageList = null;
  private FontMetrics fontData;
  
  private int waitingForIO = -1;
  private static final int WaitingForTellUser = 0,
                           WaitingForAskUser = 1,
                           WaitingForYesNo = 2;
  private TProcess processWantsIO = null;
  private String[] IOPrompt = null;
  private static String[] promptHeader = { "(from TellUser)", "(from AskUser)", "(from AskYesNo)" };
  private Button yesButton, noButton, okButton;
  private TextField userInput;
  private Color darkGreen = new Color(0.0F,0.5F,0.0F);
  private Color lightGreen = new Color(0.7F,1.0F,0.7F);

  private static int[][] turtle_x, turtle_y;
  private static int turtle_ct = 40;  // must be divisor of 360;
  
  private boolean firststart = true;
  
  static {
     int degrees = 360 / turtle_ct;
     turtle_x = new int[turtle_ct][4];
     turtle_y = new int[turtle_ct][4];
     for (int i = 0; i < turtle_ct; i++) {
        double d = (i*degrees) / 180.0 * Math.PI;
        double c = Math.cos(d);
        double s = Math.sin(d);
        turtle_x[i][0] = (int)((-5)*c + (-5)*s + 0.5);
        turtle_y[i][0] = (int)((-5)*c - (-5)*s + 0.5);
        turtle_x[i][1] = (int)(10*c + 0.5);
        turtle_y[i][1] = (int)(-10*s + 0.5);
        turtle_x[i][2] = (int)((-5)*c + 5*s + 0.5);
        turtle_y[i][2] = (int)(5*c - (-5)*s + 0.5);
        turtle_x[i][3] = turtle_x[i][0];
        turtle_y[i][3] = turtle_y[i][0];
     }
  }
  
  TTurtleHandler() {
     setBackground(Color.white);
     setLayout(null);
     yesButton = new Button("   YES   ");
     noButton = new Button("   NO   ");
     okButton = new Button("   OK   ");
     userInput = new TextField(15);
     yesButton.hide();
     noButton.hide();
     okButton.hide();
     userInput.hide();
     add(yesButton);
     add(noButton);
     add(okButton);
     add(userInput);
  }
  
  void abortInput() {
     processWantsIO = null;
     doneInput(Double.NaN);
  }
  
  synchronized private void setWaitingForIO(int n) {
     waitingForIO = n;
  }
  
  synchronized int getWaitingForIO() {
     return waitingForIO;
  }
  
  synchronized private void doneInput(double input) {
     switch (waitingForIO) {
        case WaitingForYesNo:
          yesButton.hide();
          noButton.hide();
          break;
        case WaitingForAskUser:
          userInput.hide();
          userInput.setText("");
          // no break!
        case WaitingForTellUser:
          okButton.hide();
     }
     waitingForIO = -1;
//     Graphics g = getGraphics();
//     paint(g);
//     g.dispose();

     repaint();
     try { wait(50); }
     catch (InterruptedException e) { }

     if (processWantsIO != null) {
        processWantsIO.doneInput(input);
        processWantsIO = null;
     }
  }
  
  void DoAskUser(TProcess proc, String message) {
     setWaitingForIO(WaitingForAskUser);
     IOPrompt = makeStringList(message);
     processWantsIO = proc;
     arrangeComponentsForIO();
  }
  
  void DoTellUser(TProcess proc, String message) {
     setWaitingForIO(WaitingForTellUser);
     IOPrompt = makeStringList(message);
     processWantsIO = proc;
     arrangeComponentsForIO();
  }
  
  void DoYesOrNo(TProcess proc, String message) {
     setWaitingForIO(WaitingForYesNo);
     IOPrompt = makeStringList(message);
     processWantsIO = proc;
     arrangeComponentsForIO();
  }
  
  synchronized private void arrangeComponentsForIO() {
 //   Graphics g = getGraphics();
 //    paint(g);
 //    g.dispose();
 
     repaint();
     try { wait(50); }
     catch (InterruptedException e) { }
 
     int lineHeight = fontData.getHeight();
     int h = 25 + lineHeight * (1 + IOPrompt.length) - fontData.getLeading();
     int top = ((size().height - h) / 2) + h + 20;
     if (top > size().height - 20)
        top = size().height - 20;
     int right = size().width - 50;
     Dimension s;
     switch (waitingForIO) {
        case WaitingForTellUser:
           s = okButton.preferredSize();
           okButton.reshape(right-s.width,top,s.width,s.height);
           okButton.show();
           break;
        case WaitingForAskUser:
           s = okButton.preferredSize();
           okButton.reshape(right-s.width,top,s.width,s.height);
           right -= s.width + 20;
           s = userInput.preferredSize();
           userInput.reshape(right-s.width,top,s.width,s.height);
           okButton.show();
           userInput.show();
           userInput.requestFocus();
           break;
        case WaitingForYesNo:
           s = yesButton.preferredSize();
           yesButton.reshape(right-s.width,top,s.width,s.height);
           right -= s.width + 20;
           s = noButton.preferredSize();
           noButton.reshape(right-s.width,top,s.width,s.height);
           yesButton.show();
           noButton.show();
           break;
     }
  }
  
  synchronized boolean getAlwaysHideTurtles() {
     return alwaysHideTurtles;
  }
  
  synchronized void setAlwaysHideTurtles(boolean hideThem, boolean graphicsVisible) {
      if (hideThem == alwaysHideTurtles)
         return;
      alwaysHideTurtles = hideThem;
      if (turtles.size() == 0)
         return;
      if (!graphicsVisible)
         return;
      Graphics g = getGraphics();
      if (hideThem) {
         if (OSC != null)
            g.drawImage(OSC,0,0,this);
         else {
            g.setColor(Color.black);
            g.setXORMode(Color.white);
            for (int i=0; i < turtles.size(); i++) {
               TTurtleRef tr = (TTurtleRef)turtles.elementAt(i);
               g.fillPolygon(tr.x,tr.y,4);
            }
            g.setPaintMode();
         }
      }
      else
         redrawTurtles(g,-5,-5,size().width+5,size().height+5);
      g.dispose();
  }
  
  synchronized void start() {
     checkSize();
     if (firststart) {
       DrawTurtle(0,0,0);
       firststart = false;
     }
  }
  
  synchronized void stop() {
     OSC = null;
     OSG = null;
     width = -1;
  }
  
  private void checkSize() {
     if (width !=size().width || height != size().height) {
        OSC = null;
        width = size().width;
        height = size().height;
        try {
           OSC = createImage(width,height);
           OSG = OSC.getGraphics();
           OSG.setColor(Color.white);
           OSG.fillRect(0,0,width,height);
           OSG.setFont(getFont());
        }
        catch (OutOfMemoryError e) {
           OSC = null;
           OSG = null;
        }
        if (width > height) {
           pixelSize = (2.0*basicSize) / (height-1);
           ymin = -basicSize;
           ymax = basicSize;
           xmin = ((double)(-basicSize * (width-1))) / (height-1);
           xmax = -xmin;
        }
        else {
           pixelSize = (2.0*basicSize) / (width-1);
           xmin = -basicSize;
           xmax = basicSize;
           ymin = ((double)(-basicSize * (height-1))) / (width-1);
           ymax = -xmin;
        }
        scale = 1.0 / pixelSize;
     }
  }
  
  private void convert(double x, double y) {
     xint = (int)((x-xmin)*scale);
     yint = (int)((ymax-y)*scale);
  }

  synchronized void PutLine(Color c, double x0, double y0, double x1, double y1) {
     Graphics g = getGraphics();
     g.setColor(c);
     convert(x0,y0);
     int a = xint;
     int b = yint;
     convert(x1,y1);
     g.drawLine(a,b,xint,yint);
     g.dispose();
     if (OSG != null) {
        OSG.setColor(c);
        OSG.drawLine(a,b,xint,yint);
     }
  }
  
  synchronized void PutText(Color c, double a, double b, String message) {
     Graphics g = getGraphics();
     g.setColor(c);
     convert(a,b);
     g.drawString(message,xint,yint);
     g.dispose();
     if (OSG != null) {
        OSG.setColor(c);
        OSG.drawString(message,xint,yint);
     }
  }
  
  synchronized void PutArc(Color c, double left, double top, double right, double bottom, double startAngle, double angle) {
     convert(left, top);
     int a = xint;
     int b = yint;
     convert(right,bottom);
     int start = (int)(startAngle+0.5);
     int theta = (int)(angle+0.5);
     Graphics g = getGraphics();
     g.setColor(c);
     g.drawArc(a,b,xint-a,yint-b,start,theta);
     g.dispose();
     if (OSG != null) {
        OSG.setColor(c);
        OSG.drawArc(a,b,xint-a,yint-b,start,theta);
     }
  }
  
  synchronized void ClearScreen() {
     turtles.removeAllElements();
     if (OSG != null) {
        OSG.setColor(Color.white);
        OSG.fillRect(0,0,width,height);
     }
     errorMessageList = null;
     repaint();
     try { wait(50); }
     catch (InterruptedException e) { }
//     Graphics g = getGraphics();
//     g.setColor(Color.white);
//     g.fillRect(0,0,width,height);
//     g.dispose();
  }
  
  private void putTurtle(Graphics g, TTurtleRef tr) {
     g.setColor(Color.black);
     if (OSC == null)
        g.setXORMode(Color.white);
     g.fillPolygon(tr.x,tr.y,4);
     if (OSC == null)
        g.setPaintMode();
  }
  
  synchronized Object DrawTurtle(double x, double y, double heading) {
     convert(x,y);
     if (xint < -5 || yint < -5 || xint > width + 5 || yint > height + 5)
        return null;
     int i;
     if (heading >= 0)
        i = (int)((heading + 5) / (360.0/turtle_ct));
     else
        i = (int)((heading + 365) / (360.0/turtle_ct));
     if (i < 0 || i >= turtle_ct)  // shouldn't be possible
        i = Math.abs(i % turtle_ct);
     TTurtleRef tr = new TTurtleRef();
     for (int p=0; p < 4; p++) {
        tr.x[p] = turtle_x[i][p] + xint;
        tr.y[p] = turtle_y[i][p] + yint;
     }
     tr.center_x = xint;
     tr.center_y = yint;
     turtles.addElement(tr);
     if (!alwaysHideTurtles) {
       Graphics g = getGraphics();
       putTurtle(g,tr);
       g.dispose();
     }
     return tr;
  }

  synchronized void RemoveTurtle(Object turtleRef) {
     int loc = turtles.indexOf(turtleRef);
     if (loc < 0) // shouldn'd happen
        return;
     turtles.removeElementAt(loc);
     if (alwaysHideTurtles)
        return;
     TTurtleRef tr = (TTurtleRef)turtleRef;
     Graphics g = getGraphics();
     if (OSC == null) {
        g.setColor(Color.black);
        g.setXORMode(Color.white);
        g.fillPolygon(tr.x,tr.y,4);
        g.setPaintMode();
        g.dispose();
        return;
     }
     int x0=tr.center_x, y0=tr.center_y, x1=x0, y1=y0;
     for (int p=0; p < 4; p++) {
        x0 = Math.min(x0,tr.x[p]);
        x1 = Math.max(x1,tr.x[p]);
        y0 = Math.min(y0,tr.y[p]);
        y1 = Math.max(y1,tr.y[p]);
     }
     g.clipRect(x0,y0,x1-x0+1,y1-y0+1);
     g.drawImage(OSC,0,0,this);
     redrawTurtles(g,x0-5,y0-5,x1+5,y1+5);
     g.dispose();
  }
  
  synchronized void ScrollToTurtle(double x, double y) {
  }
  
  synchronized void ScrollToHome() {
  }
  
  synchronized public void startRunning(TProcess programThread) {
     checkSize();
     turnOffError();
  }
  
  synchronized public void errorReport(String errorMessage, int position) {
     errorMessageList = makeStringList(errorMessage);
     repaint();
  }
  
  synchronized boolean turnOffError() {
     if (errorMessageList == null)
        return false;
     if (errorMessageList != null) {
        errorMessageList = null;
        Graphics g = getGraphics();
        paint(g);
        g.dispose();
     }
     return true;
  }
  
  synchronized public void doneRunning() {
     if (waitingForIO >= 0)
        abortInput();
     if (OSC != null)
        repaint();
  }
  
  synchronized public void paint(Graphics g) {
     if (waitingForIO >= 0) {
        g.setColor(Color.white);
        g.fillRect(0,0,size().width,size().height);
        drawMessage(g,IOPrompt,promptHeader[waitingForIO],darkGreen,lightGreen,Color.black);
        return;
     }
     if (OSC != null)
        g.drawImage(OSC,0,0,this);
     else {
        g.setColor(Color.white);
        g.fillRect(0,0,size().width,size().height);
     }
     if (!alwaysHideTurtles) 
        redrawTurtles(g,-5,-5,size().width+5,size().height+5);
     if (errorMessageList != null)
        drawMessage(g,errorMessageList,"*** ERROR ***",Color.red,Color.black,Color.white);
  }
  
  synchronized void redrawTurtles(Graphics g, int x0, int y0, int x1, int y1) {
     int size = turtles.size();
     for (int i=0; i < size; i++) {
        TTurtleRef tr = (TTurtleRef)turtles.elementAt(i);
        if (x0 <= tr.center_x && tr.center_x <= x1
                 && y0 <= tr.center_y && tr.center_y <= y1)
            putTurtle(g,tr);
     };
  }
  
  synchronized void removeAllTurtles() {
      boolean save = alwaysHideTurtles;
      setAlwaysHideTurtles(true,true);
      turtles.removeAllElements();
      alwaysHideTurtles = save;
  }
  
  public void update(Graphics g) {
     paint(g);
  }
  
  public boolean mouseDown(Event evt, int x, int y) {
     if (errorMessageList != null && getWaitingForIO() < 0) {
        errorMessageList = null;
        repaint();
     }
     return true;
  }
  
//  public boolean gotFocus(Event evt, Object what) {
//     if (getWaitingForIO() == WaitingForAskUser) {
//        userInput.selectAll();
//        userInput.requestFocus();
//        return true;
//     }
//     return false;
//  }
  
  public boolean action(Event evt, Object arg) {
     if (evt.target == yesButton) {
        doneInput(1.0);
        return true;
     }
     else if (evt.target == noButton) {
        doneInput(0.0);
        return true;
     }
     else if (evt.target == okButton && getWaitingForIO() == WaitingForTellUser) {
        doneInput(Double.NaN);
        return true;
     }
     else if (evt.target == okButton || evt.target == userInput) {
        double d;
        String str = userInput.getText();
        try {
            Double D = new Double(str);
            d = D.doubleValue();
        }
        catch (NumberFormatException e) {
            d = Double.NaN;
        }
        if (Double.isNaN(d)) {
           userInput.setText("Illegal input!");
           userInput.selectAll();
           userInput.requestFocus();
        }
        else
           doneInput(d);
        return true;
     }
     else
        return super.handleEvent(evt);
  }
  
  private String[] makeStringList(String str) {
     int w = size().width - 80;
     if (fontData == null) {
        Font f = getFont();
        fontData = getFontMetrics(f);
     }
     char[] ch = str.toCharArray();
     int[] offsets = new int[25];
     offsets[0] = 0;
     int ct = 0;
     int pos = 0;
     int lastBlank = -1;
     StringBuffer buf = new StringBuffer();
     while (pos < str.length()) {
        buf.append(ch[pos]);
        int cw = fontData.stringWidth(buf.toString());
        if (cw > w) {
           if (ct == offsets.length - 2)
              break;
           ct++;
           buf.setLength(0);
           if (lastBlank == -1)
              offsets[ct] = pos;
           else {
              offsets[ct] = lastBlank + 1;
              for (int i = lastBlank + 1; i <= pos; i++)
                 buf.append(ch[i]);
           }
           lastBlank = -1;
        }
        if (ch[pos] == ' ')
           lastBlank = pos;
        pos++;
     }
     if (pos > offsets[ct]) {
        ct++;
        offsets[ct] = pos;
     }
     String[] list = new String[ct];
     for (int i=0; i<ct; i++)
       list[i] = new String(ch,offsets[i],offsets[i+1]-offsets[i]);
     return list;
  }
  
  private void drawMessage(Graphics g, String[] list, String topString, Color frameColor, Color backgroundColor, Color textColor) {
     int w = size().width - 60;
     int offset = fontData.getAscent();
     int lineHeight = fontData.getHeight();
     int h = 25 + lineHeight * (1 + list.length) - fontData.getLeading();
     int top = (size().height - h) / 2;
     g.setColor(backgroundColor);
     g.fillRect(30,top,w,h);
     if (frameColor != null) {
       g.setColor(frameColor);
       g.drawRect(30,top,w,h);
       g.drawRect(31,top+1,w-2,h-2);
     }
     g.drawLine(30,top+offset+8,w+29,top+offset+8);
     g.setColor(textColor);
     g.drawString(topString,
                    30 + (w -fontData.stringWidth(topString)) / 2,
                    4 + top + offset);
     for (int i = 0; i < list.length; i++)
        g.drawString(list[i], 40, top+offset+(i+1)*lineHeight+15);     
  }

}