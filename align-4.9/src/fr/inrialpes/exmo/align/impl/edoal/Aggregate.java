/*
 * $Id: Aggregate.java 2129 2017-03-04 17:51:55Z euzenat $
 *
 * Copyright (C) INRIA, 2015, 2017
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

package fr.inrialpes.exmo.align.impl.edoal;

import java.util.List;
import java.util.Objects;
import java.net.URI;
import java.net.URISyntaxException;

import org.semanticweb.owl.align.AlignmentException;

import fr.inrialpes.exmo.align.parser.TypeCheckingVisitor;
import fr.inrialpes.exmo.align.parser.TypeCheckingVisitor.TYPE;

/**
 * <p>
 * Aggregate is the aggregation of a set of values through a particular function.
 * </p>
 * <p>
 * Values of {@code args} have to be aggregated with {@code op}.
 * </p>
 *
 * @version $Id: Aggregate.java 2129 2017-03-04 17:51:55Z euzenat $
 */

public class Aggregate implements ValueExpression {

    /** Holds the operation to apply */
    private URI operation;

    /** In principle, this may be a single expression, but we can generalise */
    private List<ValueExpression> arguments;

    /**
     * Constructs an object with the given value.
     * 
     * @param op
     *            the URI of the aggregation operation to apply.
     * @param args
     *            its list of argumenst
     * @throws NullPointerException
     *             if the value is {@code null}
     */
    public Aggregate( final URI op, final List<ValueExpression> args ) {
	if ( op == null) {
	    throw new NullPointerException("The operation must not be null");
	}
	operation = op;
	arguments = args;
    }

    public void accept( EDOALVisitor visitor ) throws AlignmentException {
	visitor.visit(this);
    }
    public TYPE accept( TypeCheckingVisitor visitor ) throws AlignmentException {
	return visitor.visit(this);
    }

    public URI getOperation() {
	return operation;
    }

    public List<ValueExpression> getArguments() {
	return arguments;
    }

    public int hashCode() {
	return Objects.hash( operation, arguments );
    }

    public boolean equals( final Object o ) {
	if ( o == this ) return true;
	if ( !(o instanceof Aggregate) ) return false;
	Aggregate a = (Aggregate)o;
	return ( operation.equals(a.getOperation()) && 
		 arguments.equals(a.getArguments()) );
    }
}
