
package tmcm.xLogicCircuits;

import java.awt.*;
import java.util.Vector;



abstract class IONub extends CircuitItem {
   static final int INPUT = 0, OUTPUT = 1, TACK = 2;
   final static Color lineDestinationColor = new Color(0,150, 0);
   final static Color lineSourceColor = new Color(150,0,150);
   int kind;
   int connect_x, connect_y;
   Line source;
   boolean changed; // used only in circuitCanvas.run() to determine whether to draw an output nub
   Vector destination = new Vector();  // vector of lines
   void drawWithLines(Graphics g) { 
      draw(g);
      if (source != null && kind != INPUT)  // don't draw source line for inputs, because the line is in the containing circuit
         source.draw(g);
      if (kind != OUTPUT)  // for outputs, destination lines are in containing circuit
         for (int i = 0; i < destination.size(); i++)
            ((Line)destination.elementAt(i)).draw(g);
   }
   void selectConnectedLines(boolean select) { 
      if (source != null)
         source.selected = select;
      for (int i = 0; i < destination.size(); i++)
         ((Line)destination.elementAt(i)).selected = select;
   }
   Rectangle getCopyOfBoundingBox(boolean addInLines) { 
      FloatRect r = new FloatRect(boundingBox.x,boundingBox.y,boundingBox.width,boundingBox.height);
      if (addInLines) {
         if (source != null)
            r.add(source.boundingBox);
         for (int i = 0; i < destination.size(); i++)
            r.add(((Line)destination.elementAt(i)).boundingBox);
      }
      r.grow(3,3);
      return r.getIntRect();
   }
   IONub getLineSource(int x, int y) {
      if (kind == OUTPUT)
         return null;
      if (x < boundingBox.x - 2 || x > boundingBox.x + boundingBox.width + 2 ||
             y < boundingBox.y - 2|| y > boundingBox.y + boundingBox.height + 2)
         return null;
      return this;
   }
   IONub getLineDestination(int x, int y) {
      if (kind == INPUT || source != null)
         return null;
      if (x < boundingBox.x - 2 || x > boundingBox.x + boundingBox.width + 2 ||
             y < boundingBox.y - 2|| y > boundingBox.y + boundingBox.height + 2)
         return null;
      return this;
   }
}

class Tack extends IONub {
   Tack() {
      kind = TACK;
   }
   void draw(Graphics g) {
      if (selected) {
         g.setColor(Color.blue);
         g.fillOval(Math.round(boundingBox.x)-2, Math.round(boundingBox.y)-2, 
                               Math.round(boundingBox.width)+4, Math.round(boundingBox.height)+4);
      }
      if (source == null)
         g.setColor(lineDestinationColor);
      else
         g.setColor(lineSourceColor);
      g.fillOval(Math.round(boundingBox.x),Math.round(boundingBox.y),Math.round(boundingBox.width),Math.round(boundingBox.height));
   }
   CircuitItem copy() {  // copied without source and destination
      Tack it = new Tack();
      it.selected = selected;
      it.on = on;
      it.reshape(boundingBox.x,boundingBox.y,boundingBox.width,boundingBox.height);
      it.kind = kind;
      it.connect_x = connect_x;
      it.connect_y = connect_y;
      return it;
   }
   void reshape(float x, float y, float width, float height) { 
      boundingBox.reshape(x,y,width,height); 
      connect_x = Math.round(x)+2;
      connect_y = Math.round(y)+2;
      if (source != null)
         source.setBoundingBox();
      for (int i = 0; i < destination.size(); i++)
         ((Line)destination.elementAt(i)).setBoundingBox();
   }
   void delete(Circuit owner) {
     owner.items.removeElement(this);
     for (int i = 0; i < destination.size(); i++) {
        Line line = (Line)destination.elementAt(i);
        owner.lines.removeElement(line);
        line.destination.source = null;           
     }
     if (source != null) {
        owner.lines.removeElement(source);
        source.source.destination.removeElement(source);
     }
   }
   void unDelete(Circuit owner) {
     owner.items.addElement(this);
     for (int i = 0; i < destination.size(); i++) {
        Line line = (Line)destination.elementAt(i);
        owner.lines.addElement(line);
        line.destination.source = line;           
     }
     if (source != null) {
        owner.lines.addElement(source);
        source.source.destination.addElement(source);
     }
   }
}

