/*
 * $Id: SPARQLLinkkerRendererVisitorTest.java 2079 2015-10-16 19:00:16Z euzenat $
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

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.impl.edoal.EDOALAlignment;
import fr.inrialpes.exmo.align.impl.renderer.SPARQLLinkkerRendererVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

/**
 * @author Nicolas Guillouet <nicolas.guillouet@inria.fr>
 *
 * JE: The same test could be made with the construct link.
 */

public class SPARQLLinkkerRendererVisitorTest {


    @Test(groups = {"full", "impl", "raw"}) //, dependsOnMethods = {"QueryWithoutLinkkey"})
    public void QueryFromSimpleLinkkeyAndIntersects() throws Exception {
        String alignmentFileName = "people_intersects_alignment.rdf";
        EDOALAlignment alignment = Utils.loadAlignement(alignmentFileName);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        SPARQLLinkkerRendererVisitor renderer = new SPARQLLinkkerRendererVisitor(writer);
        Properties properties = new Properties();
        renderer.init(properties);
        alignment.render(renderer);
        assertEquals(alignment.nbCells(), 1);
        Enumeration<Cell> cells = alignment.getElements();
        Cell cell = cells.nextElement();
        String expectedQuery0 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors-core/>\n"
                + "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n"
                + "PREFIX ns1:<http://xmlns.com/foaf/0.1/>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n\n"
                + "CONSTRUCT { ?s1 owl:sameAs ?s2 }\n"
                + "WHERE {\n"
                + "?s1 rdf:type ns0:Personne .\n"
                + "?s2 rdf:type ns1:Person .\n"
                + "?s1 ns0:nom ?o3 .\n"
                + "?s2 ns1:givenName ?o4 .\n"
                + "FILTER( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n";
        String query = renderer.getQuery( cell );
        assertEquals( query, expectedQuery0 );

	properties.setProperty( "select", "true" );
        renderer.init( properties );
        alignment.render( renderer );
        assertEquals( alignment.nbCells(), 1 );
        cells = alignment.getElements();
        cell = cells.nextElement();
        String expectedQuery = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors-core/>\n"
                + "PREFIX ns1:<http://xmlns.com/foaf/0.1/>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n\n"
                + "SELECT DISTINCT ?s1 ?s2\n"
                + "WHERE {\n"
                + "?s1 rdf:type ns0:Personne .\n"
                + "?s2 rdf:type ns1:Person .\n"
                + "?s1 ns0:nom ?o3 .\n"
                + "?s2 ns1:givenName ?o4 .\n"
                + "FILTER( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n";
        query = renderer.getQuery(cell);
        assertEquals( query, expectedQuery );

        Model values = Utils.loadValues(new String[]{"intersects_people_1.rdf", "intersects_people_2.rdf"});

        Query selectQuery = QueryFactory.create(query);
        QueryExecution selectQueryExec = QueryExecutionFactory.create(selectQuery, values);
        ResultSet results = selectQueryExec.execSelect();
        String[] expectedS1 = {
            "http://exmo.inrialpes.fr/connectors-data/people#alice_c1_1",
            "http://exmo.inrialpes.fr/connectors-data/people#alice_c2_1"};
        String[] expectedS2 = {
            "http://exmo.inrialpes.fr/connectors-data/people#alice_c1_2",
            "http://exmo.inrialpes.fr/connectors-data/people#alice_c2_2"};
        HashMap<String, Collection<String>> allResultValues = Utils.getResultValues(results);
        Collection<String> resultValues = allResultValues.get("s1");
        assertEquals(resultValues.size(), expectedS1.length);
        for ( String expected : expectedS1 ) {
            assertTrue( resultValues.contains( expected ) );
        }

        resultValues = allResultValues.get("s2");
        assertEquals(resultValues.size(), expectedS2.length);
        for ( String expected : expectedS2 ) {
            assertTrue( resultValues.contains( expected ) );
        }

	// Reverse direction
	EDOALAlignment inval = alignment.inverse();
	// JE: Useful to test
	//StringWriter swr = new StringWriter();
        //PrintWriter wr = new PrintWriter( swr );
        //fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor rr = new fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor(wr);
        //Properties pp = new Properties();
	//properties.setProperty( "select", "true" );
	//rr.init( pp );
	//inval.render( rr );
	//System.err.print( swr );
        renderer.init(properties);
        inval.render(renderer);
        query = renderer.getQuery( inval.getElements().nextElement() );
	//System.err.print( query );
        selectQuery = QueryFactory.create(query);
        selectQueryExec = QueryExecutionFactory.create(selectQuery, values);
        results = selectQueryExec.execSelect();
        allResultValues = Utils.getResultValues(results);
        resultValues = allResultValues.get("s1");
        assertEquals( resultValues.size(), expectedS2.length );
        for ( String expected : expectedS2 ) {
            assertTrue( resultValues.contains( expected ), "For expected : " + expected);
        }

        resultValues = allResultValues.get("s2");
        assertEquals( resultValues.size(), expectedS1.length );
        for ( String expected : expectedS1 ) {
            assertTrue( resultValues.contains( expected ) );
	}
    }


