/*
 * $Id: GenPlot.java 2150 2017-07-18 15:15:46Z euzenat $
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

/*
 * This program evaluates the results of several ontology aligners and plot
 * these results
*/
package fr.inrialpes.exmo.align.cli;

import org.semanticweb.owl.align.Alignment;

import fr.inrialpes.exmo.align.impl.eval.GraphEvaluator;
import fr.inrialpes.exmo.align.impl.eval.Pair;

import fr.inrialpes.exmo.ontowrap.OntologyFactory;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.lang.Integer;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.lang.reflect.Constructor;
import java.lang.InstantiationException;

import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import fr.inrialpes.exmo.align.parser.AlignmentParser;

/**
 * A basic class for ploting the results of an evaluation.
 *
 * These graphs are however computed on averaging the precision recall/graphs
 * on test directories instead of recording the actual precision recall graphs
 * which would amount at recoding all the valid and invalid alignment cells and
 * their level.
 *  
 *  <pre>
 *  java -cp procalign.jar fr.inrialpes.exmo.align.util.GenPlot [options]
 *  </pre>
 *
 *  where the options are:
 *  <pre>
 *  -o filename --output=filename
 *  -l list of compared algorithms
 *  -t output --type=output: xml/tex/html/ascii
 *  -e classname --evaluator=classname
 *  -g classname --grapher=classname
 * </pre>
 *
 * The input is taken in the current directory in a set of subdirectories (one per
 * test) each directory contains a the alignment files (one per algorithm) for that test and the
 * reference alignment file.
 *
 * If output is
 * requested (<CODE>-o</CODE> flags), then output will be written to
 *  <CODE>output</CODE> if present, stdout by default. In case of the Latex output, there are numerous files generated (regardless the <CODE>-o</CODE> flag).
 *
 * <pre>
 * $Id: GenPlot.java 2150 2017-07-18 15:15:46Z euzenat $
 * </pre>
 *
 * @author Jérôme Euzenat
 */

public class GenPlot extends CommonCLI {
    final static Logger logger = LoggerFactory.getLogger( GenPlot.class );

    int STEP = 10;
    String[] listAlgo = null;
    Vector<GraphEvaluator> listEvaluators;
    String fileNames = "";
    Constructor<?> evalConstructor = null;
    Constructor<?> graphConstructor = null;
    String xlabel;
    String ylabel;
    String type = "tex";
    int size = 0; // the set of algo to compare
    String ontoDir = null;

    public GenPlot() {
	super();
	options.addOption( createListOption( "l", "list", "List of FILEs to be included in the results (required)", "FILE", ',' ) );
	options.addOption( createRequiredOption( "t", "type", "Output TYPE (html|xml|tex|ascii|triangle; default: "+type+")", "TYPE" ) );
	options.addOption( createRequiredOption( "e", "evaluator", "Use CLASS as evaluation plotter", "CLASS" ) );
	options.addOption( createRequiredOption( "g", "grapher", "Use CLASS as graph generator", "CLASS" ) );
	//options.addOption( createRequiredOption( "s", "step", "" ) );
	options.addOption( createRequiredOption( "w", "directory", "The DIRectory containing the data to plot", "DIR" ) );
    }

    public static void main(String[] args) {
	try { new GenPlot().run( args ); }
	catch (Exception ex) { ex.printStackTrace(); };
    }

