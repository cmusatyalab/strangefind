package edu.cmu.cs.diamond.snapfind2;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_C;
import static java.awt.event.KeyEvent.VK_D;
import static java.awt.event.KeyEvent.VK_E;
import static java.awt.event.KeyEvent.VK_H;
import static java.awt.event.KeyEvent.VK_I;
import static java.awt.event.KeyEvent.VK_L;
import static java.awt.event.KeyEvent.VK_N;
import static java.awt.event.KeyEvent.VK_O;
import static java.awt.event.KeyEvent.VK_P;
import static java.awt.event.KeyEvent.VK_Q;
import static java.awt.event.KeyEvent.VK_S;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.cmu.cs.diamond.opendiamond.*;
import edu.cmu.cs.diamond.snapfind2.search.CircleAnomalyFilter;

public class SnapFind2 extends JFrame {

    public class ProgressWindow extends JFrame {
        final private SortedMap<String, JProgressBar> servers = new TreeMap<String, JProgressBar>();

        // slightly hacky way to measure number since last hit
        final private Map<String, int[]> lastInfo = new HashMap<String, int[]>();

        final private Box v = Box.createVerticalBox();

        final private Timer statsTimer = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (search == null) {
                    return;
                }

                ServerStatistics stats[] = search.getStatistics();
                boolean revalidate = false;

                // clear all
                for (String key : servers.keySet()) {
                    JProgressBar jp = servers.get(key);
                    jp.setString(key.toLowerCase() + ": No connection");
                    jp.setValue(0);
                }

                // update
                for (ServerStatistics s : stats) {
                    String name = s.getAddress().getHostName();
                    JProgressBar jp = servers.get(name);
                    if (jp == null) {
                        // create new
                        jp = new JProgressBar();
                        jp.setStringPainted(true);
                        servers.put(name, jp);
                        revalidate = true;
                    }

                    int info[] = lastInfo.get(name);
                    if (info == null) {
                        // create new
                        info = new int[2];
                        lastInfo.put(name, info);
                    }

                    int total = s.getTotalObjects();
                    int processed = s.getProcessedObjects();
                    int dropped = s.getDroppedObjects();
                    int hits = processed - dropped;

                    if (hits != info[0] || processed < info[1]) {
                        info[0] = hits;
                        info[1] = processed;
                    }

                    int processedSinceLastHit = processed - info[1];

                    jp.setMaximum(total);
                    jp.setValue(processed);
                    String str = name.toLowerCase() + ": Total: " + total
                            + ", Searched: " + processed + ", Dropped: "
                            + dropped + ", Since last hit: "
                            + processedSinceLastHit;
                    jp.setString(str);
                }

