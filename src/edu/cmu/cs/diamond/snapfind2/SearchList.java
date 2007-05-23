package edu.cmu.cs.diamond.snapfind2;

import java.awt.Dimension;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import edu.cmu.cs.diamond.opendiamond.Filter;

public class SearchList extends JPanel {
    final private List<SnapFindSearch> searches = new ArrayList<SnapFindSearch>();

    final private Box box = Box.createVerticalBox();

    public SearchList() {
        super();

        setMinimumSize(new Dimension(250, 100));
        setMaximumSize(new Dimension(250, Integer.MAX_VALUE));

        add(box);

        setBorder(BorderFactory.createTitledBorder("Searches"));
    }

    public void addSearch(SnapFindSearch f) {
        searches.add(f);
        JPanel j = f.getInterface();
        Insets in = getInsets();
        j.setMaximumSize(new Dimension(250 - in.left - in.right,
                Integer.MAX_VALUE));
        box.add(j);

        validate();
    }
    
    public Filter[] getFilters() {
        List<Filter> f = new ArrayList<Filter>();
        for (SnapFindSearch s : searches) {
            f.addAll(Arrays.asList(s.getFilters()));
        }
        return f.toArray(new Filter[0]);
    }
}
