/*
 * $Id: SPARQLSelectRendererVisitor.java 2150 2017-07-18 15:15:46Z euzenat $
 *
 * Copyright (C) INRIA, 2012-2017
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
package fr.inrialpes.exmo.align.impl.renderer;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.edoal.EDOALAlignment;
import fr.inrialpes.exmo.align.impl.edoal.EDOALCell;
import fr.inrialpes.exmo.align.impl.edoal.Expression;
import fr.inrialpes.exmo.align.impl.edoal.Linkkey;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyBinding;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyEquals;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyIntersects;
//import static fr.inrialpes.exmo.align.impl.renderer.GraphPatternRendererVisitor.blanks;

import java.io.PrintWriter;
import java.net.URI;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class SPARQLSelectRendererVisitor extends GraphPatternRendererVisitor implements AlignmentVisitor {

    Alignment alignment = null;
    Cell cell = null;
    Hashtable<String, String> nslist = null;

    boolean embedded = false;
    boolean oneway = false;
    boolean split = false;
    String splitdir = "";
    private String onto1NamedGraph, onto2NamedGraph = null;

    boolean edoal = false;

    public SPARQLSelectRendererVisitor(PrintWriter writer) {
        super(writer);
    }

    public void setGraph1Name( String name ) {
	onto1NamedGraph = name;
    }

    public void setGraph2Name( String name ) {
	onto2NamedGraph = name;
    }

    public void init( Properties p ) {
	super.init( p );
        if ( p.getProperty( "graphName1" ) != null ) {
            setGraph1Name( p.getProperty("graphName1") );
        }
        if ( p.getProperty( "graphName2" ) != null ) {
            setGraph2Name( p.getProperty("graphName2") );
        }

        if (p.getProperty("embedded") != null && !p.getProperty("embedded").equals("")) {
            embedded = true;
        }
        if (p.getProperty("oneway") != null && !p.getProperty("oneway").equals("")) {
            oneway = true;
        }
        if (p.getProperty("blanks") != null && !p.getProperty("blanks").equals("")) {
            blanks = true;
        }
        if (p.getProperty("weakens") != null && !p.getProperty("weakens").equals("")) {
            weakens = true;
        }
        if (p.getProperty("ignoreerrors") != null && !p.getProperty("ignoreerrors").equals("")) {
            ignoreerrors = true;
        }
        split = (p.getProperty("split") != null && !p.getProperty("split").equals(""));
        if (p.getProperty("dir") != null && !p.getProperty("dir").equals("")) {
            splitdir = p.getProperty("dir") + "/";
        }
    }

    public void visit(Alignment align) throws AlignmentException {
        if ( corese ) {
            throw new AlignmentException("corese case NOT IMPLEMENTED for SPARQLSelectRendererVisitor !!");
        }
        if (subsumedInvocableMethod(this, align, Alignment.class)) {
            return;
        }
        if (align instanceof EDOALAlignment) {
            alignment = align;
        } else {
            try {
                alignment = EDOALAlignment.toEDOALAlignment((BasicAlignment) align);
            } catch (AlignmentException alex) {
                throw new AlignmentException("SPARQLSELECTRenderer: cannot render simple alignment. Need an EDOALAlignment", alex);
            }
        }
        edoal = alignment.getLevel().startsWith("2EDOAL");
        for (Cell c : alignment) {
            c.accept(this);
        };
    }

    public void visit(Cell cell) throws AlignmentException {
        if ( corese ) {
            throw new AlignmentException("corese case NOT IMPLEMENTED for SPARQLSelectRendererVisitor !!");
        }
        if (subsumedInvocableMethod(this, cell, Cell.class)) {
            return;
        }
        // default behaviour
        this.cell = cell;
        URI u1 = cell.getObject1AsURI(alignment);
	if ( edoal || u1 != null ) generateSelect( (Expression)(cell.getObject1()) );
	if (!oneway) {
	    URI u2 = cell.getObject2AsURI(alignment);
	    if ( edoal || u2 != null ) generateSelect( (Expression)(cell.getObject2()) );
	}
    }

    protected void generateSelect( Expression expr ) throws AlignmentException {
	resetVariables( expr, "s", "o" );
	expr.accept( this );
	String query = createPrefixList()+"SELECT * WHERE {"+NL+getGP()+"}"+NL;                 
	// Here we store expr because they may be two queries stored for the same cell
	// We should provide the way to retrive the query... getQuery1( cell ), getQuery2( cell )
        saveQuery( expr, query );
    }

    public void visit(Relation rel) throws AlignmentException {
        if (subsumedInvocableMethod(this, rel, Relation.class)) {
            return;
        }
    }

    public void visit(final Linkkey linkkey) throws AlignmentException {
        for (LinkkeyBinding linkkeyBinding : linkkey.bindings()) {
            linkkeyBinding.accept(this);
        }
    }

    /**
     * Linkkeys are irrelevant to SELECT queries
     *
     * @param linkkeyEquals: the link key to render
     * @throws AlignmentException when something goes wrong
     */
    public void visit(final LinkkeyEquals linkkeyEquals) throws AlignmentException {
	return;
    }

    /**
     * Linkkeys are irrelevant to SELECT queries
     *
     * @param linkkeyIntersects: the link key to render
     * @throws AlignmentException when something goes wrong
     */
    public void visit(final LinkkeyIntersects linkkeyIntersects) throws AlignmentException {
	return;
    }

    /* JE: shouldn't it be in Graph ***/
    protected void beginNamedGraph(String namedGraph) {
        if (namedGraph != null) {
            addToGP("GRAPH <" + namedGraph + "> {" + NL);
        }
    }

    protected void endNamedGraph(String namedGraph) {
        if (namedGraph != null) {
            addToGP("}" + NL);
        }
    }

    protected String createSelect(List<String> listGP, String filter) {
        StringBuilder mainGPBuilder = new StringBuilder();
        for (String GP : listGP) {
            mainGPBuilder.append(GP);
        }
        return createPrefixList() + "SELECT DISTINCT ?s1 ?s2 " + NL + "WHERE {" + NL + mainGPBuilder + filter + NL + "}" + NL;
    }
}
