package distributed.systems.gridscheduler.remote;

import distributed.systems.gridscheduler.Named;
import distributed.systems.gridscheduler.model.Event;
import distributed.systems.gridscheduler.model.Job;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Queue;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public interface RemoteGridScheduler extends Remote, RemoteLogger {

	// TODO: Merge this with Logger logEvent
	/**
	 * Attempt to log Event e to any of the RemoteLoggers in the list loggers. Will try them in order until it
	 * manages to succesfully log the event.
	 * @param loggers list of all loggers
	 * @param e the event to be logged
	 * @return true if the Event was succesfully logged, false otherwise @return
	 * @throws RemoteException
	 */
	static boolean logEvent(List<Named<RemoteGridScheduler>> loggers, Event e) throws RemoteException {

		if (loggers.size() <= 0) {
			System.out.printf("No loggers found.\n");
			return false;
		}
		boolean logSubmitted = false;
		int loggerIndex = 0;
		int numLoggers = loggers.size();

		do {
			try {
				RemoteLogger rgs = loggers.get(loggerIndex).getObject();
				rgs.logEvent(e);
				logSubmitted = true;
			} catch (RemoteException ignored) {}
			loggerIndex++;
		} while (!logSubmitted && loggerIndex < (numLoggers));


		return logSubmitted;
	}

	/**
	 * Registers a ResourceManager with this GridScheduler, allowing the RM to offload jobs to this GS and vice versa.
	 * Also needed to forward logging Events to the GS.
	 * @param rrm The ResourceManager to register.
	 * @param rrmName The name of the ResourceManager to register.
	 * @return
	 * @throws RemoteException
	 */
	boolean registerResourceManager(RemoteResourceManager rrm, String rrmName) throws RemoteException;

	/**
	 * Registers another GridSchedulers with this GridScheduler, so that these can offload jobs to each other.
	 * @param rgs The GS to be registed.
	 * @param rgsName The name of the GridScheduler to register.
	 * @return true if the GS was succesfully registered, false otherwise.
	 * @throws RemoteException
	 */
	boolean registerGridScheduler(RemoteGridScheduler rgs, String rgsName) throws RemoteException;

	/**
	 * Offload a Job to this GridScheduler. The issueing ResourceManager is no longer responsible for this Job. The
	 * GridScheduler will make sure it gets completed and that the issueing Client gets contacted with the results.
	 * @param job the job to be offlaoded
	 * @return true if the Job was succesfully offloaded, false otherwise.
	 * @throws RemoteException
	 */
	boolean offloadJob(Job job) throws RemoteException;

	/**
	 * Get the name of this GridScheduler.
	 * @return the name of this GridScheduler.
	 * @throws RemoteException
	 */
	String getName() throws RemoteException;

	/**
	 * Subscribe too all Events that get logged to this GridScheduler.
	 * @param rl The stub on which all Events will get forwarded.
	 * @param rlName The name corresponding to this stub.
	 * @return true if the RemoteLogger was succesfully subscribed, false otherwise.
	 * @throws RemoteException
	 */
	boolean subscribeToEvents(RemoteLogger rl, String rlName) throws RemoteException;

	/**
	 * Get all GridSchedulers in the Grid.
	 * @return List of named GridSchedulers.
	 * @throws RemoteException
	 * @param name Name of the requesting host (for logging purposes).
	 */
	Queue<Named<RemoteGridScheduler>> getGridSchedulers(String name) throws RemoteException;

	/**
	 * Get all ResourceManagers that are registered with this GridScheduler.
	 * @return List of named ResourceManagers.
	 * @throws RemoteException
	 * @param name Name of the requesting host (for logging purposes).
	 */
	Queue<Named<RemoteResourceManager>> getResourceManagers(String name) throws RemoteException;

}
