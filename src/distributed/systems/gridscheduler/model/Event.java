package distributed.systems.gridscheduler.model;

import java.io.Serializable;


/**
 * @author Jelmer Mulder
 *         Date: 29/11/2017
 */
public abstract class Event implements Serializable{

	LogicalClock timestamp;


	public LogicalClock getTimestamp() {
		return this.timestamp;
	}


	public abstract String getLogString();

	/*public static class GenericEvent extends Event{

		private String s;


		public GenericEvent(LogicalClock timestamp, String s, Object... objects) {
			this.s = String.format(s, objects);
			this.timestamp = timestamp;
		}


		@Override
		public String getLogString() {
			return s;
		}
	}
	*/


	// TODO: Replace Event by TypedEvent?
	public static class TypedEvent extends Event {

		private EventType type;
		private Object[] args;


		public TypedEvent(LogicalClock timestamp, EventType type, Object... args) {
			this.timestamp = timestamp;
			this.type = type;
			this.args = args;

		}

		public EventType getType() {
			return this.type;
		}

		public Object[] getArgs() {
			return this.args;
		}

		@Override
		public String getLogString() {
			return String.format(type.getFormatString(), this.args);
		}
	}


}
