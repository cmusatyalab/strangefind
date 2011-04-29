/*
 *  StrangeFind, an anomaly detector for the OpenDiamond platform
 *
 *  Copyright (c) 2007-2011 Carnegie Mellon University
 *  All rights reserved.
 *
 *  StrangeFind is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2.
 *
 *  StrangeFind is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with StrangeFind. If not, see <http://www.gnu.org/licenses/>.
 *
 *  Linking StrangeFind statically or dynamically with other modules is
 *  making a combined work based on StrangeFind. Thus, the terms and
 *  conditions of the GNU General Public License cover the whole
 *  combination.
 *
 *  In addition, as a special exception, the copyright holders of
 *  StrangeFind give you permission to combine StrangeFind with free software
 *  programs or libraries that are released under the GNU LGPL or the
 *  Eclipse Public License 1.0. You may copy and distribute such a system
 *  following the terms of the GNU GPL for StrangeFind and the licenses of
 *  the other code concerned, provided that you include the source code of
 *  that other code when and as the GNU GPL requires distribution of source
 *  code.
 *
 *  Note that people who make modified versions of StrangeFind are not
 *  obligated to grant this special exception for their modified versions;
 *  it is their choice whether to do so. The GNU General Public License
 *  gives permission to release a modified version without this exception;
 *  this exception also makes it possible to release a modified version
 *  which carries forward this exception.
 */

package edu.cmu.cs.diamond.strangefind.search;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.*;

import edu.cmu.cs.diamond.opendiamond.*;
import edu.cmu.cs.diamond.strangefind.Annotator;
import edu.cmu.cs.diamond.strangefind.Decorator;
import edu.cmu.cs.diamond.strangefind.LogicEngine;
import edu.cmu.cs.diamond.strangefind.ResultViewer;
import edu.cmu.cs.diamond.strangefind.StrangeFindSearch;

public class OOMuscleAnomalyFilter implements StrangeFindSearch {
    private static final String[] LABELS = { "oomuscle.avg_width",
            "oomuscle.stddev_width", "oomuscle.min_width",
            "oomuscle.max_width" };

    private static final String[] NICE_LABELS = { "Average width",
            "Width std. dev.", "Minimum width", "Maximum width" };

    final private JSpinner ignoreSpinner = new JSpinner(new SpinnerNumberModel(
            5, 0, 100, 1));

    final private JCheckBox[] checkboxes = new JCheckBox[LABELS.length];

    final private JSpinner[] stddevs = new JSpinner[LABELS.length];

    public OOMuscleAnomalyFilter() {
        // init GUI elements
        for (int i = 0; i < NICE_LABELS.length; i++) {
            checkboxes[i] = new JCheckBox(NICE_LABELS[i]);

            JSpinner s = new JSpinner(
                    new SpinnerNumberModel(3.0, 1.0, 7.0, 0.5));
            stddevs[i] = s;
        }

        checkboxes[0].setSelected(true);
    }

    @Override
    public JPanel getInterface() {
        JPanel result = new JPanel();
        result.setBorder(BorderFactory.createTitledBorder("OO Muscle Anomaly Detector"));
        result.setLayout(new SpringLayout());

        result.add(new JLabel("Priming count"));
        result.add(ignoreSpinner);

        result.add(new JLabel(" "));
        result.add(new JLabel(" "));

        result.add(new JLabel("Descriptor"));
        result.add(new JLabel("Std. dev."));
        for (int i = 0; i < LABELS.length; i++) {
            result.add(checkboxes[i]);
            result.add(stddevs[i]);
        }

        Util.makeCompactGrid(result, LABELS.length + 3, 2, 5, 5, 2, 2);

        return result;
    }

