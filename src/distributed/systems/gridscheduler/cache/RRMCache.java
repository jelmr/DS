package distributed.systems.gridscheduler.cache;

import distributed.systems.gridscheduler.model.NodeStatus;
import distributed.systems.gridscheduler.remote.RemoteResourceManager;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DS:distributed.systems.gridscheduler.cache.RRMCache
 * Written by Glenn. Created on 2017-12-05 at 22:00.
 */
public class RRMCache implements Cache {

    private String name;
    private final RemoteResourceManager source;

    private Cached<Integer> nodeCount;
    private Cached<Integer> amountOfIdleNodes;
    private Cached<Integer> amountOfBusyNodes;
    private Cached<Integer> amountOfDownNodes;

    private Cached<List<NodeData>> allNodeData;

    private Map<String, NodeData> nodes;

    public RRMCache(String name, RemoteResourceManager source) {
        this.name = name;
        this.source = source;

        nodeCount = new Cached<>();
        amountOfIdleNodes = new Cached<>();
        amountOfBusyNodes = new Cached<>();
        amountOfDownNodes = new Cached<>();

        allNodeData = new Cached<>();

        nodes = new HashMap<>();

        forceRefresh();
    }

    @Override
    public void invalidate() {
        nodeCount.invalidate();
        amountOfIdleNodes.invalidate();
        amountOfBusyNodes.invalidate();
        amountOfDownNodes.invalidate();

        allNodeData.invalidate();
    }

    @Override
    public void forceRefresh() {
        try {
            refreshAllNodes();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void refreshAllNodes() throws RemoteException {
        if (allNodeData.isStale()) {
            allNodeData.updateValue(source.getAllNodes());
        }
        List<NodeData> allData = allNodeData.getValue();
        int[] statusCounts = {0, 0, 0};
        allData.forEach(data -> {
            if (!nodes.containsKey(data.getName())) {
                nodes.put(data.getName(), data);
            }
            nodes.get(data.getName()).setStatus(data.getStatus());
            switch (data.getStatus()) {
                case Idle:
                    statusCounts[0]++;
                    break;
                case Busy:
                    statusCounts[1]++;
                    break;
                case Down:
                    statusCounts[2]++;
                    break;
            }
        });

        nodeCount.updateValue(statusCounts[0] + statusCounts[1] + statusCounts[2]);
        amountOfIdleNodes.updateValue(statusCounts[0]);
        amountOfBusyNodes.updateValue(statusCounts[1]);
        amountOfDownNodes.updateValue(statusCounts[2]);
    }

    public String getName() {
        return name;
    }

    public int getNodeCount() {
        return getAmountOfIdleNodes() + getAmountOfBusyNodes() + getAmountOfDownNodes();
    }

    public int getAmountOfIdleNodes() {
        if (amountOfIdleNodes.isStale()) {
            try {
                refreshAllNodes();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return amountOfIdleNodes.getValue();
    }

    public int getAmountOfBusyNodes() {
        if (amountOfBusyNodes.isStale()) {
            try {
                refreshAllNodes();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return amountOfBusyNodes.getValue();
    }

    public int getAmountOfDownNodes() {
        if (amountOfDownNodes.isStale()) {
            try {
                refreshAllNodes();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return amountOfDownNodes.getValue();
    }

    public NodeStatus getStatusOf(int index) {
        if (allNodeData.isStale()) {
            try {
                refreshAllNodes();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        String name = allNodeData.getValue().get(index).getName();
        return nodes.get(name).getStatus();
    }

    public void refreshSingleNode(String nodeName, NodeStatus nodeStatus) {
        nodes.get(nodeName).setStatus(nodeStatus);
    }

    public boolean isSourceDead() {
        try {
            source.getName();
            return false;
        } catch (RemoteException e) {
            return true;
        }
    }
}
