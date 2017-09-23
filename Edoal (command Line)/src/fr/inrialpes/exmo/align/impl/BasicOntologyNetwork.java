/*
 * $Id: BasicOntologyNetwork.java 2125 2017-02-11 08:42:25Z euzenat $
 *
 * Copyright (C) INRIA, 2009-2010, 2014-2015
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

import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Iterator;
import java.net.URI;
import java.net.URISyntaxException;
import java.lang.reflect.Constructor;

import java.io.PrintWriter;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDF;

import fr.inrialpes.exmo.ontowrap.Ontology;
import fr.inrialpes.exmo.ontowrap.BasicOntology;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

import fr.inrialpes.exmo.align.parser.SyntaxElement;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import fr.inrialpes.exmo.align.parser.RDFParser;
import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.impl.Extensible;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.OntologyNetwork;

/**
 * Represents a distributed system of aligned ontologies or network of ontologies.
 *
 */

public class BasicOntologyNetwork implements OntologyNetwork, Extensible {
    final static Logger logger = LoggerFactory.getLogger( BasicOntologyNetwork.class );

    protected Hashtable<URI,OntologyTriple> ontologies;
    protected HashSet<Alignment> alignments;

    protected Extensions extensions = null;
    
    protected HashMap<URI,Map<URI,Set<Alignment>>> onto2Align;

    public BasicOntologyNetwork(){
	extensions = new Extensions();
	ontologies = new Hashtable<URI,OntologyTriple>();
	alignments = new HashSet<Alignment>();
	onto2Align = new HashMap<URI,Map<URI,Set<Alignment>>>();
    }

    public void addOntology( Ontology<Object> onto ){
	URI u = onto.getURI();
	if ( ontologies.get( u ) == null )
	    ontologies.put( u, new OntologyTriple( onto ) );
    };
    public void addOntology( URI u ){
	if ( ontologies.get( u ) == null ) {
	    Ontology<Object> onto = new BasicOntology<Object>();
	    onto.setURI( u );
	    ontologies.put( u, new OntologyTriple( onto ) );
	}
    };
    public void remOntology( URI onto ) throws AlignmentException {
	OntologyTriple ot = ontologies.get( onto );
	if ( ot != null ) {
	    for( Alignment al : ot.sourceAlignments ){
		remAlignment( al );
	    }
	    for( Alignment al : ot.targettingAlignments ){
		remAlignment( al );
	    }
	    ontologies.remove( onto ); // Or set to null
	    
	    onto2Align.remove(onto);
	    for (Map<URI,Set<Alignment>> m : onto2Align.values())
		m.remove(onto);  
	}
    };
    public void addAlignment( Alignment al ) throws AlignmentException {
	URI o1 = al.getOntology1URI();
	addOntology( o1 );
	ontologies.get( o1 ).sourceAlignments.add( al );
	URI o2 = al.getOntology2URI();
	addOntology( o2 );
	ontologies.get( o2 ).targettingAlignments.add( al );
	alignments.add( al );
	
	Map<URI,Set<Alignment>> m = onto2Align.get(al.getOntology1URI());
	if (m==null) {
	    m=new HashMap<URI,Set<Alignment>>();
	    onto2Align.put(al.getOntology1URI(), m);
	}
	Set<Alignment> aligns=m.get(al.getOntology2URI());
	if (aligns==null) {
	    aligns = new HashSet<Alignment>();
	    m.put(al.getOntology2URI(), aligns);
	}
	aligns.add(al);
    }; 
    public void remAlignment( Alignment al ) throws AlignmentException {
	ontologies.get( al.getOntology1URI() ).sourceAlignments.remove( al );
	ontologies.get( al.getOntology2URI() ).targettingAlignments.remove( al );
	alignments.remove( al );
	onto2Align.get(al.getOntology1URI()).get(al.getOntology2URI()).remove(al);
    };
    public void cleanUpAlignments() {
	for ( OntologyTriple ot : ontologies.values() ) {
	    ot.cleanUpAlignments();
	}
	alignments.clear();
	for ( Map<URI,Set<Alignment>> m : onto2Align.values() ) {
	    for ( Set<Alignment> s : m.values() ) {
		s.clear();
	    }
	}
    };
    public Set<Alignment> getAlignments(){
	return alignments;
    };
    public int nbCells() {
	int nbcells = 0; 
	for ( Alignment al : alignments ) nbcells += al.nbCells();
	return nbcells;
    }

