package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

public class NodeType {
    private static int nodeHasTransport(
            int node,
            ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
        // cannot return buses straight away in the case we find a train,
        // so we can store if a bus was found in this variable
        int returnVal = 0;

        // check all adjacent nodes for travel
        for(int adjacentNode : graph.adjacentNodes(node)){
            ImmutableSet<ScotlandYard.Transport> transportMode =
                    graph.edgeValueOrDefault(node,adjacentNode,ImmutableSet.of());
            assert transportMode != null;
            if(transportMode.contains(ScotlandYard.Transport.UNDERGROUND)) return 2;
            if(transportMode.contains(ScotlandYard.Transport.BUS))returnVal =1;
        }
        return returnVal;
    }

    // the public function to get the evaluation of the node
    public static int nodeEval(int node,
                               ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
        return nodeHasTransport(node,graph) + graph.degree(node);
    }

    public static int nodeEval(int weightingTransport,
                               int node,
                               ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
        return weightingTransport * nodeHasTransport(node,graph) + graph.degree(node);
    }
}
