package distributed.systems.gridscheduler.cache;

import distributed.systems.gridscheduler.neogui.RRMStatusPanelData;
import distributed.systems.gridscheduler.remote.RemoteResourceManager;

import java.rmi.RemoteException;
import java.util.*;

/**
 * DS:distributed.systems.gridscheduler.cache.RRMCache
 * Written by Glenn. Created on 2017-12-05 at 22:00.
 */
public class RRMCache implements Cache, RRMStatusPanelData {

    private final RemoteResourceManager source;

    private final Cached<String> name = new Cached<>();

    private final Cached<Integer> idleNodeCount = new Cached<>();
    private final Cached<Integer> busyNodeCount = new Cached<>();
    private final Cached<Integer> downNodeCount = new Cached<>();
    private final Cached<Integer> queueSize = new Cached<>();
    private final Cached<Integer> queueLimit = new Cached<>();

    public RRMCache(RemoteResourceManager source) {
        this.source = source;
    }

    @Override
    public void invalidate() {
        System.out.println("RRMCache invalidated");
        name.invalidate();

        idleNodeCount.invalidate();
        busyNodeCount.invalidate();
        downNodeCount.invalidate();
        queueSize.invalidate();
        queueLimit.invalidate();
    }

    @Override
    public void reloadStaleValues() {
        reloadStaleName();
        reloadStaleQueueSize();
        reloadStaleQueueLimit();
        reloadStaleNodeCounts();

    }

    private void reloadStaleName() {
        if (name.isStale()) try {
            name.updateValue(source.getName());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void reloadStaleQueueLimit() {
        if (queueLimit.isStale()) try {
            queueLimit.updateValue(source.getQueueLimit());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void reloadStaleQueueSize() {
        if (queueSize.isStale()) try {
            queueSize.updateValue(source.getQueueSize());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void reloadStaleNodeCounts() {
        if (idleNodeCount.isStale() | busyNodeCount.isStale() | downNodeCount.isStale()) {
            try {
                ArrayList<NodeData> allData = source.getAllNodes();
                int[] status = {0, 0, 0};
                allData.forEach(nodeData -> {
                    switch (nodeData.getStatus()) {
                        case Idle: status[0]++;
                            break;
                        case Busy: status[1]++;
                            break;
                        case Down: status[2]++;
                            break;
                    }
                });
                idleNodeCount.updateValue(status[0]);
                busyNodeCount.updateValue(status[1]);
                downNodeCount.updateValue(status[2]);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getNameValue() {
        reloadStaleName();
        return name.getValue();
    }

    @Override
    public String getNodeCountValue() {
        reloadStaleNodeCounts();
        int totalNodes = idleNodeCount.getValue() + busyNodeCount.getValue() + downNodeCount.getValue();
        return String.format("%d", totalNodes);
    }

    @Override
    public String getLoadValue() {
        reloadStaleNodeCounts();
        int totalNodes = idleNodeCount.getValue() + busyNodeCount.getValue() + downNodeCount.getValue();
        double fractionBusy = busyNodeCount.getValue() / (double) totalNodes;
        return String.format("%.2f%%", fractionBusy * 100);
    }

    @Override
    public String getAvailableValue() {
        reloadStaleNodeCounts();
        int totalNodes = idleNodeCount.getValue() + busyNodeCount.getValue() + downNodeCount.getValue();
        double fractionNotDown = (totalNodes - downNodeCount.getValue()) / (double) totalNodes;
        return String.format("%.2f%%", fractionNotDown * 100);
    }

    @Override
    public String getQueueValue() {
        reloadStaleQueueSize();
        reloadStaleQueueLimit();
        return String.format("%d / %d", queueSize.getValue(), queueLimit.getValue());
    }

    @Override
    public int getQueueSize() {
        reloadStaleQueueSize();
        return queueSize.getValue();
    }

    @Override
    public int getQueueLimit() {
        reloadStaleQueueLimit();
        return queueLimit.getValue();
    }
}
