/*
 * $Id: WGroupEval.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) 2003-2014, INRIA
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
*/
package fr.inrialpes.exmo.align.cli;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Evaluator;

import fr.inrialpes.exmo.align.impl.eval.WeightedPREvaluator;

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

import fr.inrialpes.exmo.align.parser.AlignmentParser;

/** A basic class for synthesizing the results of a set of alignments provided by
    different algorithms. The output is a table showing various classical measures
    for each test and for each algorithm. Average is also computed as Harmonic means.
    
    <pre>
    java -cp procalign.jar fr.inrialpes.exmo.align.util.WGroupEval [options]
    </pre>

    where the options are:
    <pre>
    -o filename --output=filename
    -f format = prfot (precision/recall/f-measure/overall/time) --format=prfot
    -r filename --reference=filename
    -s algo/measure
    -l list of compared algorithms
    -t output --type=output: xml/tex/html/ascii
   </pre>

   The input is taken in the current directory in a set of subdirectories (one per
   test which will be rendered by a line) each directory contains a number of
   alignment files (one per algorithms which will be renderer as a column).

    If output is requested (<CODE>-o</CODE> flags), then output will be written to
    <CODE>output</CODE> if present, stdout by default.

<pre>
$Id: WGroupEval.java 2102 2015-11-29 10:29:56Z euzenat $
</pre>

*/

public class WGroupEval extends CommonCLI {
    final static Logger logger = LoggerFactory.getLogger( WGroupEval.class );

    String reference = "refalign.rdf";
    String format = "pr";
    int fsize = 2;
    String type = "html";
    boolean embedded = false;
    String dominant = "s";
    String[] listAlgo = null;
    int size = 0;
    String color = null;
    String ontoDir = null;

    public WGroupEval() {
	super();
	options.addOption( createListOption( "l", "list", "List of FILEs to be included in the results (required)", "FILE", ',' ) );
	options.addOption( createOptionalOption( "c", "color", "Color even lines of the output in COLOR (default: lightblue)", "COLOR" ) );
	//options.addOption( createRequiredOption( "e", "evaluator", "Use CLASS as evaluation plotter", "CLASS" ) );
	options.addOption( createRequiredOption( "f", "format", "Used (weighted) MEASures and order (precision/recall/f-measure/overall/time) (default: "+format+")", "MEAS (prfot)" ) );
	options.addOption( createRequiredOption( "t", "type", "Output TYPE (html|xml|tex|ascii|triangle; default: "+type+")", "TYPE" ) );
	//options.addOption( createRequiredOption( "s", "sup", "Specifies if dominant columns are algorithms or measure (default: s)", "ALGO" ) );
	options.addOption( createRequiredOption( "r", "reference", "Name of the reference alignment FILE (default: "+reference+")", "FILE" ) );
	options.addOption( createRequiredOption( "w", "directory", "The DIRectory containing the data to evaluate", "DIR" ) );
    }


    public static void main(String[] args) {
	try { new WGroupEval().run( args ); }
	catch (Exception ex) { ex.printStackTrace(); };
    }

