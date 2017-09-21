/*
 * $Id: HTTPTransport.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) INRIA, 2006-2015
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

import fr.inrialpes.exmo.align.service.msg.Message;
import fr.inrialpes.exmo.align.service.msg.ErrorMsg;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileOutputStream;

import java.util.Vector;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Enumeration;

import java.net.URLDecoder;

import java.lang.Integer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.Request;

import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.FilenameUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTPTransport: implements the HTTP connection of the server
 * Dispatch messages to the various services
 */

public class HTTPTransport {
    final static Logger logger = LoggerFactory.getLogger( HTTPTransport.class );

    private int tcpPort;
    private String tcpHost;
    private Server server;
    private AServProtocolManager manager;
    private Vector<AlignmentServiceProfile> services;

    private String myId;
    private String serverId;
    private int localId = 0;

    private String returnType = HTTPResponse.MIME_HTML;

    // ==================================================
    // Socket & server code
    // ==================================================

    /**
     * Starts a HTTP server to given port.
     *
     * @param params: the parameters of the connection, including HTTP port and host
     * @param manager: the manager which will deal with connections
     * @param serv: the set of services to be listening on this connection
     * @throws AServException when something goes wrong (e.g., socket already in use)
     */
    public void init( Properties params, AServProtocolManager manager, Vector<AlignmentServiceProfile> serv ) throws AServException {
	this.manager = manager;
	services = serv;
	tcpPort = Integer.parseInt( params.getProperty( "http" ) );
	tcpHost = params.getProperty( "host" ) ;

	// ********************************************************************
	// JE: Jetty implementation
	server = new Server(tcpPort);

	// The handler deals with the request
	// most of its work is to deal with large content sent in specific ways 
	Handler handler = new AbstractHandler(){
		public void handle( String String, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		    String method = request.getMethod();
		    String uri = request.getPathInfo();
		    Properties params = new Properties();
		    try { decodeParams( request.getQueryString(), params ); }
		    catch ( Exception ex) {
			logger.debug( "IGNORED EXCEPTION: {}", ex );
		    };
		    // I do not decode them here because it is useless
		    // See below how it is done.
		    Properties header = new Properties();
		    Enumeration<String> headerNames = request.getHeaderNames();
		    while( headerNames.hasMoreElements() ) {
			String headerName = headerNames.nextElement();
			header.setProperty( headerName, request.getHeader(headerName) );
		    }

		    // Get the content if any
		    // This is supposed to be only an uploaded file
		    // Note that this could be made more uniform 
		    // with the text/xml part stored in a file as well.
		    String mimetype = request.getContentType();
		    // Multi part: the content provided by an upload HTML form
		    if ( mimetype != null && mimetype.startsWith("multipart/form-data") ) {
			try {
			    //if ( !ServletFileUpload.isMultipartContent( request ) ) {
			    //	logger.debug( "Does not detect multipart" );
			    //}
			    DiskFileItemFactory factory = new DiskFileItemFactory();
			    File tempDir = new File( System.getProperty("java.io.tmpdir") );
			    factory.setRepository( tempDir );
			    ServletFileUpload upload = new ServletFileUpload(factory);
			    List<FileItem> items = upload.parseRequest(request);
			    for( FileItem fi : items ) {
				if ( fi.isFormField() ) {
				    logger.trace( "  >> {} = {}", fi.getFieldName(), fi.getString() );
				    params.setProperty( fi.getFieldName(), fi.getString() );
				} else {
				    logger.trace( "  >> {} : {}", fi.getName(), fi.getSize() );
				    logger.trace( "  Stored at {}", fi.getName(), fi.getSize() );
				    try {
					// FilenameUtils.getName() needed for Internet Explorer problem
					File uploadedFile = new File( tempDir, FilenameUtils.getName( fi.getName() ) );
					fi.write( uploadedFile );
					params.setProperty( "filename", uploadedFile.toString() );
					params.setProperty( "todiscard", "true" );
				    } catch ( Exception ex ) {
					logger.warn( "Cannot load file", ex );
				    }
				    // Another solution is to run this in 
				    /*
				      InputStream uploadedStream = item.getInputStream();
				      ...
				      uploadedStream.close();
				    */
				}
			    };
			} catch ( FileUploadException fuex ) {
			    logger.trace( "Upload Error", fuex );
			}
		    } else if ( mimetype != null && mimetype.startsWith("text/xml") ) {
			// Most likely Web service request (REST through POST)
			int length = request.getContentLength();
			if ( length > 0 ) {
			    char [] mess = new char[length+1];
			    try {
				new BufferedReader(new InputStreamReader(request.getInputStream())).read( mess, 0, length);
			    } catch ( Exception e ) {
				logger.debug( "IGNORED Exception", e );
			    }
			    params.setProperty( "content", new String( mess ) );
			}
		    // File attached to SOAP messages
		    } else if ( mimetype != null && mimetype.startsWith("application/octet-stream") ) {
         		File alignFile = new File(File.separator + "tmp" + File.separator + newId() +"XXX.rdf");
         		// check if file already exists - and overwrite if necessary.
         		if (alignFile.exists()) alignFile.delete();
               	 	FileOutputStream fos = new FileOutputStream(alignFile);
            		InputStream is = request.getInputStream();
			
           	        try {
			    byte[] buffer = new byte[4096];
			    int bytes=0; 
			    while (true) {
				bytes = is.read(buffer);
				if (bytes < 0) break;
				fos.write(buffer, 0, bytes);
			    }
            		} catch (Exception e) {
			} finally {
			    fos.flush();
			    fos.close();
			}
               		is.close();
			params.setProperty( "content", "" );
			params.setProperty( "filename" ,  alignFile.getAbsolutePath()  );
         	    }

		    // Get the answer (HTTP)
		    HTTPResponse r = serve( uri, method, header, params );

		    // Return it
		    response.setContentType( r.getContentType() );
		    response.setStatus( HttpServletResponse.SC_OK );
		    response.getWriter().println( r.getData() );
		    ((Request)request).setHandled( true );
		}
	    };
	server.setHandler(handler);

	// Common part
	try { server.start(); }
	catch (Exception e) {
	    throw new AServException( "Cannot launch HTTP Server" , e );
	}
	//server.join();

	// ********************************************************************
	//if ( params.getProperty( "wsdl" ) != null ){
	//    wsmanager = new WSAServProfile();
	//    if ( wsmanager != null ) wsmanager.init( params, manager );
	//}
	//if ( params.getProperty( "http" ) != null ){
	//    htmanager = new HTMLAServProfile();
	//    if ( htmanager != null ) htmanager.init( params, manager );
	//}
	myId = "LocalHTMLInterface";
	serverId = manager.serverURL();
	logger.info( "Launched on {}/html/", serverId );
	localId = 0;
    }