    public void run(String[] args) throws Exception {
	String evalCN = "fr.inrialpes.exmo.align.impl.eval.PRecEvaluator";
	String graphCN = "fr.inrialpes.exmo.align.impl.eval.PRGraphEvaluator";

	try { 
	    CommandLine line = parseCommandLine( args );
	    if ( line == null ) return; // --help
	    
	    // Here deal with command specific arguments
	    if ( line.hasOption( 'e' ) ) evalCN = line.getOptionValue( 'e' );
	    if ( line.hasOption( 'g' ) ) graphCN = line.getOptionValue( 'g' );
	    if ( line.hasOption( 't' ) ) type = line.getOptionValue( 't' );
	    if ( line.hasOption( 'l' ) ) {
		listAlgo = line.getOptionValues( 'l' );
		size = listAlgo.length;
	    }
	    if ( line.hasOption( 'w' ) ) ontoDir = line.getOptionValue( 'w' );
	} catch( ParseException exp ) {
	    logger.error( exp.getMessage() );
	    usage();
	    System.exit(-1);
	}

	Class<?> graphClass = Class.forName( graphCN );
	Class<?>[] cparams = {};
	graphConstructor = graphClass.getConstructor( cparams );

	// JE: This is not used
	Class<?> evalClass = Class.forName( evalCN );
	Class<?>[] caparams = { Alignment.class, Alignment.class };
	evalConstructor = evalClass.getConstructor( caparams );

	// Collect correspondences from alignments in all directories
	// . -> Vector<EvalCell>
	listEvaluators = iterateDirectories();

	// Find the largest value
	int max = 0;
	for( GraphEvaluator e : listEvaluators ) {
	    int n = e.nbCells();
	    if ( n > max ) max = n;
	}
	parameters.setProperty( "scale", Integer.toString( max ) );

	xlabel = listEvaluators.get(0).xlabel();
	ylabel = listEvaluators.get(0).ylabel();

	// Vector<EvalCell> -> Vector<Pair>
	// Convert the set of alignments into the list of required point pairs
	// We must convert the 
	Vector<Vector<Pair>> toplot = new Vector<Vector<Pair>>();
	for( int i = 0; i < size ; i++ ) {
	    // Convert it with the adequate GraphPlotter
	    // Scale the point pairs to the current display (local)
	    toplot.add( i, listEvaluators.get(i).eval( parameters ) );
	    //scaleResults( STEP, 
	}

	// Set output file
	OutputStream stream;
	if ( outputfilename == null) {
	    stream = System.out;
	} else {
	    stream = new FileOutputStream( outputfilename );
	}
	PrintWriter writer = new PrintWriter (
		   new BufferedWriter(
		     new OutputStreamWriter( stream, "UTF-8" )), true);

	//System.err.println ( toplot.get(0));
	// Display the required type of output
	// Vector<Pair> -> .
	if ( type.equals("tsv") ){
	    printTSV( toplot, writer );
	} else if ( type.equals("html") ) {
	    printHTMLGGraph( toplot, writer );
	} else if ( type.equals("tex") ) {
	    printPGFTex( toplot, writer );
	} else {
	    logger.error( "Flag -t {} : not implemented yet", type );
	    usage();
	    System.exit(-1);
	}
    }

    /**
     * Iterate on each subdirectory
     * @return a vector[ each algo ] of vector [ each point ]
     * The points are computed by aggregating the values
     *  (and in the end computing the average)
     */
    public Vector<GraphEvaluator> iterateDirectories() {
	Vector<GraphEvaluator> evaluators = new Vector<GraphEvaluator>( size );
	Object[] mparams = {};
	try {
	    for( int i = 0; i < size; i++ ) {
		GraphEvaluator ev = (GraphEvaluator)graphConstructor.newInstance(mparams);
		ev.setStep( STEP );
		evaluators.add( i, ev );
	    }
	} catch ( Exception ex ) { //InstantiationException, IllegalAccessException
	    logger.error( "FATAL Exception", ex );
	    System.exit(-1);
	}

	File [] subdir = null;
	try {
	    if (ontoDir == null) {
		subdir = (new File(System.getProperty("user.dir"))).listFiles(); 
	    } else {
		subdir = (new File(ontoDir)).listFiles();
	    }
	} catch ( Exception e ) {
	    logger.error( "Cannot stat dir", e );
	    usage();
	    System.exit(-1);
	}

	// Evaluate the results in each directory
	for ( int k = subdir.length-1 ; k >= 0; k-- ) {
	    if( subdir[k].isDirectory() ) {
		// eval the alignments in a subdirectory
		iterateAlignments( subdir[k], evaluators );//, result );
	    }
	}
	return evaluators;
    }

