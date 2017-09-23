/*
 * $Id: EDOALCell.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Sourceforge version 1.2 - 2008
 * Copyright (C) INRIA, 2007-2011, 2015
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package fr.inrialpes.exmo.align.impl.edoal;

import java.util.Set;
import java.util.HashSet;
import java.net.URI;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.align.impl.BasicCell;

import fr.inrialpes.exmo.align.parser.TypeCheckingVisitor;

/**
 * This implements an ontology correspondence when it
 * is a correspondence from the EDOAL Mapping Language.
 * 
 * @author Jérôme Euzenat
 * @version $Id: EDOALCell.java 2102 2015-11-29 10:29:56Z euzenat $ 
 */

public class EDOALCell extends BasicCell {

    private URI id; // This is the id

    private Set<Transformation> transformations;
    private Set<Linkkey> linkkeys;

    public void accept( AlignmentVisitor visitor) throws AlignmentException {
        visitor.visit( this );
    }

    /**
     * Creation
     *
     * @param id: the identifier of the correspondence (may be null)
     * @param ob1 and
     * @param ob2: the two EDOAL expressions related by the correspondence
     * @param rel: the relation between the objects
     * @param m: the confidence measure in the correspondence
     * @throws AlignmentException when something goes wrong (e.g., confidence out of bounds)
     **/
    public EDOALCell( String id, Expression ob1, Expression ob2, Relation rel, double m ) throws AlignmentException {
	super( id, (Object)ob1, (Object)ob2, rel, m );
    };

    public void accept(TypeCheckingVisitor visitor) throws AlignmentException {
	visitor.visit(this);
    }

    public URI getObject1AsURI( Alignment al ) throws AlignmentException {
	if ( object1 instanceof Id ) return ((Id)object1).getURI();
	else return null;
	//throw new AlignmentException( "Cannot convert to URI "+object1 );
    }
    public URI getObject2AsURI( Alignment al ) throws AlignmentException {
	if ( object2 instanceof Id ) return ((Id)object2).getURI();
	else return null;
	//throw new AlignmentException( "Cannot convert to URI "+object2 );
    }

    public void addTransformation( Transformation trs ){
	if ( transformations == null ) {
	    transformations = new HashSet<Transformation>();
	}
	transformations.add( trs );
    }

    /**
     * May be null
     * 
     * @return the set of transformations attached to this correspondence
     */
    public Set<Transformation> transformations() {
	return transformations;
    }
    
    public void addLinkkey( Linkkey linkkey ){
	if ( linkkeys == null ) {
	    linkkeys = new HashSet<Linkkey>();
	}
	linkkeys.add( linkkey );
    }
    /**
     * May be null
     * 
     * @return the set of link keys attached to this correspondence
     */
    public Set<Linkkey> linkkeys() {
	return linkkeys;
    }

    public EDOALCell inverse() throws AlignmentException {
	EDOALCell invcell = new EDOALCell( (String)null, (Expression)object2, (Expression)object1, relation.inverse(), strength );
	if ( transformations != null ) {
	    for ( Transformation trsf : transformations ) {
		invcell.addTransformation( trsf.inverse() );
	    }
	}
	if ( linkkeys != null ) {
	    for ( Linkkey lk : linkkeys ) {
		invcell.addLinkkey( lk.inverse() );
	    }
	}
	return invcell;
    }

}

