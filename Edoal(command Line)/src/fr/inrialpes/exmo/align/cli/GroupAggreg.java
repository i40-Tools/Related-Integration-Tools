/*
 * $Id: GroupAggreg.java 2150 2017-07-18 15:15:46Z euzenat $
 *
 * Copyright (C) 2003-2014, 2017 INRIA
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

/* This program evaluates the results of several ontology aligners in a row.
*/
package fr.inrialpes.exmo.align.cli;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Evaluator;
import org.semanticweb.owl.align.AlignmentVisitor;

import fr.inrialpes.exmo.align.parser.AlignmentParser;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;

import fr.inrialpes.exmo.ontowrap.OntologyFactory;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.lang.Integer;
import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Properties;

import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

/**
 * A basic class for aggregating a set of alignments provided by different algorithms.
 * The output is an alignment per pair of ontologies obtained by aggregation with the
 * given method.
 *  
 *  <pre>
 *  java -cp procalign.jar fr.inrialpes.exmo.align.util.GroupAggreg [options]
 *  </pre>
 *
 *  where the options are:
 *  <pre>
 * -l,--list &lt;FILE&gt;          List of FILEs to be included in the results
 *                           (required)
 * -m,--aggmethod &lt;METHOD&gt;   Method to use for aggregating (min|max|avg|pool)
 * -P,--params &lt;FILE&gt;        Read parameters from FILE
 * -T,--cutmethod &lt;METHOD&gt;   Method to use for triming (hard|perc|prop|best|span)
 * -t,--threshold &lt;DOUBLE&gt;   Trim the alignment with regard to threshold
 * -w,--workDir &lt;DIR&gt;      The DIRectory containing the data to evaluate
 * </pre>
 *
 * The input is taken in the current directory in a set of subdirectories (one per
 * test which will be rendered by a line) each directory contains a number of
 * alignment files (one per algorithms which will be renderer as a column).
 *
 *  If output is requested (<CODE>-o</CODE> flags), then output will be written to
 *  <CODE>output</CODE> if present, stdout by default.
 *
 * <pre>
 * $Id: GroupAggreg.java 2150 2017-07-18 15:15:46Z euzenat $
 * </pre>
 *
 */

public class GroupAggreg extends CommonCLI {
    final static Logger logger = LoggerFactory.getLogger( GroupAggreg.class );

    String[] listAlgo = null;
    int size = 0;
    String srcDir = null;
    String dirName = ".";
    double threshold = 0.;
    String cutMethod = "hard";
    String aggMethod = "min";


    /* OPTIONS */

    public GroupAggreg() {
	super();
	options.addOption( createListOption( "l", "list", "List of FILEs to be included in the results (required)", "FILE", ',' ) );
	options.addOption( createRequiredOption( "m", "aggmethod", "METHOD to use for aggregating (min|max|avg|pool; default: "+aggMethod+")", "METHOD" ) );
	options.addOption( createRequiredOption( "t", "threshold", "Trim the alignment with regard to threshold (default: "+threshold+")", "DOUBLE" ) );
	options.addOption( createRequiredOption( "T", "cutmethod", "METHOD to use for triming (hard|perc|prop|best|span; default: "+cutMethod+")", "METHOD" ) );
	options.addOption( createRequiredOption( "o", "outputDir", "The DIRectory where to output results", "DIR" ) );
	options.addOption( createRequiredOption( "w", "directory", "The DIRectory containing the data to evaluate", "DIR" ) );
    }

    public static void main(String[] args) {
	try { new GroupAggreg().run( args ); }
	catch (Exception ex) { ex.printStackTrace(); };
    }

