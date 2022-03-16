package edu.hws.eck.umb;

import javax.swing.JFrame;

/**
 * A main program that simply creates a MandelbrotFrame and
 * makes it visible (and sets the system to exit when the frame
 * is closed).
 */
public class MandelbrotMain {

	public static void main(String[] args) {
		MandelbrotFrame frame = new MandelbrotFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

}
