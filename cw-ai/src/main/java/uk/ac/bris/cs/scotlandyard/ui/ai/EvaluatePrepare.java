package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

public class EvaluatePrepare implements EvaluatePlayers {
    private final Board board;
    private final int[] playerLocations;

    private final ImmutableMap<Piece, Board.TicketBoard> pieceTicketBoardMap;

    public EvaluatePrepare(Board board,
                           int[] playerLocations,
                           ImmutableMap<Piece, Board.TicketBoard> pieceTicketBoardMap){
        this.board = board;
        this.playerLocations = playerLocations;
        this.pieceTicketBoardMap = pieceTicketBoardMap;
    }

    // get the number of moves until mrX's location is next revealed
    private static int getMoveUntilXReveal(Board board){
        int currentMoves = board.getMrXTravelLog().size();
        int movesToGo = 0;
        // count down to 0
        // once the moves made has been matched we start counting until
        // the next reveal move is found
        for(boolean reveal : board.getSetup().moves){
            currentMoves--;
            if(currentMoves<0){
                movesToGo++;
                if(reveal) break;
            }
        }
        return movesToGo;
    }

    @Override
    public double evaluateStateWithMove(Move.SingleMove move){
        // we want to know how many moves until he is revealed (relevant at the start of the game)
        // so that we do not end up in a situation where we were on a good square but are forced
        // to move to a less optimal one
        int value;
        int movesUntilXReveal = getMoveUntilXReveal(board);

        // we want to be on a train and bus station preferably
        // okay to just be on a bus station
        // the go before a reveal (ignoring for if mrX takes a double move)
        int[] possibleFutureNodes = PossibleLocationsVectorCalculator
                .calculateForPlayer(move.destination,movesUntilXReveal-1,board);

        // we want to see if the possible future moves match up with where
        // bus and train stations are (2 points for train (and bus), 1 for bus, -1 for neither)
        // just the location vector will be returned if there were no extra moves to consider,
        // so no special cases to consider there
        ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph = board.getSetup().graph;
        value = matchesToTrainOrBus(possibleFutureNodes,graph);

        // now consider where the other players are and how the move affects that
        // most simply, we just try to be as far from other players as possible
        value += RunDSP.getTotalDistanceOfPlayerFromPoint(move.destination,playerLocations,graph);

        // consider if you will get stuck due to tickets and if you are using low tickets
        value += (int) EvaluateTickets.evalForBadTicketUsePlayer(board,move,pieceTicketBoardMap.get(move.commencedBy()));

        return value;
    }

    private int matchesToTrainOrBus(
            int[] possibleFutureNodes,
            ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {
        // for every node check if the player can get there
        for (int node : possibleFutureNodes) {
            if (node == 0) continue;
            // if so, see if this node is a bus and/or train node
            // 2 is a train, 1 is a bus, 0 is taxis only
            // node +1 as the vector starts from 0 not 1
            int busOrTrain = NodeType.nodeEval(100,node, graph);
            if (busOrTrain != 0) return busOrTrain;
        }
        return -1;
    }
}
