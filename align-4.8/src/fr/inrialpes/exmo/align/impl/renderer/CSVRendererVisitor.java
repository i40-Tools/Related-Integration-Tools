/*
 * $Id: CSVRendererVisitor.java 2086 2015-10-23 08:21:09Z euzenat $
 *
 * Copyright (C) INRIA, 2015
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
import java.io.PrintWriter;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.align.impl.URIAlignment;

import fr.inrialpes.exmo.ontowrap.LoadedOntology;

/**
 * Renders an alignment in Comma-separated-value or Tab-separated-value
 *
 * - only works with URIAlignments
 *
 * @author Jérôme Euzenat
 * @version $Id: CSVRendererVisitor.java 2086 2015-10-23 08:21:09Z euzenat $ 
 */

public class CSVRendererVisitor extends GenericReflectiveVisitor implements AlignmentVisitor {
    final static Logger logger = LoggerFactory.getLogger(CSVRendererVisitor.class);

    String sep = ",";
    PrintWriter writer = null;
    Alignment alignment = null;
    Cell cell = null;

    public CSVRendererVisitor( PrintWriter writer ){
	this.writer = writer;
    }

    public void init( Properties p ) {
	if ( p.getProperty( "separator" ) != null 
	     && !p.getProperty( "separator" ).equals("") ) sep = p.getProperty( "separator" );
    };

    public void visit( Alignment align ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, align, Alignment.class ) ) return;
	// default behaviour
	alignment = align;
	if ( ! (align instanceof URIAlignment) ) {
	    throw new AlignmentException( "Only URIAlignments can be rendered in CSV" );
	}
	writer.print("\"id\""+sep+"\"object1\""+sep+"\"relation\""+sep+"\"strength\""+sep+"\"object2\"\n");
	for( Cell c : align ) { c.accept( this ); }
    }

    public void visit( Cell cell ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, cell, Cell.class ) ) return;
	this.cell = cell;
	URI u1 = cell.getObject1AsURI( alignment );
	URI u2 = cell.getObject2AsURI( alignment );
	String id = "";
	if ( cell.getId() != null ) { id = cell.getId(); }
	writer.print("\""+id+"\""+sep+"\""+u1+"\""+sep+"\"" );
	cell.getRelation().accept( this );
	writer.print("\""+sep+"\""+cell.getStrength()+"\""+sep+"\""+u2+"\"\n");
    }

    public void visit( Relation rel ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, rel, Relation.class ) ) return;
	rel.write( writer );
    };
}
