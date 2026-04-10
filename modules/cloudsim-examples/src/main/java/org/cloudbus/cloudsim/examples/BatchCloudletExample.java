package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.EX.DatacenterBrokerEX;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Example that submits cloudlets to VMs in time-separated batches
 * instead of submitting all cloudlets at once.
 */
public class BatchCloudletExample {

	/** The VM list. */
	private static List<Vm> vmlist;

	public static void main(String[] args) {
		Log.println("Starting BatchCloudletExample...");

		try {
			// 1) Initialize CloudSim
			int numUsers = 1;
			Calendar calendar = Calendar.getInstance();
			boolean traceFlag = false;
			CloudSim.init(numUsers, calendar, traceFlag);

			// 2) Create Datacenter
			Datacenter datacenter0 = createDatacenter("Datacenter_0");

			// 3) Create a broker that supports delayed submissions
			DatacenterBrokerEX broker = new DatacenterBrokerEX("Broker", -1);
			int brokerId = broker.getId();

			// 4) Create VMs
			vmlist = new ArrayList<>();

			int mips = 1000;
			long size = 10000; // image size (MB)
			int ram = 512; // vm memory (MB)
			long bw = 1000;
			int pesNumber = 1; // number of cpus
			String vmm = "Xen"; // VMM name

			// Create two VMs to better show distribution
			for (int vmid = 0; vmid < 2; vmid++) {
				Vm vm = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm,
						new CloudletSchedulerTimeShared());
				vmlist.add(vm);
			}

			// Submit VM list to broker
			broker.submitGuestList(vmlist);

			// 5) Create cloudlets and submit them in batches
			int totalCloudlets = 20;
			int batchSize = 5;
			double firstBatchDelay = 10.0;     // time units for first batch (after VMs are created)
			double interBatchDelay = 50.0;     // time between consecutive batches

			long length = 400000;
			long fileSize = 300;
			long outputSize = 300;
			UtilizationModel utilizationModel = new UtilizationModelFull();

			List<Cloudlet> allCloudlets = new ArrayList<>();
			for (int id = 0; id < totalCloudlets; id++) {
				Cloudlet cloudlet = new Cloudlet(id, length, pesNumber, fileSize,
						outputSize, utilizationModel, utilizationModel, utilizationModel);
				cloudlet.setUserId(brokerId);
				allCloudlets.add(cloudlet);
			}

			int batchCount = (int) Math.ceil((double) totalCloudlets / batchSize);
			for (int batchIndex = 0; batchIndex < batchCount; batchIndex++) {
				int from = batchIndex * batchSize;
				int to = Math.min(from + batchSize, totalCloudlets);
				List<Cloudlet> batch = new ArrayList<>(allCloudlets.subList(from, to));
				double delay = firstBatchDelay + batchIndex * interBatchDelay;

				Log.printlnConcat("Scheduling batch ", batchIndex, " with ", batch.size(),
						" cloudlets at time ", delay);
				broker.submitCloudletList(batch, delay);
			}

			// 6) Start simulation
			CloudSim.startSimulation();
			CloudSim.stopSimulation();

			// 7) Print results
			List<Cloudlet> newList = broker.getCloudletReceivedList();
			printCloudletList(newList);

			Log.println("BatchCloudletExample finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.println("Unwanted errors happen");
		}
	}

	/**
	 * Creates a simple datacenter with a single host.
	 */
	private static Datacenter createDatacenter(String name) {
		List<Host> hostList = new ArrayList<>();
		List<Pe> peList = new ArrayList<>();

		int mips = 1000;
		peList.add(new Pe(0, new PeProvisionerSimple(mips)));

		int hostId = 0;
		int ram = 2048; // host memory (MB)
		long storage = 1000000; // host storage
		int bw = 10000;

		hostList.add(new Host(
			hostId,
			new RamProvisionerSimple(ram),
			new BwProvisionerSimple(bw),
			storage,
			peList,
			new VmSchedulerTimeShared(peList)
		));

		String arch = "x86";
		String os = "Linux";
		String vmm = "Xen";
		double time_zone = 10.0;
		double cost = 3.0;
		double costPerMem = 0.05;
		double costPerStorage = 0.001;
		double costPerBw = 0.0;
		LinkedList<Storage> storageList = new LinkedList<>();

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics,
				new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	/**
	 * Prints the Cloudlet execution details.
	 */
	private static void printCloudletList(List<Cloudlet> list) {
		Cloudlet cloudlet;
		String indent = "    ";
		Log.println();
		Log.println("========== OUTPUT ==========");
		Log.println("Cloudlet ID" + indent + "STATUS" + indent
				+ "Data center ID" + indent + "VM ID" + indent + "Time" + indent
				+ "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (Cloudlet value : list) {
			cloudlet = value;
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
				Log.print("SUCCESS");

				Log.println(indent + indent + cloudlet.getResourceId()
						+ indent + indent + indent + cloudlet.getGuestId()
						+ indent + indent
						+ dft.format(cloudlet.getActualCPUTime()) + indent
						+ indent + dft.format(cloudlet.getExecStartTime())
						+ indent + indent
						+ dft.format(cloudlet.getExecFinishTime()));
			}
		}
	}
}
