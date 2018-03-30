/*
 * $Id: RelationTransformer.java 2088 2015-10-23 13:26:27Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2004, 2007-2015
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

package fr.inrialpes.exmo.align.impl.renderer; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.align.impl.rel.EquivRelation;
import fr.inrialpes.exmo.align.impl.rel.SubsumeRelation;
import fr.inrialpes.exmo.align.impl.rel.SubsumedRelation;
import fr.inrialpes.exmo.align.impl.rel.IncompatRelation;
import fr.inrialpes.exmo.align.impl.rel.InstanceOfRelation;
import fr.inrialpes.exmo.align.impl.rel.HasInstanceRelation;

import fr.inrialpes.exmo.align.impl.rel.A2AlgebraRelation;
import fr.inrialpes.exmo.align.impl.rel.A5AlgebraRelation;
import fr.inrialpes.exmo.align.impl.rel.A16AlgebraRelation;
import fr.inrialpes.exmo.align.impl.rel.A2BaseRelation;
import fr.inrialpes.exmo.align.impl.rel.A5BaseRelation;
import fr.inrialpes.exmo.align.impl.rel.A16BaseRelation;

/**
 * This class categorises relations of the API implementation with respect to the
 * category of their use by renderers.
 *
 * It is made for being used _statically_ by renderers to decise how to render a
 * cell. In principle, this is the only thing that has to be modified when using
 * different Relation implementations.
 *
 * @author Jérôme Euzenat
 * @version $Id: RelationTransformer.java 2088 2015-10-23 13:26:27Z euzenat $ 
 */

public class RelationTransformer {
    final static Logger logger = LoggerFactory.getLogger( OWLAxiomsRendererVisitor.class );

    // Unused currently
    final public static int EQUIV = 1;
    final public static int SUBSUME = 2;
    final public static int SUBSUMED = 3;
    final public static int DISJOINT = 4;
    final public static int OVERLAP = 5;
    final public static int INSTANCEOF = 6;
    final public static int HASINSTANCE = 7;

    // This relation has a corresponding relation in OWL
    public static boolean canBeTranscribedDirectly( Relation rel ) {
	return ( isEquivalence( rel ) 
		 || isSubsumedOrEqual( rel ) 
		 || isDisjoint( rel ) 
		 || isInstanceOf( rel )) ;
    }

    // This relation is the converse of a relation in OWL
    public static boolean canBeTranscribedInverted( Relation rel ) {
	return ( subsumesOrEqual( rel ) 
		 || hasInstance( rel )) ;
    }

    // This relation is equivalence
    public static boolean isEquivalence( Relation rel ) {
	return ( rel instanceof EquivRelation
		 || ( rel instanceof A2AlgebraRelation && ((A2AlgebraRelation)rel).isIdRelation() )
		 || ( rel instanceof A5AlgebraRelation && ((A5AlgebraRelation)rel).isIdRelation() )
		 || ( rel instanceof A16AlgebraRelation && ((A16AlgebraRelation)rel).isIdRelation() )
		 );
    }

    // This relation is subXOf
    public static boolean isSubsumedOrEqual( Relation rel ) {
	try {
	    return ( rel instanceof SubsumedRelation 
		     || ( rel instanceof A5AlgebraRelation && 
			  A5AlgebraRelation.createRelation( A5BaseRelation.SUBSUMED,  A5BaseRelation.EQUIV ).entails( (A5AlgebraRelation)rel ) )
		     || ( rel instanceof A16AlgebraRelation &&
			  A16AlgebraRelation.createRelation( A16BaseRelation.SUBSUMED, A16BaseRelation.EQ_N, A16BaseRelation.EQ_E, A16BaseRelation.EN ).entails( (A16AlgebraRelation)rel ) )
		     );
	} catch (AlignmentException aex) {
	    logger.trace( "IGNORED Exception", aex );
	    return false;
	}
    }

    // This relation is superXOf
    public static boolean subsumesOrEqual( Relation rel ) {
	try {
	    return ( rel instanceof SubsumeRelation 
		     || ( rel instanceof A5AlgebraRelation && 
			  A5AlgebraRelation.createRelation( A5BaseRelation.SUBSUME, A5BaseRelation.EQUIV ).entails( (A5AlgebraRelation)rel ) )
		     || ( rel instanceof A16AlgebraRelation &&
			  A16AlgebraRelation.createRelation( A16BaseRelation.SUBSUME, A16BaseRelation.EQ_N, A16BaseRelation.EQ_E, A16BaseRelation.NE ).entails( (A16AlgebraRelation)rel ) )
		     );
	} catch (AlignmentException aex) {
	    logger.trace( "IGNORED Exception", aex );
	    return false;
	}
    }

    // This relation is disjointFrom
    public static boolean isDisjoint( Relation rel ) {
	try {
	    return ( rel instanceof IncompatRelation 
		     || ( rel instanceof A2AlgebraRelation &&
			  A2AlgebraRelation.createRelation( A2BaseRelation.DIFF ).entails( (A2AlgebraRelation)rel ) )
		     || ( rel instanceof A5AlgebraRelation && 
			  A5AlgebraRelation.createRelation( A5BaseRelation.DISJOINT ).entails( (A5AlgebraRelation)rel ) )
		     || ( rel instanceof A16AlgebraRelation && 
			  A16AlgebraRelation.createRelation( A16BaseRelation.DISJOINT, A16BaseRelation.NOTEQ_I, A16BaseRelation.EQ_E, A16BaseRelation.NE, A16BaseRelation.EN ).entails( (A16AlgebraRelation)rel ) )
		     );
	} catch (AlignmentException aex) {
	    logger.trace( "IGNORED Exception", aex );
	    return false;
	}
    }

    // This relation is isInstanceOf
    public static boolean isInstanceOf( Relation rel ) {
	try {
	    return ( rel instanceof InstanceOfRelation 
		     || ( rel instanceof A16AlgebraRelation && 
			  A16AlgebraRelation.createRelation( A16BaseRelation.ISA ).entails( (A16AlgebraRelation)rel ) )
		     );
	} catch (AlignmentException aex) {
	    logger.trace( "IGNORED Exception", aex );
	    return false;
	}
    }

    // This relation is hasInstance
    public static boolean hasInstance( Relation rel ) {
	try {
	    return ( rel instanceof HasInstanceRelation 
		     || ( rel instanceof A16AlgebraRelation && 
			  A16AlgebraRelation.createRelation( A16BaseRelation.HAS ).entails( (A16AlgebraRelation)rel ) )
		     );
	} catch (AlignmentException aex) {
	    logger.trace( "IGNORED Exception", aex );
	    return false;
	}
    }


}
