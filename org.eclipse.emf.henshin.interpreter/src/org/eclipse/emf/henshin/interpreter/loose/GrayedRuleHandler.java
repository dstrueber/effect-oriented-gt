package org.eclipse.emf.henshin.interpreter.loose;

import java.util.HashMap;
import java.util.Map;

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

public class GrayedRuleHandler {

	/**
	 * Create a 'grayed' rule from a normal rule:
	 * 'create' elements become 'preserve' elements.
	 * 'delete' elements remain 'delete' elements.
	 * 
	 * @param rule
	 * @return
	 */
	public static SpecialRuleInfo createGrayedRule(Rule rule) {
		Rule grayedRule = HenshinFactory.eINSTANCE.createRule(rule.getName());

		Map<Node, Node> orig2Grayed = new HashMap<Node, Node>();
		Map<Node, Node> grayed2Orig = new HashMap<Node, Node>();

		recreateNodes(rule.getLhs(), grayedRule.getLhs(), orig2Grayed, grayed2Orig);
		recreateNodes(rule.getRhs(), grayedRule.getRhs(), orig2Grayed, grayed2Orig);
		recreateEdges(orig2Grayed, rule.getLhs());
		recreateEdges(orig2Grayed, rule.getRhs());
		recreateMappings(rule, grayedRule, orig2Grayed);
		grayifyRule(rule, grayedRule, orig2Grayed, grayed2Orig);

		recreateParameters(rule, grayedRule);

		return new SpecialRuleInfo(grayedRule, rule, orig2Grayed, grayed2Orig);
	}

	private static void grayifyRule(Rule rule, Rule grayedRule, Map<Node, Node> orig2Grayed,
			Map<Node, Node> grayed2Orig) {
		for (Node n : rule.getRhs().getNodes()) {
			if (n.getAction() != null && n.getAction().getType() == Type.CREATE) {
				Node rhsNode = orig2Grayed.get(n);
				Node lhsNode = recreateNode(grayedRule.getLhs(), orig2Grayed, grayed2Orig, n);
				Mapping mapping = HenshinFactory.eINSTANCE.createMapping(lhsNode, rhsNode);
				grayedRule.getMappings().add(mapping);
			}
		}
		for (Edge e : rule.getRhs().getEdges()) {
			if (e.getAction() != null && e.getAction().getType() == Type.CREATE) {
				Node lhsSource = orig2Grayed.get(e.getSource().getActionNode());
				Node lhsTarget = orig2Grayed.get(e.getTarget().getActionNode());
				HenshinFactory.eINSTANCE.createEdge(lhsSource, lhsTarget, e.getType());
			}
		}
	}

	private static void recreateMappings(Rule rule, Rule grayedRule, Map<Node, Node> orig2Grayed) {
		for (Mapping m : rule.getMappings()) {
			Node lhsNode = orig2Grayed.get(m.getOrigin());
			Node rhsNode = orig2Grayed.get(m.getImage());
			Mapping mapping = HenshinFactory.eINSTANCE.createMapping(lhsNode, rhsNode);
			grayedRule.getMappings().add(mapping);
		}
	}

	private static void recreateParameters(Rule rule, Rule grayedRule) {
		for (Parameter p : rule.getParameters()) {
			Parameter param = HenshinFactory.eINSTANCE.createParameter(p.getName());
			param.setKind(p.getKind());
			param.setType(p.getType());
			param.setDescription(p.getDescription());
			grayedRule.getParameters().add(param);
		}
	}

	private static void recreateNodes(Graph graph, Graph grayedRuleGraph, Map<Node, Node> orig2Grayed,
			Map<Node, Node> grayed2Orig) {
		for (Node n : graph.getNodes()) {
			recreateNode(grayedRuleGraph, orig2Grayed, grayed2Orig, n);
		}
	}

	private static Node recreateNode(Graph grayedRuleGraph, Map<Node, Node> orig2Grayed, Map<Node, Node> grayed2Orig,
			Node n) {
		Node node = HenshinFactory.eINSTANCE.createNode(grayedRuleGraph, n.getType(), n.getName());
		for (Attribute a : n.getAttributes()) {
			Attribute attr = HenshinFactory.eINSTANCE.createAttribute(node, a.getType(), a.getValue());
			node.getAttributes().add(attr);
		}
		orig2Grayed.put(n, node);
		grayed2Orig.put(node, n);
		return node;
	}

	private static void recreateEdges(Map<Node, Node> orig2Grayed, Graph graph) {
		for (Edge e : graph.getEdges()) {
			Node src = orig2Grayed.get(e.getSource());
			Node trg = orig2Grayed.get(e.getTarget());
			HenshinFactory.eINSTANCE.createEdge(src, trg, e.getType());
		}
	}
	public static Match translateMatchFromGrayed(SpecialRuleInfo info, Match match) {
		ExtendedMatchImpl result = new ExtendedMatchImpl(info.getOriginalRule());
		for (Node n : info.getSpecialRule().getLhs().getNodes()) {
			if (match.getNodeTarget(n) != null) {
				// The following two lines are necessary in order
				// to not break the toString function for the match..
				if (!result.getNodes().contains(info.getSpecial2Orig().get(n)))
					result.getNodes().add(info.getSpecial2Orig().get(n));
				result.setNodeTarget(info.getSpecial2Orig().get(n), match.getNodeTarget(n));			
			}
		}
		for (Parameter p : info.getSpecialRule().getParameters()) {
			Parameter pOriginal = info.getOriginalRule().getParameter(p.getName());
			Object v = match.getParameterValue(p);
			result.setParameterValue(pOriginal, v);
		}
		return result;
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
