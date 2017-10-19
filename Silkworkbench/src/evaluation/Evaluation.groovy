
package evaluation
import java.io.File
import java.text.DecimalFormat

import edu.umd.cs.psl.application.inference.MPEInference
import edu.umd.cs.psl.application.learning.weight.maxlikelihood.MaxLikelihoodMPE
import edu.umd.cs.psl.config.*
import edu.umd.cs.psl.database.DataStore
import edu.umd.cs.psl.database.Database
import edu.umd.cs.psl.database.Partition
import edu.umd.cs.psl.database.rdbms.RDBMSDataStore
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver.Type
import edu.umd.cs.psl.evaluation.statistics.DiscretePredictionComparator
import edu.umd.cs.psl.evaluation.statistics.DiscretePredictionStatistics
import edu.umd.cs.psl.groovy.*
import edu.umd.cs.psl.model.argument.ArgumentType
import edu.umd.cs.psl.model.argument.GroundTerm
import edu.umd.cs.psl.model.argument.type.*
import edu.umd.cs.psl.model.atom.GroundAtom
import edu.umd.cs.psl.model.atom.RandomVariableAtom
import edu.umd.cs.psl.model.predicate.Predicate
import edu.umd.cs.psl.model.predicate.type.*
import edu.umd.cs.psl.ui.functions.textsimilarity.*
import edu.umd.cs.psl.ui.loading.InserterUtils
import edu.umd.cs.psl.util.database.Queries

/**
 * @author Omar Rana
 * @author Irlan Grangel
 * Computes the precision and recall based on Probabilistic Soft Logic(PSL)
 *
 */
public class Evaluation
{
	private ConfigManager cm
	private ConfigBundle config
	private Database testDB
	private Database trainDB
	private Database truthDB
	private PSLModel model
	private DataStore data
	private Partition testObservations
	private Partition testPredictions
	private Partition targetsPartition
	private Partition truthPartition
	private String threshold
	def dir
	def testDir
	def trainDir

	public static void main(String[] args)
	{
		Evaluation docAlign = new Evaluation()
		docAlign.execute()
	}
	
	public void execute()
	{
		Evaluation documentAligment = new Evaluation()
		documentAligment.config()
		documentAligment.definePredicates()
		documentAligment.evalResults()

	}

	public void config()
	{
		cm = ConfigManager.getManager()
		config = cm.getBundle("document-alignment")	
		def defaultPath = System.getProperty("java.io.tmpdir")
		String dbpath = config.getString("dbpath", defaultPath + File.separator + "document-alignment")
		data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, dbpath, true), config)
		model = new PSLModel(this, data)
		
	}

	/**
	 * Defines the name and the arguments of predicates that are used in the rules
	 */
	public void definePredicates(){


		model.add predicate: "eval", types: [ArgumentType.String, ArgumentType.String]
		

	}

	/**
	 * Evaluates the results of inference versus expected truth values
	 */
	public void evalResults() {
		testDir = util.ConfigManager.getFilePath() + "Silk/"
		
		File file = new File(util.ConfigManager.getFilePath()+"GoldStandard.txt");
		if (!file.exists()) {
			System.out.println("Error :: GoldStandard Missing in" + testDir + "model");
			System.exit(0);
		}
		targetsPartition = new Partition(5)
		truthPartition = new Partition(6)
		
		def insert = data.getInserter(eval, targetsPartition)
		InserterUtils.loadDelimitedDataTruth(insert, testDir + "silk.txt")

		insert  =  data.getInserter(eval, truthPartition)
		InserterUtils.loadDelimitedDataTruth(insert, util.ConfigManager.getFilePath() + "GoldStandard.txt")
		Database resultsDB = data.getDatabase(targetsPartition, [eval] as Set)
		Database truthDB = data.getDatabase(truthPartition, [eval] as Set)
		DiscretePredictionComparator dpc = new DiscretePredictionComparator(resultsDB)
		dpc.setBaseline(truthDB)
		DiscretePredictionStatistics stats = dpc.compare(eval)

		Double F1 = stats.getF1(DiscretePredictionStatistics.BinaryClass.POSITIVE)
		Double precision = stats.getPrecision(DiscretePredictionStatistics.
				BinaryClass.POSITIVE)
		Double recall = stats.getRecall(DiscretePredictionStatistics.
				BinaryClass.POSITIVE)

		System.out.println("Accuracy:" + stats.getAccuracy().round(2))
		System.out.println("Error:" + stats.getError())
		System.out.println("True Positive:" + stats.tp)
		System.out.println("True Negative:" + stats.tn)
		System.out.println("False Positive:" + stats.fp)
		System.out.println("False Negative:" + stats.fn)
		System.out.println("Precision:" + precision.round(2))
		System.out.println("Recall:" + recall.round(2))
		System.out.println("Fmeasure:" + F1.round(2))

		// Saving Precision and Recall results to file
		def resultsFile

			resultsFile = new File(testDir + "Precision/F1NoTraining.txt")
	
		resultsFile.createNewFile()
		resultsFile.write("")
		resultsFile.append("Accuracy:" + stats.getAccuracy().round(2) + '\n')
		resultsFile.append("Error:" + stats.getError() + '\n')
		resultsFile.append("Fmeasure:" + F1.round(2) +  '\n')
		resultsFile.append("True Positive:" + stats.tp + '\n')
		resultsFile.append("True Negative:" + stats.tn + '\n')
		resultsFile.append("False Positive:" + stats.fp + '\n')
		resultsFile.append("False Negative:" + stats.fn + '\n')
		resultsFile.append("Precision :" + precision.round(2) + '\n')
		resultsFile.append("Recall: " + recall.round(2) + '\n')
		resultsFile.close()
		resultsDB.close()
		truthDB.close()
	}

}