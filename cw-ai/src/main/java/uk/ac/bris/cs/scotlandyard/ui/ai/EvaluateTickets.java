package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.Objects;

// functions relating to calculating how evaluation should change based on ticket counts and use
public class EvaluateTickets {

    // we want to say a move is bad if, given the players current tickets - it could get them stuck
    // the exception is if this is the final move of the game
    private static int returnBadEvalIfStuckForPlayer(
            Board board,
            Move.SingleMove move,
            Board.TicketBoard pieceTicketBoard){
        // first check that this is not the final move of the game
        GameSetup setup = board.getSetup();
        if(setup.moves.size() == board.getMrXTravelLog().size()) return 0;

        // 2 is we cannot get out, 1 is that we can only get out by reversing the move we just did
        //, 1 is that we can get out any other way
        int out = 2;

        // first check if we have two tickets of type needed to get to destination
        // this would be moving forwards and back again
        if(pieceTicketBoard.getCount(move.ticket) >1) out = 1;

        if(canEscapeFromNode(pieceTicketBoard,board,move.destination,move)) out = 0;

        // return large, but not as bad as a loss, negative eval for getting yourself stuck
        return out * -300;
    }

    // gives small negative eval if tickets are used that the player is low on
    // favours moves that use tickets in higher quantities that achieve the same goal
    private static double discourageUsingTransportWithLowTickets(
            Board.TicketBoard tickets,
            Move move){
        return move.accept(new Move.Visitor<>() {
            @Override
            public Double visit(Move.SingleMove move) {
                int numTickets = tickets.getCount(move.ticket);
                return (double) (numTickets >=5 ? 0 : -5 + numTickets);
            }

            @Override
            public Double visit(Move.DoubleMove move) {
                int numTickets1 = tickets.getCount(move.ticket1);
                int numTickets2 = tickets.getCount(move.ticket2);
                return (double) ((numTickets1 >=5 ? 0 : -5 + numTickets1) + (numTickets2 >=5 ? 0 : -5 + numTickets2));
            }
        });
    }

    private static double discourageUsingTransportWithLowTickets(Move.SingleMove move, Board board){
        int numTickets = board.getPlayerTickets(move.commencedBy()).orElseThrow().getCount(move.ticket);
        return numTickets >=5 ? 0 : -5 + numTickets;
    }

    private static boolean canEscapeFromNode(Board.TicketBoard pieceTicketBoard,
                                             Board board,
                                             int destination,
                                             Move.SingleMove singleMove){
        // see if the node that we want to move to has an out given our ticket board
        ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph = board.getSetup().graph;

        // check for every node you can move to from the destination
        // if you have sufficient tickets for any type of travel to any of those nodes
        for(int node : graph.adjacentNodes(destination)){
            for(ScotlandYard.Transport transport : Objects.requireNonNull(graph.edgeValueOrDefault(destination, node, ImmutableSet.of()))){
                ScotlandYard.Ticket ticket = transport.requiredTicket();
                if(ticket.equals((singleMove.ticket)))
                    if(pieceTicketBoard.getCount(ticket) > 1) return true;
                else if(pieceTicketBoard.getCount(ticket) > 0) return true;
            }
        }
        return false;
    }

    // call both evaluation adjustment functions
    public static double evalForBadTicketUsePlayer(Board board, Move.SingleMove move, Board.TicketBoard pieceTicketBoard){
        return discourageUsingTransportWithLowTickets(move,board) + returnBadEvalIfStuckForPlayer(board, move, pieceTicketBoard);
    }

    // call don't need to check whether mrX is stuck because this constitues a loss
    // which is checked before in eval mrX
    public static double evalForBadTicketUseX(Move move, Board.TicketBoard tickets){
        return discourageUsingTransportWithLowTickets(tickets, move);
    }
}
