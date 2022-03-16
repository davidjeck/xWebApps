
package tmcm.xModels;

final class Transform {

     // represents an affine transform in three dimensions of the form:
     //
     //         newx = a1*x + a2*y +a3*z +tx;
     //         newy = b1*x + b2*y +b3*z +ty;
     //         newz = c1*x + c2*y +c3*z +tz;
     //

   private double[][] tm;  // The transformation matrix (without the 4-th
                               // row, which is always (0,0,0,1)).
   
   Transform() {
          // default constructor creates the identity transformation
       tm = new double[3][4];
       for (int i=0; i<3; i++)
          for (int j=0; j<4; j++)
             tm[i][j] = 0;
       tm[0][0] = 1;
       tm[1][1] = 1;
       tm[2][2] = 1;
   }
   
   
   Transform(Transform T) {
         // construct a copy of T
         // (if T is null, treat it like the identity)
      tm = new double[3][4];
      if (T == null) {
         for (int i=0; i<3; i++)
            for (int j=0; j<4; j++)
               tm[i][j] = 0;
         tm[0][0] = 1;
         tm[1][1] = 1;
         tm[2][2] = 1;
      }
      else {
       for (int i=0; i<3; i++)
          for (int j=0; j<4; j++)
             tm[i][j] = T.tm[i][j];
      }
   }
   
   Transform( double a1, double a2, double a3, double tx,
                     double b1, double b2, double b3, double ty,
                     double c1, double c2, double c3, double tz) {
         // create a transform from a list of all the matrix entries!
       tm = new double[3][4];
       tm[0][0] = a1;
       tm[0][1] = a2;
       tm[0][2] = a3;
       tm[0][3] = tx;
       tm[1][0] = b1;
       tm[1][1] = b2;
       tm[1][2] = b3;
       tm[1][3] = ty;
       tm[2][0] = c1;
       tm[2][1] = c2;
       tm[2][2] = c3;
       tm[2][3] = tz;
   }
   
         
   double newx(double x, double y, double z) {
         // the x-coord of the point that results when this transformation
         // is applied to the point (x,y,z)
      return tm[0][0]*x + tm[0][1]*y + tm[0][2]*z + tm[0][3];
   }
   
   double newy(double x, double y, double z) {
         // the y-coord of the point that results when this transformation
         // is applied to the point (x,y,z)
      return tm[1][0]*x + tm[1][1]*y + tm[1][2]*z + tm[1][3];
   }
   
   double newz(double x, double y, double z) {
         // the z-coord of the point that results when this transformation
         // is applied to the point (x,y,z)
      return tm[2][0]*x + tm[2][1]*y + tm[2][2]*z + tm[2][3];
   }
   
   void rotatex(double degrees) {
         // multiply this transform on the right by a rotation about the Z axis
      double radians = (degrees * Math.PI) / 180;
      double sin = Math.sin(radians);
      double cos = Math.cos(radians);
      for (int i=0; i<3; i++) {
         double a = tm[i][1] * cos + tm[i][2] * sin;
         tm[i][2] = tm[i][1] * (-sin) + tm[i][2] * cos;
         tm[i][1] = a;
      }
   }
   
   void rotatey(double degrees) {
         // multiply this transform on the right by a rotation about the Z axis
      double radians = (degrees * Math.PI) / 180;
      double sin = Math.sin(radians);
      double cos = Math.cos(radians);
      for (int i=0; i<3; i++) {
         double a = tm[i][2] * cos + tm[i][0] * sin;
         tm[i][0] = tm[i][2] * (-sin) + tm[i][0] * cos;
         tm[i][2] = a;
      }
   }
   
   void rotatez(double degrees) {
         // multiply this transform on the right by a rotation about the Z axis
      double radians = (degrees * Math.PI) / 180;
      double sin = Math.sin(radians);
      double cos = Math.cos(radians);
      for (int i=0; i<3; i++) {
         double a = tm[i][0] * cos + tm[i][1] * sin;
         tm[i][1] = tm[i][0] * (-sin) + tm[i][1] * cos;
         tm[i][0] = a;
      }
   }
   
