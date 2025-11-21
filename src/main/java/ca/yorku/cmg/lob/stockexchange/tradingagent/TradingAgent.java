package ca.yorku.cmg.lob.stockexchange.tradingagent;

import ca.yorku.cmg.lob.stockexchange.StockExchange;
import ca.yorku.cmg.lob.stockexchange.events.Event;
import ca.yorku.cmg.lob.stockexchange.events.NewsBoard;
import ca.yorku.cmg.lob.trader.Trader;

/**
 * A trading agent that can both:
 *  - poll the NewsBoard (pull model, used in pollingTest), and
 *  - receive pushed events as an observer (pushTest).
 *
 * The actual reaction is delegated to an ITradingStrategy.
 */
public abstract class TradingAgent implements INewsObserver {

    protected Trader t;
    protected StockExchange exc;
    protected NewsBoard news;

    protected ITradingStrategy strategy;

    /**
     * Constructor
     */
    public TradingAgent(Trader t, StockExchange e, NewsBoard n, ITradingStrategy strategy) {
        this.t = t;
        this.exc = e;
        this.news = n;
        this.strategy = strategy;

        // Register this agent as an observer for push model
        n.register(this);
    }

    /**
     * Called by StockExchangeTest in pollingTest().
     * Still uses the original pull-based behaviour.
     */
    public void timeAdvancedTo(long time) {
        pollForEvents(time);
    }

    /**
     * Pull model: ask the NewsBoard if there is an event at this time.
     */
    private void pollForEvents(long time) {
        Event e = news.getEventAt(time);
        if (e != null) {
            examineEvent(e);
        }
    }

    /**
     * Common logic to check if this agent has a position in the event's security
     * and then delegate to the strategy.
     */
    private void examineEvent(Event e) {
        int positionInSecurity =
                exc.getAccounts()
                   .getTraderAccount(t)
                   .getPosition(e.getSecrity().getTicker());

        if (positionInSecurity > 0) {
            int price = exc.getPrice(e.getSecrity().getTicker());
            actOnEvent(e, positionInSecurity, price);
        }
    }

    /**
     * Strategy hook – by default we just delegate to ITradingStrategy.
     */
    protected void actOnEvent(Event e, int pos, int price) {
        if (strategy != null) {
            strategy.actOnEvent(e, pos, price);
        }
    }

    /**
     * Observer callback – used in the push model (pushTest()).
     * NewsBoard calls this directly when an event happens.
     */
    @Override
    public void update(Event e) {
        examineEvent(e);
    }
}