    public Set<URI> getOntologies() {
	return ontologies.keySet(); // ??
    };
    public Ontology<Object> getOntologyObject( URI u ) {
	OntologyTriple ot = ontologies.get( u );
	if ( ot == null ) return null;
	return ot.onto;
    };
    public Set<Alignment> getTargetingAlignments( URI onto ){
	OntologyTriple ot = ontologies.get( onto );
	if ( ot == null ) return Collections.emptySet();
	return ot.targettingAlignments;
    }
    public Set<Alignment> getSourceAlignments( URI onto ){
	OntologyTriple ot = ontologies.get( onto );
	if ( ot == null ) return Collections.emptySet();
	return ot.sourceAlignments;
    }
    /*
      The one below is more efficient (as soon as the structure is maintained)
      Thus structure is not necessary... it should have been put rather in ontoTriple!
    public Set<Alignment> getAlignments( URI onto1, URI onto2 ){
	OntologyTriple ot1 = ontologies.get( onto1 );
	OntologyTriple ot2 = ontologies.get( onto2 );
	if ( ot1 == null || ot2 == null ) return Collections.emptySet();
	return intersectAlignments( ot1.sourceAlignments, ot2.targettingAlignments );
    }
    */

    public Set<Alignment> getAlignments( URI srcOnto, URI dstOnto ) {

	Map<URI,Set<Alignment>> m = onto2Align.get(srcOnto);
	if ( m != null ) {
	    Set<Alignment> aligns = m.get(dstOnto);
	    if ( aligns != null ) return Collections.unmodifiableSet( aligns );
	}
	return Collections.emptySet();
    }
    protected Set<Alignment> getAlignments( OntologyTriple srcTriple, OntologyTriple dstTriple ) {
	if ( srcTriple == null || dstTriple == null ) return Collections.emptySet();
	if ( srcTriple.sourceAlignments.isEmpty() || dstTriple.targettingAlignments.isEmpty() ) return Collections.emptySet();
	// It is likely that using onto2Align will be faster... !
	Set<Alignment> result = new HashSet<Alignment>();
	for ( Alignment al : srcTriple.sourceAlignments ) {
	    if ( dstTriple.targettingAlignments.contains( al ) ) result.add( al );
	}
	return result;
    }

    /**
     * Clone does some deeper cloning
     * It has the same content but a different id (no id indeed)
     */
    public BasicOntologyNetwork clone() {
	//public Object clone() {
	BasicOntologyNetwork network = copyOnto();
	network.setExtensions( extensions.convertExtension( "cloned", this.getClass().getName()+"#clone" ) );
	for ( Alignment al : alignments ) {
	    try { 
		network.addAlignment( al );
	    } catch (AlignmentException alex) {
		logger.debug( "IGNORED Exception : should not happen {}", al );
	    }
	}
	return network;
    }

    /**
     * Clone does some deeper cloning
     * It has the same content but a different id (no id indeed)
     *
     * @return a new network with the same set of ontologies
     */
    public BasicOntologyNetwork copyOnto() {
	//public Object clone() {
	BasicOntologyNetwork network = new BasicOntologyNetwork();
	for ( URI onto : ontologies.keySet() ) network.addOntology( onto );
	return network;
    }

    /**
     * Invert all alignments in a network of ontologies
     * In fact, this is closing by invert!
     * Modifies the network
     *
     * @throws AlignmentException when something goes wrong (unlikely)
     */
    public void invert() throws AlignmentException {
	Vector<Alignment> newal = new Vector<Alignment>();
	for ( Alignment al : alignments ) newal.add( al.inverse() );
	for ( Alignment al : newal ) addAlignment( al );
    }

