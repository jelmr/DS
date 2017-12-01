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

	void jobDone(Job job) throws RemoteException;

	String getName() throws RemoteException;

}
