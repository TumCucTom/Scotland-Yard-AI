package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.Arrays;

public class RunDSP {
    public static int[] runDSPonGraph(
            ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
            int startNode){

        DijkstraShortestPath dsp = new DijkstraShortestPath(graph.nodes().size());
        return dsp.getShortestPath(graph,startNode);
    }

    public static int getTotalDistanceOfPlayerFromPoint(
            int point,
            int[] playersLocations,
            ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
        // doesn't matter that we give an extra +1 for the fact the player will always be one move away from themselves
        // as this is consistent
        return Arrays.stream(
                getDistances(runDSPonGraph(graph,point),playersLocations))
                .sum();
    }

    private static int[] getDistances(int[] shortestLengths, int[] playerLocations) {
        int numDetectives = playerLocations.length;
        int[] distances = new int[numDetectives];
        // for every detective
        for(int i=0;i< numDetectives;i++){
            // add the shortest distance from mrX's location to the node where the detective is
            // to the distances array
            distances[i] = shortestLengths[playerLocations[i]];
        }
        return distances;
    }
}
