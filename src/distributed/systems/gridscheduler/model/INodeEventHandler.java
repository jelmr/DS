package distributed.systems.gridscheduler.model;

import distributed.systems.gridscheduler.remote.RemoteLogger;


/**
 * 
 * Event handler for nodes. This allows nodes to communicate their status back to the cluster
 * they are in.
 * 
 * @author Niels Brouwers
 *
 */
public interface INodeEventHandler extends RemoteLogger {

	// notify the completion of a job


}
