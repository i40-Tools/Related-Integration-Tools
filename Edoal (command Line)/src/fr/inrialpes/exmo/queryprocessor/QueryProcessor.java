/*
 * $Id: QueryProcessor.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) INRIA Rhône-Alpes, 2006, 2008
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

/*
 * QueryProcessor.java
 *
 * Created on March 20, 2006, 10:29 AM
 *
 */

package fr.inrialpes.exmo.queryprocessor;

/**
 *
 * @author Arun Sharma
 */
public interface QueryProcessor {
    /**
     * @param query -- The query string
     * @param type -- The query type, can be one of SELECT, ASK, CONSTRUCT, or DESCRIBE
     * @return  the query result as a Result object depending on type
     */
    public Result query( String query, Type type );
    
    /**
     * @param query  -- The query string
     * @return the query result as a Result object
     */
    public Result query( String query );

    /**
     * @param query -- The query string
     * @return the query results as a string
     */
    public String queryWithStringResults( String query );
    
    /**
     * @param query -- the query string
     * @return the type of the query
     */
    public int getType( String query );
    
    /**
     * Loads an ontology
     *
     * @param uri -- the URI of the ontology to load
     */
    public void loadOntology( String uri );
    
}
