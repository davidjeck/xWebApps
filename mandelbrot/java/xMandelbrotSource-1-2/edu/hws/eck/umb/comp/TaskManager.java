package edu.hws.eck.umb.comp;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;



/**
 * Provides parallelization for a collection of tasks, where each task is an
 * object of type MandelbrotTask.  The tasks can be run by a pool of threads,
 * where the size of the pool can be specified in the TaskManager constructor.
 * The default is for the number of threads to be equal to the number of processors.
 * Tasks can also be distributed to other computers, if "network workers" have
 * been added to the TaskManager.
 * <p>{@link #createJob()} returns an object of type TaskManager.Job.  This
 * job does not initially have any tasks to do.  Tasks can be added by
 * calling {@link TaskManager.Job#add(Runnable)}.  After all tasks that are part of
 * the job have been added, {@link TaskManager.Job#close()} must be called to
 * indicate that the job is complete.
 * <p>If a new job is created before a previous job is finished, the previous
 * job is automatically canceled.
 */
public class TaskManager {
	
	/**
	 * Represents a job that consists of the execution of a number of MandelbrotTasks.
	 * It is not possible to create an object of this class directly.  Objects
	 * of type TaskManager.Job are returned by
	 * {@link TaskManager#createJob()}. A job can start executing as soon as it has
	 * been created and the first task has been added.
	 */
	public static class Job {
		private static int nextJobID = 1;
		private final int jobID;
		private final TaskManager owner;
		private final ArrayList<MandelbrotTask> tasks;
		private volatile boolean closed;
		private volatile boolean finished;
		private volatile boolean canceled;
		private int nextTask;
		private int nextRepeatTask;
		private volatile int finishedTaskCount;
		private final ArrayList<MandelbrotTask> waitingFinishedTasks;
		private final ArrayList<MandelbrotTask> networkedTasks;
		private Job(TaskManager owner, Collection<? extends MandelbrotTask> tasks) {
			jobID = nextJobID++;
			this.owner = owner;
			if (tasks == null)
				this.tasks = new ArrayList<MandelbrotTask>();
			else
				this.tasks = new ArrayList<MandelbrotTask>(tasks);
			for (int i = this.tasks.size() - 1; i >= 0; i--)
				if (this.tasks.get(i) == null)
					this.tasks.remove(i);
			waitingFinishedTasks = new ArrayList<MandelbrotTask>();
			networkedTasks = new ArrayList<MandelbrotTask>();
		}
		private void finish(MandelbrotTask task) {
			synchronized(owner) {
				if (finished || task.getJobNumber() != jobID || task.isDone())
					return;
				task.makeDone();
				finishedTaskCount++;
				waitingFinishedTasks.add(task);
				if (closed && finishedTaskCount == tasks.size()) {
					finished = true;
					owner.finish(this);
				}
			}
		}
		private MandelbrotTask nextTask(boolean forNetwork) {
			synchronized(owner) {
	 			if (finished)
					return null;
				else if (nextTask < tasks.size()) {
					MandelbrotTask t = tasks.get(nextTask++);
					if (forNetwork)
						networkedTasks.add(t);
					return t;
				}
				else if (forNetwork || !closed)
					return null;
				else if (nextRepeatTask >= networkedTasks.size())
					return null;
				else {
					while (nextRepeatTask < networkedTasks.size()) {
						MandelbrotTask t = networkedTasks.get(nextRepeatTask++);
						if (!t.isDone()) {
							//System.out.println("Reassigning task " + t.getRowNumber());
							return t;
						}
					}
					return null;
				}
			}
		}
		/**
		 * Add a task to this job.  The job will not finish until all the tasks that have
		 * been added to the job have finished.  Also,  the job must be closed before it can finish.
		 * Note that tasks cannot be added to a job that has is "closed".  See {@link #close()}.
		 * @param task the task that is to be added to this job.  A null value is ignored.
		 * @throws IllegalStateException if the job has already been closed.
		 */
		public void add(MandelbrotTask task) {
			if (task == null)
				return;
			if (closed)
				throw new IllegalStateException("Can't add a new task to a job after the job has been closed.");
			synchronized(owner) {
				task.setJobNumber(jobID);
				tasks.add(task);
				owner.notifyAll();
			}
		}
		/**
		 * "Close" this job, making it possible for the job to complete.  Closing a job also makes
		 * it impossible to add new tasks to the job. A job that was created using 
		 * {@link TaskManager#createJob()} must
		 * be closed, or it will be impossible for that job to finish; the job should be closed
		 * by calling this method after all the tasks that are part of the job have been added.
		 */
		synchronized public void close() {
			if (closed)
				return;
			closed = true;
			if (finishedTaskCount == tasks.size()) {
				finished = true;
				owner.finish(this);
			}
		}
		/**
		 * Tells the fraction of tasks that have been added to this job that have been completed.
		 * @return a number between 0 and 1 obtained by dividing the number of completed tasks by
		 * the number of tasks that have been added.  If no tasks have been added, the return value
		 * is 1.  Note that the fractionDone can go down, if more tasks are added to the job.
		 */
		public double fractionDone() {
			if (tasks.size() == 0)
				return 1;
			else
				return (double)(finishedTaskCount)/tasks.size();
		}
		/**
		 * Returns the number of tasks in this job that have been completed successfully.
		 */
		public int finishedTaskCount() {
			return finishedTaskCount;
		}
		/**
		 * Returns the number of tasks that have been added to this job.
		 */
		public int totalTaskCount() {
			return tasks.size();
		}
		/**
		 * Cancel the job.  Tasks that have not yet been started will not be discarded; however,
		 * tasks that are in progress can run to completion and might finish after this method
		 * returns.  This method can be called to cancel a job even if that job has not yet been
		 * closed.
		 */
		public void cancel() {
			synchronized(owner) {
				finished = true;
				canceled = true;
				closed = true;
				owner.threadPool.cancelJob(this);
				owner.finish(this);
			}
		}
		/**
		 * Tells whether the job is finished.  A job is finished either when all the tasks
		 * that are part of the job are done or when the job has been canceled.
		 */
		public boolean isFinished() {
			return finished;
		}
		/**
		 * Tells whether the job has been canceled.  A job can be canceled by calling
		 * {@link #cancel()}.
		 */
		public boolean isCanceled() {
			return canceled;
		}
		/**
		 * Returns an array that contains tasks from this job that have completed.
		 * If this method has been called previously, only the newly completed tasks, since the
		 * last call, are returned.  The return value can be an empty array, if there are no
		 * newly completed tasks, but the return value is never null.  This method can be
		 * used to retrieve completed tasks for further processing.
		 */
		public MandelbrotTask[] finishedTasks() {
			synchronized(owner) {
				if (waitingFinishedTasks.size() == 0)
					return new MandelbrotTask[0];
				MandelbrotTask[] tasks = new MandelbrotTask[waitingFinishedTasks.size()];
				waitingFinishedTasks.toArray(tasks);
				waitingFinishedTasks.clear();
				return tasks;
			}
		}
		/**
		 * Waits either a specified amount of time or indefinitely for this job to finish.
		 * The method will return only after the job completes or after the specified timeout
		 * if the job does not complete within that time.
		 * The return value tells whether or not the job has completed.  If the job is
		 * already complete when this method is called, it returns immediately.
		 * @param timeoutMilliseconds the maximum time to wait for the job to finish. A value
		 * of 0 (or less) means to wait as long as it takes for the job to finish.
		 * @return true if the job has finished, false if not.  Note that if the argument is
		 * less than or equal to 0, then the return value has to be true.
		 */
		public boolean await(int timeoutMilliseconds) {
			synchronized(owner) {
				if (finished)
					return true;
				try {
					if (timeoutMilliseconds <= 0)
						owner.wait();
					else
						owner.wait(timeoutMilliseconds);
				}
				catch (InterruptedException e) {
				}
				return finished;
			}
		}
	}
	
