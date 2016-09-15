/*
 * $Id: EvalResult.java 1988 2014-11-14 22:20:28Z euzenat $
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

public class EvalResult extends Success {

    public EvalResult ( int surr, Message rep, String from, String to, String cont, Properties param ) {
	super( surr, rep, from, to, cont, param );
    }

    public EvalResult ( Properties mess, int surr, String from, String cont, Properties param ) {
	super( mess, surr, from, cont );
	parameters = param;
    }

    public String HTMLString(){
	String results = "";
	if ( getParameters() == null ) {
	    results += getContent();
	} else {
	    results += "Alignment method: "+getContent()+"\n<ul>\n";
	    for ( Entry<Object,Object> m : getParameters().entrySet() ) {
		results += "<li>"+m.getKey()+" : "+m.getValue()+"</li>\n";
	    }
	    results += "</ul>\n";
	}
	return results;
    }

    // A diff will never be rendered outside of HTML
    public String RESTString(){
	String results = "<EvaluationResults>";
	for ( Entry<Object,Object> m : getParameters().entrySet() ) {
	    results += "<"+m.getKey()+">"+m.getValue()+"</"+m.getKey()+">";
	}
	results += "</EvaluationResults>";
	return results;	
    }
    public String JSONString(){
	String results = "{ \"type\" : \"EvaluationResult\"";
	for ( Entry<Object,Object> m : getParameters().entrySet() ) {
	    results += ",\n  \""+m.getKey()+"\" : \""+m.getValue()+"\"";
	}
	results += "\n}";
	return results;	
    }

}
