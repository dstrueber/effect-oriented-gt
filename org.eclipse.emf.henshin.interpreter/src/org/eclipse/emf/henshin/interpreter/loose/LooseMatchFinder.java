package org.eclipse.emf.henshin.interpreter.loose;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.henshin.interpreter.Assignment;
import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.AssignmentImpl;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.Rule;

public class LooseMatchFinder {
	Rule rule;
	EGraph graph;
	Engine engineImpl;
	Map<Parameter,Object> paramValues;
	
	public LooseMatchFinder(Rule rule, EGraph graph, Engine engineImpl) {
		super();
		this.rule = rule;
		this.graph = graph;
		this.engineImpl = engineImpl;
		this.paramValues = new HashMap<Parameter,Object>();
	}

	/**
	 * Returns a loose match, potentially contain mappings for LHS and RHS nodes.
	 * 
	 * @param rule
	 * @param graph
	 * @param engineImpl
	 * @return
	 */
	public Match findOneLooseMatch() {
		SpecialRuleInfo kernelRuleInfo = KernelRuleHandler.createKernelRule(rule);
		SpecialRuleInfo grayedRuleInfo = GrayedRuleHandler.createGrayedRule(rule);

		Assignment assignment = KernelRuleHandler.convertParameterValues(kernelRuleInfo,paramValues);
		Iterable<Match> kernelMatches = engineImpl.findMatches(kernelRuleInfo.getSpecialRule(), graph, new MatchImpl(assignment, false));
		if (!kernelMatches.iterator().hasNext())
			return null;

		Match kernelMatch = kernelMatches.iterator().next();
		Match originalMatch = KernelRuleHandler.translateMatchFromKernel(kernelRuleInfo, kernelMatch);
		Match resultMatch = GrayedRuleHandler.translateMatchFromOriginal(grayedRuleInfo, originalMatch);

		List<Node> searchPlan = getUnboundNodes(resultMatch, grayedRuleInfo.getSpecialRule());
		for (Node n : searchPlan) {
			EObject ex = findExtension(resultMatch, graph, n);
			if (ex != null)
				resultMatch.setNodeTarget(n, ex);
		}

		resultMatch = GrayedRuleHandler.translateMatchFromGrayed(grayedRuleInfo, resultMatch);

		return resultMatch;
	}

	private List<Node> getUnboundNodes(Match resultMatch, Rule rule) {
		List<Node> result = new ArrayList<Node>();
		for (Node n : rule.getLhs().getNodes())
			if (resultMatch.getNodeTarget(n) == null)
				result.add(n);
		return result;
	}

	private EObject findExtension(Match currentMatch, EGraph graph, Node n) {
		// Try to find an "anchor": a node that is connected to
		// n and already has a match
		Node anchor = null;
		Edge anchorEdge = null;
		EObject anchorObject = null;
		boolean anchorIsSource = false;

		for (Edge e : n.getAllEdges()) {
			if (currentMatch.getNodeTarget(e.getSource()) != null) {
				anchor = e.getSource();
				anchorIsSource = true;
				anchorEdge = e;
				anchorObject = currentMatch.getNodeTarget(e.getSource());
				break;
			}

			if (currentMatch.getNodeTarget(e.getTarget()) != null) {
				anchor = e.getTarget();
				anchorIsSource = false;
				anchorEdge = e;
				anchorObject = currentMatch.getNodeTarget(e.getTarget());
				break;
			}
		}

		List<EObject> candidateObjects = determineCandidates(graph, n, anchor, anchorEdge, anchorObject,
				anchorIsSource);
		candidateObjects.removeAll(currentMatch.getNodeTargets());

		for (EObject cand : candidateObjects) {
			if (fulfillsConstraints(cand, n, currentMatch)) {
				return cand;
			}
		}

		return null;
	}

	private boolean fulfillsConstraints(EObject cand, Node n, Match resultMatch) {
		// minimalistic constraint checking..
		// 1. no type-constraint check. these are already covered
		// by the candidate selection process

		// 2. edge constraint checks
		for (Edge e : n.getOutgoing()) {
			if (resultMatch.getNodeTarget(e.getTarget()) != null) {
				EObject expected = resultMatch.getNodeTarget(e.getTarget());
				Object real = cand.eGet(e.getType()); // collection or 1 EObject
				boolean passed = (expected == real)
						|| ((real instanceof Collection) && ((Collection) real).contains(expected));
				if (!passed) {
					return false;
				}
			}
		}
		for (Edge e : n.getIncoming()) {
			if (resultMatch.getNodeTarget(e.getSource()) != null) {
				EObject source = resultMatch.getNodeTarget(e.getSource());
				Object real = source.eGet(e.getType()); // collection or 1 EObject
				boolean passed = (cand == real) || ((real instanceof Collection) && ((Collection) real).contains(cand));
				if (!passed) {
					return false;
				}
			}
		}

		// for now: only static values checked - attributes, ints, strings, floats
		for (Attribute a : n.getAttributes()) {
			String expectedValString = a.getValue();
			Object expectedVal = null;
			if (expectedValString.startsWith("\"") && expectedValString.endsWith("\"")) {
				expectedVal = expectedValString.substring(1, expectedValString.length() - 2);
			}
			try {
				expectedVal = Double.parseDouble(expectedValString);
			} catch (NumberFormatException e) {
			}
			try {
				expectedVal = Integer.parseInt(expectedValString);
			} catch (NumberFormatException e) {
			}

			Object actualVal = cand.eGet(a.getType());
			if (actualVal == null)
				continue;
			if (actualVal.equals(expectedVal))
				return false;
		}

		return true;
	}

	private List<EObject> determineCandidates(EGraph graph, Node n, Node anchor, Edge anchorEdge, EObject anchorObject,
			boolean anchorIsSource) {
		List<EObject> candidateObjects = new ArrayList<EObject>();
		if (anchor != null) {
			if (anchorIsSource) {
				addEdgeTargetsToCandidateObjects(anchor, anchorEdge.getType(), anchorObject, candidateObjects);
			} else {
				if (anchorEdge.getType().getEOpposite() != null) {
					addEdgeTargetsToCandidateObjects(anchor, anchorEdge.getType().getEOpposite(), anchorObject,
							candidateObjects);
				} else {
					for (EObject cand : graph.getDomain(n.getType(), false)) {
						Object o = cand.eGet(anchorEdge.getType());
						if (o == anchor || (o instanceof Collection) && ((Collection) o).contains(anchor)) {
							candidateObjects.add(cand);
						}
					}
				}
			}
		} else {
			candidateObjects.addAll(graph.getDomain(n.getType(), false));
		}
		return candidateObjects;
	}

	private void addEdgeTargetsToCandidateObjects(Node node, EReference ref, EObject anchorObject,
			List<EObject> candidateObjects) {
		Object o = anchorObject.eGet(ref);
		if (o instanceof Collection) {
			candidateObjects.addAll((Collection) o);
		} else {
			candidateObjects.add((EObject) o);
		}
	}

	public void setParameter(String key, String value) {
		Parameter param = rule.getParameter(key);
		if (param != null)
			paramValues.put(param,value);
	}

}