	private ThreadPool threadPool;
	private boolean fullShutDown;
	private Job currentJob;
	private int workerCount;
	
	/**
	 * Create a TaskManager that will use a pool of threads with one thread per available processor.
	 * @see #TaskManager(int)
	 */
	public TaskManager() {
		this(0);
	}
	
	/**
	 * Create a TaskManager that will use a pool of threads with a specified number of threads.
	 * The threads are used to execute "jobs", where a job consists of a collection of
	 * MandelbrotTask objects.  Note that even a thread pool with just one thread can be useful
	 * for asynchronous execution.
	 * @param threadPoolSize the number of thread to be used.  If the value is 0 (or less),
	 * then the number of threads will be equal to the number of available processors.
	 */
	public TaskManager(int threadPoolSize) {
		if (threadPoolSize <= 0)
			threadPoolSize = Runtime.getRuntime().availableProcessors();
		threadPool = new ThreadPool(this, threadPoolSize);
		workerCount = threadPoolSize;
	}
	
	/**
	 * This method should be called before discarding the TaskManager.  Any jobs that
	 * have not been completed are canceled (using {@link TaskManager.Job#cancel()}).
	 * Then the threads in the thread pool are allowed to die.  It is not possible
	 * to add new jobs to a TaskManager after the TaskManager has been shut down.
	 */
	synchronized public void shutDown() {
		if (fullShutDown == true) // already shutdown
			return;
		fullShutDown = true;
		if (currentJob != null)
			currentJob.cancel();
		if (threadPool != null)
			threadPool.shutDown = true;
		threadPool = null;
		shutDownNetwork();
		notifyAll();
	}
	
