/*
 *  SnapFind 2, the Java-based Diamond shell
 *
 *  Copyright (c) 2007-2008 Carnegie Mellon University
 *  All rights reserved.
 *
 *  SnapFind 2 is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2.
 *
 *  SnapFind 2 is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with SnapFind 2. If not, see <http://www.gnu.org/licenses/>.
 *
 *  Linking SnapFind 2 statically or dynamically with other modules is
 *  making a combined work based on SnapFind 2. Thus, the terms and
 *  conditions of the GNU General Public License cover the whole
 *  combination.
 * 
 *  In addition, as a special exception, the copyright holders of
 *  SnapFind 2 give you permission to combine SnapFind 2 with free software
 *  programs or libraries that are released under the GNU LGPL or the
 *  Eclipse Public License 1.0. You may copy and distribute such a system
 *  following the terms of the GNU GPL for SnapFind 2 and the licenses of
 *  the other code concerned, provided that you include the source code of
 *  that other code when and as the GNU GPL requires distribution of source
 *  code.
 *
 *  Note that people who make modified versions of SnapFind 2 are not
 *  obligated to grant this special exception for their modified versions;
 *  it is their choice whether to do so. The GNU General Public License
 *  gives permission to release a modified version without this exception;
 *  this exception also makes it possible to release a modified version
 *  which carries forward this exception.
 */

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
import static java.awt.event.KeyEvent.VK_V;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.*;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import edu.cmu.cs.diamond.opendiamond.*;
import edu.cmu.cs.diamond.snapfind2.search.CircleAnomalyFilter;
import edu.cmu.cs.diamond.snapfind2.search.NeuriteAnomalyFilter;
import edu.cmu.cs.diamond.snapfind2.search.NeuriteMultiplaneAnomalyFilter;
import edu.cmu.cs.diamond.snapfind2.search.XQueryAnomalyFilter;

public class SnapFind2 extends JFrame {

    public static final int INITIAL_SESSION_VARIABLES_UPDATE_INTERVAL = 5;

    final private static Preferences prefs = Preferences
            .userNodeForPackage(SnapFind2.class);

