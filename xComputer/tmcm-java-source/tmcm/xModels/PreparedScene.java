
package tmcm.xModels;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Vector;



class Line {  // simple object to hold coordinates of line endpoints
   short xL, yL, xR, yR;
   Line(short a, short b, short c, short d) {
      xL = a;
      yL = b;
      xR = c;
      yR = d;
   }
}



class PreparedScene {  // represents a list of lines and color changes that can
                       // be "played" into a graphics to draw a wireframe picture.
                       // The original scene is given in 3D world coordinates, but
                       // the stored scence is given in 2D, integral, window coords.

    final static double size = 20.0;  // image will be centered at (0,0) and will
                                      // include a square of this size

    private Vector item;  // lines and colors that make up the image
    
    private double viewDistance;   // The projection is from 3-space onto the xy plane;
                                   // If viewDistance is infinte, projection is from 
                                   // infinity; otherwise, it is from (0,0,viewDistance).
                                   
    private double xmax, ymax, xscale, yscale;  // window transformation data
    
    private Color currentColor;  // Color that has been added most recently to the scene
    
    
    PreparedScene(int width, int height, double viewDistance) {
            // prepare scene to draw in the window 0 <= x <= width, 0 <= y <= height,
            // using a projection from the specified viewing distance.
       item = new Vector(100);
       currentColor = Color.black;
       this.viewDistance = viewDistance;
       if (width >= height) {
          ymax = size/2;
          xmax = ((float)width/(float)height) * ymax;
       }
       else {
          xmax = size/2;
          ymax = ((float)height/(float)width) * xmax;
       }
       xscale = width/(2*xmax);
       yscale = height/(2*ymax);
    }
    
    void play(Graphics g) {  // play scene into this graphics context
       int ct = item.size();
       g.setColor(Color.black);
       for (int i = 0; i < ct; i++) {
          Object it = item.elementAt(i);
          if (it instanceof Line) {
             Line line = (Line)it;
             g.drawLine( line.xL, line.yL, line.xR, line.yR );
          }
          else
             g.setColor((Color)it);
       }
    }
    
    void addColor(Color c) {  // add color to list of items in scene
       item.addElement(c);
       currentColor = c;
    }
    
    Color getCurrentColor() {
       return currentColor;
    }
    
    int getItemCount() {
       return item.size();
    }
    
    void addLine(Transform T,
                 double x1, double y1, double z1, 
                 double x2, double y2, double z2) {
              // Apply T to (x1,y1,z1) to (x2,y2,z2), then apply
              // projection and window transformation, then add line to scene
         if (T != null) {
            double temp1 = T.newx(x1,y1,z1);
            double temp2 = T.newy(x1,y1,z1);
            z1 = T.newz(x1,y1,z1);
            x1 = temp1;
            y1 = temp2;
            temp1 = T.newx(x2,y2,z2);
            temp2 = T.newy(x2,y2,z2);
            z2 = T.newz(x2,y2,z2);
            x2 = temp1;
            y2 = temp2;
         }
         if (!Double.isInfinite(viewDistance)) {
            double limit = viewDistance * 0.999;
            if (z1 >= limit && z2 >= limit)
               return;
            if (z1 >= limit) {
               x1 = x1 + (x2-x1)/(z2-z1)*(limit-z1);
               y1 = y1 + (y2-y1)/(z2-z1)*(limit-z1);
               z1 = limit;
            }
            else if (z2 >= limit) {
               x2 = x2 + (x1-x2)/(z1-z2)*(limit-z2);
               y2 = y2 + (y1-y2)/(z1-z2)*(limit-z2);
               z2 = limit;
            }
/*
            if (z1 >= limit) {
               x2 = x2 + (x1-x2)/(z1-z2)*(limit-z2);
               y2 = y2 + (y1-y2)/(z1-z2)*(limit-z2);
               z1 = limit;
            }
            else if (z2 >= limit) {
               x1 = x1 + (x2-x1)/(z2-z1)*(limit-z1);
               y1 = y1 + (y2-y1)/(z2-z1)*(limit-z1);
               z2 = limit;
            } 
*/
            x1 = viewDistance*x1 / (viewDistance - z1);
            y1 = viewDistance*y1 / (viewDistance - z1);
            x2 = viewDistance*x2 / (viewDistance - z2);
            y2 = viewDistance*y2 / (viewDistance - z2);
         }
         x1 = xscale*(x1+xmax);
         y1 = yscale*(ymax-y1);
         x2 = xscale*(x2+xmax);
         y2 = yscale*(ymax-y2);
         double size1 = Math.sqrt(x1*x1+y1*y1);
         if (size1 > 20000) {
            x1 = x1/size1 * 20000;
            y1 = y1/size1 * 20000;
         }
         double size2 = Math.sqrt(x2*x2+y2*y2);
         if (size2 > 20000) {
            x2 = x2/size2 * 20000;
            y2 = y2/size2 * 20000;
         }
         item.addElement(new Line( (short)x1, (short)y1, (short)x2, (short)(y2) ));
    }
    
}  // end class PreparedScene



