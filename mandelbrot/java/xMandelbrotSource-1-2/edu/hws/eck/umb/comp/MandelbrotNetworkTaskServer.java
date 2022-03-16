package edu.hws.eck.umb.comp;

import java.math.BigDecimal;
import java.net.*;
import java.io.*;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

import edu.hws.eck.umb.comp.MandelbrotTask;

/**
 * A main program for running a server that can execute MandelbrotTasks.
 * Each network worker in a TaskManager communicates with a MandelbrotNetworkTaskServer
 * that does the computation for that worker.  A server can run several compute threads,
 * and so can handle several tasks at once.  By default, the server uses one less thread
 * than there are processors available (but not less than one process).  By default,
 * the server will exit after 30 minutes of inactivity.  These defaults can be changed
 * on the command line.  The following command-line arguments are understood:
 * <ul>
 * <li>-port XXX --- listen on port number XXX instead of on the default port (17071)
 * <li>-processcount XXX --- use XXX processes instead of the default number.  Use 0 for XXX to
 * use one process for each available processor.  (The default is to use one less than this,
 * if the number of processors is greater than one.)
 * <li>-timeout XXX --- Exit after XXX minutes of inactivity.  The default is 30 minutes.  Use 0 for
 * XXX to mean that there is no timeout.
 * <li>-once --- accept one connection, and exit when that connection is closed.  The default is
 * to open a new listener after the connection is closed and wait for another connection.
 * <li>-quiet --- suppress all output
 * <li>-shutdown HHH PPP --- if this option occurs on the command line, the program will
 * attempt to send a shutdown command to the MandelbrotNetworkTaskServer running on host HHH and port
 * PPP.  The program will then exit immediately; it will NOT listen for connections.
 * If PPP is omitted, the default port is used.  If HHH is also omitted, localhost is used.
 * Note that this cannot be used to shut down a server that is connected to a client, since
 * the server does not listen for new connections while it is already connected.
 * </ul>
 */
public class MandelbrotNetworkTaskServer {
	
	/**
	 * String for communicating with the client.
	 */
	public static final String HANDSHAKE = "DISTRIBUTED ULTIMATE MANDELBROT";
	public static final String NEWJOB = "NEWJOB";
	public static final String SIGNOFF = "SIGNOFF";
	public static final String SHUTDOWN = "SHUTDOWN";

	public static final int DEFAULT_PORT = 17071;

	private static ArrayBlockingQueue<MandelbrotTask> tasks;
	private static ArrayBlockingQueue<MandelbrotTask> finishedTasks;
	private static Timer timer = new Timer(true);
	private static Worker[] workers;
	private static boolean quiet;
	private static volatile int jobNum;
	private static volatile int connectionID;
	
	public static void main(String[] args) {
		int port = DEFAULT_PORT;
		int processCount = -1;
		boolean once = false;
		try {
			int i = 0;
			while (i < args.length) {
				String arg = args[i++];
				if (arg.equalsIgnoreCase("-shutdown")) {
					String host = "localhost";
					if (i < args.length)
						host = args[i];
					if (i+1 < args.length) {
						try {
							port = Integer.parseInt(args[i+1]);
						}
						catch (NumberFormatException e) {
						}
					}
					println("Sending SHUTDOWN command to " + host + ":" + port);
					doShutdown(host,port);
					println("Command sent; exiting.");
					System.exit(0);
				}
				if (arg.equalsIgnoreCase("-port"))
					port = Integer.parseInt(args[i++]);
				else if (arg.equalsIgnoreCase("-processcount"))
					processCount = Integer.parseInt(args[i++]);
				else if (arg.equalsIgnoreCase("-timeout"))
					TimeoutTask.timeout = Integer.parseInt(args[i++]);
				else if (arg.equalsIgnoreCase("-once"))
					once = true;
				else if (arg.equalsIgnoreCase("-quiet"))
					quiet = true;
				else
					throw new IllegalArgumentException();
			}
		}
		catch (Exception e) {
			err_println("Bad command line argument.");
			System.exit(1);
			return;
		}
		tasks = new ArrayBlockingQueue<MandelbrotTask>(25);
		finishedTasks = new ArrayBlockingQueue<MandelbrotTask>(25);
		try {
			if (processCount <= 0) {
				int processors = Runtime.getRuntime().availableProcessors();
				if (processors == 1 || processCount == 0)
					processCount = processors;
				else 
					processCount = processors - 1;
			}
			workers = new Worker[processCount];
			for (int i = 0; i < workers.length; i++) {
				workers[i] = new Worker();
				workers[i].setDaemon(true);
				workers[i].setPriority(workers[i].getPriority() - 1);
				workers[i].start();
			}
		}
		catch (Exception e) {
			err_println("Cound not create worker threads.");
			System.exit(1);
			return;
		}
		timer = new Timer(true);
		while (true) {
			ServerSocket listener;
			try {
				listener = new ServerSocket(port);
			}
			catch (Exception e) {
				err_println("Could not create listener on port " + port);
				err_println("Error: " + e);
				System.exit(1);
				return;
			}
			println("Listening on port " + port + "...");
			TimeoutTask.set();
			try {
				Socket socket = listener.accept();
				listener.close();
				synchronized(tasks) {
					connectionID++;
				}
				Thread connectionHandler = new ConnectionThread(socket,connectionID);
				connectionHandler.start();
				while (connectionHandler.isAlive()) {
					try {
						connectionHandler.join();
					}
					catch (InterruptedException e) {
					}
				}
				if (once)
					System.exit(0);
				TimeoutTask.set();
			}
			catch (Exception e) {
				err_println("Server socket has shut down unexpectedly.");
				err_println("Error: " + e);
				System.exit(1);
			}
		}
	}
	