class CircuitIONub extends IONub {

   final static int ONRIGHT = 0, ONTOP = 1, ONLEFT = 2, ONBOTTOM = 3;  // order is coordinated with order of constants in class Gate
   int side;
   float absolutePosition;

   CircuitIONub(int side, double position, boolean isInput) {
      this.side = side;
      this.absolutePosition = (float)position;
      this.kind = (isInput? INPUT : OUTPUT);
   }
   
   private void putNub(Graphics g, int pos, boolean source) {
      switch (pos) {
         case ONTOP: g.drawLine(connect_x,connect_y+1,connect_x,connect_y+4); break;
         case ONLEFT: g.drawLine(connect_x+1,connect_y,connect_x+4,connect_y); break;
         case ONRIGHT: g.drawLine(connect_x-4,connect_y,connect_x-1,connect_y); break;
         case ONBOTTOM: g.drawLine(connect_x,connect_y-4,connect_x,connect_y-1); break;
      }
      if (source) {
         switch (pos) {
            case ONTOP:g.drawLine(connect_x-1,connect_y+1,connect_x,connect_y); 
                         g.drawLine(connect_x,connect_y,connect_x+1,connect_y+1); break;
            case ONLEFT:g.drawLine(connect_x+1,connect_y-1,connect_x,connect_y); 
                         g.drawLine(connect_x,connect_y,connect_x+1,connect_y+1); break;
            case ONRIGHT: g.drawLine(connect_x-1,connect_y-1,connect_x,connect_y); 
                         g.drawLine(connect_x,connect_y,connect_x-1,connect_y+1); break;
            case ONBOTTOM: g.drawLine(connect_x-1,connect_y-1,connect_x,connect_y); 
                        g.drawLine(connect_x,connect_y,connect_x+1,connect_y-1); break;
         }
      }
      else {
         switch (pos) {
            case ONTOP:g.drawLine(connect_x-1,connect_y,connect_x,connect_y+1); 
                         g.drawLine(connect_x,connect_y+1,connect_x+1,connect_y); break;
            case ONLEFT:g.drawLine(connect_x,connect_y-1,connect_x+1,connect_y); 
                         g.drawLine(connect_x+1,connect_y,connect_x,connect_y+1); break;
            case ONRIGHT: g.drawLine(connect_x-1,connect_y,connect_x,connect_y-1); 
                         g.drawLine(connect_x-1,connect_y,connect_x,connect_y+1); break;
            case ONBOTTOM: g.drawLine(connect_x-1,connect_y,connect_x,connect_y-1); 
                        g.drawLine(connect_x,connect_y-1,connect_x+1,connect_y); break;
         }
      }
   }
   
   void drawIconified(Graphics g) {
      if (kind == INPUT)
         g.setColor(lineDestinationColor);
      else
         g.setColor(lineSourceColor);
      putNub(g,side,kind == OUTPUT);
   }
   
   final void drawCenter(Graphics g) {
      if (on)
         g.setColor(Color.red);
      else
         g.setColor(Color.black);
      switch (side) {
         case ONTOP: g.fillOval(Math.round(boundingBox.x)+2,Math.round(boundingBox.y)+2,Math.round(boundingBox.width)-4,Math.round(boundingBox.height)-7); break;
         case ONLEFT: g.fillOval(Math.round(boundingBox.x)+2,Math.round(boundingBox.y)+2,Math.round(boundingBox.width)-7,Math.round(boundingBox.height)-4); break;
         case ONRIGHT: g.fillOval(Math.round(boundingBox.x)+5,Math.round(boundingBox.y)+2,Math.round(boundingBox.width)-7,Math.round(boundingBox.height)-4); break;
         case ONBOTTOM: g.fillOval(Math.round(boundingBox.x)+2,Math.round(boundingBox.y)+5,Math.round(boundingBox.width)-4,Math.round(boundingBox.height)-7); break;
      }
   }
   
