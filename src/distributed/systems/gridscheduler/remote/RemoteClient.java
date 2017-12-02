package distributed.systems.gridscheduler.remote;

import distributed.systems.gridscheduler.model.Event;
import distributed.systems.gridscheduler.model.Job;
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public interface RemoteClient extends Remote {

	/**
	 * Returns the 'results' of a Job to the issueing Client.
	 * @param job The job that has completed.
	 * @throws RemoteException
	 */
	void jobDone(Job job) throws RemoteException;

	/**
	 * Get the name of this Client.
	 * @return The name of this Client.
	 * @throws RemoteException
	 */
	String getName() throws RemoteException;

}

