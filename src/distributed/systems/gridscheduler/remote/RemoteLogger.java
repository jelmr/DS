package distributed.systems.gridscheduler.remote;

import distributed.systems.gridscheduler.model.Event;
import java.rmi.RemoteException;
import java.util.List;


/**
 * @author Jelmer Mulder
 *         Date: 02/12/2017
 */
public interface RemoteLogger {

	/**
	 * Attempt to log Event e to any of the RemoteLoggers in the list loggers. Will try them in order until it
	 * manages to succesfully log the event.
	 * @param loggers list of all loggers
	 * @param e the event to be logged
	 * @return true if the Event was succesfully logged, false otherwise
	 * @throws RemoteException
	 */
	static boolean logEvent(List<RemoteLogger> loggers, Event e) throws RemoteException {

		if (loggers.size() <= 0) {
			System.out.printf("No loggers found.\n");
			return false;
		}

		boolean logSubmitted = false;
		int loggerIndex = 0;
		int numLoggers = loggers.size();

		do {
			try {
				RemoteLogger rgs = loggers.get(loggerIndex);
				rgs.logEvent(e);
				logSubmitted = true;
			} catch (RemoteException ignored) {}
			loggerIndex++;
		} while (!logSubmitted && loggerIndex < (numLoggers));


		return logSubmitted;
	}

	/**
	 * Log an Event to this Remote.
	 * @param e the Event to be logged.
	 * @return true if the Event was succesfully logged, false otherwise.
	 * @throws RemoteException
	 */
	boolean logEvent(Event e) throws RemoteException;

}
