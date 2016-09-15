#!/bin/sh
#
# This long script runs commands from the 
#
# It is highly dependent on logback and my own logback.xml
#

CWD=`pwd`
CP=$CWD/lib/procalign.jar:$CWD/lib/slf4j/logback-core-1.1.3.jar:$CWD/lib/slf4j/logback-classic-1.1.3.jar:$CWD
RESDIR='/tmp/clitest'

########################################################################
# ProcAlign
########################################################################

/bin/rm -rf $RESDIR
mkdir -p $RESDIR

# GOTO
if false; then
echo this is for avoiding some parts
#GOTO
fi

echo "\t\tTHE -z OPTIONS RELY ON LOGBACK BEING PROPERLY DEFINED"
echo "\t\t *** Testing Procalign ***"

#-------------------
echo "\t-z, -zzz"
java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.Procalign -z &> $RESDIR/zerr.txt
grep "Unrecognized option: -z" $RESDIR/zerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with PROC-ERR1; fi
java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.Procalign --zzz &> $RESDIR/zzzerr.txt
grep "Unrecognized option: --zzz" $RESDIR/zzzerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with PROC-ERR2; fi

#-------------------
echo "\t-h,--help"
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign -h &> $RESDIR/proc-h.txt
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign --help &> $RESDIR/proc-help.txt
if [ -s  $RESDIR/proc-h.txt ]; then diff $RESDIR/proc-h.txt $RESDIR/proc-help.txt > $RESDIR/proc-diff-h.txt; else echo error with PROC-HELP1; fi
if [ -s $RESDIR/proc-diff-h.txt ]; then echo error with PROC-HELP2; fi

#-------------------
echo "\t-o,--output <FILE>"
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign file://$CWD/examples/rdf/onto1.owl file://$CWD/examples/rdf/onto2.owl -o $RESDIR/proc-o1.rdf
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign file://$CWD/examples/rdf/onto1.owl file://$CWD/examples/rdf/onto2.owl --output $RESDIR/proc-output1.rdf
sed "s:<alext\:time>[^<]*</alext\:time>::" $RESDIR/proc-o1.rdf > $RESDIR/proc-o.rdf
sed "s:<alext\:time>[^<]*</alext\:time>::" $RESDIR/proc-output1.rdf > $RESDIR/proc-output.rdf
if [ -s  $RESDIR/proc-o.rdf ]; then diff $RESDIR/proc-o.rdf $RESDIR/proc-output.rdf; else echo error with PROC-OUTPUT1; fi

#-------------------
echo "\t-a,--alignment <FILE>"
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign file://$CWD/examples/rdf/onto1.owl file://$CWD/examples/rdf/onto2.owl -a file:examples/rdf/newsample.rdf | sed "s:<alext\:time>[^<]*</alext\:time>::" > $RESDIR/proc-a.rdf
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign file://$CWD/examples/rdf/onto1.owl file://$CWD/examples/rdf/onto2.owl --alignment file:examples/rdf/newsample.rdf | sed "s:<alext\:time>[^<]*</alext\:time>::" > $RESDIR/proc-align.rdf
if [ -s  $RESDIR/proc-a.rdf ]; then diff $RESDIR/proc-a.rdf $RESDIR/proc-align.rdf; else echo error with PROC-ALIGN1; fi

#-------------------
echo "\t-i <C>, --impl <C>"
#JE: A random distance available (could try with others)
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign file://$CWD/examples/rdf/onto1.owl file://$CWD/examples/rdf/onto2.owl -i fr.inrialpes.exmo.align.impl.method.StrucSubsDistAlignment | sed "s:<alext\:time>[^<]*</alext\:time>::" > $RESDIR/proc-i.rdf
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign file://$CWD/examples/rdf/onto1.owl file://$CWD/examples/rdf/onto2.owl --impl fr.inrialpes.exmo.align.impl.method.StrucSubsDistAlignment | sed "s:<alext\:time>[^<]*</alext\:time>::" > $RESDIR/proc-impl.rdf
if [ -s  $RESDIR/proc-i.rdf ]; then diff $RESDIR/proc-i.rdf $RESDIR/proc-impl.rdf; else echo error with PROC-IMPL1; fi

#-------------------
echo "\t-Dn=v"
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign file://$CWD/examples/rdf/onto1.owl file://$CWD/examples/rdf/onto2.owl -i fr.inrialpes.exmo.align.impl.method.StringDistAlignment -DstringFunction=levenshteinDistance | sed "s:<alext\:time>[^<]*</alext\:time>::" > $RESDIR/proc-id.rdf
if [ -s $RESDIR/proc-id.rdf ]; then diff $RESDIR/proc-i.rdf $RESDIR/proc-id.rdf | sed "s:<alext\:time>[^<]*</alext\:time>::" > $RESDIR/diff-proc.txt; else echo error with PROC-PARAM1; fi
if [ ! -s $RESDIR/diff-proc.txt ]; then echo error with PROC-PARAM2; fi

#-------------------
echo "\t-t,--threshold <DOUBLE>"
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign file://$CWD/examples/rdf/onto1.owl file://$CWD/examples/rdf/onto2.owl -i fr.inrialpes.exmo.align.impl.method.StringDistAlignment -DstringFunction=levenshteinDistance -t 0.4 | sed "s:<alext\:time>[^<]*</alext\:time>::" > $RESDIR/proc-t.rdf
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign file://$CWD/examples/rdf/onto1.owl file://$CWD/examples/rdf/onto2.owl fr.inrialpes.exmo.align.impl.method.StringDistAlignment -DstringFunction=levenshteinDistance --threshold 0.4 | sed "s:<alext\:time>[^<]*</alext\:time>::" > $RESDIR/proc-threshold.rdf
if [ -s $RESDIR/proc-t.rdf ]; then diff $RESDIR/proc-t.rdf $RESDIR/proc-threshold.rdf; else echo error with PROC-THRES1; fi
# test diff from previous
diff $RESDIR/proc-t.rdf $RESDIR/proc-id.rdf > $RESDIR/diff-proc.txt
if [ ! -s $RESDIR/diff-proc.txt ]; then echo error with PROC-THRES2; fi

#-------------------
echo "\t-T,--cutmethod <METHOD>"
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign file://$CWD/examples/rdf/onto1.owl file://$CWD/examples/rdf/onto2.owl -i fr.inrialpes.exmo.align.impl.method.StringDistAlignment -DstringFunction=levenshteinDistance -t 0.4 -T perc | sed "s:<alext\:time>[^<]*</alext\:time>::" > $RESDIR/proc-C.rdf
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign file://$CWD/examples/rdf/onto1.owl file://$CWD/examples/rdf/onto2.owl --impl fr.inrialpes.exmo.align.impl.method.StringDistAlignment -DstringFunction=levenshteinDistance -t 0.4 --cutmethod perc | sed "s:<alext\:time>[^<]*</alext\:time>::" > $RESDIR/proc-cutmethod.rdf
if [ -s $RESDIR/proc-C.rdf ]; then diff $RESDIR/proc-C.rdf $RESDIR/proc-cutmethod.rdf; else echo error with PROC-CUT1; fi
# test diff from previous
diff $RESDIR/proc-C.rdf $RESDIR/proc-t.rdf > $RESDIR/diff-proc.txt
if [ ! -s $RESDIR/diff-proc.txt ]; then echo error with PROC-CUT2; fi

#-------------------
echo "\t-r,--renderer <CLASS>"
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign file://$CWD/examples/rdf/onto1.owl file://$CWD/examples/rdf/onto2.owl -r fr.inrialpes.exmo.align.impl.renderer.OWLAxiomsRendererVisitor | sed "s/alext\:time: [0-9]*/alext\:time: 0/" > $RESDIR/proc-r.rdf
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign file://$CWD/examples/rdf/onto1.owl file://$CWD/examples/rdf/onto2.owl --renderer fr.inrialpes.exmo.align.impl.renderer.OWLAxiomsRendererVisitor | sed "s/alext\:time: [0-9]*/alext\:time: 0/" > $RESDIR/proc-render.rdf
if [ -s $RESDIR/proc-r.rdf ]; then diff $RESDIR/proc-r.rdf $RESDIR/proc-render.rdf; else echo error with PROC-RENDER1; fi

#-------------------
echo "\t-P,--params <FILE>"
echo "<?xml version='1.0' encoding='utf-8' standalone='no'?>
<\x21DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">
<properties>
<entry key=\"impl\">fr.inrialpes.exmo.align.impl.method.StringDistAlignment</entry>
<entry key=\"stringFunction\">levenshteinDistance</entry>
</properties>" > $RESDIR/params.xml
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign file://$CWD/examples/rdf/onto1.owl file://$CWD/examples/rdf/onto2.owl -P $RESDIR/params.xml | sed "s:<alext\:time>[^<]*</alext\:time>::" > $RESDIR/proc-p.rdf
#Will not work because we use loadFromXML
#echo "impl=fr.inrialpes.exmo.align.impl.method.StringDistAlignment
#stringFunction=levenshteinDistance" > $RESDIR/params.prop
java -cp $CP fr.inrialpes.exmo.align.cli.Procalign file://$CWD/examples/rdf/onto1.owl file://$CWD/examples/rdf/onto2.owl --params $RESDIR/params.xml | sed "s:<alext\:time>[^<]*</alext\:time>::" > $RESDIR/proc-params.rdf
if [ -s  $RESDIR/proc-p.rdf ]; then diff $RESDIR/proc-p.rdf $RESDIR/proc-params.rdf; else echo error with PROC-PARAMS1; fi
diff $RESDIR/proc-p.rdf $RESDIR/proc-id.rdf

########################################################################
# ParserPrinter
########################################################################

echo "\t\t *** Testing ParserPrinter ***"

#-------------------
echo "\t-z, -zzz"

java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter -z &> $RESDIR/zerr.txt
grep "Unrecognized option: -z" $RESDIR/zerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with PARS-ERR1; fi
java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter --zzz &> $RESDIR/zzzerr.txt
grep "Unrecognized option: --zzz" $RESDIR/zzzerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with PARS-ERR2; fi

#-------------------
echo "\t-h, --help"
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter -h &> $RESDIR/pars-h.txt
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter --help &> $RESDIR/pars-help.txt
if [ -s  $RESDIR/pars-h.txt ]; then diff $RESDIR/pars-h.txt $RESDIR/pars-help.txt; else echo error with $RESDIR/pars-h.txt; fi

#-------------------
echo "\tno-op"
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter file://$CWD/examples/rdf/newsample.rdf &> $RESDIR/pars-noop.rdf

#-------------------
echo "\t-e,--embedded"
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter file://$CWD/examples/rdf/newsample.rdf -e -o $RESDIR/pars-e.rdf
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter file://$CWD/examples/rdf/newsample.rdf --embedded -o $RESDIR/pars-emb.rdf
if [ -s  $RESDIR/pars-e.rdf ]; then diff $RESDIR/pars-e.rdf $RESDIR/pars-emb.rdf; else echo error with PARS-EMB; fi
diff $RESDIR/pars-e.rdf $RESDIR/pars-noop.rdf

#-------------------
# JE: there is no alternative parser (RDF/XML Parser are not AlignmentParsers)
echo "\t-p,--parser <CLASS>"
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter -p fr.inrialpes.exmo.align.parser.AlignmentParser file://$CWD/examples/rdf/newsample.rdf -o $RESDIR/pars-p1.rdf
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter --parser fr.inrialpes.exmo.align.parser.AlignmentParser file://$CWD/examples/rdf/newsample.rdf -o $RESDIR/pars-p2.rdf
if [ -s  $RESDIR/pars-p1.rdf ]; then diff $RESDIR/pars-p1.rdf $RESDIR/pars-p2.rdf; else echo error with PARS-PARS1; fi

#-------------------
echo "\t-o <F>, --output <F>"
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter file://$CWD/examples/rdf/newsample.rdf -o $RESDIR/pars-o1.rdf
if [ -s  $RESDIR/pars-o1.rdf ]; then diff $RESDIR/pars-o1.rdf $RESDIR/pars-noop.rdf; else echo error with PARS-OUTPUT0; fi
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter file://$CWD/examples/rdf/newsample.rdf --output $RESDIR/pars-output1.rdf
diff $RESDIR/pars-o1.rdf $RESDIR/pars-output1.rdf

