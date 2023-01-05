package org.eclipse.emf.henshin.interpreter.loose;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.model.Action.Type;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.Graph;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.Rule;

public class ApplicationRuleHandler {

	/**
	 * Create an 'application' rule from a normal rule and a loose match: - removes
	 * 'delete' elements not included in match - turns 'create' elements included in
	 * match into 'preserve'elements
	 * 
	 * @param rule
	 * @return
	 */
	public static SpecialRuleInfo createApplicationRule(Rule rule, Match partialMatch) {
		Rule applicationRule = HenshinFactory.eINSTANCE.createRule(rule.getName());

		Map<Node, Node> orig2appl = new HashMap<Node, Node>();
		Map<Node, Node> appl2orig = new HashMap<Node, Node>();
		Map<Edge, Edge> orig2applEdges = new HashMap<Edge, Edge>();
		Map<Edge, Edge> appl2origEdges = new HashMap<Edge, Edge>();

		recreateNodes(rule.getLhs(), applicationRule.getLhs(), orig2appl, appl2orig);
		recreateNodes(rule.getRhs(), applicationRule.getRhs(), orig2appl, appl2orig);
		recreateEdges(orig2appl, rule.getLhs(), orig2applEdges, appl2origEdges);
		recreateEdges(orig2appl, rule.getRhs(), orig2applEdges, appl2origEdges);
		recreateMappings(rule, applicationRule, orig2appl);

		processDeleteElements(rule, applicationRule, partialMatch, orig2appl, appl2orig, orig2applEdges,
				appl2origEdges);
		processCreateElements(rule, applicationRule, partialMatch, orig2appl, appl2orig, orig2applEdges,
				appl2origEdges);

		recreateParameters(rule, applicationRule);

		return new SpecialRuleInfo(applicationRule, rule, orig2appl, appl2orig);
	}

	private static void processDeleteElements(Rule rule, Rule applicationRule, Match partialMatch,
			Map<Node, Node> orig2appl, Map<Node, Node> appl2orig, Map<Edge, Edge> orig2applEdges, Map<Edge, Edge> appl2origEdges) {
		ArrayList<Node> nodesToDelete = new ArrayList<Node>();
		for (Node n : rule.getLhs().getNodes()) {
			if (n.getAction() != null && n.getAction().getType() == Type.DELETE
					&& partialMatch.getNodeTarget(n) == null) {
				nodesToDelete.add(orig2appl.get(n));
			}
		}
		for (Node n : nodesToDelete)
			applicationRule.getLhs().removeNode(n);

		ArrayList<Edge> edgesToDelete = new ArrayList<Edge>();
		for (Edge e : rule.getLhs().getEdges()) {
			if (e.getAction() != null && e.getAction().getType() == Type.DELETE) {
				Node srcNode = e.getSource();
				Node trgNode = e.getTarget();
				EObject srcObject = partialMatch.getNodeTarget(srcNode);
				EObject trgObject = partialMatch.getNodeTarget(trgNode);
				boolean remove = true;
				if (srcObject != null && trgObject != null) {
					Object refValue = srcObject.eGet(e.getType());
					if (refValue == trgObject)
						remove = false;
					if (refValue instanceof Collection && ((Collection) refValue).contains(trgObject)) {
						remove = false;
					}
				}
				if (remove) {
					edgesToDelete.add(orig2applEdges.get(e));
				}
			}
		}
		for (Edge e : edgesToDelete)
			applicationRule.getLhs().removeEdge(e);
	}

	private static void processCreateElements(Rule rule, Rule applRule, Match partialMatch,
			Map<Node, Node> orig2Appl, Map<Node, Node> apply2Orig, Map<Edge, Edge> orig2applEdges, Map<Edge, Edge> appl2origEdges) {
		for (Node n : rule.getRhs().getNodes()) {
			if (n.getAction() != null && n.getAction().getType() == Type.CREATE
					&& partialMatch.getNodeTarget(n) != null) {
				Node rhsNode = orig2Appl.get(n);
				Node lhsNode = recreateNode(applRule.getLhs(), orig2Appl, apply2Orig, n);
				Mapping mapping = HenshinFactory.eINSTANCE.createMapping(lhsNode, rhsNode);
				applRule.getMappings().add(mapping);
			}
		}
		for (Edge e : rule.getRhs().getEdges()) {
			if (e.getAction() != null && e.getAction().getType() == Type.CREATE
					&& partialMatch.getNodeTarget(e.getSource()) != null
					&& partialMatch.getNodeTarget(e.getTarget()) != null) {
				Node lhsSource = orig2Appl.get(e.getSource());
				Node lhsTarget = orig2Appl.get(e.getTarget());
				HenshinFactory.eINSTANCE.createEdge(lhsSource, lhsTarget, e.getType());
			}
		}
	}

	private static void recreateMappings(Rule rule, Rule applRule, Map<Node, Node> orig2Grayed) {
		for (Mapping m : rule.getMappings()) {
			Node lhsNode = orig2Grayed.get(m.getOrigin());
			Node rhsNode = orig2Grayed.get(m.getImage());
			Mapping mapping = HenshinFactory.eINSTANCE.createMapping(lhsNode, rhsNode);
			applRule.getMappings().add(mapping);
		}
	}

	private static void recreateParameters(Rule rule, Rule applRule) {
		for (Parameter p : rule.getParameters()) {
			Parameter param = HenshinFactory.eINSTANCE.createParameter(p.getName());
			param.setKind(p.getKind());
			param.setType(p.getType());
			param.setDescription(p.getDescription());
			applRule.getParameters().add(param);
		}
	}

	private static void recreateNodes(Graph graph, Graph applRuleGraph, Map<Node, Node> orig2Grayed,
			Map<Node, Node> grayed2Orig) {
		for (Node n : graph.getNodes()) {
			recreateNode(applRuleGraph, orig2Grayed, grayed2Orig, n);
		}
	}

	private static Node recreateNode(Graph applRuleGraph, Map<Node, Node> orig2Appl, Map<Node, Node> appl2Orig,
			Node n) {
		Node node = HenshinFactory.eINSTANCE.createNode(applRuleGraph, n.getType(), n.getName());
		for (Attribute a : n.getAttributes()) {
			Attribute attr = HenshinFactory.eINSTANCE.createAttribute(node, a.getType(), a.getValue());
			node.getAttributes().add(attr);
		}
		orig2Appl.put(n, node);
		appl2Orig.put(node, n);
		return node;
	}

	private static void recreateEdges(Map<Node, Node> orig2Grayed, Graph graph, Map<Edge, Edge> orig2applEdges,
			Map<Edge, Edge> appl2origEdges) {
		for (Edge e : graph.getEdges()) {
			Node src = orig2Grayed.get(e.getSource());
			Node trg = orig2Grayed.get(e.getTarget());
			Edge eNew = HenshinFactory.eINSTANCE.createEdge(src, trg, e.getType());
			orig2applEdges.put(e, eNew);
			appl2origEdges.put(e, eNew);
		}
	}

	public static Match translateMatchFromOriginal(SpecialRuleInfo info, Match match) {
		Match result = new MatchImpl(info.getSpecialRule());
		for (Node n : info.getOriginalRule().getLhs().getNodes()) {
			result.setNodeTarget(info.getOrig2Special().get(n), match.getNodeTarget(n));
		}

		for (Parameter p : info.getOriginalRule().getParameters()) {
			Parameter pSpecial = info.getSpecialRule().getParameter(p.getName());
			Object v = match.getParameterValue(p);
			result.setParameterValue(pSpecial, v);
		}
		return result;
	}

}
