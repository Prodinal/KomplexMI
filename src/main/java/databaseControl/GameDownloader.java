package databaseControl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GameDownloader {

	/**
	 * Part of the non-public api url of Steam, returns a json object
	 * containing the title and appId of every artifact on Steam.
	 */
	public static final String gamesListUrl = "http://api.steampowered.com/ISteamApps/GetAppList/v0001/";
	/**
	 * Part of the non-public api url of Steam, returns data 
	 * (unfortunately not including tags) about the game in json format.
	 * An appid needs to be appended to the end before using.
	 */
	public static final String individualGameDataUrl = "https://store.steampowered.com/api/appdetails?appids=";
	/**
	 * Url to the user page of a game. Used to gather tags for the game.
	 * An appId needs to be appended to the end before using.
	 */
	public static final String individualGamePageUrl = "https://store.steampowered.com/app/";
	private static final String tagExtractRegex = "<a href=\"https:\\/\\/store\\.steampowered\\.com\\/tags\\/.+?\" class=\"app_tag\" (style=\".+?\")?>(.+?)(\\t)+?<\\/a>";

	private final JsonParser jp;
	/**
	 * Set the max number of games to download from the steam. 
	 * These downloads might take a few minutes. Set to -1 for all games.
	 * Default value is 100.
	 */
	public static int MaxDownload = 100;

	/**
	 * Standard constructor.
	 */
	public GameDownloader() {
		jp = new JsonParser();
	}

	/**
	 * Uses the gamesListUrl to download all the appIds from Steam.
	 * 
	 * @return Integer list containing all the appIds returned from Steam.
	 */
	public List<Integer> GetAppids() {
		List<Integer> result = new ArrayList<>();
		try {
			URL url = new URL(gamesListUrl);
			URLConnection request = url.openConnection();
			request.connect();
			JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
			JsonObject rootobj = root.getAsJsonObject();

			JsonArray apps = rootobj.get("applist").getAsJsonObject().get("apps").getAsJsonObject().get("app")
					.getAsJsonArray();
			JsonObject app;
			for(int i = 0; i < apps.size(); i++) {
				app = apps.get(i).getAsJsonObject();
				result.add(app.get("appid").getAsInt());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Downloads the data to be stored later from Steam. 
	 * Creates a String list, where each entry is an rdf triplet
	 * in the same format as used by DatabaseManager.
	 * The data returned currently includes title, metascore, category, tags and type
	 * 
	 * @param appids
	 * @return
	 * @see DatabaseManager
	 */
	public List<String> DownloadGameData(List<Integer> appids) {
		List<String> result = new ArrayList<>();
		int i = 0;
		for (int appid : appids) {
			if (i >= MaxDownload && MaxDownload > 0)
				break; // for debugging
			
			System.out.print(i + "/" + MaxDownload + " " );
			List<String> gameData = GetDataForGame(appid);
			result.addAll(gameData);
			i++;
		}
		return result;
	}

	private List<String> GetDataForGame(int appid) {
		List<String> result = new ArrayList<>();
		try {
			String gameUrl = individualGameDataUrl + appid;
			URL url = new URL(gameUrl);
			URLConnection request = url.openConnection();
			request.connect();

			JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
			JsonObject gameObj = root.getAsJsonObject().get("" + appid).getAsJsonObject();
			if (!gameObj.get("success").getAsBoolean()) {
				System.out.println(appid + " - Not a game");
				return result;
			}
			JsonObject data = gameObj.get("data").getAsJsonObject();

			String title = data.get("name").getAsString();
			result.add(getTitleString(appid, title));
			System.out.println("Adding game: " + appid + " - " + title);		//DEBUG
			
			String type = data.get("type").getAsString();
			result.add(getTypeString(appid, type));
			
			if(data.get("metacritic") != null) {
				int score = data.get("metacritic").getAsJsonObject().get("score").getAsInt();
				result.add(getMetaScoreString(appid, score));
			}
			
			if(data.get("categories") != null) {
				JsonArray categories = data.get("categories").getAsJsonArray();
				String category;
				for(int i = 0; i < categories.size(); i++) {
					category = categories.get(i).getAsJsonObject().get("description").getAsString();
					result.add(getCategoryString(appid, category));
				}
			}
			if(type.equals("game")) {
				for(String tag : GetTagsForGame(appid)) {
					result.add(getTagString(appid, tag));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	/**
	 * Returns a String list containing all tags connected to the game appId.
	 * This data is not available on an Steam api, so the user friendly game page
	 * is opened, and a built in regex expression is used to extract the tags from the webpage.
	 * <a href='https://store.steampowered.com/app/570'>Example page</a> 
	 * 
	 * @param appid The appId of the game for which the tags will be downloaded.
	 * @return A String list containing all the tags.
	 */
	public List<String> GetTagsForGame(int appid){
		List<String> result = new ArrayList<>();
		
		try {
			Pattern pattern = Pattern.compile(tagExtractRegex);
			URL url = new URL(individualGamePageUrl + appid);
			InputStream is = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(StringEscapeUtils.unescapeHtml4(line).trim());
	        }
			String page = sb.toString();
			Matcher matcher = pattern.matcher(page);
			while(matcher.find()) {
				String tag = matcher.group(2);
				result.add(tag);
			}
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	private String getTitleString(int appid, String title) {
		return "" + appid + DatabaseManager.separator + DatabaseManager.title + DatabaseManager.separator + title;
	}
	private String getMetaScoreString(int appid, int score) {
		return "" + appid + DatabaseManager.separator + DatabaseManager.metaScore + DatabaseManager.separator + score;
	}
	private String getCategoryString(int appid, String category) {
		return "" + appid + DatabaseManager.separator + DatabaseManager.category + DatabaseManager.separator + category;
	}
	private String getTypeString(int appid, String type) {
		return "" + appid + DatabaseManager.separator + DatabaseManager.type + DatabaseManager.separator + type;
	}
	private String getTagString(int appid, String tag) {
		return "" + appid + DatabaseManager.separator + DatabaseManager.tag + DatabaseManager.separator + tag;
	}
}
