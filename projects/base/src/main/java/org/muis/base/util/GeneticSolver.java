package org.muis.base.util;

import java.util.Iterator;

public class GeneticSolver {
	public static class Genome {
		private float [] theGenes;

		public Genome(int count) {
			theGenes = new float[count];
		}

		public float [] getGenes() {
			return theGenes;
		}

		Genome mate(Genome mate, float mutationFactor) {

		}

		Genome mutate(float mutationFactor) {
		}
	}

	public static interface GenomeTester {
		float getFitness(Genome genome);
	}

	public static class GeneticSolution implements Comparable<GeneticSolution> {
		public final Genome genome;

		public final float fitness;

		public GeneticSolution(Genome genes, float fit) {
			genome = genes;
			fitness = fit;
		}

		@Override
		public int compareTo(GeneticSolution o) {
			double diff = fitness - o.fitness;
			if(diff > 0)
				return -1;
			else if(diff < 0)
				return 1;
			else
				return 0;
		}
	}

	public static class GeneticResults implements Iterable<GeneticSolution> {
		private Iterable<GeneticSolution> theSolutions;

		private int theGenerations;

		GeneticResults(GeneticSolution [] solutions, int generations) {
			theSolutions = prisms.util.ArrayUtils.iterable(solutions, true);
			theGenerations = generations;
		}

		public int getGenerations() {
			return theGenerations;
		}

		@Override
		public Iterator<GeneticSolution> iterator() {
			return theSolutions.iterator();
		}
	}

	private static final int TRIES = 5;

	private static final int MATE_PREFERENCE_FACTOR = 4;

	private final GenomeTester theTester;

	private int theMinSolutions;

	private float theMinFitness;

	private int theMaxGenerations;

	private int thePopulationCap;

	public GeneticSolver(GenomeTester tester) {
		theTester = tester;
		theMinSolutions = 1;
		theMinFitness = 100;
		theMaxGenerations = 10;
		thePopulationCap = 100;
	}

	public void setMinSolutions(int minSolutions) {
		theMinSolutions = minSolutions;
	}

	public void setMinFitness(float minFitness) {
		theMinFitness = minFitness;
	}

	public void setMaxGenerations(int maxGenerations) {
		theMaxGenerations = maxGenerations;
	}

	public void setPopulationCapacity(int popCap) {
		thePopulationCap = popCap;
	}

	public GeneticResults solve(Genome... init) {
		GeneticSolution [] population = new GeneticSolution[init.length];
		for(int i = 0; i < init.length; i++)
			population[i] = new GeneticSolution(init[i], theTester.getFitness(init[i]));
		java.util.Arrays.sort(population);
		int generations;
		java.util.TreeSet<GeneticSolution> solutionSet = new java.util.TreeSet<>();
		for(generations = 0; generations < theMaxGenerations && population.length > 0; generations++) {
			population = evolve(population, solutionSet);
			int good = 0;
			for(GeneticSolution soln : population) {
				if(soln.fitness >= theMinFitness)
					good++;
			}
			if(good > theMinSolutions) {
				break;
			}
		}
		GeneticSolution [] solutions;
		if(population.length > 0)
			solutions = population;
		else if(population[0].fitness >= theMinFitness) {
			java.util.ArrayList<GeneticSolution> solutionList = new java.util.ArrayList<>();
			for(GeneticSolution soln : population) {
				if(soln.fitness >= theMinFitness)
					solutionList.add(soln);
			}
			solutions = solutionList.toArray(new GeneticSolution[solutionList.size()]);
		} else if(population.length > theMinSolutions) {
			solutions = new GeneticSolution[theMinSolutions];
			System.arraycopy(population, 0, solutions, 0, theMinSolutions);
		} else
			solutions = population;
		return new GeneticResults(solutions, generations);
	}

	private GeneticSolution [] evolve(GeneticSolution [] pop, java.util.TreeSet<GeneticSolution> solutionSet) {
		for(int p = 0; p < pop.length; p++) {
			solutionSet.add(pop[p]);
			while(solutionSet.size() > thePopulationCap)
				solutionSet.pollLast();

			// Mutate each genome a certain number of times--fit genomes get mutated more times
			int numMutations = (int) Math.ceil(thePopulationCap * 2f / pop.length / p);
			for(int mutation = 0; mutation < numMutations; mutation++) {
				for(int tries = 0; tries < TRIES; tries++) {
					Genome child = pop[p].genome.mutate(mutation * 1f / numMutations);
					float fitness = theTester.getFitness(child);
					if(fitness != Float.NEGATIVE_INFINITY) {
						solutionSet.add(new GeneticSolution(child, fitness));
						while(solutionSet.size() > thePopulationCap)
							solutionSet.pollLast();
						break;
					}
				}
			}

			// Mate the genomes. Fit genomes mate with more mates. Mate selection is preferential to fit mates.
			int numMates = (int) Math.ceil((pop.length) / (p + 1f));
			for(int mateNum = 0; mateNum < numMates; mateNum++) {
				for(int tries = 0; tries < TRIES; tries++) {
					// Take the minimum of a few random indexes so simulate preferential mate selection
					int mateIdx = pop.length - 1;
					for(int mateTryIdx = 0; mateTryIdx < MATE_PREFERENCE_FACTOR; mateTryIdx++) {
						int mateTry = (int) Math.round(Math.random() * (pop.length - 1));
						if(mateTry < mateIdx)
							mateIdx = mateTry;
					}
					// Remove the possibility of something mating with itself
					if(mateIdx >= p)
						mateIdx++;
					Genome child = pop[p].genome.mate(pop[mateIdx].genome, mateNum * 1f / numMates);
					float fitness = theTester.getFitness(child);
					if(fitness != Float.NEGATIVE_INFINITY) {
						solutionSet.add(new GeneticSolution(child, fitness));
						while(solutionSet.size() > thePopulationCap)
							solutionSet.pollLast();
						break;
					}
				}
			}
		}

		if(pop.length == solutionSet.size())
			return solutionSet.toArray(pop); // Reuse if we can
		else
			return solutionSet.toArray(new GeneticSolution[solutionSet.size()]);
	}
}
