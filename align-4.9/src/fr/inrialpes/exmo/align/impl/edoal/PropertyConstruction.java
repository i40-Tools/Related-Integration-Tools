/*
 * $Id: PropertyConstruction.java 2026 2015-01-07 18:15:34Z euzenat $
 *
 * Copyright (C) 2006 Digital Enterprise Research Insitute (DERI) Innsbruck
 * Sourceforge version 1.5 - 2006 - was PropertyExpr
 * Copyright (C) INRIA, 2009-2010, 2012, 2015
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
import java.util.Vector;

import fr.inrialpes.exmo.align.parser.SyntaxElement.Constructor;
import fr.inrialpes.exmo.align.parser.SyntaxElement;

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;

import fr.inrialpes.exmo.align.parser.TypeCheckingVisitor;
import fr.inrialpes.exmo.align.parser.TypeCheckingVisitor.TYPE;

/**
 * <p>
 * Represents a PropertyConstruction.
 * </p>
 * 
 * Created on 23-Mar-2005 Committed by $Author: adrianmocan $
 * 
 * @version $Id: PropertyConstruction.java 2026 2015-01-07 18:15:34Z euzenat $
 */

public class PropertyConstruction extends PropertyExpression {

    /** Holds all expressions: ordered for comp */
    private List<PathExpression> components;
    
    /** Operator of this complex expression. */
    private Constructor operator;
    
    public PropertyConstruction() {
	super();
	components = new Vector<PathExpression>();
    }

    public PropertyConstruction( Constructor op, List<PathExpression> expressions ) {
	operator = op;
	components = expressions;
	if ( (expressions == null) || (op == null) ) {
	    throw new NullPointerException("The subexpressions and the operator must not be null");
	}
	if ( op == SyntaxElement.COMPOSE.getOperator() ) {
	    // In case of COMPOSE, the list should have only relations and end in a property
	    // Or, it can be empty! If it is a path starting from a property! (see total.xml)
	    if ( !expressions.isEmpty() ) {
		for ( PathExpression pe : components.subList( 0, components.size()-1 ) ) {
		    if ( ! ( pe instanceof RelationExpression ) ) 
			throw new IllegalArgumentException( "Property composition must be based on relation expressions" );
		}
		if ( ! ( components.get( components.size()-1 ) instanceof PropertyExpression ) ) 
		    throw new IllegalArgumentException( "Property composition must end by a property expression" );
	    }
	} else if ( op == SyntaxElement.AND.getOperator() ||
		    op == SyntaxElement.OR.getOperator() ||
		    op == SyntaxElement.NOT.getOperator() ) {
	    // Otherwise it must only contain PropertyExpressions
	    for ( PathExpression pe : components ) {
		if ( ! ( pe instanceof PropertyExpression ) ) 
		    throw new IllegalArgumentException( "Property construction with "+op+" must only contain property expressions" );
	    }
	} else throw new IllegalArgumentException( "Incorrect operator for property : "+op );
    }

    public void accept( EDOALVisitor visitor ) throws AlignmentException {
	visitor.visit( this );
    }

    public TYPE accept(TypeCheckingVisitor visitor) throws AlignmentException {
	return visitor.visit(this);
    }

    public Constructor getOperator() {
	return operator;
    }

    // Bypasses type checking!
    public void setOperator( Constructor op ) {
	operator = op;
    }

    public List<PathExpression> getComponents() {
	return components;
    }

    public void addComponent( PropertyExpression exp ) throws AlignmentException {
	if ( operator == SyntaxElement.COMPOSE.getOperator() )
	    throw new AlignmentException( "Cannot add component to a property composition path" );
	components.add( exp );
    }

    /*
    public Object clone() {
	return super.clone();
    }
    */
}
