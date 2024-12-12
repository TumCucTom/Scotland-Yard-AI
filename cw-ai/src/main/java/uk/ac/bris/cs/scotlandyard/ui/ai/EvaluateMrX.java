package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class EvaluateMrX {

    // evaluation how good the position is for mrx
    // currently only work out how close he is to all the other players
    // heuristics to add
    //      tickets
    //      getting cornered
    //      closer players should be thought about more than further
    public static double evaluateState(Board board,
                                       int MrXLocation,
                                       int[] detectivesLocations){
        // retuning high/low values if a winner has been determined from this position
        if(board.getWinner().contains(Piece.MrX.MRX)) return 1000;
        if(!board.getWinner().isEmpty()) return -1000;

        // get the shortest distances from every node to MrX
        // return the total distance MrX is away * (6 - num players)
        // this is to account for other evals skewing this with different amounts of players
       return (6-detectivesLocations.length) *
               RunDSP.getTotalDistanceOfPlayerFromPoint(MrXLocation,detectivesLocations,board.getSetup().graph);
    }

    public static double updateEvalForSpecialMoves(Move move, Board board, int mrXLocation, int[] detectivesLocations) {
        double eval = 0;
        boolean isSecretNF = moveContainsSecretAndNotFerry(move, board.getSetup().graph);

        //getting the distances from detectives to mrX
        int[] distancesFromX = getDetectiveDistanceFromX(
                RunDSP.runDSPonGraph(board.getSetup().graph,mrXLocation),
                detectivesLocations);

        // accept that using the secret for a ferry is not a waste
        // discourage using either a double or secret if you are not surrounded
        // discourage using a secret the move before you are revealed
        // encourage using a double and secret when players are closing in
        if(closeToSurrounded(distancesFromX)){
            // if double -> do nothing as it will automatically be further away
            if(isSecretNF){
                // be secretive when close to many detectives
                eval +=1;
            }
        }
        else if(moveIsDouble(move)){
            // don't play this move
            // unless all non doubles cause a loss (-1000)
            eval -= 8* distancesFromX.length;
        }
        else if(isSecretNF){
            // if the next move will be shown
            // +1 for this move +1 for next move
            // -1 for indexing from 0
            int nextMoveIndex = board.getMrXTravelLog().size()+1;
            if(nextMoveIndex <24){
                if(board.getSetup().moves.get(nextMoveIndex)){
                    // just enough to make it worse than moves that will do the same thing
                    // but not worse than moves that were originally worse
                    eval -=0.5;
                }
            }
        }
        return eval;
    }

    private static boolean closeToSurrounded(int[] distancesFromX) {
        int numDetectivesClose = 0;
        int numDetectivesVeryClose = 0;
        // if there are more detectives than they don't need to be as close to be
        // equally dangerous
        for (int distance : distancesFromX) {
            if (distance <= 3) {
                numDetectivesClose++;
                if (distance <= 2) numDetectivesVeryClose++;
            }
        }
        return numDetectivesVeryClose >= 2 || numDetectivesClose >=3;
    }

    private static boolean moveContainsSecretAndNotFerry(Move move, ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {
        return move.accept(new Move.Visitor<>() {
            @Override
            public Boolean visit(Move.SingleMove move) {
                return move.ticket == ScotlandYard.Ticket.SECRET &&
                        !Objects.requireNonNull(graph.edgeValueOrDefault(move.source(), move.destination, ImmutableSet.of()))
                                .contains(ScotlandYard.Transport.FERRY);
            }

            @Override
            public Boolean visit(Move.DoubleMove move) {
                return (move.ticket1 == ScotlandYard.Ticket.SECRET &&
                        !Objects.requireNonNull(graph.edgeValueOrDefault(move.source(), move.destination1, ImmutableSet.of()))
                                .contains(ScotlandYard.Transport.FERRY))
                        ||
                        (move.ticket2 == ScotlandYard.Ticket.SECRET &&
                                !Objects.requireNonNull(graph.edgeValueOrDefault(move.source(), move.destination2, ImmutableSet.of()))
                                        .contains(ScotlandYard.Transport.FERRY));
            }
        });
    }

    private static boolean moveIsDouble(Move move) {
        return move.accept(new Move.Visitor<>() {
            @Override
            public Boolean visit(Move.SingleMove move) {return false;}

            @Override
            public Boolean visit(Move.DoubleMove move) {return true;}
        });
    }

    private static int[] getDetectiveDistanceFromX(int[] shortestLengthsFromX, int[] detectivesLocations) {
        int numDetectives = detectivesLocations.length;
        int[] distances = new int[numDetectives];
        // for every detective
        for(int i=0;i< numDetectives;i++){
            // add the shortest distance from mrX's location to the node where the detective is
            // to the distances array
            distances[i] = shortestLengthsFromX[detectivesLocations[i]];
        }
        return distances;
    }
}
