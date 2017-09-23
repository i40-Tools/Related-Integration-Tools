# Silk Rules processing
Add as java project.
## Running the examples
To run the examples please create a file config.ttl in the main directory of the project. An example is show below:
```
@prefix aml:     <https://w3id.org/i40/aml#> .
@prefix owl:     <http://www.w3.org/2002/07/owl#> .
@prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#> .
@prefix schema:  <http://schema.org/> .
@prefix skos:    <http://www.w3.org/2004/02/skos/core#> .
@prefix xml:     <http://www.w3.org/XML/1998/namespace> .
@prefix xsd:     <http://www.w3.org/2001/XMLSchema#> .
@prefix uri:     <http://uri4uri.net/vocab.html/#>

aml:conf 
     rdfs:label "General Configuration"@en ;
     uri:path "C:/HeterogeneityExampleData/AutomationML/M2-Granularity/Testbeds-1/";
     uri:URI "C:/Users/omar/Desktop/Alligator-master/resources/aml.ttl".
```
