/*
 * $Id: AlgTest.java 2092 2015-11-20 12:03:10Z euzenat $
 *
 * Copyright (C) INRIA, 2008-2010, 2013-2015
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
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;
import org.testng.annotations.Test;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.impl.Annotations;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.BasicRelation;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.impl.method.StringDistAlignment;
import fr.inrialpes.exmo.align.impl.method.NameAndPropertyAlignment;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;

import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.Properties;
import java.util.Iterator;
import java.util.Set;

public class AlgTest {

    Alignment align1 = null;
    Alignment align2 = null;
    Alignment align3 = null;

    // ----------------------------------------------------------------
    // Test the correct behaviour of alignments using BasicRelation (default)
    // ----------------------------------------------------------------

    @Test(groups = { "full", "impl", "raw" })
    public void basicInitTest() throws Exception {
	Properties params = new Properties();
	AlignmentProcess alignment1 = new StringDistAlignment();
	alignment1.init( new URI("file:examples/rdf/onto1.owl"), new URI("file:examples/rdf/onto2.owl"));
	alignment1.align( (Alignment)null, params );
	AlignmentProcess alignment2 = new StringDistAlignment();
	alignment2.init( new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl"), new URI("file:examples/rdf/edu.mit.visus.bibtex.owl"));
	alignment2.align( (Alignment)null, params );
	align1 = alignment1;
	align2 = alignment2;
    }

    @Test(expectedExceptions = AlignmentException.class, dependsOnMethods = {"basicInitTest"}, groups = { "full", "impl", "raw" })
    public void basicErrorTest1() throws Exception {
	// should throw an exception: 
	align1.join( align2 );
    }

    @Test(expectedExceptions = AlignmentException.class, dependsOnMethods = {"basicInitTest"}, groups = { "full", "impl", "raw" })
    public void basicErrorTest2() throws Exception {
	// should throw an exception: 
	align1.meet( align2 );
    }

    @Test(expectedExceptions = AlignmentException.class, dependsOnMethods = {"basicInitTest"}, groups = { "full", "impl", "raw" })
    public void basicErrorTest3() throws Exception {
	// should throw an exception: 
	align1.compose( align2 );
    }

    @Test(groups = { "full", "impl", "raw" }, dependsOnMethods = {"basicInitTest"})
    public void basicGenericityTest() throws Exception {
	// does createNewAlignment is able to return the correct method
	Alignment al = (Alignment)((BasicAlignment)align1).clone();
	assertTrue( al instanceof ObjectAlignment );
	assertTrue( al.getExtension( Namespace.EXT.uri, Annotations.METHOD ).equals("fr.inrialpes.exmo.align.impl.method.StringDistAlignment#clone") );
    }

    @Test(groups = { "full", "impl", "raw" }, dependsOnMethods = {"basicGenericityTest"})
    public void basicFullTest() throws Exception {
   	AlignmentProcess alignment1 = new NameAndPropertyAlignment();
	alignment1.init( new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl"), new URI("file:examples/rdf/edu.mit.visus.bibtex.owl"));
	alignment1.align( (Alignment)null, new Properties() );
	align1 = alignment1;

	//System.err.println( "Align1 :" );
	//for ( Cell c : align1 ) {
	//    System.err.println( c.getObject1()+" "+c.getRelation()+" "+c.getObject2() );
	//}
	//System.err.println( "Align2 :" );
	//for ( Cell c : align2 ) {
	//    System.err.println( c.getObject1()+" "+c.getRelation()+" "+c.getObject2() );
	//}

	assertEquals( align1.nbCells(), 35 );
	assertEquals( align2.nbCells(), 10 );
	Alignment al = align1.inverse();
	assertEquals( al.getOntology1(), align1.getOntology2() );
	assertEquals( al.nbCells(), 35 );
	al = (Alignment)((BasicAlignment)align1).clone();
	assertEquals( al.getOntology1(), align1.getOntology1() );
	assertEquals( al.nbCells(), 35 );
	al = align1.diff( align2 );
	assertEquals( al.getOntology1(), align1.getOntology1() );
	assertEquals( al.nbCells(), 34 ); 
	al = align2.diff( align1 );
	assertEquals( al.getOntology1(), align1.getOntology1() );
	assertEquals( al.nbCells(), 9 );
	al = align1.meet( align2 );
	assertEquals( al.getOntology1(), align1.getOntology1() );
	assertEquals( al.nbCells(), 1 );
	al = align1.join( align2 );
	assertEquals( al.getOntology1(), align1.getOntology1() );

	//System.err.println( "al:" );
	//for ( Cell c : al ) {
	//    System.err.println( c.getObject1()+" "+c.getRelation()+" "+c.getObject2() );
	//}

	assertEquals( al.nbCells(), 44 );
	}

    @Test(expectedExceptions = AlignmentException.class, groups = { "full", "impl", "raw" }, dependsOnMethods = {"basicFullTest"})
    public void basicComposeErrorTest() throws Exception {
	Alignment al = align1.compose( align2 );
    }

    @Test(groups = { "full", "impl", "raw" }, dependsOnMethods = {"basicInitTest"})
    public void basicComposeTest() throws Exception {
	AlignmentProcess alignment1 = new NameAndPropertyAlignment();
	alignment1.init( new URI("file:examples/rdf/edu.mit.visus.bibtex.owl"), new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl"));
	alignment1.align( (Alignment)null, new Properties() );
	assertEquals( alignment1.nbCells(), 38 );
	assertEquals( align2.nbCells(), 10 );

	//System.err.println( "Align1 :" );
	//for ( Cell c : alignment1 ) {
	//    System.err.println( c.getObject1()+" "+c.getRelation()+" "+c.getObject2() );
	//}
	//System.err.println( "Align2 :" );
	//for ( Cell c : align2 ) {
	//    System.err.println( c.getObject1()+" "+c.getRelation()+" "+c.getObject2() );
	//}

	Alignment al = alignment1.compose( align2 );
	assertEquals( alignment1.nbCells(), 38 );
	assertEquals( align2.nbCells(), 10 );
	assertEquals( al.getOntology1(), alignment1.getOntology1() );
	assertEquals( al.getOntology2(), align2.getOntology2() );
	assertEquals( al.nbCells(), 5 );
    }

    // ----------------------------------------------------------------
    // Test the sole creation of algebras of relations
    // ----------------------------------------------------------------

    @Test(groups = { "full", "impl", "raw" }, dependsOnMethods = {"basicComposeTest"})
    public void disjunctiveOperatorTest() throws Exception {
	//A5AlgebraRelation.init();
	// (Disjunctive) Relation creation
	A5AlgebraRelation r1 = new A5AlgebraRelation( "=,<" );
	A5AlgebraRelation r2 = new A5AlgebraRelation( " %, )( , < " );
	//A5AlgebraRelation r4 = r1.complement();
	// meet
	A5AlgebraRelation r5 = r1.meet( r2 );
	A5AlgebraRelation r6 = r2.meet( r1 );
	//System.err.println( r5+" -- "+r5.getRelations()+" --> "+r5.hashCode() );
	//System.err.println( r6+" -- "+r6.getRelations()+" --> "+r6.hashCode() );
	assertTrue( ((A5AlgebraRelation)r5).equals( ((A5AlgebraRelation)r6) ) );
	assertEquals( r5.getPrettyLabel(), "=,<,)(,%" );
	// join
	A5AlgebraRelation r7 = r1.join( r2 );
	A5AlgebraRelation r8 = r2.join( r1 );
	//System.err.println( "R7 = "+r7.getPrettyLabel() );
       	assertTrue( r7.equals( r8 ) );
	assertEquals( r7.getPrettyLabel(), "<" );
    }

    @Test(groups = { "full", "impl", "raw" }, dependsOnMethods = {"disjunctiveOperatorTest"})
    public void algebraOperatorTest() throws Exception {
	// inverse
	A5AlgebraRelation r1 = new A5AlgebraRelation( "=,<," );
	A5AlgebraRelation r3 = r1.inverse();
	A5AlgebraRelation r4 = r3.inverse();
	assertTrue( r4.equals( r1 ) );
	assertTrue( r1.equals( r4 ) );
	//System.err.println( "R4 = "+r4.getPrettyLabel() );
	assertEquals( r3.getPrettyLabel(), "=,>" );
	A5AlgebraRelation r2 = new A5AlgebraRelation( ", %, )( , < " );
	A5AlgebraRelation r5 = r2.inverse();
	//System.err.println( "R3 = "+r3.getPrettyLabel() );
	assertEquals( r5.getPrettyLabel(), ">,)(,%" );
	// compose
	A5AlgebraRelation r12 = r1.compose( r2 );
	A5AlgebraRelation r21 = r2.compose( r1 );
	assertTrue( r12.equals( r21 ) );
	assertEquals( r12.getPrettyLabel(), "<,)(,%" );
	A5AlgebraRelation r13 = r1.compose( r3 );
	A5AlgebraRelation r31 = r3.compose( r1 );
	assertTrue( !r13.equals( r31 ) );
	assertEquals( r13.getPrettyLabel(), "=,>,<,)(,%" );
	assertEquals( r31.getPrettyLabel(), "=,>,<,)(" );
	
    }

    @Test(expectedExceptions = AlignmentException.class, groups = { "full", "impl", "raw" }, dependsOnMethods = {"disjunctiveOperatorTest"})
    public void disjunctiveOperatorErrorTest() throws Exception {
	new A5AlgebraRelation( " random string " );
    }

    @Test(expectedExceptions = AlignmentException.class, groups = { "full", "impl", "raw" }, dependsOnMethods = {"disjunctiveOperatorErrorTest"})
    public void disjunctiveOperatorErrorTest2() throws Exception {
	new A5AlgebraRelation( ">,),=" );
    }

    // ----------------------------------------------------------------
    // Redo the first tests with the algebras of relations
    // ----------------------------------------------------------------

    // Likely also create some such as in BasicAlignmentTest

    @Test(expectedExceptions = AlignmentException.class, groups = { "full", "impl", "raw" }, dependsOnMethods = {"disjunctiveOperatorErrorTest2"})
    public void badRelationClassErrorTest() throws Exception {
	BasicAlignment alignment1 = new BasicAlignment();
	alignment1.setRelationType( "fr.inrialpes.exmo.align.impl.rel.A0RelationAlgebra" );
    }

    @Test(groups = { "full", "impl", "raw" }, dependsOnMethods = {"badRelationClassErrorTest"})
    public void relalgInitTest() throws Exception {
	Properties params = new Properties();
	AlignmentProcess alignment1 = new StringDistAlignment();
	((BasicAlignment)alignment1).setRelationType( "fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation" );
	assertEquals( ((BasicAlignment)alignment1).getRelationType(), Class.forName("fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation") );
	alignment1.init( new URI("file:examples/rdf/onto1.owl"), new URI("file:examples/rdf/onto2.owl"));
	alignment1.align( (Alignment)null, params );
	AlignmentProcess alignment2 = new StringDistAlignment();
	((BasicAlignment)alignment2).setRelationType( "fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation" );
	alignment2.init( new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl"), new URI("file:examples/rdf/edu.mit.visus.bibtex.owl"));

	alignment2.align( (Alignment)null, params );
	align1 = alignment1;
	align2 = alignment2;

	// Check the type of the relations in the alignments
	Iterator<Cell> it1 = align1.iterator();
	Iterator<Cell> it2 = align2.iterator();
	assertTrue( it1.hasNext() );
	assertTrue( it2.hasNext() );
	//System.err.println( it1.next().getRelation() );
	assertTrue( it1.next().getRelation() instanceof A5AlgebraRelation );
	assertTrue( it2.next().getRelation() instanceof A5AlgebraRelation );

	// I should also test such alignments done by hand or loaded
    }

    @Test(expectedExceptions = AlignmentException.class, dependsOnMethods = {"relalgInitTest"}, groups = { "full", "impl", "raw" })
    public void relalgJoinErrorTest() throws Exception {
	// should throw an exception: 
	align1.join( align2 );
    }

    @Test(expectedExceptions = AlignmentException.class, dependsOnMethods = {"relalgInitTest"}, groups = { "full", "impl", "raw" })
    public void relalgErrorTest2() throws Exception {
	// should throw an exception: 
	align1.meet( align2 );
    }

    @Test(expectedExceptions = AlignmentException.class, dependsOnMethods = {"relalgInitTest"}, groups = { "full", "impl", "raw" })
    public void relalgErrorTest3() throws Exception {
	// should throw an exception: 
	align1.compose( align2 );
    }

    @Test(groups = { "full", "impl", "raw" }, dependsOnMethods = {"relalgInitTest"})
    public void relalgGenericityTest() throws Exception {
	// does createNewAlignment is able to return the correct method
	Alignment al = (Alignment)((BasicAlignment)align1).clone();
	assertTrue( al instanceof ObjectAlignment );
	assertTrue( al.getExtension( Namespace.EXT.uri, Annotations.METHOD ).equals("fr.inrialpes.exmo.align.impl.method.StringDistAlignment#clone") );
    }

    @Test(groups = { "full", "impl", "raw" }, dependsOnMethods = {"relalgGenericityTest"})
    public void relalgFullTest() throws Exception {
   	AlignmentProcess alignment1 = new NameAndPropertyAlignment();
	((BasicAlignment)alignment1).setRelationType( "fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation" );
	alignment1.init( new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl"), new URI("file:examples/rdf/edu.mit.visus.bibtex.owl"));
	alignment1.align( (Alignment)null, new Properties() );
	align1 = alignment1;
	assertEquals( align1.nbCells(), 35 );
	assertEquals( align2.nbCells(), 10 );
	Alignment al = align1.inverse();
	assertEquals( al.getOntology1(), align1.getOntology2() );
	assertEquals( al.nbCells(), 35 );
	al = (Alignment)((BasicAlignment)align1).clone();
	assertEquals( al.getOntology1(), align1.getOntology1() );
	assertEquals( al.nbCells(), 35 );
	//System.err.println( "ALIGN1: ");
	//for ( Cell c : align1 ) {
	//    System.err.println( c.getRelation() );
	//}
	//System.err.println( "ALIGN2: ");
	//for ( Cell c : align2 ) {
	//    System.err.println( c.getRelation() );
	//}
	al = align1.diff( align2 );
	assertEquals( al.getOntology1(), align1.getOntology1() );
	assertEquals( al.nbCells(), 34 ); // short diff
	al = align2.diff( align1 );
	assertEquals( al.getOntology1(), align1.getOntology1() );
	assertEquals( al.nbCells(), 9 );
	al = align1.meet( align2 );
	assertEquals( al.getOntology1(), align1.getOntology1() );
	assertEquals( al.nbCells(), 1 );
	al = align1.join( align2 );
	assertEquals( al.getOntology1(), align1.getOntology1() );
	assertEquals( al.nbCells(), 44 );
	}

    @Test(expectedExceptions = AlignmentException.class, groups = { "full", "impl", "raw" }, dependsOnMethods = {"relalgFullTest"})
    public void relalgComposeErrorTest() throws Exception {
	Alignment al = align1.compose( align2 );
    }

    @Test(groups = { "full", "impl", "raw" }, dependsOnMethods = {"relalgInitTest"})
    public void relalgComposeTest() throws Exception {
	AlignmentProcess alignment1 = new NameAndPropertyAlignment();
	((BasicAlignment)alignment1).setRelationType( "fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation" );
	alignment1.init( new URI("file:examples/rdf/edu.mit.visus.bibtex.owl"), new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl"));
	alignment1.align( (Alignment)null, new Properties() );
	assertEquals( alignment1.nbCells(), 38 );
	assertEquals( align2.nbCells(), 10 );

	//System.err.println( "Align1 :" );
	//for ( Cell c : alignment1 ) {
	//    System.err.println( c.getObject1()+" "+((BasicRelation)(c.getRelation())).getPrettyLabel()+" "+c.getObject2() );
	//}
	//System.err.println( "Align2 :" );
	//for ( Cell c : align2 ) {
	//    System.err.println( c.getObject1()+" "+((BasicRelation)(c.getRelation())).getPrettyLabel()+" "+c.getObject2() );
	//}
	Alignment al = alignment1.compose( align2 );
	assertEquals( alignment1.nbCells(), 38 );
	assertEquals( align2.nbCells(), 10 );
	assertEquals( al.getOntology1(), alignment1.getOntology1() );
	assertEquals( al.getOntology2(), align2.getOntology2() );
	assertEquals( al.nbCells(), 5 );
	// - Entry -- InBook -- InBook
	// - Misc -- Misc -- Misc
	// - Manual -- MastersThesis -- MastersThesis
	// - Proceedings -- Proceedings -- Proceedings
	// - Booklet -- Book -- Book
	align3 = al;
    }

    // ----------------------------------------------------------------
    // Test read/write with algebras of relations
    // ----------------------------------------------------------------

    @Test(groups = { "full", "impl", "raw" }, dependsOnMethods = {"relalgComposeTest"})
    public void relalgRoundTripTest() throws Exception {
	// Render align3
	FileOutputStream stream = new FileOutputStream("test/output/a5bibref.rdf");
	PrintWriter writer = new PrintWriter (
			      new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	AlignmentVisitor renderer = new RDFRendererVisitor( writer );
	align3.render( renderer );
	writer.flush();
	writer.close();
	// Load it back
	AlignmentParser aparser1 = new AlignmentParser( 0 );
	assertNotNull( aparser1 );
	Alignment alignment3 = aparser1.parse( "file:test/output/a5bibref.rdf" );
	assertNotNull( alignment3 );
	assertEquals( alignment3.nbCells(), 5 );
	assertEquals( ((BasicAlignment)alignment3).getRelationType(), A5AlgebraRelation.class );
	assertTrue( alignment3.iterator().next().getRelation() instanceof A5AlgebraRelation );
    }

    // Everything is made from new URI() here this tests the effective use of equals()
    @Test(groups = { "full", "impl", "raw" }, dependsOnMethods = {"relalgRoundTripTest"})
    public void relalgComposeWriteLoadTest() throws Exception {
	// Create a new alignment
	URIAlignment alignment1 = new URIAlignment();
	alignment1.init( new URI("file:examples/rdf/edu.mit.visus.bibtex.owl"), new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl"), A5AlgebraRelation.class, BasicConfidence.class );
	alignment1.addAlignCell( new URI("file:examples/rdf/edu.mit.visus.bibtex.owl#Book"),
			    new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl#Livre"),
			    "=,<",
			    0.6 );
	alignment1.addAlignCell( new URI("file:examples/rdf/edu.mit.visus.bibtex.owl#Book"),
			    new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl#Article"),
			    "%,)(,>,<",
			    0.4 );
	//			    "% )(,> ,,<",

	alignment1.addAlignCell( new URI("file:examples/rdf/edu.mit.visus.bibtex.owl#Paper"),
			    new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl#Article"),
			    ">",
			    0.5 );
	assertEquals( alignment1.nbCells(), 3 );
	FileOutputStream stream = new FileOutputStream("test/output/a5align1.rdf");
	PrintWriter writer = new PrintWriter (
			      new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	AlignmentVisitor renderer = new RDFRendererVisitor( writer );
	alignment1.render( renderer );
	writer.flush();
	writer.close();

	URIAlignment alignment2 = new URIAlignment();
	alignment2.init( new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl"), new URI("file:examples/another-dummy-ontology.owl"), A5AlgebraRelation.class, BasicConfidence.class );
	alignment2.addAlignCell( new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl#Livre"),
			    new URI("file:examples/another-dummy-ontology.owl#Livro"),
			    "<" ,
			    0.7 );
	alignment2.addAlignCell( new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl#Livre"),
			    new URI("file:examples/another-dummy-ontology.owl#Papel"),
			    "=,<" ,
			    0.3 );
	alignment2.addAlignCell( new URI("file:examples/rdf/edu.umbc.ebiquity.publication.owl#Article"),
			    new URI("file:examples/another-dummy-ontology.owl#Papel"),
			    "<",
			    0.5 );
	assertEquals( alignment2.nbCells(), 3 );
	FileOutputStream stream2 = new FileOutputStream("test/output/a5align2.rdf");
	PrintWriter writer2 = new PrintWriter (
			      new BufferedWriter(
			       new OutputStreamWriter( stream2, "UTF-8" )), true);
	AlignmentVisitor renderer2 = new RDFRendererVisitor( writer2 );
	alignment2.render( renderer2 );
	writer2.flush();
	writer2.close();

	URIAlignment alignment3 = (URIAlignment)alignment1.compose( alignment2 );
	//for ( Cell c : alignment3 ) {
	//    System.err.println( "<"+c.getObject1()+" "+c.getObject2()+" "+c.getRelation().getRelation()+" "+c.getStrength() );
	//}
	assertEquals( alignment3.nbCells(), 3 );
	// <file:examples/rdf/edu.mit.visus.bibtex.owl#Book file:examples/another-dummy-ontology.owl#Livro < 0.6
	Set<Cell> res = alignment3.getAlignCells( new URI( "file:examples/rdf/edu.mit.visus.bibtex.owl#Book" ), new URI( "file:examples/another-dummy-ontology.owl#Livro" ) );
	assertEquals( res.size(), 1 );
	// get it
	Cell c = res.iterator().next();
	assertEquals( c.getStrength(), 0.6 );
	assertEquals( c.getRelation(), new A5AlgebraRelation( "<" ) );
	// <file:examples/rdf/edu.mit.visus.bibtex.owl#Book file:examples/another-dummy-ontology.owl#Papel =,< 0.3
	res = alignment3.getAlignCells( new URI( "file:examples/rdf/edu.mit.visus.bibtex.owl#Book" ), new URI( "file:examples/another-dummy-ontology.owl#Papel" ) );
	assertEquals( res.size(), 1 );
	// get it
	c = res.iterator().next();
	assertEquals( c.getStrength(), 0.3 );
	assertEquals( c.getRelation(), new A5AlgebraRelation( "=,<" ) );
	// <file:examples/rdf/edu.mit.visus.bibtex.owl#Paper file:examples/another-dummy-ontology.owl#Papel =,>,<,)( 0.5
	res = alignment3.getAlignCells( new URI( "file:examples/rdf/edu.mit.visus.bibtex.owl#Paper" ), new URI( "file:examples/another-dummy-ontology.owl#Papel" ) );
	assertEquals( res.size(), 1 );
	// get it
	c = res.iterator().next();
	assertEquals( c.getStrength(), 0.5 );
	assertEquals( c.getRelation(), new A5AlgebraRelation( "=,>,<,)(" ) );

	FileOutputStream stream3 = new FileOutputStream("test/output/a5align3.rdf");
	PrintWriter writer3 = new PrintWriter (
			      new BufferedWriter(
			       new OutputStreamWriter( stream3, "UTF-8" )), true);
	AlignmentVisitor renderer3 = new RDFRendererVisitor( writer3 );
	alignment3.render( renderer3 );
	writer3.flush();
	writer3.close();

	// Now test the parser (including encoded elements)
	AlignmentParser aparser1 = new AlignmentParser( 0 );
	assertNotNull( aparser1 );
	Alignment alignment4 = aparser1.parse( "file:test/output/a5align3.rdf" );
	assertNotNull( alignment4 );
	assertEquals( alignment4.nbCells(), 3 );
    }

    // ----------------------------------------------------------------
    // Test the closure and merge of ontology networks...
    // ----------------------------------------------------------------

    @Test(groups = { "full", "impl", "raw" }, dependsOnMethods = {"relalgComposeWriteLoadTest"})
    public void relalgNetworkClosureTest() throws Exception {
	// first invert
	// reflexive closure?
	// symmetric closure
	// transitive closure
	// three closures in one
    }

}