	private static void doShutdown(String host, int port) {
		try {
			Socket connection = new Socket(host,port);
			PrintWriter out = new PrintWriter(connection.getOutputStream());
			out.println(HANDSHAKE);
			out.println(SHUTDOWN);
			out.flush();
			out.close();
		}
		catch (Exception e) {
			err_println("An error occurred while trying to send the SHUTDOWN: " + e);
			System.exit(1);
		}
	}

	private static void println(String s) {
		if (!quiet)
			System.out.println(s);
	}
			
	private static void err_println(String s) {
		if (!quiet)
			System.err.println(s);
	}
			
	/**
	 * Encodes a MandelbrotTask as a String that can be sent to this
	 * a MandelbrotNetworkTaskServer.  This method is used by network
	 * workers in TaskManagers to encode the tasks that the send to
	 * the servers.
	 */
	public static String encode(MandelbrotTask task) {
		StringBuffer b = new StringBuffer();
		b.append(task.getJobNumber());
		b.append(' ');
		b.append(task.getRowNumber());
		b.append(' ');
		b.append(task.getXmin());
		b.append(' ');
		b.append(task.getXmax());
		b.append(' ');
		b.append(task.getYval());
		b.append(' ');
		b.append(task.getColumnCount());
		b.append(' ');
		b.append(task.getMaxIterations());
		b.append(' ');
		b.append(task.isHighPrecision());
		return b.toString();
	}
	
	private static class TimeoutTask extends TimerTask {
		static TimerTask task;
		static int timeout = 30;
		synchronized static void set() {
			if (task != null)
				task.cancel();
			if (timeout > 0) {
				task = new TimeoutTask();
				timer.schedule(task, timeout*60000L);
			}
		}
		synchronized static void clear() {
			if (task != null)
				task.cancel();
			task = null;
		}
		public void run() {
			println("Exiting because activity timeout has expired.");
			System.exit(0);
		}
	}
	
	/**
	 * Converts a string that represents a MandelbrotTask into a ManadelbrotTask
	 * object.  The string is in the format produced by the encode() method.
	 */
	private static MandelbrotTask decode(String taskInfo) {
		Scanner scanner = new Scanner(taskInfo);
		int jobNumber = scanner.nextInt();
		int rowNumber = scanner.nextInt();
		BigDecimal xmin = scanner.nextBigDecimal();
		BigDecimal xmax = scanner.nextBigDecimal();
		BigDecimal yval = scanner.nextBigDecimal();
		int columnCount = scanner.nextInt();
		int maxIterations = scanner.nextInt();
		boolean highPrecision = scanner.nextBoolean();
		MandelbrotTask task = new MandelbrotTask(rowNumber, xmin, xmax, yval,columnCount,maxIterations,highPrecision);
		task.setJobNumber(jobNumber);
		return task;
	}
	
	static private void endJob(int newJobNum) {
		if (newJobNum != -1) {
			for (Worker w : workers)
				w.cancel(jobNum);
		}
		jobNum = newJobNum;
		tasks.clear();
	}
	
