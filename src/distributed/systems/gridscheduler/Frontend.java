package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.cache.RGSCache;
import distributed.systems.gridscheduler.cache.RRMCache;
import distributed.systems.gridscheduler.gui.*;
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
import java.util.ArrayList;
import java.util.List;


/**
 * @author Jelmer Mulder & Glenn Visser
 *         Date: 02/12/2017
 *
 *         TODO largest issues, Backend for node availability refreshes to available, why? @()
 *
 */
public class Frontend implements RemoteLogger {

	private List<Named<RemoteGridScheduler>> subscribedGridSchedulers;

	private LogicalClock logicalClock;
	private RemoteLogger stub;
	private Registry registry;
	private String name;

	private GuiHost guiHost;


	public Frontend(String[] args) {
		this.logicalClock = new LamportsClock();

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

		guiHost = new GuiHost();
		prepareGui();
		guiHost.start();

	}

	private void prepareGui() {
		for (Named<RemoteGridScheduler> namedRGS : this.subscribedGridSchedulers) {
			try {
				RemoteGridScheduler rgs = namedRGS.getObject();
				prepareRGSGui(rgs.getName(), rgs);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	private void prepareRGSGui(String rgsName, RemoteGridScheduler rgs) {
		RGSCache rgsCache = new RGSCache(rgsName, rgs);
		GSFrame frame = new GSFrame(rgsCache);
		guiHost.addRGS(rgsName, frame, rgsCache);

		try {
			for (Named<RemoteResourceManager> namedRRM : rgs.getResourceManagers(this.name)) {
				RemoteResourceManager rrm = namedRRM.getObject();
				prepareRRMGui(rgsName, rrm.getName(), rrm);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void prepareRRMGui(String rgsName, String rrmName, RemoteResourceManager rrm) {
		RRMCache rrmCache = new RRMCache(rrmName, rrm);
		RMStatusPanel panel = new RMStatusPanel(rrmCache);
		guiHost.addRRM(rgsName, rrmName, panel, rrmCache);
	}

	@Override
	public boolean logEvent(Event e) throws RemoteException {
		if (!(e instanceof Event.TypedEvent)) {
			// Can only process TypedEvents
			System.out.printf("%10s : %s\n", "[Info]", String.format("Untyped event (%s)", e.getLogString()));
			return false;
		}

		Event.TypedEvent typedEvent = ((Event.TypedEvent) e);
		EventType type = typedEvent.getType();
		Object[] args = typedEvent.getArgs();

		Thread thread = null;
		switch(type) {

			case REGISTRY_START:
				//System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				break;
			case RM_SCHEDULED_JOB_ON_NODE:
				System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				thread = new Thread(() -> handleRMScheduledNode(args));
				break;

			case RM_FINISHED_JOB:
				System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				thread = new Thread(() -> handRMFinishedJob(args));
				break;

			case CLIENT_REGISTERED_REGISTRY:
				//System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				break;

			case RM_REGISTERED_REGISTRY:
				//System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				break;

			case GS_REGISTERED_REGISTRY:
				//System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				break;

			case FRONTEND_REGISTERED_REGISTRY:
				//System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				// Whoo hoo that's us!
				break;

			case GS_ACCEPTS_RM_REGISTRATION:
				System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				thread = new Thread(() -> handleRMRegistersGS(args));
				break;

			case RM_REGISTERS_WITH_GS:
				//System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				break;

			case FRONTEND_SUBSCRIBE_TO_EVENTS_ATTEMPT:
				//System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				break;

			case GS_ACCEPTS_FRONTEND_SUBSCRIPTION:
				//System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				break;

			case RM_REGISTERED_AS_DUPLICATE:
				//System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				break;

			case GS_ACCEPTS_GS_REGISTRATION:
				System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				thread = new Thread(() -> handleGSAcceptGS(args));
				break;

			case GS_SEND_LIST_GS:
				//System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				break;

			case GS_SEND_LIST_RM:
				//System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				break;

			case CLIENT_JOB_SCHEDULE_ATTEMPT:
				//System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				break;

			case CLIENT_DETECTED_CRASHED_RM:
				System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				thread = new Thread(() -> handleClientDetectCrashedRM(args));
				break;

			case RM_RECEIVED_JOB_REQUEST:
				System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				guiHost.updateRRM(((String) args[0]));
				break;

			case RM_QUEUED_JOB:
				System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				thread = new Thread(() -> handleRMQueuedJob(args));
				break;

			case RM_OFFLOAD_TO_GS_ATTEMPT:
				//System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				break;

			case CLIENT_JOB_DONE:
				//System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				break;

			case CLIENT_EXITING:
				//System.out.printf("%10s : %s\n", "[Info]", String.format(type.getFormatString(), args));
				break;
		}
		if (thread != null) {
			thread.start();
		}

		return false;
	}

	private void handleRMQueuedJob(Object[] args) {
		String rrmName = ((String) args[0]);
		try {
			guiHost.updateRRM(rrmName);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void handleClientDetectCrashedRM(Object[] args) {
		String rrmName = (String) args[1];
		guiHost.reportDeadRRM(rrmName);
	}

	private void handleGSAcceptGS(Object[] args) {
		String rgsName = (String) args[1];

		try {
			RemoteGridScheduler rgs = (RemoteGridScheduler) registry.lookup(rgsName);
			RGSCache rgsCache = new RGSCache(rgsName, rgs);
			GSFrame frame = new GSFrame(rgsCache);
			guiHost.addRGS(rgsName, frame, rgsCache);

		} catch (RemoteException | NotBoundException | ClassCastException e) {
			e.printStackTrace();
		}
	}

	private void handRMFinishedJob(Object[] args) {
		String rrmName = (String) args[0];
		try {
			guiHost.updateRRM(rrmName);
			guiHost.updateNodes(rrmName);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void handleRMScheduledNode(Object[] args) {
		String rrmName = (String) args[0];
		String nodeName = (String) args[2];
		NodeStatus nodeStatus = NodeStatus.Busy;
		guiHost.updateNode(rrmName, nodeName, nodeStatus);
	}

	private void handleRMRegistersGS(Object[] args) {
		String rgsName = (String) args[0];
		String rrmName = (String) args[1];

		boolean searching = true;
		int tries = 0;
		while (searching && tries < 5) {
			try {
				RemoteResourceManager rrm = (RemoteResourceManager) registry.lookup(rrmName);
				RRMCache rrmCache = new RRMCache(rrmName, rrm);
				RMStatusPanel panel = new RMStatusPanel(rrmCache);
				guiHost.addRRM(rgsName, rrmName, panel, rrmCache);
				searching = false;

			} catch (RemoteException | ClassCastException e) {
				e.printStackTrace();
				tries++;
			} catch (NotBoundException e) {
				tries++;
			}
			if (searching) {
				sleepExpBackoff(tries);
			}
		}
	}

	private void sleepExpBackoff(int exponent) {
		try {
            Thread.sleep((long) (Math.pow(2, exponent) * 100));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
	}


	public static void main(String[] args) {
		if (args.length < 4) {
			System.out.printf("Usage: gradle frontend -Pargv=\"['<name>', '<registryHost>', '<registryPort>', '<gridScheduler1>' ]\"");
			System.exit(1);
		}

		new Frontend(args).start();
	}

	private void start() {

		for (Named<RemoteGridScheduler> gs : this.subscribedGridSchedulers) {

			try {
				this.logicalClock.tickSendEvent();
				Event.TypedEvent e = new Event.TypedEvent(this.logicalClock, EventType.FRONTEND_SUBSCRIBE_TO_EVENTS_ATTEMPT, this.name, gs.getName());
				RemoteGridScheduler.logEvent(this.subscribedGridSchedulers, e);
				gs.getObject().subscribeToEvents(this.getStub(), this.name);
			} catch (RemoteException e) {
				this.subscribedGridSchedulers.remove(gs);
			}
		}
	}


	public RemoteLogger getStub() throws RemoteException {

		if (this.stub == null) {
			this.stub = (RemoteLogger) UnicastRemoteObject.exportObject(this, 0);
		}

		return this.stub;
	}
}
