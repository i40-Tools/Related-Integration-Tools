/*
 * $Id: Alignment.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2005, 2007-2009, 2014
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

package org.semanticweb.owl.align; 

import java.lang.Cloneable;
import java.lang.Iterable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Collection;
import java.util.Set;
import java.net.URI;

import org.xml.sax.ContentHandler;

/**
 * Represents an Ontology alignment.
 *
 * The alignment has reference to the two aligned ontology.
 * All Alignment cells contain first the entity from the first ontology
 * The alignment is from the first ontology to the second.
 *
 * @author Jérôme Euzenat
 * @version $Id: Alignment.java 2102 2015-11-29 10:29:56Z euzenat $ 
 */


public interface Alignment extends Cloneable, Iterable<Cell>, Visitable {

    /** Initialize the alignement before using it
     * @param onto1 and
     * @param onto2: the two ontologies aligned
     * @throws AlignmentException if cannot initialize
     **/
    public void init( Object onto1, Object onto2 ) throws AlignmentException;

    /**
     * Initialize the alignement before using it, with some ontology caching trick
     * //@deprecated The OntologyCache is now internal, use init( onto1, onto2 ) instead
     * @param onto1 and
     * @param onto2: the two ontologies aligned
     * @param cache: a cache where to find and store the ontologies
     * @throws AlignmentException when cannot initialise
     **/
    //@Deprecated
    public void init( Object onto1, Object onto2, Object cache ) throws AlignmentException;

    /* Alignment methods */

    /**
     * @return the first ontology of the alignment
     */
    public Object getOntology1();

    /**
     * @return the second ontology of the alignment
     */
    public Object getOntology2();

    /**
     * @return the URI of the first ontology of the alignment
     * @throws AlignmentException when no URI is found (unlikely)
     */
    public URI getOntology1URI() throws AlignmentException;

    /**
     * @return the URI of the second ontology of the alignment
     * @throws AlignmentException when no URI is found (unlikely)
     */
    public URI getOntology2URI() throws AlignmentException;
    public void setOntology1(Object ontology) throws AlignmentException;
    public void setOntology2(Object ontology) throws AlignmentException;
    /**
     * Alignment type:
     * Currently defined as a String.
     * This string is supposed to contain two characters: among ?, 1, *, +
     * Can be implemented otherwise
     *
     * @param level the level of the alignment
     */
    public void setLevel( String level );
    public String getLevel();
    /**
     * Alignment type:
     * Currently defined a sa String.
     * This string is supposed to contain two characters: among ?, 1, *, +
     * Can be implemented otherwise
     *
     * @param type the type of the alignment
     */
    public void setType( String type );
    public String getType();

    /**
     * Set the ontology used by the alignment
     * This URI is in fact a URL of where to fetch the ontology.
     * It may be local.
     *
     * @param file: the URI of the first ontology
     */
    public void setFile1( URI file );
    public void setFile2( URI file );
    public URI getFile1();
    public URI getFile2();

    /** Cell methods **/
    /**
     * The alignment itself is a set of Alignment cells relating one
     * entity of the firt ontology to an entity of the second one.
     * These cells are indexed within the Alingnment by the URI of the
     * entities in each ontology (with one hashtable for each).
     * In addition to the coupe of entities, the cells contains a
     * qualification of the relation between them (a Relation object)
     * and a quantification of the confidence in the relation (an int).
     */

    /**
     * Cells are created and indexed at once
     *
     * @param ob1 the object of the first ontology in the correspondence
     * @param ob2 the object of the second ontology in the correspondence
     * @param relation the relation between these objects
     * @param measure the confidence measure in the correspondence
     * @return the corresponding cell
     * @throws AlignmentException when something goes wrong (unknown objects, bad relation) 
     */
    public Cell addAlignCell( Object ob1, Object ob2, String relation, double measure) throws AlignmentException;
    public Cell addAlignCell( Object ob1, Object ob2) throws AlignmentException;

    public void remCell( Cell c ) throws AlignmentException;

    /**
     * Cells are retrieved
     * These primitives are deprecated. Use getAlignCells1 and getAlignCells2
     * instead.
     * Reason: this applies only for 1:1 alignments
     * Some implementations might act cleverly (retrieving the best value).
     * Basic implementations may raise the exception
     *
     * @param ob the object of the first ontology from which to retrieve the correspondence
     * @return the corresponding cell
     * @throws AlignmentException when something goes wrong
     * //@deprecated There is more than one cell about one object
     */
    //@Deprecated
    public Cell getAlignCell1( Object ob ) throws AlignmentException;
    /**
     * //@deprecated There is more than one cell about one object
     * @param ob the object of the second ontology from which to retrieve the correspondence
     * @return the corresponding cell
     * @throws AlignmentException when something goes wrong
     */
    //@Deprecated
    public Cell getAlignCell2( Object ob ) throws AlignmentException;

