package learning;

import java.util.*;

public class LookupTable {

    // const
    private static final double cup_offset = -0.127;
    
    // args
    private static HashMap<Double,double[]> Map;
    private static double[] keys;
    
    // CONSTRUCTOR METHOD
    public LookupTable(ArrayList<Double> key_array, ArrayList<double[]> angles_array) {
        
        // init hashmap
        this.Map = new HashMap(key_array.size());
        this.keys = new double[key_array.size()];
        
        // build map
        for (int i = 0; i < angles_array.size(); i++) {
            double cur_key = key_array.get(i);
            double[] angles = angles_array.get(i);
            Map.put(cur_key, angles);
            keys[i] = cur_key;
        }
    }
    
    // RETRIEVE VALUE
    public double[] getAngles(double key) {
        
        // search for key index
        int corrected_key = (int)(key + cup_offset);
        int index = Arrays.binarySearch(keys, corrected_key);
        double hash_key = keys[index];
        double[] angles = Map.get(key);
        
        return angles;
    }
    
    



}