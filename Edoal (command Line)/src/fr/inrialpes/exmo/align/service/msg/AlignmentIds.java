/*
 * $Id: AlignmentIds.java 2150 2017-07-18 15:15:46Z euzenat $
 *
 * Copyright (C) INRIA, 2006-2009, 2011, 2013-2014, 2017
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the messages that should be sent according to the protocol
 */

public class AlignmentIds extends Success {
    final static Logger logger = LoggerFactory.getLogger( AlignmentIds.class );

    String pretty = null;

    public AlignmentIds ( int surr, Message rep, String from, String to, String cont, Properties param ) {
	super( surr, rep, from, to, cont, param );
    }

    public AlignmentIds ( int surr, Message rep, String from, String to, String cont, Properties param, String pretty ) {
	super( surr, rep, from, to, cont, param );
	this.pretty = pretty;
    }

    public AlignmentIds ( Properties mess, int surr, String from, String cont, String pretty ) {
	super( mess, surr, from, cont );
	this.pretty = pretty;
    }

    public AlignmentIds ( Properties mess, int surr, String from, String cont ) {
	super( mess, surr, from, cont );
    }

    public String HTMLString(){
	String id[] = content.split(" ", 0);
	String pid[] = pretty.split(":", 0);
	String result = "No alignment.";

	if ( id.length >= 1 ) {
	    result = "Alignment Ids: <ul>";
	    for ( int i = id.length-1; i >= 1; i-- ){
		// logger.trace("id[{}]{}", i, id[i] );
		// result += "<li><a href=\"../html/retrieve?method=fr.inrialpes.exmo.align.impl.renderer.HTMLRendererVisitor&id="+id[i]+"\">";
		result += "<li><a href=\""+id[i]+"\">";
		result += id[i];
		String pp = null;
		if ( pid != null ) {
		    pp = pid[i];
		    if (pp != null && !pp.equals("") && !pp.equals("null"))  {		
			result += " ("+pp+")";
		    }
		    // logger.trace( "pid[{}]={}", i, pp );
		} 
		result += "</a></li>";
	    }
	    result += "</ul>";
	}
	return result;
    }

    public String HTMLRESTString(){
	String result = "No alignment.";
	String id[] = content.split(" ", 0);

	if ( id.length >= 1 ) {
	    result = "Alignment Ids: <ul>";
	    for ( int i = id.length-1; i >= 1; i-- ){
		result += "<li><a href=\"../rest/retrieve?method=fr.inrialpes.exmo.align.impl.renderer.HTMLRendererVisitor&id="+id[i]+"\">"+id[i]+"</a>";
		result += "<table><tr>";
		result += "<td><form action=\"getID\"><input type=\"hidden\" name=\"id\" value=\""+id[i]+"\"/><input type=\"submit\" name=\"action\" value=\"GetID\"  disabled=\"disabled\"/></form></td>";
		result += "<td><form action=\"metadata\"><input type=\"hidden\" name=\"id\" value=\""+id[i]+"\"/><input type=\"submit\" name=\"action\" value=\"Metadata\"/></form></td>";
		result += "</li>";
	    }
	    result += "</ul>";
	}
	return result;
    }

    public String RESTString(){
	String msg = "<alignmentList>\n";
	String id[] = content.split(" ", 0);
	for ( int i = id.length-1; i >= 1; i-- ){
	    if ( ! id[i].trim().isEmpty() ) {
		msg += "        <alid>"+id[i].trim()+"</alid>\n";
	    }
	}
	msg += "      </alignmentList>";
	return msg;
    }

    public String JSONString(){
	String msg = "[";
	String id[] = content.split(" ", 0);
	for ( int i = id.length-1; i >= 1; i-- ){
	    if ( ! id[i].trim().isEmpty() ) {
		msg += "    \""+id[i].trim()+"\",\n";
	    }
	}	
	msg += "]";
	return msg;
    }
}
