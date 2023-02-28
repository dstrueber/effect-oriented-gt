
/**
 * <copyright>
 * Copyright (c) 2010-2014 Henshin developers. All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 which 
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * </copyright>
 */

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.henshin.interpreter.EGraph;
import org.eclipse.emf.henshin.interpreter.Engine;
import org.eclipse.emf.henshin.interpreter.Match;
import org.eclipse.emf.henshin.interpreter.UnitApplication;
import org.eclipse.emf.henshin.interpreter.impl.EGraphImpl;
import org.eclipse.emf.henshin.interpreter.impl.EngineImpl;
import org.eclipse.emf.henshin.interpreter.impl.UnitApplicationImpl;
import org.eclipse.emf.henshin.interpreter.loose.LocallyCompleteMatchFinder;
import org.eclipse.emf.henshin.interpreter.loose.EffectOrientedRuleApplication;
import org.eclipse.emf.henshin.interpreter.util.InterpreterUtil;
import org.eclipse.emf.henshin.interpreter.util.PartialMatchReport;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;

/**
 * Basd on the bank example for the Henshin interpreter:
 * https://wiki.eclipse.org/Henshin/Examples/Bank_Accounts
 */
public class PartialMatchExample {

	/**
	 * Relative path to the bank model files.
	 */
	public static final String PATH = "src/";

	/**
	 * Run the bank example.
	 * 
	 * @param path       Relative path to the model files.
	 * @param saveResult Whether the result should be saved.
	 */
	public static void run(String path, boolean saveResult) {
		for (int i = 0; i < 1; i++) {

			// Create a resource set with a base directory:
			HenshinResourceSet resourceSet = new HenshinResourceSet(path);

			// Load the module:
			Module module = resourceSet.getModule("partialmatching.henshin", false);

			// Load the example model into an EGraph:
			EGraph graph = new EGraphImpl(resourceSet.getResource("example-bank.xmi"));

			// Create an engine and a rule application:
			Engine engine = new EngineImpl();

			Rule ensureThatClientHasAccountRule = (Rule) module.getUnit("ensureThatClientHasAccount");
			Rule ensureThatClientHasNoAccountRule = (Rule) module.getUnit("ensureThatClientHasNoAccount");
			Rule ensureThatClientHasAccountAndPortfolioRule = (Rule) module
					.getUnit("ensureThatClientHasAccountAndPortfolio");

			System.out.println("\n ******************** \n ");

			long start = System.nanoTime();
			LocallyCompleteMatchFinder localMatcher;
			localMatcher = new LocallyCompleteMatchFinder(ensureThatClientHasNoAccountRule, graph, engine);
			localMatcher.setParameter("c","Alice");
			Match m1 = localMatcher.findLocallyCompleteMatch();
			EffectOrientedRuleApplication app1 = new EffectOrientedRuleApplication(engine, graph, ensureThatClientHasNoAccountRule, m1);
			System.out.println(app1.execute());

			resourceSet.saveEObject(graph.getRoots().get(0), "example-result-1.xmi");
			
			
			localMatcher.setParameter("c","Alice");
			Match m2 = localMatcher.findLocallyCompleteMatch();
			EffectOrientedRuleApplication app2 = new EffectOrientedRuleApplication(engine, graph, ensureThatClientHasNoAccountRule, m2);
			System.out.println(app2.execute());
			
			resourceSet.saveEObject(graph.getRoots().get(0), "example-result-1.xmi");

			
			localMatcher = new LocallyCompleteMatchFinder(ensureThatClientHasAccountRule, graph, engine);
			localMatcher.setParameter("c","Alice");
			Match m3 = localMatcher.findLocallyCompleteMatch();
			EffectOrientedRuleApplication app3 = new EffectOrientedRuleApplication(engine, graph, ensureThatClientHasAccountRule, m3);
			System.out.println(app3.execute());
			

			resourceSet.saveEObject(graph.getRoots().get(0), "example-result-2.xmi");
		
			
			localMatcher = new LocallyCompleteMatchFinder(ensureThatClientHasAccountRule, graph, engine);
			localMatcher.setParameter("c","Alice");
			Match m4 = localMatcher.findLocallyCompleteMatch();
			EffectOrientedRuleApplication app4 = new EffectOrientedRuleApplication(engine, graph, ensureThatClientHasAccountRule, m4);
			System.out.println(app4.execute());

			resourceSet.saveEObject(graph.getRoots().get(0), "example-result-2.xmi");	

			
			localMatcher = new LocallyCompleteMatchFinder(ensureThatClientHasAccountAndPortfolioRule, graph, engine);
			localMatcher.setParameter("c","Alice");
			Match m5 = localMatcher.findLocallyCompleteMatch();
			EffectOrientedRuleApplication app5 = new EffectOrientedRuleApplication(engine, graph, ensureThatClientHasAccountAndPortfolioRule, m5);
			System.out.println(app5.execute());
			

			resourceSet.saveEObject(graph.getRoots().get(0), "example-result-3.xmi");	

			
			
			localMatcher = new LocallyCompleteMatchFinder(ensureThatClientHasAccountAndPortfolioRule, graph, engine);
			localMatcher.setParameter("c","Alice");
			Match m6 = localMatcher.findLocallyCompleteMatch();
			EffectOrientedRuleApplication app6 = new EffectOrientedRuleApplication(engine, graph, ensureThatClientHasAccountAndPortfolioRule, m6);
			System.out.println(app6.execute());

			resourceSet.saveEObject(graph.getRoots().get(0), "example-result-3.xmi");

			System.exit(0);
			
			boolean runSvetlanasAlgo = false;
			if (runSvetlanasAlgo) {
				System.out.println("\n ******************** \n ");

				start = System.nanoTime();
				Match mx = InterpreterUtil.findMaximalPartialMatch(engine, module, graph);
				long totalOld = System.nanoTime() - start;


//				System.out.println("Time taken (new algo): " + totalNew);
				System.out.println("Time taken (old algo): " + totalOld);
			}

		}
	}

	public static void main(String[] args) {
		run(PATH, true); // we assume the working directory is the root of the examples plug-in
	}

}
