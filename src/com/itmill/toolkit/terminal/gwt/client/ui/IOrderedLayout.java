/* 
@ITMillApache2LicenseForJavaFiles@
 */

package com.itmill.toolkit.terminal.gwt.client.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.itmill.toolkit.terminal.gwt.client.ApplicationConnection;
import com.itmill.toolkit.terminal.gwt.client.Caption;
import com.itmill.toolkit.terminal.gwt.client.Container;
import com.itmill.toolkit.terminal.gwt.client.Paintable;
import com.itmill.toolkit.terminal.gwt.client.StyleConstants;
import com.itmill.toolkit.terminal.gwt.client.UIDL;
import com.itmill.toolkit.terminal.gwt.client.Util;

/**
 * Abstract base class for ordered layouts. Use either vertical or horizontal
 * subclass.
 * 
 * @author IT Mill Ltd
 */
public abstract class IOrderedLayout extends ComplexPanel implements Container {

    public static final String CLASSNAME = "i-orderedlayout";

    public static final int ORIENTATION_VERTICAL = 0;
    public static final int ORIENTATION_HORIZONTAL = 1;

    int orientationMode = ORIENTATION_VERTICAL;

    protected HashMap componentToCaption = new HashMap();

    protected ApplicationConnection client;

    /**
     * Contains reference to Element where Paintables are wrapped. Normally a TR
     * or a TBODY element.
     */
    protected Element childContainer;

    /*
     * Elements that provides the Layout interface implementation.
     */
    protected Element root;
    protected Element margin;

    private boolean hasComponentSpacing;

    public IOrderedLayout(int orientation) {
        orientationMode = orientation;
        constructDOM();
        setStyleName(CLASSNAME);
    }

    protected void constructDOM() {
        root = DOM.createDiv();
        margin = DOM.createDiv();
        DOM.appendChild(root, margin);
        if (orientationMode == ORIENTATION_HORIZONTAL) {
            final String structure = "<table cellspacing=\"0\" cellpadding=\"0\"><tbody><tr></tr></tbody></table>";
            DOM.setInnerHTML(margin, structure);
            childContainer = DOM.getFirstChild(DOM.getFirstChild(DOM
                    .getFirstChild(margin)));
        } else {
            childContainer = margin;
        }
        setElement(root);
    }

    public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {

        this.client = client;

        // Ensure correct implementation
        if (client.updateComponent(this, uidl, false)) {
            return;
        }

        //
        hasComponentSpacing = uidl.getBooleanAttribute("spacing");

        // Update contained components

        final ArrayList uidlWidgets = new ArrayList();
        for (final Iterator it = uidl.getChildIterator(); it.hasNext();) {
            final UIDL uidlForChild = (UIDL) it.next();
            final Paintable child = client.getPaintable(uidlForChild);
            uidlWidgets.add(child);
        }

        final ArrayList oldWidgets = getPaintables();

        final Iterator oldIt = oldWidgets.iterator();
        final Iterator newIt = uidlWidgets.iterator();
        final Iterator newUidl = uidl.getChildIterator();

        Widget oldChild = null;
        while (newIt.hasNext()) {
            final Widget child = (Widget) newIt.next();
            final UIDL childUidl = (UIDL) newUidl.next();

            if (oldChild == null && oldIt.hasNext()) {
                // search for next old Paintable which still exists in layout
                // and delete others
                while (oldIt.hasNext()) {
                    oldChild = (Widget) oldIt.next();
                    // now oldChild is an instance of Paintable
                    if (uidlWidgets.contains(oldChild)) {
                        break;
                    } else {
                        removePaintable((Paintable) oldChild);
                        oldChild = null;
                    }
                }
            }
            if (oldChild == null) {
                // we are adding components to layout
                add(child);
            } else if (child == oldChild) {
                // child already attached and updated
                oldChild = null;
            } else if (hasChildComponent(child)) {
                // current child has been moved, re-insert before current
                // oldChild
                // TODO this might be optimized by moving only container element
                // to correct position
                removeCaption(child);
                int index = getWidgetIndex(oldChild);
                if (componentToCaption.containsKey(oldChild)) {
                    index--;
                }
                remove(child);
                this.insert(child, index);
            } else {
                // insert new child before old one
                final int index = getWidgetIndex(oldChild);
                insert(child, index);
            }
            ((Paintable) child).updateFromUIDL(childUidl, client);
        }
        // remove possibly remaining old Paintable object which were not updated
        while (oldIt.hasNext()) {
            oldChild = (Widget) oldIt.next();
            final Paintable p = (Paintable) oldChild;
            if (!uidlWidgets.contains(p)) {
                removePaintable(p);
            }
        }

        // Handle component alignments
        handleAlignments(uidl);

        // Handle layout margins
        handleMargins(uidl);

    }

