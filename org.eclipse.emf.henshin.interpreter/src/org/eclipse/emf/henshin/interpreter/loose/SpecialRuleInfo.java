package org.eclipse.emf.henshin.interpreter.loose;

import java.util.Map;

import org.eclipse.emf.henshin.model.Node;
import org.eclipse.emf.henshin.model.Rule;

public class SpecialRuleInfo {

	Rule specialRule;
	Rule originalRule;
	Map<Node,Node> orig2special;
	Map<Node,Node> special2orig;
	public SpecialRuleInfo(Rule specialRule, Rule originalRule, Map<Node, Node> orig2special, Map<Node, Node> special2orig) {
		super();
		this.specialRule = specialRule;
		this.originalRule = originalRule;
		this.orig2special = orig2special;
		this.special2orig = special2orig;
	}
	public Rule getOriginalRule() {
		return originalRule;
	}
	public void setOriginalRule(Rule originalRule) {
		this.originalRule = originalRule;
	}
	public Rule getSpecialRule() {
		return specialRule;
	}
	public void setSpecialRule(Rule specialRule) {
		this.specialRule = specialRule;
	}
	public Map<Node, Node> getOrig2Special() {
		return orig2special;
	}
	public Map<Node, Node> getSpecial2Orig() {
		return special2orig;
	}
}
