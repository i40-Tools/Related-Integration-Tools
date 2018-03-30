/*
 * $Id: BasicAlignment.java 2129 2017-03-04 17:51:55Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2011, 2013-2017
 * Copyright (C) CNR Pisa, 2005
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

import java.lang.Iterable;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Collections;
import java.util.Collection;
import java.util.Properties;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.net.URI;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.ContentHandler;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.ontowrap.Ontology;
import fr.inrialpes.exmo.ontowrap.BasicOntology;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

/**
 * Represents a basic ontology alignment, i.e., a fully functionnal alignment
 * for wich the type of aligned objects is not known.
 *
 * In version 3.0 this class is virtually abstract.
 * But it cannot be declared abstract because it uses its own constructor.
 *
 * @author Jérôme Euzenat, David Loup, Raphaël Troncy
 * @version $Id: BasicAlignment.java 2129 2017-03-04 17:51:55Z euzenat $
 */

public class BasicAlignment implements Alignment, Extensible {
    final static Logger logger = LoggerFactory.getLogger( BasicAlignment.class );

    public void accept( AlignmentVisitor visitor ) throws AlignmentException {
	visitor.visit( this );
    }

    protected Ontology<Object> onto1 = null;
    protected Ontology<Object> onto2 = null;

    protected String level = "0";

    protected String type = "**";

    protected Class<? extends Relation> relationType = BasicRelation.class; // default relation class
    protected Method relationCreationMethod = null;
    //protected Constructor<Relation> relationConstructor = null;

    protected Class<?> confidenceType = BasicConfidence.class; // default confidence class

    protected Hashtable<Object,Set<Cell>> hash1 = null;

    protected Hashtable<Object,Set<Cell>> hash2 = null;

    protected long time = 0;

    protected Extensions extensions = null;

    protected Properties namespaces = null;

    public BasicAlignment() {
	hash1 = new Hashtable<Object,Set<Cell>>();
	hash2 = new Hashtable<Object,Set<Cell>>();
	extensions = new Extensions();
	namespaces = new Properties();
	if ( this instanceof AlignmentProcess ) setExtension( Namespace.EXT.uri, Annotations.METHOD, getClass().getName() );
	onto1 = new BasicOntology<Object>();
	onto2 = new BasicOntology<Object>();
    }

    /**
     * Initialises the Alignment object with two ontologies.
     * These two ontologies can be either an instance of fr.inrialpes.exmo.ontowrap.Ontology
     *        which will then replaced the one that was there at creation time
     * or a "concrete" ontology which will be inserted in the fr.inrialpes.exmo.ontowrap.Ontology
     * object.
     *
     * @param onto1 and
     * @param onto2: the two aligned ontologies
     * @param relType: the type of relations to use
     * @param confType: the type of confidence measures to use
     * @throws AlignmentException when something goes wrong
     */
    public void init( Object onto1, Object onto2, Class<? extends Relation> relType, Class<?> confType ) throws AlignmentException {
	relationType = relType;
	confidenceType = confType;
	init( onto1, onto2 );
    }

    public void init( Object onto1, Object onto2, Object cache ) throws AlignmentException {
	init( onto1, onto2 );
	// Should be interesting to set the pretty to: ontology names onto1-onto2
	//setExtension( Namespace.EXT.uri, Annotations.PRETTY, getClass().getName() );
    }

    @SuppressWarnings( "unchecked" )
    public void init( Object onto1, Object onto2 ) throws AlignmentException {
	try {
	    Class<?>[] cArg = { String.class };
	    relationCreationMethod = relationType.getMethod( "createRelation", cArg);
	} catch ( NoSuchMethodException nsmex ) {
	    // Should be different if NoSuchMethodException or IllegalAccessException
	    throw new AlignmentException ( "Cannot create relation of required type", nsmex );
	}
	if ( onto1 instanceof Ontology<?> ) {
	    this.onto1 = (Ontology<Object>)onto1; // [W:unchecked]
	    this.onto2 = (Ontology<Object>)onto2; // [W:unchecked]
	} else {
	    this.onto1.setOntology( onto1 );
	    this.onto2.setOntology( onto2 );
	}
	// Should return it
    }

    public static Properties getParameters() {
	return (Properties)null;
    }

    public int nbCells() {
	int sum = 0;
	for ( Set<Cell> s : hash1.values() ) {
	    sum += s.size();
	}
	return sum;
    }

    /* A few statistical primitives, undocumented */
    public double maxConfidence() {
	double result = 0.;
	for ( Cell c : this ) {
	    if ( c.getStrength() > result ) result = c.getStrength();
	}
	return result;
    }
    public double minConfidence() {
	double result = BasicConfidence.getTopConfidence();
	for ( Cell c : this ) {
	    if ( c.getStrength() < result ) result = c.getStrength();
	}
	return result;
    }
    public double avgConfidence() {
	double result = 0.;
	for ( Cell c : this ) {
	    result += c.getStrength();
	}
	return result/(double)nbCells();
    }
    public double varianceConfidence() {
	double total = 0.;
	double var = 0.;
	for ( Cell c : this ) {
	    var += c.getStrength() * c.getStrength();
	    total += c.getStrength();
	}
	double avg = total / (double)nbCells();
	return ( var / (double)nbCells() ) - (avg*avg) ;
    }
    // For standard deviation, take the square root of variance

    /* Alignment methods */
    public Object getOntology1() {
	return onto1.getOntology();
    };

    public Object getOntology2() {
	return onto2.getOntology();
    };

    public Ontology<Object> getOntologyObject1() {
	return onto1;
    };

