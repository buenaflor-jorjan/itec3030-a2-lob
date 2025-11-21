package ca.yorku.cmg.lob.stockexchange.tradingagent;

import ca.yorku.cmg.lob.stockexchange.events.Event;

/**
 * Implemented by any class that wants to receive news events.
 */
public interface INewsObserver {
    void update(Event e);
}
