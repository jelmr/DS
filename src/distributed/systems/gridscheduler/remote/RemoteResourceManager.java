package distributed.systems.gridscheduler.remote;

import distributed.systems.gridscheduler.model.Job;
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public interface RemoteResourceManager extends Remote {

	boolean isAlive() throws RemoteException;

	boolean registerAsDuplicate(RemoteResourceManager rrm) throws RemoteException;

	// Todo: Probably JobScheduleResponse
	boolean scheduleJob(Job job) throws RemoteException;

}
