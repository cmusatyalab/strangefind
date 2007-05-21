package edu.cmu.cs.diamond.snapfind2;

import java.awt.Component;
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

public class ThumbnailBox extends JPanel implements ActionListener {
    private static enum Command {
        STOP_COMMAND, START_COMMAND, RESULT_COMMAND
    };

    private static class CommandResult {
        final public static CommandResult STOP = new CommandResult(
                Command.STOP_COMMAND);

        final public static CommandResult START = new CommandResult(
                Command.START_COMMAND);

        final private Command c;

        final private Result r;

        public CommandResult(Command c) {
            if (c == Command.RESULT_COMMAND) {
                throw new IllegalArgumentException();
            }
            this.c = c;
            r = null;
        }

        public CommandResult(Result r) {
            c = Command.RESULT_COMMAND;
            this.r = r;
        }

        public Command getCommand() {
            return c;
        }

        public Result getResult() {
            return r;
        }
    }

    private int nextEmpty = 0;

    final private ResultViewer[] pics = new ResultViewer[6];

    final private BlockingQueue<CommandResult> q = new ArrayBlockingQueue<CommandResult>(
            1);

    final private JButton nextButton = new JButton("Next");

    final private Thread theThread = new Thread(new MyRunner());

    volatile private boolean running;

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
        theThread.start();
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

    private class Updater implements Runnable {
        private Result r;

        public void run() {
            fillNext(r);
        }

        public void setResult(Result r) {
            this.r = r;
        }
    }

    private class MyRunner implements Runnable {
        public void run() {
            Updater u = new Updater();

            while (true) {
                CommandResult r;
                try {
                    System.out.println("waiting on queue");
                    r = q.take();
                    System.out.println(" got item " + r.getCommand());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    r = CommandResult.STOP;
                }

                switch (r.getCommand()) {
                case RESULT_COMMAND:
                    Result rr = r.getResult();
                    try {
                            processResult(u, rr);
                        } catch (InterruptedException e) {
                            // stop
                            System.out.println(" *** INTERRUPTION");
                            running = false;
                            q.clear();
                            nextButton.setEnabled(false);
                        }
                    break;

                case START_COMMAND:
                    running = true;
                    
                    // clear display
                    clearAll();
                    break;

                case STOP_COMMAND:
                    running = false;
                    
                    // drain queue
                    q.clear();
                    break;
                }
            }
        }

        private void processResult(Updater u, Result r) throws InterruptedException {
            synchronized (pics) {
                while (isFull()) {
                    nextButton.setEnabled(true);
                    pics.wait();
                }

                // add one
                u.setResult(r);
                System.out.println("thumbnail got result");
                try {
                    SwingUtilities.invokeAndWait(u);
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
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
    
    public void addResult(Result r) {
        if (!running) {
            return;
        }
        
        try {
            q.put(new CommandResult(r));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        theThread.interrupt();
        q.clear();
    }

    public void start() {
        try {
            q.put(new CommandResult(Command.START_COMMAND));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
