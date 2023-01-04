package org.eclipse.emf.henshin.interpreter.matching.partial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.impl.MatchImpl;
import org.eclipse.emf.henshin.model.Attribute;
import org.eclipse.emf.henshin.model.Edge;
import org.eclipse.emf.henshin.model.HenshinFactory;
import org.eclipse.emf.henshin.model.Mapping;
import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Parameter;
import org.eclipse.emf.henshin.model.Rule;

public class KernelRuleHandler {


	public static KernelRuleInfo createKernelRule(Rule rule, Collection<Node> kernelNodes) {
		Copier copier = new Copier();
		Rule kernelRule = HenshinFactory.eINSTANCE.createRule(rule.getName());
		
		Map<Node, Node> orig2Kernel = new HashMap<Node, Node> ();
		Map<Node, Node> kernel2orig = new HashMap<Node, Node> ();

		for (Node n : rule.getLhs().getNodes()) {
			if (kernelNodes.contains(n)) {
				Node lhsNode = HenshinFactory.eINSTANCE.createNode(kernelRule.getLhs(), n.getType(), n.getName());
				for (Attribute a : n.getAttributes()) {
					Attribute attr = HenshinFactory.eINSTANCE.createAttribute(lhsNode, a.getType(), a.getValue());
					lhsNode.getAttributes().add(attr);
				}
				orig2Kernel.put(n,lhsNode);
				kernel2orig.put(lhsNode, n);
			}
		}

		for (Edge e : rule.getLhs().getEdges()) {
			if (kernelNodes.contains(e.getSource()) && 
					kernelNodes.contains(e.getTarget())) {
				Node src = orig2Kernel.get(e.getSource());
				Node trg = orig2Kernel.get(e.getTarget());
				HenshinFactory.eINSTANCE.createEdge(src, trg, e.getType());
			}
		}
			
		for (Mapping m : rule.getMappings()) {
			if (kernelNodes.contains(m.getOrigin())) {
				Node lhsNode = orig2Kernel.get(m.getOrigin());
				Node rhsNode = HenshinFactory.eINSTANCE.createNode(kernelRule.getRhs(), m.getImage().getType(), m.getImage().getName());
				Mapping mapping = HenshinFactory.eINSTANCE.createMapping(lhsNode, rhsNode);
				kernelRule.getMappings().add(mapping);
				for (Attribute a : m.getImage().getAttributes()) {
					Attribute attr = HenshinFactory.eINSTANCE.createAttribute(rhsNode, a.getType(), a.getValue());
					rhsNode.getAttributes().add(attr);
				}
				orig2Kernel.put(m.getImage(),lhsNode);
				kernel2orig.put(lhsNode, m.getImage());
			}
		}
		

		for (Edge e : rule.getRhs().getEdges()) {
			if (orig2Kernel.containsKey(e.getSource()) && 
					orig2Kernel.containsKey(e.getTarget())) {
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
		
		return new KernelRuleInfo(kernelRule, rule, orig2Kernel, kernel2orig);
	}

	
//	public static KernelRuleInfo createKernelRule(Rule rule, Collection<Node> kernelNodes) {
//		Copier copier = new Copier();
//		Rule kernelRule = (Rule) copier.copy(rule);
//		
//		Map<Node, Node> orig2Kernel = new HashMap<Node, Node> ();
//		Map<Node, Node> kernel2orig = new HashMap<Node, Node> ();
//
//		List<Node> rhsNodesToDel = new ArrayList<Node>();
//		List<Node> lhsNodesToDel = new ArrayList<Node>();
//		Set<Edge> rhsEdgesToDel = new HashSet<Edge>();
//		Set<Edge> lhsEdgesToDel = new HashSet<Edge>();
//		List<Mapping> mappingsToDel = new ArrayList<Mapping>();;
//
//		for (Node n : rule.getLhs().getNodes()) {
//			if (!kernelNodes.contains(n)) {
//				Node lhsNode = (Node) copier.get(n);
//				lhsNodesToDel.add(lhsNode);
//				lhsEdgesToDel.addAll(lhsNode.getAllEdges());
//			}
//		}
//		for (Mapping m : rule.getMappings()) {
//			if (!kernelNodes.contains(m.getOrigin())) {
//				Node imageNode = (Node) copier.get(m.getImage());
//				mappingsToDel.add((Mapping) copier.get(m));
//				rhsNodesToDel.add(imageNode);
//				rhsEdgesToDel.addAll(imageNode.getAllEdges());
//			}
//		}
//		
//		kernelRule.getMappings().removeAll(mappingsToDel);
//		kernelRule.getLhs().getNodes().removeAll(lhsNodesToDel);
//		kernelRule.getRhs().getNodes().removeAll(rhsNodesToDel);
//		kernelRule.getLhs().getEdges().removeAll(lhsEdgesToDel);
//		kernelRule.getRhs().getEdges().removeAll(rhsEdgesToDel);
//		
//		for (Node n : rule.getLhs().getNodes()) {
//			if (kernelNodes.contains(n)) {
//				orig2Kernel.put(n, (Node) copier.get(n));	
//				kernel2orig.put((Node) copier.get(n), n);	
//			}
//		}
//
//		return new KernelRuleInfo(kernelRule, rule, orig2Kernel, kernel2orig);
//	}

	public static Match translateMatch(KernelRuleInfo info, Match match) {
		Match result = new MatchImpl(info.getOriginalRule());
		for (Node n : info.getKernelRule().getLhs().getNodes()) {
			result.setNodeTarget(info.getKernel2Orig().get(n), match.getNodeTarget(n));
		}
		for (int i=0; i<info.getKernelRule().getParameters().size(); i++) {
			Parameter p = info.getKernelRule().getParameters().get(i);
			if (match.getParameterValues().size() > i) {
				Object v = match.getParameterValues().get(i);
				result.setParameterValue(p, v);
			}
		}
		return result;
	}

}
