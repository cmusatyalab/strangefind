package edu.cmu.cs.diamond.snapfind2;

import java.util.List;

import edu.cmu.cs.diamond.opendiamond.Result;

public class AnnotatedResult extends Result {
    final private Result theResult;
    final private String annotation;

    public AnnotatedResult(Result r, String annotation) {
        theResult = r;
        this.annotation = annotation;
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
}