    public void close(){
	if ( server != null ) {
	    try { server.stop(); }
            catch (Exception e) { logger.debug( "IGNORED Exception on close", e ); }
	}
    }
    
    // ==================================================
    // API parts
    // ==================================================

    /**
     * Override this to customize the server.
     *
     * (By default, this delegates to serveFile() and allows directory listing.)
     *
     * @param uri:	Percent-decoded URI without parameters, for example "/index.cgi"
     * @param method:	"GET", "POST" etc.
     * @param parms:	Parsed, percent decoded parameters from URI and, in case of POST, data.
     * @param header:	Header entries, percent decoded
     * @return an HTTP response made of these elements (see @class Response for details)
     */
    public HTTPResponse serve( String uri, String method, Properties header, Properties parms ) {
	logger.debug( "{} '{}'", method, uri );

	// Convert parms to parameters
	Properties params = new Properties();
	for ( Entry<Object,Object> e : parms.entrySet() ) {
	    //logger.trace( "  PRM: '{}' = '{}'", e.getKey(), e.getValue() );
	    String key = (String)e.getKey();
	    if ( key.startsWith( "paramn" ) ){
		params.setProperty( (String)e.getValue(),
				    parms.getProperty( "paramv"+key.substring( 6 ) ) );
	    } else if ( !key.startsWith( "paramv" ) ) {
		params.setProperty( key, (String)e.getValue() );
	    }
	}
	int start = 0;
	while ( start < uri.length() && uri.charAt(start) == '/' ) start++;
	int end = uri.indexOf( '/', start+1 );
	String oper = "";
	if ( end != -1 ) {
	    oper = uri.substring( start, end );
	    start = end+1;
	} else {
	    oper = uri.substring( start );
	    start = uri.length();
	}
	logger.trace( "Oper: {}", oper );

	// Content negotiation first
	String accept = header.getProperty( "Accept" );
	returnType = HTTPResponse.MIME_HTML;
	if ( accept == null ) accept = header.getProperty( "accept" );
	logger.trace( "Accept header: {}", accept );
	if ( accept != null && !accept.equals("") ) {
	    int indexRXML = accept.indexOf( HTTPResponse.MIME_RDFXML );
	    if ( indexRXML == -1 ) indexRXML = accept.indexOf( HTTPResponse.MIME_XML );
	    int indexJSON = accept.indexOf( HTTPResponse.MIME_JSON );
	    if ( indexRXML != -1 ) {
		if ( indexJSON > indexRXML || indexJSON == -1 ) {
		    returnType = HTTPResponse.MIME_RDFXML;
		} else {
		    returnType = HTTPResponse.MIME_JSON;
		}
	    } else if ( indexJSON != -1 ) {
		returnType = HTTPResponse.MIME_JSON;
	    }
	}
	logger.trace( "Return MIME Type: {}", returnType );
	params.setProperty( "returnType", returnType );

	if ( oper.equals( "alid" ) ){ // Asks for an alignment by URI
	    return returnAlignment( uri, returnType );
	} else if ( oper.equals( "onid" ) ){ // Asks for a network by URI
	    return returnNetwork( uri, returnType );		
	} else if ( oper.equals( "" ) ) {
	    // SHOULD BE ASSIGNED TO CONTENT NEGOCIATION AS WELL... (DEFAULT IN SERVERS)
	    //return serveFile( uri, header, new File("."), true );
	    HTMLAServProfile profile = null;
	    for ( AlignmentServiceProfile serv : services ) {
		if ( serv instanceof HTMLAServProfile ) profile = (HTMLAServProfile)serv;
		break;
	    }
	    if ( profile == null ) profile = new HTMLAServProfile();
	    return new HTTPResponse( HTTPResponse.HTTP_OK, HTTPResponse.MIME_HTML, "<html><head>"+HTMLAServProfile.HEADER+"</head><body>"+profile.about()+"</body></html>" );
	} else {
	    // Selects the relevant service for the request
	    for ( AlignmentServiceProfile serv : services ) {
		// JE2014: oper is not anymore an operation
		// It is one of: aserv. rest. wsdl. html.
		// wsdl could be served here
		// the others would be better ( man + noo + align ) / oper
		// That should be a large change
		if ( serv.accept( oper ) ) {
		    return new HTTPResponse( HTTPResponse.HTTP_OK, returnType, serv.process( uri, oper, uri.substring(start), header, params ) );
		}
	    }
	    return noManager( oper );
	}
    }

