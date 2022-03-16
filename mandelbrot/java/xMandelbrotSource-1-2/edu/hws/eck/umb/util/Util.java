package edu.hws.eck.umb.util;

import java.util.prefs.Preferences;

import javax.swing.KeyStroke;

/**
 * Provides a few basic utilities for the Mandelbrot program, notably for
 * working with Preferences.
 *
 */
public class Util {
	
	private final static String PREFS_PREFIX = "/edu/hws/edu/umb";
	private static char isMacOS = '?';
	
	/**
	 * Tells whether the operating system in Mac OS.  This is checked using
	 * Apple's recommended method, that is, checking for the existence of
	 * a system file named "mrj.version".
	 */
	public static boolean isMacOS() {
		if (isMacOS == '?') {
			try {
				String macTest = System.getProperty("mrj.version");
				if (macTest != null)
					isMacOS = 'Y';
				else
					isMacOS = 'N';
			}
			catch (Exception e) { // System.getProperty can throw a security exception
				isMacOS = 'N';
			}
		}
		return isMacOS == 'Y';
	}
	
	/**
	 * Builds a KeyStroke from a String description, for use as an accelerator
	 * key for a menu command.
	 * @param description describes the keystroke.  This description will be
	 * prefaced by "control " (on most computers) or "meta " on Mac OS, in order
	 * to use the proper command key for the operating system.  Other than that,
	 * the descriptions follow the format required for KeyStroke.getKeyStroke(String).
	 */
	public static KeyStroke getAccelerator(String description) {
		String commandKey;
		if (isMacOS())
			commandKey = "meta ";
		else
			commandKey = "control ";
		return KeyStroke.getKeyStroke(commandKey + description);
	}
	
	/**
	 * Calls getPref(prefName,null)
	 */
	public static String getPref(String prefName) {
		return getPref(prefName, null);
	}

	/**
	 * Retrieves a preference stored in the Java preferences in the user's
	 * home directory.  This uses a preference node name of "/edu/hws/edu/umb".
	 * @param prefName The key under which the preference value is stored.
	 * @param defaultValue Default return value, if the given key value does
	 * not exist, or if it is not possible to access the preferences (such as
	 * in an applet).
	 */
	public static String getPref(String prefName, String defaultValue) {
		try {
			Preferences root = Preferences.userRoot();
			Preferences node = root.node(PREFS_PREFIX);
			return node.get(prefName, defaultValue);
		}
		catch (Exception e) {
			return defaultValue;
		}
	}
	
	/**
	 * Set a preference, stored in the Java Preferences in the user's home
	 * directory.  Uses a preference node name of "/edu/hws/edu/umb". If it
	 * is not possible to save the preference, no exception is thrown; the
	 * preference is simply not saved.
	 * @param prefName The key under which the preference is to be stored.
	 * @param value The preference value to be associated with the given key.
	 */
	public static void setPref(String prefName, String value) {
		try {
			Preferences root = Preferences.userRoot();
			Preferences node = root.node(PREFS_PREFIX);
			if (value == null)
				node.remove(prefName);
			else
				node.put(prefName, value);
		}
		catch (Exception e) {
		}
	}

}
