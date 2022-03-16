package edu.hws.eck.umb.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * This class implements internationalization by providing facilities for getting strings from
 * properties files.  A single default property file is used, but other files can be added.
 * All files that have been added are searched in the reverse of the order in which they 
 * were added.  The default file is searched last.  This class does not generate errors;
 * if a property file cannot be found for a specified resource name, it is ignored.  If no
 * value can be found for a specified key, then either null or the key itself is returned rather than 
 * generating an error, depending on which method was called.
 * <p>The default properties file name is "strings.properties" in the directory vmm/resources.
 * Property files for other locales should be given names such as, for example, strings_fr.properties, '
 * and stored in the same location.
 */
public class I18n {
	
	private static ArrayList<ResourceBundle> bundles = new ArrayList<ResourceBundle>();  // resource bundles that have been loaded
	private static ArrayList<String> bundleNames = new ArrayList<String>();  // The file names for the resource bundles
	private static Locale locale;
	private static String defaultPropertiesFileName = "edu.hws.eck.umb.resources.strings";
	
	/**
	 * Sets a locale to use when searching for property files.  This does not affect
	 * any property files that have already been loaded.  If the locale is null (which is the
	 * default), then the default locale will be used -- which is almost always what you want to do!
	 * Property files that have already been loaded are reloaded.  (This method just calls
	 * "setLocale(locale,true)".)  It is not usually necessary to call this method. It could
	 * be used, for example, to give the user a choice of languages.
	 */
	public static void setLocale(Locale locale) {
		setLocale(locale,true);
	}
	
	/**
	 * Sets a locale to use when searching for property files.  If the <code>reload</code> argument is
	 * false, then this will not affect property files that have already been loaded; if <code>reload</code>
	 * is true, then all property files that have already been loaded are discarded and new ones
	 * are loaded using the new locale.
	 * If the locale is null (which is the default), then the default locale will be used -- 
	 * which is almost always what you want to do!  It is not usually necessary to call this method. It could
	 * be used, for example, to give the user a choice of languages.
	 */
	public synchronized static void setLocale(Locale locale, boolean reload) {
		I18n.locale = locale;
		if (reload) {
			bundles.clear();
			ArrayList<String> names = bundleNames;
			bundleNames = new ArrayList<String>();
			for (int i = 0; i < names.size(); i++)
				load(names.get(i));
		}
	}
	
	/**
	 * Returns the locale that is currently being used by this class.
	 * @return the current locale for internationalization.  The return value is non-null.
	 * If no non-null locale has been set by {@link #setLocale(Locale)}, then the default
	 * locale for the Java Virtual Machine is returned.
	 */
	public static Locale getLocale() {
		if (locale == null)
			return Locale.getDefault();
		else
			return locale;
	}
	
	/**
	 * Adds a file to the list of property files that are searched when looking
	 * for a string.  Property files are searched in the reverse of the order
	 * in which they are added.  That is, if a file contains a key that also
	 * occurred in a previously added file, the value in the new file will hide
	 * the value in the old file.
	 * @param fileName The resource name for the property file to be added.  The bundle is
	 * obtained using the <code>getBundle</code> method in the <code>ResourceBundle</code> class.
	 * If the file name is null, nothing is done.  If the file name is a duplicate of
	 * a name that was used previously, the bundle is NOT added to the search path for
	 * a second time.  Note that the file name should be given as a resource name, such
	 * as "edu.hws.eck.umb.strings", that can be used to locate the resource, rather than as an actual file name.
	 * @return The return value indicates whether a resource bundle was found.  If the return
	 * value is false, it means that no property file with the specified resource name was
	 * found.
	 */
	public synchronized static boolean addFile(String fileName) {
		if (fileName == null)
			return false;
		for (int i = 0; i < bundleNames.size(); i++)
			if (bundleNames.get(i).equals(fileName))
				return bundles.get(i) != null;
		if (bundles.size() == 0 && !fileName.equals(defaultPropertiesFileName))
			load(defaultPropertiesFileName);
		ResourceBundle b = load(fileName);
		return b != null;
	}
	
	/**
	 * Loads a property file with a specified resource name and returns the resulting
	 * resource bundle.  Returns null if no property file can be found.
	 */
	private synchronized static ResourceBundle load(String name) {
		if (name == null)
			return null;
		ResourceBundle bundle;
		try {
			if (locale == null)
				bundle = ResourceBundle.getBundle(name);
			else
				bundle = ResourceBundle.getBundle(name,locale);
		}
		catch (MissingResourceException e) {
			bundle = null;
		}
		bundles.add(bundle);
		bundleNames.add(name);
		return bundle;
	}
	
	/**
	 * Looks up a key in the resource bundles and returns the associated localized string.
	 * @return The value associated with the key.  If the key does not occur in any of the
	 * loaded resource bundles (or if key is null), then null  is returned.
	 */
	private static String lookup(String key) {
		if (key == null)
			return null;
		if (bundles.size() == 0)
			load(defaultPropertiesFileName);
		for (int i = bundles.size() - 1; i >= 0; i--) {
			ResourceBundle b = bundles.get(i);
			if (b != null) {
				try {
					return b.getString(key);
				}
				catch (MissingResourceException e) {
				}
			}
		}
		return null;
	}
	
	/**
	 * A convenience method that calls MessageFormat.format(str, arg...), where str is
	 * the localized string for a given key.
	 * @param key String to be translated, by looking it up in resource bundles.  The translation can contain
	 * substrings of the form {0}, {1}, {2}...
	 * @param arg The strings (or other objects) that are to be substituted for {0}, {1}, {2}, ...
	 * @return The translated string, with any occurrence of {0}, {1}, {2}... replaced by the arg's.   
	 * If no translated string is found, then the substitution is done on the key string.
	 */
	public static String tr(String key, Object... arg) {
		String s = lookup(key);
		if (s == null)
			s = key;
		if(arg == null || arg.length == 0)
			return s;
		return MessageFormat.format(s, arg);
	}
	
	
	/**
	 * Does the same thing as I18n.tr(key,arg...), except that if no value is found for the key,
	 * the return value is null.
	 * @see #tr(String, Object[])
	 */
	public static String trIfFound(String key, Object... arg) {
		String s = lookup(key);
		if (s == null)
			return null;
		if (arg == null || arg.length == 0)
			return s;
		return MessageFormat.format(s, arg);
	}
	

}
