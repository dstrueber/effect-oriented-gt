package org.eclipse.emf.henshin.interpreter.loose;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.emf.henshin.interpreter.Assignment;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.AssignmentImpl;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.model.Action.Type;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.GraphElement;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.Rule;

public class KernelRuleHandler {

	public static SpecialRuleInfo createKernelRule(Rule rule) {
		Rule kernelRule = HenshinFactory.eINSTANCE.createRule(rule.getName());

		Map<Node, Node> orig2Kernel = new HashMap<Node, Node>();
		Map<Node, Node> kernel2orig = new HashMap<Node, Node>();

		for (Node n : rule.getLhs().getNodes()) {
			if (isPreserveElement(n)) {
				Node lhsNode = HenshinFactory.eINSTANCE.createNode(kernelRule.getLhs(), n.getType(), n.getName());
				for (Attribute a : n.getAttributes()) {
					Attribute attr = HenshinFactory.eINSTANCE.createAttribute(lhsNode, a.getType(), a.getValue());
					lhsNode.getAttributes().add(attr);
				}
				orig2Kernel.put(n, lhsNode);
				kernel2orig.put(lhsNode, n);
			}
		}

		for (Edge e : rule.getLhs().getEdges()) {
			if (isPreserveElement(e)) {
				Node src = orig2Kernel.get(e.getSource());
				Node trg = orig2Kernel.get(e.getTarget());
				HenshinFactory.eINSTANCE.createEdge(src, trg, e.getType());
			}
		}

		for (Mapping m : rule.getMappings()) {
			if (isPreserveElement(m.getOrigin())) {
				Node lhsNode = orig2Kernel.get(m.getOrigin());
				Node rhsNode = HenshinFactory.eINSTANCE.createNode(kernelRule.getRhs(), m.getImage().getType(),
						m.getImage().getName());
				Mapping mapping = HenshinFactory.eINSTANCE.createMapping(lhsNode, rhsNode);
				kernelRule.getMappings().add(mapping);
				for (Attribute a : m.getImage().getAttributes()) {
					Attribute attr = HenshinFactory.eINSTANCE.createAttribute(rhsNode, a.getType(), a.getValue());
					rhsNode.getAttributes().add(attr);
				}
				orig2Kernel.put(m.getImage(), rhsNode);
				kernel2orig.put(rhsNode, m.getImage());
			}
		}

		for (Edge e : rule.getRhs().getEdges()) {
			if (orig2Kernel.containsKey(e.getSource()) && orig2Kernel.containsKey(e.getTarget())) {
				Node src = orig2Kernel.get(e.getSource());
				Node trg = orig2Kernel.get(e.getTarget());
				HenshinFactory.eINSTANCE.createEdge(src, trg, e.getType());
			}
		}

		for (Parameter p : rule.getParameters()) {
			Parameter param = HenshinFactory.eINSTANCE.createParameter(p.getName());
			param.setKind(p.getKind());
			param.setType(p.getType());
			param.setDescription(p.getDescription());
			kernelRule.getParameters().add(param);
		}

		return new SpecialRuleInfo(kernelRule, rule, orig2Kernel, kernel2orig);
	}

	private static boolean isPreserveElement(GraphElement e) {
		return e.getAction().getType().equals(Type.PRESERVE);
	}

	public static Match translateMatchFromKernel(SpecialRuleInfo info, Match match) {
		Match result = new MatchImpl(info.getOriginalRule());
		for (Node n : info.getSpecialRule().getLhs().getNodes()) {
			result.setNodeTarget(info.getSpecial2Orig().get(n), match.getNodeTarget(n));
		}
		for (Parameter p : info.getSpecialRule().getParameters()) {
			Parameter pOriginal = info.getOriginalRule().getParameter(p.getName());
			Object v = match.getParameterValue(p);
			result.setParameterValue(pOriginal, v);
		}
		return result;
	}

	public static Assignment convertParameterValues(SpecialRuleInfo info, Map<Parameter, Object> paramValues) {
		Assignment result = new AssignmentImpl(info.getSpecialRule());
		for (Entry<Parameter, Object> entry : paramValues.entrySet()) {
			Parameter p= info.getSpecialRule().getParameter(entry.getKey().getName());
			result.setParameterValue(p, entry.getValue());
		}
	
		return result;
	}

}
