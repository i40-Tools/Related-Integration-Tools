import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;

import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.silkframework.Silk;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import groovy.lang.Script;
import groovy.util.ResourceException;
import util.ConfigManager;

/**
 * This class uses Silk linkage rules to find alignment.
 * 
 * @author omar
 *
 */

public class MainSILK {

	public static void main(String[] args) throws Throwable {

		// Report.getReport(ConfigManager.getExperimentFolder());
		// Report.getResults();
		// Report.getSize();
		// System.exit(0);

		Files2Facts filesAMLInRDF = new Files2Facts();
		filesAMLInRDF.readFiles(ConfigManager.getFilePath(), ".aml", ".opcua", ".xml");
		filesAMLInRDF.convertRdf();
		Similar similar = new Similar();
		new MainSILK().preprocessRdf();
		similar.readFiles(ConfigManager.getFilePath(), ".ttl", ".opcua", ".xml");

		// preprocess data sources
		runSilkInference();
		String result = formatResult();
		cleanFiles();
		new File(ConfigManager.getFilePath() + "Silk/Precision").mkdirs();

		new File(ConfigManager.getFilePath() + "Silk/model").mkdirs();

		// final result saved
		PrintWriter writer = new PrintWriter(ConfigManager.getFilePath() + "Silk/silk.txt",
				"UTF-8");
		writer.println(result);
		writer.close();

		similar.generateModel(ConfigManager.getFilePath());

		// convert object to values
		similar.convertSimilar("Silk/silk.txt");
		System.out.println(
				"Silk inference file Saved in " + ConfigManager.getFilePath() + "silk.txt");
		evaluation();
	}

	/*
	 * Preprocess rdf files for two ontologies.
	 */
	public void preprocessRdf() throws FileNotFoundException, IOException {
		String content = IOUtils.toString(
				new FileInputStream(ConfigManager.getFilePath() + "plfile1.ttl"), "UTF-8");
		content = content.replaceAll("https://w3id.org/i40/aml", "https://w3id.org/i40/aml2");
		content = content.replaceAll("dateTime", "date");
		content = content.replaceAll(":ConnectionPoint", "");
		IOUtils.write(content, new FileOutputStream(ConfigManager.getFilePath() + "plfile1.ttl"),
				"UTF-8");

		content = IOUtils.toString(new FileInputStream(ConfigManager.getFilePath() + "plfile0.ttl"),
				"UTF-8");
		content = content.replaceAll("dateTime", "date");
		content = content.replaceAll(":ConnectionPoint", "");
		IOUtils.write(content, new FileOutputStream(ConfigManager.getFilePath() + "plfile0.ttl"),
				"UTF-8");

	}

	/**
	 * Copy data for temporary processing. Since there is no specific way to
	 * point at data source path, we have to copy it to root.
	 * 
	 * @param file
	 */
	public void copyFile(String file) {
		File source = new File(ConfigManager.getFilePath() + file);
		File dest = new File(file);
		try {
			FileUtils.copyFile(source, dest);
		} catch (IOException e) {
			e.printStackTrace();

		}
	}

	/*
	 * This function reads output for given rule and returns it. Used for
	 * concatination of all the results.
	 */
	public static String readOutput() throws FileNotFoundException, IOException {
		try (FileInputStream inputStream = new FileInputStream("links.ttl")) {
			String everything = IOUtils.toString(inputStream, "UTF-8");
			return everything;
		}
	}

	/**
	 * Deleting temporary files.
	 * 
	 * @throws IOException
	 */
	public static void cleanFiles() throws IOException {
		File file = new File("plfile0.ttl");
		file.delete();
		file = new File("plfile1.ttl");
		file.delete();
		file = new File("links.ttl");
		file.delete();
		file = new File("model.ttl");
		file.delete();

	}

	/**
	 * This function read silk linkage rules and run inference one by one. Final
	 * result is saved in a rdf model.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void runSilkInference() throws FileNotFoundException, IOException {
		new MainSILK().copyFile("plfile0.ttl");
		new MainSILK().copyFile("plfile1.ttl");
		File file = new File("rules.xml");

		Silk.executeFile(file, "AttributeRefSemantic", 4, true);
		String result = readOutput();
		Silk.executeFile(file, "SameAttributeName", 4, true);
		result += readOutput();
		Silk.executeFile(file, "SameIdentifiers", 4, true);
		result += readOutput();
		Silk.executeFile(file, "SameExternalReference", 4, true);
		result += readOutput();
		Silk.executeFile(file, "SameInternalElement", 4, true);
		result += readOutput();
		Silk.executeFile(file, "SameInternalElement2", 4, true);
		result += readOutput();

		try (Writer writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream("model.ttl"), "utf-8"))) {
			writer.write(result);
		}
	}

	/**
	 * This function formats the final result for evaluation purpose.
	 * 
	 * @return
	 */
	public static String formatResult() {
		Model modelY = ModelFactory.createDefaultModel();
		modelY.read("model.ttl", "Turtle");
		String result = "";
		String queryString = "PREFIX ns0:<https://w3id.org/i40/aml/>"
				+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
				+ "PREFIX owl:<http://www.w3.org/2002/07/owl#>"
				+ "PREFIX ns1:<https://w3id.org/i40/aml2/>"
				+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>" + "Select * where{ ?x ?y ?z } ";

		try (QueryExecution qexec1 = QueryExecutionFactory.create(queryString, modelY)) {
			ResultSet results = qexec1.execSelect();
			for (; results.hasNext();) {
				QuerySolution soln = results.nextSolution();
				RDFNode x = soln.get("x");
				Resource r = soln.getResource("z");
				if (!result.contains(
						"aml1:" + x.asNode().getLocalName() + "," + "aml2:" + r.getLocalName())) {
					result += "aml1:" + x.asNode().getLocalName() + "," + "aml2:" + r.getLocalName()
							+ "\n";
				}
			}
			qexec1.close();
		}

		return result;
	}

	/**
	 * This function is a general method to execute the Evaluation.
	 * 
	 * @throws CompilationFailedException
	 * @throws IOException
	 * @throws ScriptException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws groovy.util.ScriptException
	 * @throws ResourceException
	 */
	public static void evaluation() throws CompilationFailedException, IOException, ScriptException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, InstantiationException, ResourceException,
			groovy.util.ScriptException {
		// Needed to run the PSL rules part
		Script script = new Script() {
			@Override
			public Object run() {
				return null;
			}
		};
		try {
			script.evaluate(new File("src/evaluation/Evaluation.groovy"));
		} catch (Exception e) {
		}
	}

}