	/**
	 * Returns the number of threads that will be used in the thread pool.
	 * (possibly just starting with the next job).
	 */
	public int getThreadPoolSize() {
		return workerCount;
	}
		
	/**
	 * Set the number of (local) threads to be used in the thread pool.
	 * A new value will only take effect when the next job is created.
	 */
	synchronized public void setThreadPoolSize(int poolSize) {
		workerCount = poolSize;
	}
	
	
	/**
	 * Creates a "job" to which a collection of tasks can be added.  See {@link TaskManager.Job#add(MandelbrotTask)}.
	 * The job must be "closed," using {@link TaskManager.Job#close()} after all the tasks have been added,
	 * or the job will never complete.
	 * @return the job object.  This can be used to add tasks to the job, to get status information about
	 * the job, and to wait for the job to complete.
	 * @throws IllegalStateException if this method is called after {@link #shutDown()} has been called.
	 */
	synchronized public Job createJob() {
		if (fullShutDown)
			throw new IllegalStateException("Can't execute tasks after shutdown.");
		if (currentJob != null)
			currentJob.cancel();
		if (workerCount != threadPool.getSize()) {
			threadPool.shutDown = true;
			notifyAll();
			threadPool = new ThreadPool(this,workerCount);
		}
		currentJob  = new Job(this,null);
		newNetworkJob(currentJob.jobID);
		return currentJob;
	}
	
	/**
	 * Tells whether this TaskManager has is working on a job.  Note that the
	 * TaskManager might not really be doing anything, if the job does not have any tasks that
	 * still need to be performed.
	 */
	synchronized public boolean busy() {
		return currentJob != null;
	}
	
	
	synchronized private void finish(Job job) {  // called by a job when it is finished, so the job can be removed from this task manager.
		if (currentJob == job) {
			currentJob = null;
			if (workerCount != threadPool.getSize())
				threadPool.shutDown = true;
		}
		notifyAll();
	}
	
	synchronized private Object[] nextTask(boolean forNetwork) {
		if (currentJob == null || fullShutDown)
			return null;
		MandelbrotTask task = currentJob.nextTask(forNetwork);
		if (task == null)
			return null;
		return new Object[] { task, currentJob };
	}
	
	private static class ThreadPool {
		final Worker[] pool;
		final TaskManager owner;
		volatile boolean shutDown;
		ThreadPool(TaskManager owner, int poolSize) {
			this.owner = owner;
			pool = new Worker[poolSize];
			int priority = Thread.currentThread().getPriority();
			for (int i = 0; i < poolSize; i++) {
				pool[i] = new Worker();
				pool[i].setDaemon(true);
				try {
					pool[i].setPriority(priority-1);
				}
				catch (Exception e) {
				}
				pool[i].start();
			}
		}
		int getSize() {
			return pool.length;
		}
		void cancelJob(Job job) {
			for (Worker w : pool)
				w.cancelJob(job);
		}
		class Worker extends Thread {
			Job job;
			MandelbrotTask task;
			synchronized void cancelJob(Job canceledJob) {
				if (job == canceledJob && task != null)
					task.makeDone();
			}
			synchronized void setTask(Object[] taskinfo) {
				if (taskinfo == null) {
					task = null;
					job = null;
				}
				else {
					task = (MandelbrotTask)taskinfo[0];
					job = (Job)taskinfo[1];
				}
			}
			public void run() {
				int jobsDone = 0;
				try {
					while (!shutDown) {
						Object[] taskinfo;
						do {
							synchronized(owner) {
								taskinfo = owner.nextTask(false);
								if (taskinfo == null && !shutDown) {
									try {
										owner.wait();
									}
									catch (InterruptedException e) {
									}
								}
							}
						} while (taskinfo == null && !shutDown);
						if (shutDown)
							break;
						setTask(taskinfo);
						task = (MandelbrotTask)taskinfo[0];
						job = (Job)taskinfo[1];
						task.run();
						jobsDone++;
						job.finish(task);
						setTask(null);
					}
				}
				finally {
					System.out.println("Compute thread exiting after " + jobsDone + " tasks.");
				}
			}
		}
	}
	
