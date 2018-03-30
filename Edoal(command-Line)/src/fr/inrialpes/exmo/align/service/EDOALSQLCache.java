/*
 * $Id: EDOALSQLCache.java 2150 2017-07-18 15:15:46Z euzenat $
 *
 * Copyright (C) INRIA, 2014-2017
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 */

/*
 * This implementation works. However, it may be inneficient in terms of time
 * or space taken in the database (too many tables and too many indirections).
 * It could be replaced by another.
 *
 * Caveats:
 * - Another problem is the status of this class. It may be better to make it a
 *   subclass of SQLCache...
 */

package fr.inrialpes.exmo.align.service;

import java.util.List;
import java.util.Vector;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;

import fr.inrialpes.exmo.align.parser.SyntaxElement.Constructor;

import fr.inrialpes.exmo.align.impl.edoal.Id;
import fr.inrialpes.exmo.align.impl.edoal.PathExpression;
import fr.inrialpes.exmo.align.impl.edoal.Expression;
import fr.inrialpes.exmo.align.impl.edoal.ClassExpression;
import fr.inrialpes.exmo.align.impl.edoal.ClassId;
import fr.inrialpes.exmo.align.impl.edoal.ClassConstruction;
import fr.inrialpes.exmo.align.impl.edoal.ClassRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassTypeRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassValueRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassOccurenceRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyExpression;
import fr.inrialpes.exmo.align.impl.edoal.PropertyId;
import fr.inrialpes.exmo.align.impl.edoal.PropertyConstruction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyTypeRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyValueRestriction;
import fr.inrialpes.exmo.align.impl.edoal.RelationId;
import fr.inrialpes.exmo.align.impl.edoal.RelationExpression;
import fr.inrialpes.exmo.align.impl.edoal.RelationConstruction;
import fr.inrialpes.exmo.align.impl.edoal.RelationRestriction;
import fr.inrialpes.exmo.align.impl.edoal.RelationDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.RelationCoDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.InstanceId;
import fr.inrialpes.exmo.align.impl.edoal.InstanceExpression;

import fr.inrialpes.exmo.align.impl.edoal.Transformation;
import fr.inrialpes.exmo.align.impl.edoal.ValueExpression;
import fr.inrialpes.exmo.align.impl.edoal.Value;
import fr.inrialpes.exmo.align.impl.edoal.Apply;
import fr.inrialpes.exmo.align.impl.edoal.Aggregate;
import fr.inrialpes.exmo.align.impl.edoal.Datatype;
import fr.inrialpes.exmo.align.impl.edoal.EDOALCell;
import fr.inrialpes.exmo.align.impl.edoal.EDOALVisitor;
import fr.inrialpes.exmo.align.impl.edoal.Linkkey;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyBinding;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyEquals;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyIntersects;
import fr.inrialpes.exmo.align.impl.edoal.Comparator;

/**
 * Stores an EDOAL expression in SQL
 * and extract EDOAL expressions from the database
 *
 * JE 2017 question: why isn't this an extension of SQLCache
 *
 * @author Jérôme Euzenat
 * @version $Id: EDOALSQLCache.java 2150 2017-07-18 15:15:46Z euzenat $
 * 
 */

public class EDOALSQLCache {
    final static Logger logger = LoggerFactory.getLogger(EDOALSQLCache.class);

    private boolean isPattern = false;

    DBService service = null;
    Connection conn = null;

    // These identifiers are used for indicating in database tables in which table to look for element
    // (and additionally, what is the constructor)
    // id identifier
    public static final int ID = 0;
    // class constructor
    public static final int AND = 1;
    public static final int OR = 2;
    public static final int NOT = 3;
    // property constructor
    public static final int COMP = 4;
    // relation constructor
    public static final int INV = 5;
    public static final int SYM = 6;
    public static final int TRANS = 7;
    public static final int REFL = 8;
    // class restriction
    public static final int OCC_GEQ = 11;
    public static final int OCC_LEQ = 12;
    public static final int OCC_EQ = 13;
    public static final int ALL = 14;
    public static final int EXIST = 15;
    // property restriction
    public static final int DOM = 21;
    public static final int TYP = 22;
    public static final int VAL = 23;
    // relation restriction
    public static final int COD = 24;
    // relation restriction
    // PATH and VALUE ENTITY
    public static final int LIT = 31;
    public static final int REL = 32;
    public static final int PPT = 33;
    public static final int INST = 34;
    // Binding types
    public static final int EQUAL_KEY = 41;
    public static final int INTER_KEY = 42;
    // Transformation types
    public static final int OO = 51;
    public static final int O_ = 52;
    public static final int _O = 53;

    public static final int VALUE = 1;
    public static final int INSTANCE = 2;
    public static final int PATH = 3;
    public static final int APPLY = 4;
    public static final int RELATION = 5;
    public static final int PROPERTY = 6;
    public static final int CLASS = 7;
    public static final int REST = 8;
    public static final int AGGREGATE = 9;

    public EDOALSQLCache( DBService serv ) {
	service = serv;
    }

    // ***TODO***
    // TODO: Massively use prepared queries 
    // Won't we face a problem with constant deconnection?
    // Wont't it be necessary to reconnect?

    private PreparedStatement findLinkkey;
    private PreparedStatement findTransformation;
    private PreparedStatement insertLinkkey;
    private PreparedStatement findExprJoins;
    private PreparedStatement findClassJoins;
    private PreparedStatement findClassUri;
    private PreparedStatement findClassConst;
    private PreparedStatement insertClassConst;
    private PreparedStatement findClassRestr;
    private PreparedStatement insertClassRestr;
    private PreparedStatement findClassValueRestr;
    private PreparedStatement insertClassValueRestr;
    private PreparedStatement findPathExpr;
    private PreparedStatement findPropExpr;
    private PreparedStatement findPropId;
    private PreparedStatement findPropConst;
    private PreparedStatement insertPropConst;
    private PreparedStatement findPropValueRest;
    private PreparedStatement insertPropValueRest;
    private PreparedStatement findRelExpr;
    private PreparedStatement findRelId;
    private PreparedStatement findIdByUri;
    private PreparedStatement insertId;
    private PreparedStatement insertTransf;
    private PreparedStatement findBindings;
    private PreparedStatement insertBinding;
    private PreparedStatement insertLiteral;
    private PreparedStatement insertTypedLiteral;

