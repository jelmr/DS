package distributed.systems.gridscheduler.neogui;

/**
 * DS:distributed.systems.gridscheduler.neogui.RRMStatusPanelData
 * Written by Glenn. Created on 2017-12-06 at 17:39.
 */
public interface RRMStatusPanelData {
    String getNameValue();
    String getNodeCountValue();
    String getLoadValue();
    String getAvailableValue();
    String getQueueValue();

//    Currently handled by text conversion, not needed.
//    int getNodeCount();
//    int getBusyCount();
//    int getDownCount();

    int getQueueSize();
    int getQueueLimit();
}
