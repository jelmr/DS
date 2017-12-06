package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.model.*;
import distributed.systems.gridscheduler.remote.RemoteGridScheduler;
import distributed.systems.gridscheduler.remote.RemoteLogger;
import distributed.systems.gridscheduler.remote.RemoteResourceManager;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */

/**
 * In hind sight, it would have been much nicer to make a wrapper around RemoteGridScheduler and
 * RemoteResourceManager to perform some caching for us. This would have allowed us to use a
 * single data-structure to store everything we need to know about these remotes, instead of
 * spreading this data out over a bunch of separate Queues and HashMaps.
 */
public class RemoteGridSchedulerImpl implements RemoteGridScheduler , Runnable{

	public static final int REGISTRY_PORT = 1099;
	public static final int RM_CAPACITY_CACHE_DURATION = 1000;
	public static final int GS_CAPACITY_CACHE_DURATION = 1000;

	// job queue
	private ConcurrentLinkedQueue<Job> jobQueue;

	// local name
	private final String name;




	// TODO: Merge these two into one HashMap? Careful not to rely on rm.getName(), since that is remote operation.
	// TODO: delete load?
	// a hashmap linking each resource manager to an estimated load
	private ConcurrentHashMap<String, Integer> resourceManagerLoad;

	private ConcurrentHashMap<String, Integer> resourceManagerCapacity;
	private long lastRMCapacityUpdate;

	// name -> stub
	private ConcurrentHashMap<String, RemoteResourceManager> resourceManagers;


	// TODO: Remove this redundancy with the above 2 hashmaps
	private final ConcurrentLinkedQueue<Named<RemoteResourceManager>> registeredResourceManagers;

	private final ConcurrentLinkedQueue<Named<RemoteGridScheduler>> registeredGridSchedulers;

	private ConcurrentHashMap<String, Integer> gridSchedulerCapacity;
	private long lastGSCapacityUpdate;



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

		this.registeredGridSchedulers = new ConcurrentLinkedQueue<>();
		this.registeredResourceManagers = new ConcurrentLinkedQueue<>();

		this.resourceManagerCapacity = new ConcurrentHashMap<>();
		this.lastRMCapacityUpdate = 0;

		this.gridSchedulerCapacity = new ConcurrentHashMap<>();
		this.lastGSCapacityUpdate = 0;


