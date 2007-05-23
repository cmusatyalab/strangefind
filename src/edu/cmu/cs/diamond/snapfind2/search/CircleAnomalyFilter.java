package edu.cmu.cs.diamond.snapfind2.search;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import edu.cmu.cs.diamond.opendiamond.Filter;
import edu.cmu.cs.diamond.opendiamond.FilterCode;
import edu.cmu.cs.diamond.opendiamond.Result;
import edu.cmu.cs.diamond.opendiamond.Util;
import edu.cmu.cs.diamond.snapfind2.Annotator;
import edu.cmu.cs.diamond.snapfind2.Decorator;
import edu.cmu.cs.diamond.snapfind2.SnapFindSearch;

public class CircleAnomalyFilter implements SnapFindSearch {
    public static class Circle {
        public float x;

        public float y;

        public float a;

        public float b;

        public float t;

        public boolean inResult;

        @Override
        public String toString() {
            return "(" + x + "," + y + "), [" + a + " x " + b + "] t: " + t
                    + ", inResult: " + inResult;
        }
    }

    private static final String[] LABELS = { "circle-count",
            "circle-area-fraction", "circle-area-m0", "circle-area-m1",
            "circle-area-m2", "circle-area-m3", "circle-eccentricity-m0",
            "circle-eccentricity-m1", "circle-eccentricity-m2",
            "circle-eccentricity-m3" };

    private static final String[] NICE_LABELS = { "Count", "Area fraction",
            "Area moment 0", "Area moment 1", "Area moment 2", "Area moment 3",
            "Eccentricity moment 0", "Eccentricity moment 1",
            "Eccentricity moment 2", "Eccentricity moment 3" };

    public CircleAnomalyFilter() {
        // init GUI elements
        for (int i = 0; i < NICE_LABELS.length; i++) {
            checkboxes[i] = new JCheckBox(NICE_LABELS[i]);

            JSpinner s = new JSpinner(
                    new SpinnerNumberModel(3.0, 1.0, 7.0, 0.5));
            stddevs[i] = s;
        }

        checkboxes[0].setSelected(true);
        checkboxes[1].setSelected(true);
    }

    public Filter[] getFilters() {
        Filter circles = null;
        Filter anom = null;
        try {
            FilterCode c;

            c = new FilterCode(new FileInputStream("fil_circle.so"));
            circles = new Filter("circles", c, "f_eval_circles",
                    "f_init_circles", "f_fini_circles", 0,
                    new String[] { "rgb" }, new String[] { "-1", "-1", "0.4",
                            "1" }, 400);
            System.out.println(circles);

            List<String> paramsList = new ArrayList<String>();
            for (int i = 0; i < checkboxes.length; i++) {
                JCheckBox cb = checkboxes[i];
                if (cb.isSelected()) {
                    paramsList.add(LABELS[i]);
                    paramsList.add(stddevs[i].getValue().toString());
                }
            }

            String anomArgs[] = new String[paramsList.size() + 1];
            anomArgs[0] = "10"; // number to skip
            System.arraycopy(paramsList.toArray(), 0, anomArgs, 1, paramsList
                    .size());
            c = new FilterCode(new FileInputStream("fil_anomaly.so"));
            anom = new Filter("anomaly", c, "f_eval_afilter", "f_init_afilter",
                    "f_fini_afilter", 100, new String[] { "circles" },
                    anomArgs, 400);
            System.out.println(anom);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Filter[] { circles, anom };
    }

    final private JCheckBox[] checkboxes = new JCheckBox[LABELS.length];

    final private JSpinner[] stddevs = new JSpinner[LABELS.length];

    public JPanel getInterface() {
        // XXX do this another way
        JPanel result = new JPanel();
        result.setBorder(BorderFactory
                .createTitledBorder("Circle Anomaly Detector"));
        result.setLayout(new SpringLayout());

        result.add(new JLabel("Descriptor"));
        result.add(new JLabel("Std. dev."));
        for (int i = 0; i < LABELS.length; i++) {
            result.add(checkboxes[i]);
            result.add(stddevs[i]);
        }

        Util.makeCompactGrid(result, LABELS.length + 1, 2, 5, 5, 2, 2);

        return result;
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
                String anomStr = "<html><p>Anomalous <b>"
                        + niceSelectedLabels.get(key)
                        + "</b>: "
                        + Util.extractDouble(r
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
                        + "</html>";

                return anomStr;
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

                List<Circle> circles = extractCircles(data);
                for (Circle circle : circles) {
                    drawCircle(g, circle, scale);
                }
            }
        };
    }

    protected void drawCircle(Graphics2D g, Circle circle, double scale) {
           float x = circle.x;
           float y = circle.y;
           float a = circle.a;
           float b = circle.b;
           float t = circle.t;

           double dash;

           if (!circle.inResult) {
               return;
           }
           
           // draw
           Arc2D arc = new Arc2D.Double(-1, -1, 2, 2, 0, 360, Arc2D.CHORD);

           AffineTransform at = new AffineTransform();
           at.scale(scale, scale);

           at.translate(x, y);
           at.rotate(t);
           at.scale(a, b);

           g.setColor(Color.RED);
           g.draw(at.createTransformedShape(arc));
           /*
           switch (fill) {
           case CIRCLE_FILL_DASHED:
             dash = 5.0;
             cairo_set_line_width(cr, 1.0);
             cairo_set_dash(cr, &dash, 1, 0.0);
             cairo_set_source_rgb(cr, 1.0, 0.0, 0.0);
             cairo_stroke(cr);
             break;

           case CIRCLE_FILL_SOLID:
             // show fill, no dash
             cairo_set_line_width(cr, 2.0);
             cairo_set_dash(cr, NULL, 0, 0.0);
             cairo_set_source_rgb(cr, 1.0, 0.0, 0.0);
             cairo_stroke_preserve(cr);
             cairo_set_source_rgba(cr, 1.0, 0.0, 0.0, 0.2);
             cairo_fill(cr);
             break;

           case CIRCLE_FILL_HAIRLINE:
             cairo_set_line_width(cr, 1.0);
             cairo_set_dash(cr, NULL, 0, 0.0);
             cairo_set_source_rgb(cr, 1.0, 0.0, 0.0);
             cairo_stroke(cr);
             break;
           }
           */
    }

    static protected List<Circle> extractCircles(byte[] data) {
        List<Circle> circles = new ArrayList<Circle>();
        final int sizeOfFloatPart = 4 * 5;

        System.out.println(data.length);
        for (int i = 0; i < data.length; i += sizeOfFloatPart + 1) {
                Circle c = new Circle();
                System.out.println(" " + i);
                
                ByteBuffer b = ByteBuffer.wrap(data, i, sizeOfFloatPart + 1);
                b.order(ByteOrder.LITTLE_ENDIAN);
                
                c.x = b.getFloat();
                c.y = b.getFloat();
                c.a = b.getFloat();
                c.b = b.getFloat();
                c.t = b.getFloat();
                c.inResult = b.get() != 0;

                circles.add(c);
            }

        return circles;
    }
}
