package edu.hws.eck.umb.palette;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.hws.eck.umb.util.I18n;
import edu.hws.eck.umb.util.SimpleFileChooser;
import edu.hws.eck.umb.util.Util;

/**
 * Utilities for saving palettes to files and for reading those files.
 * Also, for coding and decoding the XML representation of palettes
 * that is used in those files.
 */
public class PaletteIO {
	
	private static SimpleFileChooser fileChooser;
	
	/**
	 * Create an XML representation for a palette (does NOT include the standard
	 * first line, &lt;?xml&nbps;vesion='1.0'&gt;, of an XML file).
	 */
	public static String paletteToXML(Palette palette) {
		StringBuffer b = new StringBuffer();
		b.append("<palette colorType=");
		b.append( palette.getColorType() == Palette.COLOR_TYPE_HSB ? "'HSB'" : "'RGB'");
		b.append(">\n");
		if (!palette.getMirrorOutOfRangeComponents())
			b.append("   <mirrorOutOfRangeComponents value='" + palette.getMirrorOutOfRangeComponents() + "'/>\n");
		int ct = palette.getDivisionPointCount();
		for (int i = 0; i < ct; i++) {
			b.append("   <divisionPoint");
			b.append(" position='" + palette.getDivisionPoint(i) + "'");
			b.append(" color='");
			float[] colorComp = palette.getDivisionPointColorComponents(i);
			b.append(colorComp[0]);
			b.append(';');
			b.append(colorComp[1]);
			b.append(';');
			b.append(colorComp[2]);
			b.append("'/>\n");
		}
		b.append("</palette>\n");
		return b.toString();
	}
	
	/**
	 * Given an XML Element that represents a palette, create the Palette object
	 * described by that Element. 
	 * @throws IOException if the Element is not a valid palette element.
	 */
	public static Palette xmlToPalette(Element paletteElement) throws IOException {
		String name = paletteElement.getNodeName();
		if (!name.equals("palette"))
			throw new IOException(I18n.tr("paletteIO.xmlToPalette.error.elementIsNotPalette"));
		String colorTypeString = paletteElement.getAttribute("colorType");
		int colorType;
		if (colorTypeString == null || colorTypeString.equalsIgnoreCase("HSB"))
			colorType = Palette.COLOR_TYPE_HSB;
		else if (colorTypeString.equalsIgnoreCase("RGB"))
			colorType = Palette.COLOR_TYPE_RGB;
		else
			throw new IOException(I18n.tr("paletteIO.xmlToPalette.error.illegalAtrributeValue", "palette", "colorType", colorTypeString));
		boolean mirrored = true;
		ArrayList<Double> divisionPoints = new ArrayList<Double>();
		ArrayList<float[]> colorComponents = new ArrayList<float[]>();
		NodeList children = paletteElement.getChildNodes();
		int ct = children.getLength();
		double previousPosition = -1;
		for (int i = 0; i < ct; i++) {
			Node node = children.item(i);
			if (! (node instanceof Element) )
				continue;
			Element element = (Element)node;
			String elementName = element.getNodeName();
			if (elementName.equals("mirrorOutOfRangeComponents")) {
				String valueString = element.getAttribute("value");
				if (valueString != null && valueString.equalsIgnoreCase("false"))
					mirrored = false;
				else if (valueString != null && !valueString.equalsIgnoreCase("true"))
					throw new IOException(I18n.tr("paletteIO.xmlToPalette.error.illegalAtrributeValue", valueString));
			}
			else if (elementName.equals("divisionPoint")) {
				String positionString = element.getAttribute("position");
				String colorString = element.getAttribute("color");
				if (positionString == null)
					throw new IOException(I18n.tr("paletteIO.xmlToPalette.error.missingAtrributeValue","divisionPoint","position"));
				if (colorString == null)
					throw new IOException(I18n.tr("paletteIO.xmlToPalette.error.missingAtrributeValue","divisionPoint","color"));
				double position;
				float[] color = new float[3];
				try {
					position = Double.parseDouble(positionString);
				}
				catch (NumberFormatException e) {
					throw new IOException(I18n.tr("","divisionPoint","position",positionString));
				}
				try {
					String[] colorStringArray = colorString.split("[;, ]+");
					color[0] = Float.parseFloat(colorStringArray[0]);
					color[1] = Float.parseFloat(colorStringArray[1]);
					color[2] = Float.parseFloat(colorStringArray[2]);
				}
				catch (Exception e) {
					throw new IOException(I18n.tr("","divisionPoint","color",colorString));
				}
				if (previousPosition == -1 && position != 0)
					throw new IOException(I18n.tr("paletteIO.xmlToPalette.error.firstPositionMustBeZero"));
				if (position <= previousPosition)
					throw new IOException(I18n.tr("paletteIO.xmlToPalette.error.positionsNotInIncreasingOrder"));
				divisionPoints.add(position);
				colorComponents.add(color);
				previousPosition = position;
			}
		}
		if (previousPosition != 1)
			throw new IOException(I18n.tr("paletteIO.xmlToPalette.error.lastPositionMustBeOne"));
		return new Palette(colorType, mirrored, divisionPoints, colorComponents);
	}
	
