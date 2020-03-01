package org.cloudbus.cloudsim.power;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;

/**
 * The class of an abstract power-aware VM allocation policy.
 * 
 * If you are using any algorithms, policies or workload included in the power package, please cite
 * the following paper:
 * 
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 3.0
 *
 * Modified to accept other allocation schemes 
 * findHostForVm2() by olasupoAjayi 
 * Last modified Sept2019
 */

public abstract class PowerVmAllocationPolicyAbstract_MQ extends VmAllocationPolicy {
	
	/** The vm table. */
	private final Map<String, Host> vmTable = new HashMap<String, Host>();
	private binarySearchBF bsbfx;
	private int oldHostListSize = 0;
	
	private int timeIndex = 0;	//to be used to get average search time
	private long avgTime = 0; //to be used with timeIndex
	private long minTime = Integer.MAX_VALUE;	//used to get shortest search time 

	public StableMarriage smg;
	public SimpleDemoGA svga;
	
	public Set<Integer> usedHosts = new HashSet<>();
	
	
	/**
	 * Instantiates a new power vm allocation policy abstract.
	 * 
	 * @param list the list
	 */
	public PowerVmAllocationPolicyAbstract_MQ(List<? extends Host> list) {
		super(list);
		bsbfx = new binarySearchBF(this.<PowerHost> getHostList());
		oldHostListSize = getHostList().size();
	}

	/*
	 * (non-Javadoc)
	 * @see org.cloudbus.cloudsim.VmAllocationPolicy#allocateHostForVm(org.cloudbus.cloudsim.Vm)
	 */
	
	@Override
	public boolean allocateHostForVm(Vm vm) {	
		return allocateHostForVm(vm, findHostForVm2(vm,5));
		//updated 18.09.2016
		//type 0 = firstFit / MQ-BAL/PABFD, 1 = BSBF, 5 = BFD, 2 = BestFit, 3 = WorstFit, 4 = RandomFit, 
		//type 6 = GA 		//type 7 = stableMarriage	8 = DE
	}