    public void compileQueries() throws SQLException {
	try {
	    findLinkkey = conn.prepareStatement( "SELECT intid FROM linkkey WHERE cellid=?" );
	    findTransformation = conn.prepareStatement( "SELECT type,joinid1,joinid2 FROM transf WHERE cellid=?" );
	    insertLinkkey = createInsertStatement( "INSERT INTO linkkey (cellid) VALUES (?)" );
	    findExprJoins = conn.prepareStatement( "SELECT type,joinid FROM edoalexpr WHERE intid='?'" );
	    findClassJoins = conn.prepareStatement( "SELECT type,joinid FROM classexpr WHERE intid='?'" );
	    findClassUri = conn.prepareStatement( "SELECT uri FROM classid WHERE intid='?'" );
	    findClassConst = conn.prepareStatement( "SELECT id FROM classlist WHERE intid='?' ORDER BY id" );
	    insertClassConst = createInsertStatement( "INSERT INTO classlist (intid,id) VALUES ('?','?')" );
	    findClassRestr = conn.prepareStatement( "SELECT path,type,joinid FROM classrest WHERE intid='?'" );
	    insertClassRestr = createInsertStatement( "INSERT INTO classrest (path,type,joinid) VALUES (?,?,?)" );
	    findClassValueRestr = conn.prepareStatement( "SELECT comp,joinid FROM valuerest WHERE type='?' AND intid='?'" );
	    insertClassValueRestr = createInsertStatement( "INSERT INTO valuerest (type,comp,joinid) VALUES (?,?,?)" );
	    findPathExpr = conn.prepareStatement( "SELECT type,joinid FROM pathexpr WHERE intid='?'" );
	    findPropExpr = conn.prepareStatement( "SELECT type,joinid FROM propexpr WHERE intid='?'" );
	    findPropId = conn.prepareStatement( "SELECT uri FROM propid WHERE intid='?'" );
	    findPropConst = conn.prepareStatement( "SELECT id FROM proplist WHERE intid='?' ORDER BY id" );
	    insertPropConst = createInsertStatement( "INSERT INTO proplist (intid,id) VALUES ('?','?')" );
	    findPropValueRest = conn.prepareStatement( "SELECT comp,joinid FROM valuerest WHERE type='?' AND intid='?'" );
	    insertPropValueRest = createInsertStatement( "INSERT INTO valuerest (type,comp,joinid) VALUES (?,?,?)" );
	    findRelExpr = conn.prepareStatement( "SELECT type,joinid FROM relexpr WHERE intid='?'" );
	    findRelId = conn.prepareStatement( "SELECT uri FROM relid WHERE intid='?'" );
	    findIdByUri = conn.prepareStatement( "SELECT intid FROM ? WHERE uri='?'" );
	    insertId = createInsertStatement( "INSERT INTO ? (uri) VALUES (?)" );
	    insertTransf = createInsertStatement( "INSERT INTO transf (cellid,type,joinid1,joinid2) VALUES (?,?,?,?)" );
	    findBindings = createStatement( "SELECT type,joinid1,joinid2 FROM binding WHERE keyid='?'" );
	    insertBinding = createInsertStatement( "INSERT INTO binding (keyid,type,joinid1,joinid2) VALUES (?,?,?,?)" );
	    insertLiteral = createInsertStatement( "INSERT INTO literal (value) VALUES (?)" );
	    insertTypedLiteral = createInsertStatement( "INSERT INTO literal (value,type) VALUES (?,'?')" );
	} catch ( SQLException sex ) {
	    logger.info( "Cannot initialize queries: Unexpected problem" );
	    throw sex;
	}
    }
    
    // PS: to disapear
    public Statement createStatement() throws SQLException {
	return service.getConnection().createStatement();
    }

    public PreparedStatement createStatement( String query ) throws SQLException {
	return service.getConnection().prepareStatement( query );
    }

    public PreparedStatement createInsertStatement( String updatePattern ) throws SQLException {
	return service.getConnection().prepareStatement( updatePattern, Statement.RETURN_GENERATED_KEYS );
    }

    public long executeUpdateWithId( PreparedStatement st, String msg ) throws SQLException {
	if ( st.executeUpdate() != 0 ) {
	    try ( ResultSet generatedKeys = st.getGeneratedKeys() ) {
		    if ( generatedKeys.next() ) {
			return generatedKeys.getLong(1);
		    } else {
			throw new SQLException("Creating "+msg+" entry failed, no ID obtained.");
		    }
		}
	} else throw new SQLException("Creating "+msg+" enty failed.");
    }

    public void init() throws AlignmentException {
	// Should be done at the begining, but not necessarily in init
	try {
	    conn = service.getConnection();
	    // service must be initialised (from SQLCache likely)
	    conn.setAutoCommit( false );
	    // visit something
	    compileQueries();
	} catch ( SQLException sqlex ) {
	    throw new AlignmentException( "Cannot connect to database", sqlex );
	    //} catch ( AlignmentException alex ) {
	} finally {
	    try { conn.setAutoCommit( true ); } catch ( SQLException sqlex ) {}
	}
    }

    // ***TODO***
    // Deal with variables here...
    // if (isPattern) { renderVariables(e); }
    public void renderVariables( Expression expr ) {
	/*
        if (expr.getVariable() != null) {
            writer.print(" " + SyntaxElement.VAR.print(DEF) + "=\"" + expr.getVariable().name());
	    }*/
    }

    // This is only for the expressions which are in correspondences

    // **TODO**
    public void erase( EDOALCell cell ) {
    }

    public Expression extractExpression( long intid ) throws SQLException, AlignmentException {
	logger.trace( "extractExpression for Id = {} ", intid );
	try ( Statement st = createStatement() ) {
	    //PS=findJoins
		ResultSet rs = st.executeQuery( "SELECT type,joinid FROM edoalexpr WHERE intid='"+intid+"'" );
		if ( !rs.next() ) throw new AlignmentException( "Cannot retrieve EDOAL expression : "+intid );
		int type = rs.getInt( "type" );
		if ( type == RELATION ) return extractRelationExpression( rs.getLong( "joinid" ) );
		else if ( type == PROPERTY ) return extractPropertyExpression( rs.getLong( "joinid" ) );
		else if ( type == INSTANCE ) return extractInstanceExpression( rs.getLong( "joinid" ) );
		else return extractClassExpression( rs.getLong( "joinid" ) );
	    }
    }

