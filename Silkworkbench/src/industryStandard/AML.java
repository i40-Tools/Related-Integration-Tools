package industryStandard;

import java.io.FileNotFoundException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;



/**
 * 
 * @author Irlan Grangel
 *
 *         Represents the AutomationML Standard as an RDF graph
 */
public class AML extends IndustryStandards {

	public AML(Model model, int newNumber) {
		super(model, newNumber);
	}

	public AML() {
		// TODO Auto-generated constructor stub
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public void setNumber(int newNumber) {
		this.number = newNumber;
	}

	/**
	 * Automation ML part for data population
	 * 
	 * @throws FileNotFoundException
	 */
	public void addsDataforAML() throws FileNotFoundException {

		StmtIterator iterator = model.listStatements();
		// RefSemantic part starts here
		while (iterator.hasNext()) {

			Statement stmt = iterator.nextStatement();
			subject = stmt.getSubject();
			predicate = stmt.getPredicate();
			object = stmt.getObject();

			if (number == 3) {

			} // all subjects are added according to ontology e.g aml
			else {
				addSubjectURI(subject, "", number, "hasDocument");
			}

			addHasType();
		}

	}

	/**
	 * This function populates with type of Element.
	 */

	private void addHasType() {
		if (predicate.asNode().getLocalName().equals("type")) {
			if (number != 3)
				addSubjectURI(subject, ":" + object.asNode().getLocalName(), number,
						"has" + predicate.asNode().getLocalName());
		}

	}

}
