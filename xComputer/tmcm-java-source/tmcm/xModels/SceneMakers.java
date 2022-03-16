
package tmcm.xModels;

import java.awt.Color;
import java.util.Vector;


abstract class SceneMaker {
   abstract void prepare(PreparedScene scene, int frameNum, Transform T);
}


class ParamVal {

   int[] frameCounts; // cumulative frame counts  (ref to master copy)
   double[] values;      // value of parameter at given frame counts
   
   ParamVal(int[] frameCounts, double[] values) {
         // Assume that values.length <= frameCounts.length!
         // Assume that first and last entries in values[] are actual
         // numbers; other entries can be NaN!!!
      this.frameCounts = frameCounts;
      this.values = values;
   }
   
   ParamVal(double value) {
      frameCounts = null;
      values = new double[1];
      values[0] = value;
   }

   double getVal(int frameNumber) {  // Asume frameNumber >= 0
      int valCt = values.length;
      if (valCt == 1)
         return values[0];
      else {
         int right = 0;
         while (right < valCt && frameCounts[right] < frameNumber)
            right++;
         if (right >= valCt)
            return values[valCt-1];
         if (frameCounts[right] == frameNumber && !Double.isNaN(values[right]))
            return values[right];
         int left = right - 1;
         while (Double.isNaN(values[right]))
            right++;
         while (Double.isNaN(values[left]))
            left--;
         return values[left] + 
                 ( (values[right]-values[left]) * 
                     (float)(frameNumber-frameCounts[left])/(float)(frameCounts[right]-frameCounts[left]) );
      }
   }
   
}  // end class ParamVal


class RGBParam extends SceneMaker {

   protected Color c;
   protected ParamVal r, g, b;
   
   RGBParam(Color c) {
      this.c = c;
   }
   
   RGBParam(ParamVal r, ParamVal g, ParamVal b) {
      this.r = r;
      this.g = g;
      this.b = b;
   }
   
   Color getVal(int frameNumber) {
      if (c == null) {
         double red = r.getVal(frameNumber);
         double green = g.getVal(frameNumber);
         double blue = b.getVal(frameNumber);
         return new Color((float)red, (float)green, (float)blue);
      }
      else
         return c;
   }

   void prepare(PreparedScene scene, int frameNum, Transform T) {
      scene.addColor(getVal(frameNum));
   }

}  // End Class RGBParam


class HSBParam extends RGBParam {

   
   HSBParam(ParamVal h, ParamVal s, ParamVal b) {
      super(null);
      if (h.values.length == 1 && s.values.length == 1 && b.values.length == 1)
         this.c = Color.getHSBColor((float)h.values[0], (float)s.values[0], (float)b.values[0]);
      else {
         this.r = h;
         this.g = s;
         this.b = b;
      }
   }
   
   Color getVal(int frameNumber) {
      if (c == null) {
         double hue = Math.min(r.getVal(frameNumber),0.99);
         double saturation = g.getVal(frameNumber);
         double brightness = b.getVal(frameNumber);
         return Color.getHSBColor((float)hue, (float)saturation, (float)brightness);
      }
      else
         return c;
   }

   void prepare(PreparedScene scene, int frameNum, Transform T) {
      scene.addColor(getVal(frameNum));
   }

}  // End Class HSBParam


class LineGroup extends SceneMaker {

   double[] x,y,z;
   int[] v1, v2;
   
   LineGroup(int points, int lines) {
      x = new double[points]; 
      y = new double[points]; 
      z = new double[points]; 
      v1 = new int[lines];
      v2 = new int[lines];
   }
      
   void prepare(PreparedScene scene, int frameNum, Transform T) {
      for (int i = 0; i < v1.length; i++)         
         scene.addLine(T,x[v1[i]],y[v1[i]],z[v1[i]],x[v2[i]],y[v2[i]],z[v2[i]]);
   }
   
}  // end class LineGroup


class ParamLineGroup extends SceneMaker {

   ParamVal[] x,y,z;
   int[] v1, v2;
   