    /**
     * Each part of the cell can be queried independently.
     * These primitives are deprecated. Use getAlignCells1 and getAlignCells2
     * instead.
     * Reason: this applies only for 1:1 alignments
     * Some implementations might act cleverly (retrieving the best value).
     * Basic implementations may raise the exception
     *
     * @param ob the object of the first ontology from which to retrieve the correspondence
     * @return the corresponding object
     * @throws AlignmentException when something goes wrong
     * //@deprecated There is more than one cell about one object
     */
    public Object getAlignedObject1( Object ob ) throws AlignmentException;
    public Object getAlignedObject2( Object ob ) throws AlignmentException;
    public Relation getAlignedRelation1( Object ob ) throws AlignmentException;
    public Relation getAlignedRelation2( Object ob ) throws AlignmentException;
    public double getAlignedStrength1( Object ob ) throws AlignmentException;
    public double getAlignedStrength2( Object ob ) throws AlignmentException;

    /**
     * Cells are retrieved
     * These primitives are deprecated. Use getAlignCells1 and getAlignCells2
     * instead.
     * Reason: this applies only for 1:1 alignments
     * Some implementations might act cleverly (retrieving the best value).
     * Basic implementations may raise the exception
     *
     * @param ob the object of the first ontology from which to retrieve the correspondences
     * @return the set of correspondences involving this object
     * @throws AlignmentException when something goes wrong
     */
    public Set<Cell> getAlignCells1( Object ob ) throws AlignmentException;
    public Set<Cell> getAlignCells2( Object ob ) throws AlignmentException;

    /**
     * Extensions are a way to read and add other information (metadata)
     * to the alignment structure itself.
     * getExtensions returns a set of tripes: uri*label*value
     * all three being String
     *
     * @return the collection of extensions of the alignment
     */
    public Collection<String[]> getExtensions();
    public String getExtension( String uri, String label );
    public void setExtension( String uri, String label, String value );

    public Enumeration<Cell> getElements();
    public Iterator<Cell> iterator();
    public int nbCells();

    // What about implementing methods here?
    // enum CutMethod {
    // HARD,
    // ...
    // }
    /**
     * Trim an alignment under a particular threshold
     *
     * @param method: a string indicating the cutting method
     * - getting those cells with strength above n (hard)
     * - getting the n best cells (best)
     * - getting those cells with strength at worse n under the best (span)
     * - getting the n% best cells (perc)
     * - getting those cells with strength at worse n% of the best (prop)
     * - getting all cells until a gap of n (hardgap)
     * - getting all cells until a gap of n% of the last (propgap)
     * @param threshold: the threshold
     * @throws AlignmentException when something goes wrong (unknown method)
     */
    public void cut( String method, double threshold ) throws AlignmentException;
    public void cut( double threshold ) throws AlignmentException;
    public void harden( double threshold ) throws AlignmentException;

    /**
     * Algebra of alignment manipulation operations: compose, join, meet.
     * @return the inverse alignment from onto2 to onto1
     * @throws AlignmentException when something goes wrong (unlikely)
     */
    public Alignment inverse() throws AlignmentException;
    public Alignment diff(Alignment align) throws AlignmentException;
    public Alignment meet(Alignment align) throws AlignmentException;
    public Alignment join(Alignment align) throws AlignmentException;
    public Alignment compose(Alignment align) throws AlignmentException;

    /** Housekeeping **/
    /**
     * The methods for outputing and dispalying alignments are common to
     * all alignment. They depend on the implementation of the similar
     * methods in Cell and Relation.
     */
    /**
     * Dumps an alignment into a SAX handler
     * Dump should be implemented as a method generating SAX events
     * for a SAXHandler provided as input 
     *
     * @param h: a SAX content handler
     */
    public void dump(ContentHandler h);
    //    public void write( PrintStream writer ) throws IOException, AlignmentException;
    //public void write( PrintWriter writer ) throws IOException, AlignmentException;

    /** Exporting
     *	The alignments are exported for other purposes.
     *
     * @param renderer an AlignmentVisitor object which determines how the alignment is rendered
     * @throws AlignmentException when something goes wrong (cannot render in this format)
    */
    public void render( AlignmentVisitor renderer ) throws AlignmentException;

    /** Implementation of the clone method
     * @return a clone of the alignment
     */
    public Object clone();

}