    public Ontology<Object> getOntologyObject2() {
	return onto2;
    };

    public URI getOntology1URI() {
	return onto1.getURI();
    };

    public URI getOntology2URI() {
	return onto2.getURI();
    };

    public void setOntology1( Object ontology ) throws AlignmentException {
	onto1.setOntology( ontology );
    };

    public void setOntology2( Object ontology ) throws AlignmentException {
	onto2.setOntology( ontology );
    };

    public void setType( String type ) { this.type = type; };

    public String getType() { return type; };
    public String invertType() {
	if ( type != null && type.length() > 1 ) {
	    char txt[] = { type.charAt(1), type.charAt(0) };
	    return new String( txt );
	} else {
	    return "**";
	}
    };

    public void setRelationType( String classname ) throws AlignmentException {
	try {
	    Class<?> cl = Class.forName( classname );
	    if ( Relation.class.isAssignableFrom( cl ) ) {
		@SuppressWarnings("unchecked") // Yes, that's ridiculous...
		    Class<? extends Relation> r = (Class<? extends Relation>)cl;
		relationType = r;
	    } else {
		throw new AlignmentException( "Class "+classname+" does not implement the Relation interface" );
	    }
	} catch ( ClassNotFoundException cnfex ) {
	    throw new AlignmentException( "Unknown relation class : "+classname, cnfex );
	}
    };

    public Class<? extends Relation> getRelationType() { 
	return relationType; 
    };

    public void setConfidenceType( String classname ) throws AlignmentException {
	try {
	    confidenceType = Class.forName( classname );
	} catch ( ClassNotFoundException cnfex ) {
	    throw new AlignmentException( "Unknown relation class : "+classname, cnfex );
	}
    };

    public Class<?> getConfidenceType() { 
	return confidenceType; 
    };

    public void setLevel(String level) { this.level = level; };

    public String getLevel() { return level; };

    public URI getFile1() { return onto1.getFile(); };

    public void setFile1(URI u) { onto1.setFile( u ); };

    public URI getFile2() { return onto2.getFile(); };

    public void setFile2(URI u) { onto2.setFile( u ); };

    public Collection<String[]> getExtensions(){ return extensions.getValues(); }

    public Extensions getExtensionsObject(){ return extensions; }

    public void setExtensions( Extensions ext ){ extensions = ext; }

    public void setExtension( String uri, String label, String value ) {
	extensions.setExtension( uri, label, value );
    };

    public String getExtension( String uri, String label ) {
	return extensions.getExtension( uri, label );
    };

    public Properties getXNamespaces(){ return namespaces; }

    public void setXNamespace( String label, String uri ) {
	namespaces.setProperty( label, uri );
    };

    public String getXNamespace( String label ) {
	return namespaces.getProperty( label );
    };

    public Enumeration<Cell> getElements() {
	return new MEnumeration<Cell>( hash1 );
    }

    public Iterator<Cell> iterator() {
	return new MIterator<Cell>( hash1 );
    }

    public ArrayList<Cell> getArrayElements() {
	ArrayList<Cell> array = new ArrayList<Cell>();
	for ( Cell c : this ) {
	    array.add( c );
	}
	return array;
    }

    public void deleteAllCells() {
	hash1 = new Hashtable<Object,Set<Cell>>();
	hash2 = new Hashtable<Object,Set<Cell>>();
    }

    /* Cell methods */
    // Could use a relationType.getIdRelation() / confidenceType.getTop() -- But kept this way
    public Cell addAlignCell( Object ob1, Object ob2 ) throws AlignmentException {
	return addAlignCell( (String)null, ob1, ob2, "=", BasicConfidence.getTopConfidence() );
    }

    public Cell addAlignCell( Object ob1, Object ob2, String relation, double measure ) throws AlignmentException {
	return addAlignCell( (String)null, ob1, ob2, relation, measure );
    }

    public Cell addAlignCell( String id, Object ob1, Object ob2, String relation, double measure ) throws AlignmentException {
	return addAlignCell( id, ob1, ob2, createRelation( relation ), measure );
    };

    /**
     * The constructor to use
     *
     * @param rel: the string representing the relation
     * @throws AlignmentException when something goes wrong
     * @return a relation of the relevant type corresponding to the given string
     */
    public Relation createRelation( String rel ) throws AlignmentException {
	// Any of them... call the local createRelation()...
	// First try to understand the relation as correct
	try {
	    return (Relation)relationCreationMethod.invoke( null, rel );
	} catch ( Exception ex ) {
	    try { // Create a relation from classname
		Class<?> relationClass = (Class<?>)Class.forName(rel);
		if ( Relation.class.isAssignableFrom( relationClass ) ) {
		    Class<?>[] cArg = { String.class };
		    @SuppressWarnings("unchecked") // Yes, that's ridiculous...
			Constructor<Relation> localRelationConstructor = ((Class<Relation>)relationClass).getConstructor( cArg );
		    return localRelationConstructor.newInstance( "rel" );
		} else throw new AlignmentException( "Unknown relation class "+rel );
		// Exception will be caught just below
	    } catch ( Exception ex2 ) {
		logger.debug( "IGNORED Exception: created Basic Relation)", ex );
		//Otherwise, just create a Basic relation
		return new BasicRelation( rel );
	    }
	}
	/*
	} catch ( IllegalAccessException iaex ) {
	    // May be only a warning 
	    logger.error( "Cannot create relation (likely incorect relation string for {})", relationType.getName() );
	    throw new AlignmentException ( "Cannot create relation (likely incorect relation string)", iaex );
	} catch ( InvocationTargetException itex ) {
	    // May be only a warning
	    throw new AlignmentException ( "Cannot create relation (error in relation creation)", itex );
	} catch ( InstantiationException iex ) {
	    // May be only a warning
	    throw new AlignmentException ( "Cannot create relation", iex );
	    }*/
    }

