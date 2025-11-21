package ca.yorku.cmg.lob.stockexchange.tradingagent;

import ca.yorku.cmg.lob.stockexchange.StockExchange;
import ca.yorku.cmg.lob.stockexchange.events.Event;
import ca.yorku.cmg.lob.stockexchange.events.NewsBoard;
import ca.yorku.cmg.lob.trader.Trader;

/**
 * A trading agent that receives news and delegates reaction to a strategy.
 */
public abstract class TradingAgent {

    protected Trader t;
    protected StockExchange exc;
    protected NewsBoard news;

    // Strategy
    protected ITradingStrategy strategy;

    /**
     * Constructor.
     */
    public TradingAgent(Trader t,
                        StockExchange e,
                        NewsBoard n,
                        ITradingStrategy strategy) {
        this.t = t;
        this.exc = e;
        this.news = n;
        this.strategy = strategy;
    }

    public void setStrategy(ITradingStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Called as time advances to {@code time}.
     * Agent polls the NewsBoard for events (pull model).
     */
    public void timeAdvancedTo(long time) {
        pollForEvents(time);
    }

    private void pollForEvents(long time) {
        Event e = news.getEventAt(time);
        if (e != null) {
            examineEvent(e);
        }
    }

    /**
     * Examine if an event is relevant for the Agent
     * (i.e., if the Agent has a position in that security).
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
     * Delegates the reaction to the configured strategy.
     */
    protected void actOnEvent(Event e, int pos, int price) {
        if (strategy != null) {
            strategy.actOnEvent(e, pos, price);
        }
    }
}
