/*
 * $Id: AlignmentParser.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2005, 2007-2010, 2012-2015
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

//Imported JAVA classes
import java.io.IOException;
import java.io.StringReader;
import java.io.Reader;
import java.io.InputStream;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.lang.Integer;
import java.lang.Double;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.AlignmentException;

import fr.inrialpes.exmo.ontowrap.Ontology;
import fr.inrialpes.exmo.ontowrap.LoadedOntology;
import fr.inrialpes.exmo.ontowrap.BasicOntology;

import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.Annotations;

/**
 * This class allows the creation of a parser for an Alignment file.
 * The class is called by:
 * AlignmentParser parser = new AlignmentParser();
 * Alignment alignment = parser.parse( input );
 * input can be a URI as a String, an InputStream
 * This new version (January 2004) parses the alignment description in
 * RDF/XML/OWL format and RDF format. It understands the EDOAL format.
 *
 */

public class AlignmentParser {
    final static Logger logger = LoggerFactory.getLogger( AlignmentParser.class );

    /**
     * the alignment that is parsed
     * We always create a URIAlignment (we could also use a BasicAlignment).
     * This is a pitty but the idea of creating a particular alignment
     * is not in accordance with using an interface.
     */
    protected Alignment alignment = null;

    /**
     * The parsing level, if equal to 3 we are in the Alignment
     * if equal to 5 we are in a cell
     * and can find metadata
     */
    protected int parseLevel = 0;

    /**
     * The parsing level, if equal to 3 we are in the Alignment
     * if equal to 5 we are in a cell
     * and can find metadata
     */
    protected boolean embedded = false;

    /**
     * The level at which we found the Alignment tag.
     * It is -1 outside the alignment.
     */
    protected int alignLevel = -1;

    /** 
     * Creates a Parser.
     * @param debugMode The value of the debug mode DEPRECATED
     */
    public AlignmentParser( int debugMode ) {}

    public AlignmentParser() {}

    public void setEmbedded( boolean b ){
	embedded = b;
    }

    /** 
     * Parses the document corresponding to the URI given in parameter
     * If the current process has links (import or include) to others documents then they are 
     * parsed.
     *
     * @param uri: URI of the document to parse
     * @param loaded: (cached ontologies)
     * @return the parsed alignment
     * @throws AlignmentException when something goes wrong
     * @deprecated use parse( URI ) instead
     */
    @Deprecated
    public Alignment parse( String uri, Hashtable<?,?> loaded ) throws AlignmentException {
	return parse( uri );
    }

    /** 
     * Parses the document given in parameter
     * If the current process has links (import or include) to others documents then they are 
     * parsed.
     *
     * @param o: a URI, InputStream, String or Reader containing the alignment
     * @return the parsed alignment
     * @throws AlignmentException when something goes wrong
     */
    private Alignment callParser( Object o ) throws AlignmentException {
	try { 
	    XMLParser parser = new XMLParser();
	    if ( embedded ) parser.setEmbedded( embedded );
	    //alignment = parser.parse( o );
	    alignment = callParser( parser, o );
	} catch ( Exception e ) {
	    logger.debug( "XMLParser failed to parse alignment (INFO)" );
	    try {
		if ( !embedded ) {
		    logger.debug( "Using RDFParser instead (log=TRACE to see exception)" );
		    RDFParser rparser = new RDFParser();
		    alignment = callParser( rparser, o );
		} else {
		    logger.debug( "Cannot parse further" );
		    throw new AlignmentException( "Cannot parse "+o+" (use logging for more info)", e );
		}
	    } catch ( Exception ex ) {
		// JE: should contain both ex and e
		logger.debug( "XMLParser throw exception", e );
		logger.debug( "RDFParser throw exception", ex );
		throw new AlignmentException( "Cannot parse "+o, ex );
	    }
	    logger.trace( "This exception was caught and is not critical", e );
	}
	return alignment;
    }

    /**
     * This dispatch is ridiculous, but that's life
     *
     * @param p: a parser
     * @param o: a URI, InputStream, String or Reader containing the alignment
     * @return the parsed alignment
     * @throws AlignmentException when something goes wrong
     */
    private Alignment callParser( XMLParser p, Object o ) throws AlignmentException {
	if ( o instanceof URI ) return p.parse( ((URI)o).toString() );
	if ( o instanceof String ) return p.parse( new ByteArrayInputStream( ((String)o).getBytes() ) );
	if ( o instanceof Reader ) return p.parse((Reader)o);
	if ( o instanceof InputStream ) return p.parse((InputStream)o);
	throw new AlignmentException( "AlignmentParser: XMLParser cannot parse :"+o );
    }

    private Alignment callParser( RDFParser p, Object o ) throws AlignmentException {
	if ( o instanceof URI ) return p.parse( ((URI)o).toString() );
	if ( o instanceof String ) return p.parse( new ByteArrayInputStream( ((String)o).getBytes() ) );
	if ( o instanceof Reader ) return p.parse((Reader)o);
	if ( o instanceof InputStream ) return p.parse((InputStream)o);
	throw new AlignmentException( "AlignmentParser: RDFParser cannot parse :"+o );
    }

    /** 
     * Parses the content of a string
     *
     * @param s: a string containing an alignment
     * @return the parsed alignment
     * @throws AlignmentException when something goes wrong
     */
    public Alignment parseString( String s ) throws AlignmentException {
	// The problem here is that InputStream are consumed by parsers
	// So they must be opened again! Like Readers...
	callParser( s );
	return alignment;
    }

    /** 
     * Parses a the content of a reader
     *
     * @param r the reader to parse
     * @return the parsed alignment
     * @throws AlignmentException when something goes wrong
     */
    public Alignment parse( Reader r ) throws AlignmentException {
	callParser( r );
	return alignment;
    }

    /** 
     * Parses a URI expressed as a String
     *
     * @param uri the URI as a String
     * @return the parsed alignment
     * @throws AlignmentException when something goes wrong
     * This is only here for compatibility purposes
     */
    public Alignment parse( String uri ) throws AlignmentException {
	try {
	    callParser( new URI( uri ) );
	} catch ( URISyntaxException urisex ) {
	    throw new AlignmentException( "Invalid URI : "+uri, urisex );
	}
	return alignment;
    }

    /** 
     * Parses a URI
     *
     * @param uri the URI
     * @return the parsed alignment
     * @throws AlignmentException when something goes wrong
     */
    public Alignment parse( URI uri ) throws AlignmentException {
	callParser( uri );
	return alignment;
    }

    /** 
     * Parses an inputStream
     *
     * @param s the Stream to parse
     * @return the parsed alignment
     * @throws AlignmentException when something goes wrong
     */
    public Alignment parse( InputStream s ) throws AlignmentException {
	callParser( s );
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
    }
    
 
}
    
