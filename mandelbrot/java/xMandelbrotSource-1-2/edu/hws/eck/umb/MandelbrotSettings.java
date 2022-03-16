package edu.hws.eck.umb;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.math.BigDecimal;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.hws.eck.umb.palette.Palette;
import edu.hws.eck.umb.palette.PaletteIO;
import edu.hws.eck.umb.palette.PaletteMapping;
import edu.hws.eck.umb.util.I18n;

/**
 * A MandelbrotSettings object holds all the information to describe a
 * MandelbrotImage, including the x/y limits, the Palette and PaletteMapping,
 * the maxIteration count, the Mandelbrot set color, the image size. and
 * whether high precision computation is enabled.
 */
public class MandelbrotSettings {
	
	private BigDecimal[] limits;
	private Palette palette;
	private Color mandelbrotColor;
	private PaletteMapping paletteMapping;
	private int maxIterations;
	private boolean highPrecisionEnabled;
	private Dimension imageSize;
	
	/**
	 * Create a MandelbrotSettings with all default values.
	 */
	public MandelbrotSettings() {
		limits = new BigDecimal[] { new BigDecimal(-2.333), new BigDecimal(1), new BigDecimal(-1.25), new BigDecimal(1.25) };
		palette = new Palette();
		mandelbrotColor = Color.BLACK;
		paletteMapping = new PaletteMapping();
		maxIterations = 100;
		highPrecisionEnabled = true;
		imageSize = null;
	}
	
	/**
	 * Create a MandelbrotSettings with values copied from the current settings
	 * of a MandelbrotDisplay.
	 */
	public MandelbrotSettings(MandelbrotDisplay display) {
		limits = display.getLimitsRequested();
		maxIterations = display.getMaxIterations();
		palette = display.getCopyOfPalette();
		mandelbrotColor = display.getMandelbrotColor();
		paletteMapping = new PaletteMapping(display.getPaletteLength(), display.getPaletteOffset());
		highPrecisionEnabled = display.getHighPrecisionEnabled();
		imageSize = display.getImageSize();
	}

	/**
	 * Get the x/y limits, as an array of four BigDecimal numbers in the order xmin, xmax, ymin, ymax.
	 */
	public BigDecimal[] getLimits() {
		return limits;
	}

	public void setLimits(BigDecimal[] limits) {
		this.limits = limits;
	}

	public Palette getPalette() {
		return palette;
	}

	public void setPalette(Palette palette) {
		this.palette = palette;
	}

	public Color getMandelbrotColor() {
		return mandelbrotColor;
	}

	public void setMandelbrotColor(Color mandelbrotColor) {
		this.mandelbrotColor = mandelbrotColor;
	}

	public PaletteMapping getPaletteMapping() {
		return paletteMapping;
	}

