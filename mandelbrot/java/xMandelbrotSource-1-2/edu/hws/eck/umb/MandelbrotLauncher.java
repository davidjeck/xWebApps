package edu.hws.eck.umb;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import edu.hws.eck.umb.util.I18n;

/**
 * An applet that contains a single button, which can be used to open
 * a MandelbrotFrame.
 */
public class MandelbrotLauncher extends JApplet {

	private JButton launchButton;
	private MandelbrotFrame window;
	
	public void init() {
		launchButton = new JButton(I18n.tr("mandelbrotLauncher.buttonName.LaunchMandelbrotViewer"));
		launchButton.addActionListener(new ButtonListener());
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(launchButton, BorderLayout.CENTER);
	}
	
	private class ButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent evt) {
			launchButton.setEnabled(false);
			if (window == null) {
				window = new MandelbrotFrame(true);
				window.addWindowListener( new WindowAdapter() {
					public void windowOpened(WindowEvent evt) {
						launchButton.setText(I18n.tr("mandelbrotLauncher.buttonName.CloseMandelbrotViewer"));
						launchButton.setEnabled(true);
					}
					public void windowClosed(WindowEvent evt) {
						launchButton.setText(I18n.tr("mandelbrotLauncher.buttonName.LaunchMandelbrotViewer"));
						launchButton.setEnabled(true);
						window = null;
					}
				} );
				window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				window.setVisible(true);
			}
			else {
				window.dispose();
			}
		}
	}

}
