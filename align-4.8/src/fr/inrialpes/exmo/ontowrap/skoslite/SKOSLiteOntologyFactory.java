/*
 * $Id: SKOSLiteOntologyFactory.java 2079 2015-10-16 19:00:16Z euzenat $
 *
 * Copyright (C) INRIA, 2008-2010, 2013, 2015
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

package fr.inrialpes.exmo.ontowrap.skoslite;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.rdf.model.Model;

import fr.inrialpes.exmo.ontowrap.OntologyCache;
import fr.inrialpes.exmo.ontowrap.OntologyFactory;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

public class SKOSLiteOntologyFactory extends OntologyFactory {
    final static Logger logger = LoggerFactory.getLogger( SKOSLiteOntologyFactory.class );

    private URI formalismUri;
    private final static String formalismId = "SKOS1.0";
    private final static OntologyCache<SKOSLiteThesaurus> cache = new OntologyCache<SKOSLiteThesaurus>();
    
    public SKOSLiteOntologyFactory() {
	try {
	    formalismUri = new URI("http://www.w3.org/2004/02/skos/core#");
	} catch (URISyntaxException e) {
	    logger.trace( "IGNORED: should never been raised", e );
	}
    }
   
    @Override
    public void clearCache() throws OntowrapException {
	cache.clear();
    }

    @Override
    public SKOSLiteThesaurus loadOntology(URI uri, boolean onlyLocalEntities) throws OntowrapException {
	SKOSLiteThesaurus onto = cache.getOntologyFromURI( uri );
	if ( onto != null ) return onto;
	onto = cache.getOntology( uri );
	if ( onto != null ) return onto;
	onto = new SKOSLiteThesaurus(uri);
	onto.setFormalism( formalismId );
	onto.setFormURI( formalismUri );
	
	onto.setURI( uri );
	//cache.recordOntology( uri, onto );
	
	return onto;
    }

    @Override
    public SKOSLiteThesaurus newOntology(Object m, boolean onlyLocalEntities) throws OntowrapException {
	if ( m instanceof Model ) {
	    SKOSLiteThesaurus onto = new SKOSLiteThesaurus((Model) m);
	    onto.setFormalism( formalismId );
	    onto.setFormURI( formalismUri );
	    //TODO Find the URI of a skos thesaurus ?
	    // This is the URI of the corresponding OWL API Ontology
	    //URI uri = ((SKOSDataset)ontology).getURI();
	    //onto.setURI( uri );
	    //cache.recordOntology( uri, onto );
	    return onto;
	} else {
	    throw new OntowrapException( "Argument is not an Jena Model: "+m );
	}
    }

}
