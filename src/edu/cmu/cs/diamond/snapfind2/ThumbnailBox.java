package edu.cmu.cs.diamond.snapfind2;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import edu.cmu.cs.diamond.opendiamond.Result;
import edu.cmu.cs.diamond.opendiamond.Search;

public class ThumbnailBox extends JPanel implements ActionListener {
    public class StatsGatherer implements Runnable {
        public void run() {
            while(running) {
                stats.update(search.getStatistics());
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    volatile protected int nextEmpty = 0;

    final static private int ROWS = 3;
    final static private int COLS = 3;
    
    final protected ResultViewer[] pics = new ResultViewer[ROWS * COLS];

    final protected JButton nextButton = new JButton("Next");

    private Thread resultGatherer;

    volatile protected boolean running;

    protected Search search;

    final protected Object fullSynchronizer = new Object();

    private Annotator annotator;

    private Decorator decorator;

    final protected StatisticsBar stats = new StatisticsBar();

    public ThumbnailBox() {
        super();
        
        Box v = Box.createVerticalBox();
        add(v);
        // v.setBorder(BorderFactory.createEtchedBorder(Color.RED, Color.BLUE));
        Box h = null;
        for (int i = 0; i < pics.length; i++) {
            boolean addBox = false;
            if (i % COLS == 0) {
                h = Box.createHorizontalBox();
                addBox = true;
            }

            ResultViewer b = new ResultViewer();

            h.add(b);
            pics[i] = b;

            if (addBox) {
                v.add(h);
            }
        }

        h = Box.createHorizontalBox();
        h.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        h.add(stats);
        h.add(Box.createHorizontalStrut(10));

        h.add(nextButton);
        nextButton.setEnabled(false);
        nextButton.addActionListener(this);
        
        v.add(h);
    }

    public void setAnnotator(Annotator a) {
        annotator = a;
    }
    
    public void setDecorator(Decorator d) {
        decorator = d;
    }
    
    protected boolean isFull() {
        return nextEmpty >= pics.length;
    }

    private void clearAll() {
        nextEmpty = 0;
        for (ResultViewer r : pics) {
            r.setResult(null);
            r.commitResult();
        }
    }

    protected void fillNext(Result r) throws InterruptedException {
        System.out.println("fillNext " + r);
        if (!running) {
            return;
        }

        // update
        final ResultViewer v = pics[nextEmpty++];

        // loading message
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    v.setText("Loading");
                }
            });
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
        }

        String annotation = null;
        String tooltipAnnotation = null;
        if (annotator != null) {
            annotation = annotator.annotate(r);
            tooltipAnnotation = annotator.annotateTooltip(r);
        }
        v.setResult(new AnnotatedResult(r, annotation, tooltipAnnotation, decorator));

        if (!running) {
            // reset
            v.setResult(null);
        }

        // update GUI
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    v.setText(null);
                    v.commitResult();
                }
            });
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    protected class ResultsGatherer implements Runnable {
        public void run() {
            try {
                while (running) {
                    // wait for next item
                    System.out.println("wait for next item...");
                    Result r = search.getNextResult();
                    System.out.println(" " + r);

                    if (r != null) {
                        // we have data

                        if (isFull()) {
                            // wait
                            synchronized (fullSynchronizer) {
                                while (isFull()) {
                                    nextButton.setEnabled(true);
                                    fullSynchronizer.wait();
                                }

                                // no longer full
                                fillNext(r);
                            }
                        } else {
                            // not full
                            fillNext(r);
                        }
                    } else {
                        // no more objects
                        running = false;
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("INTERRUPTED !");
            } finally {
                System.out.println("FINALLY stopping search");
                search.stopSearch();
                System.out.println(" done");

                // clear anything not shown
                nextButton.setEnabled(false);

                // send event
                
                
                System.out.println("done with finally");
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        // next is clicked
        nextButton.setEnabled(false);
        synchronized (fullSynchronizer) {
            clearAll();
            fullSynchronizer.notify();
        }
    }

    public void stop() {
        running = false;

        if (resultGatherer != null) {
            // interrupt anything
            resultGatherer.interrupt();

            // wait for exit
            try {
                System.out.print("joining...");
                System.out.flush();
                resultGatherer.join();
                System.out.println(" done");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        resultGatherer = null;
    }

    public void start(Search s) {
        stop();

        search = s;

        clearAll();

        System.out.println("start search");
        search.startSearch();

        running = true;

        // stats gatherer
        new Thread(new StatsGatherer()).start();
        (resultGatherer = new Thread(new ResultsGatherer())).start();
    }
}
