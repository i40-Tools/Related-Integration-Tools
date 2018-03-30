/*
 * $Id: BitSetDisjunctiveRelation.java 2129 2017-03-04 17:51:55Z euzenat $
 *
 * Copyright (C) INRIA, 2015, 2017
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
import org.semanticweb.owl.align.Relation;

import java.lang.Iterable;
import java.lang.IllegalStateException;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.Objects;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a dijunctive ontology alignment relation implementation.
 * A disjunctive ontology alignment relation is made of the disjunction of several atomic relations.
 * This disjunction is traditionally represented as a set, here implemented as BitSets
 * 
 * @author Jérôme Euzenat
 * @version $Id: BitSetDisjunctiveRelation.java 2129 2017-03-04 17:51:55Z euzenat $
 */

// IT MAY NOT BE REASONABLE TO EXTEND BASICRELATION

public abstract class BitSetDisjunctiveRelation<T extends BaseRelation> extends BasicRelation implements DisjunctiveRelation<T>, Iterable<T> {
    final static Logger logger = LoggerFactory.getLogger( BitSetDisjunctiveRelation.class );

    /** Creation **/
    protected BitSetDisjunctiveRelation(){
	super();
    }

    protected BitSetDisjunctiveRelation( BitSet rels ) {
	super();
    	relset = rels;
    }

    protected abstract BitSet createSet();

    protected abstract T getRelation( int i );

    public BitSet read( String rels ) throws AlignmentException {
	BitSet bs = createSet();
	for ( String rel : rels.split(",") ) {
	    T relation = getRelation( rel.trim() );
	    if ( relation != null ) {
		bs.set( relation.getIndex() );
	    } else {
		if ( !"".equals( rel.trim() ) ) // forgive extra commas
		    throw new AlignmentException( "Unknown base relation : "+rel );
	    }
	}
	return bs;
    }

    protected BitSet relset = null;

    public BitSet getRelations() {
	return relset;
    }

    public Iterator<T> iterator() {
	return new RelationIterator<T>();
    }

    /**
     * intersection
     *
     * @param drs: a set of bitset disjunctive relations to be intersected
     * @return the bitset corresponding to the intersected relation
     */
    @SafeVarargs protected final BitSet protojoin( BitSetDisjunctiveRelation<T>... drs ) {
	BitSet bs = (BitSet)relset.clone();
	for( BitSetDisjunctiveRelation<T> dr : drs ) {
	    bs.and( dr.getRelations() );
	}
	return bs;
    }

    @SuppressWarnings({"unchecked", "varargs"}) public DisjunctiveRelation<T> join( DisjunctiveRelation<T>... drs ) throws AlignmentException {
	throw new AlignmentException( "Cannot join relations from heterogeneous algebras" );
    }

    /**
     * union
     *
     * @param drs: a set of bitset disjunctive relations to be met
     * @return the bitset corresponding to the met relations
     */
    @SafeVarargs protected final BitSet protomeet( BitSetDisjunctiveRelation<T>... drs ) {
	BitSet bs = (BitSet)relset.clone();
	for( BitSetDisjunctiveRelation<T> dr : drs ) {
	    bs.or( dr.getRelations() );
	}
	return bs;
    }

    @SuppressWarnings({"unchecked", "varargs"}) public DisjunctiveRelation<T> meet( DisjunctiveRelation<T>... drs ) throws AlignmentException {
	throw new AlignmentException( "Cannot meet relations from heterogeneous algebras" );
    }

    /**
     * complement
     *
     * @param dr: a set of bitset disjunctive relations to be complemented
     * @return the bitset corresponding to the complemented relation
     */
    protected BitSet protocompl( BitSetDisjunctiveRelation<T> dr ) {
	BitSet bs = (BitSet)relset.clone();
	//for( int i=0; i < size; i++ ) bs.flip( i );
	return bs;
    }

    public boolean isEmpty() {
	return relset.isEmpty();
    }

    public boolean entails( BitSetDisjunctiveRelation<T> dr ) {
	if ( dr == this ) return true;
	if ( dr == null ) return false;
	// This does allocate but is certainly faster and simpler
	BitSet bs = (BitSet)relset.clone();
	bs.andNot( ((BitSetDisjunctiveRelation<?>)dr).getRelations() );
	return bs.isEmpty();
	/*
	// This does not allocate but is certainly slower
	BitSet bs = ((BitSetDisjunctiveRelation<?>)dr).getRelations();
	int size = relset.size(); // is it correct?
	for( int i=0; i < size; i++ ) {
	    if ( bs.get(i) && !relset.get(i) ) return false;
	}
	return true;
	*/
    }

    /** This is kept for displayig more correctly the result **/
    public void write( PrintWriter writer ) {
	Iterator<T> brit = iterator();
	if ( !brit.hasNext() ) return;
	brit.next().write( writer );
	while ( brit.hasNext() ) {
	    writer.print(",");
	    brit.next().write( writer );
	}
    }

    /** Shadowing BasicRelation's **/
    public String getRelation() {
	Iterator<T> brit = iterator();
	if ( !brit.hasNext() ) return "";
	String label = brit.next().getString();
	while ( brit.hasNext() ) {
	    label += ","+brit.next().getString();
	}
	return label;
    }

    public String getPrettyLabel() {
	return getRelation();
    }

    /** Are the two relations equal **/
    public boolean equals ( Object o ) {
	if ( o == this ) return true;
	if ( o == null ) return false;
	if ( o instanceof Relation ) return equals( ((Relation)o) );
	else return false;
    }

    // V5: This should be suppressed (code moved in the object method)
    public boolean equals ( Relation r ) {
	if ( r == this ) return true;
	if ( r == null ) return false;
	if ( r instanceof BitSetDisjunctiveRelation ) {
	    return relset.equals( ((BitSetDisjunctiveRelation<?>)r).getRelations() );
	} else return false;
    }

    public int hashCode() {
	return Objects.hash( relset );
	//return 3221+relset.hashCode();
    }

    private class RelationIterator<T extends BaseRelation> implements Iterator<T> {
	
	private int index = 0;
	
	private RelationIterator() {
	    index = 0;
	}

	// ( index < relset.size() ) && (??)
	public boolean hasNext() {
	    return ( ( relset.nextSetBit( index ) != -1 ) );
	}        
        
	// The class is private and always called with the proper T
	// Would be nice to tell, its the same T
	@SuppressWarnings({"unchecked"}) public T next() {
	    index = relset.nextSetBit( index );
	    if ( index != -1 ) return (T)getRelation( index++ ); //[W: unchecked]
	    else throw new IllegalStateException();
	}
    }

}

 
