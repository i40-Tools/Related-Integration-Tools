/*
 * $Id: QueryMediator.java 1988 2014-11-14 22:20:28Z euzenat $
 *
 * Copyright (C) INRIA, 2006-2009, 2013-2014
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

/*
 * QueryMediator.java
 *
 * Created on May 20, 2006, 12:15 AM
 *
 */

package fr.inrialpes.exmo.queryprocessor;

import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.parser.AlignmentParser;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Cell;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import java.util.Hashtable;
import java.util.Properties;
import java.io.IOException;

/**
 * QueryMediator
 * 
 * A query processor that mediates queries through an ontology alignment.
 * This implementation rely on an embedded QueryProcessor.
 * Another possible implementation would be to subclass a query processor.
 * This would however provide few benefits (speed gained by no indirection)
 * against loss in generality.
 * 
 * @author Arun Sharma
 * @author Jérôme Euzenat
 */
public class QueryMediator implements QueryProcessor {
    
    private BasicAlignment alignment;
    private QueryProcessor processor;
    
    // May be usefull to prohibit this...
    //public QueryMediator( ) {
    //}
    
    public QueryMediator( QueryProcessor proc, Alignment a ) throws AlignmentException {
	processor = proc;
	if ( a instanceof BasicAlignment ) {
	    alignment = (BasicAlignment)a;
	} else {
	    throw new AlignmentException( "QueryMediator requires BasicAlignments (so far)" );
	}
    }
    
    public QueryMediator( QueryProcessor proc, String alignmentURI ) throws SAXException,ParserConfigurationException,IOException {
	processor = proc;
	AlignmentParser aparser = new AlignmentParser();
	try { alignment = (BasicAlignment)aparser.parse( alignmentURI ); }
	catch ( Exception ex ) {
	    throw new ParserConfigurationException("Error on parsing");
	}
    }

    public QueryMediator( Alignment a ) throws AlignmentException {
	// For this to work we need to generate a processor
	this( (QueryProcessor)null, a );
    }
    
    public QueryMediator( String alignmentURI ) throws SAXException,ParserConfigurationException,IOException {
	// For this to work we need to generate a processor
	this( (QueryProcessor)null, alignmentURI );
    }

    /**
     * @param query -- The query string
     * @param type -- The query type, can be one of SELECT, ASK, CONSTRUCT, or DESCRIBE
     * @return Result, result form depends on type
     */
    // JE: There is a flaw in the query API: it should be defined with
    // throws QueryException because if something fails, this will be
    // done silently. (same for the other).
    public Result query(String query, Type type) {
	try {
	    String newQuery = alignment.rewriteQuery( query );
	    return processor.query( newQuery, type );
	} catch (AlignmentException e) { return (Result)null; }
    }
    
    /**
     *@param query  -- The query string
     */
    public Result query( String query ) {
	try {
	    String newQuery = alignment.rewriteQuery( query );
	    return processor.query( newQuery );
	} catch (AlignmentException e) { return (Result)null; }
    }

    /**
     *@param query -- The query string
     *@return query results as string
     */
    public String queryWithStringResults(String query) {
	try {
	    String newQuery = alignment.rewriteQuery( query );
	    return processor.queryWithStringResults( newQuery );
	} catch (AlignmentException e) { return (String)null; }
    }
    
    /**
     *@param query -- the query string
     *@return the type of the query
     */
    public int getType(String query){
	return processor.getType( query );
    }
    
    public void loadOntology(String uri){
	processor.loadOntology( uri );
    }    
}