	/*
	 * (non-Javadoc)
	 * @see org.cloudbus.cloudsim.VmAllocationPolicy#allocateHostForVm(org.cloudbus.cloudsim.Vm,
	 * org.cloudbus.cloudsim.Host)
	 */
	@Override
	public boolean allocateHostForVm(Vm vm, Host host) {
		if (host == null) {
			//Log.formatLine("%.2f: No suitable host found for VM #" + vm.getId() + "\n", CloudSim.clock());
			return false;
		}
		if (host.vmCreate(vm)) { // if vm has been successfully created in the host
			getVmTable().put(vm.getUid(), host);
			//Log.formatLine("%.2f: VM #" + vm.getId() + " of MI = " + vm.getCurrentRequestedMips() + " has been allocated to the host BBB #" + host.getId() + "; TotalMIPS=" + host.getTotalMips()+ "; AvailableMIPS=" + host.getAvailableMips()+ "; RAM=" + host.getRam()+ "; RAMProvisioner=" + host.getRamProvisioner().getUsedRam()+ "; Storage=" + host.getStorage()+ "; BW=" + host.getBw()+ "; BWProvisioner=" + host.getBwProvisioner().getUsedBw(), CloudSim.clock());
			return true;
		}
		//Log.formatLine("%.2f: Creation of VM #" + vm.getId() + "of MI = " + vm.getCurrentRequestedMips() +" on the host #" + host.getId() +" with AvailableMIPS= " + host.getAvailableMips() + " failed\n", CloudSim.clock());
		return false;
	}

	
	public PowerHost findHostForVm2(Vm vm, int type) {
        
		PowerHost ph = null;
        //adding timer now for time ph search
		long duration = 0;
		long sT = System.nanoTime();
		
                
        Log.printLine("Looking for PM ");	
		switch (type)
		{
		
		case 0:
			//FirstFit
			FirstFit ff = new FirstFit(this.<PowerHost> getHostList());
			ph = ff.getHost(vm);
			break;
			
		case 1:
			//BSBF
			if (getHostList().size() > oldHostListSize)
			{
				bsbfx.updateBT(this.<PowerHost> getHostList());
				ph = bsbfx.getHostOnly(vm);
			}
			else
				ph = bsbfx.getHostOnly(vm);
			
			oldHostListSize = getHostList().size();	//reset 	
			//Log.printLine( "VM assigned to PM  " + ph.getId()); 
			break;
					
		case 2:
			//BestFit bf = new BestFit(this.<PowerHost> getHostList());
			//ph = bf.getHost(vm);
			break;
		
		case 3:
//			WorstFit wf = new WorstFit(this.<PowerHost> getHostList());
//			ph = wf.getHost(vm);
			break;
			
		case 4:
			//RandomFit
			RandomFit rf = new RandomFit(this.<PowerHost> getHostList());
			ph = rf.getHost(vm);
			break;					
		
		
		case 5:
			//BFD
			ph = findHostForVm(vm);
			break;
		
		case 6:
			//GA
			ph = this.<PowerHost> getHostList().get(	svga.findPM(vm.getId()));
			break;
			
		case 7:
			//SM	
			ph = this.<PowerHost> getHostList().get(smg.womanChoose(vm.getId()));
			//this works bcos vmList is an ordered set and the order is exactly equivalent to women list
			//ie vm0 on vmList is the same as woman0 on womenList
			if (ph == null)
			{
				smg.menPropose();
				ph = this.<PowerHost> getHostList().get(smg.forceMatch(vm.getId()));
			}
			break;
			
		case 8:
			ph = this.<PowerHost> getHostList().get(	smg.getHostbyDE(vm.getId()));
			break;
		}
		
		duration = System.nanoTime() - sT;		
		avgTime = ((timeIndex)*avgTime + duration)/(timeIndex+1);
		timeIndex++;
		if (duration <= minTime)
			minTime = duration;
		Log.printLine( "Found PM after " + duration + " ns \n Average search time = " + avgTime + " MinTime = " + minTime);
		
		usedHosts.add(ph.getId());
		Log.printLine( "Total Number of PM used so far is " + usedHosts.size()); 
		return ph;
	}

	
	/**
	 * Find host for vm.
	 * This is the same thing as Firstfit
	 * @param vm the vm
	 * @return the power host
	 */
	public PowerHost findHostForVm(Vm vm) {
                
		for (PowerHost host : this.<PowerHost> getHostList()) {
			if (host.isSuitableForVm(vm)) {
				return host;
			}
		}
		return null;
	}
	
	binarySearchBF bsbf = new binarySearchBF(this.<PowerHost> getHostList());
	public PowerHost findHostForVm_BSBF(Vm vm)
	{
		return bsbf.getHostOnly(vm); //findHostForVm(vm);//
	}
	/*
	 * (non-Javadoc)
	 * @see org.cloudbus.cloudsim.VmAllocationPolicy#deallocateHostForVm(org.cloudbus.cloudsim.Vm)
	 */
	@Override
	public void deallocateHostForVm(Vm vm) {
		Host host = getVmTable().remove(vm.getUid());
		if (host != null) {
			host.vmDestroy(vm);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.cloudbus.cloudsim.VmAllocationPolicy#getHost(org.cloudbus.cloudsim.Vm)
	 */
	@Override
	public Host getHost(Vm vm) {
		return getVmTable().get(vm.getUid());
	}

	/*
	 * (non-Javadoc)
	 * @see org.cloudbus.cloudsim.VmAllocationPolicy#getHost(int, int)
	 */
	@Override
	public Host getHost(int vmId, int userId) {
		return getVmTable().get(Vm.getUid(userId, vmId));
	}

	/**
	 * Gets the vm table.
	 * 
	 * @return the vm table
	 */
	public Map<String, Host> getVmTable() {
		return vmTable;
	}

}