    public void run(String[] args) throws Exception {
	try { 
	    CommandLine line = parseCommandLine( args );
	    if ( line == null ) return; // --help

	    // Here deal with command specific arguments
	    if ( line.hasOption( 't' ) ) threshold = Double.parseDouble(line.getOptionValue( 't' ));
	    if ( line.hasOption( 'T' ) ) cutMethod = line.getOptionValue( 'T' );
	    if ( line.hasOption( 'm' ) ) aggMethod = line.getOptionValue( 'm' );

	    if ( line.hasOption( 'l' ) ) {
		listAlgo = line.getOptionValues( 'l' );
		size = listAlgo.length;
	    }
	    if ( line.hasOption( 'w' ) ) srcDir = line.getOptionValue( 'w' );
	    if ( line.hasOption( 'o' ) ) dirName = line.getOptionValue( 'o' );
	} catch( ParseException exp ) {
	    logger.error( exp.getMessage() );
	    usage();
	    System.exit( -1 );
	}

	// check that dirName exist and is writable
	File outDir = new File( dirName );
	if ( !outDir.isDirectory() || !outDir.canWrite() ) {
	    logger.error( "Directory {} must exist and be writable", dirName );
	    throw new AlignmentException( "Cannot output to "+dirName );
	}

	// Run it
	iterateDirectories();
    }

    /**
     * Each directory contains various alignments between the same ontologies
     */
    public void iterateDirectories (){
	File [] subdir = null;
	try {
	    if (srcDir == null) {
		subdir = ( new File(System.getProperty("user.dir") ) ).listFiles(); 
	    } else {
		subdir = ( new File(srcDir) ).listFiles();
	    }
	} catch ( Exception e ) {
	    logger.error( "Cannot stat dir ", e );
	    usage();
	}
	int size = subdir.length;
        Arrays.sort(subdir);
	int i = 0;
	for ( int j=0 ; j < size; j++ ) {
	    if ( subdir[j].isDirectory() ) iterateAlignments( subdir[j] );
	}
    }

    public void iterateAlignments ( File dir ) {
	String prefix = dir.toURI().toString()+File.separator;
	Set<BasicAlignment> listal = new HashSet<BasicAlignment>();
	// for all alignments there,
	for ( String m: listAlgo ) {
	    BasicAlignment al = loadAlignment( prefix+m+".rdf" );
	    if ( al != null ) listal.add( al );
	}
	Alignment result = null;
	try {
	    // Depends on the method
	    result = BasicAlignment.aggregate( aggMethod, listal );
	} catch ( AlignmentException alex ) {
	    logger.debug( "IGNORED: cannot aggregate for {}", prefix, alex );
	    return;
	}
	// Thresholding
	try {
	    if (threshold != 0) result.cut( cutMethod, threshold );
	} catch ( AlignmentException alex ) {
	    logger.debug( "IGNORED: Cannot trim alignment {} {}", cutMethod, threshold, alex );
	}
	// This should be printed now
	print( result, dir.getName()+".rdf" );
    }

    public BasicAlignment loadAlignment( String alignName ) {
	try {
	    // Load alignments
	    AlignmentParser aparser = new AlignmentParser();
	    return (BasicAlignment)aparser.parse( alignName );
	} catch (Exception ex) {
	    logger.debug( "IGNORED Exception", ex );
	};
	return null;
    }

    /**
     * Print the aggregated alignment, it is not void...
     * @param al: the alignment to be printed
     * @param outputfilename: the name of the file in which to print it
     */
    public void print( Alignment al, String outputfilename ) {
	if ( al == null ) return;
	PrintWriter writer = null;
	try {
	    writer = new PrintWriter (
		  new BufferedWriter(
		       new OutputStreamWriter( new FileOutputStream( dirName+File.separator+outputfilename ), "UTF-8" )), true);
	    // Create renderer
	    AlignmentVisitor renderer = new RDFRendererVisitor( writer );
	    renderer.init( parameters );
	    // Render the alignment
	    al.render( renderer );
	} catch ( FileNotFoundException fnfex ) {
	    logger.error( "Cannot print into file {}", outputfilename, fnfex );
	} catch ( UnsupportedEncodingException ueex ) {
	    logger.error( "Do not support UTF-8 encoding (should never happen) {}", outputfilename, ueex );
	} catch ( AlignmentException alex ) {
	    logger.error( "Cannot render alignment {}", al, alex );
	} finally {
	    writer.flush();
	    writer.close();
	}	    
    }

    public void usage() {
	usage( "java "+this.getClass().getName()+" [options]\nEvaluates in parallel several matching results on several tests in subdirectories" );
    }


}

