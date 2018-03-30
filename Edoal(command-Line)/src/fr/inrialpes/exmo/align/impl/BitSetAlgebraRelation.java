/*
 * $Id: BitSetAlgebraRelation.java 2102 2015-11-29 10:29:56Z euzenat $
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

package fr.inrialpes.exmo.align.impl;

import org.semanticweb.owl.align.AlignmentException;

import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a dijunctive ontology alignment relation.
 * A disjunctive ontology alignment relation is made of the disjunction of several atomic relations.
 * This disjunction is traditionally represented as a set, here implemented as BitSets
 * 
 * @author Jérôme Euzenat
 * @version $Id: BitSetAlgebraRelation.java 2102 2015-11-29 10:29:56Z euzenat $
 */

public abstract class BitSetAlgebraRelation<T extends BaseRelation> extends BitSetDisjunctiveRelation<T> implements AlgebraRelation<T> {
    final static Logger logger = LoggerFactory.getLogger( BitSetAlgebraRelation.class );

    /** Creation **/
    protected BitSetAlgebraRelation(){
	super();
    }

    protected BitSetAlgebraRelation( BitSet rels ) {
	super( rels);
    }

    /**
     * composition
     *
     * @param dr: a bitset relation to compose with this one
     * @return the bitset corresponding to the composed relation
     */
    public BitSet protocompose( BitSetAlgebraRelation<T> dr ) {
	BitSet bs = createSet();
	for ( T br : this ) {
	    for ( T br2 : dr ) {
		bs.or( compose( br, br2 ) );
	    }
	}
	return bs;
    }

    public AlgebraRelation<T> compose( AlgebraRelation<T> dr ) throws AlignmentException {
	throw new AlignmentException( "Cannot compose relations from heterogeneous algebras" );
    }

    protected abstract BitSet compose( T r1, T r2 );

    /**
     * inverse, could potentially go to Disjunctive
     *
     * @return the bitset corresponding to the relation inverse to this one
     */
    protected BitSet protoinverse() {
	BitSet bs = createSet();
	for( T r : this ) {
	    bs.set( r.getInverse().getIndex() );
	}
	return bs;
    }

    /* This is necessary to avoid that BasicRelation.inverse()
       be taken by the compiler for implementing AlgebraRelation.inverse()
     */
    public abstract BitSetAlgebraRelation<T> inverse();

    /**
     * They should be part of the interface (AlgebraRelation)
     * However, they are static
     * Hence static properties are not available in interfaces...
     * 
     * But they cannot be abstract either, because Java thinks abstract static has no meaning
     * (which is true in general

    public abstract static AlgebraRelation<T> getIdRelation();

    public abstract static AlgebraRelation<T> getNoInfoRelation();

    public abstract static AlgebraRelation<T> getInconsistentRelation();
    */
}

