/*
 * $Id: MyApp.java 2057 2015-09-11 06:24:08Z euzenat $
 *
 * Copyright (C) INRIA, 2009-2010, 2013-2014
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

// Alignment API classes
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.AlignmentVisitor;

// Alignment API implementation classes
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.method.StringDistAlignment;
import fr.inrialpes.exmo.align.impl.renderer.OWLAxiomsRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;

// Jena
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;

// OWL API
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

// HermiT
import org.semanticweb.HermiT.Reasoner;

// LogMap repair (uncomment to use: need library)
/*
import uk.ac.ox.krr.logmap2.LogMap2_RepairFacility;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;
import uk.ac.ox.krr.logmap2.oaei.reader.MappingsReaderManager;
*/

// Alcomo repair (uncomment to use: need library)
/*
import de.unima.alcomox.ontology.IOntology;
import de.unima.alcomox.mapping.Mapping;
import de.unima.alcomox.mapping.MappingWriterXml;
import de.unima.alcomox.ExtractionProblem;
import de.unima.alcomox.Settings;
*/

// IDDL
import fr.paris8.iut.info.iddl.IDDLReasoner;
import fr.paris8.iut.info.iddl.IDDLException;
import fr.paris8.iut.info.iddl.conf.Semantics;

// SAX standard classes
import org.xml.sax.SAXException;

// DOM Standard classes
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathConstants;

// Java standard classes
import java.util.Set;
import java.util.ArrayList;
import java.util.Properties;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

/**
 * MyApp
 *
 * Reconcile two ontologies in various ways
 */

public class MyApp {

    static String RESTServ = "http://aserv.inrialpes.fr/rest/";

    public static void main( String[] args ) {
	try {
	    new MyApp().run( args );
	} catch ( Exception ex ) {
	    ex.printStackTrace();
	}
    }