    /**
     * Denormalizes an ontology network: suppress all empty alignments
     * Modifies the network
     *
     * @throws AlignmentException when something goes wrong (unlikely)
     */
    public void denormalize() throws AlignmentException {
	Vector<Alignment> toremove = new Vector<Alignment>();
	for ( Alignment al : alignments ) {
	    if ( al.nbCells() == 0 ) toremove.add( al );
	}
	for ( Alignment al : toremove ) remAlignment( al );
    }

    /**
     * Normalizes an ontology network for it to have exactly one alignment between each pair of ontologies.
     * Modifies the network
     *
     * @throws AlignmentException when something goes wrong (unlikely)
     */
    public void normalize() throws AlignmentException {
	for ( OntologyTriple ot1 : ontologies.values() ) {
	    for ( OntologyTriple ot2 : ontologies.values() ) {
		Set<Alignment> als = intersectAlignments( ot1.sourceAlignments, ot2.targettingAlignments );
		if ( als.isEmpty() ) {
		    Alignment result = new BasicAlignment();
		    result.init( ot1.onto, ot2.onto );
		    addAlignment( result );
		} else if ( als.size() > 1 ) {
		    Alignment norm = normalizeAlignmentSet( als, ot1.onto, ot2.onto );
		    for ( Alignment al : als ) { // Suppress them
			remAlignment( al );
		    }
		    // Add new
		    addAlignment( norm );
		}
	    }
	}
    }

    /**
     * This only works on normalised networks (and returns a normalised network)
     * Combine all alignments into one (add an alignment to those existing merging everything)
     * 
     * @param ot1 and 
     * @param ot2: two ontology triples
     * @param newal: a new alignment to integrate with the alignment between these ontologies
     * @throws AlignmentException when something goes wrong (unlikely)
     */
    protected void mergeAlignments( OntologyTriple ot1, OntologyTriple ot2, Alignment newal ) throws AlignmentException {
	Alignment al = getAlignments( ot1, ot2 ).iterator().next();
	addAlignment( newal.join( al ) );
	remAlignment( al );
    }

    /**
     * Computes the transitive, reflexive, symmetric closure of a network of ontology
     * Reflexive: LEFT UNDONE
     * Symmetric: for each pair of ontologies
     * Transitive: for each triple of ontologies, join its alignment with the composition of the others
     * Modifies the network (but preserves the semantics)
     *
     * Reflexivity is left undone (empty alignments in principle)
     * Because it depends on (a) the type of alignment and (b) the ontology language
     * The code below is OK, but no alignment type is available...
     * generateIdAlignment should be a static method of an alignment type depending on the ontology type
     *
     * @param reflexive: if the reflexive closure must be performed (non implements)
     * @param symmetric: if the symmetric closure must be performed
     * @param transitive: if the transitive closure must be performed
     * @param params: parameters to pass to closure (currently unused)
     * @throws AlignmentException when something goes wrong
     */
    public void close( boolean reflexive, boolean symmetric, boolean transitive, Properties params ) throws AlignmentException {
	normalize();
	// Reflexive and symmetric are the base closure steps
	for ( OntologyTriple ot1 : ontologies.values() ) {
	    if ( reflexive ) {
		// mergeAlignments( ot1, ot1, generateIdAlignment( ot1 ) );
	    }
	    for ( OntologyTriple ot2 : ontologies.values() ) {
		if ( ot1 != ot2 ) {
		    if ( symmetric ) {
			mergeAlignments( ot1, ot2, getAlignments( ot2, ot1 ).iterator().next().inverse() );
		    }
		}
	    }
	}
	// At that point the alignment is normalised (but for reflexive?)
	// Iteration is performed on composition and preserves normalisation
	// In principle, it should not be necessary to apply it more than the number of ontologies
	boolean todo = transitive;
	while ( todo ) {
	    todo = applyComposition( params );
	}
    }

