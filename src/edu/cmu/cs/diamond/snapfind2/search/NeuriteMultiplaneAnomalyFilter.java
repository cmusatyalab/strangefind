/*
 *  StrangeFind, an anomaly detection system for the OpenDiamond Platform
 *
 *  Copyright (c) 2007-2008 Carnegie Mellon University
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

package edu.cmu.cs.diamond.snapfind2.search;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.*;

import edu.cmu.cs.diamond.opendiamond.*;
import edu.cmu.cs.diamond.snapfind2.Annotator;
import edu.cmu.cs.diamond.snapfind2.Decorator;
import edu.cmu.cs.diamond.snapfind2.SnapFindSearch;

final public class NeuriteMultiplaneAnomalyFilter implements SnapFindSearch {

    public NeuriteMultiplaneAnomalyFilter() {
        // init GUI elements
        for (int i = 0; i < NICE_LABELS.length; i++) {
            checkboxes[i] = new JCheckBox(NICE_LABELS[i]);
            checkboxes[i].setSelected(true);

            JSpinner s = new JSpinner(
                    new SpinnerNumberModel(3.0, 1.0, 7.0, 0.5));
            stddevs[i] = s;
        }
    }

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
                int key = Util.extractInt(r.getValue("anomalous-value.int"));
                String anomStr = "<html><p>Anomalous descriptor: <b>"
                        + niceSelectedLabels.get(key)
                        + "</b>: "
                        + Util.extractString(r
                                .getValue(selectedLabels.get(key)))
                        + "<p>mean: "
                        + Util.extractDouble(r
                                .getValue("anomalous-value-mean.double"))
                        + "<p>stddev: "
                        + Util.extractDouble(r
                                .getValue("anomalous-value-stddev.double"))
                        + "<p>object count: "
                        + Util.extractInt(r
                                .getValue("anomalous-value-count.int"))
                        + "<p>server: "
                        + Util.extractString(r.getValue("Device-Name"))
                        + "</html>";

                return anomStr;
            }

            public String annotateTooltip(Result r) {
                DecimalFormat df = new DecimalFormat("0.###");

                int key = Util.extractInt(r.getValue("anomalous-value.int"));
                String descriptor = niceSelectedLabels.get(key);

                double stddev = Util.extractDouble(r
                        .getValue("anomalous-value-stddev.double"));
                double value = Double.parseDouble(Util.extractString(r
                        .getValue(selectedLabels.get(key))));
                double mean = Util.extractDouble(r
                        .getValue("anomalous-value-mean.double"));
                double stddevDiff = (value - mean) / stddev;

                int samples = Util.extractInt(r
                        .getValue("anomalous-value-count.int"));
                String aboveOrBelow = Math.signum(stddevDiff) >= 0.0 ? "above"
                        : "below";

                String server = r.getServerName();
                String name = r.getObjectName();
                name = name.substring(name.lastIndexOf('/') + 1);

                return "<html><p><b>" + descriptor + "</b> = "
                        + df.format(value) + "<p><b>"
                        + df.format(Math.abs(stddevDiff)) + "</b> stddev <b>"
                        + aboveOrBelow + "</b> mean of <b>" + df.format(mean)
                        + "</b><hr><p>" + name + "<p>" + server + " ["
                        + samples + "]</html>";
            }

            @Override
            public String annotateOneLine(Result r) {
                return null;
            }
        };
    }

    public Decorator getDecorator() {
        // TODO Auto-generated method stub
        return null;
    }

    private static final DoubleComposer composer = new DoubleComposer() {
        public double compose(String key, double a, double b) {
            return a + b;
        }
    };

    public DoubleComposer getDoubleComposer() {
        return composer;
    }

    public String[] getApplicationDependencies() {
        return new String[] { "neurites" };
    }
    
    public Filter[] getFilters() {
        Filter neurites = null;
        Filter anom = null;
        try {
            FilterCode c;

            c = new FilterCode(
                    new FileInputStream(
                            "/usr/share/imagejfind/filter/fil_imagej_exec.so"));

            byte macroBlob[] = Util.readFully(this.getClass()
                    .getResourceAsStream("resources/Neurite_Multiplane_Diamond_Anomaly.txt"));

            neurites = new Filter("neurites", c, "f_eval_imagej_exec",
                    "f_init_imagej_exec", "f_fini_imagej_exec", 0,
                    new String[0], new String[] {}, 400, macroBlob);
            System.out.println(neurites);

            List<String> paramsList = new ArrayList<String>();
            for (int i = 0; i < checkboxes.length; i++) {
                JCheckBox cb = checkboxes[i];
                if (cb.isSelected()) {
                    paramsList.add(LABELS[i]);
                    paramsList.add(stddevs[i].getValue().toString());
                }
            }

            String anomArgs[] = new String[paramsList.size() + 2];
            anomArgs[0] = ignoreSpinner.getValue().toString(); // skip
            anomArgs[1] = UUID.randomUUID().toString(); // random value
            System.arraycopy(paramsList.toArray(), 0, anomArgs, 2, paramsList
                    .size());
            c = new FilterCode(new FileInputStream(
                    "/opt/snapfind/lib/fil_anomaly.so"));
            anom = new Filter("anomaly", c, "f_eval_afilter", "f_init_afilter",
                    "f_fini_afilter", 100, new String[] { "neurites" },
                    anomArgs, 400);
            System.out.println(anom);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Filter[] { neurites, anom };
    }

    final private JCheckBox[] checkboxes = new JCheckBox[LABELS.length];

    final private JSpinner[] stddevs = new JSpinner[LABELS.length];

    final private JSpinner ignoreSpinner = new JSpinner(new SpinnerNumberModel(
            5, 0, 100, 1));

    private static final String[] LABELS = { "total.number.of.neurites",
            "total.length.neurites", "number.of.cells",
            "average.area.of.cell.body.per.cell", "neurites.per.cell",
            "total.area.neurites", "area.neurites.per.cell" };

    private static final String[] NICE_LABELS = { "Total # neurites",
            "Total length neurites", "# of cells", "Avg. cell body area",
            "Neurites per cell", "Total area neurites",
            "Area neurites per cell" };

    public JPanel getInterface() {
        // XXX do this another way
        JPanel result = new JPanel();
        result.setBorder(BorderFactory
                .createTitledBorder("Neurite Multiplane Anomaly Detector"));
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

}
