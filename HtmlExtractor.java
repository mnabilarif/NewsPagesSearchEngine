import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.htmlparser.beans.StringBean;


public class HtmlExtractor {
	
	public String getContent(String url)
	{
		StringBean strbean = new StringBean();
		
		//do not need information about url links in the page
		strbean.setLinks(false);
		
		//substitute  incessant blanks with regular blank
		strbean.setReplaceNonBreakingSpaces(true);
		
		//substitute successive blanks with a single blank
		strbean.setCollapse(true);
		
		//give the url to be parsed
		strbean.setURL(url);
		
		return strbean.getStrings();
	}
	

	public static void main(String[] args) throws FileNotFoundException {
		int count = 1;
		Map<String, String> fileUrlMap = new HashMap<String, String>();
		fileUrlMap(fileUrlMap);
		File dir = new File("crawl_data"); // file name changed here
		for (File file: dir.listFiles()) {
			Parser aExtraction = new Parser();
			String content = aExtraction.getContent(fileUrlMap.get(file.getName()));
			File outPutFile = new File("op/" + count + ".txt");
			count++;
			PrintWriter output = new PrintWriter(outPutFile);
			output.println(content);
			output.close();
			
		}
	}

	private static void fileUrlMap(Map<String, String> map) {
		try {
			File csv = new File("mapCNNFile.csv");
			Scanner inputStream = new Scanner(csv);
			inputStream.useDelimiter(",|\\n");
			while (inputStream.hasNext()) {
				String key = inputStream.next();
				String value = inputStream.next();
				map.put(key, value);
			}
			File csv1 = new File("mapUSATodayFile.csv");
			Scanner inputStream1 = new Scanner(csv1);
			inputStream1.useDelimiter(",|\\n");
			while (inputStream1.hasNext()) {
				String key = inputStream1.next();
				String value = inputStream1.next();
				map.put(key, value);
			}
			inputStream.close();
			inputStream1.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
