package distributed.systems.gridscheduler.remote;

import distributed.systems.gridscheduler.Named;
import distributed.systems.gridscheduler.model.Job;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public interface RemoteGridScheduler extends Remote, RemoteLogger {

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
	 * @return true if the GS was succesfully registered, false otherwise.
	 * @throws RemoteException
	 */
	boolean registerGridScheduler(RemoteGridScheduler rgs) throws RemoteException;

	/**
	 * Offload a Job to this GridScheduler. The issueing ResourceManager is no longer responsible for this Job. The
	 * GridScheduler will make sure it gets completed and that the issueing Client gets contacted with the results.
	 * @param job the job to be offlaoded
	 * @param source the RM that is attempting to offload the Job.
	 * @return true if the Job was succesfully offloaded, false otherwise.
	 * @throws RemoteException
	 */
	boolean offloadJob(Job job, RemoteResourceManager source) throws RemoteException;

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
	 */
	List<Named<RemoteGridScheduler>> getGridSchedulers() throws RemoteException;

	/**
	 * Get all ResourceManagers that are registered with this GridScheduler.
	 * @return List of named ResourceManagers.
	 * @throws RemoteException
	 */
	List<Named<RemoteResourceManager>> getResourceManagers() throws RemoteException;

}
