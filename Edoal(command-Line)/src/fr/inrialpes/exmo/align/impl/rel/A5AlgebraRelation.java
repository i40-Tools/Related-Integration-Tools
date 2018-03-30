/*
 * $Id: A5AlgebraRelation.java 2102 2015-11-29 10:29:56Z euzenat $
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

package fr.inrialpes.exmo.align.impl.rel;

import fr.inrialpes.exmo.align.impl.BitSetAlgebraRelation;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Relation;

import java.util.HashMap;
import java.util.Vector;
import java.util.BitSet;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The A5 Algebras of relations
 * (see: Jérôme Euzenat, Algebras of ontology alignment relations, Proc. ISWC, 2008).
 * 
 * JEPD relations between classes (based on set theory)
 *
 * @author Jérôme Euzenat
 * @version $Id: A5AlgebraRelation.java 2102 2015-11-29 10:29:56Z euzenat $
 */

public class A5AlgebraRelation extends BitSetAlgebraRelation<A5BaseRelation> {
    final static Logger logger = LoggerFactory.getLogger( A5AlgebraRelation.class );

    protected static void init() {
	protoinit();
	// Declare relations
	declareRelation( A5BaseRelation.EQUIV, A5BaseRelation.EQUIV );
	declareRelation( A5BaseRelation.SUBSUME, A5BaseRelation.SUBSUMED );
	declareRelation( A5BaseRelation.SUBSUMED, A5BaseRelation.SUBSUME );
	declareRelation( A5BaseRelation.OVERLAP, A5BaseRelation.OVERLAP );
	declareRelation( A5BaseRelation.DISJOINT, A5BaseRelation.DISJOINT );
	// Declare id relation
	initStructures( A5BaseRelation.EQUIV );
	// Declare composition table
	// ---- EQUIV
	o( A5BaseRelation.EQUIV, A5BaseRelation.EQUIV,
	   A5BaseRelation.EQUIV );
	o( A5BaseRelation.EQUIV, A5BaseRelation.SUBSUME,
	   A5BaseRelation.SUBSUME );
	o( A5BaseRelation.EQUIV, A5BaseRelation.SUBSUMED,
	   A5BaseRelation.SUBSUMED );
	o( A5BaseRelation.EQUIV, A5BaseRelation.OVERLAP,
	   A5BaseRelation.OVERLAP );
	o( A5BaseRelation.EQUIV, A5BaseRelation.DISJOINT,
	   A5BaseRelation.DISJOINT );
	// ---- SUBSUME
	o( A5BaseRelation.SUBSUME, A5BaseRelation.EQUIV, 
	   A5BaseRelation.SUBSUME );
	o( A5BaseRelation.SUBSUME, A5BaseRelation.SUBSUME, 
	   A5BaseRelation.SUBSUME );
	o( A5BaseRelation.SUBSUME, A5BaseRelation.SUBSUMED, 
	   A5BaseRelation.EQUIV, A5BaseRelation.SUBSUME, A5BaseRelation.SUBSUMED, A5BaseRelation.OVERLAP );
	o( A5BaseRelation.SUBSUME, A5BaseRelation.OVERLAP, 
	   A5BaseRelation.SUBSUME, A5BaseRelation.OVERLAP );
	o( A5BaseRelation.SUBSUME, A5BaseRelation.DISJOINT, 
	   A5BaseRelation.SUBSUME, A5BaseRelation.OVERLAP, A5BaseRelation.DISJOINT );
	// ---- SUBSUMED
	o( A5BaseRelation.SUBSUMED, A5BaseRelation.EQUIV, 
	   A5BaseRelation.SUBSUMED );
	o( A5BaseRelation.SUBSUMED, A5BaseRelation.SUBSUME, 
	   A5BaseRelation.EQUIV, A5BaseRelation.SUBSUME, A5BaseRelation.SUBSUMED, A5BaseRelation.OVERLAP, A5BaseRelation.DISJOINT );
	o( A5BaseRelation.SUBSUMED, A5BaseRelation.SUBSUMED, 
	   A5BaseRelation.SUBSUMED );
	o( A5BaseRelation.SUBSUMED, A5BaseRelation.OVERLAP, 
	   A5BaseRelation.SUBSUMED, A5BaseRelation.OVERLAP, A5BaseRelation.DISJOINT );
	o( A5BaseRelation.SUBSUMED, A5BaseRelation.DISJOINT, 
	   A5BaseRelation.DISJOINT );
	// ---- OVERLAP
	o( A5BaseRelation.OVERLAP, A5BaseRelation.EQUIV, 
	   A5BaseRelation.OVERLAP );
	o( A5BaseRelation.OVERLAP, A5BaseRelation.SUBSUME, 
	   A5BaseRelation.SUBSUME, A5BaseRelation.OVERLAP, A5BaseRelation.DISJOINT );
	o( A5BaseRelation.OVERLAP, A5BaseRelation.SUBSUMED, 
	   A5BaseRelation.SUBSUMED, A5BaseRelation.OVERLAP );
	o( A5BaseRelation.OVERLAP, A5BaseRelation.OVERLAP, 
	   A5BaseRelation.EQUIV, A5BaseRelation.SUBSUME, A5BaseRelation.SUBSUMED, A5BaseRelation.OVERLAP, A5BaseRelation.DISJOINT );
	o( A5BaseRelation.OVERLAP, A5BaseRelation.DISJOINT, 
	   A5BaseRelation.SUBSUME, A5BaseRelation.OVERLAP, A5BaseRelation.DISJOINT );
	// ---- DISJOINT
	o( A5BaseRelation.DISJOINT, A5BaseRelation.EQUIV, 
	   A5BaseRelation.DISJOINT );
	o( A5BaseRelation.DISJOINT, A5BaseRelation.SUBSUME, 
	   A5BaseRelation.DISJOINT );
	o( A5BaseRelation.DISJOINT, A5BaseRelation.SUBSUMED, 
	   A5BaseRelation.SUBSUMED, A5BaseRelation.OVERLAP, A5BaseRelation.DISJOINT );
	o( A5BaseRelation.DISJOINT, A5BaseRelation.OVERLAP, 
	   A5BaseRelation.SUBSUMED, A5BaseRelation.OVERLAP, A5BaseRelation.DISJOINT );
	o( A5BaseRelation.DISJOINT, A5BaseRelation.DISJOINT, 
	   A5BaseRelation.EQUIV, A5BaseRelation.SUBSUME, A5BaseRelation.SUBSUMED, A5BaseRelation.OVERLAP, A5BaseRelation.DISJOINT );
	protofinalize();
    }

