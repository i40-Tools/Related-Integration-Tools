#!/bin/sh

for i in 1 2 3 4 5 6
do
    Echo Processing $i
    java -DconfigFile=admin/basic-script.xml -DlinkSpec=no$i -jar lib/silk/silk.jar 2> /dev/null
    sh bin/fix.sh results/Round$i-accepted.rdf
    grep entity1 results/Round$i-accepted.rdf | wc -l
    java -cp lib/procalign.jar fr.inrialpes.exmo.align.cli.EvalAlign -i fr.inrialpes.exmo.align.impl.eval.PRecEvaluator file:admin/reflinks.rdf file:results/Round$i-accepted.rdf
done
