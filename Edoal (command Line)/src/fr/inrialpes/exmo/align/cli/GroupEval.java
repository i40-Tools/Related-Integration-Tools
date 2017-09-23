/*
 * $Id: GroupEval.java 2150 2017-07-18 15:15:46Z euzenat $
 *
 * Copyright (C) 2003-2015, 2017, INRIA
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

import fr.inrialpes.exmo.align.impl.eval.PRecEvaluator;
import fr.inrialpes.exmo.align.impl.eval.WeightedPREvaluator; //JE:merge

import fr.inrialpes.exmo.ontowrap.OntologyFactory;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.lang.Integer;
import java.lang.reflect.Constructor;
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

/**
 * A basic class for synthesizing the results of a set of alignments provided by
 *  different algorithms. The output is a table showing various classical measures
 *  for each test and for each algorithm. Average is also computed as Harmonic means.
 *  
 *  <pre>
 *  java -cp procalign.jar fr.inrialpes.exmo.align.util.GroupEval [options]
 *  </pre>
 *
 *  where the options are:
 *  <pre>
 * -c,--color &lt;COLOR&gt;       Color even lines of the output in COLOR (default:
 *                         lightblue)
 * -D &lt;NAME=VALUE&gt;          Use value for given property
 * -d,--debug &lt;LEVEL&gt;       debug argument is deprecated, use logging instead
 *                          See http://alignapi.gforge.inria.fr/logging.html
 * -e,--evaluator &lt;CLASS&gt;   Use CLASS as evaluation plotter
 * -f,--format &lt;MEAS&gt;       Used MEASures and order
 *                         (precision/recall/f-measure/overall/time)  (default:
 *                         pr)
 * -h,--help                Print this page
 * -l,--list &lt;FILE&gt;         List of FILEs to be included in the results (required)
 * -o,--output &lt;FILE&gt;       Send output to FILE
 * -P,--params &lt;FILE&gt;       Read parameters from FILE
 * -r,--reference &lt;FILE&gt;    Name of the reference alignment FILE (default:
 *                         refalign.rdf)
 * -t,--type &lt;TYPE&gt;         Output TYPE (html|xml|tex|ascii|triangle; default:
 *                         html)
 * -w,--directory &lt;DIR&gt;     The DIRectory containing the data to evaluate
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
 * $Id: GroupEval.java 2150 2017-07-18 15:15:46Z euzenat $
 * </pre>
 *
 */

public class GroupEval extends CommonCLI {
    final static Logger logger = LoggerFactory.getLogger( GroupEval.class );

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
    String classname = "fr.inrialpes.exmo.align.impl.eval.PRecEvaluator";
    Constructor<?> evalConstructor = null;

    public GroupEval() {
	super();
	options.addOption( createListOption( "l", "list", "List of FILEs to be included in the results (required)", "FILE", ',' ) );
	options.addOption( createOptionalOption( "c", "color", "Color even lines of the output in COLOR (default: lightblue)", "COLOR" ) );
	options.addOption( createRequiredOption( "e", "evaluator", "Use CLASS as evaluation plotter", "CLASS" ) );
	options.addOption( createRequiredOption( "f", "format", "Used MEASures and order (precision/recall/f-measure/overall/time) (default: "+format+")", "MEAS (sepr)" ) );
	options.addOption( createRequiredOption( "t", "type", "Output TYPE (html|xml|tex|ascii|triangle; default: "+type+")", "TYPE" ) );
	//options.addOption( createRequiredOption( "s", "sup", "Specifies if dominant columns are algorithms or measure (default: s)", "ALGO" ) );
	options.addOption( createRequiredOption( "r", "reference", "Name of the reference alignment FILE (default: "+reference+")", "FILE" ) );
	options.addOption( createRequiredOption( "w", "directory", "The DIRectory containing the data to evaluate", "DIR" ) );
    }


    public static void main(String[] args) {
	try { new GroupEval().run( args ); }
	catch (Exception ex) { ex.printStackTrace(); };
    }

