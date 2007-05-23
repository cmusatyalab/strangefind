package edu.cmu.cs.diamond.snapfind2;

import javax.swing.JPanel;

import edu.cmu.cs.diamond.opendiamond.Filter;

public interface SnapFindSearch {
    // XXX rethink
    JPanel getInterface();
    
    Filter[] getFilters();
    
    Annotator getAnnotator();
    
    Decorator getDecorator();
}
