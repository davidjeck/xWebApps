
package tmcm.xLogicCircuits;

import java.awt.*;
import java.util.Vector;

abstract class CircuitItem {
   boolean on, selected;
   FloatRect boundingBox = new FloatRect();
   boolean hit(int x, int y) { return boundingBox.inside(x,y); }
   void reshape(float x, float y, float width, float height) { boundingBox.reshape(x,y,width,height); }
   void drawWithLines(Graphics g) { draw(g); }
   void selectConnectedLines(boolean select) { }
   void powerOff() { on = false; }
   IONub getLineSource(int x, int y) { return null; }
   IONub getLineDestination(int x, int y) { return null; }
   boolean compute() { return false; };  // returns true if anything visible element changed
   void dragTo(float x, float y, FloatRect circuitBounds) { // called ONLY when dragging item that is on circuit (not in scroller)
      if (x + boundingBox.width > circuitBounds.x + circuitBounds.width - 10)
         x = circuitBounds.x + circuitBounds.width - 5 - boundingBox.width;
      if (y + boundingBox.height > circuitBounds.y + circuitBounds.height - 10)
         y = circuitBounds.y + circuitBounds.height - 5 - boundingBox.height;
      x = Math.max(x, circuitBounds.x + 5);
      y = Math.max(y, circuitBounds.y + 5);
      reshape(x,y,boundingBox.width,boundingBox.height);
   }
   abstract Rectangle getCopyOfBoundingBox(boolean addInLines);
   abstract void draw(Graphics g);
   abstract CircuitItem copy();
   abstract void delete(Circuit owner);
   abstract void unDelete(Circuit owner);
}

final class FloatRect {
   float x,y,width,height;
   FloatRect() {
      reshape(0,0,0,0);
   }
   FloatRect(float x, float y, float width, float height) {
      reshape(x,y,width,height);
   }
   void reshape(float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
      this.width = Math.max(0,width);
      this.height = Math.max(0,height);
   }
   Rectangle getIntRect() {
      return new Rectangle(Math.round(x),Math.round(y),Math.round(width),Math.round(height));
   }
   boolean inside(float a, float b) {
      return (a >= x && b >= y && a < x+width && b < y+height);
   }
   void add(float a, float b) {
      if (a < x) {
         width += x - a;
         x = a;
      }
      else if (a > x+width)
         width = a - x;
      if (b < y) {
         height += y - b;
         y = b;
      }
      else if (b > y+height)
         height = b - y;
   }
   void add(FloatRect r) {
      add(r.x,r.y);
      add(r.x+r.width, r.y+r.height);
   }
   void grow(float dx, float dy) {
      x -= dx;
      y -= dy;
      width += 2*dx;
      height += 2*dy;
   }
}
