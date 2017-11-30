package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.model.Cluster;
import distributed.systems.gridscheduler.model.Job;
import distributed.systems.gridscheduler.model.ResourceManager;
import distributed.systems.gridscheduler.remote.RemoteGridScheduler;
import distributed.systems.gridscheduler.remote.RemoteResourceManager;
import java.io.Serializable;
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



	private ResourceManager rm;
	private RemoteResourceManager stub;


	public RemoteResourceManagerImpl(Cluster cluster) {
		this.rm = new ResourceManager(cluster);
		this.stub = null;
	}


	public static void main(String[] args) {

		if (args.length < 5) {

			System.out.printf("Usage: gradle rm  -Pargv=\"['<clusterName>', '<numberOfNodes>', '<registryHost>', '<registryPort>', '<gridScheduler1>' [... ,'gridSchedulerN']]\"");


			System.exit(1);
		}

		String clusterName = args[0];
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

				Cluster cluster = new Cluster(clusterName, rgs, numberOfNodes);

				RemoteResourceManagerImpl rrm = new RemoteResourceManagerImpl(cluster);
				RemoteResourceManager stub = rrm.getStub();

				rgs.registerResourceManager(stub);

				System.out.printf("RRM exiting.\n");
				System.exit(0);

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
	public boolean registerAsDuplicate(RemoteResourceManager rrm) throws RemoteException {
		return true;
	}


	@Override
	public boolean scheduleJob(Job job) throws RemoteException {
		return true;
	}


	@Override
	public String getName() throws RemoteException {
		return this.rm.getName();
	}

}