    public class SessionVariablesWindow extends JFrame {
        public SessionVariablesWindow() {
            super("Session Variables");
            setLocationByPlatform(true);
            setMinimumSize(new Dimension(500, 300));

            JButton save = new JButton("Save");
            JButton clear = new JButton("Clear");
            JButton load = new JButton("Load");

            save.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    saveVariables(globalSessionVariables);
                }
            });

            clear.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // clear on server
                    search.mergeSessionVariables(globalSessionVariables,
                            new DoubleComposer() {
                                public double compose(String key, double a,
                                        double b) {
                                    return 0;
                                }
                            });
                    sessionVariablesTableModel.fireTableDataChanged();
                }
            });

            load.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    loadVariables(globalSessionVariables);
                }
            });
            Box v = Box.createVerticalBox();

            Box h = Box.createHorizontalBox();

            final int neverValue = 150;
            final JSlider interval = new JSlider(5, neverValue,
                    INITIAL_SESSION_VARIABLES_UPDATE_INTERVAL);
            Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
            labelTable.put(Integer.valueOf(5), new JLabel("5 s"));
            labelTable.put(Integer.valueOf(30), new JLabel("30 s"));
            labelTable.put(Integer.valueOf(60), new JLabel("1 m"));
            labelTable.put(Integer.valueOf(120), new JLabel("2 m"));
            labelTable.put(Integer.valueOf(neverValue), new JLabel("Never"));
            interval.setLabelTable(labelTable);

            interval.setPaintLabels(true);
            h.add(new JLabel("Synchronization Interval"));
            h.add(interval);

            interval.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if (interval.getValueIsAdjusting()) {
                        return;
                    }
                    int val = interval.getValue();
                    if (val == neverValue) {
                        val = -1;
                    }
                    results.setSessionVariableUpdateInterval(val);
                }
            });

            v.add(h);

            h = Box.createHorizontalBox();
            h.add(save);
            h.add(Box.createGlue());
            h.add(clear);
            h.add(Box.createGlue());
            h.add(load);

            v.add(h);

            add(v, BorderLayout.SOUTH);

            JTable t = new JTable(sessionVariablesTableModel);
            t.setTableHeader(null);
            JScrollPane jsp = new JScrollPane(t);
            add(jsp);

            pack();
        }

        JFileChooser loadChooser = new JFileChooser();

        protected void loadVariables(Map<String, Double> sv) {
            loadChooser.setDialogTitle("Load Session Variables");
            int returnVal = loadChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                sv.clear();
                XMLDecoder d;
                try {
                    d = new XMLDecoder(new BufferedInputStream(
                            new FileInputStream(loadChooser.getSelectedFile())));
                    Object result = d.readObject();
                    d.close();
                    sv
                            .putAll((Map<? extends String, ? extends Double>) (result));
                    sessionVariablesTableModel.fireTableDataChanged();
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        JFileChooser saveChooser = new JFileChooser();

        protected void saveVariables(Map<String, Double> sv) {
            saveChooser.setDialogTitle("Save Session Variables");
            int returnVal = saveChooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                XMLEncoder e;
                try {
                    e = new XMLEncoder(
                            new BufferedOutputStream(new FileOutputStream(
                                    saveChooser.getSelectedFile())));
                    e.writeObject(sv);
                    e.close();
                } catch (FileNotFoundException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }
    }

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

    final protected SearchList searchList = new SearchList();

    final protected JButton defineScopeButton = new JButton("Define Scope");

    final protected JButton startButton = new JButton("Start");

    final protected JButton stopButton = new JButton("Stop");

    final protected Search search = Search.getSharedInstance();

    final private Map<String, Double> globalSessionVariables = new TreeMap<String, Double>();

    final public AbstractTableModel sessionVariablesTableModel = new AbstractTableModel() {
        public int getColumnCount() {
            return 2;
        }

        public int getRowCount() {
            return globalSessionVariables.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            // inefficient?
            switch (columnIndex) {
            case 0:
                return globalSessionVariables.keySet().toArray()[rowIndex];
            case 1:
                return globalSessionVariables.values().toArray()[rowIndex];
            default:
                return null;
            }
        }
    };

    final protected ThumbnailBox results = new ThumbnailBox(
            globalSessionVariables, sessionVariablesTableModel);

    private JFrame progressWindow;

    private JFrame sessionVariablesWindow;

    public SnapFind2() {
        super("Diamond Shell");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setupMenu();

        // buttons
        defineScopeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ScopeSource.commitScope();
            }
        });

        stopButton.setEnabled(false);

        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // start
                startButton.setEnabled(false);
                prepareSearch();

                // XXX
                Annotator[] ans = searchList.getAnnotators();
                Decorator[] des = searchList.getDecorators();
                DoubleComposer[] coms = searchList.getDoubleComposers();

                if (ans.length > 0) {
                    results.setAnnotator(ans[0]);
                }
                if (des.length > 0) {
                    results.setDecorator(des[0]);
                }
                if (coms.length > 0) {
                    results.setDoubleComposer(coms[0]);
                }

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
        // load scope
        search.setScope(ScopeSource.getPredefinedScopeList().get(0));

        // read all enabled searches
        Filter[] filters = searchList.getFilters();

        // set up search
        if (filters.length == 0) {
            search.setSearchlet(null);
        } else {
            Searchlet s = new Searchlet();

            for (Filter ff : filters) {
                s.addFilter(ff);
            }

            s.setApplicationDependencies(searchList
                    .getApplicationDependencies());
            search.setSearchlet(s);
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

        Box v1 = Box.createVerticalBox();
        Box r2 = Box.createHorizontalBox();
        r2.add(defineScopeButton);
        v1.add(r2);
        v1.add(Box.createVerticalStrut(4));

        Box r1 = Box.createHorizontalBox();
        r1.add(startButton);
        r1.add(Box.createHorizontalStrut(20));
        stopButton.setEnabled(false);
        r1.add(stopButton);

        v1.add(r1);
        c1.add(v1);

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

        // 
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

        mi = createMenuItem("Session Variables Window", VK_V,
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        showSessionVariablesWindow();
                    }
                });
        mi.setAccelerator(KeyStroke.getKeyStroke(VK_V, CTRL_DOWN_MASK));
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

        // Help
        menu = new JMenu("Help");
        menu.add(createMenuItem("About", VK_A, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAboutBox();
            }
        }));

        jmb.add(menu);
        
        setJMenuBar(jmb);
    }

    protected void showAboutBox() {
        JOptionPane.showMessageDialog(this, "The Diamond Shell\n" +
        		"Copyright 2007-2008 Carnegie Mellon University\n" +
        		"Licensed under the GNU GPL v2");
    }

    protected void showSessionVariablesWindow() {
        if (sessionVariablesWindow == null) {
            sessionVariablesWindow = new SessionVariablesWindow();
        }
        sessionVariablesWindow.setVisible(true);
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

        mi = new JMenuItem("Neurite Anomaly Detector");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchList.addSearch(new NeuriteAnomalyFilter());
            }
        });
        itemNew.add(mi);

        mi = new JMenuItem("Neurite Anomaly Detector (Multiplane)");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchList.addSearch(new NeuriteMultiplaneAnomalyFilter());
            }
        });
        itemNew.add(mi);

        mi = new JMenuItem("XQuery Anomaly Detector");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchList.addSearch(new XQueryAnomalyFilter(SnapFind2.this));
            }
        });
        itemNew.add(mi);
    }

    static String getImageHost() {
        String host = prefs.get("http-image-host", "localhost");
        return host;
    }

    public static void main(String[] args) {
        SnapFind2 sf = new SnapFind2();
        sf.setLocationByPlatform(true);
        sf.setVisible(true);
    }
}
