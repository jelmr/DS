package distributed.systems.gridscheduler.remote;

import distributed.systems.gridscheduler.Named;
import distributed.systems.gridscheduler.model.Event;
import distributed.systems.gridscheduler.model.Job;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public interface RemoteResourceManager extends Remote {

	static boolean logEvent(List<Named<RemoteResourceManager>> resourceManagers, Event e) throws RemoteException {

		boolean logSubmitted = false;
		int resourceManagerIndex = 0;
		int numResourceManagers = resourceManagers.size();

		do {
			try {
				RemoteResourceManager rrm = resourceManagers.get(resourceManagerIndex).getObject();
				rrm.logEvent(e);
				logSubmitted = true;
			} catch (RemoteException ignored) {}
			resourceManagerIndex++;
		} while (!logSubmitted && resourceManagerIndex < (numResourceManagers));


		return logSubmitted;
	}

	boolean isAlive() throws RemoteException;

	int getLoad() throws RemoteException;

	boolean registerAsDuplicate(RemoteResourceManager rrm) throws RemoteException;

	// Todo: Probably JobScheduleResponse
	boolean addJob(Job job) throws RemoteException;

	boolean logEvent(Event e) throws RemoteException;

	String getName() throws RemoteException;

}
