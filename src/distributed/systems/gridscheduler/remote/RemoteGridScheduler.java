package distributed.systems.gridscheduler.remote;

import distributed.systems.gridscheduler.model.Event;
import distributed.systems.gridscheduler.model.Job;
import distributed.systems.gridscheduler.model.LamportsClock;
import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public interface RemoteGridScheduler extends Remote {

	boolean registerResourceManager(RemoteResourceManager rrm) throws RemoteException;

	boolean registerGridScheduler(RemoteGridScheduler rgs) throws RemoteException;

	boolean scheduleJob(Job job, RemoteResourceManager source) throws RemoteException;

	boolean logEvent(Event e) throws RemoteException;

	String getName() throws RemoteException;


}