    public void iterateAlignments ( File dir, Vector<GraphEvaluator> evaluators ) {
	//logger.trace( "Directory : {}", dir );
	String prefix = dir.toURI().toString()+"/";

	AlignmentParser aparser = new AlignmentParser();
	Alignment refalign = null;

	try { // Load the reference alignment...
	    refalign = aparser.parse( prefix+"refalign.rdf" );
	    //logger.trace(" Reference alignment parsed");
	} catch ( Exception aex ) {
	    logger.error( "GenPlot cannot parse refalign", aex );
	    System.exit(-1);
	}

	// for all alignments there,
	for( int i = 0; i < size; i++ ) {
	    String algo = listAlgo[i];
	    Alignment al = null;
	    //logger.trace("  Considering result {} ({})", algo, i );
	    try {
		aparser.initAlignment( null );
		al = aparser.parse( prefix+algo+".rdf" );
		//logger.trace(" Alignment {} parsed", algo );
	    } catch ( Exception ex ) { 
		logger.error( "IGNORED Exception", ex );
	    }
	    // even if empty, declare refalign
	    evaluators.get(i).ingest( al, refalign );
	}
	// Unload the ontologies.
	try {
	    OntologyFactory.clear();
	} catch ( OntowrapException owex ) { // only report
	    logger.error( "IGNORED Exception", owex );
	}
    }
    
    // should be OK for changing granularity
    // This is not really scalling...
    // This is unused
    public Vector<Pair> scaleResults( int STEP, Vector<Pair> input ) {
	int j = 0;
	Vector<Pair> output = new Vector<Pair>(); // Set the size!
	Pair last = null;
	double next = 0.;//is it a double??
	for ( Pair npair : input ) {
	    if ( npair.getX() == next ) {
		output.add( npair );
		next += STEP;
	    } else if ( npair.getX() >= next ) { // interpolate
		double val;
		if ( last.getY() >= npair.getY() ) {
		    val = npair.getY() + ( ( last.getY() - npair.getY() ) / ( last.getX()-npair.getX() ) );
		} else {
		    val = last.getY() + ( (npair.getY() - last.getY() ) / ( last.getX()-npair.getX() ) );
		}
		//System.err.println( "Scaling: "+next+" / "+val );
		output.add( new Pair( next, val )  );
		next += STEP;
	    }
	    last = npair;
	}
	output.add( last );
	return( output );
    }

