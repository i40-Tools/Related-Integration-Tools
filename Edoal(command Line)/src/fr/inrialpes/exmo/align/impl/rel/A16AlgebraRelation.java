/*
 * $Id: A16AlgebraRelation.java 2102 2015-11-29 10:29:56Z euzenat $
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
 * The A16 Algebras of relations
 * (see: Armen Inants, Jérôme Euzenat, Algebras of ontology alignment relations, Proc. ISWC, 2015).
 * 
 * JEPD relations between classes, enventually empty, and individuals
 *
 */

public class A16AlgebraRelation extends BitSetAlgebraRelation<A16BaseRelation> {
    final static Logger logger = LoggerFactory.getLogger( A16AlgebraRelation.class );

    protected static void init() {
	protoinit();
	// Declare relations
	declareRelation( A16BaseRelation.EQ_N, A16BaseRelation.EQ_N );        // Equals non empty classes
	declareRelation( A16BaseRelation.SUBSUME, A16BaseRelation.SUBSUMED ); // Subsumes a non empty classe
	declareRelation( A16BaseRelation.SUBSUMED, A16BaseRelation.SUBSUME ); // Is subsumed and non empty
	declareRelation( A16BaseRelation.OVERLAP, A16BaseRelation.OVERLAP );  // Overlaping classes
	declareRelation( A16BaseRelation.DISJOINT, A16BaseRelation.DISJOINT );// Disjoint 
	declareRelation( A16BaseRelation.HAS, A16BaseRelation.ISA );          // Instance belong to class
	declareRelation( A16BaseRelation.HASNOT, A16BaseRelation.ISNOT );     // Instance does not belong to class
	declareRelation( A16BaseRelation.ISA, A16BaseRelation.HAS );          // Class has instance
	declareRelation( A16BaseRelation.ISNOT, A16BaseRelation.HASNOT );     // Class has no instance
	declareRelation( A16BaseRelation.EQ_I, A16BaseRelation.EQ_I );        // Equal instances
	declareRelation( A16BaseRelation.NOTEQ_I, A16BaseRelation.NOTEQ_I );  // Different instance
	declareRelation( A16BaseRelation.NE, A16BaseRelation.EN );            // Is subsumed by and empty
	declareRelation( A16BaseRelation.IE, A16BaseRelation.EI );            // Individual not belonging to empty class
	declareRelation( A16BaseRelation.EQ_E, A16BaseRelation.EQ_E );        // Equal empty classes
	declareRelation( A16BaseRelation.EN, A16BaseRelation.NE );            // Subsumes an empty classe
	declareRelation( A16BaseRelation.EI, A16BaseRelation.IE );            // Empty class not containing individual

	// Declare id relation
	initStructures( A16BaseRelation.EQ_N, A16BaseRelation.EQ_E, A16BaseRelation.EQ_I );

	// Declare composition table
	// ---- EQ_N
	o( A16BaseRelation.EQ_N, A16BaseRelation.EQ_N,
	   A16BaseRelation.EQ_N );
	o( A16BaseRelation.EQ_N, A16BaseRelation.SUBSUME,
	   A16BaseRelation.SUBSUME );
	o( A16BaseRelation.EQ_N, A16BaseRelation.SUBSUMED,
	   A16BaseRelation.SUBSUMED );
	o( A16BaseRelation.EQ_N, A16BaseRelation.OVERLAP,
	   A16BaseRelation.OVERLAP );
	o( A16BaseRelation.EQ_N, A16BaseRelation.DISJOINT,
	   A16BaseRelation.DISJOINT );
	o( A16BaseRelation.EQ_N, A16BaseRelation.HAS,
	   A16BaseRelation.HAS );
	o( A16BaseRelation.EQ_N, A16BaseRelation.HASNOT,
	   A16BaseRelation.HASNOT );
	o( A16BaseRelation.EQ_N, A16BaseRelation.NE,
	   A16BaseRelation.NE );
	// ---- SUBSUME
	o( A16BaseRelation.SUBSUME, A16BaseRelation.EQ_N, 
	   A16BaseRelation.SUBSUME );
	o( A16BaseRelation.SUBSUME, A16BaseRelation.SUBSUME, 
	   A16BaseRelation.SUBSUME );
	o( A16BaseRelation.SUBSUME, A16BaseRelation.SUBSUMED, 
	   A16BaseRelation.EQ_N, A16BaseRelation.SUBSUME, A16BaseRelation.SUBSUMED, A16BaseRelation.OVERLAP );
	o( A16BaseRelation.SUBSUME, A16BaseRelation.OVERLAP, 
	   A16BaseRelation.SUBSUME, A16BaseRelation.OVERLAP );
	o( A16BaseRelation.SUBSUME, A16BaseRelation.DISJOINT, 
	   A16BaseRelation.SUBSUME, A16BaseRelation.OVERLAP, A16BaseRelation.DISJOINT );
	o( A16BaseRelation.SUBSUME, A16BaseRelation.HAS,
	   A16BaseRelation.HAS );
	o( A16BaseRelation.SUBSUME, A16BaseRelation.HASNOT,
	   A16BaseRelation.HAS, A16BaseRelation.HASNOT );
	o( A16BaseRelation.SUBSUME, A16BaseRelation.NE,
	   A16BaseRelation.NE );
	// ---- SUBSUMED
	o( A16BaseRelation.SUBSUMED, A16BaseRelation.EQ_N, 
	   A16BaseRelation.SUBSUMED );
	o( A16BaseRelation.SUBSUMED, A16BaseRelation.SUBSUME, 
	   A16BaseRelation.EQ_N, A16BaseRelation.SUBSUME, A16BaseRelation.SUBSUMED, A16BaseRelation.OVERLAP, A16BaseRelation.DISJOINT );
	o( A16BaseRelation.SUBSUMED, A16BaseRelation.SUBSUMED, 
	   A16BaseRelation.SUBSUMED );
	o( A16BaseRelation.SUBSUMED, A16BaseRelation.OVERLAP, 
	   A16BaseRelation.SUBSUMED, A16BaseRelation.OVERLAP, A16BaseRelation.DISJOINT );
	o( A16BaseRelation.SUBSUMED, A16BaseRelation.DISJOINT, 
	   A16BaseRelation.DISJOINT );
	o( A16BaseRelation.SUBSUMED, A16BaseRelation.HAS,
	   A16BaseRelation.HAS, A16BaseRelation.HASNOT );
	o( A16BaseRelation.SUBSUMED, A16BaseRelation.HASNOT,
	   A16BaseRelation.HASNOT );
	o( A16BaseRelation.SUBSUMED, A16BaseRelation.NE,
	   A16BaseRelation.NE );
	// ---- OVERLAP
	o( A16BaseRelation.OVERLAP, A16BaseRelation.EQ_N, 
	   A16BaseRelation.OVERLAP );
	o( A16BaseRelation.OVERLAP, A16BaseRelation.SUBSUME, 
	   A16BaseRelation.SUBSUME, A16BaseRelation.OVERLAP, A16BaseRelation.DISJOINT );
	o( A16BaseRelation.OVERLAP, A16BaseRelation.SUBSUMED, 
	   A16BaseRelation.SUBSUMED, A16BaseRelation.OVERLAP );
	o( A16BaseRelation.OVERLAP, A16BaseRelation.OVERLAP, 
	   A16BaseRelation.EQ_N, A16BaseRelation.SUBSUME, A16BaseRelation.SUBSUMED, A16BaseRelation.OVERLAP, A16BaseRelation.DISJOINT );
	o( A16BaseRelation.OVERLAP, A16BaseRelation.DISJOINT, 
	   A16BaseRelation.SUBSUME, A16BaseRelation.OVERLAP, A16BaseRelation.DISJOINT );
	o( A16BaseRelation.OVERLAP, A16BaseRelation.HAS,
	   A16BaseRelation.HAS, A16BaseRelation.HASNOT );
	o( A16BaseRelation.OVERLAP, A16BaseRelation.HASNOT,
	   A16BaseRelation.HAS, A16BaseRelation.HASNOT );
	o( A16BaseRelation.OVERLAP, A16BaseRelation.NE,
	   A16BaseRelation.NE );
	// ---- DISJOINT
	o( A16BaseRelation.DISJOINT, A16BaseRelation.EQ_N, 
	   A16BaseRelation.DISJOINT );
	o( A16BaseRelation.DISJOINT, A16BaseRelation.SUBSUME, 
	   A16BaseRelation.DISJOINT );
	o( A16BaseRelation.DISJOINT, A16BaseRelation.SUBSUMED, 
	   A16BaseRelation.SUBSUMED, A16BaseRelation.OVERLAP, A16BaseRelation.DISJOINT );
	o( A16BaseRelation.DISJOINT, A16BaseRelation.OVERLAP, 
	   A16BaseRelation.SUBSUMED, A16BaseRelation.OVERLAP, A16BaseRelation.DISJOINT );
	o( A16BaseRelation.DISJOINT, A16BaseRelation.DISJOINT, 
	   A16BaseRelation.EQ_N, A16BaseRelation.SUBSUME, A16BaseRelation.SUBSUMED, A16BaseRelation.OVERLAP, A16BaseRelation.DISJOINT );
	o( A16BaseRelation.DISJOINT, A16BaseRelation.HAS,
	   A16BaseRelation.HASNOT );
	o( A16BaseRelation.DISJOINT, A16BaseRelation.HASNOT,
	   A16BaseRelation.HAS, A16BaseRelation.HASNOT );
	o( A16BaseRelation.DISJOINT, A16BaseRelation.NE,
	   A16BaseRelation.NE );
	// ---- ISA
	o( A16BaseRelation.ISA, A16BaseRelation.EQ_N, 
	   A16BaseRelation.ISA );
	o( A16BaseRelation.ISA, A16BaseRelation.SUBSUME, 
	   A16BaseRelation.ISA, A16BaseRelation.ISNOT  );
	o( A16BaseRelation.ISA, A16BaseRelation.SUBSUMED, 
	   A16BaseRelation.ISA );
	o( A16BaseRelation.ISA, A16BaseRelation.OVERLAP, 
	   A16BaseRelation.ISA, A16BaseRelation.ISNOT );
	o( A16BaseRelation.ISA, A16BaseRelation.DISJOINT, 
	   A16BaseRelation.ISNOT );
	o( A16BaseRelation.ISA, A16BaseRelation.HAS,
	   A16BaseRelation.EQ_I, A16BaseRelation.NOTEQ_I );
	o( A16BaseRelation.ISA, A16BaseRelation.HASNOT,
	   A16BaseRelation.NOTEQ_I );
	o( A16BaseRelation.ISA, A16BaseRelation.NE,
	   A16BaseRelation.IE );
	// ---- ISNOT
	o( A16BaseRelation.ISNOT, A16BaseRelation.EQ_N, 
	   A16BaseRelation.ISNOT );
	o( A16BaseRelation.ISNOT, A16BaseRelation.SUBSUME, 
	   A16BaseRelation.ISNOT  );
	o( A16BaseRelation.ISNOT, A16BaseRelation.SUBSUMED, 
	   A16BaseRelation.ISA, A16BaseRelation.ISNOT );
	o( A16BaseRelation.ISNOT, A16BaseRelation.OVERLAP, 
	   A16BaseRelation.ISA, A16BaseRelation.ISNOT );
	o( A16BaseRelation.ISNOT, A16BaseRelation.DISJOINT, 
	   A16BaseRelation.ISA, A16BaseRelation.ISNOT );
	o( A16BaseRelation.ISNOT, A16BaseRelation.HAS,
	   A16BaseRelation.NOTEQ_I );
	o( A16BaseRelation.ISNOT, A16BaseRelation.HASNOT,
	   A16BaseRelation.EQ_I, A16BaseRelation.NOTEQ_I );
	o( A16BaseRelation.ISNOT, A16BaseRelation.NE,
	   A16BaseRelation.IE );
	// ---- EN
	o( A16BaseRelation.EN, A16BaseRelation.EQ_N, 
	   A16BaseRelation.EN );
	o( A16BaseRelation.EN, A16BaseRelation.SUBSUME, 
	   A16BaseRelation.EN  );
	o( A16BaseRelation.EN, A16BaseRelation.SUBSUMED, 
	   A16BaseRelation.EN );
	o( A16BaseRelation.EN, A16BaseRelation.OVERLAP, 
	   A16BaseRelation.EN );
	o( A16BaseRelation.EN, A16BaseRelation.DISJOINT, 
	   A16BaseRelation.EN );
	o( A16BaseRelation.EN, A16BaseRelation.HAS,
	   A16BaseRelation.EI );
	o( A16BaseRelation.EN, A16BaseRelation.HASNOT,
	   A16BaseRelation.EI );
	o( A16BaseRelation.EN, A16BaseRelation.NE,
	   A16BaseRelation.EQ_E );
	// ---- EQ_I
	o( A16BaseRelation.EQ_I, A16BaseRelation.EQ_I, 
	   A16BaseRelation.EQ_I );
	o( A16BaseRelation.EQ_I, A16BaseRelation.NOTEQ_I, 
	   A16BaseRelation.NOTEQ_I  );
	o( A16BaseRelation.EQ_I, A16BaseRelation.ISA, 
	   A16BaseRelation.ISA );
	o( A16BaseRelation.EQ_I, A16BaseRelation.ISNOT, 
	   A16BaseRelation.ISNOT );
	o( A16BaseRelation.EQ_I, A16BaseRelation.IE, 
	   A16BaseRelation.IE );
	// ---- NOTEQ_I
	o( A16BaseRelation.NOTEQ_I, A16BaseRelation.EQ_I, 
	   A16BaseRelation.NOTEQ_I );
	o( A16BaseRelation.NOTEQ_I, A16BaseRelation.NOTEQ_I, 
	   A16BaseRelation.EQ_I, A16BaseRelation.NOTEQ_I  );
	o( A16BaseRelation.NOTEQ_I, A16BaseRelation.ISA, 
	   A16BaseRelation.ISA, A16BaseRelation.ISNOT );
	o( A16BaseRelation.NOTEQ_I, A16BaseRelation.ISNOT, 
	   A16BaseRelation.ISA, A16BaseRelation.ISNOT );
	o( A16BaseRelation.NOTEQ_I, A16BaseRelation.IE, 
	   A16BaseRelation.IE );
	// ---- HAS
	o( A16BaseRelation.HAS, A16BaseRelation.EQ_I, 
	   A16BaseRelation.HAS );
	o( A16BaseRelation.HAS, A16BaseRelation.NOTEQ_I, 
	   A16BaseRelation.HAS, A16BaseRelation.HASNOT );
	o( A16BaseRelation.HAS, A16BaseRelation.ISA, 
	   A16BaseRelation.EQ_N, A16BaseRelation.SUBSUME, A16BaseRelation.SUBSUMED, A16BaseRelation.OVERLAP );
	o( A16BaseRelation.HAS, A16BaseRelation.ISNOT, 
	   A16BaseRelation.SUBSUME, A16BaseRelation.OVERLAP, A16BaseRelation.DISJOINT );
	o( A16BaseRelation.HAS, A16BaseRelation.IE, 
	   A16BaseRelation.NE );
	// ---- HASNOT
	o( A16BaseRelation.HASNOT, A16BaseRelation.EQ_I, 
	   A16BaseRelation.HASNOT );
	o( A16BaseRelation.HASNOT, A16BaseRelation.NOTEQ_I, 
	   A16BaseRelation.HAS, A16BaseRelation.HASNOT );
	o( A16BaseRelation.HASNOT, A16BaseRelation.ISA, 
	   A16BaseRelation.SUBSUMED, A16BaseRelation.OVERLAP, A16BaseRelation.DISJOINT );
	o( A16BaseRelation.HASNOT, A16BaseRelation.ISNOT, 
	   A16BaseRelation.EQ_N, A16BaseRelation.SUBSUME, A16BaseRelation.SUBSUMED, A16BaseRelation.OVERLAP, A16BaseRelation.DISJOINT );
	o( A16BaseRelation.HASNOT, A16BaseRelation.IE, 
	   A16BaseRelation.NE );
	// ---- EI
	o( A16BaseRelation.EI, A16BaseRelation.EQ_I, 
	   A16BaseRelation.EI );
	o( A16BaseRelation.EI, A16BaseRelation.NOTEQ_I, 
	   A16BaseRelation.EI );
	o( A16BaseRelation.EI, A16BaseRelation.ISA, 
	   A16BaseRelation.EN );
	o( A16BaseRelation.EI, A16BaseRelation.ISNOT, 
	   A16BaseRelation.EN );
	o( A16BaseRelation.EI, A16BaseRelation.IE, 
	   A16BaseRelation.EQ_E );
	// ---- EQ_E
	o( A16BaseRelation.EQ_E, A16BaseRelation.EQ_E, 
	   A16BaseRelation.EQ_E );
	o( A16BaseRelation.EQ_E, A16BaseRelation.EN, 
	   A16BaseRelation.EN );
	o( A16BaseRelation.EQ_E, A16BaseRelation.EI, 
	   A16BaseRelation.EI );
	// ---- NE
	o( A16BaseRelation.NE, A16BaseRelation.EQ_E, 
	   A16BaseRelation.NE );
	o( A16BaseRelation.NE, A16BaseRelation.EN, 
	   A16BaseRelation.EQ_N, A16BaseRelation.SUBSUME, A16BaseRelation.SUBSUMED, A16BaseRelation.OVERLAP, A16BaseRelation.DISJOINT );
	o( A16BaseRelation.NE, A16BaseRelation.EI, 
	   A16BaseRelation.HAS, A16BaseRelation.HASNOT );
	// ---- IE
	o( A16BaseRelation.IE, A16BaseRelation.EQ_E, 
	   A16BaseRelation.IE );
	o( A16BaseRelation.IE, A16BaseRelation.EN, 
	   A16BaseRelation.ISA, A16BaseRelation.ISNOT );
	o( A16BaseRelation.IE, A16BaseRelation.EI, 
	   A16BaseRelation.EQ_I, A16BaseRelation.NOTEQ_I );
	protofinalize();
    }