    public void run(String[] args) throws Exception {

	try { 
	    CommandLine line = parseCommandLine( args );
	    if ( line == null ) return; // --help

	    // Here deal with command specific arguments
	    if ( line.hasOption( 'e' ) ) classname = line.getOptionValue( 'e' );
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

	Class<?> evalClass = Class.forName( classname );
	Class<?>[] cparams = { Alignment.class, Alignment.class };
	evalConstructor = evalClass.getConstructor( cparams );

	print( iterateDirectories() );
    }

    public Vector<Vector<Object>> iterateDirectories (){
	Vector<Vector<Object>> result = null;
	File [] subdir = null;
	try {
	    if (ontoDir == null) {
		subdir = ( new File(System.getProperty("user.dir") ) ).listFiles(); 
	    } else {
		subdir = ( new File(ontoDir) ).listFiles();
	    }
	} catch ( Exception e ) {
	    logger.error( "Cannot stat dir ", e );
	    usage();
	}
	int size = subdir.length;
        Arrays.sort(subdir);
	result = new Vector<Vector<Object>>(size);
	int i = 0;
	for ( int j=0 ; j < size; j++ ) {
	    if( subdir[j].isDirectory() ) {
		//logger.trace( "Entering directory {}", subdir[j] );
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
	    //logger.trace( "  Considering result {}", i );
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
	    //eval = new PRecEvaluator( align1, align2 );
	    Object[] mparams = { align1, align2 };
	    eval = (Evaluator) evalConstructor.newInstance(mparams);
	    // Compare
	    eval.eval( parameters ) ;
	} catch (Exception ex) {
	    logger.debug( "IGNORED Exception", ex );
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
	if ( outputfilename == null ) {
	    writer = System.out;
	} else {
	    try {
		writer = new PrintStream( new FileOutputStream( outputfilename ) );
	    } catch ( FileNotFoundException fnfex) {
		logger.error( "Cannot open file", fnfex );
		return;
	    }
	}
	try {
	    if ( type.equals("html") ) printHTML( result, writer );
	    else if ( type.equals("tex") ) printLATEX( result, writer );
	    else if ( type.equals("triangle") ) printTRIANGLE( result, writer );
	} finally {
	    writer.close();
	}
    }

    /**
     * A plot of the precision recall points on a triangular space
     * Added level lines provides by Christian Meilicke (U. Mannheim)
     * See his program in comment below
     *
     * @param result: the vector of vector result to be printed
     * @param writer: the writer in which to print it
     */
    public void printTRIANGLE( Vector<Vector<Object>> result, PrintStream writer ) {
	// variables for computing iterative harmonic means
	int expected = 0; // expected so far
	int foundVect[]; // found so far
	int correctVect[]; // correct so far
	long timeVect[]; // time so far
	foundVect = new int[ size ];
	correctVect = new int[ size ];
	timeVect = new long[ size ];
	for( int k = size-1; k >= 0; k-- ) {
	    foundVect[k] = 0;
	    correctVect[k] = 0;
	    timeVect[k] = 0;
	}
	for ( Vector<Object> test : result ) {
	    int nexpected = -1;
	    Enumeration<Object> f = test.elements();
	    // Too bad the first element must be skipped
	    f.nextElement();
	    for( int k = 0 ; f.hasMoreElements() ; k++) {
		PRecEvaluator eval = (PRecEvaluator)f.nextElement();
		if ( eval != null ){
		    // iterative H-means computation
		    if ( nexpected == -1 ){
			nexpected = 0;
			expected += eval.getExpected();
		    }
		    foundVect[k] += eval.getFound();
		    correctVect[k] += eval.getCorrect();
		    timeVect[k] += eval.getTime();
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
	writer.println("\n%% Plot generated by GroupEval of alignapi");
	writer.println("\\begin{tikzpicture}[cap=round]");
	writer.println("% Draw grid");
	//writer.println("\\draw[step=1cm,very thin,color=gray] (-0.2,-0.2) grid (10.0,9.0);");
	writer.println("\\draw[|-|] (-0,0) -- (10,0);");
	writer.println("%\\draw[dashed,very thin] (0,0) -- (5,8.66) -- (10,0);");
	writer.println("\\draw[dashed,very thin] (10,0) arc (0:60:10cm);");
	writer.println("\\draw[dashed,very thin] (0,0) arc (180:120:10cm);");

	writer.println("%% Level lines for recall");
	writer.println("\\draw[dashed] (10,0) arc (0:60:10cm) node[anchor=south east]  {{\\tiny R=1.}};");
	writer.println("\\draw[dotted,very thin] (9,0) arc (0:63:9cm) node[anchor=south east] {{\\tiny R=.9}};");
	writer.println("\\draw[dotted,very thin] (8,0) arc (0:66:8cm) node[anchor=south east]  {{\\tiny R=.8}};");
	writer.println("\\draw[dotted,very thin] (7,0) arc (0:70:7cm) node[anchor=south east]  {{\\tiny R=.7}};");
	writer.println("\\draw[dotted,very thin] (6,0) arc (0:73:6cm) node[anchor=south east]  {{\\tiny R=.6}};");
	writer.println("\\draw[dotted,very thin] (5,0) arc (0:76:5cm) node[anchor=south east] {{\\tiny R=.5}};");
	writer.println("\\draw[dotted,very thin] (4,0) arc (0:78:4cm) node[anchor=south east] {{\\tiny R=.4}};");
	writer.println("\\draw[dotted,very thin] (3,0) arc (0:80:3cm) node[anchor=south east] {{\\tiny R=.3}};");
	writer.println("\\draw[dotted,very thin] (2,0) arc (0:82:2cm) node[anchor=south east] {{\\tiny R=.2}};");
	writer.println("\\draw[dotted,very thin] (1,0) arc (0:84:1cm) node[anchor=south east] {{\\tiny R=.1}};");
	writer.println("%% Level lines for precision");
	writer.println("\\draw[dashed] (0,0) arc (180:120:10cm) node[anchor=south west] {{\\tiny P=1.}};");
	writer.println("\\draw[dotted,very thin] (1,0) arc (180:117:9cm) node[anchor=south west] {{\\tiny P=.9}};");
	writer.println("\\draw[dotted,very thin] (2,0) arc (180:114:8cm) node[anchor=south west] {{\\tiny P=.8}};");
	writer.println("\\draw[dotted,very thin] (3,0) arc (180:110:7cm) node[anchor=south west] {{\\tiny P=.7}};");
	writer.println("\\draw[dotted,very thin] (4,0) arc (180:107:6cm) node[anchor=south west] {{\\tiny P=.6}};");
	writer.println("\\draw[dotted,very thin] (5,0) arc (180:105:5cm) node[anchor=south west] {{\\tiny P=.5}};");
	writer.println("\\draw[dotted,very thin] (6,0) arc (180:103:4cm) node[anchor=south west] {{\\tiny P=.4}};");
	writer.println("\\draw[dotted,very thin] (7,0) arc (180:100:3cm) node[anchor=south west] {{\\tiny P=.3}};");
	writer.println("\\draw[dotted,very thin] (8,0) arc (180:96:2cm) node[anchor=south west] {{\\tiny P=.2}};");
	writer.println("\\draw[dotted,very thin] (9,0) arc (180:90:1cm) node[anchor=south west] {{\\tiny P=.1}};");
	writer.println("%% Level lines for F-measure");
	writer.println("\\draw[very thin,densely dotted] plot[smooth] coordinates { (0.56,3.29) (1.55,3.10) (2.46,2.68) (3.31,2.05) (4.12,1.19) (5.00,0.00) (6.42,1.79) (9.44,3.29)};");
	writer.println("\\draw (0.56,3.29) node[anchor=south west] {\\tiny{F=0.5}};");
	writer.println("\\draw[very thin,densely dotted] plot[smooth] coordinates { (0.92,4.19) (1.96,4.05) (2.95,3.78) (3.93,3.48) (5.00,3.32) (6.56,3.63) (9.08,4.19)};");
	writer.println("\\draw (0.92,4.19) node[anchor=south west] {\\tiny{F=0.6}};");
	writer.println("\\draw[very thin,densely dotted] plot[smooth] coordinates { (1.45,5.19) (2.59,5.11) (3.74,4.98) (5.00,4.90) (6.73,5.03) (8.55,5.19)};");
	writer.println("\\draw (1.45,5.19) node[anchor=south west] {\\tiny{F=0.7}};");
	writer.println("\\draw[very thin,densely dotted] plot[smooth] coordinates { (2.22,6.29) (3.54,6.27) (5.00,6.24) (6.91,6.28) (7.78,6.29)};");
	writer.println("\\draw (2.22,6.29) node[anchor=south west] {\\tiny{F=0.8}};");
	writer.println("\\draw[very thin,densely dotted] plot[smooth] coordinates { (3.35,7.47) (5.00,7.48) (6.65,7.47)};");
	writer.println("\\draw (3.35,7.47) node[anchor=south west] {\\tiny{F=0.9}};");

	writer.println("\\draw (0,-0.3) node {$recall$};");
	writer.println("\\draw (10,-0.3) node {$precision$};");
	//writer.println("\\draw (0,-0.3) node {0.}; ");
	//writer.println("\\draw (10,-0.3) node {1.}; ");
	writer.println("% Plots");
	int k = 0;
	for ( String m: listAlgo ) {
	    double precision = (double)correctVect[k]/foundVect[k];
	    double recall = (double)correctVect[k]/expected;
	    double prec2 = precision*precision;
	    double a = ((prec2-(recall*recall)+1)/2);
	    double b = java.lang.Math.sqrt( prec2 - (a*a) );
	    if ( b == b ) { // Test if b is not NaN! Otherwise, no square root: the point is out of the triangle
		a = a*10; b = b*10; //for printing scale 10.
		writer.println("\\draw plot[mark=+,] coordinates {("+a+","+b+")};");
		writer.println("\\draw ("+(a+.01)+","+(b+.01)+") node[anchor=south west] {"+m+"};");
	    }
	    k++;
	}
	writer.println("\\end{tikzpicture}");
	writer.println();
	writer.println("\\end{document}");
    }

    public void printHTML( Vector<Vector<Object>> result, PrintStream writer ) {
	// variables for computing iterative harmonic means
	int expected = 0; // expected so far
	int foundVect[]; // found so far
	int correctVect[]; // correct so far
	long timeVect[]; // time so far
	Formatter formatter = new Formatter(writer);

	fsize = format.length();
	// JE: the h-means computation should be put out as well
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
	correctVect = new int[ size ];
	timeVect = new long[ size ];
	for( int k = size-1; k >= 0; k-- ) {
	    foundVect[k] = 0;
	    correctVect[k] = 0;
	    timeVect[k] = 0;
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
		PRecEvaluator eval = (PRecEvaluator)f.nextElement();
		if ( eval != null ){
		    // iterative H-means computation
		    if ( nexpected == -1 ){
			expected += eval.getExpected();
			nexpected = 0;
		    }
		    foundVect[k] += eval.getFound();
		    correctVect[k] += eval.getCorrect();
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
			writer.print("</td>");
		    }
		} else {
		    for ( int i = 0 ; i < fsize; i++) writer.print("<td>n/a</td>");
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
	    double precision = (double)correctVect[k]/foundVect[k];
	    double recall = (double)correctVect[k]/expected;
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

    public void printLATEX( Vector<Vector<Object>> result, PrintStream writer ) {
	// variables for computing iterative harmonic means
	int expected = 0; // expected so far
	int foundVect[]; // found so far
	int correctVect[]; // correct so far
	long timeVect[]; // time so far
	Formatter formatter = new Formatter(writer);

	fsize = format.length();
	// JE: the h-means computation should be put out as well
	// Print the header
	writer.println("\\documentclass[11pt]{book}");
	writer.println();
	writer.println("\\begin{document}");
	writer.println("\\date{today}");
	writer.println("");
	writer.println("\n%% Plot generated by GroupEval of alignapi");
	writer.println("\\setlength{\\tabcolsep}{3pt} % May be changed");
	writer.println("\\begin{table}");
	writer.print("\\begin{tabular}{|l||");
	for ( int i = size; i > 0; i-- ) {
	    for ( int j = fsize; j > 0; j-- ) writer.print("c");
	    writer.print("|");
	}
	writer.println("}");
	writer.println("\\hline");
	// For each file do a
	writer.print("algo");
	// for each algo <td spancol='2'>name</td>
	for ( String m : listAlgo ) {
	    writer.print(" & \\multicolumn{"+fsize+"}{c|}{"+m+"}");
	}
	writer.println(" \\\\ \\hline");
	writer.print("test");
	// for each algo <td>Prec.</td><td>Rec.</td>
	for ( String m : listAlgo ) {
	    for ( int i = 0; i < fsize; i++){
		writer.print(" & ");
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
	    }
	}
	writer.println(" \\\\ \\hline");
	foundVect = new int[ size ];
	correctVect = new int[ size ];
	timeVect = new long[ size ];
	for( int k = size-1; k >= 0; k-- ) {
	    foundVect[k] = 0;
	    correctVect[k] = 0;
	    timeVect[k] = 0;
	}
	for ( Vector<Object> test : result ) {
	    int nexpected = -1;
	    // Print the directory 
	    writer.print((String)test.get(0));
	    // For each record print the values
	    Enumeration<Object> f = test.elements();
	    f.nextElement();
	    for( int k = 0 ; f.hasMoreElements() ; k++) {
		PRecEvaluator eval = (PRecEvaluator)f.nextElement();
		if ( eval != null ){
		    // iterative H-means computation
		    if ( nexpected == -1 ){
			expected += eval.getExpected();
			nexpected = 0;
		    }
		    foundVect[k] += eval.getFound();
		    correctVect[k] += eval.getCorrect();
		    timeVect[k] += eval.getTime();
		    
		    for ( int i = 0 ; i < fsize; i++){
			writer.print(" & ");
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
		    }
		} else {
		    writer.print(" & \\multicolumn{"+fsize+"}{c|}{n/a}");
		}
	    }
	    writer.println(" \\\\");
	}
	writer.print("H-mean");
	// Here we are computing a sheer average.
	// While in the column results we print NaN when the returned
	// alignment is empty,
	// here we use the real values, i.e., add 0 to both correctVect and
	// foundVect, so this is OK for computing the average.
	int k = 0;
	// ???
	for ( String m : listAlgo ) {
	    double precision = (double)correctVect[k]/foundVect[k];
	    double recall = (double)correctVect[k]/expected;
	    for ( int i = 0 ; i < fsize; i++){
		writer.print(" & ");
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
	    };
	    k++;
	}
	writer.println(" \\\\ \\hline");
	writer.println("\\end{tabular}");
	writer.println("\\caption{Plot generated by GroupEval of alignapi \\protect\\footnote{n/a: result alignment not provided or not readable -- NaN: division per zero, likely due to empty alignment.}}");
	writer.println("\\end{table}");
	writer.println("\\end{document}");
    }

    public void usage() {
	usage( "java "+this.getClass().getName()+" [options]\nEvaluates in parallel several matching results on several tests in subdirectories" );
    }

/*
      // Here is the code provided by Christian Meilicke and modified by JE
      // For computing the F-measure level line in the Triangle representation
      // The class can be put in FMeasureLines.java and compiled.
      // The same code (getPolarCoords) can be compiled to draw the line of a
      // particular matcher on this same graph eventually.

public class FMeasureLines {

    // The step of the curve
    private static double step = .1;

    // Number of points found
    private int counter = 0;
	
    public static void main(String[] args) {
	new FMeasureLines().run();
    }

    public void run() {
	for ( double i = .5; i < 1. ; i=i + .1 ) {
	    printLaTeXCurve( i );
	}
    }

    public void printLaTeXCurve( double f ) {
	// create array of precision recall pairs for f-measure 0.6
	double[][] fline = getFixedFLine( f );
	
	// create representation in polar coordinates for tikz inlcusion
	double line[][] = getPolarCoords(fline);

	System.out.print( "\\draw[very thin,dashed] plot[smooth] coordinates {" );
	for ( int i = 0; i < counter ; i++ ) {
	    if ( line[i] != null ) {
		System.out.format( " (%.2f,%.2f)", 10*line[i][0], 10*line[i][1] );
	    }
	}
	System.out.println( "};" );
	System.out.format( "\\draw (%.2f,%.2f) node[anchor=south west] {\\tiny{F=%.1f}};\n", 10*line[0][0], 10*line[0][1], f );
    }

    // Compute the pair of points for a given F-Measure
    public double[][] getFixedFLine( double f ) {
	double[][] result = new double[(int)(2*(1/step+1))][2];
	double pr = 1.;
	counter = 0;
	double p = getRGivenFAndP( f, pr );

	while ( pr > 0. ) {
	    if ( p >= 0. ) {
		result[counter][0] = p;
		result[counter][1] = pr;
		counter++;
		//System.err.println(" P / PR = "+p+" / "+pr );
	    }
	    pr -= step;
	    p = getRGivenFAndP( f, pr );
	}
	result[counter][0] = 1.;
	result[counter][1] = getRGivenFAndP( f, 1. );
	//System.err.println(" P / PR = "+1.+" / "+getRGivenFAndP( f, 1. ) );
	counter++;

	return result;
    }

    // Provides the Recall given a particular F-measure and Precision
    // F = 2PR/P+R
    // => FP+FR = 2PR
    // => FP = 2PR - FR
    // => FP/R = 2P-F
    // => R = FP/(2P-F)
    // (same for P given the symmetry of the formula)
    private static double getRGivenFAndP( double f, double p ) {
	double r = p * f / (2 * p - f);
	if ( r <= 0 || r >= 1.0 ) return -1.;
	return r;
    }

    // Transforms all coordinates in polar coords
    public double[][] getPolarCoords(double line[][]) {
	double[][] result = new double[counter][2];
	for ( int i = 0; i < counter; i++ ) {
	    double ppair[] = getPolarCoord( line[i][0], line[i][1] );
	    if ( ppair != null ) result[i] = ppair;
	}	
	return result;
    }

    // Transform the coordinates from P/R (cartesian) to ?? (polar)
    private double[] getPolarCoord( double p, double r ) {
	double pp = ((p * p) - (r * r) + 1) / 2;
	double pr = Math.sqrt(Math.abs((p * p) - (pp * pp)));
	return new double[]{pp, pr};
    }
}
*/

/* A few comments on how and why computing "weighted harmonic means" (Jérôme Euzenat)

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


}

