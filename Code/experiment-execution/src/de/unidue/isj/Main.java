package de.unidue.isj;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Iterator;
import java.util.Locale;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class Main {

	
	public static void main(String[] args) {
		
		if(args.length < 3) {
			System.out.println("Usage: data-set prediction-model research-question");
			System.out.println("data-set: 1 = BPIC12, 2 =BPIC17, 3 = Traffic, 4 = Cargo");
			System.out.println("prediction-model: 1 = LSTM; 2 = RF");
			System.out.println("research-question: 1 = RQ1: savings (output on stdout); 2 = RQ2: non-stationarity (output in directory ./out)");

			return;
		}
		int argsProcess = Integer.parseInt(args[0]);
		int argsDataset = Integer.parseInt(args[1]);
		int argsRq = Integer.parseInt(args[2]);
		
		runEmpirical(argsProcess, argsDataset, argsRq); 
	}
	

	// ***********************************************************
	public static void runEmpirical(int argsProcess, int argsDataset, int argsRq) {

		// map to local variables
		int	dataset = argsDataset;	//  the used prediction model: 1 = RNN; 2 = RF
		int process = argsProcess;
		boolean RQ1 = true;
		if(argsRq == 2)
			RQ1 = false;
		
		// files
		Reader myReader;
		CSVReader myCSV;

		// parameters that differ between the different files and thus are configured accordingly below
		String inputFileName= ""; // name of data set file
		int checkpoints = 0; // max. length of cases
		int aggregate = 1; // how data sets are read and aggregated into ensemble predictions: 1 = use aggregated files, 2 = read individually and aggregate in code
		double offset = 0; // used to move the 0/1 predictions (in Traffic, BPIC) away from zero (as this may lead to skewed accuracy)	
		int subset = 0; // size of subset considered for empirical thresholding (typically 1/3 of data set)
		int predPoint = 1; // fixed prediction points for static approach

		double split = 1f/3f; // how much of the data is used for empirical thresholding
		int nbrRuns = 10; 	   // nbr of repetitions of random experiment for empirical thr.

		// internal representation of data set
		DataSet ds = null;
		
		
		// ************ LOAD DATA SETS 
		
		try{	
			{		
				Object[][] config = {
						{(dataset/1)*(process/1), "BPIC2012-RNN",  49, "./_rnn/models-bpic",    2, 1.0, (int)(4361*split), 23}, 
						{(dataset/2)*(process/1), "BPIC2012-RF" ,  49, "./_rf/bpic.csv",        1, 1.0, (int)(4361*split), 28}, 
						
						{(dataset/1)*(process/2), "BPIC2017-RNN",  72, "./_rnn/models-bpic17",  2, 1.0, (int)(10500*split), 2}, 
						{(dataset/2)*(process/2), "BPIC2017-RF" ,  72, "./_rf/bpic17.csv",      1, 1.0, (int)(10500*split), 1}, 

						{(dataset/1)*(process/3), "Traffic-RNN",    5, "./_rnn/models-traffic", 2, 1.0, (int)(50117*split), 2}, 
						{(dataset/2)*(process/3), "Traffic-RF" ,    5, "./_rf/traffic.csv",     1, 1.0, (int)(50117*split), 1}, 

						{(dataset/1)*(process/4), "Cargo2000-RNN", 21, "./_rnn/models-c2k",     2, 0.0, (int)(1313*split), 6}, 
						{(dataset/2)*(process/4), "Cargo2000-RF" , 21, "./_rf/c2k.csv",         1, 0.0, (int)(1313*split), 13},
						
				};
	
				for(int i = 0; i < config.length; i++) {
					if(((Integer)config[i][0]) == 1) { // will be 1 only for the selected process
						checkpoints = (Integer)config[i][2];
						inputFileName = (String)config[i][3];
						aggregate = (Integer)config[i][4]; 
						offset = (Double)config[i][5];  					
						subset = (Integer)config[i][6]; 					
						predPoint = (Integer)config[i][7]; 
					}
				}
				
				// single files with ensemble predictions (RF)
				if(aggregate == 1) {
					System.out.println(inputFileName);
	
					myReader = new BufferedReader(new FileReader(inputFileName));
	
					CSVParserBuilder bld = new CSVParserBuilder();
					CSVParser prs = bld.withSeparator(';').build();
					CSVReaderBuilder bld2 = new CSVReaderBuilder(myReader);
					bld2.withCSVParser(prs); 
					myCSV = bld2.build();  
							
					ds = new DataSet(offset);		
					
					ds.initRFIndividual(myCSV, checkpoints);
					myCSV.close();			
				}
				
				// compute ensemble predictions from individual base models (RNN)
				if(aggregate == 2) {
					System.out.println(inputFileName);
					
					ds = new DataSet(offset);
					ds.initRNNIndividual(inputFileName, checkpoints);
				}

			}
		
			// data sets for RL-based have different filenames
			String processRL = "";
			if(dataset == 2)
				processRL = "20";
			
			// map to filenames
			switch(process) {
			// Traffic
			case 3: processRL += "40";
					break;
			// BPIC12
			case 1: processRL += "10";
					break;
			// BPIC17
			case 2: processRL += "20";
					break;
			// Cargo
			case 4: processRL += "30";
					break;
			}
						
			// ************ COMPUTE MAE
//			computeMAE(ds, subset);
//			System.out.println("DONE");

			// ************ COMPUTE Rate of Violations
//			computeViol(ds);
//			System.out.println("DONE");
			
			// ************ COMPUTE COSTS
			double minAlpha = 0.0f;
			double maxAlpha = 1.0f;
			double lambda = 0, kappa = 0;  
			
			// single costs for the different approaches
			long costNeverAdapt = 0;
			long costFixedPoint = 0;
			long costAlwaysAdapt = 0;
			long costRLAvg = 0;
			long costEmpiricalEpsilon = 0;
			long costAllKnowing = 0;
			long costRLbest = 0;
			long costRLworst = 0;
			long costEmpirical = 0;
			
			// total costs for the different approaches, clustered by lambda*kappa
			int clusters = 10;
			long[] totalcostNeverAdapt = new long [clusters];
			long[] totalcostFixedPoint = new long [clusters];
			long[] totalcostAlwaysAdapt = new long [clusters];
			long[] totalcostRLAvg = new long [clusters];
			long[] totalcostEmpiricalEpsilon = new long [clusters];
			long[] totalcostAllKnowing = new long [clusters];
			long[] totalcostRLbest = new long [clusters];
			long[] totalcostRLworst = new long [clusters];
			long[] totalcostEmpirical = new long [clusters];
			int[] nbrComb = new int[clusters]; // to compute averages
			int[] actThr = new int[clusters]; // to compute rate of actual thresholds for empirical

			int actThreshold = 0;
			int c = 0;
			
			for(c = 0; c < clusters; c++) {
				totalcostNeverAdapt[c] = 0;
				totalcostFixedPoint[c] = 0;
				totalcostAlwaysAdapt[c] = 0;
				totalcostRLAvg[c] = 0;
				totalcostEmpiricalEpsilon[c] = 0;
				actThr[c] = 0;
				totalcostAllKnowing[c] = 0;
				totalcostRLbest[c] = 0;
				totalcostRLworst[c] = 0;
				totalcostEmpirical[c] = 0;
				nbrComb[c] = 0;
			}

			Result r = null;
			
			if(RQ1)
				System.out.println("alpha_min\tlambda\tkappa\t"+
				"NEVER\tFIXED("+predPoint+")\tALWAYS\tRL-AVG\tEMPIRICAL("+nbrRuns+
				")\tactThr[%]\tALL-KNOWING\tRL-BEST\tRL-WORST\tEMPIRICAL-BEST");
			else
				System.out.println("Computing curves...");
			
			// --- ALPHA_MIN LOOP
			for(minAlpha = 1.0f; minAlpha > -0.05f; minAlpha -= 0.25f) 
			{
				System.out.println(String.format(Locale.GERMANY, "%.2g",minAlpha));

				for(c = 0; c < clusters; c++) {
					totalcostNeverAdapt[c] = 0;
					totalcostFixedPoint[c] = 0;
					totalcostAlwaysAdapt[c] = 0;
					totalcostRLAvg[c] = 0;
					totalcostEmpiricalEpsilon[c] = 0;
					actThr[c] = 0;
					totalcostAllKnowing[c] = 0;
					totalcostRLbest[c] = 0;
					totalcostRLworst[c] = 0;
					totalcostEmpirical[c] = 0;
					nbrComb[c] = 0;
				}
				
				// --- LAMBDA LOOP
				for(lambda = 0.0f; lambda < 1.1f; lambda += 0.25f) 
				{

					// --- KAPPA LOOP
					for(kappa = 0.0f; kappa < 1.1f; kappa += 0.25f) 
					{

						if(RQ1) {
							// NEVER ADAPT
							costNeverAdapt = Thresholding.computeTotalCostsThreshold(ds, 1.10f, lambda, kappa, minAlpha, maxAlpha, subset, true, false);	
							
							// FIXED PREDICTION POINT
							costFixedPoint = FixedPredPoint.computeTotalCostsFixed(ds, 0.0f, lambda, kappa, minAlpha, maxAlpha, subset, true, predPoint);		
	
							// ALWAYS ADAPT, i.e., NO RELIABILITY (i.e., always adapt in case of positive prediction independent of reliability estimate)
							costAlwaysAdapt = Thresholding.computeTotalCostsThreshold(ds, 0.0f, lambda, kappa, minAlpha, maxAlpha, subset, true, false);	
		
							// RL-BASED 
							costRLAvg = AliceRL.runRLEvalAvg(processRL, lambda, kappa, minAlpha, maxAlpha, subset, dataset);
							costRLbest  = AliceRL.runRLEval(true, processRL, lambda, kappa, minAlpha, maxAlpha, subset, dataset);
							costRLworst = AliceRL.runRLEval(false, processRL, lambda, kappa, minAlpha, maxAlpha, subset, dataset);	
							
							// EMPIRICAL THRESHOLDING
							r = Thresholding.computeTotalCostsEmpiricalEpsilon(ds, 0.0f, lambda, kappa, minAlpha, maxAlpha, 
									subset, true, nbrRuns);
							costEmpiricalEpsilon = r.cost;
							actThreshold = r.actThr;
							
							// ALL KKNOWING
							costAllKnowing = Thresholding.computeTotalCostsAllKnowing(ds, lambda, kappa, minAlpha, maxAlpha, subset, true);
						
							// compute cluster
							c = (int)( 
									((1.0f+lambda) * (1.0f+kappa) *1f/3f - 1f/3f ) * clusters);
	
							if(c == clusters) c = clusters-1; // map the single 1.0x1.0 combination to the last cluster
							
							totalcostNeverAdapt[c] += costNeverAdapt;
							totalcostFixedPoint[c] += costFixedPoint;
							totalcostAlwaysAdapt[c] += costAlwaysAdapt;
							totalcostRLAvg[c] += costRLAvg;
							totalcostEmpiricalEpsilon[c] += costEmpiricalEpsilon;
							actThr[c] += actThreshold;
							totalcostAllKnowing[c] += costAllKnowing;
							totalcostRLbest[c] += costRLbest;
							totalcostRLworst[c] += costRLworst;
							totalcostEmpirical[c] += costEmpirical;							
							nbrComb[c]++;
							
							System.out.println(
									"\t"+String.format(Locale.GERMANY, "%.2g",lambda)+"\t"+
										 String.format(Locale.GERMANY, "%.2g",kappa)+"\t"+
									costNeverAdapt+"\t"+
								    costFixedPoint+"\t"+
								    costAlwaysAdapt+"\t"+
								    costRLAvg+"\t"+
								    costEmpiricalEpsilon+"\t"+
								    String.format(Locale.GERMANY, "%.3g",(double)(actThreshold))+"\t"+
								    costAllKnowing+"\t"+
								    costRLbest+"\t"+
								    costRLworst+"\t"+
								    costEmpirical);
						}
						else {
						// RQ2
						costEmpirical = Thresholding.computeTotalCostsEmpirical(ds, 0.0f, lambda, kappa, minAlpha, maxAlpha, subset, true,
								true);
							
						}
						
					}
							
				}


			}
		} catch(Exception e)
		{e.printStackTrace();};
		
	}

	// ACCURACY per CASE (MEAN ABSOLUTE ERROR; RelMAE doesn't work due to same actuals and avg per case)
	public static void computeMAE(DataSet ds, int subset) throws Exception {
	
		double lastN[] = new double[100];
		for(int i = 0; i < lastN.length; i++)
				lastN[i] = 0;
						
		Iterator<Case> cases = ds.cases.iterator();
		Case myCase = null;
		
		PrintWriter wr = new PrintWriter(new FileWriter("./out/mae.csv"));
		wr.println("n;mae");
		
		int k = 0;
		double avg = 0;
		
		while(cases.hasNext()){
			myCase = cases.next();

			lastN[k % lastN.length] = computeMAECase(myCase);
			avg = 0;
			for(int i = 0; i < lastN.length; i++) {
				avg += lastN[i];
			}
			wr.println(""+myCase.caseId+";"+String.format(Locale.GERMANY, "%.15g", avg/lastN.length));
						
			k++;
		}					
		
		wr.close();
		
	}
	
	public static double computeMAECase(Case myCase) {

		Double err1 = 0.0;
		Double p = 0.0;
		Double a = 0.0;
		Double error = 0.0;
		
		int nbr = 0;
		
		for(int i = 1; i <= myCase.caseLength; i++) {
			if(myCase.reliability[i] != -1) {
				// compute absolute errors
				p = myCase.predictedDuration[i];
				a = myCase.actualDuration;
				nbr++;
				
				err1 = err1 + Math.abs(p - a);
			}
		}
		error = err1 / nbr;

		return error;
	}
	
	// ACCURACY per CASE (MEAN ABSOLUTE ERROR; RelMAE doesn't work due to same actuals and avg per case)
	public static void computeViol(DataSet ds) throws Exception {
	
		Iterator<Case> cases = ds.cases.iterator();
		Case myCase = null;
		
		int viol = 0;
		
		while(cases.hasNext()){
			myCase = cases.next();

			if(myCase.actualDuration > myCase.plannedDuration)
				viol++;
		}					
		
		System.out.println(viol);
	}
	



	
}
