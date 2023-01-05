package org.eclipse.emf.henshin.interpreter.loose;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.interpreter.Assignment;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.Unit;

public class ExtendedMatchImpl extends MatchImpl {

	public ExtendedMatchImpl(Assignment assignment, boolean isResultMatch) {
		super(assignment, isResultMatch);
	}

	public ExtendedMatchImpl(Rule rule) {
		super(rule);
	}

	List<Node> getNodes() {
		return nodes;
	}
	

	@Override
	protected void setUnit(Unit unit) {
		super.setUnit(unit);
		this.nodes = new ArrayList<>(nodes);
	}
	
}
