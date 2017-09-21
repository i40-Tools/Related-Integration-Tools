/*
 * $Id: IOTest.java 2140 2017-07-12 19:46:48Z euzenat $
 *
 * Copyright (C) INRIA, 2008-2011, 2014-2017
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
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

import java.util.Properties;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

import org.apache.commons.text.StringEscapeUtils;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.BasicCell;
import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;

/**
 * These tests corresponds to the tests presented in the examples/omwg directory
 */

public class IOTest {

    private Alignment alignment = null;
    private AlignmentParser aparser = null;

    @Test(groups = { "full", "io", "raw" }, expectedExceptions = AlignmentException.class)
    public void loadSOAPErrorTest() throws Exception {
	aparser = new AlignmentParser( 0 );
	assertNotNull( aparser );
	try { 	// shut-up log4j
	    org.apache.jena.rdf.model.impl.RDFDefaultErrorHandler.silent = true;
	    alignment = aparser.parse( "test/input/soap.xml" );
	    // error (we forgot to tell the parser that the alignment is embedded)
	} catch (Exception ex) {
	    org.apache.jena.rdf.model.impl.RDFDefaultErrorHandler.silent = false;
	    throw ex;
	}
    }

    @Test(groups = { "full", "io", "raw" }, dependsOnMethods = {"loadSOAPErrorTest"})
    public void loadSOAPTest() throws Exception {
	aparser.initAlignment( null );
	aparser.setEmbedded( true );
	alignment = aparser.parse( "file:test/input/soap.xml" );
	assertNotNull( alignment );
	assertTrue( alignment instanceof URIAlignment );
	assertEquals( alignment.getOntology2URI().toString(), "http://alignapi.gforge.inria.fr/tutorial/edu.mit.visus.bibtex.owl" );
	assertEquals( alignment.nbCells(), 57 );
    }

    private static String readFileAsString(String filePath) throws java.io.IOException {
	byte[] buffer = new byte[(int) new File(filePath).length()];
	BufferedInputStream f = new BufferedInputStream(new FileInputStream(filePath));
	f.read(buffer);
	return new String(buffer);
    }

    /**
     * The same tests as above (and elsewhere) using parseString instead of parse
     */
    @Test(groups = { "full", "io", "raw" }, expectedExceptions = AlignmentException.class)
    public void loadSOAPStringErrorTest() throws Exception {
	aparser = new AlignmentParser( 0 );
	assertNotNull( aparser );
	try { 	// shut-up log4j
	    org.apache.jena.rdf.model.impl.RDFDefaultErrorHandler.silent = true;
	    alignment = aparser.parseString( readFileAsString( "test/input/soap.xml" ) );
	    // error (we forgot to tell the parser that the alignment is embedded)
	} catch (Exception ex) {
	    org.apache.jena.rdf.model.impl.RDFDefaultErrorHandler.silent = false;
	    throw ex;
	}
    }

    @Test(groups = { "full", "io", "raw" }, dependsOnMethods = {"loadSOAPStringErrorTest"})
    public void loadStringTest() throws Exception {
	aparser.initAlignment( null );
	// a regular alignment, out of a SOAP message
	alignment = aparser.parseString( readFileAsString( "examples/rdf/newsample.rdf" ) );
	assertNotNull( alignment );
	assertTrue( alignment instanceof URIAlignment );
	assertEquals( alignment.nbCells(), 2 );
	double min = 1.;
	double max = 0.;
	for ( Cell c : alignment ) {
	    double v = c.getStrength();
	    if ( v < min ) min = v;
	    if ( v > max ) max = v;
	}
	assertEquals( min, 0.4666666666666667 );
	assertEquals( max, 1. );
    }

    @Test(groups = { "full", "io", "raw" }, dependsOnMethods = {"loadSOAPStringErrorTest"})
    public void loadSOAPStringTest() throws Exception {
	aparser.initAlignment( null );
	aparser.setEmbedded( true );
	alignment = aparser.parseString( readFileAsString( "test/input/soap.xml" ) );
	assertNotNull( alignment );
	assertTrue( alignment instanceof URIAlignment );
	assertEquals( alignment.getOntology2URI().toString(), "http://alignapi.gforge.inria.fr/tutorial/edu.mit.visus.bibtex.owl" );
	assertEquals( alignment.nbCells(), 57 );
    }

