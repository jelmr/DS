package distributed.systems.gridscheduler.model;

import distributed.systems.gridscheduler.remote.RemoteClient;
import distributed.systems.gridscheduler.remote.RemoteResourceManager;
import java.io.Serializable;


/**
 * This class represents a job that can be executed on a grid. 
 * 
 * @author Niels Brouwers
 *
 */
public class Job implements Serializable {
	private long duration;
	private JobStatus status;
	private String id;
	private RemoteResourceManager issueingResourceManager;
	private RemoteClient issueingClient;
	private String issueingClientName;
	private boolean hasBeenOffloaded;



	public Job(long duration, String id, RemoteResourceManager issueingResourceManager, RemoteClient issueingClient, String issueingClientName) {
		// Preconditions
		assert(duration > 0) : "parameter 'duration' should be > 0";

		this.duration = duration;
		this.status = JobStatus.Waiting;
		this.id = id;
		this.issueingClientName = issueingClientName;
		this.issueingClient = issueingClient;
		this.issueingResourceManager = issueingResourceManager;
		this.hasBeenOffloaded = false;
	}


	public boolean hasBeenOffloaded() {
		return hasBeenOffloaded;
	}


	public void setHasBeenOffloaded(boolean hasBeenOffloaded) {
		this.hasBeenOffloaded = hasBeenOffloaded;
	}


	public RemoteClient getIssueingClient() {
		return issueingClient;
	}


	public String getIssueingClientName() {
		return issueingClientName;
	}


	/**
	 * Returns the duration of this job. 
	 * @return the total duration of this job
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * Returns the status of this job.
	 * @return the status of this job
	 */
	public JobStatus getStatus() {
		return status;
	}

	/**
	 * Sets the status of this job.
	 * @param status the new status of this job
	 */
	public void setStatus(JobStatus status) {
		this.status = status;
	}

	/**
	 * The message ID is a unique identifier for a message. 
	 * @return the message ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return a string representation of this job object
	 */
	public String toString() {
		return "Job {ID = " + id + "}";
	}


	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Job))
			return false;

		Job job = (Job) o;

		return id.equals(job.id);
	}


	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
