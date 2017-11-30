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


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public class RemoteResourceManagerImpl implements RemoteResourceManager, Serializable {


	private RemoteGridScheduler rgs;
	private ResourceManager rm;
	private RemoteResourceManager stub;
	private LogicalClock logicalClock;


	public RemoteResourceManagerImpl(Cluster cluster, RemoteGridScheduler rgs) throws RemoteException {
		this.rm = new ResourceManager(cluster);
		this.rgs = rgs;
		this.stub = null;
		this.logicalClock = new LamportsClock();
		RemoteResourceManager stub = this.getStub();
		this.rgs.registerResourceManager(stub);
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

				Cluster cluster = new Cluster(name, rgs, numberOfNodes);

				RemoteResourceManagerImpl rm = new RemoteResourceManagerImpl(cluster, rgs);

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
	public boolean registerAsDuplicate(RemoteResourceManager rrm) throws RemoteException {
		return true;
	}


	@Override
	public boolean scheduleJob(Job job) throws RemoteException {
		String logLine = String.format("ResourceManager '%s' scheduled job with id=%d and duration=%d.", this.getName(), job.getId(), job.getDuration());
		this.rgs.logEvent(new Event.GenericEvent(this.logicalClock, logLine));
		return true;
	}


	@Override
	public boolean logEvent(Event e) throws RemoteException {
		return this.rgs.logEvent(e);
	}


	@Override
	public String getName() throws RemoteException {
		return this.rm.getName();
	}

}
