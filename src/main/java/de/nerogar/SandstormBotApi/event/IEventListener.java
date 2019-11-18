package de.nerogar.sandstormBotApi.event;

public interface IEventListener<T extends IEvent> {

	void trigger(T event);

}
