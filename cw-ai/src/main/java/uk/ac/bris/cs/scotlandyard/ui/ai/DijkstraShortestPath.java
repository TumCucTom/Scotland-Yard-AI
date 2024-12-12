package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.*;

public class DijkstraShortestPath{
    // setting up all data structures appropriately
    private final Set<Integer> visited;
    private final int size;
    private final int[] dist;
    Queue<Integer> queue;

    // give an array full of max values
    private int[] setupDistances(int size){
        int[] array = new int[size];
        Arrays.fill(array, Integer.MAX_VALUE);
        return array;
    }

    public DijkstraShortestPath(int size){
        // setting up all data structures appropriately
        visited = new HashSet<>();
        this.size = size;
        // need a +1 so that index 199 can be referenced
        // (this means index 0 will be left untouched)
        dist = setupDistances(size+1);
        queue = new ArrayDeque<>(size);
    }

    private void processNeighbours(
            int source,
            ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
        for(Integer neighbour : graph.adjacentNodes(source)){
            // if the neighbor hasn't already been processed
            if(visited.contains(neighbour)) continue;
            // the cost of travel is always one ticket
            // this could be amended later by adding heuristics for the value of using
            // different ticket types if this proves beneficial to the AI's performance
            int newDistance = dist[source] + 1;
            // if the distance through the node passed is lower update as such
            if(newDistance < dist[neighbour]) dist[neighbour] = newDistance;
            // add neighbour to queue to be processed
            queue.add(neighbour);
        }
    }

    public int[] getShortestPath(
            ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
            int startNode){
        // add the start node and consider it visited
        dist[startNode] = 0;
        queue.add(startNode);
        while(visited.size()!= size){
            // this occurs when everything has been visited
            if(queue.isEmpty()) return dist;

            // get the next node to be calculated
            int current = queue.remove();

            // consider the node visited and process the neighbors
            // if this node has not already been visited
            if (visited.contains(current)) continue;
            visited.add(current);
            processNeighbours(current,graph);
        }
        return dist;
    }
}
