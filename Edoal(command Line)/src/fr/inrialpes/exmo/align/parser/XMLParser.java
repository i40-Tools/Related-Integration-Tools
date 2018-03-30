/*
 * $Id: XMLParser.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2005, 2007-2015
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

package fr.inrialpes.exmo.align.parser;

//Imported SAX classes
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

//Imported JAXP Classes
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;

//Imported JAVA classes
import java.io.IOException;
import java.io.StringReader;
import java.io.Reader;
import java.io.InputStream;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.lang.Integer;
import java.lang.Double;
import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.AlignmentException;

import fr.inrialpes.exmo.ontowrap.Ontology;
import fr.inrialpes.exmo.ontowrap.LoadedOntology;
import fr.inrialpes.exmo.ontowrap.BasicOntology;

import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.BasicCell;
import fr.inrialpes.exmo.align.impl.Annotations;
import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.impl.Extensions;

/**
 * This class allows the creation of a parser for an Alignment file.
 * The class is called by:
 * AlignmentParser parser = new AlignmentParser();
 * Alignment alignment = parser.parse( input );
 * input can be a URI as a String, an InputStream
 * This new version (January 2004) parses the alignment description in
 * RDF/XML/OWL format
 *
 */

public class XMLParser extends DefaultHandler {
    final static Logger logger = LoggerFactory.getLogger( XMLParser.class );

    /**
     * a URI to a process
     */
    protected String uri = null;
    
    /**
     * the first Ontology 
     */
    Ontology<Object> onto1 = null;
    Ontology<Object> curronto = null;
    
    /**
     * the second Ontology 
     */
    Ontology<Object> onto2 = null;

    /**
     * the alignment that is parsed
     * We always create a URIAlignment (we could also use a BasicAlignment).
     * This is a pitty but the idea of creating a particular alignment
     * is not in accordance with using an interface.
     */
    protected URIAlignment alignment = null;

    /**
     * the content found as text...
     */
    protected String content = null;
    
    /**
     * the first entity of a cell
     */
    protected Object cl1 = null;
    
    /**
     * the second entity of a cell
     */
    protected Object cl2 = null;
    
    /**
     * the relation content as text...
     */
    protected Cell cell = null;
    
    /**
     * the relation content as text...
     */
    protected String relation = null;
    
    /**
     * the cell id as text...
     */
    protected String id = null;

    /**
     * Stores encountered cell ids
     */
    protected Set<String> cellids = null;

    /**
     * the semantics of the cell (default first-order)...
     */
    protected String sem = null;

    /**
     * Cell extensions (default null)
     */
    protected Extensions extensions = null;

    /**
     * the measure content as text...
     */
    protected String measure = null;
    
    /**
     * XML Parser
     */
    protected SAXParser parser = null;

    /**
     * The parsing level, if equal to 3 we are in the Alignment
     * if equal to 5 we are in a cell
     * and can find metadata
     */
    protected int parseLevel = 0;

    /**
     * Is the Alignment RDF for embedded in a larger XML structure to be parsed.
     */
    protected boolean embedded = false;

    /**
     * The level at which we found the Alignment tag.
     * It is -1 outside the alignment.
     */
    protected int alignLevel = -1;

    /** 
     * Creates an XML Parser.
     * @throws SAXException when syntax errors are encountered
     * @throws ParserConfigurationException when the parser cannot be configured
     */
    public XMLParser() throws ParserConfigurationException, SAXException {
	this(false);
    }

    /** 
     * Creates an XML Parser.
     * @param validate: 0 if non validating, more otherwise
     * This should become a boolean
     * @throws SAXException when syntax errors are encountered
     * @throws ParserConfigurationException when the parser cannot be configured
     */
    public XMLParser( int validate ) throws ParserConfigurationException, SAXException {
	this( (validate > 0) );
    }

    public XMLParser( boolean validate ) throws ParserConfigurationException, SAXException {
	SAXParserFactory parserFactory = SAXParserFactory.newInstance();
	parserFactory.setValidating( validate );
	parserFactory.setNamespaceAware( true );
	parser = parserFactory.newSAXParser();
	cellids = new HashSet<String>();
    }

    public void setEmbedded( boolean b ){
	embedded = b;
    }
    
