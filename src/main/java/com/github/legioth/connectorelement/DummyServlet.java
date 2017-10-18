package com.github.legioth.connectorelement;

import javax.servlet.annotation.WebServlet;

import com.vaadin.server.VaadinServlet;

// Dummy servlet just to serve theme and "widgetset" files
@WebServlet("/VAADIN/*")
public class DummyServlet extends VaadinServlet {

}
