/*
 * $Id: A2AlgebraRelation.java 2102 2015-11-29 10:29:56Z euzenat $
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
 * The A2 Algebras of relations
 * (see: Jérôme Euzenat, Algebras of ontology alignment relations, Proc. ISWC, 2008).
 * 
 * JEPD relations between classes (based on set theory)
 *
 * @author Jérôme Euzenat
 * @version $Id: A2AlgebraRelation.java 2102 2015-11-29 10:29:56Z euzenat $
 */

public class A2AlgebraRelation extends BitSetAlgebraRelation<A2BaseRelation> {
    final static Logger logger = LoggerFactory.getLogger( A2AlgebraRelation.class );

    protected static void init() {
	protoinit();
	// Declare relations
	declareRelation( A2BaseRelation.EQUIV, A2BaseRelation.EQUIV );
	declareRelation( A2BaseRelation.DIFF, A2BaseRelation.DIFF );
	// Declare id relation
	initStructures( A2BaseRelation.EQUIV );
	// Declare composition table: here through compatible triples
	/*
	o( A2BaseRelation.EQUIV, A2BaseRelation.EQUIV, A2BaseRelation.EQUIV );
	o( A2BaseRelation.EQUIV, A2BaseRelation.DIFF, A2BaseRelation.DIFF );
	o( A2BaseRelation.DIFF, A2BaseRelation.EQUIV, A2BaseRelation.DIFF );
	o( A2BaseRelation.DIFF, A2BaseRelation.DIFF, A2BaseRelation.EQUIV, A2BaseRelation.DIFF );
	*/
	t( A2BaseRelation.EQUIV, A2BaseRelation.EQUIV, A2BaseRelation.EQUIV );
	t( A2BaseRelation.EQUIV, A2BaseRelation.DIFF, A2BaseRelation.DIFF );
	t( A2BaseRelation.DIFF, A2BaseRelation.DIFF, A2BaseRelation.DIFF );
	protofinalize();
    }

    /**********************************************************************
     * NO CHANGE IS NECESSARY BEYOND THIS POINT (only change the class name)
     * Unfortunately, this MUST be copied in all class.
     **********************************************************************/

    /** Creation **/
    private A2AlgebraRelation(){};

    public A2AlgebraRelation( String rels ) throws AlignmentException {
	if ( size == -1 ) init();
	relset = instance0.read( rels );
    }

    public static A2AlgebraRelation createRelation( String rels ) throws AlignmentException {
	return new A2AlgebraRelation( rels );
    }

    public static A2AlgebraRelation createRelation( A2BaseRelation... rels ) throws AlignmentException {
	if ( size == -1 ) init();
	A2AlgebraRelation newrel = new A2AlgebraRelation();
	for ( A2BaseRelation r: rels ) newrel.relset.set( r.index );
	return newrel;
    }

    /**
     * This is private because it refers to the implementation as BitSet
     *
     * @param rels: the internal representation of the relation
     */
    private A2AlgebraRelation( BitSet rels ) {
	super( rels );
    }

    protected static HashMap<String,A2BaseRelation> relations = null;

    protected static Vector<A2BaseRelation> positions;

    protected static int size = -1;

    protected static BitSet[][] compositionTable;

    private static A2AlgebraRelation instance0;
    private static A2AlgebraRelation fullSet;
    private static A2AlgebraRelation emptySet;

    protected BitSet createSet() {
	return new BitSet( size );
    }

    protected static void protoinit() {
	logger.trace( "Initialising algebra of relations" );
	size = 0;
	relations = new HashMap<String,A2BaseRelation>();
	positions = new Vector<A2BaseRelation>();
    }