    /**********************************************************************
     * NO CHANGE IS NECESSARY BEYOND THIS POINT (only change the class name)
     * Unfortunately, this MUST be copied in all class.
     **********************************************************************/

    /** Creation **/
    private A16AlgebraRelation(){};

    public A16AlgebraRelation( String rels ) throws AlignmentException {
	if ( size == -1 ) init();
	relset = instance0.read( rels );
    }

    public static A16AlgebraRelation createRelation( String rels ) throws AlignmentException {
	return new A16AlgebraRelation( rels );
    }

    public static A16AlgebraRelation createRelation( A16BaseRelation... rels ) throws AlignmentException {
	if ( size == -1 ) init();
	A16AlgebraRelation newrel = new A16AlgebraRelation();
	for ( A16BaseRelation r: rels ) newrel.relset.set( r.index );
	return newrel;
    }

    /**
     * This is private because it refers to the implementation as BitSet
     *
     * @param rels: the internal representation of the relation
     */
    private A16AlgebraRelation( BitSet rels ) {
	super( rels );
    }

    protected static HashMap<String,A16BaseRelation> relations = null;

    protected static Vector<A16BaseRelation> positions;

    protected static int size = -1;

    protected static BitSet[][] compositionTable;

    private static A16AlgebraRelation instance0;
    private static A16AlgebraRelation fullSet;
    private static A16AlgebraRelation emptySet;

