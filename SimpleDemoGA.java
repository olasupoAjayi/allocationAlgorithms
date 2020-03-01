package org.cloudbus.cloudsim.power;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Vm;

/**
 *
 * @author OlasupoAjayi Sept2019
 * Extension of the original SimpleDemoGA by Vijini
 * Adapted for Cloud workload allocation in CloudSim
 */

//Main class
public class SimpleDemoGA {

	Population population = new Population();
	Individual fittest;
	Individual secondFittest;
	int generationCount = 0;
	List<Workload> workloads = new ArrayList<>();	//list of vms
    List<Server> servers = new ArrayList<>();	//list of pms
    Individual result;
    
	public SimpleDemoGA () {
		
	}
	
	public SimpleDemoGA (List<Vm> vmList, List<PowerHost> hostList) {
		//converter(vmList, hostList);
	}
	
	public void converter(List<Vm> vmList, List<PowerHost> hostList) {
				
		 for (Vm vm : vmList) { workloads.add(new Workload(vm)); }
		 
		 for (PowerHost h : hostList) { servers.add(new Server(h)); }	 
		 
		 this.result = find(30, hostList);
		
	}

	public int findPM (int vmId) { //, List<PowerHost> hostList) {
		return extractPM(vmId, this.result);
	}
	
	public Individual find(int popSize, List<PowerHost> hostList) {

		Random rn = new Random();
		int sSize = servers.size();
		int itNo = sSize * 65 / 100;	//65% fitness
		
		// Initialize population
		population.initializePopulation(popSize, servers, workloads, hostList);

		// Calculate fitness of each individual
		population.calculateFitness();

		System.out.println("Generation: " + generationCount + " Fittest: " + population.fittest);

		//double stop = 600;
		// repeat until best combination with greatest total free MIPS is found 
		while ( population.fittest > 390)  {		
			generationCount++;
			
			// Do selection
			selection();

			// Do crossover
			crossover();

			// Do mutation under a random probability
			if (rn.nextInt() % 7  < 5) {
				//System.out.println("MUTATING NOW...");
				mutation();
			}

			// Add fittest offspring to population
			addFittestOffspring();

			//stop = population.fittest;
			
			// Calculate new fitness value
			population.calculateFitness();
			
			System.out.println("Generation: " + generationCount + " Fittest: " + population.fittest);
		}		
		
		
		
		/*
		 * System.out.println("\nSolution found in generation " + generationCount);
		 * System.out.println("Fitness: " + population.getFittest().fitness);
		 * System.out.print("Genes: "); for (int i = 0; i < sSize; i++) {
		 * System.out.print(population.getFittest().genes[i]); }
		 * 
		 * System.out.println("");
		 */
		 
		return this.result = population.getFittest(); 
	}

	public int extractPM(int vm_id, Individual best) {
		
		//1. get the vm from workload 	//shld b one on one match
		Workload w = workloads.get(vm_id);
		int x = 0;
		//2 search 4 the associated PM
		for (; x < best.geneLength; x++) {
					if (best.getAssociatedMate2(x).assignedMate.contains(w)) 
						break;			
		}
		return best.getAssociatedMate(x);
	}
	
	
	// Selection
	void selection() {

		// Select the most fittest individual
		fittest = population.getFittest();

		// Select the second most fittest individual
		secondFittest = population.getSecondFittest();
	}

	/** Crossover
	 * This swaps genes in individuals
	 * It might fail though if : ... check this - d encoding of 0 n 1 
	 */
	void crossover() {
		Random rn = new Random();

		// Select a random crossover point
		int crossOverPoint = rn.nextInt(population.individuals[0].geneLength);

		// Swap values among parents
		for (int i = 0; i < crossOverPoint; i++) {
			double temp = fittest.genes[i];
			fittest.genes[i] = secondFittest.genes[i];
			secondFittest.genes[i] = temp;
			
			//swap the initially assigned workloads also
			List<Existence> wkTemp = fittest.getAssociatedMate2(i).assignedMate;
			fittest.setAssociatedMate(i, secondFittest.getAssociatedMate2(i).assignedMate);
			secondFittest.setAssociatedMate(i,wkTemp);			
		}

	}

