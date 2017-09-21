/*
 * $Id: Service.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) INRIA, 2013-2014
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

package fr.inrialpes.exmo.align.service.osgi;

import java.util.Properties;
import java.util.Set;
import java.util.Collection;
import java.net.URI;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.OntologyNetwork;

import fr.inrialpes.exmo.align.service.msg.Message;

/**
 * So far, AServProtocol implements directly this service interface
 * No additional code is necessary.
 * However, it may be more useful to provide an implementation that
 * decodes all messages in Java objects.
 * This may be necessary as a second step.
 *
 */

public interface Service {

    public Set<String> listmethods ();

    public Set<String> listrenderers();

    public Set<String> listservices();

    public Set<String> listevaluators();

    public Collection<Alignment> alignments();

    public Collection<URI> ontologies();

    public Collection<Alignment> alignments( URI uri1, URI uri2 );

    public String query( String query );

    public String serverURL();

    public String argline();

   /*********************************************************************
     * Basic protocol primitives
     *********************************************************************/

    /**
     * load an alignment
     *
     * @param mess: the parameters of the request
     * @return the message corresponding to the answer of the request
     */
    public Message load( Properties mess );

    public Message align( Properties mess );

    public Message existingAlignments( Properties mess );

    public Message findCorrespondences( Properties mess );

    public Message find( Properties mess );

    public Message translate( Properties mess );

    public Message render( Properties mess );

    /*********************************************************************
     * Extended protocol primitives
     *********************************************************************/

    /**
     * store an alignment
     *
     * @param mess: the parameters of the request
     * @return the message corresponding to the answer of the request
     */
    public Message store( Properties mess );

    //public Message erase( Properties mess );

    public Message metadata( Properties mess );

    /*********************************************************************
     * Extra alignment primitives
     *
     * All these primitives must create a new alignment and return its Id
     * There is no way an alignment server could modify an alignment
     *********************************************************************/

    /**
     * trim an alignment
     *
     * @param mess: the parameters of the request
     * @return the message corresponding to the answer of the request
     */
    public Message trim( Properties mess );

    public Message harden( Properties mess );

    public Message inverse( Properties mess );

    public Message meet( Properties mess );

    public Message join( Properties mess );

    public Message compose( Properties mess );

    public Message eval( Properties mess );

    public Message diff( Properties mess );

    /*********************************************************************
     * Network of alignment server implementation
     *********************************************************************/

    /**
     * load a network of ontologies
     *
     * @param params: the parameters of the request
     * @return the message corresponding to the answer of the request
     */
    public Message loadOntologyNetwork( Properties params );

    public Message renderOntologyNetwork( Properties params );

    public Message renderHTMLNetwork( Properties params );

    public Message storeOntologyNetwork( Properties params );

    public Message matchOntologyNetwork( Properties params );

    public Message trimOntologyNetwork( Properties params );

    public Message closeOntologyNetwork( Properties params );

    public Message normOntologyNetwork( Properties params );

    public Message denormOntologyNetwork( Properties params );

    public Message invertOntologyNetwork( Properties params );

    public Message setopOntologyNetwork( Properties params );

    /*********************************************************************
     * Utilities: reaching and loading ontologies
     *********************************************************************/

    /**
     * store an alignment
     *
     * @param al: the alignment to store
     * @return true if everything has worked properly
     */
    public boolean storedAlignment( Alignment al );

    public boolean storedNetwork( OntologyNetwork on );

    //public LoadedOntology reachable( URI uri );

}
