/*
 * $Id: EDOALTest.java 1985 2014-11-09 16:50:18Z euzenat $
 *
 * Copyright (C) INRIA, 2008-2011, 2013-2014
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

import fr.inrialpes.exmo.align.impl.edoal.ClassId;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Alignment;

import fr.inrialpes.exmo.align.impl.edoal.EDOALAlignment;
import fr.inrialpes.exmo.align.impl.edoal.EDOALCell;
import fr.inrialpes.exmo.align.impl.edoal.Expression;
import fr.inrialpes.exmo.align.impl.edoal.Linkkey;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyBinding;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyEquals;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyIntersects;
import fr.inrialpes.exmo.align.impl.edoal.PathExpression;
import fr.inrialpes.exmo.align.impl.edoal.PropertyId;
import fr.inrialpes.exmo.align.impl.rel.EquivRelation;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.Set;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Relation;
import static org.testng.Assert.fail;

// JE2010: THIS SHOULD TEST ALL ERRORS RAISED BY CONSTRUCTORS
/**
 * These tests corresponds to the tests presented in the examples/omwg directory
 */
public class EDOALTest {

    private Alignment alignment = null;
    private AlignmentParser aparser1 = null;

    @Test(groups = {"full", "omwg", "raw"})
    public void loadPrintTest() throws Exception {
        /*
         java -cp ../../lib/procalign.jar fr.inrialpes.exmo.align.cli.ParserPrinter wine.xml > wine2.xml
         */
        aparser1 = new AlignmentParser(0);
        assertNotNull(aparser1);
        alignment = aparser1.parse("file:examples/omwg/wine.xml");
        assertNotNull(alignment);
        assertTrue(alignment instanceof EDOALAlignment);
        FileOutputStream stream = new FileOutputStream("test/output/wine2.xml");
        PrintWriter writer = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(stream, "UTF-8")), true);
        AlignmentVisitor renderer = new RDFRendererVisitor(writer);
        alignment.render(renderer);
        writer.flush();
        writer.close();
    }

    @Test(groups = {"full", "omwg", "raw"}, dependsOnMethods = {"loadPrintTest"})
    public void roundTripTest() throws Exception {
        /*
         java -cp ../../lib/procalign.jar fr.inrialpes.exmo.align.cli.ParserPrinter wine2.xml > wine3.xml
         */
        aparser1.initAlignment(null);
        alignment = aparser1.parse("file:test/output/wine2.xml");
        assertNotNull(alignment);
        FileOutputStream stream = new FileOutputStream("test/output/wine3.xml");
        PrintWriter writer = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(stream, "UTF-8")), true);
        AlignmentVisitor renderer = new RDFRendererVisitor(writer);
        alignment.render(renderer);
        writer.flush();
        writer.close();
    }

    /* diff wine2.xml wine3.xml */
    @Test(groups = {"full", "omwg", "raw"}, dependsOnMethods = {"roundTripTest"})
    public void diffTest() throws Exception {
        aparser1.initAlignment(null);
        Alignment oldal = aparser1.parse("file:test/output/wine2.xml");
        aparser1.initAlignment(null);
        alignment = aparser1.parse("file:test/output/wine3.xml");
        assertNotNull(alignment);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(stream, "UTF-8")), true);
        AlignmentVisitor renderer = new RDFRendererVisitor(writer);
        oldal.render(renderer);
        writer.flush();
        writer.close();
        String wine2 = stream.toString();
        stream = new ByteArrayOutputStream();
        writer = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(stream, "UTF-8")), true);
        renderer = new RDFRendererVisitor(writer);
        alignment.render(renderer);
        writer.flush();
        writer.close();
        assertEquals("".equals(wine2), false);
        // This tests the round triping
        assertEquals(wine2.length(), stream.toString().length());
        // This provides an absolute value
        assertEquals(wine2.length(), 5014);
        // This does not work because (at least) the order of correspondences is never the same...
        //assertEquals( wine2, stream.toString() );
    }

    /* This is round triping wrt converting to URIALignment...
     // Does not work anymore, because now it would try to parse an OMWG file... which goes to loop
     @Test(expectedExceptions = AlignmentException.class, groups = { "full", "omwg", "raw" }, dependsOnMethods = {"roundTripTest"})
     public void anotherRoundTripTest() throws Exception {
     aparser1.initAlignment( null );
     EDOALAlignment eal = (EDOALAlignment)aparser1.parse( "file:test/output/wine2.xml" );
     assertNotNull( eal );
     assertEquals( eal.nbCells(), 5 ); 
     URIAlignment al = eal.toURIAlignment();
     assertNotNull( al );
     assertEquals( al.nbCells(), 3 );
     eal = EDOALAlignment.toEDOALAlignment( al ); // does not work because the ontology cannot be loaded!
     assertNotNull( eal );
     assertEquals( eal.nbCells(), 3 );
     } */
    @Test(groups = {"full", "omwg", "raw"}, dependsOnMethods = {"roundTripTest"})
    public void linkkeyBindingTest() throws Exception {
        PathExpression expression1 = new PropertyId(new URI("http://exmo.inria.fr/RootElement1"));
        PathExpression expression2 = new PropertyId(new URI("http://exmo.inria.fr/RootElement2"));

        LinkkeyBinding linkkeyBinding = null;
        
        linkkeyBinding = new LinkkeyEquals(expression1, expression2);
        assertEquals(linkkeyBinding.getExpression1(), expression1);
        assertEquals(linkkeyBinding.getExpression2(), expression2);
        
        linkkeyBinding = new LinkkeyIntersects(expression1, expression2);
        assertEquals(linkkeyBinding.getExpression1(), expression1);
        assertEquals(linkkeyBinding.getExpression2(), expression2);
    }

    @Test(groups = {"full", "omwg", "raw"}, dependsOnMethods = {"linkkeyBindingTest"})
    public void linkkeyTest() throws Exception {
        Relation relation = new EquivRelation();
        Expression expression1 = new ClassId("http://exmo.inria.fr/RootElement1");
        Expression expression2 = new ClassId("http://exmo.inria.fr/RootElement2");
        EDOALCell cell = new EDOALCell("1", expression1, expression2, relation, 1.0);

        Linkkey linkkey = new Linkkey();
        //Tests on bindings
        LinkkeyBinding linkkeyBinding1 = new LinkkeyIntersects(new PropertyId(), new PropertyId());
        LinkkeyBinding linkkeyBinding2 = new LinkkeyEquals(new PropertyId(), new PropertyId());
        linkkey.addBinding(linkkeyBinding1);
        linkkey.addBinding(linkkeyBinding2);
        Set<LinkkeyBinding> bindings = linkkey.bindings();
        assertEquals(2, bindings.size());
        assertTrue(bindings.contains(linkkeyBinding1));
        assertTrue(bindings.contains(linkkeyBinding2));

        //Tests on type
        linkkey.setExtension("http://ns.inria.org/edoal/1.0/", "type", "weak");
        assertEquals("weak", linkkey.getExtension("http://ns.inria.org/edoal/1.0/", "type"));
    }
}
