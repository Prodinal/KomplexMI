package databaseControl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;

import createOntology.OntologyClassCreator;

public class DatabaseManager {

	/**
	 * Name of the title property used in the database
	 */
	public static final String title = "title";
	/**
	 * Name of the category property used in the database
	 */
	public static final String category = "category";
	/**
	 * Name of the metaScore property used in the database
	 */
	public static final String metaScore = "metaScore";
	/**
	 * Name of the type property used in the database, this is not the RDF:type
	 */
	public static final String type = "type";
	/**
	 * Name of the tag property used in the database
	 */
	public static final String tag = "tag";
	/**
	 * Separator used to join parts when passing an rdf triplet as a single string.
	 * E.g. when sending data with appid 12345, whose tag is Horror the string is:
	 * 12345_ns:tag_ns:Horror (ns is the namespace used)
	 */
	public static final String separator = "_";

	private static final String generalQuery = "" 
			+ "SELECT * WHERE"
			+ "{"
			+ "?appid ?pred ?search ;" 
				   + "?titlePred ?title ." 
			+ "}";

	private final Repository repo;
	private final ValueFactory factory;

	private static final String url = "http://localhost:8080/rdf4j-server";
	private static final String repoId = "steamgames";

	/**
	 * Standard constructor, initializes and sets up
	 * the httprepository and the object factory.
	 */
	public DatabaseManager() {
		repo = new HTTPRepository(url, repoId);
		factory = repo.getValueFactory();
	}
	
	/**
	 * Deletes all data from the database.
	 */
	public void ResetDatabase() {
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.clearNamespaces();
		}
	}

	
	/**
	 * Creates triples from the String list provided and uploads them
	 * to the rdf database. The third part of the triplet will be treated as
	 * a literal, the first two parts as IRIs
	 * 
	 * @param data String list, where each entry is an RDF triplet joined by DatabaseManager.SEPARATOR
	 */
	public void SaveToDatabase(List<String> data) {
		try (RepositoryConnection conn = repo.getConnection()) {
			for (String entry : data) {
				String[] parts = entry.split(separator);
				if (parts.length != 3)
					throw new Exception("There was a " + separator + " in the data: " + entry);

				IRI appid = factory.createIRI(OntologyClassCreator.namespace, parts[0]);
				IRI pred = factory.createIRI(OntologyClassCreator.namespace, parts[1]);
				Literal value = factory.createLiteral(parts[2]);

				conn.add(appid, pred, value);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks the ontology if the given tag is a class, and if so
	 * adds the descendants of that ontolgy class to the search query.
	 * Searches the database for all the extra tags, and the original tag.
	 * Returns the appIds and the titles of the found games.
	 * 
	 * 
	 * @param tag The tag to search for.
	 * @return A String list, where each entry is an appid and a title joined by the SEPARATOR
	 *		   E.g. 12345_GameTitle
	 */
	public List<String> searchForTag(String tag){
		List<String> result = new ArrayList<>();
		try(RepositoryConnection conn = repo.getConnection()){
			TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, generalQuery);
			
			query.setBinding("pred", factory.createIRI(OntologyClassCreator.namespace, DatabaseManager.tag));
			query.setBinding("search", factory.createLiteral(tag));
			query.setBinding("titlePred", factory.createIRI(OntologyClassCreator.namespace, DatabaseManager.title));
			
			TupleQueryResult queryResult = query.evaluate();
			while(queryResult.hasNext()) {
				BindingSet set = queryResult.next();
				IRI appid = (IRI)set.getValue("appid");
				Literal title = (Literal)set.getValue("title");
				result.add(appid.getLocalName() + separator + title.getLabel());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
		
	}
}
