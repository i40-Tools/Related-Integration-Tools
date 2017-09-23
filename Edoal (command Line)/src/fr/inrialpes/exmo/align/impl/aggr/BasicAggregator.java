/*
 * $Id: BasicAggregator.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) INRIA, 2010, 2013, 2015
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

package fr.inrialpes.exmo.align.impl.aggr; 

import java.util.Hashtable;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.align.impl.BasicAlignment;

import fr.inrialpes.exmo.ontowrap.Ontology;

/**
 *
 * @author Jérôme Euzenat
 * @version $Id: BasicAggregator.java 2102 2015-11-29 10:29:56Z euzenat $ 
 *
 * This class is a reengineering of the ConsensusAggregator.
 * Note that BasicAlignment also has a static aggregate method.
 * This method takes into accound different relations.
 * These methods are redundant but it is currently unclear what is the best.
 *
 * This class is fed by "ingesting" various alignments, i.e., collecting and counting their correspondences
 * Then the extraction of an alignement is made by extracting an alignment
 *
 * So the aggregator works with the following interface:
 * aggr = new ConsensusAggregator();
 * aggr.ingest( Alignment );
 * aggr.ingest( Alignment );
 * ...
 * aggr.extract()
 *
 * The extract method is abstract here.
 *
 * Trimming the result may be achieved by the usual cut method of BasicAlignment.
 *
 * (There could be also a possibility to introduce a hook depending on the entities to be compared. 
 * For instance, in multilingual matching, if the labels are one word, then n=5, if they are more than one word, n=2)
 */

public abstract class BasicAggregator extends BasicAlignment {
    final static Logger logger = LoggerFactory.getLogger( BasicAggregator.class );

    int nbAlignments = 0;
    Hashtable<Cell, CountCell> count;

    /** Creation **/
    public BasicAggregator() {
	// Initialising the hash table
	count = new Hashtable<Cell, CountCell>();
    }

    /**
     * Record the content of alignments.
     *
     * @param al: an alignment to intergrate
     * @throws AlignmentException when something goes wrong (e.g., ontologies are different)
     */
    public void ingest( BasicAlignment al ) throws AlignmentException {
	Ontology<? extends Object> onto1 = getOntologyObject1();
	Ontology<? extends Object> onto2 = getOntologyObject2();
	logger.debug(" Onto1: {}, Onto2 {}", onto1, onto2 );
	if ( onto1 == null || onto2 == null ) {
	    onto1 = al.getOntologyObject1();
	    onto2 = al.getOntologyObject2();
	    init( onto1, onto2 );
	} else { // check that these are the same ontologies
	    if ( !onto1.getURI().equals(al.getOntology1URI()) )
		throw new AlignmentException( "Can only meet alignments with same ontologies" + onto1.getURI() + " <> " + al.getOntology1URI() );
	    if ( !onto2.getURI().equals(al.getOntology2URI()) )
		throw new AlignmentException( "Can only meet alignments with same ontologies" + onto2.getURI() + " <> " + al.getOntology2URI() );
	}
	nbAlignments++;
	for ( Cell c : al ) {
	    Cell newc = isAlreadyThere( c );
	    if ( newc == null ) {
		newc = addAlignCell( c.getObject1(), c.getObject2(), c.getRelation().toString(), 1. );
		count.put( newc, new CountCell() );
	    }
	    count.get( newc ).incr( c.getStrength() );
	}
    }

    /**
     * Extract the alignment from consensus
     * This has to be implemented in concrete classes
     *
     * @throws AlignmentException when something goes wrong
     */
    public abstract void extract() throws AlignmentException;

    /**
     * Find the relation if it already exists.
     * 
     * NOTE: It may be worth to consider that the relations do not have to
     * be equal but could be more specific or general than one another.
     * This could typically be made with algebras of relations.
     *
     * @param c: a cell to test the presence in the current aggregation
     * @return the cell corresponding to c if it exists, null otherwise
     */
    public Cell isAlreadyThere( Cell c ){
	try {
	    Set<Cell> possible = getAlignCells1( c.getObject1() );
	    Object ob2 = c.getObject2();
	    Relation r = c.getRelation();
	    if ( possible!= null ) {
		for ( Cell c2 : possible ) {
		    if ( ob2.equals( c2.getObject2() ) && r.equals( c2.getRelation() ) ) return c2;
		}
	    }
	} catch (Exception ex) {
	    logger.debug( "IGNORED Exception", ex );
	}
	return null;
    }

    protected class CountCell {
	private int occ;
	private double number;
	public CountCell() { number = 0.; occ = 0; }
	public CountCell( double i, int j ) { number = i; occ = j; }
	public void incr( double d ) { number += d; occ++; }
	public double getValue() { return number; }
	public int getOccurences() { return occ; }
    }
}


