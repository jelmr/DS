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

	public static class GenericEvent extends Event{

		private String s;


		public GenericEvent(LogicalClock timestamp, String s) {
			this.s = s;
			this.timestamp = timestamp;
		}


		@Override
		public String getLogString() {
			return s;
		}
	}


	// TODO: Perhaps make type safe Events.
	/*
	public abstract class ResourceManager extends Event {

		distributed.systems.gridscheduler.model.ResourceManager rm;


		public ResourceManager(distributed.systems.gridscheduler.model.ResourceManager rm) {
			this.rm = rm;
		}

		public class ReceivedJob extends ResourceManager {

			Job job;

			public ReceivedJob(distributed.systems.gridscheduler.model.ResourceManager rm, Job job) {
				super(rm);
				this.job = job;
			}


			@Override
			String getLogString() {
				return String.format("Resource Manager %s received a job with ID=%d\n", rm.getName(), job.getId());
			}

		}

	}
	*/


}
