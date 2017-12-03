package distributed.systems.gridscheduler.model;

/**
 * @author Jelmer Mulder
 *         Date: 02/12/2017
 */
public enum EventType {

	/**
	 * Issued when a new registry starts up.
	 * Format args:
	 * 1: (String) Registry hostname
	 * 2: (int) Registry portnumber
	 */
	REGISTRY_START("A new registry has started at %s:%d."),


	/**
	 * Issued when a Client/RM/GS registers itself with a certain Registry.
	 * Format args:
	 * 1: (String) Source name
	 * 2: (String) Target registry hostname
	 * 3: (int) Target registry portnumber
	 */
	CLIENT_REGISTERED_REGISTRY("Client '%s' has registered itself with the registry at %s:%d."),
	RM_REGISTERED_REGISTRY("ResourceManager '%s' has registered itself with the registry at %s:%d."),
	GS_REGISTERED_REGISTRY("GridScheduler '%s' has registered itself with the registry at %s:%d."),
	FRONTEND_REGISTERED_REGISTRY("Frontend '%s' has registered itself with the registry at %s:%d."),






	/**
	 * Issued when a GS accepts a RM in it's subgrid.
	 * Format args:
	 * 1: (String) Target GS Name.
	 * 2: (String) Source RM name.
	 */
	GS_ACCEPTS_RM_REGISTRATION("GridScheduler '%s' has accepted ResourceManager '%s' into its subgrid."),

	/**
	 * Issued when a RM requests to register with a GS.
	 * Format args:
	 * 1: (String) Source RM name.
	 * 2: (String) Target GS name.
	 */
	RM_REGISTERS_WITH_GS("ResourceManager '%s' has requested to join subgrid of GridScheduler '%s'."),

	/**
	 * Issued when a Frontend attempts to subscribe to the events of a GridScheduler.
	 * Format args:
	 * 1: (String) Source Frontend name
	 * 2: (String) Target GS name.
	 */
	FRONTEND_SUBSCRIBE_TO_EVENTS_ATTEMPT("Frontend '%s' attempts to subscribe to Events of GridScheduler '%s'."),

	/**
	 * Issued when a GS accepts a Frontends request to subscribe to its events.
	 * Format args:
	 * 1: (String) GS name.
	 * 2: (String) Frontend name.
	 */
	GS_ACCEPTS_FRONTEND_SUBSCRIPTION("GridScheduler '%s' has subscribed Frontend '%s' to its Events."),

	RM_REGISTERED_AS_DUPLICATE(""),

	/**
	 * Issued when a GS accepts another GS as its peer.
	 * Format args:
	 * 1: (String) Target GS Name.
	 * 2: (String) Source RM name.
	 */
	GS_ACCEPTS_GS_REGISTRATION("GridScheduler '%s' accepts GridScheduler '%s' request to become its peer."),

	/**
	 * Issued when a GS sends a list of all GS/RM to another host.
	 * Format args:
	 * 1: (String) Source GS name
	 * 2: (String) Target host
	 */
	GS_SEND_LIST_GS("GridScheduler '%s' sending List of all registered GridSchedulers to '%s'."),
	GS_SEND_LIST_RM("GridScheduler '%s' sending List of all registered ResourceManagers to '%s'."),



	/**
	 * Issued when a Client attempts to schedule a job with a RM.
	 * Format args:
	 * 1: (String) Source client name
	 * 2: (String) jobId
	 * 3: (String) Target ResourceManager name.
	 */
	CLIENT_JOB_SCHEDULE_ATTEMPT("Client '%s' attempting to schedule a job id='%s' at ResourceManager '%s'."),


	/**
	 * Issued when a Client attempts to schedule a job with a RM but fails. It assumes the RM must have crashed.
	 * Format args:
	 * 1: (String) Source client name
	 * 2: (String) Target ResourceManager name.
	 */
	CLIENT_DETECTED_CRASHED_RM("Client '%s' has detected that ResourceManager '%s' has crashed."),

	/**
	 * Issued when a RM receives a Job request from a Client or a GridScheduler.
	 * Format args:
	 * 1: (String) Target RM name.
	 * 2: (String) Job ID
	 * 3: (Int) Job duration
	 * 4: (String) Source name
	 */
	RM_RECEIVED_JOB_REQUEST("ResourceManager '%s' received a request to process job id='%s' with duration='%d' from '%s'."),

	/**
	 * Issued when a RM adds a job to its local queue.
	 * Format args:
	 * 1: (String) Source RM name.
	 * 2: (String) Job ID
	 */
	RM_QUEUED_JOB("ResourceManager '%s' added job id='%s' to its local job queue."),

	/**
	 * Issued when a RM schedules a job on one of its local Nodes.
	 * Format args:
	 * 1: (String) Source RM name.
	 * 2: (String) Job ID
	 * 3: (String) Target Node name
	 */
	RM_SCHEDULED_JOB_ON_NODE("ResourceManager '%s' scheduled job id='%s' on Node '%s'."),

	/**
	 * Issued when a RM receives a Job request from a Client or a GridScheduler.
	 * Format args:
	 * 1: (String) Source RM name.
	 * 2: (String) Job ID
	 * 3: (String) Target GS name.
	 */
	RM_OFFLOAD_TO_GS_ATTEMPT("ResourceManager '%s' is attempting to offloaded job id='%s' to GridScheduler '%s'."),

	/**
	 * Issued when a RM has finished processing a job.
	 * Format args:
	 * 1: (String) RM that finished the job
	 * 2: (String) jobId
	 */
	RM_FINISHED_JOB("ResourceManager '%s' has finished processing Job id='%s'."),

	/**
	 * Issued when a Client receives the results for a Job it issued.
	 * Format args:
	 * 1: (String) Issueing client name
	 * 2: (String) jobId
	 */
	CLIENT_JOB_DONE("Client '%s' has received results for job id='%s'."),




	/**
	 * Issued when a Client exits the system.
	 * Format args:
	 * 1: (String) Exiting client name
	 */
	CLIENT_EXITING("Client '%s' is exiting the system.");


	private String formatString;

	EventType(String formatString) {
		this.formatString = formatString;
	}


	public String getFormatString() {
		return this.formatString;
	}


}
