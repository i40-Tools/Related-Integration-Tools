/*
 * $Id: Directory.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) INRIA, 2007, 2009, 2011
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

package fr.inrialpes.exmo.align.service;

import org.semanticweb.owl.align.Alignment;

import java.util.Properties;

public interface Directory {

    /**
     * Create a connection and/or registration to a directory
     * Parameters can contain, e.g.:
     * - the directory address
     * - the declaration of the current service
     *
     * @param p: the properties of the connection to open
     * @throws AServException when something goes wrong
     */
    public void open( Properties p ) throws AServException;

    /**
     * Register an alignment to the directory (if necessary)
     *
     * @param al: the alignment to register
     * @throws AServException when something goes wrong
     */
    public void register( Alignment al ) throws AServException;

    /**
     * Unregister an alignment to the directory (if necessary)
     *
     * @param al: the alignment to unregister
     * @throws AServException when something goes wrong
     */
    public void unregister( Alignment al ) throws AServException;

    /**
     * Shutdown the connection and/or registration to the directory
     *
     * @throws AServException when something goes wrong
     */
    public void close() throws AServException;
}