#-------------------
# This is for SPARQL OUTPUT
echo "\t-c,--outputDir <DIR>"
#java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter file://$CWD/examples/rdf/newsample.rdf -w $RESDIR -o pars-o2.rdf
#java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter file://$CWD/examples/rdf/newsample.rdf --output pars-output2.rdf --outputDir $RESDIR
#if [ -s  $RESDIR/pars-o2.rdf ]; then diff $RESDIR/pars-o2.rdf $RESDIR/pars-output2.rdf; else echo error with PARS-OUTPUT2; fi

#-------------------
echo "\t-i,--inverse"
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter file://$CWD/examples/rdf/newsample.rdf -i -o $RESDIR/pars-i.rdf
if [ -s $RESDIR/pars-i.rdf ]; then diff $RESDIR/pars-i.rdf $RESDIR/pars-noop.rdf > $RESDIR/diff-pars.txt; else echo error with PARS-INV1; fi
if [ ! -s $RESDIR/diff-pars.txt ]; then echo error with PARS-INV2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter file://$CWD/examples/rdf/newsample.rdf --inverse --output $RESDIR/pars-inverse.rdf
diff $RESDIR/pars-i.rdf $RESDIR/pars-inverse.rdf

#-------------------
echo "\t-t,--threshold <DOUBLE>"
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter file://$CWD/examples/rdf/newsample.rdf -t .6 -o $RESDIR/pars-t1.rdf
if [ -s $RESDIR/pars-t1.rdf ]; then diff $RESDIR/pars-t1.rdf $RESDIR/pars-noop.rdf > $RESDIR/diff-thres.txt; else echo error with PARS-THRES1; fi
if [ ! -s $RESDIR/diff-thres.txt ]; then echo error with PARS-THRES2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter file://$CWD/examples/rdf/newsample.rdf --threshold .6 --output $RESDIR/pars-thres1.rdf
diff $RESDIR/pars-t1.rdf $RESDIR/pars-thres1.rdf

#-------------------
echo "\t-T,--cutmethod <METHOD>"
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter file://$CWD/examples/rdf/newsample.rdf -T best -t .6 -o $RESDIR/pars-g1.rdf
if [ -s $RESDIR/pars-g1.rdf ]; then diff $RESDIR/pars-g1.rdf $RESDIR/pars-t1.rdf > $RESDIR/diff-gap.txt; else echo error with PARS-GAP1; fi
if [ ! -s $RESDIR/diff-gap.txt ]; then echo error with PARS-GAP2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter file://$CWD/examples/rdf/newsample.rdf --cutmethod best --threshold .6 --output $RESDIR/pars-gap1.rdf
diff $RESDIR/pars-g1.rdf $RESDIR/pars-gap1.rdf

#-------------------
echo "\t-r,--renderer <CLASS>"
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter file://$CWD/examples/rdf/newsample.rdf -r fr.inrialpes.exmo.align.impl.renderer.OWLAxiomsRendererVisitor -o $RESDIR/pars-r1.rdf
if [ -s $RESDIR/pars-r1.rdf ]; then diff $RESDIR/pars-r1.rdf $RESDIR/pars-o1.rdf > $RESDIR/diff-rend.txt; else echo error with PARS-RENDER1; fi
if [ ! -s $RESDIR/diff-rend.txt ]; then echo error with PARS-RENDER2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.ParserPrinter file://$CWD/examples/rdf/newsample.rdf --renderer fr.inrialpes.exmo.align.impl.renderer.OWLAxiomsRendererVisitor -o $RESDIR/pars-render1.rdf
diff $RESDIR/pars-r1.rdf $RESDIR/pars-render1.rdf

#-------------------
echo "\t-Dn=v"
echo "\t(same as Procalign)"

#-------------------
echo "\t-P,--params <FILE>"
echo "\t(same as Procalign)"


########################################################################
# EvalAlign
########################################################################

echo "\t\t *** Testing EvalAlign ***"

#-------------------
echo "\t-d(<L>), --debug (<L>) DEPRECATED"

#-------------------
echo "\t-z, -zzz"

java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.EvalAlign -z &> $RESDIR/zerr.txt
grep "Unrecognized option: -z" $RESDIR/zerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with EVAL-ERR1; fi
java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.EvalAlign --zzz &> $RESDIR/zzzerr.txt
grep "Unrecognized option: --zzz" $RESDIR/zzzerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with EVAL-ERR2; fi

#-------------------
echo "\t-h, --help"
java -cp $CP fr.inrialpes.exmo.align.cli.EvalAlign -h &> $RESDIR/eval-h.txt
java -cp $CP fr.inrialpes.exmo.align.cli.EvalAlign --help &> $RESDIR/eval-help.txt
# This should be put everywhere!
if [ -s  $RESDIR/eval-h.txt ]; then diff $RESDIR/eval-h.txt $RESDIR/eval-help.txt; else echo error with EVAL-HELP1; fi

#java -cp $CP fr.inrialpes.exmo.align.cli.EvalAlign -i fr.inrialpes.exmo.align.impl.eval.PRecEvaluator html/tutorial/refalign.rdf html/tutorial/refalign.rdf  -o $RESDIR/eval-i.txt
#java -cp $CP fr.inrialpes.exmo.align.cli.EvalAlign --impl fr.inrialpes.exmo.align.impl.eval.PRecEvaluator html/tutorial/refalign.rdf html/tutorial/refalign.rdf  --output $RESDIR/eval-impl.txt
#if [ -s  $RESDIR/eval-i.txt ]; then diff $RESDIR/eval-i.txt $RESDIR/eval-impl.txt; else echo error with EVAL_HELP2; fi

#-------------------
echo "\tno-op"
java -cp $CP fr.inrialpes.exmo.align.cli.EvalAlign file://$CWD/examples/rdf/newsample.rdf file://$RESDIR/proc-o1.rdf  &> $RESDIR/eval-noop.xml
if [ ! -s  $RESDIR/eval-noop.xml ]; then echo error with EVAL-NOOP; fi

#-------------------
echo "\t-o <F>, --output <F>"
java -cp $CP fr.inrialpes.exmo.align.cli.EvalAlign file://$CWD/examples/rdf/newsample.rdf file://$RESDIR/proc-o1.rdf -o $RESDIR/eval-o1.xml
if [ -s $RESDIR/eval-o1.xml ]; then diff $RESDIR/eval-o1.xml $RESDIR/eval-noop.xml; else echo error with EVAL-OUT1; fi
java -cp $CP fr.inrialpes.exmo.align.cli.EvalAlign file://$CWD/examples/rdf/newsample.rdf file://$RESDIR/proc-o1.rdf --output $RESDIR/eval-out1.xml
diff $RESDIR/eval-out1.xml $RESDIR/eval-o1.xml

#-------------------
echo "\t-i <C>, --impl <C>"
java -cp $CP fr.inrialpes.exmo.align.cli.EvalAlign -i fr.inrialpes.exmo.align.impl.eval.WeightedPREvaluator file://$CWD/examples/rdf/newsample.rdf file://$RESDIR/proc-o1.rdf -o $RESDIR/eval-i1.xml
if [ -s $RESDIR/eval-i1.xml ]; then diff $RESDIR/eval-o1.xml $RESDIR/eval-noop.xml > $RESDIR/diff-evimpl.txt ; else echo error with EVAL-IMPL1; fi
if [ ! -s $RESDIR/diff-rend.txt ]; then echo error with EVAL-IMPL2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.EvalAlign --impl fr.inrialpes.exmo.align.impl.eval.WeightedPREvaluator file://$CWD/examples/rdf/newsample.rdf file://$RESDIR/proc-o1.rdf -o $RESDIR/eval-impl1.xml
diff $RESDIR/eval-impl1.xml $RESDIR/eval-i1.xml

#-------------------
echo "\t-Dn=v"
echo "\t(same as Procalign)"

#-------------------
echo "\t-P,--params <FILE>"
echo "\t(same as Procalign)"

########################################################################
# TestGen
########################################################################

echo "\t\t *** Testing TestGen ***"

#java -Xmx1200m -cp $CP fr.inrialpes.exmo.align.cli.TestGen -u http://www.example.org/mynewtest -w $RESDIR/outtestdir -DremoveClasses=.25 examples/rdf/edu.umbc.ebiquity.publication.owl
# --> This one
#java -Xmx1200m -cp $CP fr.inrialpes.exmo.align.cli.TestGen -t fr.inrialpes.exmo.align.gen.BenchmarkGenerator -u http://www.example.org/mynewtest -w $RESDIR/outtestdir examples/rdf/edu.umbc.ebiquity.publication.owl
#$JAVA -Xmx1200m -cp $CP fr.inrialpes.exmo.align.cli.TestGen -t fr.inrialpes.exmo.align.gen.BenchmarkGenerator -u $URI/$run/ -w $DIR/r$run $seedonto

#-------------------
echo "\t-z, -zzz"

java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.TestGen -z &> $RESDIR/zerr.txt
grep "Unrecognized option: -z" $RESDIR/zerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with GEN-ERR1; fi
java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.TestGen --zzz &> $RESDIR/zzzerr.txt
grep "Unrecognized option: --zzz" $RESDIR/zzzerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with GEN-ERR2; fi

#-------------------
echo "\t-h, --help"
java -cp $CP fr.inrialpes.exmo.align.cli.TestGen -h &> $RESDIR/test-h.txt
java -cp $CP fr.inrialpes.exmo.align.cli.TestGen --help &> $RESDIR/test-help.txt
if [ -s  $RESDIR/test-h.txt ]; then diff $RESDIR/test-h.txt $RESDIR/test-help.txt; else echo error with TEST-HELP; fi

#-------------------
echo "\t-u,--urlprefix <URI>  --uriprefix"
mkdir $RESDIR/smalltest
mkdir $RESDIR/smalltest/gentestempty1
cd $RESDIR/smalltest/gentestempty1
java -Xmx1200m -cp $CP fr.inrialpes.exmo.align.cli.TestGen -u http://www.example.org/mynewtest $CWD/examples/rdf/edu.umbc.ebiquity.publication.owl
if [ ! -s refalign.rdf ]; then echo error with GEN-EMP1; fi
if [ ! -s onto.rdf ]; then echo error with GEN-EMP2; fi
mkdir $RESDIR/smalltest/gentestempty2
cd $RESDIR/smalltest/gentestempty2
java -Xmx1200m -cp $CP fr.inrialpes.exmo.align.cli.TestGen --uriprefix http://www.example.org/mynewtest2 $CWD/examples/rdf/edu.umbc.ebiquity.publication.owl
if [ -s refalign.rdf ]; then diff refalign.rdf $RESDIR/smalltest/gentestempty1/refalign.rdf > gendiff.txt; else echo error with GEN-EMP3; fi
if [ ! -s gendiff.txt ]; then echo error with GEN-EMP4; fi
if [ -s onto.rdf ]; then diff onto.rdf $RESDIR/smalltest/gentestempty1/onto.rdf > gendiff.txt; else echo error with GEN-EMP5; fi
if [ ! -s gendiff.txt ]; then echo error with GEN-EMP6; fi
cd $CWD

#-------------------
echo "\t-w,--outdir <DIR>"
java -Xmx1200m -cp $CP fr.inrialpes.exmo.align.cli.TestGen -u http://www.example.org/mynewtest -w $RESDIR/smalltest/outtestdir1 examples/rdf/edu.umbc.ebiquity.publication.owl
java -Xmx1200m -cp $CP fr.inrialpes.exmo.align.cli.TestGen -u http://www.example.org/mynewtest --outdir $RESDIR/smalltest/outtestdir2 examples/rdf/edu.umbc.ebiquity.publication.owl
#if [ -s $RESDIR/smalltest/outtestdir1/onto.rdf ]; then diff $RESDIR/smalltest/outtestdir1/onto.rdf $RESDIR/smalltest/outtestdir2/onto.rdf; else echo error with GEN-OUT1; fi
if [ -s $RESDIR/smalltest/outtestdir1/refalign.rdf ]; then diff $RESDIR/smalltest/outtestdir1/refalign.rdf $RESDIR/smalltest/outtestdir2/refalign.rdf; else echo error with GEN-OUT2; fi