    public void run( String[] args ) throws IDDLException {
	String myId = "Test";
	Alignment al = null;
	URI uri1 = null;
	URI uri2 = null;
	String u1 = "file:ontology1.owl";
	String u2 = "file:ontology2.owl";
	String method = "fr.inrialpes.exmo.align.impl.method.StringDistAlignment";
	Properties params = new Properties();
	try {
	    uri1 = new URI( u1 );
	    uri2 = new URI( u2 );
	} catch (URISyntaxException use) { use.printStackTrace(); }

	System.out.println( "\n\n ########## MATCHING ########## " );

	System.out.println( " ***** Looking for alignment on the server ***** " );
	// ask for it
	String found = getFromURLString( RESTServ+"find?onto1="+u1+"&onto2="+u2, false );
	System.out.println( found );
	// retrieve it
	// If there exists alignments, ask for the first one
	NodeList alset = extractFromResult( found, "//findResponse/alignmentList/alid[1]/text()", false );
	System.out.println( alset );

	System.out.println( " ***** Matching ontologies on the server ***** " );
	if ( alset.getLength() == 0 ) {
	    // call for matching
	    // * tested (must add force = true)
	    //String match = getFromURLString( RESTServ+"match?onto1="+u1+"&onto2="+u2+"&method="+method+"&pretty="+myId+"&action=Match", true );
	    // This returns a URI
	}

	if ( alset.getLength() > 0 ) {
	    // Ask it
	    String alid = alset.item(0).getNodeValue();
	    found = getFromURLString( RESTServ+"retrieve?method=fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor&id="+alid, false );
	    alset = extractFromResult( found, "//retrieveResponse/result/*[1]",true );

	    // This code is heavy
	    String xmlString = null;
	    try {
		// Set up the output transformer
		TransformerFactory transfac = TransformerFactory.newInstance();
		Transformer trans = transfac.newTransformer();
		trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		trans.setOutputProperty(OutputKeys.INDENT, "yes");

		// Print the DOM node
		StringWriter sw = new StringWriter();
		DOMSource source = new DOMSource( alset.item(0) );
		trans.transform(source, new StreamResult(sw));
		xmlString = sw.toString();
		//System.out.println(xmlString);
	    } catch (TransformerException e) {
		e.printStackTrace();
	    }

	    // parse it as an alignment
	    try {
		AlignmentParser aparser = new AlignmentParser(0);
		Alignment alu = aparser.parseString( xmlString );
		al = ObjectAlignment.toObjectAlignment((URIAlignment)alu);
	    } catch (AlignmentException ae) { 
		ae.printStackTrace();
	    }
	}
	if ( al != null ) {
	    System.out.println( " Matched ontologies in "+al+" containing "+al.nbCells()+" correspondences" );
	} else {
	    System.out.println( " Obtained no alignment..." );
	}

	System.out.println( " ***** Matching ontologies locally ***** " );
	if ( al == null ){ // Unfortunatelly no alignment was available
	    AlignmentProcess ap = new StringDistAlignment();
	    try {
		ap.init( uri1, uri2 );
		params.setProperty("stringFunction","smoaDistance");
		params.setProperty("noinst","1");
		ap.align( (Alignment)null, params );
		al = ap;
		// Supplementary:
		// upload the result on the server
		// store it
	    } catch (AlignmentException ae) { ae.printStackTrace(); }
	}
	System.out.println( " Matched ontologies in "+al+" containing "+al.nbCells()+" correspondences" );

	// Alternative: find an intermediate ontology between which there are alignments
	// find (basically a graph traversal operation)
	// retrieve them
	// parse them
	// compose them

	System.out.println( "\n\n ########## REASONING ########## " );

	System.out.println( " ***** Merging ontologies ***** " );
	PrintWriter writer = null;
	File merged = null;
	try {
	    merged = File.createTempFile( "MyApp-results",".owl");
	    merged.deleteOnExit();
	    writer = new PrintWriter ( new FileWriter( merged, false ), true );
	    AlignmentVisitor renderer = new OWLAxiomsRendererVisitor( writer );
	    al.render(renderer);
	} catch (UnsupportedEncodingException uee) {
	    uee.printStackTrace();
	} catch (AlignmentException ae) {
	    ae.printStackTrace();
	} catch (IOException ioe) { 
	    ioe.printStackTrace();
	} finally {
	    if ( writer != null ) {
		writer.flush();
		writer.close();
	    }
	}
	// If merged is empty then destroy the file + exit
	System.out.println( "Merged file in "+merged );

	System.out.println( " ***** Testing consistency (and coherency) with HermiT ***** " );
	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	OWLReasoner reasoner = null;
	OWLOntology ontology = null;

	// Load the ontology 
	try {
	    ontology = manager.loadOntology( IRI.create( "file:"+merged.getPath() ) );
	    reasoner = new Reasoner( ontology );
	} catch (OWLOntologyCreationException ooce) {
	    ooce.printStackTrace(); 
	}

	if ( reasoner.isConsistent() ) {
	    System.err.println( "The aligned ontologies are consistent" );
	} else {
	    System.err.println( "The aligned ontologies are inconsistent" );
	}

	System.out.println( " ***** Testing consistency with DRAon ***** " );
	// test consistency of aligned ontologies
	ArrayList<Alignment> allist = new ArrayList<Alignment>();
	allist.add( al );
	IDDLReasoner dreasoner = new IDDLReasoner( allist, Semantics.DL );
	// Try Semantics.IDDL instead!
	if ( dreasoner.isConsistent() ) {
	    System.out.println( "IDDL: the alignment network is consistent");
         } else {
	    System.out.println( "IDDL: the alignment network is inconsistent");
	}

	// repairing with Logmap (uncomment to use logmap - library needed)
	/*
	System.out.println( " ***** Repairing with LogMap ***** " );
	try {
	    OWLOntology onto1 = manager.loadOntology(IRI.create("http://alignapi.gforge.inria.fr/tutorial/tutorial4/ontology1.owl"));
	    OWLOntology onto2 = manager.loadOntology(IRI.create("http://alignapi.gforge.inria.fr/tutorial/tutorial4/ontology2.owl"));
	    MappingsReaderManager readermanager = new MappingsReaderManager( "alignment.rdf", "RDF");
	    Set<MappingObjectStr> input_mappings = readermanager.getMappingObjects();
	    LogMap2_RepairFacility logmap2_repair = new LogMap2_RepairFacility( onto1, onto2, input_mappings, false, false);
	    //Set of mappings repaired by LogMap
	    Set<MappingObjectStr> repaired_mappings = logmap2_repair.getCleanMappings();
	    System.out.println("Num repaired mappings using LogMap: " + repaired_mappings.size());
	} catch ( Exception ex ) {
	    ex.printStackTrace();
	}
	*/

	// repairing with Alcomo (uncomment to use alcomo - library needed)
	/*
	System.out.println( " ***** Repairing with Alcomo ***** " );
	try {
	    Settings.BLACKBOX_REASONER = Settings.BlackBoxReasoner.HERMIT;
	    IOntology sourceOnt = new IOntology( "ontology1.owl" );
	    IOntology targetOnt = new IOntology( "ontology2.owl" );
	    // load the alignment that has been generated by a matcher
	    Mapping mapping = new Mapping( "alignment.rdf");
	    
	    // Find the optimal repair
	    ExtractionProblem ep = new ExtractionProblem(ExtractionProblem.ENTITIES_CONCEPTSPROPERTIES,
							 ExtractionProblem.METHOD_OPTIMAL,
							 ExtractionProblem.REASONING_COMPLETE // or EFFICIENT, if reasoning does not need to be complete
							 );
	    ep.bindSourceOntology( sourceOnt );
	    ep.bindTargetOntology( targetOnt );
	    ep.bindMapping( mapping );
	    ep.init();
	    ep.solve();
	    System.out.println("Num discarded correspondences using Alcomo: " + (mapping.size() - ep.getExtractedMapping().size()));
	    MappingWriterXml alcomoWriter = new MappingWriterXml();
	    alcomoWriter.writeMapping( "results/alcomo-alignment.rdf", ep.getExtractedMapping() );
	} catch ( Exception ex ) { ex.printStackTrace(); }
	*/

	// Reload an alignment which should be the one returned by LogMap or Alcomo
	// Neither LogMap nor Alcomo output a correct alignment!
	try {
	    AlignmentParser aparser = new AlignmentParser(0);
	    Alignment alu = aparser.parse( "file:alignment2.rdf" );
	    al = ObjectAlignment.toObjectAlignment((URIAlignment)alu);
	    merged = File.createTempFile( "MyApp-results2",".owl");
	    merged.deleteOnExit();
	    writer = new PrintWriter ( new FileWriter( merged, false ), true );
	    AlignmentVisitor renderer = new OWLAxiomsRendererVisitor( writer );
	    al.render(renderer);
	} catch (UnsupportedEncodingException uee) {
	    uee.printStackTrace();
	} catch (AlignmentException ae) {
	    ae.printStackTrace();
	} catch (IOException ioe) { 
	    ioe.printStackTrace();
	} finally {
	    if ( writer != null ) {
		writer.flush();
		writer.close();
	    }
	}
	System.out.println( " ***** Test consistency and coherence of revised alignment ***** " );
	try {
	    ontology = manager.loadOntology( IRI.create( "file:"+merged.getPath() ) );
	    reasoner = new Reasoner( ontology );
	} catch (OWLOntologyCreationException ooce) {
	    ooce.printStackTrace(); 
	}

	if ( reasoner.isConsistent() ) {
	    System.err.println( "The aligned ontologies are consistent" );
	} else {
	    System.err.println( "The aligned ontologies are inconsistent" );
	}
	// Test coherence
	for ( OWLClass cl : ontology.getClassesInSignature( true ) ) {
	    if ( !reasoner.isSatisfiable( cl ) ) {
		System.out.println( cl+" is incoherent" );
	    }
	}

	// test consistency with DRAon
	allist = new ArrayList<Alignment>();
	allist.add( al );
	dreasoner = new IDDLReasoner( allist, Semantics.DL );
	// Try Semantics.IDDL instead!
	if ( dreasoner.isConsistent() ) {
	    System.out.println( "IDDL: the alignment network is consistent");
         } else {
	    System.out.println( "IDDL: the alignment network is inconsistent");
	}

	System.out.println( " ***** Test subsumption with HermiT ***** " );
	// get the instances of a class
	OWLClass estud = manager.getOWLDataFactory().getOWLClass( IRI.create( "http://alignapi.gforge.inria.fr/tutorial/tutorial4/ontology1.owl#Estudiante" ) );   
	OWLClass person = manager.getOWLDataFactory().getOWLClass( IRI.create( "http://alignapi.gforge.inria.fr/tutorial/tutorial4/ontology2.owl#Person" ) );   
	OWLClass student = manager.getOWLDataFactory().getOWLClass( IRI.create( "http://alignapi.gforge.inria.fr/tutorial/tutorial4/ontology2.owl#Student" ) );   

	testOWLReasonerSubClass( manager, reasoner, estud, person );
	testOWLReasonerSubClass( manager, reasoner, estud, student );

	System.out.println( " ***** Testing subsumption with DRAon ***** " );
	testIDDLSubClass( dreasoner, uri1, uri2, estud, person );
	testIDDLSubClass( dreasoner, uri1, uri2, estud, student );

	System.out.println( "\n\n ########## QUERYING ########## " );

	System.out.println( " ***** Simple instance reasoning with HermiT ***** " );

	Set<OWLNamedIndividual> instances  = reasoner.getInstances( estud, false ).getFlattened();
	System.err.println("OWLReasoner(Merged): There are "+instances.size()+" students ("+clname(estud)+")");

	System.out.println( " ***** SPARQL Query answering by transforming query ***** " );
	OntModel model = (OntModel)ModelFactory.createOntologyModel( OntModelSpec.OWL_DL_MEM_RULE_INF, null );
	// Load ontology 1
	//OntModelSpec.OWL_MEM_RDFS_INF or no arguments to see the difference...
	model.read( "file:ontology1.owl" );
	// Query in ontology 1
	displayQueryAnswer( model, QueryFactory.read( "file:query.sparql" ) );
	// Load ontology 2
	model = (OntModel)ModelFactory.createOntologyModel( OntModelSpec.OWL_DL_MEM_RULE_INF, null );
	model.read( "file:ontology2.owl" );
	// Transform query
	String transformedQuery = null;
	try {
	    InputStream in = new FileInputStream( "query.sparql" );
	    BufferedReader reader = new BufferedReader( new InputStreamReader(in) );
	    String line = null;
	    String queryString = "";
	    while ((line = reader.readLine()) != null) {
		queryString += line + "\n";
	    }
	    Properties parameters = new Properties();
	    transformedQuery = ((BasicAlignment)al).rewriteQuery( queryString, parameters );
	} catch ( Exception ex ) { ex.printStackTrace(); }
	// Query ontology 2
	displayQueryAnswer( model, QueryFactory.create( transformedQuery ) );

	System.out.println( " ***** SPARQL Query answering in the merged ontology ***** " );
	//	model = (OntModel)ModelFactory.createOntologyModel( OntModelSpec.OWL_DL_MEM_RULE_INF, null );
	//model.read( "file:"+merged.getPath() );
	//model.loadImports();
	try {
	merged = new File( "results/alignment.rdf" );
            InputStream in = new FileInputStream( merged );
            //OntModelSpec.OWL_MEM_RDFS_INF or no arguments to see the difference...
            model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RDFS_INF,null);
            model.read( in, "file:"+merged.getPath() );
            in.close();
	    model.loadImports();

	/*
	    // Not better
	    InputStream in = com.hp.hpl.jena.util.FileManager.get().open( merged.getPath() );
	    model = ModelFactory.createOntologyModel( OntModelSpec.OWL_DL_MEM_RULE_INF, null );
	    OntDocumentManager odm = model.getDocumentManager();
	    FileManager fm = odm.getFileManager();
	    model.read( in, "file:"+merged.getPath() );
	    model.loadImports();

	    // Neither
	    odm = new OntDocumentManager();
	    OntModelSpec s = new OntModelSpec( OntModelSpec.OWL_DL_MEM_RULE_INF );
	    s.setDocumentManager( odm );
	    odm.setProcessImports( true );
	    model = ModelFactory.createOntologyModel( s );
	    model.read( FileManager.get().open( merged.getPath() ), "file:"+merged.getPath() );
	    System.out.println( "NB Modles= "+model.countSubModels()+" ~~ "+model.hasLoadedImport( "http://alignapi.gforge.inria.fr/tutorial/tutorial4/ontology1.owl" ));
	    in.close();
	    model.loadImports();
	*/

	    OntClass cl = model.getOntClass( "http://alignapi.gforge.inria.fr/tutorial/tutorial4/ontology1.owl#Estudiante" ) ;
	    if ( cl != null ) {
		System.err.println( "Class found" );
		com.hp.hpl.jena.util.iterator.ExtendedIterator<? extends com.hp.hpl.jena.ontology.OntResource> it = cl.listInstances();
		int i = 0;
		while ( it.hasNext() ) { i++; it.next(); }
		System.err.println( " It has "+i+" instances" );
	    } else {
		System.err.println( "Cannot find class" );
	    }
	} catch (FileNotFoundException fnfe) {
	    fnfe.printStackTrace();
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	}

