package edu.hws.eck.umb.comp;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import edu.hws.eck.umb.MandelbrotSettings;

/**
 * A command-line utility that can read Mandelbrot settings files and create the
 * images that they describe.  The images are saved to files.  The name of an output
 * image file is the same as the name of the input settings file, with the image
 * format added as a file extension (png by default).  If a file of the same name
 * already exists, it will be replaced, without any warning or notice!
 * <p>The input file names are specified on the command lines.
 * Several command-line options are supported.  An option applies to all the files
 * that come after the option on the command line.  The options are:
 * <ul>
 * <li>-size WWWxHHH --- where WWW and HHH are positive integers, specifies the size
 * of the image.  If no size is specified, 800x600 is used.  Any image size specified in
 * a settings file is ignored.
 * <li>-format XXX -- use XXX as the format for the image.  PNG is the default.
 * JPEG is also definitely supported.  Other formats might be supported on some systems.
 * <li>-onepass --- turn subpixel sampling off.  The default is to do a second pass for
 * subpixel sampling.  This is default.
 * <li>-twopass -- turn subpixel sampling on, which is the default anyway.  This option is
 * only useful if you have use "-onepass" earlier on the command line.
 * <li>-net XXX --- add one or more network workers.  The format for XXX is a list of one or more hosts,
 * separated by commas.  Each host can be specified as a host name or IP address optionally followed
 * by a colon and a port number.  The port number is only necessary if different from the default, 17071.
 * A copy the the Mandelbrot Network Server should already be running on each of the specified computers.
 * </ul>
 */
public class MandelbrotCL {

	private static final int HP_CUTOFF_EXP = 15;
	private static final double HP_CUTOFF = 1e-15;
	private static final BigDecimal TWO = new BigDecimal("2");
	private static final BigDecimal TEN = new BigDecimal("10");

	private static BigDecimal xmin, xmax, ymin, ymax;

