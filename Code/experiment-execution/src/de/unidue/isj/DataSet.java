package de.unidue.isj;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;


public class DataSet {
	

	public double offset = 0.0;
	
	public Vector<Case> cases = new Vector<Case>();
	public int processLength;
	
	public double minReliability = 1.0;
	public double maxReliability = 0.0;
	
	public DataSet(double _offset){
		offset = _offset;
	}
	
	// Read from the individual base model files and aggregate
	public void initRNNIndividual(String directory, Integer _processLength) throws Exception {
		processLength = _processLength;

		File[] files = new File(directory).listFiles();
		Reader[] myReader = new Reader[105];
		CSVReader[] myCSV = new CSVReader[105];
		
		int i = 0;
		int size = 0;
		
		// open all files
		for (File file : files) {
	        if (file.isDirectory()) {
	        	i++;
               	
	        	myReader[i] = new BufferedReader(new FileReader(new File(file, "0-results.edited.csv")));
	        	
				CSVParserBuilder bld = new CSVParserBuilder();
				CSVParser prs = bld.withSeparator(',').build();
				CSVReaderBuilder bld2 = new CSVReaderBuilder(myReader[i]);
				bld2.withCSVParser(prs); 
				myCSV[i] = bld2.build();  	        	
	        }
	    }
		size = i;
		
		// iterators (only way to do in Java): 
		// https://stackoverflow.com/questions/14917375/cannot-create-generic-array-of-how-to-create-an-array-of-mapstring-obje/14917529)
		Iterator<String[]>[] entries = (Iterator<String[]>[]) new Iterator[105];
				
		// skip the header
		for(i = 1; i <= size; i++) {
			 entries[i] = myCSV[i].iterator();
			 entries[i].next(); // skip the header
		}
		
		String[] entry;
		double pred = 0;
		double pred1 = 0;
		double act = 0;
		double plan = 0;
		boolean end = false;
		int pId = 0;
		int checkp = 0;
		
		// cases
		int currentCaseId = -1;
		int runningCaseId = -1;
		Case currentCase = null;
		
		// process steps per case
		int currentProcessStep = 0;
		
		int nbrPos = 0;
		int nbrNeg = 0;
		
		double posRel = 0;
		double negRel = 0;
				
		while(entries[1].hasNext())
		{
			pred = 0;
			
			nbrPos = 0;
			nbrNeg = 0;

			// loop over all models
			for(i = 1; i <= size; i++) {
					entry = entries[i].next();
					
					// ignore the rest of the file
					if(entry[0].equals("bucket_level")) {
						end = true;
						break;
					}

					// same for all models, so just do once
					if(i == 1) {
						plan = Double.parseDouble(entry[6]) + offset;
						act = Double.parseDouble(entry[5]) + offset;
						pId = Integer.parseInt(entry[0]);
						checkp = Integer.parseInt(entry[2]);
					}

					pred1 = Double.parseDouble(entry[4]) + offset;
					pred = pred + pred1;
					
					if(pred1 > plan) {
						nbrPos++;
					} else {
						nbrNeg++;
					}
			}
			
			if(!end) {				

				// Compute mean of predictions to generate ensemble prediction
				pred = pred / (double)size;
				
				// assign results to Case
				runningCaseId = pId; 
				
				if(runningCaseId != currentCaseId) {
					currentCaseId = runningCaseId;	
												
					currentCase = new Case();
					cases.add(currentCase);
					currentCase.caseId = currentCaseId;
								
					// Initialize (to identify the checkpoints that have no data -- different process lengths!)
					for(int j = 1; j <= _processLength; j++) {
						currentCase.reliability[j] = -1;
						currentCase.predictedDuration[j] = -1;
					}
												
					currentCase.actualDuration = act;
					currentCase.plannedDuration = plan;
				}
	
				currentProcessStep = checkp;
					
				
				// stop at max. process length (the others have basically no data)
				if(currentProcessStep > processLength)
					continue;
				
				// remember the largest checkpoint (= caseLength)
				currentCase.caseLength = currentProcessStep;
				
				// MajCount / Binary Prediction
				posRel = (double)nbrPos/(double)size;
				negRel = (double)nbrNeg/(double)size;
				
				if(posRel > negRel)
					currentCase.binaryPrediction[currentProcessStep] = true;
				else
					currentCase.binaryPrediction[currentProcessStep] = false;

				// MajCount
				if(posRel > negRel)
					currentCase.reliability[currentProcessStep] = posRel;
				else
					currentCase.reliability[currentProcessStep] = negRel;
				
				if(currentCase.reliability[currentProcessStep] < minReliability)
					minReliability = currentCase.reliability[currentProcessStep];
				
				if(currentCase.reliability[currentProcessStep] > maxReliability)
					maxReliability = currentCase.reliability[currentProcessStep];
				
				currentCase.predictedDuration[currentProcessStep] = pred;
				
			} else {
				break;
			}
			
		}

		for(i = 1; i <= size; i++) {
			myCSV[i].close();
		}
		
	}
	
	
	// Read from the individual base model files and aggregate
	public void initRFIndividual(CSVReader myCSV, Integer _processLength) throws Exception {
		processLength = _processLength;
		
		// RF models have 100 trees and thus predictions
		int size = 100;

		Iterator<String[]> entries = myCSV.iterator();
		entries.next(); // skip the header
		
		String[] entry;
		double pred = 0;
		double pred1 = 0;
		double act = 0;
		double plan = 0;
		
		int pId = 0;
		int checkp = 0;
		
		// cases
		int currentCaseId = -1;
		int runningCaseId = -1;
		Case currentCase = null;
		
		// process steps per case
		int currentProcessStep = 0;
		
		int nbrPos = 0;
		int nbrNeg = 0;
		
		double posRel = 0;
		double negRel = 0;
				
		Vector<Double> predV = null; // use for BAGV
		
		while(entries.hasNext())
		{			
			entry = entries.next();
			plan = Double.parseDouble(entry[102].replace(',','.')) + offset;
			act = Double.parseDouble(entry[101].replace(',','.')) + offset;
			pId = Integer.parseInt(entry[103]);
			checkp = Integer.parseInt(entry[104]);
					
			pred = 0;
			
			nbrPos = 0;
			nbrNeg = 0;

			predV = new Vector<Double>();
			
			// loop over all predictions
			for(int i = 0; i < size; i++) {
				pred1 = Double.parseDouble(entry[i].replace(',','.')) + offset;
				predV.add(pred1);
				pred = pred + pred1;
				
				if(pred1 > plan) {
					nbrPos++;
//					posPred += (pred1-plan);
////					posPred += (pred1/plan-plan/plan);
				} else {
					nbrNeg++;
//					negPred += (plan-pred1);
////					posPred += (pred1/plan-plan/plan);
				}
			}
			
			// Compute mean of predictions to generate ensemble prediction
			pred = pred / (double)size;
			
			// BAGV: loop again to compute
			double bagv = 0;
			double predX = 0;
			Iterator<Double> predI = predV.iterator();

			while(predI.hasNext()) {
				predX = predI.next();
				bagv += Math.pow( (predX - pred), 2);

			}

			double bagv1 = (bagv / (double)size);
			double bagv2 = Math.sqrt(bagv1);

			// normalize:
			bagv = bagv2 / Math.abs(pred);
			
			// assign results to Case
			runningCaseId = pId; 
			
			if(runningCaseId != currentCaseId) {
				currentCaseId = runningCaseId;	
											
				currentCase = new Case();
				cases.add(currentCase);
				currentCase.caseId = currentCaseId;
							
				// Initialize (to identify the checkpoints that have no data -- different process lengths!)
				for(int j = 1; j <= _processLength; j++) {
					currentCase.reliability[j] = -1;
					currentCase.predictedDuration[j] = -1;
				}
											
				currentCase.actualDuration = act;
				currentCase.plannedDuration = plan;
			}

			currentProcessStep = checkp;
					
			// stop at max. process length (the others have basically no data)
			if(currentProcessStep > processLength)
				continue;
			
			// remember the largest checkpoint (= caseLength)
			currentCase.caseLength = currentProcessStep;
			
			// MajCount / Binary Prediction
			posRel = (double)nbrPos/(double)size;
			negRel = (double)nbrNeg/(double)size;
			
			if(posRel > negRel)
				currentCase.binaryPrediction[currentProcessStep] = true;
			else
				currentCase.binaryPrediction[currentProcessStep] = false;

			// MajCount
			if(posRel > negRel)
				currentCase.reliability[currentProcessStep] = posRel;
			else
				currentCase.reliability[currentProcessStep] = negRel;
			
			if(currentCase.reliability[currentProcessStep] < minReliability)
				minReliability = currentCase.reliability[currentProcessStep];
			
			if(currentCase.reliability[currentProcessStep] > maxReliability)
				maxReliability = currentCase.reliability[currentProcessStep];
				
			currentCase.predictedDuration[currentProcessStep] = pred;		
//			System.out.println(" "+currentCase.binaryPrediction[currentProcessStep]);
		}

	}
	
	
	
}

