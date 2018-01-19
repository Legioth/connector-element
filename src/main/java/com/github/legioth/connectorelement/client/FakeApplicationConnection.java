package com.github.legioth.connectorelement.client;

import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.ConnectorMap;
import com.vaadin.client.LayoutManager;
import com.vaadin.client.ServerConnector;

public class FakeApplicationConnection extends ApplicationConnection {
    private static int nextConnectorId = 1;

    private static FakeApplicationConnection INSTANCE;

    private FakeApplicationConnection() {
        getServerRpcQueue().setConnection(this);
        LayoutManager.get(this).setConnection(this);
    }

    public static FakeApplicationConnection get() {
        if (INSTANCE == null) {
            INSTANCE = new FakeApplicationConnection();
        }
        return INSTANCE;
    }

    public void registerAndInit(ServerConnector connector) {
        String connectorId = String.valueOf(nextConnectorId++);
        ConnectorMap.get(this).registerConnector(connectorId, connector);
        connector.doInit(connectorId, this);
    }
}
