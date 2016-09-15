/*
 * $Id: GroupAlign.java 2065 2015-10-02 09:49:17Z euzenat $
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
import fr.inrialpes.exmo.align.parser.AlignmentParser;

import fr.inrialpes.exmo.ontowrap.OntologyFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.lang.Integer;
import java.lang.Long;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Properties;
import java.lang.reflect.Constructor;

import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

/** A batch class for an OWL ontology alignment processing.
    It aligns all the ontology pairs denoted 
    under subdirectory of the current directory.
    
    <pre>
    java -cp procalign.jar fr.inrialpes.exmo.align.util.GroupAlign [options]
    </pre>

    where the options are:
    <pre>
        --alignment=filename -a filename Start from an XML alignment file
	--params=filename -p filename   Read the parameters in file
	--name=filename -n filename output results in filename.rdf
	--impl=className -i classname           Use the given alignment implementation.
	--renderer=className -r className       Specifies the alignment renderer
   </pre>

<pre>
$Id: GroupAlign.java 2065 2015-10-02 09:49:17Z euzenat $
</pre>
*/

public class GroupAlign extends CommonCLI {
    final static Logger logger = LoggerFactory.getLogger( GroupAlign.class );

    String urlprefix = null;
    String source = "onto1.rdf";
    String target = "onto.rdf";
    URI uri1 = null;
    String initName = null;
    String alignmentClassName = "fr.inrialpes.exmo.align.impl.method.StringDistAlignment";
    String rendererClass = "fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor";
    String ontoDir = null;

    public GroupAlign() {
	super();
	options.addOption( createRequiredOption( "a", "alignment", "Use an initial alignment FILE", "FILE" ) );
	options.addOption( createRequiredOption( "r", "renderer", "Use the given CLASS for rendering", "CLASS" ) );
	options.addOption( createRequiredOption( "i", "impl", "Use the given AlignmentProcess implementation", "CLASS" ) );
	options.addOption( createRequiredOption( "n", "name", "Use the given URI as common source ontology", "URI" ) );
	options.addOption( createRequiredOption( "u", "uriprefix", "URI prefix of the target", "URI" ) );
	options.addOption( createRequiredOption( "s", "source", "Source ontology FILEname (default "+source+")", "FILE" ) );
	options.addOption( createRequiredOption( "t", "target", "Target ontology FILEname (default "+target+")", "FILE" ) );
	options.addOption( createRequiredOption( "w", "directory", "The DIRectory containing the data to match", "DIR" ) );
    }

    public static void main(String[] args) {
	try { new GroupAlign().run( args ); }
	catch (Exception ex) { ex.printStackTrace(); };
    }

    public void run(String[] args) throws Exception {

	try { 
	    outputfilename = "align";

	    CommandLine line = parseCommandLine( args );
	    if ( line == null ) return; // --help

	    // Here deal with command specific arguments

	    if ( line.hasOption( 'n' ) ) {
		try { uri1 = new URI( line.getOptionValue('n') );
		} catch ( Exception e ) { 
		    logger.debug( "IGNORED Exception (cannot create source URI)", e );
		}
	    }
	    if ( line.hasOption( 'r' ) ) rendererClass = line.getOptionValue( 'r' );
	    if ( line.hasOption( 'i' ) ) alignmentClassName = line.getOptionValue( 'i' );
	    if ( line.hasOption( 'a' ) ) initName = line.getOptionValue( 'a' );
	    if ( line.hasOption( 'u' ) ) urlprefix = line.getOptionValue( 'u' );
	    if ( line.hasOption( 's' ) ) source = line.getOptionValue( 's' );
	    if ( line.hasOption( 't' ) ) target = line.getOptionValue( 't' );
	    if ( line.hasOption( 'w' ) ) ontoDir = line.getOptionValue( 'w' );
	} catch( ParseException exp ) {
	    logger.error( exp.getMessage() );
	    usage();
	    System.exit(-1);
	}

	iterateDirectories();
    }

