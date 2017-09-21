/*
 * $Id: HTTPResponse.java 2102 2015-11-29 10:29:56Z euzenat $
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * HTTP response.
 * Return one of these from serve().
 */
public class HTTPResponse {

    /**
     * Some HTTP response status codes
     */
    public static final String
	HTTP_OK = "200 OK",
	HTTP_REDIRECT = "301 Moved Permanently",
	HTTP_FORBIDDEN = "403 Forbidden",
	HTTP_NOTFOUND = "404 Not Found",
	HTTP_BADREQUEST = "400 Bad Request",
	HTTP_INTERNALERROR = "500 Internal Server Error",
	HTTP_NOTIMPLEMENTED = "501 Not Implemented";

    /**
     * Common mime types for dynamic content
     */
    public static final String
	MIME_PLAINTEXT = "text/plain",
	MIME_HTML = "text/html",
	MIME_XML = "text/xml",
	MIME_JSON = "application/json",
	MIME_RDFXML = "application/rdf+xml",
	MIME_DEFAULT_BINARY = "application/octet-stream";

    /**
     * Default constructor: response = HTTP_OK, data = mime = 'null'
     */
    public HTTPResponse() {
	this.status = HTTP_OK;
    }

    /**
     * Basic constructor.
     * 
     * @param status: the HTTP status value (OK, ERR...)
     * @param mimeType: the MIME type of the answer
     * @param data: the content of the answer
     */
    public HTTPResponse( String status, String mimeType, InputStream data ) {
	this.status = status;
	this.mimeType = mimeType;
	this.data = data;
    }

    /**
     * Convenience method that makes an InputStream out of
     * given text.
     * 
     * @param status: the HTTP status value (OK, ERR...)
     * @param mimeType: the MIME type of the answer
     * @param txt: the text of the answer (to be encoded)
     */
    public HTTPResponse( String status, String mimeType, String txt ) {
	this.status = status;
	this.mimeType = mimeType;
	this.data = new ByteArrayInputStream( txt.getBytes());
	// JE: Added
	this.msg = txt;
    }
    
    /**
     * Adds given line to the header.
     * 
     * @param name: name of the property to add to the HTTP header
     * @param value: value of the property
     */
    public void addHeader( String name, String value ) {
	header.put( name, value );
    }
    
    /**
     * HTTP status code after processing, e.g. "200 OK", HTTP_OK
     */
    public String status;
    
    /**
     * MIME type of content, e.g. "text/html"
     */
    public String mimeType;
    
    /**
     * Data of the response, may be null.
     */
    public InputStream data;
    
    /**
     * Headers for the HTTP response. Use addHeader()
     * to add lines.
     */
    public Properties header = new Properties();
    // JE: Added for testing Jetty
    public String msg;
    public String getStatus() { return status; };
    public String getContentType() { return mimeType; }
    public String getData() { return msg; }
    
}

