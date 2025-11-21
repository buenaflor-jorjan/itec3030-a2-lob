package ca.yorku.cmg.lob.stockexchange.tradingagent;

import ca.yorku.cmg.lob.stockexchange.StockExchange;
import ca.yorku.cmg.lob.stockexchange.events.NewsBoard;
import ca.yorku.cmg.lob.trader.Trader;

public class TradingAgentFactory extends AbstractTradingAgentFactory {

    @Override
    public TradingAgent createAgent(String type,
                                    String style,
                                    Trader t,
                                    StockExchange e,
                                    NewsBoard n) {

        // 1) Choose strategy
        ITradingStrategy strategy;
        if ("Aggressive".equalsIgnoreCase(style)) {
            strategy = new AggressiveStrategy(t, e);
        } else {
            strategy = new ConservativeStrategy(t, e);
        }

        // 2) Choose concrete agent class
        if ("Institutional".equalsIgnoreCase(type)) {
            return new TradingAgentInstitutional(t, e, n, strategy);
        } else if ("Retail".equalsIgnoreCase(type)) {
            return new TradingAgentRetail(t, e, n, strategy);
        }

        throw new IllegalArgumentException("Unknown agent type: " + type);
    }
}
