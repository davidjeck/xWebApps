
package tmcm.xLogicCircuits;

import java.awt.*;
import java.util.Vector;


public class Gate extends CircuitItem {

   static final int NOTGATE = 0, ORGATE = 1, ANDGATE = 2;  // types of gates
   static final int FACERIGHT = 0, FACEDOWN = 1, FACELEFT = 2, FACEUP = 3;  // directions they can face
   static final float standardVertexData[][][] = {   // vertices for right-facing gates
       { {0F,0F}, {0.85F,0.5F}, {0.9F,0.37F}, {0.95F,0.37F}, {1F,0.5F}, {0.95F,.63F}, {0.9F,0.63F},  {0.85F,0.5F}, {0F,1F} },
       { {0F,0F}, {0.31F,0.035F}, {0.55F,0.12F}, {0.88F,0.34F}, {1F,0.5F}, 
                {0.88F,0.66F}, {0.55F,0.88F}, {0.31F,0.965F}, {0F,1F}, 
                {0.1F,0.75F}, {0F,0.75F}, {0.1F,0.75F}, {0.12F,0.5F}, {0.1F,0.25F}, {0F,0.25F}, {0.1F,0.25F} },
       { {0F,0F}, {0.5F,0F}, {0.75F,0.07F}, {0.93F,0.25F}, {1F,0.45F}, {1F,0.55F}, {0.93F,0.75F}, {0.75F,0.93F}, {0.5F,1F}, {0F,1F} } };
   static float vertexData[][][][] = new float[4][][][];
   static {
      vertexData[0] = standardVertexData;
      for (int v = 1; v < 4; v++) {
         vertexData[v] = new float[3][][];
         for (int i = 0; i < 3; i++) {
            vertexData[v][i] = new float[standardVertexData[i].length][2];
            for (int j = 0; j < vertexData[v][i].length; j++) {
               vertexData[v][i][j][0] = vertexData[v-1][i][j][1];
               vertexData[v][i][j][1] = 1 - vertexData[v-1][i][j][0];
            }
         }
      }
   }
   
   int kind,facing;
   CircuitIONub in1, in2, out;
   
   Gate(int typeCode, int faceCode) {  
      kind = typeCode;
      facing = faceCode;
      out = new CircuitIONub(faceCode,0.5F,false);
      if (typeCode == NOTGATE) {
         in1 = new CircuitIONub((faceCode+2)%4,0.5F,true);
      }
      else {
         in1 = new CircuitIONub((faceCode+2)%4,0.25F,true);
         in2 = new CircuitIONub((faceCode+2)%4,0.75F,true);
      }
   }
   
   void draw(Graphics g) {
      if (selected)
         g.setColor(Color.blue);
      else
         g.setColor(Color.black);
      float[][] absoluteVertices = vertexData[facing][kind];
      int x0, y0, x1, y1;
      x0 = Math.round(5 + boundingBox.x + absoluteVertices[0][0]*(boundingBox.width-10));
      y0 = Math.round(5 + boundingBox.y + absoluteVertices[0][1]*(boundingBox.height-10));
      for (int i = 1; i < absoluteVertices.length; i++) {
         x1 = Math.round(5 + boundingBox.x + absoluteVertices[i][0]*(boundingBox.width-10));
         y1 = Math.round(5 + boundingBox.y + absoluteVertices[i][1]*(boundingBox.height-10));         
         g.drawLine(x0,y0,x1,y1);
         x0 = x1;
         y0 = y1;
      }
      g.drawLine(x0,y0,Math.round(5 + boundingBox.x + absoluteVertices[0][0]*(boundingBox.width-10)),
                              Math.round(5 + boundingBox.y + absoluteVertices[0][1]*(boundingBox.height-10)));
      in1.drawIconified(g);
      if (in2 != null)
         in2.drawIconified(g);
      out.drawIconified(g);
   }

   void reshape(float x, float y, float width, float height) {
      boundingBox.reshape(x,y,width,height);
      in1.getCoordsIconified(x,y,width,height);
      if (in2 != null)
         in2.getCoordsIconified(x,y,width,height);
      out.getCoordsIconified(x,y,width,height);
   }
   