   void draw(Graphics g) {
      if (kind == OUTPUT)
         g.setColor(lineDestinationColor);
      else
         g.setColor(lineSourceColor);
      switch (side) {
         case ONTOP: g.fillOval(Math.round(boundingBox.x),Math.round(boundingBox.y),Math.round(boundingBox.width),Math.round(boundingBox.height)-3); break;
         case ONLEFT: g.fillOval(Math.round(boundingBox.x),Math.round(boundingBox.y),Math.round(boundingBox.width)-3,Math.round(boundingBox.height)); break;
         case ONRIGHT: g.fillOval(Math.round(boundingBox.x)+3,Math.round(boundingBox.y),Math.round(boundingBox.width)-3,Math.round(boundingBox.height)); break;
         case ONBOTTOM: g.fillOval(Math.round(boundingBox.x),Math.round(boundingBox.y)+3,Math.round(boundingBox.width),Math.round(boundingBox.height)-3); break;
      }
      putNub(g,(side+2)%4,kind == INPUT);
      drawCenter(g);
      if (selected) {
         g.setColor(Color.blue);
         g.drawRect(Math.round(boundingBox.x),Math.round(boundingBox.y),Math.round(boundingBox.width),Math.round(boundingBox.height));
      }
   }

   void getCoordsIconified(float x, float y, float width, float height) {
      switch (side) {
         case ONTOP:
            connect_x = Math.round(x + 5 + (absolutePosition*(width-10)));
            connect_y = Math.round(y);
            break;
         case ONLEFT:
            connect_x = Math.round(x);
            connect_y = Math.round(y + 5 + (absolutePosition*(height-10)));
            break;
         case ONRIGHT:
            connect_x = Math.round(x + width);
            connect_y = Math.round(y + 5 + (absolutePosition*(height-10)));
            break;
         case ONBOTTOM:
            connect_x = Math.round(x + 5 + (absolutePosition*(width-10)));
            connect_y = Math.round(y + height);
            break;
      }
      if (kind == INPUT) {
        if (source != null)
           source.setBoundingBox();
      }
      else {
         for (int i = 0; i < destination.size(); i++)
            ((Line)destination.elementAt(i)).setBoundingBox();
      }
   }
   
   void getCoords(float x, float y, float width, float height) {
      switch (side) {
         case ONTOP:
            connect_x = Math.round(x + 5 + (absolutePosition*(width-10)));
            connect_y = Math.round(y + 8);
            boundingBox.reshape(connect_x-5,connect_y-13,10,13);
            break;
         case ONLEFT:
            connect_x = Math.round(x + 8);
            connect_y = Math.round(y + 5 + (absolutePosition*(height-10)));
            boundingBox.reshape(connect_x-13,connect_y-5,13,10);
            break;
         case ONRIGHT:
            connect_x = Math.round(x + width - 8);
            connect_y = Math.round(y + 5 + (absolutePosition*(height-10)));
            boundingBox.reshape(connect_x,connect_y-5,13,10);
            break;
         case ONBOTTOM:
            connect_x = Math.round(x + 5 + (absolutePosition*(width-10)));
            connect_y = Math.round(y + height - 8);
            boundingBox.reshape(connect_x-5,connect_y,10,13);
            break;
      }
      if (kind == OUTPUT) {
         if (source != null) 
            source.setBoundingBox();
      }
      else {
         for (int i = 0; i < destination.size(); i++)
            ((Line)destination.elementAt(i)).setBoundingBox();
      }
   }
   
   void reshape(float x, float y, float width, float height) {  // called only during creation/dragging in scroller
      boundingBox.reshape(x,y,width,height);
      switch (side) {
         case ONTOP:  connect_x = Math.round(x+5); connect_y = Math.round(y+13); break;
         case ONLEFT:  connect_x = Math.round(x+13); connect_y = Math.round(y+5); break;
         case ONRIGHT:  connect_x = Math.round(x); connect_y = Math.round(y+5); break;
         case ONBOTTOM:  connect_x = Math.round(x+5); connect_y = Math.round(y); break;
      }
   }

