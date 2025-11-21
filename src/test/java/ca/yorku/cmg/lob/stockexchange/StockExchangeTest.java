package ca.yorku.cmg.lob.stockexchange;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ca.yorku.cmg.lob.orderbook.Order;
import ca.yorku.cmg.lob.stockexchange.tradingagent.TradingAgent;
import ca.yorku.cmg.lob.trader.Trader;



/**
 * Tests functioning of the stock exchange.
 */
class StockExchangeTest {


	StockExchange exc = null;
	String expectedVal = null;
	ArrayList<TradingAgent> traders = null; 


	/**
	 * Runs before each of pollingTest and pushTest 
	 */
	@BeforeEach
	void setUp() {
		exc = new StockExchange();
		
		exc.readSecurityListfromFile("src/test/resources/securities.csv"); 
		exc.readAccountsListFromFile("src/test/resources/accounts.csv");
		exc.readInitialPositionsFromFile("src/test/resources/initial.csv");
		exc.readPriceListfromFile("src/test/resources/prices.csv");
		exc.getNewsBoard().loadEvents("src/test/resources/events.csv");
		traders = exc.getTraders();
		
		//Read the expected test output for the assertions.
		try {
			Path filePath = Paths.get("src/test/resources/testOut.txt");
			expectedVal = Files.readString(filePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Reset objects and statics
	 */
	@AfterEach
	void tearDown() {
		Trader.resetID();
		Order.resetID();
		exc = null;
		traders = null;
		expectedVal = null;
	}

	/**
	 * This test should always pass.
	 */
	@Test
	void pollingTest() {
		
		for (int i = 0; i <= 30; i++) {
			for (TradingAgent t : this.traders) {
				t.timeAdvancedTo(i);
			}
		}
				
		System.out.println("OBSERVED:" + exc.getLogTestSample());
		System.out.println("EXPECTED:" + expectedVal);
		assertEquals(expectedVal,exc.getLogTestSample());
	}

	
	
	/**
	 * !!!!! I M P O R T A N T !!!!!!
	 * This test does not initially pass. It requires a solution to the observer part of the assignment for it to pass.
	 * To turn it off while working on the first parts uncomment the @Disabled annotation below. 
	 */
	@Disabled
	@Test
	void pushTest() {
		
		exc.getNewsBoard().runEventsList();

	
		System.out.println("OBSERVED:" + exc.getLogTestSample());
		System.out.println("EXPECTED:" + expectedVal);
		assertEquals(expectedVal,exc.getLogTestSample());
	}
	
	
}