	/**
	 * Lets the user select a file and tries to read a palette from that file.
	 * @param parent a component to act as parent to the open file dialog
	 * @return the palette from the file selected by the user, or null if the user cancels
	 * the dialog or if an error occurs while trying to read the file.  (The user has already
	 * been notified of the error when this method returns.)
	 */
	public static Palette doOpen(Component parent) {
		SimpleFileChooser chooser = getSimpleFileChooser();
		File inputFile = chooser.getInputFile(parent, I18n.tr("paletteIO.openDialog.title"));
		if (inputFile == null)
			return null;
		Document xmldoc;
		try {
			DocumentBuilder docReader  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			xmldoc = docReader.parse(inputFile);
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(parent, I18n.tr("paletteIO.openDialog.error.cantReadFile", inputFile.getName(), e.getMessage()));
			return null;
		}
		catch (SAXException e) {
			JOptionPane.showMessageDialog(parent, I18n.tr("paletteIO.openDialog.error.fileIsNotXML", inputFile.getName()));
			return null;
		} 
		catch (ParserConfigurationException e) {
			JOptionPane.showMessageDialog(parent, I18n.tr("paletteIO.openDialog.error.cantReadFile", inputFile.getName(), e.getMessage()));
			return null;
		}
		try {
			Palette palette = xmlToPalette(xmldoc.getDocumentElement());
			saveDirectoryPref();
			return palette;
		}
		catch (IOException e) {
			JOptionPane.showMessageDialog(parent, I18n.tr("paletteIO.openDialog.error.fileIsNotPalette", inputFile.getName(), e.getMessage()));
			return null;
		}
	}
	
	/**
	 * Save a palette to a file specified by the user
	 * @param parent a component to act as parent for the save file dialog
	 * @param palette the palette to be saved to the file; must be non-null
	 * @return true if the palette is sucessfully save; false if the user cancels the save file dialog
	 * of if an error occurs while trying to write the file.  (The user has already been notifed, if an
	 * error occurs.)
	 */
	public static boolean doSave(Component parent, Palette palette) {
		SimpleFileChooser chooser = getSimpleFileChooser();
		File outputFile = chooser.getOutputFile(parent,I18n.tr("paletteIO.saveDialog.title"),I18n.tr("paletteIO.saveDialog.defaultFileName"));
		if (outputFile == null)
			return false;
		try {
			PrintWriter out = new PrintWriter(outputFile);
			out.print("<?xml version='1.0'?>\n");
			out.print(paletteToXML(palette));
			out.flush();
			out.close();
			if (out.checkError())
				throw new Exception(I18n.tr("paletteIO.saveDialog.error.genericWriteError"));
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(parent, I18n.tr("paletteIO.saveDialog.error.cantWriteFile", outputFile.getName(), e.getMessage()));
			return false;
		}
		saveDirectoryPref();
		return true;
	}
		
	private static SimpleFileChooser getSimpleFileChooser() {
		if (fileChooser == null) {
			fileChooser = new SimpleFileChooser();
			String dirName = Util.getPref("palette.defaultDirectory");
			if (dirName != null) 
				fileChooser.setDefaultDirectory(dirName);
		}
		return fileChooser;
	}
	
	private static void saveDirectoryPref() {
		String dir = fileChooser.getCurrentDirectory();
		Util.setPref("palette.defaultDirectory", dir);
	}

}