    /**********************************************************************
     * NO CHANGE IS NECESSARY BEYOND THIS POINT (only change the class name)
     * Unfortunately, this MUST be copied in all class.
     **********************************************************************/

    /** Creation **/
    private A5AlgebraRelation(){};

    public A5AlgebraRelation( String rels ) throws AlignmentException {
	if ( size == -1 ) init();
	relset = instance0.read( rels );
    }

    public static A5AlgebraRelation createRelation( String rels ) throws AlignmentException {
	return new A5AlgebraRelation( rels );
    }

    public static A5AlgebraRelation createRelation( A5BaseRelation... rels ) throws AlignmentException {
	if ( size == -1 ) init();
	A5AlgebraRelation newrel = new A5AlgebraRelation();
	for ( A5BaseRelation r: rels ) newrel.relset.set( r.index );
	return newrel;
    }

    /**
     * This is private because it refers to the implementation as BitSet
     *
     * @param rels: the internal representation of the relation
     */
    private A5AlgebraRelation( BitSet rels ) {
	super( rels );
    }

    protected static HashMap<String,A5BaseRelation> relations = null;

    protected static Vector<A5BaseRelation> positions;

    protected static int size = -1;

    protected static BitSet[][] compositionTable;

    private static A5AlgebraRelation instance0;
    private static A5AlgebraRelation emptySet;
    private static A5AlgebraRelation fullSet;

    protected BitSet createSet() {
	return new BitSet( size );
    }

    protected static void protoinit() {
	logger.trace( "Initialising algebra of relations" );
	size = 0;
	relations = new HashMap<String,A5BaseRelation>();
	positions = new Vector<A5BaseRelation>();
    }

