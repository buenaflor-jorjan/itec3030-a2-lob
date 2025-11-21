package ca.yorku.cmg.lob.stockexchange.tradingagent;

import ca.yorku.cmg.lob.stockexchange.StockExchange;
import ca.yorku.cmg.lob.stockexchange.events.NewsBoard;
import ca.yorku.cmg.lob.trader.Trader;

public class TradingAgentRetail extends TradingAgent {

    public TradingAgentRetail(Trader t, StockExchange e, NewsBoard n, ITradingStrategy strategy) {
        super(t, e, n, strategy);
    }
}
