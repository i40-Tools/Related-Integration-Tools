/*
 * $Id: JENAOntologyFactory.java 2079 2015-10-16 19:00:16Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2008, 2010, 2013, 2015
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

package fr.inrialpes.exmo.ontowrap.jena25;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.ModelFactory;

import fr.inrialpes.exmo.ontowrap.LoadedOntology;
import fr.inrialpes.exmo.ontowrap.OntologyFactory;
import fr.inrialpes.exmo.ontowrap.OntologyCache;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

public class JENAOntologyFactory extends OntologyFactory {
    final static Logger logger = LoggerFactory.getLogger( JENAOntologyFactory.class );

    private static URI formalismUri = null;
    private static String formalismId = "OWL1.0";

    private static OntologyCache<JENAOntology> cache = null;

    public JENAOntologyFactory() {
	cache = new OntologyCache<JENAOntology>();
	try { 
	    formalismUri = new URI("http://www.w3.org/2002/07/owl#");
	} catch (URISyntaxException ex) { 
	    logger.debug( "IGNORED (should never happen)", ex ); 
	} // should not happen
    }

    public JENAOntology newOntology( Object ontology, boolean onlyLocalEntities ) throws OntowrapException {
	if ( ontology instanceof OntModel ) {
	    JENAOntology onto = new JENAOntology(onlyLocalEntities);
	    onto.setFormalism( formalismId );
	    onto.setFormURI( formalismUri );
	    onto.setOntology( (OntModel)ontology );
            onto.getOntology().setStrictMode(false);
	    //onto.setFile( uri );// unknown
	    // to be checked : why several ontologies in a model ???
	    // If no URI can be extracted from ontology, then we use the physical URI
	    try {
		try {
		    onto.setURI(new URI(((OntModel)ontology).listOntologies().next().getURI()));
		} catch (NoSuchElementException nse) {
		    // JE: not verysafe
		    onto.setURI(new URI(((OntModel)ontology).getNsPrefixURI("")));
		}
	    } catch ( URISyntaxException usex ){
		// Better put in the OntowrapException of loaded
		throw new OntowrapException( "URI Error ", usex );
	    }
	    cache.recordOntology( onto.getURI(), onto );
	    return onto;

	} else {
	    throw new OntowrapException( "Argument is not an OntModel: "+ontology );
	}

    }

    public JENAOntology loadOntology( URI uri , boolean onlyLocalEntities) throws OntowrapException {
	JENAOntology onto = null;
	onto = cache.getOntologyFromURI( uri );
	if ( onto != null ) return onto;
	onto = cache.getOntology( uri );
	if ( onto != null ) return onto;
	try {
	    OntModel m = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM, null );
            m.setStrictMode(false);
	    m.read(uri.toString());
	    onto = new JENAOntology(onlyLocalEntities);
	    onto.setFile(uri);
	    // to be checked : why several ontologies in a model ???
	    // If no URI can be extracted from ontology, then we use the physical URI
	    try {
		onto.setURI(new URI(m.listOntologies().next().getURI()));
	    } catch (NoSuchElementException nse) {
		if (m.getNsPrefixURI("")!=null)
		    onto.setURI(new URI(m.getNsPrefixURI("")));
		else 
		    onto.setURI(uri);
	    }
	    //onto.setURI(new URI(m.listOntologies()getOntology(null).getURI()));
	    onto.setOntology(m);
	    cache.recordOntology( uri, onto );
	    return onto;
        } catch (Exception e) {
	    throw new OntowrapException("Cannot load "+uri, e );
	}
    }

    @Override
    public void clearCache() throws OntowrapException {
	cache.clear();
    };

}
