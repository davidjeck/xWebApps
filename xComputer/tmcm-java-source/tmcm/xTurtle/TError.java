
package tmcm.xTurtle;

public class TError extends RuntimeException {
   int pos;
   TError(String message, int pos) {
      super(message);
      this.pos = pos;
   }
}