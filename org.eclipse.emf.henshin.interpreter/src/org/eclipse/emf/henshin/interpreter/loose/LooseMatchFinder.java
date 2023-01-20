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
import org.eclipse.emf.henshin.interpreter.info.VariableInfo;
import org.eclipse.emf.henshin.interpreter.matching.constraints.DanglingConstraint;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.Rule;

public class LooseMatchFinder {
	Rule rule;
	EGraph graph;
	Engine engineImpl;
	Map<Parameter, Object> paramValues;

	public LooseMatchFinder(Rule rule, EGraph graph, Engine engineImpl) {
		super();
		this.rule = rule;
		this.graph = graph;
		this.engineImpl = engineImpl;
		this.paramValues = new HashMap<Parameter, Object>();
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

		Assignment assignment = KernelRuleHandler.convertParameterValues(kernelRuleInfo, paramValues);
		Iterable<Match> kernelMatches = engineImpl.findMatches(kernelRuleInfo.getSpecialRule(), graph,
				new MatchImpl(assignment, false));
		if (!kernelMatches.iterator().hasNext())
			return null;

		Match kernelMatch = kernelMatches.iterator().next();
		Match originalMatch = KernelRuleHandler.translateMatchFromKernel(kernelRuleInfo, kernelMatch);
		Match grayedMatch = GrayedRuleHandler.translateMatchFromOriginal(grayedRuleInfo, originalMatch);

		List<Node> unboundNodeList = getUnboundNodes(grayedMatch, grayedRuleInfo.getSpecialRule());

		HashMap<Node, EObject> mapping = new HashMap<Node, EObject>();
		for (Node n : grayedRuleInfo.getSpecialRule().getLhs().getNodes()) {
			EObject x = grayedMatch.getNodeTarget(n);
			if (x != null)
				mapping.put(n, x);
		}

		Map<Node, EObject> extension = findExtension(mapping, graph, unboundNodeList, 0);

		for (Entry<Node, EObject> entry : extension.entrySet()) {
			grayedMatch.setNodeTarget(entry.getKey(), entry.getValue());
		}
		return GrayedRuleHandler.translateMatchFromGrayed(grayedRuleInfo, grayedMatch);
	}

	private List<Node> getUnboundNodes(Match resultMatch, Rule rule) {
		List<Node> result = new ArrayList<Node>();
		for (Node n : rule.getLhs().getNodes())
			if (resultMatch.getNodeTarget(n) == null)
				result.add(n);
		return result;
	}

	private EObject findExtension(Map<Node, EObject> currentMapping, EGraph graph, Node n) {
		// Try to find an "anchor": a node that is connected to
		// n and already has a match
		Node anchor = null;
		Edge anchorEdge = null;
		EObject anchorObject = null;
		boolean anchorIsSource = false;

		for (Edge e : n.getAllEdges()) {
			if (currentMapping.get(e.getSource()) != null) {
				anchor = e.getSource();
				anchorIsSource = true;
				anchorEdge = e;
				anchorObject = currentMapping.get(e.getSource());
				break;
			}

			if (currentMapping.get(e.getTarget()) != null) {
				anchor = e.getTarget();
				anchorIsSource = false;
				anchorEdge = e;
				anchorObject = currentMapping.get(e.getTarget());
				break;
			}
		}

		List<EObject> candidateObjects = determineCandidates(graph, n, anchor, anchorEdge, anchorObject,
				anchorIsSource);
		candidateObjects.removeAll(currentMapping.values());

		for (EObject cand : candidateObjects) {
			if (fulfillsConstraints(cand, n, currentMapping)) {
				return cand;
			}
		}

		return null;
	}

	private Collection<EObject> findExtensionCandidates(Map<Node, EObject> currentMatch, EGraph graph, Node n) {
		// Try to find an "anchor": a node that is connected to
		// n and already has a match
		Node anchor = null;
		Edge anchorEdge = null;
		EObject anchorObject = null;
		boolean anchorIsSource = false;

		for (Edge e : n.getAllEdges()) {
			if (currentMatch.get(e.getSource()) != null) {
				anchor = e.getSource();
				anchorIsSource = true;
				anchorEdge = e;
				anchorObject = currentMatch.get(e.getSource());
				break;
			}

			if (currentMatch.get(e.getTarget()) != null) {
				anchor = e.getTarget();
				anchorIsSource = false;
				anchorEdge = e;
				anchorObject = currentMatch.get(e.getTarget());
				break;
			}
		}

		List<EObject> candidateObjects = determineCandidates(graph, n, anchor, anchorEdge, anchorObject,
				anchorIsSource);
		candidateObjects.removeAll(currentMatch.keySet());

		Collection<EObject> result = new ArrayList<>();
		for (EObject cand : candidateObjects) {
			if (fulfillsConstraints(cand, n, currentMatch)) {
				result.add(cand);
			}
		}
		return result;
	}

