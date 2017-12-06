package distributed.systems.gridscheduler.gui;

import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel that reports information about components in the VGS. 
 * 
 * @author Niels Brouwers
 *
 */
public abstract class StatusPanel extends JPanel  {

	private static final long serialVersionUID = 4063096546482156186L;

	protected boolean dead = false;

    protected final ActionListener actionListener = e -> {repaint(); revalidate();};

    public void flagDead(boolean dead) {
        this.dead = dead;
    }
}
