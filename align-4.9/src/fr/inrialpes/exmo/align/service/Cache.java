/*
 * $Id: Cache.java 2102 2015-11-29 10:29:56Z euzenat $
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
     *
     * @param p: the initialisation parameters
     * @param prefix: the URI prefix of the current server
     * @throws AlignmentException when something goes wrong (cannot access database, but format)
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
     *
     * @param uri: the URI of the alignment whose metadata to retrieve
     * @return the retrieved alignment filled only with its metadata
     * @throws AlignmentException when something goes wrong (cannot resolve URI)
     */
    public Alignment getMetadata( String uri ) throws AlignmentException;
	
    /**
     * retrieve full alignment from id (and cache it)
     *
     * @param uri: the URI of the alignment to retrieve
     * @return the retrieved alignment
     * @throws AlignmentException when something goes wrong (cannot resolve URI)
     */
    public Alignment getAlignment( String uri ) throws AlignmentException;
	
    /**
     * retrieve full alignment from URI or description
     *
     * @param uri: the URI of the alignments to retrieve
     * @return the set of retrieved alignments
     * @throws AlignmentException when something goes wrong (cannot resolve URI)
     */
    public Set<Alignment> getAlignmentByURI( String uri ) throws AlignmentException;
    public Set<Alignment> getAlignmentsByDescription( String desc ) throws AlignmentException;
	
    /**
     * retrieve network of ontologies from id
     *
     * @param uri: the URI of the network to retrieve
     * @return the retrieved network of ontologies
     * @throws AlignmentException when something goes wrong (cannot resolve URI)
     */
    public OntologyNetwork getOntologyNetwork( String uri ) throws AlignmentException;
	
    public Set<Alignment> getAlignments( URI uri );

    /**
     * returns the alignments between two ontologies
     * if one of the ontologies is null, then return them all
     *
     * @param uri1 and
     * @param uri2: the URIs two ontologies from which retrieving alignments
     * @return the retrieved alignments
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
     *
     * @param alignment: the alignment to record
     * @param force: true if the alignment is too be recorded even if it was recorded
     * @return the URI of the recorded alignment
     */
    public String recordNewAlignment( Alignment alignment, boolean force );

    /**
     * records alignment identified by id and returns its idenfifier
     *
     * @param uri: the URI of the alignment to record
     * @param al: the alignment to record
     * @param force: true if the alignment is too be recorded even if it was recorded
     * @return the URI of the recorded alignment
     * @throws AlignmentException when something goes wrong (cannot resolve URI)
     */
    public String recordNewAlignment( String uri, Alignment al, boolean force ) throws AlignmentException;

    /**
     * records newly created network and returns its idenfifier
     *
     * @param network: the network of ontologies to record
     * @param force: true if the network is too be recorded even if it was recorded
     * @return the URI of the recorded network
     */
    public String recordNewNetwork( OntologyNetwork network, boolean force );

    /**
     * records network identified by id and returns its idenfifier
     *
     * @param uri: the URI of the network to record
     * @param network: the network of ontologies to record
     * @param force: true if the network is too be recorded even if it was recorded
     * @return the URI of the recorded network
     * @throws AlignmentException when something goes wrong (cannot resolve URI)
     */
    public String recordNewNetwork( String uri, OntologyNetwork network, boolean force ) throws AlignmentException;

    //**********************************************************************
    // STORING IN DATABASE

    public boolean isAlignmentStored( Alignment alignment );

    public boolean isNetworkStored( OntologyNetwork network );

    /**
     * Non publicised class
     *
     * @param uri: the URI of the alignment to erase
     * @param eraseFromDB: true if the alignment is erased from the database cache
     * @throws AlignmentException when something goes wrong (cannot resolve URI)
     */
    public void eraseAlignment( String uri, boolean eraseFromDB ) throws AlignmentException;

    public void eraseOntologyNetwork( String uri, boolean eraseFromDB ) throws AlignmentException;

    public void storeAlignment( String uri ) throws AlignmentException;

    public void storeOntologyNetwork( String uri ) throws AlignmentException;
}
