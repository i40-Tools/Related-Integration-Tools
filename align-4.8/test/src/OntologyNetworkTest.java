/*
 * $Id: OntologyNetworkTest.java 2092 2015-11-20 12:03:10Z euzenat $
 *
 * Copyright (C) INRIA, 2008-2011, 2013-2015
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 */

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.Properties;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.impl.Annotations;
import fr.inrialpes.exmo.align.impl.BasicOntologyNetwork;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;

import fr.inrialpes.exmo.align.parser.AlignmentParser;

import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.OntologyNetwork;

import fr.inrialpes.exmo.ontowrap.Ontology;
import fr.inrialpes.exmo.ontowrap.OntologyFactory;

import fr.inrialpes.exmo.align.gen.OntologyNetworkWeakener;
import fr.inrialpes.exmo.align.gen.NetworkAlignmentDropper;
import fr.inrialpes.exmo.align.gen.NetworkCorrespondenceDropper;
import fr.inrialpes.exmo.align.gen.NetworkAlignmentWeakener;
import fr.inrialpes.exmo.align.gen.NetworkDeconnector;

/***
 * Tests for networks of ontologies
 * 
 * closure and invert tests will be found in AlgTest
 */

public class OntologyNetworkTest {
    private OntologyNetwork noo = null;

    @BeforeClass(groups = { "full", "raw" })
    private void init(){
	noo = new BasicOntologyNetwork();
    }

