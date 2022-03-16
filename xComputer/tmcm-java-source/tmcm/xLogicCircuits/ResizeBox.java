
package tmcm.xLogicCircuits;

import java.awt.*;

class ResizeBox {

   int x,y,width,height;
   int x4,y4;
   
   static final int minWidth = 25, minHeight = 18;
   
   static final int UL = 0, UR = 1, LL = 2, LR = 3;  // vertices
   int vertexHit;
   
   int last_x, last_y;
   boolean dragging;
   Rectangle legalRect;
   
   int offset_x, offset_y;
   int center_x, center_y;
   int maxSymWidth, maxSymHeight;
   
   boolean hidden = true;
   boolean symmetric = true;
   
   void show(int x, int y, int width, int height) {
      reshape(x,y,width,height);
      hidden = false;
   }
   
   void show(float x, float y, float width, float height) {
      reshape(x,y,width,height);
      hidden = false;
   }
   
   void hide() {
      hidden = true;
   }

   void reshape(int x, int y, int width, int height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      x4 = x + width - 4;
      y4 = y + height - 4;
   }
   
   void reshape(float x, float y, float width, float height) {
      reshape(Math.round(x),Math.round(y),Math.round(width),Math.round(height));
   }
   
   void draw(Graphics g) {
      if (hidden)
         return;
      g.setColor(Color.gray);
      g.drawRect(x,y,width,height);
      g.setColor(Color.black);
      g.fillRect(x,y,4,4);
      g.fillRect(x,y4,4,4);
      g.fillRect(x4,y,4,4);
      g.fillRect(x4,y4,4,4);
   }
   
   boolean beginDrag(int a, int b, Rectangle legalRect) {
      dragging = false;
      if (hidden)
         return false;
      if (a >= x && a < x + 4) {
         if (b >= y && b < y + 4) {
            vertexHit = UL;
            offset_x = x-a;
            offset_y = y-b;
         }
         else if (b >= y4 && b < y4 + 4) {
            vertexHit = LL;
            offset_x = x-a;
            offset_y = y+height-b;
         }
         else
            return false;
      }
      else if (a >= x4 && a < x4 + 4) {
         if (b >= y && b < y + 4) {
            vertexHit = UR;
            offset_x = x+width-a;
            offset_y = y-b;
         }
         else if (b >= y4 && b < y4 + 4) {
            vertexHit = LR;
            offset_x = x+width-a;
            offset_y = y+height-b;
         }
         else
            return false;
      }
      else
         return false;
      dragging = true;
      last_x = a;
      last_y = b;
      this.legalRect = legalRect;
      symmetric = false;
      return true;
   }
   
   boolean beginSymmetricDrag(int a, int b, int maxWidth, int maxHeight) {
      dragging = false;
      if (hidden)
         return false;
      maxSymWidth = (maxWidth+1)/2;
      maxSymHeight = (maxHeight+1)/2;
      center_x = (x + width/2);
      center_y = (y + height/2);
      boolean temp = beginDrag(a,b,null);
      symmetric = true;
      return temp;
   }
   
   void continueSymDrag(int a, int b) {
      a += offset_x;
      b += offset_y;
      switch (vertexHit) {
         case UL:
            a = Math.min(Math.max(a, center_x - maxSymWidth), center_x - minWidth/2);
            b = Math.min(Math.max(b, center_y - maxSymHeight), center_y - minHeight/2);
            reshape(a,b,2*Math.abs(a-center_x),2*Math.abs(b-center_y));
            break;
         case LL:
            a = Math.min(Math.max(a, center_x - maxSymWidth), center_x - minWidth/2);
            b = Math.min(Math.max(b, center_y  + minHeight/2), center_y + maxSymHeight);
            reshape(a,center_y-Math.abs(b-center_y),2*Math.abs(a-center_x),2*Math.abs(b-center_y));
            break;
         case UR:
            a = Math.min(Math.max(a, center_x + minWidth/2), center_x + maxSymWidth);
            b = Math.min(Math.max(b, center_y - maxSymHeight), center_y - minWidth/2);
            reshape(center_x-Math.abs(a-center_x),b,2*Math.abs(a-center_x),2*Math.abs(b-center_y));
            break;
         case LR:
            a = Math.min(Math.max(a, center_x + minWidth/2), center_x + maxSymWidth);
            b = Math.min(Math.max(b, center_y + minHeight/2), center_y + maxSymHeight);
            reshape(center_x-Math.abs(a-center_x),center_y-Math.abs(b-center_y),2*Math.abs(a-center_x),2*Math.abs(b-center_y));
            break;
      }
   }
   
   void continueDrag(int a, int b) {
      if (!dragging)
         return;
      if (symmetric) {
         continueSymDrag(a,b);
         return;
      }
      a += offset_x;
      b += offset_y;
      if (legalRect != null) {
         if (a < legalRect.x)
            a = legalRect.x;
         else if (a > legalRect.x+legalRect.width)
            a = legalRect.x+legalRect.width;
         if (b < legalRect.y)
            b = legalRect.y;
         else if (b > legalRect.y+legalRect.height)
            b = legalRect.y+legalRect.height;
      }
      switch (vertexHit) {
         case UL:
           if (a >= x+width-minWidth)
              a = x+width-minWidth;
           if (b >= y+height-minHeight)
              b = y+height-minHeight;
           reshape(a,b,width+x-a,height+y-b);
           break;
         case LL:
           if (a >= x+width-minWidth)
              a = x+width-minWidth;
           if (b < y+minHeight)
              b = y+minHeight;
           reshape(a,y,width+x-a,b-y);
           break;
         case UR:
           if (a < x+minWidth)
              a = x+minWidth;
           if (b >= y+height-minHeight)
              b = y+height-minHeight;
           reshape(x,b,a-x,height+y-b);
           break;
         case LR:
           if (a < x+minWidth)
              a = x+minWidth;
           if (b < y+minHeight)
              b = y+minHeight;
           reshape(x,y,a-x,b-y);
           break;
      }
      last_x = x;
      last_y = y;
   }
   
   
   void endDrag(int x, int y) {
      if (x != last_x || y != last_y)
         continueDrag(x,y);
      dragging = false;
   }

}