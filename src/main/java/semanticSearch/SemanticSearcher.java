package semanticSearch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.OWLReasonerRuntimeException;

import createOntology.OntologyClassCreator;
import databaseControl.DatabaseManager;

public class SemanticSearcher {
	private static final String ontologyPath = "GamingOntology_done.owl";
	private static final String ontologyNamespace = "http://www.w3.org/2002/07/hu.komplexmi.e5q6ui#";
	private final DatabaseManager dm;
	
    OWLOntologyManager manager;
    OWLOntology ontology;
    OWLReasoner reasoner;
    OWLDataFactory factory;
	
	public SemanticSearcher(DatabaseManager dm) {
		this.dm = dm;
		manager = OWLManager.createOWLOntologyManager();
		
		try {
			ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyPath));
		} catch (Exception e) {
			e.printStackTrace();
		}
		OWLReasonerFactory reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();
		reasoner = reasonerFactory.createReasoner(ontology);
		
		try {
	        if (!reasoner.isConsistent()) {
	        	System.err.println("Az ontológia nem konzisztens!");
	        	
	            Node<OWLClass> incClss = reasoner.getUnsatisfiableClasses();
                System.err.println("A következõ osztályok nem konzisztensek:" + incClss.getEntities());
	        	System.exit(-1);
	        }
		} catch (OWLReasonerRuntimeException e) {
			System.err.println("Hiba a következtetõben: " + e.getMessage());
			System.exit(-1);
		}
		factory = manager.getOWLDataFactory();
	}
	
	public List<String> getSubClasses(String className, boolean direct) {
        IRI clsIRI = IRI.create(ontologyNamespace + className);
        System.out.println("Searching for IRI: " + clsIRI);
        if (!ontology.containsClassInSignature(clsIRI)) {
        	System.out.println("Tag is not present in ontology:" + className);
        	return Collections.EMPTY_LIST;
        }
        // Létrehozzuk az osztály egy példányát és lekérdezzük a leszármazottait.
        OWLClass cls = factory.getOWLClass(clsIRI);
        NodeSet<OWLClass> subClss;
		try {
			subClss = reasoner.getSubClasses(cls, direct);
		} catch (OWLReasonerRuntimeException e) {
			System.err.println("Hiba az alosztályok következtetése közben: " + e.getMessage());
			return Collections.EMPTY_LIST;
		}
        List<String> result = new ArrayList<>();
        for(OWLClass i : subClss.getFlattened()) {
        	if(!i.isBuiltIn())
        		result.add(i.getIRI().getFragment());
        }
        return result;
    }
	
	public Set<String> Search(String search){
		return Search(search, true);
	}
	
	public Set<String> Search(String search, boolean expandSearch){
		Set<String> result = new HashSet<>();
		result.addAll(dm.searchForTag(search));
		
		if(expandSearch) {
			for(String extraSearch : getSubClasses(search, false)) {
				result.addAll(dm.searchForTag(extraSearch));
			}			
		}
		
		return result;
	}
}
