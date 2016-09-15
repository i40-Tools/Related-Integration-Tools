
WHAT IS ONTOLOGY ALIGNMENT?
---------------------------

See http://alignapi.gforge.inria.fr.
See also http://www.ontologymatching.org

For our example we want to convert heterogentiy aml files in to ontology so that we can find Alignment in them.

##Step 1:
Convert aml files in to rdf (using Krextor).

## Step 2:
Rename .ttl to .owl and open in protege.

## Step 3:
Save it as Turtle format.

## Step 4:
replace all the uri;s with the following line :              



@prefix xml: <http://www.w3.org/XML/1998/namespace> .  
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .   
@prefix foaf: <http://xmlns.com/foaf/spec/> .   
@prefix prov: <http://www.w3.org/ns/prov#> .   
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .  
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .   
@prefix vann: <http://purl.org/vocab/vann/> .   
@prefix void: <http://rdfs.org/ns/void#> .   
@prefix terms: <http://purl.org/dc/terms/> .   
@prefix schema: <http://schema.org/> .   
@base <https://w3id.org/i40/aml#> .   

<https://w3id.org/i40/aml#> rdf:type owl:Ontology ;
                             
                             dct:created "2016-03-23"^^xsd:date ;
                             
                             vann:preferredNamespacePrefix "aml" ;
                             
                             vann:preferredNamespaceUri "https://w3id.org/i40/aml/" ;
                             
                             dct:creator "Irlan Grangel" ;
                             
                             owl:versionInfo "0.1" ;
                             
                             skos:definition "A vocabulary to represents the AutomationML Standard - IEC 62714."@en ;
                             
                             skos:altLabel "Alternative label for an AML object"@en ;
                             
                             dct:creator "Olga Kovalenko" ;
                             
                             skos:prefLabel "aml"@en ;
                             
                             dct:license <http://creativecommons.org/licenses/by-nc-sa/2.0/> ;
                             
                             void:vocabulary dc: ,
                                             dct: ,
                                             rdf: ,
                                             rdfs: ,
                                             xsd: ,
                                             skos: ,
                                             <http://www.w3.org/XML/1998/namespace> ,
                                             foaf: ,
                                             <https://w3id.org/i40/aml#> ;
                             
                             rdfs:isDefinedBy <https://w3id.org/i40/aml#> .


Note this will only work for AML domain.

## Step5:
Once your owl files are ready we can now move to use align api.
Clone the repository
Go to cmd  and move to directory align-4.8/examples.
You can also go there by directly right shift click open as cmd.

Once you are in that folder run the following command:

java -jar ../lib/procalign.jar file:c://file1.owl file:c://file2.owl -o rdf/rules.rdf

This will generate the rules files in rdf/ folder. You can also specify your own folders. File1 and file 2 are path of converted owl files.

## Step 6:
Once we have our rule we can use it to find alignment

Run the following command:

java -cp ../lib/procalign.jar fr.inrialpes.exmo.align.cli.ParserPrinter file:rdf/rules.rdf -r fr.inrialpes.exmo.align.impl.renderer.OWLAxiomsRendererVisitor -o rdf/output.rdf
This will save the aligned owl results in the output folder.
if you want to give manual path for rules :

java -cp ../lib/procalign.jar fr.inrialpes.exmo.align.cli.ParserPrinter file:c://rules.rdf -r fr.inrialpes.exmo.align.impl.renderer.OWLAxiomsRendererVisitor -o rdf/output.rdf

You can find converted files examples folder by name of converted.aml and converted-1.aml
