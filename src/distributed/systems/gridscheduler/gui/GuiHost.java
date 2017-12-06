/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package distributed.systems.gridscheduler.gui;

import distributed.systems.gridscheduler.cache.*;
import distributed.systems.gridscheduler.model.NodeStatus;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Glenn
 */
public class GuiHost {
    private Map<String, GSFrame> windows;
    private Map<String, RMStatusPanel> panels;

    private Map<String, RGSCache> rgsCacheMap;
    private Map<String, RRMCache> rrmCacheMap;

    boolean running = false;


    public GuiHost() {
        this.windows = new HashMap<>();
        this.panels = new HashMap<>();

        this.rgsCacheMap = new HashMap<>();
        this.rrmCacheMap = new HashMap<>();
    }

    /**
     * Adds an RGS to the UI and keeps it's data for updating
     * @param rgsName is the name of the RGS that is added
     * @param frame is the frame that represents the RGS
     * @param data is the updatable data of the RGS that is added
     */
    public void addRGS(String rgsName, GSFrame frame, RGSCache data) {
        windows.put(rgsName, frame);
        rgsCacheMap.put(rgsName, data);

        if (running) {
            // A GS has joined after start, start it.
            frame.start();
        }
    }

    /**
     * Adds an RRM to the UI (and to it's RGS ui) and keeps it's data for updating
     * @param rgsName is the name of the RGS that the RMM will be placed into
     * @param rrmName is the name of the RRM that is added
     * @param panel is the panel that represents the RMM
     * @param data is the updatable data of the RMM that is added
     */
    public void addRRM(String rgsName, String rrmName, RMStatusPanel panel, RRMCache data) {
        // Store data
        rrmCacheMap.put(rrmName, data);
        panels.put(rrmName, panel);

        // Place components and ensure repaint for the parent (frame)
        GSFrame frame = windows.get(rgsName);
        frame.addStatusPanel(panel);

        if (running) {
            // A RM has joined after start, ensure it gets painted.
            System.out.println("Force repaint");
            panel.revalidate();
        }
    }

    /**
     * Starts the UI, in this case all the frames (windows) for each RGS
     */
    public void start() {
        for (GSFrame frame : windows.values()) {
            frame.start();
        }
        running = true;
    }

    /**
     * Updates the data behind an RGS and causes it's frame to update.
     * @param rgsName is the name of the RGS whose data needs to be updated
     * @throws RemoteException
     */
    public void updateRGS(String rgsName) throws RemoteException {
        rgsCacheMap.get(rgsName).invalidate();
        windows.get(rgsName).revalidate();
        windows.get(rgsName).repaint();
    }

    /**
     * Updates the data behind an RMM and causes it's panel to update
     * @param rrmName is the name of the RMM whose data needs to be updated
     * @throws RemoteException
     */
    public void updateRRM(String rrmName) throws RemoteException {
        rrmCacheMap.get(rrmName).invalidate();
        panels.get(rrmName).revalidate();
        panels.get(rrmName).repaint();
    }

    /**
     * Updates a part of the data behind an RMM, specifically a single node and causes the RMM's panel to update.
     * @param rrmName is the name of the RMM whose data needs to be updated
     * @param nodeName is the name of the Node whose data is changed
     * @param nodeStatus is the value of the new data for the Node
     */
    public void updateNode(String rrmName, String nodeName, NodeStatus nodeStatus) {
        System.out.printf("Changing node %s to %s\n", nodeName, nodeStatus.name());

        rrmCacheMap.get(rrmName).refreshSingleNode(nodeName, nodeStatus);
        panels.get(rrmName).revalidate();
        panels.get(rrmName).repaint();
    }

    public void reportDeadRRM(String rrmName) {
        if (rrmCacheMap.get(rrmName).isSourceDead()) {
            panels.get(rrmName).flagDead(true);
        }
    }
}
