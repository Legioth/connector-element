# connector-element
A custom element that encapsulates any Vaadin `Connector`.
This is intended as a proof of concept for reusing current Vaadin connectors together with Vaadin Flow.

See https://github.com/Legioth/connector-element/blob/master/src/main/webapp/index.html for a usage example.

## Element API
The GWT module exports a custom element, `<connector-element>`, with one required attribute.
* `connector` should be set to the fully qualified name of the corresponding `AbstractComponentConnector` implementation, e.g. `com.vaadin.ui.Button`.

### Shared state
Any other attribute or property will be mapped to a correspondingly named shared state field.
For instance `<connector-element connector="com.vaadin.ui.Button" caption="Click me!"></connector-element>` will correspond to setting the same value to the `caption` property in `ButtonState`.
Attribute values are only checked once when the custom element gets attached, but changes to the corresponding JavaScript property will be discovered immediately and send a corresponding `StateChangeEvent` to the connector implementation.

### Client to server RPC
Any RPC event sent by the connector will be fired as a regular DOM event of the type `rpc`.
The DOM event will have three custom additional properties:
* `interface` is the fully qualified name of the RPC interface, e.g. `com.vaadin.shared.ui.button.ButtonServerRpc`.
* `method` is the name of the invoked RPC method, e.g. `click`.
* `arguments` is a JavaScript array of method arguments.

### Server to client RPC
The custom element defines a custom function named `rpc`.
This function expects three parameters: 
1. The name of the RPC interface to call, e.g. `com.vaadin.shared.ui.MediaControl`.
2. The name of the RPC method, e.g. `play`.
3. A JavaScript array with arguments to the RPC method.

### Children
If the connector implements `HasComponentsConnector`, its widget will be attached inside a shadow root.
Each light-dom child element will be assigned to its own `<slot>`, for which there will be a corresponding child connector attached to the connector of the `<connector-element>`.

## Known limitations
* `LayoutManager` functionality is not properly integrated
* Only supports connectors with `LoadStyle.EAGER` (which is the default).
* Only reacts to property changes but no attribute changes
* No support for Vaadin 6 style communication with `updateFromUIDL` and `updateVariable`.
* Undefined behavior for shared state variables that have a name that clashes with built-in property names.