    public long visit( final Expression e ) throws SQLException, AlignmentException {
	long intid;
	int type;
	if ( e instanceof ClassExpression ) {
	    intid = visit( (ClassExpression)e );
	    type = CLASS;
	} else if ( e instanceof PropertyExpression ) {
	    intid = visit( (PropertyExpression)e );
	    type = PROPERTY;
	} else if ( e instanceof RelationExpression ) {
	    intid = visit( (RelationExpression)e );
	    type = RELATION;
	} else if ( e instanceof InstanceExpression ) {
	    intid = visit( (InstanceExpression)e );
	    type = INSTANCE;
	} else throw new AlignmentException( "Invalid expression type in a correspondence : "+e );
	return registerExpression( "edoalexpr", type, intid );
    }

    public ClassExpression extractClassExpression( long intid ) throws SQLException, AlignmentException {
	try ( Statement st = createStatement() ) {
	    // PS=findClassJoins;
		ResultSet rs = st.executeQuery( "SELECT type,joinid FROM classexpr WHERE intid='"+intid+"'" );
		if ( rs.next() ) {
		    int type = rs.getInt( "type" );
		    if ( type == ID ) return extractClassId( rs.getLong( "joinid" ) );
		    else if ( type == OR || type == AND || type == NOT ) 
			return extractClassConstruction( type, rs.getLong( "joinid" ) );
		    else if ( type == REST )
			return extractClassRestriction( rs.getLong( "joinid" ) );
		    else throw new AlignmentException( "Invalid class expression type : "+type );
		} else {
		    throw new AlignmentException( "Cannot retrieve class expression "+intid );
		}
	    }
    }

    public long visit( final ClassExpression e ) throws SQLException, AlignmentException {
	if ( e instanceof ClassId ) return visit( (ClassId)e );
	else if ( e instanceof ClassConstruction ) return visit( (ClassConstruction)e );
	else if ( e instanceof ClassRestriction ) return visit( (ClassRestriction)e );
	else throw new AlignmentException( "Invalid ClassExpression type: "+e );
    }

    public ClassId extractClassId( long intid ) throws SQLException, AlignmentException {
	try ( Statement st = createStatement() ) {
	    // PS=findClassUri;
		ResultSet rs = st.executeQuery( "SELECT uri FROM classid WHERE intid='"+intid+"'" );
		if ( !rs.next() ) throw new AlignmentException( "Cannot retrieve class id : "+intid );
		try {
		    logger.trace( "Identified ClassId = {}", rs.getString( "uri" ) );
		    return new ClassId( new URI( rs.getString( "uri" ) ) );
		    
		} catch ( URISyntaxException urisex ) {
		    throw new AlignmentException( "Invalid URI", urisex );
		}
	    }
    }

    public long visit( final ClassId e ) throws SQLException, AlignmentException {
	long idres = registerId( e, "classid" );
	return registerExpression( "classexpr", ID, idres );
    }

    public ClassConstruction extractClassConstruction( int op, long intid ) throws SQLException, AlignmentException {
	Constructor constr = null;
	if ( op == AND ) constr = Constructor.AND;
	else if ( op == OR ) constr = Constructor.OR;
	else if ( op == NOT ) constr = Constructor.NOT;
	else throw new AlignmentException( "Invalid operator "+op );
	List<ClassExpression> expressions = new Vector<ClassExpression>();
	try ( Statement st = createStatement() ) {
	    // PS=findClassConst
		ResultSet rs = st.executeQuery( "SELECT id FROM classlist WHERE intid='"+intid+"' ORDER BY id" );
		while ( rs.next() ) {
		    expressions.add( extractClassExpression( rs.getLong( "id" ) ) );
		}
		return new ClassConstruction( constr, expressions );
	    }
    }

    public long visit( final ClassConstruction e ) throws SQLException, AlignmentException {
	// Get the constructor
	final Constructor op = e.getOperator();
	int type;
	if ( op == Constructor.OR ) type = OR;
	else if ( op == Constructor.AND ) type = AND;
	else if ( op == Constructor.NOT ) type = NOT;
	else throw new AlignmentException( "Invalid constructor "+op );
	if ( (op == Constructor.OR) || (op == Constructor.AND) ) {
	    // Create the relexpr
	    long exprres = registerExpression( "classexpr", type, 0 );
	    // Iterate on components
	    try ( Statement st = createStatement() ) {
		    for ( final ClassExpression ce : e.getComponents() ) {
			long pres = visit( ce );
			// PS=insertClassConst;
			st.executeUpdate( "INSERT INTO classlist (intid,id) VALUES ('"+exprres+"','"+pres+"')" );
		    }
		}
	    // Return the relexpr
	    return exprres;
	} else { // NOT
	    final ClassExpression ce = e.getComponents().iterator().next(); // OK...
	    // Create the component
	    long pres = visit( ce );
	    // Create the relexpr
	    return registerExpression( "classexpr", type, pres );
	}
    }
		
    public ClassRestriction extractClassRestriction( long intid ) throws SQLException, AlignmentException {
	try ( Statement st = createStatement() ) {
	    // PS=findClassRestr
		ResultSet rs = st.executeQuery( "SELECT path,type,joinid FROM classrest WHERE intid='"+intid+"'" );
		if ( !rs.next() ) throw new AlignmentException( "Cannot retrieve class restriction "+intid );
		int type = rs.getInt( "type" );
		PathExpression pe = extractPathExpression( rs.getLong( "path" ) );
		if ( type >= OCC_GEQ && type <= OCC_EQ ) {
		    int val = rs.getInt( "joinid" ); // HERE COULD BE A PROBLEM
		    Comparator comp = null;
		    if ( type == OCC_GEQ ) comp = Comparator.GREATER;
		    else if ( type == OCC_LEQ ) comp = Comparator.LOWER;
		    else if ( type == OCC_EQ ) comp = Comparator.EQUAL;
		    return new ClassOccurenceRestriction( pe, comp, val );
		} else if ( type == DOM ) 
		    return new ClassDomainRestriction( pe, extractClassExpression( rs.getLong( "joinid" ) ) );
		else if ( type == TYP ) 
		    return new ClassTypeRestriction( pe, new Datatype( extractDatatype( rs.getLong( "joinid" ) ).toString() ) );
		else if ( type == VAL )
		    return extractClassValueRestriction( pe, rs.getLong( "joinid" ) );
		else throw new AlignmentException( "Incorect class restriction type "+type );
	    }
    }