    protected BitSet createSet() {
	return new BitSet( size );
    }

    protected static void protoinit() {
	logger.trace( "Initialising algebra of relations" );
	size = 0;
	relations = new HashMap<String,A16BaseRelation>();
	positions = new Vector<A16BaseRelation>();
    }

    protected static void initStructures( A16BaseRelation... idrels ) {
	compositionTable = new BitSet[size][size];
	for( int i=0; i < size; i++ )
	    for( int j=0; j < size; j++ ) 
		compositionTable[i][j] = new BitSet( size );
	BitSet bs = new BitSet( size );
	for( A16BaseRelation rel : idrels ) {
	    bs.set( rel.index );
	}
	instance0 = new A16AlgebraRelation( bs );
	bs = new BitSet( size );
	emptySet = new A16AlgebraRelation( bs );
	bs = new BitSet( size );
	bs.flip( 0, bs.size() );
	fullSet = new A16AlgebraRelation( bs );
    }

    protected static void protofinalize() {
	//if ( instance0 == null ) instance0 = new A16AlgebraRelation( new BitSet( size ) );
	logger.trace( "Initialised algebra with {} relations", size );
    }

    public A16BaseRelation getRelation( String rel ) {
	//return A16BaseRelation.valueOf( rel );
	if ( size == -1) init();
	return relations.get( rel );
    }
    public A16BaseRelation getRelation( int i ) {
	if ( size == -1) init();
	return positions.get( i );
    }

