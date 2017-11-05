import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;

import util.ConfigManager;

/**
 * Converts computed results from object to values by getting there rdf
 * reference.
 * 
 * @author Omar Rana
 *
 */
public class Similar extends Files2Facts {

	private ArrayList<String> duplicateCheck;
	public static ArrayList<String> amlValues = new ArrayList<String>();

	public Similar() {

	}

	/**
	 * This function converts computed result into a binary format 1,0. 1
	 * represent true and 0 represents false. This conversion is required for
	 * calculation Precision and Recall.
	 * 
	 * @throws FileNotFoundException
	 * 
	 * @throws IOException
	 */
	public void convertSimilar(String path) throws FileNotFoundException {
		ArrayList<String> aml1List = new ArrayList<String>();
		ArrayList<String> aml2List = new ArrayList<String>();
		ArrayList<String> aml1Values = new ArrayList<String>();
		ArrayList<String> aml2Values = new ArrayList<String>();
		duplicateCheck = new ArrayList<String>();

		try {
			// Start reading computed result from here
			try (BufferedReader br = new BufferedReader(
					new FileReader(new File(ConfigManager.getFilePath() + path)))) {
				String line;
				while ((line = br.readLine()) != null) {
					String values[] = line.split(",");
					if (values.length > 1) {
						// add values which are true
						aml1List.add(values[0].replaceAll("aml1:", ""));
						aml2List.add(values[1].replaceAll("aml2:", ""));

					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		PrintWriter similar = new PrintWriter(ConfigManager.getFilePath() + path);

		// loop through rdf files and convert Objects of RDF in Values for
		// better readability for results
		for (File file : files) {
			InputStream inputStream = FileManager.get().open(file.getAbsolutePath());
			model = ModelFactory.createDefaultModel();

			model.read(new InputStreamReader(inputStream), null, "TURTLE");

			// converts object to values
			if (file.getName().equals("plfile0.ttl")) {

				addAmlValues(aml1List, aml1Values, "aml1:", "hasAttributeName");
				addAmlValues(aml1List, aml1Values, "aml1:", "refBaseClassPath");
				addAmlValues(aml1List, aml1Values, "aml1:", "identifier");

			}

			// converts object to values
			if (file.getName().equals("plfile1.ttl")) {
				addAmlValues(aml2List, aml2Values, "aml2:", "hasAttributeName");
				addAmlValues(aml2List, aml2Values, "aml2:", "refBaseClassPath");
				addAmlValues(aml2List, aml2Values, "aml2:", "identifier");
			}
		}

		// update orignal computed results with the new positive values
		String results = "";
		for (int j = 0; j < aml1Values.size(); j++) {

			if (j < aml2Values.size()) {
				if (!aml1Values.get(j).equals("aml1:eClassIRDI")
						&& !aml1Values.get(j).equals("aml1:eClassClassificationClass")
						&& !aml1Values.get(j).equals("aml1:eClassVersion")) {

					if (!duplicateCheck
							.contains(aml1Values.get(j) + "\t" + aml2Values.get(j) + "\t" + "1")) {
						duplicateCheck
								.add(aml1Values.get(j) + "\t" + aml2Values.get(j) + "\t" + "1");

						String res = aml1Values.get(j) + "\t" + aml2Values.get(j) + "\t" + "1"
								+ "\n";
						if (!res.contains("null")) {
							results += res;
						}
					}
				}
			}
		}

		similar.println(results);

		similar.close();

		// Stores aml values in single array
		// required for integration.
		for (int i = 0; i < aml1Values.size(); i++) {
			if (aml2Values.size() < i) {

				amlValues.add(aml1Values.get(i).replaceAll("aml1:", ""));
				amlValues.add(aml2Values.get(i).replaceAll("aml2:", ""));
			}
		}
		// removes duplicate
		amlValues = new ArrayList<String>(new HashSet<String>(amlValues));
		emulateNegativeResults(path);

	}

	/**
	 * This function emulates negatives rules results and updated the original
	 * file. Negative Rules are emulated by take Cartesian product of initial
	 * seed.
	 */
	public void emulateNegativeResults(String path) {
		try {
			ArrayList<String> aml1negList = new ArrayList<String>();
			ArrayList<String> aml2negList = new ArrayList<String>();
			HashMap<String, String> aml1negValues = new HashMap<String, String>();
			HashMap<String, String> aml2negValues = new HashMap<String, String>();
			HashMap<String, String> aml1negpred = new HashMap<String, String>();
			HashMap<String, String> aml2negpred = new HashMap<String, String>();

			ArrayList<String> otherValues = new ArrayList<String>();
			ArrayList<String> otherValues2 = new ArrayList<String>();

			// Read all Objects for the cartesian product
			try (BufferedReader br = new BufferedReader(new FileReader(
					new File(ConfigManager.getFilePath() + "Edoal/model/hasDocument.txt")))) {
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (line.contains("aml1")) {
						line = line.replaceAll("\t" + "aml1", "");
						aml1negList.add(line.replaceAll("aml1:", ""));
					}

					else {
						line = line.replaceAll("\t" + "aml2", "");
						aml2negList.add(line.replaceAll("aml2:", ""));
					}

				}

			}

			// Read all Objects type for the Cartesian product
			try (BufferedReader br = new BufferedReader(new FileReader(
					new File(ConfigManager.getFilePath() + "Edoal/model/hastype.txt")))) {
				String line;
				while ((line = br.readLine()) != null) {
					String values[] = line.split("\t");
					if (values.length > 1)

						if (line.contains("aml1")) {
							otherValues.add(values[1].replaceAll("aml1:", ""));
						}

						else {
							otherValues2.add(values[1].replaceAll("aml2:", ""));
						}

				}

			}

			PrintWriter similar = new PrintWriter(
					new FileOutputStream(new File(ConfigManager.getFilePath() + path), true));

			// Get all rdf object to values reference for all the objects
			for (File file : files) {
				InputStream inputStream = FileManager.get().open(file.getAbsolutePath());
				model = ModelFactory.createDefaultModel();

				model.read(new InputStreamReader(inputStream), null, "TURTLE");

				if (file.getName().equals("plfile0.ttl")) {

					addAmlNegValues(aml1negList, aml1negValues, "aml1:", "hasAttributeName",
							otherValues, aml1negpred);
					addAmlNegValues(aml1negList, aml1negValues, "aml1:", "refBaseClassPath",
							otherValues, aml1negpred);
					addAmlNegValues(aml1negList, aml1negValues, "aml1:", "identifier", otherValues,
							aml1negpred);
				}

				if (file.getName().equals("plfile1.ttl")) {
					addAmlNegValues(aml2negList, aml2negValues, "aml2:", "hasAttributeName",
							otherValues2, aml2negpred);
					addAmlNegValues(aml2negList, aml2negValues, "aml2:", "refBaseClassPath",
							otherValues2, aml2negpred);
					addAmlNegValues(aml2negList, aml2negValues, "aml2:", "identifier", otherValues2,
							aml2negpred);
				}
			}

			try {
				for (String key : aml1negValues.keySet()) {
					for (String negkey : aml2negValues.keySet()) {

						// predicate and object type should be same for
						// cartesian product
						String type1 = aml1negValues.get(key);
						String type2 = aml2negValues.get(negkey);
						String pred1 = aml1negpred.get(key);
						String pred2 = aml2negpred.get(negkey);

						// checks if its not in positive rules
						if (!duplicateCheck.contains(key + "\t" + negkey + "\t" + "1"))

						{
							if (type1.equals(type2) && pred1.equals(pred2)) {
								similar.println(key + "\t" + negkey + "\t" + "0");

							}
						}
					}

				}
			} catch (Exception e) {

			}
			similar.close();
		} catch (Exception e) {

		}

	}

	/**
	 * This function converts Object into Values reading from Rdf files. The
	 * values are stored in list.
	 * 
	 * @param amlList
	 * @param amlValue
	 * @param aml
	 * @return
	 */
	protected ArrayList<String> addAmlValues(ArrayList<?> amlList, ArrayList<String> amlValue,
			String aml, String predicate) {
		for (int i = 0; i < amlList.size(); i++) {
			StmtIterator iterator = model.listStatements();
			while (iterator.hasNext()) {
				Statement stmt = iterator.nextStatement();
				subject = stmt.getSubject();
				if (subject.asResource().getLocalName().equals(amlList.get(i))) {
					String value = getValue(subject, predicate);
					amlValue.add(aml + value);
					break;
				}
			}
		}
		return amlValue;
	}

}
