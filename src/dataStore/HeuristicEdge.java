package dataStore;

import java.util.HashMap;

/**
 *
 * @author yaw
 */
public class HeuristicEdge {

    public int v1;
    public int v2;

    public double currentHostingAmount;
    public double[] buildCost;
    public double[] transportCost;    //per ton of CO2
    public double[] capacities;       //capacity at each size
    public int currentSize;     //size 0 means no pipeline
    
    public double cost;

    public DataStorer data;

    public HeuristicEdge(int v1, int v2, DataStorer dataStorer) {
        this.v1 = v1;
        this.v2 = v2;
        this.data = dataStorer;

        LinearComponent[] linearComponents = dataStorer.getLinearComponents();
        int numPossibleSizes = linearComponents.length;

        // Populate capacities
        capacities = new double[numPossibleSizes + 1];   //Need 0 to represent no pipeline
        // Get max pipeline capacity.
        for (int c = 0; c < linearComponents.length; c++) {
            double maxCap = dataStorer.getTargetCaptureAmount();
            if (c < linearComponents.length - 1) {
                double alpha1 = linearComponents[c].getConAlpha() + linearComponents[c].getRowAlpha();
                double beta1 = linearComponents[c].getConBeta() + linearComponents[c].getRowBeta();
                double alpha2 = linearComponents[c + 1].getConAlpha() + linearComponents[c + 1].getRowAlpha();
                double beta2 = linearComponents[c + 1].getConBeta() + linearComponents[c + 1].getRowBeta();
                maxCap = (beta2 - beta1) / (alpha1 - alpha2);
            }
            capacities[c + 1] = maxCap;
        }

        //Construction and right-of-way costs
        HashMap<Edge, Double> edgeConstructionCosts = data.getGraphEdgeConstructionCosts();
        HashMap<Edge, Double> edgeRightOfWayCosts = data.getGraphEdgeRightOfWayCosts();
        
        // Populate buildCost
        buildCost = new double[numPossibleSizes + 1];
        for (int c = 0; c < linearComponents.length; c++) {
            double cost = (linearComponents[c].getConBeta() * edgeConstructionCosts.get(new Edge(v1, v2)) + linearComponents[c].getRowBeta() * edgeRightOfWayCosts.get(new Edge(v1, v2))) * data.getCrf();
            buildCost[c + 1] = cost;
        }

        // Populate transportCost
        transportCost = new double[numPossibleSizes + 1];
        for (int c = 0; c < linearComponents.length; c++) {
            double cost = (linearComponents[c].getConAlpha() * edgeConstructionCosts.get(new Edge(v1, v2)) + linearComponents[c].getRowAlpha() * edgeRightOfWayCosts.get(new Edge(v1, v2))) * data.getCrf() / .93;    //.93 = pipeline utilization
            transportCost[c + 1] = cost;
        }
    }
}
