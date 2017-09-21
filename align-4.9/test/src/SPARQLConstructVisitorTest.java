/*
 * $Id: SPARQLConstructVisitorTest.java 2076 2015-10-13 18:30:27Z euzenat $
 *
 * Copyright (C) INRIA, 2014-2015
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

import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.align.impl.edoal.ClassId;
import fr.inrialpes.exmo.align.impl.edoal.EDOALAlignment;
import fr.inrialpes.exmo.align.impl.edoal.EDOALCell;
import fr.inrialpes.exmo.align.impl.edoal.Expression;
import fr.inrialpes.exmo.align.impl.edoal.PropertyId;

import fr.inrialpes.exmo.align.impl.rel.EquivRelation;

import fr.inrialpes.exmo.align.impl.renderer.SPARQLConstructRendererVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Enumeration;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 * @author Nicolas Guillouet <nicolas.guillouet@inria.fr>
 */

public class SPARQLConstructVisitorTest {

    // Read the alignement that will be rendered by everyone
    @BeforeClass(groups = {"full", "impl", "raw"})
    private void init() throws Exception {
    }

    /**
     * Where we test SPARQL Queries Construction for simple relation (beetween
     * two properties)
     *
     * @throws Exception
     */
    @Test(groups = {"full", "impl", "raw"})
    public void ConstructSimplePropertiesRelation() throws Exception {
	// JE: This is absolutely non standard:
	// no init and alignment used without ontology declaration
	// This only works because the relation is built manually
        EDOALAlignment alignment = new EDOALAlignment();

        Relation opusRelation = new EquivRelation();
        Expression opusExpression1 = new PropertyId(new URI("http://exmo.inrialpes.fr/connectors#opus"));
        Expression opusExpression2 = new PropertyId(new URI("http://purl.org/ontology/mo/opus"));
        alignment.addAlignCell( "1", opusExpression1, opusExpression2, opusRelation, 1.0 );
	EDOALCell opusCell = (EDOALCell)alignment.getElements().nextElement();

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        SPARQLConstructRendererVisitor renderer = new SPARQLConstructRendererVisitor(writer);
        Properties properties = new Properties();
        renderer.init(properties);
        alignment.render(renderer);

        String expectedQuery1 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns1:<http://exmo.inrialpes.fr/connectors#>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX ns0:<http://purl.org/ontology/mo/>\n"
                + "CONSTRUCT {\n"
                + "?s ns0:opus ?o .\n"
                + "}\n"
                + "WHERE {\n"
                + "?s ns1:opus ?o .\n"
                + "}\n";
	assertEquals( renderer.getQuery(opusCell), expectedQuery1 );

	// Reverse direction
        String expectedQuery2 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors#>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX ns1:<http://purl.org/ontology/mo/>\n"
                + "CONSTRUCT {\n"
                + "?s ns0:opus ?o .\n"
                + "}\n"
                + "WHERE {\n"
                + "?s ns1:opus ?o .\n"
                + "}\n";
	EDOALAlignment inval = alignment.inverse();
	EDOALCell opusCell2 = (EDOALCell)inval.getElements().nextElement();
	renderer = new SPARQLConstructRendererVisitor(writer);
        renderer.init(properties);
        inval.render(renderer);
	assertEquals( renderer.getQuery( opusCell2 ), expectedQuery2 );

        //With Named Graph
        String namedGraph = "http://exmo.inrialpes.fr/connectors/one-graph";
        renderer = new SPARQLConstructRendererVisitor(writer);
        properties = new Properties();
	properties.setProperty( "graphName", namedGraph );
        renderer.init(properties);
        alignment.render(renderer);
        
        expectedQuery1 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns1:<http://exmo.inrialpes.fr/connectors#>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX ns0:<http://purl.org/ontology/mo/>\n"
                + "CONSTRUCT {\n"
                + "?s ns0:opus ?o .\n"
                + "}\n"
                + "WHERE {\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/one-graph> {\n"
                + "?s ns1:opus ?o .\n"
                + "}\n"
                + "}\n";
	assertEquals( renderer.getQuery(opusCell), expectedQuery1 );

	// Reverse direction
        expectedQuery2 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors#>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX ns1:<http://purl.org/ontology/mo/>\n"
                + "CONSTRUCT {\n"
                + "?s ns0:opus ?o .\n"
                + "}\n"
                + "WHERE {\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/one-graph> {\n"
                + "?s ns1:opus ?o .\n"
                + "}\n"
                + "}\n";
        renderer = new SPARQLConstructRendererVisitor(writer);
	properties = new Properties();
	properties.setProperty( "graphName", namedGraph );
        renderer.init(properties);
        inval.render(renderer);
	assertEquals( renderer.getQuery(opusCell2), expectedQuery2 );

        //For remote sparql endpoint : 
//        String remoteServiceURIName = "http://example.org/remoteSparql";
//        URI remoteServiceURI = new URI(remoteServiceURIName);
//
//        expectedQuery1 = String.format("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
//                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors#>\n"
//                + "PREFIX ns1:<http://purl.org/ontology/mo/>\n"
//                + "CONSTRUCT {\n"
//                + "?s ns1:opus ?o .\n"
//                + "}\n"
//                + "WHERE {\n"
//                + "SERVICE <%s> {\n"
//                + "?s ns0:opus ?o .\n"
//                + "}\n"
//                + "}\n", remoteServiceURIName);
//        
//        expectedQuery2 = String.format("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
//                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors#>\n"
//                + "PREFIX ns1:<http://purl.org/ontology/mo/>\n"
//                + "CONSTRUCT {\n"
//                + "?s ns0:opus ?o .\n"
//                + "}\n"
//                + "WHERE {\n"
//                + "SERVICE <%s> {\n"
//                + "?s ns1:opus ?o .\n"
//                + "}\n"
//                + "}\n", remoteServiceURIName);
//        
//        assertEquals(renderer.getQueryFromOnto1ToOnto2(opusCell, remoteServiceURI), expectedQuery1);
//        assertEquals(renderer.getQueryFromOnto2ToOnto1(opusCell, remoteServiceURI), expectedQuery2);
    }

