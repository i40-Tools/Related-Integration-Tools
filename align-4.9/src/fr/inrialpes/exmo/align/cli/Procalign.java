/*
 * $Id: Procalign.java 2065 2015-10-02 09:49:17Z euzenat $
 *
 * Copyright (C) 2003-2008, 2010-2015 INRIA
 * Copyright (C) 2004, Université de Montréal
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

package fr.inrialpes.exmo.align.cli;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.AlignmentVisitor;

import fr.inrialpes.exmo.align.impl.Annotations;
import fr.inrialpes.exmo.align.impl.Namespace;

import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Properties;
import java.lang.Double;
import java.lang.Integer;
import java.lang.Long;
import java.lang.reflect.Constructor;

import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import fr.inrialpes.exmo.align.parser.AlignmentParser;

/** A basic class for an OWL ontology alignment processing. The processor
    will parse ontologies, align them and renderings the resulting alignment.
    Command synopsis is as follows:
    
    <pre>
    java fr.inrialpes.exmo.align.util.Procalign [options] onto1 onto2 [output]
    </pre>

    or better
    <pre>
    java -jar procalign.jar onto1 onto2
    </pre>

    where the options are:
    <pre>
        --alignment=filename -a filename Start from an XML alignment file
	--params=filename -p filename   Read the parameters in file
        --output=filename -o filename Output the alignment in filename
        --impl=className -i classname           Use the given alignment implementation.
        --renderer=className -r className       Specifies the alignment renderer
        --help -h                       Print this message
    </pre>

    <CODE>onto1</CODE> and <CODE>onto2</CODE> should be URLs. If output is
    requested (<CODE>-o</CODE> flags), then output will be written to
    <CODE>output</CODE> if present, stdout by default.

<pre>
$Id: Procalign.java 2065 2015-10-02 09:49:17Z euzenat $
</pre>

@author Jérôme Euzenat
*/

public class Procalign extends CommonCLI {
    final static Logger logger = LoggerFactory.getLogger( Procalign.class );

    public Procalign() {
	super();
	options.addOption( createRequiredOption( "i", "impl", "Use the given CLASS for matcher", "CLASS" ) );
	options.addOption( createRequiredOption( "r", "renderer", "Use the given CLASS for rendering", "CLASS" ) );
	options.addOption( createRequiredOption( "a", "alignment", "Use an initial alignment FILE", "FILE" ) );
	options.addOption( createRequiredOption( "t", "threshold", "Trim the alignment with regard to threshold", "DOUBLE" ) );
	options.addOption( createRequiredOption( "T", "cutmethod", "METHOD to use for triming (hard|perc|prop|best|span)", "METHOD" ) );
    }

    public static void main( String[] args ) {
	try { new Procalign().run( args ); }
	catch ( Exception ex ) {
	    ex.printStackTrace(); 
	    System.exit(-1);
	};
    }

    public Alignment run(String[] args) throws Exception {
	URI onto1 = null;
	URI onto2 = null;
	AlignmentProcess result = null;
	String cutMethod = "hard";
	String initName = null;
	Alignment init = null;
	String alignmentClassName = "fr.inrialpes.exmo.align.impl.method.StringDistAlignment";
	String rendererClass = "fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor";
	PrintWriter writer = null;
	AlignmentVisitor renderer = null;
	double threshold = 0;
	CommandLine line = null;

	try { 
	    line = parseCommandLine( args );
	} catch( ParseException exp ) {
	    logger.error( "Cannot parse command line", exp );
	    usage();
	    exit( -1 );
	}
	if ( line == null ) exit(1); // --help
	if ( line.hasOption( 'r' ) ) rendererClass = line.getOptionValue( 'r' );
	if ( line.hasOption( 'i' ) ) alignmentClassName = line.getOptionValue( 'i' );
	if ( line.hasOption( 'a' ) ) initName = line.getOptionValue( 'a' );
	if ( line.hasOption( 't' ) ) threshold = Double.parseDouble(line.getOptionValue( 't' ));
	if ( line.hasOption( 'T' ) ) cutMethod = line.getOptionValue( 'T' );
	String[] argList = line.getArgs();
	if ( argList.length > 1 ) {
	    try {
		onto1 = new URI( argList[0] );
		onto2 = new URI( argList[1] );
	    } catch( URISyntaxException usex ) {
		logger.error( "Error in ontology URIs", usex );
		usage();
		exit( -1 );
	    }
	} else {
	    logger.error( "Require the ontology URIs" );
	    usage();
	    exit(-1);
	}

	logger.debug( "Ready to match {} and {}", onto1, onto2 );

	try {
	    if (initName != null) {
		AlignmentParser aparser = new AlignmentParser();
		Alignment al = aparser.parse( initName );
		init = al;
		logger.debug("Init parsed");
	    }

	    // Create alignment object
	    Class<?> alignmentClass = Class.forName( alignmentClassName );
	    Class<?>[] cparams = {};
	    Constructor<?> alignmentConstructor = alignmentClass.getConstructor(cparams);
	    Object[] mparams = {};
	    result = (AlignmentProcess)alignmentConstructor.newInstance( mparams );
	    result.init( onto1, onto2 );
	} catch ( Exception ex ) {
	    logger.error( "Cannot create alignment {}", alignmentClassName );
	    usage();
	    throw ex;
	}

	logger.debug("Alignment structure created");

	try {
	    // Compute alignment
	    long time = System.currentTimeMillis();
	    result.align(  init, parameters ); // add opts
	    long newTime = System.currentTimeMillis();
	    result.setExtension( Namespace.EXT.uri, Annotations.TIME, Long.toString(newTime - time) );

	    // Thresholding
	    if (threshold != 0) result.cut( cutMethod, threshold );

	    logger.debug( "Matching performed" );
	    
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

	    // Result printing (to be reimplemented with a default value)
	    try {
		Class<?>[] cparams = { PrintWriter.class };
		Constructor<?> rendererConstructor = Class.forName(rendererClass).getConstructor( cparams );
		    Object[] mparams = { (Object)writer };
		    renderer = (AlignmentVisitor) rendererConstructor.newInstance( mparams );
	    } catch (Exception ex) {
		logger.error( "Cannot create renderer {}", rendererClass );
		usage();
		throw ex;
	    }
	    
	    // Output
	    result.render(renderer);
	    logger.debug( "Output printed" );
	} catch ( Exception ex ) {
	    throw ex;
	} finally {
	    if ( writer != null ) {
		writer.flush();
		writer.close();
	    }
	}
	return result;
    }

    public void usage() {
	usage( "java "+this.getClass().getName()+" [options] ontoURI ontoURI\nMatches the two ontologies identified by <ontoURI>" );
    }
}
