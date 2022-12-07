package de.unidue.isj;

import java.util.Enumeration;
import java.util.Vector;

public class SimpleStats {
	
	Vector<Double> data = new Vector<Double>();
	int size;   

    public SimpleStats(Vector<Double> data) {
        this.data = data;
        size = data.size();
    }   

    double getMean() {
        double sum = 0.0;

        Enumeration<Double> e = data.elements();
        Double a = null;
        
        while(e.hasMoreElements()) {
        	a = e.nextElement();
        	sum += a;
        }
        	
        return sum/size;
    }

    double getVariance() {
        double mean = getMean();
        double temp = 0;
        
        Enumeration<Double> e = data.elements();
        Double a = null;
        
        while(e.hasMoreElements()) {
        	a = e.nextElement();
            temp += (a-mean)*(a-mean);
        }
        
        return temp/(size-1);
        }

    double getStdDev() {
        return Math.sqrt(getVariance());
    }

}
