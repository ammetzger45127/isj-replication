package de.unidue.isj;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.util.Iterator;

import com.opencsv.CSVReader;

public class AliceRL {
	
	public static long computeTotalCostsRL(String fileName, double lambda, double kappa, double minAlpha,double maxAlpha, int subset) throws Exception {
		String[] entry = null;
		
		
		double penalty = 100f;
		double adaptCost = penalty*lambda; 
		double compCost = penalty*lambda*kappa; 
		
		int caseId = 0;
		
		boolean truePerEpisode = false; 
		int adaptPos = -1; 				
		int caseLen = -1;				
		
		long totalCosts = 0;
		long costs = 0;
		
		double alpha = 0;
		
		totalCosts = 0;
		caseId = 0;
				
		Reader myReader = new BufferedReader(new FileReader(fileName));
		CSVReader myCSV = new CSVReader(myReader);
					
		Iterator<String[]> entries = myCSV.iterator();	
		entry = entries.next();
		while(entries.hasNext())
		{		
			entry = entries.next();
			costs = 0;
			
			caseId++;
			if(caseId < subset) continue;
			
			
			truePerEpisode = Boolean.parseBoolean(entry[10]);
			adaptPos = Integer.parseInt(entry[11]);
			caseLen = (int)Double.parseDouble(entry[12]);

			alpha = maxAlpha-((maxAlpha-minAlpha)/(caseLen)*(adaptPos-1));
			
			// compute for the four contingencies
			// Adaptation
			if(adaptPos > 0) {
				// Required adaptation
				if(truePerEpisode == true) {
					costs = (long)( adaptCost + (1-alpha)*penalty ); // adaptation costs + residual chance for penalty
				} 
				// Unnecessary adaptation
				else {
					costs = (long)( adaptCost + alpha*compCost); // adaptation costs + compensation costs if adaptation successful
				}
			}
			// No Adaptation
			else {
				// No adaptation
				if(truePerEpisode == true) {
					costs = 0; // no penalties nor adaptation costs incurred
				} 
				// Missed adaptation
				else {
					costs = (long)penalty; // penalties incurred
				}
			}
			
			totalCosts += costs;
		}

		myCSV.close();

		return totalCosts;
	}
	


	
	public static long runRLEval(boolean best, String processRL, double lambda, double kappa,
			double minAlpha, double maxAlpha, int subset, int dataset) {
		
		String inputDirectory = "./_prescriptions";
		int nbrRuns = 10;
		
		String rewardFct = "/NonPropheticCuriosityAdaptationPunish_"; // curious 
				
		long cost = 0;
		try{
			nbrRuns = 10; 
			
			double maxMCC = 0;
			int maxIndex = -1;
			double minMCC = 1;
			int minIndex = -1;

			String fileName = inputDirectory+rewardFct+processRL+"0";

			// STEP 1: Read the total_accuracy_metrics.csv and select the ones with the best and worst MCC
			for(int i = 0; i < nbrRuns; i++) 
			{
				fileName = inputDirectory+rewardFct+processRL+""+i+"/total_accuracy_metrics.csv";
				
				Reader myReader = new BufferedReader(new FileReader(fileName));

				// Input is in US style CSV
				CSVReader myCSV = new CSVReader(myReader);
				Iterator<String[]> entries = myCSV.iterator();
				
				String[] entry = entries.next();
				entry = entries.next();
				double mcc = Double.parseDouble(entry[6]);
				
				if(mcc > maxMCC) {
					maxMCC = mcc;
					maxIndex = i;
				}
				if(mcc < minMCC) {
					minMCC = mcc;
					minIndex = i; 
				}
				myCSV.close();
			}
			
			// STEP 2: For the best or worst runs, compute the costs

			// BEST
			if(best) {
				fileName = inputDirectory+rewardFct+processRL+""+maxIndex+"/diagnostic_metrics.csv";
				cost = computeTotalCostsRL(fileName, lambda, kappa, minAlpha, maxAlpha, subset);
			} else {
			// WORST
				fileName = inputDirectory+rewardFct+processRL+""+minIndex+"/diagnostic_metrics.csv";
				cost = computeTotalCostsRL(fileName, lambda, kappa, minAlpha, maxAlpha, subset);
			}
			
			return cost;
			
			
		} catch(Exception e)
		{e.printStackTrace();};
		
		return cost;
	}
		
	
		public static long runRLEvalAvg(String processRL, double lambda, double kappa,
				double minAlpha, double maxAlpha, int subset, int dataset) {
			
			String inputDirectory = "./_prescriptions";
			int nbrRuns = 10;
			
			String rewardFct = "/NonPropheticCuriosityAdaptationPunish_"; // curious 
					
			long cost = 0;
			try{
				nbrRuns = 10; 
				
				String fileName = inputDirectory+rewardFct+processRL+"0";

				// compute costs for each of the runs and average
				for(int i = 0; i < nbrRuns; i++) 
				{
					fileName = inputDirectory+rewardFct+processRL+""+i+"/diagnostic_metrics.csv";
					cost += computeTotalCostsRL(fileName, lambda, kappa, minAlpha, maxAlpha, subset);
				}
				
				return cost/nbrRuns;
								
			} catch(Exception e)
			{e.printStackTrace();};
			
			return cost;
		}
	
		
}
