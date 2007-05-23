package edu.cmu.cs.diamond.snapfind2;

import java.awt.Graphics2D;

import edu.cmu.cs.diamond.opendiamond.Result;

public interface Decorator {
    void decorate(Result r, Graphics2D g, double scale);
}
