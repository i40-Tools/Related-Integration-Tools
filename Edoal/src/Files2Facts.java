


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import uni.bonn.krextor.Krextor;

/**
 * Reads the RDF files and convert them to Datalog facts
 * 
 * @author Irlan 28.06.2016
 */
public class Files2Facts {
	public RDFNode object;
	public RDFNode predicate;
	public RDFNode subject;
	public ArrayList<File> files;
	public Model model;

	/**
	 * Converts the file to turtle format based on Krextor
	 * 
	 * @param input
	 * @param output
	 */
	public void convertRdf() {
		int i = 0;
		for (File file : files) {
			if (file.getName().endsWith(".aml")) {
				Krextor krextor = new Krextor();
				krextor.convertRdf(file.getAbsolutePath(), "aml", "turtle",
						ConfigManager.getFilePath() + "plfile" + i + ".ttl");
			} 
			i++;
		}
	}

	/**
	 * Read the RDF files of a given path
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public ArrayList<File> readFiles(String path, String type, String type2, String type3) throws Exception {
		files = new ArrayList<File>();
		File originalFilesFolder = new File(path);
		if (originalFilesFolder.isDirectory()) {
			for (File amlFile : originalFilesFolder.listFiles()) {
				if (amlFile.isFile() && (amlFile.getName().endsWith(type) || amlFile.getName().endsWith(type2)
						|| amlFile.getName().endsWith(type3))) {
					if (amlFile.getName().endsWith(".aml")) {
						String name = amlFile.getName().replace(".aml", "");
						if (name.endsWith("0") || name.endsWith("1")) {
							files.add(amlFile);
						}
					}

					else if (amlFile.getName().endsWith(".opcua")) {
						String name = amlFile.getName().replace(".opcua", "");
						if (name.endsWith("0") || name.endsWith("1")) {
							files.add(amlFile);
						}
					}

					else if (amlFile.getName().endsWith(".xml")) {
						String name = amlFile.getName().replace(".xml", "");
						if (name.endsWith("0") || name.endsWith("1")) {
							files.add(amlFile);
						}
					}

					else {
						files.add(amlFile);
					}
				}
			}
		} else {
			System.out.println("Error in the directory that you provided");
			System.exit(0);
		}
		return files;
	}

	/**
	 * Adds aml Values
	 * @param amlList
	 * @param amlValue
	 * @param aml
	 * @return
	 */
	HashMap<String, String> addAmlNegValues(ArrayList<?> amlList,HashMap<String, String> amlValue,String aml,
			String predicate,ArrayList<?> type,HashMap<String, String>pred){	
		for(int i = 0;i < amlList.size();i++){	
			StmtIterator iterator = model.listStatements();
			while (iterator.hasNext()) {
				Statement stmt = iterator.nextStatement();
				subject = stmt.getSubject();
				
				if(subject.asResource().getLocalName().equals(amlList.get(i))){
					String value = getValue(subject,predicate);					
					if(value != null && !value.contains("eClassIRDI")
							&&!value.contains("eClassClassificationClass")
							&&!value.contains("eClassVersion")){
						amlValue.put(aml + value,type.get(i).toString());
						pred.put(aml + value,predicate);
					
						iterator.close();
						break;
					}
				}
			}
		}
		return amlValue;
	}
	/**
	 * get predicate Value
	 * @param name
	 * @return
	 */
	String getValue(RDFNode name, String predicate) {
		String type = null;
		StmtIterator stmts = model.listStatements(name.asResource(), null, (RDFNode) null);
		while (stmts.hasNext()) {
			Statement stmte = stmts.nextStatement();

			if (stmte.getPredicate().asNode().getLocalName().toString().equals(predicate)) {
				type = stmte.getObject().asLiteral().getLexicalForm();
			}
						
		}
		return type;
	}
	
	
}