    public void run(String[] args) throws Exception {

	try { 
	    CommandLine line = parseCommandLine( args );
	    if ( line == null ) return; // --help

	    // Here deal with command specific arguments
	    if ( line.hasOption( 'f' ) ) format = line.getOptionValue( 'f' );
	    if ( line.hasOption( 'r' ) ) reference = line.getOptionValue( 'r' );
	    if ( line.hasOption( 's' ) ) dominant = line.getOptionValue( 's' );
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
	    logger.error("Cannot stat dir ", e);
	    usage();
	    System.exit(-1);
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
	for ( String m: listAlgo ) {
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
	    logger.debug( "IGNORED Exception", owex );
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
	    eval = new WeightedPREvaluator( align1, align2 );
	    // Compare
	    eval.eval( parameters ) ;
	} catch (Exception ex) {
	    logger.debug( "IGNORED Exception", ex );
	};
	return eval;
    }

    /**
     * The writer used to print the result
     */
    PrintStream writer = null;

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

	    if ( type.equals("html") ) printHTML( result, writer );
	    else if ( type.equals("tex") ) printLATEX( result, writer );
	    else if ( type.equals("triangle") ) printTRIANGLE( result, writer );
	} catch (Exception ex) {
	    logger.debug( "IGNORED Exception", ex );
	} finally {
	    writer.close();
	}
    }

    public void printTRIANGLE( Vector<Vector<Object>> result, PrintStream writer ) {
	// variables for computing iterative harmonic means
	double expected = 0.; // expected so far
	double foundVect[]; // found so far
	double correctFoundVect[]; // correct so far
	double correctExpVect[]; // correct so far
	long timeVect[]; // time so far
	foundVect = new double[ size ];
	correctFoundVect = new double[ size ];
	correctExpVect = new double[ size ];
	timeVect = new long[ size ];
	for( int k = size-1; k >= 0; k-- ) {
	    foundVect[k] = 0.;
	    correctFoundVect[k] = 0.;
	    correctExpVect[k] = 0.;
	    timeVect[k] = 0;
	}
	for ( Vector<Object> test : result ) {
	    double newexpected = -1.;
	    Enumeration<Object> f = test.elements();
	    // Too bad the first element must be skipped
	    f.nextElement();
	    for( int k = 0 ; f.hasMoreElements() ; k++) {
		WeightedPREvaluator eval = (WeightedPREvaluator)f.nextElement();
		if ( eval != null ){
		    // iterative H-means computation
		    if ( newexpected == -1. ){
			newexpected = eval.getExpected();
			expected += newexpected;
		    }
		    foundVect[k] += eval.getFound();
		    correctFoundVect[k] += eval.getCorrectFound();
		    correctExpVect[k] += eval.getCorrectExpected();
		    timeVect[k] += eval.getTime();
		} else {
		    correctExpVect[k] += newexpected;
		}
	    }
	}
	writer.println("\\documentclass[11pt]{book}");
	writer.println();
	writer.println("\\usepackage{pgf}");
	writer.println("\\usepackage{tikz}");
	writer.println();
	writer.println("\\begin{document}");
	writer.println("\\date{today}");
	writer.println("");
	writer.println("\n%% Plot generated by GenPlot of alignapi");
	writer.println("\\begin{tikzpicture}[cap=round]");
	writer.println("% Draw grid");
	writer.println("\\draw[step=1cm,very thin,color=gray] (-0.2,-0.2) grid (10.0,9.0);");
	writer.println("\\draw[|-|] (-0,0) -- (10,0);");
	writer.println("%\\draw[dashed,very thin] (0,0) -- (5,8.66) -- (10,0);");
	writer.println("\\draw[dashed,very thin] (10,0) arc (0:60:10cm);");
	writer.println("\\draw[dashed,very thin] (0,0) arc (180:120:10cm);");

	writer.println("\\draw (0,-0.3) node {$recall$}; ");
	writer.println("\\draw (10,-0.3) node {$precision$}; ");
	//writer.println("\\draw (0,-0.3) node {0.}; ");
	//writer.println("\\draw (10,-0.3) node {1.}; ");
	writer.println("% Plots");
	int k = 0;
	for ( String m: listAlgo ) {
	    double precision = 1. - correctFoundVect[k]/foundVect[k];
	    double recall = 1. - correctExpVect[k]/expected;
	    double prec2 = precision*precision;
	    double a = ((prec2-(recall*recall)+1)/2);
	    double b = java.lang.Math.sqrt( prec2 - (a*a) );
	    a = a*10; b = b*10; //for printing scale 10.
	    writer.println("\\draw plot[mark=+,] coordinates {("+a+","+b+")};");
	    writer.println("\\draw ("+(a+.01)+","+(b+.01)+") node[anchor=south west] {"+m+"};");
	    k++;
	}
	writer.println("\\end{tikzpicture}");
	writer.println();
	writer.println("\\end{document}");
    }

    public void printLATEX( Vector<Vector<Object>> result, PrintStream writer ) {
    }

    /* A few comments on how and why computing "weighted harmonic means"
       (Jérôme Euzenat)

Let Ai be the found alignment for test i, let Ri be the reference alignment for test i.
Let |A| be the size of A, i.e., the number of correspondences.

Let P(Ri,Ai) and R(Ri,Ai) being precision and recall respectively.

Arithmetic means is \Sum{i=1}{n} P(Ri,Ai) / n and \Sum{i=1}{n} R(Ri,Ai) / n.

Weighted harmonic means is

\Sum{i=1}{n} Wi / \Sum{i=1}{n} (Wi/P(Ri,Ai))
and
\Sum{i=1}{n} Wi / \Sum{i=1}{n} (Wi/R(Ri,Ai))

The goal of using it is that the result be the Precision and Recall of all tests (and not the average precision and recall).

If we take Wi = |Ai\cap Ri|
Then we have exactly this result:

\Sum{i=1}{n} Wi / \Sum{i=1}{n} (Wi/P(Ri,Ai))
                           = P( \cup{i=1}{n} Ri, \cup{i=1}{n} Ai )
(here no two correspondences are equivalent so \cup is a disjunct sum).

[[you can replace Wi by kilometers, Precision by kilometers-per-hour
or you can do the test by yourself to convince you that this is true]]

So our goal is to compute the weighted harmonic means with these weights because this will provide us the true precision and recall.

In fact what the algorithm does is not to compute the harmonic means! I rephrase it, it computes the harmonic means of the numbers above it but since this is equivalent to computing precision and recall, it just computes it!

How?
For each column k in the table (corresponding to an algorithm), it maintains two vectors:
correctVect[k] and foundVect[k]
which is equal to \Sum{i=1}{n} |Ai\cap Ri| and \Sim{i=1}{n} |Ai|
and it additionally stores in "expected" the size of \Sum{i=1}{n} |Ri|

So computing the average means of these columns, with the weights corresponding respectively to the size |Ai\cup Ri|, corresponds to computing:

	correctVect[k] / foundVect[k]
and
	correctVect[k] / expected

which the program does...
    */
    public void printHTML( Vector<Vector<Object>> result, PrintStream writer ) {
	// variables for computing iterative harmonic means
	int expected = 0; // expected so far
	int foundVect[]; // found so far
	int correctFoundVect[]; // correct so far
	int correctExpVect[]; // correct so far
	long timeVect[]; // time so far
	fsize = format.length();
	// JE: the writer should be put out
	// JE: the h-means computation should be put out as well
	Formatter formatter = new Formatter( writer );

	// Print the header
	if ( embedded != true ) writer.println("<html><head></head><body>");
	writer.println("<table border='2' frame='sides' rules='groups'>");
	writer.println("<colgroup align='center' />");
	// for each algo <td spancol='2'>name</td>
	for ( String m : listAlgo ) {
	    writer.println("<colgroup align='center' span='"+fsize+"' />");
	}
	// For each file do a
	writer.println("<thead valign='top'><tr><th>algo</th>");
	// for each algo <td spancol='2'>name</td>
	for ( String m : listAlgo ) {
	    writer.println("<th colspan='"+fsize+"'>"+m+"</th>");
	}
	writer.println("</tr></thead><tbody><tr><td>test</td>");
	// for each algo <td>Prec.</td><td>Rec.</td>
	for ( String m : listAlgo ) {
	    for ( int i = 0; i < fsize; i++){
		writer.print("<td>");
		if ( format.charAt(i) == 'p' ) {
		    writer.print("Prec.");
		} else if ( format.charAt(i) == 'f' ) {
		    writer.print("FMeas.");
		} else if ( format.charAt(i) == 'o' ) {
		    writer.print("Over.");
		} else if ( format.charAt(i) == 't' ) {
		    writer.print("Time");
		} else if ( format.charAt(i) == 'r' ) {
		    writer.print("Rec.");
		}
		writer.println("</td>");
	    }
	    //writer.println("<td>Prec.</td><td>Rec.</td>");
	}
	writer.println("</tr></tbody><tbody>");
	foundVect = new int[ size ];
	correctFoundVect = new int[ size ];
	correctExpVect = new int[ size ];
	timeVect = new long[ size ];
	for( int k = size-1; k >= 0; k-- ) {
	    foundVect[k] = 0;
	    correctFoundVect[k] = 0;
	    correctExpVect[k] = 0;
	    timeVect[k] = 0;
	}
	// </tr>
	// For each directory <tr>
	boolean colored = false;
	for ( Vector<Object> test : result ) {
	    double newexpected = -1.;
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
		WeightedPREvaluator eval = (WeightedPREvaluator)f.nextElement();
		if ( eval != null ) {
		    // iterative H-means computation
		    if ( newexpected == -1. ){
			newexpected = eval.getExpected();
			expected += newexpected;
		    }
		    foundVect[k] += eval.getFound();
		    correctFoundVect[k] += eval.getCorrectFound();
		    correctExpVect[k] += eval.getCorrectExpected();
		    timeVect[k] += eval.getTime();
		    
		    for ( int i = 0 ; i < fsize; i++){
			writer.print("<td>");
			if ( format.charAt(i) == 'p' ) {
			    formatter.format("%1.2f", eval.getPrecision());
			} else if ( format.charAt(i) == 'f' ) {
			    formatter.format("%1.2f", eval.getFmeasure());
			} else if ( format.charAt(i) == 'o' ) {
			    formatter.format("%1.2f", eval.getOverall());
			} else if ( format.charAt(i) == 't' ) {
			    if ( eval.getTime() == 0 ){
				writer.print("-");
			    } else {
				formatter.format("%1.2f", eval.getTime());
			    }
			} else if ( format.charAt(i) == 'r' ) {
			    formatter.format("%1.2f", eval.getRecall());
			}
			writer.println("</td>");
		    }
		} else { // JE 2013: will break if the previous tests are all NULL
		    correctExpVect[k] += newexpected;
		    // Nothing needs to be incremented for precision
		    for ( int i = 0 ; i < fsize; i++) writer.print("<td>n/a</td>");
		    writer.println();
		}
	    }
	    writer.println("</tr>");
	}
	writer.print("<tr bgcolor=\"yellow\"><td>H-mean</td>");
	// Here we are computing a sheer average.
	// While in the column results we print NaN when the returned
	// alignment is empty,
	// here we use the real values, i.e., add 0 to both correctVect and
	// foundVect, so this is OK for computing the average.
	int k = 0;
	// ???
	for ( String m : listAlgo ) {
	    double precision = 1. - (double)correctFoundVect[k]/foundVect[k];
	    double recall = 1. - (double)correctExpVect[k]/expected;
	    for ( int i = 0 ; i < fsize; i++){
		writer.print("<td>");
		if ( format.charAt(i) == 'p' ) {
		    formatter.format("%1.2f", precision);
		} else if ( format.charAt(i) == 'f' ) {
		    formatter.format("%1.2f", 2 * precision * recall / (precision + recall));
		} else if ( format.charAt(i) == 'o' ) {
		    formatter.format("%1.2f", recall * (2 - (1 / precision)));
		} else if ( format.charAt(i) == 't' ) {
		    if ( timeVect[k] == 0 ){
			writer.print("-");
		    } else {
			formatter.format("%1.2f", timeVect[k]);
		    }
		} else if ( format.charAt(i) == 'r' ) {
		    formatter.format("%1.2f", recall);
		}
		writer.println("</td>");
	    };
	    k++;
	}
	writer.println("</tr>");
	writer.println("</tbody></table>");
	writer.println("<p><small>n/a: result alignment not provided or not readable<br />");
	writer.println("NaN: division per zero, likely due to empty alignment.</small></p>");
	if ( embedded != true ) writer.println("</body></html>");
	}

    public void usage() {
	usage( "java "+this.getClass().getName()+" [options]\nEvaluates (with weighted measures) in parallel several matching results on several tests in subdirectories" );
    }
}

