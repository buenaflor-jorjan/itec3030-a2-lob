package ca.yorku.cmg.lob.stockexchange;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ca.yorku.cmg.lob.orderbook.Ask;
import ca.yorku.cmg.lob.orderbook.Bid;
import ca.yorku.cmg.lob.orderbook.Orderbook;
import ca.yorku.cmg.lob.orderbook.Trade;
import ca.yorku.cmg.lob.security.Security;
import ca.yorku.cmg.lob.security.SecurityList;
import ca.yorku.cmg.lob.stockexchange.events.NewsBoard;
import ca.yorku.cmg.lob.stockexchange.tradingagent.AbstractTradingAgentFactory;
import ca.yorku.cmg.lob.stockexchange.tradingagent.TradingAgent;
import ca.yorku.cmg.lob.stockexchange.tradingagent.TradingAgentFactory;
import ca.yorku.cmg.lob.trader.Trader;
import ca.yorku.cmg.lob.trader.TraderInstitutional;
import ca.yorku.cmg.lob.trader.TraderRetail;
import ca.yorku.cmg.lob.tradestandards.IOrder;

/**
 * Represents a stock exchange that manages securities, accounts, orders, and trades.
 */
public class StockExchange {

    private Orderbook book;
    private NewsBoard newsDesk;

    private SecurityList securities = new SecurityList();
    private AccountsList accounts = new AccountsList();
    private ArrayList<Trade> tradesLog = new ArrayList<>();
    private ArrayList<TradingAgent> traders = new ArrayList<>();

    private ArrayList<IOrder> log = new ArrayList<>();

    private Map<String, Integer> prices = new HashMap<>();

    long totalFees = 0;

    // Factory for creating TradingAgent objects with strategies
    private AbstractTradingAgentFactory agentFactory = new TradingAgentFactory();

    /**
     * Default constructor for the Exchange class.
     */
    public StockExchange() {
        book = new Orderbook();
        newsDesk = new NewsBoard(getSecurities());
    }

    /**
     * A method stub that traders or other calling environments can call to
     * register a new order.
     */
    public void submitOrder(IOrder order, long time) {
        if (order instanceof Bid) {
            book.getBids().addOrder((Bid) order);
        } else {
            book.getAsks().addOrder((Ask) order);
        }
        log.add(order);
    }

    /**
     * Returns the price of a ticker.
     */
    public int getPrice(String tkr) {
        return prices.get(tkr);
    }

    /**
     * Generate a string that describes the output, for testing purposes.
     */
    public String getLogTestSample() {
        StringBuilder out = new StringBuilder();
        for (IOrder r : log) {
            out.append(
                String.format("[%3d  %s  %6d  %6d]",
                    r.getTrader().getID(),
                    r.getSecurity().getTicker(),
                    r.getPrice(),
                    r.getQuantity())
            );
        }
        return out.toString();
    }

    /**
     * Read the initial prices of the stocks from a file.
     */
    public void readPriceListfromFile(String filePath) {
        String line;
        String delimiter = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(delimiter);

                if (parts.length != 3) {
                    System.err.println("Invalid line format: " + line);
                    continue;
                }

                String tkr = parts[0].trim();
                String priceStr = parts[2].trim();

                try {
                    int value = Integer.parseInt(priceStr);
                    prices.put(tkr, value);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number format for value: " + priceStr + " in line: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }
    }

