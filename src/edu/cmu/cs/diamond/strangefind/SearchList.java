/*
 *  StrangeFind, an anomaly detector for the OpenDiamond platform
 *
 *  Copyright (c) 2007-2008 Carnegie Mellon University
 *  All rights reserved.
 *
 *  StrangeFind is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2.
 *
 *  StrangeFind is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with StrangeFind. If not, see <http://www.gnu.org/licenses/>.
 *
 *  Linking StrangeFind statically or dynamically with other modules is
 *  making a combined work based on StrangeFind. Thus, the terms and
 *  conditions of the GNU General Public License cover the whole
 *  combination.
 * 
 *  In addition, as a special exception, the copyright holders of
 *  StrangeFind give you permission to combine StrangeFind with free software
 *  programs or libraries that are released under the GNU LGPL or the
 *  Eclipse Public License 1.0. You may copy and distribute such a system
 *  following the terms of the GNU GPL for StrangeFind and the licenses of
 *  the other code concerned, provided that you include the source code of
 *  that other code when and as the GNU GPL requires distribution of source
 *  code.
 *
 *  Note that people who make modified versions of StrangeFind are not
 *  obligated to grant this special exception for their modified versions;
 *  it is their choice whether to do so. The GNU General Public License
 *  gives permission to release a modified version without this exception;
 *  this exception also makes it possible to release a modified version
 *  which carries forward this exception.
 */

package edu.cmu.cs.diamond.strangefind;

import java.awt.Dimension;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import edu.cmu.cs.diamond.opendiamond.DoubleComposer;
import edu.cmu.cs.diamond.opendiamond.Filter;

public class SearchList extends JPanel {
    final private List<StrangeFindSearch> searches = new ArrayList<StrangeFindSearch>();

    final private Box box = Box.createVerticalBox();

    public SearchList() {
        super();

        setMinimumSize(new Dimension(250, 100));
        setMaximumSize(new Dimension(550, Integer.MAX_VALUE));

        add(box);

        setBorder(BorderFactory.createTitledBorder("Searches"));
    }

    public void addSearch(StrangeFindSearch f) {
        searches.add(f);
        JPanel j = f.getInterface();
        Insets in = getInsets();
        j.setMaximumSize(new Dimension(550 - in.left - in.right,
                Integer.MAX_VALUE));
        box.add(j);

        validate();
    }

    public List<Filter> getFilters() {
        List<Filter> f = new ArrayList<Filter>();
        for (StrangeFindSearch s : searches) {
            f.addAll(Arrays.asList(s.getFilters()));
        }
        return f;
    }

    public Annotator[] getAnnotators() {
        List<Annotator> l = new ArrayList<Annotator>();
        for (StrangeFindSearch s : searches) {
            l.addAll(Arrays.asList(s.getAnnotator()));
        }
        return l.toArray(new Annotator[0]);
    }

    public Decorator[] getDecorators() {
        List<Decorator> l = new ArrayList<Decorator>();
        for (StrangeFindSearch s : searches) {
            l.addAll(Arrays.asList(s.getDecorator()));
        }
        return l.toArray(new Decorator[0]);
    }

    public DoubleComposer[] getDoubleComposers() {
        List<DoubleComposer> l = new ArrayList<DoubleComposer>();
        for (StrangeFindSearch s : searches) {
            l.addAll(Arrays.asList(s.getDoubleComposer()));
        }
        return l.toArray(new DoubleComposer[0]);
    }

    public List<String> getApplicationDependencies() {
        List<String> f = new ArrayList<String>();
        for (StrangeFindSearch s : searches) {
            f.addAll(Arrays.asList(s.getApplicationDependencies()));
        }
        return f;
    }

    public Set<String> getPushAttributes() {
        boolean anyPushAttributes = false;

        Set<String> set = new HashSet<String>();
        for (StrangeFindSearch s : searches) {
            Set<String> z = s.getPushAttributes();
            if (z != null) {
                anyPushAttributes = true;
                set.addAll(s.getPushAttributes());
            }
        }

        if (set.contains(null) || !anyPushAttributes) {
            return null;
        } else {
            return set;
        }
    }
}
