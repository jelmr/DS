package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.model.*;
import distributed.systems.gridscheduler.remote.RemoteClient;
import distributed.systems.gridscheduler.remote.RemoteResourceManager;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author Jelmer Mulder
 *         Date: 30/11/2017
 */
public class Client implements RemoteClient {

	private String name;
	private List<Named<RemoteResourceManager>> resourceManagers;
	private int numberOfJobs;
	private long minimumJobDuration, maximumJobDuration;

	private LogicalClock logicalClock;

	private RemoteClient stub;

	private ConcurrentHashMap<String, Boolean> jobCompleted;
	private Registry registry;


	public Client() {
		this.resourceManagers = new ArrayList<>();
		this.logicalClock = new LamportsClock();
		this.jobCompleted = new ConcurrentHashMap<>();
	}


	private Client(String[] args) {
		this();

		this.name = args[0];
		this.numberOfJobs = Integer.parseInt(args[1]);
		this.minimumJobDuration = Long.parseLong(args[2]);
		this.maximumJobDuration = Long.parseLong(args[3]);

		String registryHost = args[4];
		int registryPort = Integer.parseInt(args[5]);

		try {
			registry = LocateRegistry.getRegistry(registryHost, registryPort);
			registry.rebind(name, this.getStub());


			lookupResourceManagers(args, 6, registry);


			Event event = new Event.TypedEvent(this.logicalClock, EventType.CLIENT_REGISTERED_REGISTRY, this.getName(), registryHost, registryPort);
			if (!RemoteResourceManager.logEvent(this.resourceManagers, event)) {
				System.out.printf("Couldn't find any ResourceManagers to log event to...\n");
			}
		} catch (ConnectException e) {
			System.out.printf("Could not connect to RMI registry.\n");
			System.exit(1);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}


	private void lookupResourceManagers(String[] args, int startIndex, Registry register) throws RemoteException {
		for (int i = startIndex; i<args.length; i++) {
			String rmName = args[i];
			try {
				RemoteResourceManager rrm = (RemoteResourceManager) register.lookup(rmName);
				this.resourceManagers.add(new Named<>(rmName, rrm));
			} catch (NotBoundException ignored){
			};
		}
	}


	public static void main(String[] args) throws RemoteException {

		if (args.length < 7) {
			System.out.printf("Usage: gradle client  -Pargv=\"['<name>', '<numberOfJobs>', '<minimumJobDuration>', '<maximumJobDuration>', '<registryHost>', '<registryPort>', '<resourceManager1>' [... ,'resourceManagerN']]\"");
			System.exit(1);
		}

		new Client(args).scheduleAllJobs();
		System.out.printf("REACHED END OF MAIN");


	}


	private void scheduleAllJobs() throws RemoteException {

		long jobNum = 0;


		// Used to implement round-robin for the resource managers
		int resourceManagerIndex = 0;
		int numResourceManagers = this.resourceManagers.size();

		for (int i = 0; i< this.numberOfJobs; i++) {

			int startResourceManagerIndex = resourceManagerIndex;

			String jobId = String.format("%s_%d", this.name, ++jobNum);

			long duration = this.minimumJobDuration + (int) (Math.random() * (this.maximumJobDuration - this.minimumJobDuration));
			Job job = new Job(duration, jobId, null, this.getStub(), this.name);

			resourceManagerIndex = scheduleJob(resourceManagerIndex, numResourceManagers, startResourceManagerIndex, jobId, job);

			try {
				// Sleep a while before creating a new job
				Thread.sleep(100L);
			} catch (InterruptedException e) {
				assert(false) : "Simulation runtread was interrupted";
			}

		}
	}


	private int scheduleJob(int resourceManagerIndex, int numResourceManagers, int startResourceManagerIndex, String jobId, Job job) {
		boolean scheduledJob = false;
		do {
			Named<RemoteResourceManager> namedrrm = this.resourceManagers.get(resourceManagerIndex % numResourceManagers);
			RemoteResourceManager rrm = namedrrm.getObject();
			String rmName = namedrrm.getName();

			try {
				// Log attempt to schedule job
				Event event = new Event.TypedEvent(this.logicalClock, EventType.CLIENT_JOB_SCHEDULE_ATTEMPT, this.getName(), job.getId(), rmName);

				if (!RemoteResourceManager.logEvent(this.resourceManagers, event)) {
					System.out.printf("Couldn't find any ResourceManagers to log event to...\n");
				}


				// Attempt to schedule the job.
				System.out.printf("Scheduled job '%s'.\n", job.getId());
				scheduledJob = rrm.addJob(job);

				if (scheduledJob) {
					this.jobCompleted.put(jobId, false);
					System.out.printf("DONE scheduling job '%s'.\n", job.getId());
				}

			} catch (RemoteException e) {
				// Incase of RemoteException, assume the RM has crashed. Log this.
				Event event = new Event.TypedEvent(this.logicalClock, EventType.CLIENT_DETECTED_CRASHED_RM, this.name, rmName);

				try {
					if (!RemoteResourceManager.logEvent(this.resourceManagers, event)) {
						System.out.printf("Couldn't find any ResourceManagers to log event to...\n");
						// All RMs have crashed, nothing we can do.
						// TODO: Ask GridSchedulers for a RM instead?
						System.exit(1);
					}
				} catch (RemoteException e1) {
					e1.printStackTrace();
				}

				this.resourceManagers.remove(resourceManagerIndex % numResourceManagers);
			}
			resourceManagerIndex++;
		} while (!scheduledJob && resourceManagerIndex <= (startResourceManagerIndex+ numResourceManagers));
		return resourceManagerIndex;
	}


	public boolean hasOutStandingJobs() {
		return this.jobCompleted.size() >0;
	}


	@Override
	public void jobDone(Job job) throws RemoteException {

		System.out.printf("Received response for job '%s'.\n", job.getId());


		Event event = new Event.TypedEvent(this.logicalClock, EventType.CLIENT_JOB_DONE, this.getName(), job.getId());
		if (!RemoteResourceManager.logEvent(this.resourceManagers, event)) {
			System.out.printf("Couldn't find any ResourceManagers to log event to...\n");
		}

		this.jobCompleted.remove(job.getId());


		if (!this.hasOutStandingJobs()) {
			try {
				Event exitEvent = new Event.TypedEvent(this.logicalClock, EventType.CLIENT_EXITING, this.getName());
				if (!RemoteResourceManager.logEvent(this.resourceManagers, exitEvent)) {
					System.out.printf("Couldn't find any ResourceManagers to log event to...\n");
				}

				// TODO: Find out how to make the client properly exit after it has a response for all its jobs.
				this.registry.unbind(this.name);
				UnicastRemoteObject.unexportObject(this.stub, true);
				for (Named<RemoteResourceManager> resourceManager : this.resourceManagers) {
					UnicastRemoteObject.unexportObject(resourceManager.getObject(), true);
				}
				UnicastRemoteObject.unexportObject(this.registry, true);
				this.registry = null;
				this.stub = null;
				this.resourceManagers.clear();
				this.resourceManagers = null;
			} catch (NotBoundException e) {
				e.printStackTrace();
			}
		}
	}

	public RemoteClient getStub() throws RemoteException {

		if (this.stub == null) {
			this.stub = (RemoteClient) UnicastRemoteObject.exportObject(this, 0);
		}

		return this.stub;
	}


	@Override
	public String getName() throws RemoteException {
		return this.name;
	}
}