    public BitSet compose( A16BaseRelation s, A16BaseRelation t ) {
	return compositionTable[s.index][t.index];
    }

    protected static void declareRelation( A16BaseRelation rel, A16BaseRelation inverse ) {
	rel.init( size++, inverse );
	relations.put( rel.relation, rel );
	positions.add( rel );
	logger.debug( "{} idx: {}, inv: {}", rel, rel.index, rel.inverse );
    }

    protected static void setComposition( A16BaseRelation a, A16BaseRelation b, A16BaseRelation c ) {
	compositionTable[a.index][b.index].set( c.index );
    }

    // Declaration by transitivity table
    protected static void o( A16BaseRelation a, A16BaseRelation b, A16BaseRelation... s ) {
	for ( A16BaseRelation r : s ) compositionTable[a.index][b.index].set( r.index );
	logger.debug( "{} o {} = {}", a.index, b.index, compositionTable[a.index][b.index] );
    }

    // Declaration by compatible triples
    protected static void t( A16BaseRelation a, A16BaseRelation b, A16BaseRelation c ) {
	compositionTable[a.index][b.index].set( c.index );
	compositionTable[b.inverse.index][a.inverse.index].set( c.inverse.index );
	compositionTable[a.inverse.index][c.index].set( b.index );
	compositionTable[c.inverse.index][a.index].set( b.inverse.index );
	compositionTable[b.index][c.inverse.index].set( a.index );
	compositionTable[c.index][b.inverse.index].set( a.inverse.index );
    }