#-------------------
echo "\t-Dn=v"
java -Xmx1200m -cp $CP fr.inrialpes.exmo.align.cli.TestGen -u http://www.example.org/mynewtest -DremoveClasses=.25 -w $RESDIR/smalltest/outtestdir3 examples/rdf/edu.umbc.ebiquity.publication.owl
# Here we can do with many parameters
if [ -s $RESDIR/smalltest/outtestdir3/onto.rdf ]; then diff $RESDIR/smalltest/outtestdir1/refalign.rdf $RESDIR/smalltest/outtestdir3/refalign.rdf > $RESDIR/smalltest/outtestdir2/gendiff.txt ; else echo error with GEN-OUT3; fi
if [ ! -s $RESDIR/smalltest/outtestdir2/gendiff.txt ]; then echo error with GEN-VAR1; fi

#-------------------
echo "\t-a,--alignname <FILE> "
echo "\t-o,--output <FILE>"
java -Xmx1200m -cp $CP fr.inrialpes.exmo.align.cli.TestGen -u http://www.example.org/mynewtest -a myalign.rdf -o biblio.owl -w $RESDIR/outtestdir4 examples/rdf/edu.umbc.ebiquity.publication.owl
java -Xmx1200m -cp $CP fr.inrialpes.exmo.align.cli.TestGen -u http://www.example.org/mynewtest --alignname myalign.rdf --output biblio.owl -w $RESDIR/outtestdir5 examples/rdf/edu.umbc.ebiquity.publication.owl
if [ -s $RESDIR/outtestdir4/biblio.owl ]; then diff $RESDIR/outtestdir5/biblio.owl $RESDIR/outtestdir4/biblio.owl; else echo error with GEN-OUT1; fi
if [ -s $RESDIR/outtestdir4/myalign.rdf ]; then diff $RESDIR/outtestdir5/myalign.rdf $RESDIR/outtestdir4/myalign.rdf; else echo error with GEN-AL1; fi
diff $RESDIR/outtestdir5/myalign.rdf $RESDIR/smalltest/outtestdir1/refalign.rdf > $RESDIR/outtestdir5/gendiff.txt
if [ ! -s $RESDIR/outtestdir5/gendiff.txt ]; then echo error with GEN-AL2; fi

#-------------------
echo "\t-t,--testset <CLASS>"
mkdir $RESDIR/outtestdir
java -Xmx1200m -cp $CP fr.inrialpes.exmo.align.cli.TestGen -t fr.inrialpes.exmo.align.gen.BenchmarkGenerator -u http://www.example.org/mynewtest -w $RESDIR/outtestdir examples/rdf/edu.umbc.ebiquity.publication.owl
mkdir $RESDIR/outtestdir9
java -Xmx1200m -cp $CP fr.inrialpes.exmo.align.cli.TestGen --testset fr.inrialpes.exmo.align.gen.BenchmarkGenerator --uriprefix http://www.example.org/mynewtest --outdir $RESDIR/outtestdir9 examples/rdf/edu.umbc.ebiquity.publication.owl
if [ -s $RESDIR/outtestdir9/266/onto.rdf ]; then diff $RESDIR/outtestdir/266/onto.rdf $RESDIR/outtestdir9/266/onto.rdf > $RESDIR/outtestdir9/gendiff.txt; else echo error with GEN-TSET1; fi
if [ ! -s $RESDIR/outtestdir9/gendiff.txt ]; then echo error with GEN-TSET2; fi

#-------------------
echo "\t-P,--params <FILE>"
echo "\t(same as Procalign)"

########################################################################
# GroupAlign
########################################################################

# ADJUSTED Because otherwise --alignment below does not work

for i in `cd $RESDIR/smalltest/; ls -d */`
do
sed -i '' "s;<location>http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#</location>;<location>file://$RESDIR/smalltest/gentestempty1/onto1.rdf</location>;" $RESDIR/smalltest/${i}refalign.rdf
sed -i '' "s;<location>http://www.example.org/mynewtest/onto.rdf#</location>;<location>file://$RESDIR/smalltest/${i}onto.rdf</location>;" $RESDIR/smalltest/${i}refalign.rdf
sed -i '' "s;http://ebiquity.umbc.edu/v2.1/ontology/publication.owl#;http://www.example.org/mynewtest/101/onto.rdf#;" $RESDIR/smalltest/${i}refalign.rdf
done

echo "\t\t *** Testing GroupAlign ***"

cp $RESDIR/outtestdir/101/onto.rdf $RESDIR/smalltest/gentestempty1/onto1.rdf
cp $RESDIR/outtestdir/101/onto.rdf $RESDIR/smalltest/gentestempty2/onto1.rdf
cp $RESDIR/outtestdir/101/onto.rdf $RESDIR/smalltest/outtestdir1/onto1.rdf
cp $RESDIR/outtestdir/101/onto.rdf $RESDIR/smalltest/outtestdir2/onto1.rdf
cp $RESDIR/outtestdir/101/onto.rdf $RESDIR/smalltest/outtestdir3/onto1.rdf

#-------------------
echo "\tEMPTY"
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign

#-------------------
echo "\t-z, -zzz"

java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign -z &> $RESDIR/zerr.txt
grep "Unrecognized option: -z" $RESDIR/zerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with GRAL-ERR1; fi
java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign --zzz &> $RESDIR/zzzerr.txt
grep "Unrecognized option: --zzz" $RESDIR/zzzerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with GRAL-ERR2; fi

#-------------------
echo "\t-h, --help"
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign -h &> $RESDIR/gral-h.txt
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign --help &> $RESDIR/gral-help.txt
if [ -s  $RESDIR/gral-h.txt ]; then diff $RESDIR/gral-h.txt $RESDIR/gral-help.txt; else echo error with GRAL-HELP; fi

#-------------------
echo "\t-w,--directory <DIR>"
echo "\t-o <F>, --output <F>"
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign -w $RESDIR/smalltest -o streq1.rdf
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign --directory $RESDIR/smalltest --output streq2.rdf
sed -i '' "s;<alext\:time>[^<]*</alext\:time>;;g" $RESDIR/smalltest/gentestempty1/streq1.rdf
sed -i '' "s;<alext\:time>[^<]*</alext\:time>;;g" $RESDIR/smalltest/gentestempty1/streq2.rdf
if [ -s $RESDIR/smalltest/gentestempty1/streq1.rdf ]; then diff $RESDIR/smalltest/gentestempty1/streq1.rdf $RESDIR/smalltest/gentestempty1/streq2.rdf > $RESDIR/smalltest/diffstreq.txt ; else echo error with GRAL-DIR1; fi
if [ -s $RESDIR/smalltest/diffstreq.txt ]; then echo error with GRAL-DIR2; fi

#-------------------
echo "\t-i <C>, --impl <C>"
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign -i fr.inrialpes.exmo.align.impl.method.EditDistNameAlignment -w $RESDIR/smalltest -o edna1.rdf
sed -i '' "s;<alext\:time>[^<]*</alext\:time>;;g" $RESDIR/smalltest/gentestempty1/edna1.rdf
if [ -s $RESDIR/smalltest/gentestempty1/edna1.rdf ]; then diff $RESDIR/smalltest/gentestempty1/edna1.rdf $RESDIR/smalltest/gentestempty1/streq2.rdf > $RESDIR/smalltest/diffstredna.txt ; else echo error with GRAL-IMPL1; fi
if [ ! -s $RESDIR/smalltest/diffstredna.txt ]; then echo error with GRAL-IMPL2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign --impl fr.inrialpes.exmo.align.impl.method.EditDistNameAlignment -w $RESDIR/smalltest -o edna2.rdf
sed -i '' "s;<alext\:time>[^<]*</alext\:time>;;g" $RESDIR/smalltest/gentestempty1/edna2.rdf
if [ -s $RESDIR/smalltest/gentestempty1/edna2.rdf ]; then diff $RESDIR/smalltest/gentestempty1/edna1.rdf $RESDIR/smalltest/gentestempty1/edna2.rdf > $RESDIR/smalltest/diffedna.txt ; else echo error with GRAL-IMPL3; fi
if [ -s $RESDIR/smalltest/diffedna.txt ]; then echo error with GRAL-IMPL4; fi

#-------------------
echo "\t-Dn=v"

java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign -i fr.inrialpes.exmo.align.impl.method.StringDistAlignment -DstringFunction=levenshteinDistance -w $RESDIR/smalltest -Dtype=\*\* -o lev1.rdf
sed -i '' "s;<alext\:time>[^<]*</alext\:time>;;g" $RESDIR/smalltest/gentestempty1/lev1.rdf
if [ -s $RESDIR/smalltest/gentestempty1/lev1.rdf ]; then diff $RESDIR/smalltest/gentestempty1/edna1.rdf $RESDIR/smalltest/gentestempty1/lev1.rdf > $RESDIR/smalltest/diffstredna.txt ; else echo error with GRAL-DV1; fi
if [ ! -s $RESDIR/smalltest/diffstredna.txt ]; then echo error with GRAL-DV2; fi

#-------------------

echo "\t-a,--alignment <FILE> "
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign -a refalign.rdf -w $RESDIR/smalltest -o streq1a.rdf
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign --alignment refalign.rdf --directory $RESDIR/smalltest --output streq2a.rdf
sed -i '' "s;<alext\:time>[^<]*</alext\:time>;;g" $RESDIR/smalltest/gentestempty1/streq1a.rdf
sed -i '' "s;<alext\:time>[^<]*</alext\:time>;;g" $RESDIR/smalltest/gentestempty1/streq2a.rdf
if [ -s $RESDIR/smalltest/gentestempty1/streq1a.rdf ]; then diff $RESDIR/smalltest/gentestempty1/streq1a.rdf $RESDIR/smalltest/gentestempty1/streq2a.rdf > $RESDIR/smalltest/diffstreqa.txt ; else echo error with GRAL-AL1; fi
if [ -s $RESDIR/smalltest/diffstreqa.txt ]; then echo error with GRAL-AL2; fi

#-------------------
echo "\t-n,--name <URI>"
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign -n file://$RESDIR/outtestdir/101/onto.rdf -w $RESDIR/smalltest -o streq1n.rdf
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign --name file://$RESDIR/outtestdir/101/onto.rdf --directory $RESDIR/smalltest --output streq2n.rdf
sed -i '' "s;<alext\:time>[^<]*</alext\:time>;;g" $RESDIR/smalltest/gentestempty1/streq1n.rdf
sed -i '' "s;<alext\:time>[^<]*</alext\:time>;;g" $RESDIR/smalltest/gentestempty1/streq2n.rdf
if [ -s $RESDIR/smalltest/gentestempty1/streq1n.rdf ]; then diff $RESDIR/smalltest/gentestempty1/streq1n.rdf $RESDIR/smalltest/gentestempty1/streq2n.rdf > $RESDIR/smalltest/diffstreqn.txt ; else echo error with GRAL-NAME1; fi
if [ -s $RESDIR/smalltest/diffstreqn.txt ]; then echo error with GRAL-NAME2; fi

#-------------------
echo "\t-r,--renderer <CLASS>"
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign -r fr.inrialpes.exmo.align.impl.renderer.HTMLRendererVisitor -w $RESDIR/smalltest -o streq1.html
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign --renderer fr.inrialpes.exmo.align.impl.renderer.HTMLRendererVisitor --directory $RESDIR/smalltest --output streq2.html
sed -i '' "s;alext\:time</td><td property=\"align:alext\:time\">[^<]*;;g" $RESDIR/smalltest/gentestempty1/streq1.html
sed -i '' "s;alext\:time</td><td property=\"align:alext\:time\">[^<]*;;g" $RESDIR/smalltest/gentestempty1/streq2.html
if [ -s $RESDIR/smalltest/gentestempty1/streq1.html ]; then diff $RESDIR/smalltest/gentestempty1/streq1.html $RESDIR/smalltest/gentestempty1/streq2.html ; else echo error with GRAL-RENDER1; fi