	/**Mutation
	 * Changed to partial mutation
	 * This means u can swap 1 with 0 but not 0 with 1
	 * Because of the encoding scheme
	 * 0 means no more space
	 * 1 means space available
	 */
	void mutation() {
		Random rn = new Random();

		// Select a random mutation point
		int mutationPoint = rn.nextInt(population.individuals[0].geneLength);

		// Flip values at the mutation point
		if (fittest.genes[mutationPoint] == 0) { //1) {
			fittest.genes[mutationPoint] = 1;
			// I have more space bcoms I dont have space 
		} 
		//else {
			//fittest.genes[mutationPoint] = 0;
		//}

		mutationPoint = rn.nextInt(population.individuals[0].geneLength);

		if (secondFittest.genes[mutationPoint] == 0) {
			secondFittest.genes[mutationPoint] = 1;
		} 
		//else {
			//secondFittest.genes[mutationPoint] = 0;
		//}
	}

	// Get fittest offspring
	Individual getFittestOffspring() {
		if (fittest.fitness > secondFittest.fitness) {
			return fittest;
		}
		return secondFittest;
	}

	// Replace least fittest individual from most fittest offspring
	void addFittestOffspring() {

		// Update fitness values of offspring
		fittest.calcFitness();
		secondFittest.calcFitness();

		// Get index of least fit individual
		int leastFittestIndex = population.getLeastFittestIndex();

		// Replace least fittest individual from most fittest offspring
		population.individuals[leastFittestIndex] = getFittestOffspring();
	}

}

/**
 * This is a collection of workloads matched to server  
 * @author ola
 *
 */
class Individual {

	double fitness = 0;
	int geneLength = 50; // default number of servers
	double[] genes; //hostList. gene = pm
	List <Server> as = new ArrayList<Server>(); 
	/**Individuals
	 * Creates a set of VM - PM matching
	 * Also encodes genes using 0 if the PM cannot take more workload
	 * and 1 if it can
	 * 
	 * @param sList
	 * @param wList
	 * Returns a potential allocation match / solution
	 */
	public Individual(List<Server> sList, List<Workload> wList) {
		this.as = sList;
		this.geneLength = sList.size() * 2;	//actual number of servers set here
		genes = new double[geneLength]; // 
		Random rn = new Random();		
		
		//1. simulate assignment of workload to server's PE
		for (Workload w :wList ) {
			int r = 0;
			do {
			   	r = rn.nextInt(sList.size());
	            	//used 2 handle a bug in cloudsim PE provisioner that sets pe0 to 0 and adds its value to pe1
			   	if ((as.get(r).getThePmPe0_remaining()== 0) && (as.get(r).getThePmPe1_remaining()>= 500)) {
			   		if ((as.get(r).getThePmPe1_size() / 2.5) >= w.getCapacity()) {
			   			as.get(r).setAssignedMate(w);
	            		as.get(r).setCapacityRemaining(as.get(r).getCapacityRemaining() - w.getCapacity());
	            		as.get(r).setThePmPe1_remaining(as.get(r).getThePmPe1_remaining() - w.getCapacity());
	            		break;
			   		}
			   	}
			   			
			   	if (as.get(r).getThePmPe0_remaining()>= w.getCapacity()){
			   		as.get(r).setAssignedMate(w);
            		as.get(r).setCapacityRemaining(as.get(r).getCapacityRemaining() - w.getCapacity());
            		as.get(r).setThePmPe0_remaining(as.get(r).getThePmPe0_remaining() - w.getCapacity());
            		break;			   		
			   	}
			   if (as.get(r).getThePmPe1_remaining()>= w.getCapacity()) {
				    as.get(r).setAssignedMate(w);
           			as.get(r).setCapacityRemaining(as.get(r).getCapacityRemaining() - w.getCapacity());
           			as.get(r).setThePmPe1_remaining(as.get(r).getThePmPe1_remaining() - w.getCapacity());
           			break;		
			   }  
			}while (true);	//continue until u find a suitable pm
	          
	         // System.out.println("Found Potential PM " + r + " for vm " + w.getId());
	          
	    }		
		//2. //encode genes 
		//if PE cannot take any more job, set gene to 0 else set to 1
		
		/*
		 * for (int s = 0; s < sList.size(); s++) { if
		 * (!as.get(s).getAssignedMate().isEmpty()) {
		 * 
		 * if (as.get(s).getThePmPe0_remaining() >= 500) genes[s*2] = 1; else genes[s*2]
		 * = 0;
		 * 
		 * if(sList.get(s).getThePmPe1_remaining() >= 500)/// 2.5 >= 500) genes[(s*2)+1]
		 * = 1; else genes[(s*2)+1] = 0; } else{ //to encode unassigned/un-utilized PMs
		 * genes[s*2] = 0; genes[(s*2)+1] = 0; } }
		 */
		
		//2. GENE ENCODING
		//1 if PE is used, 0 if not used
		for (int s = 0; s < sList.size(); s++) {
			if (!as.get(s).getAssignedMate().isEmpty()) {
				if (as.get(s).getThePmPe0_remaining() < as.get(s).getThePmPe0_size())
					genes[s*2] = 1;	//used
				else 
						genes[s*2] = 0;	//not used
				
				if (as.get(s).getThePmPe1_remaining() < as.get(s).getThePmPe1_size())
					genes[(s*2)+1] = 1;	//used
				else 
						genes[(s*2)+1] = 0;	//unused
			}
			else {	//not used
				genes[s*2] = 0;
				genes[(s*2)+1] = 0;
			}
		}
		}
	
	
	/**Calculate fitness
	 * Returns the amount of servers being utilized. The lower the better
	 * @param sList
	 */
	public void calcFitness() {
		
		fitness = 0;
		/*//fitness = amount of free space
			//the greater the amount of free space the better
		 * 
		 * for (int i = 0; i < genes.length / 2; i++) { fitness +=
		 * as.get(i).getThePmPe0_remaining(); fitness +=
		 * as.get(i).getThePmPe1_remaining();
		 * 
		 * // fittest.getAssociatedMate2(i).assignedMate; }
		 */
		
		for (int i = 0; i < genes.length; i++) {
			//count number of 1s and 0s
			//fitness = number of un-used ie no of zeros
			if (genes[i] == 0)
				fitness++;
		}
	}
	
