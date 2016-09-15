/*
 * $Id: TestGen.java 2065 2015-10-02 09:49:17Z euzenat $
 *
 * Copyright (C) 2011-2015, INRIA
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

import java.util.Properties;
import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import fr.inrialpes.exmo.align.gen.TestGenerator;
import fr.inrialpes.exmo.align.gen.BenchmarkGenerator;
import fr.inrialpes.exmo.align.gen.TestSet;
import fr.inrialpes.exmo.align.gen.ParametersIds;

/** 
    An utility application for generating tests from command line.
    It can either generate a single test or a whole test suite from a single ontology.
    
    <pre>
    java -cp procalign.jar fr.inrialpes.exmo.align.gen.TestGen [options] filename
    </pre>

    where filename is the seed ontology,

*/

public class TestGen extends CommonCLI {
    final static Logger logger = LoggerFactory.getLogger( TestGen.class );

    private String methodName = null;         //the name of the method
    private String fileName   = "onto.rdf";   //the name of the input file
    private String dir        = ".";          //
    private String url;                       //

    public TestGen() {
	super();
	options.addOption( createRequiredOption( "u", "uriprefix", "URI prefix of the target", "URI" ) );
	options.getOption( "uriprefix" ).setRequired( true );
	options.addOption( createRequiredOption( "t", "testset", "Use CLASS for generating the test set", "CLASS" ) );
	options.addOption( createRequiredOption( "w", "outdir", "Output DIRectory (default: current)", "DIR" ) );
	options.addOption( createRequiredOption( "a", "alignname", "FILEname of generated alignment (default: refalign.rdf)", "FILE" ) );
	// We redefine the message for -o
	options.getOption( "output" ).setDescription( "FILEname of the generated ontology (default: "+fileName+")" );
    }

    public static void main(String[] args) {
        try { new TestGen().run( args ); }
        catch ( Exception ex ) { ex.printStackTrace(); };
    }

    public void run(String[] args) throws Exception {
	try { 
	    CommandLine line = parseCommandLine( args );
	    if ( line == null ) return; // --help

	    outputfilename = fileName; // likely useless

	    // Here deal with command specific arguments
	    if ( line.hasOption( 't' ) ) methodName = line.getOptionValue( 't' );
	    if ( line.hasOption( 'o' ) ) parameters.setProperty( "ontoname", line.getOptionValue( 'o' ) );
	    if ( line.hasOption( 'a' ) ) parameters.setProperty( "alignname", line.getOptionValue( 'a' ) );
	    if ( line.hasOption( 'w' ) ) {
		dir = line.getOptionValue( 'w' );
		parameters.setProperty( "outdir", dir );
	    }
	    if ( line.hasOption( 'u' ) ) {
		url = line.getOptionValue( 'u' );
		parameters.setProperty( "urlprefix", url ); // JE: Danger urlprefix/uriprefix
	    }
	    String[] argList = line.getArgs();
	    if ( argList.length > 0 ) {
		fileName = argList[0];
		parameters.setProperty( "filename", fileName );
	    } else {
		logger.error("Require the seed ontology filename");
		usage();
		System.exit(-1);
	    }
	} catch( ParseException exp ) {
	    logger.error( exp.getMessage() );
	    usage();
	    System.exit(-1);
	}

	logger.debug( " >>>> {} from {}", methodName, fileName );

	if ( methodName == null ) { // generate one test
	    TestGenerator tg = new TestGenerator();
	    // It would certainly be better to initialise the generator with parameters
	    tg.setDirPrefix( dir );
	    tg.setURLPrefix( url );
	    if ( parameters.getProperty( "ontoname" ) != null ) tg.setOntoFilename( parameters.getProperty( "ontoname" ) );
	    if ( parameters.getProperty( "alignname" ) != null ) tg.setAlignFilename( parameters.getProperty( "alignname" ) );
	    tg.modifyOntology( fileName, (Properties)null, (String)null, parameters );
	} else { // generate a test set
	    TestSet tset = null;
	    try {
		Class<?> testSetClass = Class.forName( methodName );
		Class<?>[] cparams = {};
		Constructor<?> testSetConstructor = testSetClass.getConstructor(cparams);
		Object[] mparams = {};
		tset = (TestSet)testSetConstructor.newInstance( mparams );
	    } catch (Exception ex) {
		logger.error("Cannot create TestSet {}", methodName );
		logger.error("Caught error", ex );
		usage();
		System.exit(-1);
	    }
	    tset.generate( parameters );
	}
    }

    public void usage() {
	usage( "java "+this.getClass().getName()+" [options] -u uriprefix filename\nGenerate ontology matching tests from the seed ontology file",
	"Such that parameters may be:"+
	       "\tRemove percentage subclasses       \""+ParametersIds.REMOVE_CLASSES+"\""+
	       "\tRemove percentage properties       \""+ParametersIds.REMOVE_PROPERTIES+"\""+
	       "\tRemove percentage comments         \""+ParametersIds.REMOVE_COMMENTS+"\""+
	       "\tRemove percentage restrictions     \""+ParametersIds.REMOVE_RESTRICTIONS+"\""+
	       "\tRemove individuals                 \""+ParametersIds.REMOVE_INDIVIDUALS+"\""+
	       "\tAdd percentage subclasses          \""+ParametersIds.ADD_CLASSES+"\""+
	       "\tAdd percentage properties          \""+ParametersIds.ADD_PROPERTIES+"\""+
	       "\tRename percentage classes          \""+ParametersIds.RENAME_CLASSES+"\""+
	       "\tRename percentage properties       \""+ParametersIds.RENAME_PROPERTIES+"\""+
	       "\tnoHierarchy                        \""+ParametersIds.NO_HIERARCHY+"\""+
	       "\tLevel flattened                    \""+ParametersIds.LEVEL_FLATTENED+"\""+
	       "\tAdd nbClasses to a specific level  \""+ParametersIds.ADD_CLASSESLEVEL+"\""
	       );
    }
}
