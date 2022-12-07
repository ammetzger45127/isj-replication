package de.unidue.isj;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;

public class Thresholding {

	
	public static Result computeTotalCostsEmpiricalEpsilon(DataSet ds, double thr, double lambda, double kappa,
			double minAlpha,double maxAlpha, int subset, boolean gt, int nbrRuns) throws Exception {
		
		// thresholds for empirical
		double minThr = 0.5f;
		double empiricalThr = 0;
					
		int actThreshold = 0;
		
		long totalAvgCosts = 0;
		int totalNbrCosts = 0;
		
		long cost = 0;
		long minCost = Long.MAX_VALUE;
				
		// Randomization for cost parameters
		Random r1 = new Random();
		Random r2 = new Random();
		Random r3 = new Random();
		
		// --- EPSILON LOOP
		double epsilon = .1f; // measure for the amount of uncertainty concerning the cost model params
		// uncertainty in the rage +/- 0.25
		for(epsilon = 0.1f; epsilon < 1.10; epsilon += 0.30) {
		
			// Repetitions to account for randomization 
			for(int i = 0; i < nbrRuns; i++) {
				
				// Empirical Thresholding
				minCost = Long.MAX_VALUE;
				
				// Choose random cost parameters (i.e., reflecting some design time assumptions)
				// calculate "area" around the given (but unknown) parameters

				double lambdaET = lambda + (0.5f-r1.nextDouble())/2f*epsilon;
				if(lambdaET > 1.0) lambdaET = 1.0;
				if(lambdaET < 0.0) lambdaET = 0.0;
				
				double minAlphaET = minAlpha + (0.5f-r2.nextDouble())/2f*epsilon;
				if(minAlphaET  > 1.0) minAlphaET  = 1.0;
				if(minAlphaET  < 0.0) minAlphaET  = 0.0;
				
				double kappaET = kappa + (0.5f-r3.nextDouble())/2f*epsilon;
				if(kappaET  > 4.0) kappaET  = 4.0;
				if(kappaET  < 0.25) kappaET  = 0.25;
				
						
				for(thr = minThr; thr < 1.0f; thr += 0.01f) {
					cost = Thresholding.computeTotalCostsThreshold(ds, thr, lambdaET, kappaET, minAlphaET, maxAlpha, subset, false, false);		
	
					// Compute the optimal threshold for a subset (e.g., 1/3) of data
					if(cost < minCost) {
						minCost = cost;
						empiricalThr = thr;
					}
					
				}											
						
				// Compute the costs for the remaining (unseen) data with thresholding
				totalAvgCosts  += Thresholding.computeTotalCostsThreshold(ds, empiricalThr, lambda, kappa, minAlpha, maxAlpha, subset, true, false);	
				totalNbrCosts++; 
			
				if(empiricalThr >.5f)
					actThreshold++;
				
			}
			
		}

		return new Result(totalAvgCosts/totalNbrCosts, actThreshold);
	
	}
	
	public static long computeTotalCostsEmpirical(DataSet ds, double thr, double lambda, double kappa,
			double minAlpha,double maxAlpha, int subset, boolean gt, boolean curves) throws Exception {
	
		
		// thresholds for empirical
		double minThr = 0.5f;
		double empiricalThr = 0;
					
		long cost = 0;
		
		// Empirical Thresholding
		long minCost = Long.MAX_VALUE;
				
		for(thr = minThr; thr < 1.0f; thr += 0.01f) {
			cost = Thresholding.computeTotalCostsThreshold(ds, thr, lambda, kappa, minAlpha, maxAlpha, subset, false, curves);		

			// Compute the optimal threshold for a subset (e.g., 1/3) of data
			if(cost < minCost) {
				minCost = cost;
				empiricalThr = thr;
			}
			
		}											
						
		// Compute the costs for the remaining (unseen) data with thresholding
		return Thresholding.computeTotalCostsThreshold(ds, empiricalThr, lambda, kappa, minAlpha, maxAlpha, subset, true, curves);	

	}
	
	
	
