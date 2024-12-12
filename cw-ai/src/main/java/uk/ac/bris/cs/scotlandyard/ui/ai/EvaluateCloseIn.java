package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

public class EvaluateCloseIn implements EvaluatePlayers {

    private final Board board;
    private final int[] mrXPossibleLocations;
    private final int[] playerStartingLocations;
    private final ImmutableMap<Piece, Board.TicketBoard> pieceTicketBoardMap;

    public EvaluateCloseIn(Board board,
                           int[] mrXPossibleLocations,
                           int[] playerStartingLocations,
                           ImmutableMap<Piece, Board.TicketBoard> pieceTicketBoardMap){
        this.board = board;
        this.mrXPossibleLocations = mrXPossibleLocations;
        this.playerStartingLocations = playerStartingLocations;
        this.pieceTicketBoardMap = pieceTicketBoardMap;
    }

    @Override
    public double evaluateStateWithMove(Move.SingleMove move){
        // we know that the number on each value of the mrX vector is how many way he could have arrived at that node
        // also note that all the indices are one lower than the node the represent

        double value;

        // we need to stop the AI deciding that it's optimal to stay in the middle of the tree of where mrX could be,
        // a way to counteract this is to give more reward for getting closer to any possible x node
        // this way a state is evaluated such that being close to some possible nodes and far from others is better than
        // if you are equally far from all the nodes, even if the total distance from all nodes is the same
        // we can do this by non-linearly giving value based on how close the player is to a node (making further away nodes
        // not so bad)

        // therefore, the evaluation will be given by the sum of
        //      - log[the distance to a node where mrX could be]
        //      * the number of ways he can get there
        ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph = board.getSetup().graph;
        int[] shortestPaths = RunDSP.runDSPonGraph(graph,move.source());
        value =  3*dotProductWithNonLinearWeighting(shortestPaths,mrXPossibleLocations);

        // consider if you will get stuck due to tickets or using low tickets
        value += EvaluateTickets.evalForBadTicketUsePlayer(board,move, pieceTicketBoardMap.get(move.commencedBy()));

        // we should also add a small amount for being close to other players are this is generally a good idea
        // if we want to try and corner mrX
        int totalDistance = RunDSP.getTotalDistanceOfPlayerFromPoint(move.destination,playerStartingLocations,graph);
        value -= (double)totalDistance/2;

        // in general to give a players a direction if wavering or far away
        // then they should head closer to the last seen X location
        value += 2*shortestPaths[lastSeenXLocation(board)-1];

        // we want the player to go to a good node
        value += 0.1*NodeType.nodeEval(move.destination,graph);

        return value;
    }

    private int lastSeenXLocation(Board board) {
        ImmutableList<Boolean> moves = board.getSetup().moves;
        for(int i= board.getMrXTravelLog().size()-1;i>0;i--){
            if(moves.get(i)) return board.getMrXTravelLog().get(i).location().orElseThrow();
        }
        return 0;
    }

    // calculates dot product of two vectors with the first vector having all values
    // with the extra log function beforehand
    private double dotProductWithNonLinearWeighting(int[] A, int[] B) {
        int result = 0;
        for(int i=0;i<playerStartingLocations.length;i++){
            result += (int) ((- Math.log(A[i])*100) * B[i]);
        }
        return result;
    }

}
