package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.LogEntry;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


public class PossibleLocationsVectorCalculator {

    private static int[][][] makeMovementMatricesMrX(Board board){
        // 0 is bus
        // 2 is taxi
        // 1 is train
        // 3 is all/unknown
        int[][][] movementMatrices = new int[4][199][199];
        int busCount;
        int trainCount;
        int taxiCount;
        int totalCount;

        // for every node
        // for all adjacent nodes
        // add that as a valid move for the respective matrices
        for(int source=1;source<=199;source++){
            //set it so that no adjacent nodes have been set
            trainCount=0;taxiCount=0;totalCount=0;busCount=0;
            for(int destination : board.getSetup().graph.adjacentNodes(source)) {
                // for every combination of nodes, get the set of transports that can be used
                // for every transport contained:
                //      if it is not a ferry then add the nodes to the adjacency list and increase the total variable
                //      (this if for secret moves)
                //      now add the nodes to its own adjacency list and increase respective variable
                ImmutableSet<Transport> transports = getTransports(board, destination, source);
                if(transports.contains(Transport.BUS)){
                    movementMatrices[0][source-1][busCount] = destination-1;
                    movementMatrices[3][source-1][totalCount] = destination-1;
                    busCount++;
                    totalCount++;}
                if(transports.contains(Transport.UNDERGROUND)) {
                    movementMatrices[1][source-1][trainCount] = destination-1;
                    movementMatrices[3][source-1][totalCount] = destination-1;
                    trainCount++;
                    totalCount++;}
                if(transports.contains(Transport.TAXI)) {
                    movementMatrices[2][source-1][taxiCount] = destination-1;
                    movementMatrices[3][source-1][totalCount] = destination-1;
                    taxiCount++;
                    totalCount++;}
                if(transports.contains(Transport.FERRY)) {
                    movementMatrices[3][source-1][totalCount] = destination-1;
                    totalCount++;
                }
            }
        }
        return movementMatrices;
    }

    private static ImmutableSet<Transport> getTransports(Board board, int destination, int source) {
        ImmutableSet<Transport> transports = board.getSetup().graph
                .edgeValueOrDefault(source, destination, ImmutableSet.of());
        assert transports != null;
        return transports;
    }

    private static ImmutableMap<ScotlandYard.Ticket, int[][]> getTicketToMovementMrX(Board board){
        int[][][] moveMatrices = makeMovementMatricesMrX(board);
        return new ImmutableMap.Builder<ScotlandYard.Ticket,int[][]>()
                .put(ScotlandYard.Ticket.BUS,moveMatrices[0])
                .put(ScotlandYard.Ticket.UNDERGROUND,moveMatrices[1])
                .put(ScotlandYard.Ticket.TAXI,moveMatrices[2])
                .put(ScotlandYard.Ticket.SECRET,moveMatrices[3])
                .build();
    }

    /*
    The idea here is that we can multiply the matrices of legal moves
    based on the type of transport used to quickly and easily find
    the possible locations that mrX can be at
     */
    public static Optional<int[]> calculateForX(Board board){
        ImmutableMap<ScotlandYard.Ticket, int[][]> ticketToMovementMatrixMrX =
                getTicketToMovementMrX(board);
        ImmutableList<LogEntry> mrXLog = board.getMrXTravelLog();
        List<LogEntry> lastUnknownMoves = new ArrayList<>();
        int lastKnownLocation = -1;
        int[] mrXPossibleLocationVector = new int[199];

        // add all the unknown location moves up to when mrX was last seen
        for(int i= mrXLog.size()-1;i>0;i--){
            // get the optional location from the log
            Optional<Integer> optionalLocation =  mrXLog.get(i).location();
            // if the location is revealed add the location to the last known
            // break the loop
            if(optionalLocation.isPresent()){
                lastKnownLocation = optionalLocation.get();
                // starting position from last Log move
                mrXPossibleLocationVector[lastKnownLocation-1] = 1; // rest default to 0
                break;
            }
            lastUnknownMoves.add(mrXLog.get(i));
        }

        // figure out where X could be
        for(LogEntry entry : lastUnknownMoves){
            int[][] nextAdjacencyMatrix = ticketToMovementMatrixMrX.get(entry.ticket());
            mrXPossibleLocationVector = SMVP.calculate(nextAdjacencyMatrix, mrXPossibleLocationVector);
        }
        // return the vector of where MrX could be (weighted) if we have seen where he is before
        // otherwise we want to use different logic (dealt with outside of this class) but the
        // empty signifies we should do this
        return lastKnownLocation == -1? Optional.empty() : Optional.of(mrXPossibleLocationVector);
    }

    // get the adjacency list for the entire graph excluding ferries
    public static int[][] getAdjacencyMatrix(Board board){
        int[][] adjacencyList = new int[199][199];
        int totalCount;

        // for every node
        // for all adjacent nodes
        // add that as a valid move for the respective matrices
        for(int source=1;source<=199;source++) {
            totalCount = 0;
            for (int destination : board.getSetup().graph.adjacentNodes(source)) {
                // we cannot just do ! .empty() as we need to exclude the ferry
                ImmutableSet<Transport> transports = board.getSetup().graph
                        .edgeValueOrDefault(source, destination, ImmutableSet.of());
                if(transports.contains(Transport.BUS)
                        || transports.contains(Transport.TAXI)
                        || transports.contains(Transport.UNDERGROUND))
                {
                    adjacencyList[source-1][totalCount] = destination-1;
                    totalCount++;
                }
            }
        }
        return adjacencyList;
    }

    public static int[] calculateForPlayer(int startNode, int numberOfMoves, Board board){
        // get the matrix of possible moves
        // we don't need to worry about tickets and if they player has enough here
        // as either:
        //          we are in the first two moves - so we cannot run out of any ticket
        //          or the function was called one move before a reveal so there will be no
        //          use of the matrix as an empty was returned
        int[][] graphAdjacencyMatrix = getAdjacencyMatrix(board);

        // setup initial vector for player's location
        // start node -1 as nodes are shifted down in this class
        int[] possibleLocations = new int[199];
        possibleLocations[startNode-1] = 1;

        //see where they could have gone using any transport (not secret/ferry)
        for(int j=0;j<numberOfMoves;j++){
            possibleLocations = SMVP.calculate(graphAdjacencyMatrix,possibleLocations);
        }

        // just the location vector will be returned if there were no extra moves to consider
        return possibleLocations;
    }
}
