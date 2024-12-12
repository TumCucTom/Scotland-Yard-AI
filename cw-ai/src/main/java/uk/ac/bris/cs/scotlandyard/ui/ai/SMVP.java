package uk.ac.bris.cs.scotlandyard.ui.ai;

// implementing faster than naive, sparse matrix-vector products
public class SMVP {

    // where A is an adjacency list and B is a vector
    public static int[] calculate(int[][] A, int[] B){
        int[] C = new int[199];
        // we are just reproducing naive matrix-vector products but only doing
        // calculations where non-zero elements exist for A
        for(int i=0;i<A.length;i++){
            for(int j=0;j<A[i].length;j++){
                // if the item zero has been reached then there are no more connecting nodes so skip
                // to the next source node
                if(A[i][j]==0)break;

                // let the ith element of C =
                //      [(the element at position ij in the full matrix of A) - which is always 1 here
                //      * by the item at the position j in the given vector (so we have to take the value stored in A at ij as it is
                //          an adjacency list not a matrix)
                //      ] + this to the value already stored at ith element of C
                C[i] += B[A[i][j]];
            }
        }
        return C;
    }
}
