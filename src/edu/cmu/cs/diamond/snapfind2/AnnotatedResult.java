package edu.cmu.cs.diamond.snapfind2;

import java.awt.Graphics2D;
import java.util.List;

import edu.cmu.cs.diamond.opendiamond.Result;

public class AnnotatedResult extends Result {
    final private Result theResult;
    final private String annotation;
    final private Decorator decorator;
    final private String tooltipAnnotation;

    public AnnotatedResult(Result r, String annotation, String tooltipAnnotation, Decorator decorator) {
        theResult = r;
        this.annotation = annotation;
        this.tooltipAnnotation = tooltipAnnotation;
        this.decorator = decorator;
    }

    @Override
    public byte[] getData() {
        return theResult.getData();
    }

    @Override
    public List<String> getKeys() {
        return theResult.getKeys();
    }

    @Override
    public byte[] getValue(String key) {
        return theResult.getValue(key);
    }

    public String getAnnotation() {
        return annotation;
    }
    
    public void decorate(Graphics2D g, double scale) {
        decorator.decorate(this, g, scale);
    }

    public String getTooltipAnnotation() {
        return tooltipAnnotation;
    }

    @Override
    public String getServerName() {
        return theResult.getServerName();
    }
}
