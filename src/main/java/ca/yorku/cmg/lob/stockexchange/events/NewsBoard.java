package ca.yorku.cmg.lob.stockexchange.events;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

import ca.yorku.cmg.lob.security.Security;
import ca.yorku.cmg.lob.security.SecurityList;
import ca.yorku.cmg.lob.stockexchange.tradingagent.INewsObserver;

/**
 * A NewsBoard object generates and shares financial/economic events
 * that affect specific securities.
 *
 * It supports:
 *  - getEventAt(time)  -> pull model (used by pollingTest)
 *  - runEventsList()   -> push model using INewsObserver (used by pushTest)
 */
public class NewsBoard {

    // Events are queued ordered by time
    private PriorityQueue<Event> eventQueue =
            new PriorityQueue<>((e1, e2) -> Long.compare(e1.getTime(), e2.getTime()));

    private SecurityList securities;

    // Observers (TradingAgents) interested in news
    private List<INewsObserver> observers = new ArrayList<>();

    public NewsBoard(SecurityList x) {
        this.securities = x;
    }

    // Allowed event values
    private static final Set<String> VALID_EVENTS = new HashSet<>(
            Arrays.asList("Good", "Bad")
    );

    /**
     * Register an observer (e.g., TradingAgent) to receive pushed events.
     */
    public void register(INewsObserver o) {
        if (o != null && !observers.contains(o)) {
            observers.add(o);
        }
    }

    /**
     * Optionally support removal.
     */
    public void removeObserver(INewsObserver o) {
        observers.remove(o);
    }

    /**
     * Notify all observers about an event.
     */
    private void notifyObservers(Event e) {
        for (INewsObserver o : observers) {
            o.update(e);
        }
    }

    /**
     * Load events from file. Format:
     * [Time, Relevant Ticker, EventType], where EventType is one of "Good" or "Bad"
     */
    public void loadEvents(String filePath) {
        String line;
        String delimiter = ","; // Assuming the CSV is comma-separated

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            while ((line = br.readLine()) != null) {
                String[] values = line.split(delimiter);

                // Ensure the line has exactly three columns
                if (values.length != 3) {
                    System.err.println("Invalid line format: " + line);
                    continue;
                }

                String time = values[0].trim();
                String ticker = values[1].trim();
                String event = values[2].trim();

                // Validate the event
                if (!VALID_EVENTS.contains(event)) {
                    System.err.println("Invalid event value: " + event + " in line: " + line);
                    continue;
                }

                Security s = securities.getSecurityByTicker(ticker);

                if (s == null) {
                    System.err.println("Unknown ticker: " + ticker + " in line: " + line);
                    continue;
                }

                Event eventObj;
                switch (event) {
                    case "Good":
                        eventObj = new GoodNews(Long.parseLong(time), s);
                        break;
                    case "Bad":
                        eventObj = new BadNews(Long.parseLong(time), s);
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected event value: " + event);
                }

                eventQueue.add(eventObj);
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }
    }

    /**
     * Pull model: returns the event that happened at time {@code time}.
     *
     * Used by TradingAgent.timeAdvancedTo(...) in pollingTest().
     */
    public Event getEventAt(long time) {

        PriorityQueue<Event> clonedQueue = new PriorityQueue<>(eventQueue);
        Event e = null;

        while (!clonedQueue.isEmpty()) {
            long next = clonedQueue.peek().getTime();
            if (time > next) {
                clonedQueue.poll();
            } else if (time < next) {
                return null;
            } else { // time == next
                return clonedQueue.poll();
            }
        }
        return e;
    }

    /**
     * Push model: run through the entire queue of events and
     * notify all registered TradingAgents for each event.
     *
     * Used in StockExchangeTest.pushTest().
     */
    public void runEventsList() {
        while (!eventQueue.isEmpty()) {
            Event e = eventQueue.poll();
            notifyObservers(e);
        }
    }
}
