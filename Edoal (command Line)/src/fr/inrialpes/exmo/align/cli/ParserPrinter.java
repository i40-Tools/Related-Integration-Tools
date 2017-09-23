/*
 * $Id: ParserPrinter.java 2065 2015-10-02 09:49:17Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2004, 2007-2008, 2011-2015
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

import java.lang.Double;
import java.util.Properties;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.AlignmentException;

import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;

/** A really simple utility that loads and alignment and prints it.
 * A basic class for ontology alignment processing.
 *   Command synopsis is as follows:
 *   
 *   <pre>
 *   java fr.inrialpes.exmo.align.util.ParserPrinter [options] input [output]
 *   </pre>
 *
 *   where the options are:
 *   <pre>
 *	--renderer=className -r className  Use the given class for output.
 *	--parser=className -p className  Use the given class for input.
 *      --inverse -i              Inverse first and second ontology
 *	--threshold=threshold -t threshold      Trim the alugnment with regard to threshold
 *	--cutmethod=hard|perc|prop|best|span -T hard|perc|prop|best|span      Method to use for triming
 *      --output=filename -o filename Output the alignment in filename
 *      --help -h                       Print this message
 *   </pre>
 *
 *   The <CODE>input</CODE> is a filename. If output is
 *   requested (<CODE>-o</CODE> flags), then output will be written to
 *   <CODE>output</CODE> if present, stdout by default.
 *
 * <pre>
 * $Id: ParserPrinter.java 2065 2015-10-02 09:49:17Z euzenat $
 * </pre>
 *
 */

public class ParserPrinter extends CommonCLI {
    final static Logger logger = LoggerFactory.getLogger( ParserPrinter.class );

    public ParserPrinter() {
	super();
	options.addOption( createOption( "i", "inverse", "Inverse first and second ontology" ) );
	options.addOption( createOption( "e", "embedded", "Read the alignment as embedded in a XML file" ) );
	options.addOption( createRequiredOption( "r", "renderer", "Use the given CLASS for rendering", "CLASS" ) );
	options.addOption( createRequiredOption( "p", "parser", "Use the given CLASS for parsing", "CLASS" ) );
	options.addOption( createRequiredOption( "t", "threshold", "Trim the alignment with regard to threshold", "DOUBLE" ) );
	options.addOption( createRequiredOption( "T", "cutmethod", "METHOD to use for triming (hard|perc|prop|best|span)", "METHOD" ) );
	options.addOption( createRequiredOption( "f", "format", "Display MEASure (prof; default: f)", "MEAS (prof)" ) );
	options.addOption( createRequiredOption( "w", "outputDir", "Split the output in a DIRectory (for SPARQL)", "DIR" ) );
    }

    public static void main(String[] args) {
	try { new ParserPrinter().run( args ); }
	catch ( Exception ex ) { ex.printStackTrace(); };
    }

    public void run(String[] args) throws Exception {
	Alignment result = null;
	String initName = null;
	String dirName = null;
	PrintWriter writer = null;
	AlignmentVisitor renderer = null;
	String rendererClass = null;
	String parserClass = null;
	boolean inverse = false;	
	boolean embedded = false;	
	double threshold = 0;
	String cutMethod = "hard";

	try { 
	    CommandLine line = parseCommandLine( args );
	    if ( line == null ) return; // --help

	    // Here deal with command specific arguments	    
	    if ( line.hasOption( 'i' ) ) inverse = true;
	    if ( line.hasOption( 'e' ) ) embedded = true;
	    if ( line.hasOption( 'c' ) ) dirName = line.getOptionValue( 'c' );
	    if ( line.hasOption( 'r' ) ) rendererClass = line.getOptionValue( 'r' );
	    if ( line.hasOption( 'p' ) ) parserClass = line.getOptionValue( 'p' );
	    if ( line.hasOption( 't' ) ) threshold = Double.parseDouble(line.getOptionValue( 't' ));
	    if ( line.hasOption( 'T' ) ) cutMethod = line.getOptionValue( 'T' );
	    String[] argList = line.getArgs();
	    if ( argList.length > 0 ) {
		initName = argList[0];
	    } else {
		logger.error("Require the alignement filename");
		usage();
		System.exit(-1);
	    }
	} catch( ParseException exp ) {
	    logger.error( exp.getMessage() );
	    usage();
	    System.exit(-1);
	}

	logger.trace("Filename: {}", initName);

	try {
	    // Create parser
	    AlignmentParser aparser = null;
	    if ( parserClass == null ) aparser = new AlignmentParser();
	    else {
		try {
		    Class<?>[] cparams = {};
		    Constructor<?> parserConstructor =
			Class.forName(parserClass).getConstructor(cparams);
		    Object[] mparams = {};
		    aparser = (AlignmentParser) parserConstructor.newInstance(mparams);
		} catch (Exception ex) {
		    logger.error("Cannot create parser {}", parserClass );
		    usage();
		    return;
		}
	    }

	    aparser.setEmbedded( embedded );
	    result = aparser.parse( initName );
	    logger.debug(" Alignment structure parsed");
	    // Set output file
	    OutputStream stream;
	    if ( outputfilename == null ) {
		//writer = (PrintStream) System.out;
		stream = System.out;
	    } else {
		//writer = new PrintStream(new FileOutputStream(outputfilename));
		stream = new FileOutputStream( outputfilename );
	    }
	    if ( dirName != null ) {
	    	 File f = new File(dirName);
		 f.mkdir();
		 parameters.setProperty( "dir", dirName );
		 parameters.setProperty( "split", "true" );
	    }
	    writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	    logger.debug(" Writer ready");

	    if ( inverse ) {
		result = result.inverse();
		logger.debug(" Alignment inverted");
	    }
	    
	    // Thresholding
	    if (threshold != 0) result.cut( cutMethod, threshold );

	    // Create renderer
	    if ( rendererClass == null ) renderer = new RDFRendererVisitor( writer );
	    else {
		try {
		    Class<?>[] cparams = { PrintWriter.class };
		    Constructor<?> rendererConstructor = Class.forName(rendererClass).getConstructor( cparams );
		    Object[] mparams = { (Object)writer };
		    renderer = (AlignmentVisitor) rendererConstructor.newInstance( mparams );
		} catch (Exception ex) {
		    logger.error( "Cannot create renderer {}", rendererClass );
		    usage();
		    return;
		}
	    }
	    logger.debug(" Renderer created");

	    renderer.init( parameters );
	    logger.debug(" Renderer inited");

	    // Render the alignment
	    try {
		result.render( renderer );
		logger.debug(" Renderered");
	    } catch ( AlignmentException aex ) {
		throw aex;
	    } finally {
		writer.flush();
		writer.close();
	    }	    
	    
	} catch (Exception ex) {
	    logger.debug( "IGNORED Exception", ex );
	}
    }

    public void usage() {
	usage( "java "+this.getClass().getName()+" [options] alignfile\nParse the given <alignfile> and prints it" );
    }
}
