
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
import org.eclipse.emf.henshin.interpreter.matching.partial.PartialMatchFinder;
import org.eclipse.emf.henshin.interpreter.util.InterpreterUtil;
import org.eclipse.emf.henshin.interpreter.util.PartialMatchReport;
import org.eclipse.emf.henshin.model.Module;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.emf.henshin.model.resource.HenshinResourceSet;

/**
 * Bank example for the Henshin interpreter. Shows the usage of the interpreter.
 * 
 * @author Christian Krause
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
		for (int i = 0; i<1; i++) {
		
		// Create a resource set with a base directory:
		HenshinResourceSet resourceSet = new HenshinResourceSet(path);

		// Load the module:
		Module module = resourceSet.getModule("partialmatching.henshin", false);

		// Load the example model into an EGraph:
		EGraph graph = new EGraphImpl(resourceSet.getResource("example-bank.xmi"));

		// Create an engine and a rule application:
		Engine engine = new EngineImpl();

		Rule paySpecialBonusRule = (Rule) module.getUnit("PaySpecialBonus");

		List<String> kernelNodeNames = Arrays.asList("B");

		System.out.println("\n ******************** \n ");

		long start = System.nanoTime();
		PartialMatchFinder partialMatcher = new PartialMatchFinder();
		Match m = partialMatcher.findOneMaximalPartialMatchFromNames(paySpecialBonusRule, graph, kernelNodeNames,
				engine);
		long totalNew = System.nanoTime() - start;

		System.out.println(m);

		boolean runSvetlanasAlgo = false;
		if (runSvetlanasAlgo) {
		System.out.println("\n ******************** \n ");

		start = System.nanoTime();
		Match m2 = InterpreterUtil.findMaximalPartialMatch(engine, module, graph);
		long totalOld = System.nanoTime() - start;

		System.out.println(m2);
		
		System.out.println("Time taken (new algo): "+totalNew);
		System.out.println("Time taken (old algo): "+totalOld);
		}

		}
	}

	public static void main(String[] args) {
		run(PATH, true); // we assume the working directory is the root of the examples plug-in
	}

}
