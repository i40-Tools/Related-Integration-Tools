/*
 * $Id: BasicConfidence.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) INRIA, 2015
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

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an ontology alignment confidence
 * This class is currently unused.
 * It will be extended when implementing better confidence aggregation.
 * It should however only work with static methods.
 *
 * @author Jérôme Euzenat
 * @version $Id: BasicConfidence.java 2102 2015-11-29 10:29:56Z euzenat $
 */

public class BasicConfidence {
    final static Logger logger = LoggerFactory.getLogger( BasicConfidence.class );

    /* will never be used
    public void accept( TypeCheckingVisitor visitor ) throws AlignmentException {
	visitor.visit(this);
    }

    public void accept( AlignmentVisitor visitor) throws AlignmentException {
        visitor.visit( this );
    } */

    public static double getTopConfidence() {
	return 1.0;
    }
    public static double getBottomConfidence() {
	return 0.0;
    }

    public static double conjunction( double c1, double c2 ) {
	return Math.min( c1, c2 );
    }
    public static double disjunction( double c1, double c2 ) {
	return Math.max( c1, c2 );
    }
}


