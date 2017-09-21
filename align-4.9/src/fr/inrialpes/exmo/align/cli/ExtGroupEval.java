/*
 * $Id: ExtGroupEval.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) 2003-2005, 2007-2014 INRIA
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

/* This program evaluates the results of several ontology aligners in a row.
   It uses the generalisations of precision and recall described in
   [Ehrig & Euzenat 2005].
*/
package fr.inrialpes.exmo.align.cli;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Evaluator;

import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.eval.ExtPREvaluator;
import fr.inrialpes.exmo.align.parser.AlignmentParser;

import fr.inrialpes.exmo.ontowrap.OntologyFactory;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.lang.Integer;
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

/** A basic class for synthesizing the results of a set of alignments provided
    by different algorithms. The output is a table showing various generalisations
    of precision and recall for each test and for each algorithm.
    Average is also computed as Harmonic means.
    
    <pre>
    java -cp procalign.jar fr.inrialpes.exmo.align.util.ExtGroupEval [options]
    </pre>

    where the options are:
    <pre>
    -o filename --output=filename
    -f format = sepr (symetric/effort-based/precision-oriented/recall-oriented) --format=sepr
    -r filename --reference=filename
    -s algo/measure
    -l list of compared algorithms
    -t output --type=output: xml/tex/html/ascii
   </pre>

   The input is taken in the current directory in a set of subdirectories
   (one per test which will be rendered by a line) each directory contains
   a number of alignment files (one per algorithms which will be renderer
   as a column).

    If output is requested (<CODE>-o</CODE> flags), then output will be
    written to <CODE>output</CODE> if present, stdout by default.

<pre>
$Id: ExtGroupEval.java 2102 2015-11-29 10:29:56Z euzenat $
</pre>

    */

public class ExtGroupEval extends CommonCLI {
    final static Logger logger = LoggerFactory.getLogger( ExtGroupEval.class );

    String reference = "refalign.rdf";
    String format = "s";
    int fsize = 2;
    String type = "html";
    boolean embedded = false;
    //String dominant = "s";
    String[] listAlgo = null;
    int size = 0;
    String color = null;
    String ontoDir = null;

    public ExtGroupEval() {
	super();
	options.addOption( createListOption( "l", "list", "List of FILEs to be included in the results (required)", "FILE", ',' ) );
	options.addOption( createOptionalOption( "c", "color", "Color even lines of the output in COLOR (default: lightblue)", "COLOR" ) );
	options.addOption( createRequiredOption( "f", "format", "Extended MEASures and order (symetric/effort-based/precision-oriented/recall-oriented)  (default: "+format+")", "MEAS (sepr)" ) );
	//options.addOption( createRequiredOption( "t", "type", "Output TYPE (html|xml|tex|ascii|triangle; default: "+type+")", "TYPE" ) );
	options.addOption( createRequiredOption( "t", "type", "Output TYPE (only html available so far)", "TYPE" ) );
	//options.addOption( createRequiredOption( "s", "sup", "Are dominant columns algorithms or measure (default: s)", "ALGO" ) );
	options.addOption( createRequiredOption( "r", "reference", "Name of the reference alignment FILE (default: "+reference+")", "FILE" ) );
	options.addOption( createRequiredOption( "w", "directory", "The DIRectory containing the data to evaluate", "DIR" ) );
    }

    public static void main(String[] args) {
	try { new ExtGroupEval().run( args ); }
	catch (Exception ex) { ex.printStackTrace(); };
    }

    public void run(String[] args) throws Exception {
	try { 
	    CommandLine line = parseCommandLine( args );
	    if ( line == null ) return; // --help

	    // Here deal with command specific arguments
	    if ( line.hasOption( 'f' ) ) format = line.getOptionValue( 'f' );
	    if ( line.hasOption( 'r' ) ) reference = line.getOptionValue( 'r' );
	    //if ( line.hasOption( 's' ) ) dominant = line.getOptionValue( 's' );
	    if ( line.hasOption( 't' ) ) type = line.getOptionValue( 't' );
	    if ( line.hasOption( 'c' ) ) color = line.getOptionValue( 'c', "lightblue" );
	    if ( line.hasOption( 'l' ) ) {
		listAlgo = line.getOptionValues( 'l' );
		size = listAlgo.length;
	    }
	    if ( line.hasOption( 'w' ) ) ontoDir = line.getOptionValue( 'w' );
	} catch( ParseException exp ) {
	    logger.error( exp.getMessage() );
	    usage();
	    System.exit( -1 );
	}

	print( iterateDirectories() );
    }

