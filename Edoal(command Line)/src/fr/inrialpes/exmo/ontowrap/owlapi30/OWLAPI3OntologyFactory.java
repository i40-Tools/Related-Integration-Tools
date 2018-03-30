/*
 * $Id: OWLAPI3OntologyFactory.java 2150 2017-07-18 15:15:46Z euzenat $
 *
 * Copyright (C) INRIA, 2008-2011, 2013-2014, 2017
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

package fr.inrialpes.exmo.ontowrap.owlapi30;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owlapi.apibinding.OWLManager;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyDocumentAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.IRI;

import fr.inrialpes.exmo.ontowrap.OntowrapException;
import fr.inrialpes.exmo.ontowrap.OntologyCache;
import fr.inrialpes.exmo.ontowrap.OntologyFactory;
import fr.inrialpes.exmo.ontowrap.HeavyLoadedOntology;

public class OWLAPI3OntologyFactory extends OntologyFactory {
    final static Logger logger = LoggerFactory.getLogger( OWLAPI3OntologyFactory.class );

    private static URI formalismUri;
    private final static String formalismId = "OWL2.0";
    private final static OntologyCache<OWLAPI3Ontology> cache = new OntologyCache<OWLAPI3Ontology>();

    private OWLOntologyManager manager;
    
    public OWLAPI3OntologyFactory() {
	try {
	    formalismUri = new URI("http://www.w3.org/2002/07/owl#");
	    manager = OWLManager.createOWLOntologyManager();
	} catch (URISyntaxException ex) {
	    logger.debug( "IGNORED should never happen", ex );
	} catch (Throwable t) {
	    logger.error("Failure during static initialization", t);
	    //t.printStackTrace();
	    throw t;
	}
    }

    @Override
    public OWLAPI3Ontology newOntology( Object ontology, boolean onlyLocalEntities) throws OntowrapException {
	if ( ontology instanceof OWLOntology ) {
	    OWLAPI3Ontology onto = new OWLAPI3Ontology(onlyLocalEntities);
	    onto.setFormalism( formalismId );
	    onto.setFormURI( formalismUri );
	    onto.setOntology( (OWLOntology)ontology );
	    onto.setURI( ((OWLOntology)ontology).getOntologyID().getOntologyIRI().toURI() );
	    cache.recordOntology( onto.getURI(), onto );
	    cache.recordOntology( ((OWLOntology)ontology).getOntologyID().getOntologyIRI().toURI(), onto );
	    return onto;
	} else {
	    throw new OntowrapException( "Argument is not an OWLOntology: "+ontology );
	}
    }

    @Override
    public HeavyLoadedOntology<? extends Object> loadOntology( URI uri, boolean onlyLocalEntities ) throws OntowrapException {
	OWLAPI3Ontology onto = null;
	// logger.trace( " Loading ontology {}", uri );
	// Cache seems to be implemented in API 3.0 anyway
	// and it seems to not work well with this one
	onto = cache.getOntologyFromURI( uri );
	// logger.trace( "cache1: {}", onto );
	if ( onto != null ) return onto;
	onto = cache.getOntology( uri );
	// logger.trace( "cache2: {}", onto );
	if ( onto != null ) return onto;
	// OWLAPI's own cache
	IRI ontoIRI = IRI.create( uri );
	OWLOntology ontology = manager.getOntology( ontoIRI );
	// logger.trace( "cache3: {}", ontology );
	try {
	    // This below does not seem to work!
	    //ontology = manager.loadOntologyFromOntologyDocument( IRI.create( uri ) );
	    if ( ontology == null ) ontology = manager.loadOntology( ontoIRI );
	    // logger.trace( "loaded: {}", ontology );
	    // I must retrieve it from cache and return it!
	} catch ( OWLOntologyDocumentAlreadyExistsException oodaeex ) { // should never happen
	    // This is a cache failure
	    throw new OntowrapException("Already loaded [doc cache failure] " + uri, oodaeex );
	} catch ( OWLOntologyAlreadyExistsException ooaeex ) {
	    // This happens when the ontology has been loaded from a different URIs
	    ontology = manager.getOntology( ooaeex.getOntologyID() );
	    if ( ontology == null )
		throw new OntowrapException("Already loaded [owl cache failure] " + uri, ooaeex );
	} catch ( OWLOntologyCreationException oocex ) {
	    throw new OntowrapException( "Cannot load " + uri, oocex );
	}
	onto = new OWLAPI3Ontology( onlyLocalEntities );
	onto.setFormalism( formalismId );
	onto.setFormURI( formalismUri );
	onto.setOntology( ontology );
	onto.setFile( uri );
	try {
	    onto.setURI( ontology.getOntologyID().getOntologyIRI().toURI() );
	} catch ( Exception e ) { // Should be a NullPointerException
	    // Better put in the OntowrapException of loaded
	    // The ontology has no URI. In principle, it is not valid
	    // It may be possible to put the uri instead (now it is void)
	    logger.debug( "IGNORED Exception (ontology without URI)", e );
	}
	cache.recordOntology( uri, onto );
	// logger.trace( "after-cache: {}", cache.getOntology( uri ) );
	return onto;
    }
    
    public OWLOntologyManager getManager() {
	return manager;
    }

    @Override
    public void clearCache() throws OntowrapException {
	cache.clear();
    }

}
