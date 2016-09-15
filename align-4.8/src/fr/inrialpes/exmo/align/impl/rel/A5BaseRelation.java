/*
 * $Id: A5BaseRelation.java 2071 2015-10-04 06:42:17Z euzenat $
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

package fr.inrialpes.exmo.align.impl.rel;

import fr.inrialpes.exmo.align.impl.BaseRelation;

import java.io.PrintWriter;

/**
 * The Base relatins for the A5 algebra
 */

public enum A5BaseRelation implements BaseRelation {
    EQUIV ( "=" ),
    SUBSUMED( "<" ),
    SUBSUME( ">" ),
    OVERLAP( ")(" ),
    DISJOINT( "%" );

    /**********************************************************************
     * NO CHANGE IS NECESSARY BEYOND THIS POINT (only change the class name)
     * Unfortunately, this MUST be copied in all class.
     **********************************************************************/

    public final String relation;
    public int index;
    public A5BaseRelation inverse;

    public String getString() { return relation; }
    public int getIndex() { return index; }
    public A5BaseRelation getInverse() { return inverse; }

    public void init( int idx, A5BaseRelation inv ) {
	index = idx;
	inverse = inv;
    }

    A5BaseRelation ( String label ) {
	relation = label;
    }

    public void write( PrintWriter writer ) {
	if ( this == SUBSUMED ) writer.print( "&lt;" );
	else writer.print( relation );
    }
}
