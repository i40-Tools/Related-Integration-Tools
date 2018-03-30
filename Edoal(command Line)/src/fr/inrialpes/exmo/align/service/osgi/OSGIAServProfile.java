/*
 * $Id: OSGIAServProfile.java 1996 2014-11-23 16:30:55Z euzenat $
 *
 * Copyright (C) INRIA, 2010, 2013-2014
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

package fr.inrialpes.exmo.align.service.osgi;

import java.util.Hashtable;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Constants;

import fr.inrialpes.exmo.align.service.AlignmentServiceProfile;
import fr.inrialpes.exmo.align.service.AServProtocolManager;
import fr.inrialpes.exmo.align.service.AServException;

/**
 * OSGIAServProvile: OSGI Service profile for the Alignment server
 * 
 */

public class OSGIAServProfile implements AlignmentServiceProfile, BundleActivator {
    final static Logger logger = LoggerFactory.getLogger( OSGIAServProfile.class );

    private AServProtocolManager manager;

    private String myId;
    private String serverURL;
    private int localId = 0;

    public static BundleContext ctxt = null;

    // AlignmentServiceProfile interface
    public void init( Properties params, AServProtocolManager manager ) throws AServException {
	this.manager = manager;
    }

    public boolean accept( String prefix ) {
	return false;
    }

    public String process( String uri, String prefix, String perf, Properties header, Properties params ) {
	return "OSGI Cannot be invoked this way through HTTP service";
    }

    public void close(){
	// This may unregister the WSDL file to some directory
	stop( ctxt );
    }

    /**
     * Implements BundleActivator.start(). Registers an
     * instance of a dictionary service using the bundle context;
     * attaches properties to the service that can be queried
     * when performing a service look-up.
     * @param context the framework context for the bundle.
    **/
    public void start( BundleContext context ) {
	ctxt = context;
	logger.debug( "{} starting... ", ctxt.getBundle().getHeaders().get(Constants.BUNDLE_NAME) );
	// I am not sure that my goal is to create a new one... we have it already, right?
	//manager = new AServProtocolManager();
	Hashtable<String,String> serviceProperties = new Hashtable<String,String>();
	serviceProperties.put( "lang", "EN-uk" ); // an example
	//ServiceRegistration<?> reg = 
	ctxt.registerService( Service.class.getName(), manager, serviceProperties );
    }

    /**
     * Implements BundleActivator.stop(). Does nothing since
     * the framework will automatically unregister any registered services.
     * @param context the framework context for the bundle.
    **/
    public void stop( BundleContext context ) {
	logger.debug( "{} stoping... ", ctxt.getBundle().getHeaders().get(Constants.BUNDLE_NAME) );
	ctxt = null;
    }

}
    
