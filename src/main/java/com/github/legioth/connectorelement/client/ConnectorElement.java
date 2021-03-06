package com.github.legioth.connectorelement.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Display;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ConnectorHierarchyChangeEvent;
import com.vaadin.client.ConnectorMap;
import com.vaadin.client.HasComponentsConnector;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.Util;
import com.vaadin.client.communication.JsonDecoder;
import com.vaadin.client.communication.JsonEncoder;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.metadata.Method;
import com.vaadin.client.metadata.NoDataException;
import com.vaadin.client.metadata.Property;
import com.vaadin.client.metadata.Type;
import com.vaadin.client.metadata.TypeData;
import com.vaadin.shared.AbstractComponentState;
import com.vaadin.shared.communication.ClientRpc;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonException;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class ConnectorElement extends Element {
    public static final boolean debug = false;

    private class PropertyHandler {
        private String name;

        public PropertyHandler(String name) {
            this.name = name;
        }

        public void set(JsonValue value) {
            updateState(name, value);
        }

        public JsonValue get() {
            return readState(name);
        }
    }

    protected ConnectorElement() {
        // JSNI CTOR
    }

    public final void attachedCallback() {
        ComponentConnector connector = getConnector();
        if (connector == null) {
            try {
                connector = createConnector();
            } catch (NoDataException e) {
                log(e);
                return;
            }
        }

        FakeApplicationConnection connection = FakeApplicationConnection.get();

        boolean isChildContainer = connector instanceof HasComponentsConnector;
        FakeParentConnector fakeParent = new FakeParentConnector(this,
                isChildContainer);

        if (isChildContainer) {
            // Collects children not assigned anywhere else
            Slot defaultSlot = Slot.create(null);

            // Don't show text nodes that won't be distributed
            defaultSlot.getStyle().setDisplay(Display.NONE);

            defaultSlot.addSlotChangeHandler(() -> {
                // Update children if there's something to distribute
                if (defaultSlot.getAssignedNodes().length > 0) {
                    updateChildren();
                }
            });

            fakeParent.getWidget().getElement().appendChild(defaultSlot);
        }

        connection.registerAndInit(fakeParent);

        fakeParent.setChild(connector);

        connector.fireEvent(new StateChangeEvent(connector, null, true));
    }

    private void updateChildren() {
        HasComponentsConnector hasComponentsConnector = (HasComponentsConnector) getConnector();

        List<ComponentConnector> oldChildren = hasComponentsConnector
                .getChildComponents();
        List<ComponentConnector> children = new ArrayList<>();

        NodeList<Node> childNodes = getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.getItem(i);

            // Ignore text nodes since we can't assign them to slots
            if (!Element.is(childNode)) {
                continue;
            }

            Slot assignedSlot = Slot.getAssignedSlot(childNode);
            String slotName = assignedSlot.getAttribute("name");

            // Assigned to default slot -> this is a new child
            if (slotName == null || slotName.equals("")) {
                FakeChildConnector fakeChild = new FakeChildConnector(
                        childNode);

                Slot childSlot = fakeChild.getSlot();

                childSlot.addSlotChangeHandler(() -> {
                    // Update children if child slot has become empty
                    if (childSlot.getAssignedNodes().length == 0) {
                        updateChildren();
                    }
                });

                children.add(fakeChild);
            } else {
                FakeChildConnector fakeChild = FakeChildConnector
                        .get(assignedSlot);

                children.add(fakeChild);
            }
        }

        if (!oldChildren.equals(children)) {
            ConnectorHierarchyChangeEvent event = new ConnectorHierarchyChangeEvent();
            event.setConnector(hasComponentsConnector);
            event.setOldChildren(new ArrayList<>(oldChildren));

            hasComponentsConnector.setChildComponents(children);
            hasComponentsConnector.setChildren(new ArrayList<>(children));
            hasComponentsConnector.fireEvent(event);
        }
    }

    private ComponentConnector createConnector() throws NoDataException {
        FakeApplicationConnection connection = FakeApplicationConnection.get();

        String connectorClass = getAttribute("connector");

        Type type = TypeData.getType(TypeData.getClass(connectorClass));
        ComponentConnector connector = (ComponentConnector) type
                .createInstance();
        setConnector(connector);

        connection.registerAndInit(connector);

        /*
         * Must do this before adding property listeners, since otherwise
         * everything would seem to be defined even though it isn't
         */
        updateFullState(connector);

        addStatePropertyListeners(getStateType(connector));

        attachRpcHandler(this);

        return connector;
    }

    private static native void attachRpcHandler(ConnectorElement self)
    /*-{
        self.rpc = function(iface, method, arguments) {
          @ConnectorElement::handleRpc(*)(self, iface, method, arguments);
        };
    }-*/;

    private static void handleRpc(ConnectorElement self, String iface,
            String methodName, JsonArray argumentsJson) {
        ComponentConnector connector = self.getConnector();

        try {
            Method method = new Type(iface, null).getMethod(methodName);

            Type[] parameterTypes = method.getParameterTypes();
            Object[] arguments = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                arguments[i] = JsonDecoder.decodeValue(parameterTypes[i],
                        argumentsJson.get(i), null, connector.getConnection());
            }

            for (ClientRpc target : connector.getRpcImplementations(iface)) {
                method.invoke(target, arguments);
            }
        } catch (NoDataException e) {
            log(iface + "." + methodName);
            log(e);
        }
    }

    private void addStatePropertyListeners(Type stateType)
            throws NoDataException {
        JsArrayObject<Property> properties = stateType.getPropertiesAsArray();
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            String name = property.getName();

            addPropertyHandler(this, name, new PropertyHandler(name));

        }
    }

    private native static void addPropertyHandler(ConnectorElement self,
            String name, PropertyHandler propertyHandler)
    /*-{
        Object.defineProperty(self, name, {
            enumerable: true,
            get: $entry(function() { return propertyHandler.@PropertyHandler::get()(); }),
            set: $entry(function(value) { propertyHandler.@PropertyHandler::set(*)(value); })
        });
    }-*/;


    private void updateFullState(ComponentConnector connector)
            throws NoDataException {
        AbstractComponentState state = connector.getState();
        Type stateType = getStateType(connector);
        JsArrayObject<Property> properties = stateType.getPropertiesAsArray();
        for (int i = 0; i < properties.size(); i++) {
            Property property = properties.get(i);
            JsonValue value;
            String name = property.getName();

            if (hasAttribute(propertyToAttribute(name))
                    || hasProperty(this, name)) {
                value = getAttributeOrProperty(name);
            } else {
                continue;
            }

            Object decodeValue = JsonDecoder.decodeValue(property.getType(),
                    value, null, connector.getConnection());
            property.setValue(state, decodeValue);
        }
    }

    private JsonValue getAttributeOrProperty(String propertyName) {
        String attributeName = propertyToAttribute(propertyName);
        if (hasAttribute(attributeName)) {
            String attributeValue = getAttribute(attributeName);
            try {
                return Json.parse(attributeValue);
            } catch (JsonException e) {
                return Json.create(attributeValue);
            }
        } else if (hasProperty(this, propertyName)) {
            return Util.jso2json(getPropertyJSO(propertyName));
        } else {
            return null;
        }
    }

    private static Type getStateType(ComponentConnector connector) {
        return new Type(connector.getState().getClass().getName(), null);
    }

    public final static native boolean hasProperty(ConnectorElement self,
            String name)
    /*-{
        return self.hasOwnProperty(name);
    }-*/;

    private static native final ConnectorMap getConnectorMap(
            ApplicationConnection connection)
    /*-{
        return connection.@ApplicationConnection::connectorMap;
    }-*/;

    public final ComponentConnector getConnector() {
        return (ComponentConnector) getPropertyObject("connector");
    }

    public final void setConnector(ComponentConnector connector) {
        setPropertyObject("connector", connector);
    }

    public final void detachedCallback() {
        log("Detached");
        log(this);
    }

    public final void attributeChangedCallback(String attrName,
            JsonValue oldVal, JsonValue newVal) {
        log("Attribute change");
        log(this);
        log(attrName);
        log(oldVal);
        log(newVal);

        if (getConnector() == null) {
            // Connector not yet created
            return;
        }

        String propertyName = attributeToProperty(attrName);
        updateState(propertyName, newVal);
    }

    private static final String attributeToProperty(String attributeName) {
        StringBuilder b = new StringBuilder();
        String[] parts = attributeName.split("-");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i != 0) {
                part = Character.toUpperCase(part.charAt(0))
                        + part.substring(1);
            }
            b.append(part);
        }

        return b.toString();
    }

    private static final String propertyToAttribute(String propertyName) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < propertyName.length(); i++) {
            char c = propertyName.charAt(i);
            if (Character.isUpperCase(c)) {
                b.append('-');
                b.append(Character.toLowerCase(c));
            } else {
                b.append(c);
            }
        }

        return b.toString();
    }

    private void updateState(String propertyName, JsonValue value) {
        try {
            ComponentConnector connector = getConnector();
            Property property = getStateType(connector)
                    .getProperty(propertyName);
            Type propertyType = property.getType();
            if (propertyType == null) {
                log("Property not found in connector: " + propertyName);
                return;
            }

            Object decodedValue = JsonDecoder.decodeValue(propertyType, value,
                    null, connector.getConnection());
            property.setValue(connector.getState(), decodedValue);

            JsonObject stateChangeJson = Json.createObject();
            stateChangeJson.put(propertyName, value);

            log("Property " + propertyName + " changed to " + value.toJson());

            connector.fireEvent(
                    new StateChangeEvent(connector, stateChangeJson, false));
        } catch (NoDataException e) {
            log(propertyName);
            log(e);
        }
    }

    private JsonValue readState(String name) {
        try {
            ComponentConnector connector = getConnector();
            Property property = getStateType(connector).getProperty(name);
            Type propertyType = property.getType();

            Object value = property.getValue(connector.getState());

            return JsonEncoder.encode(value, propertyType,
                    connector.getConnection());

        } catch (NoDataException e) {
            log(name);
            log(e);
            return null;
        }
    }

    public static void log(Object message) {
        if (debug) {
            doLog(message);
        }
    }

    public static native void doLog(Object message)
    /*-{
        $wnd.console.log(message);
    }-*/;
}