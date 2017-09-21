/*
 * $Id: SEKTMappingRendererVisitor.java 2116 2016-09-19 08:38:32Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2005, 2007-2010, 2012-2016
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
import java.util.Random;
import java.io.PrintWriter;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;

import fr.inrialpes.exmo.ontowrap.LoadedOntology;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

/**
 * Renders an alignment as a new ontology merging these.
 *
 * @author Jérôme Euzenat
 * @version $Id: SEKTMappingRendererVisitor.java 2116 2016-09-19 08:38:32Z euzenat $ 
 */

public class SEKTMappingRendererVisitor extends IndentedRendererVisitor implements AlignmentVisitor {
    final static Logger logger = LoggerFactory.getLogger(SEKTMappingRendererVisitor.class);

    Alignment alignment = null;
    LoadedOntology<Object> onto1 = null;
    LoadedOntology<Object> onto2 = null;
    Cell cell = null;
    // I hate using random generator for generating symbols (address would be better)
    Random generator = null;

    public SEKTMappingRendererVisitor( PrintWriter writer ){
	super(  writer );
	generator = new Random();
    }

    public void init( Properties p ) {
	super.init( p );
    };

    public void visit( Alignment align ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, align, Alignment.class ) ) return;
	// default behaviour
	if ( align instanceof ObjectAlignment ) {
	    alignment = align;
	} else {
	    try {
		alignment = ObjectAlignment.toObjectAlignment( (URIAlignment)align );
	    } catch ( AlignmentException alex ) {
		throw new AlignmentException("SEKTMappingRenderer: cannot render simple alignment. Need an ObjectAlignment", alex );
	    }
	}
	onto1 = ((ObjectAlignment)alignment).getOntologyObject1();
	onto2 = ((ObjectAlignment)alignment).getOntologyObject2();
	indentedOutputln("MappingDocument(<\""+"\">");
	increaseIndent();
	indentedOutputln("source(<\""+onto1.getURI()+"\">)");
	indentedOutputln("target(<\""+onto2.getURI()+"\">)");

	for( Cell c : alignment ){
	    c.accept( this );
	} //end for
	decreaseIndent();
	indentedOutputln(")");
    }
    public void visit( Cell cell ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, cell, Cell.class ) ) return;
	// default behaviour
	this.cell = cell;
	String id = String.format( "s%06d", generator.nextInt(100000) );
	Object ob1 = cell.getObject1();
	Object ob2 = cell.getObject2();
	try {
	    String direction = getRelationDirection( cell.getRelation() );
	    if ( direction == null ) throw new AlignmentException( "Cannot render relation "+cell.getRelation() );
	    if ( onto1.isClass( ob1 ) ) {
		indentedOutputln("classMapping( <\"#"+id+"\">");
		increaseIndent();
		indentedOutputln(direction);
		indentedOutputln("<\""+onto1.getEntityURI( ob1 )+"\">");
		indentedOutputln("<\""+onto2.getEntityURI( ob2 )+"\">");
		decreaseIndent();
		indentedOutputln(")");
	    } else if ( onto1.isDataProperty( ob1 ) ) {
		indentedOutputln("relationMapping( <\"#"+id+"\">");
		increaseIndent();
		indentedOutputln(direction);
		indentedOutputln("<\""+onto1.getEntityURI( ob1 )+"\">");
		indentedOutputln("<\""+onto2.getEntityURI( ob2 )+"\">");
		decreaseIndent();
		indentedOutputln(")");
	    } else if ( onto1.isObjectProperty( ob1 ) ) {
		indentedOutputln("attributeMapping( <\"#"+id+"\">");
		increaseIndent();
		indentedOutputln(direction);
		indentedOutputln("<\""+onto1.getEntityURI( ob1 )+"\">");
		indentedOutputln("<\""+onto2.getEntityURI( ob2 )+"\">");
		decreaseIndent();
		indentedOutputln(")");
	    } else if ( onto1.isIndividual( ob1 ) ) {
		indentedOutputln("instanceMapping( <\"#"+id+"\">");
		increaseIndent();
		indentedOutputln(direction);
		indentedOutputln("<\""+onto1.getEntityURI( ob1 )+"\">");
		indentedOutputln("<\""+onto2.getEntityURI( ob2 )+"\">");
		decreaseIndent();
		indentedOutputln(")");
	    }
	    outputln();
	} catch ( OntowrapException owex ) {
	    throw new AlignmentException( "Cannot find entity URI", owex );
	}
    }

    public static String getRelationDirection( Relation rel ) throws AlignmentException {
	// Not really semantically meaningfull
	if ( RelationTransformer.isEquivalence( rel ) ) return "bidirectional";
	else if ( RelationTransformer.isSubsumedOrEqual( rel ) ) return "unidirectional";
	else if ( RelationTransformer.subsumesOrEqual( rel ) ) return "unidirectional";
	else if ( RelationTransformer.isDisjoint( rel ) ) return "unidirectional";
	// COWL?
	// isInstanceOf(), hasInstance()
	else return null;
    }

    public void visit( Relation rel ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, rel, Relation.class ) ) return;
	// default behaviour
	throw new AlignmentException( "Cannot render generic relation "+rel );
    }

}
