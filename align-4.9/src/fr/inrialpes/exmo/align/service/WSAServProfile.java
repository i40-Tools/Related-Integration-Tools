/*
 * $Id: WSAServProfile.java 2150 2017-07-18 15:15:46Z euzenat $
 *
 * Copyright (C) INRIA, 2007-2014, 2017
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
import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.impl.BasicOntologyNetwork;

import fr.inrialpes.exmo.align.service.msg.Message;
import fr.inrialpes.exmo.align.service.msg.ErrorMsg;
import fr.inrialpes.exmo.align.service.msg.NonConformParameters;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.OntologyNetwork;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;

import java.util.Hashtable;
import java.util.Set;
import java.util.HashSet;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.jar.Attributes.Name;

import java.lang.NullPointerException;

// For message parsing
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WSAServProfile: a SOAP and REST over HTTP provile for the Alignment server
 * It uses the HTTP server of HTTPAServProfile
 * 
 * Improvements to come:
 * - provide WSDL from that channel as well
 * - implement request_id management (fully missing here)
 * - use XML/Xpath parsers [Make it namespace aware please]
 * - clean up
 */

public class WSAServProfile implements AlignmentServiceProfile {
    final static Logger logger = LoggerFactory.getLogger( WSAServProfile.class );

    private int tcpPort;
    private String tcpHost;
    private AServProtocolManager manager;
    private String wsdlSpec = "";

    private final static String svcNS = "\n       xml:base='"+Namespace.ALIGNSVC.prefix+"'"+
	"\n       xmlns='"+Namespace.ALIGNSVC.prefix+"'";

    private boolean restful = false;

    private String myId;
    private String serverURL;
    private int localId = 0;

    private DocumentBuilder BUILDER = null;

    // ==================================================
    // Socket & server code
    // ==================================================

    /**
     * Initializes the web service profile
     */
    public void init( Properties params, AServProtocolManager manager ) throws AServException {
	this.manager = manager;
	// This may register the WSDL file to some directory
	serverURL = manager.serverURL()+"/aserv/";
	myId = "SOAPoverHTTPInterface";
	localId = 0;	

	// New XML parsing stuff
	final DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
	fac.setValidating(false);
	fac.setNamespaceAware(false); // Change this!
	try { BUILDER = fac.newDocumentBuilder(); }
	catch (ParserConfigurationException e) {
	    throw new AServException( "Cannot initialize SOAP message parsing", e );
	}

	// Read the WSDL specification
	try {
	    String classPath = System.getProperty("java.class.path",".");
	    StringTokenizer tk = new StringTokenizer(classPath,File.pathSeparator);
	    Set<String> visited = new HashSet<String>();
	    classPath = "";
	    while ( tk != null && tk.hasMoreTokens() ){
		StringTokenizer tk2 = tk;
		tk = null;
		// Iterate on Classpath
		while ( tk2 != null && tk2.hasMoreTokens() ) {
		    File file = new File( tk2.nextToken() );
		    if ( file.isDirectory() ) { // Do nothing, it is in a jar...
		    } else if ( file.toString().endsWith(".jar") &&
				!visited.contains( file.toString() ) &&
				file.exists() ) {
			visited.add( file.toString() );
			try { 
			    JarFile jar = new JarFile( file );
			    Enumeration<JarEntry> enumeration = jar.entries();
			    while( enumeration != null && enumeration.hasMoreElements() ){
				JarEntry entry = enumeration.nextElement();
				String classname = entry.toString();
				if ( classname.equals("fr/inrialpes/exmo/align/service/aserv.wsdl") ){
				    // Parse it
				    InputStream is = jar.getInputStream( entry );
				    BufferedReader in = new BufferedReader(new InputStreamReader(is));
				    String line  = in.readLine(); // Suppress the first line (<?xml...)
				    while ((line = in.readLine()) != null) {
					wsdlSpec += line + "\n";
				    }
				    if (in != null) in.close();
				    wsdlSpec = wsdlSpec.replace( "%%ASERVADDRESS%%", serverURL );
				    // exit
				    enumeration = null;
				    tk2 = null;
				    tk = null;
				    classPath = "";
				}
			    }
			    if ( wsdlSpec.equals("") ){
				// Iterate on needed Jarfiles
				// JE(caveat): this deals naively with Jar files,
				// in particular it does not deal with section'ed MANISFESTs
				Attributes mainAttributes = jar.getManifest().getMainAttributes();
				String path = mainAttributes.getValue( Name.CLASS_PATH );
				logger.debug("  >CP> "+path);
				if ( path != null && !path.equals("") ) {
				    // JE: Not sure where to find the other Jars:
				    // in the path or at the local place?
				    classPath += File.pathSeparator+file.getParent()+File.separator + path.replaceAll("[ \t]+",File.pathSeparator+file.getParent()+File.separator);
				}
			    }
			} catch (NullPointerException nullexp) { //Raised by JarFile
			    logger.warn( "IGNORED Warning {} unavailable", file );
			}
		    }
		}
		if ( !classPath.equals("") ) {
		    tk =  new StringTokenizer(classPath,File.pathSeparator);
		    classPath = "";
		}
	    }
	} catch (IOException ioex) {
	    logger.debug( "IGNORED Exception", ioex );
	}
    }