    public Cell addAlignCell( String id, Object ob1, Object ob2, Relation relation, double measure ) throws AlignmentException {
	Cell cell = createCell( id, ob1, ob2, relation, measure);
	addCell( cell );
	return cell;
    }

    public BasicCell addAlignCell( String id, Object ob1, Object ob2, Relation relation, double measure, Extensions extensions ) throws AlignmentException {
	BasicCell cell = (BasicCell)addAlignCell( id, ob1, ob2, relation, measure );
	cell.setExtensions( extensions );
	return cell;
    }

    public BasicCell createCell( String id, Object ob1, Object ob2, Relation relation, double measure) throws AlignmentException {
	return new BasicCell( id, ob1, ob2, relation, measure);
    }

    // JE2014: Check the alternative
    // for ( Cell c1 : s1 ) {
    //		if ( c.equals( c1 ) ) { found = true; break; }
    //	    }
    // We cannot add twice cells with same content? ... OK
    protected void addCell( Cell c ) throws AlignmentException {
	boolean found = false;
	Set<Cell> s1 = hash1.get(c.getObject1());
	if ( s1 != null ) {
	    // I must check that there is no one here
	    for ( Iterator<Cell> i = s1.iterator(); !found && i.hasNext(); ) {
	    	if ( c.equals( i.next() ) ) { found = true; break; }
	    }
	    if (!found) s1.add( c );
	    found = false;
	} else {
	    s1 = new HashSet<Cell>();
	    s1.add( c );
	    hash1.put(c.getObject1(),s1);
	}
	Set<Cell> s2 = hash2.get(c.getObject2());
	if( s2 != null ){
	    // I must check that there is no one here
	    for ( Iterator<Cell> i=s2.iterator(); !found && i.hasNext(); ) {
	    	if ( c.equals( i.next() ) ) found = true;
	    }
	    if (!found)	s2.add( c );
	} else {
	    s2 = new HashSet<Cell>();
	    s2.add( c );
	    hash2.put(c.getObject2(),s2);
	}
    }

    public Set<Cell> getAlignCells1( Object ob ) throws AlignmentException {
	return hash1.get( ob );
    }
    public Set<Cell> getAlignCells2( Object ob ) throws AlignmentException {
	return hash2.get( ob );
    }
    /* undocumented */
    public Set<Cell> getAlignCells( Object ob1, Object ob2 ) throws AlignmentException {
	Set<Cell> result = new HashSet<Cell>();
	for( Cell c : hash1.get( ob1 ) ) {
	    if ( c.getObject2().equals( ob2 ) ) result.add( c );
	}
	return result;
    }


    /*
     * @deprecated implemented as the one retrieving the highest strength correspondence
     */
    public Cell getAlignCell1( Object ob ) throws AlignmentException {
	if ( Annotations.STRICT_IMPLEMENTATION == true ){
	    throw new AlignmentException("getAlignCell1: deprecated (use getAlignCells1 instead)");
	} else {
	    Set<Cell> s2 = hash1.get(ob);
	    Cell bestCell = null;
	    double bestStrength = 0.;
	    if ( s2 != null ) {
		for( Cell c : s2 ){
		    double val = c.getStrength();
		    if ( val > bestStrength ) {
			bestStrength = val;
			bestCell = c;
		    }
		}
	    }
	    return bestCell;
	}
    }

    public Cell getAlignCell2(Object ob) throws AlignmentException {
	if ( Annotations.STRICT_IMPLEMENTATION == true ){
	    throw new AlignmentException("getAlignCell2: deprecated (use getAlignCells2 instead)");
	} else {
	    Set<Cell> s1 = hash2.get(ob);
	    Cell bestCell = null;
	    double bestStrength = 0.;
	    if ( s1 != null ){
		for( Cell c : s1 ){
		    double val = c.getStrength();
		    if ( val > bestStrength ) {
			bestStrength = val;
			bestCell = c;
		    }
		}
	    }
	    return bestCell;
	}
    }

    /*
     * @deprecated
     */
    public Object getAlignedObject1(Object ob) throws AlignmentException {
	Cell c = getAlignCell1(ob);
	if (c != null) return c.getObject2();
	else return null;
    };

    /*
     * @deprecated
     */
    public Object getAlignedObject2(Object ob) throws AlignmentException {
	Cell c = getAlignCell2(ob);
	if (c != null) return c.getObject1();
	else return null;
    };

    /*
     * @deprecated
     */
    public Relation getAlignedRelation1(Object ob) throws AlignmentException {
	Cell c = getAlignCell1(ob);
	if (c != null) return c.getRelation();
	else return (Relation) null;
    };

    /*
     * @deprecated
     */
    public Relation getAlignedRelation2(Object ob) throws AlignmentException {
	Cell c = getAlignCell2(ob);
	if (c != null) return c.getRelation();
	else return (Relation) null;
    };

    /*
     * @deprecated
     */
    public double getAlignedStrength1(Object ob) throws AlignmentException {
	Cell c = getAlignCell1(ob);
	if (c != null) return c.getStrength();
	else return 0;
    };

    /*
     * @deprecated
     */
    public double getAlignedStrength2(Object ob) throws AlignmentException {
	Cell c = getAlignCell2(ob);
	if (c != null) return c.getStrength();
	else return 0;
    };