#-------------------
echo "\t-s,--source <FILE>"
echo "\t-t,--target <FILE>"
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign -s onto.rdf -t onto1.rdf -w $RESDIR/smalltest -o streq1x.rdf
sed -i '' "s;<alext\:time>[^<]*</alext\:time>;;g" $RESDIR/smalltest/gentestempty1/streq1x.rdf
if [ -s $RESDIR/smalltest/gentestempty1/streq1x.rdf ]; then diff $RESDIR/smalltest/gentestempty1/streq1x.rdf $RESDIR/smalltest/gentestempty1/streq2n.rdf > $RESDIR/smalltest/diffstreqxn.txt ; else echo error with GRAL-ST1; fi
if [ ! -s $RESDIR/smalltest/diffstreqxn.txt ]; then echo error with GRAL-ST2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign --source onto.rdf --target onto1.rdf --directory $RESDIR/smalltest --output streq2x.rdf
sed -i '' "s;<alext\:time>[^<]*</alext\:time>;;g" $RESDIR/smalltest/gentestempty1/streq2x.rdf
if [ -s $RESDIR/smalltest/gentestempty1/streq1x.rdf ]; then diff $RESDIR/smalltest/gentestempty1/streq1x.rdf $RESDIR/smalltest/gentestempty1/streq2x.rdf ; else echo error with GRAL-ST3; fi

#-------------------
echo "\t-u,--uriprefix <URI>"

ln -s /tmp/clitest/smalltest /tmp/clitest/preftest

java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign -w $RESDIR/smalltest -u file:///tmp/clitest/preftest -o strpr1.rdf
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign --directory $RESDIR/smalltest --uriprefix file:///tmp/clitest/preftest --output strpr2.rdf
sed -i '' "s;<alext\:time>[^<]*</alext\:time>;;g" $RESDIR/smalltest/gentestempty1/strpr1.rdf
sed -i '' "s;<alext\:time>[^<]*</alext\:time>;;g" $RESDIR/smalltest/gentestempty1/strpr2.rdf
if [ -s $RESDIR/smalltest/gentestempty1/strpr1.rdf ]; then diff $RESDIR/smalltest/gentestempty1/strpr1.rdf $RESDIR/smalltest/gentestempty1/strpr2.rdf > $RESDIR/smalltest/diffpref.txt ; else echo error with GRAL-PR1; fi
if [ -s $RESDIR/smalltest/diffpref.txt ]; then echo error with GRAL-PR2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAlign -w $RESDIR/smalltest -u file:///tmp/clitest/dummy -o strpr3.rdf
if [ -s $RESDIR/smalltest/gentestempty1/strpr3.rdf ]; then echo error with GRAL-PR3; fi

#-------------------
echo "\t-P,--params <FILE>"
echo "\t(same as Procalign)"

########################################################################
# GroupAggreg
########################################################################

echo "\t\t *** Testing GroupAggreg ***"

#-------------------
echo "\tEMPTY"
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg  &> $RESDIR/groupagg-empty.txt
if [ ! -s $RESDIR/groupagg-empty.txt ]; then echo error with GRAG-EMPTY1; fi

#-------------------
echo "\t-z, -zzz"

java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg -z &> $RESDIR/zerr.txt
grep "Unrecognized option: -z" $RESDIR/zerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with GRAG-ERR1; fi
java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg --zzz &> $RESDIR/zzzerr.txt
grep "Unrecognized option: --zzz" $RESDIR/zzzerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with GRAG-ERR2; fi

#-------------------
echo "\t-h, --help"

java -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg -h &> $RESDIR/groupagg-h.txt
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg --help &> $RESDIR/groupagg-help.txt
if [ -s  $RESDIR/groupagg-h.txt ]; then diff $RESDIR/groupagg-h.txt $RESDIR/groupagg-help.txt; else echo error with GRAGAL-HELP; fi

#-------------------
echo "\t-l,--list <FILE>"

#refalign,lev1,streq1

cd $RESDIR/smalltest/
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg -l "lev1,streq1"
if [ ! -s gentestempty1.rdf ]; then echo error with GRAG-LIST1; fi
mv gentestempty1.rdf oldgentestempty1.rdf
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg --list "lev1,streq1"
if [ -s gentestempty1.rdf ]; then diff gentestempty1.rdf oldgentestempty1.rdf ; else echo error with GRAG-LIST2; fi

#-------------------
echo "\t-o <D>, --outputDir <D>"

mkdir $RESDIR/aggreg1a $RESDIR/aggreg1b
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg -l "lev1,streq1" -o $RESDIR/aggreg1a
if [ -s $RESDIR/aggreg1a/gentestempty1.rdf ]; then diff $RESDIR/aggreg1a/gentestempty1.rdf $RESDIR/smalltest/gentestempty1.rdf ; else echo error with GRAG-OUT1; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg --list "lev1,streq1" --output $RESDIR/aggreg1b
if [ -s $RESDIR/aggreg1b/gentestempty1.rdf ]; then diff $RESDIR/aggreg1a/gentestempty1.rdf $RESDIR/aggreg1b/gentestempty1.rdf ; else echo error with GRAG-OUT3; fi

cd $CWD

#-------------------
echo "\t-w,--directory <DIR>"

mkdir $RESDIR/aggreg2a $RESDIR/aggreg2b
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg -l "lev1,streq1" -w $RESDIR/smalltest -o $RESDIR/aggreg2a
if [ -s $RESDIR/aggreg2a/gentestempty1.rdf ]; then diff $RESDIR/aggreg1a/gentestempty1.rdf $RESDIR/aggreg2a/gentestempty1.rdf > $RESDIR/smalltest/diffgrag-w1.txt ; else echo error with GRAG-DIR1; fi
if [ -s $RESDIR/smalltest/diffgrag-w1.txt ]; then echo error with GRAG-DIR2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg --list "lev1,streq1" --directory $RESDIR/smalltest --output $RESDIR/aggreg2b
if [ -s $RESDIR/aggreg2b/gentestempty1.rdf ]; then diff $RESDIR/aggreg2a/gentestempty1.rdf $RESDIR/aggreg2b/gentestempty1.rdf ; else echo error with GRAG-DIR3; fi

#-------------------
echo "\t-m,--aggmethod <METHOD>"
# -m min/max

mkdir $RESDIR/aggreg3a $RESDIR/aggreg3b
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg -l "lev1,streq1" -w $RESDIR/smalltest -o $RESDIR/aggreg3a -m max
if [ -s $RESDIR/aggreg3a/gentestempty1.rdf ]; then diff $RESDIR/aggreg1a/gentestempty1.rdf $RESDIR/aggreg3a/gentestempty1.rdf > $RESDIR/smalltest/diffgrag-m1.txt ; else echo error with GRAG-METH1; fi
if [ ! -s $RESDIR/smalltest/diffgrag-m1.txt ]; then echo error with GRAG-METH2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg --list "lev1,streq1" --directory $RESDIR/smalltest --output $RESDIR/aggreg3b --aggmethod max
if [ -s $RESDIR/aggreg3b/gentestempty1.rdf ]; then diff $RESDIR/aggreg3a/gentestempty1.rdf $RESDIR/aggreg3b/gentestempty1.rdf ; else echo error with GRAG-METH3; fi

#-------------------
echo "\t-t,--threshold <DOUBLE>"

mkdir $RESDIR/aggreg4a $RESDIR/aggreg4b
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg -l "lev1,streq1" -w $RESDIR/smalltest -o $RESDIR/aggreg4a -m max -t 0.6
if [ -s $RESDIR/aggreg4a/gentestempty1.rdf ]; then diff $RESDIR/aggreg1a/gentestempty1.rdf $RESDIR/aggreg4a/gentestempty1.rdf > $RESDIR/smalltest/diffgrag-t1.txt ; else echo error with GRAG-THRES1; fi
if [ ! -s $RESDIR/smalltest/diffgrag-t1.txt ]; then echo error with GRAG-THRES2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg --list "lev1,streq1" --directory $RESDIR/smalltest --output $RESDIR/aggreg4b --aggmethod max --threshold 0.6
if [ -s $RESDIR/aggreg4b/gentestempty1.rdf ]; then diff $RESDIR/aggreg4a/gentestempty1.rdf $RESDIR/aggreg4b/gentestempty1.rdf ; else echo error with GRAG-THRES3; fi

#-------------------
echo "\t-T,--cutmethod <METHOD>"
# -c hard (with default 0.)

mkdir $RESDIR/aggreg5a $RESDIR/aggreg5b
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg -l "lev1,streq1" -w $RESDIR/smalltest -o $RESDIR/aggreg5a -m max -t 0.6 -T perc
if [ -s $RESDIR/aggreg5a/gentestempty1.rdf ]; then diff $RESDIR/aggreg4a/gentestempty1.rdf $RESDIR/aggreg5a/gentestempty1.rdf > $RESDIR/smalltest/diffgrag-tt1.txt ; else echo error with GRAG-CUT1; fi
if [ ! -s $RESDIR/smalltest/diffgrag-tt1.txt ]; then echo error with GRAG-CUT2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupAggreg --list "lev1,streq1" --directory $RESDIR/smalltest --output $RESDIR/aggreg5b --aggmethod max --threshold 0.6 --cutmethod perc
if [ -s $RESDIR/aggreg5b/gentestempty1.rdf ]; then diff $RESDIR/aggreg5a/gentestempty1.rdf $RESDIR/aggreg5b/gentestempty1.rdf ; else echo error with GRAG-CUT3; fi

#-------------------
echo "\t-Dn=v"
echo "\t(same as Procalign; useless: no parameters involved so far)"

#-------------------
echo "\t-P,--params <FILE>"
echo "\t(same as Procalign; useless: no parameters involved so far)"


########################################################################
# GroupEval
########################################################################

echo "\t\t *** Testing GroupEval ***"

#-------------------
echo "\t-z, -zzz"

java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.GroupEval -z &> $RESDIR/zerr.txt
grep "Unrecognized option: -z" $RESDIR/zerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with GREV-ERR1; fi
java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.GroupEval --zzz &> $RESDIR/zzzerr.txt
grep "Unrecognized option: --zzz" $RESDIR/zzzerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with GREV-ERR2; fi

#-------------------
echo "\t-h, --help"

java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval -h &> $RESDIR/groupeval-h.txt
java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval --help &> $RESDIR/groupeval-help.txt
if [ -s  $RESDIR/groupeval-h.txt ]; then diff $RESDIR/groupeval-h.txt $RESDIR/groupeval-help.txt; else echo error with GREVAL-HELP; fi

#-------------------
echo "\t-l,--list <FILE>"

cd $RESDIR/smalltest
java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval -l "refalign,lev1,streq1" > groupeval-l1.html
if [ ! -s groupeval-l1.html ]; then echo error with GREV-LIST1; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval --list "refalign,lev1,streq1" > groupeval-l2.html
if [ -s groupeval-l2.html ]; then diff groupeval-l1.html groupeval-l2.html ; else echo error with GREV-LIST2; fi
cd $CWD

#-------------------
echo "\t-w,--directory <DIR>"

java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest > $RESDIR/groupeval-w1.html
if [ -s $RESDIR/groupeval-w1.html ]; then diff $RESDIR/groupeval-w1.html $RESDIR/smalltest/groupeval-l1.html  > $RESDIR/smalltest/diffgrev-w1.txt ; else echo error with GREV-DIR1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-w1.txt ]; then echo error with GREV-DIR2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" --directory $RESDIR/smalltest > $RESDIR/groupeval-w2.html
if [ -s $RESDIR/groupeval-w2.html ]; then diff $RESDIR/groupeval-w1.html $RESDIR/groupeval-w2.html ; else echo error with GREV-DIR3; fi

#-------------------
echo "\t-o <F>, --output <F>"

