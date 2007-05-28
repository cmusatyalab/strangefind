package edu.cmu.cs.diamond.snapfind2;

import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.cmu.cs.diamond.opendiamond.ServerStatistics;

public class StatisticsBar extends JPanel {
    final private JLabel total = new JLabel();

    final private JLabel searched = new JLabel();

    final private JLabel dropped = new JLabel();

    public StatisticsBar() {
        setLayout(new BorderLayout());
        
        Box b = Box.createHorizontalBox();
        b.add(total);
        b.add(Box.createHorizontalGlue());
        b.add(searched);
        b.add(Box.createHorizontalGlue());
        b.add(dropped);
        b.add(Box.createHorizontalGlue());

        add(b);
        
        clear();
    }

    public void clear() {
        setNumbers(0, 0, 0);
    }

    private void setNumbers(int total, int searched, int dropped) {
        this.total.setText("Total: " + total);
        this.searched.setText("Searched: " + searched);
        this.dropped.setText("Dropped: " + dropped);
    }

    public void update(ServerStatistics stats[]) {
        int t = 0;
        int s = 0;
        int d = 0;
        for (ServerStatistics ss : stats) {
            t += ss.getTotalObjects();
            s += ss.getProcessedObjects();
            d += ss.getDroppedObjects();
        }
        setNumbers(t, s, d);
    }
}
