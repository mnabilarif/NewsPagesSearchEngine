import org.apache.log4j.BasicConfigurator;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class Controller {

		 public static void main(String[] args) throws Exception {
			 BasicConfigurator.configure();
			 String crawlStorageFolder = "crawl";
		        int numberOfCrawlers = 7;
		        int maxDepthOfCrawling = 16;
		        int maxPagesToFetch = 10000;
		 
		        CrawlConfig config1 = new CrawlConfig();
		        CrawlConfig config2 = new CrawlConfig();
		        config1.setCrawlStorageFolder(crawlStorageFolder+ "/crawler1");
		        config1.setMaxPagesToFetch(maxPagesToFetch);
		        config1.setMaxDepthOfCrawling(maxDepthOfCrawling);
		        config2.setCrawlStorageFolder(crawlStorageFolder+ "/crawler2");
		        config2.setMaxPagesToFetch(maxPagesToFetch);
		        config2.setMaxDepthOfCrawling(maxDepthOfCrawling);
		        
		        /*
		         * Instantiate the controller for this crawl.
		         */
		        PageFetcher pageFetcher1 = new PageFetcher(config1);
		        PageFetcher pageFetcher2 = new PageFetcher(config2);
		        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher1);
		        CrawlController controller1 = new CrawlController(config1, pageFetcher1, robotstxtServer);
		        CrawlController controller2 = new CrawlController(config2, pageFetcher2, robotstxtServer);
		        String[] crawler1Domains = new String[] { "http://www.cnn.com/" }; 
		        String[] crawler2Domains = new String[] { "http://www.usatoday.com/" }; 
		        config1.setIncludeBinaryContentInCrawling(true);
		        config2.setIncludeBinaryContentInCrawling(true);
		        controller1.setCustomData(crawler1Domains); 
		        controller2.setCustomData(crawler2Domains); 
		        /*
		         * For each crawl, you need to add some seed urls. These are the first
		         * URLs that are fetched and then the crawler starts following links
		         * which are found in these pages
		         */
		        controller1.addSeed("http://www.cnn.com/");
		        controller2.addSeed("http://www.usatoday.com/");
		        //controller.addSeed("http://www.ics.uci.edu/");
		 
		        /*
		         * Start the crawl. This is a blocking operation, meaning that your code
		         * will reach the line after this only when crawling is finished.
		         */
		        //ImageCrawler.configure(crawlDomains, storageFolder);
		        controller1.startNonBlocking(MyCrawler.class, numberOfCrawlers);
		        controller2.startNonBlocking(MyCrawler.class, numberOfCrawlers);
		        
		        controller1.waitUntilFinish(); 
		        System.out.println("Crawler 1 is finished."); 
		       
		        controller2.waitUntilFinish(); 
		        System.out.println("Crawler 2 is finished."); 
		    }
}
