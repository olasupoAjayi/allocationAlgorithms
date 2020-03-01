package org.cloudbus.cloudsim.power;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;

import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

//import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyMigrationAbstract_UP;
/**
 * This class binarySearch replaces the conventional LinearSearch 
 * with Binary Search (Red-black). In a bid to reduce VM allocation 
 * Time in DCs with large number of PM. IE from o(n) to o(log(n))
 * If an exact matching host is not found, the next closest match (greater than it)
 * is returned
 * 
 * @author ooAjayi 
 * @since 24.05.2016
 * 
 */

class  binarySearchBF   
	{		
		double reqdMIPS;
		
		PowerHost selectedHost = null;
		List <PowerHost> hosts = null;
		private  Multimap<Double, PowerHost> BTree;
		
		binarySearchBF(List <PowerHost> p )
		{
			this.hosts = p;			
			BTree = createHostListofAvailableMIPS();
		}
		
		//called occassionally to update the BTree
		public void updateBT(List <PowerHost> p)
		{
			this.hosts = p;
			BTree = createHostListofAvailableMIPS();
		}
				
		/**
		 * 
		 * @return a Multimap of AvailableMIPS and PowerHost
		 * Its a balanced red-black Tree hence with log (n) speed
		 * Uses Guava's Multimap and
		 * Returns the best fit PM for the required MIPS 
		 * @author olasupoAjayi
		 * Changed to b called only by constructor. on 15.12.2016
		 */
		Multimap<Double, PowerHost> createHostListofAvailableMIPS()
		{
			List<PowerHost> tempList = hosts;
			Multimap<Double, PowerHost> listOfAvailableMIPS = TreeMultimap.create(Ordering.natural(), Ordering.arbitrary());
											
			for (PowerHost p : tempList)
			{
				//build tree
				listOfAvailableMIPS.put(p.getAvailableMips(), p);
			}
			//setBTree(listOfAvailableMIPS);
			return listOfAvailableMIPS;
		}
		
		public Multimap<Double, PowerHost> getBTree()
		{
			return BTree;
		}
				
		/**
		 * This method searches for the best matching PM for a required vm's MIPS
		 * getHost also checks if the selected PM has a utilization of zero. getHostOnly does not do this 
		 * @param vm
		 * @return PowerHost
		 */
		PowerHost getHost (Vm vm)
		{
			this.reqdMIPS = vm.getMips();
			//Multimap<Double, PowerHost> sortedPHs = createHostListofAvailableMIPS();
			Multimap<Double, PowerHost> sortedPHs = getBTree();
			PowerHost selectedHost = null;					
			//gets mips that are >= reqdMIPS ie best matches
			//Double x = TreeMultimap.<Double, PowerHost>create().keySet().ceiling(reqdMIPS);
			NavigableSet<Double> ns =  (NavigableSet<Double>) sortedPHs.keySet();
			Double x = ns.ceiling(reqdMIPS);
			
			//get all PMs that can provide reqdMIPS
			Collection<PowerHost> selectedHost1 = sortedPHs.get(x);
			Iterator<PowerHost> i = selectedHost1.iterator();  					
			
			while (i.hasNext())
			{
				PowerHost s = i.next(); 
				if (s.isSuitableForVm(vm))
					return  selectedHost = s;
	
//lines 105 to 116 work! lines 102-103 were used 2 replace 105-116 4 a particular test on 2.05.2019 ooAjayi				
//				if (!s.isSuitableForVm(vm))
//				{
//					continue;
//				}
//				else 
//				{
//					if (getUtilizationOfCpuMips(s) == 0) //&& isHostOverUtilizedAfterAllocation(selectedHost, vm))
//						return  selectedHost = s;
//					else
//						continue;
//				}
//				
				
			}	
			//if best match isnt found, return FIRST match
			if (selectedHost == null)
				selectedHost = adjuster(vm, selectedHost1, sortedPHs, null);			
			
			return selectedHost;				
		}	
		
		/**
		 * This method recursively searches for a suitable PM
		 * It is called by all getHost methods as a last resort to finding 
		 * a suitable PM. It returns the next first fit
		 * @param vm
		 * @param possibleHosts
		 * @param tree
		 * @return
		 */
		PowerHost adjuster (Vm vm, Collection<PowerHost> possibleHosts, Multimap<Double, PowerHost> tree, Set<? extends Host> excludedHosts)
		{			
			//remove unsuitable hosts ie previously tested collections
			Iterator keyItr = tree.keySet().iterator();
			keyItr.next();
			keyItr.remove();
						
			//get next matching PMs
			NavigableSet<Double> ns =  (NavigableSet<Double>) tree.keySet();
			Double x = ns.ceiling(vm.getMips());
				
			PowerHost sHost = null;
			Collection<PowerHost> nextKeySet = tree.get(x);
			Iterator<PowerHost> i = nextKeySet.iterator();
			
			//System.out.println(ns);
			while (i.hasNext())
			{
				PowerHost s = i.next();
				
				if (excludedHosts.contains(s))
				{
					//i.remove();
					continue;	
					//==check effect of this, continue rather than remove d PM== 26.05.2016
					//checked = no difference at all
				}
				if (s.isSuitableForVm(vm))
				{
					if (getUtilizationOfCpuMips(s) != 0)// && isHostOverUtilizedAfterAllocation(s, vm))
					{	
						continue;							
					}
					sHost = s;
				}								
			}			
			//if u get to this point then u need to re-iterate
			if (sHost == null)				
			sHost = adjuster(vm, nextKeySet, tree, excludedHosts);
			return sHost;
			//option 2 use for (Collection c : multimap.asMap().values()){}
		}
		
		/**
		 * Used in the initialization / allocation stage by some classes
		 * PowerVmAllocation 
		 * @param vm
		 * @return
		 */
		PowerHost getHostOnly (Vm vm)
		{
			this.reqdMIPS = vm.getMips();
			//Multimap<Double, PowerHost> sortedPHs = createHostListofAvailableMIPS();
			Multimap<Double, PowerHost> sortedPHs = getBTree();
						
			//get next matching PMs
			NavigableSet<Double> ns =  (NavigableSet<Double>) sortedPHs.keySet();
			Double x = ns.ceiling(reqdMIPS);
			
			PowerHost sHost = null;
			Collection<PowerHost> nextKeySet = sortedPHs.get(x);
			Iterator<PowerHost> i = nextKeySet.iterator();
			
			while (i.hasNext())
			{
				PowerHost s = i.next(); 
				if (s.isSuitableForVm(vm))
				{
					return sHost = s;
					//break;
				}
				else
					continue; 						
			}
			//if after looping thru d first collection and no PH found, then call in the adjuster
			if (sHost == null)
			{
				try
				{
				sHost = adjusterOnly(vm, nextKeySet, sortedPHs);
				}
				catch (Exception e)
				{
					sHost = null;
				}
			}
			return sHost;
							
		}
		
		PowerHost adjusterOnly (Vm vm, Collection<PowerHost> possibleHosts, Multimap<Double, PowerHost> tree)
		{			
			//remove unsuitable hosts ie previously tested collections
			Iterator keyItr = tree.keySet().iterator();
			keyItr.next();
			keyItr.remove();
						
			//get next matching PMs
			NavigableSet<Double> ns =  (NavigableSet<Double>) tree.keySet();
			Double x = ns.ceiling(vm.getMips());
				
			PowerHost sHost = null;
			Collection<PowerHost> nextKeySet = tree.get(x);
			Iterator<PowerHost> i = nextKeySet.iterator();
						
			while (i.hasNext())
			{
				PowerHost s = i.next(); 
				if (!s.isSuitableForVm(vm))
					continue;
				else
					return  sHost = s;
											
			}
			//if u get to this point then u need to re-iterate
			if (sHost == null)				
			sHost = adjusterOnly(vm, nextKeySet, tree);
			return sHost;
			//option 2 use for (Collection c : multimap.asMap().values()){}
		}
		
		
		
		PowerHost getHost (Vm vm, Set<? extends Host> excludedHosts)
		{
			this.reqdMIPS = vm.getMips();
			//Multimap<Double, PowerHost> sortedPHs = createHostListofAvailableMIPS();
			Multimap<Double, PowerHost> sortedPHs = getBTree();
			PowerHost sHost = null;
			
			//get next matching PMs
			NavigableSet<Double> ns = (NavigableSet<Double>) sortedPHs.keySet();
			Double x = ns.ceiling(reqdMIPS);
						
			Collection<PowerHost> nextKeySet = sortedPHs.get(x);
			Iterator<PowerHost> vItr = nextKeySet.iterator();
			
			while (vItr.hasNext())
			{
				PowerHost s = vItr.next();
				
				if (excludedHosts.contains(s))
				{
					//vItr.remove();
					continue;	
					//==check effect of this, continue rather than remove d PM== 26.05.2016
					//checked = no difference at all
				}
				if (s.isSuitableForVm(vm))
				{
					if (getUtilizationOfCpuMips(s) != 0) //&& isHostOverUtilizedAfterAllocation(selectedHost, vm))
					{	
						continue;							
					}
					sHost = s;
				}								
			}		
			
			if (sHost == null)
			{
				try
				{
					sHost = adjuster(vm, nextKeySet, sortedPHs, excludedHosts);
				}
				catch (Exception e)
				{
					sHost = null;
				}
			}
							
			
			return sHost;		
			
		}
		
		
		/**
		 * Gets the utilization of the CPU in MIPS for the current potentially allocated VMs.
		 *
		 * @param host the host
		 *
		 * @return the utilization of the CPU in MIPS
		 */
		double getUtilizationOfCpuMips(PowerHost host) {
			double hostUtilizationMips = 0;
			for (Vm vm2 : host.getVmList()) {
				if (host.getVmsMigratingIn().contains(vm2)) {
					// calculate additional potential CPU usage of a migrating in VM
					hostUtilizationMips += host.getTotalAllocatedMipsForVm(vm2) * 0.9 / 0.1;
				}
				hostUtilizationMips += host.getTotalAllocatedMipsForVm(vm2);
			}
			return hostUtilizationMips;
		}	
		
//		protected boolean isHostOverUtilizedAfterAllocation(PowerHost host, Vm vm) {
//			boolean isHostOverUtilizedAfterAllocation = true;
//			if (host.vmCreate(vm)) {
//				isHostOverUtilizedAfterAllocation = isHostOverUtilized_UP(host);
//				host.vmDestroy(vm);
//			}
//			return isHostOverUtilizedAfterAllocation;
//		}
		
/**
 * Old approach		
 * 
 *
		TreeMap<Double, PowerHost> createHostListAvailableMIPS()
		{
			List<PowerHost> tempList = hosts;//v.getHostList();						
			TreeMap<Double, PowerHost> listOfAvailableMIPS = new TreeMap<Double, PowerHost>();
			
			//<HostID and HostAvailableMIPS>
						
			for (PowerHost p : tempList)
			{
				//build tree
				listOfAvailableMIPS.put(p.getAvailableMips(), p);
			}			
			
			return listOfAvailableMIPS;
		}
		
		PowerHost getHost1 (Vm vm)
		{
			this.reqdMIPS = vm.getMips();
			TreeMap<Double, PowerHost> sortedPHs = createHostListAvailableMIPS();
			List<PowerHost> exHosts = new LinkedList<PowerHost>();
			
			selectedHost = sortedPHs.get(sortedPHs.ceilingKey(reqdMIPS));
			while (!selectedHost.isSuitableForVm(vm))
			{
				exHosts.add(selectedHost);
				selectedHost = adjuster1(vm, exHosts, sortedPHs);
			}
				//if (selectedHost.isSuitableForVm(vm))
					if (getUtilizationOfCpuMips(selectedHost) == 0) //&& isHostOverUtilizedAfterAllocation(selectedHost, vm)) 
						return  selectedHost;			//check last if statement
		
					else return null;
				
		}	
		
		PowerHost getHostOnly1 (Vm vm)
		{
			this.reqdMIPS = vm.getMips();
			TreeMap<Double, PowerHost> sortedPHs = createHostListAvailableMIPS();
			List<PowerHost> exHosts = new LinkedList<PowerHost>();
			
			selectedHost = sortedPHs.get(sortedPHs.ceilingKey(reqdMIPS));
			while (!selectedHost.isSuitableForVm(vm))
			{
				exHosts.add(selectedHost);
				selectedHost = adjuster1(vm, exHosts, sortedPHs);
			}				 
			return  selectedHost;			//check last if statement				
		}
		
		PowerHost getHost1 (Vm vm, List<PowerHost> excludedHosts)
		{
			this.reqdMIPS = vm.getMips();
			TreeMap<Double, PowerHost> sortedPHs = createHostListAvailableMIPS(); 
						
			for (PowerHost p : excludedHosts)
			{
				//removes all excludedHosts from the tree
				sortedPHs.remove(p.getAvailableMips());
			}
			
			selectedHost = sortedPHs.get(sortedPHs.ceilingKey(reqdMIPS));
			while (!selectedHost.isSuitableForVm(vm))
			{
				excludedHosts.add(selectedHost);
				selectedHost = adjuster1(vm, excludedHosts, sortedPHs);
			}
				//if (selectedHost.isSuitableForVm(vm))
					if (getUtilizationOfCpuMips(selectedHost) == 0) //&& isHostOverUtilizedAfterAllocation(selectedHost, vm)) 
						return  selectedHost;			//check last if statement
		
					else return null;
		}	
		
		//PowerHost getHost1 (Vm vm, Set<? extends Host> excludedHosts)
		//{
		//	this.reqdMIPS = vm.getMips();
		//	TreeMap<Double, PowerHost> sortedPHs = createHostListAvailableMIPS(); 
		//	List<? extends Host> exHosts = new LinkedList<Host>();
						
		//	for (Host p : excludedHosts)
		//	{
				//removes all excludedHosts from the tree
		//		sortedPHs.remove(p.getAvailableMips());				
		//	}
			
		//	Host selectedHost = sortedPHs.get(sortedPHs.ceilingKey(reqdMIPS));
		//	while (!selectedHost.isSuitableForVm(vm))
		//	{				
				//excludedHosts.add(selectedHost);				
		//		selectedHost = getHost(vm, excludedHosts);
		//	}
				//if (selectedHost.isSuitableForVm(vm))
		//			if (getUtilizationOfCpuMips((PowerHost)selectedHost) == 0) //&& isHostOverUtilizedAfterAllocation(selectedHost, vm)) 
		//				return  (PowerHost)selectedHost;			//check last if statement
		
		//			else return null;		
		//}
		
		PowerHost adjuster1 (Vm vm, List<PowerHost> excludedHosts, TreeMap<Double, PowerHost> tree)
		{
			this.reqdMIPS = vm.getMips();
			TreeMap<Double, PowerHost> sortedPHs = tree; 
			
			for (PowerHost p : excludedHosts)
			{
				//removes all excludedHosts from the tree
				sortedPHs.remove(p.getAvailableMips());
			}
			
			selectedHost = sortedPHs.get(sortedPHs.ceilingKey(reqdMIPS));
			return selectedHost;	
		}
		
		*/
		
	}