    @Test(groups = {"full", "impl", "raw"}, dependsOnMethods = {"QueryFromSimpleLinkkeyAndIntersects"})
    public void QueryFromSimpleLinkkeyAndEquals() throws Exception {
        String alignmentFileName = "people_equals_alignment.rdf";
        EDOALAlignment alignment = Utils.loadAlignement(alignmentFileName);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        SPARQLLinkkerRendererVisitor renderer = new SPARQLLinkkerRendererVisitor(writer);
        Properties properties = new Properties();
	properties.setProperty( "select", "true" );
        renderer.init(properties);
        alignment.render(renderer);

        assertEquals(alignment.nbCells(), 1);
        Enumeration<Cell> cells = alignment.getElements();
        Cell cell = cells.nextElement();

        String expectedQuery = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors-core/>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX ns1:<http://xmlns.com/foaf/0.1/>\n"
                + "SELECT DISTINCT ?s1 ?s2\n\n"
                + "WHERE {\n"
                + "?s1 rdf:type ns0:Personne .\n"
                + "?s2 rdf:type ns1:Person .\n"
                + "?s1 ns0:nom ?o1 .\n"
                + "?s2 ns1:givenName ?o1 .\n"
                + "MINUS { \n"
                + "?s1 ns0:nom ?o1 .\n"
                + "?s1 ns0:nom ?o2 .\n"
                + "?s2 ns1:givenName ?o1 .\n"
                + "FILTER(?s1 != ?s2 && ?o2 != ?o1 && NOT EXISTS {\n"
                + "?s2 ns1:givenName ?o2 .\n"
                + "}) \n"
                + "} \n"
                + "MINUS {\n"
                + "?s1 ns0:nom ?o1 .\n"
                + "?s2 ns1:givenName ?o1 .\n"
                + "?s2 ns1:givenName ?o2 .\n"
                + "FILTER(?s1 != ?s2 && ?o1 != ?o2 && NOT EXISTS {\n"
                + "?s1 ns0:nom ?o2 .\n"
                + "}) \n"
                + "} \n"
                + "FILTER(?s1 != ?s2)\n"
                + "}\n";
        String query = renderer.getQuery(cell);
//        System.out.println("QueryFromSimpleLinkkeyFromEquals expected Query : " + expectedQuery);
//        System.out.println("QueryFromSimpleLinkkeyFromEquals returned Query : " + query);
//        assertEquals(query, expectedQuery);

        Model values = Utils.loadValues(new String[]{"equals_people_1.rdf", "equals_people_2.rdf"});
        Query selectQuery = QueryFactory.create(query);
        String[] expectedS1 = {
            "http://exmo.inrialpes.fr/connectors-data/people#alice_c1_1"};
        String[] expectedS2 = {
            "http://exmo.inrialpes.fr/connectors-data/people#alice_c1_2",};
        HashMap<String, Collection<String>> allResultValues = Utils.getResultValues(QueryExecutionFactory.create(selectQuery, values).execSelect());
        Collection<String> resultValues = allResultValues.get("s1");
        assertEquals(resultValues.size(), expectedS1.length);
        for (String expected : expectedS1) {
            assertTrue(resultValues.contains(expected), "For expected : " + expected);
        }

        resultValues = allResultValues.get("s2");
        assertEquals(resultValues.size(), expectedS2.length);
        for (String expected : expectedS2) {
            assertTrue(resultValues.contains(expected), "For expected : " + expected);
        }

	// Reverse direction
	EDOALAlignment inval = alignment.inverse();
        renderer.init(properties);
        inval.render(renderer);
        query = renderer.getQuery( inval.getElements().nextElement() );
        selectQuery = QueryFactory.create(query);
        allResultValues = Utils.getResultValues(QueryExecutionFactory.create(selectQuery, values).execSelect());

        resultValues = allResultValues.get("s1");
        assertEquals(resultValues.size(), expectedS1.length);
        for (String expected : expectedS2) {//Change here
            assertTrue(resultValues.contains(expected), "For expected : " + expected);
        }

        resultValues = allResultValues.get("s2");
        assertEquals(resultValues.size(), expectedS2.length);
        for (String expected : expectedS1) {//Change here
            assertTrue(resultValues.contains(expected), "For expected : " + expected);
        }
    }