   boolean compute() {
      if (kind == NOTGATE)
         out.on = !in1.on;
      else {
         if (kind == ANDGATE)
            out.on = in1.on && in2.on;
         else
            out.on = in1.on || in2.on;
      }
      return false;  // gates don't change visible appearance
   }
   
   void powerOff() {
      on = false;
      in1.on = false;
      if (in2 != null)
         in2.on = false;
      out.on = false;
   }

   void drawWithLines(Graphics g) { 
      draw(g);
      if (in1.source != null)
         in1.source.draw(g);
      if (in2 != null && in2.source != null)
         in2.source.draw(g);
      for (int i = 0; i < out.destination.size(); i++)
         ((Line)out.destination.elementAt(i)).draw(g);
   }
   
   void selectConnectedLines(boolean select) { 
      if (in1.source != null)
         in1.source.selected = select;
      if (in2 != null && in2.source != null)
         in2.source.selected = select;
      for (int i = 0; i < out.destination.size(); i++)
         ((Line)out.destination.elementAt(i)).selected = select;
   }
   
   Rectangle getCopyOfBoundingBox(boolean addInLines) { 
      FloatRect r = new FloatRect(boundingBox.x,boundingBox.y,boundingBox.width,boundingBox.height);
      if (addInLines) {
         if (in1.source != null)
            r.add(in1.source.boundingBox);
         if (in2 != null && in2.source != null)
            r.add(in2.source.boundingBox);
         for (int i = 0; i < out.destination.size(); i++)
            r.add(((Line)out.destination.elementAt(i)).boundingBox);
      }
      r.grow(1,1);
      return r.getIntRect();
   }

   IONub getLineSource(int x, int y) { 
      if (x < boundingBox.x - 2 || x > boundingBox.x + boundingBox.width + 2 ||
             y < boundingBox.y - 2|| y > boundingBox.y + boundingBox.height + 2)
         return null;
      return out;
   }
   
   IONub getLineDestination(int x, int y) { 
      if (in1.source != null && (in2 == null || in2.source != null))
         return null;
      if (x < boundingBox.x - 2 || x > boundingBox.x + boundingBox.width + 2 ||
             y < boundingBox.y - 2|| y > boundingBox.y + boundingBox.height + 2)
         return null;
      if (in2 == null)
         return in1;
      else if (in1.source != null)
         return in2;
      else if (in2.source != null)
         return in1;
      else {
         double d1 = (x-in1.connect_x)*(x-in1.connect_x) + (y-in1.connect_y)*(y-in1.connect_y); 
         double d2 = (x-in2.connect_x)*(x-in2.connect_x) + (y-in2.connect_y)*(y-in2.connect_y);
         if (d1 <= d2)
            return in1;
         else
            return in2;
      }
   }

   CircuitItem copy() {  // copied without source and destination
      Gate it = new Gate(kind,facing);
      it.selected = selected;
      it.on = on;
      it.reshape(boundingBox.x,boundingBox.y,boundingBox.width,boundingBox.height);
      in1.copyDataInto(it.in1);
      if (kind != NOTGATE)
         in2.copyDataInto(it.in2);
      out.copyDataInto(it.out);
      return it;
   }
   
   void delete(Circuit owner) {
      owner.items.removeElement(this);
      if (in1.source != null) {
         owner.lines.removeElement(in1.source);
         in1.source.source.destination.removeElement(in1.source);
      }
      if (in2 != null && in2.source != null) {
         owner.lines.removeElement(in2.source);
         in2.source.source.destination.removeElement(in2.source);
      }      
      for (int i = 0; i < out.destination.size(); i++) {
         for (int j = 0; j < out.destination.size(); j++) {
            Line line = (Line)out.destination.elementAt(i);
            owner.lines.removeElement(line);
            line.destination.source = null;           
         }
      }
   }

   void unDelete(Circuit owner) {
      owner.items.addElement(this);
      if (in1.source != null) {
         owner.lines.addElement(in1.source);
         in1.source.source.destination.addElement(in1.source);
      }
      if (in2 != null && in2.source != null) {
         owner.lines.addElement(in2.source);
         in2.source.source.destination.addElement(in2.source);
      }      
      for (int i = 0; i < out.destination.size(); i++) {
         for (int j = 0; j < out.destination.size(); j++) {
            Line line = (Line)out.destination.elementAt(i);
            owner.lines.addElement(line);
            line.destination.source = line;           
         }
      }
   }

}