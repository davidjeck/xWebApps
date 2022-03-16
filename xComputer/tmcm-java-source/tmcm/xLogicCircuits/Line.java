
package tmcm.xLogicCircuits;

import java.awt.*;
import java.util.Vector;

class Line extends CircuitItem {
   IONub source, destination;
   int pos; // position in vector; just for use in Circuit.copy() and saving files
   boolean changed;  // for use in deciding whether to draw this
   Line (IONub source, IONub destination) {
      this.source = source;
      this.destination = destination;
      if (source != null) {
         source.destination.addElement(this);
         on = source.on;
      }
      if (destination != null)
         destination.source = this;
      setBoundingBox();
   }
   void setBoundingBox() {
      if (source != null && destination != null) {
         boundingBox.reshape(source.connect_x,source.connect_y,1,1);
         boundingBox.add(destination.connect_x,destination.connect_y);
      }
   }
   void draw(Graphics g) {
      if (on)
         if (selected)
            g.setColor(Color.magenta);
         else
            g.setColor(Color.red);
      else
         if (selected)
            g.setColor(Color.blue);
         else
            g.setColor(Color.black);
      g.drawLine(source.connect_x,source.connect_y,destination.connect_x,destination.connect_y);
/*      double x = destination.connect_x;  // draw "Arrowhead"
      double y = destination.connect_y;
      double dx = x - source.connect_x;
      double dy = y - source.connect_y;
      double len = Math.sqrt(dx*dx + dy*dy);
      if (destination instanceof Tack) {
         x = x - 2.0*dx/len;
         y = y - 2.0*dy/len;
         len -= 2;
      }
      if (len <= 25)
         return;
      dx = 3.5*dx/len;
      dy = 3.5*dy/len;
      g.drawLine((int)(x+0.5),(int)(y+0.5),(int)(x - dx - dy + 0.5),(int)(y - dy + dx + 0.5));
      g.drawLine((int)(x+0.5),(int)(y+0.5),(int)(x - dx + dy + 0.5),(int)(y - dy - dx + 0.5));      */
   }
   boolean compute() {  // called only by source IONub; passes on value to destination
      changed = (on != source.on);
      on = source.on;
      return changed;
   }
   CircuitItem copy() {  // copied without source and destination
      Line it = new Line(null,null);
      it.selected = selected;
      it.on = on;
      it.reshape(boundingBox.x,boundingBox.y,boundingBox.width,boundingBox.height);
      return it;
   }
   Rectangle getCopyOfBoundingBox(boolean addInLines) { 
       Rectangle r = new Rectangle( Math.round(boundingBox.x),Math.round(boundingBox.y),
                            Math.round(boundingBox.width),Math.round(boundingBox.height) ); 
       r.grow(2,2);
       return r;
   }
   boolean hit(int x, int y) { 
      boundingBox.grow(3,3);
      boolean firstPass = boundingBox.inside(x,y);
      boundingBox.grow(-3,-3);
      if (!firstPass)
         return false;
      int ny = source.connect_x - destination.connect_x;
      int nx = -(source.connect_y - destination.connect_y);
      if (nx == 0 || ny == 0)
         return true;
      int len = nx*(x-source.connect_x) + ny*(y-source.connect_y);
      double dist = Math.abs(len / Math.sqrt(nx*nx + ny*ny));
      return (dist <= 3);
   }
   void delete(Circuit owner) {
      on = false;
      owner.lines.removeElement(this);
      if (source != null)
         source.destination.removeElement(this);
      if (destination != null) {
         destination.source = null;
         destination.on = false;
      }
   }
   void unDelete(Circuit owner) {
      owner.lines.addElement(this);
      if (source != null)
         source.destination.addElement(this);
      if (destination != null)
         destination.source = this;
   }
}