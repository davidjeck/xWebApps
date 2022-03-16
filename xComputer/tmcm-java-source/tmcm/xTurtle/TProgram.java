package tmcm.xTurtle;

class TProgram {

   int[] ins, data, pos;
   int insCt = 0;
   
   String[] stringStore;
   int stringCt = 0;
   
   double[] constantStore;
   int constantCt = 0;
   
   int globalOffsetCt = 0;
   int appendStart = 0;
   int GrabCount = 0;
   
   TProgram() {
      this(1000);
   }
   
   TProgram(int initialSize) {
      ins = new int[initialSize];
      data = new int[initialSize];
      pos = new int[initialSize];
      constantStore = new double[initialSize / 10];
      stringStore = new String[ (initialSize < 1000)? 10 : initialSize / 100 ];
   }
   
   void addInstruction(int insCode, int dataVal, int position) {
      if (insCt >= ins.length) {
         int[] temp = new int[insCt + 500];
         System.arraycopy(ins,0,temp,0,insCt);
         ins = temp;
         temp = new int[insCt + 500];
         System.arraycopy(data,0,temp,0,insCt);
         data = temp;
         temp = new int[insCt + 500];
         System.arraycopy(pos,0,temp,0,insCt);
         pos = temp;
      }
      ins[insCt] = insCode;
      data[insCt] = dataVal;
      pos[insCt] = position;
      insCt++;
   }
   
   int addString(String str) {
      if (stringCt >= stringStore.length) {
         String[] temp = new String[stringCt + 25];
         System.arraycopy(stringStore,0,temp,0,stringCt);
         stringStore = temp;
      }
      stringStore[stringCt] = str;
      stringCt++;
      return stringCt - 1;
   }
   
   int addConstant(double val) {
      for (int i = 0; i < constantCt; i++)
         if (val == constantStore[i])
            return i;
      if (constantCt >= constantStore.length) {
         double[] temp = new double[constantCt + 25];
         System.arraycopy(constantStore,0,temp,0,constantCt);
         constantStore = temp;
      }
      constantStore[constantCt] = val;
      constantCt++;
      return constantCt - 1;
   }

}