		// start the polling thread
		running = true;
		pollingThread = new Thread(this);
		pollingThread.start();
	}


	public static void main(String[] args) throws RemoteException {


		if (args.length < 3) {
			System.out.printf("Usage: gradle gs  -Pargv=\"['<name>', '<registryHost>', '<registryPort>' [, '<peerGsName1>, ... ,'<peerGsNameN>']]\"");
			System.exit(1);
		}

		String name = args[0];
		new RemoteGridSchedulerImpl(name).start(args);
	}


	private void start(String[] args) throws RemoteException {
		String name = this.getName();


		String registryHost = args[1];
		int registryPort = Integer.parseInt(args[2]);


		try {
			// Register self in registry
			RemoteGridScheduler rgs = this.getStub();
			Registry registry = LocateRegistry.getRegistry(registryHost, registryPort);
			registry.rebind(name, rgs);
			this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.GS_REGISTERED_REGISTRY, this.getName(), registryHost, REGISTRY_PORT));


			// Connect to peer GS, add them all to registeredGridScheduler queue.
			for (int i = 3; i < args.length; i++) {
				String peerGsName = args[i];
				try {
					RemoteGridScheduler peerGs = (RemoteGridScheduler) registry.lookup(peerGsName);
					Queue<Named<RemoteGridScheduler>> gridSchedulers = peerGs.getGridSchedulers(this.getName());

					synchronized (this.registeredGridSchedulers) {
						Named<RemoteGridScheduler> e = new Named<>(peerGsName, peerGs);
						if (!this.registeredGridSchedulers.contains(e)) {
							this.registeredGridSchedulers.add(e);
						}

						for (Named<RemoteGridScheduler> gridScheduler : gridSchedulers) {
							if (!this.registeredGridSchedulers.contains(gridScheduler)) {
								this.registeredGridSchedulers.add(gridScheduler);
							}
						}
					}
				} catch (NotBoundException | ConnectException e) {
					// TODO: Exit or operate solo?
				}
			}

			// Register all found GS as peers.
			for (Named<RemoteGridScheduler> registeredGridScheduler : this.registeredGridSchedulers) {
				if (!registeredGridScheduler.getName().equals(this.name)) {

					try {
						registeredGridScheduler.getObject().registerGridScheduler(this.getStub(), this.getName());
					} catch (java.rmi.ConnectException e) {
						// This host is apparantly dead, remove it
						this.registeredGridSchedulers.remove(registeredGridScheduler);
					}
				}
			}




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
		RemoteResourceManager rrm = this.resourceManagers.get(name);
		if (rrm != null) {
			return rrm;
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
		synchronized (this.registeredGridSchedulers) {
			Named<RemoteGridScheduler> e = new Named<>(rgsName, rgs);
			if (this.registeredGridSchedulers.contains(e)){
				this.registeredGridSchedulers.remove(e);
			}
			this.registeredGridSchedulers.add(e);
		}
		return true;
	}


	@Override
	public boolean offloadJob(Job job) throws RemoteException {
		if (this.lastRMCapacityUpdate+ RM_CAPACITY_CACHE_DURATION < System.currentTimeMillis()) {
			this.updateRMCapacities();
		}
		if (this.lastGSCapacityUpdate+ GS_CAPACITY_CACHE_DURATION < System.currentTimeMillis()) {
			this.updateGSCapacities();
		}

		boolean wasOffloadedBefore = job.hasBeenOffloaded();
		System.out.printf("Received a job to offload. ---------------------------\n");
		// Mark this job as having been offloaded to prevent offload-cycles.
		job.setHasBeenOffloaded(true);


		// Keep trying the highestCapacityGS until we run out of GSs or we succeed.
		boolean jobDispatched = false;
		String targetGsName = this.getHighestCapacityGS();
		System.out.printf("highest cap GS: %s\n", targetGsName);
		do {

			RemoteGridScheduler targetGs = this.getGridSchedulerByName(targetGsName);

			// If no other GS to offload to, or if we are the highest capacity GS keep it within this GS.
			if (wasOffloadedBefore
					|| targetGsName == null
					|| targetGs == null
					|| this.name.equals(targetGsName)
					|| this.resourceManagerCapacity.get(this.getHighestCapacityRM()) > this.gridSchedulerCapacity.get(targetGsName)) {
				System.out.printf("This GS is null or is us\n");

				// Keep trying the highestCapacityRM until we run out of RMs or we succeed.
				do {
					String targetRmName = this.getHighestCapacityRM();
					System.out.printf("Highest cap RM: %s\n", targetRmName);
					try {
						if (targetRmName == null || !this.resourceManagers.containsKey(targetRmName)) {
							throw new RemoteException();	// ugly but whatever. Make sure we execute the catch.
						}
						RemoteResourceManager targetRm = this.resourceManagers.get(targetRmName);
						targetRm.offloadJob(job, this.name);
						jobDispatched = true;

					} catch (RemoteException e) {
						this.removeResourceManager(targetRmName);
					}
				} while (this.resourceManagers.size() > 0 && !jobDispatched);

				// If we found some other GS with the highest capacity
			} else {
				targetGs.offloadJob(job);
				jobDispatched = true;
			}
		} while (this.gridSchedulerCapacity.size() > 0 && !jobDispatched);

		return jobDispatched;
	}


	private String getHighestCapacityRM() {
		System.out.printf("Checking highest capacity RM");
		int maxCapacity = Integer.MIN_VALUE;
		String maxCapacityRM = null;
		for (String rmName : this.resourceManagerCapacity.keySet()) {

			int capacity = this.resourceManagerCapacity.get(rmName);
			System.out.printf("%s: %d\n", rmName, capacity);
			if (capacity >= maxCapacity) {
				maxCapacity = capacity;
				maxCapacityRM = rmName;

			}
		}
		System.out.printf("%s has highest cap!", maxCapacity);
		return maxCapacityRM;
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
	public Queue<Named<RemoteGridScheduler>> getGridSchedulers(String name) throws RemoteException {
		this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.GS_SEND_LIST_GS, this.name, name));
		return this.registeredGridSchedulers;
	}


	@Override
	public Queue<Named<RemoteResourceManager>> getResourceManagers(String name) throws RemoteException {
		this.logEvent(new Event.TypedEvent(this.logicalClock, EventType.GS_SEND_LIST_RM, this.name, name));
		return this.registeredResourceManagers;
	}


	@Override
	public int getHighestCapacityRMCapacity() throws RemoteException {
		if (this.lastRMCapacityUpdate+ RM_CAPACITY_CACHE_DURATION < System.currentTimeMillis()) {
			this.updateRMCapacities();
		}

		if (this.resourceManagerCapacity.size() <= 0) {
			// TODO: Throw some exception / log something?
			return Integer.MIN_VALUE;
		}

		int highestCapacity = Integer.MIN_VALUE;

		for (String rmName : this.resourceManagerCapacity.keySet()) {
			int capacity = this.resourceManagerCapacity.get(rmName);

			if (capacity > highestCapacity) {
				highestCapacity = capacity;
			}
		}

		return highestCapacity;
	}



	private void updateRMCapacities() {
		System.out.printf("Updating Rm capactieis");
		this.lastRMCapacityUpdate = System.currentTimeMillis();
		for (String rmName : this.resourceManagers.keySet()) {
			RemoteResourceManager rm = this.resourceManagers.get(rmName);
			try {
				int capacity = rm.getCapacity();
				System.out.printf("RM %s: %d", rmName, capacity);
				this.resourceManagerCapacity.put(rmName, capacity);
			} catch (RemoteException e) {
				this.removeResourceManager(rmName);
			}
		}
		System.out.printf("Done updating RM capacities");
	}
	//@Override
	public String getHighestCapacityGS() throws RemoteException {
		if (this.lastGSCapacityUpdate+ GS_CAPACITY_CACHE_DURATION < System.currentTimeMillis()) {
			System.out.printf("Updating GS capacities !!!\n");
			this.updateGSCapacities();
		}

		if (this.gridSchedulerCapacity.size() <= 0) {
			// TODO: Throw some exception / log something?
			return null;
		}

		int highestCapacity = Integer.MIN_VALUE;
		String highestCapacityName = null;

		System.out.printf("Getting highest cap GS:");
		for (String gsName : this.gridSchedulerCapacity.keySet()) {
			if (gsName.equals(this.name)) {
				System.out.printf("Skipping self");
				// Skip self
				continue;
			}

			int capacity = this.gridSchedulerCapacity.get(gsName);
			System.out.printf("GS from cache: %s: %d\n", gsName, capacity);

			if (capacity > highestCapacity) {
				highestCapacity = capacity;
				highestCapacityName = gsName;
			}
		}

		System.out.printf("Highest cap GS from cache is %s\n", highestCapacityName);
		return highestCapacityName;
	}

	private void updateGSCapacities() {
		System.out.printf("Updating GS capacities");
		this.lastGSCapacityUpdate = System.currentTimeMillis();
		for (Named<RemoteGridScheduler> namedGs : this.registeredGridSchedulers) {
			String gsName = namedGs.getName();
			RemoteGridScheduler gs = namedGs.getObject();
			try {
				int capacity = gs.getHighestCapacityRMCapacity();
				System.out.printf("GS %s: %d\n", gsName, capacity);
				this.gridSchedulerCapacity.put(gsName, capacity);
			} catch (RemoteException e) {
				this.removeGridScheduler(gsName);
			}
		}
		System.out.printf("Done updating GS capacities\n");
	}

	private RemoteGridScheduler getGridSchedulerByName(String name) {
		for (Named<RemoteGridScheduler> gs : this.registeredGridSchedulers) {
			if (gs.getName().equals(name)) {
				return gs.getObject();
			}
		}

		return null;
	}


	/**
	 * Gets the number of jobs that are waiting for completion.
	 * @return
	 */
	public int getWaitingJobs() throws RemoteException {
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

	private void removeGridScheduler(String name) {
		this.registeredGridSchedulers.remove(new Named<RemoteGridScheduler>(name, null));
		this.gridSchedulerCapacity.remove(name);
	}

	private void removeResourceManager(String name) {
		this.resourceManagerLoad.remove(name);
		this.resourceManagers.remove(name);
		this.registeredResourceManagers.remove(new Named<RemoteResourceManager>(name, null));
		this.resourceManagerCapacity.remove(name);
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