    /** 
     * Parses the document corresponding to the URI given in parameter
     * If the current process has links (import or include) to others documents then they are 
     * parsed.
     * @param uri URI of the document to parse
     * Note: SAXParser has a parse( String uri ) method but it does not do content negotiation
     * Hence we have to reimplement it (2014)
     *
     * @return the parsed alignment
     * @throws AlignmentException when something goes wrong
     */
    public Alignment parse( String uri ) throws AlignmentException {
	try {
	    // Reimplemented URI parsing because this does not do content negotiation
	    //parser.parse( uri, this );
	    URLConnection connection = new URL( uri ).openConnection();
	    connection.setRequestProperty( "Accept", "text/xml, application/rdf+xml" );
	    parser.parse( connection.getInputStream(), this );
	} catch ( SAXException sex ) {
	    throw new AlignmentException( "Parsing error", sex );
	} catch ( IOException ioex ) {
	    throw new AlignmentException( "I/O error", ioex );
	}
	return alignment;
    }

    /** 
     * Parses a reader, used for reading from a string
     *
     * @param r the reader from which to parse
     * @return the parsed alignment
     * @throws AlignmentException when something goes wrong
     */
    public Alignment parse( Reader r ) throws AlignmentException {
	try {
	    parser.parse( new InputSource( r ), this );
	} catch ( SAXException sex ) {
	    throw new AlignmentException( "Parsing error", sex );
	} catch ( IOException ioex ) {
	    throw new AlignmentException( "I/O error", ioex );
	}
	return alignment;
    }

    /** 
     * Parses a string instead of a URI
     *
     * @param s String the string to parse
     * @return the parsed alignment
     * @throws AlignmentException when something goes wrong
     */
    public Alignment parse( InputStream s ) throws AlignmentException {
	try {
	    parser.parse( s, this );
	} catch ( SAXException sex ) {
	    throw new AlignmentException( "Parsing error", sex );
	} catch ( IOException ioex ) {
	    throw new AlignmentException( "I/O error", ioex );
	}
	return alignment;
    }

    /** 
     * Allows to have the parser filling an existing alignment instead
     * of creating a new one
     * @param al URIAlignment the alignment to be returned by the parser
     *
     * Note that this function is also useful for reseting the parser 
     * and using it once again by parser.initAlignment( null )
     * Otherwise, this may lead to errors.
     */
    public void initAlignment( URIAlignment al ) {
	alignment = al;
	cellids = new HashSet<String>();
    }
    