    protected HTTPResponse noManager( String type ) {
	if ( returnType == HTTPResponse.MIME_JSON ) {
	    return new HTTPResponse( HTTPResponse.HTTP_OK, HTTPResponse.MIME_JSON, "{ \"type\" : \"AServErrorMsg\",\n  \"content\" : \"No "+type+" service launched\"\n}" );
	} else if ( returnType == HTTPResponse.MIME_RDFXML ) {
	    return new HTTPResponse( HTTPResponse.HTTP_OK, HTTPResponse.MIME_RDFXML, "<AServErrorMsg>No "+type+" service launched</AServErrorMsg>" );
	} else {
	    return new HTTPResponse( HTTPResponse.HTTP_OK, HTTPResponse.MIME_HTML, "<html><head>"+HTMLAServProfile.HEADER+"</head><body>"+"<ErrMsg>No "+type+" service launched</ErrMsg>"+"<hr /><center><small><a href=\".\">Alignment server</a></small></center></body></html>" );
	}
    }

    /**
     * Returns the alignment in negociated format
     *
     * @param uri: the URI of an alignment
     * @param mimeType: the MIME type in which the alignment is requested
     * @return an HTTP response corresponding to the alignment in the requested MIME type
     */
    public HTTPResponse returnAlignment( String uri, String mimeType ) {
	Properties params = new Properties();
	params.setProperty( "id", manager.serverURL()+uri );
	if ( returnType == HTTPResponse.MIME_JSON ) { // YES string compared by ==.
	    params.setProperty( "method", "fr.inrialpes.exmo.align.impl.renderer.JSONRendererVisitor" );
	} else if ( returnType == HTTPResponse.MIME_RDFXML ) {
	    params.setProperty( "method", "fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor" );
	} else {
	    params.setProperty( "method", "fr.inrialpes.exmo.align.impl.renderer.HTMLRendererVisitor" );
	}
	logger.trace( "Alignment URI : {}", manager.serverURL()+uri);
	Message answer = manager.render( params );
	if ( answer instanceof ErrorMsg ) {
	    return new HTTPResponse( HTTPResponse.HTTP_NOTFOUND, HTTPResponse.MIME_PLAINTEXT, "Alignment server: unknown alignment : "+answer.getContent() );
	} else {
	    return new HTTPResponse( HTTPResponse.HTTP_OK, mimeType, answer.getContent() );
	}
    }

