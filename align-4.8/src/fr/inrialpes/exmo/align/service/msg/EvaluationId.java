/*
 * $Id: EvaluationId.java 1902 2014-03-17 19:39:04Z euzenat $
 *
 * Copyright (C) INRIA, 2008, 2011, 2014
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

package fr.inrialpes.exmo.align.service.msg;

import java.util.Properties;

/**
 * The id of the result of an evaluation
 */

public class EvaluationId extends Success {

    String pretty = null;

    public EvaluationId ( int surr, Message rep, String from, String to, String cont, Properties param ) {
	super( surr, rep, from, to, cont, param );
    }
    public String getPretty( String alid ) {
	// getextension "pretty"
	// if no pretty then 
	return alid;
    };
    public String HTMLString(){
	return getContent();
    }

    public String RESTString(){
	return "<alid>"+content+"</alid>";	
    }
    public String JSONString(){
	return "{ \"type\" : \"EvaluationId\", \"id\" : \""+content+"\" }";	
    }

}