    // JE: beware this does only remove the exact equal cell
    // not those with same value
    public void remCell( Cell c ) throws AlignmentException {
	Set<Cell> s1 = hash1.get(c.getObject1());
	if ( s1 != null ) s1.remove( c );
	if (s1.isEmpty()) hash1.remove(c.getObject1());
	Set<Cell> s2 = hash2.get(c.getObject2());
	if( s2 != null ) s2.remove( c );
	if (s2.isEmpty()) hash2.remove(c.getObject2());
    }

    /*
     * @deprecated
     */
    public void removeAlignCell( Cell c ) throws AlignmentException {
	remCell( c );
    }

    /***************************************************************************
     * The cut function suppresses from an alignment all the cell over a
     * particular threshold
     *
     * @param threshold: the threshold under which to cut
     * @throws AlignmentException when something goes wrong (unlikely)
     **************************************************************************/
    public void cut2(double threshold) throws AlignmentException {
	for ( Cell c : this ) {
	    if ( c.getStrength() < threshold ) remCell( c );
	}
    }

    /***************************************************************************
     * Default cut implementation
     * For compatibility with API until version 1.1
     *
     * @param threshold: the threshold under which to cut
     * @throws AlignmentException when something goes wrong (unlikely)
     **************************************************************************/
    public void cut( double threshold ) throws AlignmentException {
	cut( "hard", threshold );
    }

    /***************************************************************************
     * Cut refinement :
     * Rule:
     * threshold is betweew 1 and 0
     *
     * @param method: the cut method to be used described as:
     * - getting those cells with strength above n (hard)
     * - getting the n best cells (best)
     * - getting those cells with strength at worse n under the best (span)
     * - getting the n% best cells (perc)
     * - getting those cells with strength at worse n% of the best (prop)
     * - getting all cells until a gap of n (hardgap)
     * - getting all cells until a gap of n% of the last (propgap)
     * @param threshold: the threshold under which to cut
     * @throws AlignmentException when something goes wrong (unlikely)
     **************************************************************************/
    public void cut( String method, double threshold ) throws AlignmentException
    {
	// Check that threshold is a percent
	if ( threshold > 1. || threshold < 0. )
	    throw new AlignmentException( "Not a percentage or threshold : "+threshold );
	// Create a sorted list of cells
	// For sure with sorted lists, we could certainly do far better
	List<Cell> buffer = getArrayElements();
	Collections.sort( buffer );
	int size = buffer.size();
	int i = 0; // the number of cells to keep
	// Depending on the method, find the limit
	if ( method.equals("perc") ){
	    i = (new Double(size*threshold)).intValue();
	} else if ( method.equals("best") ){
	    i = java.lang.Math.min( size, new Double(threshold*100).intValue() );
	} else if ( method.equals("hardgap") || method.equals("propgap") ){
	    double gap;
	    double last = buffer.get(0).getStrength();
	    if ( method.equals("propgap") ) gap = last * threshold;
	    else gap = threshold;
	    for( i=1; i < size ; i++ ) {
		if ( last - buffer.get(i).getStrength() > gap ) break;
		else {
		    last = buffer.get(i).getStrength();
		    if ( method.equals("propgap") ) gap = last * threshold;
		}
	    }
	} else {
	    double max;
	    if ( method.equals("hard") ) max = threshold;
	    else if ( method.equals("span") ) max = buffer.get(0).getStrength() - threshold;
	    else if ( method.equals("prop") ) max = buffer.get(0).getStrength() * threshold;
	    else throw new AlignmentException( "Not a cut specification : "+method );
	    for( i=0; i < size ; i++) {
		if ( buffer.get(i).getStrength() < max ) break;
	    }
	}
	// Introduce the result back in the structure
	size = i;
	hash1.clear();
	hash2.clear();
	for( i=0; i < size; i++ ) {
	    addCell( buffer.get(i) );
	}
    };

    /**
     * The harden function acts like threshold but put all weights to 1.
     *
     * @param threshold: the threshold above which to harden
     * @throws AlignmentException when something goes wrong (unlikely)
     */
    public void harden(double threshold) throws AlignmentException {
	for ( Cell c : this ) {
	    if (c.getStrength() < threshold) remCell( c );
	    else c.setStrength( BasicConfidence.getTopConfidence() );
	}
    }

    /**
     * Algebraic part
     * This is to be improved by (TODO):
     * - improving cell equivalence (maybe not dependent on the confidence... and
     *     grounding it on abstract data types)
     * - using algebraic meet and join for relations and confidences
     *     (the type of relation used can be declared in the alignment)
     * - check compatibility and setup for type and level
     * - conserve extensions if necessary
     */
    /**
     * This method is used by the algebraic operators
     * It has to be overriden by implementations.
     *
     * @param onto1 and
     * @param onto2: the two aligned ontologies
     * @param relType: the type of relations to use
     * @param confType: the type of confidence measures to use
     * @throws AlignmentException when something goes wrong
     * @return a newly created alignment
     */
    public BasicAlignment createNewAlignment( Object onto1, Object onto2, Class<? extends Relation> relType, Class<?> confType ) throws AlignmentException {
	BasicAlignment align = new BasicAlignment();
	align.init( onto1, onto2, relType, confType );
	return align;
    }

