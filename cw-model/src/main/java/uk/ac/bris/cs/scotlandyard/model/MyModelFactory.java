package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import javax.annotation.Nonnull;

import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.MyGameStateFactory;
import uk.ac.bris.cs.scotlandyard.model.Model.Observer.Event;

import java.util.*;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	private final class MyModel implements Model{

		private ImmutableSet<Observer> observerSet;
		private GameState state;
		public MyModel(final ImmutableSet<Observer> observerSet, final GameState state) {
			this.observerSet = observerSet;
			this.state = state;
		}

		@Override @Nonnull
		public Board getCurrentBoard(){return state;}

		@Override
		public void registerObserver(Observer observer){
			// check observer is not null and legal
			if(observer == null) throw new NullPointerException("Observer is null");

			// return a new set with the extra observer added
			//  iff the set does not contain the observer
			if(observerSet.contains(observer)) throw new IllegalArgumentException("Observer already registered");
			observerSet = new ImmutableSet.Builder<Observer>()
					.addAll(observerSet)
					.add(observer)
					.build();
		}

		@Override
		public void unregisterObserver(Observer observer){
			// check observer is not null and legal
			//	- that the observer should be already registered
			if(observer == null) throw new NullPointerException("Observer is null");
			if(!observerSet.contains(observer)) throw new IllegalArgumentException("Observer is not registered");

			// create a new builder for an immutableset of observers
			// add all exisiting observers to the build, bar the oberser supplied as an argument
			ImmutableSet.Builder<Observer> observerSetBuilder = new ImmutableSet.Builder<Observer>();
			for(Observer existingObserver : observerSet){
				if(!existingObserver.equals(observer)) observerSetBuilder.add(existingObserver);
			}

			// build and return
			observerSet = observerSetBuilder.build();
		}

		@Override @Nonnull
		public ImmutableSet<Observer> getObservers(){return observerSet;}

		@Override public void chooseMove(@Nonnull Move move){
			// advance the model with the move given
			state = state.advance(move);

			// see who the winner(s) if applicable are
			ImmutableSet<Piece> winners = state.getWinner();

			// if there are winners give the observers the game over event
			// otherwise give the move made event
			Event update = winners.isEmpty()? Event.MOVE_MADE : Event.GAME_OVER;

			// update observers with corresponding information
			for(Observer observer : observerSet){
				observer.onModelChanged(state, update);
			}
		}
	}
	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {

		MyGameStateFactory myGameStateFactory = new MyGameStateFactory();
		GameState state = myGameStateFactory.build(setup, mrX, detectives);
		return new MyModel(ImmutableSet.of(),state);
	}
}
