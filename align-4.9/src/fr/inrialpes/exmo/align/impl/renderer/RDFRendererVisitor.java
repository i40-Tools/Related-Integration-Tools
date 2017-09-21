/*
 * $Id: RDFRendererVisitor.java 2140 2017-07-12 19:46:48Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2010, 2012-2017
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

import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Properties;
import java.io.PrintWriter;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.text.StringEscapeUtils;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.align.impl.Annotations;
import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicCell;
import fr.inrialpes.exmo.align.impl.BasicRelation;
import fr.inrialpes.exmo.align.impl.BasicConfidence;
import fr.inrialpes.exmo.align.impl.Extensible;

import fr.inrialpes.exmo.ontowrap.Ontology;

import fr.inrialpes.exmo.align.parser.SyntaxElement;
import fr.inrialpes.exmo.align.parser.SyntaxElement.Constructor;

import fr.inrialpes.exmo.align.impl.edoal.PathExpression;
import fr.inrialpes.exmo.align.impl.edoal.Expression;
import fr.inrialpes.exmo.align.impl.edoal.ClassExpression;
import fr.inrialpes.exmo.align.impl.edoal.ClassId;
import fr.inrialpes.exmo.align.impl.edoal.ClassConstruction;
import fr.inrialpes.exmo.align.impl.edoal.ClassTypeRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassValueRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassOccurenceRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyId;
import fr.inrialpes.exmo.align.impl.edoal.PropertyConstruction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyTypeRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyValueRestriction;
import fr.inrialpes.exmo.align.impl.edoal.RelationId;
import fr.inrialpes.exmo.align.impl.edoal.RelationConstruction;
import fr.inrialpes.exmo.align.impl.edoal.RelationDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.RelationCoDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.InstanceId;

import fr.inrialpes.exmo.align.impl.edoal.Transformation;
import fr.inrialpes.exmo.align.impl.edoal.ValueExpression;
import fr.inrialpes.exmo.align.impl.edoal.Value;
import fr.inrialpes.exmo.align.impl.edoal.Apply;
import fr.inrialpes.exmo.align.impl.edoal.Aggregate;
import fr.inrialpes.exmo.align.impl.edoal.Datatype;
import fr.inrialpes.exmo.align.impl.edoal.EDOALCell;
import fr.inrialpes.exmo.align.impl.edoal.EDOALVisitor;
import fr.inrialpes.exmo.align.impl.edoal.Linkkey;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyBinding;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyEquals;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyIntersects;

/**
 * Renders an alignment in its RDF format
 *
 * @author Jérôme Euzenat
 * @version $Id: RDFRendererVisitor.java 2140 2017-07-12 19:46:48Z euzenat $
 */
public class RDFRendererVisitor extends IndentedRendererVisitor implements AlignmentVisitor, EDOALVisitor {
    final static Logger logger = LoggerFactory.getLogger(RDFRendererVisitor.class);

    Alignment alignment = null;
    Cell cell = null;
    Hashtable<String, String> nslist = null;
    boolean embedded = false; // if the output is XML embeded in a structure

    private static Namespace DEF = Namespace.ALIGNMENT;

    private boolean isPattern = false;

    public RDFRendererVisitor(PrintWriter writer) {
        super(writer);
    }

    public void init(Properties p) {
	super.init( p );
        if (p.getProperty("embedded") != null
                && !p.getProperty("embedded").equals("")) {
            embedded = true;
        }
    }

    public String encodeURI( URI u ) {
	return StringEscapeUtils.escapeXml10( u.toASCIIString() );
    }

