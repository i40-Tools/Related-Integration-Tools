/*
 * $Id: OWLAxiomsRendererVisitor.java 2116 2016-09-19 08:38:32Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2004, 2007-2016
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

import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.impl.Extensions;
import fr.inrialpes.exmo.align.impl.ObjectAlignment;
import fr.inrialpes.exmo.align.impl.URIAlignment;

import fr.inrialpes.exmo.ontowrap.LoadedOntology;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

import fr.inrialpes.exmo.align.parser.SyntaxElement;
import fr.inrialpes.exmo.align.parser.SyntaxElement.Constructor;

import fr.inrialpes.exmo.align.impl.edoal.Id;
import fr.inrialpes.exmo.align.impl.edoal.PathExpression;
import fr.inrialpes.exmo.align.impl.edoal.Expression;
import fr.inrialpes.exmo.align.impl.edoal.ClassExpression;
import fr.inrialpes.exmo.align.impl.edoal.ClassId;
import fr.inrialpes.exmo.align.impl.edoal.ClassConstruction;
import fr.inrialpes.exmo.align.impl.edoal.ClassTypeRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassValueRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassOccurenceRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyExpression;
import fr.inrialpes.exmo.align.impl.edoal.PropertyId;
import fr.inrialpes.exmo.align.impl.edoal.PropertyConstruction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyTypeRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyValueRestriction;
import fr.inrialpes.exmo.align.impl.edoal.RelationExpression;
import fr.inrialpes.exmo.align.impl.edoal.RelationId;
import fr.inrialpes.exmo.align.impl.edoal.RelationConstruction;
import fr.inrialpes.exmo.align.impl.edoal.RelationDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.RelationCoDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.InstanceExpression;
import fr.inrialpes.exmo.align.impl.edoal.InstanceId;

import fr.inrialpes.exmo.align.impl.edoal.Transformation;
import fr.inrialpes.exmo.align.impl.edoal.ValueExpression;
import fr.inrialpes.exmo.align.impl.edoal.Value;
import fr.inrialpes.exmo.align.impl.edoal.Apply;
import fr.inrialpes.exmo.align.impl.edoal.Aggregate;
import fr.inrialpes.exmo.align.impl.edoal.Datatype;
import fr.inrialpes.exmo.align.impl.edoal.Comparator;
import fr.inrialpes.exmo.align.impl.edoal.EDOALCell;
import fr.inrialpes.exmo.align.impl.edoal.EDOALAlignment;
import fr.inrialpes.exmo.align.impl.edoal.EDOALVisitor;
import fr.inrialpes.exmo.align.impl.edoal.Linkkey;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyBinding;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyEquals;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyIntersects;

/**
 * Renders an alignment as a new ontology merging these.
 *
 * @author Jérôme Euzenat
 * @version $Id: OWLAxiomsRendererVisitor.java 2116 2016-09-19 08:38:32Z euzenat $ 
 */

public class OWLAxiomsRendererVisitor extends IndentedRendererVisitor implements AlignmentVisitor, EDOALVisitor {
    final static Logger logger = LoggerFactory.getLogger( OWLAxiomsRendererVisitor.class );

    boolean heterogeneous = false;
    boolean edoal = false;
    Alignment alignment = null;
    LoadedOntology<? extends Object> onto1 = null;
    LoadedOntology<? extends Object> onto2 = null;
    Cell cell = null;
    Relation toProcess = null;

    private static Namespace DEF = Namespace.ALIGNMENT;
    
    public OWLAxiomsRendererVisitor( PrintWriter writer ){
	super( writer );
    }

    public void init( Properties p ) {
	super.init( p );
	if ( p.getProperty("heterogeneous") != null ) heterogeneous = true;
    };