    /**
     * This only works on normalised networks (and returns a normalised network)
     * 
     * Like usually, it could be improved as:
     * for each triple of ontologies: o1, o2, o3
     * al(o1,o3) = al(o1,o3) &amp; al(o1,o2) o al(o2,o3)
     *           &amp; al(o1,o2) o al(o3,o2)-1
     *           &amp; al(o2,o1)-1 o al(o2,o3)
     *           &amp; ...
     * But this assumes symetric closure as well...
     *
     * @param params: parameters to pass to closure (currently unused)
     * @throws AlignmentException when something goes wrong
     * @return true if the composition has generated new correspondences
     */
    protected boolean applyComposition( Properties params ) throws AlignmentException {
	boolean change = false;
	for ( OntologyTriple ot1 : ontologies.values() ) {
	    for ( OntologyTriple ot2 : ontologies.values() ) {
		if ( ot1 != ot2 ) {
		    // Now that it is normalised (but it is not)...
		    Alignment al1 = onto2Align.get( ot1.onto.getURI() ).get( ot2.onto.getURI() ).iterator().next();
		    for ( OntologyTriple ot3 : ontologies.values() ) {
			if ( ( ot1 != ot3 ) && ( ot3 != ot2 ) ) {
			    Alignment al2 = onto2Align.get( ot2.onto.getURI() ).get( ot3.onto.getURI() ).iterator().next();
			    BasicAlignment al3 = (BasicAlignment)onto2Align.get( ot1.onto.getURI() ).get( ot3.onto.getURI() ).iterator().next();
			    // Replaces al3 by al3 & ( al1 o al2 )
			    Alignment result = al1.compose( al2 ); 
			    // result is a new alignment...
			    // Check that result is entailed by al3 (semantic question)
			    if ( !al3.contains( result ) ) {
				change = true;
				mergeAlignments( ot1, ot3, result );
			    }
			}
		    }
		}
	    }
	}
	return change;
    }

    /**
     * Merge all alignments between the same ontology as one (conjunction)
     * Create a new alignment...
     *
     * @param als: a set of alignments
     * @param onto1 and
     * @param onto2: the two ontologies between which these alignments are
     * @throws AlignmentException when something goes wrong
     * @return a new alignment equivalent to als, but normalised
     */
    protected static Alignment normalizeAlignmentSet( Set<Alignment> als, Ontology<Object> onto1, Ontology<Object> onto2 ) throws AlignmentException {
	Alignment result = null;
	if ( als.size() == 0 ) { // If no element, create new
	    result = new BasicAlignment();
	    result.init( onto1, onto2 );
	} else if ( als.size() > 1 ) { // If more than one element, merge them
	    result = null;
	    for ( Alignment al : als ) {
		if ( result == null ) {
		    result = al;
		} else {
		    result.join( al );
		}
	    }
	} else {
	    result = (Alignment)als.iterator().next().clone();
	}
	return result;
    }

    /**
     * Only one alignment between onto1 and onto2, rassembling all those existing and all their inverse
     * The two ontologies must belong to the network otherwise they should be failure (Exception, null??)
     * What is missing is the transitive closure...
     *
     * JE: beware: will bug if not provided with BasicOntologyNetworks
     *
     * @param on: a network of ontologies
     * @param onto1 and
     * @param onto2: the URI of two ontologies between which the alignments will be normalised
     * @throws AlignmentException when something goes wrong
     * @return a single new alignment between onto1 and onto2 equivalence to those of the network
     */
    public static Alignment getQuasiNormalizedAlignment( OntologyNetwork on, URI onto1, URI onto2 ) throws AlignmentException {
	// getAlignments returns a non mutable structure so it has to be copied
	Set<Alignment> als = new HashSet<Alignment>();
	for ( Alignment al : on.getAlignments( onto1, onto2 ) ) {
	    als.add( al );
	}
	for ( Alignment al : on.getAlignments( onto2, onto1 ) ) {
	    als.add( al.inverse() );
	}
	//logger.trace( "     - {}/{} == {} alignments", onto1, onto2, als.size() );
	Alignment result = null;
	if ( als.size() == 0 ) { // If no element, create new
	    result = new BasicAlignment();
	    // JE2015: Instead of storing these ontology objects in the triples,
	    // They could be created here (to decide)
	    result.init( ((BasicOntologyNetwork)on).getOntologyObject( onto1 ), ((BasicOntologyNetwork)on).getOntologyObject( onto2 ) );
	} else {
	    Iterator<Alignment> it = als.iterator();
	    result = (Alignment)it.next().clone();
	    while ( it.hasNext() ) {
		result.join( it.next() );
	    }
	}
	return result;
    }

