package edu.cmu.cs.diamond.snapfind2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import edu.cmu.cs.diamond.opendiamond.Result;

public class ThumbnailBox extends JPanel implements Runnable, ActionListener {
    private int nextEmpty = 0;

    final private ResultViewer[] pics = new ResultViewer[6];

    final private BlockingQueue<Result> q = new ArrayBlockingQueue<Result>(6);

    final private JButton nextButton = new JButton("Next");

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

        nextButton.setEnabled(false);
        nextButton.addActionListener(this);
    }

    public BlockingQueue<Result> getQueue() {
        return q;
    }

    public JButton getButton() {
        return nextButton;
    }

    private boolean isFull() {
        return nextEmpty >= pics.length;
    }

    void clearAll() {
        nextEmpty = 0;
        for (ResultViewer r : pics) {
            r.setResult(null);
        }
    }

    private void fillNext(Result r) {
        System.out.println("fillNext " + r);
        pics[nextEmpty++].setResult(r);
    }

    public void run() {
        while (true) {
            Result r;
            try {
                System.out.println("waiting on queue");
                r = q.take();
            } catch (InterruptedException e) {
                // try again
                continue;
            }
            if (r == null) {
                // no more elements
                System.out.println("no more elements from queue");
                return;
            } else {
                synchronized (pics) {
                    while (true) {
                        if (isFull()) {
                            try {
                                nextButton.setEnabled(true);
                                pics.wait();
                            } catch (InterruptedException e) {
                                // loop around again
                                continue;
                            }
                        }

                        // add one
                        final Result r2 = r;
                        System.out.println("thumbnail got result");
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                public void run() {
                                    fillNext(r2);
                                }
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        // next is clicked
        nextButton.setEnabled(false);
        synchronized (pics) {
            clearAll();
            pics.notify();
        }
    }
}