java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest -o $RESDIR/groupeval-o1.html
if [ -s $RESDIR/groupeval-o1.html ]; then diff $RESDIR/groupeval-o1.html $RESDIR/groupeval-w1.html ; else echo error with GREV-OUT1; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest --output $RESDIR/groupeval-o2.html
if [ -s $RESDIR/groupeval-o2.html ]; then diff $RESDIR/groupeval-o1.html $RESDIR/groupeval-o2.html ; else echo error with GREV-OUT3; fi

#-------------------
echo "\t-f,--format <MEAS>"

java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval -f f -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest > $RESDIR/groupeval-f1.html
if [ -s $RESDIR/groupeval-f1.html ]; then diff $RESDIR/groupeval-w1.html $RESDIR/groupeval-f1.html  > $RESDIR/smalltest/diffgrev-f1.txt ; else echo error with GREV-FORM1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-f1.txt ]; then echo error with GREV-FORM2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval --format f -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest > $RESDIR/groupeval-f2.html
if [ -s $RESDIR/groupeval-f2.html ]; then diff $RESDIR/groupeval-f1.html $RESDIR/groupeval-f2.html ; else echo error with GREV-FORM3; fi

#-------------------
echo "\t-c,--color (<COLOR>)"

java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval -c -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest > $RESDIR/groupeval-c1.html
if [ -s $RESDIR/groupeval-c1.html ]; then diff $RESDIR/groupeval-w1.html $RESDIR/groupeval-c1.html  > $RESDIR/smalltest/diffgrev-c1.txt ; else echo error with GREV-COL1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-c1.txt ]; then echo error with GREV-COL2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval --color -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest > $RESDIR/groupeval-c2.html
if [ -s $RESDIR/groupeval-c2.html ]; then diff $RESDIR/groupeval-c1.html $RESDIR/groupeval-c2.html ; else echo error with GREV-COL3; fi

#-------------------
echo "\t-r,--reference <FILE>"
java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval -r lev1.rdf -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest -o $RESDIR/groupeval-r1.html
if [ -s $RESDIR/groupeval-r1.html ]; then diff $RESDIR/groupeval-w1.html $RESDIR/groupeval-r1.html > $RESDIR/smalltest/diffgrev-r1.txt ; else echo error with GREV-REF1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-r1.txt ]; then echo error with GREV-REF2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval --reference lev1.rdf -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest -o $RESDIR/groupeval-r2.html
if [ -s $RESDIR/groupeval-r2.html ]; then diff $RESDIR/groupeval-r1.html $RESDIR/groupeval-r2.html ; else echo error with GREV-REF3; fi

#-------------------
echo "\t-t,--type <TYPE>         Output TYPE (html|xml|tex|ascii|triangle)"
java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval -t triangle -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest -o $RESDIR/groupeval-ttr1.tex
if [ -s $RESDIR/groupeval-ttr1.tex ]; then diff $RESDIR/groupeval-w1.html $RESDIR/groupeval-ttr1.tex > $RESDIR/smalltest/diffgrev-t1.txt ; else echo error with GREV-TYP1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-t1.txt ]; then echo error with GREV-TYP2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupEval --type triangle -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest -o $RESDIR/groupeval-ttr2.tex
if [ -s $RESDIR/groupeval-ttr2.tex ]; then diff $RESDIR/groupeval-ttr1.tex $RESDIR/groupeval-ttr2.tex ; else echo error with GREV-TYP3; fi

#-------------------
echo "\t-Dn=v"
echo "\t(same as Procalign; useless: no parameters involved so far)"

#-------------------
echo "\t-P,--params <FILE>"
echo "\t(same as Procalign; useless: no parameters involved so far)"

########################################################################
# WGroupEval
########################################################################

echo "\t\t *** Testing WGroupEval ***"

#-------------------
echo "\t-z, -zzz"

java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval -z &> $RESDIR/zerr.txt
grep "Unrecognized option: -z" $RESDIR/zerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with WGREV-ERR1; fi
java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval --zzz &> $RESDIR/zzzerr.txt
grep "Unrecognized option: --zzz" $RESDIR/zzzerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with WGREV-ERR2; fi

#-------------------
echo "\t-h, --help"

java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval -h &> $RESDIR/wgrev-h.txt
java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval --help &> $RESDIR/wgrev-help.txt
if [ -s  $RESDIR/wgrev-h.txt ]; then diff $RESDIR/wgrev-h.txt $RESDIR/wgrev-help.txt; else echo error with WGREV-HELP; fi

#-------------------
echo "\t-l,--list <FILE>"

cd $RESDIR/smalltest
java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval -l "refalign,lev1,streq1" > wgrev-l1.html
if [ ! -s wgrev-l1.html ]; then echo error with WGREV-LIST1; fi
java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval --list "refalign,lev1,streq1" > wgrev-l2.html
if [ -s wgrev-l2.html ]; then diff wgrev-l1.html wgrev-l2.html ; else echo error with WGREV-LIST2; fi
cd $CWD

#-------------------
echo "\t-w,--directory <DIR>"

java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest > $RESDIR/wgrev-w1.html
if [ -s $RESDIR/wgrev-w1.html ]; then diff $RESDIR/wgrev-w1.html $RESDIR/smalltest/wgrev-l1.html  > $RESDIR/smalltest/diffgrev-w1.txt ; else echo error with WGREV-DIR1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-w1.txt ]; then echo error with WGREV-DIR2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" --directory $RESDIR/smalltest > $RESDIR/wgrev-w2.html
if [ -s $RESDIR/wgrev-w2.html ]; then diff $RESDIR/wgrev-w1.html $RESDIR/wgrev-w2.html ; else echo error with WGREV-DIR3; fi

#-------------------
echo "\t-o <F>, --output <F>"

java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest -o $RESDIR/wgrev-o1.html
if [ -s $RESDIR/wgrev-o1.html ]; then diff $RESDIR/wgrev-o1.html $RESDIR/wgrev-w1.html ; else echo error with WGREV-OUT1; fi
java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest --output $RESDIR/wgrev-o2.html
if [ -s $RESDIR/wgrev-o2.html ]; then diff $RESDIR/wgrev-o1.html $RESDIR/wgrev-o2.html ; else echo error with WGREV-OUT3; fi

#-------------------
echo "\t-f,--format <MEAS>"

java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval -f f -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest > $RESDIR/wgrev-f1.html
if [ -s $RESDIR/wgrev-f1.html ]; then diff $RESDIR/wgrev-w1.html $RESDIR/wgrev-f1.html  > $RESDIR/smalltest/diffgrev-f1.txt ; else echo error with WGREV-FORM1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-f1.txt ]; then echo error with WGREV-FORM2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval --format f -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest > $RESDIR/wgrev-f2.html
if [ -s $RESDIR/wgrev-f2.html ]; then diff $RESDIR/wgrev-f1.html $RESDIR/wgrev-f2.html ; else echo error with WGREV-FORM3; fi

#-------------------
echo "\t-c,--color (<COLOR>)"

java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval -c -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest > $RESDIR/wgrev-c1.html
if [ -s $RESDIR/wgrev-c1.html ]; then diff $RESDIR/wgrev-w1.html $RESDIR/wgrev-c1.html  > $RESDIR/smalltest/diffgrev-c1.txt ; else echo error with WGREV-COL1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-c1.txt ]; then echo error with WGREV-COL2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval --color -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest > $RESDIR/wgrev-c2.html
if [ -s $RESDIR/wgrev-c2.html ]; then diff $RESDIR/wgrev-c1.html $RESDIR/wgrev-c2.html ; else echo error with WGREV-COL3; fi

#-------------------
echo "\t-r,--reference <FILE>"
java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval -r lev1.rdf -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest -o $RESDIR/wgrev-r1.html
if [ -s $RESDIR/wgrev-r1.html ]; then diff $RESDIR/wgrev-w1.html $RESDIR/wgrev-r1.html > $RESDIR/smalltest/diffgrev-r1.txt ; else echo error with WGREV-REF1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-r1.txt ]; then echo error with WGREV-REF2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval --reference lev1.rdf -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest -o $RESDIR/wgrev-r2.html
if [ -s $RESDIR/wgrev-r2.html ]; then diff $RESDIR/wgrev-r1.html $RESDIR/wgrev-r2.html ; else echo error with WGREV-REF3; fi

#-------------------
echo "\t-t,--type <TYPE>         Output TYPE (html|xml|tex|ascii|triangle)"
java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval -t triangle -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest -o $RESDIR/wgrev-ttr1.tex
if [ -s $RESDIR/wgrev-ttr1.tex ]; then diff $RESDIR/wgrev-w1.html $RESDIR/wgrev-ttr1.tex > $RESDIR/smalltest/diffgrev-t1.txt ; else echo error with WGREV-TYP1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-t1.txt ]; then echo error with WGREV-TYP2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.WGroupEval --type triangle -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest -o $RESDIR/wgrev-ttr2.tex
if [ -s $RESDIR/wgrev-ttr2.tex ]; then diff $RESDIR/wgrev-ttr1.tex $RESDIR/wgrev-ttr2.tex ; else echo error with WGREV-TYP3; fi

#-------------------
echo "\t-Dn=v"
echo "\t(same as Procalign; useless: no parameters involved so far)"

#-------------------
echo "\t-P,--params <FILE>"
echo "\t(same as Procalign; useless: no parameters involved so far)"

########################################################################
# ExtGroupEval
########################################################################

echo "\t\t *** Testing ExtGroupEval ***"

#-------------------
echo "\t-z, -zzz"

java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval -z &> $RESDIR/zerr.txt
grep "Unrecognized option: -z" $RESDIR/zerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with EXTGRPEV-ERR1; fi
java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval --zzz &> $RESDIR/zzzerr.txt
grep "Unrecognized option: --zzz" $RESDIR/zzzerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with EXTGRPEV-ERR2; fi

#-------------------
echo "\t-h, --help"

java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval -h &> $RESDIR/extgrpev-h.txt
java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval --help &> $RESDIR/extgrpev-help.txt
if [ -s  $RESDIR/extgrpev-h.txt ]; then diff $RESDIR/extgrpev-h.txt $RESDIR/extgrpev-help.txt; else echo error with EXTGRPEV-HELP; fi

#-------------------
echo "\t-l,--list <FILE>"

cd $RESDIR/smalltest
java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval -l "refalign,lev1,streq1" > extgrpev-l1.html
if [ ! -s extgrpev-l1.html ]; then echo error with EXTGRPEV-LIST1; fi
java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval --list "refalign,lev1,streq1" > extgrpev-l2.html
if [ -s extgrpev-l2.html ]; then diff extgrpev-l1.html extgrpev-l2.html ; else echo error with EXTGRPEV-LIST2; fi
cd $CWD

#-------------------
echo "\t-w,--directory <DIR>"

java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest > $RESDIR/extgrpev-w1.html
if [ -s $RESDIR/extgrpev-w1.html ]; then diff $RESDIR/extgrpev-w1.html $RESDIR/smalltest/extgrpev-l1.html  > $RESDIR/smalltest/diffgrev-w1.txt ; else echo error with EXTGRPEV-DIR1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-w1.txt ]; then echo error with EXTGRPEV-DIR2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" --directory $RESDIR/smalltest > $RESDIR/extgrpev-w2.html
if [ -s $RESDIR/extgrpev-w2.html ]; then diff $RESDIR/extgrpev-w1.html $RESDIR/extgrpev-w2.html ; else echo error with EXTGRPEV-DIR3; fi

#-------------------
echo "\t-o <F>, --output <F>"

java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest -o $RESDIR/extgrpev-o1.html
if [ -s $RESDIR/extgrpev-o1.html ]; then diff $RESDIR/extgrpev-o1.html $RESDIR/extgrpev-w1.html ; else echo error with EXTGRPEV-OUT1; fi
java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest --output $RESDIR/extgrpev-o2.html
if [ -s $RESDIR/extgrpev-o2.html ]; then diff $RESDIR/extgrpev-o1.html $RESDIR/extgrpev-o2.html ; else echo error with EXTGRPEV-OUT3; fi

#-------------------
echo "\t-f,--format <MEAS>"

