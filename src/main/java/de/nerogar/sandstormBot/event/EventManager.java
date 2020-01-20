package de.nerogar.sandstormBot.event;

import de.nerogar.sandstormBot.Logger;
import de.nerogar.sandstormBot.Main;
import de.nerogar.sandstormBotApi.event.IEvent;
import de.nerogar.sandstormBotApi.event.IEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventManager {

	private class EventContainer<T extends IEvent> extends ArrayList<IEventListener<T>> {}

	private Map<Class<? extends IEvent>, EventContainer<? extends IEvent>> listeners;

	public EventManager() {
		listeners = new HashMap<>();
	}

	@SuppressWarnings("unchecked")
	public <T extends IEvent> void register(Class<T> eventClass, IEventListener<T> listener) {
		List<IEventListener<T>> eventListeners = (EventContainer<T>) listeners.computeIfAbsent(eventClass, ec -> new EventContainer<T>());
		eventListeners.add(listener);
	}

	@SuppressWarnings("unchecked")
	public <T extends IEvent> void unregister(Class<T> eventClass, IEventListener<T> listener) {
		EventContainer<T> eventContainer = (EventContainer<T>) listeners.get(eventClass);
		eventContainer.remove(listener);
	}

	@SuppressWarnings("unchecked")
	public <T extends IEvent> void trigger(T event) {
		List<IEventListener<T>> eventListeners = (EventContainer<T>) listeners.computeIfAbsent(event.getClass(), c -> new EventContainer<>());

		for (IEventListener<T> eventListener : eventListeners) {
			eventListener.trigger(event);
		}
	}

}
