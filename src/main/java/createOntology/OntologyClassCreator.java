package createOntology;

import java.io.FileOutputStream;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import openllet.core.OpenlletOptions;
import openllet.owlapi.OpenlletReasonerFactory;

public class OntologyClassCreator {
	
	public static final String namespace = "http://hu.komplexmi.e5q6ui#";
	
	final OWLOntologyManager manager;
	final OWLOntology onto;
	final PrefixManager pm;
	final OWLReasoner reasoner;
	final OWLDataFactory fac;
	
	/**
	 *  Standard constructor for the class.
	 *  Initializes and sets up the private ontology manager, 
	 *  the ontology used, the reasoner and the object factory.
	 */
	public OntologyClassCreator() throws Exception {
		manager = OWLManager.createOWLOntologyManager();
		fac = manager.getOWLDataFactory();
		onto = manager.createOntology();
		manager.getOntologyFormat(onto);
		pm = (PrefixDocumentFormat)manager.getOntologyFormat(onto);
		pm.setDefaultPrefix(namespace);
		System.out.println("Ontológia betöltve: " + manager.getOntologyDocumentIRI(onto));

		OpenlletOptions.USE_UNIQUE_NAME_ASSUMPTION = true;
		OWLReasonerFactory reasonerFactory = new OpenlletReasonerFactory();
		reasoner = reasonerFactory.createReasoner(onto);
	}
	
	private OWLClass cls(String name) {
		return fac.getOWLClass(name, pm);
	}
	
	/**
	 * Creates an owl class for every element in the given list.
	 * Every created entity uses the namespace http://hu.komplexmi.e5q6ui#
	 *
	 * @param  tags  A String list containing all the tags.
	 */
	public void CreateClasses(List<String> tags) {
		for(String tag : tags) {
			OWLClass tagClass = cls(tag);
			System.out.println("Creating class: " + tagClass.getIRI());
			OWLAxiom declaration = fac.getOWLDeclarationAxiom(tagClass);
			manager.addAxiom(onto, declaration);
		}
	}
	
	
	/**
	 * Saves the ontology in an RDF Xml format to the filepath given.
	 * .owl extension advised.
	 *
	 * @param  filePath  The filepath where the new ontology file should be created.
	 */
	public void SaveOntology(String filePath) {
		try {
			manager.saveOntology(onto, new RDFXMLDocumentFormat(), new FileOutputStream(filePath));
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
}




