    public A16AlgebraRelation compose( A16AlgebraRelation dr ) {
	return new A16AlgebraRelation( protocompose( dr ) );
    }

    public A16AlgebraRelation compose( Relation r ) {
	if ( r instanceof A16AlgebraRelation )
	    return new A16AlgebraRelation( protocompose( (A16AlgebraRelation)r ) );
	//throw new AlignmentException ( "Cannot compose heterogeneous relations" );
	else return null;
    }

    public A16AlgebraRelation inverse() {
	return new A16AlgebraRelation( protoinverse() );
    }

    public A16AlgebraRelation join( A16AlgebraRelation... drs ) {
	return new A16AlgebraRelation( protojoin( drs ) );
    }

    public A16AlgebraRelation meet( A16AlgebraRelation... drs ) {
	return new A16AlgebraRelation( protomeet( drs ) );
    }

    public static A16AlgebraRelation getInconsistentRelation() throws AlignmentException {
	if ( size == -1 ) init();
	return emptySet;
    }

    public static A16AlgebraRelation getNoInfoRelation() throws AlignmentException {
	if ( size == -1 ) init();
	return fullSet;
    }

    public static A16AlgebraRelation getIdRelation() throws AlignmentException {
	if ( size == -1 ) init();
	return instance0;
    }

    public boolean isIdRelation() {
	return instance0.entails( this );
    }
}

