import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import com.csvreader.CsvWriter;  
import edu.uci.ics.crawler4j.crawler.Page; 
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.crawler.exceptions.PageBiggerThanMaxSizeException;
import edu.uci.ics.crawler4j.fetcher.PageFetchResult;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.parser.HtmlParseData; 
import edu.uci.ics.crawler4j.url.WebURL; 

public class MyCrawler extends WebCrawler {
	private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|xml|x-icon" + "|mid|mp2|mp3|mp4" 
			   + "|wav|avi|mov|mpeg|ram|m4v" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$"); 
	
	private String[] myCrawlDomains;
	

    private final static String CSV_PATH = "crawl/fetch_NewsSite1.csv";
    private CsvWriter cw;  
    private File csv1;  
    private File csv2;
    private File csv3;  
    private File csv4;  
    private File csv5;  
    private File csv6;
    private Set<String> set1 = new HashSet<String>();
    private Set<String> set2 = new HashSet<String>();
    private PageFetcher pageFetcher;
  
    public MyCrawler() throws IOException { 
    	csv1 = new File("crawl/fetch_NewsSite1.csv"); 
    	if (csv1.isFile()) {
    		csv1.delete();
    	}
        csv2 = new File("crawl/fetch_NewsSite2.csv"); 
        	if (csv2.isFile()) {
        		csv2.delete();
        	}
        csv3 = new File("crawl/visit_NewsSite1.csv"); 
        	if (csv1.isFile()) {
        		csv1.delete();
        	}
        csv4 = new File("crawl/visit_NewsSite2.csv"); 
        	if (csv1.isFile()) {
        		csv1.delete();
        	}
        csv5 = new File("crawl/urls_Newsite1.csv"); 
        	if (csv1.isFile()) {
        		csv1.delete();
        	}
        csv6 = new File("crawl/urls_Newsite2.csv"); 
        	if (csv1.isFile()) {
        		csv1.delete();
        	}
    }  
	 public void onStart() { 
		  myCrawlDomains = (String[]) myController.getCustomData();
		 } 

/**
* This method receives two parameters. The first parameter is the page
* in which we have discovered this new url and the second parameter is
* the new url. You should implement this function to specify whether
* the given url should be crawled or not (based on your crawling logic).
* In this example, we are instructing the crawler to ignore urls that
* have css, js, git, ... extensions and to only accept urls that start
* with "http://www.ics.uci.edu/". In this case, we didn't need the
* referringPage parameter to make the decision.
*/
@Override
public boolean shouldVisit(Page referringPage, WebURL url) {
		String ur = url.getURL();  
	    String statuscode =  String.valueOf(referringPage.getStatusCode());
	    String href = url.getURL().toLowerCase();
	    try {
	    if(myCrawlDomains[0]=="http://www.cnn.com/"){
			cw = new CsvWriter(new FileWriter(csv5, true), ',');
			cw.write(ur);
			set1.add(ur);
			cw.endRecord();  
			cw.close(); 
		 }
	    if(myCrawlDomains[0]=="http://www.usatoday.com/"){
				cw = new CsvWriter(new FileWriter(csv6, true), ',');
				cw.write(ur); 
				set2.add(ur);
		        cw.endRecord();  
		        cw.close();  
			}
		}
		catch (IOException e) {  
	        e.printStackTrace();  
	    }
      	return (!FILTERS.matcher(href).matches()
      	&& href.startsWith("http://www.cnn.com/"))||
      	(!FILTERS.matcher(href).matches()
      	&& href.startsWith("http://www.usatoday.com/"));
	 
}
public void onBeforeExit() {
	try {
		 if(myCrawlDomains[0]=="http://www.cnn.com/"){
		 File outPutFile = new File("output1.txt");
		 PrintWriter output;
		 output = new PrintWriter(outPutFile);
	     output.println(set1.size());
	     output.close();
		 }
		 if(myCrawlDomains[0]=="http://www.usatoday.com/"){
				File outPutFile = new File("output2.txt");
				PrintWriter output;
		 output = new PrintWriter(outPutFile);
	     output.println(set2.size());
	     output.close();
		 }
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {
    // Do nothing by default
    // Sub-classed can override this to add their custom functionality
	String ur = webUrl.getURL();  
	String statuscode =  String.valueOf(statusCode);
	try {
		if(myCrawlDomains[0]=="http://www.cnn.com/"){
			cw = new CsvWriter(new FileWriter(csv1, true), ',');
			cw.write(ur);  
			cw.write(statuscode);  
            cw.endRecord();
			cw.close(); 
		 }
		if(myCrawlDomains[0]=="http://www.usatoday.com/"){
			cw = new CsvWriter(new FileWriter(csv2, true), ',');
			cw.write(ur);  
	        cw.write(statuscode);
	        cw.endRecord();  
	        cw.close();  
		}
	}
	catch (IOException e) {  
        e.printStackTrace();  
    }
}

/**
* This function is called when a page is fetched and ready
* to be processed by your program.
*/
@Override
public void visit(Page page)  {
	  String url = page.getWebURL().getURL();  
	  String statuscode =  String.valueOf(page.getStatusCode());
	  try{
		  if(myCrawlDomains[0]=="http://www.cnn.com/"){
		  cw = new CsvWriter(new FileWriter(csv3, true), ',');
          cw.write(url);  
          cw.write(String.valueOf((page.getContentData().length)/1024));
          cw.write(String.valueOf(page.getParseData().getOutgoingUrls().size()));
          cw.write(page.getContentType());
          cw.endRecord();  
          cw.close();  
		  }
		  if(myCrawlDomains[0]=="http://www.usatoday.com/"){
			  cw = new CsvWriter(new FileWriter(csv4, true), ',');
	          cw.write(url);  
	          cw.write(String.valueOf((page.getContentData().length)/1024));
	          cw.write(String.valueOf(page.getParseData().getOutgoingUrls().size()));
	        		//System.out.println("Text length: " + text.length()); System.out.println("Html length: " + html.length()); System.out.println("Number of outgoing links: " + links.size());
	          cw.write(page.getContentType());
	          cw.endRecord();  
	          cw.close();  
		  }
	  }
	  catch (IOException e) {  
          e.printStackTrace();  
      }
}
}