java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval -f f -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest > $RESDIR/extgrpev-f1.html
if [ -s $RESDIR/extgrpev-f1.html ]; then diff $RESDIR/extgrpev-w1.html $RESDIR/extgrpev-f1.html  > $RESDIR/smalltest/diffgrev-f1.txt ; else echo error with EXTGRPEV-FORM1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-f1.txt ]; then echo error with EXTGRPEV-FORM2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval --format f -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest > $RESDIR/extgrpev-f2.html
if [ -s $RESDIR/extgrpev-f2.html ]; then diff $RESDIR/extgrpev-f1.html $RESDIR/extgrpev-f2.html ; else echo error with EXTGRPEV-FORM3; fi

#-------------------
echo "\t-c,--color (<COLOR>)"

java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval -c -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest > $RESDIR/extgrpev-c1.html
if [ -s $RESDIR/extgrpev-c1.html ]; then diff $RESDIR/extgrpev-w1.html $RESDIR/extgrpev-c1.html  > $RESDIR/smalltest/diffgrev-c1.txt ; else echo error with EXTGRPEV-COL1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-c1.txt ]; then echo error with EXTGRPEV-COL2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval --color -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest > $RESDIR/extgrpev-c2.html
if [ -s $RESDIR/extgrpev-c2.html ]; then diff $RESDIR/extgrpev-c1.html $RESDIR/extgrpev-c2.html ; else echo error with EXTGRPEV-COL3; fi

#-------------------
echo "\t-r,--reference <FILE>"
java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval -r lev1.rdf -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest -o $RESDIR/extgrpev-r1.html
if [ -s $RESDIR/extgrpev-r1.html ]; then diff $RESDIR/extgrpev-o1.html $RESDIR/extgrpev-r1.html > $RESDIR/smalltest/diffgrev-r1.txt ; else echo error with EXTGRPEV-REF1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-r1.txt ]; then echo error with EXTGRPEV-REF2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval --reference lev1.rdf -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest -o $RESDIR/extgrpev-r2.html
if [ -s $RESDIR/extgrpev-r2.html ]; then diff $RESDIR/extgrpev-r1.html $RESDIR/extgrpev-r2.html ; else echo error with EXTGRPEV-REF3; fi

#-------------------
echo "\t-t,--type <TYPE>         Output TYPE (html|xml|tex|ascii|triangle)"
java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval -t triangle -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest -o $RESDIR/extgrpev-ttr1.tex
if [ -s $RESDIR/extgrpev-ttr1.tex ]; then diff $RESDIR/extgrpev-o1.html $RESDIR/extgrpev-ttr1.tex > $RESDIR/smalltest/diffgrev-t1.txt ; else echo error with EXTGRPEV-TYP1; fi
# Useless: only html is available at the moment, so the diff is empty
#if [ ! -s $RESDIR/smalltest/diffgrev-t1.txt ]; then echo error with EXTGRPEV-TYP2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.ExtGroupEval --type triangle -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" -w $RESDIR/smalltest -o $RESDIR/extgrpev-ttr2.tex
if [ -s $RESDIR/extgrpev-ttr2.tex ]; then diff $RESDIR/extgrpev-ttr1.tex $RESDIR/extgrpev-ttr2.tex ; else echo error with EXTGRPEV-TYP3; fi

#-------------------
echo "\t-Dn=v"
echo "\t(same as Procalign; useless: no parameters involved so far)"

#-------------------
echo "\t-P,--params <FILE>"
echo "\t(same as Procalign; useless: no parameters involved so far)"

########################################################################
# GroupOutput
########################################################################

echo "\t\t *** Testing GroupOutput ***"

#-------------------
echo "\t-z, -zzz"

java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput -z &> $RESDIR/zerr.txt
grep "Unrecognized option: -z" $RESDIR/zerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with GROUT-ERR1; fi
java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput --zzz &> $RESDIR/zzzerr.txt
grep "Unrecognized option: --zzz" $RESDIR/zzzerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with GROUT-ERR2; fi

#-------------------
echo "\t-h, --help"
java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput -h &> $RESDIR/grout-h.txt
java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput --help &> $RESDIR/grout-help.txt
if [ -s  $RESDIR/grout-h.txt ]; then diff $RESDIR/grout-h.txt $RESDIR/grout-help.txt; else echo error with $RESDIR/grout-h.txt; fi
diff $RESDIR/grout-h.txt $RESDIR/grout-help.txt

#-------------------
echo "\t-l,--list <FILE>"

cd $RESDIR/smalltest
java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" > grout-l1.tex
if [ ! -s grout-l1.tex ]; then echo error with GROUT-LIST1; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput --list "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" > grout-l2.tex
if [ -s grout-l2.tex ]; then diff grout-l1.tex grout-l2.tex ; else echo error with GROUT-LIST2; fi
cd $CWD

#-------------------
echo "\t-o <F>, --output <F>"

cd $RESDIR/smalltest
java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput -l "refalign,edna1,streq1,lev1" -o $RESDIR/grout-o1.tex
if [ ! -s $RESDIR/grout-o1.tex ]; then echo error with GROUT-OUT1; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput -l "refalign,edna1,streq1,lev1" --output $RESDIR/grout-o2.tex
if [ -s $RESDIR/grout-o2.tex ]; then diff $RESDIR/grout-o1.tex $RESDIR/grout-o2.tex ; else echo error with GROUT-OUT3; fi
cd $CWD

#-------------------
echo "\t-w,--directory <DIR>"

java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput -l "refalign,edna1,streq1,lev1" -w $RESDIR/smalltest -o $RESDIR/grout-w1.tex
if [ -s $RESDIR/grout-w1.tex ]; then diff $RESDIR/grout-o1.tex $RESDIR/grout-w1.tex ; else echo error with GROUT-DIR1; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput -l "refalign,edna1,streq1,lev1" --directory $RESDIR/smalltest --output $RESDIR/grout-w2.tex
if [ -s $RESDIR/grout-w2.tex ]; then diff $RESDIR/grout-o1.tex $RESDIR/grout-o2.tex ; else echo error with GROUT-DIR3; fi

#-------------------
echo "\t-v,--value"

java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput -l "refalign,edna1,streq1,lev1" -v -w $RESDIR/smalltest -o $RESDIR/grout-v1.tex
if [ -s $RESDIR/grout-v1.tex ]; then diff $RESDIR/grout-o1.tex $RESDIR/grout-v1.tex > $RESDIR/smalltest/diffgrout-v1.txt; else echo error with GROUT-VAL1; fi
if [ ! -s $RESDIR/smalltest/diffgrout-v1.txt ]; then echo error with GROUT-VAL2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput -l "refalign,edna1,streq1,lev1" --values --directory $RESDIR/smalltest --output $RESDIR/grout-v2.tex
if [ -s $RESDIR/grout-v2.tex ]; then diff $RESDIR/grout-v1.tex $RESDIR/grout-v2.tex ; else echo error with GROUT-VAL3; fi

#-------------------
echo "\t-e,--labels"

java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput -l "refalign,edna1,streq1,lev1" -e -w $RESDIR/smalltest -o $RESDIR/grout-e1.tex
if [ -s $RESDIR/grout-e1.tex ]; then diff $RESDIR/grout-o1.tex $RESDIR/grout-e1.tex > $RESDIR/smalltest/diffgrout-e1.txt; else echo error with GROUT-LAB1; fi
if [ ! -s $RESDIR/smalltest/diffgrout-e1.txt ]; then echo error with GROUT-LAB2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput -l "refalign,edna1,streq1,lev1" --labels --directory $RESDIR/smalltest --output $RESDIR/grout-e2.tex
if [ -s $RESDIR/grout-e2.tex ]; then diff $RESDIR/grout-e1.tex $RESDIR/grout-e2.tex ; else echo error with GROUT-LAB3; fi

#-------------------
echo "\t-c,--color (<COLOR>)"

java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput -c pink -l "refalign,edna1,streq1,lev1" -w $RESDIR/smalltest > $RESDIR/grout-c1.tex
if [ -s $RESDIR/grout-c1.tex ]; then diff $RESDIR/grout-o1.tex $RESDIR/grout-c1.tex  > $RESDIR/smalltest/diffgrev-c1.txt ; else echo error with GROUT-COL1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-c1.txt ]; then echo error with GROUT-COL2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput --color pink -l "refalign,edna1,streq1,lev1" -w $RESDIR/smalltest > $RESDIR/grout-c2.tex
if [ -s $RESDIR/grout-c2.tex ]; then diff $RESDIR/grout-c1.tex $RESDIR/grout-c2.tex ; else echo error with GROUT-COL3; fi

#-------------------
echo "\t-f,--format <MEAS>"

java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput -f r -l "refalign,edna1,streq1,lev1" -w $RESDIR/smalltest > $RESDIR/grout-f1.tex
if [ -s $RESDIR/grout-f1.tex ]; then diff $RESDIR/grout-w1.tex $RESDIR/grout-f1.tex  > $RESDIR/smalltest/diffgrev-f1.txt ; else echo error with GROUT-FORM1; fi
# These are always the same values, so no change with these parameters...
#if [ ! -s $RESDIR/smalltest/diffgrev-f1.txt ]; then echo error with GROUT-FORM2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput --format r -l "refalign,edna1,streq1,lev1" -w $RESDIR/smalltest > $RESDIR/grout-f2.tex
if [ -s $RESDIR/grout-f2.tex ]; then diff $RESDIR/grout-f1.tex $RESDIR/grout-f2.tex ; else echo error with GROUT-FORM3; fi

#-------------------
echo "\t-t,--type <TYPE>         Output TYPE (tex|html) / only tex available in fact"

java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput -t html -l "refalign,edna1,streq1,lev1" -w $RESDIR/smalltest -o $RESDIR/grout-t1.tex
# The results are necessary empty with html
#if [ -s $RESDIR/grout-t1.tex ]; then diff $RESDIR/grout-o1.tex $RESDIR/grout-t1.tex > $RESDIR/smalltest/diffgrev-t1.txt ; else echo error with GROUT-TYP1; fi
#if [ ! -s $RESDIR/smalltest/diffgrev-t1.txt ]; then echo error with GROUT-TYP2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GroupOutput --type html -l "refalign,edna1,streq1,lev1" -w $RESDIR/smalltest -o $RESDIR/grout-t2.tex
#if [ -s $RESDIR/grout-t2.tex ]; then diff $RESDIR/grout-t1.tex $RESDIR/grout-t2.tex ; else echo error with GROUT-TYP3; fi

#-------------------
echo "\t-Dn=v"
echo "\t(same as Procalign; useless: no parameters involved so far)"

#-------------------
echo "\t-P,--params <FILE>"
echo "\t(same as Procalign; useless: no parameters involved so far)"

########################################################################
# GenPlot
########################################################################

echo "\t\t *** Testing GenPlot ***"

#-------------------
echo "\t-h, --help"
java -cp $CP fr.inrialpes.exmo.align.cli.GenPlot -h &> $RESDIR/genplot-h.txt
java -cp $CP fr.inrialpes.exmo.align.cli.GenPlot --help &> $RESDIR/genplot-help.txt
if [ -s  $RESDIR/genplot-h.txt ]; then diff $RESDIR/genplot-h.txt $RESDIR/genplot-help.txt; else echo error with $RESDIR/genplot-h.txt; fi

#-------------------
echo "\t-z,--zzz"

java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.GenPlot -z &> $RESDIR/zerr.txt
grep "Unrecognized option: -z" $RESDIR/zerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with GENPLOT-ERR1; fi
java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.GenPlot --zzz &> $RESDIR/zzzerr.txt
grep "Unrecognized option: --zzz" $RESDIR/zzzerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with GENPLOT-ERR2; fi

#-------------------
echo "\t-l,--list <FILE>"

cd $RESDIR/smalltest
java -cp $CP fr.inrialpes.exmo.align.cli.GenPlot -l "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" > genplot-l1.tex
if [ ! -s genplot-l1.tex ]; then echo error with GENPLOT-LIST1; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GenPlot --list "refalign,edna1,streq1,streq1n,streq1x,streq2,streq2n,streq2x,lev1" > genplot-l2.tex
if [ -s genplot-l2.tex ]; then diff genplot-l1.tex genplot-l2.tex ; else echo error with GENPLOT-LIST2; fi
cd $CWD