   ParamLineGroup(int points, int lines) {
      x = new ParamVal[points]; 
      y = new ParamVal[points]; 
      z = new ParamVal[points]; 
      v1 = new int[lines];
      v2 = new int[lines];
   }
   
   void prepare(PreparedScene scene, int frameNum, Transform T) {
      for (int i = 0; i < v1.length; i++)
         scene.addLine(T,x[v1[i]].getVal(frameNum),y[v1[i]].getVal(frameNum),z[v1[i]].getVal(frameNum),
                         x[v2[i]].getVal(frameNum),y[v2[i]].getVal(frameNum),z[v2[i]].getVal(frameNum));
   }
   
}  // end class ParamLineGroup


class ExtrudeObject extends SceneMaker {

   ParamVal[] x,y;
   int repCount;
   
   ExtrudeObject(int reps, Vector params) {
      int points = params.size() / 2;
      repCount = reps;
      x = new ParamVal[points];
      y = new ParamVal[points];
      for (int i = 0; i < points; i++) {
         x[i] = (ParamVal)params.elementAt(2*i);
         y[i] = (ParamVal)params.elementAt(2*i+1);
      }
   }

   void prepare(PreparedScene scene, int frameNum, Transform T) {
      double[] a = new double[x.length];
      double[] b = new double[x.length];
      double zmin = - (repCount - 1.0) / 2.0;
      double zmax = (repCount - 1.0) / 2.0;
      for (int i = 0; i < x.length; i++) {
         a[i] = x[i].getVal(frameNum);
         b[i] = y[i].getVal(frameNum);
         scene.addLine(T,a[i],b[i],zmin,a[i],b[i],zmax);
      }
      double z = zmin;
      for (int rep = 0; rep < repCount; rep++) {
         for (int i = 0; i < x.length-1; i++)
            scene.addLine(T,a[i],b[i],z,a[i+1],b[i+1],z);
         z += 1;
      }
   }
   
}  // end class ExtrudeObject


class LatheObject extends SceneMaker {

   ParamVal[] x,y;
   int repCount;
   double[] sin, cos;
   
   LatheObject(int reps, Vector params) {
      int points = params.size() / 2;
      repCount = reps;
      x = new ParamVal[points];
      y = new ParamVal[points];
      for (int i = 0; i < points; i++) {
         x[i] = (ParamVal)params.elementAt(2*i);
         y[i] = (ParamVal)params.elementAt(2*i+1);
      }
      sin = new double[repCount];
      cos = new double[repCount];
      for (int i = 0; i < repCount; i++) {
         sin[i] = Math.sin(2*i*Math.PI/repCount);
         cos[i] = Math.cos(2*i*Math.PI/repCount);
      }
   }

   void prepare(PreparedScene scene, int frameNum, Transform T) {
      double[] a = new double[x.length];
      double[] b = new double[x.length];
      for (int i = 0; i < x.length; i++) {
         a[i] = x[i].getVal(frameNum);
         b[i] = y[i].getVal(frameNum);
      }
      for (int rep = 0; rep < repCount; rep++)
         for (int i = 0; i < x.length-1; i++)
            scene.addLine(T,cos[rep]*a[i],b[i],sin[rep]*a[i],cos[rep]*a[i+1],b[i+1],sin[rep]*a[i+1]);
      for (int i = 0; i < x.length; i++) {
         for (int rep = 0; rep < repCount-1; rep++)
            scene.addLine(T,cos[rep]*a[i],b[i],sin[rep]*a[i],cos[rep+1]*a[i],b[i],sin[rep+1]*a[i]);
         scene.addLine(T,cos[repCount-1]*a[i],b[i],sin[repCount-1]*a[i],a[i],b[i],0);
      }
   }
   
}  // end class LatheObject


class ComplexObject extends SceneMaker {

  SceneMaker[] parts;
  
  ComplexObject(Vector items) {
     parts = new SceneMaker[items.size()];
     for (int i = 0; i < items.size(); i++)
        parts[i] = (SceneMaker)items.elementAt(i);
  }
  
