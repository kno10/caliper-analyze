package com.googlecode.caliperanalyze.util;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Naive least squares fitting using matrix operations. Not very robust against
 * outliers, although regularization can help a bit.
 * 
 * Not an iterative optimization algorithm, but the direct computation.
 * 
 * Not thoroughly tested, use at your own risk.
 * 
 * @author Erich Schubert
 */
public class LeastSquaresFit {
  /**
   * Thikonov regularized linear least squares, aka ridge regression.
   * 
   * Solve (A * x - b)^2 with regularization.
   * 
   * @param mat Matrix A
   * @param vec Vector b
   * @param lambda Regularization parameter lambda.
   * @return Least squares fit
   */
  public static DenseMatrix64F tikhonovLeastSquares(DenseMatrix64F mat, DenseMatrix64F vec, double lambda) {
    final int dof = mat.numCols;
    // w = (X' * X + \lambda * I)^{-1} * X' * t
    DenseMatrix64F omat = new DenseMatrix64F(dof, dof);
    // X' * X
    CommonOps.multTransA(mat, mat, omat);
    // + lambda * I
    // DenseMatrix64F l = CommonOps.identity(dof);
    // CommonOps.scale(lambda, l);
    // CommonOps.add(l, omat, omat);
    for (int i = 0; i < dof; i++) {
      omat.add(i, i, lambda);
    }
    // Invert.
    CommonOps.invert(omat);
    // * X'
    DenseMatrix64F omat2 = new DenseMatrix64F(dof, vec.numRows);
    CommonOps.multTransB(omat, mat, omat2);
    DenseMatrix64F res = new DenseMatrix64F(dof, 1);
    CommonOps.mult(omat2, vec, res);
    return res;
  }
}
