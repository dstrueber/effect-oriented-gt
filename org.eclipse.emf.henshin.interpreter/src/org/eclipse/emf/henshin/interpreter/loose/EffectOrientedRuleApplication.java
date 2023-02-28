package org.eclipse.emf.henshin.interpreter.loose;

import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.RuleApplication;
import org.eclipse.emf.henshin.interpreter.impl.RuleApplicationImpl;
import org.eclipse.emf.henshin.model.Rule;

public class EffectOrientedRuleApplication {
	private Engine engine;
	private EGraph graph;
	private Rule rule;
	private Match partialMatch;

	public EffectOrientedRuleApplication(Engine engine, EGraph graph, Rule rule, Match partialMatch) {
		this.engine = engine;
		this.graph = graph;
		this.rule = rule;
		this.partialMatch = partialMatch;
	}
	
	public boolean execute() {
		SpecialRuleInfo applicationRuleInfo = ApplicationRuleHandler.createApplicationRule(rule, partialMatch);
		Rule applicationRule = applicationRuleInfo.getSpecialRule();
		Match match = ApplicationRuleHandler.translateMatchFromOriginal(applicationRuleInfo, partialMatch);
		RuleApplication applicationRuleApp = new RuleApplicationImpl(engine, graph, applicationRule, match);
		return applicationRuleApp.execute(null);
	}
}
