/*
 * $Id: LOVAlignmentService.java 2059 2015-09-11 06:52:25Z euzenat $
 *
 * Copyright (C) INRIA, 2006-2009, 2010, 2013-2014
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Properties;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.OntologyNetwork;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.inrialpes.exmo.align.impl.BasicOntologyNetwork;

public class LOVAlignmentService extends AlignmentService {
	
    public static void main( String[] args ) {
	try { new LOVAlignmentService().run( args ); }
	catch ( Exception ex ) { logger.error( "FATAL error", ex ); };
    }
    
    protected void init( Properties parameters ) {
	logger.info("LOVAlignment server running");
	// Here should everything be done!
	// If no ontologynetwork for LOV
	//    create it
	BasicOntologyNetwork nolov;
	try {
	    nolov = readLOV( "http://lov.okfn.org/dataset/lov/api/v1/vocabs" );
	    String id = manager.alignmentCache.recordNewNetwork( nolov, true );
	    logger.trace( "Nolov loaded as {}", id );
	    // JE: it should also be matched
	    // JE: it should also be stored
	} catch (AlignmentException e) {
	    logger.debug( "IGNORED: cannot record LOV network of ontology" );
	}
	// Otherwise
	//    update it (check each filename)
    }

    /*
     * URI is LOV uri
     */
    public static BasicOntologyNetwork readLOV( String uri ) throws AlignmentException {
	JsonObject jsonobject;
	try {
	    URL url = new URL( uri );
	    HttpURLConnection request = (HttpURLConnection) url.openConnection();
	    request.connect();
	    // Convert to a JSON object to print data
	    JsonParser jp = new JsonParser(); 
	    //convert the input stream to a json element
	    JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent())); 
	    jsonobject = root.getAsJsonObject();
	} catch (IOException e) {
	    throw new AlignmentException( "Ontology Network error: unable to create Json Object from ("+uri+")" );
	}

	/*
	 * lov Structure at May 27th, 2014
	 * using api: http://lov.okfn.org/dataset/lov/api/v1/vocabs
	 * "vocabularies": [
	 *     {
	 *      "uri", "namespace", "prefix", 
	 *      "titles": [ {"value", "language", "dataType"} ],
	 *      "descriptions": [ {"value", "language", "dataType"} ],
	 *      	"lastVersionReviewed": {"date", "versionDecimal", "label", "link"}
	 *     }
	 * ]
	 * We are getting the ontologies' uri
	 */

	BasicOntologyNetwork on = new BasicOntologyNetwork();
	//fetching the ontologies in the network
	for ( JsonElement element : jsonobject.get("vocabularies").getAsJsonArray() ) {
	    JsonObject data = element.getAsJsonObject();
	    // main data
	    String ontouri = data.get("uri").isJsonNull() ? null : data.get("uri").getAsString();
	    try {
		// JE2014:
		// I want more than that! I want to create an ontology object
		// with the ontology locator
		on.addOntology( new URI( ontouri ) );
	    } catch (URISyntaxException e) {
		logger.debug( "IGNORED: Malformed URI in LOV data : {}", ontouri );
	    }
	    /* if other information is needed:
	       String namespace = data.get("namespace").isJsonNull() ? null : data.get("namespace").getAsString();
	       String prefix = data.get("prefix").isJsonNull() ? null : data.get("prefix").getAsString();
	       
	       // titles
	       JsonArray array_titles = data.get("titles").getAsJsonArray();
	       Iterator<JsonElement> itr_titles = array_titles.iterator();
	       while(itr_titles.hasNext()) {
	       JsonElement titles = itr_titles.next();
	       JsonObject data_titles = titles.getAsJsonObject();
	       String title_value = data_titles.get("value").isJsonNull() ? null : data_titles.get("value").getAsString();
	       String title_language = data_titles.get("language").isJsonNull() ? null : data_titles.get("language").getAsString();
	       String title_dataType = data_titles.get("dataType").isJsonNull() ? null : data_titles.get("dataType").getAsString();
	       }
	       
	       // descriptions
	       JsonArray array_descriptions = data.get("descriptions").getAsJsonArray();
	       Iterator<JsonElement> itr_descriptions = array_descriptions.iterator();
	       while(itr_descriptions.hasNext()) {
	       JsonElement descriptions = itr_descriptions.next();
	       JsonObject data_descriptions = descriptions.getAsJsonObject();
	       String descriptions_value = data_descriptions.get("value").isJsonNull() ? null : data_descriptions.get("value").getAsString();
	       String descriptions_language = data_descriptions.get("language").isJsonNull() ? null : data_descriptions.get("language").getAsString();
	       String descriptions_dataType = data_descriptions.get("dataType").isJsonNull() ? null : data_descriptions.get("dataType").getAsString();
	       }
	 
	       // descriptions
	       if (!data.get("lastVersionReviewed").isJsonNull()) {
	            JsonObject lastVersionReviewed = data.get("lastVersionReviewed").getAsJsonObject();
	            String lastVersionReviewed_date = lastVersionReviewed.get("date").isJsonNull() ? null : lastVersionReviewed.get("date").getAsString();
	            String lastVersionReviewed_versionDecimal = lastVersionReviewed.get("versionDecimal").isJsonNull() ? null : lastVersionReviewed.get("versionDecimal").getAsString();
	            String lastVersionReviewed_label = lastVersionReviewed.get("label").isJsonNull() ? null : lastVersionReviewed.get("label").getAsString();
	            String lastVersionReviewed_link = lastVersionReviewed.get("link").isJsonNull() ? null : lastVersionReviewed.get("link").getAsString();
	        }
	    */
	}
	return on;
    }

}
