/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solver;

import dataStore.DataStorer;
import dataStore.Edge;
import dataStore.HeuristicEdge;
import dataStore.Sink;
import dataStore.Source;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import static utilities.Utilities.convertIntegerArray;

/**
 *
 * @author t92t161
 */
public class Heuristic {

    private DataStorer data;

    private Source[] sources;
    private Sink[] sinks;
    
    // Graph
    private int[] graphVertices;
    HeuristicEdge[][] adjacencyMatrix;
    HashMap<Integer, Integer> cellNumToVertexNum;

    public Heuristic(DataStorer data) {
        this.data = data;

        sources = data.getSources();
        sinks = data.getSinks();
        graphVertices = data.getGraphVertices();
    }

    public void heuristic() {
        // Initialize sources and sinks
        for (Source src : sources) {
            src.setRemainingCapacity(src.getProductionRate());
        }
        for (Sink snk : sinks) {
            snk.setRemainingCapacity(snk.getCapacity());
        }
        
        // Make directed edge graph
        Set<Edge> originalEdges = data.getGraphEdgeCosts().keySet();
        adjacencyMatrix = new HeuristicEdge[graphVertices.length][graphVertices.length];

        for (int u = 0; u < graphVertices.length; u++) {
            cellNumToVertexNum.put(graphVertices[u], u);
            for (int v = 0; v < graphVertices.length; v++) {
                if (originalEdges.contains(new Edge(graphVertices[u], graphVertices[v]))) {
                    adjacencyMatrix[u][v] = new HeuristicEdge(graphVertices[u], graphVertices[v], data);
                    adjacencyMatrix[u][v].currentHostingAmount = 0;
                    adjacencyMatrix[u][v].currentSize = 0;
                }
            }
        }
        
        double amountCaptured = 0;  // Amount of CO2 currently captured/injected by algorithm
        
        // Make cost array
        Pair[][] pairCosts = makePairwiseCostArray();
        
        // Schedule cheapest
    }
    
    public Pair[][] makePairwiseCostArray() {
        Pair[][] pairCosts = new Pair[sources.length][sinks.length];
        for (int srcNum = 0; srcNum < sources.length; srcNum++) {
            for (int snkNum = 0; snkNum < sinks.length; snkNum++) {
                Source src = sources[srcNum];
                Sink snk = sinks[snkNum];
                
                double transferAmount = Math.min(src.getRemainingCapacity(), snk.getRemainingCapacity());
                double cost = 0;
                
                // Incurr opening cost if source not yet used
                if (src.getRemainingCapacity() == src.getProductionRate()) {
                    cost += src.getOpeningCost(data.getCrf());
                }
                
                // Incurr opening cost if sink not yet used
                if (snk.getRemainingCapacity() == snk.getCapacity()) {
                    cost += snk.getOpeningCost(data.getCrf());
                }
                
                cost += transferAmount * src.getCaptureCost();
                cost += transferAmount * snk.getInjectionCost();
                
                // Assign costs to graph
                setGraphCosts(src, snk, transferAmount);
                
                // Find shortest path between src and snk
                Object[] data = dijkstra(src, snk);
                HashSet<HeuristicEdge> path = (HashSet<HeuristicEdge>) data[0];
                double pathCost = (double) data[1];
                
                cost += pathCost;
                
                pairCosts[srcNum][snkNum] = new Pair(path, cost);
            }
        }
        return pairCosts;
    }
    
    public void setGraphCosts(Source src, Sink snk, double transferAmount) {
        //TODO
    }

    // Dijkstra to run on graph edges
    public Object[] dijkstra(Source src, Sink snk) {
        int srcVertexNum = cellNumToVertexNum.get(src.getCellNum());
        int snkVertexNum = cellNumToVertexNum.get(snk.getCellNum());

        int numNodes = graphVertices.length;
        PriorityQueue<Heuristic.Data> pQueue = new PriorityQueue<>(numNodes);
        double[] costs = new double[numNodes];
        int[] previous = new int[numNodes];
        Heuristic.Data[] map = new Heuristic.Data[numNodes];

        for (int vertex = 0; vertex < numNodes; vertex++) {
            costs[vertex] = Double.MAX_VALUE;
            previous[vertex] = -1;
            map[vertex] = new Heuristic.Data(vertex, costs[vertex]);
        }

        costs[srcVertexNum] = 0;
        map[srcVertexNum].distance = 0;
        pQueue.add(map[srcVertexNum]);

        while (!pQueue.isEmpty()) {
            Heuristic.Data u = pQueue.poll();
            for (int neighbor = 0; neighbor < graphVertices.length; neighbor++) {
                if (adjacencyMatrix[cellNumToVertexNum.get(u.cellNum)][neighbor] != null) {
                    double altDistance = costs[u.cellNum] + adjacencyMatrix[cellNumToVertexNum.get(u.cellNum)][neighbor].getCost();
                    if (altDistance < costs[neighbor]) {
                        costs[neighbor] = altDistance;
                        previous[neighbor] = cellNumToVertexNum.get(u.cellNum);

                        map[neighbor].distance = altDistance;
                        pQueue.add(map[neighbor]);
                    }
                }
            }

        }
        
        HashSet<HeuristicEdge> path = new HashSet<>();
        int node = snkVertexNum;
        while (node != srcVertexNum) {
            int previousNode = previous[node];
            path.add(adjacencyMatrix[previousNode][node]);
            node = previousNode;
        }
        path.add(adjacencyMatrix[srcVertexNum][node]);

        return new Object[]{path, costs[snkVertexNum]};
    }

    private class Data implements Comparable<Data> {

        public int cellNum;
        public double distance;

        public Data(int cellNum, double distance) {
            this.cellNum = cellNum;
            this.distance = distance;
        }

        @Override
        public int compareTo(Data other) {
            return Double.valueOf(distance).compareTo(other.distance);
        }

        @Override
        public int hashCode() {
            return cellNum;
        }

        public boolean equals(Data other) {
            return distance == other.distance;
        }
    }
    
    private class Pair {
        public HashSet<HeuristicEdge> path;
        public double cost;
        
        public Pair(HashSet<HeuristicEdge> path, double cost) {
            this.path = path;
            this.cost = cost;
        }
    }
}