    @Test(groups = {"full", "impl", "raw"}, dependsOnMethods = {"QueryFromSimpleLinkkeyAndIntersects"})
    public void QueryFromRelationLinkkeyAndIntersects() throws Exception {
        String alignmentFileName = "people_relation_intersects_alignment.rdf";
        EDOALAlignment alignment = Utils.loadAlignement(alignmentFileName);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        SPARQLLinkkerRendererVisitor renderer = new SPARQLLinkkerRendererVisitor(writer);
        Properties properties = new Properties();
	properties.setProperty( "select", "true" );
        renderer.init(properties);
        alignment.render(renderer);

        assertEquals(alignment.nbCells(), 1);
        Enumeration<Cell> cells = alignment.getElements();
        Cell cell = cells.nextElement();

        Model values = Utils.loadValues(new String[]{"intersects_people_1.rdf", "intersects_people_2.rdf"});

        String query = renderer.getQuery(cell);
        String expectedQuery = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors-core/>\n"
                + "PREFIX ns1:<http://xmlns.com/foaf/0.1/>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n\n"
                + "SELECT DISTINCT ?s1 ?s2\n"
                + "WHERE {\n"
                + "?s1 rdf:type ns0:Personne .\n"
                + "?s2 rdf:type ns1:Person .\n"
                + "?s1 ns0:connait ?o3 .\n"
                + "?s2 ns1:knows ?o5 .\n"
                + "?o5 rdf:type ns1:Person .\n"
                + "?o5 ns1:givenName ?o4 .\n"
	    // JE2015: Here we have a URI (o3) compared to a string (o4)
                + "FILTER( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n"
                + "";
        assertEquals( query, expectedQuery );
        Query selectQuery = QueryFactory.create(query);
        QueryExecution selectQueryExec = QueryExecutionFactory.create(selectQuery, values);
        ResultSet results = selectQueryExec.execSelect();
        String[] expectedS1 = {
            "http://exmo.inrialpes.fr/connectors-data/people#alice_c1_1",
            "http://exmo.inrialpes.fr/connectors-data/people#alice_c2_1"};
        String[] expectedS2 = {
            "http://exmo.inrialpes.fr/connectors-data/people#alice_c1_2",
            "http://exmo.inrialpes.fr/connectors-data/people#alice_c2_2"};

        HashMap<String, Collection<String>> allResultValues = Utils.getResultValues(results);
        Collection<String> resultValues = allResultValues.get("s1");
        assertEquals(resultValues.size(), expectedS1.length);
        for (String expected : expectedS1) {
            assertTrue(resultValues.contains(expected));
        }

        resultValues = allResultValues.get("s2");
        assertEquals(resultValues.size(), expectedS2.length);
        for (String expected : expectedS2) {
            assertTrue(resultValues.contains(expected));
        }

	// Reverse direction
	EDOALAlignment inval = alignment.inverse();
        renderer.init(properties);
        inval.render(renderer);
        query = renderer.getQuery( inval.getElements().nextElement() );
        selectQuery = QueryFactory.create(query);
        selectQueryExec = QueryExecutionFactory.create(selectQuery, values);
        results = selectQueryExec.execSelect();

        allResultValues = Utils.getResultValues(results);
        resultValues = allResultValues.get("s1");
        assertEquals(resultValues.size(), expectedS2.length);
        for (String expected : expectedS2) {
            assertTrue(resultValues.contains(expected));
        }

        resultValues = allResultValues.get("s2");
        assertEquals(resultValues.size(), expectedS1.length);
        for (String expected : expectedS1) {
            assertTrue(resultValues.contains(expected));
        }
    }
    
