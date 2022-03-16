
package tmcm.xModels;

import java.util.Vector;
import java.awt.*;

class Model {

   ComplexObject scene;
   int[] frameCounts;
   int frames;
   RGBParam bgColor;
   ParamVal viewDistance;
   
   int MemoryLimit = 600000;   // If the guestimated number of bytes for storing
                               // prepared frames is greater than this, then they are not stored.
   
   private boolean storeFrames;
   private int lastFrame = -1;  // -1 indicates that no frames have ever been drawn
   
   private PreparedScene[] preparedFrames;
   private PreparedScene currentFrame;
   private int width = -1, height = -1;  // values used in preparing frames
   
   void setSize(int wd, int ht) {  // compare to stored width, height, possibly invalidate frames
      if (width == wd && height == ht)
         return;
      width = wd;
      height = ht;
      preparedFrames = null;
      currentFrame = null;
   }
   
   void drawFrame(Graphics g, int frameNum) {
      if (width == -1) // THIS INDICATES AN ERROR AND SHOULDN'T HAPPEN
         return;
      frameNum = Math.max(0,Math.min(frames-1,frameNum));

      if (frameNum != lastFrame || currentFrame == null)
         makeCurrentFrame(frameNum);

      Color bg = bgColor.getVal(frameNum);
      g.setColor(bg);
      g.fillRect(0,0,width,height);
      currentFrame.play(g);

   }
   
   private void makeCurrentFrame(int frameNum) {
      if (preparedFrames != null && preparedFrames[frameNum] != null) {
         currentFrame = preparedFrames[frameNum];
         lastFrame = frameNum;
         return;
      }
      double vd = viewDistance.getVal(frameNum);
      currentFrame = new PreparedScene(width,height,vd);
      scene.prepare(currentFrame,frameNum,new Transform());
      if (lastFrame == -1) // This is the very first frame; decide whether or not to store frames
         storeFrames = ((12*currentFrame.getItemCount() + 70) * frames) < MemoryLimit;
      lastFrame = frameNum;
      if (storeFrames) {
         if (preparedFrames == null)
            preparedFrames = new PreparedScene[frames];
         preparedFrames[frameNum] = currentFrame;
      }
   }

} // end class Model