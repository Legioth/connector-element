package com.github.legioth.connectorelement.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ui.AbstractComponentConnector;

public class FakeChildConnector extends AbstractComponentConnector {

    private static int nextSlotId;

    private static class FakeChildWidget extends SimplePanel {
        private FakeChildConnector connector;

        public FakeChildWidget(FakeChildConnector connector, String slotName) {
            super(Slot.create(slotName));
            this.connector = connector;
        }

        public Slot getSlot() {
            Element element = getElement();
            return (Slot) element;
        }
    }

    private final String slotName = "slot" + nextSlotId++;
    private final FakeChildWidget widget;

    public FakeChildConnector(Node childNode) {
        widget = new FakeChildWidget(this, slotName);
        widget.getSlot().assign(childNode);
    }

    @Override
    public Widget getWidget() {
        return widget;
    }

    public static FakeChildConnector get(Slot slot) {
        EventListener maybeWidget = Event.getEventListener(slot);
        if (maybeWidget instanceof FakeChildWidget) {
            return ((FakeChildWidget) maybeWidget).connector;
        } else {
            return null;
        }
    }

    public Slot getSlot() {
        return widget.getSlot();
    }

}
