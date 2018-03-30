/*
 * $Id: SPARQLLinkkerRendererVisitorTest.java 2143 2017-07-12 20:11:04Z euzenat $
 *
 * Copyright (C) INRIA, 2014-2015, 2017
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
import fr.inrialpes.exmo.align.impl.edoal.EDOALCell;
import fr.inrialpes.exmo.align.impl.edoal.Linkkey;
import fr.inrialpes.exmo.align.impl.renderer.SPARQLLinkkerRendererVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
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
        EDOALAlignment alignment = Utils.loadAlignement("people_intersects_alignment.rdf");
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
                + "FILTER ( langMatches( lang( ?o3 ), \"fr\" ) )\n"
                + "?s2 ns1:givenName ?o4 .\n"
                + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n\n";

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
                + "FILTER ( langMatches( lang( ?o3 ), \"fr\" ) )\n"
                + "?s2 ns1:givenName ?o4 .\n"
                + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n\n";
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
        EDOALAlignment alignment = Utils.loadAlignement("people_equals_alignment.rdf");
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
	    + "PREFIX ns1:<http://xmlns.com/foaf/0.1/>\n"
	    + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
	    + "\n"
	    + "SELECT DISTINCT ?s1 ?s2\n"
	    + "WHERE {\n"
	    + "?s1 rdf:type ns0:Personne .\n"
	    + "?s2 rdf:type ns1:Person .\n"
	    + "?s1 ns0:nom ?o3 .\n"
	    + "?s2 ns1:givenName ?o4 .\n"
	    + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
	    + "MINUS { ?s1 ns0:nom ?o5 .\n"
	    + "FILTER NOT EXISTS { ?s2 ns1:givenName ?o6 .\n"
	    + " FILTER ( lcase(str(?o5)) = lcase(str(?o6)) ) } }\n"
	    + "MINUS { ?s2 ns1:givenName ?o6 .\n"
	    + "FILTER NOT EXISTS { ?s1 ns0:nom ?o5 .\n"
	    + " FILTER ( lcase(str(?o5)) = lcase(str(?o6)) ) } }\n"
	    + "}\n\n";
        String query = renderer.getQuery(cell);
//        System.out.println("QueryFromSimpleLinkkeyFromEquals expected Query : " + expectedQuery);
//        System.out.println("QueryFromSimpleLinkkeyFromEquals returned Query : " + query);
        assertEquals(query, expectedQuery);

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

    // Two link keys in one cell must be rendered...
    @Test(groups = {"full", "impl", "raw"}, dependsOnMethods = {"QueryFromSimpleLinkkeyAndIntersects", "QueryFromSimpleLinkkeyAndEquals"})
    public void QueryFromMultipleLinkkeys() throws Exception {
        EDOALAlignment alignment = Utils.loadAlignement("people_multiple_alignment.rdf");
        assertEquals( alignment.nbCells(), 1 );
        Enumeration<Cell> cells = alignment.getElements();
        EDOALCell cell = (EDOALCell)cells.nextElement();
	Set<Linkkey> lks = cell.linkkeys();
	assertEquals( lks.size(), 2 );

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        SPARQLLinkkerRendererVisitor renderer = new SPARQLLinkkerRendererVisitor(writer);
        Properties properties = new Properties();
	properties.setProperty( "select", "true" ); // returns SELECT queries
        renderer.init(properties);
        alignment.render(renderer);

	String expectedQuery1 = 
	    "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
	    + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors-core/>\n"
	    + "PREFIX ns1:<http://xmlns.com/foaf/0.1/>\n"
	    + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
	    + "\n"
	    + "SELECT DISTINCT ?s1 ?s2\n"
	    + "WHERE {\n"
	    + "?s1 rdf:type ns0:Personne .\n"
	    + "?s2 rdf:type ns1:Person .\n"
	    + "?s1 ns0:nom ?o3 .\n"
	    + "?s2 ns1:givenName ?o4 .\n"
	    + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
	    + "}\n\n";
	String expectedQuery2 = 
	    "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
	    + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors-core/>\n"
	    + "PREFIX ns1:<http://xmlns.com/foaf/0.1/>\n"
	    + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
	    + "\n"
	    + "SELECT DISTINCT ?s1 ?s2\n"
	    + "WHERE {\n"
	    + "?s1 rdf:type ns0:Personne .\n"
	    + "?s2 rdf:type ns1:Person .\n"
	    + "?s1 ns0:nom ?o3 .\n"
	    + "?s2 ns1:givenName ?o4 .\n"
	    + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
	    + "MINUS { ?s1 ns0:nom ?o5 .\n"
	    + "FILTER NOT EXISTS { ?s2 ns1:givenName ?o6 .\n"
	    + " FILTER ( lcase(str(?o5)) = lcase(str(?o6)) ) } }\n"
	    + "MINUS { ?s2 ns1:givenName ?o6 .\n"
	    + "FILTER NOT EXISTS { ?s1 ns0:nom ?o5 .\n"
	    + " FILTER ( lcase(str(?o5)) = lcase(str(?o6)) ) } }\n"
	    + "}\n\n";
 
        String query = renderer.getQuery(cell);

	//System.err.println( query);
	assertTrue( query.contains( expectedQuery1 ) );
	assertTrue( query.contains( expectedQuery2 ) );

	// Same with CONSTRUCT queries
        renderer = new SPARQLLinkkerRendererVisitor(writer);
        renderer.init( new Properties() );
        alignment.render(renderer);

	expectedQuery1 = 
	    "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
	    + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors-core/>\n"
	    + "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n"
	    + "PREFIX ns1:<http://xmlns.com/foaf/0.1/>\n"
	    + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
	    + "\n"
	    + "CONSTRUCT { ?s1 owl:sameAs ?s2 }\n"
	    + "WHERE {\n"
	    + "?s1 rdf:type ns0:Personne .\n"
	    + "?s2 rdf:type ns1:Person .\n"
	    + "?s1 ns0:nom ?o3 .\n"
	    + "?s2 ns1:givenName ?o4 .\n"
	    + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
	    + "}\n\n";
	expectedQuery2 = 
	    "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
	    + "PREFIX ns0:<http://exmo.inrialpes.fr/connectors-core/>\n"
	    + "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n"
	    + "PREFIX ns1:<http://xmlns.com/foaf/0.1/>\n"
	    + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
	    + "\n"
	    + "CONSTRUCT { ?s1 owl:sameAs ?s2 }\n"
	    + "WHERE {\n"
	    + "?s1 rdf:type ns0:Personne .\n"
	    + "?s2 rdf:type ns1:Person .\n"
	    + "?s1 ns0:nom ?o3 .\n"
	    + "?s2 ns1:givenName ?o4 .\n"
	    + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
	    + "MINUS { ?s1 ns0:nom ?o5 .\n"
	    + "FILTER NOT EXISTS { ?s2 ns1:givenName ?o6 .\n"
	    + " FILTER ( lcase(str(?o5)) = lcase(str(?o6)) ) } }\n"
	    + "MINUS { ?s2 ns1:givenName ?o6 .\n"
	    + "FILTER NOT EXISTS { ?s1 ns0:nom ?o5 .\n"
	    + " FILTER ( lcase(str(?o5)) = lcase(str(?o6)) ) } }\n"
	    + "}\n\n";
 
        query = renderer.getQuery(cell);
	//System.err.println( query);
	assertTrue( query.contains( expectedQuery1 ) );
	assertTrue( query.contains( expectedQuery2 ) );
    }

    @Test(groups = {"full", "impl", "raw"}, dependsOnMethods = {"QueryFromMultipleLinkkeys"})
    public void QueryFromRelationLinkkeyAndIntersects() throws Exception {
        EDOALAlignment alignment = Utils.loadAlignement("people_relation_intersects_alignment.rdf");
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
                + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n\n";
	//System.err.println( query);
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

    // Rendering is non deterministic here...
    @Test(groups = {"full", "impl", "raw"}, dependsOnMethods = {"QueryFromSimpleLinkkeyAndEquals", "QueryFromRelationLinkkeyAndIntersects"})
    public void QueryFromRelationLinkkeyWithSameAs() throws Exception {
        EDOALAlignment alignment = Utils.loadAlignement("selected_lk_communes.rdf");
	// without sameAs
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        SPARQLLinkkerRendererVisitor renderer = new SPARQLLinkkerRendererVisitor(writer);
        Properties properties = new Properties();
        renderer.init(properties);
        alignment.render(renderer);

        assertEquals(alignment.nbCells(), 1);
        Enumeration<Cell> cells = alignment.getElements();
        Cell cell = cells.nextElement();

        String expectedQuery1 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
	    + "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n"
	    + "PREFIX ns0:<http://rdf.insee.fr/def/geo#>\n"
	    + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
	    + "PREFIX ns1:<http://www.geonames.org/ontology#>\n"
	    + "\nCONSTRUCT { ?s1 owl:sameAs ?s2 }\n"
	    + "WHERE {\n"
	    + "?s1 rdf:type ns0:Commune .\n"
	    + "?s2 rdf:type ns1:Commune .\n"
	    + "?s1 ns0:subdivisionDe ?o3 .\n"
	    + "?s2 ns1:parentFeature ?o3 .\n"
	    + "?s1 ns0:nom ?o4 .\n"
	    + "?s2 ns1:name ?o5 .\n"
	    + "FILTER ( lcase(str(?o4)) = lcase(str(?o5)) )\n"
	    + "}\n\n";
        String expectedQuery2 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
	    + "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n"
	    + "PREFIX ns0:<http://rdf.insee.fr/def/geo#>\n"
	    + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
	    + "PREFIX ns1:<http://www.geonames.org/ontology#>\n"
	    + "\nCONSTRUCT { ?s1 owl:sameAs ?s2 }\n"
	    + "WHERE {\n"
	    + "?s1 rdf:type ns0:Commune .\n"
	    + "?s2 rdf:type ns1:Commune .\n"
	    + "?s1 ns0:nom ?o3 .\n"
	    + "?s2 ns1:name ?o4 .\n"
	    + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
	    + "?s1 ns0:subdivisionDe ?o5 .\n"
	    + "?s2 ns1:parentFeature ?o5 .\n"
	    + "}\n\n";

	    String query = renderer.getQuery(cell);
//        System.out.println("QueryFromSimpleLinkkeyFromEquals expected Query : " + expectedQuery);
//        System.out.println("QueryFromSimpleLinkkeyFromEquals returned Query : " + query);
	//System.err.println( query);
	    assertTrue( query.equals( expectedQuery1 ) || query.equals( expectedQuery2 ) );

	// with sameAS
        stringWriter = new StringWriter();
        writer = new PrintWriter(stringWriter);
        renderer = new SPARQLLinkkerRendererVisitor(writer);
	properties.setProperty( "sameAs", "true" );
        renderer.init(properties);
        alignment.render(renderer);

        assertEquals(alignment.nbCells(), 1);
        cells = alignment.getElements();
        cell = cells.nextElement();

        expectedQuery1 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
	    + "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n"
	    + "PREFIX ns0:<http://rdf.insee.fr/def/geo#>\n"
	    + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
	    + "PREFIX ns1:<http://www.geonames.org/ontology#>\n"
	    + "\nCONSTRUCT { ?s1 owl:sameAs ?s2 }\n"
	    + "WHERE {\n"
	    + "?s1 rdf:type ns0:Commune .\n"
	    + "?s2 rdf:type ns1:Commune .\n"
	    + "?s1 ns0:subdivisionDe ?o3 .\n"
	    + "?s2 ns1:parentFeature ?o4 .\n"
	    + "FILTER ( ?o3 = ?o4 || EXISTS { ?o3 owl:sameAs ?o4 . } )\n"
	    + "?s1 ns0:nom ?o5 .\n"
	    + "?s2 ns1:name ?o6 .\n"
	    + "FILTER ( lcase(str(?o5)) = lcase(str(?o6)) )\n"
	    + "}\n\n";
        expectedQuery2 = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
	    + "PREFIX owl:<http://www.w3.org/2002/07/owl#>\n"
	    + "PREFIX ns0:<http://rdf.insee.fr/def/geo#>\n"
	    + "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
	    + "PREFIX ns1:<http://www.geonames.org/ontology#>\n"
	    + "\nCONSTRUCT { ?s1 owl:sameAs ?s2 }\n"
	    + "WHERE {\n"
	    + "?s1 rdf:type ns0:Commune .\n"
	    + "?s2 rdf:type ns1:Commune .\n"
	    + "?s1 ns0:nom ?o3 .\n"
	    + "?s2 ns1:name ?o4 .\n"
	    + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
	    + "?s1 ns0:subdivisionDe ?o5 .\n"
	    + "?s2 ns1:parentFeature ?o6 .\n"
	    + "FILTER ( ?o5 = ?o6 || EXISTS { ?o5 owl:sameAs ?o6 . } )\n"
	    + "}\n\n";

	query = renderer.getQuery(cell);
//        System.out.println("QueryFromSimpleLinkkeyFromEquals expected Query : " + expectedQuery);
//        System.out.println("QueryFromSimpleLinkkeyFromEquals returned Query : " + query);
	//System.err.println( query);
	    assertTrue( query.equals( expectedQuery1 ) || query.equals( expectedQuery2 ) );
    }


    @Test(groups = {"full", "impl", "raw"}, dependsOnMethods = {"QueryFromRelationLinkkeyAndIntersects"})
    public void QueryWithNamedGraphAndIntersectsLinkkey() throws Exception {
        EDOALAlignment alignment = Utils.loadAlignement("people_intersects_alignment.rdf");
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
                + "FILTER ( langMatches( lang( ?o3 ), \"fr\" ) )\n"
                + "}\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
                + "?s2 ns1:givenName ?o4 .\n"
                + "}\n"
                + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n\n";
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
                + "FILTER ( langMatches( lang( ?o4 ), \"fr\" ) )\n"
                + "}\n"
                + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n\n";
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
                + "FILTER ( langMatches( lang( ?o3 ), \"fr\" ) )\n"
                + "}\n"
                + "?s2 ns1:givenName ?o4 .\n"
                + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n\n";
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
                + "FILTER ( langMatches( lang( ?o4 ), \"fr\" ) )\n"
                + "}\n"
                + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n\n";
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
                + "FILTER ( langMatches( lang( ?o3 ), \"fr\" ) )\n"
                + "GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
                + "?s2 ns1:givenName ?o4 .\n"
                + "}\n"
                + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n\n";
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
                + "FILTER ( langMatches( lang( ?o4 ), \"fr\" ) )\n"
                + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n\n";
        renderer = new SPARQLLinkkerRendererVisitor(writer);
        properties = new Properties();
	properties.setProperty( "graphName1", onto2NamedGraph );
	properties.setProperty( "select", "true" );
        renderer.init(properties);
        inval.render(renderer);
        assertEquals( renderer.getQuery( inval.getElements().nextElement() ), expectedQuery );

        //With Relation        
        alignment = Utils.loadAlignement("people_relation_intersects_alignment.rdf");
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
                + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
                + "}\n\n";
        assertEquals( query, expectedQuery );
        
    }
    
    
    @Test(groups = {"full", "impl", "raw"}, dependsOnMethods = {"QueryFromRelationLinkkeyAndIntersects"})
    public void QueryWithNamedGraphAndEqualsLinkkey() throws Exception {
        EDOALAlignment alignment = Utils.loadAlignement("people_equals_alignment.rdf");
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
	    + "FILTER ( lcase(str(?o3)) = lcase(str(?o4)) )\n"
	    + "MINUS { GRAPH <http://exmo.inrialpes.fr/connectors/onto1-graph> {\n"
	    + "?s1 ns0:nom ?o5 .\n"
	    + "}\n"
	    + "FILTER NOT EXISTS { GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
	    + "?s2 ns1:givenName ?o6 .\n"
	    + "}\n"
	    + " FILTER ( lcase(str(?o5)) = lcase(str(?o6)) ) } }\n"
	    + "MINUS { GRAPH <http://exmo.inrialpes.fr/connectors/onto2-graph> {\n"
	    + "?s2 ns1:givenName ?o6 .\n"
	    + "}\n"
	    + "FILTER NOT EXISTS { GRAPH <http://exmo.inrialpes.fr/connectors/onto1-graph> {\n"
	    + "?s1 ns0:nom ?o5 .\n"
	    + "}\n"
	    + " FILTER ( lcase(str(?o5)) = lcase(str(?o6)) ) } }\n"
	    //+ "FILTER(?s1 != ?s2)\n"
	    + "}\n\n";
        
        String query = renderer.getQuery(cell);
	assertEquals( query, expectedQuery );
	Dataset dataset = DatasetFactory.create();
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
        dataset = DatasetFactory.create();
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
