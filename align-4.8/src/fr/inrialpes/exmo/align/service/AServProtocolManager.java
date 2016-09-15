/*
 * $id: AServProtocolManager.java 1902 2014-03-17 19:39:04Z euzenat $
 *
 * Copyright (C) INRIA, 2006-2015
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
 *
 */

package fr.inrialpes.exmo.align.service;

import fr.inrialpes.exmo.align.parser.AlignmentParser;
import fr.inrialpes.exmo.align.parser.SyntaxElement;
import fr.inrialpes.exmo.align.impl.Annotations;
import fr.inrialpes.exmo.align.impl.BasicOntologyNetwork;
import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.eval.DiffEvaluator;
import fr.inrialpes.exmo.align.impl.rel.EquivRelation;
import fr.inrialpes.exmo.align.service.osgi.Service;
import fr.inrialpes.exmo.align.service.msg.Message;
import fr.inrialpes.exmo.align.service.msg.AlignmentId;
import fr.inrialpes.exmo.align.service.msg.AlignmentIds;
import fr.inrialpes.exmo.align.service.msg.AlignmentMetadata;
import fr.inrialpes.exmo.align.service.msg.EntityList;
import fr.inrialpes.exmo.align.service.msg.EvalResult;
import fr.inrialpes.exmo.align.service.msg.OntologyNetworkId;
import fr.inrialpes.exmo.align.service.msg.OntologyURI;
import fr.inrialpes.exmo.align.service.msg.RenderedAlignment;
import fr.inrialpes.exmo.align.service.msg.RenderedNetwork;
import fr.inrialpes.exmo.align.service.msg.TranslatedMessage;
import fr.inrialpes.exmo.align.service.msg.ErrorMsg;
import fr.inrialpes.exmo.align.service.msg.NonConformParameters;
import fr.inrialpes.exmo.align.service.msg.RunTimeError;
import fr.inrialpes.exmo.align.service.msg.UnknownAlignment;
import fr.inrialpes.exmo.align.service.msg.UnknownMethod;
import fr.inrialpes.exmo.align.service.msg.UnknownOntologyNetwork;
import fr.inrialpes.exmo.align.service.msg.UnreachableAlignment;
import fr.inrialpes.exmo.align.service.msg.UnreachableOntology;
import fr.inrialpes.exmo.align.service.msg.CannotRenderAlignment;
import fr.inrialpes.exmo.align.service.msg.CannotStoreAlignment;
import fr.inrialpes.exmo.align.service.msg.UnreachableOntologyNetwork;

import fr.inrialpes.exmo.ontowrap.OntologyFactory;
import fr.inrialpes.exmo.ontowrap.Ontology;
import fr.inrialpes.exmo.ontowrap.LoadedOntology;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Evaluator;
import org.semanticweb.owl.align.OntologyNetwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.lang.ClassNotFoundException;
import java.lang.NoClassDefFoundError;
import java.lang.InstantiationException;
import java.lang.NoSuchMethodException;
import java.lang.IllegalAccessException;
import java.lang.NullPointerException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.HashSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes.Name;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * This is the main class which controls the behaviour of the Alignment Server
 * It is as independent from the OWL API as possible.
 * However, it is still necessary to test for the reachability of an ontology and moreover to resolve its URI for that of its source.
 * For these reasons we still need a parser of OWL files here.
 */

public class AServProtocolManager implements Service {
    final static Logger logger = LoggerFactory.getLogger( AServProtocolManager.class );

    Cache alignmentCache = null;
    Properties commandLineParams = null;
    Set<String> renderers = null;
    Set<String> methods = null;
    Set<String> services = null;
    Set<String> evaluators = null;

    Hashtable<String,Directory> directories = null;

    // This should be stored somewhere
    int localId = 0; // surrogate of emitted messages
    String serverId = null; // id of this alignment server

    /*********************************************************************
     * Initialization and constructor
     *********************************************************************/

    public AServProtocolManager ( Hashtable<String,Directory> dir ) {
	directories = dir;
    }

    public void init( DBService connection, Properties prop ) throws AlignmentException {
	commandLineParams = prop;
	serverId = prop.getProperty("prefix");
	if ( serverId == null || serverId.equals("") )
	    serverId = "http://"+prop.getProperty("host")+":"+prop.getProperty("http");
	alignmentCache = new SQLCache( connection );
	alignmentCache.init( prop, serverId );
	renderers = implementations( AlignmentVisitor.class );
	methods = implementations( AlignmentProcess.class );
	methods.remove( "fr.inrialpes.exmo.align.impl.DistanceAlignment" ); // this one is generic, but not abstract
	services = implementations( AlignmentServiceProfile.class );
	evaluators = implementations( Evaluator.class );
    }

    public void close() {
	try { alignmentCache.close(); }
	catch (AlignmentException alex) { 
	    logger.trace( "IGNORED Exception", alex );
	}
    }

    public void reset() {
	try {
	    alignmentCache.reset();
	} catch (AlignmentException alex) {
	    logger.trace( "IGNORED Exception", alex );
	}
    }

    public void flush() {
	alignmentCache.flushCache();
    }

    public void shutdown() {
	try { 
	    alignmentCache.close();
	    System.exit(0);
	} catch (AlignmentException alex) {
	    logger.trace( "IGNORED Exception", alex );
	}
    }

    private int newId() { return localId++; }

    /*********************************************************************
     * Extra administration primitives
     *********************************************************************/

    public Set<String> listmethods (){
	return methods;
    }

    public Set<String> listrenderers(){
	return renderers;
    }

    public Set<String> listservices(){
	return services;
    }

    public Set<String> listevaluators(){
	return evaluators;
    }

    /*
    public Enumeration alignments(){
	return alignmentCache.listAlignments();
    }
    */
    public Collection<Alignment> alignments() {
	return alignmentCache.alignments();
    }

    public Collection<URI> ontologies() {
	return alignmentCache.ontologies();
    }
    
    public Collection<Alignment> alignments( URI uri1, URI uri2 ) {
	return alignmentCache.alignments( uri1, uri2 );
    }
    
    public Collection<URI> ontologyNetworkUris() {
    return ((VolatilCache) alignmentCache).ontologyNetworkUris();
    }

    public Collection<OntologyNetwork> ontologyNetworks() {
    	return alignmentCache.ontologyNetworks();
    }
 
    public String query( String query ){
	//return alignmentCache.query( query );
	return "Not available yet";
    }

    public String serverURL(){
	return serverId;
    }

    public String argline(){
	return commandLineParams.getProperty( "argline" );
    }

   /*********************************************************************
     * Basic protocol primitives
     *********************************************************************/

    // DONE
    // Implements: store (different from store below)
    public Message load( Properties params ) {
	// load the alignment
	String name = params.getProperty("url");
	String file = null;
	if ( name == null || name.equals("") ){
	    file  = params.getProperty("filename");
	    if ( file != null && !file.equals("") ) name = "file://"+file;
	}
	//logger.trace("Preparing for loading {}", name);
	Alignment al = null;
	try {
	    //logger.trace(" Parsing alignment");
	    AlignmentParser aparser = new AlignmentParser();
	    al = aparser.parse( name );
	    //logger.trace(" Alignment parsed");
	} catch (Exception e) {
	    return new UnreachableAlignment( params, newId(), serverId,name );
	}
	// We preserve the pretty tag within the loaded ontology
	String pretty = al.getExtension( Namespace.EXT.uri, Annotations.PRETTY );
	if ( pretty == null ) pretty = params.getProperty("pretty");
	if ( pretty != null && !pretty.equals("") ) {
	    al.setExtension( Namespace.EXT.uri, Annotations.PRETTY, pretty );
	}
	boolean force = false;
	String rewrite = params.getProperty("force");
	if ( rewrite != null && !rewrite.equals("") ) force = true;
	// register it
	String id = alignmentCache.recordNewAlignment( al, force );
	// if the file has been uploaded: discard it
	if ( params.getProperty("todiscard") != null ) {
	    try {
		File f = new File( name );
		f.delete();
	    } catch ( Exception ex ) {
		logger.debug( "IGNORED EXCEPTION : {}", ex );
	    }
	}
	return new AlignmentId( params, newId(), serverId, id ,pretty );
    }

