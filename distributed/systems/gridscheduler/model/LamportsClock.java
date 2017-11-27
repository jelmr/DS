package distributed.systems.gridscheduler.model;

/**
 * @author Jelmer Mulder
 *         Date: 27/11/2017
 */
public class LamportsClock implements LogicalClock {

	private long timestamp;


	public LamportsClock() {
		timestamp = 0L;
	}

	public LamportsClock(LamportsClock lc) {
		this.timestamp = lc.timestamp;
	}



	@Override
	public void tickInternalEvent() {
		this.timestamp++;
	}


	@Override
	public void tickSendEvent() {
		this.timestamp++;
	}


	@Override
	public void tickReceiveEvent(LogicalClock clockSender) {
		assert clockSender instanceof LamportsClock: "Attempting to compare two different type of LogicalClocks.";

		LamportsClock lcSender = (LamportsClock)clockSender;

		this.timestamp = Math.max(this.timestamp, lcSender.timestamp) + 1;
	}


	@Override
	public int compareTo(LogicalClock o) {
		assert o instanceof LamportsClock: "Attempting to compare two different type of LogicalClocks.";

		return Long.compare(this.timestamp, ((LamportsClock) o).timestamp);
	}
}









