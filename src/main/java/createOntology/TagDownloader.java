package createOntology;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;

public class TagDownloader {
	private static final String URL = "https://store.steampowered.com/tag/browse#global_492";
	private static final String tagExtractRegex = "^\\s*<div class=\"tag_browse_tag( active)?\" data-tagid=\"[0-9]+\">(.*?)<\\/div>$";
	
	private static final String tagsCsvPath = "tags.csv";
	
	public static List<String> getTagsFromSteamPage(){
		List<String> result = new ArrayList<String>();
		Pattern pattern = Pattern.compile(tagExtractRegex);
		
		try {
			URL url = new URL(URL);
			InputStream is = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			
			String line = null;
			while ((line = br.readLine()) != null) {
				line = StringEscapeUtils.unescapeHtml4(line);
	            System.out.println(line);
	            Matcher matcher = pattern.matcher(line);
	            if(matcher.find()) {
	            	System.out.println("Found tag: " + matcher.group(2));
	            	result.add(matcher.group(2).replaceAll(" ", "_"));
	            }
	        }
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static List<String> getTagsFromSteamSpy(){
		List<String> result = new ArrayList<String>();
		
		try {
			FileInputStream fs = new FileInputStream(new File(tagsCsvPath));
			BufferedReader br = new BufferedReader(new InputStreamReader(fs));
			int counter = 0;
			String line;
			while((line = br.readLine()) != null) {
				if(counter >= 50)
					break;
				
				String tag = line.split(";")[1];
				result.add(tag.replaceAll(" ", "_"));
				counter++;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
}