    /**
     * Returns the network in HTML or RDF
     *
     * @param uri: the URI of a network of ontologies
     * @param mimeType: the MIME type in which the network of ontologies is requested
     * @return an HTTP response corresponding to the network of ontologies in the requested MIME type
     */
    public HTTPResponse returnNetwork( String uri, String mimeType ) {
    	Properties params = new Properties();
    	params.setProperty( "id", manager.serverURL()+uri );
    	logger.trace( "Network URI : {}", manager.serverURL()+uri);
    	if ( returnType == HTTPResponse.MIME_HTML ) { // YES string compared by ==.
	    Message answer = manager.renderHTMLNetwork(params);
	    if ( answer instanceof ErrorMsg ) {
		return new HTTPResponse( HTTPResponse.HTTP_NOTFOUND, HTTPResponse.MIME_PLAINTEXT, "Alignment server: unknown network : "+answer.getContent() );
	    } else {
		return new HTTPResponse( HTTPResponse.HTTP_OK, mimeType, "<html><head>"+HTMLAServProfile.HEADER+"</head><body>"+answer.getContent()+"</body></html>" );
	    }
    	} else {
	    Message answer = manager.renderOntologyNetwork(params);
	    if ( answer instanceof ErrorMsg ) {
		return new HTTPResponse( HTTPResponse.HTTP_NOTFOUND, HTTPResponse.MIME_PLAINTEXT, "Alignment server: unknown network : "+answer.getContent() );
	    } else {
		return new HTTPResponse( HTTPResponse.HTTP_OK, mimeType, answer.getContent() );
	    }
    	}
    }
    // ===============================================
    // Util

    private int newId() { return localId++; }

    private void decodeParams( String params, Properties p ) throws InterruptedException {
	if ( params == null ) return;
	
	for ( String next : params.split("&") ) {
	    int sep = next.indexOf( '=' );
	    if ( sep >= 0 ){
		try {
		    p.put( URLDecoder.decode( next.substring( 0, sep ), "iso-8859-1" ).trim(),
			   // JE: URLDecoder allows for : and / but not #
			   URLDecoder.decode( next.substring( sep+1 ), "iso-8859-1" ));
		} catch ( Exception ex ) { //never thrown
		    logger.debug( "IGNORED (SHOULD NEVER BEEN TROWN: {}", ex );
		};
	    }
	}
    }
}

