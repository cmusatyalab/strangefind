package edu.cmu.cs.diamond.snapfind2.search;

import java.awt.Graphics2D;
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

public class XQueryAnomalyFilter implements SnapFindSearch {

    public XQueryAnomalyFilter() {
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
                String keyValue = selectedLabels.get(key);
                String strValue = Util.extractString(r.getValue(keyValue));
                System.out.println("key: " + key + ", value: " + keyValue + ", strValue: " + strValue);
                double value = Double.parseDouble(strValue);
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
        };
    }

    public Decorator getDecorator() {
        return new Decorator() {
            public void decorate(Result r, Graphics2D g, double scale) {
                byte data[] = r.getValue("circle-data");
                if (data == null) {
                    return;
                }
            }
        };
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
        return new String[] { "xquery" };
    }

    public Filter[] getFilters() {
        Filter xquery = null;
        Filter anom = null;
        try {
            FilterCode c;

            c = new FilterCode(
                    new FileInputStream(
                            "/coda/coda.cs.cmu.edu/usr/agoode/diamond-git/anomaly-test/fil_xquery.so"));

            byte queryBlob[] = Util
                    .readFully(new FileInputStream(
                            "/coda/coda.cs.cmu.edu/usr/agoode/diamond-git/anomaly-test/item.xql"));

            xquery = new Filter("xquery", c, "f_eval_xquery", "f_init_xquery",
                    "f_fini_xquery", 0, new String[0], new String[] {}, 400,
                    queryBlob);
            System.out.println(xquery);

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
                    "f_fini_afilter", 100, new String[] { "xquery" }, anomArgs,
                    400);
            System.out.println(anom);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Filter[] { xquery, anom };
    }

    final private JCheckBox[] checkboxes = new JCheckBox[LABELS.length];

    final private JSpinner[] stddevs = new JSpinner[LABELS.length];

    final private JSpinner ignoreSpinner = new JSpinner(new SpinnerNumberModel(
            5, 0, 100, 1));

    private static final String[] LABELS = { "cell-count", "cell-inner-area-mean", "cell-outer-area-mean" };

    private static final String[] NICE_LABELS = { "Cell Count", "Cell Inner Area Mean", "Cell Outer Area Mean" };

    public JPanel getInterface() {
        // XXX do this another way
        JPanel result = new JPanel();
        result.setBorder(BorderFactory
                .createTitledBorder("XQuery Anomaly Detector"));
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
