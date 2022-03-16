package edu.hws.eck.umb;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import edu.hws.eck.umb.util.I18n;

/**
 * A frame containing a MandelbrotPanel and its menu bar.
 */
public class MandelbrotFrame extends JFrame {
	
	/**
	 * Construct a frame for use in a standalone program.
	 */
	public MandelbrotFrame() {
		this(false);
	}

	/**
	 * Construct a MandelbrotFrame for use in a standalone program or
	 * applet.  The size and location of the frame are set, but the frame
	 * is not made visible by this constructor.
	 * @param isForApplet tells if the frame is for use in an applet.  When
	 * used with an applet, there is no File menu in the menu bar, and the
	 * menu command for configuring multiprocessing/networking is left out
	 * of the Control menu.
	 */
	public MandelbrotFrame(boolean isForApplet) {
		super(I18n.tr("mandelbrotFrame.title"));
		MandelbrotPanel panel = new MandelbrotPanel();
		final MandelbrotDisplay display = panel.getDisplay();
		setContentPane(panel);
		setJMenuBar(new MandelbrotMenus(display,this,panel.getStatusBar(),isForApplet));
		pack();
		setLocation(20,60);
		addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent evt) {
				display.closing();
			}
		});
	}

}