	private boolean fulfillsConstraints(EObject cand, Node n, Map<Node, EObject> currentMatch) {
		// minimalistic constraint checking..
		// 1. no type-constraint check. these are already covered
		// by the candidate selection process

		// 2. edge constraint checks
		for (Edge e : n.getOutgoing()) {
			if (currentMatch.get(e.getTarget()) != null) {
				EObject expected = currentMatch.get(e.getTarget());
				Object real = cand.eGet(e.getType()); // collection or 1 EObject
				boolean passed = (expected == real)
						|| ((real instanceof Collection) && ((Collection) real).contains(expected));
				if (!passed) {
					return false;
				}
			}
		}
		for (Edge e : n.getIncoming()) {
			if (currentMatch.get(e.getSource()) != null) {
				EObject source = currentMatch.get(e.getSource());
				Object real = source.eGet(e.getType()); // collection or 1 EObject
				boolean passed = (cand == real) || ((real instanceof Collection) && ((Collection) real).contains(cand));
				if (!passed) {
					return false;
				}
			}
		}

		// dangling constraint checks
		DanglingConstraint dc = retrieveDanglingConstraint(n);
		if (!dc.check(cand, graph))
			return false;

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

	private DanglingConstraint retrieveDanglingConstraint(Node n) {
		Map<Node, DanglingConstraint> node2Constraint = new HashMap<>();
		DanglingConstraint entry = node2Constraint.get(n);
		if (entry == null) {
			Map<EReference, Integer> incoming = VariableInfo.getEdgeCounts(n, true);
			Map<EReference, Integer> outgoing = VariableInfo.getEdgeCounts(n, false);
			entry = new DanglingConstraint(outgoing, incoming, false);
			node2Constraint.put(n, entry);
		}
		return entry;
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
			paramValues.put(param, value);
	}

	public Map<Node, EObject> findExtension(Map<Node, EObject> currentMappings, EGraph graph,
			List<Node> unboundNodeList, int position) {
		Node n = unboundNodeList.get(position);

		Collection<EObject> candidates = findExtensionCandidates(currentMappings, graph, n);
		if (!candidates.isEmpty()) {
//			 1. wiederhole, solange es noch neue gültige Teil-Lösungsschritte gibt:
//		     a) wähle einen neuen gültigen Teil-Lösungsschritt
			for (EObject x : candidates) {
//		     b) falls Wahl gültig ist:
//            I) erweitere Vektor um Wahl;
				Map<Node, EObject> currentMappingsExtended = new HashMap<>(currentMappings);
				currentMappingsExtended.put(n, x);

//           II) falls Vektor vollständig ist, return true; // Lösung gefunden!
				if (position == unboundNodeList.size() - 1) {
					return currentMappingsExtended;
				}
//               sonst: falls (FindeLösung(Stufe+1, Vektor)) return true; // Lösung!
				else {
					Map<Node, EObject> nextSolution = findExtension(currentMappingsExtended, graph, unboundNodeList,
							position + 1);
					if (nextSolution != null && checkDanglingPostponed(n, graph, nextSolution, unboundNodeList)) {
						return nextSolution;
					} else {
//	                    sonst mache Wahl rückgängig; // Sackgasse (Backtracking)!
						currentMappingsExtended.remove(n);
					}
				}

			}
		} else { // d.h., falls candidates == empty
			if (position == unboundNodeList.size() - 1) {
				return currentMappings;
			} else {
				return findExtension(currentMappings, graph, unboundNodeList, position + 1);
			}
		}

		// 2. Da es keinen neuen Teil-Lösungsschritt gibt: return false // Keine Lösung!
		return null;
	}

	private boolean checkDanglingPostponed(Node n, EGraph graph, Map<Node, EObject> nextSolution,
			List<Node> unboundNodeList) {
		DanglingConstraint dc = retrieveDanglingConstraint(n);

		// adapt edge counts
		Map<EReference, Integer> removedIncoming = new HashMap<>();
		Map<EReference, Integer> removedOutgoing = new HashMap<>();

		for (Edge e : n.getIncoming()) {
			if (nextSolution.get(e.getSource()) == null) {
				dc.increaseIncoming(e.getType(), -1);
				if (removedIncoming.containsKey(e.getType()))
					removedIncoming.put(e.getType(), 1 + removedIncoming.get(e.getType()));
				else
					removedIncoming.put(e.getType(), 1);
			}
		}
		for (Edge e : n.getOutgoing()) {
			if (nextSolution.get(e.getTarget()) == null) {
				dc.increaseOutgoing(e.getType(), -1);
				if (removedOutgoing.containsKey(e.getType()))
					removedOutgoing.put(e.getType(), 1 + removedOutgoing.get(e.getType()));
				else
					removedOutgoing.put(e.getType(), 1);
			}
		}

		boolean passed = dc.check(nextSolution.get(n), graph);

		// restore original edge counts
		for (Entry<EReference, Integer> entry : removedIncoming.entrySet())
			dc.increaseIncoming(entry.getKey(), entry.getValue());
		for (Entry<EReference, Integer> entry : removedOutgoing.entrySet())
			dc.increaseOutgoing(entry.getKey(), entry.getValue());
		return passed;
	}

}