	private static class ConnectionThread extends Thread { // handles the connectio with the client
		final Socket socket;
		final int myConnectionID;
		volatile int tasksDone;
		ConnectionThread(Socket socket, int connectionID) {
			this.socket = socket;
			myConnectionID = connectionID;
			setDaemon(true);
		}
		public void run() {  // Open the connection, then sends results back to the client as they become available.
			InetAddress peer = null;
			try {
				peer = socket.getInetAddress();
				println("Connected to " + peer);
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				Scanner in = new Scanner(socket.getInputStream());
				out.println(HANDSHAKE + " " + workers.length); // Tells the client how many tasks can be handled at once.
				out.flush();
				try {
					String handshake = in.nextLine();
					if (!handshake.equals(HANDSHAKE))
						throw new IllegalArgumentException();
				}
				catch (Exception e) {
					throw new IOException("The other side of the connection is not a Mandelbrot client.");
				}
				ReaderThread readerThread = new ReaderThread(in);
				readerThread.start();
				mainloop: while (connectionID == myConnectionID) {
					MandelbrotTask task = null;
					while (task == null && connectionID == myConnectionID) {
						try {
							task = finishedTasks.take();
						}
						catch (InterruptedException e) {
							break mainloop;
						}
					}
					tasksDone++;
					if (task.getJobNumber() == jobNum) {
						int[] results = task.getResults();
						out.print(task.getJobNumber());
						out.print(' ');
						out.print(task.getRowNumber());
						out.print(' ');
						out.print(results.length);
						out.print(' ');
						for (int i = 0; i < results.length-1; i++) {
							out.print(results[i]);
							out.print(' ');
						}
						out.println(results[results.length-1]);
						out.flush();
						if (out.checkError())
							throw new IOException("Error while trying to transmit data.");
					}
				}
				if (readerThread.error != null)
					throw readerThread.error;
				println("Connection from " + peer + " closed normally.");
			}
			catch (Exception e) {
				err_println("Connection from " + peer + " closed with error :" + e);
			}
			finally {
				endJob(-1);
				try {
					socket.close();
				}
				catch (Exception e) {
				}
			}
		}
		private class ReaderThread extends Thread {
			Scanner in;
			volatile Exception error;
			public ReaderThread(Scanner in) {
				this.in = in;
				setDaemon(true);
			}
			public void run() {  // Receives jobs and commands from the client. Adds the jobs to a queue to feed them to computational threads.
				try {
					while (true) {
						String taskInfo;
						try {
							taskInfo = in.nextLine();
							TimeoutTask.set();
							if (taskInfo.equals(SHUTDOWN)) {
								System.out.println("Received shutdown command.  Exiting.");
								System.exit(0);
							}
							else if (taskInfo.equals(SIGNOFF))
								return;
							else if (taskInfo.startsWith(NEWJOB)) {
								int newJob = Integer.parseInt(taskInfo.substring(NEWJOB.length()).trim());
								println("Starting job " + newJob + "; " + tasksDone + " tasks completed.");
								endJob(newJob);
								continue;
							}
						}
						catch (Exception e) {
							break;
						}
						MandelbrotTask task;
						try {
							task = decode(taskInfo);
						}
						catch (Exception e) {
							throw new IOException("Illegal Mandelbot task data received.");
						}
						tasks.put(task);
					}
				}
				catch (Exception e) {
					error = e;
				}
				finally {
					synchronized (tasks) {
						if (connectionID == myConnectionID)
							connectionID++;
					}
					ConnectionThread.this.interrupt();
				}
			}
		}
	}
	
	
	private static class Worker extends Thread { // Computes MandelbrotTasks
		MandelbrotTask currentTask;
		synchronized void cancel(int jobNum) {
			if (currentTask != null && currentTask.getJobNumber() == jobNum)
				currentTask.makeDone();
		}
		synchronized private void setTask(MandelbrotTask task) {
			currentTask = task;
		}
		public void run() {
			MandelbrotTask task = null;
			while (true) {
				while (task == null) {
					try {
						task = tasks.take();
					}
					catch (InterruptedException e) {
					}
				}
				setTask(task);
				task.run();
				setTask(null);
				while (task != null) {
					try {
						finishedTasks.put(task);
						task = null;
					}
					catch (InterruptedException e) {
					}
				}
			}
		}
	}

}
