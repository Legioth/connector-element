package com.github.legioth.connectorelement.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

public class Slot extends Element {

    protected Slot() {
        // JSO
    }

    public static Slot create(String name) {
        Element slot = Document.get().createElement("slot");
        if (name != null && !name.isEmpty()) {
            slot.setAttribute("name", name);
        }
        return (Slot) slot;
    }

    public native final Node[] getAssignedNodes()
    /*-{
        return this.assignedNodes(); 
    }-*/;

    public native final Node[] addSlotChangeHandler(
            Runnable runnable)
    /*-{
        this.addEventListener("slotchange", function() {
           runnable.@Runnable::run()();
        }); 
    }-*/;

    public static native final Slot getAssignedSlot(Node slotable)
    /*-{
        return slotable.assignedSlot; 
    }-*/;

    public native final void assign(Node slotable)
    /*-{
        slotable.slot = this.name; 
    }-*/;
}
