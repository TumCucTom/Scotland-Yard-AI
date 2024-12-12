package uk.ac.bris.cs.scotlandyard.ui.ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move.SingleMove;

public interface EvaluatePlayers {
    // evaluates the future state of the board and how the move will affect the player
    double evaluateStateWithMove(SingleMove move);
}
