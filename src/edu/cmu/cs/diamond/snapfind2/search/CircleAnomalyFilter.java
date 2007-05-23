package edu.cmu.cs.diamond.snapfind2.search;

import java.awt.Component;
import java.awt.Container;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import edu.cmu.cs.diamond.opendiamond.Filter;
import edu.cmu.cs.diamond.opendiamond.FilterCode;
import edu.cmu.cs.diamond.snapfind2.SnapFindSearch;

public class CircleAnomalyFilter implements SnapFindSearch {
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
            anomArgs[0] = "10";
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
        result.setBorder(BorderFactory.createTitledBorder("Anomaly Detector"));
        result.setLayout(new SpringLayout());

        result.add(new JLabel("Attribute"));
        result.add(new JLabel("Std. dev."));
        for (int i = 0; i < LABELS.length; i++) {
            result.add(checkboxes[i]);
            result.add(stddevs[i]);
        }

        makeCompactGrid(result, LABELS.length + 1, 2, 5, 5, 2, 2);

        return result;
    }

    // http://java.sun.com/docs/books/tutorial/uiswing/examples/layout/SpringGridProject/src/layout/SpringUtilities.java
    /* Used by makeCompactGrid. */
    private static SpringLayout.Constraints getConstraintsForCell(int row,
            int col, Container parent, int cols) {
        SpringLayout layout = (SpringLayout) parent.getLayout();
        Component c = parent.getComponent(row * cols + col);
        return layout.getConstraints(c);
    }

    /**
     * Aligns the first <code>rows</code> * <code>cols</code> components of
     * <code>parent</code> in a grid. Each component in a column is as wide as
     * the maximum preferred width of the components in that column; height is
     * similarly determined for each row. The parent is made just big enough to
     * fit them all.
     * 
     * @param rows
     *            number of rows
     * @param cols
     *            number of columns
     * @param initialX
     *            x location to start the grid at
     * @param initialY
     *            y location to start the grid at
     * @param xPad
     *            x padding between cells
     * @param yPad
     *            y padding between cells
     */
    public static void makeCompactGrid(Container parent, int rows, int cols,
            int initialX, int initialY, int xPad, int yPad) {
        SpringLayout layout;
        try {
            layout = (SpringLayout) parent.getLayout();
        } catch (ClassCastException exc) {
            System.err
                    .println("The first argument to makeCompactGrid must use SpringLayout.");
            return;
        }

        // Align all cells in each column and make them the same width.
        Spring x = Spring.constant(initialX);
        for (int c = 0; c < cols; c++) {
            Spring width = Spring.constant(0);
            for (int r = 0; r < rows; r++) {
                width = Spring.max(width, getConstraintsForCell(r, c, parent,
                        cols).getWidth());
            }
            for (int r = 0; r < rows; r++) {
                SpringLayout.Constraints constraints = getConstraintsForCell(r,
                        c, parent, cols);
                constraints.setX(x);
                constraints.setWidth(width);
            }
            x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
        }

        // Align all cells in each row and make them the same height.
        Spring y = Spring.constant(initialY);
        for (int r = 0; r < rows; r++) {
            Spring height = Spring.constant(0);
            for (int c = 0; c < cols; c++) {
                height = Spring.max(height, getConstraintsForCell(r, c, parent,
                        cols).getHeight());
            }
            for (int c = 0; c < cols; c++) {
                SpringLayout.Constraints constraints = getConstraintsForCell(r,
                        c, parent, cols);
                constraints.setY(y);
                constraints.setHeight(height);
            }
            y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
        }

        // Set the parent's size.
        SpringLayout.Constraints pCons = layout.getConstraints(parent);
        pCons.setConstraint(SpringLayout.SOUTH, y);
        pCons.setConstraint(SpringLayout.EAST, x);
    }

}
