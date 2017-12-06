package distributed.systems.gridscheduler.remote;

import distributed.systems.gridscheduler.Named;
import distributed.systems.gridscheduler.cache.NodeData;
import distributed.systems.gridscheduler.model.Event;
import distributed.systems.gridscheduler.model.Job;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public interface RemoteResourceManager extends Remote, RemoteLogger {

	// TODO: Merge this with Logger logEvent
	/**
	 * Attempt to log Event e to any of the RemoteLoggers in the list loggers. Will try them in order until it
	 * manages to succesfully log the event.
	 * @param loggers list of all loggers
	 * @param e the event to be logged
	 * @return true if the Event was succesfully logged, false otherwise @return
	 * @throws RemoteException
	 */
	static boolean logEvent(List<Named<RemoteResourceManager>> loggers, Event e) throws RemoteException {

		if (loggers.size() <= 0) {
			System.out.printf("No loggers found.\n");
			return false;
		}
		boolean logSubmitted = false;
		int loggerIndex = 0;
		int numLoggers = loggers.size();

		do {
			try {
				RemoteLogger rgs = (RemoteLogger)loggers.get(loggerIndex).getObject();
				rgs.logEvent(e);
				logSubmitted = true;
			} catch (RemoteException ignored) {}
			loggerIndex++;
		} while (!logSubmitted && loggerIndex < (numLoggers));


		return logSubmitted;
	}

	/**
	 * Check if this ResourceManager is still alive.
	 * @return true if the RM is alive, false otherwise.
	 * @throws RemoteException
	 */
	boolean isAlive() throws RemoteException;

	//TODO: Delete this?
	/**
	 * Return the load of this cluster.
	 * @return the load of this cluster (in number of requests).
	 * @throws RemoteException
	 */
	int getLoad() throws RemoteException;

	/**
	 * Get the capacity of this RM.
	 * @return the capacity of this RM. That is, the number of free nodes minus the amount of queued jobs.
	 * @throws RemoteException
	 */
	int getCapacity() throws RemoteException;

	/**
	 * Register the ResourceManager 'rrm' as a duplicate for this ResourceManager and vice versa. Jobs issued by one RM
	 * will be forwarded (but not processed) to the other, so that in the event of a crash the other RM can take over
	 * these jobs.
	 * @param rrm The RM to register as duplicate.
	 * @return true if succesfully registered as duplicate, false otherwise.
	 * @throws RemoteException
	 */
	boolean registerAsDuplicate(RemoteResourceManager rrm) throws RemoteException;

	/**
	 * Issue a Job to be processed by this ResourceManager.
	 * @param job the job to be processed
	 * @return true if the job was succesfully queued, false otherwise.
	 * @throws RemoteException
	 */
	boolean addJob(Job job) throws RemoteException;

	/**
	 * Issue a Job to be processed by this ResourceManager. Since this is an offloaded job, the RM MUST accept.
	 * @param job the job to be processed
	 * @param issueingGsName name of the GS issueing this offloaded request
	 * @return true if the job was succesfully queued, false otherwise.
	 * @throws RemoteException
	 */
	boolean offloadJob(Job job, String issueingGsName) throws RemoteException;

	/**
	 * Get the name of this ResourceManager
	 * @return the name of this RM.
	 * @throws RemoteException
	 */
	String getName() throws RemoteException;

	/**
	 * Used by Nodes to notify this ResourceManager that a Job has been completed.
	 * @param job the job that has been completed.
	 * @throws RemoteException
	 */
	public void jobDone(Job job) throws RemoteException;

	ArrayList<NodeData> getAllNodes() throws RemoteException;

	NodeData getNode(int index) throws RemoteException;

	Integer getQueueSize() throws RemoteException;

    Integer getQueuedJobs() throws RemoteException;
}
