package distributed.systems.gridscheduler.cache;

import distributed.systems.gridscheduler.model.NodeStatus;

import java.io.Serializable;

/**
 * DS:distributed.systems.gridscheduler.cache.NodeData
 * Written by Glenn. Created on 2017-12-05 at 16:30.
 */
public class NodeData implements Serializable {

    private static final long serialVersionUID = 7598795436168166743L;

    private String name;
    private NodeStatus status;

    public NodeData(String name, NodeStatus status) {
        this.name = name;
        this.status = status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public NodeStatus getStatus() {
        return status;
    }
}
