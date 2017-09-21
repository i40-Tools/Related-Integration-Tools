/*
 * $Id: EDOALParserTest.java 2095 2015-11-20 15:13:45Z euzenat $
 *
 * Copyright (C) INRIA, 2009-2011, 2015
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

import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.impl.Extensions;
import fr.inrialpes.exmo.align.impl.edoal.EDOALAlignment;
import fr.inrialpes.exmo.align.impl.edoal.EDOALCell;
import fr.inrialpes.exmo.align.impl.edoal.Linkkey;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyBinding;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyEquals;
import fr.inrialpes.exmo.align.impl.edoal.PropertyId;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import fr.inrialpes.exmo.align.util.NullStream;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertFalse;

/**
 * These tests corresponds to the tests presented in the examples/omwg directory
 */
public class EDOALParserTest {

    private AlignmentParser aparser1 = null;
    protected Extensions extensions = null;
    

    @Test(groups = {"full", "omwg", "raw"})
    public void setUp() throws Exception {
        aparser1 = new AlignmentParser(0);
        assertNotNull(aparser1);
    }

    @Test(groups = {"full", "omwg", "raw"}, dependsOnMethods = {"setUp"})
    public void typedParsingTest() throws Exception {
        //AlignmentParser aparser2 = new AlignmentParser(2);
        aparser1.initAlignment(null);
        Alignment al = aparser1.parse("file:examples/omwg/total.xml");
        assertNotNull(al);
	assertEquals( al.nbCells(), 17 ); //Should be changed if new tests added...
    }

    @Test(expectedExceptions = AlignmentException.class, groups = {"full", "omwg", "raw"}, dependsOnMethods = {"setUp"})
    public void alignmentSameIdParsingTest() throws Exception {
        // Load the full test
        aparser1.initAlignment(null);
        aparser1.parse("file:test/input/alignment4repeatedId.rdf");
    }

    @Test(groups = {"full", "omwg", "raw"}, dependsOnMethods = {"setUp"})
    public void linkkeyParsingTest() throws Exception {
        // Load the full test
        aparser1.initAlignment(null);
        EDOALAlignment alignment = (EDOALAlignment) aparser1.parse("file:test/input/alignment2.rdf");
        assertNotNull(alignment);
        Enumeration<Cell> cells = alignment.getElements();
        assertTrue( cells.hasMoreElements() );
        EDOALCell cell = (EDOALCell)cells.nextElement();
        assertFalse(cells.hasMoreElements());

        Set<Linkkey> linkkeys = cell.linkkeys();
        assertEquals(linkkeys.size(), 1);
        Linkkey linkkey = linkkeys.iterator().next();
        //assertEquals(linkkey.getExtension("http://ns.inria.org/edoal/1.0/#", "type"), "weak");
	assertEquals(linkkey.getExtension("http://exmo.inrialpes.fr/align/service#", "type"), "weak");

        Set<LinkkeyBinding> bindings = linkkey.bindings();
        assertEquals(bindings.size(), 2);
        Iterator<LinkkeyBinding> bindingIter = bindings.iterator();
        LinkkeyBinding binding = bindingIter.next();
        LinkkeyBinding firstBinding = null;
        LinkkeyBinding secondBinding = null;
        if(binding instanceof LinkkeyEquals){
            firstBinding = binding;
            secondBinding =  bindingIter.next();
        }
        else{
            firstBinding = bindingIter.next();
            secondBinding =  binding;
            
        }
        assertEquals(((PropertyId)firstBinding.getExpression1()).getURI().toString(), "http://purl.org/ontology/mo/opus");
        assertEquals(((PropertyId)firstBinding.getExpression2()).getURI().toString(), "http://exmo.inrialpes.fr/connectors#number");
        
        assertEquals(((PropertyId)secondBinding.getExpression1()).getURI().toString(), "http://purl.org/ontology/mo/name");
        assertEquals(((PropertyId)secondBinding.getExpression2()).getURI().toString(), "http://exmo.inrialpes.fr/connectors#nom");
    }
    
    @Test(expectedExceptions = AlignmentException.class, groups = {"full", "omwg", "raw"}, dependsOnMethods = {"linkkeyParsingTest"})
    public void linkkeyIncorrectParsingTest() throws Exception {
        // Load the full test
        aparser1.initAlignment(null);
        aparser1.parse("file:test/input/alignment4incorrect.rdf");
    }

