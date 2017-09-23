/*
 * $Id: TestSet.java 2120 2017-01-11 12:11:11Z euzenat $
 *
 * Copyright (C) INRIA 2011-2014, 2017
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

/*
 * This class describes a test set (such as the benchmark test set of OAEI)
 * It is described as a hierarchy of test cases which can be generated from one another
 * This class is an abstract 
 * Generates the OAEI Benchmark dataset from an ontology
 * It can generate it in a continuous way (each test build on top of a previous one)
 * or generate tests independently.
 * 
 * Variations can also be obtained.
 *
 * The API is:
 * initTestCases: creates the test structure (abstract) made of TestCases
 * generate: generates the actual tests using a TestGenerator
 */

package fr.inrialpes.exmo.align.gen;

import java.util.Properties;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.AlignmentException;

public abstract class TestSet {
    final static Logger logger = LoggerFactory.getLogger( TestSet.class );

    private String initOntoFile;                         //the initial ontology file
    protected String secondOntoFile;                         //the secondary ontology file
    protected boolean continuous = false;

    private TestGenerator generator;                                  // a TestGenerator
   
    protected TestCase root;
    protected HashMap<String,TestCase> tests;
    static String FULL = "1.0f";

    public TestSet() {
	generator = new TestGenerator();
	tests = new HashMap<String,TestCase>();
    }

    public void addTestChild( String from, String name, Properties params ) {
	addTestChild( tests.get( from ), name, params );
    }
    public void addTestChild( TestCase father, String name, Properties params ) {
	//logger.trace( "{} [{}]", name, father );
	TestCase c = father.addSubTest( name, params );
	tests.put( name, c );
    }
    public TestCase initTests( String name ) {
	TestCase c = TestCase.initTestTree( name );
	tests.put( name, c );
	root = c;
	return c;
    }

    public Properties newProperties( String k, String v, String k2, String v2 ) {
	Properties p = newProperties( k2, v2 );
	p.setProperty( k, v );
	return p;
    }
    public Properties newProperties( String k, String v ) {
	Properties p = new Properties();
	p.setProperty( k, v );
	return p;
    }

    // ----------------------------------------------------------
    // Declare test generation tree
    public abstract void initTestCases( Properties params );

    // ----------------------------------------------------------
    // Generates the test set
    public void generate( Properties params ) throws AlignmentException {
	// Generator parameters... are these OK?
	initOntoFile = params.getProperty( "filename" ); // If no filename error
	if ( params.getProperty( "urlprefix" ) != null ) generator.setURLPrefix( params.getProperty( "urlprefix" ) );
	if ( params.getProperty( "outdir" ) != null ) generator.setDirPrefix( params.getProperty( "outdir" ) );
	if ( params.getProperty( "continuous" ) != null ) continuous = true;
	String ontoname = params.getProperty( "ontoname" );
	if ( ontoname != null ) {
	    generator.setOntoFilename( params.getProperty( "ontoname" ) );
	} else {
	    ontoname = "onto.rdf"; // could be better
	}
	secondOntoFile = initOntoFile; // default value

	// Initialises test cases tree
	initTestCases( params );
	// Print it
	// printTestHierarchy( root, 0 );
	if ( params.getProperty( "alignname" ) != null ) generator.setAlignFilename( params.getProperty( "alignname" ) );
	// Recursively generate all tests
	// All this is really tied to 
	Properties parameters = new Properties();
	parameters.setProperty( "copy101", "" );
	if ( params.getProperty( "urlprefix" ) != null ) 
	    parameters.setProperty( "urlprefix", params.getProperty( "urlprefix" ) );
	Properties newalign = generator.modifyOntology( initOntoFile, (Properties)null, root.name, parameters );
	for ( TestCase sub : root.subTests ) {
	    generateTest( sub, newalign );
	}
    }

    public void generateTest( TestCase c, Properties align ) throws AlignmentException {
	Properties newalign;
	if ( continuous ) {
	    newalign = generator.incrementModifyOntology( c.father.name, (Properties)align.clone(), c.name, c.parameters );
	} else {
	    // Here we should start from the initOntoFile 101
	    newalign = generator.modifyOntology( secondOntoFile, (Properties)null, c.name, c.cumulated );
	}
	for ( TestCase sub : c.subTests ) {
	    generateTest( sub, newalign );
	}
    }

    public void printTestHierarchy( TestCase c, int level ) {
	for ( int i = 0; i < level; i++ ) System.out.print( "  " );
	System.out.print( c.name+" [" );
	for ( Entry<Object,Object> m : c.cumulated.entrySet() ) {
	    System.out.print( " "+m.getKey()+"="+m.getValue()+";" );
	}
	System.out.println( " ]" );
	for ( TestCase sub : c.subTests ) printTestHierarchy( sub, level+1 );
    }
}

class TestCase {
    public String name = null;            // test name = directory
    public TestCase father = null;        // test this one is generated from
    public Properties parameters = null;  // new modifications to apply
    public Properties cumulated = null;   // cummulated modifications from root
    public Set<TestCase> subTests = null; // tests derived from this one

    public TestCase(){
	subTests = new HashSet<TestCase>();
    }

    public TestCase( String n ){
	subTests = new HashSet<TestCase>();
	name = n;
    }

    public TestCase( String n, Properties p ){
	subTests = new HashSet<TestCase>();
	name = n;
	parameters = p;
    }

    public TestCase addSubTest( String name, Properties params ) {
	TestCase t = new TestCase( name, params );
	// Add the diferential parameters (eventually)
	t.father = this;
	subTests.add( t );
	t.cumulated = (Properties)cumulated.clone();
	for ( Object k : params.keySet() ) {
	    t.cumulated.setProperty( (String)k, params.getProperty( (String)k ) );
	}
	for ( Entry<Object,Object> m : params.entrySet() ) {
	    t.cumulated.setProperty( (String)m.getKey(), (String)m.getValue() );
	}
	return t;
    }

    public static TestCase initTestTree( String n ) {
	TestCase c = new TestCase( n );
	c.cumulated = new Properties();
	return c;
    }

}


