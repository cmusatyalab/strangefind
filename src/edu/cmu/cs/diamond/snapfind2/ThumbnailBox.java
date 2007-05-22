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
    volatile protected int nextEmpty = 0;

    final protected ResultViewer[] pics = new ResultViewer[6];

    final protected JButton nextButton = new JButton("Next");

    private Thread theThread;

    volatile protected boolean running;

    protected Search search;

    final protected Object fullSynchronizer = new Object();

    public ThumbnailBox() {
        Box v = Box.createVerticalBox();
        add(v);
        // v.setBorder(BorderFactory.createEtchedBorder(Color.RED, Color.BLUE));
        Box h = null;
        for (int i = 0; i < pics.length; i++) {
            boolean addBox = false;
            if (i % 3 == 0) {
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

        nextButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        v.add(nextButton);
        nextButton.setEnabled(false);
        nextButton.addActionListener(this);
    }

    protected boolean isFull() {
        return nextEmpty >= pics.length;
    }

    private void clearAll() {
        nextEmpty = 0;
        for (ResultViewer r : pics) {
            r.setResult(null);
            r.validateResult();
        }
    }

    protected void fillNext(Updater u, Result r) throws InterruptedException {
        System.out.println("fillNext " + r);
        if (!running) {
            return;
        }
        
        // update
        ResultViewer v = pics[nextEmpty++];
        v.setResult(r);
        
        if (!running) {
            // reset
            v.setResult(null);
        }
        u.setResultViewer(v);
        
        // update GUI
        try {
            SwingUtilities.invokeAndWait(u);
        } catch (InterruptedException e) {
            throw e;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    protected class Updater implements Runnable {
        private ResultViewer r;

        public void run() {
            r.validateResult();
        }

        public void setResultViewer(ResultViewer r) {
            this.r = r;
        }
    }

    protected class MyRunner implements Runnable {
        public void run() {
            Updater u = new Updater();

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
                                fillNext(u, r);
                            }
                        } else {
                            // not full
                            fillNext(u, r);
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

        if (theThread != null) {
            // interrupt anything
            theThread.interrupt();

            // wait for exit
            try {
                System.out.print("joining...");
                System.out.flush();
                theThread.join();
                System.out.println(" done");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        theThread = null;
    }

    public void start(Search s) {
        stop();

        search = s;

        clearAll();

        System.out.println("start search");
        search.startSearch();

        running = true;

        (theThread = new Thread(new MyRunner())).start();
    }
}
