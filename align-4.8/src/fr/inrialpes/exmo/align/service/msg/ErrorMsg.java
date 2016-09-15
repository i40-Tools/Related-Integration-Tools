/*
 * $Id: ErrorMsg.java 1988 2014-11-14 22:20:28Z euzenat $
 *
 * Copyright (C) INRIA, 2006-2011, 2014
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
import java.util.Map;
import java.util.Map.Entry;

/**
 * Contains the messages that should be sent according to the protocol
 */

public class ErrorMsg extends Message {

    public ErrorMsg ( int surr, Message rep, String from, String to, String cont, Properties param ) {
	super( surr, rep, from, to, cont, param );
    }

    public ErrorMsg ( Properties mess, int surr, String from, String cont ) {
	super( mess, surr, from, cont );
    }

    public String HTMLString(){
	String message = "Generic error: "+content;
	if ( parameters != null ) {
	    message += "<ul>";
	    for ( Entry<Object,Object> m : parameters.entrySet()) {
		message += "<li>"+m.getKey()+" = "+m.getValue()+"</li>";
	    }
	    message += "/<ul>";
	}
	return message;
    }
    public String RESTString(){
	return "<error>" + getXMLContent() + "</error>";
    }
    public String HTMLRESTString(){
	return HTMLString();
    }
    public String SOAPString(){
	return "    <ErrorMsg>"+RESTString()+"</ErrorMsg>\n";
    }
    public String JSONString(){
	return "{ \"type\" : \"ErrorMsg\", \"content\" : \""+getJSONContent()+"\" }";
    }
}