    private Alignment renderparse( Alignment al, String filename, String encoding) throws Exception {
	FileOutputStream stream = new FileOutputStream("test/output/"+filename+".rdf");
        //ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(stream, encoding)), true);
	RDFRendererVisitor renderer = new RDFRendererVisitor(writer);
        renderer.setIndentString("");	// Indent should be empty
        renderer.setNewLineString("");
        renderer.setEncoding(encoding);
        alignment.accept(renderer);
        writer.flush();
        writer.close();
        stream.close();
        //return stream.toString();
	aparser.initAlignment( null );
	return aparser.parse( "file:test/output/"+filename+".rdf" );
    }

    @Test(groups = { "full", "io", "raw" })
    public void loadUTF8AndElseTest() throws Exception {
	aparser.initAlignment( null );
	//	aparser.setEmbedded( true );
	// load encoding-utf.rdf
	alignment = aparser.parse( "file:examples/rdf/encoding-utf8.rdf" );
	assertNotNull( alignment );
	assertTrue( alignment instanceof URIAlignment );
	assertEquals( alignment.nbCells(), 3 );
	/*
	System.err.println("From UTF");
	for ( Cell c : alignment ) {
	    System.err.println( ((BasicCell)c).getObject1AsURI(alignment)+" == "+StringEscapeUtils.escapeXml10(((BasicCell)c).getObject1AsURI(alignment).toASCIIString())+" == "+java.net.URLDecoder.decode(((BasicCell)c).getObject1AsURI(alignment).toString(), "UTF-8") );
	    System.err.println( ((BasicCell)c).getObject2AsURI(alignment)+" == "+StringEscapeUtils.escapeXml10(((BasicCell)c).getObject2AsURI(alignment).toASCIIString())+" == "+java.net.URLDecoder.decode(((BasicCell)c).getObject2AsURI(alignment).toString(), "UTF-8") );
	    }
	*/
	// load encoding-iso.rdf
	aparser.initAlignment( null );
	Alignment refalign = aparser.parse( "file:examples/rdf/encoding-iso.rdf" );
	assertNotNull( refalign );
	assertTrue( refalign instanceof URIAlignment );
	assertEquals( refalign.nbCells(), 3 );

	// eval utf-8 against iso
	PRecEvaluator eval = new PRecEvaluator( refalign, alignment );
	assertNotNull( eval );
	eval.eval( new Properties() ) ;
	assertEquals( eval.getPrecision(), 1.0 );
	assertEquals( eval.getRecall(), 1.0 );

	// output RDF utf-8
	Alignment resutfutf = renderparse( alignment, "utfutf", "UTF-8" );
	assertNotNull( resutfutf );
	Alignment resisoutf = renderparse( refalign, "isoutf", "UTF-8" );
	assertNotNull( resisoutf );
	assertEquals( resutfutf.nbCells(), resisoutf.nbCells() );

	// compare them again
	eval = new PRecEvaluator( resutfutf, resisoutf );
	assertNotNull( eval );
	eval.eval( new Properties() ) ;
	assertEquals( eval.getPrecision(), 1.0 );
	assertEquals( eval.getRecall(), 1.0 );

	// output RDF iso
	Alignment resutfiso = renderparse( alignment, "utfiso", "iso-8859-1" );
	assertNotNull( resutfiso );
	Alignment resisoiso = renderparse( refalign, "isoutf", "iso-8859-1" );
	assertNotNull( resisoiso );
	assertEquals( resutfiso.nbCells(), resisoiso.nbCells() );

	// and again
	eval = new PRecEvaluator( resutfiso, resisoiso );
	assertNotNull( eval );
	eval.eval( new Properties() ) ;
	assertEquals( eval.getPrecision(), 1.0 );
	assertEquals( eval.getRecall(), 1.0 );
    }
}
