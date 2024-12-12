package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;


public class Stephen implements Ai {


    @Nonnull @Override public String name() { return "Stephen"; }
    private Move getBestMoveFromPossibleX(Board board){
        return PlayerMoves.getBestMove(board);
    }

    @Nonnull @Override public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {

        return getBestMoveFromPossibleX(board);
    }
}
