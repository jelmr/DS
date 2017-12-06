package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.model.Event;
import distributed.systems.gridscheduler.model.EventType;
import distributed.systems.gridscheduler.model.LamportsClock;
import distributed.systems.gridscheduler.model.LogicalClock;
import distributed.systems.gridscheduler.remote.RemoteClient;
import distributed.systems.gridscheduler.remote.RemoteGridScheduler;
import distributed.systems.gridscheduler.remote.RemoteLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Jelmer Mulder
 *         Date: 02/12/2017
 */
public class TextFrontend implements RemoteLogger {

	private List<Named<RemoteGridScheduler>> subscribedGridSchedulers;

	private LogicalClock logicalClock;
	private RemoteLogger stub;
	private Registry registry;
	private String name;
	private QueuedLogger logger;


	public TextFrontend(String[] args) {
		this.logicalClock = new LamportsClock();

		try {
			PrintStream ps = new PrintStream(new FileOutputStream(new File("test.log")));
			logger = new QueuedLogger(ps);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			logger = new QueuedLogger(System.out);

		}

		this.name = args[0];

		String registryHost = args[1];
		int registryPort = Integer.parseInt(args[2]);

		String rgsName = args[3];

		try {
			registry = LocateRegistry.getRegistry(registryHost, registryPort);
			registry.rebind(name, this.getStub());

			RemoteGridScheduler rgs = (RemoteGridScheduler) registry.lookup(rgsName);

			// Fill subscribedGridSchedulers with a list of all known GridSchedulers
			this.subscribedGridSchedulers = new ArrayList<>();
			this.subscribedGridSchedulers.add(new Named<>(rgsName, rgs));
			this.subscribedGridSchedulers.addAll(rgs.getGridSchedulers(name));


			Event event = new Event.TypedEvent(this.logicalClock, EventType.FRONTEND_REGISTERED_REGISTRY, this.name, registryHost, registryPort);
			if (!RemoteGridScheduler.logEvent(this.subscribedGridSchedulers, event)) {
				System.out.printf("Couldn't find any GridSchedulers to log event to...\n");
			}
		} catch (ConnectException e) {
			System.out.printf("Could not connect to RMI registry.\n");
			System.exit(1);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			System.out.printf("Could not find GridScheduler '%s'.\n", rgsName);
			System.exit(1);
		}
	}


	@Override
	public boolean logEvent(Event e) throws RemoteException {

		this.logger.log(e);

		return true;
	}


	public static void main(String[] args) {
		if (args.length < 4) {
			System.out.printf("Usage: gradle frontend -Pargv=\"['<name>', '<registryHost>', '<registryPort>', '<gridScheduler1>' ]\"");
			System.exit(1);
		}

		new TextFrontend(args).start();
	}


	private void start() {
		for (Named<RemoteGridScheduler> gs : this.subscribedGridSchedulers) {
			try {
				Event.TypedEvent e = new Event.TypedEvent(this.logicalClock, EventType.FRONTEND_SUBSCRIBE_TO_EVENTS_ATTEMPT, this.name, gs.getName());
				RemoteGridScheduler.logEvent(this.subscribedGridSchedulers, e);
				gs.getObject().subscribeToEvents(this.getStub(), this.name);
			} catch (RemoteException e) {
				this.subscribedGridSchedulers.remove(gs);
			}
		}

        System.out.println("Jelmer is up");
	}


	public RemoteLogger getStub() throws RemoteException {

		if (this.stub == null) {
			this.stub = (RemoteLogger) UnicastRemoteObject.exportObject(this, 0);
		}

		return this.stub;
	}
}