                if (revalidate) {
                    v.removeAll();
                    for (JProgressBar jp : servers.values()) {
                        v.add(jp);
                    }
                    validate();
                    pack();
                }
            }
        });

        public ProgressWindow() {
            super("Progress Window");
            setLocationByPlatform(true);

            add(v);

            setMinimumSize(new Dimension(500, 300));

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentHidden(ComponentEvent e) {
                    statsTimer.stop();
                }

                @Override
                public void componentShown(ComponentEvent e) {
                    statsTimer.start();
                }
            });
        }
    }

    final private List<Scope> scopes = ScopeSource.getPredefinedScopeList();

    final protected SearchList searchList = new SearchList();

    final protected JButton startButton = new JButton("Start");

    final protected JButton stopButton = new JButton("Stop");

    final protected Search search = Search.getSharedInstance();

    final protected ThumbnailBox results = new ThumbnailBox();

    private JMenu scopeMenu;

    private JFrame progressWindow;

    public SnapFind2() {
        super("Diamond Shell");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setupMenu();

        // buttons
        stopButton.setEnabled(false);

        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // start
                startButton.setEnabled(false);
                prepareSearch();

                // XXX
                results.setAnnotator(searchList.getAnnotators()[0]);
                results.setDecorator(searchList.getDecorators()[0]);

                results.start(search);
            }
        });

        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // stop
                System.out.println(" *** stop search");
                stopButton.setEnabled(false);
                results.stop();
            }
        });

        search.addSearchEventListener(new SearchEventListener() {
            public void searchStopped(SearchEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        startButton.setEnabled(true);
                        stopButton.setEnabled(false);
                    }
                });
            }

            public void searchStarted(SearchEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        stopButton.setEnabled(true);
                    }
                });
            }
        });

        setupWindow();

        pack();
    }

    protected void prepareSearch() {
        // read all enabled searches
        Filter[] filters = searchList.getFilters();

        // set up search
        FilterCode fc;
        try {
            fc = new FilterCode(new FileInputStream(
                    "/opt/snapfind/lib/fil_rgb.a"));
            Filter f = new Filter("rgb", fc, "f_eval_img2rgb",
                    "f_init_img2rgb", "f_fini_img2rgb", 1, new String[0],
                    new String[0], 400);
            Searchlet s = new Searchlet();
            s.addFilter(f);

            for (Filter ff : filters) {
                s.addFilter(ff);
            }

            s.setApplicationDependencies(new String[] { "rgb" });
            search.setSearchlet(s);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupWindow() {
        Box b = Box.createHorizontalBox();
        // b.setPreferredSize(new Dimension(850, 540));
        add(b);

        // left side
        Box c1 = Box.createVerticalBox();
        c1.add(searchList);

        Dimension minSize = new Dimension(100, 5);
        Dimension prefSize = new Dimension(250, 5);
        Dimension maxSize = new Dimension(250, 5);

        JComponent filler = new Box.Filler(minSize, prefSize, maxSize);
        // filler.setBorder(BorderFactory.createEtchedBorder());
        c1.add(filler);

        Box r1 = Box.createHorizontalBox();
        r1.add(startButton);
        r1.add(Box.createHorizontalStrut(20));
        stopButton.setEnabled(false);
        r1.add(stopButton);
        c1.add(r1);

        b.add(c1);
        // b.add(new JSeparator(SwingConstants.VERTICAL));
        // b.add(Box.createHorizontalGlue());

        // right side
        Box c2 = Box.createVerticalBox();
        c2.add(results);
        b.add(c2);
    }

    private void setupMenu() {
        JMenuBar jmb = new JMenuBar();

        JMenu menu;
        JMenuItem mi;

        // Search
        menu = new JMenu("Search");
        menu.setMnemonic(VK_S);

        JMenu itemNew = new JMenu("New");
        itemNew.setMnemonic(VK_N);
        menu.add(itemNew);
        populateFiltersMenu(itemNew);

        menu.add(createMenuItem("New From Example...", VK_E,
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        newSearchFromExample();
                    }
                }));
        menu.addSeparator();

        menu.add(createMenuItem("Open...", VK_O, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openSearch();
            }
        }));
        menu.add(createMenuItem("Import...", VK_I, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                importSearch();
            }
        }));
        menu.add(createMenuItem("Save As...", VK_A, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveAsSearch();
            }
        }));
        menu.addSeparator();
        mi = createMenuItem("Quit", VK_Q, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        menu.add(mi);

        jmb.add(menu);

        // Scope
        scopeMenu = new JMenu("Scope");
        scopeMenu.setMnemonic(VK_C);
        populateScopeMenu(scopeMenu);
        jmb.add(scopeMenu);

        // Debug
        menu = new JMenu("Debug");
        menu.setMnemonic(VK_D);
        mi = createMenuItem("Stats Window", VK_S, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showStatsWindow();
            }
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(VK_I, CTRL_DOWN_MASK));
        menu.add(mi);

        mi = createMenuItem("Progress Window", VK_P, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showProgressWindow();
            }
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(VK_P, CTRL_DOWN_MASK));
        menu.add(mi);

        mi = createMenuItem("Log Window", VK_L, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showLogWindow();
            }
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(VK_L, CTRL_DOWN_MASK));
        menu.add(mi);

        mi = createMenuItem("Cache Window", VK_C, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showCacheWindow();
            }
        });
        mi.setAccelerator(KeyStroke.getKeyStroke(VK_H, CTRL_DOWN_MASK));
        menu.add(mi);

        jmb.add(menu);

        setJMenuBar(jmb);
    }

    protected void showCacheWindow() {
        // TODO Auto-generated method stub

    }

    protected void showProgressWindow() {
        if (progressWindow == null) {
            progressWindow = new ProgressWindow();
        }
        progressWindow.setVisible(true);
    }

    protected void showLogWindow() {
        // TODO Auto-generated method stub

    }

    protected void showStatsWindow() {
        // TODO Auto-generated method stub

    }

    private void populateScopeMenu(JMenu menu) {
        JRadioButtonMenuItem first = null;
        ButtonGroup bg = new ButtonGroup();

        for (final Scope s : scopes) {
            final JRadioButtonMenuItem mi = new JRadioButtonMenuItem(s
                    .getName());
            bg.add(mi);
            mi.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if (mi.isSelected()) {
                        System.out.println("Setting scope to " + s);
                        search.setScope(s);
                    }
                }
            });

            if (first == null) {
                first = mi;
                first.setSelected(true);
            }
            menu.add(mi);
        }
    }

    protected void newSearchFromExample() {
        // TODO
    }

    protected void saveAsSearch() {
        // TODO Auto-generated method stub

    }

    protected void importSearch() {
        // TODO Auto-generated method stub

    }

    private JMenuItem createMenuItem(String title, int mnemonic,
            ActionListener a) {
        JMenuItem mi;
        mi = new JMenuItem(title, mnemonic);
        mi.addActionListener(a);
        return mi;
    }

    protected void openSearch() {
        // TODO Auto-generated method stub

    }

    private void populateFiltersMenu(JMenu itemNew) {
        // XXX do this differently
        JMenuItem mi = new JMenuItem("Circle Anomaly Detector");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchList.addSearch(new CircleAnomalyFilter());
            }
        });
        itemNew.add(mi);
    }

    public static void main(String[] args) {
        SnapFind2 sf = new SnapFind2();
        sf.setLocationByPlatform(true);
        sf.setVisible(true);
    }
}
