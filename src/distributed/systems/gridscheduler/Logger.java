package distributed.systems.gridscheduler;

import distributed.systems.gridscheduler.model.Event;
import distributed.systems.gridscheduler.remote.RemoteResourceManager;
import java.rmi.RemoteException;
import java.util.List;


/**
 * @author Jelmer Mulder
 *         Date: 02/12/2017
 */
public interface Logger {


	static boolean logEvent(List<Logger> loggers, Event e) throws RemoteException {

		if (loggers.size() <= 0) {
			System.out.printf("No loggers found.\n");
			return false;
		}

		boolean logSubmitted = false;
		int loggerIndex = 0;
		int numLoggers = loggers.size();

		do {
			try {
				Logger rgs = loggers.get(loggerIndex);
				rgs.logEvent(e);
				logSubmitted = true;
			} catch (RemoteException ignored) {}
			loggerIndex++;
		} while (!logSubmitted && loggerIndex < (numLoggers));


		return logSubmitted;
	}

	boolean logEvent(Event e) throws RemoteException;

}
