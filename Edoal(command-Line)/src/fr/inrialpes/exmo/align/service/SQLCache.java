/*
 * $Id: SQLCache.java 2150 2017-07-18 15:15:46Z euzenat $
 *
 * Copyright (C) Seungkeun Lee, 2006
 * Copyright (C) INRIA, 2006-2017
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

package fr.inrialpes.exmo.align.service;

import java.util.Vector;
import java.util.Date;
import java.util.Properties;
import java.util.Map.Entry;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicRelation;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.BasicOntologyNetwork;
import fr.inrialpes.exmo.align.impl.Annotations;
import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.edoal.EDOALAlignment;
import fr.inrialpes.exmo.align.impl.edoal.EDOALCell;
import fr.inrialpes.exmo.align.impl.edoal.Expression;
import fr.inrialpes.exmo.align.impl.edoal.Linkkey;
import fr.inrialpes.exmo.align.impl.edoal.Transformation;

import fr.inrialpes.exmo.ontowrap.Ontology;

import org.semanticweb.owl.align.OntologyNetwork;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;

/**
 * This class implements, in addition to VolatilCache,
 * the persistent storage of Alignments and networks
 * In a SQL database.
 *
 * It loads Alignments and network from the database
 * It stores them in the database on demand
 */

public class SQLCache extends VolatilCache implements Cache {
    final static Logger logger = LoggerFactory.getLogger( SQLCache.class );

    String host = null;
    String port = null;
    int rights = 1; // writing rights in the database (default is 1)

    final int VERSION = 480; // Version of the API to be stored in the database
    /* 300: initial database format
       301: ADDED alignment id as primary key
       302: ALTERd cached/stored/ouri tag forms
       310: ALTERd extension table with added URIs and method -> val 
       340: ALTERd size of relation in cell table (5 -> 25)
       400: ALTERd size of relation in cell table (5 -> 255 because of URIs)
            ALTERd all URI size to 255
	    ALTERd level size to 25
            ADDED cell_id as keys?
       450: ADDED ontology table / reduced alignment table
	    ADDED prefix in server
            ADDED dependency database (no action)
       470: CHANGED THE STICKER BECAUSE OF IMPLEMENTATION
            SUPPRESSED alignment information (id,source) from ontology
            ADDED ontology ids to alignments (onto1, onto2)
            ADDED network table
            ADDED networkontology join table
            ADDED networkalignment join table
            ADDED FOREIGN KEY CONSTRAINTS
            // URIINdex:
            ADDED multiple uris for alignments (?)
            CHANGED the alext namespace
       480: ADDED management of EDOAL alignments
            ADDED reltype and conftype to Alignment table
       490: no change => does not exist
     */

    DBService service = null;
    Connection conn = null;
	
    final static int CONNECTION_ERROR = 1;
    final static int SUCCESS = 2;
    final static int INIT_ERROR = 3;

    //**********************************************************************

    public SQLCache( DBService serv ) {
	service = serv;
	try {
	    conn = service.getConnection();
	} catch( Exception e ) {
	    logger.warn( "Cannot connect to DB", e );
	}
	resetTables();
    }

    public void reset() throws AlignmentException {
	resetTables();
	// reload alignment descriptions
	try {
	    load( true );
	} catch ( SQLException sqlex ) {
	    throw new AlignmentException( "SQL exception", sqlex );
	}

    }

    /**
     * loads the alignment descriptions from the database and put them in the
     * alignmentTable hashtable
     *
     * @param p: the initialisation parameters
     * @param prefix: the URI prefix of the current server
     * @throws AlignmentException when something goes wrong (cannot access database, but format)
     */
    public void init( Properties p, String prefix ) throws AlignmentException {
	super.init( p, prefix );
	port = p.getProperty("http"); // bad idea
	host = p.getProperty("host");
	Statement st = null;
	try {
	    st = createStatement();
	    // test if a database is here, otherwise create it
	    ResultSet rs = conn.getMetaData().getTables(null,null, "server", new String[]{"TABLE"});
	    if ( !rs.next() ) {
		initDatabase();
	    } else {
		updateDatabase(); // in case it is necessary to upgrade
	    }
	    String pref = p.getProperty("prefix");
	    if ( pref == null || pref.equals("") ) {
		rs = st.executeQuery( "SELECT prefix FROM server WHERE port='port'" );
		while( rs.next() ) {
		    idprefix = rs.getString("prefix");
		}
	    }
	    // register by the database
	    registerServer( host, port, rights==1, idprefix );
	    // load alignment descriptions
	    load( true );
	} catch (SQLException sqlex) {
	    throw new AlignmentException( "SQLException", sqlex );
	} finally {
	    try {
		if ( st != null ) st.close();
	    } catch (SQLException sqlex) {};
	}
    }

    public void close() throws AlignmentException  {
	Statement st = null;
	try {
	    st = createStatement();
	    // unregister by the database
	    st.executeUpdate( "DELETE FROM server WHERE host='"+host+"' AND port='"+port+"'" );
	} catch (SQLException sqlex) {
	    throw new AlignmentException( "SQL Exception", sqlex );
	} finally {
	    try {
		if ( st != null ) st.close();
		if ( conn != null ) conn.close();
	    } catch (SQLException sqlex) {};
	}
    }

    public Statement createStatement() throws SQLException {
	return service.getConnection().createStatement();
    }

    // **********************************************************************
    // LOADING FROM DATABASE

    /**
     * loads the alignment descriptions and networks of ontologies from the
     * database and put them in the indexes
     * 
     * @param force: true if data is downloaded from the database
     * @throws SQLException when something goes wrong (e.g., no database access)
     */
    private void load( boolean force ) throws SQLException {
	logger.debug( "Loading alignments and networks..." );
	String id = null;
	Alignment alignment = null;
	OntologyNetwork noo = null;
	Vector<String> idInfo = new Vector<String>();
	
	if (force) {
	    // Retrieve the alignment ids
	    try ( Statement st = conn.createStatement() ) {
		    ResultSet rs = st.executeQuery("SELECT id FROM alignment");
		    while( rs.next() ) {
			id = rs.getString("id");
			idInfo.add(id);	
		    }
		}
	    // For each alignment id store metadata
	    for( int i = 0; i < idInfo.size(); i++ ) {
		id = idInfo.get(i);
		alignment = retrieveDescription( id );
		recordAlignment( recoverAlignmentUri( id ), alignment, true );
		// URIINdex: load alternative URIs
		try ( Statement st = conn.createStatement() ) {
			ResultSet rs = st.executeQuery("SELECT uri,prefered FROM alignmenturis WHERE id="+quote(id)+";");
			while( rs.next() ) {
			    alignmentURITable.put( rs.getString("uri"), alignment );
			}	    
		    }
	    }
	    // ONETW: Load ontology networks
	    idInfo.clear();
	    try ( Statement st = conn.createStatement() ) {
		    ResultSet rs = st.executeQuery("SELECT id FROM network");
		    while( rs.next() ) {
			id = rs.getString("id");
			idInfo.add(id);	
		    }
		}
	    for( int i = 0; i < idInfo.size(); i++ ) {
		id = idInfo.get(i);
		noo = retrieveOntologyNetwork( id );
		recordNetwork( recoverNetworkUri( id ), noo, true );
	    }							
	}
    }
    