	public static long computeTotalCostsThreshold(DataSet ds, double thr, double lambda, double kappa,
			double minAlpha,double maxAlpha, int subset, boolean gt, boolean curves) throws Exception {
		
		// PARAMS
		double penalty = 100f;
		double adaptCost = penalty*lambda; 
		double compCost = penalty*kappa; 

		long cost = 0;
		int pId = 0;
		
		boolean pred = false;
		
		Case myCase = null;
		Iterator<Case> cases = ds.cases.iterator();
		double alpha = 0;
		
		boolean adapted = false;
		
		Writer out = null;
		if(gt == true && curves == true) {
			out = new BufferedWriter(new FileWriter("./out/curves("+lambda+", "+kappa+", "+minAlpha+").csv"));
			out.write("caseID;earliness;avg_alarms;avg_true_alarms\n");
		}
		int k = 0;
		int kk = 0;
		double earliness[] = new double[100];
		double alarms[] = new double[100];
		double trueAlarms[] = new double[100];
		
		if(curves == true) {
			for(k = 0; k < 100; k++) {
				earliness[k] = 0;
				alarms[k] = 0;
				trueAlarms[k] = 0;
			}
		}
		
		while(cases.hasNext()){
			myCase = cases.next();
			pId++;
			
			if(gt == true) {
				if(pId < subset) continue;
			} else {
				if(pId >= subset) break;
			}

			adapted = false;
			kk++;

			for(int i = 1; i <= myCase.caseLength; i++) {						
				// Compute alpha 
				 alpha = maxAlpha-((float)(maxAlpha-minAlpha)/(double)(myCase.caseLength) * (i-1));
				 
				pred = myCase.binaryPrediction[i];
				
				if(!adapted) {
					// ADAPT
					boolean above = false;
					above = myCase.reliability[i] > thr;

					if( pred == true  && above) 
					{	
						// if true positive: adaptation costs + residual chance for penalty
						if(myCase.actualDuration > myCase.plannedDuration) {
							trueAlarms[kk % 100] = 1;
							cost += (long)( adaptCost + (1f-alpha)*penalty );
						}

						// if false positive: also consider compensation costs 
						if(myCase.actualDuration <= myCase.plannedDuration) {
							cost += (long)( adaptCost + alpha*compCost ); 
							trueAlarms[kk % 100] = 0;
						}						
						
						adapted = true;
						alarms[kk % 100] = 1;
						earliness[kk % 100] = 1-((double)i / (double)myCase.caseLength);
					
					}
				}


			}
			
			// compute the costs at the final checkpoint when no adaptation has been made thus far
			if(adapted == false) {
				if(myCase.actualDuration > myCase.plannedDuration) {
					cost += penalty;
					trueAlarms[kk % 100] = 0;
				} else {
					trueAlarms[kk % 100] = 1;
				}
				
				alarms[kk % 100] = 0;
				earliness[kk % 100] = 1;
			}
				
			if(gt == true && curves == true) {
				double earlinessRate = 0;
				double alarmRate = 0;
				double trueAlarmRate = 0;
				
				for(k = 0; k < 100; k++) {
					earlinessRate += earliness[k];
					alarmRate += alarms[k];
					trueAlarmRate += trueAlarms[k];
				}

				earlinessRate = earlinessRate / 100f;
				alarmRate = alarmRate / 100f;
				trueAlarmRate = trueAlarmRate / 100f;

				if(kk > 100) 
				{
					out.write((kk+subset)+";"+String.format(Locale.GERMANY, "%.5g", earlinessRate)+";"+String.format(Locale.GERMANY, "%.5g", alarmRate)+";"
							+String.format(Locale.GERMANY, "%.5g", trueAlarmRate)+"\n");
				}
				
			}
		}
		if(gt == true && curves == true) {
			out.close();
		}
		
		return cost;
	}
	

	public static long computeTotalCostsAllKnowing(DataSet ds, double lambda, double kappa,
			double minAlpha,double maxAlpha, int subset, boolean gt) throws Exception {
		
		// PARAMS
		double penalty = 100f;
		double adaptCost = penalty*lambda; 
		
		long cost = 0;
		
		int pId = 0;
		
		Case myCase = null;
		Iterator<Case> cases = ds.cases.iterator();
		double alpha = 0;
		
		while(cases.hasNext()){
			myCase = cases.next();
			pId++;
			if(gt == true) {
				if(pId < subset) continue;
			} else {
				if(pId >= subset) break;
			}
			
			int i = 1;						
			// Compute alpha 
			 alpha = maxAlpha-((float)(maxAlpha-minAlpha)/(double)(myCase.caseLength) * (i-1));
				 
			// ADAPT if ACTUAL VIOLATION, and use first prediction point
			if( myCase.actualDuration > myCase.plannedDuration ) 
					cost += (long)( adaptCost + (1f-alpha)*penalty);
		}
		
		return cost;
	}
	
}