   /**
     * The second alignment is suppresed from the first one meaning that for
     * any pair (o, o', n, r) in O and (o, o', n', r) in O' the resulting
     * alignment will contain:
     * ( o, o', diff(n,n'), r)
     * any pair which is only in the first alignment is preserved.
     * 
     * @param align: the alignment which is substracted
     * @throws AlignmentException when something goes wrong
     * @return the difference between the current alignment and align
     */
    public Alignment diff( Alignment align ) throws AlignmentException {
	// Could also test: onto1 == getOntologyObject1();
	if ( !onto1.getURI().equals(align.getOntology1URI()) )
	    throw new AlignmentException("Can only diff alignments with same ontologies");
	if ( !onto2.getURI().equals(align.getOntology2URI()) )
	    throw new AlignmentException("Can only diff alignments with same ontologies");
	BasicAlignment result = createNewAlignment( onto1, onto2, relationType, confidenceType );
	for ( Cell c1 : this ) {
	    Set<Cell> s2 = align.getAlignCells1( c1.getObject1() );
	    boolean found = false;
	    if ( s2 != null ){
		for ( Cell c2 : s2 ){
		    //if ( uri1.toString().equals(c2.getObject2AsURI().toString()) ) {
		    if ( c1.equals( c2 ) ) {
			found = true;
		    }
		}
	    }
	    if ( !found ) result.addCell( c1 );
	}
	return result;
    }

    /* JE: If I define equals, I must define hashCode! */
    public boolean equals( Alignment align ) {
	if ( align instanceof BasicAlignment ) {
	    return ( this.onto1.equals( ((BasicAlignment)align).onto1 )
		     && this.onto2.equals( ((BasicAlignment)align).onto2 )
		     && this.contains( align ) 
		     && ((BasicAlignment)align).contains( this ) );
	} else return false;
	}

    public int hashCode() {
	// not using java.util.Objects because equality not based on hash
	//return Objects.hash( onto1, onto2, hash1, hash2 );
	int result = 77 + 13*onto1.hashCode() + 17*onto2.hashCode() ;
	for ( Cell c : this ) {
	    result = 7*result + c.hashCode();
	}
	return result;
    }

    /**
     * Each cell of @align is contained in this alignment
     * no semantics (a semantic predicate would be entails).
     * 
     * @param align: the alignment for testing containment
     * @return true is align is contained
     */
    public boolean contains( Alignment align ) {
	try {
	    for ( Cell c : align ) {
		Set<Cell> sc = getAlignCells1( c.getObject1() );
		if ( sc == null ) return false;
		boolean found = false;
		for ( Cell c2 : sc ) {
		    if ( c.equals( c2 ) ) {
			found = true;
			break;
		    }
		}
		if ( !found ) return false;
	    }
	} catch (AlignmentException alex) {
	    logger.debug( "IGNORED Exception in contains tests", alex );
	    return false;
	}
	return true;
    }

   /**
     * Aggregating several alignments with aggregator modality applied on confidence
     * For any set (o, o', n, r) in O and (o, o', n', r) in O' the resulting
     * alignment will contain:
     * ( o, o', aggr(n,n'), r)
     * any pair which is in only one alignment is preserved.
     *
     * This does not pay any attention to the relations used: it returns a random relation!
     * 
     * @param modality: the aggregation modality
     * @param aligns: the alignments to be aggregated
     * @throws AlignmentException when something goes wrong
     * @return the aggregated alignment
     */
    public static Alignment aggregate( String modality, Set<BasicAlignment> aligns ) throws AlignmentException {
	// Could also test: onto1 == getOntologyObject1();
	logger.debug(" Call {} with {}", modality, aligns );
	int size = aligns.size();
	if ( size == 0 ) return (Alignment)null;
	BasicAlignment first = aligns.iterator().next();
	logger.debug(" Size: {}, Alignment {}", size, first );
	Ontology<? extends Object> onto1 = first.getOntologyObject1();
	Ontology<? extends Object> onto2 = first.getOntologyObject2();
	logger.debug(" Onto1: {}, Onto2 {}", onto1, onto2 );
	for ( Alignment al : aligns ) {
	    if ( !onto1.getURI().equals(al.getOntology1URI()) )
		throw new AlignmentException( "Can only meet alignments with same ontologies" + onto1.getURI() + " <> " + al.getOntology1URI() );
	    if ( !onto2.getURI().equals(al.getOntology2URI()) )
		throw new AlignmentException( "Can only meet alignments with same ontologies" + onto2.getURI() + " <> " + al.getOntology2URI() );
	}
	BasicAlignment result = new BasicAlignment();
	result.init( onto1, onto2 );
	result.setExtension( Namespace.EXT.uri, Annotations.METHOD, "fr.inrialpes.exmo.align.impl.BasicAlignment#aggregate" );
	Hashtable<Object,Hashtable<Object,List<Cell>>> lcells = new Hashtable<Object,Hashtable<Object,List<Cell>>>();
	// Collect all alignments...
	logger.debug( "Collect all alignments..." );
	for ( Alignment al : aligns ) {
	    //logger.trace(" AL: {}", al );
	    for ( Cell c : al ) {
		//logger.trace( "  {} -- {}", c.getObject1(), c.getObject2() );
		Hashtable<Object,List<Cell>> h = lcells.get( c.getObject1() );
		if ( h == null ) {
		    h = new Hashtable<Object,List<Cell>>();
		    lcells.put( c.getObject1(), h );
		}
		List<Cell> s = h.get( c.getObject2() );
		if ( s == null ) {
		    s = new Vector<Cell>();
		    h.put( c.getObject2(), s );
		}
		s.add( c );
	    }
	}
	// iterate on all cells
	logger.debug( "Iterate on all cells" );
	for ( Hashtable<Object,List<Cell>> h: lcells.values() ) {
	    for ( List<Cell> s: h.values() ) {
		Cell cell = s.iterator().next(); // yes dangerous
		// Aggregate them depending on modality
		double val = 0.;
		if ( modality.equals("min") ) {
		    if ( size <= s.size() ) {
			val = 1.;
			for ( Cell c: s ) if ( c.getStrength() < val ) val = c.getStrength();
		    }
		} else if ( modality.equals("max") ) {
		    for ( Cell c: s ) if ( c.getStrength() > val ) val = c.getStrength();
		} else if ( modality.equals("avg") ) {
		    for ( Cell c: s ) val += c.getStrength();
		    val = val/(double)size;
		} else if ( modality.equals("pool") ) {
		    val = (double)(s.size())/(double)size;
		}
		// This assumes that the relation is "=".
		if ( val > 0. ) result.addAlignCell( null, cell.getObject1(), cell.getObject2(), BasicRelation.createRelation("="), val );
	    }
	}
	return result;
    }

