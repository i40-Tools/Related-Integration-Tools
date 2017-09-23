/*
 * $Id: BasicEvaluator.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) INRIA, 2004, 2007-2008, 2010, 2013
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

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Evaluator;

import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;

import java.io.PrintWriter;

/**
 * Evaluate proximity between two alignments.
 * This function implements a simple weighted symetric difference.
 * There are many different things to compute in such a function...
 * Add classification per type of objects (Ind, Class, Prop...)
 */

public abstract class BasicEvaluator implements Evaluator {
    protected double result = 1.;
    protected Alignment align1;
    protected Alignment align2;

    /** Creation 
    * Creates an evaluator
    *
    * @param align1 and
    * @param align2: the two alignments to be evaluated 
    * @throws AlignmentException when something goes wrong (never in this case)
    **/
    public BasicEvaluator( Alignment align1, Alignment align2 ) throws AlignmentException {
	this.align1 = align1;
	this.align2 = align2;
    }

    public void convertToObjectAlignments( Alignment al1, Alignment al2 ) throws AlignmentException {
	align1 = convertToObjectAlignment( al1 );
	align2 = convertToObjectAlignment( al2 );
    }

    public ObjectAlignment convertToObjectAlignment( Alignment al ) throws AlignmentException {
	if ( al instanceof ObjectAlignment ) {
	    return (ObjectAlignment)al;
	} else if ( al instanceof URIAlignment ) {
	    return ObjectAlignment.toObjectAlignment( (URIAlignment)al );
	} else {
	    throw new AlignmentException( "Cannot convert to ObjectAlignment : "+al );
	}
    }

    public void write( PrintWriter writer ) throws java.io.IOException {
	writer.print("<rdf:RDF>\n  <Evaluation class=\"BasicEvaluator\">\n    <result>");
 	writer.print(result);
 	writer.print("</result>\n  </Evaluation>\n</rdf:RDF>\n");
    }

}


