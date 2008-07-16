package edu.cmu.cs.diamond.snapfind2.search;

import java.awt.Component;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.cmu.cs.diamond.opendiamond.*;
import edu.cmu.cs.diamond.snapfind2.Annotator;
import edu.cmu.cs.diamond.snapfind2.Decorator;
import edu.cmu.cs.diamond.snapfind2.SnapFindSearch;

public class XQueryAnomalyFilter implements SnapFindSearch {

    public XQueryAnomalyFilter(Component parent) {
        attrMap = new HashMap<String, String>();

        // load file
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Attribute Map Files", "attrmap");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            parseAttrFile(f, attrMap);
        } else {
            // XXX
            throw new RuntimeException("You must choose a file");
        }

        // init GUI elements
        checkboxes = new JCheckBox[attrMap.size()];
        stddevs = new JSpinner[attrMap.size()];
        niceLabels = new String[attrMap.size()];
        labels = new String[attrMap.size()];
        queries = new String[attrMap.size()];

        {
            int i = 0;
            for (Entry<String, String> e : attrMap.entrySet()) {
                // convert spaces to underscores, in an attempt to not crash the
                // lexer?
                labels[i] = e.getKey().replaceAll("\\t", " ").replaceAll("_",
                        "__").replaceAll(" ", "_");

                niceLabels[i] = e.getKey();
                queries[i] = e.getValue();
                i++;
            }
        }

        for (int i = 0; i < niceLabels.length; i++) {
            checkboxes[i] = new JCheckBox(niceLabels[i]);
            checkboxes[i].setToolTipText(queries[i]);
            checkboxes[i].setSelected(true);

            JSpinner s = new JSpinner(
                    new SpinnerNumberModel(3.0, 1.0, 7.0, 0.5));
            stddevs[i] = s;
        }
    }

    static private void parseAttrFile(File f, Map<String, String> attrMap) {
        FileReader fr = null;
        try {
            fr = new FileReader(f);
            BufferedReader in = new BufferedReader(fr);

            String line;
            while ((line = in.readLine()) != null) {
                String tokens[] = line.split(":", 2);

                String key = tokens[0].replaceAll("\\t", " ");
                attrMap.put(key, tokens[1]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Annotator getAnnotator() {
        final List<String> selectedLabels = new ArrayList<String>();
        final List<String> niceSelectedLabels = new ArrayList<String>();
        for (int i = 0; i < checkboxes.length; i++) {
            JCheckBox c = checkboxes[i];
            if (c.isSelected()) {
                selectedLabels.add(labels[i]);
                niceSelectedLabels.add(niceLabels[i]);
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
                System.out.println("key: " + key + ", value: " + keyValue
                        + ", strValue: " + strValue);
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
        return new String[] { "xquery" };
    }

    public Filter[] getFilters() {
        Filter xquery = null;
        Filter anom = null;
        try {
            FilterCode c;

            c = new FilterCode(new FileInputStream(
                    "/opt/snapfind/lib/fil_xquery.so"));

            byte queryBlob[] = generateQueryBlob();

            System.out.println("queryBlob: " + new String(queryBlob));

            xquery = new Filter("xquery", c, "f_eval_xquery", "f_init_xquery",
                    "f_fini_xquery", 0, new String[0], new String[] {}, 400,
                    queryBlob);
            System.out.println(xquery);

            List<String> paramsList = new ArrayList<String>();
            for (int i = 0; i < checkboxes.length; i++) {
                JCheckBox cb = checkboxes[i];
                if (cb.isSelected()) {
                    paramsList.add(labels[i]);
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

    private byte[] generateQueryBlob() {
        StringBuilder sb = new StringBuilder();

        sb.append("document { <attributes>");

        for (int i = 0; i < labels.length; i++) {
            // TODO(agoode) validate or escape xpath/xquery input
            sb.append("<attribute name='" + labels[i] + "' value='"
                    + queries[i] + "'/>");
        }

        sb
                .append("<attribute name='image-1' value='{//Images/ImageFile[1]/text()}'/>"
                        + "<attribute name='image-2' value='{//Images/ImageFile[2]/text()}'/>"
                        + "<attribute name='image-3' value='{//Images/ImageFile[3]/text()}'/>"
                        + "</attributes>}");

        return sb.toString().getBytes();
    }

    final private JCheckBox[] checkboxes;

    final private JSpinner[] stddevs;

    final private JSpinner ignoreSpinner = new JSpinner(new SpinnerNumberModel(
            5, 0, 100, 1));

    final private Map<String, String> attrMap;

    final private String labels[];

    final private String niceLabels[];

    final private String queries[];

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
        for (int i = 0; i < labels.length; i++) {
            result.add(checkboxes[i]);
            result.add(stddevs[i]);
        }

        Util.makeCompactGrid(result, labels.length + 3, 2, 5, 5, 2, 2);

        return result;
    }

}
