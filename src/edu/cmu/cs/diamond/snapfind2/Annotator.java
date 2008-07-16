package edu.cmu.cs.diamond.snapfind2;

import edu.cmu.cs.diamond.opendiamond.Result;

public interface Annotator {
    String annotate(Result r);
    
    String annotateTooltip(Result r);

    String annotateOneLine(Result r);
}
