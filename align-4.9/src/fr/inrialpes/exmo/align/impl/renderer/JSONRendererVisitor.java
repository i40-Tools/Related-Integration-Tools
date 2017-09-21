/*
 * $Id: JSONRendererVisitor.java 2116 2016-09-19 08:38:32Z euzenat $
 *
 * Copyright (C) INRIA, 2012, 2014-2016
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
import java.util.Set;
import java.util.Map.Entry;
import java.util.Properties;
import java.io.PrintWriter;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.align.impl.Annotations;
import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.BasicCell;
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
import java.util.Collection;

/**
 * Renders an alignment in JSON (and in fact in JSON-LD) IETF RFC 7159 +
 * http://www.w3.org/TR/json-ld/
 *
 * application/json : media type available
 *
 * @author Jérôme Euzenat
 * @version $Id: JSONRendererVisitor.java 2116 2016-09-19 08:38:32Z euzenat $
 */
public class JSONRendererVisitor extends IndentedRendererVisitor implements AlignmentVisitor, EDOALVisitor {
    final static Logger logger = LoggerFactory.getLogger(JSONRendererVisitor.class);

    Alignment alignment = null;
    Cell cell = null;
    Hashtable<String, String> nslist = null;

    // We do not want a default namespace here
    private static Namespace DEF = Namespace.NONE;

    private boolean isPattern = false;

    public JSONRendererVisitor(PrintWriter writer) {
        super(writer);
    }

    public void init( Properties p ) {
	super.init( p );
    }

    public void visit(Alignment align) throws AlignmentException {
        if (subsumedInvocableMethod(this, align, Alignment.class)) {
            return;
        }
        // default behaviour
        String extensionString = "";
        alignment = align;
        nslist = new Hashtable<String, String>();
        nslist.put(Namespace.ALIGNMENT.prefix, Namespace.ALIGNMENT.shortCut);
        nslist.put(Namespace.RDF.prefix, Namespace.RDF.shortCut);
        nslist.put(Namespace.XSD.prefix, Namespace.XSD.shortCut);
        // how does it get there for RDF?
        nslist.put(Namespace.EDOAL.prefix, Namespace.EDOAL.shortCut);
        // Get the keys of the parameter
        int gen = 0;
        for ( String[] ext : align.getExtensions() ) {
            String prefix = ext[0];
            String name = ext[1];
            if (!(prefix.endsWith("#") || prefix.endsWith("/"))) {
                prefix += "#";
            }
            String tag = nslist.get(prefix);
            if (tag == null) {
                tag = "ns" + gen++;
                nslist.put(prefix, tag);
            }
            tag += ":" + name;
            extensionString += INDENT + "\"" + tag + "\" : \"" + ext[2] + "\"," + NL;
        }
        if ( align instanceof BasicAlignment ) {
            for ( Entry<Object,Object> e : ((BasicAlignment)align).getXNamespaces().entrySet() ) {
		String label = (String)e.getKey();
                if ( !label.equals("rdf") && !label.equals("xsd") && !label.equals("<default>") ) {
                    extensionString += INDENT + "\"" + label + "\" : \"" + e.getValue() + "\"," + NL;
                }
            }
        }
        indentedOutputln("{ \"@type\" : \"" + SyntaxElement.ALIGNMENT.print(DEF) + "\",");
        increaseIndent();
        indentedOutputln("\"@context\" : {");
        increaseIndent();
	for ( Entry<String,String> e : nslist.entrySet() ) {
            indentedOutputln("\"" + e.getValue() + "\" : \"" + e.getKey() + "\",");
        }
        // Not sure that this is fully correct
        indentedOutputln("\"" + SyntaxElement.MEASURE.print(DEF) + "\" : { \"@type\" : \"xsd:float\" }");
        decreaseIndent();
        indentedOutputln("},");
        String idext = align.getExtension(Namespace.ALIGNMENT.uri, Annotations.ID);
        if (idext != null) {
            //indentedOutputln("\"rdf:about\" : \""+idext+"\",");
            indentedOutputln("\"@id\" : \"" + idext + "\",");
        }
        if (alignment.getLevel().startsWith("2EDOALPattern")) {
            isPattern = true;
        }
        indentedOutputln("\"" + SyntaxElement.LEVEL.print(DEF) + "\" : \"" + align.getLevel() + "\",");
        indentedOutputln("\"" + SyntaxElement.TYPE.print(DEF) + "\" : \"" + align.getType() + "\",");
        output(extensionString);
        indentedOutputln("\"" + SyntaxElement.MAPPING_SOURCE.print(DEF) + "\" : ");
        increaseIndent();
        if (align instanceof BasicAlignment) {
            printOntology(((BasicAlignment) align).getOntologyObject1());
        } else {
            printBasicOntology(align.getOntology1URI(), align.getFile1());
        }
        decreaseIndent();
        outputln(",");
        indentedOutputln("\"" + SyntaxElement.MAPPING_TARGET.print(DEF) + "\" : ");
        increaseIndent();
        if (align instanceof BasicAlignment) {
            printOntology(((BasicAlignment) align).getOntologyObject2());
        } else {
            printBasicOntology(align.getOntology2URI(), align.getFile2());
        }
        outputln(",");
        decreaseIndent();
        indentedOutputln("\"" + SyntaxElement.MAP.print(DEF) + "\" : [");
        increaseIndent();
        boolean first = true;
        for (Cell c : align) {
            if (first) {
                first = false;
            } else {
                outputln(",");
            }
            c.accept(this);
        };
        outputln();
        decreaseIndent();
        indentedOutputln("]");
        decreaseIndent();
        indentedOutputln("}");
    }