    /**
     * Generates a sorted iterator for the alignment
     * WARNING: this is quite expensive since it allocates and sort a structure each time
     *
     * @return the set of cells in a sorted structure
     */
    public TreeSet<Cell> getSortedIterator() {
	TreeSet<Cell> result = new TreeSet<Cell>();
	for ( Collection<Cell> cc : hash1.values() ) {
	    for ( Cell c : cc ) {
		result.add( c );
	    }
	}
	return result;
    }

   /**
    * V5:  meet and join could be implemented with variable arity (...).
    * This is not possible without changing API.
    *
    * JE2015: Meet and Join (and others?) Must be reimplemented so that, if object1 and
    * object2 are the same, then the cell is meet or join.
    * Similarly, cell meet and join must rely on corresponding operations on relations.
    * In this case, Cell operations may return a set of cells.
    */

   /**
     * The second alignment is meet with the first one meaning that for
     * any pair (e, e', n, r) in A and (e, e', n', r) in A' the resulting
     * alignment will contain:
     * ( e, e', meet(n,n'), r)
     * any pair which is in only one alignment is discarded.
     *
     * @param align:
     */
    public Alignment meet( Alignment align ) throws AlignmentException {
	// Could also test: onto1 == getOntologyObject1();
	if ( ! onto1.getURI().equals(align.getOntology1URI()) )
	    throw new AlignmentException("Can only meet alignments with same ontologies");
	if ( ! onto2.getURI().equals(align.getOntology2URI()) )
	    throw new AlignmentException("Can only meet alignments with same ontologies");
	BasicAlignment result = createNewAlignment( onto1, onto2, relationType, confidenceType );
	for ( Cell c1 : this ) {
	    Set<Cell> s2 = align.getAlignCells1( c1.getObject1() );
	    if ( s2 != null ){
		Set<Cell> found = new HashSet<Cell>();
		for ( Cell c2 : s2 ){
		    if ( c1.getObject2().equals( c2.getObject2() ) ) {
			found.add( c2 );
		    }
		}
		for ( Cell c : found ) {
		    for ( Cell cm : ((BasicCell)c).meet( c1 ) ) result.addCell( cm );
		}
	    }
	}
	result.normalise();
	return result;
    }

   /**
     * The second alignment is join with the first one meaning that for
     * any pair (e, e', n, r) in A and (e, e', n', r) in A' the resulting
     * alignment will contain:
     * ( e, e', join(n,n'), r)
     * any pair which is in only one alignment is preserved.
     */
    public Alignment join( Alignment align ) throws AlignmentException {
	// Could also test: onto1 == getOntologyObject1();
	if ( onto1.getURI() != align.getOntology1URI() )
	    throw new AlignmentException( "Can only join alignments with same ontologies: ("+onto1.getURI()+" - "+align.getOntology1URI()+")");
	if ( onto2.getURI() != align.getOntology2URI() )
	    throw new AlignmentException("Can only join alignments with same ontologies: ("+onto2.getURI()+" - "+align.getOntology2URI()+")");
	BasicAlignment result = createNewAlignment( onto1, onto2, relationType, confidenceType );
	for ( Cell c1 : this ) {
	    Set<Cell> s2 = align.getAlignCells1( c1.getObject1() );
	    if ( s2 != null ) {
		Set<Cell> found = new HashSet<Cell>();
		for ( Cell c2 : s2 ){
		    if ( c1.getObject2().equals( c2.getObject2() ) ) {
			found.add( c2 );
		    }
		}
		if ( found.isEmpty() ) {
		    result.addCell( c1 );
		} else {
		    for ( Cell c : found ) {
			for ( Cell cm : ((BasicCell)c).join( c1 ) ) result.addCell( cm );
		    }
		}
	    } else {
		// again, no new cell
		result.addCell( c1 );
	    }
	}
	for ( Cell c2 : align ) {
	    Set<Cell> sc = getAlignCells1( c2.getObject1() );
	    if ( sc == null ) {
		// again, no new cell
		result.addCell( c2 );
	    } else {
		boolean toadd = true;
		for ( Cell c1 : sc ) {
		    if ( c1.getObject2().equals( c2.getObject2() ) ) {
			toadd = false;
			break;
		    }
		}
		if ( toadd ) result.addCell( c2 );
	    }
	}
	result.normalise();
	return result;
    }