	//-------------------------- Support for networking --------------------------------------
	
	/**
	 * A possible value for the status of a network worker.  Negative values indicate
	 * that an error has occurred, and the worker cannot process any tasks.
	 */
	public static int NET_STATUS_INACTIVE = 0;
	public static int NET_STATUS_CONNECTING = 1;
	public static int NET_STATUS_CONNECTED = 2;
	public static int NET_STATUS_CLOSING = 3;
	public static int NET_STATUS_CLOSED = 4;
	public static int NET_STATUS_ERROR_CANT_CONNECT = -1;
	public static int NET_STATUS_ERROR_WRITE_ERROR = -2;
	public static int NET_STATUS_ERROR_READ_ERROR = -3;
	public static int NET_STATUS_ERROR_BAD_PEER = -4;
	
	/**
	 * A container for information about the status of a network worker.
	 */
	public static class NetworkWorkerInfo {
		final public String host;
		final public int port;
		final public int status;
		final public int tasksDone;
		final public int workerID;
		public NetworkWorkerInfo(String host, int port, int status, int tasksDone, int workerID) {
			this.host = host;
			this.port = port;
			this.status = status;
			this.tasksDone = tasksDone;
			this.workerID = workerID;
		}
	}
	
	private boolean networkingEnabled;
	private ArrayList<NetworkWorker> networkWorkers;
	private int nextNetworkWorkerID;
	
	/**
	 * Tells whether networking is enabled in this TaskManager.
	 * @return
	 */
	public boolean getNetworkingEnabled() {
		return networkingEnabled;
	}
	
	/**
	 * Sets the value of the networkingEnabled property in this TaskManager.  If the
	 * value is changed from true to false, any existing network workers are stopped and
	 * are removed from the TaskManager. It is not legal to add network workers unless
	 * networking is enabled.
	 */
	public void setNetworkingEnabled(boolean enabled) {
		if (enabled == networkingEnabled)
			return;
		networkingEnabled = enabled;
		if (!enabled && networkWorkers != null) {
			for (NetworkWorker nw : networkWorkers)
				nw.finish(false);
			networkWorkers = null;
		}
	}
	
	/**
	 * Add a network worker to the TaskManager.  Once the network connection is opened, the
	 * network worker will be able to compute MandelbrotTasks, just like a local compute thread.
	 * Network computation is robust in the sense that if a network connection fails, tasks that
	 * were assigned to that worker are reassigned to local threads so that the job as a whole
	 * can still complete successfully.
	 * @param host The IP address or host name of the computer on which the Mandelbrot computation server
	 * is running.  A MandelbrotNetworkTaskServer (or something that implements exactly the same
	 * protocol) should already be running on the specified host before this method is called.
	 * @param port The port on which the server is listening
	 * @return The id number of the worker, which can be used to get information about the
	 * worker and to remove it.
	 * @throws IllegalStateException if networking is not currently enabled in this TaskManager
	 */
	public int addNetworkWorker(String host, int port) {
		if (!networkingEnabled)
			throw new IllegalStateException("Can't add network worker when networing is disabled");
		NetworkWorker w = new NetworkWorker(host,port,currentJob);
		if (networkWorkers == null)
			networkWorkers  = new ArrayList<NetworkWorker>();
		networkWorkers.add(w);
		w.start();
		return w.id;
	}
	
