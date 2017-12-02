package distributed.systems.gridscheduler.remote;

import distributed.systems.gridscheduler.Logger;
import distributed.systems.gridscheduler.model.Job;
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public interface RemoteGridScheduler extends Remote, Logger {


	boolean registerResourceManager(RemoteResourceManager rrm, String rrmName) throws RemoteException;

	boolean registerGridScheduler(RemoteGridScheduler rgs) throws RemoteException;

	boolean offloadJob(Job job, RemoteResourceManager source) throws RemoteException;


	String getName() throws RemoteException;

	// TODO: Subscribe to events


}
