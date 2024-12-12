package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.Optional;

public class PlayerMoves {

    public static Move getBestMove(Board board){
        // calculate where MrX could be
        Optional<int[]> locations = PossibleLocationsVectorCalculator.calculateForX(board);
        int[] startingPlayerLocations = getPlayerLocations(board);
        ImmutableSet<Move> moves =  board.getAvailableMoves();
        // get the ticket board for before when the move was played
        ImmutableMap<Piece, Board.TicketBoard> pieceTicketBoardMap = getTicketBoardMap(board);

        // if mrX is about to be revealed
        // prepare and move to better locations for spreading resources
        int size = board.getMrXTravelLog().size();
        if(size == 24) return calcBestMove(moves, new EvaluateCloseIn(board,locations.orElseThrow(),startingPlayerLocations,pieceTicketBoardMap));
        if(board.getSetup().moves.get(size))
            return calcBestMove(moves, new EvaluatePrepare(board,startingPlayerLocations, pieceTicketBoardMap));

        // otherwise close in on Mrx if we have already seen where he is
        // prepared if we are still waiting to see where he is
        return calcBestMove(moves, locations.isPresent() ?
                new EvaluateCloseIn(board,locations.get(),startingPlayerLocations,pieceTicketBoardMap) :
                new EvaluatePrepare(board,startingPlayerLocations, pieceTicketBoardMap));
    }

    private static ImmutableMap<Piece, Board.TicketBoard> getTicketBoardMap(Board board) {
        ImmutableMap.Builder<Piece, Board.TicketBoard> ticketBoardImmutableMap =
                new ImmutableMap.Builder<>();

        for(Piece piece : board.getPlayers()){
            if(piece.isMrX()) continue;
            ticketBoardImmutableMap.put(piece,board.getPlayerTickets(piece).orElseThrow());
        }
        return ticketBoardImmutableMap.build();
    }

    private static int[] getPlayerLocations(Board board) {
        int[] locations = new int[6];
        int count = 0;

        // for every piece, if it is a detective
        // add its location to the locations array
        for(Piece piece : board.getPlayers()){
            if(piece.isDetective()){
                int node = board.getDetectiveLocation((Piece.Detective)piece).orElseThrow();
                locations[count] = node;
                count++;
            }
        }

        // convert the List to an array of correct size;
        int[] intArrLocations = new int[count];
        System.arraycopy(locations, 0, intArrLocations, 0, count);
        return intArrLocations;
    }

    private static Move calcBestMove(ImmutableSet<Move> moves,
                                     EvaluatePlayers eval)
    {
        System.out.println(eval);
        // initialise the best move vars
        double bestMoveVal = -1000;
        Move bestMove = moves.asList().get(0);

        // for every move evaluate the move branch
        // get the best move according to the eval
        for(Move move : moves){
            // we know all moves for players are single moves
            Move.SingleMove singleMove = (Move.SingleMove)move;

            // evaluate
            double moveVal = eval.evaluateStateWithMove(singleMove);
            System.out.print(singleMove);
            System.out.println(" "+moveVal);
            if(moveVal >= bestMoveVal){
                bestMoveVal = moveVal;
                bestMove = move;
            }
        }
        System.out.println("----------------");
        return bestMove;
    }
}
