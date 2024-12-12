package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

import java.util.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {
	private final class MyGameState implements GameState{

		private final GameSetup setup;
		private final ImmutableSet<Piece> remaining;
		private final ImmutableList<LogEntry> log;
		private final ImmutableSet<Move> moves;
		private final ImmutableSet<Piece> winner;
		private final FinalDestinationVisitor finalDestinationVisitor = new FinalDestinationVisitor();
		private final Player mrX;
		private final List<Player> detectives;

		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives){

			// arguments that should be non-null then throw error if null
			if(mrX == null) throw new NullPointerException("mrX is missing!");
			if(detectives == null) throw new NullPointerException("Players are missing!");

			// Detectives should be setup correctly, namely
			//		MrX should not be a detective
			//		There should be no detectives which are the same player
			//		No detectives should start in the same place
			//		All detectives should exist
			//		Detectives should not have double or secret tickets
			//		Detectives should not be the black piece
			for(int i=0;i<detectives.size();i++){
				Player player = detectives.get(i);
				if(player == null) throw new NullPointerException("A player does not exist!");
				if(player.isMrX()) throw new IllegalArgumentException("MrX in players");
				for(int j=i+1;j<detectives.size();j++){
					Player player2 = detectives.get(j);
					if(player == player2)throw new IllegalArgumentException("Duplicate detectives!");
					if(player.location() == player2.location()){
						throw new IllegalArgumentException("Players in same places!");
					}
				}
				if(player.has(Ticket.DOUBLE) || player.has(Ticket.SECRET)) throw new IllegalArgumentException("Wrong ticket!");
				if(player.piece().webColour().equals("#000")) throw new IllegalArgumentException("Detective is black!");
			}

			// the graph should have its nodes and edges
			if(setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty!");
			// there should be one mrX - more than one covered in the detectives code
			if(mrX.isDetective()) throw new IllegalArgumentException("No mrx present!");

			// if setup does not have any moves then the game is not playable (0 turn for all players)
			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Game length is zero!");

			// initialising
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.winner = getWinner();
			this.moves = getAvailableMoves();
		}

		private Player getPlayerFromPiece(Piece piece){
			for(Player player : detectives){
				if(player.piece() == piece) return player;
			}
			if(piece == mrX.piece()) return mrX;
			throw new IllegalArgumentException("Piece does not belong to a player!");
		}

		private ImmutableList<LogEntry> getNewLog(Move move){
			//return a new the new log entry depending on whether a double or single move was used
			//	done via visitor pattern
			return move.accept(new Visitor<>(){
				@Override public ImmutableList<LogEntry> visit(SingleMove move){
					// create new LogEntry depending on whether MrX should reveal his location
					// this turn
					LogEntry newEntry = setup.moves.get(log.size()) ?
							LogEntry.reveal(move.ticket, move.destination) :
							LogEntry.hidden(move.ticket);

					// build return a new immutable list with the new move 'appended'
					return new ImmutableList.Builder<LogEntry>()
							.addAll(log)
							.add(newEntry)
							.build();
				}
				@Override public ImmutableList<LogEntry> visit(DoubleMove move){
					// create 2 new Log Entries depending on whether MrX should reveal his location
					// that turn
					LogEntry newEntry1 = setup.moves.get(log.size()) ?
							LogEntry.reveal(move.ticket1, move.destination1) :
							LogEntry.hidden(move.ticket1);

					LogEntry newEntry2 = setup.moves.get(log.size()+1) ?
							LogEntry.reveal(move.ticket2, move.destination2) :
							LogEntry.hidden(move.ticket2);

					// build return a new immutable list with the new moves 'appended'
					return new ImmutableList.Builder<LogEntry>()
							.addAll(log)
							.add(newEntry1)
							.add(newEntry2)
							.build();
				}

			});
		}

		private int getNewLocation(Move move){
			//return a new the new location depending on whether a double or single move was used
			//	done via visitor pattern
			return move.accept(finalDestinationVisitor);
		}

		private Player getUpdatedTakeTicketPlayer(Move move, Player player){
			//return a new MrX with correct number of tickets depending on whether a
			// double or single move was used - done via visitor pattern
			return move.accept(new Visitor<>(){
				@Override public Player visit(SingleMove move){return player.use(move.ticket);}
				@Override public Player visit(DoubleMove move){
					Player usedFirstTicketPlayer = player.use(move.ticket1);
					Player usedDoubleTicketPlayer = usedFirstTicketPlayer.use(Ticket.DOUBLE);
					return usedDoubleTicketPlayer.use(move.ticket2);}
			});
		}

		private Player getUpdatedGiveTicketPlayer(Move move, Player player){
			//return new MrX with correct number of tickets depending on whether a
			// double or single move was used - done via visitor pattern
			return move.accept(new Visitor<>(){
				@Override public Player visit(SingleMove move){return player.give(move.ticket);}
				@Override public Player visit(DoubleMove move){
					Player temp = player.give(move.ticket1);
					return temp.give(move.ticket2);}
			});
		}

		private Board.GameState advanceMrX(Move move) {

			// Get updated log
			ImmutableList<LogEntry> newLog = getNewLog(move);

			// Get new mrx
			//	take appropriate ticket away
			// 	update his location
			Player updatedLocationMrX = mrX.at(getNewLocation(move));
			Player newMrX = getUpdatedTakeTicketPlayer(move,updatedLocationMrX);

			// Make it so that none of the players have made a move yet
			ImmutableSet.Builder<Piece> newRemainingBuilder = new ImmutableSet.Builder<>();
			for(Player player : detectives){
				// add back all detectives who still have tickets left
				if(getPlayerTickets(player.piece()).isPresent()) newRemainingBuilder.add(player.piece());
			}
			ImmutableSet<Piece> newRemaining = newRemainingBuilder.build();

			return new MyGameState(setup, newRemaining, newLog, newMrX, detectives);
		}

		private Board.GameState advanceDetective(Move move){
			// Get new detective
			//	take appropriate ticket away
			// 	update their location
			Player currentPlayer = getPlayerFromPiece(move.commencedBy());
			Player updatedLocationPlayer = currentPlayer.at(getNewLocation(move));
			Player newPlayer = getUpdatedTakeTicketPlayer(move,updatedLocationPlayer);

			// Update detectives with new player
			ImmutableList.Builder<Player> newDetectivesBuilder = new ImmutableList.Builder<>();
			for(Player player : detectives){
				if(player!=currentPlayer){
					newDetectivesBuilder.add(player);
				}
			}
			// re add the new player and build
			ImmutableList<Player> newDetectives = newDetectivesBuilder.add(newPlayer).build();

			// update mrx to have the ticket the player used
			Player newMrX = getUpdatedGiveTicketPlayer(move, mrX);

			// stop player from being able to move this turn again
			ImmutableSet.Builder<Piece> newRemainingBuilder = new ImmutableSet.Builder<>();
			for(Piece piece : remaining){
				if(piece != move.commencedBy()) newRemainingBuilder.add(piece);
			}
			ImmutableSet<Piece> newRemaining = newRemainingBuilder.build();

			// if there are no detectives left to play or all the remaining players are stuck
			// make it mrX's turn now
			// and make mrx the only player remaining
			boolean stuck = true;
			for(Piece piece : newRemaining){
				if(playerCanMove(getPlayerFromPiece(piece))) stuck = false;
			}
			if(newRemaining.isEmpty() || stuck) {
				ImmutableSet.Builder<Piece> newRemainingBuilder2 = new ImmutableSet.Builder<>();
				newRemainingBuilder2.add(newMrX.piece());
				newRemaining = newRemainingBuilder2.build();
			}

			return new MyGameState(setup, newRemaining, log, newMrX, newDetectives);
		}

		@Nonnull @Override
		public Board.GameState advance(Move move){
			// don't allow a move that is not in the available moves to be played
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move : "+ move);

			// if this is mrx's move
			if(move.commencedBy().isMrX()){
				return advanceMrX(move);
			}

			return advanceDetective(move);
		}

		@Nonnull @Override
		public GameSetup getSetup(){return setup;}

		@Nonnull @Override
		public com.google.common.collect.ImmutableSet<Piece> getPlayers(){
			List<Piece> players = new ArrayList<>();

			// get the pieces for each player
			for(Player player : detectives){
				players.add(player.piece());
			}

			// build and return the immutableSet
			return new ImmutableSet.Builder<Piece>()
					.addAll(players)
					.add(mrX.piece())
					.build();
		}

		@Nonnull @Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective){

			// for every detective
			for(Player player : detectives){
				// if their pieces colour matches the colour given then
				// return that player's location that holds said piece
				if(player.piece() == detective) return Optional.of(player.location());
			}

			// if it doesn't exist
			return Optional.empty();
		}

		@Nonnull @Override
		public Optional<Board.TicketBoard> getPlayerTickets(Piece piece) {

			//get the player and check they exist
			// if they don't, return empty
			Optional<Player> currentPlayer = getPlayerFromPieceOrEmpty(piece);

			// return empty optional or the ticketboard respectively
			return currentPlayer.isEmpty() ? Optional.empty() :
                    Optional.of((TicketBoard) ticket -> (new ImmutableMap.Builder<Ticket,Integer>()
							.putAll(currentPlayer.orElseThrow().tickets())
							.build())
							.getOrDefault(ticket, 0));
		}

		private Optional<Player> getPlayerFromPieceOrEmpty(Piece piece) {
			for(Player player : detectives){
				if(player.piece() == piece) return Optional.of(player);
			}
			if(piece == mrX.piece()) return Optional.of(mrX);
			return Optional.empty();
		}

		@Nonnull @Override
		public com.google.common.collect.ImmutableList<LogEntry> getMrXTravelLog(){return log;}

		private com.google.common.collect.ImmutableSet<Piece> givePlayerWinners(){
			ImmutableSet.Builder<Piece> winners = new ImmutableSet.Builder<>();
			for(Player player :detectives){
				winners.add(player.piece());
			}
			return winners.build();
		}

		private com.google.common.collect.ImmutableSet<Piece> giveMrXWinner(){
			return new ImmutableSet.Builder<Piece>()
					.add(mrX.piece())
					.build();
		}

		private com.google.common.collect.ImmutableSet<Piece> giveNoWinners(){
			return ImmutableSet.of();
		}

		private boolean playerCanMove(Player player){
			// still works for mrX as he cannot make a double move if he cannot first make a legal
			// single move
			return !makeSingleMoves(setup,detectives,player, player.location()).isEmpty();
		}

		@Nonnull @Override
		public com.google.common.collect.ImmutableSet<Piece> getWinner(){

			boolean ticketsLeft = false;
			// only have this boolean in use if it is the players turn
			boolean playersAreStuck = true;
			for (Player player :detectives){
				// if a detective and mrx occupy same square
				// players win
				if(player.location() == mrX.location()) return givePlayerWinners();

				// 	seeing if this player has tickets
				if(getPlayerTickets(player.piece()).isPresent()) ticketsLeft = true;

				// 	seeing if this player can move
				if(playerCanMove(player)) playersAreStuck = false;
			}
			// if all detectives run out of tickets
			// mrx wins
			// 	giving winner if none have tickets
			if(!ticketsLeft) return giveMrXWinner();

			// if all detectives cannot move
			// mrx wins
			// 	giving mrX as winner if all the players cannot move
			if(playersAreStuck) return giveMrXWinner();

			// if mrx cannot move anywhere
			// players win
			if(!playerCanMove(mrX) && remaining.contains(mrX.piece())) return givePlayerWinners();

			// if mrx has filled the log and players have all moved
			// mrx wins
			if(remaining.contains(mrX.piece()) && log.size() == setup.moves.size()) return giveMrXWinner();

			// if none of the above is satisfied then the game is not over and
			// no winners should be returned
			return giveNoWinners();
		}

		// helper function for getting the legal moves for players
		private static Set<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			Set<SingleMove> moves = new HashSet<>();

			// for every node connected to the source
			for(int destination : setup.graph.adjacentNodes(source)) {
				// and for every detective in the game
				// if there is a detective on the destination
				// skip this destination
				boolean skip = false;
				for(Player detective : detectives){
					if(detective.location() == destination){
						skip = true;
						break;
					}
				}
				if (skip) continue;

				// for every form transport from the source to the destination
				for(Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
					// check if the player has the required ticket to take this transport
					if(player.hasAtLeast(t.requiredTicket(),1)){
						// if so create the corresponding singleMove and add it to the list of all moves
						// this will add the ferry as it requires secret
						SingleMove newMove = new SingleMove(player.piece(),source,t.requiredTicket(),destination);
						moves.add(newMove);
					}
				}

				// check if the player ( always MrX) has a secret ticket
				if(player.has(Ticket.SECRET) && player.hasAtLeast(Ticket.SECRET,1)){
					// for every mode of transport from the source to destintation
					for(Transport ignored : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
						// create the corresponding singleMove with a secret ticket
						// could have an if statement to stop ferry being added again however since a set
						//	is in use it des not matter
						SingleMove newMove = new SingleMove(player.piece(),source,Ticket.SECRET,destination);
						moves.add(newMove);
					}
				}
			}

			// return the full set of moves for the given player
			return moves;
		}

		// helper function for getting double moves
		private static Set<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			Set<DoubleMove> moves = new HashSet<>();

			// return no extra moves if mrx does not have double tickets remaining
			if(!player.hasAtLeast(Ticket.DOUBLE,1)) return moves;

			// calculating the first part of the move
			// using parameterised constructor to add all moves
			Set<SingleMove> possibleFirstMoves = new HashSet<>(makeSingleMoves(setup,detectives,player,source));

			// calculating second part of the move
			for(SingleMove firstMove : possibleFirstMoves){
				// get new variables needed for second move
				Ticket usedTicket = firstMove.ticket;
				int secondSource = firstMove.destination;

				// creating a temporary mrX, the same as mrX but with one less
				//	ticket of the type used in the first move
				Player mrXUsedTicket = player.use(usedTicket);

				// getting second moves that can be made
				// using parameterised constructor to add all moves
				Set<SingleMove> correspondingSecondMoves = new HashSet<>(makeSingleMoves(setup,detectives,mrXUsedTicket,secondSource));

				// for all the second moves create a new double move with the first move info
				//	add to the final moves
				for(SingleMove secondMove : correspondingSecondMoves){
					DoubleMove newMove = new DoubleMove(player.piece(),source,
							firstMove.ticket,secondSource,
							secondMove.ticket,secondMove.destination);
					moves.add(newMove);
				}
			}

			// return the full list of moves;
			return moves;
		}

		@Nonnull @Override
		public com.google.common.collect.ImmutableSet<Move> getAvailableMoves(){
			Set<Move> moves = new HashSet<>();

			// don't allow any moves to be made if the game is over
			if(!winner.isEmpty()) return ImmutableSet.copyOf(moves);

			// for every piece left to make a move
			for(Piece remains : remaining){
				// get the player of the remaining piece
				Player player = getPlayerFromPiece(remains);

				// add all single moves
				moves.addAll(makeSingleMoves(setup, detectives, player, player.location()));
				// add double moves if there is enough moves left
				// checks inside function eliminate the need for checking if player is mrx here
				if(log.size()+2 <= setup.moves.size()) moves.addAll(makeDoubleMoves(setup,detectives,player,player.location()));
			}

			// return an ImmutableSet with items of moves
			return ImmutableSet.copyOf(moves);
		}
	}

	@Nonnull
	@Override
	public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);

	}
}