    protected static void initStructures( A5BaseRelation... idrels ) {
	compositionTable = new BitSet[size][size];
	for( int i=0; i < size; i++ )
	    for( int j=0; j < size; j++ ) 
		compositionTable[i][j] = new BitSet( size );
	BitSet bs = new BitSet( size );
	for( A5BaseRelation rel : idrels ) {
	    bs.set( rel.index );
	}
	instance0 = new A5AlgebraRelation( bs );
	bs = new BitSet( size );
	emptySet = new A5AlgebraRelation( bs );
	bs = new BitSet( size );
	bs.flip( 0, bs.size() );
	fullSet = new A5AlgebraRelation( bs );
    }

    protected static void protofinalize() {
	//if ( instance0 == null ) instance0 = new A5AlgebraRelation( new BitSet( size ) );
	logger.trace( "Initialised algebra with {} relations", size );
    }

    public A5BaseRelation getRelation( String rel ) {
	// This would rely on enum id (EQUIV) and not label ("=")
	//return A5BaseRelation.valueOf( rel );
	if ( size == -1) init();
	return relations.get( rel );
    }
    public A5BaseRelation getRelation( int i ) {
	if ( size == -1) init();
	return positions.get( i );
    }

    public BitSet compose( A5BaseRelation s, A5BaseRelation t ) {
	return compositionTable[s.index][t.index];
    }

    protected static void declareRelation( A5BaseRelation rel, A5BaseRelation inverse ) {
	rel.init( size++, inverse );
	relations.put( rel.relation, rel );
	positions.add( rel );
	logger.debug( "{} idx: {}, inv: {}", rel, rel.index, rel.inverse );
    }

    protected static void setComposition( A5BaseRelation a, A5BaseRelation b, A5BaseRelation c ) {
	compositionTable[a.index][b.index].set( c.index );
    }

    // Declaration by transitivity table
    protected static void o( A5BaseRelation a, A5BaseRelation b, A5BaseRelation... s ) {
	for ( A5BaseRelation r : s ) compositionTable[a.index][b.index].set( r.index );
	logger.debug( "{} o {} = {}", a.index, b.index, compositionTable[a.index][b.index] );
    }

    // Declaration by compatible triples
    protected static void t( A5BaseRelation a, A5BaseRelation b, A5BaseRelation c ) {
	compositionTable[a.index][b.index].set( c.index );
	compositionTable[b.inverse.index][a.inverse.index].set( c.inverse.index );
	compositionTable[a.inverse.index][c.index].set( b.index );
	compositionTable[c.inverse.index][a.index].set( b.inverse.index );
	compositionTable[b.index][c.inverse.index].set( a.index );
	compositionTable[c.index][b.inverse.index].set( a.inverse.index );
    }

    public A5AlgebraRelation compose( A5AlgebraRelation dr ) {
	return new A5AlgebraRelation( protocompose( dr ) );
    }

    public A5AlgebraRelation compose( Relation r ) {
	if ( r instanceof A5AlgebraRelation )
	    return new A5AlgebraRelation( protocompose( (A5AlgebraRelation)r ) );
	//throw new AlignmentException ( "Cannot compose heterogeneous relations" );
	else return null;
    }

    public A5AlgebraRelation inverse() {
	return new A5AlgebraRelation( protoinverse() );
    }

    public final A5AlgebraRelation join( A5AlgebraRelation... drs ) {
	return new A5AlgebraRelation( protojoin( drs ) );
    }

    public final A5AlgebraRelation meet( A5AlgebraRelation... drs ) {
	return new A5AlgebraRelation( protomeet( drs ) );
    }

    public static A5AlgebraRelation getInconsistentRelation() throws AlignmentException {
	if ( size == -1 ) init();
	return emptySet;
    }

    public static A5AlgebraRelation getNoInfoRelation() throws AlignmentException {
	if ( size == -1 ) init();
	return fullSet;
    }

    public static A5AlgebraRelation getIdRelation() throws AlignmentException {
	if ( size == -1 ) init();
	return instance0;
    }

    public boolean isIdRelation() {
	return instance0.entails( this );
    }
}

