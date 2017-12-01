package distributed.systems.gridscheduler.remote;

import distributed.systems.gridscheduler.model.Event;
import distributed.systems.gridscheduler.model.INodeEventHandler;
import distributed.systems.gridscheduler.model.Job;
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public interface RemoteResourceManager extends Remote {

	boolean isAlive() throws RemoteException;

	int getLoad() throws RemoteException;

	boolean registerAsDuplicate(RemoteResourceManager rrm) throws RemoteException;

	// Todo: Probably JobScheduleResponse
	boolean addJob(Job job) throws RemoteException;

	boolean logEvent(Event e) throws RemoteException;

	String getName() throws RemoteException;

}
