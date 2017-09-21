/*
 * $Id: EvalAlign.java 2078 2015-10-15 21:07:19Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2008, 2010-2015
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

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Evaluator;

import fr.inrialpes.exmo.align.parser.AlignmentParser;
import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.lang.Integer;
import java.lang.Double;
import java.util.Properties;

import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/** A really simple utility that loads and alignment and prints it.
    A basic class for an OWL ontology alignment processing. The processor
    will parse ontologies, align them and renderings the resulting alignment.
    Command synopsis is as follows:
    
    <pre>
    java fr.inrialpes.exmo.align.util.EvalAlign [options] input [output]
    </pre>

    where the options are:
    <pre>
        --alignment=filename -a filename Start from an XML alignment file
        --debug[=n] -d [n]              Report debug info at level n,
        --output=filename -o filename Output the alignment in filename
        --help -h                       Print this message
    </pre>

    The <CODE>input</CODE> is a filename. If output is
    requested (<CODE>-o</CODE> flags), then output will be written to
    <CODE>output</CODE> if present, stdout by default.

<pre>
$Id: EvalAlign.java 2078 2015-10-15 21:07:19Z euzenat $
</pre>

@author Jérôme Euzenat
    */

public class EvalAlign extends CommonCLI {
    final static Logger logger = LoggerFactory.getLogger( EvalAlign.class );

    public EvalAlign() {
	super();
	options.addOption( createRequiredOption( "i", "impl", "Use the given CLASS for evaluator", "CLASS" ) );
	// .withType(Class.class)
    }

    public static void main(String[] args) {
	try { new EvalAlign().run( args ); }
	catch ( Exception ex ) { ex.printStackTrace(); };
    }

    public void run(String[] args) throws Exception {
	Evaluator eval = null;
	String alignName1 = null;
	String alignName2 = null;
	PrintWriter writer = null;
	CommandLine line = null;
	Class<?> evaluatorClass = null;

	try { 
	    line = parseCommandLine( args );
	    if ( line == null ) return; // --help
	} catch( ParseException exp ) {
	    logger.error( "Cannot parse command line", exp );
	    usage();
	    System.exit(-1);
	}
	if ( line.hasOption( 'i' ) ) evaluatorClass = Class.forName( line.getOptionValue( 'i' ) );
	String[] argList = line.getArgs();
	if ( argList.length > 1 ) {
	    alignName1 = argList[0];
	    alignName2 = argList[1];
	} else {
	    logger.error( "Require the alignment URIs" );
	    usage();
	    System.exit(-1);
	}

	logger.debug(" Filename: {} --- {}", alignName1, alignName2);

	Alignment align1 = null, align2 = null;
	try {
	    // Load alignments
	    AlignmentParser aparser = new AlignmentParser();
	    align1 = aparser.parse( alignName1 );
	    //logger.trace(" Alignment structure1 parsed");
	    aparser.initAlignment( null );
	    align2 = aparser.parse( alignName2 );
	    //logger.trace(" Alignment structure2 parsed");
	} catch ( Exception ex ) { 
	    ex.printStackTrace(); 
	}

	boolean totry = true; // JE2013: This should not be necessary anymore
	while ( totry ) {
	    totry = false;
	    if ( evaluatorClass != null ) {
		// Create evaluator object
		try {
		    Class<?>[] cparams = { Alignment.class, Alignment.class };
		    Constructor<?> evaluatorConstructor = evaluatorClass.getConstructor(cparams);
		    Object[] mparams = { align1, align2};
		    eval = (Evaluator)evaluatorConstructor.newInstance(mparams);
		} catch (InstantiationException ex) {
		    logger.debug( "IGNORED Exception", ex );
		} catch (InvocationTargetException ex) {
		    logger.debug( "IGNORED Exception", ex );
		} catch (IllegalAccessException ex) {
		    logger.debug( "IGNORED Exception", ex );
		} catch (NoSuchMethodException ex) {
		    logger.error( "No such method: {}", evaluatorClass );
		    usage();
		    throw( ex );
		}
	    } else { eval = new PRecEvaluator( align1, align2 ); };

	    // Compare
	    try {
		eval.eval( parameters ) ;
	    } catch ( AlignmentException aex ) {
		if ( align1 instanceof ObjectAlignment ) {
		    throw aex;
		} else {
		    try {
			align1 = ObjectAlignment.toObjectAlignment( (URIAlignment)align1 );
			align2 = ObjectAlignment.toObjectAlignment( (URIAlignment)align2 );
			totry = true;
		    } catch ( AlignmentException aaex ) { throw aex; }
		}
	    }
	}

	
	// Set output file
	try {
	    OutputStream stream;
	    if ( outputfilename == null ) {
		stream = System.out;
	    } else {
		stream = new FileOutputStream( outputfilename );
	    }
	    writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( stream, "UTF-8" )), true);
	    eval.write( writer );
	} catch ( IOException ex ) {
	    logger.debug( "IGNORED Exception", ex );
	} finally {
	    writer.flush();
	    writer.close();
	}
    }

    public void usage() {
	usage( "java "+this.getClass().getName()+" [options] refalignURI alignURI\nEvaluate two alignments identified by <alignURI>" );
    }
}
