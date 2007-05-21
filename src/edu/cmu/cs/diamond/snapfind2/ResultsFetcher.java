package edu.cmu.cs.diamond.snapfind2;

import java.util.concurrent.BlockingQueue;

import javax.swing.JButton;

import edu.cmu.cs.diamond.opendiamond.Result;
import edu.cmu.cs.diamond.opendiamond.Search;

public class ResultsFetcher implements Runnable {
    final private ThumbnailBox results;

    private Search search;

    final private JButton startButton;

    final private JButton stopButton;

    public ResultsFetcher(ThumbnailBox results, JButton startButton,
            JButton stopButton) {
        this.results = results;
        this.startButton = startButton;
        this.stopButton = stopButton;
    }

    public void setSearch(Search search) {
        this.search = search;
    }

    public void run() {
        while (true) {
            boolean done = false;
            Result r = search.getNextResult();
            System.out.println(" *** thread "
                    + Thread.currentThread().toString() + " got result " + r);

            if (r == null) {
                results.stop();
                done = true;
            }

            results.addResult(r);
            System.out.println("snapfind put result");

            if (done) {
                break;
            }
        }

        // update buttons
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }
}