    // See HTTPTransport about it
    public boolean accept( String prefix ) {
	if ( prefix.equals("aserv") || prefix.equals("rest") || prefix.equals("wsdl") ) return true;
	else return false;
    }

    public String process( String uri, String prefix, String perf, Properties header, Properties params ) {
	restful = false;
	if ( prefix.equals("rest") ) {
	    restful = true;
	    return protocolAnswer( uri, perf, header, params );
	} else if ( prefix.equals("aserv") ) {
	    return protocolAnswer( uri, perf, header, params );
	} else if ( prefix.equals("wsdl") ) {
	    return wsdlAnswer( false );
	} else return "";//about();
    }

    public void close(){
	// This may unregister the WSDL file to some directory
    }
    
    // ==================================================
    // API parts
    // ==================================================

    /**
     * HTTP protocol implementation
     * each call of the protocol is a direct URL
     * and the answer is through the resulting page (RDF? SOAP? HTTP? JSON?)
     * Not implemented yet
     * but reserved if appears useful
     *
     * @param uri: the URI of the request
     * @param perf: its performative (the action to be processed)
     * @param header: the HTTP header
     * @param param: the parameters of the request
     * @return the answer to the request as a string
     */
    public String protocolAnswer( String uri, String perf, Properties header, Properties param ) {
	String method = null;
	String message = null;
	Properties newparameters = null;
	Message answer = null;
	String msg = "";

	// Set parameters if necessary
	if ( restful ) {
	    method = perf;
	    newparameters = param;
	} else {
	    method = header.getProperty("SOAPAction");
	    if ( param.getProperty( "filename" ) == null ) {
		// NOTE: we currently pass the file in place of a SOAP message
		// hence there is no message and no parameters to parse
		// However, there is a way to pass SOAP messages with attachments
		// It would be better to implement this. See:
		// http://www.oracle.com/technology/sample_code/tech/java/codesnippet/webservices/attachment/index.html
		message = param.getProperty("content");
		// Create the DOM tree for the SOAP message
		Document domMessage = null;
		try {
		    domMessage = BUILDER.parse( new ByteArrayInputStream( message.trim().getBytes() ) );
		} catch  ( IOException ioex ) {
		    logger.debug( "IGNORED Exception {}", ioex );
		    answer = new NonConformParameters(0,(Message)null,myId,"Cannot Parse SOAP message",message,(Properties)null);
		} catch  ( SAXException saxex ) {
		    logger.debug( "IGNORED Exception {}", saxex );
		    answer = new NonConformParameters(0,(Message)null,myId,"Cannot Parse SOAP message",message,(Properties)null);
		} catch ( NullPointerException npex ) {
		    logger.debug( "IGNORED Exception {}", npex );
		    answer = new NonConformParameters(0,(Message)null,myId,"Cannot Parse SOAP message",message,(Properties)null);
		}
		newparameters = getParameters( domMessage );
	    } else {
		newparameters = new Properties();
	    }
	}
	//logger.debug( "Analised header" );

	// Process the action
	if ( perf.equals("WSDL") || method.equals("wsdl") || method.equals("wsdlRequest") ) {
	    msg += wsdlAnswer( !restful );
	} else if ( method.equals("listalignmentsRequest") || method.equals("listalignments") ) { // -> List of URI
	    if ( param.getProperty("returnType") == HTTPResponse.MIME_JSON ) {
		msg = "{ \"type\" : \"listalignmentsResponse\",\n";
		if ( newparameters.getProperty("msgid") != null ) {
		    msg += "  \"in-reply-to\" : \""+newparameters.getProperty("msgid")+"\",\n";
		}
		msg += "  \"answer\" : [";
		boolean first = true;
		for( Alignment al: manager.alignments() ){
		    String id = al.getExtension(Namespace.ALIGNMENT.uri, Annotations.ID);
		    if ( first ) {
			msg += "\n        \""+id+"\"";
			first = false;
		    } else {
			msg += ",\n        \""+id+"\"";
		    }
		}
		msg += "\n]}";
	    } else {
		msg = "    <listalignmentsResponse "+svcNS+">\n";
		if ( newparameters.getProperty("msgid") != null ) {
		    msg += "      <in-reply-to>"+newparameters.getProperty("msgid")+"</in-reply-to>\n";
		}
		msg += "      <alignmentList>\n";
		for( Alignment al: manager.alignments() ){
		    String id = al.getExtension(Namespace.ALIGNMENT.uri, Annotations.ID);
		    msg += "        <alid>"+id+"</alid>\n";
		}
		msg += "      </alignmentList>\n    </listalignmentsResponse>\n";
	    }
	} else if ( method.equals("listnetworksRequest") || method.equals("listnetworks") ) { // -> List of URI
	    if ( param.getProperty("returnType") == HTTPResponse.MIME_JSON ) {
		msg = "{ \"type\" : \"listnetworksResponse\",\n";
		if ( newparameters.getProperty("msgid") != null ) {
		    msg += "  \"in-reply-to\" : \""+newparameters.getProperty("msgid")+"\",\n";
		}
		msg += "  \"answer\" : [";
		boolean first = true;
		for( OntologyNetwork oNetwork : manager.ontologyNetworks() ){
		    String id = ((BasicOntologyNetwork)oNetwork).getExtension( Namespace.ALIGNMENT.uri, Annotations.ID );
		    if ( first ) {
			msg += "\n        \""+id+"\"";
			first = false;
		    } else {
			msg += ",\n        \""+id+"\"";
		    }
		}
		msg += "\n]}";
	    } else {
		msg = "    <listnetworksResponse "+svcNS+">\n";
		if ( newparameters.getProperty("msgid") != null ) {
		    msg += "      <in-reply-to>"+newparameters.getProperty("msgid")+"</in-reply-to>\n";
		}
		msg += "      <networkList>\n";
		for( OntologyNetwork oNetwork : manager.ontologyNetworks() ){
		    String id = ((BasicOntologyNetwork)oNetwork).getExtension( Namespace.ALIGNMENT.uri, Annotations.ID );
		    msg += "        <onid>"+id+"</onid>\n";
		}
		msg += "      </networkList>\n    </listnetworksResponse>\n";
	    }
	} else if ( method.equals("matchnetworkRequest") || method.equals("matchnetwork") ) { // -> List of URI
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else if ( newparameters.getProperty( "method" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.matchOntologyNetwork( newparameters );
	    }
	    msg += render( "matchnetworkResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("printnetworkRequest") || method.equals("printnetwork") ) { // -> List of URI
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.renderOntologyNetwork( newparameters );
	    }
	    msg += render( "printnetworkResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("trimnetworkRequest") || method.equals("trimnetwork") ) { // -> List of URI
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else if ( newparameters.getProperty( "threshold" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		if ( newparameters.getProperty( "type" ) == null ) {
		    newparameters.setProperty( "type", "hard" );
		}
		answer = manager.trimOntologyNetwork( newparameters );
	    }
	    msg += render( "trimnetworkResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("invertnetworkRequest") || method.equals("invertnetwork") ) { // -> List of URI
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.invertOntologyNetwork( newparameters );
	    }
	    msg += render( "invertnetworkResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("normalizenetworkRequest") || method.equals("normalizenetwork") ) { // -> List of URI
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.normOntologyNetwork( newparameters );
	    }
	    msg += render( "normalizenetworkResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("denormalizenetworkRequest") || method.equals("denormalizenetwork") ) { // -> List of URI
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.denormOntologyNetwork( newparameters );
	    }
	    msg += render( "denormalizenetworkResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("closenetworkRequest") || method.equals("closenetwork") ) { // -> List of URI
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.closeOntologyNetwork( newparameters );
	    }
	    msg += render( "closenetworkResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("setopnetworksRequest") || method.equals("setopnetworks") ) { // -> List of URI
	    if ( newparameters.getProperty( "id1" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else if ( newparameters.getProperty( "id2" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else if ( newparameters.getProperty( "oper" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.setopOntologyNetwork( newparameters );
	    }
	    msg += render( "setopnetworkResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("storenetworkRequest") || method.equals("storenetwork") ) { // -> List of URI
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.storeOntologyNetwork( newparameters );
	    }
	    msg += render( "storenetworkResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("loadnetworkRequest") || method.equals("loadnetwork") ) { // -> List of URI
	    if ( newparameters.getProperty( "url" ) == null &&
		 param.getProperty( "filename" ) != null ) {
		// HTTP Server has stored it in filename (HTMLAServProfile)
		newparameters.setProperty( "url",  "file://"+param.getProperty( "filename" ) );
	    } else if ( newparameters.getProperty( "url" ) == null &&
		 param.getProperty( "filename" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    }
	    answer = manager.loadOntologyNetwork( newparameters );
	    msg += render( "loadnetworkResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("listmethodsRequest") || method.equals("listmethods") ) { // -> List of String
	    msg += getClasses( "listmethodsResponse", manager.listmethods(), param.getProperty("returnType"), newparameters );
	} else if ( method.equals("listrenderersRequest") || method.equals("listrenderers") ) { // -> List of String
	    msg += getClasses( "listrenderersResponse", manager.listrenderers(), param.getProperty("returnType"), newparameters );
	} else if ( method.equals("listservicesRequest") || method.equals("listservices") ) { // -> List of String
	    msg += getClasses( "listservicesResponse", manager.listservices(), param.getProperty("returnType"), newparameters );
	} else if ( method.equals("listevaluatorsRequest") || method.equals("listevaluators") ) { // -> List of String
	    msg += getClasses( "listevaluatorsResponse", manager.listevaluators(), param.getProperty("returnType"), newparameters );
	} else if ( method.equals("storeRequest") || method.equals("store") ) { // URI -> URI
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.store( newparameters );
	    }
	    msg += render( "storeResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("invertRequest") || method.equals("invert") ) { // URI -> URI
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.inverse( newparameters );
	    }
	    msg += render( "invertResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("trimRequest") || method.equals("trim") ) { // URI * string * float -> URI
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else if ( newparameters.getProperty( "threshold" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		if ( newparameters.getProperty( "type" ) == null ) {
		    newparameters.setProperty( "type", "hard" );
		}
		answer = manager.trim( newparameters );
	    }
	    msg += render( "trimResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("matchRequest") || method.equals("match") ) { // URL * URL * URI * String * boolean * (newparameters) -> URI
	    if ( newparameters.getProperty( "onto1" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else if ( newparameters.getProperty( "onto2" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.align( newparameters );
	    }
	    msg += render( "matchResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("align") ) { // URL * URL * (newparameters) -> URI
	    // This is a dummy method for emulating a WSAlignement service
	    if ( newparameters.getProperty( "onto1" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else if ( newparameters.getProperty( "onto2" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else { // Use the required method if it exists
		if ( newparameters.getProperty( "wsmethod" ) == null ) {
		    newparameters.setProperty( "method", "fr.inrialpes.exmo.align.impl.method.StringDistAlignment" );
		} else {
		    newparameters.setProperty( "method", newparameters.getProperty( "wsmethod" ) );
	    	} // Match the two ontologies
		Message result = manager.align( newparameters );
		if ( result instanceof ErrorMsg ) {
		    answer = result;
		} else {
		    // I got an answer so ask the manager to return it as RDF/XML
		    newparameters = new Properties();
		    newparameters.setProperty( "id",  result.getContent() );
		    if ( newparameters.getProperty( "id" ) == null ) {
			answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
		    } else {
			newparameters.setProperty( "method",  "fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor" );
			newparameters.setProperty( "embedded", "true" );
			answer = manager.render( newparameters );
		    }
		}
	    }
	    msg += render( "alignResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("correspRequest") || method.equals("corresp") ) { // URI * URI * boolean * (newparameters) -> URI*
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else if ( newparameters.getProperty( "entity" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.findCorrespondences( newparameters );
	    }
	    msg += render( "correspResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("findRequest") || method.equals("find") ) { // URI * URI -> List of URI
	    if ( newparameters.getProperty( "onto1" ) == null && newparameters.getProperty( "onto2" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.existingAlignments( newparameters );
            }
	    msg += render( "findResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("getRequest") || method.equals("get") ) { // URI | String -> List of URI
	    if ( newparameters.getProperty( "uri" ) == null || newparameters.getProperty( "desc" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.getAlignments( newparameters );
            }
	    msg += render( "getResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("evalRequest") || method.equals("eval") ) { // URI * URI -> List of URI
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else if ( newparameters.getProperty( "ref" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.eval( newparameters );
            }
	    msg += render( "evalResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("retrieveRequest") || method.equals("retrieve")) { // URI * method -> XML
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else if ( newparameters.getProperty( "method" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		newparameters.setProperty( "embedded", "true" );
		answer = manager.render( newparameters );
	    }
	    msg += render( "retrieveResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("metadataRequest") || method.equals("metadata") ) { // URI -> XML
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		newparameters.setProperty( "embedded", "true" );
		newparameters.setProperty( "method", "fr.inrialpes.exmo.align.impl.renderer.XMLMetadataRendererVisitor");
		answer = manager.render( newparameters );
            }
	    msg += render( "metadataResponse", answer, param.getProperty("returnType"), newparameters);
	} else if ( method.equals("loadRequest") || method.equals("load") ) { // URL -> URI
	    if ( newparameters.getProperty( "url" ) == null &&
		 param.getProperty( "filename" ) != null ) {
		// HTTP Server has stored it in filename (HTMLAServProfile)
		newparameters.setProperty( "url",  "file://"+param.getProperty( "filename" ) );
	    } else if ( newparameters.getProperty( "url" ) == null &&
		 param.getProperty( "filename" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    }
	    answer = manager.load( newparameters );
	    msg += render( "loadResponse", answer, param.getProperty("returnType"), newparameters);
	    /*
	      // This has never been in use.
	} else if ( method.equals("loadfileRequest") ) { // XML -> URI
	    if ( newparameters.getProperty( "url" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.load( newparameters );
	    }
	    msg += "    <loadResponse"+svcNS+">\n"+answer.SOAPString()+"    </loadResponse>\n";
	    */
	} else if ( method.equals("translateRequest") || method.equals("translate") ) { // XML * URI -> XML
	    if ( newparameters.getProperty( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else if ( newparameters.getProperty( "query" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Properties)null);
	    } else {
		answer = manager.translate( newparameters );
	    }
	    msg += render( "translateResponse", answer, param.getProperty("returnType"), newparameters);
	} else {
	    if ( param.getProperty("returnType") == HTTPResponse.MIME_JSON ) {
		msg += "{ \"type\" : \"UnRecognizedAction\" }";
	    } else {
		msg += "    <UnRecognizedAction "+svcNS+"/>\n";
	    }
	}

	if ( restful ) {
	    return msg;
	} else {
	    return "<"+Namespace.SOAP_ENV.shortCut+":Envelope\n"  +
		"   xmlns:"+Namespace.SOAP_ENV.shortCut+"='"+Namespace.SOAP_ENV.prefix+"'\n"+
		"   xmlns:"+Namespace.XSI.shortCut+"='"+Namespace.XSI.prefix+"'\n" +
		"   xmlns:"+Namespace.XSD.shortCut+"='"+Namespace.XSD.uri+"'>\n" +
		"  <"+Namespace.SOAP_ENV.shortCut+":Body>\n"+msg+"  </"+Namespace.SOAP_ENV.shortCut+":Body>\n" +
		"</"+Namespace.SOAP_ENV.shortCut+":Envelope>\n";
	}
    }
    
    public String render( String type, Message mess, String mimeType, Properties param ) {
	String res;
	if ( mimeType.equals( HTTPResponse.MIME_JSON ) ) {
	    res = "{ \"type\" : \""+type+"\",\n";
	    if ( mess.getId() != 0 ) res += "  \"id\" : \""+mess.getId()+"\",\n";
	    if ( mess.getSender() != null ) res += "  \"sender\" : \""+mess.getSender()+"\",\n";
	    if ( mess.getReceiver() != null ) res += "  \"receiver\" : \""+mess.getReceiver()+"\",\n";
	    if ( mess.getInReplyTo() != null ) res += "  \"in-reply-to\" : \""+mess.getInReplyTo().getId()+"\",\n";
	    res += "  \"answer\" : "+mess.JSONString()+" }";
	} else {
	    res = "    <"+type+" "+svcNS+">\n";
	    if ( mess.getId() != 0 ) res += "      <id>"+mess.getId()+"</id>\n";
	    if ( mess.getSender() != null ) res += "      <sender>"+mess.getSender()+"</sender>\n";
	    if ( mess.getReceiver() != null ) res += "      <receiver>"+mess.getReceiver()+"</receiver>\n";
	    if ( mess.getInReplyTo() != null ) res += "      <in-reply-to>"+mess.getInReplyTo().getId()+"</in-reply-to>\n";
	    res += "      "+mess.SOAPString()+"\n    </"+type+">\n";
	}
	return res;
    }

    private String getClasses( String type, Set<String> classlist, String mimeType, Properties param ){
	String res = "";
	if ( mimeType == HTTPResponse.MIME_JSON ) {
	    res = "{ \"type\" : \""+type+"\",\n  \"answer\" : [";
	    if ( param.getProperty("msgid") != null ) {
		res += "  \"in-reply-to\" : \""+param.getProperty("msgid")+"\",\n";
	    }
	    boolean first = true;
	    for( String mt: classlist ) {
		if ( first ) {
		    res += "\n        \""+mt+"\"";
		    first = false;
		} else {
		    res += ",\n        \""+mt+"\"";
		}
	    }
	    res += "\n]}";
	} else {
	    res = "    <"+type+" "+svcNS+">\n";
	    if ( param.getProperty("msgid") != null ) {
		res += "        <in-reply-to>"+param.getProperty("msgid")+"</in-reply-to>\n";
	    }
	    res += "      <classList>\n";
	    for( String mt: classlist ) {
		res += "        <classname>"+mt+"</classname>\n";
	    }
	    res += "      </classList>\n    </"+type+">\n";
	}
	return res;
    }

    public String wsdlAnswer( boolean embedded ) { 
	if ( embedded )	return wsdlSpec;
	else return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+wsdlSpec;
    }

    /**
     * Extract parameters from a DOM document resulting from parsing a SOAP messgae
     *
     * @param doc: a DOM document resulting from parsing a SOAP messgae
     * @return the content of the SOAP message as a property set
     */
    private Properties getParameters( Document doc ) {
	Properties params = new Properties();
	XPath path = XPathFactory.newInstance().newXPath();
	try {
	    XPathExpression expr = path.compile("//Envelope/Body/*");
	    NodeList result = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
	    // Believe it or not, NodeList has no iterator!
	    for (int i = 0; i < result.getLength(); i++) {
		Node item = result.item(i);
		// Check getNodeType() please
		String key = item.getNodeName();
		if ( key != null ) {
		    String val = item.getTextContent().trim();
		    // This is for <param name="k">value</param>
		    if ( key.equals("param") ) {
			key = item.getAttributes().getNamedItem("name").getNodeValue();
		    }
		    params.setProperty( key, val );
		}
	    }
	} catch (XPathExpressionException e) {
	  logger.warn( "[getParameters] XPath exception: should not occur");
	} catch (NullPointerException e) {
	  logger.warn( "[getParameters] NullPointerException: should not occur");
	}
	return params;
    }

}
