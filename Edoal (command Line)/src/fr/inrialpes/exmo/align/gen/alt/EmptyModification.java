/*
 * $Id: EmptyModification.java 2145 2017-07-14 20:29:42Z euzenat $
 *
 * Copyright (C) INRIA, 2011-2013, 2015, 2017
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package fr.inrialpes.exmo.align.gen.alt;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.Ontology;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inrialpes.exmo.align.gen.Alterator;
import fr.inrialpes.exmo.align.gen.ParametersIds;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;

public class EmptyModification extends BasicAlterator {
    final static Logger logger = LoggerFactory.getLogger( EmptyModification.class );

    protected boolean relocateSource = false;

    public EmptyModification( OntModel o ) {
	modifiedModel = o;
	// get the default namespace of the model
	modifiedOntologyNS = modifiedModel.getNsPrefixURI("");
    };

    // Clearly here setDebug, setNamespace are important

    public Alterator modify( Properties params ) throws AlignmentException {
	//logger.trace( "NEW MODIFICATION ------------------------------------" );
	relocateSource = ( params.getProperty( "copy101" ) != null );

	if ( alignment == null ) {
	    if ( modifiedOntologyNS == null ) {
		if ( params != null && params.getProperty( "urlprefix" ) != null ) {
		    modifiedOntologyNS = params.getProperty( "urlprefix" );
		} else {
		    logger.error( "Unknown ontology URI" );
		    throw new AlignmentException( "Unknown ontology URI" );
		}
	    }

	    initOntologyNS = modifiedOntologyNS;

	    alignment = new Properties();
	    alignment.setProperty( "##", initOntologyNS );

	    // Jena has a bug when URIs contain non alphabetical characters
	    // in the localName, it does not split correctly ns/localname
	    for ( OntClass cls : modifiedModel.listNamedClasses().toList() ) {
		String uri = cls.getURI();
		if ( uri.startsWith( modifiedOntologyNS ) ) {
		    String ln = uri.substring( uri.lastIndexOf("#")+1 );
		    //add them to the initial alignment
		    if ( ln != null && !ln.equals("") )	alignment.put( ln, ln );
		} 
	    }
	    for ( OntProperty prop : modifiedModel.listAllOntProperties().toList() ) {
		String uri = prop.getURI();
		if ( uri.startsWith( modifiedOntologyNS ) ) {
		    String ln = uri.substring( uri.lastIndexOf("#")+1 );
		    //add them to the initial alignment
		    if ( ln != null && !ln.equals("") )	alignment.put( ln, ln );
		}
	    }
	}
	return this;
    }

    // In case of 101, I want to have the empty test
    public void relocateTest( String base1, String base2 ) {
	super.relocateTest( relocateSource?base2:base1, base2 );
    }    

    //the initial reference alignment
    public void initializeAlignment( Properties al ) {
        alignment = al;
	initOntologyNS = al.getProperty( "##" );
    }

}
