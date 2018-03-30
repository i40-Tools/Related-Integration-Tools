/*
 * $Id: TransformQuery.java 2101 2015-11-20 23:16:46Z euzenat $
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

package fr.inrialpes.exmo.align.cli;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;

import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import org.semanticweb.owl.align.AlignmentException;

/**
 * Transform a query according to an alignment
 * {@literal TransformQuery [-a alignmentURI] [-e] [-q query] < query}
 * Query can be taken from (priority):
 * - parameter
 * - a queryfile (-q)
 * - standard input
 */

public class TransformQuery extends CommonCLI {
    final static Logger logger = LoggerFactory.getLogger( ParserPrinter.class );

    public TransformQuery() {
	super();
	options.addOption( createOption( "e", "echo", "Echo the input query" ) );
	//options.addOption( createRequiredOption( "p", "process", "Process the query against a particular CLASS", "CLASS" ) );
	options.addOption( createRequiredOption( "q", "query", "get the query from the corresponding FILE", "FILE" ) );
	options.addOption( createRequiredOption( "a", "alignment", "Use the alignment identified by URI", "URI" ) );
    }

    public static void main(String[] args) {
	try { new TransformQuery().run( args ); }
	catch (Exception ex) { ex.printStackTrace(); };
    }

    public void run(String[] args) throws Exception {
	BasicAlignment al = null;
	String alignmentURL = null;
	String query = null;
	String result = null;
	//String processorClass = null;
	String queryFile = null;
	PrintWriter writer = null;
	boolean echo = false;

	try { 
	    CommandLine line = parseCommandLine( args );
	    if ( line == null ) return; // --help

	    // Here deal with command specific arguments	    
	    //if ( line.hasOption( 'p' ) ) processorClass = line.getOptionValue( 'p' );
	    if ( line.hasOption( 'q' ) ) queryFile = line.getOptionValue( 'q' );
	    if ( line.hasOption( 'a' ) ) alignmentURL = line.getOptionValue( 'a' );
	    if ( line.hasOption( 'e' ) ) echo = true;
	    String[] argList = line.getArgs();
	    if ( argList.length > 0 ) {
		query = argList[0];
	    }
	} catch( ParseException exp ) {
	    logger.error( exp.getMessage() );
	    usage();
	    System.exit(-1);
	}

	try {
	    if ( query == null ) {
		query = "";
		try {
		    InputStream in = (queryFile!=null)?new FileInputStream( queryFile ):System.in;
		    BufferedReader reader = new BufferedReader( new InputStreamReader(in) );
		    String line = null;
		    while ((line = reader.readLine()) != null) {
			query += line + "\n";
		    }
		} catch (IOException x) {
		    System.err.println(x);
		    System.exit( -1 );
		}
	    }

	    if ( echo ) System.out.println( query );

	    // load the alignment
	    if ( alignmentURL != null ) {
		AlignmentParser aparser = new AlignmentParser();
		al = (BasicAlignment)aparser.parse( alignmentURL );

		// Create query processor
		result = al.rewriteQuery( query, parameters );
	    } else {
		result = query;
	    }
	    
	    // Set output file
	    OutputStream stream;
	    if ( outputfilename == null ) {
		stream = System.out;
	    } else {
		stream = new FileOutputStream( outputfilename );
	    }
	    writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);

	    // Print
	    try {
		writer.println( result );
	    } finally {
		writer.flush();
		writer.close();
	    }}
	    catch (Exception ex) {
	    ex.printStackTrace();
	    logger.error( ex.getMessage() );
	    System.exit(-1);
	}
    }

    public void usage() {
	usage( "java "+this.getClass().getName()+" [options] QUERY\nTransforms the given QUERY according to an alignment" );
    }
}
