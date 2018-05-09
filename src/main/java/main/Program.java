package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import createOntology.OntologyClassCreator;
import createOntology.TagDownloader;
import databaseControl.DatabaseManager;
import databaseControl.GameDownloader;
import semanticSearch.SemanticSearcher;

public class Program {
	private static String outputPath = "GamingOntology.owl";
	
	/**
	 * Main entry point of the program. The first parameter must
	 * be either 'Search' 'CreateOntology' or 'FillDB'
	 * 
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args) {		
		try {
			System.out.println("Program start");			
			
			if(args == null || args.length == 0) {
				System.out.println("Missing function argument, possible values: (Search, CreateOntology, FillDB)");
				System.exit(-1);
			}
			
			ProgramStart(args);
			
			//CreateOntology();
			
			//DownloadGamesList();
			
			//GetGamesData();
			
			//PrintGameDataToDb();
			
			//RunSearchFunction();
			
			System.out.println("Finished c:");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Main control function, decides which function to run based on parameter.
	 * 'Search' runs the main function of the project: expects a tag from user input, and searches for said tag.
	 * Prints out the results in three columns: appId, title, url (where the game might be accessed)
	 * Also prints out which games are found thanks to the ontology so we can feel good about ourselves.
	 * 'CreateOntology' Asks the user for necessary input, and Creates an ontology containing all the tags,
	 * but no additional information, ontology needs to be ordered manually.
	 * 'FillDB' Resets the database, downloads the set number of games from Steam,
	 *  and fills the RDF database with this data.
	 * 
	 * @param args String list, first element must be either 'Search' 'CreateOntology' or 'FillDB'
	 * @throws Exception If something breaks. Shouldnt really happen.
	 */
	public static void ProgramStart(String[] args) throws Exception {
		String function = args[0].toLowerCase().trim();
		
		switch (function) {
			case "search": RunSearchFunction(); break;
			case "createontology": CreateOntology(); break;
			case "filldb": PrintGameDataToDb(); break;
			default: System.out.println("Unknown keyword: " + function); break;
		}
	}
	
	private static void RunSearchFunction() throws IOException {
		while(true) {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	        System.out.print("Enter tag to search for: ");
	        String term = br.readLine();
			if(term.equals("-1")) {
				break;
			}
			Search(term);
		}
	}
	
	private static void Search(String term) {
		SemanticSearcher searcher = new SemanticSearcher(new DatabaseManager());
		Set<String> results = searcher.Search(term);
		System.out.println("Results:");
		System.out.printf ("%-8s %-40s %s \n", "Appid", "Title", "Url");
		for(String r : results) {
			String[] parts = r.split(DatabaseManager.separator);
			System.out.printf ("%-8s %-40s %s \n", parts[0], parts[1], GameDownloader.individualGamePageUrl + parts[0]);
		}
		Set<String> withoutOntologyResults = searcher.Search(term, false);
		Set<String> leftOutGames = new HashSet<String>(results);
		leftOutGames.removeAll(withoutOntologyResults);
		System.out.println("These games would have been left out had it not been for an ontology:");
		System.out.printf ("%-8s %-10s \n", "Appid", "Title");
		for(String r : leftOutGames) {
			String[] parts = r.split(DatabaseManager.separator);
			System.out.printf ("%-8s %-15s \n", parts[0], parts[1]);
		}
	}
	
	private static void debugGetSubclasses() {
		SemanticSearcher searcher = new SemanticSearcher(new DatabaseManager());
		System.out.println(searcher.getSubClasses("Horror", false));
	}
	
	private static void debugSearchForTag() {
		DatabaseManager manager = new DatabaseManager();
		System.out.println(manager.searchForTag("Multiplayer"));
	}
	
	private static void debugGetTagsForGame() {
		GameDownloader gd = new GameDownloader();
		gd.GetTagsForGame(69);
	}
	
	private static List<String> DownloadTags() {
		List<String> tags = TagDownloader.getTagsFromSteamSpy();
		System.out.println(tags);
		return tags;
	}
	
	private static void CreateOntology() throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Ontology name or path: ");
        String term = br.readLine();
        if(!(term.isEmpty() || term.length() == 0)) {
        	outputPath = term;
        } else {
        	System.out.println("Using default path: " + outputPath);
        }
		
        List<String> tags = DownloadTags();
		OntologyClassCreator creator = new OntologyClassCreator();
		creator.CreateClasses(tags);
		creator.SaveOntology(outputPath);
	}
	
	private static void DownloadGamesList() {
		GameDownloader gd = new GameDownloader();
		List<Integer> appids = gd.GetAppids();
		System.out.println(appids);
		System.out.println(appids.size());
	}
	
	private static void GetGamesData() {
		GameDownloader gd = new GameDownloader();
		List<String> data = gd.DownloadGameData(gd.GetAppids());
		System.out.println(data);
	}
	
	private static void PrintGameDataToDb() {
		GameDownloader gd = new GameDownloader();
		List<String> data = gd.DownloadGameData(gd.GetAppids());
		DatabaseManager manager = new DatabaseManager();
		manager.ResetDatabase();
		manager.SaveToDatabase(data);
	}

}