	displayQueryAnswer( model, QueryFactory.read( "file:query.sparql" ) );

	System.out.println( " ***** Import data across ontologies ***** " );
	System.out.println( " Not yet ready " );

    }

    public void displayQueryAnswer( Model model, Query query ) {
	// Execute the query and obtain results
	QueryExecution qe = QueryExecutionFactory.create( query, model );
	ResultSet results = qe.execSelect();
	// Output query results	
	ResultSetFormatter.out(System.out, results, query);
	if ( qe != null ) qe.close();
    }

    private String clname( OWLClassExpression cl ) {
	return cl.asOWLClass().getIRI().getFragment();
    }

    public void testOWLReasonerSubClass( OWLOntologyManager manager, OWLReasoner reasoner, OWLClassExpression d1, OWLClassExpression d2 ) {
	OWLAxiom axiom = manager.getOWLDataFactory().getOWLSubClassOfAxiom( d1, d2 );
	if ( reasoner.isEntailed( axiom ) ) {
	    System.out.println( "OWLReasoner(Merged): "+clname(d1)+" is subclass of "+clname(d2) );
	} else {
	    System.out.println( "OWLReasoner(Merged): "+clname(d1)+" is not necessarily subclass of "+clname(d2) );
	}
    }

    public void testIDDLSubClass( IDDLReasoner dreasoner, URI onto1, URI onto2, OWLClassExpression d1, OWLClassExpression d2 ) throws IDDLException {
	Alignment al2 = new ObjectAlignment();
	try {
	    al2.init( onto1, onto2 );
	    // add the cell
	    al2.addAlignCell( d1, d2, "&lt;", 1. );
	} catch (AlignmentException ae) { ae.printStackTrace(); }
	if ( dreasoner.isEntailed( al2 ) ) {
	    System.out.println( "IDDL: "+clname(d1)+" <= "+clname(d2)+" is entailed" );
	} else {
	    System.out.println( "IDDL: "+clname(d1)+" <= "+clname(d2)+" is not entailed" );
	}
    }

    public String getFromURLString( String u, boolean print ){
	URL url = null;
	String result = "<?xml version='1.0'?>";
	try {
	    url = new URL( u );
	    BufferedReader in = new BufferedReader(
    				new InputStreamReader(
    				  url.openStream()));
	    String inputLine;
	    while ((inputLine = in.readLine()) != null) {
		if (print) System.out.println(inputLine);
		result += inputLine;
	    }
	    in.close();
	}
	catch ( MalformedURLException mue ) { mue.printStackTrace(); }
	catch ( IOException mue ) { mue.printStackTrace(); }
	return result;
    }

    public NodeList extractFromResult( String found, String path, boolean print ){
	Document document = null;
	NodeList nodes = null;
	try { // Parse the returned stringAS XML
	    DocumentBuilder parser =
		DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    document = parser.parse(new ByteArrayInputStream( found.getBytes() ));
	} catch ( ParserConfigurationException pce ) { pce.printStackTrace(); }
	catch ( SAXException se ) { se.printStackTrace(); }
	catch ( IOException ioe ) { ioe.printStackTrace(); }

	try { // Apply the Xpath expression
	    XPathFactory factory = XPathFactory.newInstance();
	    XPath xpath = factory.newXPath();
	    //XPathExpression expr = xpath.compile("//book[author='Neal Stephenson']/title/text()");
	    XPathExpression expr = xpath.compile( path );
	    Object result = expr.evaluate( document, XPathConstants.NODESET );
	    nodes = (NodeList)result;
	    if ( print ) {
		for ( int i = 0; i < nodes.getLength(); i++) {
		    System.out.println(nodes.item(i).getNodeValue()); 
		}
	    }
	} catch (XPathExpressionException xpee) { xpee.printStackTrace(); }
	return nodes;
    }
}
