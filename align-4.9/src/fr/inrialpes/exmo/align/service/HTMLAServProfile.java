/*
 * $Id: HTMLAServProfile.java 2150 2017-07-18 15:15:46Z euzenat $
 *
 * Copyright (C) INRIA, 2006-2015, 2017
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

import fr.inrialpes.exmo.align.impl.Annotations;
import fr.inrialpes.exmo.align.impl.BasicOntologyNetwork;
import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.service.msg.Message;
import fr.inrialpes.exmo.align.service.msg.ErrorMsg;
import fr.inrialpes.exmo.align.service.msg.AlignmentId;
import fr.inrialpes.exmo.align.service.msg.AlignmentIds;
import fr.inrialpes.exmo.align.service.msg.EvaluationId;
import fr.inrialpes.exmo.align.service.msg.OntologyNetworkId;
import fr.inrialpes.exmo.align.service.msg.UnknownOntologyNetwork;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.OntologyNetwork;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.net.URI;
import java.net.URISyntaxException;
import java.lang.Integer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTMLAServProfile: an HTML provile for the Alignment server
 */

public class HTMLAServProfile implements AlignmentServiceProfile {
    final static Logger logger = LoggerFactory.getLogger( HTMLAServProfile.class );

    private AServProtocolManager manager;

    private Properties parameters = null;

    private String serverURL;
    private int localId = 0;

    public static final int MAX_FILE_SIZE = 10000;

    public static final String HEADER = "<style type=\"text/css\">body { font-family: sans-serif } button {background-color: #DDEEFF; margin-left: 1%; border: #CCC 1px solid;}</style>";

    public void init( Properties params, AServProtocolManager manager ) throws AServException {
	parameters = params;
	this.manager = manager;
	serverURL = manager.serverURL()+"/html/";
    }

    public boolean accept( String prefix ) {
	if ( prefix.equals("admin") || prefix.equals("html") || prefix.equals("noo")) return true;
	else return false;
    }

    public String process( String uri, String prefix, String perf, Properties header, Properties params ) {
	if ( prefix.equals("html") ) {
	    return htmlAnswer( uri, perf, header, params );
	} else if ( prefix.equals("noo") ) {
	    return ontologyNetworkAnswer( uri, perf, header, params );
	} else if ( prefix.equals("admin") ) {
	    return adminAnswer( uri, perf, header, params );
	} else return about();
    }

    public void close(){
    }
    
    // ==================================================
    // API parts
    // ==================================================

    protected String about() {
	String result = "<h1>Alignment server</h1><center><div style=\"align: center;\"><br />";
	if ( parameters != null && // guard against non init()-ed profile
	     parameters.getProperty( "banner" ) != null ) {
	    result += parameters.getProperty( "banner" );
	}
	result +=
	    "<form style=\"height: 15px; width: 150px; position: relative;\" action=\"html/\"><button style=\"background-color: lightblue;\" title=\"Alignment menu\" type=\"submit\">Alignments</button></form>"
	    + "<form style=\"height: 15px; width: 150px; margin-left: 5px; position: relative;\" action=\"noo/\"><button style=\"background-color: lightgreen;\" title=\"Network of ontologies menu\" type=\"submit\">Ontology networks</button></form>"
	    + "<form style=\"height: 15px; width: 150px; margin-left: 5px; position: relative;\" action=\"../admin/\"><button style=\"background-color: lightpink;\" title=\"Server management functions\" type=\"submit\">Server management</button></form>"
	    + "</div>"
	    + "<div style=\"align: center;\"><br />"
	    + "<div>"+AlignmentService.class.getPackage().getImplementationTitle()+" "+AlignmentService.class.getPackage().getImplementationVersion()+"</div>"
	    + "(C) INRIA, 2006-2015<br />"
	    + "<a href=\"http://alignapi.gforge.inria.fr\">http://alignapi.gforge.inria.fr</a><br />"
	    + "</div></center>";
	return result;
    }