    @Override
    public Filter[] getFilters() {
        Filter rgb = null;  // only for thumbnail
        Filter thumb = null;
        Filter oomuscle = null;
        Filter anom = null;
        try {
            FilterCode c;

            c = new FilterCode(new FileInputStream("/opt/snapfind/lib/fil_rgb"));
            rgb = new Filter("rgb", c, 1, Arrays.asList(new String[0]),
                    Arrays.asList(new String[0]));

            c = new FilterCode(new FileInputStream("/opt/snapfind/lib/fil_thumb"));
            thumb = new Filter("thumb", c, 1,
                    Arrays.asList(new String[] { "rgb" }),
                    Arrays.asList(new String[] {
                                    Integer.toString(ResultViewer
                                            .getPreferredWidth()),
                                    Integer.toString(ResultViewer
                                            .getPreferredHeight()) }));

            FileInputStream fis = new FileInputStream("/opt/snapfind/lib/fil_oomuscle.zip");
            ZipInputStream zip = new ZipInputStream(fis);
            ZipEntry entry;
            c = null;
            byte blob[] = new byte[0];
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals("filter")) {
                    c = new FilterCode(zip);
                } else if (name.equals("blob")) {
                    blob = Util.readFully(zip);
                }
            }
            zip.close();
            fis.close();
            if (c == null) {
                throw new IOException("Missing filter code");
            }
            oomuscle = new Filter("oomuscle", c, 1,
                    Arrays.asList(new String[0]),
                    Arrays.asList(new String[] { "0", "0", "0" }), blob);

            List<String> paramsList = new ArrayList<String>();
            for (int i = 0; i < checkboxes.length; i++) {
                paramsList.add(LABELS[i]);
                paramsList.add(stddevs[i].getValue().toString());
            }

            StringBuilder logicalExpression = new StringBuilder();
            boolean anySelected = false;
            boolean exactlyOneSelected = false;
            String lastSelected = null;
            logicalExpression.append("OR(");
            for (int i = 0; i < checkboxes.length; i++) {
                if (checkboxes[i].isSelected()) {
                    if (anySelected) {
                        exactlyOneSelected = false;
                        logicalExpression.append(",");
                    } else {
                        exactlyOneSelected = true;
                    }
                    lastSelected = "$" + (i + 1);
                    logicalExpression.append(lastSelected);
                    anySelected = true;
                }
            }
            logicalExpression.append(")");

            if (!anySelected) {
                logicalExpression = new StringBuilder(); // clear
            } else if (exactlyOneSelected) {
                logicalExpression = new StringBuilder(lastSelected);
            }

