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

	public static final String title = "title";
	public static final String category = "category";
	public static final String metaScore = "metaScore";
	public static final String type = "type";
	public static final String tag = "tag";
	public static final String separator = "_";

	public static final String generalQuery = "" 
			+ "SELECT * WHERE"
			+ "{"
			+ "?appid ?pred ?search ;" 
				   + "?titlePred ?title ." 
			+ "}";

	private final Repository repo;
	private final ValueFactory factory;

	private static final String url = "http://localhost:8080/rdf4j-server";
	private static final String repoId = "steamgames";

	public DatabaseManager() {
		repo = new HTTPRepository(url, repoId);
		factory = repo.getValueFactory();
	}
	
	public void ResetDatabase() {
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.clearNamespaces();
		}
	}

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