    /**
     * Where we test SPARQL Queries Construction for classes relation
     *
     * @throws Exception
     */
    @Test(groups = {"full", "impl", "raw"})
    public void ConstructSimpleClassesRelation() throws Exception {
        EDOALAlignment alignment = new EDOALAlignment();

        Relation classesRelation = new EquivRelation();
        Expression rootElementExpression = new ClassId(new URI("http://exmo.inrialpes.fr/connectors#RootElement"));
        Expression musicalWorkExpression = new ClassId(new URI("http://purl.org/ontology/mo/MusicalWork"));
	alignment.addAlignCell( "1", rootElementExpression, musicalWorkExpression, classesRelation, 1.0 );
	EDOALCell classCell = (EDOALCell)alignment.getElements().nextElement();

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        SPARQLConstructRendererVisitor renderer = new SPARQLConstructRendererVisitor(writer);
        Properties properties = new Properties();
        renderer.init(properties);
        alignment.render(renderer);

        String expectedQuery1 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns1:<http://exmo.inrialpes.fr/connectors#>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX ns0:<http://purl.org/ontology/mo/>\n"
                + "CONSTRUCT {\n"
                + "?s rdf:type ns0:MusicalWork .\n"
                + "}\n"
                + "WHERE {\n"
                + "?s rdf:type ns1:RootElement .\n"
                + "}\n";
	assertEquals( renderer.getQuery( classCell ), expectedQuery1 );

	// Reverse direction
        String expectedQuery2 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors#>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX ns1:<http://purl.org/ontology/mo/>\n"
                + "CONSTRUCT {\n"
                + "?s rdf:type ns0:RootElement .\n"
                + "}\n"
                + "WHERE {\n"
                + "?s rdf:type ns1:MusicalWork .\n"
                + "}\n";
	EDOALAlignment inval = alignment.inverse();
	EDOALCell classCell2 = (EDOALCell)inval.getElements().nextElement();
        renderer = new SPARQLConstructRendererVisitor(writer);
        renderer.init(properties);
        inval.render(renderer);
	assertEquals( renderer.getQuery( classCell2 ), expectedQuery2 );
    }

    @Test(groups = {"full", "impl", "raw"})
    public void ConstructSimpleComposePropertyRelation() throws Exception {
        String alignmentFileName = "alignment1b.rdf";
        EDOALAlignment alignment = Utils.loadAlignement(alignmentFileName);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        SPARQLConstructRendererVisitor renderer = new SPARQLConstructRendererVisitor(writer);
        Properties properties = new Properties();
        renderer.init(properties);
        alignment.render(renderer);
        assertEquals(alignment.nbCells(), 1);
        Cell cell = alignment.getElements().nextElement();

        String expectedQuery1 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns2:<http://exmo.inrialpes.fr/connectors#>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX ns1:<http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX ns0:<http://purl.org/ontology/mo/>\n"
                + "CONSTRUCT {\n"
                + "?s ns0:key _:o2 .\n"
                + "_:o2 ns1:label ?o .\n"
                + "}\n"
                + "WHERE {\n"
                + "?s ns2:key ?o .\n"
                + "}\n";
	assertEquals( renderer.getQuery( cell ), expectedQuery1 );

	// Reverse direction
        String expectedQuery2 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors#>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX ns2:<http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX ns1:<http://purl.org/ontology/mo/>\n"
                + "CONSTRUCT {\n"
                + "?s ns0:key ?o .\n"
                + "}\n"
                + "WHERE {\n"
                + "?s ns1:key ?o2 .\n"
                + "?o2 ns2:label ?o .\n"
                + "}\n";
        EDOALAlignment inval = alignment.inverse();
        renderer = new SPARQLConstructRendererVisitor(writer);
        renderer.init(properties);
        inval.render(renderer);
	assertEquals( renderer.getQuery( inval.getElements().nextElement() ), expectedQuery2 );
    }