    @Test(groups = {"full", "omwg", "raw"}, dependsOnMethods = {"linkkeyIncorrectParsingTest"})
    public void linkkeyAlgebraParsingTest() throws Exception {
        // Load the full test
        aparser1.initAlignment(null);
        EDOALAlignment alignment = (EDOALAlignment) aparser1.parse("file:test/input/alignment4.rdf");
        assertNotNull(alignment);
	assertEquals( alignment.nbCells(), 2 );
	assertEquals( alignment.getRelationType(), A5AlgebraRelation.class );

	// Check the class of relations
	//
	/*
        Enumeration<Cell> cells = alignment.getElements();
        assertTrue( cells.hasMoreElements() );
        EDOALCell cell = (EDOALCell)cells.nextElement();
        assertFalse(cells.hasMoreElements());

        Set<Linkkey> linkkeys = cell.linkkeys();
        assertEquals(linkkeys.size(), 1);
        Linkkey linkkey = linkkeys.iterator().next();
        //assertEquals(linkkey.getExtension("http://ns.inria.org/edoal/1.0/#", "type"), "weak");
	assertEquals(linkkey.getExtension("http://exmo.inrialpes.fr/align/service#", "type"), "weak");

        Set<LinkkeyBinding> bindings = linkkey.bindings();
        assertEquals(bindings.size(), 2);
        Iterator<LinkkeyBinding> bindingIter = bindings.iterator();
        LinkkeyBinding binding = bindingIter.next();
        LinkkeyBinding firstBinding = null;
        LinkkeyBinding secondBinding = null;
        if(binding instanceof LinkkeyEquals){
            firstBinding = binding;
            secondBinding =  bindingIter.next();
        }
        else{
            firstBinding = bindingIter.next();
            secondBinding =  binding;
            
        }
        assertEquals(((PropertyId)firstBinding.getExpression1()).getURI().toString(), "http://purl.org/ontology/mo/opus");
        assertEquals(((PropertyId)firstBinding.getExpression2()).getURI().toString(), "http://exmo.inrialpes.fr/connectors#number");
        
        assertEquals(((PropertyId)secondBinding.getExpression1()).getURI().toString(), "http://purl.org/ontology/mo/name");
        assertEquals(((PropertyId)secondBinding.getExpression2()).getURI().toString(), "http://exmo.inrialpes.fr/connectors#nom");
	*/
    }
    
    @Test(groups = {"full", "omwg", "raw"}, dependsOnMethods = {"linkkeyAlgebraParsingTest"})
    public void roundTripTest() throws Exception {
        // Load the full test
        aparser1.initAlignment(null);
        Alignment alignment = aparser1.parse("file:examples/omwg/total.xml");
        assertNotNull(alignment);
        // Print it in a string
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(stream, "UTF-8")), true);
        AlignmentVisitor renderer = new RDFRendererVisitor(writer);
        alignment.render(renderer);
        writer.flush();
        writer.close();
        String str1 = stream.toString();
        // Read it again
        aparser1 = new AlignmentParser(0);
        aparser1.initAlignment(null);
        //System.err.println( str1 );
        Alignment al = aparser1.parseString(str1);
        assertEquals(alignment.nbCells(), al.nbCells());
        // Print it in another string
        stream = new ByteArrayOutputStream();
        writer = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(stream, "UTF-8")), true);
        renderer = new RDFRendererVisitor(writer);
        al.render(renderer);
        writer.flush();
        writer.close();
        String str2 = stream.toString();
	// They should be the same... (no because of invertion...)
        //assertEquals( str1, str2 );
        // But have the same length
        assertEquals(str1.length(), str2.length(), "STR 1 : \n " + str1 + "STR2 : \n" + str2);
    }

    @Test(groups = {"full", "omwg", "raw"}, dependsOnMethods = {"linkkeyAlgebraParsingTest"})
    public void transformationRoundTripTest() throws Exception {
        // Load the full test
        aparser1.initAlignment(null);
        Alignment alignment = aparser1.parse("file:examples/omwg/transf.rdf");
        assertNotNull(alignment);
	assertEquals( alignment.nbCells(), 5 );
	int nbTransf = 0;
	for ( Cell c : alignment ) {
	    //System.err.println( ">> "+c.getId() );
	    if ( ((EDOALCell)c).transformations() != null )
		nbTransf += ((EDOALCell)c).transformations().size();
	}
	assertEquals( nbTransf, 8 );
	// Find a cell with project 
        // Print it in a string
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(stream, "UTF-8")), true);
        AlignmentVisitor renderer = new RDFRendererVisitor(writer);
        alignment.render(renderer);
        writer.flush();
        writer.close();
        String str1 = stream.toString();
        // Read it again
        aparser1 = new AlignmentParser(0);
        aparser1.initAlignment(null);
        //System.err.println( str1 );
        Alignment al = aparser1.parseString(str1);
        assertEquals( alignment.nbCells(), al.nbCells());
	nbTransf = 0;
	for ( Cell c : al ) {
	    //System.err.println( ">> "+c.getId() );
	    if ( ((EDOALCell)c).transformations() != null )
		nbTransf += ((EDOALCell)c).transformations().size();
	}
	assertEquals( nbTransf, 8 );
        // Print it in another string
        stream = new ByteArrayOutputStream();
        writer = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(stream, "UTF-8")), true);
        renderer = new RDFRendererVisitor(writer);
        al.render(renderer);
        writer.flush();
        writer.close();
        String str2 = stream.toString();
	// They should be the same... (no because of invertion...)
        //assertEquals( str1, str2 );
        // But have the same length
        assertEquals(str1.length(), str2.length(), "STR 1 : \n " + str1 + "STR2 : \n" + str2);
    }

}