	/**
	 * Returns the number of network workers that have been added to this TaskManager.
	 * @return
	 */
	public int getNetworkWorkerCount() {
		if (networkWorkers == null)
			return 0;
		else
			return networkWorkers.size();
	}
	
	/**
	 * Returns status info for a specified worker.  If the given worker ID does not
	 * exist, the return value is null.
	 */
	public NetworkWorkerInfo getNetworkWorkerInfo(int workerID) {
		if (networkWorkers == null)
			return null;
		for (NetworkWorker nw : networkWorkers)
			if (nw.id == workerID)
				return new NetworkWorkerInfo(nw.host,nw.port,nw.status,nw.tasksDone,nw.id);
		return null;
	}
	
	/**
	 * Returns an array containing status info for all network workers.  If there are not
	 * workers, an empty array is returned.
	 */
	public NetworkWorkerInfo[] getAllNetworkWorkerInfo() {
		if (networkWorkers == null)
			return new NetworkWorkerInfo[0];
		NetworkWorkerInfo[] info = new NetworkWorkerInfo[networkWorkers.size()];
		for (int i = 0; i < info.length; i++) {
			NetworkWorker nw = networkWorkers.get(i);
			info[i] = new NetworkWorkerInfo(nw.host,nw.port,nw.status,nw.tasksDone,nw.id);
		}
		return info;
	}
	
	/**
	 * Removes a network worker with a specified ID number, if there is one.
	 * @return true if a worker with the specified ID was found and removed, false if not
	 */
	public boolean removeNetworkWorker(int workerID) {
		if (networkWorkers == null)
			return false;
		for (int i = 0; i < networkWorkers.size(); i++)
			if (networkWorkers.get(i).id == workerID) {
				networkWorkers.get(i).finish(false);
				networkWorkers.remove(i);
				return true;
			}
		return false;
	}
	
	private void shutDownNetwork() {
		if (networkWorkers == null)
			return;
		for (NetworkWorker nw : networkWorkers)
			nw.finish(true);
		networkWorkers = null;
	}
	
	private void newNetworkJob(int jobID) {
		if (networkWorkers == null)
			return;
		for (NetworkWorker nw : networkWorkers)
			nw.newJob(jobID);
	}
		
