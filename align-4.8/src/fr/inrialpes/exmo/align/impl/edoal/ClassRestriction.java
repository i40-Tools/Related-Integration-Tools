/*
 * $Id: ClassRestriction.java 1956 2014-07-10 16:26:56Z nguillouet $
 *
 * Copyright (C) 2006 Digital Enterprise Research Insitute (DERI) Innsbruck
 * Sourceforge version 1.6 - 2006
 * Copyright (C) INRIA, 2009, 2012
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

/**
 * <p>
 * Superclass for all ClassRestrictions.
 * </p>
 * <p>
 * To successfully subclass this class the <code>clone</code> and
 * <code>equals</code> methods must be overwritten. And if new fields were
 * introduced, the <code>hashCode</code> and <code>toString</code> methods,
 * too.
 * </p>
 * <p>
 * Created on 24-Mar-2005 Committed by $Author: poettler_ric $
 * </p>
 * 
 * @version $Id: ClassRestriction.java 1956 2014-07-10 16:26:56Z nguillouet $
 */
public abstract class ClassRestriction extends ClassExpression { 

    /** The attribute on which the restriction should be applied. */
    protected PathExpression constrainedPath = null;
    
    public ClassRestriction( PathExpression att ) {
	super();
	constrainedPath = att;
    }

    public PathExpression getRestrictionPath() {
	return constrainedPath;
    }

    public void setRestrictionPath( PathExpression att) {
	constrainedPath = att;
    }

    /*
    public Object clone() {
	try {
	    ClassRestriction clone = (ClassRestriction) super.clone();
	    clone.restriction = (Restriction) restriction.clone();
	    clone.attribute = (PathExpression) attribute.clone();
	    clone.target = (ExpressionDefinition) target.clone();
	    return clone;
	} catch (CloneNotSupportedException e) {
	    assert true : "Object is always cloneable";
	}
	return null;
    }
    */

}
