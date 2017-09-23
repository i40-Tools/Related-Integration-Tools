/*
 * $Id: OntologyFactory.java 2150 2017-07-18 15:15:46Z euzenat $
 *
 * Copyright (C) INRIA, 2008, 2010-2015, 2017
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

package fr.inrialpes.exmo.ontowrap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public abstract class OntologyFactory {
    final static Logger logger = LoggerFactory.getLogger( OntologyFactory.class );

    public static final int ANY = 0;
    public static final int DIRECT = 1;
    public static final int INDIRECT = 2;
    public static final int ASSERTED = 3;
    public static final int UNASSERTED = 4;
    public static final int INHERITED = 5;
    public static final int FULL = 6;
    public static final int NAMED = 7;
    public static final int LOCAL = 8;
    public static final int GLOBAL = 9;
    public static final int UNNAMED = 10;
    public static final int MENTIONNED = 11;
    public static final int ALL = 12;

    private static Map<URI,Set<String>> factories = null;

    protected static Hashtable<String,OntologyFactory> instances = null;

    /*
     * Changing this variable, is preferably made by the:
     * setDefaultFactory( )
     * below
     */
    private static String API_NAME="fr.inrialpes.exmo.ontowrap.owlapi30.OWLAPI3OntologyFactory";

    public static Set<String> getFactories( URI formalism ) {
	if ( factories == null ){
	    HashMap<URI, Set<String>> facts = new HashMap<URI, Set<String>>();
	    try {
		HashSet<String> owl = new HashSet<String>();
		owl.add( "fr.inrialpes.exmo.ontowrap.jena25.JENAOntologyFactory" );
		owl.add( "fr.inrialpes.exmo.ontowrap.owlapi30.OWLAPI3OntologyFactory" );
		owl.add( "fr.inrialpes.exmo.ontowrap.owlapi10.OWLAPIOntologyFactory" );
		facts.put( new URI("http://www.w3.org/2002/07/owl#"), owl );

		HashSet<String> rdfs = new HashSet<String>();
		rdfs.add( "fr.inrialpes.exmo.ontowrap.jena25.JENAOntologyFactory" );
		facts.put( new URI("http://www.w3.org/2000/01/rdf-schema#"), rdfs );

		//HashSet<String> owl2 = new HashSet<String>();
		//owl2.add( "fr.inrialpes.exmo.ontowrap.jena25.JENAOntologyFactory" );
		//owl2.add( "fr.inrialpes.exmo.ontowrap.owlapi30.OWLAPI3OntologyFactory" );
		//facts.put( new URI("http://www.w3.org/2002/07/owl#"), owl2 );

		HashSet<String> skos = new HashSet<String>();
		skos.add( "fr.inrialpes.exmo.ontowrap.skoslite.SKOSLiteOntologyFactory" );
		skos.add( "fr.inrialpes.exmo.ontowrap.skosapi.SKOSOntologyFactory" );
		facts.put( new URI("http://www.w3.org/2004/02/skos/core#"), skos );

	    } catch ( URISyntaxException uriex ) {
		logger.debug( "IGNORED (should never happen)", uriex );
	    }
	    factories = facts;
	}
	return factories.get( formalism );
    }

    public static String getDefaultFactory(){
	return API_NAME;
    }

    public static void setDefaultFactory( String className ){
	API_NAME = className;
    }

    public static OntologyFactory getFactory() {
	return newInstance(API_NAME);
    }

    protected static OntologyFactory newInstance( String apiName ) {
	if ( instances == null ) instances = new Hashtable<String,OntologyFactory>();
	OntologyFactory of = instances.get( apiName );
	if ( of != null ) return of;
	try {
	    // This should also be a static getInstance!
	    Class<?> ofClass = Class.forName( apiName );
	    Class<?>[] cparams = {};
	    java.lang.reflect.Constructor<?> ofConstructor = ofClass.getConstructor( cparams );
	    Object[] mparams = {};
	    of = (OntologyFactory)ofConstructor.newInstance(mparams);
	} catch ( ClassNotFoundException cnfex ) {
	    logger.debug( "IGNORED Exception", cnfex ); // better raise errors
	} catch ( NoSuchMethodException nsmex ) {
	    logger.debug( "IGNORED Exception", nsmex );
	} catch ( InstantiationException ieex ) {
	    logger.debug( "IGNORED Exception", ieex );
	} catch ( IllegalAccessException iaex ) {
	    logger.debug( "IGNORED Exception", iaex );
	} catch ( InvocationTargetException itex ) {
	    logger.debug( "IGNORED Exception", itex );
	}
	if ( of != null ) instances.put( apiName, of );
	return of;
    }

    public static void clear() throws OntowrapException {
	if ( instances != null ) {
	    for ( OntologyFactory of : instances.values() ){
		of.clearCache();
	    }
	}
    }

    /**
     * All Ontologies must implement clearCache()
     * which unload their ontologies if any cache is enabled.
     * 
     * @throws OntowrapException when something goes wrong
     */
    public abstract void clearCache() throws OntowrapException;

    /**
     * Encapsulate an ontology already in the environment
     * These methods should rather be in a LoadableOntologyFactory
     * 
     * @param onto: the ontology to be encapsulated
     * @param onlyLocalEntities: true if only the entities in the namespace of the ontology have to be considered
     * @return the LoadedOntology object
     * @throws OntowrapException when something goes wrong
     */
    public abstract LoadedOntology<? extends Object> newOntology( Object onto , boolean onlyLocalEntities ) throws OntowrapException;
    
    public  LoadedOntology<? extends Object> newOntology( Object onto ) throws OntowrapException {
	return newOntology( onto, false );
    }

    /**
     * Load an ontology, cache enabled
     * These methods should rather be in a LoadableOntologyFactory
     * 
     * @param uri: the URI (URL) of the ontology to load
     * @return the loaded ontology
     * @throws OntowrapException when something goes wrong
     */
    public LoadedOntology<? extends Object> loadOntology( URI uri ) throws OntowrapException {
	// logger.trace( "loading URI: {}", uri );
	return loadOntology( uri, true );
    }
    
    /**
     * Loads and ontology which is not loaded yet
     * 
     * @param onto: the Ontology object corresponding to the ontology to load
     * @return the corresponding LoadedOntology object
     * @throws OntowrapException when something goes wrong
     */
    public LoadedOntology<? extends Object> loadOntology( Ontology<? extends Object> onto ) throws OntowrapException {
	// logger.trace( "loading Ontology: {}", onto );
	try { // Try with file
	    return loadOntology( onto.getFile() );
	} catch ( Exception ex ) {
	    try { // Try with URI
		return loadOntology( onto.getURI() );
	    } catch ( Exception ex2 ) {
		throw new OntowrapException( "Cannot load ontology "+onto );
	    }
	}
    }
    
    /**
     * Load an ontology, cache enabled
     * These methods should rather be in a LoadableOntologyFactory
     * 
     * @param uri: the URI (URL) of the ontology to load
     * @param onlyLocalEntities: true if only the entities in the namespace of the ontology have to be considered
     * @return the LoadedOntology object
     * @throws OntowrapException when something goes wrong
     */
    public abstract LoadedOntology<? extends Object> loadOntology( URI uri, boolean onlyLocalEntities ) throws OntowrapException;

    /**
     * Load an ontology, cache enabled if true, disabled otherwise
     * This will disappear: cache will be dispatched in implementations
    public LoadedOntology loadOntology( URI uri, OntologyCache<LoadedOntology> ontologies ) throws OntowrapException {
	LoadedOntology onto = null;
	if ( ontologies != null ) {
	    onto = ontologies.getOntologyFromURI( uri );
	    if ( onto != null ) return onto;
	    onto = ontologies.getOntology( uri );
	    if ( onto != null ) return onto;
	};
	onto = loadOntology( uri );
	if ( ontologies != null ) ontologies.recordOntology( uri, onto );
	return onto;
    };
     */
}
