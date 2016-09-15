/*
 * $Id: IncompatRelation.java 2071 2015-10-04 06:42:17Z euzenat $
 *
 * Copyright (C) INRIA, 2004, 2008, 2011-2012, 2015
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

package fr.inrialpes.exmo.align.impl.rel;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.align.impl.BasicRelation;

import fr.inrialpes.exmo.align.parser.TypeCheckingVisitor;

/**
 * Represents an OWL equivalence relation.
 *
 * @version $Id: IncompatRelation.java 2071 2015-10-04 06:42:17Z euzenat $
 */

public class IncompatRelation extends BasicRelation
{
    public void accept( AlignmentVisitor visitor) throws AlignmentException {
        visitor.visit( this );
    }
    public void accept( TypeCheckingVisitor visitor ) throws AlignmentException {
	visitor.visit(this);
    }

    static final String prettyLabel = "%";

    /** Creation **/
    public IncompatRelation(){
	super(prettyLabel);
    }

    private static IncompatRelation instance = null;

    public static IncompatRelation getInstance() {
	if ( instance == null ) instance = new IncompatRelation();
	return instance;
    }

    public Relation compose( Relation r ) {
    	if ( r instanceof EquivRelation || r instanceof SubsumedRelation )
	    return this;
    	return null;
    }
}


