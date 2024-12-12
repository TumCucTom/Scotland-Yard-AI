package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class Clive implements Ai {

	@Nonnull @Override public String name() { return "Clive"; }

	private Move getBestMoveFromMMAB(Board board){
		MiniMaxAlphaBeta mmab = new MiniMaxAlphaBeta(board);
		return mmab.giveBestMove();
	}

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		return getBestMoveFromMMAB(board);
	}
}