    /**
     * Retuns a list of Paintables currently rendered in layout
     * 
     * @return list of Paintable objects
     */
    protected ArrayList getPaintables() {
        final ArrayList al = new ArrayList();
        final Iterator it = iterator();
        while (it.hasNext()) {
            final Widget w = (Widget) it.next();
            if (w instanceof Paintable) {
                al.add(w);
            }
        }
        return al;
    }

    /**
     * Removes Paintable from DOM and its reference from ApplicationConnection.
     * 
     * Also removes Paintable's Caption if one exists
     * 
     * @param p
     *                Paintable to be removed
     */
    public boolean removePaintable(Paintable p) {
        final Caption c = (Caption) componentToCaption.get(p);
        if (c != null) {
            componentToCaption.remove(c);
            remove(c);
        }
        client.unregisterPaintable(p);
        return remove((Widget) p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.itmill.toolkit.terminal.gwt.client.Layout#replaceChildComponent(com.google.gwt.user.client.ui.Widget,
     *      com.google.gwt.user.client.ui.Widget)
     */
    public void replaceChildComponent(Widget from, Widget to) {
        client.unregisterPaintable((Paintable) from);
        final Caption c = (Caption) componentToCaption.get(from);
        if (c != null) {
            remove(c);
            componentToCaption.remove(c);
        }
        final int index = getWidgetIndex(from);
        if (index >= 0) {
            remove(index);
            insert(to, index);
        }
    }

    protected void insert(Widget w, int beforeIndex) {
        if (w instanceof Caption) {
            final Caption c = (Caption) w;
            // captions go into same container element as their
            // owners
            final Element container = getWidgetWrapperFor((Widget) c.getOwner())
                    .getContainerElement();
            final Element captionContainer = DOM.createDiv();
            DOM.insertChild(container, captionContainer, 0);
            insert(w, captionContainer, beforeIndex, false);
        } else {
            WidgetWrapper wr = new WidgetWrapper();
            DOM.insertChild(childContainer, wr.getElement(), beforeIndex);
            insert(w, wr.getContainerElement(), beforeIndex, false);
        }
    }

    public boolean hasChildComponent(Widget component) {
        return getWidgetIndex(component) >= 0;
    }

    public void updateCaption(Paintable component, UIDL uidl) {

        Caption c = (Caption) componentToCaption.get(component);

        if (Caption.isNeeded(uidl)) {
            if (c == null) {
                final int index = getWidgetIndex((Widget) component);
                c = new Caption(component, client);
                insert(c, index);
                componentToCaption.put(component, c);
            }
            c.updateCaption(uidl);
        } else {
            if (c != null) {
                remove(c);
                componentToCaption.remove(component);
            }
        }
    }

    public void removeCaption(Widget w) {
        final Caption c = (Caption) componentToCaption.get(w);
        if (c != null) {
            this.remove(c);
            componentToCaption.remove(w);
        }
    }

    public void add(Widget w) {
        WidgetWrapper wr = new WidgetWrapper();
        DOM.appendChild(childContainer, wr.getElement());
        super.add(w, wr.getContainerElement());
    }

    public boolean remove(int index) {
        return remove(getWidget(index));
    }

    public boolean remove(Widget w) {
        final Element wrapper = getWidgetWrapperFor(w).getElement();
        final boolean removed = super.remove(w);
        if (removed) {
            if (!(w instanceof Caption)) {
                DOM.removeChild(childContainer, wrapper);
            }
            return true;
        }
        return false;
    }

    public Widget getWidget(int index) {
        return getChildren().get(index);
    }

    public int getWidgetCount() {
        return getChildren().size();
    }

    public int getWidgetIndex(Widget child) {
        return getChildren().indexOf(child);
    }

    protected void handleMargins(UIDL uidl) {
        final MarginInfo margins = new MarginInfo(uidl
                .getIntAttribute("margins"));
        setStyleName(margin, CLASSNAME + "-" + StyleConstants.MARGIN_TOP,
                margins.hasTop());
        setStyleName(margin, CLASSNAME + "-" + StyleConstants.MARGIN_RIGHT,
                margins.hasRight());
        setStyleName(margin, CLASSNAME + "-" + StyleConstants.MARGIN_BOTTOM,
                margins.hasBottom());
        setStyleName(margin, CLASSNAME + "-" + StyleConstants.MARGIN_LEFT,
                margins.hasLeft());
    }

    protected void handleAlignments(UIDL uidl) {
        // Component alignments as a comma separated list.
        // See com.itmill.toolkit.terminal.gwt.client.ui.AlignmentInfo.java for
        // possible values.
        final int[] alignments = uidl.getIntArrayAttribute("alignments");
        int alignmentIndex = 0;
        // Insert alignment attributes
        final Iterator it = getPaintables().iterator();
        boolean first = true;
        while (it.hasNext()) {

            // Calculate alignment info
            final AlignmentInfo ai = new AlignmentInfo(
                    alignments[alignmentIndex++]);

            final WidgetWrapper wr = getWidgetWrapperFor((Widget) it.next());
            wr.setAlignment(ai.getVerticalAlignment(), ai
                    .getHorizontalAlignment());

            // Handle spacing in this loop as well
            if (first) {
                wr.setSpacingEnabled(false);
                first = false;
            } else {
                wr.setSpacingEnabled(hasComponentSpacing);
            }
        }
    }

    /**
     * WidgetWrapper classe. Helper classe for spacing and alignment handling.
     * 
     */
    class WidgetWrapper extends UIObject {

        Element td;

        public WidgetWrapper() {
            if (orientationMode == ORIENTATION_VERTICAL) {
                setElement(DOM.createDiv());
                // Apply 'hasLayout' for IE (needed to get accurate dimension
                // calculations)
                if (Util.isIE()) {
                    DOM.setStyleAttribute(getElement(), "zoom", "1");
                }
            } else {
                setElement(DOM.createTD());
            }
        }

        public WidgetWrapper(Element element) {
            if (DOM.getElementProperty(element, "className").equals("i_align")) {
                td = element;
                setElement(DOM.getParent(DOM.getParent(DOM.getParent(DOM
                        .getParent(td)))));
            } else {
                setElement(element);
            }
        }

        Element getContainerElement() {
            if (td != null) {
                return td;
            } else {
                return getElement();
            }
        }

        void setAlignment(String verticalAlignment, String horizontalAlignment) {

            // Set vertical alignment
            if (Util.isIE()) {
                DOM.setElementAttribute(getElement(), "vAlign",
                        verticalAlignment);
            } else {
                DOM.setStyleAttribute(getElement(), "verticalAlign",
                        verticalAlignment);
            }

            // Set horizontal alignment
            if (Util.isIE()) {
                DOM.setElementAttribute(getElement(), "align",
                        horizontalAlignment);
            } else if (!horizontalAlignment.equals("left")) {
                // use one-cell table to implement horizontal alignments, only
                // for values other than "left" (which is default)
                // build one cell table
                if (td == null) {
                    final Element table = DOM.createTable();
                    final Element tBody = DOM.createTBody();
                    final Element tr = DOM.createTR();
                    td = DOM.createTD();
                    DOM.appendChild(table, tBody);
                    DOM.appendChild(tBody, tr);
                    DOM.appendChild(tr, td);
                    DOM.setElementAttribute(table, "cellpadding", "0");
                    DOM.setElementAttribute(table, "cellspacing", "0");
                    DOM.setStyleAttribute(table, "width", "100%");
                    // use className for identification
                    DOM.setElementProperty(td, "className", "i_align");
                    // move possible content to cell
                    while (DOM.getChildCount(getElement()) > 0) {
                        final Element content = DOM.getFirstChild(getElement());
                        if (content != null) {
                            DOM.removeChild(getElement(), content);
                            DOM.appendChild(td, content);
                        }
                    }
                    DOM.appendChild(getElement(), table);
                }
                // set alignment
                DOM.setElementAttribute(td, "align", horizontalAlignment);
            }
        }

        void setSpacingEnabled(boolean b) {
            setStyleName(
                    getElement(),
                    CLASSNAME
                            + "-"
                            + (orientationMode == ORIENTATION_HORIZONTAL ? StyleConstants.HORIZONTAL_SPACING
                                    : StyleConstants.VERTICAL_SPACING), b);
        }

    }

    /**
     * Returns given widgets WidgetWrapper
     * 
     * @param child
     * @return
     */
    public WidgetWrapper getWidgetWrapperFor(Widget child) {
        final Element containerElement = DOM.getParent(child.getElement());
        return new WidgetWrapper(containerElement);
    }

}