            String anomArgs[] = new String[paramsList.size() + 3];
            anomArgs[0] = ignoreSpinner.getValue().toString(); // skip
            anomArgs[1] = UUID.randomUUID().toString(); // random value
            anomArgs[2] = LogicEngine
                    .getMachineCodeForExpression(logicalExpression.toString());
            System.arraycopy(paramsList.toArray(), 0, anomArgs, 3, paramsList
                    .size());
            c = new FilterCode(new FileInputStream("/opt/snapfind/lib/fil_anomaly"));
            anom = new Filter("anomaly", c, 1,
                    Arrays.asList(new String[] { "oomuscle" }),
                    Arrays.asList(anomArgs));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Filter[] { rgb, thumb, oomuscle, anom };
    }

    @Override
    public Annotator getAnnotator() {
        final List<String> selectedLabels = new ArrayList<String>();
        final List<String> niceSelectedLabels = new ArrayList<String>();
        for (int i = 0; i < checkboxes.length; i++) {
            JCheckBox c = checkboxes[i];
            if (c.isSelected()) {
                selectedLabels.add(LABELS[i]);
                niceSelectedLabels.add(NICE_LABELS[i]);
            }
        }

        return new Annotator() {
            public String annotate(Result r) {

                boolean useHTML = true;

                DecimalFormat df = new DecimalFormat("0.###");
                StringBuilder sb = new StringBuilder();

                if (useHTML) {
                    sb.append("<html>");
                }

                String server = r.getServerName();
                int samples = getSamples(r, 0);

                for (int i = 0; i < LABELS.length; i++) {
                    boolean isA = getIsAnomalous(r, i);

                    if (useHTML) {
                        sb.append("<p>");
                    } else {
                        sb.append("\n\n");
                    }
                    if (isA) {
                        if (useHTML) {
                            sb.append("<b>");
                        }
                        sb.append("*");
                    }

                    String descriptor = NICE_LABELS[i];
                    double stddev = getStddev(r, i);
                    double value = getValue(r, i);
                    double mean = getMean(r, i);
                    double stddevDiff = getStddevDiff(stddev, value, mean);
                    String aboveOrBelow = getAboveOrBelow(stddevDiff, "+", "−");

                    sb.append(descriptor);
                    if (isA) {
                        if (useHTML) {
                            sb.append("</b>");
                        }
                    }
                    sb.append(" = " + format(mean, df) + " " + aboveOrBelow
                            + " " + format(Math.abs(stddevDiff), df) + "σ ("
                            + format(value, df) + ")");
                }

                if (useHTML) {
                    sb.append("<hr><p>" + server + " [" + samples +
                            "]</html>");
                } else {
                    sb.append("\n---\n\n" + server + " [" + samples + "]");
                }

                return sb.toString();
            }

            private double getValue(Result r, int descriptor) {
                double value = Util.extractDouble(r
                        .getValue("anomaly-descriptor-value-" + descriptor
                                + ".double"));
                return value;
            }

            private double getStddevDiff(double stddev, double value,
                    double mean) {
                double stddevDiff = (value - mean) / stddev;
                return stddevDiff;
            }

            private double getMean(Result r, int descriptor) {
                double mean = Util.extractDouble(r
                        .getValue("anomaly-descriptor-mean-" + descriptor
                                + ".double"));
                return mean;
            }

            private double getStddev(Result r, int descriptor) {
                double stddev = Util.extractDouble(r
                        .getValue("anomaly-descriptor-stddev-" + descriptor
                                + ".double"));
                return stddev;
            }

            private boolean getIsAnomalous(Result r, int descriptor) {
                int isAnomalous = Util.extractInt(r
                        .getValue("anomaly-descriptor-is_anomalous-"
                                + descriptor + ".int"));
                return isAnomalous == 1 ? true : false;
            }

            private String getAboveOrBelow(double stddevDiff, String above,
                    String below) {
                String aboveOrBelow = Math.signum(stddevDiff) >= 0.0 ? above
                        : below;
                return aboveOrBelow;
            }

            private int getSamples(Result r, int descriptor) {
                int samples = Util.extractInt(r
                        .getValue("anomaly-descriptor-count-" + descriptor
                                + ".int"));
                return samples;
            }

            private String format(double d, DecimalFormat df) {
                if (Double.isNaN(d)) {
                    return "NaN";
                } else {
                    return df.format(d);
                }
            }

            public String annotateTooltip(Result r) {
                return annotate(r);
            }

            @Override
            public String annotateOneLine(Result r) {
                return null;
            }

            @Override
            public String annotateVerbose(Result r) {
                return null;
            }

            @Override
            public String annotateNonHTML(Result r) {
                return "TODO: not implemented";
            }
        };
    }

    @Override
    public Decorator getDecorator() {
        return null;
    }

    @Override
    public DoubleComposer getDoubleComposer() {
        return new DoubleComposer() {
            public double compose(String key, double a, double b) {
                return a + b;
            }
        };
    }

    @Override
    public String[] getApplicationDependencies() {
        return new String[] { "thumb" };
    }

    @Override
    public Set<String> getPushAttributes() {
        Set<String> set = new HashSet<String>();
        String a = "anomaly-descriptor-";
        for (int i = 0; i < LABELS.length; i++) {
            set.add(a + "value-" + i + ".double");
            set.add(a + "mean-" + i + ".double");
            set.add(a + "stddev-" + i + ".double");
            set.add(a + "count-" + i + ".int");
            set.add(a + "is_anomalous-" + i + ".int");
        }
        set.add("thumbnail.jpeg");
        set.add("_cols.int");
        set.add("_rows.int");
        set.add("Device-Name");
        return set;
    }
}
