package com.github.legioth.connectorelement.client;

import com.google.gwt.core.client.EntryPoint;
import com.vaadin.client.metadata.ConnectorBundleLoader;

import elemental.json.JsonValue;

public class ConnectorElementExporter implements EntryPoint {

    @SuppressWarnings("unused")
    // GWT didn't allow calling JSO methods straight from JSNI, so using these
    // bridge methods
    private static class JsniBridge {
        public static void attachedCallback(ConnectorElement connector) {
            connector.attachedCallback();
        }

        public static void detachedCallback(ConnectorElement connector) {
            connector.detachedCallback();
        }

        /*
         * To support attribute changes, we'd also have to generate a custom
         * class with a suitable observedAttributes value for each connector
         * type. This is a "regression" from custom elements v0.
         */
        public static void attributeChangedCallback(ConnectorElement connector,
                String attrName, JsonValue oldVal, JsonValue newVal) {
            connector.attributeChangedCallback(attrName, oldVal, newVal);
        }
    }

    @Override
    public void onModuleLoad() {
        // Only support eager connectors for now
        ConnectorBundleLoader.get()
                .loadBundle(ConnectorBundleLoader.EAGER_BUNDLE_NAME, null);

        registerWebComponent();
    }

    private static native void registerWebComponent()
    /*-{
        var attach = $entry(@JsniBridge::attachedCallback(*));
        var detach = $entry(@JsniBridge::detachedCallback(*));
        
        // Work around JSNI syntax not supporting "class"
        (new $wnd.Function("attach", "detach", 
          "class ConnectorElement extends HTMLElement { " +
             "connectedCallback() { attach(this); } " + 
             "disconnectedCallback() { detach(this); } " + 
           "};" + 
           "customElements.define('connector-element', ConnectorElement);"
        ))(attach, detach);
    }-*/;

}
