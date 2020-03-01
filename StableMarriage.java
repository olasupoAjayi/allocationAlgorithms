/*
This code plug into CloudSim and applies the stable roommate algorithm
to allocating user workloads to Cloud servers.
by Olasupo Ajayi June2019
*/
package org.cloudbus.cloudsim.power;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import org.cloudbus.cloudsim.Vm;

class StableMarriage {
  /**
	Converts lists into stablemarriage terms
	ie PMs => men and Vms => women
	In this implementations, the men make the proposals.
	Also Vms/women can be more than PM/Men
	
	ToDo: 
	Multiple women can be matched to one man
	This is actually STABLE ROOMMATE not STABLE MARRIAGE
*/

    public static void converter(List<Vm> vmList, List<PowerHost> hostList){
    	List<Woman> women = new ArrayList<>();	//list of vms
        List<Man> men = new ArrayList<>();	//list of pms
	
        for (Vm vm : vmList) {
        	women.add(new Woman(vm));
        }
	
        for (PowerHost h : hostList) {
        	men.add(new Man(h));
        }

        //assign preferences               
        women.forEach(w -> {
            w.receiveOptions(men);
            System.out.println(w + " prefers " + w.getPreferredMates());
        });
        men.forEach(m -> {
            m.receiveOptions(women);
            System.out.println(m + " prefers " + m.getPreferredMates());
        });
       
        //findStableMarriages(women, men);
    }

    /**
     * Use the stable marriage algorithm to find stable pairs from the
     * lists of men and women (host and vm).
     * PM = men and they propose. Vm = women
 	PM preferences = workload with MI requirehostts < its capacity
	Vm preference = PM with GREATEST AVAILABLE MIPS. 
     **/


    public static void findStableMarriages(List<Woman> women, List<Man> men) {
      
	List<? extends Person> leastCommonGender = women.size() <= men.size() ? women : men;
        do {        
            // Every single man proposes to a woman.
            for (Man man : men)
               // if (man.isLonely())	removed to allow multiple proposals
                    man.propose();

            // The women pick their favorite suitor.
            for (Woman woman : women)
                woman.chooseMate();

            // End the process if everybody has a mate.
            if (!leastCommonGender.stream().anyMatch(Person::isLonely))
                break;

        } while (true);

        women.forEach(w -> System.out.println(w + " allocated to " + w.getMate()));
    }

  
}

class Person {
    private final int id;
    protected Person mate;
    protected List<Person> preferredMates;
    private double MI;
    protected List<Person> mates4Men;

    public Person(int id, double mi) {
        this.id = id;
	this.MI = mi;
    }

    public boolean isLonely() {
	mates4Men = null;
        return mate == null;	
    }

    public void setMate(Person mate) {
        if (this.mate != mate) {	
          
	  // store Remove old mates mate.
            if (this.mate != null)
                this.mate.mate = null;

            // Set the new mate.
            this.mate = mate;

            // If new mate is someone, update their mate.
            if (mate != null)
                mate.mate = this;
        }
    }

    public Person getMate() {
        return mate;
    }

    public void receiveOptions(List<? extends Person> mates) {
        // Preferences are subjective.
        preferredMates = new ArrayList<>(mates);
        //Collections.shuffle(preferredMates);
    }

    public List<Person> getPreferredMates() {
        return preferredMates;
    }

    public int getId() {
        return this.id;
    }

    public double getMI() {
        return this.MI;
    }
}

class Woman extends Person {
  
	public Woman (Vm vm){
	 super(vm.getId(), vm.getCurrentRequestedTotalMips());		
	}

  	private List<Man> suitors = new ArrayList<>();

    @Override
    public void receiveOptions(List<? extends Person> mates) {
	//mates shld b a list of PMs

        preferredMates = new ArrayList<>();
	for (Person x : mates)	
	{
		if (x.getMI() >= this.getMI())
			preferredMates.add(x);	//if d PM cannot support the Vm then no point adding it in preference list
	}
	
    }

    public void recieveProposal(Man suitor) {
        suitors.add(suitor);
    }

    public void chooseMate() {
        for (Person mostDesired : preferredMates) {
            if (mostDesired == mate || suitors.contains(mostDesired)) {
                setMate(mostDesired);
                break;
            }
        }
    }
}

class Man extends Person {
	
    public Man(PowerHost pm) {
        super(pm.getId(), pm.getAvailableMips());
    }

    @Override
    public void receiveOptions(List<? extends Person> mates) {
	//mates shld b a list of Vms

    	preferredMates = new ArrayList<>();
    	for (Person x : mates)	
    	{
    		if (x.getMI() <= this.getMI())
    			preferredMates.add(x);	
    	}
    }

    public void propose() {
    	double totalMIPS = 0.0;	//cumulative mi of all the women he can handle
        //if (!preferredMates.isEmpty()) {
    	while (!preferredMates.isEmpty()) {	//to allow multiple propsal as long as u can handle dem
    		Woman fiance = (Woman) preferredMates.remove(0);
    		totalMIPS += fiance.getMI();
    		if (this.getMI() >= totalMIPS) {	//check if he can handle xtra
	            fiance.recieveProposal(this);	
    		}
		//else
		//preferredMates.add(fiance);		//put her back in his list
        }
    }

}
