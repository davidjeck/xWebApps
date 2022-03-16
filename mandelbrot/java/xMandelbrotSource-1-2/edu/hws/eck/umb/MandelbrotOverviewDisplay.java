package edu.hws.eck.umb;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;

import edu.hws.eck.umb.palette.Palette;
import edu.hws.eck.umb.util.I18n;

/**
 * Defines a subclass of MandelbrotDisplay to be used in an "Overview window".
 * The only public method in this class creates and opens the Overview window
 * as a modeless dialog.  The window contains a MandelbrotDisplay that shows
 * the full Mandlebrot set with a grayscale palette.  The x and y limits for
 * the main display are shown beneath the image.  Furthermore, the x/y limits
 * are marked on the display with a red box or cross.  Finally, if an orbit is
 * drawn on the main display, an orbit with the same starting point is drawn
 * on the overview display.  The overview display listens for property change
 * events in the main display, so that it can adjust the limits and the orbit
 * when the ones in the main display change.
 */
public class MandelbrotOverviewDisplay extends MandelbrotDisplay {
	
	private MandelbrotDisplay owner;
	private JLabel limitsDisplay;
	
	public static JDialog createDialog(MandelbrotDisplay parent) {
		Component p = parent.getParent();
		while (p != null && !(p instanceof Frame))
			p = p.getParent();
		JDialog dialog = new JDialog((Frame)p, I18n.tr("mandelbrotOverviewDisplay.DialogTitle"));
		final MandelbrotOverviewDisplay display = new MandelbrotOverviewDisplay(parent);
		JPanel content = new JPanel();
		content.setLayout(new BorderLayout(2,2));
		content.setBackground(Color.DARK_GRAY);
		content.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
		content.add(display,BorderLayout.CENTER);
		content.add(display.limitsDisplay,BorderLayout.SOUTH);
		dialog.setContentPane(content);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.pack();
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent evt) {
				display.closing();
			}
		});
		if (p != null) {
			Rectangle parentRect = p.getBounds();
			Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
			int x = parentRect.x + parentRect.width + 8;
			if (x + dialog.getWidth() > screensize.width)
				x = screensize.width - dialog.getWidth() - 5;
			int y = parentRect.y + 8;
			if (y + dialog.getHeight() > screensize.height)
				y = screensize.height - dialog.getHeight() - 5;
			dialog.setLocation(x, y);
		}
		return dialog;
	}
	
	private MandelbrotOverviewDisplay(MandelbrotDisplay parent) {
		super(false,false);
		owner = parent;
		limitsDisplay = new JLabel();
		limitsDisplay.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		limitsDisplay.setFont(new Font("Monospaced",Font.PLAIN,11));
		limitsDisplay.setBackground(Color.WHITE);
		limitsDisplay.setOpaque(true);
		checkLimits();
		setPreferredSize(new Dimension(250,250));
		setPalette(new Palette(Palette.COLOR_TYPE_RGB));
		parent.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				String prop = evt.getPropertyName();
				if (prop.equals(MandelbrotDisplay.PROPERTY_LIMITS)) {
					repaint();
					checkLimits();  // reset the numerical limits that are shown at the bottom of the window
				}
				else if (prop.equals(MandelbrotDisplay.PROPERTY_NUMBER_OF_POINTS_ON_ORBIT))
					setPointsOnOrbit((Integer)evt.getNewValue());
				else if (prop.equals(MandelbrotDisplay.PROPERTY_ORBIT_POINT)) {
					BigDecimal[] pt = (BigDecimal[])evt.getNewValue();
					if (pt == null)
						setOrbitStart(null,null);
					else
						setOrbitStart(pt[0], pt[1]);
				}
			}
		});
	}
	
	private void checkLimits() {
		String[] limits = owner.getLimitsAsStrings();
		limitsDisplay.setText("<html>" + I18n.tr("term.MinimumX") + " = " + limits[0] + "<br>" +
				I18n.tr("term.MaximumX") + " = " + limits[1] + "<br>" +
				I18n.tr("term.MinimumY") + " = " + limits[2] + "<br>" +
				I18n.tr("term.MaximumY") + " = " + limits[3] + "</html>");
	}
	
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);  // draws the Mandelbrot image (and orbit if there is one)
		BigDecimal[] limits = getLimits();  // draw  a box or cross to mark the x/y limits
		BigDecimal[] ownerLimits = owner.getLimits();
		double xmin = limits[0].doubleValue();
		double xmax = limits[1].doubleValue();
		double ymin = limits[2].doubleValue();
		double ymax = limits[3].doubleValue();
		double dx = (xmax - xmin) / getWidth();
		double dy = (ymax - ymin) / getHeight();
		double owner_xmin = ownerLimits[0].doubleValue();
		double owner_xmax = ownerLimits[1].doubleValue();
		double owner_ymin = ownerLimits[2].doubleValue();
		double owner_ymax = ownerLimits[3].doubleValue();
		int x1 = (int)((owner_xmin - xmin) / dx + 0.499);
		int x2 = (int)((owner_xmax - xmin) / dx + 0.499);
		int y1 = (int)((ymax - owner_ymax) / dy + 0.499);
		int y2 = (int)((ymax - owner_ymin) / dy + 0.499);
		int w = x2 - x1;
		int h = y2 - y1;
		int mx = (x1 + x2) / 2;
		int my = (y1 + y2) / 2;
		if (w < 15) {
			g.setColor(Color.WHITE);
			g.fillRect(mx-1,y1-15,3,15);
			g.fillRect(mx-1,y2,3,15);
			g.fillRect(x1-15,my-1,15,3);
			g.fillRect(x2,my-1,15,3);
		}
		g.setColor(Color.WHITE);
		g.drawRect(x1-1,y1-1,w+1,h+1);
		g.drawRect(x1+1,y1+1,w-3,h-3);
		g.setColor(Color.RED);
		if (w < 15) {
			g.drawLine(mx,y1-14,mx,y1);
			g.drawLine(mx,y2,mx,y2+13);
			g.drawLine(x1-14,my,x1,my);
			g.drawLine(x2,my,x2+13,my);
		}
		g.drawRect(x1,y1,w-1,h-1);
	}

}
