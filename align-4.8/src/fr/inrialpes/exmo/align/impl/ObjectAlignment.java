/*
 * $Id: ObjectAlignment.java 2062 2015-10-01 16:44:20Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2011, 2013-2015
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

import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.SAXException;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.ontowrap.OntologyFactory;
import fr.inrialpes.exmo.ontowrap.Ontology;
import fr.inrialpes.exmo.ontowrap.LoadedOntology;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

/**
 * Represents an OWL ontology alignment. An ontology comprises a number of
 * collections. Each ontology has a number of classes, properties and
 * individuals, along with a number of axioms asserting information about those
 * objects.
 *
 * @author J�r�me Euzenat
 * @version $Id: ObjectAlignment.java 2062 2015-10-01 16:44:20Z euzenat $
 */

public class ObjectAlignment extends BasicAlignment {
    final static Logger logger = LoggerFactory.getLogger( ObjectAlignment.class );

    // Overloaded
    //protected LoadedOntology<Object> onto1 = null;
    //protected LoadedOntology<Object> onto2 = null;

    // This is used for factorising loadInit()
    protected ObjectAlignment init = null;

    public ObjectAlignment() {}

    public void init( Object onto1, Object onto2 ) throws AlignmentException {
	if ( (onto1 instanceof LoadedOntology) && (onto2 instanceof LoadedOntology) ){
	    super.init( onto1, onto2 );
	} else if ( onto1 instanceof URI && onto2 instanceof URI ) {
		super.init( loadOntology( (URI)onto1 ),
			    loadOntology( (URI)onto2 ) );
	} else {
	    throw new AlignmentException("Arguments must be LoadedOntology or URI");
	};
    }

    /*
     * @deprecated
     */
    public LoadedOntology<Object> ontology1(){
	return (LoadedOntology<Object>)onto1;
    }

    /*
     * @deprecated
     */
    public LoadedOntology<Object> ontology2(){
	return (LoadedOntology<Object>)onto2;
    }

    public LoadedOntology<Object> getOntologyObject1(){
	return (LoadedOntology<Object>)onto1;
    }

    public LoadedOntology<Object> getOntologyObject2(){
	return (LoadedOntology<Object>)onto2;
    }

    public void loadInit( Alignment al ) throws AlignmentException {
	if ( al instanceof URIAlignment ) {
	    init = toObjectAlignment( (URIAlignment)al );
	} else if ( al instanceof ObjectAlignment ) {
	    init = (ObjectAlignment)al;
	}
    }

    public URI getOntology1URI() { return onto1.getURI(); };

    public URI getOntology2URI() { return onto2.getURI(); };

    public ObjectCell createCell(String id, Object ob1, Object ob2, Relation relation, double measure) throws AlignmentException {
	return new ObjectCell( id, ob1, ob2, relation, measure);
    }

    /**
     * Generate a copy of this alignment object
     */
    public ObjectAlignment createNewAlignment( Object onto1, Object onto2, Class<? extends Relation> relType, Class<?> confType ) throws AlignmentException {
	ObjectAlignment align = new ObjectAlignment();
	align.init( onto1, onto2, relType, confType );
	return align;
    }

    public ObjectAlignment inverse() throws AlignmentException {
	ObjectAlignment result = createNewAlignment( onto2, onto1, relationType, confidenceType );
	invertContent( result, "inverted", "http://exmo.inrialpes.fr/align/impl/ObjectAlignment#inverse" );
	return result;
    }

    /**
     * This is a clone with the URI instead of Object objects
     */
    public URIAlignment toURIAlignment() throws AlignmentException {
	return toURIAlignment( false );
    }

    public URIAlignment toURIAlignment( boolean strict ) throws AlignmentException {
	URIAlignment align = new URIAlignment();
	align.init( getOntology1URI(), getOntology2URI(), relationType, confidenceType );
	align.setType( getType() );
	align.setLevel( getLevel() );
	align.setFile1( getFile1() );
	align.setFile2( getFile2() );
	align.setExtensions( extensions.convertExtension( "EDOALURIConverted", this.getClass().getName()+"#toURI" ) );
	for ( Cell c : this ) {
	    try {
		align.addAlignCell( c.getId(), c.getObject1AsURI(this), c.getObject2AsURI(this), c.getRelation(), c.getStrength() );
	    } catch (AlignmentException aex) {
		// Sometimes URIs are null, this is ignored
		if ( strict ) {
		    throw new AlignmentException( "Cannot convert to URIAlignment" );
		}
	    }
	};
	return align;
    }

    public static ObjectAlignment toObjectAlignment( URIAlignment al ) throws AlignmentException {
	ObjectAlignment alignment = new ObjectAlignment();
	try {
	    alignment.init( al.getFile1(), al.getFile2(), al.getRelationType(), al.getConfidenceType() );
	} catch ( AlignmentException aex ) {
	    try { // Really a friendly fallback
		alignment.init( al.getOntology1URI(), al.getOntology2URI(), al.getRelationType(), al.getConfidenceType() );
	    } catch ( AlignmentException xx ) {
		throw aex;
	    }
	}
	alignment.setType( al.getType() );
	alignment.setLevel( al.getLevel() );
	alignment.setExtensions( al.extensions.convertExtension( "ObjectURIConverted", "fr.inrialpes.exmo.align.ObjectAlignment#toObject" ) );
	LoadedOntology<Object> o1 = alignment.getOntologyObject1();
	LoadedOntology<Object> o2 = alignment.getOntologyObject2();
	Object obj1 = null;
	Object obj2 = null;

	try {
	    for ( Cell c : al ) {
		try {
		    obj1 = o1.getEntity( c.getObject1AsURI( alignment ) );
		} catch ( NullPointerException npe ) {
		    throw new AlignmentException( "Cannot dereference entity "+c.getObject1AsURI( alignment ), npe );
		}
		try {
		    obj2 = o2.getEntity( c.getObject2AsURI( alignment ) );
		} catch ( NullPointerException npe ) {
		    throw new AlignmentException( "Cannot dereference entity "+c.getObject2AsURI( alignment ), npe );
		}
		//logger.trace( "{} {} {} {}", obj1, obj2, c.getRelation(), c.getStrength() );
		if ( obj1 == null ) throw new AlignmentException( "Cannot dereference entity "+c.getObject1AsURI( alignment ) );
		if ( obj2 == null ) throw new AlignmentException( "Cannot dereference entity "+c.getObject2AsURI( alignment ) );
		Cell newc = alignment.addAlignCell( c.getId(), obj1, obj2,
						    c.getRelation(), c.getStrength() );
		Collection<String[]> exts = c.getExtensions();
		if ( exts != null ) {
		    for ( String[] ext : exts ){
			newc.setExtension( ext[0], ext[1], ext[2] );
		    }
		}
	    }
	} catch ( OntowrapException owex ) {
	    throw new AlignmentException( "Cannot dereference entity", owex );
	}
	return alignment;
    }

    static LoadedOntology<?> loadOntology( URI ref ) throws AlignmentException {
	OntologyFactory factory = OntologyFactory.getFactory();
	try {
	    return factory.loadOntology( ref );
	} catch ( OntowrapException owex ) {
	    throw new AlignmentException( "Cannot load ontology "+ref, owex );
	}
    }
}