    /**
     * The second alignment is composed with the first one meaning that for
     * any pair (o, o', n, r) in O and (o',o", n', r') in O' the resulting
     * alignment will contain:
     * ( o, o", join(n,n'), compose(r, r')) iff compose(r,r') exists.
     *
     * @param align: the alignment to compose with this one
     * @return the composed alignments
     * @throws AlignmentException when something goes wrong (e.g., the two alignments are not connected)
     */
    public Alignment compose( Alignment align ) throws AlignmentException {
	if ( !onto2.getURI().equals( align.getOntology1URI() ) )
	    throw new AlignmentException("Can only compose alignments with a common ontologies");
	BasicAlignment result = createNewAlignment( onto1, ((BasicAlignment)align).getOntologyObject2(), relationType, confidenceType );
	for ( Cell c1 : this ) {
	    Set<Cell> cells2 = align.getAlignCells1(c1.getObject2());
	    if ( cells2 != null ) {
		for ( Cell c2 : cells2 ) {
		    Cell newCell = c1.compose(c2);
		    //logger.debug( "{} o {} = {}", c1.getRelation(), c2.getRelation(), newCell.getRelation() );
		    if ( newCell != null ) result.addCell( newCell );
		}
	    }
	}
	result.normalise();
	return result;
    }

    // This will reduce cells with correspondences with common objects
    public void normalise() throws AlignmentException {
	Set<Cell> todel = new HashSet<Cell>();
	for ( Cell c1 : this ) {
	    if ( !todel.contains( c1 ) ) {
		Set<Cell> cells2 = getAlignCells1(c1.getObject1());
		if ( cells2.size() > 1 ) {
		    Object o2 = c1.getObject2();
		    Set<Cell> totreat = new HashSet<Cell>();
		    for ( Cell c2: cells2 ) {
			if ( o2.equals( c2.getObject2() ) ) {
			    totreat.add( c2 );
			} 
		    }
		    if ( totreat.size() > 1 ) {
			double confidence = c1.getStrength();
			BasicRelation relation = (BasicRelation)c1.getRelation();
			for( Cell c: totreat ) {
			    if ( c != c1 ) {
				confidence = BasicConfidence.conjunction( confidence, c.getStrength() );
				// No chance that it is not a basic relation, but if yes, what?
				if ( relation != null && c.getRelation() instanceof BasicRelation ) {
				    relation = relation.join( c.getRelation() );
				}
				todel.add( c );
			    }
			    if ( relation != null ) { // composition is not undefined (<>empty)
				c1.setStrength( confidence );
				c1.setRelation( relation );
			    }
			}
		    }
		}
	    }
	}
	for( Cell c : todel ) remCell( c );
    }

    /**
     * A new alignment is created such that for
     * any pair (o, o', n, r) in O the resulting alignment will contain:
     * ( o', o, n, inverse(r)) iff compose(r) exists.
     */

    public BasicAlignment inverse() throws AlignmentException {
	BasicAlignment result = createNewAlignment( onto2, onto1, relationType, confidenceType );
	invertContent( result, "inverted", "http://exmo.inrialpes.fr/align/impl/BasicAlignment#inverse" );
	return result;
    }

    public void invertContent( BasicAlignment result, String label, String method ) throws AlignmentException {
	result.setFile1( getFile2() );
	result.setFile2( getFile1() );
	result.setType( invertType() );
	result.setLevel( getLevel() );
	result.setExtensions( extensions.convertExtension( label, method ) );
	for ( Entry<Object,Object> e : namespaces.entrySet() ) {
	    result.setXNamespace( (String)e.getKey(), (String)e.getValue() );
	}
	for ( Cell c : this ) {
	    result.addCell( c.inverse() );
	}
    }

    /** Housekeeping **/
    public void dump(ContentHandler h) {
    };

    /**
     * Incorporate the cells of the alignment into its own alignment. Note: for
     * the moment, this does not copy but really incorporates. So, if hardening
     * is applied, then the ingested alignmment will be modified as well.
     * JE: May be a "force" boolean for really ingesting or copying may be
     *     useful
     *
     * @param alignment: the alignment to be included in the current alignment
     * @throws AlignmentException when something goes wrong
     */
    public void ingest( Alignment alignment ) throws AlignmentException {
	if ( alignment != null )
	    for ( Cell c : alignment ) 
		addCell( c );
    }

    /**
     * Generate a copy of this alignment object
     * It has the same content but a different id (no id indeed)
     *
     * @return a clone of this alignment
     */
    public Object clone() {
	BasicAlignment align;
	try { align = createNewAlignment( onto1, onto2, relationType, confidenceType ); }
	catch (AlignmentException ae) { 
	    logger.debug( "IGNORED Exception: alignment not cloned", ae );
	    return null; 
	}
	align.setType( getType() );
	align.setLevel( getLevel() );
	align.setFile1( getFile1() );
	align.setFile2( getFile2() );
	align.setExtensions( extensions.convertExtension( "cloned", this.getClass().getName()+"#clone" ) );
	for ( Entry<Object,Object> e : namespaces.entrySet() ) {
	    align.setXNamespace( (String)e.getKey(), (String)e.getValue() );
	}
	try { align.ingest( this ); }
	catch (AlignmentException ex) { 
	    logger.debug( "IGNORED Exception: alignment not ingested", ex );
	}
	return align;
    }

    /**
     * Returns default exception for conversion to URIAlignments
     *
     * @return a URI alignment corresponding to this alignment
     * @throws AlignmentException when something goes wrong (the alignment cannot be converted)
     */
    public URIAlignment toURIAlignment() throws AlignmentException {
	throw new AlignmentException("[BasicAlignment].toURIAlignment() cannot process");
    }

    /**
     * This should be rewritten in order to generate the axiom ontology instead
     * of printing it! And then use ontology serialization for getting it
     * printed.
     *
     * @param renderer: the renderer with which to render the alignment
     * @throws AlignmentException when something goes wrong
     */
    public void render( AlignmentVisitor renderer ) throws AlignmentException {
	accept(renderer);
    }

