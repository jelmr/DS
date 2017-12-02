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

	RM_REGISTERED_AS_DUPLICATE(""),
	GS_REGISTERED_WITH_GS(""),

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

	NODE_JOB_DONE(""),
	RM_JOB_DONE(""),

	/**
	 * Issued when a Client receives the results for a Job it issued.
	 * Format args:
	 * 1: (String) Issueing client name
	 * 2: (String) jobId
	 */
	CLIENT_JOB_DONE("Client '%s' has received results for job id='%s'."),




	CLIENT_EXITING("Client '%s' is exiting the system.");


	private String formatString;

	EventType(String formatString) {
		this.formatString = formatString;
	}


	public String getFormatString() {
		return this.formatString;
	}


}
