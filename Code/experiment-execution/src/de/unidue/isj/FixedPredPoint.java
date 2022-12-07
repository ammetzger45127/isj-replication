package de.unidue.isj;

import java.util.Iterator;

public class FixedPredPoint {
	
	public static long computeTotalCostsFixed(DataSet ds, double thr, double lambda, double kappa,
			double minAlpha,double maxAlpha, int subset, boolean gt, int predPoint) throws Exception {
		
		// PARAMS
		double penalty = 100f;
		double adaptCost = penalty*lambda; 
		double compCost = penalty*kappa; 
		
		long cost = 0;
		
		int pId = 0;
		
		boolean pred = false;
		boolean adapted = false;
		
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
			
			for(int i = 1; i <= myCase.caseLength; i++) {		
				adapted = false;
				
				// Compute alpha 
				alpha = maxAlpha-((float)(maxAlpha-minAlpha)/(double)(myCase.caseLength) * (i-1));
				 
				pred = myCase.binaryPrediction[i];
				
				if(i == predPoint) {
					
					if( pred == true ) 
					{	
						adapted = true;
						
						// if true positive: adaptation costs + residual chance for penalty
						if(myCase.actualDuration > myCase.plannedDuration)
							cost += (long)( adaptCost + (1f-alpha)*penalty );

						// if false positive: also consider compensation costs 
						if(myCase.actualDuration <= myCase.plannedDuration)
							cost += (long)( adaptCost + alpha*compCost ); 
											
					break; // end loop
					}
				}


			}
			
			if(adapted == false) {
				if(myCase.actualDuration > myCase.plannedDuration) {
					cost += penalty;
				}
			}
			
		}
		
		return cost;
		
	}
	
}