    protected static Set<Alignment> intersectAlignments( Set<Alignment> s1, Set<Alignment> s2 ) {
	Set<Alignment> result = new HashSet<Alignment>();
	for ( Alignment x : s1 ) {
	    if ( s2.contains( x ) ) result.add( x );
	}
	return result;
    }

    /**
     * Match ontologies in a network, using existing ontologies as input...
     * Modifies the network
     * TODO
     * Alternative definition!
     * {@literal public void match( Class<? extends AlignmentProcess> method, boolean reflexive ) throws AlignmentException}
     *
     * @param method: the ontology matching method to use to match
     * @param reflexive: if alignments have to be found between the same ontology
     * @param symmetric: if alignments must be computed symmetrically
     * @param params: parameters passed to the matching method
     * @throws AlignmentException when something goes wrong
     */
    public void match( String method, boolean reflexive, boolean symmetric, Properties params ) throws AlignmentException {
	for ( OntologyTriple ot1 : ontologies.values() ) {
	    for ( OntologyTriple ot2 : ontologies.values() ) {
		if ( ot1 != ot2 || reflexive ) {
		    Set<Alignment> als = intersectAlignments( ot1.sourceAlignments, ot2.targettingAlignments );
		    Alignment init = normalizeAlignmentSet( als, ot1.onto, ot2.onto );
		    for ( Alignment al : als ) {
			remAlignment( al );
		    }
		    // Create the alignment process
		    // JE: reentrance would be useful
		    // Async is not honoured (but not here)
		    AlignmentProcess ap = null;
		    try {
			// Create alignment object
			Class<?> alignmentClass = Class.forName( method );
			Class<?>[] cparams = {};
			Constructor<?> alignmentConstructor = alignmentClass.getConstructor(cparams);
			Object[] mparams = {};
			ap = (AlignmentProcess)alignmentConstructor.newInstance( mparams );
			ap.init( ot1.onto, ot2.onto );
		    } catch ( Exception ex ) {
			logger.error( "Cannot create alignment {}", method );
			throw new AlignmentException( "Cannot create alignment "+method, ex );
		    }
		    // Compute alignment
		    long time = System.currentTimeMillis();
		    ap.align( init, params ); // or params?
		    long newTime = System.currentTimeMillis();
		    ap.setExtension( Namespace.EXT.uri, Annotations.TIME, Long.toString(newTime - time) );
		    // replace
		    addAlignment( ap );
		}
		if ( ot1 == ot2 && !symmetric ) break;
	    }
	}
    }

    /**
     * Applies a threshold to all alignments in a network
     * Modifies the network
     *
     * @param method: the trimming method
     * @param threshold: the trimming threshold
     * @throws AlignmentException when something goes wrong
     */
    public void trim( String method, double threshold ) throws AlignmentException {
	for ( Alignment al : alignments ) {
	    al.cut( method, threshold );
	}
    }

    /**
     * Intersection of two ontology networks...
     * Creates a new network
     * The semantics used is the reduced semantics in which ontologies identified by their URIs can only be matched to themselves
     * So the homomorphism is trivial
     *
     * TODO: add extensions? i.e., partial clone as BasicAlignment (same in diff and join)
     *
     * @param on1 and
     * @param on2: the two networks to be met
     * @throws AlignmentException when something goes wrong
     * @return the result of the meet operation
     */
    public static BasicOntologyNetwork meet( OntologyNetwork on1, OntologyNetwork on2 ) throws AlignmentException {
	BasicOntologyNetwork result = new BasicOntologyNetwork();
	Set<URI> ontologies2ndNetwork = on2.getOntologies();
	// Collect ontologies
	for ( URI onto1 : on1.getOntologies() ) {
	    if ( ontologies2ndNetwork.contains( onto1 ) ) {
		result.addOntology( onto1 ); // Better to add the Ontology<Object>, not the URI
	    }
	}
	// Meet alignments
	for ( URI onto1 : result.getOntologies() ) {
	    for ( URI onto2 : result.getOntologies() ) {
		//logger.trace( "MEET {}/{}", onto1, onto2 );
		// Get the alignments from both networks
		Alignment init1 = getQuasiNormalizedAlignment( on1, onto1, onto2 );
		Alignment init2 = getQuasiNormalizedAlignment( on2, onto1, onto2 );
		// Meet them
		result.addAlignment( init1.meet( init2 ) );
	    }
	}
	return result;
    }