    /**
     * Can be used for reducing the amount of memory taken by an alignment
     * Does nothing in BasicAlignment.
     */
    public void cleanUp() {}

    /**
     * - replaces all entity IRI by their counterpart in the ontology
     *
     * Caveats:
     * - This does only work for alignments with =
     * - This does not care for the *:x status of alignments
     * - This does work from ontology1 to ontology2, not the other way around
     *    (use invert() in this case).
     * @param aQuery -- query to be re-written
     * @return -- rewritten query:
     * @throws AlignmentException when something goes wrong
     */    
    public String rewriteQuery( String aQuery ) throws AlignmentException {
	return rewriteSPARQLQuery( aQuery );
    }

    public String rewriteQuery( String aQuery, Properties prefix ) throws AlignmentException {
	return rewriteSPARQLQuery( aQuery, prefix );
    }

    public String rewriteSPARQLQuery( String aQuery ) throws AlignmentException {
	return rewriteSPARQLQuery( aQuery, new Properties() );
    }

    public String rewriteSPARQLQuery( String aQuery, Properties prefix ) throws AlignmentException {
	// The first part expands the prefixes of the query
	aQuery = aQuery.trim().replaceAll("PREFIX", "prefix");
        String mainQuery = ""; 

	// Collect and reduce prefix
        if( aQuery.indexOf("prefix") != -1 )  {
            String[] pref = aQuery.split("prefix");               
            for(int j=0; j < pref.length; j++)  {
                String str = "";
                if(!pref[0].equals(""))   
                    str = pref[0];
                else
                    str = pref[pref.length-1];
                mainQuery = str.substring(str.indexOf('>')+1, str.length());
            }

            for( int i = 0; i < pref.length; i++ )  {   
                String currPrefix = pref[i].trim();       
                if(!currPrefix.equals("") && currPrefix.indexOf('<') != -1 && currPrefix.indexOf('>') != -1)  {
                    int begin = currPrefix.indexOf('<');
                    int end = currPrefix.indexOf('>');
                    String ns = currPrefix.substring(0, currPrefix.indexOf(':')).trim();
                    String iri = currPrefix.substring(begin+1, end).trim();
		    prefix.setProperty( ns, iri );
		    mainQuery = Pattern.compile(ns+":([A-Za-z0-9_-]+)").matcher(mainQuery).replaceAll("<"+iri+"$1>");
                }
            }
        } else mainQuery = aQuery;

	mainQuery = translateMessage( mainQuery );

	// Post process prefix
	for ( Entry<Object,Object> m : prefix.entrySet() ) {
	    if ( m.getKey() != null ) {
		mainQuery = Pattern.compile("<"+m.getValue()+"([A-Za-z0-9_-]+)>").matcher(mainQuery).replaceAll( m.getKey()+":$1" );
		mainQuery = "PREFIX "+m.getKey()+": <"+m.getValue()+"> \n" + mainQuery;
	    }
	}

        return mainQuery;
    }

    // I hope that this works with subclasses
    public String translateMessage( String query ) {
	String result = query;
	try {
	    // The second part replaces the named items by their counterparts
	    for( Cell cell : this ){
                URI uri1 = cell.getObject1AsURI(this);
                URI uri2 = cell.getObject2AsURI(this);
                if(uri1 != null && uri2 != null){
		result = result.replaceAll( cell.getObject1AsURI(this).toString(),
					    cell.getObject2AsURI(this).toString() );
                }
	    }
	} catch (AlignmentException alex) {
	    logger.debug( "IGNORED AlignmentException {}", alex );
	}
	return result;
    }

}

class MEnumeration<T> implements Enumeration<T> {
    private Enumeration<Set<T>> set = null; // The enumeration of sets
    private Iterator<T> current = null; // The current set's enumeration

    MEnumeration( Hashtable<Object,Set<T>> s ){
	set = s.elements();
	while( set.hasMoreElements() && current == null ){
	    current = set.nextElement().iterator();
	    if( !current.hasNext() ) current = null;
	}
    }
    public boolean hasMoreElements(){
	return ( current != null);
    }
    public T nextElement(){
	T val = current.next();
	if( !current.hasNext() ){
	    current = null;
	    while( set.hasMoreElements() && current == null ){
		current = set.nextElement().iterator();
		if( !current.hasNext() ) current = null;
	    }
	}
	return val;
    }

}

class MIterator<T> implements Iterator<T> {
    // Because of the remove, the implentation should be different
    // Keeping the last element at hand
    private Enumeration<Set<T>> set = null; // The enumeration of sets
    private Iterator<T> current = null; // The current set's enumeration
    private Iterator<T> next = null; // The next set enumeration

    MIterator( Hashtable<Object,Set<T>> s ){
	set = s.elements();
	if ( set.hasMoreElements() ) {
	    current = set.nextElement().iterator();
	    if ( current.hasNext() ) {
		next = current;
	    } else {
		while( set.hasMoreElements() && next == null ){
		    next = set.nextElement().iterator();
		    if( !next.hasNext() ) next = null;
		}
	    }
	}
    }
    public boolean hasNext(){
	return ( next != null && next.hasNext() );
    }
    public T next(){
	current = next;
	T val = current.next();
	if( !current.hasNext() ){
	    next = null;
	    while( set.hasMoreElements() && next == null ){
		next = set.nextElement().iterator();
		if( !next.hasNext() ) next = null;
	    }
	}
	return val;
    }
    public void remove(){
	if ( current != null ) current.remove();
    }
}
