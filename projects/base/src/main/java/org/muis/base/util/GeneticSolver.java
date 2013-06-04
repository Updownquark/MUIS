package org.muis.base.util;

import java.util.Iterator;
import java.util.Random;

public class GeneticSolver<T> {
	public static class Genome {
		private float [] theGenes;

		public Genome(int count) {
			theGenes = new float[count];
		}

		public float [] getGenes() {
			return theGenes;
		}

		void mate(Genome mate, float mutationFactor, Random random, Genome child) {
			for(int i = 0; i < theGenes.length; i++) {
				float rand = getMutation(random.nextFloat(), mutationFactor);
				child.theGenes[i] = theGenes[i] + (mate.theGenes[i] - theGenes[i]) * rand;
				if((rand * 1000) % 13 <= 2)
					child.theGenes[i] += getMutation(random.nextFloat(), mutationFactor);
			}
		}

		void mutate(float mutationFactor, Random random, Genome spawn) {
			for(int i = 0; i < theGenes.length; i++) {
				float rand = getMutation(random.nextFloat(), mutationFactor);
				spawn.theGenes[i] = theGenes[i] * rand;
				if((rand * 1000) % 13 <= 2)
					spawn.theGenes[i] += getMutation(random.nextFloat(), mutationFactor);
			}
		}
	}

	public static interface GenomeTester extends AutoCloseable {
		float getFitness(Genome genome);

		@Override
		void close();
	}

	public static interface GenomeTesterMaker<T> {
		GenomeTester createTester(T metadata);
	}

	public static class GeneticSolution implements Comparable<GeneticSolution> {
		Genome genome;

		float fitness;

		GeneticSolution(int size) {
			genome = new Genome(size);
		}

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

		public Genome getGenome() {
			return genome;
		}

		public float getFitness() {
			return fitness;
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

	private static class SolutionPool {
		private java.util.concurrent.ConcurrentLinkedQueue<GeneticSolution> theSolutionSet;

		private int theGenomeSize;

		SolutionPool(int genomeSize) {
			theSolutionSet = new java.util.concurrent.ConcurrentLinkedQueue<>();
			theGenomeSize = genomeSize;
		}

		GeneticSolution get() {
			GeneticSolution ret = theSolutionSet.poll();
			if(ret == null)
				ret = new GeneticSolution(theGenomeSize);
			return ret;
		}

		void retire(GeneticSolution solution) {
			theSolutionSet.add(solution);
		}
	}

	private static final int TRIES = 5;

	private static final float MUTATION_RATE = 1.3f;

	private static final int MATE_PREFERENCE_FACTOR = 4;

	private final GenomeTesterMaker<T> theTesterMaker;

	private Random theRandom;

	private int theMinSolutions;

	private float theMinFitness;

	private int theMaxGenerations;

	private int thePopulationCap;

	public GeneticSolver(GenomeTesterMaker<T> testerMaker) {
		theTesterMaker = testerMaker;
		theRandom = new Random();
		theMinSolutions = 1;
		theMinFitness = 100;
		theMaxGenerations = 10;
		thePopulationCap = 100;
	}

	public void setRandom(Random random) {
		theRandom = random;
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

	public GeneticResults solve(T metaData, Genome... init) {
		if(init.length == 0)
			throw new IllegalArgumentException("Genetic solver requires one or more initialization genomes");
		int genomeSize = init[0].getGenes().length;
		for(int i = 1; i < init.length; i++) {
			if(init[i].getGenes().length != genomeSize)
				throw new IllegalArgumentException("All initialization genomes must be of the same size");
		}
		SolutionPool pool = new SolutionPool(init[0].getGenes().length);
		GenomeTester tester = theTesterMaker.createTester(metaData);
		GeneticSolution [] population = new GeneticSolution[init.length];
		for(int i = 0; i < init.length; i++)
			population[i] = new GeneticSolution(init[i], tester.getFitness(init[i]));
		java.util.Arrays.sort(population);
		int generations;
		java.util.TreeSet<GeneticSolution> solutionSet = new java.util.TreeSet<>();
		for(generations = 0; generations < theMaxGenerations && population.length > 0; generations++) {
			population = evolve(population, solutionSet, tester, pool);
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

	private float nr() {
		return theRandom.nextFloat();
	}

	private GeneticSolution [] evolve(GeneticSolution [] pop, java.util.TreeSet<GeneticSolution> solutionSet, GenomeTester tester,
		SolutionPool pool) {
		for(int p = 0; p < pop.length; p++) {
			solutionSet.add(pop[p]);
			while(solutionSet.size() > thePopulationCap)
				pool.retire(solutionSet.pollLast());

			// Mutate each genome a certain number of times--fit genomes get mutated more times
			int numMutations = (int) Math.ceil(thePopulationCap * 2f / pop.length / p);
			for(int mutation = 0; mutation < numMutations; mutation++) {
				for(int tries = 0; tries < TRIES; tries++) {
					GeneticSolution child = pool.get();
					pop[p].genome.mutate((mutation + 1f) / numMutations, theRandom, child.genome);
					child.fitness = tester.getFitness(child.genome);
					if(child.fitness != Float.NEGATIVE_INFINITY) {
						solutionSet.add(child);
						while(solutionSet.size() > thePopulationCap)
							pool.retire(solutionSet.pollLast());
						break;
					} else {
						pool.retire(child);
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
						int mateTry = Math.round(nr() * (pop.length - 1));
						if(mateTry < mateIdx)
							mateIdx = mateTry;
					}
					// Remove the possibility of something mating with itself
					if(mateIdx >= p)
						mateIdx++;
					GeneticSolution child = pool.get();
					pop[p].genome.mate(pop[mateIdx].genome, mateNum * 1f / numMates, theRandom, child.genome);
					child.fitness = tester.getFitness(child.genome);
					if(child.fitness != Float.NEGATIVE_INFINITY) {
						solutionSet.add(child);
						while(solutionSet.size() > thePopulationCap)
							pool.retire(solutionSet.pollLast());
						break;
					} else {
						pool.retire(child);
					}
				}
			}
		}

		if(pop.length == solutionSet.size())
			return solutionSet.toArray(pop); // Reuse if we can
		else
			return solutionSet.toArray(new GeneticSolution[solutionSet.size()]);
	}

	static float getMutation(float random, float mutationFactor) {
		return ((random - .5f) * MUTATION_RATE + MUTATION_RATE / 2f) * mutationFactor;
	}
}
