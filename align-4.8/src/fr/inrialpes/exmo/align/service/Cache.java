/*
 * $Id: Cache.java 1989 2014-11-17 11:59:19Z euzenat $
 *
 * Copyright (C) INRIA, 2014
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package fr.inrialpes.exmo.align.service;

import java.util.Collection;
import java.util.Set;
import java.util.Properties;
import java.net.URI;
import java.net.URISyntaxException;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.OntologyNetwork;
import org.semanticweb.owl.align.AlignmentException;

/**
 * This interface caches the content of the alignment database.
 * It may be implemented in various ways: SQL or RDF repositories
 */

public interface Cache {
    /**
     * loads the alignment descriptions from the database and put them in the
     * alignmentTable hashtable
     */
    public void init( Properties p, String prefix ) throws AlignmentException;

    public void reset() throws AlignmentException;

    public void close() throws AlignmentException;

    // **********************************************************************
    // INDEXES

    public Collection<Alignment> alignments();
    public Collection<URI> ontologies();
    public Collection<OntologyNetwork> ontologyNetworks();
    public Collection<Alignment> alignments( URI u1, URI u2 );

    //**********************************************************************
    // DEALING WITH URIs

    // Public because this is now used by AServProtocolManager
    public String generateAlignmentUri();
    
    public String generateOntologyNetworkUri();
    
    //**********************************************************************
    // FETCHING FROM CACHE
    /**
     * retrieve alignment metadata from id
     * This is more difficult because we return the alignment we have 
     * disreagarding if it is complete o only metadata
     */
    public Alignment getMetadata( String uri ) throws AlignmentException;
	
    /**
     * retrieve full alignment from id (and cache it)
     */
    public Alignment getAlignment( String uri ) throws AlignmentException;
	
    /**
     * retrieve full alignment from URI or description
     */
    public Set<Alignment> getAlignmentByURI( String uri ) throws AlignmentException;
    public Set<Alignment> getAlignmentsByDescription( String desc ) throws AlignmentException;
	
    /**
     * retrieve network of ontologies from id
     */
    public OntologyNetwork getOntologyNetwork( String uri ) throws AlignmentException;
	
    public Set<Alignment> getAlignments( URI uri );

    /**
     * returns the alignments between two ontologies
     * if one of the ontologies is null, then return them all
     */
    public Set<Alignment> getAlignments( URI uri1, URI uri2 );

    /**
     * The equivalent primitives to both getAlignments() 
     * within a network of ontologies are available from the network itself
     */

    public void flushCache();

    //**********************************************************************
    // RECORDING ALIGNMENTS AND NETWORKS

    /**
     * records newly created alignment and returns its idenfifier
     */
    public String recordNewAlignment( Alignment alignment, boolean force );

    /**
     * records alignment identified by id and returns its idenfifier
     */
    public String recordNewAlignment( String uri, Alignment al, boolean force ) throws AlignmentException;

    /**
     * records newly created network and returns its idenfifier
     */
    public String recordNewNetwork( OntologyNetwork network, boolean force );

    /**
     * records network identified by id and returns its idenfifier
     */
    public String recordNewNetwork( String uri, OntologyNetwork network, boolean force ) throws AlignmentException;

    //**********************************************************************
    // STORING IN DATABASE

    public boolean isAlignmentStored( Alignment alignment );

    public boolean isNetworkStored( OntologyNetwork network );

    /**
     * Non publicised class
     */
    public void eraseAlignment( String uri, boolean eraseFromDB ) throws AlignmentException;

    public void eraseOntologyNetwork( String uri, boolean eraseFromDB ) throws AlignmentException;

    public void storeAlignment( String uri ) throws AlignmentException;

    public void storeOntologyNetwork( String uri ) throws AlignmentException;
}
