package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.model.*;
import distributed.systems.gridscheduler.remote.RemoteGridScheduler;
import distributed.systems.gridscheduler.remote.RemoteResourceManager;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public class RemoteGridSchedulerImpl implements RemoteGridScheduler {

	public static final int REGISTRY_PORT = 1099;

	private GridScheduler gs;
	private RemoteGridScheduler stub;
	private Logger logger;

	private LogicalClock logicalClock;


	public RemoteGridSchedulerImpl(String url) {
		this.gs = new GridScheduler(url);
		this.stub = null;
		this.logicalClock = new LamportsClock();


		// TODO: probably want to write this to a file instead
		logger = new Logger(System.out);
	}


	public static void main(String[] args) throws RemoteException {


		if (args.length < 1) {
			System.out.printf("Usage: gradle gs  -Pargv=\"['<clusterUrl>']\"");
			System.exit(1);
		}

		LocateRegistry.createRegistry(REGISTRY_PORT);
		System.out.printf("Started registry on port %d.\n", REGISTRY_PORT);

		String url = args[0];

		System.out.printf("Trying to start Grid Scheduler with URL '%s'\n", url);

		try {
			RemoteGridSchedulerImpl gs = new RemoteGridSchedulerImpl(url);
			RemoteGridScheduler rgs = (RemoteGridScheduler) UnicastRemoteObject.exportObject(gs, 0);

			Registry registry = LocateRegistry.getRegistry();
			registry.bind(url, rgs);

		} catch (RemoteException e) {
			System.out.printf("Remote Exception:\n");
			e.printStackTrace();
		} catch (AlreadyBoundException e) {
			System.out.printf("The url '%s' is already bound.", url);
			System.exit(1);
		}
	}

	public RemoteGridScheduler getStub() throws RemoteException {

		if (this.stub == null) {
			this.stub = (RemoteGridScheduler) UnicastRemoteObject.exportObject(this, 0);
		}

		return this.stub;
	}

	@Override
	public boolean registerResourceManager(RemoteResourceManager rrm) throws RemoteException {
		String logLine = String.format("ResourceManager '%s' joined GridScheduler '%s'.", rrm.getName(), this.getName());
		this.logEvent(new Event.GenericEvent(this.logicalClock, logLine));
		return true;
	}


	@Override
	public boolean registerGridScheduler(RemoteGridScheduler rgs) throws RemoteException {
		return false;
	}


	@Override
	public boolean scheduleJob(Job job, RemoteResourceManager source) throws RemoteException {
		return false;
	}


	@Override
	public boolean logEvent(Event e) {
		this.logger.log(e);
		return true;
	}


	@Override
	public String getName() throws RemoteException {
		return this.gs.getName();
	}

}
