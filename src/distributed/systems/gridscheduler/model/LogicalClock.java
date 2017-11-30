package distributed.systems.gridscheduler.model;

import java.io.Serializable;


/**
 * @author Jelmer Mulder
 *         Date: 27/11/2017
 */
public interface LogicalClock extends Comparable<LogicalClock>, Serializable{

	void tickInternalEvent();

	void tickSendEvent();

	void tickReceiveEvent(LogicalClock clockSender);

}