    /** 
     * Called by the XML parser at the begining of an element.
     * The corresponing graph component is create for each element.
     *
     * @param namespaceURI: 	The namespace of the current element
     * @param pName: 			The local name of the current element
     * @param qname:					The name of the current element 
     * @param atts: 					The attributes name of the current element 
     * @throws SAXException when something goes wrong
     */
    public void startElement( String namespaceURI, String pName, String qname, Attributes atts ) throws SAXException {
	logger.trace( "startElement XMLParser : {}", pName );
	parseLevel++;
	if( namespaceURI.equals( Namespace.ALIGNMENT.uri+"#" )
	    || namespaceURI.equals( Namespace.ALIGNMENT.uri ) )  {
	    if (pName.equals( SyntaxElement.RULE_RELATION.name )) {
	    } else if (pName.equals( SyntaxElement.SEMANTICS.name )) {
	    } else if (pName.equals( SyntaxElement.MEASURE.name )) {
	    } else if (pName.equals( SyntaxElement.ENTITY2.name )) {
		//logger.trace( " resource = {}", atts.getValue(SyntaxElement.RDF_RESOURCE.print()) );
		try {
		    cl2 = new URI( atts.getValue(SyntaxElement.RDF_RESOURCE.print()) );
		} catch (URISyntaxException e) {
		    throw new SAXException("Malformed URI: "+atts.getValue(SyntaxElement.RDF_RESOURCE.print()));
		}
	    } else if (pName.equals( SyntaxElement.ENTITY1.name )) {
		//logger.trace(" resource = {}", atts.getValue(SyntaxElement.RDF_RESOURCE.print()));
		try {
		    cl1 = new URI( atts.getValue( SyntaxElement.RDF_RESOURCE.print() ) );
		} catch (URISyntaxException e) {
		    throw new SAXException("Malformed URI: "+atts.getValue(SyntaxElement.RDF_RESOURCE.print()));
		}
	    } else if (pName.equals( SyntaxElement.CELL.name )) {
		if ( alignment == null )
		    { throw new SAXException("No alignment provided"); };
		if ( atts.getValue( SyntaxElement.RDF_ID.print() ) != null ){
		    id = atts.getValue( SyntaxElement.RDF_ID.print() );
		} else if ( atts.getValue( SyntaxElement.RDF_ABOUT.print() ) != null ){
		    id = atts.getValue( SyntaxElement.RDF_ABOUT.print() );
		}
		sem = null;
		measure = null;
		relation = null;
		extensions = null;
		cl1 = null;
		cl2 = null;
	    } else if (pName.equals( SyntaxElement.MAP.name )) {//"map"
		try {
		    alignment.init( onto1, onto2 );
		} catch ( AlignmentException e ) {
		    throw new SAXException("Catched alignment exception", e );
		}
	    } else if (pName.equals( SyntaxElement.FORMALISM.name )) {
		if ( atts.getValue( SyntaxElement.URI.name ) != null )
		    try {
			curronto.setFormURI( new URI(atts.getValue( SyntaxElement.URI.name )) );
		    } catch ( URISyntaxException e ) {
			throw new SAXException("Malformed URI"+atts.getValue( SyntaxElement.URI.name ), e );
		    };
		if ( atts.getValue( SyntaxElement.NAME.name ) != null )
		    curronto.setFormalism( atts.getValue( SyntaxElement.NAME.name ) );
	    } else if (pName.equals( SyntaxElement.FORMATT.name )) {
	    } else if (pName.equals( SyntaxElement.LOCATION.name )) {
	    } else if (pName.equals( SyntaxElement.ONTOLOGY.name )) {
		String about = atts.getValue( SyntaxElement.RDF_ABOUT.print() );
		if ( about != null && !about.equals("") ) {
		    try {
			curronto.setURI( new URI( about ) );
		    } catch (URISyntaxException e) {
			throw new SAXException("onto2: malformed URI");
		    }
		}
	    } else if (pName.equals( SyntaxElement.MAPPING_TARGET.name )) {
		curronto = onto2;
	    } else if (pName.equals( SyntaxElement.MAPPING_SOURCE.name )) {
		curronto = onto1;
	    } else if (pName.equals("uri2")) { // Legacy
	    } else if (pName.equals("uri1")) { // Legacy
	    } else if (pName.equals( SyntaxElement.ALIGNMENT.name )) {
		alignLevel = parseLevel;
		parseLevel = 2; // for embeded (RDF is usually 1)
		if ( alignment == null ) alignment = new URIAlignment();
		onto1 = alignment.getOntologyObject1();
		onto2 = alignment.getOntologyObject2();
		String about = atts.getValue( SyntaxElement.RDF_ABOUT.print() );
		if ( about != null && !about.equals("") ) {
		    alignment.setExtension( Namespace.ALIGNMENT.uri, Annotations.ID, about );
		};
	    } else {
		// This should avoid all the stuff before
		if ( SyntaxElement.getSyntaxElement( Namespace.ALIGNMENT, pName ) == null &&
		     // Deals with legacy extensions (have changed prefix)
		     !pName.equals( Annotations.TIME ) && 
		     !pName.equals( Annotations.METHOD ) && 
		     !pName.equals( Annotations.PRETTY ) ) {
		    logger.warn( "Unknown element name : {}", pName );
		}
	    };
	} else if ( namespaceURI.equals( Namespace.SOAP_ENV.prefix )) { //"http://schemas.xmlsoap.org/soap/envelope/"))  {
	    // Ignore SOAP namespace
	    if ( !pName.equals("Envelope") && !pName.equals("Body") ) {
		throw new SAXException("[XMLParser] unknown element name: "+pName); };
	} else if (namespaceURI.equals( Namespace.RDF.prefix )) { //"http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	    if ( !pName.equals("RDF") ) {
		throw new SAXException("[XMLParser] unknown element name: "+pName); };
	} else if (namespaceURI.equals( Namespace.EDOAL.prefix )) { 
	    throw new SAXException("[XMLParser] EDOAL alignment must have type EDOAL: "+pName);
	} else {
	    if ( alignLevel != -1 && parseLevel != 3 && parseLevel != 5 && !embedded ) throw new SAXException("[XMLParser("+parseLevel+")] Unknown namespace : "+namespaceURI);
	}
    }

    private Object getEntity( Object ontology, String name ) throws SAXException {
	try { return new URI( name ); }
	catch (URISyntaxException e) {
	    throw new SAXException("[XMLParser] bad URI syntax : "+name);}
    }

    /**
     * From a patch proposed by Sabine Massmann
     * Get around some nasty double parsing bug. Sometimes it parses
     * 6.925955630686735E-4 as
     * content XMLParser : 6.925955630686735
     * content XMLParser : E-4
     *
     * @param ch: an array of chars from the string part of XML
     * @param start: the beging of the string to extract
     * @param length: the length of the string to extract
     */
    public void characters( char ch[], int start, int length ) {
	String newContent = new String( ch, start, length );
	if ( content != null && content.indexOf('.',0) != -1 // a float
	     && newContent != null && !newContent.startsWith("\n ") 
	     ) {
	    content += newContent;
	} else {
	    content = newContent; 
	}
	logger.trace( "content XMLParser : {}", content );
    }

    /** 
     * Called by the XML parser at the end of an element.
     *
     * @param namespaceURI: 	The namespace of the current element
     * @param pName: 			The local name of the current element
     * @param qName:					The name of the current element 
     * @throws SAXException when something goes wrong
     */
    public  void endElement( String namespaceURI, String pName, String qName ) throws SAXException {
	logger.trace( "endElement XMLParser : {}", pName );
	if( namespaceURI.equals( Namespace.ALIGNMENT.uri+"#" )
	    || namespaceURI.equals( Namespace.ALIGNMENT.uri ) )  {
	    try {
		if (pName.equals( SyntaxElement.RULE_RELATION.name )) {
		    relation = content;
		} else if (pName.equals( SyntaxElement.MEASURE.name )) {
		    measure = content;
		} else if (pName.equals( SyntaxElement.SEMANTICS.name )) {
		    sem = content;
		} else if (pName.equals( SyntaxElement.ENTITY2.name )) {
		} else if (pName.equals( SyntaxElement.ENTITY1.name )) {
		} else if (pName.equals( SyntaxElement.CELL.name )) {
		    //logger.trace( " {}", cl1 );
		    //logger.trace( " {}", cl2 );
		    //logger.trace( " {}", relation);
		    //logger.trace( " {}", Double.parseDouble(measure));
		    if ( cl1 == null || cl2 == null ) {
			// Maybe we could just print this out and fail in the end.
			//throw new SAXException( "Missing entity "+cl1+" "+cl2 );
			// The cell is void
			logger.warn( "(cell voided), missing entity {} {}", cl1, cl2 );
		    } else if ( measure == null || relation == null ){
			cell = alignment.addAlignCell( cl1, cl2);
		    } else {
			// This test must be revised for more generic confidence (which should then be strings)
			double conf = Double.parseDouble( measure );
			/* JE2015: leave this for the confidence implmementation
			if ( conf > 1. || conf < 0. )
			    throw new SAXException( "Bad confidence value : "+conf+" (should belong to [0. 1.])" );
			*/
			cell = alignment.addAlignCell( cl1, cl2, relation, conf );}
		    if ( id != null ) {
			// There is no registration of cell ids in the alignment...
			if ( cellids.contains( id ) ) 
			    throw new AlignmentException( "CellIds must be unique within one alignment: "+id );
			cellids.add( id );
			cell.setId( id );
		    }
		    if ( sem != null ) cell.setSemantics( sem );
		    if ( extensions != null ) ((BasicCell)cell).setExtensions( extensions );
		} else if (pName.equals( SyntaxElement.MAP.name )) {
		} else if (pName.equals("uri1")) { // Legacy
		    try {
			URI u = new URI( content );
			onto1.setURI( u );
			if ( onto1.getFile() == null ) onto1.setFile( u );
		    } catch (URISyntaxException e) {
			throw new SAXException("uri1: malformed URI : "+content);
		    }
		} else if (pName.equals("uri2")) { // Legacy
		    try {
			URI u = new URI( content );
			onto2.setURI( u );
			if ( onto2.getFile() == null ) onto2.setFile( u );
		    } catch (URISyntaxException e) {
			throw new SAXException("uri1: malformed URI : "+content);
		    }
		} else if (pName.equals( SyntaxElement.ONTOLOGY.name )) {
		} else if (pName.equals( SyntaxElement.LOCATION.name )) {
		    try { curronto.setFile( new URI( content ) );
		    } catch (URISyntaxException e) {
			throw new SAXException("Malformed URI : "+content );
		    }
		} else if (pName.equals( SyntaxElement.FORMALISM.name )) {
		} else if (pName.equals( SyntaxElement.FORMATT.name )) {
		} else if (pName.equals( SyntaxElement.MAPPING_SOURCE.name ) || pName.equals( SyntaxElement.MAPPING_TARGET.name )) {
		    if ( curronto.getFile() == null && 
			 content != null && !content.equals("") ) {
			try {
			    URI u = new URI( content );
			    curronto.setFile( u );
			    if ( curronto.getURI() == null ) curronto.setURI( u );
			} catch (URISyntaxException e) {
			    throw new SAXException(pName+": malformed URI : "+content );
			}
		    };
		    curronto = null;
		} else if (pName.equals( SyntaxElement.TYPE.name )) {
		    alignment.setType( content );
		} else if (pName.equals( SyntaxElement.LEVEL.name )) {
		    if ( content.startsWith("2") ) { // Maybe !startsWith("0") would be better
			throw new SAXException("Cannot parse Level 2 alignments (so far)");
		    } else {
			alignment.setLevel( content );
		    }
		} else if (pName.equals( SyntaxElement.RELATION_CLASS.name )) {
		    alignment.setRelationType( content );
		} else if (pName.equals( SyntaxElement.CONFIDENCE_CLASS.name )) {
		    alignment.setConfidenceType( content );
		} else if (pName.equals( SyntaxElement.XML.name )) {
		    //if ( content.equals("no") )
		    //	{ throw new SAXException("Cannot parse non XML alignments"); }
		} else if (pName.equals( SyntaxElement.ALIGNMENT.name )) {
		    parseLevel = alignLevel; // restore level<
		    alignLevel = -1;
		} else {
		    if ( namespaceURI.equals( Namespace.ALIGNMENT.uri+"#" ) ) namespaceURI = Namespace.ALIGNMENT.uri;
		    // For trapping those past URIs
		    if ( namespaceURI == Namespace.ALIGNMENT.uri &&
			 SyntaxElement.getSyntaxElement( Namespace.ALIGNMENT, pName ) == null &&
			 ( pName.equals( Annotations.TIME ) || pName.equals( Annotations.METHOD ) || pName.equals( Annotations.PRETTY ) ) ) {
			namespaceURI = Namespace.EXT.uri;
		    }
		    if ( parseLevel == 3 ){
			alignment.setExtension( namespaceURI, pName, content );
		    } else if ( parseLevel == 5 ) {
			extensions.setExtension( namespaceURI, pName, content );
		    } else 
			logger.warn( "("+parseLevel+") Unknown element name : {}", pName );
		    //throw new SAXException("[XMLParser] Unknown element name : "+pName);
		};
	    } catch ( AlignmentException e ) { throw new SAXException("[XMLParser] Exception raised", e); };
	} else if(namespaceURI.equals( Namespace.SOAP_ENV.prefix ) ) {//"http://schemas.xmlsoap.org/soap/envelope/"))  {
	    // Ignore SOAP namespace
	    if ( !pName.equals("Envelope") && !pName.equals("Body") ) {
		throw new SAXException("[XMLParser] unknown element name: "+pName); };
	} else if(namespaceURI.equals( Namespace.RDF.prefix ) ) {//"http://www.w3.org/1999/02/22-rdf-syntax-ns#"))  {
	    if ( !pName.equals("RDF") ) {
		throw new SAXException("[XMLParser] unknown element name: "+pName); };
	} else if (namespaceURI.equals( Namespace.EDOAL.prefix )) { 
	    throw new SAXException("[XMLParser] EDOAL alignment must have type EDOAL: "+pName);
	} else {
	    if ( parseLevel == 3 && alignLevel != -1 ){
		alignment.setExtension( namespaceURI, pName, content );
	    } else if ( parseLevel == 5 && alignLevel != -1 ) {
		if ( extensions == null ) extensions = new Extensions();
		extensions.setExtension( namespaceURI, pName, content );
	    } else if (  !embedded ) throw new SAXException("[XMLParser] Unknown namespace : "+namespaceURI);
	}
	content = null; // set it for the character patch
	parseLevel--;
    } //end endElement
    
    /** Can be used for loading the ontology if it is not available **/
 
}//end class
    
