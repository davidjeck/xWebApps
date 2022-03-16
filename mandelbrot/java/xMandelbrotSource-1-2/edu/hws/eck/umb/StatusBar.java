package edu.hws.eck.umb;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;

import javax.swing.*;

import edu.hws.eck.umb.util.I18n;

/**
 * A label meant to be used as a status bar for a Mandelbrot display.  The label
 * listens for property change events from the display so that it can show appropriate
 * information.  It is also used by MandelbrotMenus to display some information when
 * the user selects certain commands.
 */
class StatusBar extends JLabel {
	
	private String mainText;
	private Timer timer;
	
	public StatusBar(final MandelbrotDisplay display) {
		setOpaque(true);
		setBackground(new Color(230,230,230));
		setForeground(new Color(150,0,0));
		setBorder(BorderFactory.createEmptyBorder(3,5,2,1));
		setText(I18n.tr("statusBar.text.idle"));
		display.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				String prop = evt.getPropertyName();
				if (prop.equals(MandelbrotDisplay.PROPERTY_STATUS)) {
					MandelbrotDisplay.StatusInfo info = (MandelbrotDisplay.StatusInfo)evt.getNewValue();
					if (info.status == MandelbrotDisplay.STATUS_IDLE || info.status == MandelbrotDisplay.STATUS_DONE_FIRST_PASS)
						setText(I18n.tr("statusBar.text.Idle"));
					else {
						String text = "";
						if (display.getSubpixelSamplingEnabled()) {
							if (info.status == MandelbrotDisplay.STATUS_RUNNING_FIRST_PASS)
								text = I18n.tr("statusBar.text.FirstPass") + "  ";
							else if (info.status == MandelbrotDisplay.STATUS_RUNNING_SECOND_PASS)
								text = I18n.tr("statusBar.text.SecondPass") + "  ";
						}
						BigDecimal[] limits = display.getLimits();
						int rows = display.getActualImageSize().height;
						BigDecimal dy = limits[3].subtract(limits[2]).divide(new BigDecimal(rows-1), limits[3].scale(), BigDecimal.ROUND_HALF_EVEN);
						boolean useHighPrecision = display.getHighPrecisionEnabled() && Math.abs(dy.doubleValue()) < MandelbrotDisplay.HP_CUTOFF;
						if (useHighPrecision)
							text += I18n.tr("statusBar.text.HighPrecision", ""+limits[0].scale());
						else
							text += I18n.tr("statusBar.text.NormalPrecision");
						int percent = (int)(info.fractionComplete * 100);
						text += " " + I18n.tr("statusBar.text.PercentComplete", ""+percent);
						setText(text);
					}
				}
				else if (prop.equals(MandelbrotDisplay.PROPERTY_ACTUAL_IMAGE_SIZE)) {
					Dimension size = (Dimension)evt.getNewValue();
					if (size != null)
						setTempText(I18n.tr("statusBar.text.NewImageSize", ""+size.width, ""+size.height), 3);
				}
				else if (prop.equals(MandelbrotDisplay.PROPERTY_ORBIT_POINT)) {
					String[] pt = display.getOrbitStartPointAsStrings();
					if (pt != null)
						setTempText(I18n.tr("statusBar.text.OrbitPointCoords", pt[0], pt[1]), 6);
					else
						clearTempText();
				}
			}
		});
	}
	
	/**
	 * Set the main text of the label.  (If temporary text is being shown,
	 * the main text doesn't appear until the temporary text times out.)
	 */
	public void setText(String text) {
		mainText = text;
		if (timer == null)
			super.setText(text);
	}
	
	/**
	 * Clear the temporary text, if any, immediately, and show the main text again.
	 */
	public void clearTempText() {
		if (timer != null)
			timer.stop();
		super.setText(mainText);
	}
	
	/**
	 * Set temporary text that will show in the label for a specified number
	 * of seconds, replacing its usual text during that time.
	 */
	public void setTempText(String text, int secondsToShow) {
		super.setText(text);
		if (timer != null)
			timer.stop();
		timer = new Timer(1,new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				StatusBar.super.setText(mainText);
				timer = null;
			}
		});
		timer.setInitialDelay(1000*secondsToShow);
		timer.setRepeats(false);
		timer.start();
	}

}
