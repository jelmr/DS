package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.model.*;
import distributed.systems.gridscheduler.remote.RemoteGridScheduler;
import distributed.systems.gridscheduler.remote.RemoteLogger;
import distributed.systems.gridscheduler.remote.RemoteResourceManager;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
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


	// TODO: Remove this redundancy with the above 2 hashmaps
	private List<Named<RemoteResourceManager>> registeredResourceManagers;

	private List<Named<RemoteGridScheduler>> registeredGridSchedulers;




	private long pollSleep = 1000;

	// polling thread
	private Thread pollingThread;
	private boolean running;



	private ConcurrentLinkedQueue<Named<RemoteLogger>> subscribedLoggers;

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
		this.subscribedLoggers = new ConcurrentLinkedQueue<>();

		this.registeredGridSchedulers = new ArrayList<>();
		this.registeredResourceManagers = new ArrayList<>();


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

		// TODO: Need to broadcast to any other servers?
		this.registeredResourceManagers.add(new Named<>(rrmName, rrm));
		this.resourceManagers.put(rrmName, rrm);
		this.resourceManagerLoad.put(rrmName, 0);
		return true;
	}


	@Override
	public boolean registerGridScheduler(RemoteGridScheduler rgs, String rgsName) throws RemoteException {
		this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.GS_ACCEPTS_GS_REGISTRATION, this.getName(), rgsName));

		// TODO: Need to broadcast to any other servers?
		this.registeredGridSchedulers.add(new Named<>(rgsName, rgs));
		return true;
	}


	@Override
	public boolean offloadJob(Job job) throws RemoteException {
		// TODO: Change to take CLient as source i.s.o RM?
		String minLoadRM = getLeastLoadedResourceManager();
		RemoteResourceManager rrm = this.resourceManagers.get(minLoadRM);
		rrm.addJob(job);

		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO
		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO
		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO
		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO
		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO
		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO
		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO
		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO
		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO
		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO
		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO
		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO
		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO
		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO
		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO
		// TODO: MAKE THIS OFFLOAD TO OTHER CLUSTERS TOO

		return true;
	}


	private String getLeastLoadedResourceManager() {
		int minLoad = Integer.MAX_VALUE;
		String minLoadRM = null;
		for (String rmName : this.resourceManagerLoad.keySet()) {
			int load = this.resourceManagerLoad.get(rmName);

			if (load <= minLoad) {
				minLoad = load;
				minLoadRM = rmName;

			}
		}
		return minLoadRM;
	}


	@Override
	public boolean logEvent(Event e) {
		for (Named<RemoteLogger> subscribedLogger : this.subscribedLoggers) {
			try {
				subscribedLogger.getObject().logEvent(e);
			} catch (RemoteException e1) {
				// If a RemoteException occured, assume the subscriber logger has exited and remove it from the list.
				subscribedLoggers.remove(subscribedLogger);
			}
		}
		this.logger.log(e);
		return true;
	}


	@Override
	public String getName() throws RemoteException {
		return this.name;
	}


	@Override
	public boolean subscribeToEvents(RemoteLogger rl, String rlName) throws RemoteException {
		this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.GS_ACCEPTS_FRONTEND_SUBSCRIPTION, this.name, rlName));
		this.subscribedLoggers.add(new Named<>(rlName, rl));
		return true;
	}


	@Override
	public List<Named<RemoteGridScheduler>> getGridSchedulers(String name) throws RemoteException {
		this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.GS_SEND_LIST_GS, this.name, name));
		return this.registeredGridSchedulers;
	}


	@Override
	public List<Named<RemoteResourceManager>> getResourceManagers(String name) throws RemoteException {
		this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.GS_SEND_LIST_RM, this.name, name));
		return this.registeredResourceManagers;
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