    public long visit( final ClassRestriction e ) throws SQLException, AlignmentException {
	long idres;
	long pathid = visit( e.getRestrictionPath() );
	if ( e instanceof ClassValueRestriction ) idres = visit( pathid, (ClassValueRestriction)e );
	else if ( e instanceof ClassTypeRestriction ) idres = visit( pathid, (ClassTypeRestriction)e );
	else if ( e instanceof ClassDomainRestriction ) idres = visit( pathid, (ClassDomainRestriction)e );
	else if ( e instanceof ClassOccurenceRestriction ) idres = visit( pathid, (ClassOccurenceRestriction)e );
	else throw new AlignmentException( "Invalid ClassExpression type: "+e );
	return registerExpression( "classexpr", REST, idres );
    }

    public ClassValueRestriction extractClassValueRestriction( PathExpression pe, long intid ) throws SQLException, AlignmentException {
	// PS=findClassValueRestr
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT comp,joinid FROM valuerest WHERE type='"+CLASS+"' AND intid='"+intid+"'" );
		if ( !rs.next() ) throw new AlignmentException( "Cannot retrieve class value restriction "+intid );
		Comparator comp;
		try {
		    comp = Comparator.getComparator( new URI( rs.getString( "comp" ) ) );
		} catch (URISyntaxException urisex) {
		    throw new AlignmentException( "Invalid comparator URI : "+rs.getString( "comp" ) );
		}
		ValueExpression ve = extractValueExpression( rs.getLong( "joinid" ) );
		return new ClassValueRestriction( pe, comp, ve );
	    }
    }

    public long visit( long pathid, final ClassValueRestriction c ) throws SQLException, AlignmentException {
	// Create the restriction
	long val = visit( c.getValue() );
	String uri = c.getComparator().toString(); // what about retrieving?
	// Register it in value rest
	// PS=insertClassValueRestr
	PreparedStatement st2 = createInsertStatement( "INSERT INTO valuerest (type,comp,joinid) VALUES (?,?,?)" );
	st2.setInt( 1, CLASS );
	st2.setString( 2, uri );
	st2.setLong( 3, val );
	long res = executeUpdateWithId( st2, "class value restriction" );
	// Register it finally
	return registerClassRestriction( VAL, pathid, res );
    }

    public long visit( long pathid, final ClassTypeRestriction c ) throws SQLException, AlignmentException {
	// Create the restriction
	long typ = visit( c.getType() );
	// Register it
	return registerClassRestriction( TYP, pathid, typ );
    }

    public long visit( long pathid, final ClassDomainRestriction c ) throws SQLException, AlignmentException {
	// Create the restriction
	long dom = visit( c.getDomain() );
	// Register it
	return registerClassRestriction( c.isUniversal()?ALL:EXIST, pathid, dom );
    }

    public long visit( long pathid, final ClassOccurenceRestriction c ) throws SQLException, AlignmentException {
	// Create the restriction
	int val = c.getOccurence();
	Comparator comp = c.getComparator();
	// Register it
	int type;
	if ( Comparator.EQUAL.equals( comp ) ) type = OCC_EQ;
	else if ( Comparator.GREATER.equals( comp ) ) type = OCC_GEQ;
	else if ( Comparator.LOWER.equals( comp ) ) type = OCC_LEQ;
	else throw new AlignmentException( "Cannot deal with cardinality comparator "+comp );
	return registerClassRestriction( type, pathid, val );
    }

    public long registerClassRestriction( int type, long path, long joinid ) throws SQLException, AlignmentException {
	// PS=insertClassRest
	try ( PreparedStatement st2 = createInsertStatement( "INSERT INTO classrest (path,type,joinid) VALUES (?,?,?)" ) ) {
		st2.setLong( 1, path );
		st2.setInt( 2, type );
		st2.setLong( 3, joinid );
		return executeUpdateWithId( st2, "class restriction" );
	    }
    }

    public PathExpression extractPathExpression( long intid ) throws SQLException, AlignmentException {
	// PS=findPathExpr
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT type,joinid FROM pathexpr WHERE intid='"+intid+"'" );
		if ( !rs.next() ) throw new AlignmentException( "Cannot retrieve path : "+intid );
		if ( rs.getInt( "type" ) == RELATION ) return extractRelationExpression( rs.getLong( "joinid" ) );
		else return extractPropertyExpression( rs.getLong( "joinid" ) );
	    }
    }

    public long visit( PathExpression e ) throws SQLException, AlignmentException {
	long intres;
	int type;
	if ( e instanceof PropertyExpression ) {
	    intres = visit( (PropertyExpression)e );
	    type = PROPERTY;
	} else if ( e instanceof RelationExpression ) {
	    intres = visit( (RelationExpression)e );
	    type = RELATION;
	} else throw new AlignmentException( "Invalid ClassExpression type: "+e );
	return registerExpression( "pathexpr", type, intres );
    }

    public PropertyExpression extractPropertyExpression( long intid ) throws SQLException, AlignmentException {
	// PS=findPropExpr
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT type,joinid FROM propexpr WHERE intid='"+intid+"'" );
		if ( rs.next() ) {
		    int type = rs.getInt( "type" );
		    if ( type == ID ) return extractPropertyId( rs.getLong( "joinid" ) );
		    else if ( type == OR || type == AND || type == COMP || type == NOT ) 
			return extractPropertyConstruction( type, rs.getLong( "joinid" ) );
		    else if ( type == DOM ) return extractPropertyDomainRestriction( rs.getLong( "joinid" ) );
		    else if ( type == TYP ) return extractPropertyTypeRestriction( rs.getLong( "joinid" ) );
		    else if ( type == VAL ) return extractPropertyValueRestriction( rs.getLong( "joinid" ) );
		    else throw new AlignmentException( "Invalid property expression type : "+type );
		} else {
		    throw new AlignmentException( "Cannot retrieve property expression "+intid );
		}
	    }
    }

    public long visit( PropertyExpression e ) throws SQLException, AlignmentException {
	if ( e instanceof PropertyValueRestriction ) {
	    return visit( (PropertyValueRestriction)e );
	} else if ( e instanceof PropertyTypeRestriction ) {
	    return visit( (PropertyTypeRestriction)e );
	} else if ( e instanceof PropertyDomainRestriction ) {
	    return visit( (PropertyDomainRestriction)e );
	} else if ( e instanceof PropertyId ) {
	    return visit( (PropertyId)e );
	} else if ( e instanceof PropertyConstruction ) {
	    return visit( (PropertyConstruction)e ); // It does the job...
	} else throw new AlignmentException( "Invalid property expression "+e );
    }

    public PropertyId extractPropertyId( long intid ) throws SQLException, AlignmentException {
	// PS=findPropId
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT uri FROM propid WHERE intid='"+intid+"'" );
		if ( rs.next() ) {
		    try {
			return new PropertyId( new URI( rs.getString( "uri" ) ) );
		    } catch (URISyntaxException uriex) {
			throw new AlignmentException( "Badly formatted URI "+rs.getString("uri"), uriex );
		    }
		} else {
		    throw new AlignmentException( "Cannot retrieve property "+intid );
		}
	    }
    }

    public long visit( PropertyId e ) throws SQLException {
	long idres = registerId( e, "propid" );
	return registerExpression( "propexpr", ID, idres );
    }

    // *Beware that these are well registered in PathExpression first*
    public PropertyConstruction extractPropertyConstruction( int op, long intid ) throws SQLException, AlignmentException {
	Constructor constr = null;
	if ( op == AND ) constr = Constructor.AND;
	else if ( op == OR ) constr = Constructor.OR;
	else if ( op == NOT ) constr = Constructor.NOT;
	else if ( op == COMP ) constr = Constructor.COMP;
	else throw new AlignmentException( "Invalid operator "+op );
	List<PathExpression> expressions = new Vector<PathExpression>();
	// PS=findPropConst
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT id FROM proplist WHERE intid='"+intid+"' ORDER BY id" );
		while ( rs.next() ) {
		    expressions.add( extractPathExpression( rs.getLong( "id" ) ) );
		}
		return new PropertyConstruction( constr, expressions );
	    }
    }

    public long visit( final PropertyConstruction e ) throws SQLException, AlignmentException {
	// Get the constructor
	final Constructor op = e.getOperator();
	int type;
	if ( op == Constructor.OR ) type = OR;
	else if ( op == Constructor.AND ) type = AND;
	else if ( op == Constructor.COMP ) type = COMP;
	else if ( op == Constructor.NOT ) type = NOT;
	else throw new AlignmentException( "Invalid constructor "+op );
	if ((op == Constructor.OR) || (op == Constructor.AND) || (op == Constructor.COMP)) {
	    // Create the relexpr
	    long exprres = registerExpression( "propexpr", type, 0 );
	    // Iterate on components
	    // PS=insertPropConst
	    try ( Statement st = createStatement() ) {
		    for ( final PathExpression re : e.getComponents() ) {
			long pres = visit( re );
			st.executeUpdate( "INSERT INTO proplist (intid,id) VALUES ('"+exprres+"','"+pres+"')" );
		    }
		}
	    // Return the propexpr
	    return exprres;
	} else { // NOT
	    final PathExpression re = e.getComponents().iterator().next(); // OK...
	    // Create the component
	    long pres = visit( re );
	    // Create the relexpr
	    return registerExpression( "propexpr", type, pres );
	}
    }

    public PropertyValueRestriction extractPropertyValueRestriction( long intid ) throws SQLException, AlignmentException {
	// PS=findPropValueRest
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT comp,joinid FROM valuerest WHERE type='"+PROPERTY+"' AND intid='"+intid+"'" );
		if ( !rs.next() ) throw new AlignmentException( "Cannot retrieve property value restriction "+intid );
		Comparator comp;
		try {
		    comp = Comparator.getComparator( new URI( rs.getString( "comp" ) ) );
		} catch (URISyntaxException urisex) {
		    throw new AlignmentException( "Invalid comparator URI : "+rs.getString( "comp" ) );
		}
		ValueExpression ve = extractValueExpression( rs.getLong( "joinid" ) );
		return new PropertyValueRestriction( comp, ve );
	    }
    }

    public long visit( final PropertyValueRestriction c ) throws SQLException, AlignmentException {
	// Create the restriction
	long val = visit( c.getValue() );
	String uri = c.getComparator().toString(); // what about retrieving?
	long res = 0;
	// Register it in value rest
	// PS=insertPropValueRest
	try ( PreparedStatement st2 = createInsertStatement( "INSERT INTO valuerest (type,comp,joinid) VALUES (?,?,?)" ) ) {
		st2.setInt( 1, PROPERTY );
		st2.setString( 2, uri );
		st2.setLong( 3, val );
		res = executeUpdateWithId( st2, "property value restriction" );
		// Register it finally
		return registerExpression( "propexpr", VAL, res );
	    }
    }

    public PropertyDomainRestriction extractPropertyDomainRestriction( long intid ) throws SQLException, AlignmentException {
	return new PropertyDomainRestriction( extractClassExpression( intid ) );
    }

    public long visit( final PropertyDomainRestriction c ) throws SQLException, AlignmentException {
	// Create the domain restriction
	long dom = visit( c.getDomain() );
	// Register it
	return registerExpression( "propexpr", DOM, dom );
    }

    public PropertyTypeRestriction extractPropertyTypeRestriction( long intid ) throws SQLException, AlignmentException {
	return new PropertyTypeRestriction( new Datatype( extractDatatype( intid ).toString() ) );
    }

    public long visit( final PropertyTypeRestriction c ) throws SQLException, AlignmentException {
	// Create the domain restriction
	long typ = visit( c.getType() );
	// Register it
	return registerExpression( "propexpr", TYP, typ );
    }

    public RelationExpression extractRelationExpression( long intid ) throws SQLException, AlignmentException {
	// PS=findRelExpr
	try ( Statement st = createStatement() ) {
		// Get the relexpr entry (operator,id)
		ResultSet rs = st.executeQuery( "SELECT type,joinid FROM relexpr WHERE intid='"+intid+"'" );
		if ( !rs.next() ) throw new AlignmentException( "Cannot find relation expression "+intid );
		int op = rs.getInt( "type" );
		if ( op == ID ) {
		    return extractRelationId( rs.getLong( "joinid" ) );
		} else if ( op == DOM ) {
		    return extractRelationDomainRestriction( rs.getLong( "joinid" ) );
		} else if ( op == COD ) {
		    return extractRelationCoDomainRestriction( rs.getLong( "joinid" ) );
		} else {
		    return extractRelationConstruction( op, rs.getLong( "joinid" ) );
		}
	    }
    }

    public long visit( RelationExpression e ) throws SQLException, AlignmentException {
	if ( e instanceof RelationRestriction ) {
	    return visit( (RelationRestriction)e );
	} else if ( e instanceof RelationId ) {
	    return visit( (RelationId)e );
	} else if ( e instanceof RelationConstruction ) {
	    return visit( (RelationConstruction)e ); // It does the job...
	} else throw new AlignmentException( "Invalid relation expression "+e );
    }
    

    public RelationId extractRelationId( long intid ) throws SQLException, AlignmentException {
	// findRelId
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT uri FROM relid WHERE intid='"+intid+"'" );
		if ( rs.next() ) {
		    try {
			return new RelationId( new URI( rs.getString( "uri" ) ) );
		    } catch (URISyntaxException uriex) {
			throw new AlignmentException( "Badly formatted URI "+rs.getString("uri"), uriex );
		    }
		} else {
		    throw new AlignmentException( "Cannot retrieve relation "+intid );
		}
	    }
    }

    public long visit( RelationId e ) throws SQLException {
	long idres = registerId( e, "relid" );
	return registerExpression( "relexpr", ID, idres );
    }

    public long registerId( Id expr, String tablename ) throws SQLException {
	// If it exists do not add:
	String uri = expr.getURI().toString();
	//logger.trace( "Register Id: "+"SELECT intid FROM "+tablename+" WHERE uri='"+uri+"'" );
	// PS=findIdByUri
	try ( Statement st = createStatement() ) {
	    ResultSet rs = st.executeQuery( "SELECT intid FROM "+tablename+" WHERE uri='"+uri+"'" );
		if ( rs.next() ) {
		    return rs.getLong("intid");
		    // PS=insertId
		} else try ( PreparedStatement st2 = createInsertStatement( "INSERT INTO ? (uri) VALUES (?)" ) ) {
			    st2.setString( 1, tablename );
			    st2.setString( 2, uri );
			    return executeUpdateWithId( st2, tablename );
			}
	    }
    }

    public long registerExpression( String tablename, int type, long join ) throws SQLException {
	try ( PreparedStatement st2 = createInsertStatement( "INSERT INTO "+tablename+" (type,joinid) VALUES (?,?)" ) ) {
		st2.setInt( 1, type );
		st2.setLong( 2, join );
		return executeUpdateWithId( st2, tablename );
	    }
    }

    public RelationConstruction extractRelationConstruction( int op, long intid ) throws SQLException, AlignmentException {
	Constructor constr = null;
	if ( op == AND ) constr = Constructor.AND;
	else if ( op == OR ) constr = Constructor.OR;
	else if ( op == NOT ) constr = Constructor.NOT;
	else if ( op == COMP ) constr = Constructor.COMP;
	else if ( op == INV ) constr = Constructor.INVERSE;
	else if ( op == SYM ) constr = Constructor.SYMMETRIC;
	else if ( op == TRANS ) constr = Constructor.TRANSITIVE;
	else if ( op == REFL ) constr = Constructor.REFLEXIVE;
	else throw new AlignmentException( "Invalid operator "+op );
	List<RelationExpression> expressions = new Vector<RelationExpression>();
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT id FROM rellist WHERE intid='"+intid+"' ORDER BY id" );
		while ( rs.next() ) {
		    expressions.add( extractRelationExpression( rs.getLong( "id" ) ) );
		}
		return new RelationConstruction( constr, expressions );
	    }
    }

    public long visit( final RelationConstruction e ) throws SQLException, AlignmentException {
	// Get the constructor
	final Constructor op = e.getOperator();
	int type;
	if ( op == Constructor.OR ) type = OR;
	else if ( op == Constructor.AND ) type = AND;
	else if ( op == Constructor.COMP ) type = COMP;
	else if ( op == Constructor.NOT ) type = NOT;
	else if ( op == Constructor.INVERSE ) type = INV;
	else if ( op == Constructor.REFLEXIVE ) type = REFL;
	else if ( op == Constructor.TRANSITIVE ) type = TRANS;
	else if ( op == Constructor.SYMMETRIC ) type = SYM;
	else throw new AlignmentException( "Invalid constructor "+op );
	if ((op == Constructor.OR) || (op == Constructor.AND) || (op == Constructor.COMP)) {
	    // Create the relexpr
	    long exprres = registerExpression( "relexpr", type, 0 );
	    // Iterate on components
	    try ( Statement st = createStatement() ) {
		    for ( final PathExpression re : e.getComponents() ) {
			long pres = visit( re );
			st.executeUpdate( "INSERT INTO rellist (intid,id) VALUES ('"+exprres+"','"+pres+"')" );
		    }
		}
	    // Return the relexpr
	    return exprres;
	} else { // NOT
	    final PathExpression re = e.getComponents().iterator().next(); // OK...
	    // Create the component
	    long pres = visit( re );
	    // Create the relexpr
	    return registerExpression( "relexpr", type, pres );
	}
    }

    public RelationCoDomainRestriction extractRelationCoDomainRestriction( long intid ) throws SQLException, AlignmentException {
	ClassExpression codom = extractClassExpression( intid );
	return new RelationCoDomainRestriction( codom );
    }

    public long visit( final RelationRestriction c ) throws SQLException, AlignmentException {
	if ( c instanceof RelationCoDomainRestriction ) return visit( (RelationCoDomainRestriction)c );
	else if ( c instanceof RelationDomainRestriction ) return visit( (RelationDomainRestriction)c );
	else throw new AlignmentException("Creating relation restriction entry failed.");
    }

    public long visit( final RelationCoDomainRestriction c ) throws SQLException, AlignmentException {
	// Create the codomain restriction
	long codom = visit( c.getCoDomain() );
	// Register it
	return registerExpression( "relexpr", COD, codom );
    }

    public RelationDomainRestriction extractRelationDomainRestriction( long intid ) throws SQLException, AlignmentException {
	ClassExpression dom = extractClassExpression( intid );
	return new RelationDomainRestriction( dom );
    }

    public long visit( final RelationDomainRestriction c ) throws SQLException, AlignmentException {
	// Create the domain restriction
	long dom = visit( c.getDomain() );
	// Register it
	return registerExpression( "relexpr", DOM, dom );
    }

    public ValueExpression extractValueExpression( long intid ) throws SQLException, AlignmentException {
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT type, joinid FROM valueexpr WHERE intid='"+intid+"'" );
		if ( rs.next() ) {
		    int type = rs.getInt( "type" );
		    if ( type == INSTANCE ) return extractInstanceExpression( rs.getLong( "joinid" ) );
		    else if ( type == VALUE ) return extractValue( rs.getLong( "joinid" ) );
		    else if ( type == PATH ) return extractPathExpression( rs.getLong( "joinid" ) );
		    else if ( type == APPLY ) return extractApply( rs.getLong( "joinid" ), 0 );
		    else if ( type == AGGREGATE ) return extractApply( rs.getLong( "joinid" ), 1 );
		    else throw new AlignmentException( "Unknown ValueExpression type "+type );
		} else {
		    throw new AlignmentException( "Cannot retrieve value expression "+intid );
		}
	    }
    }

    public long visit( ValueExpression e ) throws SQLException, AlignmentException {
	long idres;
	int type;
	if ( e instanceof InstanceExpression ) {
	    idres = visit( (InstanceExpression)e );
	    type = INSTANCE;
	} else if ( e instanceof Value ) {
	    idres = visit( (Value)e );
	    type = VALUE;
	} else if ( e instanceof PathExpression ) {
	    idres = visit( (PathExpression)e );
	    type = PATH;
	} else if ( e instanceof Apply ) {
	    idres = visit( (Apply)e );
	    type = APPLY;
	} else if ( e instanceof Aggregate ) {
	    idres = visit( (Aggregate)e );
	    type = AGGREGATE;
	} else throw new AlignmentException( "Unknown ValueExpression "+e );
	return registerExpression( "valueexpr", type, idres );
    }

    // (no instance expression table)
    public InstanceExpression extractInstanceExpression( long intid ) throws SQLException, AlignmentException {
	return extractInstanceId( intid );
    }

    // (no instance expression table: it is dealt with in instanceid)
    public long visit( InstanceExpression e ) throws SQLException, AlignmentException {
	if ( e instanceof InstanceId ) return visit( (InstanceId)e );
	else throw new AlignmentException( "Unknown InstanceExpression "+e );
    }

    public InstanceId extractInstanceId( long intid ) throws SQLException, AlignmentException {
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT uri FROM instexpr WHERE intid='"+intid+"'" );
		if ( rs.next() ) {
		    try {
			return new InstanceId( new URI( rs.getString( "uri" ) ) );
		    } catch (URISyntaxException uriex) {
			throw new AlignmentException( "Badly formatted URI "+rs.getString("uri"), uriex );
		    }
		} else throw new AlignmentException( "Cannot retrieve instance "+intid );
	    }
    }

    public long visit( InstanceId e ) throws SQLException {
	return registerId( e, "instexpr" );
    }

    // (play with NULL)
    public Value extractValue( long intid ) throws SQLException, AlignmentException {
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT type,value FROM literal WHERE intid='"+intid+"'" );
		if ( rs.next() ) {
		    Long typeid = rs.getLong( "type" );
		    if ( typeid != 0 ) {
			return new Value( rs.getString( "value" ), extractDatatype( typeid ) );
		    } else {
			return new Value( rs.getString( "value" ) );
		    }
		} else throw new AlignmentException( "Cannot retrieve value "+intid );
	    }
    }

    public long visit( final Value e ) throws SQLException, AlignmentException {
	String restQuery = "";
	long typid = 0;
        if ( e.getType() != null ) {
	    typid = visitDatatype( e.getType().toString() );
	    restQuery = "AND type='"+typid+"'";
        }
	// If it exists do not add:
	String val = e.getValue();
	// PS=??
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT intid FROM literal WHERE value="+SQLCache.quote( val )+""+restQuery );
		if ( rs.next() ) {
		    return rs.getLong("intid");
		} else {
		    // PS=insertLiteral
		    // PS=insertTypedLiteral
		    PreparedStatement st2 = null;
		    if ( typid != 0 ) {
			st2 = insertTypedLiteral;
			st2.setString( 1, val );
			st2.setLong( 2, typid );
		    } else {
			st2 = insertLiteral;
			st2.setString( 1, val );
		    }
		    //String query = "INSERT INTO literal (value) VALUES (?)";
		    //if ( typid != 0 ) {
		    //query = "INSERT INTO literal (value,type) VALUES (?,'?')";
		    //}
		    //try ( PreparedStatement st2 = createInsertStatement( query ) ) {
		    //	    st2.setString( 1, val );
		    //	    st2.setLong( 2, typid );
		    return executeUpdateWithId( st2, "value" );
		    //}
		}
	    }
    }

    // (play with NULL)
    public ValueExpression extractApply( long intid, int type ) throws SQLException, AlignmentException {
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT operation FROM apply WHERE intid='"+intid+"'" );
		if ( rs.next() ) {
		    try {
			URI opuri = new URI( rs.getString( "operation" ) );
			try ( Statement st2 = createStatement() ) {
				ResultSet rs2 = st2.executeQuery( "SELECT id FROM arglist WHERE intid='"+intid+"'" );
				List<ValueExpression> args = new Vector<ValueExpression>();
				while ( rs2.next() ) {
				    args.add( extractValueExpression( rs2.getLong( "id" ) ) );
				}
				if ( type == 0 ) {
				    return new Apply( opuri, args );
				} else {
				    return new Aggregate( opuri, args );
				}				    
			    }
		    } catch ( URISyntaxException urisex ) {
			throw new AlignmentException( "Invalid operation URI", urisex );
		    }
		} else throw new AlignmentException( "Cannot retrieve apply "+intid );
	    }
    }

    public long visit( final Apply e ) throws SQLException, AlignmentException {
	// Get the constructor
	final URI op = e.getOperation();
	// Create the relexpr
	try ( PreparedStatement st2 = createInsertStatement( "INSERT INTO apply (type,operation) VALUES (0,?)" ) ) {
		st2.setString( 1, op.toString() );
		long exprres = executeUpdateWithId( st2, "apply" );
		// Iterate on arguments
		try ( Statement st = createStatement() ) {
			for ( final ValueExpression ve : e.getArguments() ) {
			    long pres = visit( ve );
			    st.executeUpdate( "INSERT INTO arglist (intid,id) VALUES ('"+exprres+"','"+pres+"')" );
			}
		    }
		// Return the expr
		return exprres;
	    }
    }

    public long visit( final Aggregate e ) throws SQLException, AlignmentException {
	// Get the constructor
	final URI op = e.getOperation();
	// Create the relexpr
	try ( PreparedStatement st2 = createInsertStatement( "INSERT INTO apply (type,operation) VALUES (1,?)" ) ) {
		st2.setString( 1, op.toString() );
		long exprres = executeUpdateWithId( st2, "apply" );
		// Iterate on arguments
		try ( Statement st = createStatement() ) {
			for ( final ValueExpression ve : e.getArguments() ) {
			    long pres = visit( ve );
			    st.executeUpdate( "INSERT INTO arglist (intid,id) VALUES ('"+exprres+"','"+pres+"')" );
			}
		    }
		// Return the expr
		return exprres;
	    }
    }

    public URI extractDatatype( long intid ) throws SQLException, AlignmentException {
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT uri FROM typeexpr WHERE intid='"+intid+"'" );
		if ( rs.next() ) {
		    try {
			return new URI( rs.getString("uri") );
		    } catch (URISyntaxException uriex) {
			throw new AlignmentException( "Badly formatted URI "+rs.getString("uri"), uriex );
		    }
		} else throw new AlignmentException( "Cannot retrieve datatype "+intid );
	    }
    }

    // Note: in EDOAL, values have datatypes which simply are URIs!
    public long visitDatatype( String uri ) throws SQLException {
	// If it exists do not add:
	// PS=findIdByUri (with typeexpr as first argument)
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT intid FROM typeexpr WHERE uri='"+uri+"'" );
		if ( rs.next() ) {
		    return rs.getLong("intid"); // try getInt on tables
		} else {
		    // PS=insertId (same as before)
		    try ( PreparedStatement st2 = createInsertStatement( "INSERT INTO typeexpr (uri) VALUES (?)" ) ) {
			    st2.setString( 1, uri );
			    return executeUpdateWithId( st2, "typeexpr" );
			}
		}
	    }
    }

    public long visit( Datatype e ) throws SQLException {
	return visitDatatype( e.getType() );
    }

    // Called for each cell in an edoal alignment
    public void extractTransformations( String cellid, EDOALCell cell ) throws SQLException, AlignmentException {
	// For all the Linkkeys with cellid as cellid
	findTransformation.setString( 1, cellid );
	ResultSet rs = findTransformation.executeQuery();
	// Store the result
	while ( rs.next() ) { 
	    int type = rs.getInt( "type" );
	    String tp; 
	    if ( type == OO ) tp = "oo";
	    else if ( type == O_ ) tp = "o-";
	    else if ( type == _O ) tp = "-o";
	    else throw new AlignmentException( "Invalid transformation type : "+type );
	    cell.addTransformation( new Transformation( tp, 
							extractValueExpression( rs.getLong( "joinid1" ) ),
							extractValueExpression( rs.getLong( "joinid2" ) ) ) );
	}
    }

    // Called for each cell in an edoal alignment
    public long visit( final Transformation transf, String cellid ) throws SQLException, AlignmentException {
	String tp = transf.getType();
	long ob1 = visit( transf.getObject1() );
	long ob2 = visit( transf.getObject2() );
	int type;
	if ( tp.equals("oo") ) type = OO;
	else if ( tp.equals( "o-" ) ) type = O_;
	else if ( tp.equals( "-o" ) ) type = _O;
	else throw new AlignmentException( "Invalid transformation type : "+tp );
	// PS=insertTransf
	PreparedStatement st2 = createInsertStatement( "INSERT INTO transf (cellid,type,joinid1,joinid2) VALUES (?,?,?,?)" );
	st2.setString( 1, cellid );
	st2.setInt( 2, type );
	st2.setLong( 3, ob1 );
	st2.setLong( 4, ob2 );
	return executeUpdateWithId( st2, "transformation" );
    }

    // Called for each cell in an edoal alignment
    public void extractLinkkeys( String cellid, EDOALCell cell ) throws SQLException, AlignmentException {
	// For all the Linkkeys with cellid as cellid
	findLinkkey.setString( 1, cellid );
	ResultSet rs = findLinkkey.executeQuery();
	// Store the result
	while ( rs.next() ) { 
	    Linkkey linkkey = new Linkkey();
	    extractBindings( rs.getLong( "intid" ), linkkey );
	    cell.addLinkkey( linkkey );
	}
    }

    public void visit( final Linkkey linkkey, String cellid ) throws SQLException, AlignmentException {
	// Add the linkkey to the table
	insertLinkkey.setString( 1, cellid );
	long keyid = executeUpdateWithId( insertLinkkey, "linkkey" );
	// For each dinding add the binding
        for ( LinkkeyBinding linkkeyBinding : linkkey.bindings() ) {
	    visit( linkkeyBinding, keyid );
        }
    }

    public void extractBindings( long keyid, Linkkey key ) throws SQLException, AlignmentException {
	// For all the Linkkeys with cellid as cellid
	// PS=findBindings
	try ( Statement st = createStatement() ) {
		ResultSet rs = st.executeQuery( "SELECT type,joinid1,joinid2 FROM binding WHERE keyid='"+keyid+"'" );
		// Store the result
		while ( rs.next() ) {
		    PathExpression p1 = extractPathExpression( rs.getLong( "joinid1" ) );
		    PathExpression p2 = extractPathExpression( rs.getLong( "joinid1" ) );
		    if ( rs.getInt( "type" ) == EQUAL_KEY ) {
			key.addBinding( new LinkkeyEquals( p1, p2 ) );
		    } else {
			key.addBinding( new LinkkeyIntersects( p1, p2 ) );
		    }
		}
	    }
    }

    private void visit( LinkkeyBinding linkkeyBinding, long keyid ) throws SQLException, AlignmentException {
	// Store the two paths
	long p1 = visit( linkkeyBinding.getExpression1() );
	long p2 = visit( linkkeyBinding.getExpression2() );
	int type = ( linkkeyBinding instanceof LinkkeyEquals )?EQUAL_KEY:INTER_KEY;
	// PS=insertBinding
	try ( Statement st = createStatement() ) {
		st.executeUpdate( "INSERT INTO binding (keyid,type,joinid1,joinid2) VALUES ("+keyid+","+type+","+p1+","+p2+")" );
	    }
    }
}