    public void visit(Alignment align) throws AlignmentException {
        //logger.trace( "Processing alignment {}", align );
        if (subsumedInvocableMethod(this, align, Alignment.class)) {
            return;
        }
        // default behaviour
        String extensionString = "";
        alignment = align;
        nslist = new Hashtable<String, String>();
        nslist.put( Namespace.ALIGNMENT.prefix, Namespace.ALIGNMENT.shortCut);
        nslist.put( Namespace.EXT.prefix, Namespace.EXT.shortCut);
        nslist.put( Namespace.RDF.prefix, Namespace.RDF.shortCut);
        nslist.put( Namespace.XSD.prefix, Namespace.XSD.shortCut);
        // Get the keys of the parameter
        int gen = 0;
        for ( String[] ext : align.getExtensions() ) {
            String prefix = ext[0];
            String name = ext[1];
            String tag = nslist.get( prefix );
            if ( prefix.equals( Namespace.ALIGNMENT.uri ) ) { // only because of id!
                tag = name;
            } else {
                if ( tag == null ) {
                    tag = "ns" + gen++;
                    nslist.put(prefix, tag);
                }
                tag += ":" + name;
            }
            extensionString += INDENT + "<" + tag + ">" + ext[2] + "</" + tag + ">" + NL;
        }
        if (embedded == false) {
            outputln("<?xml version='1.0' encoding='"+ENC+"' standalone='no'?>");
        }
        output("<" + SyntaxElement.RDF.print(DEF) + " xmlns='" + Namespace.ALIGNMENT.prefix + "'");
        // JE2009: (1) I must use xml:base
        //output(NL+"         xml:base='"+Namespace.ALIGNMENT.uri+"'");
	increaseIndent();
	increaseIndent();
	increaseIndent();
	increaseIndent();
	for ( Entry<String,String> e : nslist.entrySet() ) {
	    outputln();
            indentedOutput(" xmlns:" + e.getValue() + "='" + e.getKey() + "'");
        }
        if ( align instanceof BasicAlignment ) {
	    for ( Entry<Object,Object> e : ((BasicAlignment)align).getXNamespaces().entrySet() ) {
		String label = (String)e.getKey();
                if ( !label.equals("rdf") && !label.equals("xsd")  && !label.equals("<default>") ) {
		    outputln();
                    indentedOutput(" xmlns:" + label + "='" + e.getValue() + "'");
                }
            }
        }
	decreaseIndent();
	decreaseIndent();
	decreaseIndent();
	decreaseIndent();
        outputln(">" );
        indentedOutput("<" + SyntaxElement.ALIGNMENT.print(DEF));
        String idext = align.getExtension(Namespace.ALIGNMENT.uri, Annotations.ID);
        if (idext != null) {
            output(" " + SyntaxElement.RDF_ABOUT.print(DEF) + "=\"" + idext + "\"");
        }
        outputln(">" );
        increaseIndent();
        indentedOutputln("<" + SyntaxElement.XML.print(DEF) + ">yes</" + SyntaxElement.XML.print(DEF) + ">");
        if (alignment.getLevel().startsWith("2EDOALPattern")) {
            isPattern = true;
        }
        indentedOutputln("<" + SyntaxElement.LEVEL.print(DEF) + ">" + align.getLevel() + "</" + SyntaxElement.LEVEL.print(DEF) + ">");
        indentedOutputln("<" + SyntaxElement.TYPE.print(DEF) + ">" + align.getType() + "</" + SyntaxElement.TYPE.print(DEF) + ">");
        if ( align instanceof BasicAlignment ) {
	    Class<?> relationType = ((BasicAlignment)align).getRelationType();
	    if ( relationType != BasicRelation.class ) {
		indentedOutputln("<" + SyntaxElement.RELATION_CLASS.print(DEF) + ">" + relationType.getName() + "</" + SyntaxElement.RELATION_CLASS.print(DEF) + ">");
	    }
	    Class<?> confidenceType = ((BasicAlignment)align).getConfidenceType();
	    if ( confidenceType != BasicConfidence.class ) {
		indentedOutputln("<" + SyntaxElement.CONFIDENCE_CLASS.print(DEF) + ">" + confidenceType.getName() + "</" + SyntaxElement.CONFIDENCE_CLASS.print(DEF) + ">");
	    }
	}
        output(extensionString);
	// Brings complications
	//if ( align instanceof BasicAlignment ) printExtensions( (Extensible)align );
        indentedOutputln("<" + SyntaxElement.MAPPING_SOURCE.print(DEF) + ">");
        increaseIndent();
        if (align instanceof BasicAlignment) {
            printOntology(((BasicAlignment) align).getOntologyObject1());
        } else {
            printBasicOntology(align.getOntology1URI(), align.getFile1());
        }
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.MAPPING_SOURCE.print(DEF) + ">");
        indentedOutputln("<" + SyntaxElement.MAPPING_TARGET.print(DEF) + ">");
        increaseIndent();
        if (align instanceof BasicAlignment) {
            printOntology(((BasicAlignment) align).getOntologyObject2());
        } else {
            printBasicOntology(align.getOntology2URI(), align.getFile2());
        }
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.MAPPING_TARGET.print(DEF) + ">");
        for (Cell c : align) {
            c.accept(this);
        };
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.ALIGNMENT.print(DEF) + ">");
        outputln("</" + SyntaxElement.RDF.print(DEF) + ">" );
    }

    private void printBasicOntology(URI u, URI f) {
        indentedOutputln("<" + SyntaxElement.ONTOLOGY.print(DEF) + " " + SyntaxElement.RDF_ABOUT.print(DEF) + "=\"" + u + "\">");
        increaseIndent();
        if (f != null) {
            indentedOutputln("<" + SyntaxElement.LOCATION.print(DEF) + ">" + f + "</" + SyntaxElement.LOCATION.print(DEF) + ">");
        } else {
            indentedOutputln("<" + SyntaxElement.LOCATION.print(DEF) + ">" + u + "</" + SyntaxElement.LOCATION.print(DEF) + ">");
        }
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.ONTOLOGY.print(DEF) + ">");
    }

    public void printOntology( Ontology<Object> onto ) {
        URI u = onto.getURI();
        URI f = onto.getFile();
	indentedOutputln("<" + SyntaxElement.ONTOLOGY.print(DEF) + " " + SyntaxElement.RDF_ABOUT.print(DEF) + "=\"" + encodeURI( u ) + "\">");
	increaseIndent();
	if (f != null) {
	    indentedOutputln("<" + SyntaxElement.LOCATION.print(DEF) + ">" + f + "</" + SyntaxElement.LOCATION.print(DEF) + ">");
	} else {
	    indentedOutputln("<" + SyntaxElement.LOCATION.print(DEF) + ">" + encodeURI(u) + "</" + SyntaxElement.LOCATION.print(DEF) + ">");
	}
	if (onto.getFormalism() != null) {
	    indentedOutputln("<" + SyntaxElement.FORMATT.print(DEF) + ">");
	    increaseIndent();
	    indentedOutputln("<" + SyntaxElement.FORMALISM.print(DEF) + " " + SyntaxElement.NAME.print() + "=\"" + onto.getFormalism() + "\" " + SyntaxElement.URI.print() + "=\"" + encodeURI(onto.getFormURI()) + "\"/>");
	    decreaseIndent();
	    indentedOutputln("</" + SyntaxElement.FORMATT.print(DEF) + ">");
	}
	decreaseIndent();
	indentedOutputln("</" + SyntaxElement.ONTOLOGY.print(DEF) + ">");
    }

    protected void printExtensions( final Extensible extent ) {
	if ( extent.getExtensions() != null ) {
	    for ( String[] ext : extent.getExtensions() ) {
		String uri = ext[0];
		String tag = (nslist==null)?null:nslist.get(uri);
		if (tag == null) {
		    tag = ext[1];
		    // That's heavy.
		    // Maybe adding an extra: ns extension in the alignment at parsing time
		    // would help redisplaying it better...
		    indentedOutputln("<alignapilocalns:" + tag + " xmlns:alignapilocalns=\"" + uri + "\">" + ext[2] + "</alignapilocalns:" + tag + ">");
		} else {
		    tag += ":" + ext[1];
		    indentedOutputln("<" + tag + ">" + ext[2] + "</" + tag + ">");
		}
	    }
	}
    }

    public void visit( Cell cell ) throws AlignmentException {
        //logger.trace( "Processing cell {}", cell );
        if (subsumedInvocableMethod(this, cell, Cell.class)) {
            return;
        }
        // default behaviour
        this.cell = cell;
        URI u1 = cell.getObject1AsURI(alignment);
        URI u2 = cell.getObject2AsURI(alignment);
        if ((u1 != null && u2 != null)
                || alignment.getLevel().startsWith("2EDOAL")) { //expensive test
            indentedOutputln("<" + SyntaxElement.MAP.print(DEF) + ">");
            increaseIndent();
            indentedOutput("<" + SyntaxElement.CELL.print(DEF));
            if (cell.getId() != null && !cell.getId().equals("")) {
                output(" " + SyntaxElement.RDF_ABOUT.print(DEF) + "=\"" + cell.getId() + "\"");
            }
            outputln(">" );
            increaseIndent();
            if ( alignment.getLevel().startsWith("2EDOAL") ) {
                indentedOutputln("<" + SyntaxElement.ENTITY1.print(DEF) + ">");
                increaseIndent();
                //logger.trace( "Processing ob1 {}", cell.getObject1() );
                ((Expression) (cell.getObject1())).accept(this);
                decreaseIndent();
                outputln();
                indentedOutputln("</" + SyntaxElement.ENTITY1.print(DEF) + ">");
                indentedOutputln("<" + SyntaxElement.ENTITY2.print(DEF) + ">");
                increaseIndent();
                ((Expression) (cell.getObject2())).accept(this);
                //logger.trace( "Processing ob2 {}", cell.getObject2() );
                decreaseIndent();
                outputln();
                indentedOutputln("</" + SyntaxElement.ENTITY2.print(DEF) + ">");
            } else {
		indentedOutputln("<" + SyntaxElement.ENTITY1.print(DEF) + " " + SyntaxElement.RDF_RESOURCE.print(DEF) + "='" + encodeURI(u1) + "'/>");
		indentedOutputln("<" + SyntaxElement.ENTITY2.print(DEF) + " " + SyntaxElement.RDF_RESOURCE.print(DEF) + "='" + encodeURI(u2) + "'/>");
            }
            indentedOutput("<" + SyntaxElement.RULE_RELATION.print(DEF) + ">");
            cell.getRelation().accept(this);
            outputln("</" + SyntaxElement.RULE_RELATION.print(DEF) + ">" );
            indentedOutputln("<" + SyntaxElement.MEASURE.print(DEF) + " " + SyntaxElement.RDF_DATATYPE.print(DEF) + "='" + Namespace.XSD.getUriPrefix() + "float'>" + cell.getStrength() + "</" + SyntaxElement.MEASURE.print(DEF) + ">");
            if (cell.getSemantics() != null
                    && !cell.getSemantics().equals("")
                    && !cell.getSemantics().equals("first-order")) {
                indentedOutputln("<" + SyntaxElement.SEMANTICS.print(DEF) + ">" + cell.getSemantics() + "</" + SyntaxElement.SEMANTICS.print(DEF) + ">");
            }
	    if ( cell instanceof EDOALCell ) { // output Linkkeys and Transformations²
		Set<Linkkey> linkkeys = ((EDOALCell)cell).linkkeys();
		if ( linkkeys != null ) {
		    for (Linkkey linkkey : linkkeys) {
			linkkey.accept(this);
		    }
		}
		Set<Transformation> transfs = ((EDOALCell)cell).transformations();
		if (transfs != null) {
		    for (Transformation transf : transfs) {
			indentedOutputln("<" + SyntaxElement.TRANSFORMATION.print(DEF) + ">");
			increaseIndent();
			transf.accept(this);
			decreaseIndent();
			outputln();
			indentedOutputln("</" + SyntaxElement.TRANSFORMATION.print(DEF) + ">");
		    }
		}
	    }
	    if ( cell instanceof BasicCell ) printExtensions( (Extensible)cell );
            decreaseIndent();
            indentedOutputln("</" + SyntaxElement.CELL.print(DEF) + ">");
            decreaseIndent();
            indentedOutputln("</" + SyntaxElement.MAP.print(DEF) + ">");
        }
    }

    public void visit(Relation rel) throws AlignmentException {
        if (subsumedInvocableMethod(this, rel, Relation.class)) {
            return;
        }
        // default behaviour
        rel.write(writer);
    }

    ;

    // ********** EDOAL

    public void renderVariables(Expression expr) {
        if (expr.getVariable() != null) {
            output(" " + SyntaxElement.VAR.print(DEF) + "=\"" + expr.getVariable().name());
        }
    }

    public void visit(final ClassId e) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.CLASS_EXPR.print(DEF));
        if (e.getURI().toString() != null) {
	    output(" " + SyntaxElement.RDF_ABOUT.print(DEF));
	    output("=\"" + encodeURI(e.getURI()) + "\"");
        }
        if (isPattern) {
            renderVariables(e);
        }
        output("/>");
    }

    public void visit(final ClassConstruction e) throws AlignmentException {
        final Constructor op = e.getOperator();
        String sop = SyntaxElement.getElement(op).print(DEF);
        indentedOutput("<" + SyntaxElement.CLASS_EXPR.print(DEF));
        if (isPattern) {
            renderVariables(e);
        }
        outputln(">" );
        increaseIndent();
        indentedOutput("<" + sop);
        if ((op == Constructor.AND) || (op == Constructor.OR)) {
            output(" " + SyntaxElement.RDF_PARSETYPE.print(DEF) + "=\"Collection\"");
        }
        outputln(">" );
        increaseIndent();
        for (final ClassExpression ce : e.getComponents()) {
            output(linePrefix);
            ce.accept(this);
            outputln();
        }
        decreaseIndent();
        indentedOutputln("</" + sop + ">");
        decreaseIndent();
        indentedOutput("</" + SyntaxElement.CLASS_EXPR.print(DEF) + ">");
    }

    public void visit(final ClassValueRestriction c) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.VALUE_COND.print(DEF));
        if (isPattern) {
            renderVariables(c);
        }
        outputln(">" );
        increaseIndent();
        indentedOutputln("<" + SyntaxElement.ONPROPERTY.print(DEF) + ">");
        increaseIndent();
        c.getRestrictionPath().accept(this);
        decreaseIndent();
        outputln();
        indentedOutputln("</" + SyntaxElement.ONPROPERTY.print(DEF) + ">");
        indentedOutput("<" + SyntaxElement.COMPARATOR.print(DEF));
        output(" " + SyntaxElement.RDF_RESOURCE.print(DEF));
	output("=\"" + encodeURI(c.getComparator().getURI()));
        outputln("\"/>" );
        indentedOutputln("<" + SyntaxElement.VALUE.print(DEF) + ">");
        increaseIndent();
        c.getValue().accept(this);
        outputln();
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.VALUE.print(DEF) + ">");
        decreaseIndent();
        indentedOutput("</" + SyntaxElement.VALUE_COND.print(DEF) + ">");
    }

    public void visit(final ClassTypeRestriction c) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.TYPE_COND.print(DEF));
        if (isPattern) {
            renderVariables(c);
        }
        outputln(">" );
        increaseIndent();
        indentedOutputln("<" + SyntaxElement.ONPROPERTY.print(DEF) + ">");
        increaseIndent();
        c.getRestrictionPath().accept(this);
        outputln();
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.ONPROPERTY.print(DEF) + ">");
        c.getType().accept(this); // Directly -> to be changed for rendering all/exists
        decreaseIndent();
        outputln();
        indentedOutput("</" + SyntaxElement.TYPE_COND.print(DEF) + ">");
    }

    public void visit(final ClassDomainRestriction c) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.DOMAIN_RESTRICTION.print(DEF));
        if (isPattern) {
            renderVariables(c);
        }
        outputln(">" );
        increaseIndent();
        indentedOutputln("<" + SyntaxElement.ONPROPERTY.print(DEF) + ">");
        increaseIndent();
        c.getRestrictionPath().accept(this);
        outputln();
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.ONPROPERTY.print(DEF) + ">");
        if (c.isUniversal()) {
            indentedOutputln("<" + SyntaxElement.ALL.print(DEF) + ">");
        } else {
            indentedOutputln("<" + SyntaxElement.EXISTS.print(DEF) + ">");
        }
        increaseIndent();
        c.getDomain().accept(this);
        outputln();
        decreaseIndent();
        if (c.isUniversal()) {
            indentedOutputln("</" + SyntaxElement.ALL.print(DEF) + ">");
        } else {
            indentedOutputln("</" + SyntaxElement.EXISTS.print(DEF) + ">");
        }
        decreaseIndent();
        indentedOutput("</" + SyntaxElement.DOMAIN_RESTRICTION.print(DEF) + ">");
    }

    public void visit(final ClassOccurenceRestriction c) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.OCCURENCE_COND.print(DEF));
        if (isPattern) {
            renderVariables(c);
        }
        outputln(">" );
        increaseIndent();
        indentedOutputln("<" + SyntaxElement.ONPROPERTY.print(DEF) + ">");
        increaseIndent();
        c.getRestrictionPath().accept(this);
        outputln();
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.ONPROPERTY.print(DEF) + ">");
        indentedOutput("<" + SyntaxElement.COMPARATOR.print(DEF));
        output(" " + SyntaxElement.RDF_RESOURCE.print(DEF));
	output("=\"" + encodeURI(c.getComparator().getURI()));
        outputln("\"/>" );
        indentedOutput("<" + SyntaxElement.VALUE.print(DEF) + ">");
        output( ""+c.getOccurence() );
        outputln("</" + SyntaxElement.VALUE.print(DEF) + ">");
        decreaseIndent();
        indentedOutput("</" + SyntaxElement.OCCURENCE_COND.print(DEF) + ">");
    }

    public void visit(final PropertyId e) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.PROPERTY_EXPR.print(DEF));
        if (e.getURI().toString() != null) {
            output(" " + SyntaxElement.RDF_ABOUT.print(DEF));
	    output("=\"" + encodeURI(e.getURI()) + "\"");
	    if ( e.getLanguage() != null ) {
		output(" " + SyntaxElement.LANG.print(DEF));
		output("=\""+e.getLanguage()+"\"");
	    }
        }
        if (isPattern) {
            renderVariables(e);
        }
        output("/>");
    }

    public void visit(final PropertyConstruction e) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.PROPERTY_EXPR.print(DEF));
        if (isPattern) {
            renderVariables(e);
        }
        outputln(">" );
        increaseIndent();
        final Constructor op = e.getOperator();
        String sop = SyntaxElement.getElement(op).print(DEF);
        indentedOutput("<" + sop);
        if ((op == Constructor.AND) || (op == Constructor.OR) || (op == Constructor.COMP)) {
            output(" " + SyntaxElement.RDF_PARSETYPE.print(DEF) + "=\"Collection\"");
        }
        outputln(">" );
        increaseIndent();
        if ((op == Constructor.AND) || (op == Constructor.OR) || (op == Constructor.COMP)) {
            for (final PathExpression pe : e.getComponents()) {
                output(linePrefix);
                pe.accept(this);
                outputln();
            }
        } else {
            for (final PathExpression pe : e.getComponents()) {
                pe.accept(this);
                outputln();
            }
        }
        decreaseIndent();
        indentedOutputln("</" + sop + ">");
        decreaseIndent();
        indentedOutput("</" + SyntaxElement.PROPERTY_EXPR.print(DEF) + ">");
    }

    public void visit(final PropertyValueRestriction c) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.PROPERTY_VALUE_COND.print(DEF));
        if (isPattern) {
            renderVariables(c);
        }
        outputln(">" );
        increaseIndent();
        indentedOutput("<" + SyntaxElement.COMPARATOR.print(DEF));
        output(" " + SyntaxElement.RDF_RESOURCE.print(DEF));
	output("=\"" + encodeURI(c.getComparator().getURI()));
        outputln("\"/>" );
        indentedOutputln("<" + SyntaxElement.VALUE.print(DEF) + ">");
        increaseIndent();
        c.getValue().accept(this);
        outputln();
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.VALUE.print(DEF) + ">");
        decreaseIndent();
        indentedOutput("</" + SyntaxElement.PROPERTY_VALUE_COND.print(DEF) + ">");
    }

    public void visit(final PropertyDomainRestriction c) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.PROPERTY_DOMAIN_COND.print(DEF));
        if (isPattern) {
            renderVariables(c);
        }
        outputln(">" );
        increaseIndent();
        indentedOutputln("<" + SyntaxElement.TOCLASS.print(DEF) + ">");
        increaseIndent();
        c.getDomain().accept(this);
        outputln();
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.TOCLASS.print(DEF) + ">");
        decreaseIndent();
        indentedOutput("</" + SyntaxElement.PROPERTY_DOMAIN_COND.print(DEF) + ">");
    }

    public void visit(final PropertyTypeRestriction c) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.PROPERTY_TYPE_COND.print(DEF));
        if (isPattern) {
            renderVariables(c);
        }
        outputln(">" );
        increaseIndent();
        c.getType().accept(this);
        decreaseIndent();
        indentedOutput("</" + SyntaxElement.PROPERTY_TYPE_COND.print(DEF) + ">");
    }

    public void visit(final RelationId e) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.RELATION_EXPR.print(DEF));
        if (e.getURI().toString() != null) {
            output(" " + SyntaxElement.RDF_ABOUT.print(DEF));
	    output("=\"" + encodeURI(e.getURI()) + "\"");
        }
        if (isPattern) {
            renderVariables(e);
        }
        output("/>");
    }

    public void visit(final RelationConstruction e) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.RELATION_EXPR.print(DEF));
        if (isPattern) {
            renderVariables(e);
        }
        outputln(">" );
        increaseIndent();
        final Constructor op = e.getOperator();
        String sop = SyntaxElement.getElement(op).print(DEF);
        indentedOutput("<" + sop);
        if ((op == Constructor.OR) || (op == Constructor.AND) || (op == Constructor.COMP)) {
            output(" " + SyntaxElement.RDF_PARSETYPE.print(DEF) + "=\"Collection\"");
        }
        outputln(">" );
        increaseIndent();
        if ((op == Constructor.AND) || (op == Constructor.OR) || (op == Constructor.COMP)) {
            for (final PathExpression re : e.getComponents()) {
                output(linePrefix);
                re.accept(this);
                outputln();
            }
        } else { // NOT... or else: enumerate them
            for (final PathExpression re : e.getComponents()) {
                re.accept(this);
                outputln();
            }
        }
        decreaseIndent();
        indentedOutputln("</" + sop + ">");
        decreaseIndent();
        indentedOutput("</" + SyntaxElement.RELATION_EXPR.print(DEF) + ">");
    }

    public void visit(final RelationCoDomainRestriction c) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.RELATION_CODOMAIN_COND.print(DEF));
        if (isPattern) {
            renderVariables(c);
        }
        outputln(">" );
        increaseIndent();
        indentedOutputln("<" + SyntaxElement.TOCLASS.print(DEF) + ">");
        increaseIndent();
        c.getCoDomain().accept(this);
        outputln();
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.TOCLASS.print(DEF) + ">");
        decreaseIndent();
        indentedOutput("</" + SyntaxElement.RELATION_CODOMAIN_COND.print(DEF) + ">");
    }

    public void visit(final RelationDomainRestriction c) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.RELATION_DOMAIN_COND.print(DEF));
        if (isPattern) {
            renderVariables(c);
        }
        outputln(">" );
        increaseIndent();
        indentedOutputln("<" + SyntaxElement.TOCLASS.print(DEF) + ">");
        increaseIndent();
        c.getDomain().accept(this);
        outputln();
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.TOCLASS.print(DEF) + ">");
        decreaseIndent();
        indentedOutput("</" + SyntaxElement.RELATION_DOMAIN_COND.print(DEF) + ">");
    }

    public void visit(final InstanceId e) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.INSTANCE_EXPR.print(DEF));
        if (e.getURI().toString() != null) {
            output(" " + SyntaxElement.RDF_ABOUT.print(DEF));
	    output("=\"" + encodeURI(e.getURI()) + "\"");
        }
        if (isPattern) {
            renderVariables(e);
        }
        output("/>");
    }

    public void visit(final Value e) throws AlignmentException {
        indentedOutput("<" + SyntaxElement.LITERAL.print(DEF) + " ");
        if (e.getType() != null) {
            output(SyntaxElement.ETYPE.print(DEF) + "=\"" + e.getType() + "\" ");
        }
        if (e.getLang() != null) {
            output(SyntaxElement.LANG.print(DEF) + "=\"" + e.getLang() + "\" ");
        }
        output(SyntaxElement.STRING.print(DEF) + "=\"" + e.getValue() + "\"/>");
    }

    public void visit(final Apply e) throws AlignmentException {
        indentedOutputln("<" + SyntaxElement.APPLY.print(DEF) + " " + SyntaxElement.OPERATOR.print(DEF) + "=\"" + e.getOperation() + "\">");
        increaseIndent();
        indentedOutputln("<" + SyntaxElement.ARGUMENTS.print(DEF) + " " + SyntaxElement.RDF_PARSETYPE.print(DEF) + "=\"Collection\">");
        increaseIndent();
        for (final ValueExpression ve : e.getArguments()) {
            output(linePrefix);
            ve.accept(this);
            outputln();
        }
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.ARGUMENTS.print(DEF) + ">");
        decreaseIndent();
        indentedOutput("</" + SyntaxElement.APPLY.print(DEF) + ">");
    }

    public void visit(final Aggregate e) throws AlignmentException {
        indentedOutputln("<" + SyntaxElement.AGGREGATE.print(DEF) + " " + SyntaxElement.OPERATOR.print(DEF) + "=\"" + e.getOperation() + "\">");
        increaseIndent();
        indentedOutputln("<" + SyntaxElement.ARGUMENTS.print(DEF) + " " + SyntaxElement.RDF_PARSETYPE.print(DEF) + "=\"Collection\">");
        increaseIndent();
        for (final ValueExpression ve : e.getArguments()) {
            output(linePrefix);
            ve.accept(this);
            outputln();
        }
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.ARGUMENTS.print(DEF) + ">");
        decreaseIndent();
        indentedOutput("</" + SyntaxElement.AGGREGATE.print(DEF) + ">");
    }

    public void visit(final Transformation transf) throws AlignmentException {
        indentedOutputln("<" + SyntaxElement.TRANSF.print(DEF) + " " + SyntaxElement.TRDIR.print(DEF) + "=\"" + transf.getType() + "\">");
        increaseIndent();
        indentedOutputln("<" + SyntaxElement.TRENT1.print(DEF) + ">");
        increaseIndent();
        transf.getObject1().accept(this);
        decreaseIndent();
        outputln();
        indentedOutputln("</" + SyntaxElement.TRENT1.print(DEF) + ">");
        indentedOutputln("<" + SyntaxElement.TRENT2.print(DEF) + ">");
        increaseIndent();
        transf.getObject2().accept(this);
        decreaseIndent();
        outputln();
        indentedOutputln("</" + SyntaxElement.TRENT2.print(DEF) + ">");
        decreaseIndent();
        indentedOutput("</" + SyntaxElement.TRANSF.print(DEF) + ">");
    }

    public void visit(final Datatype e) {
        indentedOutput("<" + SyntaxElement.EDATATYPE.print(DEF) + ">");
        output("<" + SyntaxElement.DATATYPE.print(DEF) + " " + SyntaxElement.RDF_ABOUT.print(DEF) + "=\"" + e.getType() + "\"/>");
        output("</" + SyntaxElement.EDATATYPE.print(DEF) + ">");
    }

    public void visit(final Linkkey linkkey) throws AlignmentException {
        indentedOutputln("<" + SyntaxElement.LINKKEYS.print(DEF) + ">");
        increaseIndent();
        indentedOutputln("<" + SyntaxElement.LINKKEY.print(DEF) + ">");
        increaseIndent();
	printExtensions( linkkey );
        for (LinkkeyBinding linkkeyBinding : linkkey.bindings()) {
            indentedOutputln("<" + SyntaxElement.LINKKEY_BINDING.print(DEF) + ">");
            increaseIndent();
            linkkeyBinding.accept(this);
            decreaseIndent();
            indentedOutputln("</" + SyntaxElement.LINKKEY_BINDING.print(DEF) + ">");
        }
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.LINKKEY.print(DEF) + ">");
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.LINKKEYS.print(DEF) + ">");
    }

    private void visitLinkKeyBinding(LinkkeyBinding linkkeyBinding, SyntaxElement syntaxElement) throws AlignmentException {
        indentedOutputln("<" + syntaxElement.print(DEF) + ">");
        increaseIndent();
        indentedOutputln("<" + SyntaxElement.LINKEY_PROPERTY1.print(DEF) + ">");
        increaseIndent();
        linkkeyBinding.getExpression1().accept(this);
	outputln();
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.LINKEY_PROPERTY1.print(DEF) + ">");
        indentedOutputln("<" + SyntaxElement.LINKEY_PROPERTY2.print(DEF) + ">");
        increaseIndent();
        linkkeyBinding.getExpression2().accept(this);
	outputln();
        decreaseIndent();
        indentedOutputln("</" + SyntaxElement.LINKEY_PROPERTY2.print(DEF) + ">");
        decreaseIndent();
        indentedOutputln("</" + syntaxElement.print(DEF) + ">");
    }

    public void visit(final LinkkeyEquals linkkeyEquals) throws AlignmentException {
        visitLinkKeyBinding(linkkeyEquals, SyntaxElement.LINKEY_EQUALS);
    }

    public void visit(final LinkkeyIntersects linkkeyIntersects) throws AlignmentException {
        visitLinkKeyBinding(linkkeyIntersects, SyntaxElement.LINKEY_INTERSECTS);
    }

}
