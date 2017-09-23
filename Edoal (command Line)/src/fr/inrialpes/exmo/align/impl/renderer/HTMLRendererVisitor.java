/*
 * $Id: HTMLRendererVisitor.java 2113 2016-09-05 06:22:17Z euzenat $
 *
 * Copyright (C) INRIA, 2006-2010, 2012, 2014-2015
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

import java.util.Properties;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.io.PrintWriter;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.align.impl.Annotations;
import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.ObjectCell;
import fr.inrialpes.exmo.align.impl.edoal.EDOALAlignment;
import fr.inrialpes.exmo.align.impl.edoal.Expression;
import fr.inrialpes.exmo.align.impl.edoal.ClassExpression;
import fr.inrialpes.exmo.align.impl.edoal.PropertyExpression;
import fr.inrialpes.exmo.align.impl.edoal.RelationExpression;
import fr.inrialpes.exmo.align.impl.edoal.InstanceExpression;

import fr.inrialpes.exmo.ontowrap.LoadedOntology;

/**
 * Renders an alignment in HTML
 *
 * TODO:
 * - add CSS categories
 * - add resource chooser
 *
 * @author Jérôme Euzenat
 * @version $Id: HTMLRendererVisitor.java 2113 2016-09-05 06:22:17Z euzenat $ 
 */

public class HTMLRendererVisitor extends HTMLMetadataRendererVisitor implements AlignmentVisitor {
    final static Logger logger = LoggerFactory.getLogger(SKOSRendererVisitor.class);
    
    Cell cell = null;
    String alid = "";

    public HTMLRendererVisitor( PrintWriter writer ){
	super( writer );
    }

    public void visit( Alignment align ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, align, Alignment.class ) ) return;
	// default behaviour
	alignment = align;
	printHeaders( align );
	indentedOutputln("<body>");
	indentedOutputln("<div typeof=\"align:Alignment\">");
	increaseIndent();
	printAlignmentMetadata( align );
	indentedOutputln("<h2>Correspondences ("+align.nbCells()+")</h2>");
	if ( align instanceof EDOALAlignment ) {
	    indentedOutputln("<p style=\"font-size: 80%\">EDOAL alignments are not deeply displayed in HTML, some expressions are displayed as [[ EDOAL ]]. Use RDF renderer to see the actual alignment.</p>");
	    
	}
	indentedOutputln("<div rel=\"align:map\">");
	increaseIndent();
	indentedOutputln("<table>");
	increaseIndent();
	indentedOutputln("<tr><td>object1</td><td>relation</td><td>strength</td><td>object2</td><td>Id</td></tr>");
	if ( align instanceof BasicAlignment ) {
	    for( Cell c : ((BasicAlignment)align).getSortedIterator() ) {
		c.accept( this );
	    }
	} else {
	    for( Cell c : align ){
		c.accept( this );
	    }
	} //end for
	decreaseIndent();
	indentedOutputln("</table>");
	decreaseIndent();
	indentedOutputln("</div>");
	decreaseIndent();
	indentedOutputln("</div>");
	decreaseIndent();
	indentedOutputln("</body>");
	decreaseIndent();
	indentedOutputln("</html>");
    }

    private String renderObject( URI u, Object o ) {
	if ( u != null ) return u.toString();
	else if ( o instanceof Expression ) {
	    if ( o instanceof ClassExpression ) return "[[ EDOAL Class expression ]]";
	    else if ( o instanceof PropertyExpression ) return "[[ EDOAL Property expression ]]";
	    else if ( o instanceof RelationExpression ) return "[[ EDOAL Relation expression ]]";
	    else if ( o instanceof InstanceExpression ) return "[[ EDOAL Instance expression ]]";
	    else return "[[ EDOAL Unknown expression ]]";
	} else return "** Unknown object **";
    }

    public void visit( Cell cell ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, cell, Cell.class ) ) return;
	// default behaviour
	this.cell = cell;
	String u1 = renderObject( cell.getObject1AsURI( alignment ), cell.getObject1() );
	String u2 = renderObject( cell.getObject2AsURI( alignment ), cell.getObject2() );
	indentedOutput("<tr typeof=\"align:Cell\">");
	output("<td rel=\"align:entity1\" href=\""+u1+"\">"+u1+"</td><td property=\"align:relation\">");
	cell.getRelation().accept( this );
	output("</td><td property=\"align:measure\" datatype=\"xsd:float\">"+cell.getStrength()+"</td>");
	output("<td rel=\"align:entity2\" href=\""+u2+"\">"+u2+"</td>");
	if ( cell.getId() != null ) {
	    String id = cell.getId();
	    // Would be useful to test for the Alignment URI
	    if ( alid != null && id.startsWith( alid ) ) {
		output("<td>"+id.substring( id.indexOf( '#' ) )+"</td>");
	    } else {
		output("<td>"+id+"</td>");
	    }
	} else output("<td></td>");
	//if ( !cell.getSemantics().equals("first-order") )
	//	indentedOutputln("<semantics>"+cell.getSemantics()+"</semantics>");
	outputln("</tr>");
    }

    public void visit( Relation rel ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, rel, Relation.class ) ) return;
	// default behaviour
	rel.write( writer );
    };
}
