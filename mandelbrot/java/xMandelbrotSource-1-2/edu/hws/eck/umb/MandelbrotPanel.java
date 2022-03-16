package edu.hws.eck.umb;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;

/**
 * A MandelbrotPanel shows a MandelbrotDisplay and (optionally) its status bar. If 
 * a fixed images size has been set for the display, then the display is contained 
 * in a JScrollPane that can be used to scroll the image; if the image size is set
 * to match the window size, then the MandelbrotDisplay is contained directly 
 * in the panel.  The panel listens for property change events from the display
 * so that it can swap the JScrollPane in and out as necessary.
 */
public class MandelbrotPanel extends JPanel {

	private MandelbrotDisplay display;
	private JScrollPane scroller;
	private StatusBar statusBar;
	
	public MandelbrotPanel() {
		this(true);
	}

	public MandelbrotPanel(boolean showStatusBar) {
		setLayout(new BorderLayout(2,2));
		setBackground(Color.DARK_GRAY);
		setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
		display = new MandelbrotDisplay();
		if (display.getImageSize() == null)
			add(display,BorderLayout.CENTER);
		else {
			scroller = new JScrollPane(display);
			add(scroller,BorderLayout.CENTER);
		}
		if (showStatusBar) {
			statusBar =  new StatusBar(display);
			add( statusBar, BorderLayout.SOUTH);
		}
		display.addPropertyChangeListener(MandelbrotDisplay.PROPERTY_REQUESTED_IMAGE_SIZE, new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName() == MandelbrotDisplay.PROPERTY_REQUESTED_IMAGE_SIZE) {
					Dimension oldSize = (Dimension)evt.getOldValue();
					Dimension newSize = (Dimension)evt.getNewValue();
					if ((newSize == null) == (oldSize == null))
						return;
					if (newSize == null) { // so oldSize != null and scroller is in window
						remove(scroller);
						scroller = null;
						add(display,BorderLayout.CENTER);
						validate();
					}
					else { // size != null, so oldSize == null and display is in window
						remove(display);
						scroller = new JScrollPane(display);
						add(scroller,BorderLayout.CENTER);
						validate();
					}
				}
			}
		});
	}
	
	public StatusBar getStatusBar() {
		return statusBar;
	}
	
	public MandelbrotDisplay getDisplay() {
		return display;
	}
		
}
