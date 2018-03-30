/*
 * $Id: BasicRelation.java 2150 2017-07-18 15:15:46Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2005, 2007, 2009-2015, 2017
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

package fr.inrialpes.exmo.align.impl;

import fr.inrialpes.exmo.align.parser.TypeCheckingVisitor;

import fr.inrialpes.exmo.align.impl.rel.EquivRelation;
import fr.inrialpes.exmo.align.impl.rel.SubsumedRelation;
import fr.inrialpes.exmo.align.impl.rel.SubsumeRelation;
import fr.inrialpes.exmo.align.impl.rel.IncompatRelation;
import fr.inrialpes.exmo.align.impl.rel.InstanceOfRelation;
import fr.inrialpes.exmo.align.impl.rel.HasInstanceRelation;
import fr.inrialpes.exmo.align.impl.rel.NonTransitiveImplicationRelation;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Relation;

import org.xml.sax.ContentHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an ontology alignment relation.
 *
 * @author Jérôme Euzenat
 * @version $Id: BasicRelation.java 2150 2017-07-18 15:15:46Z euzenat $
 */

public class BasicRelation implements Relation {
    final static Logger logger = LoggerFactory.getLogger( BasicRelation.class );

    private static Map<String, Class<?>> classIndex = null;

    static Class<?> getClass( String label ) {
	if ( label == null ) 
	    throw new NullPointerException("The string to search must not be null");
	if ( classIndex == null ){
	    HashMap<String, Class<?>> index = new HashMap<String, Class<?>>();
	    index.put( "Equivalence", EquivRelation.class );
	    index.put( "=", EquivRelation.class );
	    index.put( "equivalence", EquivRelation.class );
	    index.put( "ClassMapping", EquivRelation.class );
	    
	    index.put( "Subsumes", SubsumeRelation.class );
	    index.put( ">", SubsumeRelation.class );
	    index.put( "&gt;", SubsumeRelation.class );
	    
	    index.put( "SubsumedBy", SubsumedRelation.class );
	    index.put( "<", SubsumedRelation.class );
	    index.put( "&lt;", SubsumedRelation.class );
	    
	    index.put("><", IncompatRelation.class );
	    index.put("%", IncompatRelation.class );
	    index.put("DisjointFrom",IncompatRelation.class);
	    index.put("Disjoint",IncompatRelation.class);
	    index.put("disjointFrom",IncompatRelation.class);
	    index.put("disjoint",IncompatRelation.class);
	    
	    index.put( "InstanceOf", InstanceOfRelation.class );
	    
	    index.put( "HasInstance", HasInstanceRelation.class );

	    index.put( "~>", NonTransitiveImplicationRelation.class );
	    index.put( "~&gt;", NonTransitiveImplicationRelation.class );
	    classIndex = index;
	}
	return classIndex.get(label);
    }

    public void accept( TypeCheckingVisitor visitor ) throws AlignmentException {
	visitor.visit(this);
    }

    public void accept( AlignmentVisitor visitor) throws AlignmentException {
        visitor.visit( this );
    }

    /**
     * The initial relation given by the user (through parser for instance)
     * This is never used in subclass relations (because they share one relation)
     */
    protected String relation = null;

    public int index = 0;

    /** printable format **/
    public String getRelation() {
	return relation;
    }

    /**
     * The pretty relation attached to the relation type
     * This is overriden as static in subclasses
     */
    protected String prettyLabel = null;

    public String getPrettyLabel() {
	if ( prettyLabel != null ) return prettyLabel;
	else return relation;
    }

    /**
     * The name to use if no other information is available
     * 
     * @return the name of the relation class
     */
    public String getClassName() {
	return getClass().toString();
    }

    /** Creation **/
    public BasicRelation() {
    }

    public BasicRelation( String rel ){
	relation = rel;
    }

    /**
     * The constructor to use
     *
     * @param rel: the string representation of the relation
     * @return the corresponding relation object
     * @throws AlignmentException if something goes wrong (e.g., rel cannot be interpreted)
     */
    public static BasicRelation createRelation( String rel ) throws AlignmentException {
	Class<?> relationClass = getClass( rel );
	if ( relationClass != null ) {
	    try { // Get existing relation... 
		Method m = relationClass.getMethod("getInstance");
		return (BasicRelation)m.invoke(null);
	    } catch ( Exception ex ) {}; // should not happen
	}
	// JE2015: This is now dealt with from BasicAlignment
	try { // Create a relation from classname
	    relationClass = Class.forName(rel);
	    Constructor<?> relationConstructor = relationClass.getConstructor((Class[])null);
	    return (BasicRelation)relationConstructor.newInstance((Object[])null);
	} catch ( Exception ex ) {
	    logger.debug( "IGNORED Exception: created Basic Relation)", ex );
	    //Otherwise, just create a Basic relation
	    return new BasicRelation( rel );
	}
    }

    /**
     * By default the inverse is the relation itself
     *
     * @return the inverse of the relation
     **/
    public Relation inverse() {
	return this;
    }

    /**
     * By default... no composition possible
     *
     * @param r: a relation with which to compose
     * @return this relation composed with r
     **/
    public Relation compose( Relation r ) {
    	return null;
    }

    /**
     * By default... meet if equals
     *
     * @param rels: a set of relations to meet with this one
     * @return the relation which is the result of the meet
     **/
    public BasicRelation meet( Relation... rels ) {
	for( Relation r : rels ) {
	    if ( !this.equals( r ) ) return null;
	}
	return this;
    }

    /**
     * By default... we are not in disjunctive relations... then returns null
     *
     *
     * @param rels: a set of relations to join with this one
     * @return the relation which is the result of the join
     **/
    public BasicRelation join( Relation... rels ) {
	for( Relation r : rels ) {
	    if ( !this.equals( r ) ) return null;
	}
	return this;
    }

    /**
     * Housekeeping
     *
     * @param h: the content handler in which to dump the relation
     **/
    public void dump( ContentHandler h ){};

    /**
     * This is kept for displayig more correctly the result
     *
     *
     * @param writer: the writer where to print the result
     **/
    public void write( PrintWriter writer ) {
	if ( relation != null ) {
	    writer.print( relation );
	} else if ( getPrettyLabel() != null ) {
	    writer.print( getPrettyLabel() );
	} else {
	    writer.print( getClassName() );
	}
    }

    /**
     * Are the two relations equal
     *
     * @param o: an object with which to compare this one
     * @return true is o is equal to this object
     **/
    public boolean equals( Object o ) {
	if ( o == this ) return true;
	if ( o == null ) return false;
	if ( o instanceof Relation ) return equals( (Relation)o );
	return false;
    }

    // V5: should be deprecated (code moved in the Object method)
    public boolean equals( Relation r ) {
	if ( r == this ) return true;
	if ( r == null ) return false;
	return ( relation.equals( r.getRelation() ) );
    }

    public int hashCode() {
	return Objects.hash( relation );
    }

}


