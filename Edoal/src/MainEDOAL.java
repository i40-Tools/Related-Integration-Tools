import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import javax.script.ScriptException;

import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
import org.codehaus.groovy.control.CompilationFailedException;

import groovy.lang.Script;
import groovy.util.ResourceException;
import util.ConfigManager;

/**
 * This class uses Edoal Queries to find alignment.
 * 
 * @author omar
 *
 */

public class MainEDOAL {

	public static void main(String[] args) throws Throwable {

		// Report.getReport(ConfigManager.getExperimentFolder());
		// Report.getResults();
		//
		// System.exit(0);

		Files2Facts filesAMLInRDF = new Files2Facts();
		filesAMLInRDF.readFiles(ConfigManager.getFilePath(), ".aml", ".opcua", ".xml");
		filesAMLInRDF.convertRdf();
		Similar similar = new Similar();
		new MainEDOAL().preprocessRdf();
		similar.readFiles(ConfigManager.getFilePath(), ".ttl", ".opcua", ".xml");

		// gets all queries seperated by --
		String a[];
		String result = "";
		try (BufferedReader br = new BufferedReader(new FileReader("query.sparql"))) {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
				line = br.readLine();
			}
			String everything = sb.toString();
			a = everything.split("--");
		}

		// gets dataset for query
		Model modelY = FileManager.get().loadModel(ConfigManager.getFilePath() + "plfile0.ttl");
		Model modelX = FileManager.get().loadModel(ConfigManager.getFilePath() + "plfile1.ttl");

		Dataset dataset = DatasetFactory.create();
		dataset.addNamedModel("https://w3id.org/i40/aml2#", modelX);
		dataset.addNamedModel("https://w3id.org/i40/aml#", modelY);

		// run each query one by one.
		for (int i = 0; i < a.length; i++) {
			QueryExecution qexec = QueryExecutionFactory.create(a[i],
					dataset.getNamedModel("urn:x-arq:UnionGraph"));
			Model model = qexec.execConstruct();

			// formatting final result
			String queryString = "PREFIX ns0:<https://w3id.org/i40/aml/>"
					+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
					+ "PREFIX owl:<http://www.w3.org/2002/07/owl#>"
					+ "PREFIX ns1:<https://w3id.org/i40/aml2/>"
					+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>"
					+ "Select * where{ ?x ?y ?z } ";

			try (QueryExecution qexec1 = QueryExecutionFactory.create(queryString, model)) {
				ResultSet results = qexec1.execSelect();
				for (; results.hasNext();) {
					QuerySolution soln = results.nextSolution();
					RDFNode x = soln.get("x");
					Resource r = soln.getResource("z");
					if (!result.contains("aml1:" + x.asNode().getLocalName() + "," + "aml2:"
							+ r.getLocalName())) {
						result += "aml1:" + x.asNode().getLocalName() + "," + "aml2:"
								+ r.getLocalName() + "\n";
					}
				}
				qexec1.close();
			}
		}
		// final result saved
		new File(ConfigManager.getFilePath() + "Edoal/Precision").mkdirs();

		new File(ConfigManager.getFilePath() + "Edoal/model").mkdirs();

		PrintWriter writer = new PrintWriter(ConfigManager.getFilePath() + "Edoal/edoal.txt",
				"UTF-8");
		writer.println(result);
		writer.close();

		similar.generateModel(ConfigManager.getFilePath());
		// convert object to values
		similar.convertSimilar("Edoal/edoal.txt");
		evaluation();
		System.out.println(
				"Edoal inference file Saved in " + ConfigManager.getFilePath() + "edoal.txt");
	}

	/*
	 * Preprocess rdf files for two ontologies.
	 */
	void preprocessRdf() throws FileNotFoundException, IOException {
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
