package edu.hws.eck.umb;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.hws.eck.umb.comp.MandelbrotTask;
import edu.hws.eck.umb.comp.TaskManager;
import edu.hws.eck.umb.palette.Palette;
import edu.hws.eck.umb.palette.PaletteMapping;
import edu.hws.eck.umb.util.I18n;

/**
 * The main class of the Mandelbrot Viewer.  Computes and displays a Mandelbrot image
 * and provides facilities for manipulating all its properties.  Property change listeners
 * can register with the display if they want to react to changing properties.
 */
public class MandelbrotDisplay extends JPanel {

	/*
	 * Constants that name properties in property change events emitted by this display.
	 */
	public final static String PROPERTY_LIMITS = "mb_property_limits";
	public final static String PROPERTY_MAX_ITERATIONS = "mb_property_iterations";
	public final static String PROPERTY_PALETTE = "mb_property_palette";
	public final static String PROPERTY_HIGH_PRECISION = "mb_property_hp_enabled";
	public final static String PROPERTY_SUBPIXEL_SAMPLING = "mb_subpixel_sampling";
	public final static String PROPERTY_MANDLELBROT_COLOR ="mp_propery_mb_color";
	public final static String PROPERTY_REQUESTED_IMAGE_SIZE = "mb_property_size";
	public final static String PROPERTY_ACTUAL_IMAGE_SIZE = "mb_OSC_size";
	public final static String PROPERTY_ORBIT_POINT = "mb_orbit_point";
	public final static String PROPERTY_NUMBER_OF_POINTS_ON_ORBIT = "mb_points_on_orbit";
	public final static String PROPERTY_CURRENT_MOUSE_ACTION = "mb_mouse_action";
	public final static String PROPERTY_STATUS = "mb_stauts";
	
	/*
	 * Constants that specify the available mouse tools.  These can be used in the
	 * {@link #setDefaultMouseAction(int)} method, and they are the values of the
	 * property named MandelbrotDisplay.PROPERTY_CURRENT_MOUSE_ACTION.
	 */
	public final static int MOUSE_ACTION_ZOOM_IN = 0;
	public final static int MOUSE_ACTION_ZOOM_OUT = 1;
	public final static int MOUSE_ACTION_DRAG = 2;
	public final static int MOUSE_ACTION_SHOW_ORBIT = 3;
	public final static int MOUSE_ACTION_SHOW_COORDS = 4;
	public final static int MOUSE_ACTION_RECENTER_ON_POINT = 5;
	
	/*
	 * Constants that define the values of the property named
	 * MandelbrotDisplay.PROPERTY_STATUS.
	 */
	public final static int STATUS_IDLE = 0;
	public final static int STATUS_RUNNING_FIRST_PASS = 1;
	public final static int STATUS_DONE_FIRST_PASS = 2;
	public final static int STATUS_RUNNING_SECOND_PASS = 3;
		
	private static final int HP_CUTOFF_EXP = 15;
	public static final double HP_CUTOFF = 1e-15;
	
	private int maxIterations = 100;
	private final Palette palette;
	private final PaletteMapping paletteMapping;
	private int[] paletteColors;
	private float[][] paletteColorComponents;
	private boolean highPrecisionEnabled = true;
	private boolean subpixelSamplingEnabled = true;
	private int rgbForMandelbrot = 0;
	private float[] mandelbrotColorComponents = new float[] { 0,0,0 };
	private Dimension imageSize;  // null means to use window size for image size
	private int pointsOnOrbit = 1000;

	private int status = STATUS_IDLE;
	
	private BufferedImage OSC;
	private BigDecimal xmin_requested, xmax_requested, ymin_requested, ymax_requested;
	private BigDecimal xmin, xmax, ymin, ymax;
	private int scale = HP_CUTOFF_EXP + 5;
	
	private int[][] iterationCounts;
	private int[][] subPixelCounts;
	
	private double fractionComplete;
	private final TaskManager taskManager;
	private TaskManager.Job currentJob;
	private volatile boolean needsRedraw;
	private boolean newPalette = true;
	private boolean resizing;
	private Timer resizeTimer;
	private boolean wasPainted;
	private Timer periodicRepaintTimer = new Timer(500, new ActionListener() {
		public void actionPerformed(ActionEvent evt) {
			grabAvailableResults();
			repaint();
		}
	});

	private boolean announceChanges;
	
	private Dimension dragImageOffset;
	private Rectangle dragZoomRect;
	private BigDecimal[] orbitStartPoint;
	private String[] coordinateStrings;
	private Point coordinatePoint;
	private ArrayList<Point> orbitPoints;
	private int defaultMouseAction = MOUSE_ACTION_ZOOM_IN;
	
	private static final BigDecimal TWO = new BigDecimal("2");
	private static final BigDecimal TEN = new BigDecimal("10");
	
	/**
	 * A class that is simply a container for the current status plus
	 * the fraction of the current computation that is complete, when the
	 * status is not STATUS_IDLE.  Values associated with the property
	 * named PROPERTY_STATUS are of this type.
	 */
	public static class StatusInfo {
		public final int status;
		public final double fractionComplete;
		public StatusInfo(int status, double fractionComplete) {
			this.status = status;
			this.fractionComplete = fractionComplete;
		}
	}
	
	/**
	 * A simple class that acts as a container for the current Palette
	 * and PaletteMapping.  Values associates with the property named
	 * PROPERTY_PALETTE are of this type.
	 */
	public static class PaletteInfo {
		public final Palette palette;
		public final PaletteMapping paletteMapping;
		public PaletteInfo(Palette p, PaletteMapping pm) {
			palette = p;
			paletteMapping = pm;
		}
	}
	
	/**
	 * Create a MandlelbrotDisplay that emits PropertyChangeEvents and
	 * in which the mouse is enabled.  (This is used for the main display
	 * of the application, but not for the displays in the overview window
	 * or the palette edit dialog.)
	 */
	public MandelbrotDisplay() {
		this(true,true);
	}
	
