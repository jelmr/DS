package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.model.*;
import distributed.systems.gridscheduler.remote.RemoteGridScheduler;
import distributed.systems.gridscheduler.remote.RemoteResourceManager;
import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public class RemoteResourceManagerImpl implements RemoteResourceManager, Serializable, INodeEventHandler {


	public static final int MAX_QUEUE_SIZE = 32;

	private Cluster cluster;
	private Queue<Job> jobQueue;
	private int jobQueueSize;
	private String name;

	private RemoteGridScheduler rgs;
	private RemoteResourceManager stub;
	private LogicalClock logicalClock;


	public RemoteResourceManagerImpl(String name, int numberOfNodes, RemoteGridScheduler rgs) throws RemoteException {

		this.name = name;
		this.cluster = new Cluster(name, this, rgs, numberOfNodes);

		this.stub = this.getStub();
		this.rgs = rgs;
		this.rgs.registerResourceManager(this.stub);

		this.logicalClock = new LamportsClock();

		this.jobQueue = new ConcurrentLinkedQueue<Job>();
		this.jobQueueSize = cluster.getNodeCount() + MAX_QUEUE_SIZE;
	}


	public static void main(String[] args) {

		if (args.length < 5) {

			System.out.printf("Usage: gradle rm  -Pargv=\"['<name>', '<numberOfNodes>', '<registryHost>', '<registryPort>', '<gridScheduler1>' [... ,'gridSchedulerN']]\"");


			System.exit(1);
		}

		String name = args[0];
		int numberOfNodes = Integer.parseInt((args[1]));
		String registryHost = args[2];
		int registryPort = Integer.parseInt(args[3]);

		RemoteGridScheduler rgs = null;
		try {

			Registry register = LocateRegistry.getRegistry(registryHost, registryPort);

			for (int i=4; i<args.length; i++) {
				String gridSchedulerURL = args[i];
				try {
					rgs = (RemoteGridScheduler) register.lookup(gridSchedulerURL);
				} catch (NotBoundException ignored){
					continue;
				};

				// If we found a reachable GridScheduler
				RemoteResourceManagerImpl rm = new RemoteResourceManagerImpl(name, numberOfNodes, rgs);

				RemoteResourceManager rrm = rm.getStub();
				Registry registry = LocateRegistry.getRegistry();
				registry.bind(name, rrm);

				// TODO :Stop termination

			}
		} catch (RemoteException e) {
			System.out.printf("Remote Exception:\n");
			e.printStackTrace();
		} catch (AlreadyBoundException e) {
			System.out.printf("The url '%s' is already bound.", name);
			System.exit(1);
		}

		if (rgs == null) {
			System.out.printf("Could not connect to any grid scheduler.\n");
			System.exit(1);
		}



	}

	public RemoteResourceManager getStub() throws RemoteException {

		if (this.stub == null) {
			this.stub = (RemoteResourceManager) UnicastRemoteObject.exportObject(this, 0);
		}

		return this.stub;
	}


	@Override
	public boolean isAlive() throws RemoteException {
		return true;
	}


	@Override
	public int getLoad() throws RemoteException {
		return 0;
	}


	@Override
	public boolean registerAsDuplicate(RemoteResourceManager rrm) throws RemoteException {
		return true;
	}


	@Override
	public boolean addJob(Job job) throws RemoteException {
		String logLine = String.format("ResourceManager '%s' received request to process job id='%d' with duration=%d from '%s'.", this.getName(), job.getId(), job.getDuration(), job.getIssueingClientName());
		this.rgs.logEvent(new Event.GenericEvent(this.logicalClock, logLine));


		if (jobQueue.size() >= jobQueueSize) { // if the jobqueue is full, offload the job to the grid scheduler
			this.rgs.scheduleJob(job, this.stub);

		} else { // otherwise store it in the local queue
			jobQueue.add(job);
			scheduleJobs();
		}
		return true;
	}

	/**
	 * Tries to find a waiting job in the jobqueue.
	 * @return
	 */
	public Job getWaitingJob() {
		// find a waiting job
		for (Job job : jobQueue)
			if (job.getStatus() == JobStatus.Waiting)
				return job;

		// no waiting jobs found, return null
		return null;

	}

	/**
	 * Tries to schedule jobs in the jobqueue to free nodes.
	 */
	public void scheduleJobs() {
		// while there are jobs to do and we have nodes available, assign the jobs to the
		// free nodes
		Node freeNode;
		Job waitingJob;

		while ( ((waitingJob = getWaitingJob()) != null) && ((freeNode = cluster.getFreeNode()) != null) ) {
			freeNode.startJob(waitingJob);
		}

	}


	/**
	 * Called when a job is finished
	 * <p>
	 * pre: parameter 'job' cannot be null
	 */
	public void jobDone(Job job) {
		// preconditions
		assert(job != null) : "parameter 'job' cannot be null";

		// job finished, remove it from our pool
		jobQueue.remove(job);
	}


	@Override
	public boolean logEvent(Event e) throws RemoteException {
		return this.rgs.logEvent(e);
	}


	@Override
	public String getName() throws RemoteException {
		return this.name;
	}


}