    protected static void initStructures( A2BaseRelation... idrels ) {
	compositionTable = new BitSet[size][size];
	for( int i=0; i < size; i++ )
	    for( int j=0; j < size; j++ ) 
		compositionTable[i][j] = new BitSet( size );
	BitSet bs = new BitSet( size );
	for( A2BaseRelation rel : idrels ) {
	    bs.set( rel.index );
	}
	instance0 = new A2AlgebraRelation( bs );
	bs = new BitSet( size );
	emptySet = new A2AlgebraRelation( bs );
	bs = new BitSet( size );
	bs.flip( 0, bs.size() );
	fullSet = new A2AlgebraRelation( bs );
    }

    protected static void protofinalize() {
	//if ( instance0 == null ) instance0 = new A2AlgebraRelation( new BitSet( size ) );
	logger.trace( "Initialised algebra with {} relations", size );
    }

    public A2BaseRelation getRelation( String rel ) {
	// This would rely on enum id (EQUIV) and not label ("=")
	//return A2BaseRelation.valueOf( rel );
	if ( size == -1) init();
	return relations.get( rel );
    }
    public A2BaseRelation getRelation( int i ) {
	if ( size == -1) init();
	return positions.get( i );
    }

    public BitSet compose( A2BaseRelation s, A2BaseRelation t ) {
	return compositionTable[s.index][t.index];
    }

    protected static void declareRelation( A2BaseRelation rel, A2BaseRelation inverse ) {
	rel.init( size++, inverse );
	relations.put( rel.relation, rel );
	positions.add( rel );
	logger.debug( "{} idx: {}, inv: {}", rel, rel.index, rel.inverse );
    }

    protected static void setComposition( A2BaseRelation a, A2BaseRelation b, A2BaseRelation c ) {
	compositionTable[a.index][b.index].set( c.index );
    }

    // Declaration by transitivity table
    protected static void o( A2BaseRelation a, A2BaseRelation b, A2BaseRelation... s ) {
	for ( A2BaseRelation r : s ) compositionTable[a.index][b.index].set( r.index );
	logger.debug( "{} o {} = {}", a.index, b.index, compositionTable[a.index][b.index] );
    }

    // Declaration by compatible triples
    protected static void t( A2BaseRelation a, A2BaseRelation b, A2BaseRelation c ) {
	compositionTable[a.index][b.index].set( c.index );
	compositionTable[b.inverse.index][a.inverse.index].set( c.inverse.index );
	compositionTable[a.inverse.index][c.index].set( b.index );
	compositionTable[c.inverse.index][a.index].set( b.inverse.index );
	compositionTable[b.index][c.inverse.index].set( a.index );
	compositionTable[c.index][b.inverse.index].set( a.inverse.index );
    }

    public A2AlgebraRelation compose( A2AlgebraRelation dr ) {
	return new A2AlgebraRelation( protocompose( dr ) );
    }

    public A2AlgebraRelation compose( Relation r ) {
	if ( r instanceof A2AlgebraRelation )
	    return new A2AlgebraRelation( protocompose( (A2AlgebraRelation)r ) );
	//throw new AlignmentException ( "Cannot compose heterogeneous relations" );
	else return null;
    }

    public A2AlgebraRelation inverse() {
	return new A2AlgebraRelation( protoinverse() );
    }

    public A2AlgebraRelation join( A2AlgebraRelation... drs ) {
	return new A2AlgebraRelation( protojoin( drs ) );
    }

    public A2AlgebraRelation meet( A2AlgebraRelation... drs ) {
	return new A2AlgebraRelation( protomeet( drs ) );
    }

    public static A2AlgebraRelation getInconsistentRelation() throws AlignmentException {
	if ( size == -1 ) init();
	return emptySet;
    }

    public static A2AlgebraRelation getNoInfoRelation() throws AlignmentException {
	if ( size == -1 ) init();
	return fullSet;
    }

    public static A2AlgebraRelation getIdRelation() throws AlignmentException {
	if ( size == -1 ) init();
	return instance0;
    }

    public boolean isIdRelation() {
	return instance0.entails( this );
    }
}

