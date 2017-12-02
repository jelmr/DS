package distributed.systems.gridscheduler.remote;

import distributed.systems.gridscheduler.Logger;
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
public interface RemoteResourceManager extends Remote, Logger {

	// TODO: Merge this with Logger logEvent
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
				Logger rgs = (Logger)loggers.get(loggerIndex).getObject();
				rgs.logEvent(e);
				logSubmitted = true;
			} catch (RemoteException ignored) {}
			loggerIndex++;
		} while (!logSubmitted && loggerIndex < (numLoggers));


		return logSubmitted;
	}

	boolean isAlive() throws RemoteException;

	int getLoad() throws RemoteException;

	boolean registerAsDuplicate(RemoteResourceManager rrm) throws RemoteException;

	// Todo: Probably JobScheduleResponse
	boolean addJob(Job job) throws RemoteException;

	String getName() throws RemoteException;

	public void jobDone(Job job) throws RemoteException;

}
