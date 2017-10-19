# Silk Rules processing for I4.0 standards

## Dependencies
This tool depends on the following software

* JDK 1.8
* Eclipse plugin for Groovy

Donwload Eclipse groovy plugin: https://github.com/groovy/groovy-eclipse/wiki           
Makes sure to download plugin according to your ide version.

## IDE support Running Project in Eclipse
The quick and easy way to start compiling, running and coding **Edoal** is we provide a java project in Eclipse .

Thus, you need to install tools:
* Eclipse IDE: https://www.eclipse.org/downloads/

Import the project in eclipse and click build. The maven dependancies will be downloaded automatically.                      

Add config.ttl to the root of your project. This file configures how the experiments will be run                

You can find Heterogeneity examples at :                         
https://github.com/i40-Tools/HeterogeneityExampleData                                

To run the AML examples please create a file config.ttl in the main directory of the project. An example is show below:
```
@prefix owl:     <http://www.w3.org/2002/07/owl#> .
@prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#> .
@prefix schema:  <http://schema.org/> .
@prefix skos:    <http://www.w3.org/2004/02/skos/core#> .
@prefix xml:     <http://www.w3.org/XML/1998/namespace> .
@prefix xsd:     <http://www.w3.org/2001/XMLSchema#> .
@prefix uri:     <http://uri4uri.net/vocab.html/#>
@prefix aml:     <https://w3id.org/i40/aml#> .
@prefix sto:     <https://w3id.org/i40/sto#>.
@prefix ontosec: <http://www.semanticweb.org/ontologies/2008/11/OntologySecurity.owl#>


aml:conf 
     rdfs:label "General Configuration"@en ;
     uri:path "C:/HeterogeneityExampleData/AutomationML/Single-Heterogeneity/M2/Testbeds-2/";
	 uri:experimentFolder "E:/ExperimentsToKCAP/Experiment1/run -1/";
     sto:Standard "aml";
     uri:URI "C:/Experiments/edoal/resources/".     
```
Please note:  
```
uri:path refers to Heterogeneity path                    
uri:URI refers to the ontology path (optional)
```

Just give path of AML heterogenity and folders will be created automatically.                  

Please add GoldStandard at the root for evaluation.
You can obtain GoldStandard using CPSDocument Generator.

If you want to reproduce the results for paper, please use the report class.
```
## Updating Silk Rules 
Please navigate to src folder and change files starting with rules.

## Updating Krextor Rules 
### What is Krextor?

Krextor is a an extensible XSLT-based framework for extracting RDF from XML.
Please note that the resources folder should be added to the project in order to run Krextor.

Read more at : https://github.com/EIS-Bonn/krextor

Please navigate to /resources/amlrules/aml.xsl

Here you can update, remove or add rules for RDF conversion.


## License

* Copyright (C) 2015-2017 EIS Uni-Bonn
* Licensed under the Apache License

``