    private void printBasicOntology(URI u, URI f) {
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.ONTOLOGY.print(DEF) + "\"," + NL);
        increaseIndent();
        //indentedOutput("\rdf:about\" : \""+u+"\","+NL);
        indentedOutput("\"@id\" : \"" + u + "\"," + NL);
        if (f != null) {
            indentedOutput("\"" + SyntaxElement.LOCATION.print(DEF) + "\" : \"" + f + "\"," + NL);
        } else {
            indentedOutput("\"" + SyntaxElement.LOCATION.print(DEF) + "\" : \"" + u + "\"," + NL);
        }
        decreaseIndent();
        indentedOutput("}");
    }

    public void printOntology( Ontology<?> onto ) {
        URI u = onto.getURI();
        URI f = onto.getFile();
        indentedOutputln("{ \"@type\" : \"" + SyntaxElement.ONTOLOGY.print(DEF) + "\",");
        increaseIndent();
        //indentedOutput("\"rdf:about\" : \""+u+"\","+NL);
        indentedOutput("\"@id\" : \"" + u + "\"," + NL);
        if (f != null) {
            indentedOutput("\"" + SyntaxElement.LOCATION.print(DEF) + "\" : \"" + f + "\"");
        } else {
            indentedOutput("\"" + SyntaxElement.LOCATION.print(DEF) + "\" : \"" + u + "\"");
        }
        if ( onto.getFormalism() != null ) {
            outputln(",");
            indentedOutputln("\"" + SyntaxElement.FORMATT.print(DEF) + "\" : ");
            increaseIndent();
            indentedOutputln("{ \"@type\" : \"" + SyntaxElement.FORMALISM.print(DEF) + "\",");
            increaseIndent();
            indentedOutputln("\"" + SyntaxElement.NAME.print(DEF) + "\" : \"" + onto.getFormalism() + "\",");
            indentedOutputln("\"" + SyntaxElement.URI.print() + "\" : \"" + onto.getFormURI() + "\"");
            decreaseIndent();
            indentedOutputln("}");
            decreaseIndent();
        }
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(Cell cell) throws AlignmentException {
        if (subsumedInvocableMethod(this, cell, Cell.class)) {
            return;
        }
        // default behaviour
        this.cell = cell;
        URI u1 = cell.getObject1AsURI(alignment);
        URI u2 = cell.getObject2AsURI(alignment);
        if ((u1 != null && u2 != null)
                || alignment.getLevel().startsWith("2EDOAL")) { //expensive test
            indentedOutputln("{ \"@type\" : \"" + SyntaxElement.CELL.print(DEF) + "\",");
            increaseIndent();
            if (cell.getId() != null && !cell.getId().equals("")) {
                //indentedOutputln("\"rdf:about\" : \""+cell.getId()+"\",");
                indentedOutputln("\"@id\" : \"" + cell.getId() + "\",");
            }
            if ( cell instanceof BasicCell ) printExtensions( (Extensible)cell );
            if (alignment.getLevel().startsWith("2EDOAL")) {
                indentedOutputln("\"" + SyntaxElement.ENTITY1.print(DEF) + "\" : ");
                increaseIndent();
                ((Expression) (cell.getObject1())).accept(this);
                decreaseIndent();
                outputln(",");
                indentedOutputln("\"" + SyntaxElement.ENTITY2.print(DEF) + "\" : ");
                increaseIndent();
                ((Expression) (cell.getObject2())).accept(this);
                decreaseIndent();
                outputln(",");
            } else {
                indentedOutputln("\"" + SyntaxElement.ENTITY1.print(DEF) + "\" : \"" + u1.toString() + "\",");
                indentedOutputln("\"" + SyntaxElement.ENTITY2.print(DEF) + "\" : \"" + u2.toString() + "\",");
            }
            indentedOutput("\"" + SyntaxElement.RULE_RELATION.print(DEF) + "\" : \"");
            cell.getRelation().accept(this);
            outputln("\"," );
            indentedOutput("\"" + SyntaxElement.MEASURE.print(DEF) + "\" : \"" + cell.getStrength() + "\"");
            if (cell.getSemantics() != null
                    && !cell.getSemantics().equals("")
                    && !cell.getSemantics().equals("first-order")) {
                outputln(",");
                indentedOutput("\"" + SyntaxElement.SEMANTICS.print(DEF) + "\" : \"" + cell.getSemantics() + "\"");
            }
	    if ( cell instanceof EDOALCell ) { // Here put the transf and the linkkeys
		Set<Linkkey> linkeys = ((EDOALCell)cell).linkkeys();
		if ( linkeys != null ) {
		    outputln(",");
		    indentedOutputln("\"" + SyntaxElement.LINKKEYS.print(DEF) + "\" : [");
		    increaseIndent();
		    boolean first = true;
		    for ( Linkkey linkkey : linkeys ) {
			if (first) {
			    first = false;
			} else {
			    outputln(",");
			}
			linkkey.accept(this);
		    }
		    outputln();
		    decreaseIndent();
		    indentedOutput("]");
		}
		Set<Transformation> transfs = ((EDOALCell) cell).transformations();
		if (transfs != null) {
		    outputln(",");
		    indentedOutputln("\"" + SyntaxElement.TRANSFORMATION.print(DEF) + "\" : [");
		    increaseIndent();
		    for ( Transformation transf : transfs ) {
			transf.accept(this);
		    }
		    outputln();
		    decreaseIndent();
		    indentedOutput("]");
		}
	    }
            decreaseIndent();
            outputln();
            indentedOutput("}");
        }
    }

    // DONE: could also be a qualified class name
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
            outputln(",");
            indentedOutputln("\"" + SyntaxElement.VAR.print(DEF) + "\" : \"" + expr.getVariable().name() + "\"");
        }
    }

    public void visit(final ClassId e) throws AlignmentException {
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.CLASS_EXPR.print(DEF) + "\"");
        increaseIndent();
        if (e.getURI() != null) {
            //indentedOutput("\rdf:about\" : \""+u+"\","+NL);
            outputln(",");
            indentedOutput("\"@id\" : \"" + e.getURI() + "\"");
        }
        if (isPattern) {
            renderVariables(e);
        }
        decreaseIndent();
        outputln();
        indentedOutput("}");
    }

    public void visit(final ClassConstruction e) throws AlignmentException {
        final Constructor op = e.getOperator();
        String sop = SyntaxElement.getElement(op).print(DEF);
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.CLASS_EXPR.print(DEF) + "\"");
        increaseIndent();
        if (isPattern) {
            renderVariables(e);
        }
        outputln(",");
        indentedOutput("\"" + sop + "\" : [" + NL);
        increaseIndent();
        boolean first = true;
        for (final ClassExpression ce : e.getComponents()) {
            if (first) {
                first = false;
            } else {
                outputln(",");
            }
            ce.accept(this);
        }
        outputln();
        decreaseIndent();
        indentedOutputln("]");
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final ClassValueRestriction c) throws AlignmentException {
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.VALUE_COND.print(DEF) + "\"");
        increaseIndent();
        if (isPattern) {
            renderVariables(c);
        }
        outputln(",");
        indentedOutputln("\"" + SyntaxElement.ONPROPERTY.print(DEF) + "\" : ");
        increaseIndent();
        c.getRestrictionPath().accept(this);
        decreaseIndent();
        outputln(",");
        indentedOutput("\"" + SyntaxElement.COMPARATOR.print(DEF) + "\" : \"" + c.getComparator().getURI() + "\"," + NL);
        indentedOutput("\"" + SyntaxElement.VALUE.print(DEF) + "\" : " + NL);
        increaseIndent();
        c.getValue().accept(this);
        outputln();
        decreaseIndent();
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final ClassTypeRestriction c) throws AlignmentException {
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.TYPE_COND.print(DEF) + "\"");
        increaseIndent();
        if (isPattern) {
            renderVariables(c);
        }
        outputln(",");
        indentedOutputln("\"" + SyntaxElement.ONPROPERTY.print(DEF) + "\" : ");
        increaseIndent();
        c.getRestrictionPath().accept(this);
        outputln(",");
        c.getType().accept(this);
        outputln();
        decreaseIndent();
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final ClassDomainRestriction c) throws AlignmentException {
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.DOMAIN_RESTRICTION.print(DEF) + "\"");
        increaseIndent();
        if (isPattern) {
            renderVariables(c);
        }
        outputln(",");
        indentedOutputln("\"" + SyntaxElement.ONPROPERTY.print(DEF) + "\" : ");
        increaseIndent();
        c.getRestrictionPath().accept(this);
        decreaseIndent();
        outputln(",");
        if (c.isUniversal()) {
            indentedOutput("\"" + SyntaxElement.ALL.print(DEF) + "\" : " + NL);
        } else {
            indentedOutput("\"" + SyntaxElement.EXISTS.print(DEF) + "\" : " + NL);
        }
        increaseIndent();
        c.getDomain().accept(this);
        outputln();
        decreaseIndent();
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final ClassOccurenceRestriction c) throws AlignmentException {
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.OCCURENCE_COND.print(DEF) + "\"");
        increaseIndent();
        if (isPattern) {
            renderVariables(c);
        }
        outputln(",");
        indentedOutputln("\"" + SyntaxElement.ONPROPERTY.print(DEF) + "\" : ");
        increaseIndent();
        c.getRestrictionPath().accept(this);
        decreaseIndent();
        outputln(",");
        indentedOutput("\"" + SyntaxElement.COMPARATOR.print(DEF) + "\" : \"" + c.getComparator().getURI() + "\"," + NL);
        indentedOutput("\"" + SyntaxElement.VALUE.print(DEF) + "\" : " + NL);
        increaseIndent();
        indentedOutputln("{ \"@type\" : \"" + SyntaxElement.LITERAL.print(DEF) + "\",");
        increaseIndent();
        indentedOutputln("\"" + SyntaxElement.ETYPE.print(DEF) + "\" : \"xsd:int\",");
        indentedOutputln("\"" + SyntaxElement.STRING.print(DEF) + "\" : \"" + c.getOccurence() + "\"");
        decreaseIndent();
        indentedOutputln("}");
        decreaseIndent();
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final PropertyId e) throws AlignmentException {
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.PROPERTY_EXPR.print(DEF) + "\"");
        increaseIndent();
        if (e.getURI() != null) {
            //indentedOutput("\rdf:about\" : \""+u+"\","+NL);
            outputln(",");
            indentedOutput("\"@id\" : \"" + e.getURI() + "\"");
        }
        if (isPattern) {
            renderVariables(e);
        }
        outputln();
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final PropertyConstruction e) throws AlignmentException {
        final Constructor op = e.getOperator();
        String sop = SyntaxElement.getElement(op).print(DEF);
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.PROPERTY_EXPR.print(DEF) + "\"");
        increaseIndent();
        if (isPattern) {
            renderVariables(e);
        }
        outputln(",");
        indentedOutput("\"" + sop + "\" : [" + NL);
        increaseIndent();
        boolean first = true;
        for (final PathExpression pe : e.getComponents()) {
            if (first) {
                first = false;
            } else {
                outputln(",");
            }
	    output(linePrefix);
            pe.accept(this);
        }
        outputln();
        decreaseIndent();
        indentedOutputln("]");
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final PropertyValueRestriction c) throws AlignmentException {
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.PROPERTY_VALUE_COND.print(DEF) + "\"");
        increaseIndent();
        if (isPattern) {
            renderVariables(c);
        }
        outputln(",");
        indentedOutput("\"" + SyntaxElement.COMPARATOR.print(DEF) + "\" : \"" + c.getComparator().getURI() + "\"," + NL);
        indentedOutput("\"" + SyntaxElement.VALUE.print(DEF) + "\" : " + NL);
        increaseIndent();
        c.getValue().accept(this);
        outputln();
        decreaseIndent();
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final PropertyDomainRestriction c) throws AlignmentException {
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.PROPERTY_DOMAIN_COND.print(DEF) + "\"");
        increaseIndent();
        if (isPattern) {
            renderVariables(c);
        }
        outputln(",");
        indentedOutput("\"" + SyntaxElement.TOCLASS.print(DEF) + "\" : " + NL);
        increaseIndent();
        c.getDomain().accept(this);
        outputln();
        decreaseIndent();
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final PropertyTypeRestriction c) throws AlignmentException {
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.PROPERTY_TYPE_COND.print(DEF) + "\"");
        increaseIndent();
        if (isPattern) {
            renderVariables(c);
        }
        outputln(",");
        c.getType().accept(this);
        outputln();
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final RelationId e) throws AlignmentException {
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.RELATION_EXPR.print(DEF) + "\"");
        increaseIndent();
        if (e.getURI() != null) {
            outputln(",");
            //indentedOutput("\rdf:about\" : \""+u+"\"");
            indentedOutput("\"@id\" : \"" + e.getURI() + "\"");
        }
        if (isPattern) {
            renderVariables(e);
        }
        outputln();
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final RelationConstruction e) throws AlignmentException {
        final Constructor op = e.getOperator();
        String sop = SyntaxElement.getElement(op).print(DEF);
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.RELATION_EXPR.print(DEF) + "\"");
        increaseIndent();
        if (isPattern) {
            renderVariables(e);
        }
        outputln(",");
        indentedOutput("\"" + sop + "\" : [" + NL);
        increaseIndent();
        boolean first = true;
        for (final PathExpression re : e.getComponents()) {
            if (first) {
                first = false;
            } else {
                outputln(",");
            }
            re.accept(this);
        }
        outputln();
        decreaseIndent();
        indentedOutputln("]");
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final RelationCoDomainRestriction c) throws AlignmentException {
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.RELATION_CODOMAIN_COND.print(DEF) + "\"");
        increaseIndent();
        if (isPattern) {
            renderVariables(c);
        }
        outputln(",");
        indentedOutput("\"" + SyntaxElement.TOCLASS.print(DEF) + "\" : " + NL);
        increaseIndent();
        c.getCoDomain().accept(this);
        outputln();
        decreaseIndent();
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final RelationDomainRestriction c) throws AlignmentException {
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.RELATION_DOMAIN_COND.print(DEF) + "\"");
        increaseIndent();
        if (isPattern) {
            renderVariables(c);
        }
        outputln(",");
        indentedOutput("\"" + SyntaxElement.TOCLASS.print(DEF) + "\" : " + NL);
        increaseIndent();
        c.getDomain().accept(this);
        outputln();
        decreaseIndent();
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final InstanceId e) throws AlignmentException {
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.INSTANCE_EXPR.print(DEF) + "\"");
        increaseIndent();
        if (e.getURI() != null) {
            //indentedOutput("\rdf:about\" : \""+u+"\"");
            outputln(",");
            indentedOutput("\"@id\" : \"" + e.getURI() + "\"");
        }
        if (isPattern) {
            renderVariables(e);
        }
        outputln();
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final Value e) throws AlignmentException {
        indentedOutput("{ \"@type\" : \"" + SyntaxElement.LITERAL.print(DEF) + "\"");
        increaseIndent();
        if (e.getType() != null) {
            outputln(",");
            indentedOutput("\"" + SyntaxElement.ETYPE.print(DEF) + "\" : \"" + e.getType() + "\"");
        }
        outputln(",");
        indentedOutputln("\"" + SyntaxElement.STRING.print(DEF) + "\" : \"" + e.getValue() + "\"");
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final Apply e) throws AlignmentException {
        indentedOutputln("{ \"@type\" : \"" + SyntaxElement.APPLY.print(DEF) + "\",");
        increaseIndent();
        indentedOutputln("\"" + SyntaxElement.OPERATOR.print(DEF) + "\" : \"" + e.getOperation() + "\",");
        indentedOutputln("\"" + SyntaxElement.ARGUMENTS.print(DEF) + "\" : [");
        increaseIndent();
        boolean first = true;
        for (final ValueExpression ve : e.getArguments()) {
            if (first) {
                first = false;
            } else {
                outputln(",");
            }
            ve.accept(this);
        }
        outputln();
        decreaseIndent();
        indentedOutputln("]");
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final Aggregate e) throws AlignmentException {
        indentedOutputln("{ \"@type\" : \"" + SyntaxElement.AGGREGATE.print(DEF) + "\",");
        increaseIndent();
        indentedOutputln("\"" + SyntaxElement.OPERATOR.print(DEF) + "\" : \"" + e.getOperation() + "\",");
        indentedOutputln("\"" + SyntaxElement.ARGUMENTS.print(DEF) + "\" : [");
        increaseIndent();
        boolean first = true;
        for (final ValueExpression ve : e.getArguments()) {
            if (first) {
                first = false;
            } else {
                outputln(",");
            }
            ve.accept(this);
        }
        outputln();
        decreaseIndent();
        indentedOutputln("]");
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final Transformation transf) throws AlignmentException {
        indentedOutputln("{ \"@type\" : \"" + SyntaxElement.TRANSF.print(DEF) + "\",");
        increaseIndent();
        indentedOutputln("\"" + SyntaxElement.TRDIR.print(DEF) + "\" : \"" + transf.getType() + "\",");
        indentedOutputln("\"" + SyntaxElement.TRENT1.print(DEF) + "\" : ");
        increaseIndent();
        transf.getObject1().accept(this);
        decreaseIndent();
        outputln(",");
        indentedOutputln("\"" + SyntaxElement.TRENT2.print(DEF) + "\" : ");
        increaseIndent();
        transf.getObject2().accept(this);
        decreaseIndent();
        outputln();
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final Datatype e) throws AlignmentException {
        indentedOutputln("\"" + SyntaxElement.EDATATYPE.print(DEF) + "\" : ");
        increaseIndent();
        indentedOutputln("{ \"@type\" : \"" + SyntaxElement.DATATYPE.print(DEF) + "\",");
        increaseIndent();
        indentedOutput("\"@id\" : \"" + e.getType() + "\" }");
        decreaseIndent();
        decreaseIndent();
    }

    public void visit(final Linkkey linkkey) throws AlignmentException {
        indentedOutputln("{ \"@type\" : \"" + SyntaxElement.LINKKEY.print(DEF) + "\",");
        increaseIndent();
	printExtensions( linkkey );
        indentedOutputln("\"" + SyntaxElement.LINKKEY_BINDING.print(DEF) + "\" : [");
        increaseIndent();
        boolean first = true;
        for (LinkkeyBinding linkkeyBinding : linkkey.bindings()) {
            if (first) {
                first = false;
            } else {
                outputln(",");
            }
            increaseIndent();
            linkkeyBinding.accept(this);
            decreaseIndent();
        }
        outputln();
        decreaseIndent();
        indentedOutputln("]");
        decreaseIndent();
        indentedOutput("}");
    }

    private void visitLinkKeyBinding(LinkkeyBinding linkkeyBinding, SyntaxElement syntaxElement) throws AlignmentException {
        indentedOutputln("{ \"@type\" : \"" + syntaxElement.print(DEF) + "\",");
        increaseIndent();
        indentedOutputln("\"" + SyntaxElement.LINKEY_PROPERTY1.print(DEF) + "\" :");
        increaseIndent();
        linkkeyBinding.getExpression1().accept(this);
        outputln(",");
        decreaseIndent();
        indentedOutputln("\"" + SyntaxElement.LINKEY_PROPERTY2.print(DEF) + "\" :");
        increaseIndent();
        linkkeyBinding.getExpression2().accept(this);
        decreaseIndent();
        outputln();
        decreaseIndent();
        indentedOutput("}");
    }

    public void visit(final LinkkeyEquals linkkeyEquals) throws AlignmentException {
        visitLinkKeyBinding(linkkeyEquals, SyntaxElement.LINKEY_EQUALS);
    }

    public void visit(final LinkkeyIntersects linkkeyIntersects) throws AlignmentException {
        visitLinkKeyBinding(linkkeyIntersects, SyntaxElement.LINKEY_INTERSECTS);
    }

    protected void printExtensions( final Extensible extent ) {
	if ( extent.getExtensions() != null ) {
	    for ( String[] ext : extent.getExtensions() ) {
		String uri = ext[0];
		String tag = (nslist==null)?null:nslist.get(uri);
		if (tag == null) {
		    indentedOutputln( "\""+uri+":"+ext[1]+"\" : \"" + ext[2] + "\",");
		} else {
		    indentedOutputln( "\""+tag+":"+ext[1]+"\" : \"" + ext[2] + "\",");
		}
	    }
	}
    }

}
