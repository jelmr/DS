package distributed.systems.gridscheduler.neogui;

import distributed.systems.gridscheduler.cache.RGSCache;
import distributed.systems.gridscheduler.cache.RRMCache;
import distributed.systems.gridscheduler.model.NodeStatus;

import javax.swing.*;
import java.util.HashMap;

/**
 * DS:distributed.systems.gridscheduler.neogui.NeoGuiHost
 * Written by Glenn. Created on 2017-12-06 at 19:54.
 */
public class NeoGuiHost {

    // Refresh 250ms after last update to fix Event De-sync
    private static final int FULL_REFRESH_DELAY = 250;
    private final Timer completeRefreshTimer;

    private final HashMap<String, RGSStatusFrame> rgsFrames;
    private final HashMap<String, RRMStatusPanel> rrmPanels;

    private final HashMap<String, RGSCache> rgsData;
    private final HashMap<String, RRMCache> rrmData;

    private boolean running = false;

    public NeoGuiHost() {
        rgsFrames = new HashMap<>();
        rrmPanels = new HashMap<>();
        rgsData = new HashMap<>();
        rrmData = new HashMap<>();

        completeRefreshTimer = new Timer(FULL_REFRESH_DELAY, e -> deepRefresh());
        completeRefreshTimer.setRepeats(false);
    }

    private void deepRefresh() {
        for (String rgsName : rgsData.keySet()) {
            rgsData.get(rgsName).invalidate();
            rgsFrames.get(rgsName).revalidate();
        }
        for (String rrmName : rrmData.keySet()) {
            rrmData.get(rrmName).invalidate();
            rrmPanels.get(rrmName).revalidate();
        }
    }

    public void start() {
        for (RGSStatusFrame frame : rgsFrames.values()) {
            frame.setVisible(true);
        }
        completeRefreshTimer.start();
        System.out.println("Timer started in start");
        running = true;
    }

    public void addRGS(String rgsName, RGSStatusFrame frame, RGSCache data) {
        rgsFrames.put(rgsName, frame);
        rgsData.put(rgsName, data);
        if (running) {
            frame.setVisible(true);
            frame.revalidate();
            completeRefreshTimer.start();
            System.out.println("Timer started in addRGS");
        }
    }

    public void addRRM(String rgsName, String rrmName, RRMStatusPanel panel, RRMCache data) {
        rgsFrames.get(rgsName).addPanel(panel);
        rrmPanels.put(rrmName, panel);
        rrmData.put(rrmName, data);

        if (running) {
            panel.revalidate();
            completeRefreshTimer.start();
            System.out.println("Timer started in addRMM");
        }
    }

    public void updateRRM(String rrmName) {
        rrmData.get(rrmName).invalidate();
        rrmPanels.get(rrmName).revalidate();
        completeRefreshTimer.start();
        System.out.println("Timer started in update RRM");
    }

    public void updateNodes(String rrmName) {
        // TODO optimize further?
        updateRRM(rrmName);
    }

    public void updateNode(String rrmName, String nodeName, NodeStatus nodeStatus) {
        // TODO optimize further?
        updateRRM(rrmName);
    }

    public void reportDeadRRM(String rrmName) {
        // TODO mark RRM as inactive inside rrmData.
    }
}
