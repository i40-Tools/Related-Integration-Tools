/*
 * $Id: HTMLMetadataRendererVisitor.java 2116 2016-09-19 08:38:32Z euzenat $
 *
 * Copyright (C) INRIA, 2006-2010, 2012-2016
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 */

package fr.inrialpes.exmo.align.impl.renderer; 

import java.util.Hashtable;
import java.util.Properties;
import java.util.Map.Entry;
import java.io.PrintWriter;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.AlignmentException;

import fr.inrialpes.exmo.align.impl.Annotations;
import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.ontowrap.LoadedOntology;

/**
 * Renders an alignment in HTML
 *
 * TODO:
 * - add CSS categories
 * - add resource chooser
 *
 * @author Jérôme Euzenat
 * @version $Id: HTMLMetadataRendererVisitor.java 2116 2016-09-19 08:38:32Z euzenat $ 
 */

public class HTMLMetadataRendererVisitor extends IndentedRendererVisitor implements AlignmentVisitor {
    final static Logger logger = LoggerFactory.getLogger(HTMLMetadataRendererVisitor.class);
    
    Alignment alignment = null;
    Hashtable<String,String> nslist = null;
    boolean embedded = false; // if the output is XML embeded in a structure

    public HTMLMetadataRendererVisitor( PrintWriter writer ){
	super( writer );
    }

    public void init( Properties p ) {
	super.init( p );
	if ( p.getProperty( "embedded" ) != null 
	     && !p.getProperty( "embedded" ).equals("") ) embedded = true;
    };

    public void visit( Alignment align ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, align, Alignment.class ) ) return;
	// default behaviour
	alignment = align;
	printHeaders( align );
	indentedOutputln("<body>");
	indentedOutputln("<div typeof=\"align:Alignment\">");
	increaseIndent();
	printAlignmentMetadata( align );
	decreaseIndent();
	indentedOutputln("</div>");
	decreaseIndent();
	indentedOutputln("</body>");
	decreaseIndent();
	indentedOutputln("</html>");
    }

    protected void printHeaders( Alignment align ) {
	nslist = new Hashtable<String,String>();
	nslist.put(Namespace.ALIGNMENT.uri,"align");
	nslist.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#","rdf");
	nslist.put("http://www.w3.org/2001/XMLSchema#","xsd");
	//nslist.put("http://www.omwg.org/TR/d7/ontology/alignment","omwg");
	// Get the keys of the parameter
	int gen = 0;
	for ( String[] ext : align.getExtensions() ){
	    String prefix = ext[0];
	    String name = ext[1];
	    String tag = nslist.get(prefix);
	    if ( tag == null ) {
		tag = "ns"+gen++;
		nslist.put( prefix, tag );
	    }
	    if ( tag.equals("align") ) { tag = name; }
	    else { tag += ":"+name; }
	    //extensionString += "  <"+tag+">"+((String[])ext)[2]+"</"+tag+">\n";
	}
	if ( embedded == false ) {
	    indentedOutputln("<?xml version=\"1.0\" encoding=\""+ENC+"\" standalone=\"no\"?>");
	    indentedOutputln("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML+RDFa 1.0//EN\" \"http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd\">");
	}
	indentedOutput("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\"");
	for ( Entry<String,String> e : nslist.entrySet() ) {
	    outputln();
	    output("       xmlns:"+e.getValue()+"='"+e.getKey()+"'");
	}
	outputln(">");
	increaseIndent();
	indentedOutputln("<head><title>Alignment</title><style type=\"text/css\">body {font-family: sans-serif}</style></head>");
    }

    protected void printAlignmentMetadata( Alignment align ) throws AlignmentException {
	String alid = align.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID );
	String pid = align.getExtension( Namespace.EXT.uri, Annotations.PRETTY );
	if ( alid == null ) alid = "Anonymous alignment";
	increaseIndent();
	if ( pid == null ) {
	    indentedOutputln("<h1>"+alid+"</h1>");
	} else {
	    indentedOutputln("<h1>"+alid+" ("+pid+")</h1>");
	}
	indentedOutputln("<table border=\"0\">");
	increaseIndent();
	indentedOutputln("<tr><td>onto1</td><td><div rel=\"align:onto1\"><div typeof=\"align:Ontology\" about=\""+align.getOntology1URI()+"\">");
	increaseIndent();
	indentedOutputln("<table>");
	increaseIndent();
	indentedOutputln("<tr><td>uri: </td><td>"+align.getOntology1URI()+"</td></tr>");
	if ( align.getFile1() != null )
	    indentedOutputln("<tr><td><span property=\"align:location\" content=\""+align.getFile1()+"\"/>file:</td><td><a href=\""+align.getFile1()+"\">"+align.getFile1()+"</a></td></tr>");
	if ( align instanceof BasicAlignment && ((BasicAlignment)align).getOntologyObject1().getFormalism() != null ) {
	    indentedOutputln("<tr><td>type:</td><td><span rel=\"align:formalism\"><span typeof=\"align:Formalism\"><span property=\"align:name\">"+((BasicAlignment)align).getOntologyObject1().getFormalism()+"</span><span property=\"align:uri\" content=\""+((BasicAlignment)align).getOntologyObject1().getFormURI()+"\"/></span></span></td></tr>");
	}
	decreaseIndent();
	indentedOutputln("</table>");
	decreaseIndent();
	indentedOutputln("</div></div></td></tr>");
	indentedOutputln("<tr><td>onto2</td><td><div rel=\"align:onto2\"><div typeof=\"align:Ontology\" about=\""+align.getOntology2URI()+"\">");
	increaseIndent();
	indentedOutputln("<table>");
	increaseIndent();
	indentedOutputln("<tr><td>uri: </td><td>"+align.getOntology2URI()+"</td></tr>");
	if ( align.getFile2() != null )
	    indentedOutputln("<tr><td><span property=\"align:location\" content=\""+align.getFile2()+"\"/>file:</td><td><a href=\""+align.getFile2()+"\">"+align.getFile2()+"</a></td></tr>");
	if ( align instanceof BasicAlignment && ((BasicAlignment)align).getOntologyObject2().getFormalism() != null ) {
	    indentedOutputln("<tr><td>type:</td><td><span rel=\"align:formalism\"><span typeof=\"align:Formalism\"><span property=\"align:name\">"+((BasicAlignment)align).getOntologyObject2().getFormalism()+"</span><span property=\"align:uri\" content=\""+((BasicAlignment)align).getOntologyObject2().getFormURI()+"\"/></span></span></td></tr>");
	}
	decreaseIndent();
	indentedOutputln("</table>");
	decreaseIndent();
	indentedOutputln("</div></div></td></tr>");
	indentedOutputln("<tr><td>level</td><td property=\"align:level\">"+align.getLevel()+"</td></tr>");
	indentedOutputln("<tr><td>type</td><td property=\"align:type\">"+align.getType()+"</td></tr>");
	// RDFa: Get the keys of the parameter (to test)
	for ( String[] ext : align.getExtensions() ){
	    indentedOutputln("<tr><td>"+ext[0]+" : "+ext[1]+"</td><td property=\""+nslist.get(ext[0])+":"+ext[1]+"\">"+ext[2]+"</td></tr>");
	}
	decreaseIndent();
	indentedOutputln("</table>");
    }

    public void visit( Cell c ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, c, Cell.class ) ) return;
	// default behaviour
    };

    public void visit( Relation r ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, r, Relation.class ) ) return;
	// default behaviour
    };
    
}
