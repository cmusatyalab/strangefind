package edu.cmu.cs.diamond.snapfind2;

import javax.swing.Box;
import javax.swing.JPanel;

import edu.cmu.cs.diamond.opendiamond.Result;

public class ThumbnailBox extends JPanel {
    private int nextEmpty = 0;
    
    final private ResultViewer[] pics = new ResultViewer[6];
    
    public ThumbnailBox() {
        Box v = Box.createVerticalBox();
        add(v);
//        v.setBorder(BorderFactory.createEtchedBorder(Color.RED, Color.BLUE));
        Box h = null;
        for (int i = 0; i < pics.length; i++) {
            boolean addBox = false;
            if (i % 3 == 0) {
                h = Box.createHorizontalBox();
                addBox = true;
            }

            ResultViewer b = new ResultViewer();
            
            h.add(b);
            pics[i] = b;
            
            if (addBox) {
                v.add(h);
            }
        }
    }
    
    public boolean isFull() {
        return nextEmpty >= pics.length;
    }
    
    public void clearAll() {
        nextEmpty = 0;
        for (ResultViewer r : pics) {
            r.setResult(null);
        }
    }
    
    public void fillNext(Result r) {
        System.out.println("fillNext " + r);
        pics[nextEmpty++].setResult(r);
    }
}