   void translate(double dx, double dy, double dz) {
         // multiply this transform on the right by a translation
      for (int i=0; i<3; i++)
         tm[i][3] = tm[i][0] * dx + tm[i][1] * dy + tm[i][2] * dz + tm[i][3];
   }
   
   void scale(double sx, double sy, double sz) {
         // multiply this transform on the right by a scaling transformation
      for (int row=0; row<3; row++) {
         tm[row][0] *= sx;
         tm[row][1] *= sy;
         tm[row][2] *= sz;
      }
   }
   
   void xyShear(double xshear, double yshear) {
         // multiply on the right by the shear transformation
         //   x = x + z*xshear, y = y + z*yshear
       tm[0][2] += tm[0][0]*xshear + tm[0][1]*yshear;
       tm[1][2] += tm[1][0]*xshear + tm[1][1]*yshear;
       tm[2][2] += tm[2][0]*xshear + tm[2][1]*yshear;
   }
   
   void xSkew(double skew) {
        // multiply on the right by the shear transformation x = x + skew * y
      tm[0][1] += skew*tm[0][0];
      tm[1][1] += skew*tm[1][0];
      tm[2][1] += skew*tm[2][0];
   }
   
   void ySkew(double skew) {
        // multiply on the right by the shear transformation y = y + skew * x
      tm[0][0] += skew*tm[0][1];
      tm[1][0] += skew*tm[1][1];
      tm[2][0] += skew*tm[2][1];
   }
   
   void rotateAboutLine(double angle, double x, double y, double z) {
         // multiply this transform on the right by a rotation
         // about the line the line through (0,0,0) and (x,y,z)
       double size = Math.sqrt(x*x + y*y + z*z);
       if (size <1.0e-10 || Math.abs(angle) < 0.01)
          return;
       x /= size;  // normalize
       y /= size;
       z /= size;
       
       if (Math.abs(z) < 1e-7) {
          if (Math.abs(y) < 1e-7) {
             if (x > 0)
                rotatex(angle);
             else
                rotatex(-angle);
             return;
          }
          if (Math.abs(x) < 1e-7) {
             if (y > 0)
                rotatey(angle);
             else
                rotatey(-angle);
             return;
          }
          double a = 180.0/Math.PI * Math.acos(x/Math.sqrt(x*x+y*y));
          if (y > 0)
             a = -a;
          rotatez(-a);
          rotatex(angle);
          rotatez(a);
          return;
       }
       
       if (Math.abs(y) < 1e-7) {
          if (Math.abs(x) < 1e-7) {
             if (z > 0)
                rotatez(angle);
             else
                rotatez(-angle);
             return;
          }
          double a = 180.0/Math.PI * Math.acos(z/Math.sqrt(x*x+z*z));
          if (x > 0)
             a = -a;
          rotatey(-a);
          rotatez(angle);
          rotatey(a);
          return;
       }
       
       
       double a1 = Math.acos(z/Math.sqrt(y*y+z*z));
       if (y < 0)
          a1 = -a1;
       double s = Math.sin(a1);
       double c = Math.cos(a1);
       z = s*y+c*z;
       a1 = 180.0/Math.PI * a1;
       double a2 = 180.0/Math.PI * Math.acos(z/Math.sqrt(x*x+z*z));
       if (x > 0)
          a2 = -a2;
       rotatex(-a1);
       rotatey(-a2);
       rotatez(angle);
       rotatey(a2);
       rotatex(a1);
   }
   
   void multiply(Transform T) {
         // multiply this transform on the right by the transformation T
      if (T == null)
         return;
      for (int row=0; row<3; row++) {
            double a = tm[row][0]*T.tm[0][0] + tm[row][1]*T.tm[1][0] + tm[row][2]*T.tm[2][0];
            double b = tm[row][0]*T.tm[0][1] + tm[row][1]*T.tm[1][1] + tm[row][2]*T.tm[2][1];
            double c = tm[row][0]*T.tm[0][2] + tm[row][1]*T.tm[1][2] + tm[row][2]*T.tm[2][2];
            tm[row][3] = tm[row][0]*T.tm[0][3] + tm[row][1]*T.tm[1][3] + 
                                                 tm[row][2]*T.tm[2][3] + tm[row][3];
            tm[row][0] = a;
            tm[row][1] = b;
            tm[row][2] = c;
      }
   }
   
   

}