    public void visit( Alignment align ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, align, Alignment.class ) ) return;
	// default behaviour
	//logger.trace( "Alignment: {}", align );
	if ( align instanceof ObjectAlignment ) {
	    alignment = align;
	    onto1 = ((ObjectAlignment)alignment).getOntologyObject1();
	    onto2 = ((ObjectAlignment)alignment).getOntologyObject2();
	} else if ( align instanceof EDOALAlignment ) {
	    alignment = align;
	    edoal = true;
	} else {
	    try {
		alignment = ObjectAlignment.toObjectAlignment( (URIAlignment)align );
		onto1 = ((ObjectAlignment)alignment).getOntologyObject1();
		onto2 = ((ObjectAlignment)alignment).getOntologyObject2();
	    } catch ( AlignmentException alex ) {
		logger.debug( "Cannot convert to ObjectAlignment", alex );
		throw new AlignmentException("OWLAxiomsRenderer: cannot render simple alignment. Need an ObjectAlignment", alex );
	    }
	}
	indentedOutputln("<rdf:RDF");
	increaseIndent();
	increaseIndent();
	indentedOutputln("xmlns:owl=\"http://www.w3.org/2002/07/owl#\"");
	indentedOutputln("xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
	indentedOutputln("xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" ");
	indentedOutputln("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\">");
	outputln();
	decreaseIndent();
	indentedOutputln("<owl:Ontology rdf:about=\"\">");
	increaseIndent();
	indentedOutputln("<rdfs:comment>Matched ontologies</rdfs:comment>");
	indentedOutputln("<rdfs:comment>Generated by fr.inrialpes.exmo.align.renderer.OWLAxiomsRendererVisitor</rdfs:comment>");
	for ( String[] ext : align.getExtensions() ){
	    indentedOutputln("<rdfs:comment>"+ext[1]+": "+ext[2]+"</rdfs:comment>");
	}
	indentedOutputln("<owl:imports rdf:resource=\""+align.getOntology1URI().toString()+"\"/>");
	indentedOutputln("<owl:imports rdf:resource=\""+align.getOntology2URI().toString()+"\"/>");
	decreaseIndent();
	indentedOutputln("</owl:Ontology>");
	outputln();
	decreaseIndent();
	
	try {
	    logger.debug( "Alignment with {} cells", alignment.nbCells() );
	    for( Cell c : alignment ){
		Object ob1 = c.getObject1();
		Object ob2 = c.getObject2();
		//logger.trace( "Rendering {} -- {}", ob1, ob2 );
		//logger.trace( "OB1: {}, OB2: {}", ob1.getClass(), ob2.getClass() );
		if ( heterogeneous || edoal ||
		     ( onto1.isClass( ob1 ) && onto2.isClass( ob2 ) ) ||
		     ( onto1.isDataProperty( ob1 ) && onto2.isDataProperty( ob2 ) ) ||
		     ( onto1.isObjectProperty( ob1 ) && onto2.isObjectProperty( ob2 ) ) ||
		     ( onto1.isIndividual( ob1 ) && onto2.isIndividual( ob2 ) ) ) {
		    c.accept( this );
		}
	    } //end for
	} catch ( OntowrapException owex ) {
	    throw new AlignmentException( "Error accessing ontology", owex );
	}
	//logger.trace( "Heterogeneous: {}", heterogeneous);
	//logger.trace( "EDOAL: {}", edoal);
	//logger.trace( "onto1: {}", onto1);
	decreaseIndent();
	indentedOutputln("</rdf:RDF>");
    }

    public void visit( Cell cell ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, cell, Cell.class ) ) return;
	// default behaviour
	if ( cell.getId() != null ) {
	    outputln();
	    outputln();
	    indentedOutputln("<!-- "+cell.getId()+" -->");
	}
	if ( cell instanceof EDOALCell ) {
	    cell.accept( this );
	} else {
	    this.cell = cell;
	    Object ob1 = cell.getObject1();
	    Object ob2 = cell.getObject2();
	    //logger.trace( " {} <-> {} ", ob1,  ob2 );
	    URI u1;
	    increaseIndent();
	    try {
		Relation rel = cell.getRelation();
		if ( RelationTransformer.canBeTranscribedInverted( rel ) ){
		    u1 = onto2.getEntityURI( ob2 );
		} else {
		    u1 = onto1.getEntityURI( ob1 );
		}
		if ( ob1 instanceof ClassExpression || onto1.isClass( ob1 ) ) {
		    indentedOutputln("<owl:Class rdf:about=\""+u1+"\">");
		    increaseIndent();
		    rel.accept( this );
		    decreaseIndent();
		    indentedOutputln("</owl:Class>");
		} else if ( ob1 instanceof PropertyExpression || onto1.isDataProperty( ob1 ) ) {
		    indentedOutputln("<owl:DatatypeProperty rdf:about=\""+u1+"\">");
		    increaseIndent();
		    rel.accept( this );
		    decreaseIndent();
		    indentedOutputln("</owl:DatatypeProperty>");
		} else if ( ob1 instanceof RelationExpression || onto1.isObjectProperty( ob1 ) ) {
		    indentedOutputln("<owl:ObjectProperty rdf:about=\""+u1+"\">");
		    increaseIndent();
		    rel.accept( this );
		    decreaseIndent();
		    indentedOutputln("</owl:ObjectProperty>");
		} else if ( ob1 instanceof InstanceExpression || onto1.isIndividual( ob1 ) ) {
		    indentedOutputln("<owl:Thing rdf:about=\""+u1+"\">");
		    increaseIndent();
		    rel.accept( this );
		    decreaseIndent();
		    indentedOutputln("</owl:Thing>");
		}
	    } catch ( OntowrapException owex ) {
		throw new AlignmentException( "Error accessing ontology", owex );
	    }
	    decreaseIndent();
	}
    }

    public void visit( EDOALCell cell ) throws AlignmentException {
	this.cell = cell;
	toProcess = cell.getRelation();
	increaseIndent();
	if ( RelationTransformer.canBeTranscribedInverted( toProcess ) ) {
	    ((Expression)cell.getObject2()).accept( this );
	} else {
	    ((Expression)cell.getObject1()).accept( this );
	}
	decreaseIndent();
	outputln();
    }
    // Classical dispatch
    // This is the previous code... which is the one which was used.
    // It should be reintroduced in the dispatch!
    public void visit( Relation rel ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, rel, Relation.class ) ) return;
	// default behaviour
	Object ob2 = cell.getObject2();
	if ( edoal ) {
	    String owlrel = getRelationName( rel, ob2 );
	    if ( owlrel == null ) throw new AlignmentException( "Relation "+rel+" cannot apply to "+ob2 );
	    indentedOutputln("<"+owlrel+">");
	    increaseIndent();
	    if ( RelationTransformer.canBeTranscribedInverted( rel ) ) {
		((Expression)cell.getObject1()).accept( this );
	    } else {
		((Expression)ob2).accept( this );
	    }
	    decreaseIndent();
	    outputln();
	    indentedOutput("</"+owlrel+">");
	} else {
	    String owlrel = getRelationName( onto2, rel, ob2 );
	    if ( owlrel == null ) throw new AlignmentException( "Cannot express relation "+rel );
	    try {
		indentedOutputln("<"+owlrel+" rdf:resource=\""+onto2.getEntityURI( ob2 )+"\"/>");
		//indentedOutputln("    <"+owlrel+" rdf:resource=\""+onto2.getEntityURI( ob2 )+"\"/>");
	    } catch ( OntowrapException owex ) {
		throw new AlignmentException( "Error accessing ontology", owex );
	    }
	}
    }

    public void printRel( Object ob, LoadedOntology<? extends Object> onto, Relation rel ) throws AlignmentException {
	if ( !edoal ) {
	    String owlrel = getRelationName( onto, rel, ob );
	    if ( owlrel == null ) throw new AlignmentException( "Cannot express relation "+rel );
	    try {
		outputln("    <"+owlrel+" rdf:resource=\""+onto.getEntityURI( ob )+"\"/>");
	    } catch ( OntowrapException owex ) {
		throw new AlignmentException( "Error accessing ontology", owex );
	    }
	} else {
	    String owlrel = getRelationName( rel, ob );
	    if ( owlrel == null ) throw new AlignmentException( "Cannot express relation "+rel );
	    if ( ob instanceof InstanceId ) {
		indentedOutput("<"+owlrel+" rdf:resource=\""+((InstanceId)ob).getURI()+"\"/>");
	    } else {
		indentedOutput("<"+owlrel+">");
		outputln();
		increaseIndent();
		((Expression)ob).accept( this ); // ?? no cast
		decreaseIndent();
		outputln();
		indentedOutput("</"+owlrel+">");
	    }
	}
    }

    /**
     * For EDOAL relation name depends on type of expressions
     * 
     * @param rel: the relation to render
     * @param ob: an object on which this relation applies
     * @return the rendered relation
     */
    public String getRelationName( Relation rel, Object ob ) {
	if ( RelationTransformer.isEquivalence( rel ) ) {
	    if ( ob instanceof ClassExpression ) {
		return "owl:equivalentClass";
	    } else if ( ob instanceof PropertyExpression || ob instanceof RelationExpression ) {
		return "owl:equivalentProperty";
	    } else if ( ob instanceof InstanceExpression ) {
		return "owl:sameAs";
	    }
	} else if ( RelationTransformer.isDisjoint( rel ) ) {
	    if ( ob instanceof ClassExpression ) {
		return "owl:disjointFrom";
	    } else if ( ob instanceof InstanceExpression ) {
		return "owl:differentFrom";
	    }
	} else if ( RelationTransformer.subsumesOrEqual( rel ) ) {
	    if ( ob instanceof ClassExpression ) {
		return "owl:subClassOf";
	    } else if ( ob instanceof PropertyExpression || ob instanceof RelationExpression ) {
		return "owl:subPropertyOf";
	    }
	} else if ( RelationTransformer.isSubsumedOrEqual( rel ) ) {
	    if ( ob instanceof ClassExpression ) {
		return "owl:subClassOf";
	    } else if ( ob instanceof PropertyExpression || ob instanceof RelationExpression ) {
		return "owl:subPropertyOf";
	    }
	} else if ( RelationTransformer.isInstanceOf( rel ) ) {
	    if ( ob instanceof ClassExpression ) {
		return "rdf:type";
	    }
	} else if ( RelationTransformer.hasInstance( rel ) ) {
	    if ( ob instanceof InstanceExpression ) {
		return "rdf:type";
	    }
	}
	return null;
    }

    /**
     * Regular: relation name depends on loaded ontology
     *
     * @param onto: the ontology in which belongs ob
     * @param rel: a relation to render
     * @param ob: an object affected by this relation
     * @return the rendered relation
     */
    public String getRelationName( LoadedOntology<? extends Object> onto, Relation rel, Object ob ) {
	try {
	    if ( RelationTransformer.isEquivalence( rel ) ) {
		if ( onto.isClass( ob ) ) {
		    return "owl:equivalentClass";
		} else if ( onto.isProperty( ob ) ) {
		    return "owl:equivalentProperty";
		} else if ( onto.isIndividual( ob ) ) {
		    return "owl:sameAs";
		}
	    } else if ( RelationTransformer.subsumesOrEqual( rel ) ) {
		if ( onto.isClass( ob ) ) {
		    return "rdfs:subClassOf";
		} else if ( onto.isProperty( ob ) ) {
		    return "rdfs:subPropertyOf";
		}
	    } else if ( RelationTransformer.isSubsumedOrEqual( rel ) ) {
		if ( onto.isClass( ob ) ) {
		    return "rdfs:subClassOf";
		} else if ( onto.isProperty( ob ) ) {
		    return "rdfs:subPropertyOf";
		}
	    } else if ( RelationTransformer.isDisjoint( rel ) ) {
		if ( onto.isClass( ob ) ) {
		    return "rdfs:disjointFrom";
		} else if ( onto.isIndividual( ob ) ) {
		    return "owl:differentFrom";
		}
	    } else if ( RelationTransformer.isInstanceOf( rel ) ) {
		if ( onto.isClass( ob ) ) {
		    return "rdf:type";
		}
	    } else if ( RelationTransformer.hasInstance( rel ) ) {
		if ( onto.isIndividual( ob ) ) {
		    return "rdf:type";
		}
	    }
	} catch ( OntowrapException owex ) {}; // return null anyway
	return null;
    }

    // ******* EDOAL

    public void visit( final ClassId e ) throws AlignmentException {
	if ( toProcess == null ) {
	    indentedOutput("<owl:Class "+SyntaxElement.RDF_ABOUT.print(DEF)+"=\""+e.getURI()+"\"/>");
	} else {
	    Relation toProcessNext = toProcess;
	    toProcess = null;
	    indentedOutputln("<owl:Class "+SyntaxElement.RDF_ABOUT.print(DEF)+"=\""+e.getURI()+"\">");
	    increaseIndent();
	    toProcessNext.accept( this );
	    outputln();
	    decreaseIndent();
	    indentedOutput("</owl:Class>");
	}
    }

    public void visit( final ClassConstruction e ) throws AlignmentException {
	Relation toProcessNext = toProcess;
	toProcess = null;
	final Constructor op = e.getOperator();
	String owlop = null;
	// Very special treatment
	if ( toProcessNext != null && e.getComponents().size() == 0 ) {
	    if ( op == Constructor.AND ) owlop = "http://www.w3.org/2002/07/owl#Thing";
	    else if ( op == Constructor.OR ) owlop = "http://www.w3.org/2002/07/owl#Nothing";
	    else if ( op == Constructor.NOT ) throw new AlignmentException( "Complement constructor cannot be empty");
	    indentedOutputln("<owl:Class "+SyntaxElement.RDF_ABOUT.print(DEF)+"=\""+owlop+"\">");
	    increaseIndent();
	    toProcessNext.accept( this ); 
	    outputln();
	    decreaseIndent();
	    indentedOutput("</owl:Class>");
	} else {
	    if ( op == Constructor.AND ) owlop = "intersectionOf";
	    else if ( op == Constructor.OR ) owlop = "unionOf";
	    else if ( op == Constructor.NOT ) owlop = "complementOf";
	    else throw new AlignmentException( "Unknown class constructor : "+op );
	    if ( e.getComponents().size() == 0 ) {
		if ( op == Constructor.AND ) indentedOutput("<owl:Thing/>");
		else if ( op == Constructor.OR ) indentedOutput("<owl:Nothing/>");
		else throw new AlignmentException( "Complement constructor cannot be empty");
	    } else {
		indentedOutputln("<owl:Class>");
		increaseIndent();
		indentedOutput("<owl:"+owlop);
		if ( ( (op == Constructor.AND) || (op == Constructor.OR) ) ) 
		    output(" "+SyntaxElement.RDF_PARSETYPE.print(DEF)+"=\"Collection\"");
		outputln(">");
		increaseIndent();
		for (final ClassExpression ce : e.getComponents()) {
		    output(linePrefix);
		    ce.accept( this );
		    outputln();
		}
		decreaseIndent();
		indentedOutputln("</owl:"+owlop+">");
		if ( toProcessNext != null ) { toProcessNext.accept( this ); outputln(); }
		decreaseIndent();
		indentedOutput("</owl:Class>");
	    }
	}
    }

    public void visit( final ClassValueRestriction c ) throws AlignmentException {
	Relation toProcessNext = toProcess;
	toProcess = null;
	indentedOutputln("<owl:Restriction>");
	increaseIndent();
	indentedOutputln("<owl:onProperty>");
	increaseIndent();
	c.getRestrictionPath().accept( this );
	decreaseIndent();
	outputln();
	indentedOutputln("</owl:onProperty>");
	ValueExpression ve = c.getValue();
	if ( ve instanceof Value ) {
	    indentedOutput("<owl:hasValue");
	    if ( ((Value)ve).getType() != null ) {
		output( " rdf:datatype=\""+((Value)ve).getType()+"\"" );
	    }
	    outputln( ">"+((Value)ve).getValue()+"</owl:hasValue>");
	} else if ( ve instanceof InstanceId ) {
	    indentedOutputln("<owl:hasValue>");
	    increaseIndent();
	    ve.accept( this );
	    decreaseIndent();
	    outputln();
	    indentedOutputln("</owl:hasValue>");
	} else throw new AlignmentException( "OWL does not support path constraints in hasValue : "+ve );
	if ( toProcessNext != null ) { toProcessNext.accept( this ); outputln(); }
	decreaseIndent();
	indentedOutput("</owl:Restriction>");
    }

    public void visit( final ClassTypeRestriction c ) throws AlignmentException {
	Relation toProcessNext = toProcess;
	toProcess = null;
	indentedOutputln("<owl:Restriction>");
	increaseIndent();
	indentedOutputln("<owl:onProperty>");
	increaseIndent();
	c.getRestrictionPath().accept( this );
	outputln();
	decreaseIndent();
	indentedOutputln("</owl:onProperty>");
	indentedOutputln("<owl:allValuesFrom>");
	increaseIndent();
	c.getType().accept( this );
	outputln();
	decreaseIndent();
	indentedOutputln("</owl:allValuesFrom>");
	if ( toProcessNext != null ) { toProcessNext.accept( this ); outputln(); }
	decreaseIndent();
	indentedOutput("</owl:Restriction>");
    }

    public void visit( final ClassDomainRestriction c ) throws AlignmentException {
	Relation toProcessNext = toProcess;
	toProcess = null;
	indentedOutputln("<owl:Restriction>");
	increaseIndent();
	indentedOutputln("<owl:onProperty>");
	increaseIndent();
	c.getRestrictionPath().accept( this );
	outputln();
	decreaseIndent();
	indentedOutputln("</owl:onProperty>");
	if ( c.isUniversal() ) {
	    indentedOutputln("<owl:allValuesFrom>");
	} else {
	    indentedOutputln("<owl:someValuesFrom>");
	}
	increaseIndent();
	c.getDomain().accept( this );
	outputln();
	decreaseIndent();
	if ( c.isUniversal() ) {
	    indentedOutputln("</owl:allValuesFrom>");
	} else {
	    indentedOutputln("</owl:someValuesFrom>");
	}
	if ( toProcessNext != null ) { toProcessNext.accept( this ); outputln(); }
	decreaseIndent();
	indentedOutput("</owl:Restriction>");
    }

    // TOTEST
    public void visit( final ClassOccurenceRestriction c ) throws AlignmentException {
	Relation toProcessNext = toProcess;
	toProcess = null;
	indentedOutputln("<owl:Restriction>");
	increaseIndent();
	indentedOutputln("<owl:onProperty>");
	increaseIndent();
	c.getRestrictionPath().accept( this );
	outputln();
	decreaseIndent();
	indentedOutputln("</owl:onProperty>");
	String cardinality = null;
	Comparator comp = c.getComparator();
	if ( comp == Comparator.EQUAL ) cardinality = "cardinality";
	else if ( comp == Comparator.LOWER ) cardinality = "maxCardinality";
	else if ( comp == Comparator.GREATER ) cardinality = "minCardinality";
	else throw new AlignmentException( "Unknown comparator : "+comp.getURI() );
	indentedOutput("<owl:"+cardinality+" rdf:datatype=\"&xsd;nonNegativeInteger\">");
	output(""+c.getOccurence());
	outputln("</owl:"+cardinality+">");
	if ( toProcessNext != null ) { toProcessNext.accept( this ); outputln(); }
	decreaseIndent();
	indentedOutput("</owl:Restriction>");
    }
    
    public void visit(final PropertyId e) throws AlignmentException {
	if ( toProcess == null ) {
	    indentedOutput("<owl:DatatypeProperty "+SyntaxElement.RDF_ABOUT.print(DEF)+"=\""+e.getURI()+"\"/>");
	} else {
	    Relation toProcessNext = toProcess;
	    toProcess = null;
	    indentedOutputln("<owl:DatatypeProperty "+SyntaxElement.RDF_ABOUT.print(DEF)+"=\""+e.getURI()+"\">");
	    increaseIndent();
	    toProcessNext.accept( this );
	    outputln();
	    decreaseIndent();
	    indentedOutput("</owl:DatatypeProperty>");
	}
    }

    /**
     * OWL, and in particular OWL 2, does not allow for more Relation (ObjectProperty)
     * and Property (DataProperty) constructor than owl:inverseOf
     * It is thus imposible to transcribe our and, or and not constructors.
     * 
     * @param e: a compound property to render
     * @throws AlignmentException when something goes wrong
     */
    public void visit( final PropertyConstruction e ) throws AlignmentException {
	Relation toProcessNext = toProcess;
	toProcess = null;
	indentedOutputln("<owl:DatatypePropety>");
	increaseIndent();
	final Constructor op = e.getOperator();
	String owlop = null;
	if ( op == Constructor.COMP ) owlop = "propertyChainAxiom";
	// JE: FOR TESTING
	//owlop = "FORTESTING("+op.name()+")";
	if ( owlop == null ) throw new AlignmentException( "Cannot translate property construction in OWL : "+op );
	indentedOutput("<owl:"+owlop);
	if ( (op == Constructor.AND) || (op == Constructor.OR) || (op == Constructor.COMP) ) output(" "+SyntaxElement.RDF_PARSETYPE.print(DEF)+"=\"Collection\"");
	outputln(">");
	increaseIndent();
	if ( (op == Constructor.AND) || (op == Constructor.OR) || (op == Constructor.COMP) ) {
	    for ( final PathExpression pe : e.getComponents() ) {
		output(linePrefix);
		pe.accept( this );
		outputln();
	    }
	} else {
	    for (final PathExpression pe : e.getComponents()) {
		pe.accept( this );
		outputln();
	    }
	}
	decreaseIndent();
	indentedOutputln("</owl:"+owlop+">");
	if ( toProcessNext != null ) { toProcessNext.accept( this ); outputln(); }
	decreaseIndent();
	indentedOutput("</owl:DatatypePropety>");
    }
	
    public void visit(final PropertyValueRestriction c) throws AlignmentException {
	Relation toProcessNext = toProcess;
	toProcess = null;
	indentedOutputln("<owl:DatatypeProperty>");
	increaseIndent();
	indentedOutputln("<rdfs:range>");
	increaseIndent();
	indentedOutputln("<rdfs:Datatype>");
	increaseIndent();
	indentedOutputln("<owl:oneOf>");
	increaseIndent();
	// In EDOAL, this does only contain one value and is thus rendered as:
	indentedOutputln("<rdf:Description>");
	increaseIndent();
	ValueExpression ve = c.getValue();
	if ( ve instanceof Value ) {
	    indentedOutput("<rdf:first");
	    if ( ((Value)ve).getType() != null ) {
		output( " rdf:datatype=\""+((Value)ve).getType()+"\"" );
	    }
	    outputln( ">"+((Value)ve).getValue()+"</rdf:first>");
	} else {
	    indentedOutputln("<rdf:first>");
	    ve.accept( this );
	    outputln("</rdf:first>");
	    indentedOutputln("<rdf:rest rdf:resource=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#nil\"/>");
	}
	decreaseIndent();
	indentedOutputln("</rdf:Description>");
	// This is incorrect for more than one value... see the OWL:
	/*
         <rdfs:Datatype>
          <owl:oneOf>
           <rdf:Description>
            <rdf:first rdf:datatype="&xsd;integer">1</rdf:first>
             <rdf:rest>
              <rdf:Description>
               <rdf:first rdf:datatype="&xsd;integer">2</rdf:first>
               <rdf:rest rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"/>
              </rdf:Description>
             </rdf:rest>
            </rdf:Description>
           </owl:oneOf>
          </rdfs:Datatype>
	*/
	decreaseIndent();
	indentedOutputln("</owl:oneOf>");
	decreaseIndent();
	indentedOutputln("</rdfs:Datatype>");
	decreaseIndent();
	indentedOutputln("</rdfs:range>");
	if ( toProcessNext != null ) { toProcessNext.accept( this ); outputln(); }
	decreaseIndent();
	indentedOutput("</owl:DatatypeProperty>");
    }

    public void visit(final PropertyDomainRestriction c) throws AlignmentException {
	Relation toProcessNext = toProcess;
	toProcess = null;
	indentedOutputln("<owl:DatatypeProperty>");
	increaseIndent();
	indentedOutputln("<rdfs:domain>");
	increaseIndent();
	c.getDomain().accept( this );
	outputln();
	decreaseIndent();
	indentedOutputln("</rdfs:domain>");
	if ( toProcessNext != null ) { toProcessNext.accept( this ); outputln(); }
	decreaseIndent();
	indentedOutput("</owl:DatatypeProperty>");
    }

    public void visit(final PropertyTypeRestriction c) throws AlignmentException {
	Relation toProcessNext = toProcess;
	toProcess = null;
	indentedOutputln("<owl:DatatypeProperty>");
	increaseIndent();
	indentedOutputln("<rdfs:range>");
	increaseIndent();
	c.getType().accept( this );
	decreaseIndent();
	indentedOutputln("</rdfs:range>");
	if ( toProcessNext != null ) { toProcessNext.accept( this ); outputln(); }
	decreaseIndent();
	indentedOutput("</owl:DatatypeProperty>");
    }
	
    public void visit( final RelationId e ) throws AlignmentException {
	if ( toProcess == null ) {
	    indentedOutput("<owl:ObjectProperty "+SyntaxElement.RDF_ABOUT.print(DEF)+"=\""+e.getURI()+"\"/>");
	} else {
	    Relation toProcessNext = toProcess;
	    toProcess = null;
	    indentedOutputln("<owl:ObjectProperty "+SyntaxElement.RDF_ABOUT.print(DEF)+"=\""+e.getURI()+"\">");
	    increaseIndent();
	    toProcessNext.accept( this );
	    outputln();
	    decreaseIndent();
	    indentedOutput("</owl:ObjectProperty>");
	}
    }

    /**
     * OWL, and in particular OWL 2, does not allow for more Relation (ObjectProperty)
     * and Property (DataProperty) constructor than owl:inverseOf
     * It is thus imposible to transcribe our and, or and not constructors.
     * Moreover, they have no constructor for the symmetric, transitive and reflexive
     * closure and the compositional closure (or composition) can only be obtained by
     * defining a property subsumed by this closure through an axiom.
     * It is also possible to rewrite the reflexive closures as axioms as well.
     * But the transitive closure can only be obtained through subsumption.
     * 
     * @param e: a compound relation to render
     * @throws AlignmentException when something goes wrong
     */
    public void visit( final RelationConstruction e ) throws AlignmentException {
	Relation toProcessNext = toProcess;
	toProcess = null;
	indentedOutputln("<owl:ObjectProperty>");
	increaseIndent();
	final Constructor op = e.getOperator();
	String owlop = null;
	if ( op == Constructor.INVERSE ) {
	    owlop = "inverseOf";
	} else if ( op == Constructor.COMP ) {
	    owlop = "propertyChainAxiom";
	}
	// JE: FOR TESTING
	//owlop = "FORTESTING("+op.name()+")";
	if ( owlop == null ) throw new AlignmentException( "Cannot translate relation construction in OWL : "+op );
	indentedOutput("<owl:"+owlop);
	if ( (op == Constructor.OR) || (op == Constructor.AND) || (op == Constructor.COMP) ) output(" "+SyntaxElement.RDF_PARSETYPE.print(DEF)+"=\"Collection\"");
	outputln(">");
	increaseIndent();
	if ( (op == Constructor.AND) || (op == Constructor.OR) || (op == Constructor.COMP) ) {
	    for (final PathExpression re : e.getComponents()) {
		output(linePrefix);
		re.accept( this );
		outputln();
	    }
	} else { // NOT... or else: enumerate them
	    for (final PathExpression re : e.getComponents()) {
		re.accept( this );
		outputln();
	    }
	}
	decreaseIndent();
	indentedOutputln("</owl:"+owlop+">");
	if ( toProcessNext != null ) { toProcessNext.accept( this ); outputln(); }
	decreaseIndent();
	indentedOutput("</owl:ObjectProperty>");
    }
	
    public void visit(final RelationCoDomainRestriction c) throws AlignmentException {
	Relation toProcessNext = toProcess;
	toProcess = null;
	indentedOutputln("<owl:ObjectProperty>");
	increaseIndent();
	indentedOutputln("<rdfs:range>");
	increaseIndent();
	c.getCoDomain().accept( this );
	outputln();
	decreaseIndent();
	indentedOutputln("</rdfs:range>");
	if ( toProcessNext != null ) { toProcessNext.accept( this ); outputln(); }
	decreaseIndent();
	indentedOutput("</owl:ObjectProperty>");
    }

    public void visit(final RelationDomainRestriction c) throws AlignmentException {
	Relation toProcessNext = toProcess;
	toProcess = null;
	indentedOutputln("<owl:ObjectProperty>");
	increaseIndent();
	indentedOutputln("<rdfs:domain>");
	increaseIndent();
	c.getDomain().accept( this );
	outputln();
	decreaseIndent();
	indentedOutputln("</rdfs:domain>");
	if ( toProcessNext != null ) { toProcessNext.accept( this ); outputln(); }
	decreaseIndent();
	indentedOutput("</owl:ObjectProperty>");
    }

    public void visit( final InstanceId e ) throws AlignmentException {
	if ( toProcess == null ) {
	    indentedOutput("<owl:Individual "+SyntaxElement.RDF_ABOUT.print(DEF)+"=\""+e.getURI()+"\"/>");
	} else {
	    Relation toProcessNext = toProcess;
	    toProcess = null;
	    indentedOutputln("<owl:Individual "+SyntaxElement.RDF_ABOUT.print(DEF)+"=\""+e.getURI()+"\">");
	    increaseIndent();
	    toProcessNext.accept( this );
	    outputln();
	    decreaseIndent();
	    indentedOutput("</owl:Individual>");
	}
    }

    // Unused: see ClassValueRestriction above
    public void visit( final Value e ) throws AlignmentException {
    }

    // OWL does not allow for function calls
    public void visit( final Apply e ) throws AlignmentException {
	throw new AlignmentException( "Cannot render function call in OWL "+e );
    }

    public void visit( final Aggregate e ) throws AlignmentException {
	throw new AlignmentException( "Cannot render value aggregation in OWL "+e );
    }

    // Not implemented. We only ignore transformations in OWL
    public void visit( final Transformation transf ) throws AlignmentException {
	logger.debug( "Transformations ignored in OWL" );
    }

    /**
     * Our Datatypes are only strings identifying datatypes.
     * For OWL, they should be considered as built-in types because we do 
     * not know how to add other types.
     * Hence we could simply have used a rdfs:Datatype="&lt;name&gt;"
     *
     * OWL offers further possiblities, such as additional owl:withRestriction
     * clauses
     * 
     * @param e: a datatype to render
     */
    public void visit( final Datatype e ) {
	indentedOutput("<owl:Datatype><owl:onDataType rdf:resource=\""+e.getType()+"\"/></owl:Datatype>");
    }

    public void visit(final Linkkey linkkey) throws AlignmentException {
	logger.debug( "Cannot (yet) render linkkeys in OWL" );
    }
    
    public void visit(final LinkkeyBinding linkkeyBinding) throws AlignmentException {
	logger.debug( "Cannot (yet) render linkkeys in OWL" );
    }
    public void visit(final LinkkeyEquals linkkeyEquals) throws AlignmentException {
	logger.debug( "Cannot (yet) render linkkeys in OWL" );
    }
    public void visit(final LinkkeyIntersects linkkeyIntersects) throws AlignmentException {
	logger.debug( "Cannot (yet) render linkkeys in OWL" );
    }
}