   void dragTo(float x, float y, FloatRect circuitBounds) {
      float dist = y + 5 - circuitBounds.y;
      side = ONTOP;
      if (x + 5 - circuitBounds.x < dist) {
         dist = x + 5 - circuitBounds.x;
         side = ONLEFT;
      }
      if (circuitBounds.x + circuitBounds.width - (x+10) < dist) {
         dist = circuitBounds.x + circuitBounds.width - (x+10);
         side = ONRIGHT;
      }
      if (circuitBounds.y + circuitBounds.height - (y+10) < dist) {
         dist = circuitBounds.y + circuitBounds.height - (y+10);
         side = ONBOTTOM;
      }
      if (side == ONTOP || side == ONBOTTOM) {
         x = Math.min(x,Math.round(circuitBounds.width+circuitBounds.x)-15);
         x = Math.max(x,10);
         absolutePosition = x / circuitBounds.width;
      }
      else {
         y = Math.min(y,Math.round(circuitBounds.height+circuitBounds.y-15));
         y = Math.max(y,10);
         absolutePosition = y / circuitBounds.height;
      }
      getCoords(Math.round(circuitBounds.x), Math.round(circuitBounds.y), 
                       Math.round(circuitBounds.width), Math.round(circuitBounds.height));
   }
      
   void copyDataInto(CircuitIONub it) {
      it.selected = selected;
      it.on = on;
      it.reshape(boundingBox.x,boundingBox.y,boundingBox.width,boundingBox.height);
      it.connect_x = connect_x;
      it.connect_y = connect_y;
   }

   CircuitItem copy() {  // copied without source and destination
      CircuitIONub it = new CircuitIONub(side,absolutePosition,kind==INPUT);
      copyDataInto(it);
      return it;
   }
   
   void delete(Circuit owner) {
      on = false;
      if (kind == INPUT) {
         owner.inputs.removeElement(this);
         for (int i = 0; i < destination.size(); i++) {
            Line line = (Line)destination.elementAt(i);
            owner.lines.removeElement(line);
            line.destination.source = null;           
         }
         if (owner.saveContainerWhileEnlarged != null && source != null) {  // source line, if any, is in the containing circuit!
            owner.saveContainerWhileEnlarged.lines.removeElement(source);
            source.source.destination.removeElement(source);
         }
      }
      else {
         owner.outputs.removeElement(this);
         if (source != null) {
            owner.lines.removeElement(source);
            source.source.destination.removeElement(source);
         }
         if (owner.saveContainerWhileEnlarged != null) {  // source line, if any, is in the containing circuit!
            for (int i = 0; i < destination.size(); i++) {
               Line line = (Line)destination.elementAt(i);
               owner.saveContainerWhileEnlarged.lines.removeElement(line);
               line.destination.source = null;
            }
         }
      }
   }
   
   void unDelete(Circuit owner) {
      if (kind == INPUT) {
         owner.inputs.addElement(this);
         for (int i = 0; i < destination.size(); i++) {
            Line line = (Line)destination.elementAt(i);
            owner.lines.addElement(line);
            line.destination.source = line;           
         }
         if (owner.saveContainerWhileEnlarged != null && source != null) {  // source line, if any, is in the containing circuit!
            owner.saveContainerWhileEnlarged.lines.addElement(source);
            source.source.destination.addElement(source);
         }
      }
      else {
         owner.outputs.addElement(this);
         if (source != null) {
            owner.lines.addElement(source);
            source.source.destination.addElement(source);
         }
         if (owner.saveContainerWhileEnlarged != null) {  // source line, if any, is in the containing circuit!
            for (int i = 0; i < destination.size(); i++) {
               Line line = (Line)destination.elementAt(i);
               owner.saveContainerWhileEnlarged.lines.addElement(line);
               line.destination.source = line;
            }
         }
      }
   }
   
}