#-------------------
echo "\t-o <F>, --output <F>"

cd $RESDIR/smalltest
java -cp $CP fr.inrialpes.exmo.align.cli.GenPlot -l "refalign,edna1,streq1,lev1" -o $RESDIR/genplot-o1.tex
if [ ! -s $RESDIR/genplot-o1.tex ]; then echo error with GENPLOT-OUT1; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GenPlot -l "refalign,edna1,streq1,lev1" --output $RESDIR/genplot-o2.tex
if [ -s $RESDIR/genplot-o2.tex ]; then diff $RESDIR/genplot-o1.tex $RESDIR/genplot-o2.tex ; else echo error with GENPLOT-OUT3; fi
cd $CWD

#-------------------
echo "\t-w,--directory <DIR>"

java -cp $CP fr.inrialpes.exmo.align.cli.GenPlot -l "refalign,edna1,streq1,lev1" -w $RESDIR/smalltest -o $RESDIR/genplot-w1.tex
if [ -s $RESDIR/genplot-w1.tex ]; then diff $RESDIR/genplot-o1.tex $RESDIR/genplot-w1.tex ; else echo error with GENPLOT-DIR1; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GenPlot -l "refalign,edna1,streq1,lev1" --directory $RESDIR/smalltest --output $RESDIR/genplot-w2.tex
if [ -s $RESDIR/genplot-w2.tex ]; then diff $RESDIR/genplot-o1.tex $RESDIR/genplot-o2.tex ; else echo error with GENPLOT-DIR3; fi

#-------------------
echo "\t-e,--evaluator <CLASS>"

mv $RESDIR/lev1.table $RESDIR/lev1-init.table
java -cp $CP fr.inrialpes.exmo.align.cli.GenPlot -e fr.inrialpes.exmo.align.impl.eval.WeightedPREvaluator -l "refalign,edna1,streq1,lev1" -w $RESDIR/smalltest -o $RESDIR/genplot-e1.tex
if [ -s $RESDIR/lev1.table ]; then diff $RESDIR/lev1-init.table $RESDIR/lev1.table > $RESDIR/smalltest/diffgrev-e1.txt; else echo error with GENPLOT-EVA1; fi
echo "--> This is not used yet!"
if [ ! -s $RESDIR/smalltest/diffgrev-e1.txt ]; then echo error with GENPLOT-EVA2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GenPlot --evaluator fr.inrialpes.exmo.align.impl.eval.WeightedPREvaluator -l "refalign,edna1,streq1,lev1" -w $RESDIR/smalltest -o $RESDIR/genplot-e2.tex
if [ -s $RESDIR/genplot-e2.tex ]; then diff $RESDIR/genplot-e1.tex $RESDIR/genplot-e2.tex ; else echo error with GENPLOT-EVA3; fi

#-------------------
echo "\t-g,--grapher <CLASS> "

java -cp $CP fr.inrialpes.exmo.align.cli.GenPlot -g fr.inrialpes.exmo.align.impl.eval.ROCCurveEvaluator -l "refalign,edna1,streq1,lev1" -w $RESDIR/smalltest -o $RESDIR/genplot-g1.tex
if [ -s $RESDIR/genplot-g1.tex ]; then diff $RESDIR/genplot-o1.tex $RESDIR/genplot-g1.tex > $RESDIR/smalltest/diffgrev-g1.txt ; else echo error with GENPLOT-GRA1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-g1.txt ]; then echo error with GENPLOT-GRA2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GenPlot --grapher fr.inrialpes.exmo.align.impl.eval.ROCCurveEvaluator -l "refalign,edna1,streq1,lev1" -w $RESDIR/smalltest -o $RESDIR/genplot-g2.tex
if [ -s $RESDIR/genplot-g2.tex ]; then diff $RESDIR/genplot-g1.tex $RESDIR/genplot-g2.tex ; else echo error with GENPLOT-GRA3; fi

#-------------------
echo "\t-t,--type <TYPE>         Output TYPE (tsv|tex|html(|xml))"

java -cp $CP fr.inrialpes.exmo.align.cli.GenPlot -t html -l "refalign,edna1,streq1,lev1" -w $RESDIR/smalltest -o $RESDIR/genplot-t1.html
if [ -s $RESDIR/genplot-t1.html ]; then diff $RESDIR/genplot-o1.tex $RESDIR/genplot-t1.html > $RESDIR/smalltest/diffgrev-t1.txt ; else echo error with GENPLOT-TYP1; fi
if [ ! -s $RESDIR/smalltest/diffgrev-t1.txt ]; then echo error with GENPLOT-TYP2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.GenPlot --type html -l "refalign,edna1,streq1,lev1" -w $RESDIR/smalltest -o $RESDIR/genplot-t2.html
if [ -s $RESDIR/genplot-t2.html ]; then diff $RESDIR/genplot-t1.html $RESDIR/genplot-t2.html ; else echo error with GENPLOT-TYP3; fi

#-------------------
echo "\t-Dn=v"
echo "\t(same as Procalign)"

#-------------------
echo "\t-P,--params <FILE>"
echo "\t(same as Procalign)"


########################################################################
# TransformQuery
########################################################################

echo "\t\t *** Testing TransformQuery ***"

#-------------------
echo "\t-z, -zzz"

java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery -z &> $RESDIR/zerr.txt
grep "Unrecognized option: -z" $RESDIR/zerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with TRANSQ-ERR1; fi
java -Dlog.level=INFO -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery --zzz &> $RESDIR/zzzerr.txt
grep "Unrecognized option: --zzz" $RESDIR/zzzerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with TRANSQ-ERR2; fi

#-------------------
echo "\t-h, --help"
java -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery -h &> $RESDIR/queryt-h.txt
java -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery --help &> $RESDIR/queryt-help.txt
if [ -s  $RESDIR/queryt-h.txt ]; then diff $RESDIR/queryt-h.txt $RESDIR/queryt-help.txt; else echo error with $RESDIR/queryt-h.txt; fi

#-------------------
echo "\tno-op"
java -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery &> $RESDIR/queryt-noop.rdf <<EOF
PREFIX dt:   <http://example.org/datatype#> .
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
PREFIX xmls:  <http://www.w3.org/2001/XMLSchema#> .
PREFIX foaf:  <http://xmlns.com/foaf/0.1/> .
PREFIX onto1: <http://www.example.org/ontology1#> .

SELECT *
FROM XXX
WHERE {
    ?X rdf:type <http://www.example.org/ontology1#reviewedarticle>.
    ?X rdf:type onto1:reviewedarticle .
  }
EOF
if [ ! -s  $RESDIR/queryt-noop.rdf ]; then echo error with TRANSQ-NOOP; fi

#-------------------
echo "\t<QUERY>"
java -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery 'PREFIX dt:   <http://example.org/datatype#> .
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
PREFIX xmls:  <http://www.w3.org/2001/XMLSchema#> .
PREFIX foaf:  <http://xmlns.com/foaf/0.1/> .
PREFIX onto1: <http://www.example.org/ontology1#> .

SELECT *
FROM XXX
WHERE {
    ?X rdf:type <http://www.example.org/ontology1#reviewedarticle>.
    ?X rdf:type onto1:reviewedarticle .
  }' > $RESDIR/queryt-q0.rdf
diff $RESDIR/queryt-q0.rdf $CWD/examples/rdf/query.sparql
java -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery -a file://$CWD/examples/rdf/newsample.rdf 'PREFIX dt:   <http://example.org/datatype#> .
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
PREFIX xmls:  <http://www.w3.org/2001/XMLSchema#> .
PREFIX foaf:  <http://xmlns.com/foaf/0.1/> .
PREFIX onto1: <http://www.example.org/ontology1#> .

SELECT *
FROM XXX
WHERE {
    ?X rdf:type <http://www.example.org/ontology1#reviewedarticle>.
    ?X rdf:type onto1:reviewedarticle .
  }' > $RESDIR/queryt-q1.rdf
if [ -s $RESDIR/queryt-q1.rdf ]; then diff $RESDIR/queryt-q1.rdf $RESDIR/queryt-noop.rdf > $RESDIR/diff-query1.txt; else echo error with TRANSQ-QUERY1; fi
if [ ! -s $RESDIR/diff-query1.txt ]; then echo error with TRANSQ-QUERY2; fi

#-------------------
echo "\t-q,--query <FILE>"
java -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery file://$CWD/examples/rdf/newsample.rdf -q $CWD/examples/rdf/query.sparql > $RESDIR/queryt-q1.rdf
if [ -s $RESDIR/queryt-q1.rdf ]; then diff $RESDIR/queryt-q1.rdf $RESDIR/queryt-noop.rdf > $RESDIR/diff-query1.txt; else echo error with TRANSQ-QUERY1; fi
if [ ! -s $RESDIR/diff-query1.txt ]; then echo error with TRANSQ-QUERY2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery file://$CWD/examples/rdf/newsample.rdf --query $CWD/examples/rdf/query.sparql > $RESDIR/queryt-q2.rdf
diff $RESDIR/queryt-q1.rdf $RESDIR/queryt-q2.rdf

#-------------------
echo "\t-a,--alignment <URI>"
java -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery -a file://$CWD/examples/rdf/newsample.rdf -q $CWD/examples/rdf/query.sparql > $RESDIR/queryt-q1.rdf
if [ -s $RESDIR/queryt-q1.rdf ]; then diff $RESDIR/queryt-q1.rdf $RESDIR/queryt-noop.rdf > $RESDIR/diff-query1.txt; else echo error with TRANSQ-QUERY1; fi
if [ ! -s $RESDIR/diff-query1.txt ]; then echo error with TRANSQ-QUERY2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery --alignment file://$CWD/examples/rdf/newsample.rdf --query $CWD/examples/rdf/query.sparql > $RESDIR/queryt-q2.rdf
diff $RESDIR/queryt-q1.rdf $RESDIR/queryt-q2.rdf

#-------------------
echo "\t-o <F>, --output <F>"
java -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery -a file://$CWD/examples/rdf/newsample.rdf -q $CWD/examples/rdf/query.sparql -o $RESDIR/queryt-o1.rdf
if [ -s $RESDIR/queryt-o1.rdf ]; then diff $RESDIR/queryt-o1.rdf $RESDIR/queryt-noop.rdf > $RESDIR/diff-output1.txt; else echo error with TRANSQ-OUTPUT1; fi
if [ ! -s $RESDIR/diff-output1.txt ]; then echo error with TRANSQ-OUTPUT2; fi
diff $RESDIR/queryt-o1.rdf $RESDIR/queryt-q1.rdf
java -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery --alignment file://$CWD/examples/rdf/newsample.rdf --query $CWD/examples/rdf/query.sparql --output $RESDIR/queryt-o2.rdf
diff $RESDIR/queryt-o1.rdf $RESDIR/queryt-o2.rdf

#-------------------
echo "\t-e,--echo"
java -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery -a file://$CWD/examples/rdf/newsample.rdf -q $CWD/examples/rdf/query.sparql -e > $RESDIR/queryt-e1.rdf
if [ -s $RESDIR/queryt-e1.rdf ]; then diff $RESDIR/queryt-q1.rdf $RESDIR/queryt-e1.rdf > $RESDIR/diff-echo1.txt; else echo error with TRANSQ-ECHO1; fi
if [ ! -s $RESDIR/diff-echo1.txt ]; then echo error with TRANSQ-ECHO2; fi
java -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery --alignment file://$CWD/examples/rdf/newsample.rdf --query $CWD/examples/rdf/query.sparql --echo > $RESDIR/queryt-e2.rdf
diff $RESDIR/queryt-e1.rdf $RESDIR/queryt-e2.rdf

#-------------------
echo "\t-Dn=v (used for prefix)"
java -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery -a file://$CWD/examples/rdf/newsample.rdf -q $CWD/examples/rdf/query.sparql -Donto2=http://www.example.org/ontology2# -o $RESDIR/queryt-d1.rdf
if [ -s $RESDIR/queryt-d1.rdf ]; then diff $RESDIR/queryt-q1.rdf $RESDIR/queryt-d1.rdf > $RESDIR/diff-prefix1.txt; else echo error with TRANSQ-PREFIX1; fi
if [ ! -s $RESDIR/diff-prefix1.txt ]; then echo error with TRANSQ-PREFIX2; fi

