package de.nerogar.sandstormBot.gui;

import de.nerogar.sandstormBot.event.EventManager;

import java.util.ArrayList;
import java.util.List;

public class Gui {

	private EventManager       eventManager;
	private List<MessagePanel> panels;

	public Gui(EventManager eventManager) {
		this.eventManager = eventManager;
		panels = new ArrayList<>();
	}

	public void addPanel(MessagePanel panel) {
		panels.add(panel);
	}

	public void update() {
		for (MessagePanel panel : panels) {
			panel.update();
		}
	}

}