    /**
     * This does average plus plot
     *
     * @param result the resulting plot
     * @param writer in which the output is sent
     */
    public void printPGFTex( Vector<Vector<Pair>> result, PrintWriter writer ){
	int i = 0;
	String marktable[] = { "+", "*", "x", "-", "|", "o", "asterisk", "star", "oplus", "oplus*", "otimes", "otimes*", "square", "square*", "triangle", "triangle*", "diamond", "diamond*", "pentagon", "pentagon*"};
	String colortable[] = { "black", "red", "green!50!black", "blue", "cyan", "magenta" }	;
	writer.println("\\documentclass[11pt]{book}");
	writer.println();
	writer.println("\\usepackage{pgf}");
	writer.println("\\usepackage{tikz}");
	writer.println("\\usetikzlibrary{plotmarks}");
	writer.println();
	writer.println("\\begin{document}");
	writer.println("\\date{today}");
	writer.println("");
	writer.println("\n%% Plot generated by GenPlot of alignapi");
	writer.println("\\begin{tikzpicture}[cap=round]");
	writer.println("% Draw grid");
	writer.println("\\draw[step="+(STEP/10)+"cm,very thin,color=gray] (-0.2,-0.2) grid ("+STEP+","+STEP+");");
	writer.println("\\draw[->] (-0.2,0) -- (10.2,0);");
	writer.println("\\draw (5,-0.3) node {$"+xlabel+"$}; ");
	writer.println("\\draw (0,-0.3) node {0.}; ");
	writer.println("\\draw (10,-0.3) node {1.}; ");
	writer.println("\\draw[->] (0,-0.2) -- (0,10.2);");
	writer.println("\\draw (-0.3,0) node {0.}; ");
	writer.println("\\draw (-0.3,5) node[rotate=90] {$"+ylabel+"$}; ");
	writer.println("\\draw (-0.3,10) node {1.}; ");
	writer.println("% Plots");
	i = 0;
	for ( String m : listAlgo ) {
	    writer.print("\\draw["+colortable[i%6] );
	    if ( !listEvaluators.get(i).isValid() ) writer.print(",dotted");
	    writer.println("] plot[mark="+marktable[i%19]+"] file {"+m+".table};");
	    //,smooth
	    i++;
	}
	// And a legend
	writer.println("% Legend");
	i = 0;
	for ( String m : listAlgo ) {
	    writer.print("\\draw["+colortable[i%6] );
	    if ( !listEvaluators.get(i).isValid() ) writer.print(",dotted");
	    writer.println("] plot[mark="+marktable[i%19]+"] coordinates {("+((i%3)*3+1)+","+(-(i/3)*.8-1)+") ("+((i%3)*3+3)+","+(-(i/3)*.8-1)+")};");
	    //,smooth
	    writer.println("\\draw["+colortable[i%6]+"] ("+((i%3)*3+2)+","+(-(i/3)*.8-.8)+") node {"+m+"};");
	    writer.printf("\\draw["+colortable[i%6]+"] ("+((i%3)*3+2)+","+(-(i/3)*.8-1.2)+") node {%1.2f};\n", listEvaluators.get(i).getGlobalResult() );
	    i++;
	}
	writer.println("\\end{tikzpicture}");
	writer.println();
	writer.println("\\end{document}");

	File dir = null;
	if ( outputfilename == null ) {
	    dir = new File(".");
	} else {
	    dir = (new File(outputfilename)).getParentFile();
	}
	i = 0;
	for( Vector<Pair> table : result ) {
	    String algo = listAlgo[i];
	    // Open one file
	    PrintWriter auxwriter = null;
	    try {
		// Here, if -o is used, every file should go there
		auxwriter = new PrintWriter (
				    new BufferedWriter(
                                       new OutputStreamWriter(
					  new FileOutputStream(new File(dir,algo+".table")), "UTF-8" )), true);
		// Print header
		auxwriter.println("#Curve 0, "+(STEP+1)+" points");
		auxwriter.println("#x y type");
		auxwriter.println("%% Plot generated by GenPlot of alignapi");
		auxwriter.println("%% Include in PGF tex by:\n");
		auxwriter.println("%% \\begin{tikzpicture}[cap=round]");
		auxwriter.println("%% \\draw[step="+(STEP/10)+"cm,very thin,color=gray] (-0.2,-0.2) grid ("+STEP+","+STEP+");");
		auxwriter.println("%% \\draw[->] (-0.2,0) -- (10.2,0) node[right] {$"+xlabel+"$}; ");
		auxwriter.println("%% \\draw[->] (0,-0.2) -- (0,10.2) node[above] {$"+ylabel+"$}; ");
		auxwriter.println("%% \\draw plot[mark=+,smooth] file {"+algo+".table};");
		auxwriter.println("%% \\end{tikzpicture}");
		auxwriter.println();
		for( Pair p : table ) {
		    //logger.trace( " >> {} - {}", p.getX(), p.getY() );
		    auxwriter.println( p.getX()*10+" "+ p.getY()*10 );
		}
	    } catch ( FileNotFoundException fnfex ) {
		logger.error( "IGNORED Exception", fnfex );
	    } catch ( UnsupportedEncodingException ueex ) {
		logger.error( "IGNORED Exception", ueex );
	    } finally {
		if ( auxwriter != null ) auxwriter.close();
	    }
	    // UnsupportedEncodingException + FileNotFoundException
	    i++;
	}
    }

