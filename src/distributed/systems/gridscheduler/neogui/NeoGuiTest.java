package distributed.systems.gridscheduler.neogui;

import javax.swing.*;

/**
 * DS:distributed.systems.gridscheduler.neogui.NeoGuiTest
 * Written by Glenn. Created on 2017-12-06 at 17:46.
 */
public class NeoGuiTest extends JFrame{

    public static void main(String[] args) {
        RGSStatusFrameData rgsData = new RGSDummy("GS1 - Alfred");
        RGSStatusFrame frame = new RGSStatusFrame(rgsData);
        RRMDummy rrmData = new RRMDummy("RM1 - James", 20, 0, 0, 0, 42);
        RRMStatusPanel panel = new RRMStatusPanel(rrmData);

        frame.setVisible(true);

        safeWait(500);
        frame.addPanel(panel);
        panel.revalidate();

        safeWait(500);
        rrmData.setBusyCount(20);
        rrmData.setQueueSize(27);
        panel.revalidate();
    }

    private static void safeWait(int timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
