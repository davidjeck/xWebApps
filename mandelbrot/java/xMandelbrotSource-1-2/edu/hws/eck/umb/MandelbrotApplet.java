package edu.hws.eck.umb;

import java.net.URL;

import javax.swing.JApplet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

/**
 * Shows a Mandelbrot Viewer as an applet.  The applet takes an XML
 * settings file as an optional parameter, with parameter name "settings".
 * If the param exists and is valid, then the applet will load
 * the settings at startup.  The settings is specified as a file
 * relative to the document base (the URL of the web page on
 * which the applet appears).  Note that any image size specification
 * in the settings file is ignore; the image size will match the
 * actual area available in the applet.  Also, high precision is
 * enabled, regardless of what it says in the settings file.
 */
public class MandelbrotApplet extends JApplet {
	
	// Updated June 2011 to implement the "settings" param.
	
	public void init() {
		MandelbrotPanel panel = new MandelbrotPanel();
		MandelbrotDisplay display = panel.getDisplay();
		String settingsFile = getParameter("settings");
		if (settingsFile != null) {
			try {
				URL settingsLoc = new URL(getDocumentBase(), settingsFile);
				DocumentBuilder docReader  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document xmldoc = docReader.parse(settingsLoc.openStream());
				MandelbrotSettings settings = MandelbrotSettings.createFromXML(xmldoc.getDocumentElement());
				settings.setImageSize(null); // image size will match size if applet].
				settings.setHighPrecisionEnabled(true);  // high precision computationn is enabled.
				display.applySettings(settings);
			}
			catch (Exception e) {
			}
		}
		setContentPane(panel);
		setJMenuBar(new MandelbrotMenus(display,null,panel.getStatusBar(),true));
	}
	
}
