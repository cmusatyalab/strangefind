package edu.cmu.cs.diamond.snapfind2;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import edu.cmu.cs.diamond.opendiamond.Filter;

public class SearchList extends JPanel {
    final private List<Filter> filters = new ArrayList<Filter>();
    final private Box box = Box.createVerticalBox();
    
    public SearchList() {
        super();
        
        setMinimumSize(new Dimension(250, 100));
        setMaximumSize(new Dimension(250, Integer.MAX_VALUE));

        add(box);
        
        setBorder(BorderFactory.createTitledBorder("Searches"));
    }
    
    public void addFilter(Filter f, String name) {
        filters.add(f);
        Box b2 = Box.createHorizontalBox();
        b2.add(new JCheckBox(name, true));
        b2.add(Box.createHorizontalStrut(10));
        b2.add(new JLabel("zz"));
        b2.add(Box.createHorizontalStrut(10));
        b2.add(new JButton("Edit"));
        
        box.add(b2);
        
        validate();
    }
}
