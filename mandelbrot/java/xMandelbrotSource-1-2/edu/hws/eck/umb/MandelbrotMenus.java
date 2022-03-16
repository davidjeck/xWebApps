package edu.hws.eck.umb;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import edu.hws.eck.umb.palette.Palette;
import edu.hws.eck.umb.palette.PaletteEditDialog;
import edu.hws.eck.umb.util.I18n;
import edu.hws.eck.umb.util.SimpleFileChooser;
import edu.hws.eck.umb.util.Util;

/**
 * Defines a menu bar for a MandelbrotDisplay (and implements all the
 * commands in that menu bar).  Although this is a very big class,
 * only the constructor is public.
 */
public class MandelbrotMenus extends JMenuBar {
	
	private MandelbrotDisplay display;
	private boolean isApplet;
	private boolean isFrame;
	private UndoManager undoManager;
	private StatusBar statusBar;
	private boolean addToUndoManagerIsSuspended;
	
	private JMenuItem undoMenuItem;
	private JMenuItem redoMenuItem;
	private JCheckBoxMenuItem enableHighPrecision;
	private JCheckBoxMenuItem enableSubpixelSampling;
	
	private JDialog overviewDialog;
	private JDialog paletteEditorDialog;
	
	private int[] maxIterationMenuValues = { 50, 100, 250, 500, 1000, 2000, 5000, 10000, 50000 };
	private ArrayList<JRadioButtonMenuItem> maxIterationMenuItems;
	private Dimension[] imageSizes = { null, // null indicates image size matches window size
		new Dimension(160,120), new Dimension(400,300), new Dimension(640,480), new Dimension(800,600), 
		new Dimension(1024,768), new Dimension(1280,800), new Dimension(1280,1024), new Dimension(1440,900), 
		new Dimension(1680,1050), new Dimension(1920,1200)
	};
	private int[] zoomByFactors={5,10,20,50,100,250,1000,10000,100000};
	private ArrayList<JRadioButtonMenuItem> imageSizeMenuItems;
	private JRadioButtonMenuItem[] mandelbrotColorMenuItems;
	private JRadioButtonMenuItem[] toolsMenuItems;
	private int[] toolCodes;
	
	private SimpleFileChooser fileChooser;
	
	private final static String ALL_SETTINGS = "MB_CHANGE_ALL_SETTINGS";
	
