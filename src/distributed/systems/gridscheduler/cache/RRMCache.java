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

    private Map<String, Cached<NodeData>> nodes;

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
        nodes.forEach((s, nodeStatusCached) -> nodeStatusCached.invalidate());
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
                nodes.put(data.getName(), new Cached<>());
            }
            nodes.get(data.getName()).updateValue(data);
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
            System.out.println("Fix stale");
            try {
                refreshAllNodes();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        String name = allNodeData.getValue().get(index).getName();
        NodeStatus result = nodes.get(name).getValue().getStatus();
        if (result == NodeStatus.Busy) {
            System.out.println("According to nodes:");
            nodes.forEach((s, nodeDataCached) -> System.out.printf("%s, %s\n", s, nodeDataCached.getValue().getStatus().name()));

            System.out.println("According to allNodeData:");
            allNodeData.getValue().forEach((nodeData) -> System.out.printf("%s, %s\n", nodeData.getName(), nodeData.getStatus().name()));
        }
        return result;
    }

    public void refreshSingleNode(String nodeName, NodeStatus nodeStatus) {
        try {
            source.getAllNodes().forEach(nodeData -> {
                System.out.println("nodeData = " + nodeData.getStatus().name());
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        nodes.get(nodeName).updateValue(new NodeData(nodeName, nodeStatus));
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
