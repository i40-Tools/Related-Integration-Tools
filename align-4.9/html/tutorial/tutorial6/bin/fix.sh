#!/bin/sh


ed -s $1 <<EOF 2>/dev/null
6 a
  <xml>yes</xml>
  <level>1</level>
  <type>**</type>
  <onto1>
    <Ontology rdf:about="http://rdf.insee.fr/geo/">
      <location>file:admin/regions-2010.rdf</location>
      <formalism>
        <Formalism align:name="OWL1.0" align:uri="http://www.w3.org/2002/07/owl#"/>
      </formalism>
    </Ontology>
  </onto1>
  <onto2>
    <Ontology rdf:about="http://ec.europa.eu/eurostat/ramon/ontologies/geographic.rdf#">
      <location>file:admin/nuts2008_complete.rdf</location>
      <formalism>
        <Formalism align:name="OWL1.0" align:uri="http://www.w3.org/2002/07/owl#"/>
      </formalism>
    </Ontology>
  </onto2>
.
w
q
EOF