	/**
	 * Converts gene back to Server / PM
	 * @param g index of gene
	 * @return id of associated Server or PM
	 */
	public int getAssociatedMate(int g)
	{
		if (g % 2 == 0) {	//even number
			return as.get(g/2).getId();
		}
		else
			return as.get((g-1)/2).getId();		
	}
	/**
	 * 
	 * @param g index of the gene of interest
	 * @return returns the Server associated with the requested gene g
	 */
	public Server getAssociatedMate2(int g)
	{
		if (g % 2 == 0) {	//even number
			return as.get(g/2);
		}
		else
			return as.get((g-1)/2);		
	}
	
	/**
	 * 
	 * @param g index of the gene of interest
	 * @param associatedWorkloads
	 *  used for cross-over to swap associated workloads btw servers
	 */
	public void setAssociatedMate(int g, List<Existence> associatedWorkloads) {
		
		if (g % 2 == 0) {	//even number
			as.get(g/2).setAssignedMate(associatedWorkloads);
		}
		else
			as.get((g-1)/2).setAssignedMate(associatedWorkloads);		
	}
	
}

//Population class
class Population {

	int popSize = 0; // different combinations of assignments
	Individual[] individuals; 
	double fittest = 0;
	List<Server> sv2 = new ArrayList<Server>() ;
	List<Workload> wk2 = new ArrayList<Workload>();

	/**RESET SERVER
	 * 
	 * @param size     number of individuals in the population
	 * @param hostList list of Physical machines
	 * This is needed in order to get new individuals in the population
who are not in anyway related / associated with previously created individuals
Basically 2 reset the MIPS utilization 
 
	 */
	 
	 private List<Server> resetSv(List<PowerHost> ph_){
		 List<Server> newServers = new ArrayList<Server>();
		 for (PowerHost h : ph_) { newServers.add(new Server(h)); }
	 		return newServers;
	 }	 
	 
	public void initializePopulation(int size, List<Server> svList, List<Workload> wkList, List<PowerHost> hostList) {

		this.popSize = size;
		individuals = new Individual[popSize];	
		
		for (int i = 0; i < individuals.length; i++) {				
			individuals[i] = new Individual( svList, wkList);
			svList = resetSv(hostList);
						
			//System.out.println("====================ITERATION " +i+ "======================");
			;
		}
		/*
		 * for (Individual id : individuals) { for (int g = 0; g < id.geneLength; g++)
		 * System.out.print(id.genes[g]);
		 * System.out.println("========INDIVIDUAL ============= \n" ); }
		 */
		
	}