	private class NetworkWorker extends Thread {
		final String host;
		final int port;
		final int id;
		int peerProcessCount;
		String outgoingMessage;
		volatile int tasksDone;
		volatile int status;
		ArrayList<Object[]> outstandingTasks = new ArrayList<Object[]>();
		synchronized void newJob(int jobID) {
			sendMessage(MandelbrotNetworkTaskServer.NEWJOB + " " + jobID);
			outstandingTasks.clear(); // clear out left-over tasks from previous job (in case job was canceled)
		}
		synchronized void finish(boolean doShutdown) {
			if (status == NET_STATUS_CONNECTED || status == NET_STATUS_CONNECTING) {
				status = NET_STATUS_CLOSING;
				if (doShutdown)
					outgoingMessage = MandelbrotNetworkTaskServer.SHUTDOWN;
				else
					outgoingMessage = MandelbrotNetworkTaskServer.SIGNOFF;
			}
			notifyAll();
			this.interrupt();
		}
		synchronized void newTask(Object[] taskInfo) {
			outstandingTasks.add(taskInfo);
		}
		synchronized void sendMessage(String message) {
			outgoingMessage = message;
			notifyAll();
		}
		synchronized void finishTask(int jobnum, int rownum, int[] results) {
			for (int i = 0; i < outstandingTasks.size(); i++) {
				Object[] taskInfo = outstandingTasks.get(i);
				MandelbrotTask task = (MandelbrotTask)taskInfo[0];
				if (task.getJobNumber() == jobnum && task.getRowNumber() == rownum) {
					outstandingTasks.remove(i);
					notifyAll();
					Job job = (Job)taskInfo[1];
					job.finish(task);
					task.setResults(results);
					tasksDone++;
					return;
				}
			}
		}
		NetworkWorker(String host, int port, Job currentJob) {
			this.host = host;
			this.port = port;
			id = nextNetworkWorkerID++;
			if (currentJob != null)
				outgoingMessage = MandelbrotNetworkTaskServer.NEWJOB + " " + currentJob.jobID;
			status = NET_STATUS_CONNECTING;
			try {
				setPriority(Thread.currentThread().getPriority()-1);
				setDaemon(true);
			}
			catch (Exception e) {
			}
			System.out.println("Created Network Worker");
		}
		public void run() { // Opens connection, then sends tasks to the server.  Responses are received in a separate ReaderThread.
			Socket socket;
			PrintWriter out;
			BufferedReader in;
			try {
				socket = new Socket(host,port);
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream());
			}
			catch (Exception e) {
				status = NET_STATUS_ERROR_CANT_CONNECT;
				System.out.println("Network thread exiting after 0 tasks; status = " + status);
				return;
			}
			if (status == NET_STATUS_CLOSING)
				return;
			System.out.println("Connected");
			try {
				out.println(MandelbrotNetworkTaskServer.HANDSHAKE);
				out.flush();
				if (out.checkError())
					throw new Exception();
				String handshake = in.readLine();
				if (handshake == null || ! handshake.startsWith(MandelbrotNetworkTaskServer.HANDSHAKE)) {
					status = NET_STATUS_ERROR_BAD_PEER;
					return;
				}
				try {
					peerProcessCount = Integer.parseInt(handshake.substring(MandelbrotNetworkTaskServer.HANDSHAKE.length()).trim());
				}
				catch (NumberFormatException e) {
					peerProcessCount = 1;
				}
				if (peerProcessCount <= 0)
					peerProcessCount = 1;
				status = NET_STATUS_CONNECTED;
				new ReaderThread(in).start();
				while (true) {
					if (outgoingMessage != null) {
						out.println(outgoingMessage);
						out.flush();
						outgoingMessage = null;
					}
					if (status != NET_STATUS_CONNECTED)
						break;
					synchronized(this) {
						while (status == NET_STATUS_CONNECTED && outstandingTasks.size() >= peerProcessCount) {
							try {
								wait();
							}
							catch (InterruptedException e) {
							}
							if (outgoingMessage != null && status == NET_STATUS_CONNECTED) {
								out.println(outgoingMessage);
								out.flush();
								outgoingMessage = null;
							}
						}
					}
					Object[] taskInfo = null;
					while (taskInfo == null && status == NET_STATUS_CONNECTED) {
						synchronized(TaskManager.this) {
							taskInfo = nextTask(true);
							if (taskInfo == null && status == NET_STATUS_CONNECTED) {
								try {
									TaskManager.this.wait();
								}
								catch (InterruptedException e) {
								}
							}
						}
					}
					if (taskInfo != null && status == NET_STATUS_CONNECTED) {
						if (outgoingMessage != null) {
							out.println(outgoingMessage);
							outgoingMessage = null;
						}
						newTask(taskInfo);
						MandelbrotTask task = (MandelbrotTask)taskInfo[0];
						String taskString = MandelbrotNetworkTaskServer.encode(task);
						out.println(taskString);
						out.flush();
						if (out.checkError())
							throw new Exception();
					}
				}
			}
			catch (Exception e) {
				if (status != NET_STATUS_CLOSING)
					status = NET_STATUS_ERROR_WRITE_ERROR;
			}
			finally {
				try {
					socket.close();
				}
				catch (Exception e) {
				}
				if (status == NET_STATUS_CLOSING)
					status = NET_STATUS_CLOSED;
				System.out.println("Network thread exiting after " + tasksDone + " tasks; status = " + status);
			}
		}
		class ReaderThread extends Thread {
			BufferedReader in;
			ReaderThread(BufferedReader in) {
				this.in = in;
				setDaemon(true);
			}
			public void run() {
				try {
					while (status == NET_STATUS_CONNECTED) {
						String taskInfo = in.readLine();
						if (taskInfo == null)
							throw new Exception();
						Scanner data = new Scanner(taskInfo);
						int jobnum = data.nextInt();
						int rownum = data.nextInt();
						int resultCt = data.nextInt();
						int[] results = new int[resultCt];
						for (int i = 0; i < resultCt; i++)
							results[i] = data.nextInt();
						data.close();
						if (status == NET_STATUS_CONNECTED)
							finishTask(jobnum, rownum, results);
					}
				}
				catch (Exception e) {
					if (status == NET_STATUS_CONNECTED)
						status = NET_STATUS_ERROR_READ_ERROR;
				}
			}
		}
	}
	

}