    /**
     * Difference between two ontology networks...
     * Creates a new network
     * This keeps the ontologies of the first network and suppress from
     * the alignements those correspondences which are there...
     *
     * The semantics used is the reduced semantics in which ontologies identified by their URIs can only be matched to themselves
     * So the homomorphism is trivial
     *
     * @param on1 and
     * @param on2: the two networks to be substracted
     * @throws AlignmentException when something goes wrong
     * @return the result of the diff operation
     */
    public static BasicOntologyNetwork diff( OntologyNetwork on1, OntologyNetwork on2 ) throws AlignmentException {
	BasicOntologyNetwork result = new BasicOntologyNetwork();
	Set<URI> ontologies2ndNetwork = on2.getOntologies();
	// Collect ontologies
	for ( URI onto1 : on1.getOntologies() ) {
	    result.addOntology( onto1 );
	}
	// Diff alignments
	for ( URI onto1 : result.getOntologies() ) {
	    for ( URI onto2 : result.getOntologies() ) {
		// Get the alignments from both networks
		Alignment init1 = getQuasiNormalizedAlignment( on1, onto1, onto2 );
		// JE: For these ones we need to check that the ontology are there first
		//logger.trace( "DIFF {}/{}", onto1, onto2 );
		if ( ontologies2ndNetwork.contains( onto1 )
		     && ontologies2ndNetwork.contains( onto2 ) ) {
		    Alignment init2 = getQuasiNormalizedAlignment( on2, onto1, onto2 );
		    // Meet them
		    result.addAlignment( init1.diff( init2 ) );
		} else {
		    result.addAlignment( init1 );
		}
	    }
	}
	return result;
    }

    /**
     * Union of two ontology networks...
     * Creates a new network
     * Add all ontologies and merge all alignments...
     *
     * The semantics used is the reduced semantics in which ontologies identified by their URIs can only be matched to themselves
     * So the homomorphism is trivial
     *
     * @param on1 and
     * @param on2: the two networks to be joined
     * @throws AlignmentException when something goes wrong
     * @return the result of the join operation
     */
    public static BasicOntologyNetwork join( OntologyNetwork on1, OntologyNetwork on2 ) throws AlignmentException {
	BasicOntologyNetwork result = new BasicOntologyNetwork();
	Set<URI> ontologies2ndNetwork = on2.getOntologies();
	// Collect ontologies
	for ( URI onto2 : ontologies2ndNetwork ) {
		result.addOntology( onto2 );
	}
	for ( URI onto1 : on1.getOntologies() ) {
	    if ( !ontologies2ndNetwork.contains( onto1 ) ) {
		result.addOntology( onto1 );
	    }
	}
	// Join alignments
	for ( URI onto1 : result.getOntologies() ) {
	    for ( URI onto2 : result.getOntologies() ) {
		//logger.trace( "JOIN {}/{}", onto1, onto2 );
		// Get the alignments from both networks
		if ( !ontologies2ndNetwork.contains( onto1 ) || !ontologies2ndNetwork.contains( onto2 ) ) {
		    Set<URI> ontologies1stNetwork = on1.getOntologies();
		    if ( !ontologies1stNetwork.contains( onto1 ) || !ontologies1stNetwork.contains( onto2 ) ) {
			// JE2015: This null is a mess!
			; // Do nothing... better add an empty alignment...
		    } else {
			result.addAlignment( getQuasiNormalizedAlignment( on1, onto1, onto2 ) );
		    }
		} else {
		    Set<URI> ontologies1stNetwork = on1.getOntologies();
		    if ( !ontologies1stNetwork.contains( onto1 ) || !ontologies1stNetwork.contains( onto2 ) ) {
			result.addAlignment( getQuasiNormalizedAlignment( on2, onto1, onto2 ) );
		    } else {
			Alignment init1 = getQuasiNormalizedAlignment( on1, onto1, onto2 );
			Alignment init2 = getQuasiNormalizedAlignment( on2, onto1, onto2 );
			Alignment zz = init1.join( init2 );
			// Join them
			result.addAlignment( zz );
		    }
		}
	    }
	}
	return result;
    }