    // Implements: align
    @SuppressWarnings( "unchecked" )
    public Message align( Properties params ){
	Message result = null;
	// These are added to the parameters wich are in the message
	for ( Entry<Object,Object> e : commandLineParams.entrySet() ) {
	    if ( params.getProperty( (String)e.getKey() ) == null ) params.setProperty( (String)e.getKey(), (String)e.getValue() );
	}
	// Do the fast part (retrieve)
	result = retrieveAlignment( params );
	if ( result != null ) return result;
	String uri = alignmentCache.generateAlignmentUri();

	Aligner althread = new Aligner( params, uri );
	Thread th = new Thread(althread);
	// Do the slow part (align)
	if ( params.getProperty("async") != null ) {
	    th.start();
	    // Parameters are used
	    return new AlignmentId( params, newId(), serverId, uri );
	} else {
	    th.start();
	    try{ th.join(); }
	    catch ( InterruptedException is ) {
		return new ErrorMsg( params, newId(), serverId,"Interrupted exception" );
	    };
	    return althread.getResult();
	}
    }

    /**
     * returns null if alignment not retrieved
     * Otherwise returns AlignmentId or an ErrorMsg
     */
    private Message retrieveAlignment( Properties params ){
	String method = params.getProperty("method");
	// find and access o, o'
	URI uri1 = null;
	URI uri2 = null;
	try {
	    uri1 = new URI( params.getProperty("onto1"));
	    uri2 = new URI( params.getProperty("onto2"));
	} catch (Exception e) {
	    return new NonConformParameters( params, newId(), serverId,"nonconform/params/onto" );
	};
	Set<Alignment> alignments = alignmentCache.getAlignments( uri1, uri2 );
	if ( alignments != null && params.getProperty("force") == null ) {
	    for ( Alignment al: alignments ){
		String meth2 = al.getExtension( Namespace.EXT.uri, Annotations.METHOD );
		if ( meth2 != null && meth2.equals(method) ) {
		    return new AlignmentId( params, newId(), serverId,
					   al.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID ) ,
					   al.getExtension( Namespace.EXT.uri, Annotations.PRETTY ) );
		}
	    }
	}
	return (Message)null;
    }

    // DONE
    // Implements: query-aligned
    public Message existingAlignments( Properties params ){
	// find and access o, o'
	String onto1 = params.getProperty("onto1");
	String onto2 = params.getProperty("onto2");
	URI uri1 = null;
	URI uri2 = null;
	Set<Alignment> alignments = null;
	try {
	    if( onto1 != null && !onto1.equals("") ) {
		uri1 = new URI( onto1 );
	    }
	    if ( onto2 != null && !onto2.equals("") ) {
		uri2 = new URI( onto2 );
	    }
	    alignments = alignmentCache.getAlignments( uri1, uri2 );
	} catch (Exception e) {
	    return new ErrorMsg( params, newId(), serverId,"MalformedURI problem" );
	}; //done below
	String msg = " ";
	String prettys = ":";
	if ( alignments != null ) {
	    for ( Alignment al : alignments ) {
		msg += al.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID )+" ";
		prettys += al.getExtension( Namespace.EXT.uri, Annotations.PRETTY )+ ":";
	    }
	}
	return new AlignmentIds( params, newId(), serverId, msg, prettys );
    }

    // DONE
    // Implements: query-aligned
    public Message getAlignments( Properties params ){
	String uri = params.getProperty("uri");
	String desc = params.getProperty("desc");
	Set<Alignment> alignments = null;
	try {
	    if ( uri != null && !uri.equals("") ) alignments = alignmentCache.getAlignmentByURI( uri );
	    if ( desc != null && alignments == null && !desc.equals("") ) { // Then try the description
		alignments = alignmentCache.getAlignmentsByDescription( desc );
	    }
	} catch ( Exception ex ) {
	    return new ErrorMsg( params, newId(), serverId, "Exception raised" );
	}; //done below
	String msg = " ";
	String prettys = ":";
	if ( alignments != null ) {
	    for ( Alignment al : alignments ) {
		msg += al.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID )+" ";
		prettys += al.getExtension( Namespace.EXT.uri, Annotations.PRETTY )+ ":";
	    }
	}
	return new AlignmentIds( params, newId(), serverId, msg, prettys );
    }

    public Message findCorrespondences( Properties params ) {
	// Retrieve the alignment
	Alignment al = null;
	String id = params.getProperty("id");
	try {
	    al = alignmentCache.getAlignment( id );
	} catch (Exception e) {
	    return new UnknownAlignment( params, newId(), serverId,id );
	}
	// Find matched
	URI uri = null;
	try {
	    uri = new URI( params.getProperty("entity") );
	} catch (Exception e) {
	    return new ErrorMsg( params, newId(), serverId,"MalformedURI problem" );
	};
	// Retrieve correspondences
	String msg = params.getProperty("strict");
	boolean strict = ( msg != null && !msg.equals("0") && !msg.equals("false") && !msg.equals("no") );
	msg = "";
	try {
	    Set<Cell> cells = null;
	    if ( al instanceof ObjectAlignment ) {
		LoadedOntology<Object> onto = ((ObjectAlignment)al).getOntologyObject1();
		Object obj1 = onto.getEntity( uri );
		cells = al.getAlignCells1( obj1 );
	    } else if ( al instanceof URIAlignment ) {
		cells = ((URIAlignment)al).getAlignCells1( uri );
	    } else if ( al instanceof BasicAlignment ) {
		cells = al.getAlignCells1( uri );
	    }
	    if ( cells != null ) {
		for ( Cell c : cells ) {
		    if ( !strict || c.getRelation() instanceof EquivRelation ) {
			msg += c.getObject2AsURI( al )+" ";
		    }
		}
	    }
	} catch ( AlignmentException alex ) { // should never happen
	    return new ErrorMsg( params, newId(), serverId,"Unexpected Alignment API Error" );
	} catch ( OntowrapException owex ) {
	    return new ErrorMsg( params, newId(), serverId,"Cannot find entity" );
	}
	return new EntityList( params, newId(), serverId, msg );
    }

    // ABSOLUTELY NOT IMPLEMENTED
    // But look at existingAlignments
    // Implements: find
    // This may be useful when calling WATSON
    public Message find(Properties params){
    //\prul{search-success}{a --request ( find (O, T) )--> S}{O' <= Match(O,T); S --inform (O')--> a}{reachable(O) & Match(O,T)!=null}
    //\prul{search-void}{a - request ( find (O, T) ) \rightarrow S}{S - failure (nomatch) \rightarrow a}{reachable(O)\wedge Match(O,T)=\emptyset}
    //\prul{search-unreachable}{a - request ( find (O, T) ) \rightarrow S}{S - failure ( unreachable (O) ) \rightarrow a}{\neg reachable(O)}
	return new OntologyURI( params, newId(), serverId,"Find not implemented" );
    }

    // Implements: translate
    // This should be applied to many more kind of messages with different kind of translation
    public Message translate(Properties params){
	// Retrieve the alignment
	String id = params.getProperty("id");
	BasicAlignment al = null;
	try {
	    // JE:This one is risky
	    al = (BasicAlignment)alignmentCache.getAlignment( id );
	} catch (Exception e) {
	    return new UnknownAlignment( params, newId(), serverId,id );
	}
	// Translate the query
	try {
	    String translation = al.rewriteSPARQLQuery( params.getProperty("query") );
	    return new TranslatedMessage( params, newId(), serverId,translation );
	} catch (AlignmentException e) {
	    return new ErrorMsg( params, newId(), serverId,e.toString() );
	}
    }

    // DONE
    // Implements: render
    public Message render( Properties params ){
	// Retrieve the alignment
	String id = params.getProperty( "id" );
	Alignment al = null;
	try {
	    logger.trace("Alignment sought for {}", id);
	    al = alignmentCache.getAlignment( id );
	    logger.trace("Alignment found");
	} catch (Exception e) {
	    logger.debug( "Alignment {} not found", id, e );
	    return new UnknownAlignment( params, newId(), serverId,id );
	}
	// Render it
	String method = params.getProperty("method");
	PrintWriter writer = null;
	// Redirect the output in a String
	ByteArrayOutputStream result = new ByteArrayOutputStream(); 
	try { 
	    writer = new PrintWriter (
			  new BufferedWriter(
			       new OutputStreamWriter( result, "UTF-8" )), true);
	    AlignmentVisitor renderer = null;
	    try {
		Class<?>[] cparams = { PrintWriter.class };
		Constructor<?> rendererConstructor = Class.forName( method ).getConstructor( cparams );
		Object[] mparams = { (Object)writer };
		renderer = (AlignmentVisitor) rendererConstructor.newInstance( mparams );
	    } catch ( ClassNotFoundException cnfex ) {
		// should return the message
		logger.error( "Unknown method", cnfex );
		return new UnknownMethod( params, newId(), serverId,method );
	    }
	    renderer.init( params );
	    al.render( renderer );
	} catch ( AlignmentException e ) {
	    return new CannotRenderAlignment( params, newId(), serverId,id );
	} catch ( Exception e ) { // These are exceptions related to I/O
	    writer.flush();
	    //logger.trace( "Resulting rendering : {}", result.toString() );
	    logger.error( "Cannot render alignment", e );
	    return new Message( params, newId(), serverId,"Failed to render alignment" );
	} finally {
	    writer.flush();
	    writer.close();
	}
	return new RenderedAlignment( params, newId(), serverId, result.toString() );
    }


    /*********************************************************************
     * Extended protocol primitives
     *********************************************************************/

    // Implementation specific
    public Message store( Properties params ) {
	String id = params.getProperty("id");
	Alignment al = null;
	try {
	    try {
	    	al = alignmentCache.getAlignment( id );
	    } catch( Exception ex ) {
	    	logger.warn( "Unknown Id {} in Store", id );
		return new UnknownAlignment( params, newId(), serverId, id );
	    }
	    // Be sure it is not already stored
	    if ( !alignmentCache.isAlignmentStored( al ) ) {
		alignmentCache.storeAlignment( id );
		// Retrieve the alignment again
		al = alignmentCache.getAlignment( id );
		// for all directories...
		for ( Directory d : directories.values() ){
		    // Declare the alignment in the directory
		    try { d.register( al ); }
		    catch ( AServException e ) {
			logger.debug( "IGNORED Exception in alignment registering", e );
		    }
		}
	    }
	    // register by them
	    // Could also be an AlreadyStoredAlignment error
	    return new AlignmentId( params, newId(), serverId, id,
				   al.getExtension( Namespace.EXT.uri, Annotations.PRETTY ));
	} catch ( Exception ex ) {
	    logger.debug( "Impossible storage ", ex );
	    return new CannotStoreAlignment( params, newId(), serverId, id );
	}
    }

    // Implementation specific
    public Message erase( Properties params ) {
	String id = params.getProperty("id");
	Alignment al = null;
	try {
	    al = alignmentCache.getAlignment( id );
	    // Erase it from directories
	    for ( Directory d : directories.values() ){
		try { d.register( al ); }
		catch ( AServException e ) { 
		    logger.debug( "IGNORED Cannot register alignment", e );
		}
	    }
	    // Erase it from storage
	    try {
		alignmentCache.eraseAlignment( id, true );
	    } catch ( Exception ex ) {
		logger.debug( "IGNORED Cannot erase alignment", ex );
	    }
	    // Should be a SuppressedAlignment
	    return new AlignmentId( params, newId(), serverId, id ,
				   al.getExtension( Namespace.EXT.uri, Annotations.PRETTY ));
	} catch ( Exception ex ) {
	    return new UnknownAlignment( params, newId(), serverId,id );
	}
    }

    /*
     * Returns only the metadata of an alignment and returns it in 
     * parameters
     */
    public Message metadata( Properties params ){
	// Retrieve the alignment
	String id = params.getProperty("id");
	Alignment al = null;
	try {
	    al = alignmentCache.getMetadata( id );
	} catch (Exception e) {
	    return new UnknownAlignment( params, newId(), serverId,id );
	}
	// JE: Other possibility is to render the metadata through XMLMetadataRendererVisitor into content...
	// Put all the local metadata in parameters
	Properties p = new Properties();
	p.setProperty( "file1", al.getFile1().toString() );
	p.setProperty( "file2", al.getFile2().toString() );
	p.setProperty( Namespace.ALIGNMENT.uri+"#level", al.getLevel() );
	p.setProperty( Namespace.ALIGNMENT.uri+"#type", al.getType() );
	for ( String[] ext : al.getExtensions() ){
	    p.setProperty( ext[0]+ext[1], ext[2] );
	}
	return new AlignmentMetadata( params, newId(), serverId, id, p );
    }

    /*********************************************************************
     * Extra alignment primitives
     *
     * All these primitives must create a new alignment and return its Id
     * There is no way an alignment server could modify an alignment
     *********************************************************************/

    public Message trim( Properties params ) {
	// Retrieve the alignment
	String id = params.getProperty("id");
	Alignment al = null;
	try {
	    al = alignmentCache.getAlignment( id );
	} catch (Exception e) {
	    return new UnknownAlignment( params, newId(), serverId,id );
	}
	// get the trim parameters
	String type = params.getProperty("type");
	if ( type == null ) type = "hard";
	double threshold = Double.parseDouble( params.getProperty("threshold"));
	al = (BasicAlignment)((BasicAlignment)al).clone();
	try { al.cut( type, threshold );}
	catch (AlignmentException e) {
	    return new ErrorMsg( params, newId(), serverId,e.toString() );
	}
	String pretty = al.getExtension( Namespace.EXT.uri, Annotations.PRETTY );
	if ( pretty != null ){
	    al.setExtension( Namespace.EXT.uri, Annotations.PRETTY, pretty+"/trimmed "+threshold );
	};
	String newId = alignmentCache.recordNewAlignment( al, true );
	return new AlignmentId( params, newId(), serverId, newId,
			       al.getExtension( Namespace.EXT.uri, Annotations.PRETTY ));
    }

    public Message harden( Properties params ){
	return new NonConformParameters( params, newId(), serverId, "Harden not implemented" );
    }

    public Message inverse( Properties params ){
	// Retrieve the alignment
	String id = params.getProperty("id");
	Alignment al = null;
	try {
	    al = alignmentCache.getAlignment( id );
	} catch (Exception e) {
	    return new UnknownAlignment( params, newId(), serverId,"unknown/Alignment/"+id );
	}

	// Invert it
	try { al = al.inverse(); }
	catch (AlignmentException e) {
	    return new ErrorMsg( params, newId(), serverId,e.toString() );
	}
	String newId = alignmentCache.recordNewAlignment( al, true );
	return new AlignmentId( params, newId(), serverId, newId,
			       al.getExtension( Namespace.EXT.uri, Annotations.PRETTY ));
    }

    public Message meet( Properties params ){
	// Retrieve alignments
	return new NonConformParameters( params, newId(), serverId, "Meet not available" );
    }

    public Message join( Properties params ){
	// Retrieve alignments
	return new NonConformParameters( params, newId(), serverId, "Join not available" );
    }

    public Message compose( Properties params ){
	// Retrieve alignments
	return new NonConformParameters( params, newId(), serverId, "Compose not available" );
    }

    public Message eval( Properties params ){
	// Retrieve the alignment
	String id = params.getProperty("id");
	Alignment al = null;
	try {
	    al = alignmentCache.getAlignment( id );
	} catch (Exception e) {
	    return new UnknownAlignment( params, newId(), serverId,"unknown/Alignment/"+id );
	}
	// Retrieve the reference alignment
	String rid = params.getProperty("ref");
	Alignment ref = null;
	try {
	    ref = alignmentCache.getAlignment( rid );
	} catch (Exception e) {
	    return new UnknownAlignment( params, newId(), serverId,"unknown/Alignment/"+rid );
	}
	// Set the comparison method
	String classname = params.getProperty("method");
	if ( classname == null ) classname = "fr.inrialpes.exmo.align.impl.eval.PRecEvaluator";
	Evaluator eval = null;
	try {
	    Class<?>[] cparams = { Alignment.class, Alignment.class };
	    Class<?> evaluatorClass = Class.forName( classname );
	    Constructor<?> evaluatorConstructor = evaluatorClass.getConstructor( cparams );
	    Object [] mparams = { (Object)ref, (Object)al };
	    eval = (Evaluator)evaluatorConstructor.newInstance( mparams );
	} catch ( ClassNotFoundException cnfex ) {
	    logger.error( "Unknown method", cnfex );
	    return new UnknownMethod( params, newId(), serverId,classname );
	} catch ( InvocationTargetException itex ) {
	    String msg = itex.toString();
	    if ( itex.getCause() != null ) msg = itex.getCause().toString();
	    return new ErrorMsg( params, newId(), serverId,msg );
	} catch ( Exception ex ) {
	    return new ErrorMsg( params, newId(), serverId,ex.toString() );
	}
	// Compare it
	try { eval.eval( params); }
	catch ( AlignmentException e ) {
	    return new ErrorMsg( params, newId(), serverId,e.toString() );
	}
	// Could also be EvaluationId if we develop a more elaborate evaluation description
	return new EvalResult( params, newId(), serverId, classname, eval.getResults() );
    }

    public Message diff( Properties params ){
	// Retrieve the alignment
	String id1 = params.getProperty("id1");
	Alignment al1 = null;
	try {
	    al1 = alignmentCache.getAlignment( id1 );
	} catch (Exception e) {
	    return new UnknownAlignment( params, newId(), serverId,"unknown/Alignment/"+id1 );
	}
	// Retrieve the reference alignment
	String id2 = params.getProperty("id2");
	Alignment al2 = null;
	try {
	    al2 = alignmentCache.getAlignment( id2 );
	} catch (Exception e) {
	    return new UnknownAlignment( params, newId(), serverId,"unknown/Alignment/"+id2 );
	}
	try { 
	    DiffEvaluator diff = new DiffEvaluator( al1, al2 );
	    diff.eval( params ); 
	    // This will only work with HTML
	    return new EvalResult( params, newId(), serverId, diff.HTMLString(), (Properties)null );
	} catch (AlignmentException e) {
	    return new ErrorMsg( params, newId(), serverId,e.toString() );
	}
    }

    /**
     * Store evaluation result from its URI
     */
    public Message storeEval( Properties params ){
	return new ErrorMsg( params, newId(), serverId,"Not yet implemented" );
    }

    /**
     * Evaluate a track: a set of results
     */
    // It is also possible to try a groupeval ~> with a zipfile containing results
    //            ~~> But it is more difficult to know where is the reference (non public)
    // There should also be options for selecting the result display
    //            ~~> PRGraph (but this may be a Evaluator)
    //            ~~> Triangle
    //            ~~> Cross
    public Message groupEval( Properties params ){
	return new ErrorMsg( params, newId(), serverId,"Not yet implemented" );
    }

    /**
     * Store the result
     */
    public Message storeGroupEval( Properties params ){
	return new ErrorMsg( params, newId(), serverId,"Not yet implemented" );
    }

    /**
     * Retrieve the results (all registered result) of a particular test
     */
    public Message getResults( Properties params ){
	return new ErrorMsg( params, newId(), serverId,"Not yet implemented" );
    }

    /*
      public boolean storedAlignment( Properties params ) {
	// Retrieve the alignment
	String id = params.getProperty("id");
	Alignment al = null;
	try {
	    al = alignmentCache.getAlignment( id );
	} catch (Exception e) {
	    return false;
	}
	return alignmentCache.isAlignmentStored( al );
	}*/

    // This only an indirection
    public boolean storedAlignment( Alignment al ) {
	return alignmentCache.isAlignmentStored( al );
    }

    public boolean storedNetwork( OntologyNetwork on ) {
	return alignmentCache.isNetworkStored( on );
    }


    /********************************************************************************************************
     * Ontology Networks
     */
    
    public Message loadOntologyNetwork( Properties params ) {
	// load the ontology network
	String name = params.getProperty("url");
	String file = null;
	if ( name == null || name.equals("") ){
	    file  = params.getProperty("filename");
	    if ( file != null && !file.equals("") ) name = "file://"+file;
	}
	logger.trace("Preparing for loading {}", name);
	BasicOntologyNetwork noo = null;
	try {
	    noo = (BasicOntologyNetwork) BasicOntologyNetwork.read( name );
	    logger.trace(" Ontology network parsed");
	} catch (Exception e) {
	    return new UnreachableOntologyNetwork( params, newId(), serverId, name );
	}
	// We preserve the pretty tag within the loaded ontology network
	String pretty = noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY ); 
	if ( pretty == null ) pretty = params.getProperty("pretty");
	if ( pretty != null && !pretty.equals("") ) {
	    noo.setExtension( Namespace.EXT.uri, Annotations.PRETTY, pretty );
	}
	// register it
	String id = registerNetwork( noo, null );
	logger.debug(" Ontology network loaded, id: {} total ontologies: {} total alignments: {}",id, noo.getOntologies().size(),noo.getAlignments().size());
	return new OntologyNetworkId( params, newId(), serverId, id, pretty );
    }

    
    public Message renderOntologyNetwork( Properties params ) {
	// fine
    	BasicOntologyNetwork noo = null;
    	String id = params.getProperty( "id" );
    	try {
	    noo = (BasicOntologyNetwork)alignmentCache.getOntologyNetwork(id);
	} catch (AlignmentException e) {
	    return new UnreachableOntologyNetwork( params, newId(), serverId, id );
	}
	// Print it in a string	 
	noo.setIndentString( "  " );
	noo.setNewLineString( System.getProperty("line.separator") );
	ByteArrayOutputStream result = new ByteArrayOutputStream(); 
	PrintWriter writer = null;
	try {
	    writer = new PrintWriter (
				      new BufferedWriter(
							 new OutputStreamWriter( result, "UTF-8" )), true);
	} catch (UnsupportedEncodingException e) {
	    return new ErrorMsg( params, newId(), serverId,"Network encoding error" );
	}
	try {
	    noo.write( writer );
	} catch (AlignmentException e) {
	    return new ErrorMsg( params, newId(), serverId,"Network writing error" );
	} finally {
	    writer.flush();
	    writer.close();
	}
	return new RenderedNetwork( params, newId(), serverId, result.toString() );
    }

    public Message renderHTMLNetwork( Properties params ){ //called by HTTPTransport
    	// Retrieve the alignment
    	String result = new String();
    	String idON = new String();
    	String pidON = new String();
    	String id = params.getProperty( "id" );
    	BasicOntologyNetwork noo = null;
    	try {
    	    logger.trace("Network sought for {}", id);
    	    noo = (BasicOntologyNetwork) alignmentCache.getOntologyNetwork(id);
    	    idON = noo.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID );
	    pidON = noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY );
    	    logger.trace("Network found");
    	} catch (Exception e) {
    	    return new UnknownOntologyNetwork( params, newId(), serverId,id );
	}
	result = "<h1>" + id+ " ("+pidON+")" +"</h1>";
	result += "<table border=\"0\">\n";
	result += "<h2>Network metadata</h2>\n<table border=\"0\">\n";
	for ( String[] ext : noo.getExtensions() ){
	    result += "<tr><td>"+ext[0]+" : "+ext[1]+"</td><td>"+ext[2]+"</td></tr>\n";
	}
    	Collection<URI> networkOntology = noo.getOntologies();
	result += "</table>\n<h2>Ontologies ("+networkOntology.size()+")</h2>\n";
	result += "<ul>";
	for ( URI onto : networkOntology ) {
	    result += "<li><a href=\"" + onto.toString() +"\">"+ onto.toString() + "</a></li>";
	}
	result += "</ul>";
	
	Set<Alignment> networkAlignments = noo.getAlignments();
    	result += "<h2>Alignments ("+networkAlignments.size()+")</h2>\n"; 
	result += "<ul>";
	for (Alignment al : networkAlignments) {	
	    String idAl = al.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID );
	    String pidAl = al.getExtension( Namespace.EXT.uri, Annotations.PRETTY );
	    if ( pidAl == null ) pidAl = idAl; else pidAl = idAl+" ("+pidAl+")";
	    result += "<li><a href=\""+idAl+"\">"+pidAl+"</a></li>";
	}
	result += "</ul>";
	// was renderedAlignement!
    	return new RenderedNetwork( params, newId(), serverId, result );
    }
    
    public Message storeOntologyNetwork( Properties params ) {
    	String id = params.getProperty("id");
      	OntologyNetwork noo = null;
    	try {
    	    noo = alignmentCache.getOntologyNetwork( id );
    	    // Be sure it is not already stored
    	    if ( !alignmentCache.isNetworkStored(noo) ) {
		try {
		    alignmentCache.storeOntologyNetwork( id );
		} catch (AlignmentException e) {
		    return new UnknownOntologyNetwork( params, newId(), serverId,id );
		}
		return new OntologyNetworkId( params, newId(), serverId, id,
					      ((BasicOntologyNetwork) noo).getExtension( Namespace.EXT.uri, Annotations.PRETTY ));
    	    } else {
    	    	return new ErrorMsg( params, newId(), serverId,"Network already stored" );
    	    }
    	} catch (Exception e) {
    	    return new UnknownOntologyNetwork( params, newId(), serverId,id );
    	}
    }

    public Message matchOntologyNetwork( Properties params ) {
      	//parameters: onID, method, reflexive, symmetric 	
       	BasicOntologyNetwork noo = null;
    	boolean reflexive = false;
    	boolean symmetric = false;
    	if (params.getProperty("reflexive") != null) reflexive = true;
    	if (params.getProperty("symmetric") != null) symmetric = true;
    	String id = params.getProperty("id");
    	String method = params.getProperty("method");
	// find
    	try {
	    noo = (BasicOntologyNetwork)alignmentCache.getOntologyNetwork( id );
	} catch ( AlignmentException alex ) {
	    return new UnknownOntologyNetwork( params, newId(), serverId,id );
	}
	// always clone
	String pretty = params.getProperty("pretty");
	if ( pretty == null || pretty.equals("") ) {
	    pretty = noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY ) + "/matched";
	}
	noo = noo.clone();
	noo.setExtension( Namespace.EXT.uri, Annotations.PRETTY, pretty );
	// match
    	try { 
	    noo.match( method, reflexive, symmetric, params );
	} catch ( AlignmentException alex ) {
	    return new ErrorMsg( params, newId(), serverId, "Network alignment error" );
	}
	// register
	String newid = registerNetwork( noo, null );
    	logger.debug(" Network alignments results, id: {} total ontologies: {} total alignments: {}",id, noo.getOntologies().size(),noo.getAlignments().size());
    	return new OntologyNetworkId( params, newId(), serverId, newid,
				      noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY ));
    }
    
    public Message trimOntologyNetwork( Properties params ) {
    	BasicOntologyNetwork noo = null;
    	String id = params.getProperty("id");
    	String method = params.getProperty("type");
    	double threshold = Double.parseDouble(params.getProperty("threshold"));
	// find
    	try {
	    noo = (BasicOntologyNetwork)alignmentCache.getOntologyNetwork( id );
	} catch ( AlignmentException e ) {
	    return new UnknownOntologyNetwork( params, newId(), serverId,id );
	}
	// clone
    	String pretty = params.getProperty("pretty");
	if ( pretty == null || pretty.equals("") ) {
	    pretty = noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY ) + "/trimmed";
	}
	noo = noo.clone();
	// trim
    	try {
	    noo.trim( method, threshold );
	    noo.setExtension( Namespace.EXT.uri, Annotations.PRETTY, pretty );
	} catch (AlignmentException e) {
    	    return new ErrorMsg( params, newId(), serverId,"Network alignment error" );
    	}
	// register
	String newid = registerNetwork( noo, null );
    	logger.debug(" Ontology network trimmed from id: {}, to new id: {} total ontologies: {} total alignments: {}",id, newid, noo.getOntologies().size(),noo.getAlignments().size());	
    	return new OntologyNetworkId( params, newId(), serverId, newid,
				   noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY ));
    }
   
    public Message closeOntologyNetwork( Properties params ) {
	BasicOntologyNetwork noo = null;
	boolean sym = false;
	boolean trans = false;
	boolean refl = false;
	String id = params.getProperty("id");
	if (params.getProperty("symmetric") != null) sym = true;
	if (params.getProperty("transitive") != null) trans = true;
	if (params.getProperty("reflexive") != null) refl = true;
	// find
	try {
	    noo = (BasicOntologyNetwork)alignmentCache.getOntologyNetwork(id);
	} catch (AlignmentException e) {
	    return new UnknownOntologyNetwork( params, newId(), serverId,id );
	}
	// clone if needed
   	boolean newnet = false;
    	if (params.getProperty("new") != null) newnet = true;
	if ( newnet ) {
	    String pretty = params.getProperty("pretty");
	    if ( pretty == null || pretty.equals("") ) {
		pretty = noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY ) + "/closed";
	    }
	    noo = noo.clone();
	    noo.setExtension( Namespace.EXT.uri, Annotations.PRETTY, pretty );
	}
	// close
	try {
	    noo.close( sym, trans, refl, params );
	} catch (AlignmentException e) {
	    return new ErrorMsg( params, newId(), serverId,"Invert network alignment error" );
	}
	// register
	String newid = registerNetwork( noo, newnet?null:id );
	logger.debug(" Ontology network inverted from id: {}, to new id: {} total ontologies: {} total alignments: {}",id, newid, noo.getOntologies().size(),noo.getAlignments().size());	
	return new OntologyNetworkId( params, newId(), serverId, newid,
				      noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY ));
    }

    public Message normOntologyNetwork( Properties params ) {
	String id = params.getProperty("id");
	BasicOntologyNetwork noo = null;
	// find
	try {
	    noo = (BasicOntologyNetwork)alignmentCache.getOntologyNetwork(id);
	} catch (AlignmentException e) {
	    return new UnknownOntologyNetwork( params, newId(), serverId,id );
	}
	// clone if needed
   	boolean newnet = false;
    	if (params.getProperty("new") != null) newnet = true;
	if ( newnet ) {
	    String pretty = params.getProperty("pretty");
	    if ( pretty == null || pretty.equals("") ) {
		pretty = noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY ) + "/normalized";
	    }
	    noo = noo.clone();
	    noo.setExtension( Namespace.EXT.uri, Annotations.PRETTY, pretty );
	}
	// normalize
	try {
	    noo.normalize();
	} catch (AlignmentException e) {
	    return new ErrorMsg( params, newId(), serverId,"Network normalization error" );
	}
	// register it
	String newid = registerNetwork( noo, newnet?null:id );
	logger.debug(" Ontology network normalized from id: {}, to new id: {} total ontologies: {} total alignments: {}",id, newid, noo.getOntologies().size(),noo.getAlignments().size());	
	return new OntologyNetworkId( params, newId(), serverId, newid,
				      noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY ));
    }

    public Message denormOntologyNetwork( Properties params ) {
	String id = params.getProperty("id");
	BasicOntologyNetwork noo = null;
	// find
	try {
	    noo = (BasicOntologyNetwork)alignmentCache.getOntologyNetwork(id);
	} catch (AlignmentException e) {
	    return new UnknownOntologyNetwork( params, newId(), serverId,id );
	}
	// clone if needed
   	boolean newnet = false;
    	if (params.getProperty("new") != null) newnet = true;
	if ( newnet ) {
	    String pretty = params.getProperty("pretty");
	    if ( pretty == null || pretty.equals("") ) {
		pretty = noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY ) + "/denormalized";
	    }
	    noo = noo.clone();
	    noo.setExtension( Namespace.EXT.uri, Annotations.PRETTY, pretty );
	}
	// denormalize
	try {
	    noo.denormalize();
	} catch (AlignmentException e) {
	    return new ErrorMsg( params, newId(), serverId,"Network denormalization error" );
	}
	// register it
	String newid = registerNetwork( noo, newnet?null:id );
	logger.debug(" Ontology network normalized from id: {}, to new id: {} total ontologies: {} total alignments: {}",id, newid, noo.getOntologies().size(),noo.getAlignments().size());	
	return new OntologyNetworkId( params, newId(), serverId, newid,
				      noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY ));
    }

    public Message invertOntologyNetwork( Properties params ) {
	String id = params.getProperty("id");
	BasicOntologyNetwork noo = null;
	// find
	try {
	    noo = (BasicOntologyNetwork) alignmentCache.getOntologyNetwork(id);
	} catch (AlignmentException e) {
	    return new UnknownOntologyNetwork( params, newId(), serverId,id );
	}
	// clone
   	String pretty = params.getProperty("pretty");
	if ( pretty == null || pretty.equals("") ) {
	    pretty = noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY ) + "/normalized";
	}
	noo = noo.clone();
	noo.setExtension( Namespace.EXT.uri, Annotations.PRETTY, pretty );
	// invert
	try {
	    noo.invert();
	} catch (AlignmentException e) {
	    return new ErrorMsg( params, newId(), serverId,"Network invert error" );
	}
	// register it
	String newid = registerNetwork( noo, null );
	logger.debug(" Ontology network inverted from id: {}, to new id: {} total ontologies: {} total alignments: {}",id, newid, noo.getOntologies().size(),noo.getAlignments().size());	
	return new OntologyNetworkId( params, newId(), serverId, newid,
				      noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY ));
    }

    public Message setopOntologyNetwork( Properties params ) {
	BasicOntologyNetwork noo = null;
	BasicOntologyNetwork noo1 = null;
	BasicOntologyNetwork noo2 = null;
	String op = params.getProperty("oper");
	// JE2014: Should run an exception if no oper available
	String id1 = params.getProperty("id1");
	String id2 = params.getProperty("id2");
	// find
	try {
	    noo1 = (BasicOntologyNetwork) alignmentCache.getOntologyNetwork(id1);
	} catch (AlignmentException e) {
	    return new UnknownOntologyNetwork( params, newId(), serverId,id1 );
	}
	try {
	    noo2 = (BasicOntologyNetwork) alignmentCache.getOntologyNetwork(id2);
	} catch (AlignmentException e) {
	    return new UnknownOntologyNetwork( params, newId(), serverId,id2 );
	}
	// apply
	try {
	    if ( op.equals("meet") ) noo = BasicOntologyNetwork.meet( noo1, noo2 );
	    else if ( op.equals("join") ) noo = BasicOntologyNetwork.join( noo1, noo2 );
	    else if ( op.equals("diff") ) noo = BasicOntologyNetwork.diff( noo1, noo2 );
	} catch  (AlignmentException e) {
	    // JE2014: should rather be: cannot apply method
	    return new UnknownOntologyNetwork( params, newId(), serverId, op );
	}
	String pretty = params.getProperty("pretty");
	if ( pretty != null && !pretty.equals("") ) {
	    noo.setExtension( Namespace.EXT.uri, Annotations.PRETTY, pretty );
	} else {
	    pretty = noo1.getExtension( Namespace.EXT.uri, Annotations.PRETTY );
	    pretty += "_"+op+"_";
	    pretty += noo2.getExtension( Namespace.EXT.uri, Annotations.PRETTY );
	    noo.setExtension( Namespace.EXT.uri, Annotations.PRETTY, pretty ); 
	}
	// register
	String newid = registerNetwork( noo, null );
	logger.debug(" Ontology network operation set from id1: {}, id2: {} to new id: {}total ontologies: {} total alignments: {}",id1, id2, newid, noo.getOntologies().size(),noo.getAlignments().size());	
	return new OntologyNetworkId( params, newId(), serverId, newid,
				      noo.getExtension( Namespace.EXT.uri, Annotations.PRETTY ));
    }

    // If id != null, a new network has been created
    protected String registerNetwork( BasicOntologyNetwork noo, String id ) {
	String newid = id;
	if ( id == null ) newid = alignmentCache.recordNewNetwork( noo, true );
	for ( Alignment al : noo.getAlignments() ) {
	    alignmentCache.recordNewAlignment( al, false );
	}
	return newid;
    }

    /*********************************************************************
     * Network of alignment server implementation
     *********************************************************************/

    /**
     * Ideal network implementation protocol:
     *
     * - publication (to some directory)
     * registerID
     * publishServices
     * unregisterID
     * (publishRenderer)
     * (publishMethods) : can be retrieved through the classical interface.
     *  requires a direcory
     *
     * - subscribe style
     * subscribe() : ask to receive new metadata
     * notify( metadata ) : send new metadata to subscriber
     * unsubscribe() :
     * update( metadata ) : update some modification
     *   requires to store the subscribers
     *
     * - query style: this is the classical protocol that can be done through WSDL
     * getMetadata()
     * getAlignment()
     *   requires to store the node that can be 
     */

    // Implements: reply-with
    public Message replywith(Properties params){

    //\prul{redirect}{a - request ( q(x)~reply-with:~i) \rightarrow S}{
    //Q \Leftarrow Q\cup\{\langle a, i, !i', q(x), S'\rangle\}\		\
    //S - request( q( R(x) )~reply-with:~i')\rightarrow S'}{S'\in C(q)}
	return new Message( params, newId(), serverId,"dummy//" );
    }

    // Implements: reply-to
    public Message replyto(Properties params){

    //\prul{handle-return}{S' - inform ( y~reply-to:~i') \rightarrow S}{
    //Q \Leftarrow Q-\{\langle a, i, i', _, S'\rangle\}\		\
    //S - inform( R^{-1}(y)~reply-to:~i)\rightarrow a}{\langle a, i, i', _, S'\rangle \in Q, \neg surr(y)}

    //\prul{handle-return}{S' - inform ( y~reply-to:~i') \rightarrow S}{
    //Q \Leftarrow Q-\{\langle a, i, i', _, S'\rangle\}\	\
    //R \Leftarrow R\cup\{\langle a, !y', y, S'\rangle\}\		\
    //S - inform( R^{-1}(y)~reply-to:~i)\rightarrow a}{\langle a, i, i', _, S'\rangle \in Q, surr(y)}
	return new Message( params, newId(), serverId,"dummy//" );
    }

    // Implements: failure
    public Message failure(Properties params){

    //\prul{failure-return}{S' - failure ( y~reply-to:~i') \rightarrow S}{
    //Q \Leftarrow Q-\{\langle a, i, i', _, S'\rangle\}\		\
    //S - failure( R^{-1}(y)~reply-to:~i)\rightarrow a}{\langle a, i, i', _, S'\rangle \in Q}
	return new Message( params, newId(), serverId,"dummy//" );
    }

    /*********************************************************************
     * Utilities: reaching and loading ontologies
     *********************************************************************/

    public LoadedOntology<? extends Object> reachable( URI uri ){
	try { 
	    OntologyFactory factory = OntologyFactory.getFactory();
	    return factory.loadOntology( uri );
	} catch ( Exception e ) { return null; }
    }

    /*********************************************************************
     * Utilities: Finding the implementation of an interface
     *
     * This is starting causing "java.lang.OutOfMemoryError: PermGen space"
     * (when it was in static)
     * Remedied (for the moment by improving the visited cache)
     * This may also benefit by first filling the visited by the path of the
     * libraries we know.
     *********************************************************************/

    /**
     * Display all the classes inheriting or implementing a given
     * interface in the currently loaded packages.
     * @param toclass the interface to implement
     */
    public Set<String> implementations( Class<?> toclass ) {
	return implementations( toclass, new HashSet<String>() );
    }

    public Set<String> implementations( Class<?> tosubclass, Set<String> list ) {
	Set<String> visited = new HashSet<String>();
	//visited.add();
	String classPath = System.getProperty("java.class.path",".");
	// Hack: this is not necessary
	//classPath = classPath.substring(0,classPath.lastIndexOf(File.pathSeparatorChar));
	//logger.trace( "CLASSPATH = {}", classPath );
	StringTokenizer tk = new StringTokenizer(classPath,File.pathSeparator);
	classPath = "";
	while ( tk != null && tk.hasMoreTokens() ){
	    StringTokenizer tk2 = tk;
	    tk = null;
	    // Iterate on Classpath
	    while ( tk2.hasMoreTokens() ) {
		try {
		    File file = new File( tk2.nextToken() );
		    if ( file.isDirectory() ) {
			//logger.trace("DIR {}", file);
			String subs[] = file.list();
			for( int index = 0 ; index < subs.length ; index ++ ){
			    //logger.trace("    {}", subs[index]);
			    // IF class
			    if ( subs[index].endsWith(".class") ) {
				String classname = subs[index].substring(0,subs[index].length()-6);
				if (classname.startsWith(File.separator)) 
				    classname = classname.substring(1);
				classname = classname.replace(File.separatorChar,'.');
				if ( implementsInterface( classname, tosubclass ) ) {
				    list.add( classname );
				}
			    }
			}
		    } else {
			String canon = null;
			try {
			    canon = file.getCanonicalPath();
			} catch ( IOException ioex ) {
			    canon = file.toString();
			    logger.warn( "IGNORED Invalid Jar path", ioex );
			}
			if ( canon.endsWith(".jar") &&
			     !visited.contains( canon ) && 
			     file.exists() ) {
			    //logger.trace("JAR {}", file);
			    visited.add( canon );
			    JarFile jar = null;
			    try {
				jar = new JarFile( file );
				exploreJar( list, visited, tosubclass, jar );
				// Iterate on needed Jarfiles
				// JE(caveat): this deals naively with Jar files,
				// in particular it does not deal with section'ed MANISFESTs
				Attributes mainAttributes = jar.getManifest().getMainAttributes();
				String path = mainAttributes.getValue( Name.CLASS_PATH );
				//logger.trace("  >CP> {}", path);
				if ( path != null && !path.equals("") ) {
				    // JE: Not sure where to find the other Jars:
				    // in the path or at the local place?
				    //classPath += File.pathSeparator+file.getParent()+File.separator + path.replaceAll("[ \t]+",File.pathSeparator+file.getParent()+File.separator);
				    // This replaces the replaceAll which is not tolerant on Windows in having "\" as a separator
				    // Is there a way to make it iterable???
				    for( StringTokenizer token = new StringTokenizer(path," \t"); token.hasMoreTokens(); )
					classPath += File.pathSeparator+file.getParent()+File.separator+token.nextToken();
				}
			    } catch (NullPointerException nullexp) { //Raised by JarFile
				//logger.trace( "JarFile, file {} unavailable", file );
			    }
			}
		    }
		} catch( IOException e ) {
		    continue;
		}
	    }
	    if ( !classPath.equals("") ) {
		tk =  new StringTokenizer(classPath,File.pathSeparator);
		classPath = "";
	    }
	}
	return list;
    }
    
    public void exploreJar( Set<String> list, Set<String> visited, Class<?> tosubclass, JarFile jar ) {
	Enumeration<JarEntry> enumeration = jar.entries();
	while( enumeration != null && enumeration.hasMoreElements() ){
	    JarEntry entry = enumeration.nextElement();
	    String entryName = entry.toString();
	    //logger.trace("    {}", entryName);
	    int len = entryName.length()-6;
	    if( len > 0 && entryName.substring(len).compareTo(".class") == 0) {
		entryName = entryName.substring(0,len);
		// Beware, in a Jarfile the separator is always "/"
		// and it would not be dependent on the current system anyway.
		//entryName = entryName.replaceAll(File.separator,".");
		entryName = entryName.replaceAll("/",".");
		if ( implementsInterface( entryName, tosubclass ) ) {
			    list.add( entryName );
		}
	    } else {
		String canon = entryName;
		try {
		    canon = new File( entryName ).getCanonicalPath();
		} catch ( IOException ioex ) {
		    logger.warn( "IGNORED Invalid Jar path", ioex );
		}
		if( canon.endsWith(".jar") &&
		    !visited.contains( canon ) ) { // a jar in a jar
		    //logger.trace("JAR {}", entryName);
		    visited.add( canon );
		    //logger.trace(  "jarEntry is a jarfile={}", je.getName() );
		    InputStream jarSt = null;
		    OutputStream out = null;
		    File f = null;
		    try {
			jarSt = jar.getInputStream( (ZipEntry)entry );
			f = File.createTempFile( "aservTmpFile"+visited.size(), "jar" );
			out = new FileOutputStream( f );
			byte buf[]=new byte[1024];
			int len1 ;
			while( (len1 = jarSt.read(buf))>0 )
			    out.write(buf,0,len1);
			JarFile inJar = new JarFile( f );
			exploreJar( list, visited, tosubclass, inJar );
		    } catch (IOException ioex) {
			logger.warn( "IGNORED Cannot read embedded jar", ioex );
		    } finally {
			try {
			    jarSt.close();
			    out.close();
			    f.delete();
			} catch ( Exception ex ) {
			    logger.debug( "IGNORED Exception on close", ex );
			};
		    }
		}
	    } 
	}
    }

    public boolean implementsInterface( String classname, Class<?> tosubclass ) {
	try {
	    // This is a little crazy but at least save PermGem
	    // Because Class.forName is actually allocating PermGem
	    // These are our largest libraries, but others may be to ban
	    // Hope that they do not implement any of our interfaces
	    if ( classname.startsWith( "org.apache.lucene" )
		 || classname.startsWith( "org.tartarus" )
		 || classname.startsWith( "com.hp.hpl.jena" )
		 || classname.startsWith( "org.apache.jena" )
		 || classname.startsWith( "arq." )
		 || classname.startsWith( "riotcmd" )
		 || classname.startsWith( "org.openjena" )
		 || classname.startsWith( "uk.ac.manchester.cs.owlapi" )
		 || classname.startsWith( "org.coode" )
		 || classname.startsWith( "org.semanticweb.owlapi" )
		 || classname.startsWith( "de.uulm.ecs.ai.owlapi" )
		 || classname.startsWith( "org.apache.xerces" )
		 || classname.startsWith( "org.apache.xml" )
		 || classname.startsWith( "org.apache.html" )
		 || classname.startsWith( "org.apache.wml" )
		 ) return false;
	    Class<?> cl = Class.forName(classname);
	    int mod = cl.getModifiers();
	    if ( !Modifier.isAbstract( mod )
		 && !Modifier.isInterface( mod )
		 && tosubclass.isAssignableFrom( cl ) ) return true;
	    // Not one of our classes
	} catch ( NoClassDefFoundError ncdferr ) {
	    //logger.trace("   NCDF ******** {}", classname);
	} catch ( ClassNotFoundException cnfex ) {
	    //logger.trace("   CNF ******** {}", classname);
	}
	return false;
    }

    protected class Aligner implements Runnable {
	private Properties params = null;
	private Message result = null;
	private String id = null;

	public Aligner( Properties p, String id ) {
	    params = p;
	    this.id = id;
	}

	public Message getResult() {
	    return result;
	}

	public void run() {
	    String method = params.getProperty("method");
	    // find and access o, o'
	    URI uri1 = null;
	    URI uri2 = null;

	    try {
		uri1 = new URI( params.getProperty("onto1"));
		uri2 = new URI( params.getProperty("onto2"));
	    } catch (Exception e) {
		result = new NonConformParameters( params, newId(), serverId,"nonconform/params/onto" );
		return;
	    };

	    // find initial alignment
	    Alignment init = null;
	    if ( params.getProperty("init") != null && !params.getProperty("init").equals("") ) {
		try {
		    //logger.trace(" Retrieving init");
		    try {
			init = alignmentCache.getAlignment( params.getProperty("init") );
		} catch (Exception e) {
			result = new UnknownAlignment( params, newId(), serverId,params.getProperty("init") );
			return;
		    }
		} catch (Exception e) {
		    result = new UnknownAlignment( params, newId(), serverId,params.getProperty("init") );
		    return;
		}
	    }
	    
	    // Create alignment object
	    try {
		if ( method == null )
		    method = "fr.inrialpes.exmo.align.impl.method.StringDistAlignment";
		Class<?> alignmentClass = Class.forName(method);
		Class<?>[] cparams = {};
		Constructor<?> alignmentConstructor = alignmentClass.getConstructor( cparams );
		Object[] mparams = {};
		AlignmentProcess aresult = (AlignmentProcess)alignmentConstructor.newInstance( mparams );
		try {
		    aresult.init( uri1, uri2 );
		    long time = System.currentTimeMillis();
		    aresult.align( init, params ); // add opts
		    long newTime = System.currentTimeMillis();
		    aresult.setExtension( Namespace.EXT.uri, Annotations.TIME, Long.toString(newTime - time) );
		    aresult.setExtension( Namespace.EXT.uri, Annotations.TIME, Long.toString(newTime - time) );
		    String pretty = params.getProperty( "pretty" );
		    if ( pretty != null && !pretty.equals("") )
			aresult.setExtension( Namespace.EXT.uri, Annotations.PRETTY, pretty );
		} catch (AlignmentException e) {
		    if ( reachable( uri1 ) == null ){
			result = new UnreachableOntology( params, newId(), serverId,params.getProperty("onto1") );
		    } else if ( reachable( uri2 ) == null ){
			result = new UnreachableOntology( params, newId(), serverId,params.getProperty("onto2") );
		    } else {
			result = new NonConformParameters( params, newId(), serverId,"nonconform/params/"+e.getMessage() );
		    }
		    return;
		}
		// ask to store A'
		alignmentCache.recordNewAlignment( id, aresult, true );
		result = new AlignmentId( params, newId(), serverId, id,
			       aresult.getExtension( Namespace.EXT.uri, Annotations.PRETTY ));
	    } catch ( ClassNotFoundException cnfex ) {
		logger.error( "Unknown method", cnfex );
		result = new UnknownMethod( params, newId(), serverId,method );
	    } catch (NoSuchMethodException e) {
		result = new RunTimeError( params, newId(), serverId, "No such method: "+method+"(Object, Object)" );
	    } catch (InstantiationException e) {
		result = new RunTimeError( params, newId(), serverId, "Instantiation" );
	    } catch (IllegalAccessException e) {
		result = new RunTimeError( params, newId(), serverId, "Cannot access" );
	    } catch (InvocationTargetException e) {
		result = new RunTimeError( params, newId(), serverId, "Invocation target" );
	    } catch (AlignmentException e) {
		result = new NonConformParameters( params, newId(), serverId, "nonconform/params/" );
	    } catch (Exception e) {
		result = new RunTimeError( params, newId(), serverId, "Unexpected exception :"+e );
	    }
	}
    }

}
