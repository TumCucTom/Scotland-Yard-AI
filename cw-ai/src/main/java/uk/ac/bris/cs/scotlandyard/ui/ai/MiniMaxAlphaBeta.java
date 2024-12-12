package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.*;

import com.google.common.collect.ImmutableSet;

import java.lang.Math;

public class MiniMaxAlphaBeta {

    private final Board board;
    private int[] detectivesLocations;
    private int mrXLocation;
    private final int depth;
    private final FinalDestinationVisitor finalDestinationVisitor;
    private UpdatableTicketBoard mrXTickets;
    private final ImmutableValueGraph<Integer,ImmutableSet<ScotlandYard.Transport>> graph;


    public MiniMaxAlphaBeta(Board board){
        // getting the players and MrX locations
        ImmutableSet<Piece> playerPieces = board.getPlayers();
        int[] detectivesLocations = new int[playerPieces.size()-1];
        int count = 0;
        for(Piece piece : playerPieces){
            if(piece.isDetective()){
                detectivesLocations[count] = board.getDetectiveLocation((Piece.Detective)piece).orElseThrow();
                count++;
            }
            else{
                // we know that mrX is moving next if we are in this class
                mrXLocation = board.getAvailableMoves().asList().get(0).source();
                mrXTickets = new UpdatableTicketBoard(board.getPlayerTickets(piece).orElseThrow());
            }
        }
        this.detectivesLocations = detectivesLocations;
        this.board = board;
        depth = getDepth(detectivesLocations.length);
        finalDestinationVisitor = new FinalDestinationVisitor();
        graph = board.getSetup().graph;
    }

    private int getDepth(int length) {
        int[] depth = {-1,2,1,1,0,0};
        return depth[length];
    }


    // yes, could check if mrx makes the move every round but more efficient to pass
    // the mrX moving var
    private double searchMMAB(double depth, double alpha, double beta, boolean mrXMoving){
        // return the evaluation function once depth reached
        // set the special move option if mrx has just played
        if(depth == 0)return EvaluateMrX.evaluateState(board,mrXLocation,detectivesLocations);

        // get playable moves for this state
        ImmutableSet<Move> moves = board.getAvailableMoves();

        // if mrX is playing
        if(mrXMoving) {
            double evaluation = -1000;
            for (Move move : moves) {

                // make a move
                // update MrX location
                // and tickets
                UpdatableTicketBoard oldTickets = mrXTickets;
                mrXTickets.updateCount(move);
                mrXLocation = move.accept(finalDestinationVisitor);

                // update the evaluation if a secret or double move is being used incorrectly
                evaluation = EvaluateMrX.updateEvalForSpecialMoves(move,board,mrXLocation,detectivesLocations) +
                        EvaluateTickets.evalForBadTicketUseX(move,mrXTickets) +
                        0.1*NodeType.nodeEval(move.accept(finalDestinationVisitor),graph) +
                        Math.max(evaluation, searchMMAB(depth-1, alpha, beta, false));
                alpha = Math.max(alpha, evaluation);

                // beta cutoff for this branch
                if (evaluation >= beta) break;

                // return mrx to his original location
                // and tickets
                mrXTickets = oldTickets;
                mrXLocation = move.source();
            }
            return evaluation;
        }
        // otherwise it's the players turn
        double evaluation = 1000;

        for (Move move : moves) {

            // make a move
            // change the player to the correct location
            int[] oldLocations = detectivesLocations;
            updatePlayerList(move);

            // if mrX is now to move, flip to calculate for him
            ImmutableSet<Move> nextMoves = board.getAvailableMoves();
            if(!nextMoves.isEmpty() && nextMoves.asList().get(0).commencedBy().equals(Piece.MrX.MRX)){
                evaluation = Math.min(evaluation, searchMMAB(depth-1,alpha,beta, true));
            }
            // otherwise players are still to play so add to the score
            // TO-DO - this makes things very slow for a high number of players
            else evaluation = Math.min(evaluation, searchMMAB(depth,alpha,beta,false));
            beta = Math.min(beta, evaluation);

            // alpha cutoff for this branch
            if (evaluation <= alpha) break;

            // return the player to original position
            detectivesLocations = oldLocations;
        }
        return evaluation;
    }

    private void updatePlayerList(Move move) {
        for(int i=0;i<detectivesLocations.length;i++){
            if(detectivesLocations[i] == move.source()){
                detectivesLocations[i] = move.accept(finalDestinationVisitor);
                break;
            }
        }
    }

    public Move giveBestMove(){
        ImmutableSet<Move> moves = board.getAvailableMoves();
        double bestMoveVal = -1000;
        Move bestMove = moves.asList().get(0);

        // for every move evaluate the move branch
        // get the best move according to the eval
        for(Move move : moves){
            // change mrX location and ticketboard
            UpdatableTicketBoard oldTickets = mrXTickets;
            mrXTickets.updateCount(move);
            mrXLocation = move.accept(finalDestinationVisitor);

            // evaluate
            double moveVal = EvaluateMrX.updateEvalForSpecialMoves(move,board,mrXLocation,detectivesLocations) +
                    EvaluateTickets.evalForBadTicketUseX(move,mrXTickets) +
                    0.1*NodeType.nodeEval(move.accept(finalDestinationVisitor),graph) +
                    searchMMAB(depth,-1000,1000, false);
            if(moveVal >= bestMoveVal){
                bestMoveVal = moveVal;
                bestMove = move;
            }

            // return ticketboard
            mrXTickets = oldTickets;
            mrXLocation = move.source();
        }
        return bestMove;
    }


}