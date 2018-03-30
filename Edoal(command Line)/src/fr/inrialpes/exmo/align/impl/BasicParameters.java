/*
 * $Id: BasicParameters.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) INRIA, 2004-2005, 2008-2010, 2012-2014
 * Copyright (C) University of Montréal, 2004
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

package fr.inrialpes.exmo.align.impl; 

// import java classes
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Collection;
import java.util.Properties;
import java.util.Map.Entry;
import java.io.PrintStream;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.semanticweb.owl.align.Parameters;

/**
  * @deprecated
  * This class implements Parameters for compatibility purposes.
  * Parameter list structure used everywhere at the begining of the API.
  * Parameters has been "morally" deprecated in the Alignment API for long, 
  * it is now (4.4) not used anymore and marked deprecated.
  *
  * By default and for means of communication, parameter names and values 
  * are Strings (even if their type is Object).
  *
  * A note about unchecked warnings
  * java.util.Properties is declared as {@literal hashtable<Object,Object>}
  * However all its accessors can only put String as key in the hashtable
  * But propertyNames returns Enumeration and not {@literal Enumeration<String>}
  * Using keySet will not change anything, because it will be {@literal Set<Object>}
  * Java 6 introduces {@literal Set<String> stringPropertyNames()} !!
  * 
  * @author Jérôme Euzenat
  * @version $Id: BasicParameters.java 2102 2015-11-29 10:29:56Z euzenat $ 
 */

@Deprecated
public class BasicParameters extends Properties implements Parameters, Cloneable {
    final static Logger logger = LoggerFactory.getLogger( BasicParameters.class );
 
    static final long serialVersionUID = 400L;

    public BasicParameters() {}

    public BasicParameters( Properties prop ) {
	for ( Entry<Object,Object> e : prop.entrySet() ) {
	    setProperty( (String)e.getKey(), (String)e.getValue() );
	}
    }
  
    public void setParameter( String name, String value ){
	setProperty( name, value );
    }

    public void unsetParameter( String name ){
	setProperty( name, (String)null );
    }

    public String getParameter( String name ){
	return getProperty( name );
    }
    
    @SuppressWarnings( "unchecked" )
    public Enumeration<String> getNames(){
	return (Enumeration<String>)propertyNames(); //[W:unchecked]
    }

    public Collection<Object> getValues(){
	return values();
    }

    /**
     * This is legacy code.
     * java.lang.Properties offers p.storeToXML( System.out, "" )
     * and p.loadFromXML( new FileInputStream( filename ) )
     * which are natural implementers for this.
     */
    public void write() {
	System.out.println("<?xml version='1.0' ?>");
	System.out.println("<Parameters>");
	for ( Entry<Object,Object> e : entrySet() ) {
	    System.out.println("  <param name='"+e.getKey()+"'>"+e.getValue()+"</param>");
	}
	System.out.println("</Parameters>");
    }

    /**
     * displays the current parameters (debugging)
     *
     * @param stream: the stream in which to display the parameters
     */
    public void displayParameters( PrintStream stream ){
	stream.println("Parameters:");
	for ( Entry<Object,Object> e : entrySet() ) {
	    stream.println("  "+e.getKey()+" = "+e.getValue());
	}
    }

    public static BasicParameters read( String filename ){
	return read( new BasicParameters(), filename );
    }

    public static BasicParameters read( BasicParameters p, String filename ){
	try {
	    // open the stream
	    DocumentBuilderFactory docBuilderFactory =
		DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
	    Document doc = docBuilder.parse(new File(filename));

	    // normalize text representation
	    doc.getDocumentElement().normalize();

	    // Get the params
	    NodeList paramList = doc.getElementsByTagName("param");
	    int totalParams = paramList.getLength();
	    for (int s = 0; s < totalParams; s++) {
		Element paramElement = (Element)paramList.item(s);
		String paramName = paramElement.getAttribute("name");
		NodeList paramContent = paramElement.getChildNodes();
		String paramValue = paramContent.item(0).getNodeValue().trim();
		p.setParameter(paramName, paramValue); 
	    }
	} catch ( SAXParseException err ) {
	    logger.debug( "IGNORED SAX Parsing exception", err );
	} catch ( SAXException e ) {
	    logger.debug( "IGNORED SAX exception", e );
	} catch ( Throwable t ) {
	    logger.debug( "IGNORED Exception", t );
	}

	return p;
    }

    public Object clone() {
	return super.clone();
    }    
}
