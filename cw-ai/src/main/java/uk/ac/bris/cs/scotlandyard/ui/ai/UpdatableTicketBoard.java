package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class UpdatableTicketBoard implements Board.TicketBoard {
    private Map<ScotlandYard.Ticket,Integer> tickets;

    private void makeTicketArrayFromTicketBoard(Board.TicketBoard ticketBoard) {
        tickets = new HashMap<>();
        tickets.put(ScotlandYard.Ticket.TAXI,ticketBoard.getCount(ScotlandYard.Ticket.TAXI));
        tickets.put(ScotlandYard.Ticket.BUS,ticketBoard.getCount(ScotlandYard.Ticket.BUS));
        tickets.put(ScotlandYard.Ticket.UNDERGROUND,ticketBoard.getCount(ScotlandYard.Ticket.UNDERGROUND));
        tickets.put(ScotlandYard.Ticket.SECRET,ticketBoard.getCount(ScotlandYard.Ticket.SECRET));
    }

    public UpdatableTicketBoard(Board.TicketBoard ticketBoard){
        makeTicketArrayFromTicketBoard(ticketBoard);
    }
    @Override
    public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
        return tickets.get(ticket);
    }

    public void updateCount(Move move){
        move.accept(new Move.Visitor<>() {
            @Override
            public Boolean visit(Move.SingleMove move) {
                // update the map to reflect the move that was made
                ScotlandYard.Ticket ticket = move.ticket;
                tickets.replace(ticket, tickets.get(ticket)-1);
                return false;
            }

            @Override
            public Boolean visit(Move.DoubleMove move) {
                // update the map to reflect the two moves that were made
                ScotlandYard.Ticket ticket = move.ticket1;
                tickets.replace(ticket, tickets.get(ticket)-1);
                ticket = move.ticket2;
                tickets.replace(ticket, tickets.get(ticket)-1);
                return false;
            }
        });
    }
}
