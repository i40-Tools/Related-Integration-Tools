/*
 * $Id: AlignmentServiceProfile.java 1901 2014-03-14 13:41:41Z euzenat $
 *
 * Copyright (C) INRIA Rhône-Alpes, 2006, 2014
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

import java.util.Properties;

public interface AlignmentServiceProfile {

    /**
     * Creates the Service object and declares it after any
     * required registery
     */
    public void init( Properties p, AServProtocolManager m ) throws AServException;

    /**
     * Tells if it accept requests with this prefix
     */
    public boolean accept( String prefix );

    /**
     * Process a particular request
     */
    public String process( String uri, String prefix, String perf, Properties header, Properties params );

    /**
     * Shutdown the Service and undeclare it from any registery
     */
    public void close() throws AServException;
}
