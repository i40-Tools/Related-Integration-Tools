/*
 * $Id: A16BaseRelation.java 2075 2015-10-11 17:59:45Z euzenat $
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
 * The Base relatins for the A16 algebra
 */

public enum A16BaseRelation implements BaseRelation {
    // A5
    EQ_N( "=n" ),
    SUBSUMED( "<" ),
    SUBSUME( ">" ),
    OVERLAP( ")(" ),
    DISJOINT( "%" ),

    // A5-A2 glue
    HAS("3"),
    HASNOT("-3"),
    ISA("E"),
    ISNOT("-E"),

    //A2
    EQ_I("=i"),
    NOTEQ_I("-=i"),

    // Empty classes
    NE("NE"),
    IE("IE"),
    EQ_E("=e"),
    EN("EN"),
    EI("EI");

    /**********************************************************************
     * NO CHANGE IS NECESSARY BEYOND THIS POINT (only change the class name)
     * Unfortunately, this MUST be copied in all class.
     **********************************************************************/

    public final String relation;
    public int index;
    public A16BaseRelation inverse;

    public String getString() { return relation; }
    public int getIndex() { return index; }
    public A16BaseRelation getInverse() { return inverse; }

    public void init( int idx, A16BaseRelation inv ) {
	index = idx;
	inverse = inv;
    }

    A16BaseRelation ( String label ) {
	relation = label;
    }

    public void write( PrintWriter writer ) {
	writer.print( relation );
    }
    
}