    @Test(groups = {"full", "impl", "raw"})
    public void ConstructComplexComposePropertyRelation() throws Exception {
        String alignmentFileName = "alignment1.rdf";
        EDOALAlignment alignment = Utils.loadAlignement(alignmentFileName);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        SPARQLConstructRendererVisitor renderer = new SPARQLConstructRendererVisitor(writer);
        Properties properties = new Properties();
        renderer.init(properties);
        alignment.render(renderer);
        assertEquals(alignment.nbCells(), 1);
        Cell cell = alignment.getElements().nextElement();

        String expectedQuery1 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns0:<http://purl.org/NET/c4dm/keys.owl#>\n"
                + "PREFIX ns3:<http://exmo.inrialpes.fr/connectors#>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX ns2:<http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX ns1:<http://purl.org/ontology/mo/>\n"
                + "CONSTRUCT {\n"
                + "?s rdf:type ns0:Work .\n"
                + "?s ns1:key _:o2 .\n"
                + "_:o2 rdf:type ns0:Key .\n"
                + "_:o2 ns2:label ?o .\n"
                + "}\n"
                + "WHERE {\n"
                + "?s ns3:key ?o .\n"
                + "}\n";
	assertEquals( renderer.getQuery( cell ), expectedQuery1 );

	// Reverse direction
        String expectedQuery2 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns1:<http://purl.org/NET/c4dm/keys.owl#>\n"
                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors#>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX ns3:<http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX ns2:<http://purl.org/ontology/mo/>\n"
                + "CONSTRUCT {\n"
                + "?s ns0:key ?o .\n"
                + "}\n"
                + "WHERE {\n"
                + "?s rdf:type ns1:Work .\n"
                + "?s ns2:key ?o2 .\n"
                + "?o2 rdf:type ns1:Key .\n"
                + "?o2 ns3:label ?o .\n"
                + "}\n";
        EDOALAlignment inval = alignment.inverse();
        renderer = new SPARQLConstructRendererVisitor(writer);
        renderer.init(properties);
        inval.render(renderer);
	assertEquals( renderer.getQuery( inval.getElements().nextElement() ), expectedQuery2 );
    }

    @Test(groups = {"full", "impl", "raw"})
    public void ConstructWeirdComposePropertyRelation() throws Exception {
        String alignmentFileName = "alignment1c.rdf";
        EDOALAlignment alignment = Utils.loadAlignement(alignmentFileName);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        SPARQLConstructRendererVisitor renderer = new SPARQLConstructRendererVisitor(writer);
        Properties properties = new Properties();
        renderer.init(properties);
        alignment.render(renderer);
        assertEquals(alignment.nbCells(), 1);
        Cell cell = alignment.getElements().nextElement();

        String expectedQuery1 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns0:<http://purl.org/NET/c4dm/keys.owl#>\n"
                + "PREFIX ns3:<http://exmo.inrialpes.fr/connectors#>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX ns2:<http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX ns1:<http://purl.org/ontology/mo/>\n"
                + "CONSTRUCT {\n"
                + "?s _:o3 _:o2 .\n"
                + "_:o2 rdf:type ns0:Key .\n"
                + "_:o2 ns1:key _:o4 .\n"
                + "_:o4 ns2:label ?o .\n"
                + "}\n"
                + "WHERE {\n"
                + "?s ns3:key ?o .\n"
                + "}\n";
	assertEquals( renderer.getQuery( cell ), expectedQuery1 );

	// Reverse direction
        String expectedQuery2 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns1:<http://purl.org/NET/c4dm/keys.owl#>\n"
                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors#>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX ns3:<http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX ns2:<http://purl.org/ontology/mo/>\n"
                + "CONSTRUCT {\n"
                + "?s ns0:key ?o .\n"
                + "}\n"
                + "WHERE {\n"
                + "?s ?o3 ?o2 .\n"
                + "?o2 rdf:type ns1:Key .\n"
                + "?o2 ns2:key ?o4 .\n"
                + "?o4 ns3:label ?o .\n"
                + "}\n";
        EDOALAlignment inval = alignment.inverse();
        renderer = new SPARQLConstructRendererVisitor(writer);
        renderer.init(properties);
        inval.render(renderer);
	assertEquals( renderer.getQuery( inval.getElements().nextElement() ), expectedQuery2 );
    }
}