	/**
	 * Create a Mandelbrot display.  Initially, the display shows the full Mandelbrot set,
	 * using a Spectrum palette with a maximum iteration count of 100.  Both high precision
	 * computation and subpixel sampling are enabled.
	 * @param announceChanges the display emits property change events only if this is true
	 * @param enableMouse the mouse can be used on the display only if this is true
	 */
	public MandelbrotDisplay(boolean announceChanges, boolean enableMouse) {
		this.announceChanges = announceChanges;
		if (enableMouse)
			addMouseListener(new MouseHandler());
		setPreferredSize(new Dimension(800,600));
		setOpaque(true);
		setBackground(Color.LIGHT_GRAY);
		needsRedraw = true;
		taskManager = new TaskManager();
		xmin_requested = new BigDecimal("-2.333");
		xmax_requested = new BigDecimal("1");
		ymin_requested = new BigDecimal("-1.25");
		ymax_requested = new BigDecimal("1.25");
		addComponentListener( new ComponentAdapter() {
			public void componentResized(ComponentEvent evt) {
				if (resizeTimer != null)
					resizeTimer.stop();
				if (!wasPainted || imageSize != null)
					return;
				resizing = true;
				resizeTimer = new Timer(333, new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						resizing = false;
						resizeTimer = null;
						needsRedraw = true;
						repaint();
					}
				});
				resizeTimer.setInitialDelay(333);
				resizeTimer.setRepeats(false);
				resizeTimer.start();
			}
		});
		palette = new Palette();
		paletteMapping = new PaletteMapping();
		ChangeListener cl = new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				newPalette = true;
				repaint();
			}
		};
		palette.addChangeListener(cl);
		paletteMapping.addChangeListener(cl);
	}
	
	/**
	 * Can be called when the display is no longer needed, to shut down the
	 * display's TaskManager cleanly.
	 */
	public void closing() {
		taskManager.shutDown();
	}
	
	/**
	 * Returns null if the image size is set to match the window size.  Otherwise,
	 * returns the set fixed image size.
	 */
	public Dimension getImageSize() {
		return imageSize;
	}
	
	Dimension getActualImageSize() {
		if (OSC == null)
			return new Dimension(-1,-1);
		else
			return new Dimension(OSC.getWidth(), OSC.getHeight());
	}
	
	/**
	 * Set the image size.  A null value indicates that the image size should match the
	 * window size. Emits a property change event with name PROPERTY_REQUESTED_IMAGE_SIZE if the new
	 * value is not equal to the old value (and if the display is set to emit events).
	 */
	public void setImageSize(Dimension size) {
		if (imageSize == null && size == null || imageSize != null && imageSize.equals(size))
			return;
		Dimension oldSize = imageSize;
		imageSize = size;
		if (imageSize != null)
			setPreferredSize(imageSize);
		needsRedraw = true;
		invalidate();
		if (getParent() != null)
			getParent().validate();
		if (announceChanges)
			firePropertyChange(PROPERTY_REQUESTED_IMAGE_SIZE, oldSize, imageSize);
		repaint();
	}
	
	/**
	 * Returns a copy of the current palette; changing the copy will not affect
	 * the dispaly.
	 */
	public Palette getCopyOfPalette() {
		return palette.clone();
	}
	
	/**
	 * Sets the palette to be used in the window.  Palette properties are copied
	 * from the parameter; no reference is kept to the parameter itself.
	 * Emits a property change event with name PROPERTY_PALETTE if the new
	 * values are not equal to the old values (and if the display is set to emit events).
	 */
	public void setPalette(Palette p) {
		if (p.equals(palette))
			return;
		PaletteInfo oldVal = null;
		if (announceChanges)
			oldVal = new PaletteInfo(palette.clone(),paletteMapping.clone());
		palette.copyFrom(p);
		if (announceChanges)
			firePropertyChange(PROPERTY_PALETTE, oldVal, new PaletteInfo(p.clone(),paletteMapping.clone()));
	}
	
	/**
	 * Sets both the palette and the palette mapping for the display.  Value are
	 * copied from the parameters.  No references  are kept to the parameters.
	 * Emits a single property change event with name PROPERTY_PALETTE if the new
	 * values are not equal to the old values (and if the display is set to emit events).
	 */
	public void setPaletteInfo(Palette p, PaletteMapping pm) {
		if (p.equals(palette) && pm.equals(paletteMapping))
			return;
		PaletteInfo oldval = null;
		if (announceChanges)
			oldval = new PaletteInfo(palette.clone(),paletteMapping.clone());
		palette.copyFrom(p);
		paletteMapping.setLength(pm.getLength());
		paletteMapping.setOffset(pm.getOffset());
		if (announceChanges)
			firePropertyChange(PROPERTY_PALETTE, oldval, new PaletteInfo(p.clone(),pm.clone()));
	}
	
	public double getFractionComplete() {
		return fractionComplete;
	}
	
	public int getMaxIterations() {
		return maxIterations;
	}
	
	/**
	 * Set the maximum number of iterations to be computed for each point in the image.
	 * Emits a property change event with name PROPERTY_MAX_ITERATIONS if the new
	 * value is not equal to the old value (and if the display is set to emit events).
	 */
	public void setMaxIterations(int m) {
		if (m <= 0)
			m = 0;
		if (m == maxIterations)
			return;
		int oldval = maxIterations;
		maxIterations = m;
		paletteColors = null;
		paletteColorComponents = null;
		needsRedraw = true;
		repaint();
		if (announceChanges)
			firePropertyChange(PROPERTY_MAX_ITERATIONS, oldval, m);
	}
	
	public Color getMandelbrotColor() {
		return new Color(rgbForMandelbrot);
	}
	
	/**
	 * Set the color that is used to display the Mandelbrot set itself (that is, for points
	 * where the number of iterations is equal to maxIterations).
	 * Emits a property change event with name PROPERTY_MANDELBROT_COLOR if the new
	 * value is not equal to the old value (and if the display is set to emit events).
	 */
	public void setMandelbrotColor(Color c) {
		if ((c.getRGB() & 0xFFFFFF) == rgbForMandelbrot)
			return;
		int oldval = rgbForMandelbrot;
		rgbForMandelbrot = (c.getRGB() & 0xFFFFFF);
		mandelbrotColorComponents = c.getRGBColorComponents(null);
		synchronized(this) {
			if (subPixelCounts == null) {
				if (OSC != null && iterationCounts != null) {
					for (int row = 0; row < iterationCounts.length; row++)
						if (iterationCounts[row] != null) {
							for (int col = 0; col < iterationCounts[row].length; col++)
								if (iterationCounts[row][col] == maxIterations)
									OSC.setRGB(col,row,rgbForMandelbrot);
						}
				}
			}
			else
				applyPalette();
			repaint();
		}
		if (announceChanges)
			firePropertyChange(PROPERTY_MANDLELBROT_COLOR, new Color(oldval), c);
	}
	
	public int getPaletteLength() {
		return paletteMapping.getLength();
	}
	
	/**
	 * Set the length of the palette, that is the number of colors.  If teh specified value is
	 * zero (or less), then the length matches the value of maxIterations.
	 * Emits a property change event with name PROPERTY_PALETTE if the new
	 * value is not equal to the old value (and if the display is set to emit events).
	 */
	public void setPaletteLength(int length) {
		if (length < 0)
			length = 0;
		if (paletteMapping.getLength() == length)
			return;
		PaletteInfo oldval = null;
		if (announceChanges)
			oldval = new PaletteInfo(palette.clone(), paletteMapping.clone());
		paletteMapping.setLength(length);
		if (announceChanges)
			firePropertyChange(PROPERTY_PALETTE, oldval, new PaletteInfo(palette.clone(),paletteMapping.clone()));
	}
	
	public int getPaletteOffset() {
		return paletteMapping.getOffset();
	}
	
	/**
	 * Set the palette offset (telling where in the array of colors the palette starts).
	 * Emits a property change event with name PROPERTY_PALETTE if the new
	 * value is not equal to the old value (and if the display is set to emit events).
	 */
	public void setPaletteOffset(int offset) {
		if (paletteMapping.getOffset() == offset)
			return;
		PaletteInfo oldval = null;
		if (announceChanges)
			oldval = new PaletteInfo(palette.clone(), paletteMapping.clone());
		paletteMapping.setOffset(offset);
		if (announceChanges)
			firePropertyChange(PROPERTY_PALETTE, oldval, new PaletteInfo(palette.clone(),paletteMapping.clone()));
	}
	
	public boolean getHighPrecisionEnabled() {
		return highPrecisionEnabled;
	}
	
	/**
	 * Set whether high precision computation is enabled.
	 * Emits a property change event with name PROPERTY_HIGH_PRECISION if the new
	 * value is not equal to the old value (and if the display is set to emit events).
	 */
	public void setHighPrecisionEnabled(boolean enabled) { 
		if (highPrecisionEnabled == enabled)
			return;
		highPrecisionEnabled = enabled;
		if (Math.abs( (xmax.doubleValue() - xmin.doubleValue())/getWidth() ) < HP_CUTOFF) {
			needsRedraw = true;
			repaint();
		}
		if (announceChanges) {
			firePropertyChange(PROPERTY_HIGH_PRECISION, ! highPrecisionEnabled, highPrecisionEnabled);
		}
	}
	
	public boolean getSubpixelSamplingEnabled() {
		return subpixelSamplingEnabled;
	}

	/**
	 * Set whether a second compute pass for subpixel sampling is enabled.
	 * Emits a property change event with name PROPERTY_SUBPIXEL_SAMPLING if the new
	 * value is not equal to the old value (and if the display is set to emit events).
	 */
	public void setSubpixelSamplingEnabled(boolean subpixelSamplingEnabled) {
		if (this.subpixelSamplingEnabled == subpixelSamplingEnabled)
			return;
		this.subpixelSamplingEnabled = subpixelSamplingEnabled;
		if (announceChanges) {
			firePropertyChange(PROPERTY_SUBPIXEL_SAMPLING, ! subpixelSamplingEnabled, subpixelSamplingEnabled);
		}
		synchronized(this) {
			if (OSC == null)
				return;
			if (!subpixelSamplingEnabled)
				subPixelCounts = null;
			if (subpixelSamplingEnabled && status == STATUS_DONE_FIRST_PASS) {
				startSecondPass();
				periodicRepaintTimer.start();
			}
			else if (!subpixelSamplingEnabled) {
				if (status == STATUS_RUNNING_SECOND_PASS && currentJob != null) {
					currentJob.cancel();
					currentJob = null;
				}
				if (status != STATUS_RUNNING_FIRST_PASS)
					setStatusWithFractionComplete(STATUS_DONE_FIRST_PASS, 1);
				applyPalette();
				repaint();
			}
		}
	}

	public int getPointsOnOrbit() {
		return pointsOnOrbit;
	}
	
	/**
	 * Set the number of points displayed on an orbit (when the user uses the mouse with
	 * the mouse action set to MOUSE_ACTION_SHOW_ORBIT).  NOTE: No way is provided in
	 * the current application interface to change this value.
	 * Emits a property change event with name PROPERTY_NUMBER_OF_POINTS_ON_ORBIT if the new
	 * value is not equal to the old value (and if the display is set to emit events).
	 */
	public void setPointsOnOrbit(int points) {
		if (points == pointsOnOrbit)
			return;
		int oldval = points;
		pointsOnOrbit = points;
		if (announceChanges)
			firePropertyChange(PROPERTY_NUMBER_OF_POINTS_ON_ORBIT, oldval, pointsOnOrbit);
		if (orbitPoints != null)
			repaint();
	}
	
	/**
	 * Returns the x/y limits that were requested, in the order xmin, xmax, ymin, ymax.
	 * The actual limits used might be different, because the actual limits are adjusted
	 * to match the aspect ratio of the image.
	 * @return
	 */
	public BigDecimal[] getLimitsRequested() {
		return new BigDecimal[] { xmin_requested, xmax_requested, ymin_requested, ymax_requested };
	}
	
	/**
	 * Get the actual limits used for the image.  (If the image has not yet been created,
	 * the requested limits are returned, and these might be adjusted when the image
	 * is drawn to reflect the aspect ratio of the image.)  Actual limits can also change
	 * when the image changes size.
	 */
	public BigDecimal[] getLimits() {
		if (OSC == null)
			return getLimitsRequested();
		else
			return new BigDecimal[] { xmin, xmax, ymin, ymax };
	}
	
	/**
	 * Sets the limits.  The parameter must be an array of four BigDecimals giving
	 * xmin, xmax, ymin, ymax in that order.
	 * @throws IllegalArgumentException if xmin is greater than or equal to xmax or
	 * if ymin is greater than or equal to ymax.
	 */
	public void setLimits(BigDecimal[] limits) {
		setLimits(limits[0], limits[1], limits[2], limits[3]);
	}
	
	/**
	 * Set the limits for the image.  Note that these requested limits might be
	 * adjusted to match the aspect ratio of the image.
	 * @throws IllegalArgumentException if xmin is greater than or equal to xmax or
	 * if ymin is greater than or equal to ymax.
	 */
	public void setLimits(BigDecimal xmin, BigDecimal xmax, BigDecimal ymin, BigDecimal ymax) {
		if (xmin.equals(this.xmin_requested) && xmax.equals(this.xmax_requested) 
				&& ymin.equals(this.ymin_requested) && ymax.equals(this.ymax_requested))
			return;
		if (xmax.compareTo(xmin) <= 0 || ymax.compareTo(ymin) <= 0)
			throw new IllegalArgumentException("maximums must be less than minimums");
		BigDecimal[] oldVal = getLimits();
		xmin_requested = xmin;
		xmax_requested = xmax;
		ymin_requested = ymin;
		ymax_requested = ymax;
		if (OSC != null)
			checkAspect();
		needsRedraw = true;
		if (announceChanges) {
			if ( ! (this.xmin.equals(oldVal[0]) && this.xmax.equals(oldVal[1]) && 
					this.ymin.equals(oldVal[2]) && this.ymax.equals(oldVal[3])) ) {
				firePropertyChange(PROPERTY_LIMITS, oldVal, new BigDecimal[] { this.xmin, this.xmax, this.ymin, this.ymax });
			}
		}
		repaint();
	}
	
	/**
	 * Get an array containing the values of xmin, xmax, ymin, and ymax as strings.
	 * The number of decimal places shown in the string depends on the scale (the size
	 * of the difference between xmin and xmax), and this might be fewer places than
	 * shown by the toString() method in the BigDecimal class.
	 */
	public String[] getLimitsAsStrings() {
		if (OSC == null)
			return new String[] { xmin_requested.toString(), xmax_requested.toString(), 
				ymin_requested.toString(), ymax_requested.toString() };
		BigDecimal dx = xmax.subtract(xmin);
		int d = 5;
		while (dx.compareTo(TEN) < 0) {
			d++;
			dx = dx.multiply(TEN);
		}
		BigDecimal x1 = xmin.setScale(d, BigDecimal.ROUND_HALF_EVEN);
		BigDecimal x2 = xmax.setScale(d, BigDecimal.ROUND_HALF_EVEN);
		BigDecimal y1 = ymin.setScale(d, BigDecimal.ROUND_HALF_EVEN);
		BigDecimal y2 = ymax.setScale(d, BigDecimal.ROUND_HALF_EVEN);
		return new String[] { x1.toString(), x2.toString(), y1.toString(), y2.toString() };
	}
	
	String[] getOrbitStartPointAsStrings() {
		if (orbitStartPoint == null)
			return null;
		BigDecimal dx = xmax.subtract(xmin);
		int d = 5;
		while (dx.compareTo(TEN) < 0) {
			d++;
			dx = dx.multiply(TEN);
		}
		BigDecimal x = orbitStartPoint[0].setScale(d, BigDecimal.ROUND_HALF_EVEN);
		BigDecimal y = orbitStartPoint[1].setScale(d, BigDecimal.ROUND_HALF_EVEN);
		return new String[] { x.toString(), y.toString() };
	}
	
	/**
	 * Returns an array whose length is equal to maxIterations+1 whose i-th
	 * entry is the number of pixels in the image for which the iteration count
	 * is i.  This method is used by the Palette editor to make the histogram
	 * in its histogram panel.
	 */
	synchronized public int[] createIterationCountHistogram() {
		if (OSC == null)
			return null;
		int[] histogram = new int[maxIterations];
		if (iterationCounts == null)
			return histogram;
		int rows = OSC.getHeight();
		int cols = OSC.getWidth();
		for (int row = 0; row < rows; row++) {
			int[] rowCounts = iterationCounts[row];
			if (rowCounts != null) {
				for (int col = 0; col < cols; col++)
					if (rowCounts[col] < maxIterations)
						histogram[rowCounts[col]]++;
			}
		}
		return histogram;
	}
	
	/**
	 * Return the current mouse action, which is one of the six mouse action
	 * constants such as MOUSE_ACTION_ZOOM_IN.
	 */
	public int getDefaultMouseAction() {
		return defaultMouseAction;
	}
	
	/**
	 * Set the mouse action.  The parameter must be one of the six constants, such 
	 * as MOUSE_ACTION_ZOOM_IN, that specify mouse actions.  If the new value is
	 * not the same as the old, a property change event with name
	 * PROPERTY_CURRENT_MOUSE_ACTION is emitted (if the display is set to emit such events).
	 */
	public void setDefaultMouseAction(int a) {
		if (a == defaultMouseAction)
			return;
		if (a < 0 || a > 5)
			throw new IllegalArgumentException("Illegal mouse action code " + a);
		int oldval = defaultMouseAction;
		defaultMouseAction = a;
		if (announceChanges)
			firePropertyChange(PROPERTY_CURRENT_MOUSE_ACTION, oldval, defaultMouseAction);
	}
	
	/**
	 * Set the display to show the image described by a given MandelbrotSettings object.
	 */
	public void applySettings(MandelbrotSettings settings) {
		setMaxIterations(settings.getMaxIterations());
		setPaletteInfo(settings.getPalette(),settings.getPaletteMapping());
		setMandelbrotColor(settings.getMandelbrotColor());
		setImageSize(settings.getImageSize());
		setLimits(settings.getLimits());
		setHighPrecisionEnabled(settings.isHighPrecisionEnabled()); // comes after setLimits, in case redraw is necessary only because of change in this value
	}
	
	/**
	 * Zoom the display about its center point by a given factor.  Values greater than
	 * 1 zoom out; values less than 1 zoom in.
	 * @throws IllegalArgumentException if the zoom factor is less than or equal to zero
	 */
	public void doZoom(double zoomFactor) {
		if (zoomFactor <= 0)
			throw new IllegalArgumentException("Zoom factor must be positive.");
		if (OSC == null)
			return;
		doZoom(OSC.getWidth()/2,OSC.getHeight()/2,zoomFactor,false);
	}
	
	/**
	 * Output the current image to a file.
	 * @param file  the output file
	 * @param format the format of the image (PNG and JPEG are definitely supported)
	 * @return true if the image format is supported and the image can be written, false otherwise
	 * @throws IOException if an exception occurs while trying to write the image
	 */
	synchronized public boolean writeImage(File file, String format) throws IOException {
		if (OSC == null)
			return false;
		else
			return ImageIO.write(OSC,format,file);
	}
	
	TaskManager getTaskManager() {  // for use by MultiprocessingConfigDialog
		return taskManager;
	}
	
	synchronized protected void paintComponent(Graphics g) {
		wasPainted = true;
		if (dragImageOffset != null) {
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(0,0,getWidth(),getHeight());
			g.drawImage(OSC,dragImageOffset.width,dragImageOffset.height,null);
			return;
		}
		if (resizing) {
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(0,0,getWidth(),getHeight());
			if (OSC != null)
				g.drawImage(OSC,0,0,null);
			return;
		}
		if (needsRedraw) {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			if (currentJob != null) {
				currentJob.cancel();
				currentJob = null;
				periodicRepaintTimer.stop();
			}
			int imageWidth = imageSize == null ? getWidth() : imageSize.width;
			int imageHeight = imageSize == null ? getHeight() : imageSize.height;
			subPixelCounts = null;
			iterationCounts = null;
			if (OSC == null || OSC.getHeight() != imageWidth || OSC.getWidth() != imageHeight) {
				Dimension oldSize = OSC == null ? null : new Dimension(OSC.getWidth(), OSC.getHeight());
				Dimension newSize = new Dimension(imageWidth,imageHeight);
				OSC = null;
				try {
					OSC = new BufferedImage(imageWidth,imageHeight,BufferedImage.TYPE_INT_RGB);
					iterationCounts = new int[OSC.getHeight()][OSC.getWidth()*2];  // just for memory check.
					checkAspect();
				}
				catch (OutOfMemoryError e) {
					OSC = null;
					iterationCounts = null;
					g.setColor(Color.WHITE);
					g.fillRect(0,0,getWidth(),getHeight());
					g.setColor(Color.RED);
					g.drawString(I18n.tr("mandelbrotDisplay.OutOfMemory"),20,30);
					setStatusWithFractionComplete(STATUS_IDLE, 0);
					return;
				}
				if (announceChanges && !newSize.equals(oldSize))
					firePropertyChange(PROPERTY_ACTUAL_IMAGE_SIZE, oldSize, newSize);
			}
			iterationCounts = new int[OSC.getHeight()][];
			setStatusWithFractionComplete(STATUS_RUNNING_FIRST_PASS,0);
			Graphics osg = OSC.getGraphics();
			osg.setColor(Color.LIGHT_GRAY);
			osg.fillRect(0,0,OSC.getWidth(),OSC.getHeight());
			osg.dispose();
			needsRedraw = false;
			currentJob = taskManager.createJob();
			int rows = OSC.getHeight();
			int cols = OSC.getWidth();
			BigDecimal dy = ymax.subtract(ymin).divide(new BigDecimal(rows-1), ymax.scale(), BigDecimal.ROUND_HALF_EVEN);
			if (!highPrecisionEnabled && dy.doubleValue() < 1E-20) {
				OSC = null;
				iterationCounts = null;
				g.setColor(Color.WHITE);
				g.fillRect(0,0,getWidth(),getHeight());
				g.setColor(Color.RED);
				g.drawString(I18n.tr("mandelbrotDisplay.HighPrecisionRequired"),20,30);
				setStatusWithFractionComplete(STATUS_IDLE, 0);
				return;		
			}
			boolean usingHighPrecision = highPrecisionEnabled && Math.abs(dy.doubleValue()) < HP_CUTOFF;
			for (int i = 0; i < rows; i++) {
				BigDecimal yval = ymax.subtract(dy.multiply(new BigDecimal(i)));
				MandelbrotTask task = new MandelbrotTask(i,xmin,xmax,yval,cols,maxIterations,usingHighPrecision);
				currentJob.add(task);
			}
			currentJob.close();
			currentJob.await(200);
			if (newPalette) {
				paletteColors = null;  // will be recomputed in grabAvailableResults()
				paletteColorComponents = null;
				newPalette = false;
			}
			grabAvailableResults();
			if (currentJob != null) {
				periodicRepaintTimer.start();
			}
			setCursor(Cursor.getDefaultCursor());
		}
		if (newPalette) {
			int length = paletteMapping.getLength();
			if (length == 0)
				length = maxIterations;
			paletteColors = palette.makeRGBs(length, paletteMapping.getOffset());
			paletteColorComponents = null;
			applyPalette();
			newPalette = false;
		}
		if (OSC.getWidth() < getWidth()) {
			int w = getWidth() - OSC.getWidth();
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(OSC.getWidth(),0,w,getHeight());
			g.setColor(Color.BLACK);
			g.drawLine(OSC.getWidth(),0,OSC.getWidth(),OSC.getHeight());
		}
		if (OSC.getHeight() < getHeight()) {
			int h = getHeight() - OSC.getHeight();
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(0,OSC.getHeight(),OSC.getWidth(),h);
			g.setColor(Color.BLACK);
			g.drawLine(0,OSC.getHeight(),OSC.getWidth(),OSC.getHeight());
		}
		g.drawImage(OSC,0,0,null);
		if (dragZoomRect != null) {
			g.setColor(Color.WHITE);
			g.drawRect(dragZoomRect.x,dragZoomRect.y,dragZoomRect.width-1,dragZoomRect.height-1);
			g.setColor(Color.BLACK);
			g.drawRect(dragZoomRect.x-1,dragZoomRect.y-1,dragZoomRect.width+1,dragZoomRect.height+1);
			g.drawRect(dragZoomRect.x+1,dragZoomRect.y+1,dragZoomRect.width-3,dragZoomRect.height-3);
		}
		if (orbitPoints != null) {
			g.setColor(Color.BLACK);
			for (Point p: orbitPoints) {
				g.fillRect(p.x-1,p.y-3,3,7);
				g.fillRect(p.x-3,p.y-1,7,3);
			}
			g.setColor(Color.WHITE);
			for (Point p: orbitPoints) {
				g.fillRect(p.x,p.y-2,1,5);
				g.fillRect(p.x-2,p.y,5,1);
			}
		}
		if (coordinateStrings != null) {
			FontMetrics fm = g.getFontMetrics(g.getFont());
			int h = fm.getHeight() * (coordinateStrings.length-1) + fm.getAscent() + fm.getDescent();
			int w = 0;
			for (String s : coordinateStrings) {
				int sw = fm.stringWidth(s);
				if (sw > w)
					w = sw;
			}
			int x = coordinatePoint.x - w - 13;
			int y = coordinatePoint.y - h - 13;
			if (x < 0)
				x = coordinatePoint.x + 20;
			if (y < 0)
				y = coordinatePoint.y + 20;
			g.setColor(Color.WHITE);
			g.fillRect(x+2,y+2,w+6,h+6);
			g.setColor(Color.BLACK);
			g.drawRect(x+2,y+2,w+6,h+6);
			for (int i = 0; i < coordinateStrings.length; i++)
				g.drawString(coordinateStrings[i], x + 5, y + 5 + fm.getAscent() + fm.getHeight()*i);
		}
	}
	
	private void setStatusWithFractionComplete(int newStatus, double fractionComplete) {
		if (newStatus == status && fractionComplete == this.fractionComplete)
			return;
		double oldFrac = this.fractionComplete;
		int oldStatus = status;
		status = newStatus;
		this.fractionComplete = fractionComplete;
		if (announceChanges)
			firePropertyChange(PROPERTY_STATUS, new StatusInfo(oldStatus,oldFrac), new StatusInfo(status,fractionComplete));
	}
		
	private void checkAspect() {  // adjust requested x/y limits to match aspect ration of image
		xmin = xmin_requested;
		xmax = xmax_requested;
		ymin = ymin_requested;
		ymax = ymax_requested;
		
		if (xmin.scale() < HP_CUTOFF_EXP + 8)
			xmin.setScale(HP_CUTOFF_EXP + 8);
		if (xmax.scale() < HP_CUTOFF_EXP + 8)
			xmax.setScale(HP_CUTOFF_EXP + 8);
		if (ymin.scale() < HP_CUTOFF_EXP + 8)
			ymin.setScale(HP_CUTOFF_EXP + 8);
		if (ymax.scale() < HP_CUTOFF_EXP + 8)
			ymax.setScale(HP_CUTOFF_EXP + 8);
		BigDecimal dx = xmax.subtract(xmin).setScale(Math.max(xmax.scale(),HP_CUTOFF_EXP)*2, BigDecimal.ROUND_HALF_EVEN);
		dx = dx.divide(new BigDecimal(OSC.getWidth()),BigDecimal.ROUND_HALF_EVEN);
		int precision = 0;
		while (dx.compareTo(TWO) < 0) {
		   precision++;
		   dx = dx.multiply(TEN);
		}
		if (precision < HP_CUTOFF_EXP)
			precision = HP_CUTOFF_EXP;
		scale = precision + 5 + (precision-10)/10;
		xmin = xmin.setScale(scale,BigDecimal.ROUND_HALF_EVEN);
		xmax = xmax.setScale(scale,BigDecimal.ROUND_HALF_EVEN);
		ymin = ymin.setScale(scale,BigDecimal.ROUND_HALF_EVEN);
		ymax = ymax.setScale(scale,BigDecimal.ROUND_HALF_EVEN);
		
		BigDecimal width = xmax.subtract(xmin);
		BigDecimal height = ymax.subtract(ymin);
		BigDecimal aspect = width.divide(height,BigDecimal.ROUND_HALF_EVEN);
		BigDecimal windowAspect = new BigDecimal((double)OSC.getWidth()/(double)OSC.getHeight());
		if (aspect.compareTo(windowAspect) < 0) {
			BigDecimal newWidth = width.multiply(windowAspect).divide(aspect,BigDecimal.ROUND_HALF_EVEN);
			BigDecimal center = xmax.add(xmin).divide(TWO,BigDecimal.ROUND_HALF_EVEN);
			xmax = center.add(newWidth.divide(TWO,BigDecimal.ROUND_HALF_EVEN)).setScale(scale, BigDecimal.ROUND_HALF_EVEN);
			xmin = center.subtract(newWidth.divide(TWO,BigDecimal.ROUND_HALF_EVEN)).setScale(scale,BigDecimal.ROUND_HALF_EVEN);
		}
		else if (aspect.compareTo(windowAspect) > 0) {
			BigDecimal newHeight = height.multiply(aspect).divide(windowAspect,BigDecimal.ROUND_HALF_EVEN);
			BigDecimal center = ymax.add(ymin).divide(TWO,BigDecimal.ROUND_HALF_EVEN);
			ymax = center.add(newHeight.divide(TWO,BigDecimal.ROUND_HALF_EVEN)).setScale(scale,BigDecimal.ROUND_HALF_EVEN);
			ymin = center.subtract(newHeight.divide(TWO,BigDecimal.ROUND_HALF_EVEN)).setScale(scale,BigDecimal.ROUND_HALF_EVEN);
		}
	}

	synchronized private void grabAvailableResults() { // called periodically during the computation to get available results and apply them to the image
		if (currentJob == null)
			return;
		MandelbrotTask[] tasks;
		double fractionComplete;
		boolean finished;
		synchronized(taskManager) {
			tasks = currentJob.finishedTasks();
			finished = currentJob.isFinished();
			fractionComplete = currentJob.fractionDone();
		}
		for (MandelbrotTask mbt : tasks) {
			int y = mbt.getRowNumber();
			int[] counts = mbt.getResults();
			if (paletteColors == null) {
				int offset = paletteMapping.getOffset();
				int length = paletteMapping.getLength();
				if (length == 0)
					length = maxIterations;
				paletteColors = palette.makeRGBs(length,offset);
				paletteColorComponents = null;
			}
			if (status == STATUS_RUNNING_SECOND_PASS) {
				subPixelCounts[y] = counts;
				if (y > 0 && subPixelCounts[y-1] != null)
					applySubpixelData(y-1);
				if (y < subPixelCounts.length-1 && subPixelCounts[y+1] != null)
					applySubpixelData(y);
			}
			else {
				iterationCounts[y] = counts;
				for (int x = 0; x < counts.length; x++) {
					int rgb;
					if (counts[x] == maxIterations)
						rgb = rgbForMandelbrot;
					else 
						rgb = paletteColors[counts[x] % paletteColors.length];
					OSC.setRGB(x, y, rgb);
				}
				repaint(0,y,counts.length,1);
			}
		}
		setFractionComplete(fractionComplete);
		if (finished) {
			if (status == STATUS_RUNNING_SECOND_PASS || !subpixelSamplingEnabled) {
				periodicRepaintTimer.stop();
				currentJob = null;
				int newStatus = (status == STATUS_RUNNING_FIRST_PASS) ? STATUS_DONE_FIRST_PASS : STATUS_IDLE;
				setStatusWithFractionComplete(newStatus, 1);
				return;
			}
			else {
				startSecondPass();
			}
		}
	}
	
	private void startSecondPass() {  // set up a TaskManager job to do a second pass for subpixel sampling
		   // Iteration counts are computed for points on "grid lines" BETWEEN pixels.
		   // The colors form these points are then averaged into the colors for the pixels themselves.
		   // The displayed color comes half from the pixel color and half form the colors at the
		   // four surrounding grid points.
		currentJob = taskManager.createJob();
		int rows = OSC.getHeight() + 1;
		int cols = OSC.getWidth() + 1;
		subPixelCounts = new int[rows][];
		BigDecimal dy = ymax.subtract(ymin).divide(new BigDecimal(rows-1), ymax.scale(), BigDecimal.ROUND_HALF_EVEN);
		BigDecimal halfDy = dy.divide(TWO, ymax.scale(), BigDecimal.ROUND_HALF_EVEN);
		BigDecimal xmin1 = xmin.subtract(halfDy);
		BigDecimal xmax1 = xmax.add(halfDy);
		BigDecimal ymax1 = ymax.add(halfDy);
		boolean usingHighPrecision = highPrecisionEnabled && Math.abs(dy.doubleValue()) < HP_CUTOFF;
		for (int i = 0; i < rows; i++) {
			BigDecimal yval = ymax1.subtract(dy.multiply(new BigDecimal(i)));
			MandelbrotTask task = new MandelbrotTask(i,xmin1,xmax1,yval,cols,maxIterations,usingHighPrecision);
			currentJob.add(task);
		}
		currentJob.close();
		setStatusWithFractionComplete(STATUS_RUNNING_SECOND_PASS, 0);
	}
	
	private void applySubpixelData(int row) {  // set the colors of a row of pixels using data from first and second passes.
		if (paletteColorComponents == null) {
			paletteColorComponents = new float[paletteColors.length][];
			for (int i = 0; i < paletteColors.length; i++)
				paletteColorComponents[i] = new Color(paletteColors[i]).getRGBColorComponents(null);
		}
		int[] rowData, beforeData, afterData;
		rowData = iterationCounts[row];
		beforeData = subPixelCounts[row];
		afterData = subPixelCounts[row+1];
		for (int col = 0; col < rowData.length; col++) {
			float[] a; // RGB color components for the pixels
			float[] b, c, d, e;  // RGB color components for the four surrounding grid points.
			if (rowData[col] == maxIterations)
				a = mandelbrotColorComponents;
			else
				a = paletteColorComponents[rowData[col] % paletteColors.length];
			if (beforeData[col] == maxIterations)
				b = mandelbrotColorComponents;
			else
				b = paletteColorComponents[beforeData[col] % paletteColors.length];
			if (beforeData[col+1] == maxIterations)
				c = mandelbrotColorComponents;
			else
				c = paletteColorComponents[beforeData[col+1] % paletteColors.length];
			if (afterData[col] == maxIterations)
				d = mandelbrotColorComponents;
			else
				d = paletteColorComponents[afterData[col] % paletteColors.length];
			if (afterData[col+1] == maxIterations)
				e = mandelbrotColorComponents;
			else
				e = paletteColorComponents[afterData[col+1] % paletteColors.length];
			float x,y,z;
			x = (4*a[0] + b[0] + c[0] + d[0] + e[0])/8;
			y = (4*a[1] + b[1] + c[1] + d[1] + e[1])/8;
			z = (4*a[2] + b[2] + c[2] + d[2] + e[2])/8;
			int rgb = new Color(x,y,z).getRGB();
			OSC.setRGB(col, row, rgb);
		}
	}
	
	private void applyPalette() { // Called when the palette changes; changes the (computed parts of) the image to match.
		if (paletteColors == null || OSC == null)
			return;
		int rows = OSC.getHeight();
		for (int y = 0; y < rows; y++) {
			if (iterationCounts[y] != null) {
				if (subPixelCounts != null && subPixelCounts[y] != null && subPixelCounts[y+1] != null) {
					applySubpixelData(y);
				}
				else {
					for (int x = 0; x < iterationCounts[y].length; x++) {
						int rgb;
						if (iterationCounts[y][x] == maxIterations)
							rgb = rgbForMandelbrot;
						else 
							rgb = paletteColors[iterationCounts[y][x] % paletteColors.length];
						OSC.setRGB(x, y, rgb);
					}
				}
			}
		}
	}
	
	private void setFractionComplete(double f) {
		if (f == fractionComplete)
			return;
		double oldval = fractionComplete;
		fractionComplete = f;
		int s = status;
		firePropertyChange(PROPERTY_STATUS, new StatusInfo(s,oldval), new StatusInfo(s,f));
	}
	
	private void doZoom(int x, int y, double zoomFactor, boolean recenter) {
		if (OSC == null)
			return;
		BigDecimal zf = new BigDecimal(zoomFactor);
		BigDecimal X = new BigDecimal(x);
		BigDecimal Y = new BigDecimal(y);
		BigDecimal ImageWidth = new BigDecimal(OSC.getWidth());
		BigDecimal ImageHeight = new BigDecimal(OSC.getHeight());
		BigDecimal oldWidth = xmax.subtract(xmin);
		BigDecimal oldHeight = ymax.subtract(ymin);
		BigDecimal newWidth = oldWidth.multiply(zf);
		BigDecimal newHeight = oldHeight.multiply(zf);
		BigDecimal pixelWidth = newWidth.divide(ImageWidth,BigDecimal.ROUND_HALF_EVEN);
		BigDecimal pixelHeight = newHeight.divide(ImageHeight,BigDecimal.ROUND_HALF_EVEN);
		BigDecimal centerX = xmin.add(X.multiply(oldWidth).divide(ImageWidth,BigDecimal.ROUND_HALF_EVEN));
		BigDecimal centerY = ymax.subtract(Y.multiply(oldHeight).divide(ImageHeight,BigDecimal.ROUND_HALF_EVEN));
		BigDecimal newXmin,newXmax,newYmin,newYmax;
		if (recenter) {
			newXmin = centerX.subtract(newWidth.divide(TWO,BigDecimal.ROUND_HALF_EVEN));
			newYmax = centerY.add(newHeight.divide(TWO,BigDecimal.ROUND_HALF_EVEN));
		}
		else {
			newXmin = centerX.subtract(X.multiply(pixelWidth));
			newYmax = centerY.add(Y.multiply(pixelHeight));
		}
		newYmin = newYmax.subtract(newHeight);
		newXmax = newXmin.add(newWidth);
		setLimits(newXmin, newXmax, newYmin, newYmax);
	}
	
	private void doRecenter(int x, int y) {
		BigDecimal pixelWidth = xmax.subtract(xmin).divide(new BigDecimal(OSC.getWidth()),BigDecimal.ROUND_HALF_EVEN);
		BigDecimal x1 = xmin.add(pixelWidth.multiply(new BigDecimal(x)));
		BigDecimal y1 = ymax.subtract(pixelWidth.multiply(new BigDecimal(y)));
		BigDecimal newXmin = x1.subtract(pixelWidth.multiply(new BigDecimal(OSC.getWidth()/2))).setScale(xmin.scale(),BigDecimal.ROUND_HALF_EVEN);
		BigDecimal newYmin = y1.subtract(pixelWidth.multiply(new BigDecimal(OSC.getHeight()/2))).setScale(xmin.scale(),BigDecimal.ROUND_HALF_EVEN);
		BigDecimal newXmax = newXmin.add(xmax.subtract(xmin)).setScale(xmin.scale(),BigDecimal.ROUND_HALF_EVEN);
		BigDecimal newYmax = newYmin.add(ymax.subtract(ymin)).setScale(xmin.scale(),BigDecimal.ROUND_HALF_EVEN);
		setLimits(newXmin,newXmax,newYmin,newYmax);
	}
	
		
	private void doZoomInOnRect(Rectangle rect) {
		if (OSC == null)
			return;
		BigDecimal rectX = new BigDecimal(rect.x);
		BigDecimal rectY = new BigDecimal(rect.y);
		BigDecimal rectW = new BigDecimal(rect.width);
		BigDecimal rectH = new BigDecimal(rect.height);
		BigDecimal ImageWidth = new BigDecimal(OSC.getWidth());
		BigDecimal ImageHeight = new BigDecimal(OSC.getHeight());
		BigDecimal pixelWidth = xmax.subtract(xmin).divide(ImageWidth,BigDecimal.ROUND_HALF_EVEN);
		BigDecimal pixelHeight = ymax.subtract(ymin).divide(ImageHeight,BigDecimal.ROUND_HALF_EVEN);
		BigDecimal newXmin,newXmax,newYmin,newYmax;
		newXmin = xmin.add(pixelWidth.multiply(rectX));
		newYmax = ymax.subtract(pixelHeight.multiply(rectY));
		BigDecimal newWidth = pixelWidth.multiply(rectW);
		BigDecimal newHeight = pixelHeight.multiply(rectH);
		newXmax = newXmin.add(newWidth);
		newYmin = newYmax.subtract(newHeight);
		setLimits(newXmin, newXmax, newYmin, newYmax);
	}
	
	private void doZoomOutFromRect(Rectangle rect) {
		if (OSC == null)
			return;
		BigDecimal rectX = new BigDecimal(rect.x);
		BigDecimal rectY = new BigDecimal(rect.y);
		BigDecimal rectW = new BigDecimal(rect.width);
		BigDecimal rectH = new BigDecimal(rect.height);
		BigDecimal ImageWidth = new BigDecimal(OSC.getWidth());
		BigDecimal ImageHeight = new BigDecimal(OSC.getHeight());
		BigDecimal newPixelWidth = xmax.subtract(xmin).divide(rectW,BigDecimal.ROUND_HALF_EVEN);
		BigDecimal newPixelHeight = ymax.subtract(ymin).divide(rectH,BigDecimal.ROUND_HALF_EVEN);
		BigDecimal newXmin,newXmax,newYmin,newYmax;
		newXmin = xmin.subtract(newPixelWidth.multiply(rectX));
		newYmax = ymax.add(newPixelHeight.multiply(rectY));
		BigDecimal newWidth = newPixelWidth.multiply(ImageWidth);
		BigDecimal newHeight = newPixelHeight.multiply(ImageHeight);
		newXmax = newXmin.add(newWidth);
		newYmin = newYmax.subtract(newHeight);
		setLimits(newXmin, newXmax, newYmin, newYmax);
	}
	
	private void showCoords(int x, int y) {
		BigDecimal pixelWidth = xmax.subtract(xmin).divide(new BigDecimal(OSC.getWidth()),BigDecimal.ROUND_HALF_EVEN);
		BigDecimal dx = pixelWidth;
		int d = 3;
		while (dx.compareTo(TEN) < 0) {
			d++;
			dx = dx.multiply(TEN);
		}
		BigDecimal x1 = xmin.add(pixelWidth.multiply(new BigDecimal(x))).setScale(d,BigDecimal.ROUND_HALF_EVEN);
		BigDecimal y1 = ymax.subtract(pixelWidth.multiply(new BigDecimal(y))).setScale(d,BigDecimal.ROUND_HALF_EVEN);
		String s1 = "x = " + x1;
		String s2 = "y = " + y1;
		String s3 = null;
		if (x >= 0 && x < OSC.getWidth() && y >= 0 && y < OSC.getHeight()) {
			synchronized(this) {
				if (iterationCounts != null && iterationCounts[y] != null)
					s3 = I18n.tr("mandelbrotDisplay.IterationCount") + " = " + iterationCounts[y][x];
			}
		}
		if (s3 == null)
			coordinateStrings = new String[] { s1, s2 };
		else
			coordinateStrings = new String[] { s1, s2, s3 };
		coordinatePoint = new Point(x,y);
		repaint();
	}
	
	private void showOrbit(int startX, int startY) { 
		BigDecimal pixelWidth = xmax.subtract(xmin).divide(new BigDecimal(OSC.getWidth()),BigDecimal.ROUND_HALF_EVEN);
		BigDecimal pixelHeight = ymax.subtract(ymin).divide(new BigDecimal(OSC.getHeight()),BigDecimal.ROUND_HALF_EVEN);
		BigDecimal x = xmin.add(pixelWidth.multiply(new BigDecimal(startX))).setScale(scale,BigDecimal.ROUND_HALF_EVEN);
		BigDecimal y = ymax.subtract(pixelHeight.multiply(new BigDecimal(startY))).setScale(scale,BigDecimal.ROUND_HALF_EVEN);
		BigDecimal zx = x;
		BigDecimal zy = y;
		BigDecimal BIG = new BigDecimal(100);
		orbitPoints = new ArrayList<Point>();
		orbitPoints.add(new Point(startX,startY));
		for (int i = 1; i < pointsOnOrbit; i++) {
			BigDecimal zx2 = zx.multiply(zx).setScale(scale,BigDecimal.ROUND_HALF_EVEN);
			BigDecimal zy2 = zy.multiply(zy).setScale(scale,BigDecimal.ROUND_HALF_EVEN);
			if (zx2.add(zy2).abs().compareTo(BIG) > 0)
				break;
			BigDecimal newZX = zx2.subtract(zy2).add(x);
			BigDecimal newZY = TWO.multiply(zx).multiply(zy).setScale(scale,BigDecimal.ROUND_HALF_EVEN).add(y);
			zx = newZX;
			zy = newZY;
			if (zx.compareTo(xmin) >= 0 && zx.compareTo(xmax) <= 0 && zy.compareTo(ymin) >= 0 && zy.compareTo(ymax) <= 0) {
				int px = zx.subtract(xmin).divide(pixelWidth,BigDecimal.ROUND_HALF_EVEN).intValue();
				int py = ymax.subtract(zy).divide(pixelHeight,BigDecimal.ROUND_HALF_EVEN).intValue();
				orbitPoints.add(new Point(px,py));
			}
		}
		repaint(); 
		BigDecimal[] oldval = orbitStartPoint;
		orbitStartPoint = new BigDecimal[] { x, y };
		if (announceChanges)
			firePropertyChange(PROPERTY_ORBIT_POINT, oldval, orbitStartPoint);
	}
	
	protected void setOrbitStart(BigDecimal x, BigDecimal y) {
		if (x == null)
			orbitPoints = null;
		else {
			BigDecimal pixelWidth = xmax.subtract(xmin).divide(new BigDecimal(OSC.getWidth()),BigDecimal.ROUND_HALF_EVEN);
			BigDecimal pixelHeight = ymax.subtract(ymin).divide(new BigDecimal(OSC.getHeight()),BigDecimal.ROUND_HALF_EVEN);
			BigDecimal zx = x;
			BigDecimal zy = y;
			BigDecimal BIG = new BigDecimal(100);
			orbitPoints = new ArrayList<Point>();
			for (int i = 0; i < pointsOnOrbit; i++) {
				if (zx.compareTo(xmin) >= 0 && zx.compareTo(xmax) <= 0 && zy.compareTo(ymin) >= 0 && zy.compareTo(ymax) <= 0) {
					int px = zx.subtract(xmin).divide(pixelWidth,BigDecimal.ROUND_HALF_EVEN).intValue();
					int py = ymax.subtract(zy).divide(pixelHeight,BigDecimal.ROUND_HALF_EVEN).intValue();
					orbitPoints.add(new Point(px,py));
				}
				BigDecimal zx2 = zx.multiply(zx).setScale(scale,BigDecimal.ROUND_HALF_EVEN);
				BigDecimal zy2 = zy.multiply(zy).setScale(scale,BigDecimal.ROUND_HALF_EVEN);
				if (zx2.add(zy2).abs().compareTo(BIG) > 0)
					break;
				BigDecimal newZX = zx2.subtract(zy2).add(x);
				BigDecimal newZY = TWO.multiply(zx).multiply(zy).setScale(scale,BigDecimal.ROUND_HALF_EVEN).add(y);
				zx = newZX;
				zy = newZY;
			}
		}
		repaint(); 
	}
		
	private class MouseHandler extends MouseAdapter implements MouseMotionListener {
		boolean dragging;
		boolean movedMouse = false;
		int startX, startY;
		int mouseAction;
		public void mousePressed(MouseEvent evt) {
			if (OSC == null || dragging)
				return;
			startX = evt.getX();
			startY = evt.getY();
			if (startX > OSC.getWidth() || startY > OSC.getHeight())
				return;
			if (evt.isShiftDown())
				mouseAction = MOUSE_ACTION_DRAG;
			else if (evt.isControlDown() || evt.isMetaDown())
				mouseAction = MOUSE_ACTION_ZOOM_IN;
			else if (evt.isAltDown())
				mouseAction = MOUSE_ACTION_ZOOM_OUT;
			else
				mouseAction = defaultMouseAction;
			if (mouseAction == MOUSE_ACTION_RECENTER_ON_POINT) {
				doRecenter(startX, startY);
				return;
			}
			if (mouseAction == MOUSE_ACTION_ZOOM_IN && evt.getClickCount() == 2) {
				doZoom(startX, startY, 0.5, false);
				return;
			}
			if (mouseAction == MOUSE_ACTION_ZOOM_OUT && evt.getClickCount() == 2) {
				doZoom(startX, startY, 2, false);
				return;
			}
			dragging = true;
			addMouseMotionListener(this);
			movedMouse = false;
			if (mouseAction == MOUSE_ACTION_SHOW_COORDS)
				showCoords(startX,startY);
			else if (mouseAction == MOUSE_ACTION_SHOW_ORBIT)
				showOrbit(startX,startY);
		}
		public void mouseReleased(MouseEvent evt) {
			if (!dragging)
				return;
			removeMouseMotionListener(this);
			if (mouseAction == MOUSE_ACTION_DRAG && dragImageOffset != null) {
				BigDecimal dx = xmax.subtract(xmin).divide(new BigDecimal(OSC.getWidth()),BigDecimal.ROUND_HALF_EVEN);
				BigDecimal dy = ymax.subtract(ymin).divide(new BigDecimal(OSC.getHeight()),BigDecimal.ROUND_HALF_EVEN);
				BigDecimal offsetX = dx.multiply(new BigDecimal(dragImageOffset.width)).setScale(xmin.scale(),BigDecimal.ROUND_HALF_EVEN);
				BigDecimal offsetY = dy.multiply(new BigDecimal(dragImageOffset.height)).setScale(xmin.scale(),BigDecimal.ROUND_HALF_EVEN);
				BigDecimal xmin1 = xmin.subtract(offsetX).setScale(xmin.scale(),BigDecimal.ROUND_HALF_EVEN);
				BigDecimal xmax1 = xmax.subtract(offsetX).setScale(xmin.scale(),BigDecimal.ROUND_HALF_EVEN);
				BigDecimal ymin1 = ymin.add(offsetY).setScale(xmin.scale(),BigDecimal.ROUND_HALF_EVEN);
				BigDecimal ymax1 = ymax.add(offsetY).setScale(xmin.scale(),BigDecimal.ROUND_HALF_EVEN);
				setLimits(xmin1,xmax1,ymin1,ymax1);
			}
			else if (mouseAction == MOUSE_ACTION_ZOOM_IN && dragZoomRect != null)
				doZoomInOnRect(dragZoomRect);
			else if (mouseAction == MOUSE_ACTION_ZOOM_OUT && dragZoomRect != null)
				doZoomOutFromRect(dragZoomRect);
		    dragging = false;
		    dragImageOffset = null;
		    dragZoomRect = null;
		    coordinateStrings = null;
		    coordinatePoint = null;
		    orbitPoints = null;
		    if (orbitStartPoint != null) {
		    	BigDecimal[] oldval = orbitStartPoint;
		    	orbitStartPoint = null;
		    	if (announceChanges)
		    		firePropertyChange(PROPERTY_ORBIT_POINT, oldval, orbitStartPoint);
		    }
		    repaint();
		}
		public void mouseDragged(MouseEvent evt) {
			if (!dragging)
				return;
			int x = evt.getX();
			int y = evt.getY();
			if (mouseAction == MOUSE_ACTION_SHOW_COORDS) {
				showCoords(x,y);
				return;
			}
			if (mouseAction == MOUSE_ACTION_SHOW_ORBIT) {
				showOrbit(x,y);
				return;
			}
			int offsetX = x - startX;
			int offsetY = y - startY;
			if (!movedMouse && Math.abs(offsetX) < 3 && Math.abs(offsetY) < 3)
				return;
			movedMouse = true;
			if (mouseAction == MOUSE_ACTION_DRAG) {
				if (Math.abs(offsetX) < 3 && Math.abs(offsetY) < 3)
					dragImageOffset = null;
				else
					dragImageOffset = new Dimension(offsetX,offsetY);
			}
			else if (mouseAction == MOUSE_ACTION_ZOOM_IN || mouseAction == MOUSE_ACTION_ZOOM_OUT) {
				int width = Math.abs(offsetX);
				int height = Math.abs(offsetY);
				if (width < 3 || height < 3)
					dragZoomRect = null;
				else {
					double aspect = (double)OSC.getWidth() / OSC.getHeight();
					double rectAspect = (double)width / height;
					if (aspect > rectAspect)
						width = (int)(width*aspect/rectAspect+0.499);
					else if (aspect < rectAspect)
						height = (int)(height*rectAspect/aspect+0.499);
					int xmin;
					if (startX < x)
						xmin = startX;
					else
						xmin = startX - width;
					int ymin;
					if (startY < y)
						ymin = startY;
					else
						ymin = startY - height;
					dragZoomRect = new Rectangle(xmin,ymin,width,height);
				}
			}
			repaint();
		}
		public void mouseMoved(MouseEvent e) {
		}
	}
}