  void prepare(PreparedScene scene, int frameNum, Transform T) {
     Color saveColor = scene.getCurrentColor();
     for (int i = 0; i < parts.length; i++)
        parts[i].prepare(scene,frameNum,T);
     if (!saveColor.equals(scene.getCurrentColor()))
        scene.addColor(saveColor);
  }

}  // end class ComplexObject


class TransformInfo {

   final static int scale = 0,     // 1, 2, or 3 parameters
                    translate = 1, // 1, 2, or 3 parameters
                    xrotate = 2,   // 1 parameter
                    yrotate = 3,   // 1 parameter
                    zrotate = 4,   // 1 parameter
                    rotateAboutPoint = 5,  // 3 parameters: angle, x, and y
                    rotateAboutLine = 6,   // 4 or 7 parameters
                    xyshear = 7,  // two parameters
                    xskew = 8, // one parameter
                    yskew = 9; // one parameter
                    
   int transformType;
   ParamVal[] data;
   
   TransformInfo(int type, Vector params) {
      transformType = type;
      data = new ParamVal[params.size()];
      for (int i = 0; i < data.length; i++)
         data[i] = (ParamVal)params.elementAt(i);
   }

   void apply(Transform T, int frameNum) {
      double a,b,c,d;
      switch (transformType) {
         case scale:
           a = data[0].getVal(frameNum);
           b = (data.length > 1)? data[1].getVal(frameNum) : a;
           c = (data.length > 2)? data[2].getVal(frameNum) : b;
           T.scale(a,b,c);
           break;
         case translate:
           a = data[0].getVal(frameNum);
           b = (data.length > 1)? data[1].getVal(frameNum) : 0;
           c = (data.length > 2)? data[2].getVal(frameNum) : 0;
           T.translate(a,b,c);
           break;
         case xrotate:
           a = data[0].getVal(frameNum);
           T.rotatex(a);
           break;
         case yrotate:
           a = data[0].getVal(frameNum);
           T.rotatey(a);
           break;
         case zrotate:
           a = data[0].getVal(frameNum);
           T.rotatez(a);
           break;
         case rotateAboutPoint:
           a = data[0].getVal(frameNum);
           b = data[1].getVal(frameNum);
           c = data[2].getVal(frameNum);
           T.translate(b,c,0);
           T.rotatez(a);
           T.translate(-b,-c,0);
           break;
         case rotateAboutLine:
           double e = 0, f = 0, g = 0;
           if (data.length == 7) {
             e = data[4].getVal(frameNum);
             f = data[5].getVal(frameNum);
             g = data[6].getVal(frameNum);
             b = e - data[1].getVal(frameNum);
             c = f - data[2].getVal(frameNum);
             d = g - data[3].getVal(frameNum);
             T.translate(e,f,g);
           }
           else {
              b = data[1].getVal(frameNum);
              c = data[2].getVal(frameNum);
              d = data[3].getVal(frameNum);
           }
           a = data[0].getVal(frameNum);
           T.rotateAboutLine(a,b,c,d);
           if (data.length == 7)
              T.translate(-e,-f,-g);
           break;
         case xyshear:
           a = data[0].getVal(frameNum);
           b = data[1].getVal(frameNum);
           T.xyShear(a,b);
           break;
         case xskew:
           a = data[0].getVal(frameNum);
           T.xSkew(a);
           break;
         case yskew:
           a = data[0].getVal(frameNum);
           T.ySkew(a);
           break;
       }
   }

}  // end of class TransformInfo

class TransformedObject extends SceneMaker {

  TransformInfo[] transforms;
  SceneMaker basicObject;
  
  TransformedObject(SceneMaker obj, Vector transformList) {
     basicObject = obj;
     transforms = new TransformInfo[transformList.size()];
     for (int i = 0; i < transforms.length; i++)
        transforms[i] = (TransformInfo)transformList.elementAt(i);
  }
  
  void prepare(PreparedScene scene, int frameNum, Transform T) {
     Transform compose = new Transform(T);
     for (int i = transforms.length - 1; i >= 0; i--)
        transforms[i].apply(compose,frameNum);
     basicObject.prepare(scene,frameNum,compose);
  }

}  // end class TransformedObject