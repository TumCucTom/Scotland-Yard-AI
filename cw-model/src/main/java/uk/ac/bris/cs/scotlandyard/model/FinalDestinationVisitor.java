package uk.ac.bris.cs.scotlandyard.model;

public class FinalDestinationVisitor implements Move.Visitor<Integer> {
    // retuning wherever the player ends up after a move
    @Override public Integer visit(Move.SingleMove move){return (move.destination);}
    @Override public Integer visit(Move.DoubleMove move){return (move.destination2);}
}
