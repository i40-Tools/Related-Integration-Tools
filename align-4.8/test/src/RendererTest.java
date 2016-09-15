/*
 * $Id: RendererTest.java 2087 2015-10-23 08:29:01Z euzenat $
 *
 * Copyright (C) INRIA, 2009-2015
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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Alignment;

import fr.inrialpes.exmo.align.parser.AlignmentParser;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.impl.renderer.COWLMappingRendererVisitor;
import fr.inrialpes.exmo.align.impl.renderer.HTMLRendererVisitor;
import fr.inrialpes.exmo.align.impl.renderer.HTMLMetadataRendererVisitor;
import fr.inrialpes.exmo.align.impl.renderer.JSONRendererVisitor;
import fr.inrialpes.exmo.align.impl.renderer.OWLAxiomsRendererVisitor;
import fr.inrialpes.exmo.align.impl.renderer.SEKTMappingRendererVisitor;
import fr.inrialpes.exmo.align.impl.renderer.SKOSRendererVisitor;
import fr.inrialpes.exmo.align.impl.renderer.SWRLRendererVisitor;
import fr.inrialpes.exmo.align.impl.renderer.XMLMetadataRendererVisitor;
import fr.inrialpes.exmo.align.impl.renderer.XSLTRendererVisitor;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.Properties;

public class RendererTest {

    private Alignment alignment = null;
    private ObjectAlignment oalignment = null;

    private boolean valueSimilarTo( int obtained, int expected ) {
	if ( (expected-1 <= obtained) && (obtained <= expected+1) ) return true;
	else return false;
    }

    // Read the alignement that will be rendered by everyone
    @BeforeClass(groups = { "full", "impl", "raw" })
    private void init() throws Exception {
	AlignmentParser aparser = new AlignmentParser( 0 );
        assertNotNull( aparser );
        aparser.initAlignment( null );
        alignment = aparser.parse( "file:test/output/bibref2.rdf" );
        assertNotNull( alignment );
	assertEquals( alignment.nbCells(), 32);
	oalignment = ObjectAlignment.toObjectAlignment( (URIAlignment)alignment );
    }

    @Test(groups = { "full", "impl", "raw" })
    public void RDFrenderer() throws Exception {
	ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
	PrintWriter writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	AlignmentVisitor renderer = new RDFRendererVisitor( writer );
	alignment.render( renderer );
	writer.flush();
	writer.close();
	//System.err.println( stream.toString() );
	assertTrue( valueSimilarTo( stream.toString().length(), 11638 ), "Rendered differently: expected "+11638+" but was "+stream.toString().length() );
	Properties params = new Properties();
	params.setProperty( "embedded", "1");
    }

    @Test(groups = { "full", "impl", "raw" })
    public void SKOSrenderer() throws Exception {
	ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
	PrintWriter writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	AlignmentVisitor renderer = new SKOSRendererVisitor( writer );
	alignment.render( renderer );
	writer.flush();
	writer.close();
	assertTrue( valueSimilarTo( stream.toString().length(), 6705 ), "Rendered differently: expected "+6705+" but was "+stream.toString().length() );
	Properties params = new Properties();
	params.setProperty( "embedded", "1");
	stream = new ByteArrayOutputStream();
	writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	renderer = new SKOSRendererVisitor( writer );
	renderer.init( params );
	alignment.render( renderer );
	writer.flush();
	writer.close();
	assertTrue( valueSimilarTo( stream.toString().length(), 6650 ), "Rendered differently: expected "+6650+" but was "+stream.toString().length() );
	params.setProperty( "pre2008", "1");
	stream = new ByteArrayOutputStream(); 
	writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	renderer = new SKOSRendererVisitor( writer );
	renderer.init( params );
	alignment.render( renderer );
	writer.flush();
	writer.close();
	//System.err.println( stream.toString() );
	assertTrue( valueSimilarTo( stream.toString().length(), 6559 ), "Rendered differently: expected "+6559+" but was "+stream.toString().length() );
    }

    @Test(groups = { "full", "impl", "raw" })
    public void OWLrenderer() throws Exception {
	ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
	PrintWriter writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	AlignmentVisitor renderer = new OWLAxiomsRendererVisitor( writer );
	oalignment.render( renderer ); // test error with alignment
	writer.flush();
	writer.close();
	//System.err.println( stream.toString() );
	assertTrue( valueSimilarTo( stream.toString().length(), 6818 ), "Rendered differently: expected "+6818+" but was "+stream.toString().length() );
    }

    @Test(groups = { "full", "impl", "raw" })
    public void SEKTMappingrenderer() throws Exception {
	// not really
	ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
	PrintWriter writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	AlignmentVisitor renderer = new SEKTMappingRendererVisitor( writer );
	oalignment.render( renderer ); // test error with alignment
	writer.flush();
	writer.close();
	//System.err.println( stream.toString() );
	assertTrue( valueSimilarTo( stream.toString().length(), 5966 ), "Rendered differently: expected "+5966+" but was "+stream.toString().length() );
    }

    @Test(groups = { "full", "impl", "raw" })
    public void SWRLrenderer() throws Exception {
	ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
	PrintWriter writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	AlignmentVisitor renderer = new SWRLRendererVisitor( writer );
	oalignment.render( renderer ); // test error with alignment
	writer.flush();
	writer.close();
	//System.err.println( stream.toString() );
	assertTrue( valueSimilarTo( stream.toString().length(), 33720 ), "Rendered differently: expected "+33720+" but was "+stream.toString().length() );
    }

    @Test(groups = { "full", "impl", "raw" })
    public void XSLTrenderer() throws Exception {
	ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
	PrintWriter writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	AlignmentVisitor renderer = new XSLTRendererVisitor( writer );
	alignment.render( renderer );
	writer.flush();
	writer.close();
	//System.err.println( stream.toString() );
	assertTrue( valueSimilarTo( stream.toString().length(), 6164 ), "Rendered differently: expected "+6164+" but was "+stream.toString().length() );
    }

    @Test(groups = { "full", "impl", "raw" })
    public void COWLrenderer() throws Exception {
	ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
	PrintWriter writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	AlignmentVisitor renderer = new COWLMappingRendererVisitor( writer );
	oalignment.render( renderer ); // test error with alignment
	writer.flush();
	writer.close();
	//System.err.println( stream.toString() );
	assertTrue( valueSimilarTo( stream.toString().length(), 12844 ), "Rendered differently: expected "+12844+" but was "+stream.toString().length() );
    }

    @Test(groups = { "full", "impl", "raw" })
    public void HTMLrenderer() throws Exception {
	ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
	PrintWriter writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	AlignmentVisitor renderer = new HTMLRendererVisitor( writer );
	alignment.render( renderer );
	writer.flush();
	writer.close();
	//System.err.println( stream.toString() );
	assertTrue( valueSimilarTo( stream.toString().length(), 16467 ), "Rendered differently: expected "+16467+" but was "+stream.toString().length() );
    }

    @Test(groups = { "full", "impl", "raw" })
    public void HTMLMetadatarenderer() throws Exception {
	ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
	PrintWriter writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	AlignmentVisitor renderer = new HTMLMetadataRendererVisitor( writer );
	alignment.render( renderer );
	writer.flush();
	writer.close();
	//System.err.println( stream.toString() );
	assertTrue( valueSimilarTo( stream.toString().length(), 2111 ), "Rendered differently: expected "+2111+" but was "+stream.toString().length() );
    }

    @Test(groups = { "full", "impl", "raw" })
    public void JSONrenderer() throws Exception {
	ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
	PrintWriter writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	AlignmentVisitor renderer = new JSONRendererVisitor( writer );
	alignment.render( renderer );
	writer.flush();
	writer.close();
	//System.err.println( stream.toString() );
	assertTrue( valueSimilarTo( stream.toString().length(), 9540 ), "Rendered differently: expected "+9540+" but was "+stream.toString().length() );
    }

    @Test(groups = { "full", "impl", "raw" })
    public void XMLMetadatarenderer() throws Exception {
	ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
	PrintWriter writer = new PrintWriter (
			  new BufferedWriter(
			  new OutputStreamWriter( stream, "UTF-8" )), true);
	AlignmentVisitor renderer = new XMLMetadataRendererVisitor( writer );
	alignment.render( renderer );
	writer.flush();
	writer.close();
	//System.err.println( stream.toString() );
	assertTrue( valueSimilarTo( stream.toString().length(), 860 ), "Rendered differently: expected "+860+" but was "+stream.toString().length() );
    }


    @Test(groups = { "full", "impl", "raw" }, dependsOnMethods = {"XMLMetadatarenderer"})
    public void XMLMetadatarendererNewStyle() throws Exception {
	AlignmentParser aparser = new AlignmentParser( 0 );
        assertNotNull( aparser );
        aparser.initAlignment( null );
        alignment = aparser.parse( "file:examples/rdf/newsample.rdf" );
        assertNotNull( alignment );
	ByteArrayOutputStream stream = new ByteArrayOutputStream(); 
	PrintWriter writer = new PrintWriter (
			  new BufferedWriter(
			  new OutputStreamWriter( stream, "UTF-8" )), true);
	AlignmentVisitor renderer = new XMLMetadataRendererVisitor( writer );
	Properties p = new Properties();
	p.setProperty( "newstyle", "true" );
	renderer.init( p );
	alignment.render( renderer );
	writer.flush();
	writer.close();
	//System.err.println( stream.toString() );
	assertTrue( valueSimilarTo( stream.toString().length(), 1139 ), "Rendered differently: expected "+1139+" but was "+stream.toString().length() );
    }

}