    @Test(groups = {"full", "impl", "raw"}, dependsOnMethods = {"QueryFromRelationLinkkeyAndIntersects"})
    public void QueryWithNamedGraphAndIntersectsLinkkey() throws Exception {
        String alignmentFileName = "people_intersects_alignment.rdf";
        EDOALAlignment alignment = Utils.loadAlignement(alignmentFileName);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        String onto1NamedGraph = "http://exmo.inrialpes.fr/connectors/onto1-graph";
        String onto2NamedGraph = "http://exmo.inrialpes.fr/connectors/onto2-graph";

        //With named Graph on onto1 and onto2
        SPARQLLinkkerRendererVisitor renderer = new SPARQLLinkkerRendererVisitor(writer);
        Properties properties = new Properties();
	properties.setProperty( "graphName1", onto1NamedGraph );
	properties.setProperty( "graphName2", onto2NamedGraph );
	properties.setProperty( "select", "true" );
        renderer.init(properties);
        alignment.render(renderer);
        assertEquals(alignment.nbCells(), 1);
        Enumeration<Cell> cells = alignment.getElements();
        Cell cell = cells.nextElement();

        String expectedQuery = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors-core/>\n"
                + "PREFIX ns1:<http://xmlns.com/foaf/0.1/>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n\n"
                + "SELECT DISTINCT ?s1 ?s2\n"
                + "WHERE {\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto1-graph> {\n"
                + "?s1 rdf:type ns0:Personne .\n"
                + "}\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
                + "?s2 rdf:type ns1:Person .\n"
                + "}\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto1-graph> {\n"
                + "?s1 ns0:nom ?o3 .\n"
                + "}\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
                + "?s2 ns1:givenName ?o4 .\n"
                + "}\n"
                + "FILTER( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n";
        assertEquals( renderer.getQuery(cell), expectedQuery );

	// Reverse direction
        expectedQuery = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns1:<http://exmo.inrialpes.fr/connectors-core/>\n"
                + "PREFIX ns0:<http://xmlns.com/foaf/0.1/>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n\n"
                + "SELECT DISTINCT ?s1 ?s2\n"
                + "WHERE {\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
                + "?s1 rdf:type ns0:Person .\n"
                + "}\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto1-graph> {\n"
                + "?s2 rdf:type ns1:Personne .\n"
                + "}\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
                + "?s1 ns0:givenName ?o3 .\n"
                + "}\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto1-graph> {\n"
                + "?s2 ns1:nom ?o4 .\n"
                + "}\n"
                + "FILTER( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n";
	EDOALAlignment inval = alignment.inverse();
	properties.setProperty( "graphName1", onto2NamedGraph );
	properties.setProperty( "graphName2", onto1NamedGraph );
	properties.setProperty( "select", "true" );
        renderer.init(properties);
        inval.render(renderer);
        assertEquals( renderer.getQuery( inval.getElements().nextElement() ), expectedQuery );


        //With named Graph only on onto1
        renderer = new SPARQLLinkkerRendererVisitor(writer);
        properties = new Properties();
	properties.setProperty( "graphName1", onto1NamedGraph );
	properties.setProperty( "select", "true" );
        renderer.init(properties);
        alignment.render(renderer);
        assertEquals(alignment.nbCells(), 1);
        cells = alignment.getElements();
        cell = cells.nextElement();

        expectedQuery = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors-core/>\n"
                + "PREFIX ns1:<http://xmlns.com/foaf/0.1/>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n\n"
                + "SELECT DISTINCT ?s1 ?s2\n"
                + "WHERE {\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto1-graph> {\n"
                + "?s1 rdf:type ns0:Personne .\n"
                + "}\n"
                + "?s2 rdf:type ns1:Person .\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto1-graph> {\n"
                + "?s1 ns0:nom ?o3 .\n"
                + "}\n"
                + "?s2 ns1:givenName ?o4 .\n"
                + "FILTER( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n";
        assertEquals(renderer.getQuery(cell), expectedQuery);

	// Reverse direction
        expectedQuery = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns1:<http://exmo.inrialpes.fr/connectors-core/>\n"
                + "PREFIX ns0:<http://xmlns.com/foaf/0.1/>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n\n"
                + "SELECT DISTINCT ?s1 ?s2\n"
                + "WHERE {\n"
                + "?s1 rdf:type ns0:Person .\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto1-graph> {\n"
                + "?s2 rdf:type ns1:Personne .\n"
                + "}\n"
                + "?s1 ns0:givenName ?o3 .\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto1-graph> {\n"
                + "?s2 ns1:nom ?o4 .\n"
                + "}\n"
                + "FILTER( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n";
        renderer = new SPARQLLinkkerRendererVisitor(writer);
        properties = new Properties();
	properties.setProperty( "graphName2", onto1NamedGraph );
	properties.setProperty( "select", "true" );
        renderer.init(properties);
        inval.render(renderer);
        assertEquals( renderer.getQuery( inval.getElements().nextElement() ), expectedQuery );

        //With named Graph only on onto2
        expectedQuery = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors-core/>\n"
                + "PREFIX ns1:<http://xmlns.com/foaf/0.1/>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n\n"
                + "SELECT DISTINCT ?s1 ?s2\n"
                + "WHERE {\n"
                + "?s1 rdf:type ns0:Personne .\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
                + "?s2 rdf:type ns1:Person .\n"
                + "}\n"
                + "?s1 ns0:nom ?o3 .\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
                + "?s2 ns1:givenName ?o4 .\n"
                + "}\n"
                + "FILTER( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n";
        renderer = new SPARQLLinkkerRendererVisitor(writer);
        properties = new Properties();
	properties.setProperty( "graphName2", onto2NamedGraph );
	properties.setProperty( "select", "true" );
        renderer.init(properties);
        alignment.render(renderer);
        assertEquals(alignment.nbCells(), 1);
        cells = alignment.getElements();
        cell = cells.nextElement();
        assertEquals(renderer.getQuery(cell), expectedQuery);

	// Reverse direction
        expectedQuery = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns1:<http://exmo.inrialpes.fr/connectors-core/>\n"
                + "PREFIX ns0:<http://xmlns.com/foaf/0.1/>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n\n"
                + "SELECT DISTINCT ?s1 ?s2\n"
                + "WHERE {\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
                + "?s1 rdf:type ns0:Person .\n"
                + "}\n"
                + "?s2 rdf:type ns1:Personne .\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
                + "?s1 ns0:givenName ?o3 .\n"
                + "}\n"
                + "?s2 ns1:nom ?o4 .\n"
                + "FILTER( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n";
        renderer = new SPARQLLinkkerRendererVisitor(writer);
        properties = new Properties();
	properties.setProperty( "graphName1", onto2NamedGraph );
	properties.setProperty( "select", "true" );
        renderer.init(properties);
        inval.render(renderer);
        assertEquals( renderer.getQuery( inval.getElements().nextElement() ), expectedQuery );

        //With Relation        
        alignmentFileName = "people_relation_intersects_alignment.rdf";
        alignment = Utils.loadAlignement(alignmentFileName);
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        renderer = new SPARQLLinkkerRendererVisitor(writer);
        properties = new Properties();
	properties.setProperty( "graphName1", onto1NamedGraph );
	properties.setProperty( "graphName2", onto2NamedGraph );
	properties.setProperty( "select", "true" );
        renderer.init(properties);
        alignment.render(renderer);

        assertEquals(alignment.nbCells(), 1);
        cells = alignment.getElements();
        cell = cells.nextElement();

        String query = renderer.getQuery(cell);
        expectedQuery = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors-core/>\n"
                + "PREFIX ns1:<http://xmlns.com/foaf/0.1/>\n"
	        + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n\n"
                + "SELECT DISTINCT ?s1 ?s2\n"
                + "WHERE {\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto1-graph> {\n"
                + "?s1 rdf:type ns0:Personne .\n"
                + "}\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
                + "?s2 rdf:type ns1:Person .\n"
                + "}\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto1-graph> {\n"
                + "?s1 ns0:connait ?o3 .\n"
                + "}\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
                + "?s2 ns1:knows ?o5 .\n"
                + "?o5 rdf:type ns1:Person .\n"
                + "?o5 ns1:givenName ?o4 .\n"
                + "}\n"
	    // JE2015: Here we have a URI (o3) compared to a string (o4)
                + "FILTER( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n";
        assertEquals( query, expectedQuery );
        
    }
    
    
    @Test(groups = {"full", "impl", "raw"}, dependsOnMethods = {"QueryFromRelationLinkkeyAndIntersects"})
    public void QueryWithNamedGraphAndEqualsLinkkey() throws Exception {
        String alignmentFileName = "people_equals_alignment.rdf";
        EDOALAlignment alignment = Utils.loadAlignement(alignmentFileName);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        String onto1NamedGraph = "http://exmo.inrialpes.fr/connectors/onto1-graph";
        String onto2NamedGraph = "http://exmo.inrialpes.fr/connectors/onto2-graph";

        //With named Graph on onto1 and onto2
        SPARQLLinkkerRendererVisitor renderer = new SPARQLLinkkerRendererVisitor(writer);
        Properties properties = new Properties();
	properties.setProperty( "graphName1", onto1NamedGraph );
	properties.setProperty( "graphName2", onto2NamedGraph );
	properties.setProperty( "select", "true" );
        renderer.init(properties);
        alignment.render(renderer);
        assertEquals(alignment.nbCells(), 1);
        Enumeration<Cell> cells = alignment.getElements();
        Cell cell = cells.nextElement();

        String expectedQuery = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors-core/>\n"
	    + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
                + "PREFIX ns1:<http://xmlns.com/foaf/0.1/>\n\n"
                + "SELECT DISTINCT ?s1 ?s2\n"
                + "WHERE {\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto1-graph> {\n"
                + "?s1 rdf:type ns0:Personne .\n"
                + "}\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
                + "?s2 rdf:type ns1:Person .\n"
                + "}\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto1-graph> {\n"
                + "?s1 ns0:nom ?o1 .\n"
                + "}\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
                + "?s2 ns1:givenName ?o1 .\n"
                + "}\n"
                + "MINUS { \n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto1-graph> {\n"
                + "?s1 ns0:nom ?o1 .\n"
                + "?s1 ns0:nom ?o2 .\n"
                + "}\n"
                + "?s2 ns1:givenName ?o1 .\n"
                + "FILTER(?s1 != ?s2 && ?o2 != ?o1 && NOT EXISTS {\n"
                + "?s2 ns1:givenName ?o2 .\n"
                + "}) \n"
                + "} \n"
                + "MINUS {\n"
                + "?s1 ns0:nom ?o1 .\n"
                + "?s2 ns1:givenName ?o1 .\n"
                + "?s2 ns1:givenName ?o2 .\n"
                + "FILTER(?s1 != ?s2 && ?o1 != ?o2 && NOT EXISTS {\n"
                + "?s1 ns0:nom ?o2 .\n"
                + "}) \n"
                + "} \n"
                + "FILTER(?s1 != ?s2)\n"
                + "}\n";
        
        String query = renderer.getQuery(cell);
        Dataset dataset = DatasetFactory.createMem();
        dataset.addNamedModel(onto1NamedGraph, Utils.loadValues(new String[]{"equals_people_1.rdf"}));
        dataset.addNamedModel(onto2NamedGraph, Utils.loadValues(new String[]{"equals_people_2.rdf"}));
        Query selectQuery = QueryFactory.create(query);
        String[] expectedS1 = {
            "http://exmo.inrialpes.fr/connectors-data/people#alice_c1_1"};
        String[] expectedS2 = {
            "http://exmo.inrialpes.fr/connectors-data/people#alice_c1_2",};
        HashMap<String, Collection<String>> allResultValues = Utils.getResultValues(QueryExecutionFactory.create(selectQuery, dataset).execSelect());
        Collection<String> resultValues = allResultValues.get("s1");
        assertEquals(resultValues.size(), expectedS1.length);
        for (String expected : expectedS1) {
            assertTrue(resultValues.contains(expected), "For expected : " + expected);
        }

        resultValues = allResultValues.get("s2");
        assertEquals(resultValues.size(), expectedS2.length);
        for (String expected : expectedS2) {
            assertTrue(resultValues.contains(expected), "For expected : " + expected);
        }

	// Reverse direction
	EDOALAlignment inval = alignment.inverse();
        renderer = new SPARQLLinkkerRendererVisitor(writer);
        properties = new Properties();
	properties.setProperty( "graphName2", onto1NamedGraph );
	properties.setProperty( "graphName1", onto2NamedGraph );
	properties.setProperty( "select", "true" );
        renderer.init(properties);
        inval.render(renderer);
	query = renderer.getQuery( inval.getElements().nextElement() );
        dataset = DatasetFactory.createMem();
        dataset.addNamedModel(onto1NamedGraph, Utils.loadValues(new String[]{"equals_people_1.rdf"}));
        dataset.addNamedModel(onto2NamedGraph, Utils.loadValues(new String[]{"equals_people_2.rdf"}));
        selectQuery = QueryFactory.create( query );
        allResultValues = Utils.getResultValues(QueryExecutionFactory.create(selectQuery, dataset).execSelect());

        resultValues = allResultValues.get("s1");
        assertEquals(resultValues.size(), expectedS1.length);
        for (String expected : expectedS2) {//Change here
            assertTrue(resultValues.contains(expected), "For expected : " + expected);
        }

        resultValues = allResultValues.get("s2");
        assertEquals(resultValues.size(), expectedS2.length);
        for (String expected : expectedS1) {//Change here
            assertTrue(resultValues.contains(expected), "For expected : " + expected);
        }
    }
}
