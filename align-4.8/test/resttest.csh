
setenv serv http://aserv.inrialpes.fr

####################################################################
# Administration primitives

# listalignments
curl -L -H "Accept: text/xml" $serv/rest/listalignments

# listnetworks
curl -L -H "Accept: text/xml" $serv/rest/listnetworks

# listmethods
curl -L -H "Accept: text/xml" $serv/rest/listmethods

# listrenderers
curl -L -H "Accept: text/xml" $serv/rest/listrenderers

# listservices
curl -L -H "Accept: text/xml" $serv/rest/listservices

# listevaluators
curl -L -H "Accept: text/xml" $serv/rest/listevaluators

# wsdl
curl -L -H "Accept: text/xml" $serv/wsdl
curl -L -H "Accept: text/xml" $serv'/wsdl?'

####################################################################
# Alignment primitives

# load
curl -L -H "Accept: text/xml" $serv'/rest/load?url=http://alignapi.gforge.inria.fr/tutorial/refalign.rdf&pretty=le%20test'

setenv alid

# match
curl -L -H "Accept: text/xml" $serv'/rest/match?onto1=http://alignapi.gforge.inria.fr/tutorial/myOnto.owl&onto2=http://alignapi.gforge.inria.fr/tutorial/edu.mit.visus.bibtex.owl&force=true&pretty=test%20rest'

# find
curl -L -H "Accept: text/xml" $serv'/rest/find?onto2=http://alignapi.gforge.inria.fr/tutorial/edu.mit.visus.bibtex.owl'

# retrieve
curl -L -H "Accept: text/xml" $serv'/rest/retrieve?id='$alid'&method=fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor'

# trim
curl -L -H "Accept: text/xml" $serv'/rest/trim?id='$alid'&threshold=.99'

# invert
curl -L -H "Accept: text/xml" $serv'/rest/invert?id='$alid

# align
curl -L -H "Accept: text/xml" $serv'/rest/align?onto2=http://alignapi.gforge.inria.fr/tutorial/myOnto.owl&onto1=http://alignapi.gforge.inria.fr/tutorial/edu.mit.visus.bibtex.owl'

# translate
curl -L -H "Accept: text/xml" $serv'/rest/translate?id='$alid'&query=PREFIX%20rdf%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E%0APREFIX%20onto1%3A%20%3Chttp%3A%2F%2Falignapi.gforge.inria.fr%2Ftutorial%2FmyOnto.owl%23%3E%0ASELECT%20*%0AFROM%20%3C%3E%0AWHERE%20%7B%0A%3FX%20rdf%3Atype%20onto1%3AArticle.%0A%7D'

# eval
curl -L -H "Accept: text/xml" $serv'/rest/eval?id='$alid'&ref='alid'&method=fr.inrialpes.exmo.align.impl.eval.PRecEvaluator'

# corresp
curl -L -H "Accept: text/xml" $serv'/rest/corresp?id='$alid'&entity=http://alignapi.gforge.inria.fr/tutorial/myOnto.owl%23Article'

# metadata
curl -L -H "Accept: text/xml" $serv'/rest/metadata?id='$alid

####################################################################
# Network primitives

# loadnetwork
curl -L -H "Accept: text/xml" $serv'/rest/loadnetwork?url=file:R4SCdemo/R4SC-raw.rdf&pretty=raw%20test'

setenv onid 

# matchnetwork
curl -L -H "Accept: text/xml" $serv'/rest/matchnetwork?id='$onid'&method=fr.inrialpes.exmo.align.impl.method.StringDistAlignment&new=<boolean>'

# closenetwork
curl -L -H "Accept: text/xml" $serv'/rest/closenetwork?id='$onid'&sym=<boolean>&trans=<boolean>&refl=<boolean>'

# trimnetwork
curl -L -H "Accept: text/xml" $serv'/rest/trimnetwork?id='$onid'&threshold=.9'

# invertnetwork
curl -L -H "Accept: text/xml" $serv'/rest/invertnetwork?id='$onid

# printnetwork
curl -L -H "Accept: text/xml" $serv'/rest/printnetwork?id='$onid

# normalizenetwork
curl -L -H "Accept: text/xml" $serv'/rest/normalizenetwork?id='$onid

# denormalizenetwork
curl -L -H "Accept: text/xml" $serv'/rest/denormalizenetwork?id='$onid

####################################################################
# Primitives affecting the database content

# store
curl -L -H "Accept: text/xml" $serv'/rest/store?id='$alid

# storenetwork
curl -L -H "Accept: text/xml" $serv'/rest/storenetwork?id='$onid