#-------------------
echo "\t-P,--params <FILE>"
echo "<?xml version='1.0' encoding='utf-8' standalone='no'?>
<\x21DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">
<properties>
<entry key=\"onto2\">http://www.example.org/ontology2#</entry>
</properties>" > $RESDIR/params.xml
java -cp $CP fr.inrialpes.exmo.align.cli.TransformQuery -P $RESDIR/params.xml -a file://$CWD/examples/rdf/newsample.rdf -q $CWD/examples/rdf/query.sparql -o $RESDIR/queryt-P1.rdf
if [ -s $RESDIR/queryt-P1.rdf ]; then diff $RESDIR/queryt-P1.rdf $RESDIR/queryt-d1.rdf > $RESDIR/diff-prefix2.txt; else echo error with TRANSQ-PREFIX3; fi
if [ -s $RESDIR/diff-prefix2.txt ]; then echo error with TRANSQ-PREFIX4; fi

########################################################################
# AlignmentService
########################################################################

echo "\t\t *** Testing AlignmentService ***"
echo "\tTHIS WILL ONLY WORK WITH A RUNNING MYSQL SET FOR THE SERVER"
echo "\tTHIS IS ALSO VERY SENSITIVE TO TIMEOUTS"

#-------------------
echo "\t-z, -zzz"

java -Dlog.level=INFO -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -z &> $RESDIR/zerr.txt
grep "Unrecognized option: -z" $RESDIR/zerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-ERR1; fi
java -Dlog.level=INFO -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --zzz &> $RESDIR/zzzerr.txt
grep "Unrecognized option: --zzz" $RESDIR/zzzerr.txt > $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-ERR2; fi

#-------------------
echo "\t-h, --help"
java -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -h &> $RESDIR/aser-h.txt
java -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --help &> $RESDIR/aser-help.txt
if [ -s  $RESDIR/aser-h.txt ]; then diff $RESDIR/aser-h.txt $RESDIR/aser-help.txt; else echo error with ASERV-HELP1; fi

#-------------------
echo "\t-o <F>, --output <F>"
java -Dlog.level=DEBUG -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -o $RESDIR/aserv.log &
sleep 15
if [ -s $RESDIR/aserv.log ]; then grep "Alignment server running" $RESDIR/aserv.log > $RESDIR/oerr.txt; else echo error with ASERV-OUT1; fi
if [ ! -s $RESDIR/oerr.txt ]; then echo error with ASERV-OUT2; fi
kill -TERM $!
echo > $RESDIR/aserv.log
java -Dlog.level=DEBUG -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --output $RESDIR/aserv2.log &
sleep 15
if [ -s $RESDIR/aserv2.log ]; then grep "Alignment server running" $RESDIR/aserv2.log > $RESDIR/oerr.txt; else echo error with ASERV-OUT3; fi
if [ ! -s $RESDIR/oerr.txt ]; then echo error with ASERV-OUT4; fi
kill -TERM $!

#-------------------
echo "\t-i,--impl <CLASS>"
java -Dlog.level=WARN -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -i fr.inrialpes.exmo.align.cli.GenPlot &> $RESDIR/err.txt &
sleep 5;
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-IMPL1; else grep "Cannot create service for fr.inrialpes.exmo.align.cli.GenPlot" $RESDIR/err.txt > $RESDIR/ierr.txt; fi
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-IMPL2; fi
kill -TERM $!
java -Dlog.level=WARN -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --impl fr.inrialpes.exmo.align.cli.GenPlot2 &> $RESDIR/err.txt &
sleep 5;
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-IMPL3; else grep "Cannot create service for fr.inrialpes.exmo.align.cli.GenPlot2" $RESDIR/err.txt > $RESDIR/ierr.txt; fi
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-IMPL4; fi
kill -TERM $!

#-------------------
echo "\t-A,--jade <PORT>"
echo "\tOnly works with increased PermGenSize"
java -XX:PermSize=128m -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -A 5555 &> $RESDIR/err.txt &
sleep 20;
if [ -s $RESDIR/err.txt ]; then grep "is ready" $RESDIR/err.txt > $RESDIR/jerr.txt; else echo error with ASERV-JADE1; fi
if [ ! -s $RESDIR/jerr.txt ]; then echo error with ASERV-JADE2; fi
kill -TERM $!
java -XX:PermSize=128m -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --jade 5555 &> $RESDIR/err.txt &
sleep 20;
if [ -s $RESDIR/err.txt ]; then grep "is ready" $RESDIR/err.txt > $RESDIR/jerr.txt; else echo error with ASERV-JADE3; fi
if [ ! -s $RESDIR/jerr.txt ]; then echo error with ASERV-JADE4; fi
kill -TERM $!
/bin/rm -f $RESDIR/err.txt $RESDIR/jerr.txt 

#-------------------
echo "\t-W,--wsdl <PORT>"
java -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -W 5555 &> $RESDIR/err.txt &
sleep 5;
kill -TERM $!
if [ -s $RESDIR/err.txt ]; then echo error with ASERV-WSDL1; fi
java -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --wsdl 5555 &> $RESDIR/err.txt &
sleep 5;
kill -TERM $!
if [ -s $RESDIR/err.txt ]; then echo error with ASERV-WSDL2; fi

#-------------------
echo "\t-H,--http <PORT>"
java -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -H 5555 &> $RESDIR/err.txt &
sleep 5;
kill -TERM $!
if [ -s $RESDIR/err.txt ]; then echo error with ASERV-HTTP1; fi
java -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --http 5555 &> $RESDIR/err.txt &
sleep 5;
kill -TERM $!
if [ -s $RESDIR/err.txt ]; then echo error with ASERV-HTTP2; fi

#-------------------
echo "\t-X,--jxta <PORT>"
java -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -X 5555 &> $RESDIR/err.txt &
sleep 5;
kill -TERM $!
if [ -s $RESDIR/err.txt ]; then echo error with ASERV-JXTA1; fi
java -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --jxta 5555 &> $RESDIR/err.txt &
sleep 5;
kill -TERM $!
if [ -s $RESDIR/err.txt ]; then echo error with ASERV-JXTA2; fi

#-------------------
echo "\t-O,--oyster"
echo "\t\tTO BE DEPRECATED"
#java -cp $CP:lib/oyster/oyster.jar:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -O &> $RESDIR/err.txt &
#sleep 5;
#if [ -s $RESDIR/err.txt ]; then echo error with ASERV-OYSTER1; fi
#kill -TERM $!
#java -cp $CP:lib/oyster/oyster.jar:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --oyster &> $RESDIR/err.txt &
#sleep 5;
#if [ -s $RESDIR/err.txt ]; then echo error with ASERV-OYSTER2; fi
#kill -TERM $!

#-------------------
echo "\t-S,--host <HOSTNAME>"
java -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -S aserv.inria.fr &> $RESDIR/err.txt &
sleep 5;
kill -TERM $!
if [ -s $RESDIR/err.txt ]; then echo error with ASERV-HOST1; fi
java -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --host aserv.inria.fr &> $RESDIR/err.txt &
sleep 5;
kill -TERM $!
if [ -s $RESDIR/err.txt ]; then echo error with ASERV-HOST2; fi

#-------------------
echo "\t-u,--uriprefix <URI>"
java -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -u http://www.example.org &> $RESDIR/err.txt &
sleep 5;
kill -TERM $!
if [ -s $RESDIR/err.txt ]; then echo error with ASERV-URIP1; fi
java -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --uriprefix http://www.example.org &> $RESDIR/err.txt &
sleep 5;
kill -TERM $!
if [ -s $RESDIR/err.txt ]; then echo error with ASERV-URIP2; fi

#-------------------
echo "\t-B,--dbms <DBMS>"
java -Dlog.level=ERROR -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -B postgres &> $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-DBMS1; else grep "Cannot connect to database" $RESDIR/err.txt > $RESDIR/dberr.txt; fi
if [ ! -s $RESDIR/dberr.txt ]; then echo error with ASERV-DBMS2; fi
java -Dlog.level=ERROR -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --dbms postgres &> $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-DBMS3; else grep "Cannot connect to database" $RESDIR/err.txt > $RESDIR/dberr.txt; fi
if [ ! -s $RESDIR/dberr.txt ]; then echo error with ASERV-DBMS4; fi

#-------------------
echo "\t-m,--dbmshost <HOST>"
java -Dlog.level=ERROR -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -m www.gloubi.boulga &> $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-DBHOST1; else grep "Communications link failure" $RESDIR/err.txt > $RESDIR/dberr.txt; fi
if [ ! -s $RESDIR/dberr.txt ]; then echo error with ASERV-DBHOST2; fi
java -Dlog.level=ERROR -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --dbmshost www.gloubi.boulga &> $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-DBHOST3; else grep "Communications link failure" $RESDIR/err.txt > $RESDIR/dberr.txt; fi
if [ ! -s $RESDIR/dberr.txt ]; then echo error with ASERV-DBHOST4; fi

#-------------------
echo "\t-s,--dbmsport <PORT>"
java -Dlog.level=ERROR -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -s 5555 &> $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-DBPORT1; else grep "Communications link failure" $RESDIR/err.txt > $RESDIR/dberr.txt; fi
if [ ! -s $RESDIR/dberr.txt ]; then echo error with ASERV-DBPORT2; fi
java -Dlog.level=ERROR -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --dbmsport 5555 &> $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-DBPORT3; else grep "Communications link failure" $RESDIR/err.txt > $RESDIR/dberr.txt; fi
if [ ! -s $RESDIR/dberr.txt ]; then echo error with ASERV-DBPORT4; fi

#-------------------
echo "\t-b,--dbmsbase <BASE>"
java -Dlog.level=ERROR -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -b myAlignDB &> $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-DBDB1; else grep "Access denied for user" $RESDIR/err.txt > $RESDIR/dberr.txt; fi
if [ ! -s $RESDIR/dberr.txt ]; then echo error with ASERV-DBDB2; fi
java -Dlog.level=ERROR -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --dbmsbase myAlignDB &> $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-DBDB3; else grep "Access denied for user" $RESDIR/err.txt > $RESDIR/dberr.txt; fi
if [ ! -s $RESDIR/dberr.txt ]; then echo error with ASERV-DBDB4; fi

#-------------------
echo "\t-l,--dbmsuser <USER>"
java -Dlog.level=ERROR -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -l scott &> $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-DBUSR1; else grep "Access denied for user" $RESDIR/err.txt > $RESDIR/dberr.txt; fi
if [ ! -s $RESDIR/dberr.txt ]; then echo error with ASERV-DBUSR2; fi
java -Dlog.level=ERROR -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --dbmsuser scott &> $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-DBUSR3; else grep "Access denied for user" $RESDIR/err.txt > $RESDIR/dberr.txt; fi
if [ ! -s $RESDIR/dberr.txt ]; then echo error with ASERV-DBUSR4; fi

#-------------------
echo "\t-p,--dbmspass <PASS>"
java -Dlog.level=ERROR -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService -p tiger &> $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-DBPAS1; else grep "Access denied for user" $RESDIR/err.txt > $RESDIR/dberr.txt; fi
if [ ! -s $RESDIR/dberr.txt ]; then echo error with ASERV-DBPAS2; fi
java -Dlog.level=ERROR -cp $CP:lib/alignsvc.jar fr.inrialpes.exmo.align.service.AlignmentService --dbmspass tiger &> $RESDIR/err.txt
if [ ! -s $RESDIR/err.txt ]; then echo error with ASERV-DBPAS3; else grep "Access denied for user" $RESDIR/err.txt > $RESDIR/dberr.txt; fi
if [ ! -s $RESDIR/dberr.txt ]; then echo error with ASERV-DBPAS4; fi

#-------------------
echo "\t-Dn=v"
echo "\t(same as Procalign)"

#-------------------
echo "\t-P,--params <FILE>"
echo "\t(same as Procalign)"

########################################################################
echo Here I reach the end





