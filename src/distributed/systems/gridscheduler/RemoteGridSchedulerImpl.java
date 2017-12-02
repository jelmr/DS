package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.model.*;
import distributed.systems.gridscheduler.remote.RemoteGridScheduler;
import distributed.systems.gridscheduler.remote.RemoteResourceManager;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public class RemoteGridSchedulerImpl implements RemoteGridScheduler , Runnable{

	public static final int REGISTRY_PORT = 1099;



	// job queue
	private ConcurrentLinkedQueue<Job> jobQueue;

	// local name
	private final String name;


	// TODO: Merge these two into one HashMap? Careful not to rely on rm.getName(), since that is remote operation.
	// a hashmap linking each resource manager to an estimated load
	private ConcurrentHashMap<String, Integer> resourceManagerLoad;

	// name -> stub
	private ConcurrentHashMap<String, RemoteResourceManager> resourceManagers;



	private long pollSleep = 1000;

	// polling thread
	private Thread pollingThread;
	private boolean running;



	private LogicalClock logicalClock;
	private RemoteGridScheduler stub;
	private QueuedLogger logger;
	private Registry registry;


	public RemoteGridSchedulerImpl(String name) {

		this.stub = null;
		this.logicalClock = new LamportsClock();


		// TODO: probably want to write this to a file instead
		logger = new QueuedLogger(System.out);



		// init members
		this.name = name;
		this.resourceManagerLoad = new ConcurrentHashMap<>();
		this.resourceManagers = new ConcurrentHashMap<>();
		this.jobQueue = new ConcurrentLinkedQueue<>();


		// start the polling thread
		running = true;
		pollingThread = new Thread(this);
		pollingThread.start();
	}


	public static void main(String[] args) throws RemoteException {


		if (args.length < 1) {
			System.out.printf("Usage: gradle gs  -Pargv=\"['<clusterUrl>']\"");
			System.exit(1);
		}

		String url = args[0];
		new RemoteGridSchedulerImpl(url).start();
	}


	private void start() throws RemoteException {
		String name = this.getName();

		registry = LocateRegistry.createRegistry(REGISTRY_PORT);

		// TODO: Get proper IP instead of localhost
		String registryHost = "127.0.0.1";
		this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.REGISTRY_START, registryHost, REGISTRY_PORT));


		try {
			RemoteGridScheduler rgs = this.getStub();
			Registry registry = LocateRegistry.getRegistry();
			registry.rebind(name, rgs);
			this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.GS_REGISTERED_REGISTRY, this.getName(), registryHost, REGISTRY_PORT));

		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}


	public RemoteGridScheduler getStub() throws RemoteException {

		if (this.stub == null) {
			this.stub = (RemoteGridScheduler) UnicastRemoteObject.exportObject(this, 0);
		}

		return this.stub;
	}

	public RemoteResourceManager getResourceManager(String name) throws RemoteException{
		if (this.resourceManagers.contains(name)) {
			return this.resourceManagers.get(name);
		} else {
			try {
				return (RemoteResourceManager) this.registry.lookup(name);
			} catch (NotBoundException e) {
				return null;
			}
		}
	}

	@Override
	public boolean registerResourceManager(RemoteResourceManager rrm, String rrmName) throws RemoteException {
		this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.GS_ACCEPTS_RM_REGISTRATION, this.getName(), rrmName));

		// TODO: Implement
		return true;
	}


	@Override
	public boolean registerGridScheduler(RemoteGridScheduler rgs) throws RemoteException {
		// TODO: Implement
		return false;
	}


	@Override
	public boolean offloadJob(Job job, RemoteResourceManager source) throws RemoteException {
		// TODO: Change to take CLient as source i.s.o RM?
		// TODO: Implement
		return false;
	}


	@Override
	public boolean logEvent(Event e) {
		this.logger.log(e);
		return true;
	}


	@Override
	public String getName() throws RemoteException {
		return this.name;
	}

	/**
	 * Gets the number of jobs that are waiting for completion.
	 * @return
	 */
	public int getWaitingJobs() {
		return jobQueue.size();
	}

	// finds the least loaded resource manager and returns its name
	private String getLeastLoadedRM() {
		String ret = null;
		int minLoad = Integer.MAX_VALUE;

		// loop over all resource managers, and pick the one with the lowest load
		for (String key : resourceManagerLoad.keySet())
		{
			if (resourceManagerLoad.get(key) <= minLoad)
			{
				ret = key;
				minLoad = resourceManagerLoad.get(key);
			}
		}

		return ret;
	}

	private void removeResourceManager(String name) {
		this.resourceManagerLoad.remove(name);
		this.resourceManagers.remove(name);
	}

	@Override
	public void run() {
		while (running) {
			// send a message to each resource manager, requesting its load
			for (String name : resourceManagerLoad.keySet())
			{
				try {
					// TODO: Make this asynchronous.
					RemoteResourceManager rm = this.getResourceManager(name);
					this.resourceManagerLoad.put(name, rm.getLoad());

				} catch (RemoteException ignored) {}
			}

			// schedule waiting messages to the different clusters
			for (Job job : jobQueue)
			{
				String leastLoadedRM =  getLeastLoadedRM();

				if (leastLoadedRM!=null) {


					boolean jobSubmitted = false;
					do {
						try {
							this.getResourceManager(leastLoadedRM).addJob(job);
							jobSubmitted = true;
						} catch (RemoteException e) {
							// TODO: Check if the RM really is unavailable
							this.removeResourceManager(name);	 // This RM is not accessible? Remove it.
						}

						leastLoadedRM = getLeastLoadedRM();
					} while (!jobSubmitted);


					jobQueue.remove(job);

					// increase the estimated load of that RM by 1 (because we just added a job)
					int load = resourceManagerLoad.get(leastLoadedRM);
					resourceManagerLoad.put(leastLoadedRM, load+1);

				}

			}

			// sleep
			try
			{
				Thread.sleep(pollSleep);
			} catch (InterruptedException ex) {
				assert(false) : "Grid scheduler runtread was interrupted";
			}

		}

	}

	/**
	 * Polling thread runner. This thread polls each resource manager in turn to get its load,
	 * then offloads any job in the waiting queue to that resource manager
	 */


	/**
	 * Stop the polling thread. This has to be called explicitly to make sure the program
	 * terminates cleanly.
	 *
	 */
	public void stopPollThread() {
		running = false;
		try {
			pollingThread.join();
		} catch (InterruptedException ex) {
			assert(false) : "Grid scheduler stopPollThread was interrupted";
		}

	}
}
