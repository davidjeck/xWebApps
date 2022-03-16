
package tmcm.xTurtle;

interface TurtleNotification {
   public void errorReport(String errorMessage, int position);
   public void startRunning(TProcess runner);
   public void doneRunning();
}

