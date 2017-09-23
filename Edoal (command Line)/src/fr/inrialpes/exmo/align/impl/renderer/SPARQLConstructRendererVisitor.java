/*
 * $Id: SPARQLConstructRendererVisitor.java 2116 2016-09-19 08:38:32Z euzenat $
 *
 * Copyright (C) INRIA, 2012-2016
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

import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.edoal.EDOALAlignment;
import fr.inrialpes.exmo.align.impl.edoal.Expression;
import fr.inrialpes.exmo.align.impl.edoal.Linkkey;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyBinding;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyEquals;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyIntersects;

public class SPARQLConstructRendererVisitor extends GraphPatternRendererVisitor implements AlignmentVisitor {
    final static Logger logger = LoggerFactory.getLogger(SPARQLConstructRendererVisitor.class);

    Alignment alignment = null;
    Cell cell = null;
    Hashtable<String, String> nslist = null;

    private String namedGraph = null;

    boolean embedded = false;

    //boolean oneway = false; // JE2015: This is useless for construct: one should use invert() instead...

    boolean edoal = false;

    boolean requestedblanks = false;

    private String content_Corese = "";                     // resulting string for Corese

    public SPARQLConstructRendererVisitor(PrintWriter writer) {
        super(writer);
    }

    public void setGraphName( String name ) {
	namedGraph = name;
    }

    /**
     * Initialises the parameters of the renderer
     */
    public void init(Properties p) {
	super.init( p );
        if ( p.getProperty("graphName") != null ) {
            setGraphName( p.getProperty("graphName") );
        }

        if (p.getProperty("embedded") != null
                && !p.getProperty("embedded").equals("")) {
            embedded = true;
        }
        //if (p.getProperty("oneway") != null && !p.getProperty("oneway").equals("")) {
        //    oneway = true;
        //}
        if (p.getProperty("blanks") != null && !p.getProperty("blanks").equals("")) {
            requestedblanks = true;
        }
        if (p.getProperty("weakens") != null && !p.getProperty("weakens").equals("")) {
            weakens = true;
        }
        if (p.getProperty("ignoreerrors") != null && !p.getProperty("ignoreerrors").equals("")) {
            ignoreerrors = true;
        }
        if (p.getProperty("corese") != null && !p.getProperty("corese").equals("")) {
            corese = true;
        }

        split((p.getProperty("split") != null && !p.getProperty("split").equals("")), p.getProperty("dir") + "/");
    }

    public void visit(Alignment align) throws AlignmentException {
        if (subsumedInvocableMethod(this, align, Alignment.class)) {
            return;
        }
        if (align instanceof EDOALAlignment) {
            alignment = align;
        } else {
            try {
                alignment = EDOALAlignment.toEDOALAlignment((BasicAlignment) align);
            } catch (AlignmentException alex) {
                throw new AlignmentException("SPARQLConstructRenderer: cannot render simple alignment. Need an EDOALAlignment", alex);
            }
        }
        edoal = alignment.getLevel().startsWith("2EDOAL");
        content_Corese = "<?xml version=\"1.0\" encoding=\""+ENC+"\"?>" + NL;
        content_Corese += "<!DOCTYPE rdf:RDF [" + NL;
        content_Corese += "<!ENTITY rdf \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" + NL;
        content_Corese += "<!ENTITY rdfs \"http://www.w3.org/2000/01/rdf-schema#\">" + NL;
        content_Corese += "<!ENTITY rul \"http://ns.inria.fr/edelweiss/2011/rule#\">" + NL;
        content_Corese += "]>" + NL;
        content_Corese += "<rdf:RDF xmlns:rdfs=\"&rdfs;\" xmlns:rdf=\"&rdf;\" xmlns = \'&rul;\' >" + NL + NL + NL;
        for (Cell c : alignment) {
            c.accept(this);
        };
        content_Corese += "</rdf:RDF>" + NL;
        if (corese) {
            saveQuery(align, content_Corese);
        }
    }

    public void visit(Cell cell) throws AlignmentException {
        if (subsumedInvocableMethod(this, cell, Cell.class)) return;
        // default behaviour
        this.cell = cell;
        URI u1 = cell.getObject1AsURI(alignment);
        URI u2 = cell.getObject2AsURI(alignment);
	Relation rel = cell.getRelation();
	if ( ( RelationTransformer.isEquivalence( rel ) || RelationTransformer.isSubsumedOrEqual( rel ) )
	     && ( edoal || (u1 != null && u2 != null) ) ) {
	    generateConstruct(cell, (Expression) (cell.getObject1()), (Expression) (cell.getObject2()));
	    //if (!oneway) {
	    //    generateConstruct(cell, (Expression) (cell.getObject2()), (Expression) (cell.getObject1()));
	    //}
        }
    }

    public void visit(Relation rel) throws AlignmentException {
        if (subsumedInvocableMethod(this, rel, Relation.class)) {
            return;
        }
        // default behaviour
        // rel.write( writer );
    }

    // No use of Link keys with construct
    public void visit(final Linkkey linkkey) {}
    public void visit(final LinkkeyEquals linkkeyEquals) {}
    public void visit(final LinkkeyIntersects linkkeyIntersects) {}

    protected void generateConstruct( Cell cell, Expression expr1, Expression expr2 ) throws AlignmentException {
        // Here the generation is dependent on global variables
        blanks = true;
        resetVariables( expr2, "s", "o" );
        expr2.accept( this );
        String GP2 = getGP();
        List<String> listGP2 = new ArrayList<String>( getBGP() );
        blanks = requestedblanks; // ??
        resetVariables( expr1, "s", "o" );
        expr1.accept( this );
        String GP1 = getGP();
        // End of global variables
        String query = "";
        if ( !GP2.contains("UNION") && !GP2.contains("FILTER") ) {
            query = createConstruct( GP1, GP2 );
            if ( corese ) {
                content_Corese += createCoreseQuery( query );
            }
        } else if ( weakens ) {
            String tmp = "";
            for( String str : listGP2 ) {
                if ( !str.contains("UNION") && !str.contains("FILTER") ) {
                    tmp += str;
                }
            }
            if ( !tmp.equals("") ) {
                query = createConstruct( tmp, GP1 );
            }
        } else if ( ignoreerrors ) {
            query = createConstruct( GP1, GP2 );
        }
        if ( corese ) return;
        saveQuery( cell, query );
    }

    protected String createConstruct( String GP1, String GP2 ) {
        if (namedGraph == null) {
            return createPrefixList() + "CONSTRUCT {" + NL + GP2 + "}" + NL + "WHERE {" + NL + GP1 + "}" + NL;
        } else {
            return createPrefixList() + "CONSTRUCT {" + NL + GP2 + "}" + NL + "WHERE {" + NL + "GRAPH <" + namedGraph + "> {"+ NL + GP1 + "}" + NL + "}" + NL;
        }
    }

    protected String createCoreseQuery(String query) {
        return "<rule>" + NL + "<body>" + NL + "<![CDATA[" + NL + query + "]]>" + NL + "</body>" + NL + "</rule>" + NL + NL;
    }

}