    public Vector<Vector<Object>> iterateDirectories (){
	Vector<Vector<Object>> result = null;
	File [] subdir = null;
	try {
	    if (ontoDir == null) {
		subdir = (new File(System.getProperty("user.dir"))).listFiles(); 
	    } else {
		subdir = (new File(ontoDir)).listFiles();
	    }
	} catch (Exception e) {
	    logger.error("Cannot stat dir", e);
	    usage();
	}
	int size = subdir.length;
        Arrays.sort(subdir);
	result = new Vector<Vector<Object>>(size);
	int i = 0;
	for ( int j=0 ; j < size; j++ ) {
	    if( subdir[j].isDirectory() ) {
		//logger.trace("Entering directory {}", subdir[j]);
		// eval the alignments in a subdirectory
		// store the result
		Vector<Object> vect = iterateAlignments( subdir[j] );
		if ( vect != null ){
		    result.add(i, vect);
		    i++;
		}
	    }
	}
	return result;
    }

    public Vector<Object> iterateAlignments ( File dir ) {
	String prefix = dir.toURI().toString()+"/";
	Vector<Object> result = new Vector<Object>();
	boolean ok = false;
	result.add(0,(Object)dir.getName().toString());
	int i = 0;
	// for all alignments there,
	for ( String m : listAlgo ) {
	    i++;
	    // call eval
	    // store the result in a record
	    // return the record.
	    //logger.trace("  Considering result {}", i);
	    Evaluator evaluator = eval( prefix+reference, prefix+m+".rdf");
	    if ( evaluator != null ) ok = true;
	    result.add( i, evaluator );
	}
	// Unload the ontologies.
	try {
	    OntologyFactory.clear();
	} catch ( OntowrapException owex ) {
	    logger.debug( "INGORED Exception", owex );
	}

	if ( ok == true ) return result;
	else return null;
    }

    public Evaluator eval( String alignName1, String alignName2 ) {
	Evaluator eval = null;
	try {
	    // Load alignments
	    AlignmentParser aparser = new AlignmentParser();
	    Alignment align1 = aparser.parse( alignName1 );
	    //logger.trace(" Alignment structure1 parsed");
	    aparser.initAlignment( null );
	    Alignment align2 = aparser.parse( alignName2 );
	    //logger.trace(" Alignment structure2 parsed");
	    // Create evaluator object
	    eval = new ExtPREvaluator(ObjectAlignment.toObjectAlignment( (URIAlignment)align1 ), 
				      ObjectAlignment.toObjectAlignment( (URIAlignment)align2 ) );
	    // Compare
	    eval.eval( parameters ) ;
	} catch (Exception ex) {
	    logger.debug( "IGNORED Extension", ex );
	};
	return eval;
    }

    /**
     * This does not only print the results but compute the average as well
     *
     * @param result: the vector of vector result to be printed
     */
    public void print( Vector<Vector<Object>> result ) {
	PrintStream writer = null;
	try {
	    if ( outputfilename == null ) {
		writer = System.out;
	    } else {
		writer = new PrintStream( new FileOutputStream( outputfilename ) );
	    }

	    printHTML( result, writer );
	} catch (Exception ex) {
	    logger.debug( "IGNORED Exception", ex );
	} finally {
	    writer.close();
	}
    }