    /**
     * Reads the security list from a file and populates the exchange.
     */
    public void readSecurityListfromFile(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            boolean isFirstLine = true; // Skip header

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length >= 2) {
                    String code = parts[0].trim();
                    String description = parts[1].trim();
                    securities.addSecurity(code, description);
                } else {
                    System.err.println("Skipping malformed line: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads the accounts list from a file and populates the exchange.
     * Uses the Abstract Factory to create TradingAgent objects.
     */
    public void readAccountsListFromFile(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            boolean isFirstLine = true; // Skip header

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length >= 5) {
                    String traderTitle = parts[0].trim();
                    String traderType = parts[1].trim();   // "Retail" or "Institutional"
                    String accType     = parts[2].trim();   // "Basic" or "Pro"
                    long initBalance   = Long.parseLong(parts[3].trim());
                    String tradingStyle = parts[4].trim();  // "Conservative" or "Aggressive"

                    // 1. Create Trader (Retail vs Institutional)
                    Trader t;
                    if ("Retail".equals(traderType)) {
                        t = new TraderRetail(traderTitle);
                    } else {
                        t = new TraderInstitutional(traderTitle);
                    }

                    // 2. Create Account (Basic vs Pro)
                    if ("Basic".equals(accType)) {
                        accounts.addAccount(new AccountBasic(t, initBalance));
                    } else {
                        accounts.addAccount(new AccountPro(t, initBalance));
                    }

                    // 3. Use Abstract Factory to create the TradingAgent with the right strategy
                    String typeForFactory =
                        "Retail".equals(traderType) ? "Retail" : "Institutional";

                    TradingAgent agent = agentFactory.createAgent(
                        typeForFactory,   // "Retail" / "Institutional"
                        tradingStyle,     // "Conservative" / "Aggressive"
                        t,
                        this,
                        newsDesk
                    );

                    traders.add(agent);

                } else {
                    System.err.println("Skipping malformed line (too few attributes): " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads initial positions from a file and updates account holdings.
     */
    public void readInitialPositionsFromFile(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            boolean isFirstLine = true; // Skip header

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length >= 3) {
                    Integer tid = Integer.valueOf(parts[0].trim());
                    String tkr = parts[1].trim();
                    Integer count = Integer.valueOf(parts[2].trim());
                    Trader trad = accounts.getTraderByID(tid);

                    if (trad == null) {
                        System.err.println("Initial Balances: Trader does not exist: " + line);
                    } else if (securities.getSecurityByTicker(tkr) == null) {
                        System.err.println("Initial Balances: Ticker not traded in this exchange: " + line);
                    } else {
                        accounts.getTraderAccount(trad).updatePosition(tkr, count);
                    }
                } else {
                    System.err.println("Skipping malformed line (too few attributes): " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes a file containing orders and submits them to the exchange.
     */
    public void processOrderFile(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            boolean isFirstLine = true; // Skip header

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length >= 6) {
                    int traderID = Integer.valueOf(parts[0].trim());
                    String tkr   = parts[1].trim();
                    String type  = parts[2].trim();
                    int qty      = Integer.valueOf(parts[3].trim());
                    int price    = Integer.valueOf(parts[4].trim());
                    long time    = Long.valueOf(parts[5].trim());

                    Trader t = getAccounts().getTraderByID(traderID);
                    Security sec = getSecurities().getSecurityByTicker(tkr);

                    if ((t != null) && (sec != null)) {
                        if ("ask".equals(type)) {
                            submitOrder(new Ask(t, sec, price, qty, time), time);
                        } else if ("bid".equals(type)) {
                            submitOrder(new Bid(t, sec, price, qty, time), time);
                        } else {
                            System.err.println("Order type not found (skipping): " + line);
                        }
                    }
                } else {
                    System.err.println("Skipping malformed line: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String printAskTable(boolean header) {
        return book.getAsks().printAllOrders(header);
    }

    public String printBidTable(boolean header) {
        return book.getBids().printAllOrders(header);
    }

    public String printTradesLog(boolean header) {
        StringBuilder output = new StringBuilder();
        if (header) {
            output.append("[From____  To______  Tkr_  Quantity  Price__  Time____]\n");
        }
        for (Trade t : tradesLog) {
            output.append(t.toString());
        }
        return output.toString();
    }

    public String printBalances(boolean header) {
        return accounts.debugPrintBalances(header);
    }

    public String printFeesCollected(boolean header) {
        if (header) {
            return String.format(
                "            Fees Collected TOTAL: %16s",
                String.format("$%,.2f", this.totalFees / 100.0)
            );
        } else {
            return String.format(
                "%16s",
                String.format("$%,.2f", this.totalFees / 100.0)
            );
        }
    }

    //
    // GETTERS
    //

    public SecurityList getSecurities() {
        return securities;
    }

    public AccountsList getAccounts() {
        return accounts;
    }

    public ArrayList<TradingAgent> getTraders() {
        return traders;
    }

    public NewsBoard getNewsBoard() {
        return newsDesk;
    }
}
