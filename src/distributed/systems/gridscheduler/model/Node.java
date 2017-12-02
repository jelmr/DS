package distributed.systems.gridscheduler.model;

import distributed.systems.gridscheduler.Logger;
import distributed.systems.gridscheduler.remote.RemoteResourceManager;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 * This class represents a Node within a virtual cluster. Nodes can run jobs, go up and go down. 
 * 
 * @author Niels Brouwers
 *
 */
public class Node {
	private NodeStatus status;
	private Job runningJob = null;
	private long startTime;

	private String name;
	private List<RemoteResourceManager> handlers;

	/**
	 * Constructs a new Node object.
	 * @param name
	 */
	public Node(String name) {
		this.name = name;
		this.status = NodeStatus.Idle;
		this.handlers = new ArrayList<>();
	}


	public String getName() {
		return name;
	}


	/**
	 * Add a node event handler to this node.
	 * @see INodeEventHandler
	 * @param handler event handler, can't be null
	 */
	public void addNodeEventHandler(RemoteResourceManager handler) {

		// precondition
		assert(handler != null);
		handlers.add(handler);
	}

	/**
	 * 
	 * @return the status of the node
	 */
	public NodeStatus getStatus() {
		return status;
	}

	/**
	 * Starts a job at this node.
	 * <P>
	 * <DL>
	 * <DT><B>Preconditions:</B>
	 * <DD>the node should be idle
	 * </DL>
	 */
	public void startJob(Job job) {
		// preconditions
		assert(status == NodeStatus.Idle) : "The status of a node should be idle when it starts a job, but it's not.";

		runningJob = job;
		runningJob.setStatus(JobStatus.Running);

		startTime = System.currentTimeMillis();

		status = NodeStatus.Busy;

	}

	/**
	 * Polls the node, the node checks if the job it is executing is done and fires a JobFinished
	 * event accordingly. It then updates its state.
	 */
	public void poll() {
		if (runningJob != null) {

			// check if the job has finished
			if (System.currentTimeMillis() - startTime > runningJob.getDuration()) {
				// job done
				runningJob.setStatus(JobStatus.Done);

				// fire event handler
				for (RemoteResourceManager handler : handlers)
					try {
						handler.jobDone(runningJob);
					} catch (RemoteException ignored) {
						// TODO: Do something?
					}

				// set node status
				runningJob = null;
				status = NodeStatus.Idle;

			}

		}

	}

}
