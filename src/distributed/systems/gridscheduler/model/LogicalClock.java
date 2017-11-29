package distributed.systems.gridscheduler.model;

/**
 * @author Jelmer Mulder
 *         Date: 27/11/2017
 */
public interface LogicalClock extends Comparable<LogicalClock> {

	void tickInternalEvent();

	void tickSendEvent();

	void tickReceiveEvent(LogicalClock clockSender);

}