    /**
     * loads the description of alignments from the database and set them
     * in an alignment object
     * 
     * @param id: the id of an alignment
     * @return the loaded alignment
     */
    protected Alignment retrieveDescription( String id ) {
	ResultSet rs;
	String tag;
	String value;

	logger.debug( "Loading alignment {}", id );
	BasicAlignment result;
	Statement st = null;
	try {
	    st = createStatement();
	    String onto1 = "", onto2 = "";
	    // Get basic ontology metadata
	    rs = st.executeQuery( "SELECT * FROM alignment WHERE id = '" + id  +"'" );
	    if ( ! rs.next() ) logger.debug( "IGNORED cannot find retrieve "+id );
	    String level = rs.getString("level");
	    if ( level.contains( "2EDOAL" ) ) {
		result = new EDOALAlignment();
	    } else {
		result = new URIAlignment();
	    }
	    result.setLevel( level );
	    result.setType( rs.getString("type") );
	    String relClassName = (String)rs.getObject("reltype");
	    if ( relClassName != null ) {
		result.setRelationType( relClassName );
	    }
	    String confClassName = (String)rs.getObject("conftype");
	    if ( confClassName != null ) {
		result.setConfidenceType( confClassName );
	    }

	    // Get the ontolologies
	    onto1 = rs.getString( "onto1" );
	    onto2 = rs.getString( "onto2" );
	    retrieveOntology( onto1, result.getOntologyObject1() );
	    result.setExtension( SVCNS, OURI1, onto1 );
	    retrieveOntology( onto2, result.getOntologyObject2() );
	    result.setExtension( SVCNS, OURI2, onto2 );
	    result.init( result.getOntologyObject1(), result.getOntologyObject2() );

	    // Get dependencies if necessary
	    
	    // Get extension metadata
	    rs = st.executeQuery( "SELECT * FROM extension WHERE id = '" + id + "'" );
	    while(rs.next()) {
		tag = rs.getString("tag");
		value = rs.getString("val");
		result.setExtension( rs.getString("uri"), tag, value);
	    }
	    // has been extracted from the database
	    //result.setExtension( SVCNS, STORED, "DATE");
	    // not yet cached (this instruction should be useless)
	    result.setExtension( SVCNS, CACHED, (String)null );
	    return result;
	} catch (Exception e) { // URI exception that should not occur
	    logger.debug( "IGNORED unlikely URI exception", e);
	    return null;
	} finally {
	    try { st.close(); }
	    catch (Exception ex) { logger.debug( "Ignored exception on close", ex ); };
	}
    }

    public void retrieveOntology( String uri, Ontology<?> ob ) throws SQLException, AlignmentException, URISyntaxException {
	Statement st = createStatement();
	ResultSet rs = st.executeQuery( "SELECT * FROM ontology WHERE uri = '" + uri  +"'" );
	if ( rs.next() ) {
	    ob.setURI( new URI(rs.getString("uri"))  );
	    if ( rs.getString("file") != null ) ob.setFile( new URI( rs.getString("file") ) );
	    if ( rs.getString("formuri") != null ) ob.setFormURI( new URI( rs.getString("formuri") ) );
	    if ( rs.getString("formname") != null ) ob.setFormalism( rs.getString("formname") );
	} else throw new AlignmentException( "Unknown ontology : "+uri );
    }

    /**
     * loads the full alignment from the database and put it in the
     * alignmentTable hastable
     * 
     * should be invoked when:
     * 	( result.getExtension(CACHED) == ""
     *  AND result.getExtension(STORED) != "") {
     * 
     * @param uri: the uri of an alignment
     * @param alignment: the alignment structure containing its metadata
     * @return the retrieved alignment
     * @throws SQLException when the database is not accessible
     * @throws URISyntaxException when the URI is invalid
     * @throws AlignmentException when something else goes wrong (no corresponding alignment)
     */
    protected Alignment retrieveAlignment( String uri, Alignment alignment ) throws SQLException, AlignmentException, URISyntaxException {
	String id = stripAlignmentUri( uri );

	// Get cells
	Statement st = createStatement();
	Statement st2 = createStatement();
	EDOALSQLCache esrv = null;
	ResultSet rs = st.executeQuery( "SELECT * FROM cell WHERE id = '" + id + "'" );
	while( rs.next() ) {
	    logger.trace( "Loading cell {}", rs.getString( "cell_id" ) );
	    Cell cell;
	    if ( alignment instanceof URIAlignment ) {
		URI ent1 = new URI( rs.getString("uri1") );
		URI ent2 = new URI( rs.getString("uri2") );
		if ( ent1 == null || ent2 == null ) break;
		cell = ((URIAlignment)alignment).addAlignCell( ent1, ent2, rs.getString("relation"), Double.parseDouble(rs.getString("measure")) );
	    } else { // load the cell (if this is not a URIAlignment, this is an EDOALAlignment)
		esrv = new EDOALSQLCache( service );
		esrv.init();
		logger.debug( " Entity1" );
		Object ent1 = esrv.extractExpression( Long.parseLong( rs.getString("uri1") ) );
		logger.debug( "{}", ent1 );
		logger.debug( " Entity2" );
		Object ent2 = esrv.extractExpression( Long.parseLong( rs.getString("uri2") ) );
		logger.debug( "{}", ent2 );
		if ( ent1 == null || ent2 == null ) break;
		cell = ((EDOALAlignment)alignment).addAlignCell( ent1, ent2, rs.getString("relation"), Double.parseDouble(rs.getString("measure")) );
	    }
	    String cid = rs.getString( "cell_id" );
	    if ( cid != null && !cid.equals("") ) {
		if ( !cid.startsWith("##") ) {
		    cell.setId( cid );
		}
		if ( cell instanceof EDOALCell ) { // load linkkeys and transformations
		    esrv.extractTransformations( cid, ((EDOALCell)cell) );
		    esrv.extractLinkkeys( cid, ((EDOALCell)cell) );
		}
		ResultSet rse2 = st2.executeQuery("SELECT * FROM extension WHERE id = '" + cid + "'");
		while ( rse2.next() ) {
		    cell.setExtension( rse2.getString("uri"), 
				       rse2.getString("tag"), 
				       rse2.getString("val") );
		}
	    }
	    cell.setSemantics( rs.getString( "semantics" ) );
	}
	
	// reset
	resetCacheStamp(alignment);
	st.close();
	return alignment;
    }

    // Load an ontology network
    protected OntologyNetwork retrieveOntologyNetwork( String id ) {
	logger.debug( "Loading network of ontology {}", id );
	BasicOntologyNetwork network = new BasicOntologyNetwork();
	Statement st = null;

	try {
	    st = createStatement();
	    // Get basic ontology metadata
	    // Basically if nothing is available
	    //ResultSet rs = st.executeQuery( "SELECT * FROM network WHERE id = '" + id  +"'" );
	    //while( rs.next() ) {}
	    ResultSet rs = st.executeQuery( "SELECT * FROM networkontology WHERE network = '" + id  +"'" );
	    while ( rs.next() ) {
		// TODO: Create the ontology if non existant [NO: Todo when storing!]
		network.addOntology( new URI( rs.getString( "onto" ) ) );
	    }
	    rs = st.executeQuery( "SELECT * FROM networkalignment WHERE network = '" + id  +"'" );
	    while ( rs.next() ) {
		// get the alignment with that URI and set it (only the description of the alignment)
		network.addAlignment( getMetadata( recoverAlignmentUri( rs.getString("align") ) ) );
	    }

	    // Get extension metadata (including URI)
	    rs = st.executeQuery( "SELECT * FROM extension WHERE id = '" + id + "'" );
	    String tag;
	    String value;
	    while(rs.next()) {
		tag = rs.getString("tag");
		value = rs.getString("val");
		network.setExtension( rs.getString("uri"), tag, value);
	    }
	} catch ( URISyntaxException uriex ) { // URI exception that should not occur
	    logger.debug( "IGNORED unlikely URI exception", uriex );
	    network = null;
	} catch ( SQLException sqlex ) {
	    logger.warn( "SQL Problem retrieving ontology network", sqlex );
	    network = null;
	} catch ( AlignmentException alex ) {
	    logger.warn( "Problem retrieving ontology network", alex );
	    network = null;
	} finally {
	    try { st.close(); } catch (Exception ex) {};
	}
	// has been extracted from the database
	//network.setExtension( SVCNS, STORED, "DATE");
	// not yet cached (this instruction should be useless)
	network.setExtension( SVCNS, CACHED, (String)null );

	return network;
    }
    
