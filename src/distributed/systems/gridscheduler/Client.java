package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.model.Job;
import distributed.systems.gridscheduler.remote.RemoteResourceManager;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Jelmer Mulder
 *         Date: 30/11/2017
 */
public class Client {

	private String name;
	private List<RemoteResourceManager> resourceManagers;
	private int numberOfJobs;
	private long minimumJobDuration, maximumJobDuration;


	public Client() {
		this.resourceManagers = new ArrayList<>();
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
			Registry register = LocateRegistry.getRegistry(registryHost, registryPort);
			lookupResourceManagers(args, 6, register);
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
				this.resourceManagers.add(rrm);
			} catch (NotBoundException ignored){
			};
		}
	}


	public static void main(String[] args) {

		if (args.length < 7) {
			System.out.printf("Usage: gradle client  -Pargv=\"['<name>', '<numberOfJobs>', '<minimumJobDuration>', '<maximumJobDuration>', '<registryHost>', '<registryPort>', '<resourceManager1>' [... ,'resourceManagerN']]\"");
			System.exit(1);
		}

		new Client(args).start();

	}


	private void start() {

		long jobId = 0;


		// Used to implement round-robin for the resource managers
		int resourceManagerIndex = 0;
		int numResourceManagers = this.resourceManagers.size();

		for (int i = 0; i< this.numberOfJobs; i++) {

			int startResourceManagerIndex = resourceManagerIndex;

			long duration = this.minimumJobDuration + (int) (Math.random() * (this.maximumJobDuration - this.minimumJobDuration));
			Job job = new Job(duration, jobId++, null, this.name);

			boolean scheduledJob = false;
			do {
				try {
					RemoteResourceManager rrm = this.resourceManagers.get(resourceManagerIndex % numResourceManagers);

					System.out.printf("Scheduling a job.\n");
					rrm.addJob(job);
					System.out.printf("DONE Scheduling a job.\n");

					scheduledJob = true;
				} catch (RemoteException e) {
					System.out.printf("-------------------------------------------------------------\n");
					e.printStackTrace();
				}
				resourceManagerIndex++;
			} while (!scheduledJob && resourceManagerIndex <= (startResourceManagerIndex+ numResourceManagers));

			try {
				// Sleep a while before creating a new job
				Thread.sleep(100L);
			} catch (InterruptedException e) {
				assert(false) : "Simulation runtread was interrupted";
			}

		}
	}

}
