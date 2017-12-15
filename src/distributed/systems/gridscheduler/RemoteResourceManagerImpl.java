package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.cache.NodeData;
import distributed.systems.gridscheduler.model.*;
import distributed.systems.gridscheduler.remote.RemoteClient;
import distributed.systems.gridscheduler.remote.RemoteGridScheduler;
import distributed.systems.gridscheduler.remote.RemoteResourceManager;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public class RemoteResourceManagerImpl implements RemoteResourceManager, Serializable, INodeEventHandler {

    private final static long SCHEDULING_PERIOD = 1000; /* check for things to schedule every second */
	public static final int MAX_QUEUE_SIZE = 32;

	private Cluster cluster;
	private Queue<Job> jobQueue;
	private int jobQueueSize;
	private String name;

	// TODO: Poll nodes for outstanding requests
	private ConcurrentHashMap<Job, Node> outstandingJobs;

	private Named<RemoteGridScheduler> rgs;
	private RemoteResourceManager stub;
	private LogicalClock logicalClock;

	private QueuedLogger logger;



	public RemoteResourceManagerImpl(String name, int numberOfNodes, Named<RemoteGridScheduler> rgs) throws RemoteException {

		this.logger = new QueuedLogger(System.out);
		this.logicalClock = new LamportsClock();

		this.name = name;
		this.cluster = new Cluster(name, this, rgs, numberOfNodes);

		this.stub = this.getStub();
		this.rgs = rgs;
		this.rgs.getObject().registerResourceManager(this.stub, name);
		this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.RM_REGISTERS_WITH_GS, this.getName(), this.rgs.getName()));


		this.outstandingJobs = new ConcurrentHashMap<>();

		this.jobQueue = new ConcurrentLinkedQueue<>();
		this.jobQueueSize = cluster.getNodeCount() + MAX_QUEUE_SIZE;

		Timer t = new Timer();
		t.schedule(new TimerTask() {
            @Override
            public void run() {
                scheduleJobs();
            }
        }, 0, SCHEDULING_PERIOD);

	}


	public static void main(String[] args) {

		if (args.length < 5) {

			System.out.printf("Usage: gradle rm  -Pargv=\"['<name>', '<numberOfNodes>', '<registryHost>', '<registryPort>', '<gridScheduler1>' [... ,'gridSchedulerN']]\"");


			System.exit(1);
		}

		String name = args[0];
		int numberOfNodes = Integer.parseInt((args[1]));
		RegistryManager registryManager = null;
		try {
			registryManager = new RegistryManager(args[2], Integer.parseInt(args[3]));
		} catch (RemoteException e) {
			System.out.println("Couldn't reach the registry");
			return;
		}

//		String registryHost = args[2];
//		int registryPort = Integer.parseInt(args[3]);

		RemoteGridScheduler rgs = null;
		try {
//			Registry register = LocateRegistry.getRegistry(registryHost, registryPort);

			for (int i=4; i<args.length; i++) {
				String gsName = args[i];
				try {
					rgs = (RemoteGridScheduler) registryManager.lookup(gsName);
//					rgs = (RemoteGridScheduler) register.lookup(gsName);
				} catch (NotBoundException ignored){
					System.out.printf("Couldn't connect to %s\n", gsName);
					continue;
				};

				// If we found a reachable GridScheduler
				Named<RemoteGridScheduler> namedRgs = new Named<>(gsName, rgs);
				RemoteResourceManagerImpl rm = new RemoteResourceManagerImpl(name, numberOfNodes, namedRgs);
				RemoteResourceManager rrm = rm.getStub();

				rm.logicalClock.tickSendEvent();
				rm.logEvent(new Event.TypedEvent(rm.logicalClock, EventType.RM_REGISTERED_REGISTRY, rm.getName(),
						registryManager.getRegistryHost(),registryManager.getRegistryPort()));
				registryManager.bind(name, rrm);

//				Naming.rebind("//" + registryHost + ":" + registryPort + "/" + name, rrm);
//				register.rebind(name, rrm);

				// TODO :Stop termination

			}
		} catch (RemoteException e) {
			System.out.printf("Remote Exception:\n");
			e.printStackTrace();
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
	public int getCapacity() throws RemoteException {
		int numQueuedJobs = this.jobQueue.size();
		int numFreeNodes = this.cluster.getNumFreeNodes();
		System.out.printf("My capacity is %d-%d=%d", numFreeNodes, numQueuedJobs, numFreeNodes-numQueuedJobs);
		return numFreeNodes - numQueuedJobs;
	}


	@Override
	public boolean registerAsDuplicate(RemoteResourceManager rrm) throws RemoteException {
		return true;
	}


	@Override
	public boolean addJob(Job job) throws RemoteException {

		this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.RM_RECEIVED_JOB_REQUEST, this.getName(), job.getId(), job.getDuration(), job.getIssueingClientName()));


		if (jobQueue.size() >= jobQueueSize) { // if the jobqueue is full, offload the job to the grid scheduler
			this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.RM_OFFLOAD_TO_GS_ATTEMPT, this.getName(), job.getId(), this.rgs.getName()));
			this.rgs.getObject().offloadJob(job);

		} else { // otherwise store it in the local queue
			jobQueue.add(job);
			this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.RM_QUEUED_JOB, this.getName(), job.getId(), job.getDuration()));

			scheduleJobs();
		}
		return true;
	}


	@Override
	public boolean offloadJob(Job job, String issueingGsName) throws RemoteException {
		System.out.printf("Has accepted an offload job from %s id=%s\n", issueingGsName, job.getId());
		jobQueue.add(job);
		this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.RM_RECEIVED_JOB_OFFLOAD_REQUEST, this.getName(), job.getId(), issueingGsName));
		scheduleJobs();
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
			try {
				// TODO: Remove try catch?
				this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.RM_SCHEDULED_JOB_ON_NODE, this.name, waitingJob.getId(), freeNode.getName()));
			} catch (RemoteException ignored) {}
			this.outstandingJobs.put(waitingJob, freeNode);
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
		RemoteClient client = job.getIssueingClient();
		try {
			this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.RM_FINISHED_JOB, this.name, job.getId()));
			client.jobDone(job);
		} catch (RemoteException ignored) {
			// TODO: Do something
		}
		jobQueue.remove(job);

		// Schedule new jobs waiting in the queue
		this.scheduleJobs();
	}

	@Override
	public ArrayList<NodeData> getAllNodes() throws RemoteException {
		List<Node> source = cluster.getNodes();
		ArrayList<NodeData> result = new ArrayList<>(source.size());
		for (Node element : source) {
			result.add(new NodeData(element.getName(), element.getStatus()));
		}
		return result;
	}

	@Override
	public NodeData getNode(int index) throws RemoteException {
		Node node = cluster.getNodes().get(index);
		return new NodeData(node.getName(), node.getStatus());
	}

	@Override
	public Integer getQueueSize() throws RemoteException {
		return jobQueueSize;
	}

	@Override
    public Integer getQueuedJobs() throws RemoteException {
        return jobQueue.size();
    }

    @Override
	public boolean logEvent(Event e) throws RemoteException {
		this.logicalClock.tickReceiveEvent(e.getTimestamp());
		this.logger.log(e);
		return this.rgs.getObject().logEvent(e);
	}


	@Override
	public String getName() throws RemoteException {

		return this.name;
	}


}
