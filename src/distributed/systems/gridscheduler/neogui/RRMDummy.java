package distributed.systems.gridscheduler.neogui;

/**
 * DS:distributed.systems.gridscheduler.neogui.RRMDummy
 * Written by Glenn. Created on 2017-12-06 at 18:15.
 */
class RRMDummy implements RRMStatusPanelData {
    private String name;

    private int nodeCount;
    private int busyCount;
    private int downCount;

    private int queueSize;
    private int queueLimit;

    RRMDummy(String name, int nodeCount, int busyCount, int downCount, int queueSize, int queueLimit) {
        this.name = name;
        this.nodeCount = nodeCount;
        this.busyCount = busyCount;
        this.downCount = downCount;
        this.queueSize = queueSize;
        this.queueLimit = queueLimit;
    }

    @Override
    public String getNameValue() {
        return name;
    }

    @Override
    public String getNodeCountValue() {
        return String.valueOf(nodeCount);
    }

    @Override
    public String getLoadValue() {
        int calc = (int) ((busyCount / (double) nodeCount) * 100);
        return String.format("%d%%", calc);
    }

    @Override
    public String getAvailableValue() {
        int calc = (int) (((nodeCount - downCount) / (double) nodeCount) * 100);
        return String.format("%s%%", calc);
    }

    @Override
    public String getQueueValue() {
        return String.format("%d / %d", queueSize, queueLimit);
    }

    @Override
    public int getQueueSize() {
        return queueSize;
    }

    @Override
    public int getQueueLimit() {
        return queueLimit;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public int getBusyCount() {
        return busyCount;
    }

    public int getDownCount() {
        return downCount;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public void setBusyCount(int busyCount) {
        this.busyCount = busyCount;
    }

    public void setDownCount(int downCount) {
        this.downCount = downCount;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public void setQueueLimit(int queueLimit) {
        this.queueLimit = queueLimit;
    }
}