    /**
     * HTTP administration interface
     * Handles administrative specific requests
     * Allows some limited administration of the server through HTTP
     *
     * @param uri: the URL comming from the HTTP server
     * @param perf: its performative (the action to be processed)
     * @param header: the HTTP header
     * @param params: the parameters of the query
     * @return the HTML output to be displayed
     */
    public String adminAnswer( String uri, String perf, Properties header, Properties params ) {
	//logger.trace( "ADMIN[{}]", perf);
	String msg = "";
        if ( perf.equals("listmethods") ){
	    msg = "<h1>Embedded classes</h1>\n<h2>Methods</h2><ul compact=\"1\">";
	    for( String m : manager.listmethods() ) {
		msg += "<li>"+m+"</li>";
	    }
	    msg += "</ul>";
	    msg += "<h2>Renderers</h2><ul compact=\"1\">";
	    for( String m : manager.listrenderers() ) {
		msg += "<li>"+m+"</li>";
	    }
	    msg += "</ul>";
	    msg += "<h2>Services</h2><ul compact=\"1\">";
	    for( String m : manager.listservices() ) {
		msg += "<li>"+m+"</li>";
	    }
	    msg += "</ul>";
	    msg += "<h2>Evaluators</h2><ul compact=\"1\">";
	    for( String m : manager.listevaluators() ) {
		msg += "<li>"+m+"</li>";
	    }
	    msg += "</ul>";
	} else if ( perf.equals("argline") ){
	    msg = "<h1>Command line arguments</h1>\n<pre>\n"+manager.argline()+"\n<pre>\n";
	} else if ( perf.equals("prmsqlquery") ){
	    msg = "<h1>SQL query</h1><form action=\"sqlquery\">Query:<br /><textarea name=\"query\" rows=\"20\" cols=\"80\">SELECT \nFROM \nWHERE </textarea> (sql)<br /><small>An SQL SELECT query</small><br /><input type=\"submit\" value=\"Query\"/></form>";
	} else if ( perf.equals("sqlquery") ){
	    String answer = manager.query( params.getProperty("query") );
	    msg = "<pre>"+answer+"</pre>";
	} else if ( perf.equals("about") ){
	    msg = about();
	} else if ( perf.equals("shutdown") ){
	    manager.shutdown();
	    msg = "<h1>Server shut down</h1>";
	} else if ( perf.equals("prmreset") ){
	    manager.reset();
	    msg = "<h1>Alignment server reset from database</h1>";
	} else if ( perf.equals("prmflush") ){
	    manager.flush();
	    msg = "<h1>Cache has been flushed</h1>";
	} else if ( perf.equals("addservice") ){
	    msg = perf;
	} else if ( perf.equals("addmethod") ){
	    msg = perf;
	} else if ( perf.equals("addrenderer") ){
	    msg = perf;
	} else if ( perf.equals("") ) {
	    msg = "<h1>Alignment server administration</h1>";
	    msg += "<form action=\"listmethods\"><button style=\"background-color: lightpink;\" title=\"List embedded plug-ins\" type=\"submit\">Embedded classes</button></form>";
	    msg += "<form action=\"/wsdl\"><button style=\"background-color: lightpink;\" title=\"WSDL Description\" type=\"submit\">WSDL Description</button></form>";
	    msg += "<form action=\"prmsqlquery\"><button style=\"background-color: lightpink;\" title=\"Query the SQL database (unavailable)\" type=\"submit\">SQL Query</button></form>";
	    msg += "<form action=\"prmflush\"><button style=\"background-color: lightpink;\" title=\"Free memory by unloading correspondences\" type=\"submit\">Flush caches</button></form>";
	    msg += "<form action=\"prmreset\"><button style=\"background-color: lightpink;\" title=\"Restore launching state (reload from database)\" type=\"submit\">Reset server</button></form>";
	    //	    msg += "<form action=\"shutdown\"><button style=\"background-color: lightpink;\" title=\"Shutdown server\" type=\"submit\">Shutdown</button></form>";
	    msg += "<form action=\"..\"><button style=\"background-color: lightpink;\" title=\"About...\" type=\"submit\">About</button></form>";
	    msg += "<form action=\"../html/\"><button style=\"background-color: lightblue;\" title=\"Back to alignment menu\" type=\"submit\">Alignments</button></form>";
	    msg += "<form action=\"../noo/\"><button style=\"background-color: lightgreen;\" title=\"Back to network of ontologies menu\" type=\"submit\">Ontology networks</button></form>";
	} else {
	    msg = "Cannot understand: "+perf;
	}
	return "<html><head>"+HEADER+"</head><body>"+msg+"<hr /><center><small><a href=\".\">Server administration</a></small></center></body></html>";
    }

    
    /**
     * HTTP ontology networks interface
     * Handles network of ontologies specific requests
     * Allows the ontology networks management through HTTP
     *
     * @param uri: the URL comming from the HTTP server
     * @param perf: its performative (the action to be processed)
     * @param header: the HTTP header
     * @param params: the parameters of the query
     * @return the HTML output to be displayed
     */
    public String ontologyNetworkAnswer( String uri, String perf, Properties header, Properties params ) {
	logger.trace( "ONTONET[{}]", perf);
	String msg = "";
	String eSource = "on";
        if ( perf.equals("listnetworks") ){
    	    msg = "<h1>Available networks</h1>";
    	    msg += "<form action=\"listnetworks\"><ul compact=\"1\">";
    	    for ( OntologyNetwork oNetwork : manager.ontologyNetworks() ) {
    	    	BasicOntologyNetwork noo = (BasicOntologyNetwork) oNetwork;
    	    	String id = noo.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID );
		String pid = noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY );
		if ( pid == null ) pid = id; else pid = id+" ("+pid+")";
		msg += "<li><a href=\"" + id +"\">" + pid + "</a></li>";
	    }
	    msg += "</ul></form>";
        } else if ( perf.equals("prmload") ){
	    //TODO add two more parameters TYPE of file (json/html, etc) and STRUCTURE of the file
	    msg = "<h1>Load a network of ontologies</h1>";
	    msg += "<form action=\"load\">";
    	    msg += "Network URL: <input type=\"text\" name=\"url\" size=\"80\"/><br />";
    	    msg += "<small>This is the URL of ontology network. It must be reachable by the server (i.e., file://localhost/absolutePathTo/file or file:///absolutePathTo/file if localhost omitted)</small><br />";
    	    msg += "Pretty name: <input type=\"text\" name=\"pretty\" size=\"80\"/><br />";
    	    msg += "<input type=\"submit\" value=\"Load\"></form>";
    	    msg += "Ontology network file: <form enctype=\"multipart/form-data\" action=\"load\" method=\"POST\">";
    	    msg += "<input type=\"hidden\" name=\"MAX_FILE_SIZE\" value=\""+MAX_FILE_SIZE+"\"/>";
    	    msg += "<input name=\"content\" type=\"file\" value=\"\" size=\"35\"/><br />"; // size=\"35\" ??
    	    msg += "Pretty name: <input type=\"text\" name=\"pretty\" size=\"80\"/><br />";
    	    msg += "<input type=\"submit\" Value=\"Upload\"/>";
    	    msg +=  " </form>";
        } else if ( perf.equals("load") ) {
	    Message answer = manager.loadOntologyNetwork( params );
    	    if ( answer instanceof ErrorMsg ) {
    	    	msg = testErrorMessages( answer, params, eSource );
    	    } else {
    		msg = "<h1>Ontology Network loaded</h1>";
    		msg += displayAnswerON( answer, params );
    	    }
	} else if ( perf.equals("prmstore") ){
	    msg = "<h1>Store a network of ontologies</h1><form action=\"store\">";
	    msg += networkChooser( "Network id: ", "id", params.getProperty("id"), true )+"<br />";
	    msg += "<input type=\"submit\" value=\"Store\"/></form>";
	} else if ( perf.equals("store") ){
	    String id = params.getProperty("id");
	    String url = params.getProperty("url");
	    if ( url != null && !url.equals("") ) {
		Message answer = manager.loadOntologyNetwork( params );
		if ( answer instanceof ErrorMsg ) {
		    msg = testErrorMessages( answer, params, eSource );
		} else {
		    id = answer.getContent();
		}
	    }
	    if ( id != null ){ // Store it
		Message answer = manager.storeOntologyNetwork( params );
		if ( answer instanceof ErrorMsg ) {
		    msg = testErrorMessages( answer, params, eSource );
		} else {
		    msg = "<h1>Ontology Network stored</h1>";
		    msg += displayAnswerON( answer, params );
		}
	    }	
	} else if ( perf.equals("list") ){
	} else if ( perf.equals("prmtrim") ){
	    msg ="<h1>Trim networks</h1><form action=\"trim\">";
	    msg += networkChooser( "Network id: ", "id", params.getProperty("id"), false )+"<br />";
	    msg += "Type: <select name=\"type\"><option value=\"hard\">hard</option><option value=\"perc\">perc</option><option value=\"best\">best</option><option value=\"span\">span</option><option value=\"prop\">prop</option></select><br />Threshold: <input type=\"text\" name=\"threshold\" size=\"4\"/> <small>A value between 0. and 1. with 2 digits</small><br /><input type=\"submit\" name=\"action\" value=\"Trim\"/><br /></form>";
	} else if ( perf.equals("trim") ){
	    Message answer = manager.trimOntologyNetwork( params );
	    if ( answer instanceof ErrorMsg ) {
	    	msg = testErrorMessages( answer, params, eSource );
	    } else {
	    	msg ="<h1>Trimmed network</h1>";
	    	msg += displayAnswerON( answer, params );
	    }  
	} else if ( perf.equals("prmmatch") ){
	    msg = "<h1>Match a network of ontologies</h1><form action=\"match\">";
	    msg += networkChooser( "Network id: ", "id", params.getProperty("id"), false )+"<br />";
	    msg += "<br />Methods: <select name=\"method\">";
	    for( String idMethod : manager.listmethods() ) {
		msg += "<option value=\""+idMethod+"\">"+idMethod+"</option>"; 
	    }
	    msg += "</select><br /><br />";
	    msg += "Pretty name: <input type=\"text\" name=\"pretty\" size=\"80\"/><br />";
	    msg += "  <input type=\"checkbox\" name=\"force\" /> Force <input type=\"checkbox\" name=\"async\" /> Asynchronous<br />";
	    msg += "Additional parameters:<br /><input type=\"text\" name=\"paramn1\" size=\"15\"/> = <input type=\"text\" name=\"paramv1\" size=\"65\"/><br /><input type=\"text\" name=\"paramn2\" size=\"15\"/> = <input type=\"text\" name=\"paramv2\" size=\"65\"/><br /><input type=\"text\" name=\"paramn3\" size=\"15\"/> = <input type=\"text\" name=\"paramv3\" size=\"65\"/><br /><input type=\"text\" name=\"paramn4\" size=\"15\"/> = <input type=\"text\" name=\"paramv4\" size=\"65\"/>";
	    msg += "<br /><input type=\"checkbox\" name=\"reflexive\" /> Reflexive ";
	    msg += "<input type=\"checkbox\" name=\"symmetric\" /> Symmetric ";
	    msg += "<br /><br /><input type=\"submit\" name=\"action\" value=\"Match\"/> ";
	    msg += "</form>";
	} else if ( perf.equals("match") ) {
	    //logger.debug("Matching network {}", params.getProperty("id"));
	    Message answer = manager.matchOntologyNetwork( params );
	    if ( answer instanceof ErrorMsg ) {
		msg = testErrorMessages( answer, params, eSource );
	    } else {
		msg = "<h1>Ontology Network matched</h1>";
		msg += displayAnswerON( answer, params );
	    }	    
	} else if ( perf.equals("prmretrieve") ){
	    msg = "<h1>Retrieve a network of ontology</h1><form action=\"retrieve\">";
	    msg += networkChooser( "Network id: ", "id", params.getProperty("id"), false )+"<br />";
	    msg += "<br /><input type=\"submit\" value=\"Retrieve\"/></form>";
	} else if ( perf.equals("retrieve") ) {
	    Message answer = manager.renderOntologyNetwork( params );
	    if ( answer instanceof ErrorMsg ) {
		msg = testErrorMessages( answer, params, eSource );
	    } else {
		// Depending on the type we should change the MIME type
	    	return answer.getContent();
	    }
	} else if ( perf.equals("prminvert") ) {
	    msg = "<h1>Invert a network of ontologies</h1><form action=\"invert\">";
	    msg += networkChooser( "Network id: ", "id", params.getProperty("id"), false )+"<br />";
	    msg += "<input type=\"submit\" name=\"invert\" value=\"Invert\" /> ";
	    msg += "</form>";
	} else if ( perf.equals("invert") ){
	    Message answer = manager.invertOntologyNetwork( params );
	    if ( answer instanceof ErrorMsg ) {
	    	msg = testErrorMessages( answer, params, eSource );
	    } else {
	    	msg ="<h1>Inverted network</h1>";
	    	msg += displayAnswerON( answer, params );
	    }  
	} else if ( perf.equals("prmclose") ){
	    msg = "<h1>Close a network of ontologies</h1><form action=\"close\">";
	    msg += networkChooser( "Network id: ", "id", params.getProperty("id"), false )+"<br />";
	    msg += "<input type=\"checkbox\" name=\"symmetric\" value=\"Symmetric\"/> Symmetric ";
	    msg += "<input type=\"checkbox\" name=\"transitive\" value=\"Transitive\"/> Transitive ";
	    msg += "<input type=\"checkbox\" name=\"reflexive\" value=\"Reflexive\"/> Reflexive ";
	    msg += "<input type=\"checkbox\" name=\"new\" /> New ";
	    msg += "<br /><input type=\"submit\" name=\"close\" value=\"Close\" /> ";
	    msg += "</form>";
	} else if ( perf.equals("close") ){
	    Message answer = manager.closeOntologyNetwork( params );
	    if ( answer instanceof ErrorMsg ) {
	    	msg = testErrorMessages( answer, params, eSource );
	    } else {
	    	msg ="<h1>Closed network</h1>";
	    	msg += displayAnswerON( answer, params );
	    }  
	} else if ( perf.equals("prmnormalize") ){
	    msg = "<h1>Normalize/denormalize a network of ontologies</h1><form action=\"norm\">";
	    msg += networkChooser( "Network id: ", "id", params.getProperty("id"), false )+"<br />";
	    msg += "<input type=\"checkbox\" name=\"new\" /> New ";
	    msg += "<input type=\"submit\" name=\"oper\" value=\"Normalize\" /> ";
	    msg += "<input type=\"submit\" name=\"oper\" value=\"Denormalize\" /> ";
	    msg += "</form>";
	} else if ( perf.equals("norm") ){
	    Message answer = null;
	    if ( params.getProperty( "oper" ).equals( "Normalize" ) ) {
		answer = manager.normOntologyNetwork( params );
	    } else {
		answer = manager.denormOntologyNetwork( params );
	    }
	    if ( answer instanceof ErrorMsg ) {
	    	msg = testErrorMessages( answer, params, eSource );
	    } else {
	    	msg ="<h1>Normalized/denormalized network</h1>";
	    	msg += displayAnswerON( answer, params );
	    }
	} else if ( perf.equals("prmoperset") ){
	    msg = "<h1>Algebraic operations on ontology networks</h1><form action=\"opset\">";
	    msg += networkChooser( "Network 1: ", "id1", params.getProperty("id1"), false );
	    msg +="<select name=\"oper\"><option value=\"meet\"><option value=\"join\"><option value=\"diff\"></select>";
	    msg += networkChooser( "Network 2: ", "id2", params.getProperty("id2"), false )+"<br />";
	    msg += "<input type=\"submit\" name=\"action\" value=\"Execute\"/> ";
	    msg += "</form>";
	} else if ( perf.equals("opset") ){
	    Message answer = manager.setopOntologyNetwork( params );
	    if ( answer instanceof ErrorMsg ) {
	    	msg = testErrorMessages( answer, params, eSource );
	    } else {
	    	msg ="<h1>Operation applied</h1>";
	    	msg += displayAnswerON( answer, params );
	    }
	} else if ( perf.equals("") ) {
	    msg = "<h1>Ontology networks</h1>";
	    msg += "<form action=\"listnetworks\"><button style=\"background-color: lightgreen;\" title=\"List of networks of ontologies stored in the server\" type=\"submit\">Available networks</button></form>";
	    msg += "<form action=\"prmload\"><button style=\"background-color: lightgreen;\" title=\"Load a network of ontologies from a valid source\" type=\"submit\">Load a network</button></form>";
	    msg += "<form action=\"prmmatch\"><button style=\"background-color: lightgreen;\" title=\"Match all ontologies in a network of ontologies\" type=\"submit\">Apply matching</button></form>";
	    msg += "<form action=\"prmtrim\"><button style=\"background-color: lightgreen;\" title=\"Trim a network of ontologies\" type=\"submit\">Trim network</button></form>";
	    msg += "<form action=\"prminvert\"><button style=\"background-color: lightgreen;\" title=\"Invert a network of ontologies\" type=\"submit\">Invert network</button></form>";
	    msg += "<form action=\"prmnormalize\"><button style=\"background-color: lightgreen;\" title=\"Normalise/denormalize a network of ontologies\" type=\"submit\">Normalise network</button></form>";
	    //msg += "<form action=\"prmoperset\"><button style=\"background-color: lightgreen;\" title=\"Set operations on networks of ontologies\" type=\"submit\">Set operations on networks</button></form>";
	    msg += "<form action=\"prmclose\"><button style=\"background-color: lightgreen;\" title=\"Close a network of ontologies\" type=\"submit\">Close network</button></form>";
	    msg += "<form action=\"prmretrieve\"><button style=\"background-color: lightgreen;\" title=\"Display a network of ontologies\" type=\"submit\">Show network</button></form>";
	    msg += "<form action=\"prmstore\"><button style=\"background-color: lightgreen;\" title=\"Store a network of ontologies in the server\" type=\"submit\">Store network</button></form>";
	    msg += "<form action=\"../html/\"><button style=\"background-color: lightblue;\" title=\"Back to alignment menu\" type=\"submit\">Alignments</button></form>";
	    msg += "<form action=\"../admin/\"><button style=\"background-color: lightpink;\" title=\"Server management functions\" type=\"submit\">Server management</button></form>";
	} else {
	    msg = "Cannot understand: "+perf;
	}
	return "<html><head>"+HEADER+"</head><body>"+msg+"<hr /><center><small><a href=\".\">Network management</a></small></center></body></html>";
    }

    public String networkChooser( String header, String label, String selected, boolean stored ) {
	String msg = header+"<select name=\""+label+"\">";
	for ( OntologyNetwork on : manager.ontologyNetworks() ) {		    	
	    if ( !stored || !manager.storedNetwork( on ) ){
		String id = ((BasicOntologyNetwork)on).getExtension( Namespace.ALIGNMENT.uri, Annotations.ID );
		String pid = ((BasicOntologyNetwork)on).getExtension( Namespace.EXT.uri, Annotations.PRETTY );
		if ( pid == null ) pid = id; else pid = id+" ("+pid+")";
		if ( selected != null && selected.equals( id ) ){
		    msg += "<option selected=\"1\" value=\""+id+"\">"+pid+"</option>";
		} else { msg += "<option value=\""+id+"\">"+pid+"</option>";}
	    }
	}
	return msg+"</select>";
    }

    /**
     * User friendly HTTP interface
     * uses the protocol but offers user-targeted interaction
     * Handles alignment specific requests
     *
     * @param uri: the URL comming from the HTTP server
     * @param perf: its performative (the action to be processed)
     * @param header: the HTTP header
     * @param params: the parameters of the query
     * @return the HTML output to be displayed
     */
    public String htmlAnswer( String uri, String perf, Properties header, Properties params ) {
	logger.trace("HTML[{}]", perf );
	// REST get
	String msg = "";
	String eSource = "al";
	if ( perf.equals("listalignments") ) {
	    URI uri1 = null;	
	    String u1 = params.getProperty("uri1");
	    try {
		if ( u1 != null && !u1.equals("all") ) uri1 = new URI( u1 );
	    } catch ( URISyntaxException usex ) {
		logger.debug( "IGNORED Invalid URI parameter", usex );
	    };
	    URI uri2 = null;
	    String u2 = params.getProperty("uri2");
	    try {
		if ( u2 != null && !u2.equals("all") ) uri2 = new URI( u2 );
	    } catch ( URISyntaxException usex ) {
		logger.debug( "IGNORED Invalid URI parameter", usex );
	    };
	    // Add two onto checklist
	    Collection<URI> ontologies = manager.ontologies();
	    msg = "<h1>Available alignments</h1><form action=\"listalignments\">";
	    msg += "Onto1:  <select name=\"uri1\"><option value=\"all\"";
	    if ( uri1 == null ) msg += " selected=\"1\"";
	    msg += ">all</option>";
	    for ( URI ont : ontologies ) {
		msg += "<option";
		if ( ont.equals( uri1 ) ) msg += " selected =\"1\"";
		msg += " value=\""+ont+"\">"+ont+"</option>"; //simplify
	    }
	    msg += "</select>";
	    msg += "Onto2:  <select name=\"uri2\"><option value=\"all\"";
	    if ( uri2 == null ) msg += " selected=\"1\"";
	    msg += ">all</option>";
	    for ( URI ont : ontologies ) { 
		msg += "<option";
		if ( ont.equals( uri2 ) ) msg += " selected =\"1\"";
		msg += " value=\""+ont+"\">"+ont+"</option>"; //simplify
	    }
	    msg += "</select>";
	    msg += "&nbsp;<input type=\"submit\" value=\"Restrict\"/></form>";
	    // would be better as a JavaScript which updates
	    Collection<Alignment> alignments = null;
	    if ( uri1 == null && uri2 == null ) {
		alignments = manager.alignments();
	    } else {
		alignments = manager.alignments( uri1, uri2 );
	    }
	    if ( alignments == null ) {
		msg += "<p>No alignment matches these ontologies</p>";
	    } else {
		msg += "<ul compact=\"1\">";
		for ( Alignment al : alignments ) {
		    String id = al.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID );
		    String pid = al.getExtension( Namespace.EXT.uri, Annotations.PRETTY );
		    if ( pid == null ) pid = id; else pid = id+" ("+pid+")";
		    //msg += "<li><a href=\"../html/retrieve?method=fr.inrialpes.exmo.align.impl.renderer.HTMLRendererVisitor&id="+id+"\">"+pid+"</a></li>";
		    msg += "<li><a href=\""+id+"\">"+pid+"</a></li>";
		}
		msg += "</ul>";
	    }
	} else if ( perf.equals("manalignments") ){ // Manage alignments
	    msg = "<h1>Available alignments</h1><ul compact=\"1\">";
	    for ( Alignment al : manager.alignments() ) {
		String id = al.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID );
		String pid = al.getExtension( Namespace.EXT.uri, Annotations.PRETTY );
		if ( pid == null ) pid = id; else pid = id+" ("+pid+")";
		//msg += "<li><a href=\"../html/retrieve?method=fr.inrialpes.exmo.align.impl.renderer.HTMLRendererVisitor&id="+id+"\">"+pid+"</a> "+al.nbCells()+" <a href=\"../html/errrazze?id="+id+"\">DEL</a></li>";
		msg += "<li><a href=\""+id+"\">"+pid+"</a> "+al.nbCells()+" <a href=\"../html/errrazze?id="+id+"\">DEL</a></li>";
	    }
	    msg += "</ul>";
	} else if ( perf.equals("errrazze") ){ // Suppress an alignment
	    String id = params.getProperty("id");
	    if ( id != null && !id.equals("") ) { // Erase it
		Message answer = manager.erase( params );
		if ( answer instanceof ErrorMsg ) {
		    msg = testErrorMessages( answer, params, eSource );
		} else {
		    msg = "<h1>Alignment deleted</h1>";
		    msg += displayAnswer( answer, params );
		}
	    }
	} else 	if ( perf.equals("prmstore") ) {
	    msg = "<h1>Store an alignment</h1><form action=\"store\">";
	    msg += alignmentChooser( "Alignment id: ", "id", null, true )+"<br />";
	    msg += "<input type=\"submit\" value=\"Store\"/></form>";
	} else if ( perf.equals("store") ) {
	    // here should be done the switch between store and load/store
	    String id = params.getProperty("id");
	    String url = params.getProperty("url");
	    if ( url != null && !url.equals("") ) { // Load the URL
		Message answer = manager.load( params );
		if ( answer instanceof ErrorMsg ) {
		    msg = testErrorMessages( answer, params, eSource );
		} else {
		    id = answer.getContent();
		}
	    }
	    if ( id != null ){ // Store it
		Message answer = manager.store( params );
		if ( answer instanceof ErrorMsg ) {
		    msg = testErrorMessages( answer, params, eSource );
		} else {
		    msg = "<h1>Alignment stored</h1>";
		    msg += displayAnswer( answer, params );
		}
	    }
	} else if ( perf.equals("prmtrim") ) {
	    msg ="<h1>Trim alignments</h1><form action=\"trim\">";
	    msg += alignmentChooser( "Alignment id: ", "id", params.getProperty("id"), false )+"<br />";
	    msg += "Type: <select name=\"type\"><option value=\"hard\">hard</option><option value=\"perc\">perc</option><option value=\"best\">best</option><option value=\"span\">span</option><option value=\"prop\">prop</option></select><br />Threshold: <input type=\"text\" name=\"threshold\" size=\"4\"/> <small>A value between 0. and 1. with 2 digits</small><br /><input type=\"submit\" name=\"action\" value=\"Trim\"/><br /></form>";
	} else if ( perf.equals("trim") ) {
	    String id = params.getProperty("id");
	    String threshold = params.getProperty("threshold");
	    if ( id != null && !id.equals("") && threshold != null && !threshold.equals("") ){ // Trim it
		Message answer = manager.trim( params );
		if ( answer instanceof ErrorMsg ) {
		    msg = testErrorMessages( answer, params, eSource );
		} else {
		    msg = "<h1>Alignment trimmed</h1>";
		    msg += displayAnswer( answer, params );
		}
	    }
	} else if ( perf.equals("prminv") ) {
	    msg ="<h1>Invert alignment</h1><form action=\"inv\">";
	    msg += alignmentChooser( "Alignment id: ", "id", params.getProperty("id"), false )+"<br />";
	    msg += "<input type=\"submit\" name=\"action\" value=\"Invert\"/><br /></form>";
	} else if ( perf.equals("inv") ) {
	    String id = params.getProperty("id");
	    if ( id != null && !id.equals("") ){ // Invert it
		Message answer = manager.inverse( params );
		if ( answer instanceof ErrorMsg ) {
		    msg = testErrorMessages( answer, params, eSource );
		} else {
		    msg = "<h1>Alignment inverted</h1>";
		    msg += displayAnswer( answer, params );
		}
	    }
	} else if ( perf.equals("prmmatch") ) {
	    String RESTOnto1 = "";
	    String RESTOnto2 = "";
	    String readonlyOnto = "";
	    //Ontologies from Cupboard may be already provided here.
	    if ( params.getProperty("restful") != null && 
		 (params.getProperty("renderer")).equals("HTML") ) {
		RESTOnto1 = params.getProperty("onto1");
		RESTOnto2 = params.getProperty("onto2");
		//if(RESTOnto1 != null && !RESTOnto1.equals("") && RESTOnto2 != null && !RESTOnto2.equals("")) 
		readonlyOnto = "readonly=\"readonly\"";
	    }
	    msg ="<h1>Match ontologies</h1><form action=\"match\">Ontology 1: <input type=\"text\" name=\"onto1\" size=\"80\" value="+RESTOnto1+" " +readonlyOnto+"> (uri)<br />Ontology 2: <input type=\"text\" name=\"onto2\" size=\"80\" value="+RESTOnto2+" "+readonlyOnto+ "> (uri)<br /><small>These are the URL of places where to find these ontologies. They must be reachable by the server (i.e., file:// URI are acceptable if they are on the server)</small><br /><!--input type=\"submit\" name=\"action\" value=\"Find\"/><br /-->Methods: <select name=\"method\">";
	    for( String id : manager.listmethods() ) {
		msg += "<option value=\""+id+"\">"+id+"</option>";
	    }
	    msg += "</select><br />Initial alignment id:  <select name=\"id\"><option value=\"\" selected=\"1\"></option>";
	    // Not used because empty should be first (and selected)
	    //alignmentChooser( "Initial alignment id: ", "id", null, true );
	    for( Alignment al: manager.alignments() ){
		String id = al.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID);
		String pid = al.getExtension( Namespace.EXT.uri, Annotations.PRETTY );
		if ( pid == null ) pid = id; else pid = id+" ("+pid+")";
		msg += "<option value=\""+id+"\">"+pid+"</option>";
	    }
	    msg += "</select><br />";
	    msg += "Pretty name: <input type=\"text\" name=\"pretty\" size=\"80\"/><br />";
	    msg += "<input type=\"submit\" name=\"action\" value=\"Match\"/>";
	    msg += "  <input type=\"checkbox\" name=\"force\" /> Force <input type=\"checkbox\" name=\"async\" /> Asynchronous<br />";
	    msg += "Additional parameters:<br /><input type=\"text\" name=\"paramn1\" size=\"15\"/> = <input type=\"text\" name=\"paramv1\" size=\"65\"/><br /><input type=\"text\" name=\"paramn2\" size=\"15\"/> = <input type=\"text\" name=\"paramv2\" size=\"65\"/><br /><input type=\"text\" name=\"paramn3\" size=\"15\"/> = <input type=\"text\" name=\"paramv3\" size=\"65\"/><br /><input type=\"text\" name=\"paramn4\" size=\"15\"/> = <input type=\"text\" name=\"paramv4\" size=\"65\"/></form>";
	} else if ( perf.equals("match") ) {
	    Message answer = manager.align( params );
	    if ( answer instanceof ErrorMsg ) {
		msg = testErrorMessages( answer, params, eSource );
	    } else {
		msg = "<h1>Alignment results</h1>";
		msg += displayAnswer( answer, params );
	    }
	} else if ( perf.equals("prmfind") ) {
	    msg ="<h1>Find alignments between ontologies</h1><form action=\"find\">Ontology 1: <input type=\"text\" name=\"onto1\" size=\"80\"/> (uri)<br />Ontology 2: <input type=\"text\" name=\"onto2\" size=\"80\"/> (uri)<br /><small>These are the URI identifying the ontologies. Not those of places where to upload them.</small><br /><input type=\"submit\" name=\"action\" value=\"Find\"/></form>";
	    msg += "<h1>Find alignments by URIs</h1><form action=\"get\">URI: <input type=\"text\" name=\"uri\" size=\"80\"/> (uri)<br />Description: <input type=\"text\" name=\"desc\" size=\"80\"/> (found in pretty)<br /><input type=\"submit\" name=\"action\" value=\"Get\"/></form>";
	} else if ( perf.equals("find") ) {
	    Message answer = manager.existingAlignments( params );
	    if ( answer instanceof ErrorMsg ) {
		msg = testErrorMessages( answer, params, eSource );
	    } else {
		msg = "<h1>Found alignments</h1>";
		msg += displayAnswer( answer, params );
	    }
	} else if ( perf.equals("get") ) {
	    Message answer = manager.getAlignments( params );
	    if ( answer instanceof ErrorMsg ) {
		msg = testErrorMessages( answer, params, eSource );
	    } else {
		msg = "<h1>Found alignments</h1>";
		msg += displayAnswer( answer, params );
	    }
	} else if ( perf.equals("corresp") ) {
	    Message answer = manager.findCorrespondences( params );
	    if ( answer instanceof ErrorMsg ) {
		msg = testErrorMessages( answer, params, eSource );
	    } else {
		msg = "<h1>Found correspondences</h1>";
		msg += displayAnswer( answer, params );
	    }
	} else if ( perf.equals("prmretrieve") ) {
	    msg = "<h1>Retrieve alignment</h1><form action=\"retrieve\">";
	    msg += alignmentChooser( "Alignment id: ", "id", params.getProperty("id"), false )+"<br />";
	    msg += "Rendering: <select name=\"method\">";
	    for( String id : manager.listrenderers() ) {
		msg += "<option value=\""+id+"\">"+id+"</option>";
	    }
	    msg += "</select><br /><input type=\"submit\" value=\"Retrieve\"/></form>";
	} else if ( perf.equals("retrieve") ) {

	    Message answer = manager.render( params );
	    if ( answer instanceof ErrorMsg ) {
		msg = testErrorMessages( answer, params, eSource );
	    } else {
		// Depending on the type we should change the MIME type
		// This should be returned in answer.getParameters()
		return answer.getContent();

	    }
	// Metadata not done yet
	} else if ( perf.equals("prmmetadata") ) {
	    msg = "<h1>Retrieve alignment metadata</h1><form action=\"metadata\">";
	    msg += alignmentChooser( "Alignment id: ", "id", params.getProperty("id"), false )+"<br />";
	    msg += "<input type=\"submit\" value=\"Get metadata\"/></form>";
	} else if ( perf.equals("metadata") ) {
	    if( params.getProperty("renderer") == null || (params.getProperty("renderer")).equals("HTML") )
	    	params.setProperty("method", "fr.inrialpes.exmo.align.impl.renderer.HTMLMetadataRendererVisitor");
	    else
		params.setProperty("method", "fr.inrialpes.exmo.align.impl.renderer.XMLMetadataRendererVisitor");
	    Message answer = manager.render( params );
	    //logger.trace( "Content: {}", answer.getContent() );
	    if ( answer instanceof ErrorMsg ) {
		msg = testErrorMessages( answer, params, eSource );
	    } else {
		// Depending on the type we should change the MIME type
		return answer.getContent();
	    }
	    // render
	    // Alignment in HTML can be rendre or metadata+tuples
	} else if ( perf.equals("prmload") ) {
	    // Should certainly be good to offer store as well
	    msg = "<h1>Load an alignment</h1><form action=\"load\">Alignment URL: <input type=\"text\" name=\"url\" size=\"80\"/> (uri)<br /><small>This is the URL of the place where to find this alignment. It must be reachable by the server (i.e., file:// URI is acceptable if it is on the server).</small><br />Pretty name: <input type=\"text\" name=\"pretty\" size=\"80\"/><br />";
	    msg += "<input type=\"checkbox\" name=\"force\" /> Force <br />";
	    msg += "<input type=\"submit\" value=\"Load\"/></form>";
	    //msg += "Alignment file: <form ENCTYPE=\"text/xml; charset=utf-8\" action=\"loadfile\" method=\"POST\">";
	    msg += "Alignment file: <form enctype=\"multipart/form-data\" action=\"load\" method=\"POST\">";
	    msg += "<input type=\"hidden\" name=\"MAX_FILE_SIZE\" value=\""+MAX_FILE_SIZE+"\"/>";
	    msg += "<input name=\"content\" type=\"file\" size=\"35\">";
	    msg += "<br /><small>NOTE: Max file size is "+(MAX_FILE_SIZE/1024)+"KB; this is experimental but works</small><br />";
	    msg += "Pretty name: <input type=\"text\" name=\"pretty\" size=\"80\"/><br />";
	    msg += "<input type=\"checkbox\" name=\"force\" /> Force <br />";
	    msg += "<input type=\"submit\" Value=\"Upload\">";
	    msg +=  " </form>";
	} else if ( perf.equals("load") ) {
	    // load
	    Message answer = manager.load( params );
	    if ( answer instanceof ErrorMsg ) {
		msg = testErrorMessages( answer, params, eSource );
	    } else {
		msg = "<h1>Alignment loaded</h1>";
		msg += displayAnswer( answer, params );
	    }
	} else if ( perf.equals("prmtranslate") ) {
	    msg = "<h1>Translate query</h1><form action=\"translate\">";
	    msg += alignmentChooser( "Alignment id: ", "id", params.getProperty("id"), false )+"<br />";
	    msg += "PREFIX rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt; .<br /><br />SPARQL query:<br /> <textarea name=\"query\" rows=\"20\" cols=\"80\">PREFIX foaf: <http://xmlns.com/foaf/0.1/>\nSELECT *\nFROM <>\nWHERE {\n\n}</textarea> (SPARQL)<br /><small>A SPARQL query (PREFIX prefix: &lt;uri&gt; SELECT variables FROM &lt;url&gt; WHERE { triples })</small><br /><input type=\"submit\" value=\"Translate\"/></form>";
	} else if ( perf.equals("translate") ) {
	    Message answer = manager.translate( params );
	    if ( answer instanceof ErrorMsg ) {
		msg = testErrorMessages( answer, params, eSource );
	    } else {
		msg = "<h1>Message translation</h1>";
		msg += "<h2>Initial message</h2><pre>"+(params.getProperty("query")).replaceAll("&", "&amp;").replaceAll("<", "&lt;")+"</pre>";
		msg += "<h2>Translated message</h2><pre>";
		msg += answer.HTMLString().replaceAll("&", "&amp;").replaceAll("<", "&lt;");
		msg += "</pre>";
	    }
	} else if ( perf.equals("prmeval") ) {
	    msg ="<h1>Evaluate alignment</h1><form action=\"eval\">";
	    msg += alignmentChooser( "Alignment to evaluate: ", "id", params.getProperty("id"), false )+"<br />";
	    msg += alignmentChooser( "Reference alignment: ", "ref", params.getProperty("ref"), false )+"<br />";
	    msg += "Evaluator: ";
	    msg += "<select name=\"method\">";
	    for( String id : manager.listevaluators() ) {
		msg += "<option value=\""+id+"\">"+id+"</option>";
	    }
	    msg += "</select><br /><input type=\"submit\" name=\"action\" value=\"Evaluate\"/>\n";
	    msg += "</form>\n";
	} else if ( perf.equals("eval") ) {
	    Message answer = manager.eval( params );
	    if ( answer instanceof ErrorMsg ) {
		msg = testErrorMessages( answer, params, eSource );
	    } else {
		msg = "<h1>Evaluation results</h1>";
		msg += displayAnswer( answer, params );
	    }
	} else if ( perf.equals("saveeval") ) {
	} else if ( perf.equals("prmgrpeval") ) {
	} else if ( perf.equals("grpeval") ) {
	} else if ( perf.equals("savegrpeval") ) {
	} else if ( perf.equals("prmresults") ) {
	} else if ( perf.equals("getresults") ) {
	} else if ( perf.equals("prmdiff") ) {
	    msg ="<h1>Compare alignments</h1><form action=\"diff\">";
	    msg += alignmentChooser( "First alignment: ", "id1", params.getProperty("id1"), false )+"<br />";
	    msg += alignmentChooser( "Second alignment: ", "id2", params.getProperty("id2"), false )+"<br />";
	    msg += "<br /><input type=\"submit\" name=\"action\" value=\"Compare\"/>\n";
	    msg += "</form>\n";
	} else if ( perf.equals("diff") ) {
	    Message answer = manager.diff( params );
	    if ( answer instanceof ErrorMsg ) {
		msg = testErrorMessages( answer, params, eSource );
	    } else {
		msg = "<h1>Comparison results</h1>";
		msg += displayAnswer( answer, params );
	    }
	} else if ( perf.equals("") ) {
	    msg = "<h1>Alignments</h1>";
	    msg += "<form action=\"../html/listalignments\"><button title=\"List of all the alignments stored in the server\" type=\"submit\">Available alignments</button></form>";
	    msg += "<form action=\"prmload\"><button title=\"Upload an existing alignment in this server\" type=\"submit\">Load alignments</button></form>";
	    msg += "<form action=\"prmfind\"><button title=\"Find existing alignements between two ontologies\" type=\"submit\">Find alignment</button></form>";
	    msg += "<form action=\"prmmatch\"><button title=\"Apply matchers to ontologies for obtaining an alignment\" type=\"submit\">Match ontologies</button></form>";
	    msg += "<form action=\"prmtrim\"><button title=\"Trim an alignment above some threshold\" type=\"submit\">Trim alignment</button></form>";
	    msg += "<form action=\"prminv\"><button title=\"Swap the two ontologies of an alignment\" type=\"submit\">Invert alignment</button></form>";
	    msg += "<form action=\"prmstore\"><button title=\"Persistently store an alignent in this server\" type=\"submit\" >Store alignment</button></form>";
	    msg += "<form action=\"prmretrieve\"><button title=\"Render an alignment in a particular format\" type=\"submit\">Render alignment</button></form>";
	    msg += "<form action=\"prmtranslate\"><button title=\"Query translation through an alignment\" type=\"submit\">Translate query</button></form>";
	    msg += "<form action=\"prmeval\"><button title=\"Evaluation of an alignment\" type=\"submit\">Evaluate alignment</button></form>";
	    msg += "<form action=\"prmdiff\"><button title=\"Compare two alignments\" type=\"submit\">Compare alignment</button></form>";
	    msg += "<form action=\"../noo/\"><button style=\"background-color: lightgreen;\" title=\"Back to network of ontologies menu\" type=\"submit\">Ontology networks</button></form>";
	    msg += "<form action=\"../admin/\"><button style=\"background-color: lightpink;\" title=\"Server management functions\" type=\"submit\">Server management</button></form>";
	} else {
	    msg = "Cannot understand command "+perf;
	}
	return "<html><head>"+HEADER+"</head><body>"+msg+"<hr /><center><small><a href=\".\">Alignment management</a></small></center></body></html>";
    }

    // ===============================================
    // Util

    public String alignmentChooser( String header, String label, String selected, boolean stored ) {
	String msg = header+"<select name=\""+label+"\">";
	for ( Alignment al : manager.alignments() ) {
	    // display only those non stored
	    if ( !stored || !manager.storedAlignment( al ) ){
		String id = al.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID);
		String pid = al.getExtension( Namespace.EXT.uri, Annotations.PRETTY );
		if ( pid == null ) pid = id; else pid = id+" ("+pid+")";
		if ( selected != null && selected.equals( id ) ){
		    msg += "<option selected=\"1\" value=\""+id+"\">"+pid+"</option>";
		} else { msg += "<option value=\""+id+"\">"+pid+"</option>";}
	    }
	}
	return msg + "</select><br />";
    }

    private String testErrorMessages( Message answer, Properties param, String source ) {
	return testErrorMessages( answer, param, source, null );
    }

    private String testErrorMessages( Message answer, Properties param, String errorSource, String returnType ) {
	if ( returnType == HTTPResponse.MIME_RDFXML ) {
	    return answer.RESTString();
	} else if ( returnType == HTTPResponse.MIME_JSON ) {
	    return answer.JSONString();
	} else {
	    switch (errorSource) {
	    case "al": return "<h1>Alignment error</h1>"+answer.HTMLString();
	    case "on": return "<h1>Ontology Network error</h1>"+answer.HTMLString();
	    default:   return "<h1>Unknown source error</h1>"+answer.HTMLString();
	    }
	}
    }

    private String displayAnswer( Message answer, Properties param ) {
	return displayAnswer( answer, param, null );
    }

    private String displayAnswer( Message answer, Properties param, String returnType ) {
	String result = null;
	// String tested by == but should work!
	// Ugly and returned by catched by FindBugs
	if ( returnType == HTTPResponse.MIME_RDFXML ) {
	    if( param.getProperty("return").equals("HTML") ) { // RESTFUL but in HTML ??
	    	result = answer.HTMLRESTString();
	    	if ( answer instanceof AlignmentId && ( answer.getParameters() == null || answer.getParameters().getProperty("async") == null ) ) {
		    result += "<table><tr>";
		    result += "<td><form action=\"getID\"><input type=\"hidden\" name=\"id\" value=\""+answer.getContent()+"\"/><input type=\"submit\" name=\"action\" value=\"GetID\"  disabled=\"disabled\"/></form></td>";
		    result += "<td><form action=\"metadata\"><input type=\"hidden\" name=\"id\" value=\""+answer.getContent()+"\"/><input type=\"submit\" name=\"action\" value=\"Metadata\"/></form></td>";
		    result += "</tr></table>";
	    	} else if( answer instanceof AlignmentIds && ( answer.getParameters() == null || answer.getParameters().getProperty("async") == null )) {
	    	result = answer.HTMLRESTString();
		}
	    } else {
		result = answer.RESTString();
	    }
	} else if ( returnType == HTTPResponse.MIME_JSON ) {
	    result = answer.JSONString();
	} else {
	    result = answer.HTMLString();
	    // Improved return
	    if ( answer instanceof AlignmentId && ( answer.getParameters() == null || answer.getParameters().getProperty("async") == null ) ){
		result += "<table><tr>";
		// STORE
		result += "<td><form action=\"store\"><input type=\"hidden\" name=\"id\" value=\""+answer.getContent()+"\"/><input type=\"submit\" name=\"action\" value=\"Store\"/></form></td>";
		// TRIM (2)
		result += "<td><form action=\"prmtrim\"><input type=\"hidden\" name=\"id\" value=\""+answer.getContent()+"\"/><input type=\"submit\" name=\"action\" value=\"Trim\"/></form></td>";
		// RETRIEVE (1)
		result += "<td><form action=\"prmretrieve\"><input type=\"hidden\" name=\"id\" value=\""+answer.getContent()+"\"/><input type=\"submit\" name=\"action\" value=\"Show\"/></form></td>";
		// Note at that point it is not possible to get the methods
		// COMPARE (2)
		// INV
		result += "<td><form action=\"inv\"><input type=\"hidden\" name=\"id\" value=\""+answer.getContent()+"\"/><input type=\"submit\" name=\"action\" value=\"Invert\"/></form></td>";
		result += "</tr></table>";
	    } else if ( answer instanceof EvaluationId && ( answer.getParameters() == null || answer.getParameters().getProperty("async") == null ) ){
		result += "<table><tr>";
		// STORE (the value should be the id here, not the content)
		result += "<td><form action=\"saveeval\"><input type=\"hidden\" name=\"id\" value=\""+answer.getContent()+"\"/><input type=\"submit\" name=\"action\" value=\"Store\"/></form></td>";
		result += "</tr></table>";
	    }
	}
	return result;
    }
    
    private String displayAnswerON( Message answer, Properties param ) {
    	return displayAnswerON( answer, param, null );
    }

    private String displayAnswerON( Message answer, Properties param, String returnType ) {
    	String result = null;
    	if ( returnType == HTTPResponse.MIME_RDFXML ) {
    	    if ( param.getProperty("return").equals("HTML") ) { // RESTFUL but in HTML ??
    	    	result = answer.HTMLRESTString();
    	    	if ( answer instanceof OntologyNetworkId && ( answer.getParameters() == null || answer.getParameters().getProperty("async") == null ) ) {
    		    result += "<table><tr>";
    		    result += "<td><form action=\"getID\"><input type=\"hidden\" name=\"id\" value=\""+answer.getContent()+"\"/><input type=\"submit\" name=\"action\" value=\"GetID\"  disabled=\"disabled\"/></form></td>";
    		    result += "<td><form action=\"metadata\"><input type=\"hidden\" name=\"id\" value=\""+answer.getContent()+"\"/><input type=\"submit\" name=\"action\" value=\"Metadata\"/></form></td>";
    		    result += "</tr></table>";
    	    	//} else if( answer instanceof OntologyNetworkIds && ( answer.getParameters() == null || answer.getParameters().getProperty("async") == null )) { }
    		    result = answer.HTMLRESTString();
    		}
    	    } else {
    		result = answer.RESTString();
    	    }
    	} else if ( returnType == HTTPResponse.MIME_JSON ) {
    	    result = answer.JSONString();
    	} else {
    	    result = answer.HTMLString();
    	    // Improved return
    	    if ( answer instanceof OntologyNetworkId && ( answer.getParameters() == null || answer.getParameters().getProperty("async") == null ) ){
    		result += "<table><tr>";
    		// STORE ONTOLOGY NETWORK
    		result += "<td><form action=\"store\"><input type=\"hidden\" name=\"id\" value=\""+answer.getContent()+"\"/><input type=\"submit\" name=\"action\" value=\"Store\"/></form></td>";
    		// RETRIEVE ONTOLOGY NETWORK
    		result += "<td><form action=\"prmretrieve\"><input type=\"hidden\" name=\"id\" value=\""+answer.getContent()+"\"/><input type=\"submit\" name=\"action\" value=\"Show\"/></form></td>";  
    		result += "</tr></table>";
    	    }
    	}
    	return result;
    }

}