    //**********************************************************************
    // FETCHING FROM CACHE
	
    /**
     * retrieve full alignment from id (and cache it)
     * 
     * @param uri: the URI of an alignment
     * @param result: its alignment structure
     * @return the fetched alignment
     * @throws AlignmentException when something goes wrong
     */
    protected Alignment fetchAlignment( String uri, Alignment result ) throws AlignmentException {
	try { 
	    return retrieveAlignment( uri, result );
	} catch ( SQLException sqlex ) {
	    logger.trace( "Cache: cannot read from DB", sqlex );
	    throw new AlignmentException( "getAlignment: SQL exception", sqlex );
	} catch ( URISyntaxException urisex ) {
	    logger.trace( "Cache: Incorrect URI", urisex );
	    throw new AlignmentException( "getAlignment: Cannot find alignment", urisex );
	}
    }
	
    //**********************************************************************
    // STORING IN DATABASE
    /**
     * quote:
     * Prepare a string to be used in SQL queries by preceeding occurences of
     * "'", """, and "\'" by a "\"".
     * This should be implemented at a lower level within Java itself
     * (or the sql package).
     * This function is used here for protecting everything to be entered in
     * the database
     * 
     * @param s: a string to be passed to SQL
     * @return the string with quotes protected
     */
    public static String quote( String s ) {
	if ( s == null ) return "NULL";
	String result = "'";
	char[] chars = s.toCharArray();
	int j = 0;
	int i = 0;
	char c;
	for ( ; i < chars.length; i++ ){
	    c = chars[i];
	    if ( c == '\'' || c == '"' || c == '\\' ) {
		result += new String( chars, j, i-j ) + "\\" + c;
		j = i+1;
	    };
	}
	return result + new String( chars, j, i-j ) + "'";
    }

    /**
     * Non publicised class
     *
     * @param uri: the URI of the alignment to suppress from the store
     * @throws AlignmentException when something goes wrong (cannot resolve URI)
     */
    public void unstoreAlignment( String uri ) throws AlignmentException {
	Alignment alignment = getAlignment( uri );
	if ( alignment != null ) {
	    unstoreAlignment( uri, alignment );
	}
    }

    // Suppress it from the cache...
    public void unstoreAlignment( String uri, Alignment alignment ) throws AlignmentException {
	try {
	    Statement st = null;
	    String id = stripAlignmentUri( uri );
	    try {
		conn.setAutoCommit( false );
		st = createStatement();
		// Delete cell's extensions
		ResultSet rs = st.executeQuery( "SELECT cell_id FROM cell WHERE id='"+id+"'" );
		while ( rs.next() ){
		    String cid = rs.getString("cell_id");
		    if ( cid != null && !cid.equals("") ) {
			st.executeUpdate( "DELETE FROM extension WHERE id='"+cid+"'" );
		    }
		}
		unstoreEDOALAlignment( id, alignment ); 
		st.executeUpdate("DELETE FROM cell WHERE id='"+id+"'");
		st.executeUpdate("DELETE FROM extension WHERE id='"+id+"'");
		//ontologies do not depend on alignments
		//st.executeUpdate("DELETE FROM ontology WHERE id='"+id+"'");
		st.executeUpdate("DELETE FROM dependency WHERE id='"+id+"'");
		st.executeUpdate("DELETE FROM alignmenturis WHERE id='"+id+"'");
		st.executeUpdate("DELETE FROM alignment WHERE id='"+id+"'");
		alignment.setExtension( SVCNS, STORED, (String)null);
	    } catch ( SQLException sex ) {
		conn.rollback();
		logger.warn( "SQLError", sex );
		throw new AlignmentException( "SQL Exception", sex );
	    } finally {
		conn.setAutoCommit( false );
		if ( st != null ) st.close();
	    }
	} catch ( SQLException sex ) {
	    throw new AlignmentException( "Cannot establish SQL Connexion", sex );
	}
    }

    public void unstoreEDOALAlignment( String id, Alignment alignment ) throws AlignmentException {
	// should seriously think of making it static?
	EDOALSQLCache esrv = new EDOALSQLCache( service );
	esrv.init();
	for ( Cell c : alignment ) {
	    if ( c instanceof EDOALCell ) esrv.erase( (EDOALCell)c );
	}
    }