	public static void main(String[] args) {
		Dimension size = null;
		String format = "PNG";
		boolean subsampled = true;
		boolean fileDone = false;
		TaskManager taskManager = new TaskManager();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-onepass")) {
				subsampled = false;
				System.out.println("Second pass for subpixel sampling DISABLED.");
			}
			else if (args[i].equalsIgnoreCase("-twopass")) {
				subsampled = true;
				System.out.println("Second pass for subpixel sampling ENABLED.");
			}
			else if (args[i].equalsIgnoreCase("-size") || args[i].equalsIgnoreCase("-g")) {
				if (i == args.length-1)
					System.out.println("Missing value for " + args[i] + "; IGNORED.");
				else {
					i++;
					String g = args[i];
					String[] nums = g.split("x");
					if (nums.length < 2)
						nums = g.split("X");
					try {
						int w = Integer.parseInt(nums[0]);
						int h = Integer.parseInt(nums[1]);
						if (w <= 0 || h <= 0)
							throw new Exception();
						size = new Dimension(w,h);
						System.out.println("Image size set to " + w + "x" + h + ".");
					}
					catch (Exception e) {
						System.out.println("Bad value for " + args[i] + "; IGNORED.");
					}
				}
			}
			else if (args[i].equalsIgnoreCase("-format")) {
				if (i == args.length-1)
					System.out.println("Missing value for -format; IGNORED.");
				else {
					i++;
					format = args[i];
					System.out.println("Image format set to " + format + ".");
				}
			}
			else if (args[i].equalsIgnoreCase("-net")) {
				if (i == args.length-1)
					System.out.println("Missing value for -net; IGNORED.");
				else {
					i++;
					taskManager.setNetworkingEnabled(true);
					System.out.println("Enabling network.");
					String[] netinfo = args[i].split(",");
					for (int j = 0; j < netinfo.length; j++) {
						String[] hostinfo = netinfo[j].split(":");
						int port;
						if (hostinfo.length == 1)
							port = MandelbrotNetworkTaskServer.DEFAULT_PORT;
						else {
							try {
								port = Integer.parseInt(hostinfo[1].trim());
							}
							catch (NumberFormatException e) {
								System.out.println("Illegal port number " + hostinfo[1]);
								continue;
							}
						}
						System.out.println("Adding network worker " + hostinfo[0] + ":" + port);
						taskManager.addNetworkWorker(hostinfo[0], port);
					}
				}
			}
			else {
				processFile(args[i],size,format,subsampled,taskManager);
				fileDone = true;
			}
		}
		if (!fileDone) {
			System.out.println("No files specified on command line!");
		}
	}

	private static void processFile(String fileName, Dimension size,
			String format, boolean subsampled, TaskManager taskManager) {
		File inputFile = new File(fileName);
		Document xmldoc;
		MandelbrotSettings settings;
		System.out.println();
		System.out.println();
		System.out.println("Processing file " + fileName);
		try {
			DocumentBuilder docReader  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			xmldoc = docReader.parse(inputFile);
		}
		catch (IOException e) {
			System.out.println("Can't read file " + fileName + "; IGNORED.\n\n");
			return;
		}
		catch (SAXException e) {
			System.out.println("File " + fileName + " is not XML; IGNORED.\n\n");
			return;
		} 
		catch (ParserConfigurationException e) {
			System.out.println("Can't read any XML file!!  Not supported.  IGNORED.\n\n");
			return;
		}
		try {
			settings = MandelbrotSettings.createFromXML(xmldoc.getDocumentElement());
		}
		catch (IOException e) {
			System.out.println("File " + fileName + " does not contain Mandelbrot Settings; IGNORED\n\n");
			return;
		}
		if (size == null)
			size = new Dimension(800,600);
		System.out.println("  Image size = " + size.width + "-by-" + size.height);
		BufferedImage OSC;
		int[][] subPixelCounts = null;
		int[][] iterationCounts;
		try {
			OSC = new BufferedImage(size.width,size.height,BufferedImage.TYPE_INT_RGB);
			iterationCounts = new int[OSC.getHeight()][OSC.getWidth()*2];  // twice as big as necessary, for memory check
		}
		catch (OutOfMemoryError e) {
			System.out.println("  Not enough memory to process " + size.width +
					"-by-" + size.height + " image.  IGNORED.\n\n");
			return;
		}
		if (subsampled)
			subPixelCounts = new int[OSC.getHeight() + 1][OSC.getWidth() + 1];
		TaskManager.Job currentJob = taskManager.createJob();
		System.out.println("  Using " + taskManager.getThreadPoolSize() + " threads");
		int rows = OSC.getHeight();
		int cols = OSC.getWidth();  
		xmin = settings.getLimits()[0];
		xmax = settings.getLimits()[1];
		ymin = settings.getLimits()[2];
		ymax = settings.getLimits()[3];  
		checkAspect(OSC);  
		System.out.println();
		System.out.println("  xmin = " + xmin);
		System.out.println("  xmax = " + xmax);
		System.out.println("  ymin = " + ymin);
		System.out.println("  ymax = " + ymax);
		System.out.println();
		BigDecimal dy = ymax.subtract(ymin).setScale(2*ymax.scale()).divide(new BigDecimal(rows-1), ymax.scale(), BigDecimal.ROUND_HALF_EVEN);
		boolean usingHighPrecision = settings.isHighPrecisionEnabled() && Math.abs(dy.doubleValue()) < HP_CUTOFF;
		int maxIterations = settings.getMaxIterations();
		for (int i = 0; i < rows; i++) {
			BigDecimal yval = ymax.subtract(dy.multiply(new BigDecimal(i)));
			MandelbrotTask task = new MandelbrotTask(i,xmin,xmax,yval,cols,maxIterations,usingHighPrecision);
			currentJob.add(task);
		}
		currentJob.close();
		iterationCounts = null;
		iterationCounts = new int[OSC.getHeight()][];
		System.out.println("  Total Memory Available: "+Runtime.getRuntime().totalMemory()/1000000.0 + " meg");
		System.out.println("  Free Memory: "+Runtime.getRuntime().freeMemory()/1000000.0 + " meg");
		System.out.println("  Computing data for file " + fileName + " ... ");
		if (subsampled)
			System.out.println("\n  Computing first pass.");
		System.out.flush();
		int tasksCompleted = 0;
		while (true) {
			boolean finished = currentJob.await(1000*60*5);
			MandelbrotTask[] tasks = currentJob.finishedTasks();
			if (tasks.length > 0) {
				for (MandelbrotTask task: tasks)
					iterationCounts[task.getRowNumber()] = task.getResults();
				tasksCompleted += tasks.length;
				if (subsampled)
					System.out.println("  " + tasksCompleted + " rows of " + rows + " completed for first pass.");
				else
					System.out.println("  " + tasksCompleted + " rows of " + rows + " completed.");
			}
			if (finished)
				break;
		}
		boolean doSubsamples = true;
		if (subsampled) {
			System.out.println("\n  Done first pass.  Starting second pass...");
			Runtime.getRuntime().gc();
			try {
				subPixelCounts = new int[OSC.getHeight() + 1][];
			}
			catch (OutOfMemoryError e) {
				System.out.println("  Not enough moroy for second pass!  Continuing with first pass data only.");
				doSubsamples = false;
			}
			if (doSubsamples) {
				System.out.println("  Free Memory: "+Runtime.getRuntime().freeMemory()/1000000.0 + " meg");
				System.out.flush();
				currentJob = taskManager.createJob();
				rows = OSC.getHeight() + 1;
				cols = OSC.getWidth() + 1;
				subPixelCounts = new int[rows][];
				BigDecimal halfDy = dy.divide(TWO, ymax.scale(), BigDecimal.ROUND_HALF_EVEN);
				BigDecimal xmin1 = xmin.subtract(halfDy);
				BigDecimal xmax1 = xmax.add(halfDy);
				BigDecimal ymax1 = ymax.add(halfDy);
				for (int i = 0; i < rows; i++) {
					BigDecimal yval = ymax1.subtract(dy.multiply(new BigDecimal(i)));
					MandelbrotTask task = new MandelbrotTask(i,xmin1,xmax1,yval,cols,maxIterations,usingHighPrecision);
					currentJob.add(task);
				}
				currentJob.close();
				tasksCompleted = 0;
				while (true) {
					boolean finished = currentJob.await(1000*60*5);
					MandelbrotTask[] tasks = currentJob.finishedTasks();
					if (tasks.length > 0) {
						for (MandelbrotTask task: tasks)
							subPixelCounts[task.getRowNumber()] = task.getResults();
						tasksCompleted += tasks.length;
						System.out.println("  " + tasksCompleted + " rows of " + rows + " completed for second pass.");
					}
					if (finished)
						break;
				}
			}
		}
		int rgbForMandelbrot = settings.getMandelbrotColor().getRGB();
		int colorCt = settings.getPaletteMapping().getLength();
		if (colorCt == 0)
			colorCt = maxIterations;
		int[] paletteColors = settings.getPalette().makeRGBs(colorCt,settings.getPaletteMapping().getOffset());
		if (subsampled && doSubsamples) {
			float[][] paletteColorComponents = new float[paletteColors.length][];
			for (int i = 0; i < paletteColors.length; i++)
				paletteColorComponents[i] = new Color(paletteColors[i]).getRGBColorComponents(null);
			float[] mandelbrotColorComponents = settings.getMandelbrotColor().getRGBColorComponents(null);
			for (int y = 0; y < iterationCounts.length; y++) {
				applySubpixelData(OSC,y,maxIterations,paletteColorComponents,
						mandelbrotColorComponents,iterationCounts,subPixelCounts);
			}
		}
		else {
			for (int y = 0; y < iterationCounts.length; y++) {
				int[] counts = iterationCounts[y];
				for (int x = 0; x < counts.length; x++) {
					int rgb;
					if (counts[x] == maxIterations)
						rgb = rgbForMandelbrot;
					else 
						rgb = paletteColors[counts[x] % paletteColors.length];
					OSC.setRGB(x, y, rgb);
				}
			}
		}
		iterationCounts = null;
		subPixelCounts = null;
		String outputFileName = fileName + "_" + OSC.getWidth() + "x" + OSC.getHeight() + "." + format.toLowerCase();
		System.out.print("\n  Saving " + outputFileName + " ...");
		try {
			File outputFile = new File(outputFileName);
			if ( ImageIO.write(OSC, format, outputFile) )
				System.out.println("  Done.");
			else {
				if (outputFile.isFile())
					outputFile.delete();
				throw new Exception("  Format '" + format + "' not implemented.");
			}
		}
		catch (Exception e) {
			System.out.println("  ERROR WHILE TRYING TO WRITE FILE: " + e);
		}
		System.out.println();
	}



	private static void checkAspect(BufferedImage OSC) {
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
		int scale = precision + 5 + (precision-10)/10;
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

	private static void applySubpixelData(BufferedImage OSC, int row,
			int maxIterations, float[][] paletteColorComponents,
			float[] mandelbrotColorComponents, int[][] iterationCounts,
			int[][] subPixelCounts) {
		int[] rowData, beforeData, afterData;
		rowData = iterationCounts[row];
		beforeData = subPixelCounts[row];
		afterData = subPixelCounts[row+1];
		for (int col = 0; col < rowData.length; col++) {
			float[] a, b, c, d, e;
			if (rowData[col] == maxIterations)
				a = mandelbrotColorComponents;
			else
				a = paletteColorComponents[rowData[col] % paletteColorComponents.length];
			if (beforeData[col] == maxIterations)
				b = mandelbrotColorComponents;
			else
				b = paletteColorComponents[beforeData[col] % paletteColorComponents.length];
			if (beforeData[col+1] == maxIterations)
				c = mandelbrotColorComponents;
			else
				c = paletteColorComponents[beforeData[col+1] % paletteColorComponents.length];
			if (afterData[col] == maxIterations)
				d = mandelbrotColorComponents;
			else
				d = paletteColorComponents[afterData[col] % paletteColorComponents.length];
			if (afterData[col+1] == maxIterations)
				e = mandelbrotColorComponents;
			else
				e = paletteColorComponents[afterData[col+1] % paletteColorComponents.length];
			float x,y,z;
			x = (4*a[0] + b[0] + c[0] + d[0] + e[0])/8;
			y = (4*a[1] + b[1] + c[1] + d[1] + e[1])/8;
			z = (4*a[2] + b[2] + c[2] + d[2] + e[2])/8;
			int rgb = new Color(x,y,z).getRGB();
			OSC.setRGB(col, row, rgb);
		}
	}

}
