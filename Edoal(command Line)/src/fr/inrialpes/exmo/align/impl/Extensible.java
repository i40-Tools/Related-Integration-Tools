/*
 * $Id: Extensible.java 2146 2017-07-14 20:31:38Z euzenat $
 *
 * Copyright (C) INRIA, 2015
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package fr.inrialpes.exmo.align.impl;

import fr.inrialpes.exmo.align.impl.Extensions;

import java.util.Collection;

/**
 *
 * @author Nicolas Guillouet (nicolas@meaningengines.com)
 */

public interface Extensible {
    
    /**
     * Extensions are a way to read and add other information (metadata)
     * to the alignment structure itself.
     * getExtensions returns a set of tripes: uri*label*value
     * all three being String
     *
     * @return a collection of tripes: uri*label*value implemented as String[]
     */

    public Collection<String[]> getExtensions();
    public String getExtension( String uri, String label );
    public void setExtension( String uri, String label, String value );
    public void setExtensions( Extensions p );
}
