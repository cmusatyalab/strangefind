package edu.cmu.cs.diamond.snapfind2;

import javax.swing.JProgressBar;

import edu.cmu.cs.diamond.opendiamond.ServerStatistics;

public class StatisticsBar extends JProgressBar {
    public StatisticsBar() {
        super();
        setStringPainted(true);
        clear();
    }

    public void clear() {
        setNumbers(0, 0, 0);
    }

    private void setNumbers(int total, int searched, int dropped) {
        setIndeterminate(false);
        setString("Total: " + total + ", Searched: " + searched + ", Dropped: "
                + dropped);
        setMaximum(total);
        setValue(searched);
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
    
    public void setIndeterminateMessage(String message) {
        setIndeterminate(true);
        setString(message);
    }
}
