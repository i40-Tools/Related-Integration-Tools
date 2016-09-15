/*
 * $Id: JadeFIPAAServiceAgent.java 1988 2014-11-14 22:20:28Z euzenat $
 *
 * Copyright (C) Orange R&D, 2006
 * Copyright (C) INRIA, 2006, 2008-2009, 2011-2014
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

package fr.inrialpes.exmo.align.service.jade;

import java.util.Iterator;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.content.Predicate;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import fr.inrialpes.exmo.align.service.AServProtocolManager;
import fr.inrialpes.exmo.align.service.msg.Message;
import fr.inrialpes.exmo.align.service.msg.ErrorMsg;
import fr.inrialpes.exmo.align.service.jade.messageontology.ALIGN;
import fr.inrialpes.exmo.align.service.jade.messageontology.Action;
import fr.inrialpes.exmo.align.service.jade.messageontology.CUT;
import fr.inrialpes.exmo.align.service.jade.messageontology.FIND;
import fr.inrialpes.exmo.align.service.jade.messageontology.JADEFIPAAlignmentServerOntology;
import fr.inrialpes.exmo.align.service.jade.messageontology.LOAD;
import fr.inrialpes.exmo.align.service.jade.messageontology.METADATA;
import fr.inrialpes.exmo.align.service.jade.messageontology.Parameter;
import fr.inrialpes.exmo.align.service.jade.messageontology.RETRIEVE;
import fr.inrialpes.exmo.align.service.jade.messageontology.STORE;
import fr.inrialpes.exmo.align.service.jade.messageontology.TRANSLATE;

public class JadeFIPAAServiceAgent extends Agent {
    final static Logger logger = LoggerFactory.getLogger( JadeFIPAAServiceAgent.class );

    private static final long serialVersionUID = 460;
    public static final String SERVICE_NAME = "Alignment";
    public static final String SERVICE_TYPE = "Alignment-service";

    private AServProtocolManager manager;
    private Properties initialParameters;
    
    //	FIPA ACL stuff

    private ContentManager CTmanager=new ContentManager();
    private SLCodec codec=new SLCodec();
    private Ontology ontology=JADEFIPAAlignmentServerOntology.getInstance();


    protected void setup() {
	logger.info( "{} started", getAID().getName() );
	super.setup();
	codec = new SLCodec();
	
	// ontology =  ContextAgentManagerOntology.getInstance();
	CTmanager = this.getContentManager();
	
	// logger.trace("agent {} {} is created", getAID(), getLocalName() );

	CTmanager.registerOntology(ontology);
	CTmanager.registerLanguage(codec);

	// Read arguments
	Object[] args = getArguments();
	if ( args != null ) {
	    /**for (int i = 0; i < args.length; ++i) {
	       logger.debug( "Arg-{} = {}", i, args[i] );
	       }**/
	    manager = (AServProtocolManager)args[0];
	    initialParameters = (Properties)args[1];
	}

	// Add initial behaviours to manage incoming message
	addBehaviour(new CyclicBehaviour(this) {
		private static final long serialVersionUID = 330;
		public void action() {
		    
		    String perf; // performative
		    String info; //parameters
		    Properties params = initialParameters;
		    
		    MessageTemplate tpl = MessageTemplate.and(MessageTemplate.and(
						MessageTemplate.MatchLanguage( codec.getName()),
						MessageTemplate.MatchOntology( ontology.getName())),
						MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

		    ACLMessage msg = myAgent.receive(tpl);
		    if (msg != null) {
			//logger.debug( "Received message: {}", msg.toString() );
			try{
			    ContentElement ce = null;
			    ce = CTmanager.extractContent(msg);
			    params = decodeMessage(ce,params);
			    
			    if (ce instanceof ALIGN) {
				Message answer = manager.align( params );
				if(!(answer instanceof ErrorMsg)) {
				    ACLMessage JADEanswer=msg.createReply();
				    JADEanswer.setLanguage(codec.getName());
				    JADEanswer.setOntology(ontology.getName());
				    JADEanswer.setPerformative(ACLMessage.INFORM);
				    //JADEanswer.setContent(answer.getContent());
				    ((Action)ce).setResult(answer.getContent());
				    CTmanager.fillContent(JADEanswer, ce);
				    myAgent.send(JADEanswer);								
				} else {
				    logger.warn( answer.getContent() );
				}
			    } else if (ce instanceof LOAD) {
				Message answer = manager.load( params );
				if(!(answer instanceof ErrorMsg)) {
				    ACLMessage JADEanswer=msg.createReply();
				    JADEanswer.setLanguage(codec.getName());
				    JADEanswer.setOntology(ontology.getName());
				    JADEanswer.setPerformative(ACLMessage.INFORM);
				    ((Action)ce).setResult(answer.getContent());
				    CTmanager.fillContent(JADEanswer, ce);
				    //JADEanswer.setContent(answer.getContent());
				    myAgent.send(JADEanswer);
				} else {
				    logger.warn( answer.getContent() );
				}
			    } else if (ce instanceof RETRIEVE) {
				Message answer = manager.render( params );
				if(!(answer instanceof ErrorMsg)) {
				    ACLMessage JADEanswer=msg.createReply();
				    JADEanswer.setLanguage(codec.getName());
				    JADEanswer.setOntology(ontology.getName());
				    JADEanswer.setPerformative(ACLMessage.INFORM);
				    //JADEanswer.setContent(answer.getContent());
				    ((Action)ce).setResult(answer.getContent());
				    CTmanager.fillContent(JADEanswer, ce);
				    myAgent.send(JADEanswer);
				} else {
				    logger.warn( answer.getContent() );
				}
			    } else if (ce instanceof TRANSLATE) {
				//TODO
			    } else if (ce instanceof METADATA) {
				//TODO
			    } else if (ce instanceof STORE) {
				Message answer = manager.store( params );
				if(!(answer instanceof ErrorMsg)) {
				    ACLMessage JADEanswer=msg.createReply();
				    JADEanswer.setLanguage(codec.getName());
				    JADEanswer.setOntology(ontology.getName());
				    JADEanswer.setPerformative(ACLMessage.INFORM);
				    //JADEanswer.setContent(answer.getContent());
				    ((Action)ce).setResult(answer.getContent());
				    CTmanager.fillContent(JADEanswer, ce);
				    myAgent.send(JADEanswer);
				} else {
				    logger.warn( answer.getContent() );
				}
			    } else if (ce instanceof FIND) {
				Message answer = manager.existingAlignments( params );
				if(!(answer instanceof ErrorMsg)) {
				    ACLMessage JADEanswer=msg.createReply();
				    JADEanswer.setLanguage(codec.getName());
				    JADEanswer.setOntology(ontology.getName());
				    JADEanswer.setPerformative(ACLMessage.INFORM);
				    //JADEanswer.setContent(answer.getContent());
				    ((Action)ce).setResult(answer.getContent());
				    CTmanager.fillContent(JADEanswer, ce);
				    myAgent.send(JADEanswer);
				} else {
				    logger.warn( answer.getContent() );
				}
			    } else if (ce instanceof CUT) {
				Message answer = manager.trim( params );
				if(!(answer instanceof ErrorMsg)) {
				    ACLMessage JADEanswer=msg.createReply();
				    JADEanswer.setLanguage(codec.getName());
				    JADEanswer.setOntology(ontology.getName());
				    JADEanswer.setPerformative(ACLMessage.INFORM);
				    //JADEanswer.setContent(answer.getContent());
				    ((Action)ce).setResult(answer.getContent());
				    CTmanager.fillContent(JADEanswer, ce);
				    myAgent.send(JADEanswer);
				} else {
				    logger.warn( answer.getContent() );
				}
			    } else {
				ACLMessage JADEanswer=msg.createReply();
				JADEanswer.setLanguage(codec.getName());
				JADEanswer.setOntology(ontology.getName());
				JADEanswer.setPerformative(ACLMessage.NOT_UNDERSTOOD);						
				myAgent.send(JADEanswer);
			    }
			} catch( CodecException ce ) {
			    logger.debug( "IGNORED Exception", ce );
			} catch( OntologyException oe ) {
			    logger.debug( "IGNORED Exception", oe );
			}
		    } else {
			block();
		    }
		    params = initialParameters;
		}
		
	    });//end of CyclicBehaviour
	
	// Register with the DF
	registerWithDF();
    }//end of Setup
    
    protected void takeDown() {
	logger.info( "Agent Alignement Service closed" );
	this.doDelete();
    }
    
    private void registerWithDF() {
	DFAgentDescription dfd = new DFAgentDescription();
	dfd.setName(getAID());
	ServiceDescription sd = new ServiceDescription();
	sd.setName(getLocalName()+'-'+SERVICE_NAME);
	sd.setType(SERVICE_TYPE);
	dfd.addServices(sd);
	try {
	    logger.debug( "Registering with DF..." );
	    DFService.register(this, dfd);
	    logger.debug( "Registration OK." );
	}
	catch ( FIPAException fex ) {
	    logger.warn( "Error registering with DF", fex );
	}
    }
    
    private Properties decodeMessage(ContentElement ce, Properties param) {
	Properties toReturn = param;
	Action action= (Action)ce;
	// Not iterable in this version
	//for ( Parameter ontoParam : action.getAllHasParameter() ) {
	for( Iterator<Parameter> iter = action.getAllHasParameter(); iter.hasNext(); ) {
	    Parameter ontoParam = iter.next();
	    toReturn.setProperty( ontoParam.getName(), ontoParam.getValue() ); 
	}
	return toReturn;
    }

}
