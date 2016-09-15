/*
 * $Id: Result.java 1996 2014-11-23 16:30:55Z euzenat $
 *
 * Copyright (C) INRIA Rhône-Alpes, 2006, 2008, 2014
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 */

package fr.inrialpes.exmo.queryprocessor;

import java.util.Collection;
import java.util.Vector;

/**
 *
 * @author Arun Sharma
 */
public interface Result {
    /**@return the type of the result set
     */
    public int getType();
    
    /**@return the reslut for ASK type queries
     */
    public boolean getAskResult() throws QueryTypeMismatchException;
    
    /**
     *@return the RDF graph for construct queries
     */
    public RDFGraph getConstructResult() throws QueryTypeMismatchException;
    
    /**@return a collection set for SELECT queries
     */
    public Collection<Vector<Object>> getSelectResult() throws QueryTypeMismatchException;

    /**@return an XML string for the SELECT queries
     */
    public String getSelectResultasXML() throws QueryTypeMismatchException;

}