    public void iterateDirectories (){
	File [] subdir = null;
	try { 
		if(ontoDir == null){
		    subdir = (new File(System.getProperty("user.dir"))).listFiles(); 
		}
		else{
			subdir = (new File(ontoDir)).listFiles();
		}
	} catch ( Exception e ) {
	    logger.error( "Cannot stat dir", e );
	    usage();
	}
	int size = subdir.length;
	for ( int i=0 ; i < size; i++ ) {
	    if( subdir[i].isDirectory() ) {
		// Align
		//logger.trace("Directory: {}", subdir[i]);
		align( subdir[i] );
	    }
	}
    }

    public void align ( File dir ) {
	String prefix = null;
	AlignmentProcess result = null;
	Alignment init = null;
	PrintWriter writer = null;

	if ( urlprefix != null ){
	    prefix = urlprefix+"/"+dir.getName()+"/";
	} else {
	    // sounds like the only way to have something portable
	    // This is the modification for acomodating the HCONE 
	    prefix = dir.toURI().toString();
	    // The problem is that is brings
	    // file:/localpath
	    // instead of
	    // file://localhost/localpath
	    // Apparently should be file:///c:/localpath
	}
	//logger.trace{}("Here it is {} (end by /?)", prefix );

	try {
	    if ( !source.equalsIgnoreCase("onto1.rdf") 
		 && !target.equalsIgnoreCase("onto1.rdf") ) {
		uri1 = new URI( prefix+source );
	    } else if ( uri1 == null ) uri1 = new URI( prefix+source );
	    URI uri2 = new URI( prefix+target );

	    //logger.trace(" Handler set");
	    //logger.trace(" URI1: {}", uri1);
	    //logger.trace(" URI2: {}", uri2);
	    
	    try {
		if ( initName != null ) {
		    AlignmentParser aparser = new AlignmentParser();
		    init = aparser.parse( prefix+initName );
		    uri1 = init.getFile1();
		    uri2 = init.getFile2();
		    logger.debug(" Init parsed");
		}

		// Create alignment object
		Object[] mparams = {};
		Class<?>[] cparams = {};
		Class<?> alignmentClass = Class.forName( alignmentClassName );
		Constructor<?> alignmentConstructor = alignmentClass.getConstructor(cparams);
		result = (AlignmentProcess)alignmentConstructor.newInstance( mparams );
		result.init( uri1, uri2 );
	    } catch (Exception ex) {
		logger.debug( "Cannot create alignment {}", alignmentClassName );
		throw ex;
	    }

	    logger.debug(" Alignment structure created");

	    // Compute alignment
	    long time = System.currentTimeMillis();
	    result.align( init, parameters );
	    long newTime = System.currentTimeMillis();
	    result.setExtension( Namespace.EXT.uri, Annotations.TIME, Long.toString(newTime - time) );
	    
	    logger.debug(" Alignment performed");
	    
	    // Set output file
	    writer = new PrintWriter (
                         new BufferedWriter(
                             new OutputStreamWriter( 
                                 new FileOutputStream(dir+File.separator+outputfilename), "UTF-8" )), true);
	    AlignmentVisitor renderer = null;

	    try {
		Class<?>[] cparams = { PrintWriter.class };
		Constructor<?> rendererConstructor =
		    Class.forName(rendererClass).getConstructor(cparams);
		Object[] mparams = { (Object)writer };
		renderer = (AlignmentVisitor)rendererConstructor.newInstance(mparams);
	    } catch (Exception ex) {
		logger.debug( "Cannot create renderer {}", rendererClass );
		throw ex;
	    }

	    logger.debug(" Outputing result to {}/{}", dir, outputfilename );

	    // Output
	    result.render( renderer);

	    logger.debug(" Done...{}", renderer );
	} catch (Exception ex) {
	    logger.debug( "IGNORED Exception", ex );
	} finally {
	    // JE: This instruction is very important
	    if ( writer != null ) writer.close();
	    // Unload the ontologies
	    try { OntologyFactory.clear(); 
	    } catch (Exception e) {
		logger.debug( "IGNORED Exception on close", e );
	    };
	}
    }

    public void usage() {
	usage( "java "+this.getClass().getName()+" [options]\nMatches pairs of ontologies in subdirectories" );
    }
}