	public void setPaletteMapping(PaletteMapping paletteMapping) {
		this.paletteMapping = paletteMapping;
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public boolean isHighPrecisionEnabled() {
		return highPrecisionEnabled;
	}

	public void setHighPrecisionEnabled(boolean highPrecisionEnabled) {
		this.highPrecisionEnabled = highPrecisionEnabled;
	}

	/**
	 * Get the image size.  A null value means that the image size matches
	 * the window size.
	 */
	public Dimension getImageSize() {
		return imageSize;
	}

	/**
	 * @param imageSize the size of the image, or null to indicate that the 
	 * image size should be set to match the window size.
	 */
	public void setImageSize(Dimension imageSize) {
		this.imageSize = imageSize;
	}
	
	/**
	 * Return a String containing an XML representation of this MandelbrotSettings
	 * object (without the first line of an XML file, &lt;?xml&nbsp;version='1.0'>).
	 * When a settings file is save, this method provides the XML that is sent to the
	 * file.
	 */
	public String toXML() {
		StringBuffer b = new StringBuffer();
		b.append("<mandelbrot_settings_2>\n");
		if (imageSize != null)
			b.append("<image_size width='" + imageSize.width + "' height='" + imageSize.height + "'/>\n");
		b.append("<limits>\n");
		b.append("   <xmin>" + limits[0] + "</xmin>\n");
		b.append("   <xmax>" + limits[1] + "</xmax>\n");
		b.append("   <ymin>" + limits[2] + "</ymin>\n");
		b.append("   <ymax>" + limits[3] + "</ymax>\n");
		b.append("</limits>\n");
		b.append(PaletteIO.paletteToXML(palette));
		b.append("<mandelbrot_color r='" + mandelbrotColor.getRed() + "' g='" +
				mandelbrotColor.getGreen() + "' b='" + mandelbrotColor.getBlue() + "'/>\n");
		b.append("<palette_mapping length='" + paletteMapping.getLength() + 
				"' offset='" + paletteMapping.getOffset() + "'/>\n");
		b.append("<max_iterations value='" + maxIterations + "'/>\n");
		b.append("<high_precision_enabled value='" + highPrecisionEnabled + "'/>\n");
		b.append("</mandelbrot_settings_2>\n");
		return b.toString();
	}
	
	/**
	 * Reconstruct a MandelbrotSettings object from its XML representation, given
	 * as an XML Element.
	 * @return the MandelbrotSettings object defined by the given Element
	 * @throws IOException if the Element does not represent a valid MandelbrotSettings object
	 */
	public static MandelbrotSettings createFromXML(Element settingsElement) throws IOException {
		MandelbrotSettings settings = new MandelbrotSettings();
		if (!settingsElement.getNodeName().equals("mandelbrot_settings_2"))
			throw new IOException(I18n.tr("mandelbrotSettings.xmlError.NotASettingsElement"));
		NodeList children = settingsElement.getChildNodes();
		int ct = children.getLength();
		for (int i = 0; i < ct; i++) {
			Node node = children.item(i);
			if ( !(node instanceof Element) )
				continue;
			Element element = (Element)node;
			String name = element.getNodeName();
			if (name.equals("palette")) {
				settings.palette = PaletteIO.xmlToPalette(element);
			}
			else if (name.equals("palette_mapping")) {
				int length = attributeToInt(element,"length");
				if (length < 0)
					throw new IOException(I18n.tr("mandelbrotSettings.xmlError.attributeCantBeNegative","palette_mapping","length"));
				int offset = attributeToInt(element,"offset");
				if (length < 0)
					throw new IOException(I18n.tr("mandelbrotSettings.xmlError.attributeCantBeNegative","palette_mapping","offset"));
				settings.paletteMapping = new PaletteMapping(length,offset);
			}
			else if (name.equals("mandelbrot_color")) {
				int r = attributeToInt(element,"r");
				int g = attributeToInt(element,"g");
				int b = attributeToInt(element,"b");
				if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
					throw new IOException(I18n.tr("mandelbrotSettings.xmlError.badColorComponent"));
				settings.mandelbrotColor = new Color(r,g,b);
			}
			else if (name.equals("max_iterations")) {
				int max = attributeToInt(element,"value");
				if (max <= 0)
					throw new IOException(I18n.tr("mandelbrotSettings.xmlError.attributemustBePositive",element.getNodeName(),"value"));
				settings.maxIterations = max;
			}
			else if (name.equals("high_precision_enabled")) {
				String b = element.getAttribute("value");
				if (b.equalsIgnoreCase("true"))
					settings.highPrecisionEnabled = true;
				else if (b.equalsIgnoreCase("false"))
					settings.highPrecisionEnabled = false;
				else
					throw new IOException(I18n.tr("mandelbrotSettings.xmlError.attributemustBeBoolean",element.getNodeName(),"value"));
			}
			else if (name.equals("image_size")) {
				int width = attributeToInt(element, "width");
				if (width <= 0)
					throw new IOException(I18n.tr("mandelbrotSettings.xmlError.attributemustBePositive",element.getNodeName(),"width"));
				int height = attributeToInt(element, "height");
				if (height <= 0)
					throw new IOException(I18n.tr("mandelbrotSettings.xmlError.attributemustBePositive",element.getNodeName(),"height"));
				settings.imageSize = new Dimension(width,height);
			}
			else if (name.equals("limits")) {
				BigDecimal x1 = getLimitFromLimitsElement(element,"xmin");
				BigDecimal x2 = getLimitFromLimitsElement(element,"xmax");
				BigDecimal y1 = getLimitFromLimitsElement(element,"ymin");
				BigDecimal y2 = getLimitFromLimitsElement(element,"ymax");
				if (x2.compareTo(x1) <= 0 || y2.compareTo(y1) <= 0)
					throw new IOException(I18n.tr("mandelbrotSettings.xmlError.LimitsOutOfOrder"));
				settings.limits = new BigDecimal[] { x1, x2, y1, y2 };
			}
		}
		return settings;
	}
	
	private static int attributeToInt(Element element, String attributeName) throws IOException {
		String a = element.getAttribute(attributeName);
		if (a == null || a.length() == 0)
			throw new IOException(I18n.tr("mandelbrotSettings.xmlError.missingAttribute", element.getNodeName(), attributeName));
		try {
			return Integer.parseInt(a);
		}
		catch (NumberFormatException e) {
			throw new IOException(I18n.tr("mandelbrotSettings.xmlError.badAttributeValue", element.getNodeName(), attributeName));
		}
	}
	
	private static BigDecimal getLimitFromLimitsElement(Element limitsElement, String itemName) throws IOException {
		NodeList nodes = limitsElement.getElementsByTagName(itemName);
		if (nodes.getLength() == 0)
			throw new IOException(I18n.tr("mandelbrotSettings.xmlError.missingValueInLimits", itemName));
		if (nodes.getLength() > 1)
			throw new IOException(I18n.tr("mandelbrotSettings.xmlError.extraValueInLimits", itemName));
		String d = ((Element)nodes.item(0)).getTextContent();
		try {
			return new BigDecimal(d);
		}
		catch (NumberFormatException e) {
			throw new IOException(I18n.tr("mandelbrotSettings.xmlError.badValueInLimits", itemName));
		}
	}

}
