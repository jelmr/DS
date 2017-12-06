package distributed.systems.gridscheduler.gui;

import java.awt.*;
import java.util.ArrayList;

import javax.swing.*;

import distributed.systems.gridscheduler.cache.RGSCache;



/**
 * 
 * The DebugPanel is a swing frame that displays information about the system. Use the
 * addStatusPanel function to addRGS StatusPanel objects to it, and the debug panel will
 * automatically refresh them at a set rate. 
 * 
 * @author Niels Brouwers, Boaz Pat-El
 *
 */
public class GSFrame extends JFrame {
	/**
	 * Generated serialverionUID
	 */
	private static final long serialVersionUID = 7764398835092386415L;
	
	// update at a rate of 10 frames/second 
	private static final int REFRESH_DELAY_MS = 250;

	// list of status panels, used for automatic updating
	private ArrayList<StatusPanel> statusPanels;
	private Panel panelForScrollPane;
	private ScrollPane scrollPane;

	private final Timer timer = new Timer(REFRESH_DELAY_MS, null);

	/**
	 * Constructs a new DebugPanel object. 
	 * Adds a status panel that displays the scheduler to the window.
	 * This is done so that the scheduler panel will always be on top.
	 * @param rgsCache The scheduler that is monitored by this Panel
	 */
	public GSFrame(RGSCache rgsCache) {
		super("Status");
		this.setSize(340, 680);
		this.setResizable(false);
		this.setLayout(null);
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		timer.addActionListener(e -> {repaint(); revalidate();});
		timer.setRepeats(true);
		timer.setCoalesce(true);
		
		statusPanels = new ArrayList<>();

		// Create the gridscheduler status panel and addRGS it to the frame
		GSStatusPanel schedulerStatusPanel = new GSStatusPanel(rgsCache);
		//statusPanels.addRGS(schedulerStatusPanel);
		this.add(schedulerStatusPanel);
		// Place and resize the status panel
		schedulerStatusPanel.setLocation(0, 0);
		schedulerStatusPanel.setSize(schedulerStatusPanel.getPreferredSize());
		
		// Now addRGS the scrollpane to the frame that contains all clusterpanels
		scrollPane = new ScrollPane();
		scrollPane.setSize(getSize().width - 8, getSize().height - 32);
		this.add(scrollPane);
		// Place the scrollpane beneath the gridscheduler status panel
		scrollPane.setLocation(0, schedulerStatusPanel.getSize().height);
		
		// Now resize the frame so that the gridscheduler status panel and
		// the scrollpane both can be displayed on the frame in full.
		setSize(getSize().width, getSize().height + schedulerStatusPanel.getSize().height);
		setPreferredSize(getSize());
		
		FlowLayout layout = new FlowLayout();
		layout.setAlignment(FlowLayout.LEFT);
		
		panelForScrollPane = new Panel(layout);
		panelForScrollPane.setSize(0, 0);
		
		scrollPane.add(panelForScrollPane);
		scrollPane.setWheelScrollingEnabled(true);
	}
	
	/**
	 * Adds a status panel to the window. Use this instead of the regular addRGS method to make
	 * sure the panel is updated automatically. 
	 * @param statusPanel panel to addRGS
	 */
	public void addStatusPanel(StatusPanel statusPanel)
	{
		//addRGS(statusPanel);
		statusPanels.add(statusPanel);
		panelForScrollPane.add(statusPanel);
		timer.addActionListener(statusPanel.actionListener);
		
		// Automatically increase the size of the panelForScrollPane, 
		// otherwise we cannot scroll to see all the clusters
		Dimension nextDimension = panelForScrollPane.getPreferredSize();
		nextDimension.height += statusPanel.getPreferredSize().height + RMStatusPanel.padding + 1;
		panelForScrollPane.setPreferredSize(nextDimension);
		if (timer.isRunning()) {
			System.out.println("Timer was running; restart...");
			// Make sure timer is still available.
			timer.restart();
		}
	}
	
	public void start() {
		this.setVisible(true);
		timer.start();
	}
	
}
