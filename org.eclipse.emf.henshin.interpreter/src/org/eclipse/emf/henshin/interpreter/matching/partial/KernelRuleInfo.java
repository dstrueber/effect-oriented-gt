package org.eclipse.emf.henshin.interpreter.matching.partial;

import java.util.Map;

import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;

public class KernelRuleInfo {
	public KernelRuleInfo(Rule kernelRule, Rule originalRule, Map<Node, Node> orig2Kernel, Map<Node, Node> kernel2orig) {
		super();
		this.kernelRule = kernelRule;
		this.originalRule = originalRule;
		this.orig2Kernel = orig2Kernel;
		this.kernel2Orig = kernel2orig;
	}
	Rule kernelRule;
	Rule originalRule;
	public Rule getOriginalRule() {
		return originalRule;
	}
	public void setOriginalRule(Rule originalRule) {
		this.originalRule = originalRule;
	}
	Map<Node,Node> orig2Kernel;
	Map<Node,Node> kernel2Orig;
	public Map<Node, Node> getOrig2Kernel() {
		return orig2Kernel;
	}
	public void setOrig2Kernel(Map<Node, Node> orig2Kernel) {
		this.orig2Kernel = orig2Kernel;
	}
	public Map<Node, Node> getKernel2Orig() {
		return kernel2Orig;
	}
	public void setKernel2Orig(Map<Node, Node> kernel2Orig) {
		this.kernel2Orig = kernel2Orig;
	}
	public Rule getKernelRule() {
		return kernelRule;
	}
	public void setRule(Rule rule) {
		this.kernelRule = rule;
	}
}