    @Test(groups = { "full", "raw" })
    public void aFastTest() {
	assertNotNull( noo, "Alignment was null" );
    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"aFastTest"})
	public void ontologyTest() throws URISyntaxException, AlignmentException {
	assertEquals( noo.getOntologies().size(), 0 );
	// Load
	URI u1 = new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl");
	URI u2 = new URI("file:examples/rdf/edu.mit.visus.bibtex.owl");
	// addOntology
	noo.addOntology( u1 );
	assertEquals( noo.getOntologies().size(), 1 );
	// addOntology
	noo.addOntology( u2 );
	assertEquals( noo.getOntologies().size(), 2 );
	noo.addOntology( u2 );
	assertEquals( noo.getOntologies().size(), 2 );
	// remOntology
	noo.remOntology( u1 );
	assertEquals( noo.getOntologies().size(), 1 );
	noo.addOntology( u1 );
	assertEquals( noo.getOntologies().size(), 2);
    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"ontologyTest"})
	public void alignmentTest() throws ParserConfigurationException, SAXException, IOException, URISyntaxException, AlignmentException {
	//, RDFParserException
	assertEquals( noo.getAlignments().size(), 0 );
	assertEquals( noo.getOntologies().size(), 2);
	// addAlignment
	Alignment al1 = new AlignmentParser( 0 ).parse( "file:examples/rdf/newsample.rdf" );
	noo.addAlignment( al1 );
	assertEquals( noo.getOntologies().size(), 4);
	// addAlignment
	Alignment al2 = new URIAlignment();
	al2.init( al1.getOntology1URI(), al1.getOntology2URI() );
	noo.addAlignment( al2 );
	assertEquals( noo.getAlignments().size(), 2 );
	assertEquals( noo.getOntologies().size(), 4);
	noo.addAlignment( al2 );
	assertEquals( noo.getAlignments().size(), 2 );
	assertEquals( noo.getOntologies().size(), 4);
	// remAlignment
	noo.remAlignment( al1 );
	assertEquals( noo.getAlignments().size(), 1 );
	// addAlignment
	noo.addAlignment( al1 );
	assertEquals( noo.getAlignments().size(), 2 );
	// impact on ontologies?
    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"ontologyTest","alignmentTest"})
    public void lambdaTest() throws URISyntaxException {
	URI u = new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl");
	assertEquals( noo.getTargetingAlignments(u).size(), 0 );
	assertEquals( noo.getSourceAlignments(u).size(), 0 );
	u = new URI("file:examples/rdf/edu.mit.visus.bibtex.owl");
	assertEquals( noo.getTargetingAlignments(u).size(), 0 );
	assertEquals( noo.getSourceAlignments(u).size(), 0 );
	u = new URI("http://www.example.org/ontology1");
	assertEquals( noo.getTargetingAlignments(u).size(), 0 );
	assertEquals( noo.getSourceAlignments(u).size(), 2 );
	u = new URI("http://www.example.org/ontology2");
	assertEquals( noo.getTargetingAlignments(u).size(), 2 );
	assertEquals( noo.getSourceAlignments(u).size(), 0 );
    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"lambdaTest"})
	public void expandTest() throws URISyntaxException, AlignmentException {

	assertEquals( noo.getOntologies().size(), 4);
	assertEquals( noo.getAlignments().size(), 2);

	Alignment al2 = new URIAlignment();
	al2.init( new URI("http://www.example.org/ontology1"), new URI("file:examples/rdf/edu.mit.visus.bibtex.owl") );
	al2.addAlignCell( new URI("http://www.example.org/ontology1#reviewedarticle"), new URI("http://purl.org/net/nknouf/ns/bibtex#Article"), "<", 1.0 );
	al2.addAlignCell( new URI("http://www.example.org/ontology1#journalarticle"), new URI("http://purl.org/net/nknouf/ns/bibtex#Article"), "<", 1.0 );
	al2.addAlignCell( new URI("http://www.example.org/ontology1#journalarticle"), new URI("http://purl.org/net/nknouf/ns/bibtex#Article"), "=", .5 );
	assertEquals( al2.nbCells(), 3);
	noo.addAlignment( al2 );

	al2 = new URIAlignment();
	al2.init( new URI("http://www.example.org/ontology2"), new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl") );
	al2.addAlignCell( new URI("http://www.example.org/ontology2#journalarticle"), new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#Article"), "<", .78 );
	al2.addAlignCell( new URI("http://www.example.org/ontology2#article"), new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#Article"), "=", 1.0 );
	al2.addAlignCell( new URI("http://www.example.org/ontology2#reference"), new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#Publication"), "=", .6 );
	assertEquals( al2.nbCells(), 3);
	noo.addAlignment( al2 );

	al2 = new URIAlignment();
	al2.init( new URI("file:examples/rdf/edu.mit.visus.bibtex.owl"), new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl") );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasPublisher"), new URI("http://www.example.org/ontology1#publisher"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasURL"), new URI("http://www.example.org/ontology1#softCopyURI"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#Inproceedings"), new URI("http://www.example.org/ontology1#InProceedings"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasEditor"), new URI("http://www.example.org/ontology1#editor"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasNumber"), new URI("http://www.example.org/ontology1#number"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasNote"), new URI("http://www.example.org/ontology1#note"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasType"), new URI("http://www.example.org/ontology1#type"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasBooktitle"), new URI("http://www.example.org/ontology1#booktitle"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasJournal"), new URI("http://www.example.org/ontology1#journal"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasKeywords"), new URI("http://www.example.org/ontology1#keyword"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#Article"), new URI("http://www.example.org/ontology1#Article"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasChapter"), new URI("http://www.example.org/ontology1#chapter"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#Techreport"), new URI("http://www.example.org/ontology1#TechReport"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasSchool"), new URI("http://www.example.org/ontology1#school"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#Misc"), new URI("http://www.example.org/ontology1#Misc"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasPages"), new URI("http://www.example.org/ontology1#pages"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#Phdthesis"), new URI("http://www.example.org/ontology1#PhdThesis"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#Mastersthesis"), new URI("http://www.example.org/ontology1#MastersThesis"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#Incollection"), new URI("http://www.example.org/ontology1#InCollection"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasAbstract"), new URI("http://www.example.org/ontology1#abstract"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasAddress"), new URI("http://www.example.org/ontology1#address"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#Book"), new URI("http://www.example.org/ontology1#Book"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasSeries"), new URI("http://www.example.org/ontology1#series"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasTitle"), new URI("http://www.example.org/ontology1#title"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#Inbook"), new URI("http://www.example.org/ontology1#InBook"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasInstitution"), new URI("http://www.example.org/ontology1#institution"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasEdition"), new URI("http://www.example.org/ontology1#edition"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasVolume"), new URI("http://www.example.org/ontology1#volume"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#Proceedings"), new URI("http://www.example.org/ontology1#Proceedings"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasSize"), new URI("http://www.example.org/ontology1#softCopySize"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasAuthor"), new URI("http://www.example.org/ontology1#author"), "=", 1.0 );
	al2.addAlignCell( new URI("http://purl.org/net/nknouf/ns/bibtex#hasOrganization"), new URI("http://www.example.org/ontology1#organization"), "=", 1.0 );
	assertEquals( al2.nbCells(), 32);
	noo.addAlignment( al2 );

	al2 = new URIAlignment();
	al2.init( new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl"), new URI("file:examples/rdf/edu.mit.visus.bibtex.owl") );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#Article"), new URI( "http://purl.org/net/nknouf/ns/bibtex#Article"), "=", 1.0 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#volume"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasVolume"), "=", 0.8 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#note"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasNote"), "=", 0.73 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#type"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasType"), "=", 0.73 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#address"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasAddress"), "=", 0.82 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#TechReport"), new URI( "http://purl.org/net/nknouf/ns/bibtex#Techreport"), "=", 1.0 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#SoftCopy"), new URI( "http://purl.org/net/nknouf/ns/bibtex#Conference"), "=", 0.22 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#InBook"), new URI( "http://purl.org/net/nknouf/ns/bibtex#Inbook"), "=", 1.0 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#chapter"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasChapter"), "=", 0.82 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#series"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasSeries"), "=", 0.8 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#author"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasAuthor"), "=", 0.8 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#Misc"), new URI( "http://purl.org/net/nknouf/ns/bibtex#Misc"), "=", 1.0 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#Book"), new URI( "http://purl.org/net/nknouf/ns/bibtex#Book"), "=", 1.0 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#publishedOn"), new URI( "http://purl.org/net/nknouf/ns/bibtex#howPublished"), "=", 0.78 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#firstAuthor"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasAuthor"), "=", 0.6 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#InCollection"), new URI( "http://purl.org/net/nknouf/ns/bibtex#Incollection"), "=", 1.0 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#Resource"), new URI( "http://purl.org/net/nknouf/ns/bibtex#Phdthesis"), "=", 0.23 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#publisher"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasPublisher"), "=", 0.86 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#school"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasSchool"), "=", 0.8 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#softCopy"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasCopyright"), "=", 0.4 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#version"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasEdition"), "=", 0.35 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#softCopyURI"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasCopyright"), "=", 0.35 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#description"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasEdition"), "=", 0.38 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#MastersThesis"), new URI( "http://purl.org/net/nknouf/ns/bibtex#Mastersthesis"), "=", 1.0 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#Proceedings"), new URI( "http://purl.org/net/nknouf/ns/bibtex#Proceedings"), "=", 1.0 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#PhdThesis"), new URI( "http://purl.org/net/nknouf/ns/bibtex#Phdthesis"), "=", 1.0 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#institution"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasInstitution"), "=", 0.88 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#number"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasNumber"), "=", 0.8 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#keyword"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasKeywords"), "=", 0.78 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#softCopyFormat"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasCopyright"), "=", 0.31 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#title"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasTitle"), "=", 0.77 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#journal"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasJournal"), "=", 0.82 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/person.owl#Person"), new URI( "http://purl.org/net/nknouf/ns/bibtex#Mastersthesis"), "=", 0.32 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#InProceedings"), new URI( "http://purl.org/net/nknouf/ns/bibtex#Inproceedings"), "=", 1.0 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#edition"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasEdition"), "=", 0.82 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#abstract"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasAbstract"), "=", 0.84 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#editor"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasEditor"), "=", 0.8 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#counter"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasChapter"), "=", 0.35 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#softCopySize"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasSize"), "=", 0.42 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#organization"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasOrganization"), "=", 0.89 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#pages"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasPages"), "=", 0.77 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#booktitle"), new URI( "http://purl.org/net/nknouf/ns/bibtex#hasBooktitle"), "=", 0.86 );
	al2.addAlignCell( new URI("http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#Publication"), new URI( "http://purl.org/net/nknouf/ns/bibtex#Unpublished"), "=", 0.45 );
	assertEquals( al2.nbCells(), 43);
	noo.addAlignment( al2 );

	assertEquals( noo.getOntologies().size(), 4);
	assertEquals( noo.getAlignments().size(), 6);

	URI u = new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl");
	assertEquals( noo.getTargetingAlignments(u).size(), 2 );
	assertEquals( noo.getSourceAlignments(u).size(), 1 );
	u = new URI("file:examples/rdf/edu.mit.visus.bibtex.owl");
	assertEquals( noo.getTargetingAlignments(u).size(), 2 );
	assertEquals( noo.getSourceAlignments(u).size(), 1 );
	u = new URI("http://www.example.org/ontology1");
	assertEquals( noo.getTargetingAlignments(u).size(), 0 );
	assertEquals( noo.getSourceAlignments(u).size(), 3 );
	u = new URI("http://www.example.org/ontology2");
	assertEquals( noo.getTargetingAlignments(u).size(), 2 );
	assertEquals( noo.getSourceAlignments(u).size(), 1 );
    }

    // Then I would apply the weakening
    @Test(groups = { "full", "raw" }, dependsOnMethods = {"expandTest"})
	public void weakenAlignmentDropper() throws URISyntaxException, AlignmentException {
	OntologyNetwork noon = null;
	OntologyNetworkWeakener weakener = new NetworkAlignmentDropper();
	Properties propThres = new Properties();
	noon = weakener.weaken( noo, 1., propThres );
	Set<Alignment> s = noon.getTargetingAlignments(new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl"));
	assertEquals( noon.getAlignments().size(), 0);
	noon = weakener.weaken( noo, .5, propThres );
	assertEquals( noon.getAlignments().size(), 3);
	noon = weakener.weaken( noo, 0., propThres );
	assertEquals( noon.getAlignments().size(), 6);
	noon = weakener.weaken( noo, 1., (Properties)null );
	assertEquals( noon.getAlignments().size(), 0);
	noon = weakener.weaken( noo, .5, (Properties)null );
	assertEquals( noon.getAlignments().size(), 3);
	noon = weakener.weaken( noo, 0., (Properties)null );
	assertEquals( noon.getAlignments().size(), 6);
    }

    //@Test(groups = { "full", "raw" }, dependsOnMethods = {"expandTest"})
    //	public void weakenNetworkDeconnector() throws URISyntaxException, AlignmentException {
    //}

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"expandTest"})
	public void weakenCorrespondenceDropper() throws URISyntaxException, AlignmentException {
	OntologyNetwork noon = null;
	OntologyNetworkWeakener weakener = new NetworkCorrespondenceDropper();
	Properties propThres = new Properties();
	propThres.setProperty( "threshold", "true" );

	int init = 0;
	for ( Alignment al : noo.getAlignments() ) { init += al.nbCells(); }

	int size = 0;
	noon = weakener.weaken( noo, 1., propThres );
	for ( Alignment al : noon.getAlignments() ) { size += al.nbCells(); }
	assertEquals( size, 0);
	noon = weakener.weaken( noo, .5, propThres );
	size = 0;
	for ( Alignment al : noon.getAlignments() ) { size += al.nbCells(); }
	assertTrue( init/2 <= size && size <= init/2 + 1, "Expected "+init/2+" but got "+size );
	noon = weakener.weaken( noo, 0., propThres );
	size = 0;
	for ( Alignment al : noon.getAlignments() ) { size += al.nbCells(); }
	assertEquals( size, init);
	noon = weakener.weaken( noo, 1., (Properties)null );
	size = 0;
	for ( Alignment al : noon.getAlignments() ) { size += al.nbCells(); }
	assertEquals( size, 0);
	noon = weakener.weaken( noo, .5, (Properties)null );
	size = 0;
	for ( Alignment al : noon.getAlignments() ) { size += al.nbCells(); }
	assertTrue( init/2 <= size && size <= init/2 + 1, "Expected "+init/2+" but got "+size );
	noon = weakener.weaken( noo, 0., (Properties)null );
	size = 0;
	for ( Alignment al : noon.getAlignments() ) { size += al.nbCells(); }
	assertEquals( size, init);
    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"expandTest"})
	public void weakenAlignmentWeakener() throws URISyntaxException, AlignmentException {
	OntologyNetwork noon = null;
	OntologyNetworkWeakener weakener = new NetworkAlignmentWeakener();
	Properties propThres = new Properties();
	int init = noo.getSourceAlignments(new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl")).iterator().next().nbCells();

	propThres.setProperty( "threshold", "true" );
	noon = weakener.weaken( noo, 1., propThres );
	assertEquals( noon.getSourceAlignments(new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl")).iterator().next().nbCells(), 0);
	noon = weakener.weaken( noo, .5, propThres );
	int val = noon.getSourceAlignments(new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl")).iterator().next().nbCells();
	assertTrue( init/2 <= val && val <= init/2+1, "Expected "+init/2+" but got "+val );
	noon = weakener.weaken( noo, 0., propThres );
	assertEquals( noon.getSourceAlignments(new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl")).iterator().next().nbCells(), init);
	noon = weakener.weaken( noo, 1., (Properties)null );
	assertEquals( noon.getSourceAlignments(new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl")).iterator().next().nbCells(), 0);
	noon = weakener.weaken( noo, .5, (Properties)null );
	val = noon.getSourceAlignments(new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl")).iterator().next().nbCells();
	assertTrue( init/2 <= val && val <= init/2+1, "Expected "+init/2+" but got "+val );
	noon = weakener.weaken( noo, 0., (Properties)null );
	assertEquals( noon.getSourceAlignments(new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl")).iterator().next().nbCells(), init);
    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"lambdaTest"},expectedExceptions = AlignmentException.class)
    public void weakenNADExceptionTest10() throws URISyntaxException, AlignmentException {
	new NetworkAlignmentDropper().weaken( (OntologyNetwork)null, .5, (Properties)null );
    }
    @Test(groups = { "full", "raw" }, dependsOnMethods = {"lambdaTest"},expectedExceptions = AlignmentException.class)
    public void weakenNADExceptionTest11() throws URISyntaxException, AlignmentException {
	new NetworkAlignmentDropper().weaken( noo, 1.2, (Properties)null );
    }
    @Test(groups = { "full", "raw" }, dependsOnMethods = {"lambdaTest"},expectedExceptions = AlignmentException.class)
    public void weakenNADExceptionTest12() throws URISyntaxException, AlignmentException {
	new NetworkAlignmentDropper().weaken( noo, -.2, (Properties)null );
    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"lambdaTest"},expectedExceptions = AlignmentException.class)
    public void weakenNCDExceptionTest10() throws URISyntaxException, AlignmentException {
	new NetworkCorrespondenceDropper().weaken( (OntologyNetwork)null, .5, (Properties)null );
    }
    @Test(groups = { "full", "raw" }, dependsOnMethods = {"lambdaTest"},expectedExceptions = AlignmentException.class)
    public void weakenNCDExceptionTest11() throws URISyntaxException, AlignmentException {
	new NetworkCorrespondenceDropper().weaken( noo, 1.2, (Properties)null );
    }
    @Test(groups = { "full", "raw" }, dependsOnMethods = {"lambdaTest"},expectedExceptions = AlignmentException.class)
    public void weakenNCDExceptionTest12() throws URISyntaxException, AlignmentException {
	new NetworkCorrespondenceDropper().weaken( noo, -.2, (Properties)null );
    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"lambdaTest"},expectedExceptions = AlignmentException.class)
    public void weakenNAWExceptionTest10() throws URISyntaxException, AlignmentException {
	new NetworkAlignmentWeakener().weaken( (OntologyNetwork)null, .5, (Properties)null );
    }
    @Test(groups = { "full", "raw" }, dependsOnMethods = {"lambdaTest"},expectedExceptions = AlignmentException.class)
    public void weakenNAWExceptionTest11() throws URISyntaxException, AlignmentException {
	new NetworkAlignmentWeakener().weaken( noo, 1.2, (Properties)null );
    }
    @Test(groups = { "full", "raw" }, dependsOnMethods = {"lambdaTest"},expectedExceptions = AlignmentException.class)
    public void weakenNAWExceptionTest12() throws URISyntaxException, AlignmentException {
	new NetworkAlignmentWeakener().weaken( noo, -.2, (Properties)null );
    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"lambdaTest"},expectedExceptions = AlignmentException.class)
    public void weakenNDExceptionTest10() throws URISyntaxException, AlignmentException {
	new NetworkDeconnector().weaken( (OntologyNetwork)null, .5, (Properties)null );
    }
    @Test(groups = { "full", "raw" }, dependsOnMethods = {"lambdaTest"},expectedExceptions = AlignmentException.class)
    public void weakenNDExceptionTest11() throws URISyntaxException, AlignmentException {
	new NetworkDeconnector().weaken( noo, 1.2, (Properties)null );
    }
    @Test(groups = { "full", "raw" }, dependsOnMethods = {"lambdaTest"},expectedExceptions = AlignmentException.class)
    public void weakenNDExceptionTest12() throws URISyntaxException, AlignmentException {
	new NetworkDeconnector().weaken( noo, -.2, (Properties)null );
    }

    /**
     * ALGEBRAIC OPERATION TESTS
     * - prepare two networks
     * - meet/diff/join
     */

    private BasicOntologyNetwork noo1;
    private BasicOntologyNetwork noo2;

    @Test(groups = { "full", "raw" })
    public void initTwoNetworkTest() throws URISyntaxException, AlignmentException {
	assertEquals( noo.getOntologies().size(), 0 );
	// Create networks
	noo1 = new BasicOntologyNetwork();
	noo2 = new BasicOntologyNetwork();
	// Create ontologies
	URI uA = new URI("http://example.org/ontoA");
	URI uB = new URI("http://example.org/ontoB");
	URI uC = new URI("http://example.org/ontoC");
	URI uD = new URI("http://example.org/ontoD");
	// addOntology
	noo1.addOntology( uA );
	noo1.addOntology( uB );
	noo1.addOntology( uC );
	noo2.addOntology( uA );
	noo2.addOntology( uC );
	noo2.addOntology( uD );
	// Here I must do a test with what happens with empty alignements!
	//OntologyNetwork onmeet = BasicOntologyNetwork.meet( noo1, noo2 );
	//assertEquals( onmeet.getOntologies().size(), 2 );
	//OntologyNetwork ondiff = BasicOntologyNetwork.diff( noo1, noo2 );
	//assertEquals( ondiff.getOntologies().size(), 3 );
	//OntologyNetwork onjoin = BasicOntologyNetwork.join( noo1, noo2 );
	//assertEquals( onjoin.getOntologies().size(), 4 );
	// createAlignments
	Alignment alAB1 = new URIAlignment();
	alAB1.init( uA, uB );
	Alignment alAC1 = new URIAlignment();
	alAC1.init( uA, uC );
	Alignment alBC1 = new URIAlignment();
	alBC1.init( uB, uC );
	Alignment alAC2 = new URIAlignment();
	alAC2.init( uA, uC );
	Alignment alAD2 = new URIAlignment();
	alAD2.init( uA, uD );
	Alignment alCD2 = new URIAlignment();
	alCD2.init( uC, uD );
	// add cells (all =, 1.0)
	alAB1.addAlignCell( new URI("http://example.org/ontoA#c1"), new URI("http://example.org/ontoB#c1") );
	alAB1.addAlignCell( new URI("http://example.org/ontoA#c2"), new URI("http://example.org/ontoB#c2") );
	alAC1.addAlignCell( new URI("http://example.org/ontoA#cx"), new URI("http://example.org/ontoC#cx") );
	alAC1.addAlignCell( new URI("http://example.org/ontoA#cy"), new URI("http://example.org/ontoC#cy") );
	alBC1.addAlignCell( new URI("http://example.org/ontoB#c1"), new URI("http://example.org/ontoC#c1") );
	alBC1.addAlignCell( new URI("http://example.org/ontoB#c2"), new URI("http://example.org/ontoC#c2") );
	alAC2.addAlignCell( new URI("http://example.org/ontoA#cy"), new URI("http://example.org/ontoC#cy") );
	alAC2.addAlignCell( new URI("http://example.org/ontoA#cz"), new URI("http://example.org/ontoC#cz") );
	alAD2.addAlignCell( new URI("http://example.org/ontoA#c1"), new URI("http://example.org/ontoD#c1") );
	alAD2.addAlignCell( new URI("http://example.org/ontoA#c2"), new URI("http://example.org/ontoD#c2") );
	alCD2.addAlignCell( new URI("http://example.org/ontoD#c1"), new URI("http://example.org/ontoC#c1") );
	alCD2.addAlignCell( new URI("http://example.org/ontoD#c2"), new URI("http://example.org/ontoC#c2") );
	// add alignments
	noo1.addAlignment( alAB1 );
	noo1.addAlignment( alAC1 );
	noo1.addAlignment( alBC1 );
	noo2.addAlignment( alAC2 );
	noo2.addAlignment( alAD2 );
	noo2.addAlignment( alCD2 );
	// tests
	assertEquals( noo1.getAlignments( uA, uC ).size(), 1 );
	assertEquals( noo2.getAlignments( uA, uC ).size(), 1 );
	assertEquals( noo1.getAlignments( new URI("http://example.org/ontoA"), new URI("http://example.org/ontoC") ).size(), 1 );
	assertEquals( noo1.getAlignments( new URI("http://example.org/ontoA"), new URI("http://example.org/ontoC") ).iterator().next().nbCells(), 2 );
	assertEquals( noo2.getAlignments( new URI("http://example.org/ontoA"), new URI("http://example.org/ontoC") ).size(), 1 );
	assertEquals( noo1.getAlignments( uC, uA ).size(), 0 );
	assertEquals( noo1.getAlignments( uC, uC ).size(), 0 );
	assertEquals( noo2.getAlignments( uC, uA ).size(), 0 );
	assertEquals( noo1.getAlignments( new URI("http://example.org/ontoC"), new URI("http://example.org/ontoA") ).size(), 0 );
	assertEquals( noo1.getAlignments( new URI("http://example.org/ontoC"), new URI("http://example.org/ontoC") ).size(), 0 );
	assertEquals( noo2.getAlignments( new URI("http://example.org/ontoC"), new URI("http://example.org/ontoA") ).size(), 0 );

    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"initTwoNetworkTest"})
    public void normalizeNetworkTest() throws URISyntaxException, AlignmentException {
	BasicOntologyNetwork on = noo1.clone();
	on.normalize();
	assertEquals( on.getOntologies().size(), 3 );
	assertEquals( on.getAlignments( new URI("http://example.org/ontoA"), new URI("http://example.org/ontoC") ).size(), 1 );
	assertEquals( on.getAlignments( new URI("http://example.org/ontoC"), new URI("http://example.org/ontoA") ).size(), 1 );
	assertEquals( on.getAlignments( new URI("http://example.org/ontoC"), new URI("http://example.org/ontoC") ).size(), 1 );
	assertEquals( on.getAlignments( new URI("http://example.org/ontoA"), new URI("http://example.org/ontoC") ).iterator().next().nbCells(), 2 );
    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"initTwoNetworkTest"})
    public void meetNetworkTest() throws URISyntaxException, AlignmentException {
	OntologyNetwork onmeet = BasicOntologyNetwork.meet( noo1, noo2 );
	assertEquals( onmeet.getOntologies().size(), 2 );
	Set<Alignment> sal = onmeet.getAlignments( new URI("http://example.org/ontoA"), new URI("http://example.org/ontoC") );
	assertEquals( sal.size(), 1 );
	Alignment al = sal.iterator().next();
	assertEquals( al.nbCells(), 1 );
	// check that this is the one between y and y
	Cell c = al.iterator().next();
	assertEquals( (URI)c.getObject1(), new URI( "http://example.org/ontoA#cy" ) );
	assertEquals( (URI)c.getObject2(), new URI( "http://example.org/ontoC#cy" ) );
    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"initTwoNetworkTest"})
    public void diffNetworkTest() throws URISyntaxException, AlignmentException {
	OntologyNetwork ondiff = BasicOntologyNetwork.diff( noo1, noo2 );
	assertEquals( ondiff.getOntologies().size(), 3 );
	Set<Alignment> sal = ondiff.getAlignments( new URI("http://example.org/ontoA"), new URI("http://example.org/ontoC") );
	assertEquals( sal.size(), 1 );
	Alignment al = sal.iterator().next();
	assertEquals( al.nbCells(), 1 );
	// check that this is the one between x and x
	Cell c = al.iterator().next();
	assertEquals( (URI)c.getObject1(), new URI( "http://example.org/ontoA#cx" ) );
	assertEquals( (URI)c.getObject2(), new URI( "http://example.org/ontoC#cx" ) );

	ondiff = BasicOntologyNetwork.diff( noo2, noo1 );
	assertEquals( ondiff.getOntologies().size(), 3 );
	sal = ondiff.getAlignments( new URI("http://example.org/ontoA"), new URI("http://example.org/ontoC") );
	assertEquals( sal.size(), 1 );
	al = sal.iterator().next();
	assertEquals( al.nbCells(), 1 );
	// check that this is the one between z and z
	c = al.iterator().next();
	assertEquals( (URI)c.getObject1(), new URI( "http://example.org/ontoA#cz" ) );
	assertEquals( (URI)c.getObject2(), new URI( "http://example.org/ontoC#cz" ) );
    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"initTwoNetworkTest"})
    public void joinNetworkTest() throws URISyntaxException, AlignmentException {
	OntologyNetwork onjoin = BasicOntologyNetwork.join( noo1, noo2 );
	assertEquals( onjoin.getOntologies().size(), 4 );
	// Here I even make the test on the converse!
	Set<Alignment> sal = onjoin.getAlignments( new URI("http://example.org/ontoC"), new URI("http://example.org/ontoA") );
	assertEquals( sal.size(), 1 );
	Alignment al = sal.iterator().next();
	assertEquals( al.nbCells(), 3 );
	Set<Cell> cs = al.getAlignCells1( new URI( "http://example.org/ontoC#cz" ) );
	assertEquals( cs.size(), 1 );
	Cell c = cs.iterator().next();
	assertEquals( (URI)c.getObject2(), new URI( "http://example.org/ontoA#cz" ) );
	cs = al.getAlignCells1( new URI( "http://example.org/ontoC#cx" ) );
	assertEquals( cs.size(), 1 );
	c = cs.iterator().next();
	assertEquals( (URI)c.getObject2(), new URI( "http://example.org/ontoA#cx" ) );
	cs = al.getAlignCells1( new URI( "http://example.org/ontoC#cy" ) );
	assertEquals( cs.size(), 1 );
	c = cs.iterator().next();
	assertEquals( (URI)c.getObject2(), new URI( "http://example.org/ontoA#cy" ) );
    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"meetNetworkTest", "diffNetworkTest", "joinNetworkTest"})
    public void symCloseNetworkTest() throws URISyntaxException, AlignmentException {
	// First test symmetric closure
	// Beware, closed ness is destructuve
	BasicOntologyNetwork closedon = noo1.clone();
	closedon.close( false, true, false, (Properties)null );
	assertEquals( closedon.getOntologies().size(), 3 );
	// check not lost
	Set<Alignment> sal = closedon.getAlignments( new URI("http://example.org/ontoA"), new URI("http://example.org/ontoC") );
	assertEquals( sal.size(), 1 );
	Alignment al = sal.iterator().next();
	assertEquals( al.nbCells(), 2 );
	// check symmetric
	sal = closedon.getAlignments( new URI("http://example.org/ontoC"), new URI("http://example.org/ontoA") );
	assertEquals( sal.size(), 1 );
	al = sal.iterator().next();
	assertEquals( al.nbCells(), 2 );
	Set<Cell> cs = al.getAlignCells1( new URI( "http://example.org/ontoC#cy" ) );
	assertEquals( cs.size(), 1 );
	Cell c = cs.iterator().next();
	assertEquals( (URI)c.getObject2(), new URI( "http://example.org/ontoA#cy" ) );
	cs = al.getAlignCells1( new URI( "http://example.org/ontoC#c2" ) );
	assertTrue( ( cs == null ) || ( cs.size() == 0 ) );
    }

    // Second test transitive closure
    @Test(groups = { "full", "raw" }, dependsOnMethods = {"symCloseNetworkTest"})
    public void transCloseNetworkTest() throws URISyntaxException, AlignmentException {
	BasicOntologyNetwork closedon = noo1.clone();
	closedon.close( false, false, true, (Properties)null );
	assertEquals( closedon.getOntologies().size(), 3 );
	Set<Alignment> sal = closedon.getAlignments( new URI("http://example.org/ontoA"), new URI("http://example.org/ontoC") );
	assertEquals( sal.size(), 1 );
	Alignment al = sal.iterator().next();
	assertEquals( al.nbCells(), 4 );
	Set<Cell> cs = al.getAlignCells1( new URI( "http://example.org/ontoA#cy" ) );
	assertEquals( cs.size(), 1 );
	Cell c = cs.iterator().next();
	assertEquals( (URI)c.getObject2(), new URI( "http://example.org/ontoC#cy" ) );
	cs = al.getAlignCells1( new URI( "http://example.org/ontoA#c2" ) );
	assertEquals( cs.size(), 1 );
	c = cs.iterator().next();
	assertEquals( (URI)c.getObject2(), new URI( "http://example.org/ontoC#c2" ) );
    }

    // Third test symmetric-transitive closure
    @Test(groups = { "full", "raw" }, dependsOnMethods = {"transCloseNetworkTest"})
    public void symTransCloseNetworkTest() throws URISyntaxException, AlignmentException {
	BasicOntologyNetwork closedon = noo1.clone();
	closedon.close( false, true, true, (Properties)null );
	assertEquals( closedon.getOntologies().size(), 3 );
	// Here I even make the test on the converse!
	Set<Alignment> sal = closedon.getAlignments( new URI("http://example.org/ontoC"), new URI("http://example.org/ontoA") );
	assertEquals( sal.size(), 1 );
	Alignment al = sal.iterator().next();
	assertEquals( al.nbCells(), 4 );
	Set<Cell> cs = al.getAlignCells1( new URI( "http://example.org/ontoC#cy" ) );
	assertEquals( cs.size(), 1 );
	Cell c = cs.iterator().next();
	assertEquals( (URI)c.getObject2(), new URI( "http://example.org/ontoA#cy" ) );
	cs = al.getAlignCells1( new URI( "http://example.org/ontoC#c2" ) );
	assertEquals( cs.size(), 1 );
	c = cs.iterator().next();
	assertEquals( (URI)c.getObject2(), new URI( "http://example.org/ontoA#c2" ) );
    }

    /**
     * HERE WE SHOULD ADD TESTS FOR PRINTER/WRITER
     * - print on
     * - read it
     * - do it with an empty OntologyNetwork first
     */
    @Test(groups = { "full", "raw" }, dependsOnMethods = {"expandTest"})
    public void printEmptyNetworkTest() throws URISyntaxException, AlignmentException, UnsupportedEncodingException {
	BasicOntologyNetwork newnoo = new BasicOntologyNetwork();
	newnoo.setIndentString( "" );
	newnoo.setNewLineString( "" );	
	// Print it in a string
	ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
	PrintWriter writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	try {
	    newnoo.write( writer );
	} finally {
	    writer.flush();
	    writer.close();
	}
	String str1 = stream.toString();
	assertEquals( str1, "<?xml version='1.0' encoding='utf-8' standalone='no'?><rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#' xml:base='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#' xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' xmlns:align='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#' xmlns:alext='http://exmo.inrialpes.fr/align/ext/1.0/' xmlns:xsd='http://www.w3.org/2001/XMLSchema#'><OntologyNetwork></OntologyNetwork></rdf:RDF>" );
    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"expandTest"},expectedExceptions = AlignmentException.class)
    public void printNoURINetworkTest() throws URISyntaxException, AlignmentException, UnsupportedEncodingException {
	BasicOntologyNetwork newnoo = (BasicOntologyNetwork)noo;
	newnoo.setIndentString( "" );
	newnoo.setNewLineString( "" );	
	// Print it in a string
	ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
	PrintWriter writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	try {
	    newnoo.write( writer );
	} finally {
	    writer.flush();
	    writer.close();
	}
    }

    @Test(groups = { "full", "raw" }, dependsOnMethods = {"printNoURINetworkTest"})
    public void printNetworkTest() throws FileNotFoundException, URISyntaxException, AlignmentException, UnsupportedEncodingException {
	BasicOntologyNetwork newnoo = (BasicOntologyNetwork)noo;
	newnoo.setIndentString( "" );
	newnoo.setNewLineString( "" );
	int i = 0;
	String prefix = "test/output/align";
	for( Alignment al : newnoo.getAlignments() ) {
	    i++;
	    al.setExtension( Namespace.ALIGNMENT.uri, Annotations.ID, "file:"+prefix+i+".rdf" );
	    // I should also save these alignments...
	    FileOutputStream stream = new FileOutputStream( prefix+i+".rdf" );
	    PrintWriter writer = new PrintWriter (
                          new BufferedWriter(
                               new OutputStreamWriter( stream, "UTF-8" )), true);
	    try {
		al.render( new RDFRendererVisitor( writer ) );
	    } finally {
		writer.flush();
		writer.close();
	    }
	}
	// Print it in a string
	ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
	PrintWriter writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	try {
	    newnoo.write( writer );
	} finally {
	    writer.flush();
	    writer.close();
	}
	String str1 = stream.toString();
	//System.err.println(str1);
	assertEquals( str1, "<?xml version='1.0' encoding='utf-8' standalone='no'?><rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#' xml:base='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#' xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' xmlns:align='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#' xmlns:alext='http://exmo.inrialpes.fr/align/ext/1.0/' xmlns:xsd='http://www.w3.org/2001/XMLSchema#'><OntologyNetwork><ontology rdf:resource='http://www.example.org/ontology2'/><ontology rdf:resource='http://www.example.org/ontology1'/><ontology rdf:resource='file:examples/rdf/edu.mit.visus.bibtex.owl'/><ontology rdf:resource='file:examples/rdf/edu.umbc.ebiquity.publication.owl'/><alignment rdf:resource='file:test/output/align1.rdf'/><alignment rdf:resource='file:test/output/align2.rdf'/><alignment rdf:resource='file:test/output/align3.rdf'/><alignment rdf:resource='file:test/output/align4.rdf'/><alignment rdf:resource='file:test/output/align5.rdf'/><alignment rdf:resource='file:test/output/align6.rdf'/></OntologyNetwork></rdf:RDF>" );
	// Read it from the string
	OntologyNetwork bon = BasicOntologyNetwork.read( new ByteArrayInputStream( str1.getBytes() ) );
	// Check the topology
	assertEquals( bon.getAlignments().size(), 6 );
	assertEquals( bon.getOntologies().size(), 4 );
    }

}