	// Get the fittest individual
	public Individual getFittest() {
		double maxFit = Integer.MIN_VALUE;
		int maxFitIndex = 0;
		for (int i = 0; i < individuals.length; i++) {
			System.out.println("Individual "+ i+ " Fitness is " + individuals[i].fitness );
			if (maxFit <= individuals[i].fitness) {				
				maxFit = individuals[i].fitness;
				maxFitIndex = i;
			}			
		}
		fittest = individuals[maxFitIndex].fitness;
		return individuals[maxFitIndex];
	}

	// Get the second most fittest individual
	public Individual getSecondFittest() {
		int maxFit1 = 0;
		int maxFit2 = 0;
		for (int i = 0; i < individuals.length; i++) {
			if (individuals[i].fitness > individuals[maxFit1].fitness) {
				maxFit2 = maxFit1;
				maxFit1 = i;
			} else if (individuals[i].fitness > individuals[maxFit2].fitness) {
				maxFit2 = i;
			}
		}
		return individuals[maxFit2];
	}

	// Get index of least fittest individual
	public int getLeastFittestIndex() {
		double minFitVal = Integer.MAX_VALUE;
		int minFitIndex = 0;
		for (int i = 0; i < individuals.length; i++) {
			if (minFitVal >= individuals[i].fitness) {
				minFitVal = individuals[i].fitness;
				minFitIndex = i;
			}
		}
		return minFitIndex;
	}

	// Calculate fitness of each individual
	public void calculateFitness() {

		for (int i = 0; i < individuals.length; i++) {
			individuals[i].calcFitness();
		}
		getFittest();
	}

}


class Existence {
	
	private final int id;
	private double capacity;	
	protected List<Existence> assignedMate;
	
	public Existence (int id, double mi) {
	        this.id = id;
	        this.capacity = mi;
	    }
	  public int getId() {
	        return this.id;
	    }

	    public double getCapacity() {
	        return this.capacity;
	    }
	    
		public List<Existence> getAssignedMate() {
				return assignedMate;
		}
		
		public void setAssignedMate(Existence e) {
				this.assignedMate.add(e);
		}
		
		public void setAssignedMate(List<Existence> assignedMate) {
			this.assignedMate = assignedMate;
		}
		
		
}


class Workload extends Existence {
	
	public Workload (Vm vm){
		 super(vm.getId(), vm.getCurrentRequestedTotalMips());		
		}
}

class Server extends Existence {
	protected PowerHost thePm;
	protected double thePmPe0_size;
	protected double thePmPe0_remaining;
	protected double thePmPe1_size;
	protected double thePmPe1_remaining;
	private double capacityRemaining;
	
	public Server (PowerHost pm) {
        super(pm.getId(), pm.getAvailableMips());
        this.thePm = pm;        
        this.thePmPe0_size = this.thePmPe0_remaining = pm.getPeList().get(0).getPeProvisioner().getAvailableMips();
        this.thePmPe1_size = this.thePmPe1_remaining =pm.getPeList().get(1).getPeProvisioner().getAvailableMips();
        this.setCapacityRemaining(this.getCapacity());
        this.assignedMate = new ArrayList<Existence>();
    }
	
	public void holdWorkload(Existence w) {
		this.setCapacityRemaining(this.getCapacity() - w.getCapacity());
	}
	
    public double getCapacityRemaining() {
        return this.capacityRemaining;
    }
    
    public void setCapacityRemaining(double value) {
        this.capacityRemaining = value;
    }
	public double getThePmPe0_remaining() {
		return thePmPe0_remaining;
	}
	public void setThePmPe0_remaining(double thePmPe0_remaining) {
		this.thePmPe0_remaining = thePmPe0_remaining;
	}
	public double getThePmPe1_remaining() {
		return thePmPe1_remaining;
	}
	public void setThePmPe1_remaining(double thePmPe1_remaining) {
		this.thePmPe1_remaining = thePmPe1_remaining;
	}
	public double getThePmPe0_size() {
		return thePmPe0_size;
	}
	public double getThePmPe1_size() {
		return thePmPe1_size;
	}
}