    public Collection<String[]> getExtensions(){ return extensions.getValues(); }

    public void setExtensions( Extensions ext ){ extensions = ext; }

    public void setExtension( String uri, String label, String value ) {
	extensions.setExtension( uri, label, value );
    };

    public String getExtension( String uri, String label ) {
	return extensions.getExtension( uri, label );
    };

    /**
     * Printing
     * here we do not use renderers; may be later
     */

    protected String NL = System.getProperty("line.separator");
    protected String INDENT = "  ";
    private static Namespace DEF = Namespace.ALIGNMENT;
    public void setIndentString( String ind ) { INDENT = ind; }	
    public void setNewLineString( String nl ) { NL = nl; }	

    public void write( PrintWriter writer ) throws AlignmentException {
	writer.print("<?xml version='1.0' encoding='utf-8");
	writer.print("' standalone='no'?>"+NL+NL);
	writer.print("<"+SyntaxElement.RDF.print(DEF)+" xmlns='"+Namespace.ALIGNMENT.prefix+"'");
	writer.print(NL+INDENT+INDENT+" xml:base='"+Namespace.ALIGNMENT.prefix+"'");
	writer.print(NL+INDENT+INDENT+" xmlns:"+Namespace.RDF.shortCut+"='"+Namespace.RDF.prefix+"'");
	writer.print(NL+INDENT+INDENT+" xmlns:"+Namespace.ALIGNMENT.shortCut+"='"+Namespace.ALIGNMENT.prefix+"'");
	writer.print(NL+INDENT+INDENT+" xmlns:"+Namespace.EXT.shortCut+"='"+Namespace.EXT.prefix+"'");
	writer.print(NL+INDENT+INDENT+" xmlns:"+Namespace.XSD.shortCut+"='"+Namespace.XSD.prefix+"'");
	String idext = getExtension( Namespace.ALIGNMENT.uri, Annotations.ID );
	writer.print(">"+NL+INDENT+"<"+SyntaxElement.ONTOLOGYNETWORK.print(DEF));
	if ( idext != null ) {
	    writer.print(" "+SyntaxElement.RDF_ABOUT.print(DEF)+"='"+idext+"'");
	}
	writer.print(">"+NL);
        for ( String[] ext : getExtensions() ) {
            String prefix = ext[0];
            String name = ext[1];
	    String tag;
	    if ( prefix.equals( Namespace.EXT.prefix ) ) {
                tag = Namespace.EXT.shortCut+":"+name;
            } else {
                tag = prefix+"#"+name;
            }
	    writer.print(INDENT+"<"+tag+">"+ext[2]+"</"+tag+">"+NL);
	}
	for( URI u : ontologies.keySet() ) {
	    writer.print(INDENT+INDENT+"<"+SyntaxElement.ONONTOLOGY.print(DEF)+" rdf:resource=\'"+u+"'/>"+NL);
	}
	writer.print(NL);
	for( Alignment al : alignments ) {
	    String aluri = al.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID );
	    if ( aluri != null ) {
		writer.print(INDENT+INDENT+"<"+SyntaxElement.ONALIGNMENT.print(DEF)+" rdf:resource='"+aluri+"'/>"+NL);
	    } else {
		throw new AlignmentException( "Cannot print alignment without URI : "+al );
	    }
	}
	writer.print(INDENT+"</"+SyntaxElement.ONTOLOGYNETWORK.print(DEF)+">"+NL);
	writer.print("</"+SyntaxElement.RDF.print(DEF)+">"+NL);
    }

    /**
     * Parsing
     * here we use Jena
     *
     * @param uri: the URL of an RDF graph describing a network
     * @throws AlignmentException when something goes wrong (I/O usually)
     * @return the network of ontologies that has been read
     */

    // Certainly not only URIs for testing reasons
    public static OntologyNetwork read( String uri ) throws AlignmentException {
	// Initialize the syntax description (could be restricted)
	Model rdfModel = ModelFactory.createDefaultModel();
	for ( SyntaxElement el : SyntaxElement.values() ) {
	    if ( el.isProperty == true ) {
		el.resource = rdfModel.createProperty( el.id() );
	    } else {
		el.resource = rdfModel.createResource( el.id() );
	    }
	}
	// Parse with JENA
	rdfModel.read( uri );
	// Collect data
	return parse( rdfModel );
    }

    public static OntologyNetwork read( InputStream is ) throws AlignmentException {
	if ( is == null ) throw new AlignmentException("The inputstream must not be null");
	// Initialize the syntax description (could be restricted)
	Model rdfModel = ModelFactory.createDefaultModel();
	for ( SyntaxElement el : SyntaxElement.values() ) {
	    if ( el.isProperty == true ) {
		el.resource = rdfModel.createProperty( el.id() );
	    } else {
		el.resource = rdfModel.createResource( el.id() );
	    }
	}
	// Parse with JENA
	rdfModel.read( is, null );
	// Collect data
	return parse( rdfModel );
    }

    public static OntologyNetwork parse( final Model rdfModel ) throws AlignmentException {
	BasicOntologyNetwork on = null;
	// Get the statement including alignment resource as rdf:type
	StmtIterator stmtIt = rdfModel.listStatements(null, RDF.type,(Resource)SyntaxElement.getResource("OntologyNetwork"));
	// Take the first one if it exists
	if ( !stmtIt.hasNext() ) throw new AlignmentException("There is no ontology network in the RDF document");
	Statement node = stmtIt.nextStatement();
	if (node == null) throw new NullPointerException("OntologyNetwork must not be null");
	Resource res = node.getSubject();

	try {
	    on = new BasicOntologyNetwork();
	    // getting the id of the document
	    final String id = res.getURI();
	    if ( id != null ) on.setExtension( Namespace.ALIGNMENT.uri, Annotations.ID, id );
	    
	    stmtIt = res.listProperties((Property)SyntaxElement.ONONTOLOGY.resource );
	    while ( stmtIt.hasNext() ) {
		RDFNode onto = stmtIt.nextStatement().getObject();
		if ( onto.isURIResource() ) {
		    on.addOntology( new URI( onto.asResource().getURI() ) );
		} else {
		    throw new AlignmentException( "Ontologies must be identified by URIs" );
		}
	    }
	    AlignmentParser aparser = new AlignmentParser();
	    stmtIt = res.listProperties((Property)SyntaxElement.ONALIGNMENT.resource );
	    while ( stmtIt.hasNext() ) {
		RDFNode al = stmtIt.nextStatement().getObject();
		if ( al.isURIResource() ) {
		    String aluri = al.asResource().getURI();
		    Alignment alobject = aparser.parse( aluri );
		    if ( alobject.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID ) == null ) {
			alobject.setExtension( Namespace.ALIGNMENT.uri, Annotations.ID, aluri );
		    }
		    on.addAlignment( alobject );
		} else {
		    logger.debug( "IGNORED Exception : Alignments must be identified by URIs" );
		}
	    }
	    RDFParser.parseExtensions( res, on );
	} catch ( AlignmentException alex ) {
	    throw alex;
	} catch ( URISyntaxException urisex ) {
	    throw new AlignmentException("There is some error in parsing alignment: " + res.getLocalName(), urisex);
	} finally { // Clean up memory
	    rdfModel.close();
	}
	return on;
    }
}

class OntologyTriple {

    public Ontology<Object> onto;
    public HashSet<Alignment> targettingAlignments;
    public HashSet<Alignment> sourceAlignments;

    OntologyTriple( Ontology<Object> o ){
	onto = o;
	targettingAlignments = new HashSet<Alignment>();
	sourceAlignments = new HashSet<Alignment>();
    }

    public void cleanUpAlignments() {
	targettingAlignments.clear();
	sourceAlignments.clear();
    }
}