    public void printHTML( Vector<Vector<Object>> result, PrintStream writer ) {
	// variables for computing iterative harmonic means
	int expected = 0; // expected so far
	int foundVect[]; // found so far
	double symVect[]; // symmetric similarity
	double effVect[]; // effort-based similarity
	double precOrVect[]; // precision-oriented similarity
	double recOrVect[]; // recall-oriented similarity

	fsize = format.length();
	try {
	    Formatter formatter = new Formatter( writer );
	    // Print the header
	    writer.println("<html><head></head><body>");
	    writer.println("<table border='2' frame='sides' rules='groups'>");
	    writer.println("<colgroup align='center' />");
	    // for each algo <td spancol='2'>name</td>
	    for ( String m : listAlgo ) {
		writer.println("<colgroup align='center' span='"+2*fsize+"' />");
	    }
	    // For each file do a
	    writer.println("<thead valign='top'><tr><th>algo</th>");
	    // for each algo <td spancol='2'>name</td>
	    for ( String m : listAlgo ) {
		writer.println("<th colspan='"+((2*fsize))+"'>"+m+"</th>");
	    }
	    writer.println("</tr></thead><tbody><tr><td>test</td>");
	    // for each algo <td>Prec.</td><td>Rec.</td>
	    for ( String m : listAlgo ) {
		for ( int i = 0; i < fsize; i++){
		    if ( format.charAt(i) == 's' ) {
			writer.println("<td colspan='2'><center>Symmetric</center></td>");
		    } else if ( format.charAt(i) == 'e' ) {
			writer.println("<td colspan='2'><center>Effort</center></td>");
		    } else if ( format.charAt(i) == 'p' ) {
			writer.println("<td colspan='2'><center>Prec. orient.</center></td>");
		    } else if ( format.charAt(i) == 'r' ) {
			writer.println("<td colspan='2'><center>Rec. orient.</center></td>");
		    }
		}
		//writer.println("<td>Prec.</td><td>Rec.</td>");
	    }
	    writer.println("</tr></tbody><tbody>");
	    foundVect = new int[ size ];
	    symVect = new double[ size ];
	    effVect = new double[ size ];
	    precOrVect = new double[ size ];
	    recOrVect = new double[ size ];
	    for( int k = size-1; k >= 0; k-- ) {
		foundVect[k] = 0;
		symVect[k] = 0.;
		effVect[k] = 0.;
		precOrVect[k] = 0.;
		recOrVect[k] = 0.;
	    }
	    // </tr>
	    // For each directory <tr>
	    boolean colored = false;
	    for ( Vector<Object> test : result ) {
		int nexpected = -1;
		if ( colored == true && color != null ){
		    colored = false;
		    writer.println("<tr bgcolor=\""+color+"\">");
		} else {
		    colored = true;
		    writer.println("<tr>");
		};
		// Print the directory <td>bla</td>
		writer.println("<td>"+(String)test.get(0)+"</td>");
		// For each record print the values <td>bla</td>
		Enumeration<Object> f = test.elements();
		f.nextElement();
		for( int k = 0 ; f.hasMoreElements() ; k++) {
		    ExtPREvaluator eval = (ExtPREvaluator)f.nextElement();
		    if ( eval != null ){
			// iterative H-means computation
			if ( nexpected == -1 ){
			    nexpected = eval.getExpected();
			    expected += nexpected;
			}
			// If foundVect is -1 then results are invalid
			if ( foundVect[k] != -1 ) foundVect[k] += eval.getFound();
			for ( int i = 0 ; i < fsize; i++){
			    writer.print("<td>");
			    if ( format.charAt(i) == 's' ) {
				formatter.format("%1.2f", eval.getSymPrecision());
				writer.print("</td><td>");
				formatter.format("%1.2f", eval.getSymRecall());
				symVect[k] += eval.getSymSimilarity();
			    } else if ( format.charAt(i) == 'e' ) {
				formatter.format("%1.2f", eval.getEffPrecision());
				writer.print("</td><td>");
				formatter.format("%1.2f", eval.getEffRecall());
				effVect[k] += eval.getEffSimilarity();
			    } else if ( format.charAt(i) == 'p' ) {
				formatter.format("%1.2f", eval.getPrecisionOrientedPrecision());
				writer.print("</td><td>");
				formatter.format("%1.2f", eval.getPrecisionOrientedRecall());
				precOrVect[k] += eval.getPrecisionOrientedSimilarity();
			    } else if ( format.charAt(i) == 'r' ) {
				formatter.format("%1.2f", eval.getRecallOrientedPrecision());
				writer.print("</td><td>");
				formatter.format("%1.2f", eval.getRecallOrientedRecall());
				recOrVect[k] += eval.getRecallOrientedSimilarity();
			    }
			    writer.print("</td>");
			}
		    } else {
			for ( int i = 0 ; i < fsize; i++) writer.print("<td>n/a</td>");
		    }
		}
		writer.println("</tr>");
	    }
	    writer.print("<tr bgcolor=\"yellow\"><td>H-mean</td>");
	    int k = 0;
	    for ( String m : listAlgo ) {
		if ( foundVect[k] != -1 ){
		    for ( int i = 0 ; i < fsize; i++){
			writer.print("<td>");
			if ( format.charAt(i) == 's' ) {
			    formatter.format("%1.2f", symVect[k]/foundVect[k]);
			    writer.print("</td><td>");
			    formatter.format("%1.2f", symVect[k]/expected);
			} else if ( format.charAt(i) == 'e' ) {
			    formatter.format("%1.2f", effVect[k]/foundVect[k]);
			    writer.print("</td><td>");
			    formatter.format("%1.2f", effVect[k]/expected);
			} else if ( format.charAt(i) == 'p' ) {
			    formatter.format("%1.2f", precOrVect[k]/foundVect[k]);
			    writer.print("</td><td>");
			    formatter.format("%1.2f", precOrVect[k]/expected);
			} else if ( format.charAt(i) == 'r' ) {
			    formatter.format("%1.2f", recOrVect[k]/foundVect[k]);
			    writer.print("</td><td>");
			    formatter.format("%1.2f", recOrVect[k]/expected);
			}
			writer.println("</td>");
		    }
		} else {
		    writer.println("<td colspan='2'><center>Error</center></td>");
		}
		//};
		k++;
	    }
	    writer.println("</tr>");
	    writer.println("</tbody></table>");
	    writer.println("<p><small>n/a: result alignment not provided or not readable<br />");
	    writer.println("NaN: division per zero, likely due to empty alignent.</small></p>");
	    writer.println("</body></html>");
	} catch (Exception ex) {
	    logger.debug( "IGNORED Exception", ex );
	} finally {
	    writer.flush();
	    writer.close();
	}
    }

    public void usage() {
	usage( "java "+this.getClass().getName()+" [options]\nEvaluates (with extended evaluators) in parallel several matching results on several tests in subdirectories" );
    }
}
