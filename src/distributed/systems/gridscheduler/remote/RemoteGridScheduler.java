package distributed.systems.gridscheduler.remote;

import distributed.systems.gridscheduler.model.Event;
import distributed.systems.gridscheduler.model.Job;
import distributed.systems.gridscheduler.model.LamportsClock;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public interface RemoteGridScheduler extends Remote {

	default boolean logEvent(List<RemoteGridScheduler> gridSchedulers, Event e) throws RemoteException {

		boolean logSubmitted = false;
		int gridSchedulerIndex = 0;
		int numGridSchedulers = gridSchedulers.size();

		do {
			try {
				RemoteGridScheduler rgs = gridSchedulers.get(gridSchedulerIndex);
				rgs.logEvent(e);
				logSubmitted = true;
			} catch (RemoteException ignored) {}
			gridSchedulerIndex++;
		} while (!logSubmitted && gridSchedulerIndex < (numGridSchedulers));


		return logSubmitted;
	}

	boolean registerResourceManager(RemoteResourceManager rrm) throws RemoteException;

	boolean registerGridScheduler(RemoteGridScheduler rgs) throws RemoteException;

	boolean scheduleJob(Job job, RemoteResourceManager source) throws RemoteException;

	boolean logEvent(Event e) throws RemoteException;

	String getName() throws RemoteException;


}
