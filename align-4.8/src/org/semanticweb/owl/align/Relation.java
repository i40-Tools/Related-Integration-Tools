/*
 * $Id: Relation.java 2062 2015-10-01 16:44:20Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2005, 2007, 2009
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

package org.semanticweb.owl.align;

import java.io.PrintWriter;

import org.xml.sax.ContentHandler;

/**
 *
 * @author Jérôme Euzenat
 * @version $Id: Relation.java 2062 2015-10-01 16:44:20Z euzenat $
 */


public interface Relation extends Visitable {
    /** Creation **/
    public String getRelation();

    // V5: this, especially compose should be allowed to throw errors
    public Relation inverse();
    public Relation compose( Relation r );

    // V5: This is a bad choice to have introduced this here
    // It should be deprecated in version 5 of the API!
    public boolean equals( Relation r );

    /** Housekeeping **/
    public void dump( ContentHandler h );
    public void write( PrintWriter writer );

}