	/**
	 * Create a menu bar for use with a given MandelbrotDisplay.  The menu bar listens for
	 * property change events from the display to enable/disable and set the text of some
	 * of its commands, and (most important) to implement the undo/redo functionality in
	 * the Edit menu.
	 * @param display The MandelbrotDisplay with which this menu bar is associated
	 * @param frame The frame in which the menu bar will be used, or null if it will not
	 * be used in a frame.  Keyboard accelerators are used only if the menu bar is in a frame,
	 * and the File menu is added only if the menu bar is for a frame and is not for an applet
	 * (that is if it is for a frame in a standalone application).
	 * @param statusBar The status bar for the MandelbrotDisplay.  The menu bar displays information
	 * about some commands here, if the statusBar is non-null.
	 * @param isForApplet Tells if the menu bar is for use by an applet (in the applet itself or
	 * in a frame opened by the applet).  If so, the File menu and the multiprocessing configuration
	 * command are omitted.
	 */
	public MandelbrotMenus(MandelbrotDisplay display, JFrame frame, StatusBar statusBar, boolean isForApplet) {
		this.display = display;
		this.statusBar = statusBar;
		this.isApplet = isForApplet;
		this.isFrame = (frame != null);
		undoManager = new UndoManager();
		undoManager.setLimit(100);
		if (!isApplet && isFrame)
			add(makeFileMenu(frame));
		add(makeEditMenu());
		add(makeControlMenu());
		add(makeIterationsMenu());
		add(makeImageSizeMenu());
		add(makeToolsMenu());
		add(makeExamplesMenu());
		display.addPropertyChangeListener( new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				String p = evt.getPropertyName();
				if (p.equals(MandelbrotDisplay.PROPERTY_REQUESTED_IMAGE_SIZE))
					checkImageSize((Dimension)evt.getNewValue());
				else if (p.equals(MandelbrotDisplay.PROPERTY_MAX_ITERATIONS))
					checkMaxIterations((Integer)evt.getNewValue());
				else if (p.equals(MandelbrotDisplay.PROPERTY_HIGH_PRECISION))
					enableHighPrecision.setSelected(MandelbrotMenus.this.display.getHighPrecisionEnabled());
				else if (p.equals(MandelbrotDisplay.PROPERTY_SUBPIXEL_SAMPLING))
					enableSubpixelSampling.setSelected(MandelbrotMenus.this.display.getSubpixelSamplingEnabled());
				else if (p.equals(MandelbrotDisplay.PROPERTY_MANDLELBROT_COLOR))
					checkMandelbrotColor();
				else if (p.equals(MandelbrotDisplay.PROPERTY_CURRENT_MOUSE_ACTION))
					checkTool();
				if (!addToUndoManagerIsSuspended && ((p.equals(MandelbrotDisplay.PROPERTY_REQUESTED_IMAGE_SIZE)) ||
						(p.equals(MandelbrotDisplay.PROPERTY_LIMITS)) ||
						(p.equals(MandelbrotDisplay.PROPERTY_MANDLELBROT_COLOR)) ||
						(p.equals(MandelbrotDisplay.PROPERTY_MAX_ITERATIONS)) ||
						(p.equals(MandelbrotDisplay.PROPERTY_PALETTE)) )) {
					undoManager.addEdit(new UndoableChange(p,evt.getOldValue(),evt.getNewValue()));
					updateUndoRedoItems();
				}
			}
		});
	}
	
	private void setStatusBarText(String text, int secondsToKeep) {
		if (statusBar != null)
			statusBar.setTempText(text, secondsToKeep);
	}
	
	private KeyStroke getAccelerator(String accel) {
		if (isFrame)
			return Util.getAccelerator(accel);
		else
			return null;
	}
	
	private JMenu makeFileMenu(final JFrame frame) {  // only used when display is in a Frame
		JMenu menu = new JMenu(I18n.tr("mandelbrotMenu.menuName.File"));
		final JMenuItem save = new JMenuItem(I18n.tr("mandelbrotMenu.command.SaveSettings"));
		final JMenuItem open = new JMenuItem(I18n.tr("mandelbrotMenu.command.OpenSettings"));
		final JMenuItem savePNG = new JMenuItem(I18n.tr("mandelbrotMenu.command.SavePNGImage"));
		final JMenuItem saveJPG = new JMenuItem(I18n.tr("mandelbrotMenu.command.SaveJPEGImage"));
		final JMenuItem quit = new JMenuItem(I18n.tr("mandelbrotMenu.command.Quit"));
		ActionListener lis = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Object src = evt.getSource();
				if (src == save) {
					doSave();
				}
				else if (src == open) {
					MandelbrotSettings settings = doOpen();
					if (settings != null) {
						MandelbrotSettings oldVal = new MandelbrotSettings(display);
						addToUndoManagerIsSuspended = true;
						display.applySettings(settings);
						addToUndoManagerIsSuspended = false;
						undoManager.addEdit(new UndoableChange(ALL_SETTINGS, oldVal, settings));
						updateUndoRedoItems();
					}
				}
				else if (src == savePNG) {
					doSaveImage("PNG");
				}
				else if (src == saveJPG) {
					doSaveImage("JPEG");
				}
				else if (src == quit) {
					frame.dispose();
				}
			}
		};
		save.addActionListener(lis);
		save.setAccelerator(getAccelerator("S"));
		open.addActionListener(lis);
		open.setAccelerator(getAccelerator("O"));
		savePNG.addActionListener(lis);
		savePNG.setAccelerator(getAccelerator("shift S"));
		saveJPG.addActionListener(lis);
		quit.addActionListener(lis);
		quit.setAccelerator(getAccelerator("Q"));
		menu.add(save);
		menu.add(open);
		menu.addSeparator();
		menu.add(savePNG);
		menu.add(saveJPG);
		menu.addSeparator();
		menu.add(quit);
		return menu;
	}
	
	private JMenu makeEditMenu() {
		JMenu menu = new JMenu(I18n.tr("mandelbrotMenu.menuName.Edit"));
		undoMenuItem = new JMenuItem(undoManager.getUndoPresentationName());
		undoMenuItem.setAccelerator(getAccelerator("Z"));
		undoMenuItem.setEnabled(false);
		redoMenuItem = new JMenuItem(undoManager.getRedoPresentationName());
		redoMenuItem.setAccelerator(getAccelerator("shift Z"));
		redoMenuItem.setEnabled(false);
		final JMenuItem copyLimits = new JMenuItem(I18n.tr("mandelbrotMenu.command.CopyLimitsToClipboard"));
		copyLimits.setAccelerator(getAccelerator("C"));
		ActionListener lis = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Object src = evt.getSource();
				if (src == undoMenuItem) {
					addToUndoManagerIsSuspended = true;
					undoManager.undo();
					addToUndoManagerIsSuspended = false;
					updateUndoRedoItems();
				}
				else if (src == redoMenuItem) {
					addToUndoManagerIsSuspended = true;
					undoManager.redo();
					addToUndoManagerIsSuspended = false;
					updateUndoRedoItems();
				}
				else if (src == copyLimits) {
					try {
						BigDecimal[] limits = display.getLimits();
						StringWriter strout = new StringWriter();
						PrintWriter out = new PrintWriter(strout);
						out.println(I18n.tr("term.MinimumX") + ": " + limits[0]);
						out.println(I18n.tr("term.MaximumX") + ": " + limits[1]);
						out.println(I18n.tr("term.MinimumY") + ": " + limits[2]);
						out.println(I18n.tr("term.MaximumY") + ": " + limits[3]);
						out.close();
						Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
						StringSelection sel = new StringSelection(strout.toString());
						clipboard.setContents(sel, sel);
						setStatusBarText(I18n.tr("mandlebrotMenus.statusText.LimitsCopied"), 2);
					}
					catch (Exception e) {
						JOptionPane.showMessageDialog(display, I18n.tr("mandelbrotMenu.error.CannotCopyToClipboard"));
					}
				}
			}
		};
		undoMenuItem.addActionListener(lis);
		redoMenuItem.addActionListener(lis);
		copyLimits.addActionListener(lis);
		menu.add(undoMenuItem);
		menu.add(redoMenuItem);
		menu.addSeparator();
		menu.add(copyLimits);
		return menu;
	}
	
	private JMenu makeControlMenu() {
		JMenu menu = new JMenu(I18n.tr("mandelbrotMenu.menuName.Control"));
		String[] defaultPaletteNames = new String[] { "Spectrum", "PaleSpectrum", "DarkSpectrum","Grayscale",
				"CyclicGrayscale","CyclicRedCyan", "EarthSky", "HotCold", "Fire" };		
		final JMenuItem editPaletteMenuItem = new JMenuItem(I18n.tr("mandelbrotMenu.command.ShowPaletteEditor"));
		JMenu mbColor = new JMenu(I18n.tr("mandelbrotMenu.menuName.MandelbrotColor"));
		JMenu defalutPalettes = new JMenu(I18n.tr("mandelbrotMenu.menuName.ApplyDefaultPalette"));
		final JRadioButtonMenuItem black = new JRadioButtonMenuItem(I18n.tr("colorName.Black"));
		final JRadioButtonMenuItem gray = new JRadioButtonMenuItem(I18n.tr("colorName.Gray"));
		final JRadioButtonMenuItem white = new JRadioButtonMenuItem(I18n.tr("colorName.White"));
		final JRadioButtonMenuItem blue = new JRadioButtonMenuItem(I18n.tr("colorName.Blue"));
		final JRadioButtonMenuItem custom = new JRadioButtonMenuItem(I18n.tr("menuCommand.Custom"));
		final JMenuItem defaultLimits = new JMenuItem(I18n.tr("mandelbrotMenu.command.RestoreDefaultLimits"));
		final JMenuItem allDefaults = new JMenuItem(I18n.tr("mandelbrotMenu.command.RestoreAllDefaults"));
		final JMenuItem zoomIn = new JMenuItem(I18n.tr("mandelbrotMenu.command.ZoomIn"));
		final JMenuItem zoomOut = new JMenuItem(I18n.tr("mandelbrotMenu.command.ZoomOut"));
		JMenu zoomInByMenu = new JMenu(I18n.tr("mandelbrotMenu.command.ZoomInBy"));
		JMenu zoomOutByMenu = new JMenu(I18n.tr("mandelbrotMenu.command.ZoomOutBy"));
		final JMenuItem setLimits = new JMenuItem(I18n.tr("mandelbrotMenu.command.SetLimits"));
		final JMenuItem overviewMenuItem = new JMenuItem(I18n.tr("mandelbrotMenu.command.ShowOverviewWindow"));
		final JMenuItem multiprocessingConfig = new JMenuItem(I18n.tr("mandelbrotMenu.command.ConfigureMultiprocessing"));
		final JMenuItem help = new JMenuItem(I18n.tr("mandelbrotMenu.command.ControlMenu.help"));
		enableHighPrecision = new JCheckBoxMenuItem(I18n.tr("mandelbrotMenu.command.EnableHighPrecision"));
		enableSubpixelSampling = new JCheckBoxMenuItem(I18n.tr("mandelbrotMenu.command.EnableSubpixelSampling"));
		ActionListener lis = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Object src = evt.getSource();
				if (src == editPaletteMenuItem) {
					if (paletteEditorDialog == null) {
						paletteEditorDialog = PaletteEditDialog.createDialog(display);
						paletteEditorDialog.setVisible(true);
						editPaletteMenuItem.setText(I18n.tr("mandelbrotMenu.command.HidePaletteEditor"));
						paletteEditorDialog.addWindowListener(new WindowAdapter() {
							public void windowClosed(WindowEvent evt) {
								paletteEditorDialog = null;
								editPaletteMenuItem.setText(I18n.tr("mandelbrotMenu.command.ShowPaletteEditor"));
							}
						});
					}
					else {
						paletteEditorDialog.dispose();
 					}
				}
				else if (src == black)
					display.setMandelbrotColor(Color.BLACK);
				else if (src == gray)
					display.setMandelbrotColor(Color.GRAY);
				else if (src == white)
					display.setMandelbrotColor(Color.WHITE);
				else if (src == blue)
					display.setMandelbrotColor(Color.BLUE);
				else if (src == custom) {
					Color c = display.getMandelbrotColor();
					c = JColorChooser.showDialog(display, I18n.tr(""), c);
					if (c != null)
						display.setMandelbrotColor(c);
				}
				else if (src == defaultLimits)
					display.setLimits(new BigDecimal(-2.333), new BigDecimal(1), new BigDecimal(-1.25), new BigDecimal(1.25));
				else if (src == allDefaults) { // restore all default values
					MandelbrotSettings oldVal = new MandelbrotSettings(display);
					MandelbrotSettings newVal = new MandelbrotSettings();
					addToUndoManagerIsSuspended = true;  // turn off saving undo items; the entire transaction will be recorded as a single item
					display.applySettings(newVal);
					addToUndoManagerIsSuspended = false;
					undoManager.addEdit(new UndoableChange(ALL_SETTINGS, oldVal, newVal));  // add this transaction as an undo item
					updateUndoRedoItems();
					display.setDefaultMouseAction(MandelbrotDisplay.MOUSE_ACTION_ZOOM_IN);
					display.setSubpixelSamplingEnabled(false);
				}
				else if (src == zoomIn)
					display.doZoom(0.5);
				else if (src == zoomOut)
					display.doZoom(2);
				else if (src == enableHighPrecision)
					display.setHighPrecisionEnabled(enableHighPrecision.isSelected());
				else if (src == enableSubpixelSampling)
					display.setSubpixelSamplingEnabled(enableSubpixelSampling.isSelected());
				else if (src == overviewMenuItem) {
					if (overviewDialog == null) {
						overviewDialog = MandelbrotOverviewDisplay.createDialog(display);
						overviewDialog.setVisible(true);
						overviewMenuItem.setText(I18n.tr("mandelbrotMenu.command.HideOverviewWindow"));
						overviewDialog.addWindowListener(new WindowAdapter() {
							public void windowClosed(WindowEvent evt) {
								overviewDialog = null;
								overviewMenuItem.setText(I18n.tr("mandelbrotMenu.command.ShowOverviewWindow"));
							}
						});
					}
					else {
						overviewDialog.dispose();
					}
				}
				else if (src == setLimits) {
					BigDecimal[] limits = SetLimitsDialog.showDialog(display, display.getLimitsAsStrings());
					if (limits != null)
						display.setLimits(limits);
				}
				else if (src == multiprocessingConfig)
					MultiprocessingConfigDialog.showDialog(display);
				else if (src == help) {
					JOptionPane.showMessageDialog(display, I18n.tr("mandelbrotMenu.ControlMenu.HelpText"));
				}
				else { // src is in default palette menu or zoomInBy/zoomOutBy menu
					String cmd = evt.getActionCommand();
					try {
						double zoomFactor = Double.parseDouble(cmd); // if it's a number, it's a zoom factor
						display.doZoom(zoomFactor);
					}
					catch (NumberFormatException e) {
						String paletteName = cmd;
						Palette p = Palette.makeDefaultPalette(paletteName);
						display.setPalette(p);
					}
				}
			}
		};
		editPaletteMenuItem.addActionListener(lis);
		editPaletteMenuItem.setAccelerator(getAccelerator("P"));
		ButtonGroup grp = new ButtonGroup();
		black.addActionListener(lis);
		grp.add(black);
		mbColor.add(black);
		black.setAccelerator(getAccelerator("K"));
		gray.addActionListener(lis);
		grp.add(gray);
		mbColor.add(gray);
		white.addActionListener(lis);
		grp.add(white);
		mbColor.add(white);
		blue.addActionListener(lis);
		grp.add(blue);
		mbColor.add(blue);
		custom.addActionListener(lis);
		grp.add(custom);
		mbColor.add(custom);
		mandelbrotColorMenuItems = new JRadioButtonMenuItem[] { black, gray, white, blue, custom };
		checkMandelbrotColor();
		for (String paletteName : defaultPaletteNames) {
			JMenuItem item = new JMenuItem(I18n.tr("paletteEditDialog.menuCommand.LoadDefault."+paletteName));
			item.setActionCommand(paletteName);
			item.addActionListener(lis);
			defalutPalettes.add(item);
		}
		defaultLimits.addActionListener(lis);
		defaultLimits.setAccelerator(getAccelerator("R"));
		allDefaults.addActionListener(lis);
		allDefaults.setAccelerator(getAccelerator("shift R"));
		zoomIn.addActionListener(lis);
		zoomIn.setAccelerator(getAccelerator("I"));
		zoomOut.addActionListener(lis);
		zoomOut.setAccelerator(getAccelerator("shift I"));
		for (int zoom : zoomByFactors) {
			JMenuItem zi = new JMenuItem(I18n.tr("mandelbrotMenu.command.ZoomByFactor",zoom));
			zi.setActionCommand(""+(1.0/zoom));
			zi.addActionListener(lis);
			zoomInByMenu.add(zi);
			JMenuItem zo = new JMenuItem(I18n.tr("mandelbrotMenu.command.ZoomByFactor",zoom));
			zo.setActionCommand(""+zoom);
			zo.addActionListener(lis);
			zoomOutByMenu.add(zo);
		}
		setLimits.addActionListener(lis);
		overviewMenuItem.addActionListener(lis);
		enableHighPrecision.addActionListener(lis);
		enableHighPrecision.setSelected(display.getHighPrecisionEnabled());
		enableSubpixelSampling.addActionListener(lis);
		enableSubpixelSampling.setSelected(display.getSubpixelSamplingEnabled());
		multiprocessingConfig.addActionListener(lis);
		help.addActionListener(lis);
		menu.add(editPaletteMenuItem);
		menu.add(mbColor);
		menu.add(defalutPalettes);
		menu.addSeparator();
		menu.add(defaultLimits);
		menu.add(allDefaults);
		menu.add(zoomIn);
		menu.add(zoomOut);
		menu.add(zoomInByMenu);
		menu.add(zoomOutByMenu);
		menu.add(setLimits);
		menu.addSeparator();
		menu.add(overviewMenuItem);
		menu.addSeparator();
		menu.add(enableHighPrecision);
		menu.add(enableSubpixelSampling);
		if (!isApplet) {
			menu.add(multiprocessingConfig);
		}
		menu.addSeparator();
		menu.add(help);
		return menu;
	}
	
	private void checkMandelbrotColor() {   // Synchronize selected item in MandelbrotColor menu with the color from the display
											// This is called in response to a property change event from the display.
		Color c = display.getMandelbrotColor();
		if (c.equals(Color.BLACK))
			mandelbrotColorMenuItems[0].setSelected(true);
		else if (c.equals(Color.GRAY))
			mandelbrotColorMenuItems[1].setSelected(true);
		else if (c.equals(Color.WHITE))
			mandelbrotColorMenuItems[2].setSelected(true);
		else if (c.equals(Color.BLUE))
			mandelbrotColorMenuItems[3].setSelected(true);
		else
			mandelbrotColorMenuItems[4].setSelected(true);
	}

	private JMenu makeIterationsMenu() {
		JMenu menu = new JMenu(I18n.tr("mandelbrotMenu.menuName.MaximumIterationCount"));
		ButtonGroup grp = new ButtonGroup();
		maxIterationMenuItems = new ArrayList<JRadioButtonMenuItem>();
		for (int m : maxIterationMenuValues)
			maxIterationMenuItems.add(new JRadioButtonMenuItem("" + m));
		maxIterationMenuItems.add(new JRadioButtonMenuItem(I18n.tr("menuCommand.Custom")));
		ActionListener lis = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Object src = evt.getSource();
				for (int i = 0; i < maxIterationMenuValues.length; i++) {
					if (src == maxIterationMenuItems.get(i)) {
						display.setMaxIterations(maxIterationMenuValues[i]);
						return;
					}
				}
				String s = JOptionPane.showInputDialog(display, I18n.tr("mandelbrotMenus.question.GetCustomMaxIterations"), 
						""+display.getMaxIterations());
				if (s == null)
					return;
				try {
					int n = Integer.parseInt(s);
					if (n < 2)
						throw new NumberFormatException();
					display.setMaxIterations(n);
				}
				catch (NumberFormatException e) {
					JOptionPane.showMessageDialog(display, I18n.tr("mandelbrotMenus.error.BadMaxIterationValue"));
				}
			}
		};
		for (JRadioButtonMenuItem item : maxIterationMenuItems) {
			grp.add(item);
			menu.add(item);
			item.addActionListener(lis);
		}
		checkMaxIterations(display.getMaxIterations());
		return menu;
	}
	
	private void checkMaxIterations(int maxIter) {    // Synchronize selected item in MaxIterations menu with the value from the display
													  // This is called in response to a property change event from the display.
		JRadioButtonMenuItem custom = maxIterationMenuItems.get(maxIterationMenuItems.size()-1);
		for (int i = 0; i < maxIterationMenuValues.length; i++) {
			if (maxIter == maxIterationMenuValues[i]) {
				maxIterationMenuItems.get(i).setSelected(true);
				custom.setText(I18n.tr("menuCommand.Custom"));
				return;
			}
		}
		custom.setText(I18n.tr("menuCommand.CustomWithCurrentValue", ""+maxIter));
		custom.setSelected(true);
	}
	
	private JMenu makeImageSizeMenu() {
		JMenu menu = new JMenu(I18n.tr("mandelbrotMenu.menuName.ImageSize"));
		imageSizeMenuItems = new ArrayList<JRadioButtonMenuItem>();
		final JRadioButtonMenuItem trackWindow = new JRadioButtonMenuItem(I18n.tr("mandelbrotMenu.command.ImageSizeMatchesWindowSize"));
		imageSizeMenuItems.add(trackWindow);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		boolean foundscreen = false;
		for (int i = 1; i < imageSizes.length; i++) {
			Dimension size = imageSizes[i];
			String name = I18n.tr("mandelbrotMenu.command.ImageSizeWithWidthAndHeight",""+size.width,""+size.height);
			if (size.equals(screenSize)) {
				name = "*" + name;
				foundscreen = true;
			}
			imageSizeMenuItems.add(new JRadioButtonMenuItem(name));
		}
		if (! foundscreen) {
			imageSizeMenuItems.add(new JRadioButtonMenuItem("*" + 
					I18n.tr("mandelbrotMenu.command.ImageSizeWithWidthAndHeight",""+screenSize.width,""+screenSize.height)));
			Dimension[] temp = new Dimension[imageSizes.length+1];
			System.arraycopy(imageSizes,0,temp,0,imageSizes.length);
			temp[temp.length-1] = screenSize;
			imageSizes = temp;
		}
		imageSizeMenuItems.add(new JRadioButtonMenuItem(I18n.tr("menuCommand.Custom")));
		ActionListener lis = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Object src = evt.getSource();
				for (int i = 0; i < imageSizeMenuItems.size() - 1; i++) {
					if (src == imageSizeMenuItems.get(i)) {
						display.setImageSize(imageSizes[i]);
						return;
					}
				}
				Dimension newSize = ImageSizeDialog.showDialog(display, display.getImageSize());
				if (newSize != null)
					display.setImageSize(newSize);
			}
		};
		ButtonGroup grp = new ButtonGroup();
		for (JRadioButtonMenuItem item : imageSizeMenuItems) {
			grp.add(item);
			item.addActionListener(lis);
			menu.add(item);
		}
		imageSizeMenuItems.get(0).setAccelerator(getAccelerator("EQUALS"));
		checkImageSize(display.getImageSize());
		return menu;
	}

	private void checkImageSize(Dimension size) {  // Synchronize selected item in ImageSize menu with the display
        										   // This is called in response to a property change event from the display.
		JRadioButtonMenuItem custom = imageSizeMenuItems.get(imageSizeMenuItems.size()-1);
		if (size == null) {
			imageSizeMenuItems.get(0).setSelected(true);
			custom.setText(I18n.tr("menuCommand.Custom"));
			return;
		}
		for (int i = 1; i < imageSizes.length; i++) {
			if (size.equals(imageSizes[i])) {
				imageSizeMenuItems.get(i).setSelected(true);
				custom.setText(I18n.tr("menuCommand.Custom"));
				return;
			}
		}
		custom.setText(I18n.tr("menuCommand.CustomWithCurrentValue",
				I18n.tr("mandelbrotMenu.command.ImageSizeWithWidthAndHeight",""+size.width,""+size.height)));
		custom.setSelected(true);
	}
	
	private JMenu makeToolsMenu() {
		JMenu menu = new JMenu(I18n.tr("mandelbrotMenu.menuName.Tools"));
		final JRadioButtonMenuItem zoomIn = new JRadioButtonMenuItem(I18n.tr("mandelbrotMenu.command.tool.ZoomIn"));
		final JRadioButtonMenuItem zoomOut = new JRadioButtonMenuItem(I18n.tr("mandelbrotMenu.command.tool.ZoomOut"));
		final JRadioButtonMenuItem drag = new JRadioButtonMenuItem(I18n.tr("mandelbrotMenu.command.tool.Drag"));
		final JRadioButtonMenuItem orbit = new JRadioButtonMenuItem(I18n.tr("mandelbrotMenu.command.tool.ShowOrbit"));
		final JRadioButtonMenuItem coords = new JRadioButtonMenuItem(I18n.tr("mandelbrotMenu.command.tool.ShowCoords"));
		final JRadioButtonMenuItem recenter = new JRadioButtonMenuItem(I18n.tr("mandelbrotMenu.command.tool.RecenterOnPoint"));
		final JMenuItem help = new JMenuItem(I18n.tr("mandelbrotMenu.command.ToolMenu.help"));
		toolsMenuItems = new JRadioButtonMenuItem[] {zoomIn, zoomOut, drag, orbit, coords, recenter };
		toolCodes = new int[] { MandelbrotDisplay.MOUSE_ACTION_ZOOM_IN, MandelbrotDisplay.MOUSE_ACTION_ZOOM_OUT, 
				MandelbrotDisplay.MOUSE_ACTION_DRAG, MandelbrotDisplay.MOUSE_ACTION_SHOW_ORBIT, 
				MandelbrotDisplay.MOUSE_ACTION_SHOW_COORDS, MandelbrotDisplay.MOUSE_ACTION_RECENTER_ON_POINT};
		ActionListener lis = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				Object src = evt.getSource();
				if (src == help) {
					JOptionPane.showMessageDialog(display, I18n.tr("mandelbrotMenu.ToolMenu.HelpText"));
					return;
				}
				for (int i = 0; i < toolsMenuItems.length; i++) {
					if (src == toolsMenuItems[i]) {
						display.setDefaultMouseAction(toolCodes[i]);
						return;
					}
				}
			}
		};
		ButtonGroup grp = new ButtonGroup();
		for (JRadioButtonMenuItem item : toolsMenuItems) {
			menu.add(item);
			grp.add(item);
			item.addActionListener(lis);
		}
		checkTool();
		menu.addSeparator();
		help.addActionListener(lis);
		zoomIn.setAccelerator(getAccelerator("M"));
		menu.add(help);
		return menu;
	}
	
	private void checkTool() {  // Synchronize selected item in Tools menu with selected tool in the display
		                        // This is called in response to a property change event from the display.
		int tool = display.getDefaultMouseAction();
		for (int i = 0; i < toolCodes.length; i++) {
			if (tool == toolCodes[i]) {
				toolsMenuItems[i].setSelected(true);
				return;
			}
		}
	}
	
	private JMenu makeExamplesMenu() {
		JMenu menu = new JMenu(I18n.tr("mandelbrotMenu.menuName.Examples"));
		ClassLoader cl = getClass().getClassLoader();
		ActionListener lis = new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				String file = evt.getActionCommand();
				try {
					ClassLoader cl = getClass().getClassLoader();
					URL url = cl.getResource("edu/hws/eck/umb/resources/examples/" + file);
					DocumentBuilder docReader  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					Document xmldoc = docReader.parse(url.openStream());
					MandelbrotSettings settings = MandelbrotSettings.createFromXML(xmldoc.getDocumentElement());
					MandelbrotSettings oldVal = new MandelbrotSettings(display);
					addToUndoManagerIsSuspended = true;
					display.applySettings(settings);
					addToUndoManagerIsSuspended = false;
					undoManager.addEdit(new UndoableChange(ALL_SETTINGS, oldVal, settings));
					updateUndoRedoItems();
				}
				catch (Exception e) {
				}
			}
		};
		for (int i = 1; i <= 12; i++) {
			try {
				URL url = cl.getResource("edu/hws/eck/umb/resources/examples/Example" + i + ".xml_68x51.png");
				Image img = Toolkit.getDefaultToolkit().createImage(url);
				ImageIcon icon = new ImageIcon(img);
				JMenuItem item = new JMenuItem(icon);
				item.addActionListener(lis);
				item.setActionCommand("Example" + i + ".xml");
				menu.add(item);
			}
			catch (Exception e) {
			}
		}
		return menu;
	}
		
	private SimpleFileChooser getFileChooser() {
		if (fileChooser == null) {
			fileChooser = new SimpleFileChooser();
			String dirName = Util.getPref("fileio.defaultDirectory");
			if (dirName != null) 
				fileChooser.setDefaultDirectory(dirName);
		}
		return fileChooser;
	}

	private void saveDirectoryPref() {
		String dir = fileChooser.getCurrentDirectory();
		Util.setPref("fileio.defaultDirectory", dir);
	}
	
	private void doSave() {  // Save a settings file.
		SimpleFileChooser chooser = getFileChooser();
		File outputFile = chooser.getOutputFile(display,I18n.tr("mandelbrotMenus.saveDialog.title"),I18n.tr("mandelbrotMenus.saveDialog.defaultFileName"));
		if (outputFile == null)
			return;
		try {
			PrintWriter out = new PrintWriter(outputFile);
			out.print("<?xml version='1.0'?>\n");
			out.print(new MandelbrotSettings(display).toXML());
			out.flush();
			out.close();
			if (out.checkError())
				throw new Exception(I18n.tr("mandelbrotMenus.saveDialog.error.genericWriteError"));
			setStatusBarText(I18n.tr("statusBar.text.SettingsSavedToFile", outputFile.getAbsolutePath()), 3);
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(display, I18n.tr("mandelbrotMenus.saveDialog.error.cantWriteFile", outputFile.getName(), e.getMessage()));
			return;
		}
		saveDirectoryPref();
	}

	private void doSaveImage(String format) { // Save an image; format is PNG or JPEG
		SimpleFileChooser chooser = getFileChooser();
		String defaultFileName = I18n.tr("mandelbrotMenus.saveImageDialog.defaultFileNameWithoutExtension");
		if (format.equals("PNG"))
			defaultFileName += ".png";
		else
			defaultFileName += ".jpeg";
		String title = format.equals("PNG") ? I18n.tr("mandelbrotMenus.savePNGImageDialog.title") : 
				I18n.tr("mandelbrotMenus.saveJPEGImageDialog.title");
		File outputFile = chooser.getOutputFile(display,title,defaultFileName);
		if (outputFile == null)
			return;
		try {
			if ( ! display.writeImage(outputFile, format) ) 
				throw new IOException(I18n.tr("mandelbrotMenus.saveImageDialog.ImageFormatNotSupported",format));
			setStatusBarText(I18n.tr("statusBar.text.ImageSavedToFile", outputFile.getAbsolutePath()), 3);
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(display, I18n.tr("mandelbrotMenus.saveDialog.error.cantWriteFile", outputFile.getName(), e.getMessage()));
			return;
		}
		saveDirectoryPref();
	}

	private MandelbrotSettings doOpen() {  // Open a settings file
		SimpleFileChooser chooser = getFileChooser();
		File inputFile = chooser.getInputFile(display, I18n.tr("mandelbrotMenus.openDialog.title"));
		if (inputFile == null)
			return null;
		Document xmldoc;
		try {
			DocumentBuilder docReader  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			xmldoc = docReader.parse(inputFile);
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(display, I18n.tr("mandelbrotMenus.openDialog.error.cantReadFile", inputFile.getName(), e.getMessage()));
			return null;
		}
		catch (SAXException e) {
			JOptionPane.showMessageDialog(display, I18n.tr("mandelbrotMenus.openDialog.error.fileIsNotXML", inputFile.getName()));
			return null;
		} 
		catch (ParserConfigurationException e) {
			JOptionPane.showMessageDialog(display, I18n.tr("mandelbrotMenus.openDialog.error.cantReadFile", inputFile.getName(), e.getMessage()));
			return null;
		}
		try {
			MandelbrotSettings settings = MandelbrotSettings.createFromXML(xmldoc.getDocumentElement());
			saveDirectoryPref();
			return settings;
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(display, I18n.tr("mandelbrotMenus.openDialog.error.fileIsNotMandelbrotSettngs", inputFile.getName(), e.getMessage()));
			return null;
		}
	}
	
	private void updateUndoRedoItems() {  // Called whenever the Undo/Redo status changes
		undoMenuItem.setText(undoManager.getUndoPresentationName());
		redoMenuItem.setText(undoManager.getRedoPresentationName());
		undoMenuItem.setEnabled(undoManager.canUndo());
		redoMenuItem.setEnabled(undoManager.canRedo());
	}
	
	private class UndoableChange extends AbstractUndoableEdit { // An item for the Undo and Redo commands
		
		private String propertyName;
		private Object oldValue;
		private Object newValue;
		
		UndoableChange(String propertyName, Object oldValue, Object newValue) {
			this.propertyName = propertyName;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}
		
		public String getPresentationName() {
			if (propertyName.equals(ALL_SETTINGS))
				return I18n.tr("mandelbrotMenus.nameForEditAction.ChangeAllSettings");
			else if (propertyName.equals(MandelbrotDisplay.PROPERTY_REQUESTED_IMAGE_SIZE))
				return I18n.tr("mandelbrotMenus.nameForEditAction.ChangeImageSize");
			else if (propertyName.equals(MandelbrotDisplay.PROPERTY_LIMITS))
				return I18n.tr("mandelbrotMenus.nameForEditAction.ChangeLimits");
			else if (propertyName.equals(MandelbrotDisplay.PROPERTY_MANDLELBROT_COLOR))
				return I18n.tr("mandelbrotMenus.nameForEditAction.ChangeMandelbrotColor");
			else if (propertyName.equals(MandelbrotDisplay.PROPERTY_MAX_ITERATIONS))
				return I18n.tr("mandelbrotMenus.nameForEditAction.ChangeMaxIterations");
			else if (propertyName.equals(MandelbrotDisplay.PROPERTY_PALETTE))
				return I18n.tr("mandelbrotMenus.nameForEditAction.ModifyPalette");
			else
				return null;
		}

		public void undo() {
			super.undo();
			apply(true);
		}
		
		public void redo() {
			super.redo();
			apply(false);
		}
		
		private void apply(boolean undo) {
			Object nextVal = undo ? oldValue : newValue;
			if (propertyName.equals(ALL_SETTINGS))
				display.applySettings((MandelbrotSettings)nextVal);
			else if (propertyName.equals(MandelbrotDisplay.PROPERTY_REQUESTED_IMAGE_SIZE))
				display.setImageSize((Dimension)nextVal);
			else if (propertyName.equals(MandelbrotDisplay.PROPERTY_LIMITS))
				display.setLimits((BigDecimal[])nextVal);
			else if (propertyName.equals(MandelbrotDisplay.PROPERTY_MANDLELBROT_COLOR))
				display.setMandelbrotColor((Color)nextVal);
			else if (propertyName.equals(MandelbrotDisplay.PROPERTY_MAX_ITERATIONS))
				display.setMaxIterations((Integer)nextVal);
			else if (propertyName.equals(MandelbrotDisplay.PROPERTY_PALETTE)) {
				MandelbrotDisplay.PaletteInfo info = (MandelbrotDisplay.PaletteInfo)nextVal;
				display.setPaletteInfo(info.palette, info.paletteMapping);
			}
		}

	}

	
}