    public void storeAlignment( String uri ) throws AlignmentException {
	logger.trace( "Storing alignment "+uri );
	String query = null;
	BasicAlignment alignment = (BasicAlignment)getAlignment( uri );
	String id = stripAlignmentUri( uri );
	Statement st = null;
	// We store stored date (this is done now for being registered in the database)
	alignment.setExtension( SVCNS, STORED, new Date().toString());
	// We empty cached date
	alignment.setExtension( SVCNS, CACHED, (String)null );

	EDOALSQLCache esrv = null;
	if ( alignment instanceof EDOALAlignment ) {
	    esrv = new EDOALSQLCache( service );
	    esrv.init();
	    logger.trace( "EDOAL Alignment: created object" );
	}
	try {
	    // Try to store at most 3 times.
	    // Otherwise, an exception EOFException will be thrown (relation with Jetty???)
	    // [JE2013: Can we check this?]
	    for( int i=0; i < 3 ; i++ ) {
		logger.trace( "Trying to store : "+i );
		try {
		    st = createStatement();
		    logger.debug( "Storing alignment {} as {}", uri, id );
		    conn.setAutoCommit( false );
		    recordOntology( st,
				    alignment.getOntology1URI(),
				    alignment.getFile1(), 
				    alignment.getOntologyObject1() );
		    recordOntology( st,
				    alignment.getOntology2URI(),
				    alignment.getFile2(), 
				    alignment.getOntologyObject2() );
		    Class<?> relClass = alignment.getRelationType();
		    String relClassName = null;
		    if ( relClass != BasicRelation.class ) {
			relClassName = relClass.getName();
		    }
		    Class<?> confClass = alignment.getConfidenceType();
		    String confClassName = null;
		    if ( confClass != BasicConfidence.class ) {
			confClassName = confClass.getName();
		    }
		    // This cannot be done in the end because of foreign keys, rollback takes care of it
		    query = "INSERT INTO alignment " + 
			"(id, type, level, reltype, conftype, onto1, onto2) " +
			"VALUES (" +quote(id)+","+quote(alignment.getType())+","+quote(alignment.getLevel())+","+quote(relClassName)+","+quote(confClassName)+","+quote(alignment.getOntology1URI().toString())+","+quote(alignment.getOntology2URI().toString())+")";
		    st.executeUpdate(query);
		    // TODO: Store dependencies
		    // Store extensions
		    for ( String[] ext : alignment.getExtensions() ) {
			String turi = ext[0];
			String tag = ext[1];
			String val = ext[2];
			query = "INSERT INTO extension " + 
			    "(id, uri, tag, val) " +
			    "VALUES (" + quote(id) + "," +  quote(turi) + "," +  quote(tag) + "," + quote(val) + ")";
			st.executeUpdate(query);
		    }
		    // Store cells
		    logger.trace( "Storing cells" );
		    for( Cell c : alignment ) {
			String cellid = null;
			if ( c.getObject1() != null && c.getObject2() != null ){
			    cellid = c.getId();
			    if ( cellid != null ){
				if ( cellid.startsWith("#") ) {
				    cellid = alignment.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID ) + cellid;
				}
			    } else if ( ( c.getExtensions() != null ) ||
					( ( c instanceof EDOALCell ) &&
					  ( ( ((EDOALCell)c).transformations() != null && !((EDOALCell)c).transformations().isEmpty() ) ||
					    ( ((EDOALCell)c).linkkeys() != null  && !((EDOALCell)c).linkkeys().isEmpty() ) ) ) ) {
				// if no id and we need one for  => generate one.
				cellid = generateCellId();
			    }
			    else cellid = "";
			    logger.trace( "Storing cell: "+cellid );
			    String uri1, uri2;
			    if ( c instanceof EDOALCell ) {
				uri1 = String.valueOf( esrv.visit( (Expression)((EDOALCell)c).getObject1() ) );
				uri2 = String.valueOf( esrv.visit( (Expression)((EDOALCell)c).getObject2() ) );
			    } else {
				uri1 = c.getObject1AsURI(alignment).toString();
				uri2 = c.getObject2AsURI(alignment).toString();
			    }
			    String strength = c.getStrength() + ""; // crazy Java
			    String sem;
			    if ( !c.getSemantics().equals("first-order") )
				sem = c.getSemantics();
			    else sem = "";
			    // May be time to have a prettyPrint() in Relation.
			    StringWriter relStringWriter = new StringWriter();
			    c.getRelation().write( new PrintWriter( relStringWriter ) );
			    try {
				relStringWriter.close();
			    } catch ( IOException ioex ) {
				logger.warn( "Cannot close StringWriter", ioex );
			    };
			    String rel = relStringWriter.toString();
			    query = "INSERT INTO cell " + 
				"(id, cell_id, uri1, uri2, measure, semantics, relation) " +
				"VALUES (" + quote(id) + "," + quote(cellid) + "," + quote(uri1) + "," + quote(uri2) + "," + quote(strength) + "," + quote(sem) + "," + quote(rel) + ")";
			    st.executeUpdate(query);
			}
			// Store extensions
			if ( cellid != null && !cellid.equals("") && c.getExtensions() != null ) {
			    // Store extensions
			    for ( String[] ext : c.getExtensions() ) {
				String turi = ext[0];
				String tag = ext[1];
				String val = ext[2];
				query = "INSERT INTO extension " + 
				    "(id, uri, tag, val) " +
				    "VALUES (" + quote(cellid) + "," +  quote(turi) + "," +  quote(tag) + "," + quote(val) + ")";
				st.executeUpdate(query);
			    }
			}
			logger.trace( "Stored cell: "+cellid );
			// Store transformations and linkkeys
			if ( c instanceof EDOALCell ) { // store linkkeys and transformations if any
			    if ( ((EDOALCell)c).transformations() != null && !((EDOALCell)c).transformations().isEmpty() ) {
				for ( Transformation transf : ((EDOALCell)c).transformations() ) {
				    esrv.visit( transf, cellid );
				}
			    }
			    if ( ((EDOALCell)c).linkkeys() != null  && !((EDOALCell)c).linkkeys().isEmpty() ) {
				for ( Linkkey lk : ((EDOALCell)c).linkkeys() ) {
				    esrv.visit( lk, cellid );
				}
			    }
			}
			logger.trace( "Stored trsnformations and linkleys: "+cellid );
		    }
		    // URIINdex: store alternative URIs
		    for ( Entry<String,Alignment> entry : alignmentURITable.entrySet() ) {
			if ( entry.getValue() == alignment ) {
			    query = "INSERT INTO alignmenturis "+"(id, uri,prefered) VALUES ("+quote(entry.getKey())+","+quote(id)+",false);";
			    st.executeUpdate(query);
			}
		    }
		    st.close();
		} catch ( SQLException sqlex ) {
		    // Unstore the date
		    alignment.setExtension( SVCNS, STORED, "");
		    logger.warn( "SQLError", sqlex );
		    conn.rollback(); // seems to work well!
		    throw new AlignmentException( "SQLException", sqlex );
		} finally {
		    if ( st != null ) st.close();
		    conn.setAutoCommit( true );
		}
		break; // succeeded
	    }
	} catch (SQLException sqlex) {
	    logger.trace( "SQLError", sqlex );
	    throw new AlignmentException( "SQLRollBack issue", sqlex );
	}
	// We reset cached date
	resetCacheStamp(alignment);
    }

    // Do not add transaction here: this is handled by caller
    public void	recordOntology( Statement st, URI uri, URI file, Ontology<Object> onto ) throws SQLException {
    // modify to use queryStatement.setNull for sformalism and sformuri
	String suri = uri.toString();
	String sfile = "";
	if ( file != null ) sfile = file.toString();
	String sformuri = "NULL";
	String query = "SELECT * FROM ontology WHERE uri="+quote(suri)+";";
	ResultSet res = st.executeQuery(query);

	if ( !res.next() ) {	    
	    if ( onto != null ) {
		logger.debug( "Recording ontology {} with file {} formalism {} formURI {}", suri, sfile, onto.getFormalism(), onto.getFormURI() );
	    	if ( onto.getFormURI() != null ) sformuri = onto.getFormURI().toString();	
		query = "INSERT INTO ontology " + 
		    "(uri, file, formname, formuri) " +
		    "VALUES ("+ quote(suri)+","+quote(sfile)+","+quote(onto.getFormalism())+","+quote(sformuri)+")";
	    } else {
		query = "INSERT INTO ontology " + 
		    "(uri, file) " +
		    "VALUES ("+ quote(suri)+","+quote(sfile)+")";
	    }
	    st.executeUpdate(query);
	} else {
		String sformname = "";
		if ( res.getString("formname") != null ) sformname = res.getString("formname");
		if ( onto != null && sformname.equals("") ) { // JE: checktest
		logger.debug( "Updating ontology {} with formalism {}", suri, onto.getFormalism() );
    	if ( onto.getFormURI() != null ) sformuri = onto.getFormURI().toString();	
		query = "UPDATE ontology SET formname="+quote(onto.getFormalism())+", formuri="+quote(sformuri)+" WHERE uri="+quote(suri)+";";
		st.executeUpdate(query);
	    }
	}
    }

    public void storeOntologyNetwork( String uri ) throws AlignmentException {
	String query = null;
	BasicOntologyNetwork network = (BasicOntologyNetwork)getOntologyNetwork( uri );
	String id = stripNetworkUri( uri );
	String qid = quote( id );
	Statement st = null;
	// We store stored date
	network.setExtension( SVCNS, STORED, new Date().toString());
	// We empty cached date
	network.setExtension( SVCNS, CACHED, (String)null );

	try {
	    // Try to store at most 3 times.
	    // Otherwise, an exception EOFException will be thrown (relation with Jetty???)
	    // [JE2013: Can we check this?]
	    for( int i=0; i < 3 ; i++ ) {
		try {
		    st = createStatement();
		    logger.debug( "Storing network {} as {}", uri, id );
		    conn.setAutoCommit( false );
		    // Declare ontology network
		    st.executeUpdate("INSERT INTO network (id) VALUES ("+qid+");");
		    // Store alignments
		    for ( Alignment align : network.getAlignments() ) {
			String alid = align.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID );
			if ( !isAlignmentStored( align ) ) {
			    storeAlignment( alid );
			}
			query = "INSERT INTO networkalignment (network,align) VALUES (" 
			    +qid+","+quote( stripAlignmentUri( alid ) )+");";
			st.executeUpdate(query);
		    }
		    // Store ontologies...
		    for ( URI onto : network.getOntologies() ) {
			String quri = quote(onto.toString());
			Statement st2 = createStatement();
			ResultSet rs2 = st2.executeQuery( "SELECT * FROM ontology WHERE uri = " +quri+ ";" );
			if( !rs2.next() ) {
			// Store it (with not a lot of information: too bad)
			    st2.executeUpdate("INSERT INTO ontology (uri) VALUES (" +quri+ ");");
			}
			query = "INSERT INTO networkontology (network,onto) VALUES ("+qid+","+quri+");";
			st.executeUpdate(query);
		    }
		    // Store extensions
		    for ( String[] ext : network.getExtensions() ) {
			String turi = ext[0];
			String tag = ext[1];
			String val = ext[2];
			query = "INSERT INTO extension " + 
			    "(id, uri, tag, val) " +
			    "VALUES (" + qid + "," +  quote(turi) + "," +  quote(tag) + "," + quote(val) + ");";
			st.executeUpdate(query);
		    }
		    st.close();
		} catch ( SQLException sqlex ) {
		    logger.warn( "SQLError", sqlex );
		    conn.rollback();
		    throw new AlignmentException( "SQLException", sqlex );
		} finally {
		    conn.setAutoCommit( true );
		    if ( st != null ) st.close();
		}
		break;
	    }
	} catch (SQLException sqlex) {
	    throw new AlignmentException( "SQLRollBack issue", sqlex );
	}
	// We reset cached date
	resetCacheStamp( network );
    }

    public void unstoreOntologyNetwork( String uri, BasicOntologyNetwork network ) throws AlignmentException {
	try {
	    Statement st = null;
	    String id = stripAlignmentUri( uri );
	    try {
		st = createStatement();
		conn.setAutoCommit( false );
		st.executeUpdate("DELETE FROM network WHERE id='"+id+"'");
		st.executeUpdate("DELETE FROM extension WHERE id='"+id+"'");
		network.setExtension( SVCNS, STORED, (String)null);
	    } catch ( SQLException sqlex ) {
		conn.rollback();
		logger.warn( "SQLError", sqlex );
		throw new AlignmentException( "SQL Exception", sqlex );
	    } finally {
		conn.setAutoCommit( false );
		if ( st != null ) st.close();
	    }
	} catch ( SQLException sqlex ) {
	    throw new AlignmentException( "Cannot establish SQL Connexion", sqlex );
	}
    }

    // **********************************************************************
    // DATABASE CREATION AND UPDATING
    /*
      # server info

      create table server (
      host varchar(50),
      port varchar(5),
      prefix varchar(50),
      edit varchar(5), 
      version VARCHAR(5)
      );
   
      # ontology info

      create table ontology (
      uri varchar(255),
      file varchar(255),
      formname varchar(50),
      formuri varchar(255)
      PRIMARY KEY (uri));

      # alignment info
      
      create table alignment (
      id varchar(100), 
      type varchar(5),
      level varchar(25),
      reltype varchar(255),
      conftype varchar(255),
      onto1 varchar(255),
      onto2 varchar(255),
      FOREIGN KEY (onto1) REFERENCES ontology (uri)
      FOREIGN KEY (onto2) REFERENCES ontology (uri)
      PRIMARY KEY (id));

      # cell info

      create table cell(
      id varchar(100),
      cell_id varchar(255),
      uri1 varchar(255),
      uri2 varchar(255),
      semantics varchar(30),
      measure varchar(20),
      relation varchar(255),
      FOREIGN KEY (id) REFERENCES alignment (id));

      # extension info
      
      CREATE TABLE extension (
      id varchar(100),
      uri varchar(200),
      tag varchar(50),
      val varchar(500));
      
      # extension info
      
      CREATE TABLE alignmenturis (
      id varchar(100),
      uri varchar(255),
      prefered boolean);
      // Implicit constraint, for each id, there is at most one prefered set to true
      
      # network info

      create table network (
      id varchar(100),
      PRIMARY KEY (id));

      # network-ontology info

      create table networkontology (
      network varchar(100),
      onto varchar(255),
      FOREIGN KEY (network) REFERENCES network (id)
      FOREIGN KEY (onto) REFERENCES ontology (uri)
      PRIMARY KEY (network,onto));

      # network-alignment info

      create table networkalignment (
      network varchar(100),
      align varchar(100),
      FOREIGN KEY (network) REFERENCES network (id)
      FOREIGN KEY (align) REFERENCES alignment (id)
      PRIMARY KEY (network,align));

      # dependencies info

      create table dependency (
      id varchar(100), 
      dependsOn varchar(100),
      FOREIGN KEY (id) REFERENCES alignment (id),
      FOREIGN KEY (dependsOn) REFERENCES alignment (id),
      PRIMARY KEY (id, dependsOn));

      # dependencies info

      CREATE TABLE edoalexpr (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      type INT, 
      joinid BIGINT, 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE valueexpr (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      type INT, 
      joinid BIGINT, 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE instexpr (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      uri VARCHAR(250), 
      var VARCHAR(250), 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE literal (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      type BIGINT, 
      value VARCHAR(500), 
      PRIMARY KEY (intid))")

      # dependencies info

      CREATE TABLE typeexpr (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      uri VARCHAR(250), 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE apply (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      type INT,
      operation VARCHAR(255), 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE arglist (
      intid BIGINT NOT NULL, 
      id BIGINT NOT NULL)

      # dependencies info

      CREATE TABLE classexpr (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      type INT, 
      joinid BIGINT, 
      var VARCHAR(250), 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE classid (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      uri VARCHAR(250), 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE classlist (
      intid BIGINT NOT NULL, 
      id BIGINT NOT NULL)

      # dependencies info

      CREATE TABLE classrest (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      path BIGINT, 
      type INT, 
      joinid BIGINT, 
      var VARCHAR(250), 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE pathexpr (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      type INT, 
      joinid BIGINT, 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE propexpr (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      type INT, 
      joinid BIGINT, 
      var VARCHAR(250), 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE propid (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      uri VARCHAR(250), 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE proplist (
      intid BIGINT NOT NULL, 
      id BIGINT NOT NULL)

      # dependencies info

      CREATE TABLE valuerest (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      type INT, 
      path BIGINT, 
      comp VARCHAR(250), 
      joinid BIGINT, 
      var VARCHAR(250), 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE relexpr (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      type INT, 
      joinid BIGINT, 
      var VARCHAR(250), 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE relid (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      uri VARCHAR(250), 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE rellist (
      intid BIGINT NOT NULL, 
      id BIGINT NOT NULL)

      # dependencies info

      CREATE TABLE transf (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      cellid VARCHAR(255), 
      type INT, 
      joinid1 BIGINT, 
      joinid2 BIGINT, 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE linkkey (
      intid BIGINT NOT NULL AUTO_INCREMENT, 
      cellid VARCHAR(255), 
      PRIMARY KEY (intid))

      # dependencies info

      CREATE TABLE binding (
      keyid BIGINT, 
      type INT, 
      joinid1 BIGINT, 
      joinid2 BIGINT)

    */

    public void initDatabase() throws SQLException {
	logger.info( "Initialising database" );
	Statement st = createStatement();
	try {
	    conn.setAutoCommit( false );
	    // Create tables
	    st.executeUpdate("CREATE TABLE server (host VARCHAR(50), port VARCHAR(5), prefix VARCHAR (50), edit BOOLEAN, version VARCHAR(5))");	    
	    st.executeUpdate("CREATE TABLE ontology (uri VARCHAR(255), formname VARCHAR(50), formuri VARCHAR(255), file VARCHAR(255), PRIMARY KEY (uri))");
	    st.executeUpdate("CREATE TABLE alignment (id VARCHAR(100), type VARCHAR(5), level VARCHAR(25), reltype VARCHAR(255), conftype VARCHAR(255), onto1 VARCHAR(255), onto2 VARCHAR(255), FOREIGN KEY (onto1) REFERENCES ontology (uri), FOREIGN KEY (onto2) REFERENCES ontology (uri), PRIMARY KEY (id))");
	    st.executeUpdate("CREATE TABLE network (id VARCHAR(100), PRIMARY KEY (id))");
	    st.executeUpdate("CREATE TABLE networkontology (network VARCHAR(100), onto VARCHAR(255), FOREIGN KEY (network) REFERENCES network (id), FOREIGN KEY (onto) REFERENCES ontology (uri), PRIMARY KEY (network,onto))");
	    st.executeUpdate("CREATE TABLE networkalignment (network VARCHAR(100), align VARCHAR(100), FOREIGN KEY (network) REFERENCES network (id), FOREIGN KEY (align) REFERENCES alignment (id), PRIMARY KEY (network,align))");
	    st.executeUpdate("CREATE TABLE dependency (id VARCHAR(100), dependsOn VARCHAR(100), FOREIGN KEY (id) REFERENCES alignment (id), FOREIGN KEY (dependsOn) REFERENCES alignment (id), primary key (id, dependsOn))");
	    st.executeUpdate("CREATE TABLE cell (id VARCHAR(100), cell_id VARCHAR(255), uri1 VARCHAR(255), uri2 VARCHAR(255), semantics VARCHAR(30), measure VARCHAR(20), relation VARCHAR(255), FOREIGN KEY (id) REFERENCES alignment (id))");
	    st.executeUpdate("CREATE TABLE extension (id VARCHAR(100), uri VARCHAR(200), tag VARCHAR(50), val VARCHAR(500))");
	    st.executeUpdate("CREATE TABLE alignmenturis (id varchar(100), uri varchar(255), prefered boolean);");
	    initEDOALTables( st );

	    st.close();

	    // Register *DATABASE* Because of the values (that some do not like), this is a special statement
	    registerServer( "dbms", "port", false, idprefix );
	} catch ( SQLException sex ) {
	    logger.warn( "SQLError", sex );
	    conn.rollback();
	    throw sex;
	} finally {
	    conn.setAutoCommit( true );
	}
    }

    public void initEDOALTables( Statement st ) throws SQLException {
	// Bloody incompats
	String SERIAL = "";
	if ( service.getPrefix().equals( "jdbc:mysql" ) ) SERIAL = "BIGINT NOT NULL AUTO_INCREMENT";
	else if ( service.getPrefix().equals( "jdbc:postgresql" ) ) SERIAL = "BIGSERIAL";
	st.executeUpdate("CREATE TABLE edoalexpr (intid "+SERIAL+", type INT, joinid BIGINT, PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE valueexpr (intid "+SERIAL+", type INT, joinid BIGINT, PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE instexpr (intid "+SERIAL+", uri VARCHAR(250), var VARCHAR(250), PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE literal (intid "+SERIAL+", type BIGINT, value VARCHAR(500), PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE typeexpr (intid "+SERIAL+", uri VARCHAR(250), PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE apply (intid "+SERIAL+", type INT, operation VARCHAR(255), PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE arglist (intid BIGINT NOT NULL, id BIGINT NOT NULL)");
	st.executeUpdate("CREATE TABLE classexpr (intid "+SERIAL+", type INT, joinid BIGINT, var VARCHAR(250), PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE classid (intid "+SERIAL+", uri VARCHAR(250), PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE classlist (intid BIGINT NOT NULL, id BIGINT NOT NULL)");
	st.executeUpdate("CREATE TABLE classrest (intid "+SERIAL+", path BIGINT, type INT, joinid BIGINT, var VARCHAR(250), PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE pathexpr (intid "+SERIAL+", type INT, joinid BIGINT, PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE propexpr (intid "+SERIAL+", type INT, joinid BIGINT, var VARCHAR(250), PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE propid (intid "+SERIAL+", uri VARCHAR(250), PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE proplist (intid BIGINT NOT NULL, id BIGINT NOT NULL)");
	st.executeUpdate("CREATE TABLE valuerest (intid "+SERIAL+", type INT, path BIGINT, comp VARCHAR(250), joinid BIGINT, var VARCHAR(250), PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE relexpr (intid "+SERIAL+", type INT, joinid BIGINT, var VARCHAR(250), PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE relid (intid "+SERIAL+", uri VARCHAR(250), PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE rellist (intid BIGINT NOT NULL, id BIGINT NOT NULL)");
	st.executeUpdate("CREATE TABLE transf (intid "+SERIAL+", cellid VARCHAR(255), type INT, joinid1 BIGINT, joinid2 BIGINT, PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE linkkey (intid "+SERIAL+", cellid VARCHAR(255), PRIMARY KEY (intid))");
	st.executeUpdate("CREATE TABLE binding (keyid BIGINT NOT NULL, type INT, joinid1 BIGINT, joinid2 BIGINT)");
    }

    public void resetDatabase( boolean force ) throws SQLException, AlignmentException {
	Statement st = null;
	try {
	    st = createStatement();
	    conn.setAutoCommit( false );
	    // Check that no one else is connected...
	    if ( force != true ){
		ResultSet rs = st.executeQuery("SELECT COUNT(*) AS rowcount FROM server WHERE edit=1");
		rs.next();
		int count = rs.getInt("rowcount") ;
		rs.close() ;
		if ( count > 1 ) {
		    throw new AlignmentException("Cannot init database: other processes use it");
		}
	    }
	    // Suppress old databases if exists
	    st.executeUpdate("DROP TABLE IF EXISTS alignmenturis");
	    st.executeUpdate("DROP TABLE IF EXISTS extension");
	    st.executeUpdate("DROP TABLE IF EXISTS cell");
	    st.executeUpdate("DROP TABLE IF EXISTS dependency");
	    st.executeUpdate("DROP TABLE IF EXISTS networkalignment");
	    st.executeUpdate("DROP TABLE IF EXISTS networkontology");
	    st.executeUpdate("DROP TABLE IF EXISTS network");
	    st.executeUpdate("DROP TABLE IF EXISTS alignment");
	    st.executeUpdate("DROP TABLE IF EXISTS ontology");
	    st.executeUpdate("DROP TABLE IF EXISTS server");

	    st.executeUpdate("DROP TABLE IF EXISTS edoalexpr");
	    st.executeUpdate("DROP TABLE IF EXISTS valueexpr");
	    st.executeUpdate("DROP TABLE IF EXISTS instexpr");
	    st.executeUpdate("DROP TABLE IF EXISTS literal");
	    st.executeUpdate("DROP TABLE IF EXISTS typeexpr");
	    st.executeUpdate("DROP TABLE IF EXISTS apply");
	    st.executeUpdate("DROP TABLE IF EXISTS arglist");
	    st.executeUpdate("DROP TABLE IF EXISTS classexpr");
	    st.executeUpdate("DROP TABLE IF EXISTS classid");
	    st.executeUpdate("DROP TABLE IF EXISTS classlist");
	    st.executeUpdate("DROP TABLE IF EXISTS classrest");
	    st.executeUpdate("DROP TABLE IF EXISTS pathexpr");
	    st.executeUpdate("DROP TABLE IF EXISTS propexpr");
	    st.executeUpdate("DROP TABLE IF EXISTS propid");
	    st.executeUpdate("DROP TABLE IF EXISTS proplist");
	    st.executeUpdate("DROP TABLE IF EXISTS valuerest");
	    st.executeUpdate("DROP TABLE IF EXISTS relexpr");
	    st.executeUpdate("DROP TABLE IF EXISTS relid");
	    st.executeUpdate("DROP TABLE IF EXISTS rellist");
	    st.executeUpdate("DROP TABLE IF EXISTS transf");
	    st.executeUpdate("DROP TABLE IF EXISTS linkkey");
	    st.executeUpdate("DROP TABLE IF EXISTS binding");
	    // Redo it
	    initDatabase();
	  
	    // Register *THIS* server, etc. characteristics (incl. version name)
	    registerServer( host, port, rights==1, idprefix );
	} catch ( SQLException sex ) {
	    logger.warn( "SQLError", sex );
	    conn.rollback();
	    throw sex;
	} finally {
	    if ( st != null ) st.close();
	    conn.setAutoCommit( true );
	}
    }
    
    private void registerServer( String host, String port, Boolean writeable, String prefix ) throws SQLException {
	// Register *THIS* server, etc. characteristics (incl. version name)
	try ( PreparedStatement pst = conn.prepareStatement("INSERT INTO server (host, port, edit, version, prefix) VALUES (?,?,?,?,?)") ) {
		pst.setString(1,host);
		pst.setString(2,port);
		pst.setBoolean(3,writeable);
		pst.setString(4,VERSION+"");
		pst.setString(5,idprefix);
		pst.executeUpdate();
	    }
    }

    /*
     * A dummy method, since it exists just ALTER TABLE ... DROP and ALTER TABLE ... ADD in SQL Language.
     * each dbms has its own language for manipulating table columns....
     */
    public void renameColumn(Statement st, String tableName, String oldName, String newName, String newType) throws SQLException { 
	try {
	    conn.setAutoCommit( false );
	    st.executeUpdate("ALTER TABLE "+tableName+" ADD "+newName+" "+newType);
	    st.executeUpdate("UPDATE "+tableName+" SET "+newName+"="+oldName);
	    st.executeUpdate("ALTER TABLE "+tableName+" DROP "+oldName);  
	} catch ( SQLException sex ) {
	    logger.warn( "SQLError", sex );
	    conn.rollback();
	    throw sex;
	} finally {
	    conn.setAutoCommit( true );
	}
    }
    
    /*
    * Another dummy method, since it exists just ALTER TABLE ... DROP and ALTER TABLE ... ADD in SQL Language.
    * each dbms has its own language for manipulating table columns....     
    */
    public void changeColumnType(Statement st, String tableName, String columnName, String newType) throws SQLException { 
	try {
	    conn.setAutoCommit( false );
	    String tempName = columnName+"temp";
	    renameColumn(st,tableName,columnName,tempName,newType);
	    renameColumn(st,tableName,tempName,columnName,newType);
	} catch ( SQLException sex ) {
	    logger.warn( "SQLError", sex );
	    conn.rollback();
	    throw sex;
	} finally {
	    conn.setAutoCommit( true );
	}
    }

    public void updateDatabase() throws SQLException, AlignmentException {
	try ( Statement st = createStatement() ) {
		// get the version number (port is the entry which is always here)
		ResultSet rs = st.executeQuery("SELECT version FROM server WHERE port='port'");
		rs.next();
		int version = rs.getInt("version") ;
		logger.debug( "Current version: {}", version );
		if ( version < VERSION ) {
		    if ( version >= 302 ) {
			if ( version < 310 ) {
			    logger.info( "Upgrading to version 3.1" );
			    // ALTER database
			    renameColumn(st,"extension","method","val","VARCHAR(500)");
			    // case mysql
			    //st.executeUpdate("ALTER TABLE extension CHANGE method val VARCHAR(500)");
			    st.executeUpdate("ALTER TABLE extension ADD uri VARCHAR(200);");
			    // Modify extensions
			    ResultSet rse = st.executeQuery("SELECT * FROM extension");
			    Statement st2 = createStatement();
			    while ( rse.next() ){
				String tag = rse.getString("tag");
				//logger.trace(" Treating tag {} of {}", tag, rse.getString("id"));
				if ( !tag.equals("") ){
				    int pos;
				    String ns;
				    String name;
				    if ( (pos = tag.lastIndexOf('#')) != -1 ) {
					ns = tag.substring( 0, pos );
					name = tag.substring( pos+1 );
				    } else if ( (pos = tag.lastIndexOf(':')) != -1 && pos > 5 ) {
					ns = tag.substring( 0, pos )+"#";
					name = tag.substring( pos+1 );
				    } else if ( (pos = tag.lastIndexOf('/')) != -1 ) {
					ns = tag.substring( 0, pos+1 );
					name = tag.substring( pos+1 );
				    } else {
					ns = Namespace.ALIGNMENT.uri;
					name = tag;
				    }
				    //logger.trace("  >> {} : {}", ns, name);
				    st2.executeUpdate("UPDATE extension SET tag='"+name+"', uri='"+ns+"' WHERE id='"+rse.getString("id")+"' AND tag='"+tag+"'");
				}
			    }
			}
			// Nothing to do with 340: subsumed by 400
			if ( version < 400 ) {
			    logger.info("Upgrading to version 4.0");
			    // ALTER database 
			    changeColumnType(st,"cell","relation", "VARCHAR(255)");
			    changeColumnType(st,"cell","uri1", "VARCHAR(255)");
			    changeColumnType(st,"cell","uri2", "VARCHAR(255)");
			    
			    changeColumnType(st,"alignment","level", "VARCHAR(255)");
			    changeColumnType(st,"alignment","uri1", "VARCHAR(255)");
			    changeColumnType(st,"alignment","uri2", "VARCHAR(255)");
			    changeColumnType(st,"alignment","file1", "VARCHAR(255)");
			    changeColumnType(st,"alignment","file2", "VARCHAR(255)");
			    
			    renameColumn(st,"alignment","owlontology1","ontology1", "VARCHAR(255)");
			    renameColumn(st,"alignment","owlontology2","ontology2", "VARCHAR(255)");
			}
			if ( version < 450 ) {
			    logger.info("Upgrading to version 4.5");
			    logger.info("Creating Ontology table");
			    st.executeUpdate("CREATE TABLE ontology (id VARCHAR(255), uri VARCHAR(255), source BOOLEAN, file VARCHAR(255), formname VARCHAR(50), formuri VARCHAR(255), primary key (id, source))");
			    ResultSet rse = st.executeQuery("SELECT * FROM alignment");
			    while ( rse.next() ){
				Statement st2 = createStatement();
				// No Ontology _type_ available then
				st2.executeUpdate("INSERT INTO ontology (id, uri, source, file) VALUES ('"+rse.getString("id")+"','"+rse.getString("uri1")+"','1','"+rse.getString("file1")+"')");
				st2.executeUpdate("INSERT INTO ontology (id, uri, source, file) VALUES ('"+rse.getString("id")+"','"+rse.getString("uri2")+"','0','"+rse.getString("file2")+"')");
			    }
			    logger.info("Cleaning up Alignment table");
			    st.executeUpdate("ALTER TABLE alignment DROP ontology1");  
			    st.executeUpdate("ALTER TABLE alignment DROP ontology2");  
			    st.executeUpdate("ALTER TABLE alignment DROP uri1");  
			    st.executeUpdate("ALTER TABLE alignment DROP uri2");  
			    st.executeUpdate("ALTER TABLE alignment DROP file1");  
			    st.executeUpdate("ALTER TABLE alignment DROP file2");  
			    logger.debug("Altering server table");
			    st.executeUpdate("ALTER TABLE server ADD prefix VARCHAR(50);");
			    st.executeUpdate("UPDATE server SET prefix='"+idprefix+"'");
			    logger.debug("Updating server with prefix");
			    Statement stmt = null;
			    try { // In all alignment
				conn.setAutoCommit( false );
				stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
							    ResultSet.CONCUR_UPDATABLE);
				ResultSet uprs = stmt.executeQuery( "SELECT id FROM alignment" );
				while ( uprs.next() ) {
				    String oldid = uprs.getString("id");
				    String newid = stripAlignmentUri( oldid );
				    //logger.trace("Updating {} to {}", oldid, newid );
				    uprs.updateString( "id", newid );
				    uprs.updateRow();
				    // In all cell (for id and cell_id)
				    st.executeUpdate("UPDATE cell SET id='"+newid+"' WHERE id='"+oldid+"'" );
				    // In all extension
				    st.executeUpdate("UPDATE extension SET id='"+newid+"' WHERE id='"+oldid+"'" );
				    // In all ontology
				    st.executeUpdate("UPDATE ontology SET id='"+newid+"' WHERE id='"+oldid+"'" );
				}
				// Now, for each cell, with an id,
				// either recast the id ... or not
				conn.commit();
			    } catch ( SQLException e ) {
				logger.warn( "IGNORED Failed to update", e );
			    } finally {
				if ( stmt != null ) { stmt.close(); }
				conn.setAutoCommit( true );
			    }
			    logger.info("Creating dependency table");
			    st.executeUpdate("CREATE TABLE dependency (id VARCHAR(255), dependsOn VARCHAR(255))");
			    logger.info("Fixing legacy errors in cached/stored");
			    rse = st.executeQuery("SELECT id FROM extension WHERE tag='stored' AND val=''");
			    Statement st2 = createStatement();
			    while ( rse.next() ) {
				ResultSet rse2 = st2.executeQuery("SELECT val FROM extension WHERE tag='stored' AND id='"+rse.getString("id")+"'");
				if ( rse2.next() ) {
				    st2.executeUpdate( "UPDATE extension SET val='"+rse2.getString("val")+"' WHERE tag='stored' AND id='"+rse.getString("id")+"'" );
				}
			    }
			    // This did not worked!
			    //st.executeUpdate( "UPDATE extension SET val=( SELECT e2.val FROM extension e2 WHERE e2.tag='cached' AND e2.id=extension.id ) WHERE tag='stored' AND val=''" );
			    // We should also implement a clean up (suppress all starting with http://)
			}
			if ( version < 465 ) { // Unfortunately 4.7 was released with 465 tag...
			    logger.info("Upgrading to version 4.7");
			    logger.info("Updating Alignment table");
			    st.executeUpdate("ALTER TABLE alignment ADD onto1 VARCHAR(255);");
			    st.executeUpdate("ALTER TABLE alignment ADD onto2 VARCHAR(255);");
			    ResultSet rse = st.executeQuery("SELECT uri,id,source FROM ontology");
			    Statement st2 = createStatement();
			    while ( rse.next() ){
				if ( rse.getBoolean("source") ) {
				    st2.executeUpdate("UPDATE alignment SET onto1='"+rse.getString("uri")+"' WHERE id='"+rse.getString("id")+"'");
				} else {
				    st2.executeUpdate("UPDATE alignment SET onto2='"+rse.getString("uri")+"' WHERE id='"+rse.getString("id")+"'");
				}
			    }
			    logger.info("Cleaning up ontology table");
			    st.executeUpdate("ALTER TABLE ontology RENAME TO oldont;");
			    st.executeUpdate("CREATE TABLE ontology (uri VARCHAR(255), formname VARCHAR(50), formuri VARCHAR(255), file VARCHAR(255), PRIMARY KEY (uri))");
			    st.executeUpdate("INSERT INTO ontology SELECT DISTINCT uri FROM oldont;");
			    st.executeUpdate("UPDATE ontology SET formname=oldont.formname, formuri=oldont.formuri, file=oldont.file FROM oldont WHERE ontology.uri = oldont.uri");
			    st.executeUpdate("DROP TABLE oldont;");
			    logger.info("Creating network tables");
			    st.executeUpdate("CREATE TABLE network (id VARCHAR(100), PRIMARY KEY (id))");
			    st.executeUpdate("CREATE TABLE networkontology (network VARCHAR(100), onto VARCHAR(255), FOREIGN KEY (network) REFERENCES network (id), FOREIGN KEY (onto) REFERENCES ontology (uri), PRIMARY KEY (network,onto))");
			    st.executeUpdate("CREATE TABLE networkalignment (network VARCHAR(100), align VARCHAR(100), FOREIGN KEY (network) REFERENCES network (id), FOREIGN KEY (align) REFERENCES alignment (id), PRIMARY KEY (network,align))");
			    // CREATE PRIMARY AND FOREIGN KEYS
			    logger.info("Adding foreign keys");
			    // suppress orphean cells (the reciprocal would delete empty alignments)
			    st.executeUpdate("DELETE FROM cell WHERE id NOT IN (SELECT al.id FROM alignment al)");
			    st.executeUpdate("ALTER TABLE cell ADD CONSTRAINT cellid FOREIGN KEY (id) REFERENCES alignment (id);");
			    st.executeUpdate("ALTER TABLE dependency ADD CONSTRAINT deppk PRIMARY KEY (id,dependsOn);");
			    st.executeUpdate("ALTER TABLE dependency ADD CONSTRAINT depalfk FOREIGN KEY (id) REFERENCES alignment (id);");
			    st.executeUpdate("ALTER TABLE dependency ADD CONSTRAINT aldepfk FOREIGN KEY (dependsOn) REFERENCES alignment (id);");
			    st.executeUpdate("ALTER TABLE alignment ADD CONSTRAINT alon1fk FOREIGN KEY (onto1) REFERENCES ontology (uri);");
			    st.executeUpdate("ALTER TABLE alignment ADD CONSTRAINT alon2fk FOREIGN KEY (onto2) REFERENCES ontology (uri);");
			    //This was already the primary key
			    //st.executeUpdate("ALTER TABLE alignment ADD CONSTRAINT PRIMARY KEY (id);");
			    // ADDED TABLE FOR MULTIPLE URIs
			    // URIINdex:
			    logger.info("Creating URI index table");
			    st.executeUpdate("CREATE TABLE alignmenturis (id varchar(100), uri varchar(255), prefered boolean);");
			    // CHANGE EXTENSION NAMESPACE
			    logger.info("Changing extension namespaces");
			    // Normalise
			    st2.executeUpdate("UPDATE extension SET uri='"+Namespace.ALIGNMENT.uri+"#' WHERE uri='"+Namespace.ALIGNMENT.uri+"'");
			    st2.executeUpdate("UPDATE extension SET uri='"+Namespace.EXT.uri+"' WHERE uri='"+Namespace.ALIGNMENT.uri+"#' AND (tag='time' OR tag='method' OR tag='pretty')");
			}
			if ( version < 480 ) {
			    logger.info("Upgrading to version 4.8");
			    if ( version < 471 ) {
				logger.info("Creating EDOAL tables");
				initEDOALTables( createStatement() );
			    }
			    if ( version < 472 ) {
				logger.info("Adding reltype attribute");
				st.executeUpdate("ALTER TABLE alignment ADD reltype VARCHAR(255);");
				st.executeUpdate("ALTER TABLE alignment ADD conftype VARCHAR(255);");
			    }
			}
			// Version 490 does not change anything in the format
			// So no increment
			// ALTER version
			st.executeUpdate("UPDATE server SET version='"+VERSION+"'");
		    } else throw new AlignmentException( "Database must be upgraded ("+version+" -> "+VERSION+")" );
		}
	    }
    }    
}
