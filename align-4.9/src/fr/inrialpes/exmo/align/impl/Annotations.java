/*
 * $Id: Annotations.java 2150 2017-07-18 15:15:46Z euzenat $
 *
 * Copyright (C) INRIA, 2008-2009, 2014, 2017
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package fr.inrialpes.exmo.align.impl;

public class Annotations {

    public final static String ID = "id";
    public final static String METHOD = "method";
    public final static String DERIVEDFROM = "derivedFrom";
    public final static String PARAMETERS = "parameters";
    public final static String CERTIFICATE = "certificate";
    public final static String TIME = "time";
    public final static String LIMITATIONS = "limitations";
    public final static String PROPERTIES = "properties";
    public final static String PRETTY = "pretty";
    public final static String PROVENANCE = "provenance";
    public final static String SAMEAS = "sameAs";

    /* Set to true for rejecting the use of deprecated (non deterministic) primitives */
    // JE2009: Unrelated to Annotations...
    public final static boolean STRICT_IMPLEMENTATION = false;

}