    /**
     * This does average plus generate the call for Google Chart API
     *
     * @param result the resulting plot
     * @param writer in which the output is sent
     */
    public void printHTMLGGraph( Vector<Vector<Pair>> result, PrintWriter writer ){
	writer.print("<img src=\"http://chart.apis.google.com/chart?");
	writer.print("chs=600x500&cht=lxy&chg=10,10&chof=png");
	writer.print("&chxt=x,x,y,y&chxr=0,0.0,1.0,0.1|2,0.0,1.0,0.1&chxl=1:|"+xlabel+"|3:|"+ylabel+"&chma=b&chxp=1,50|3,50&chxs=0N*sz1*|2N*sz1*");
	writer.print("&chd=t:"); // data
	boolean firstalg = true;
	for( Vector<Pair> table : result ) {
	    if ( !firstalg ) writer.print("|");
	    firstalg = false;
	    boolean firstpoint = true;
	    String Yval = "|";
	    for( Pair p : table ) {
		if ( !firstpoint ) {
		    writer.print(",");
		    Yval += ",";
		}
		firstpoint = false;
		Yval += String.format("%1.2f", p.getY()*10);
		//logger.trace( " >> {} - {}", p.getX(), p.getY() );
		writer.printf( "%1.2f", p.getX()*10 );
	    }
	    writer.print( Yval );
	}
	writer.print("&chdl="); // labels
	int i = 0;
	//String marktable[] = { "+", "*", "x", "-", "|", "o", "asterisk", "star", "oplus", "oplus*", "otimes", "otimes*", "square", "square*", "triangle", "triangle*", "diamond", "diamond*", "pentagon", "pentagon*"};
	//String colortable[] = { "black", "red", "green!50!black", "blue", "cyan", "magenta" };
	String colortable[] = { "000000", "ffff00", "ff00ff", "00ffff", "ff0000", "00ff00", "0000ff", "888888", "8888ff", "88ff88", "ff8888", "8800ff", "88ff00", "008800", "ff8800", "0088ff", "000088","ff0088","00ff88", "888800", "880088", "008888", "880000", "008800", "000088", "88ffff", "ff88ff", "ffff88" };
	String style = "";
	String color = "";
	for ( String m : listAlgo ) {
	    if ( i > 0 ) {
		writer.print( "|" );
		color += ",";
		style += "|";
	    }
	    writer.print( m );
	    color += colortable[i%28];
	    if ( !listEvaluators.get(i).isValid() ) {
		style += "2,6,3";
	    } else {
		style += "2";
	    }
	    i++;
	}
	//writer.print("&chdlp=b"); // legend position (but ugly)
	writer.print("&chco="+color); // colors
	writer.print("&chls="+style); // linestyle
	writer.println("&chds=0,10\"/>");
    }

    // 2010: TSV output is not finished
    // It is supposed to provide
    // List of algo
    // List of STEP + points
    public void printTSV( Vector<Vector<Pair>> points, PrintWriter writer ) {
	// Print first line
	for ( String m : listAlgo ) {
	    writer.print("\t"+m );
	}
	// Print others
	for ( int i= 0; i < 100 ; i += STEP ) {
	    for( int j = 0; j < size; j++ ){
		Pair precrec = points.get(j).get(i);
		writer.println( precrec.getX()+" "+precrec.getY() );
	    }
	}
	writer.println();
    }

    public void usage() {
	usage( "java "+this.getClass().getName()+" [options]\nGenerate a graphic presentation of evaluation results" );
    }
}
