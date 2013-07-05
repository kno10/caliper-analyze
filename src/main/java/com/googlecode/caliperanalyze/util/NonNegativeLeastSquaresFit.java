package com.googlecode.caliperanalyze.util;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

public class NonNegativeLeastSquaresFit {
  /**
   * Solve min ||E x - f|| for x >= 0.
   * 
   * Using the Henson and Lavson approach of active sets.
   * 
   * Compared to the original algorithm, we use a different storage of the P and
   * Z sets, by permuting an array and storing a split point.
   * 
   * {@code idx[i < act]} is the set P, {@code idx[i >= act]} is the set Z. EP
   * is stored in transposed form, and the rows are ordered according to idx, 0
   * columns are not used.
   * 
   * TODO: improve to use: R. Bro, S. D. Jong, A fast non-negativity-constrained
   * least squares algorithm, Journal of Chemometrics, Vol. 11, No. 5, p.
   * 393–401, 1997
   * 
   * @param E Matrix A
   * @param f Target vector b.
   * @return x
   */
  public static DenseMatrix64F nnls(DenseMatrix64F E, DenseMatrix64F f) {
    // TODO: make tolerance parameterizable
    final double tolerance = 0.01;
    final int maxiter = 1000;

    final int dof = E.numCols;
    // Step 1: Initialize active/passive set P/Z:
    int[] idx = new int[dof];
    for (int i = 0; i < dof; i++) {
      idx[i] = i;
    }
    // Number of active variables (remainder is passive, splitpoint P/Z)
    int act = 0;

    // Output vector storage:
    DenseMatrix64F x = new DenseMatrix64F(dof, 1);
    double[] xv = x.data;
    // Multiplication storage:
    DenseMatrix64F fmEx = new DenseMatrix64F(E.numRows, 1);
    DenseMatrix64F w = new DenseMatrix64F(dof, 1);
    double[] wv = w.data;

    // Working copy of (part of) A, stored in transposed form.
    DenseMatrix64F EPT = new DenseMatrix64F(E.numCols, E.numRows);
    DenseMatrix64F EPTEPinv = new DenseMatrix64F(E.numCols, E.numCols);
    DenseMatrix64F EPTEPinvEPT = new DenseMatrix64F(E.numCols, E.numRows);

    // Gradient computation storage:
    DenseMatrix64F z = new DenseMatrix64F(dof, 1);
    double[] zv = z.data;

    int iter = 0;
    while (act < dof) {
      // System.err.println("Current x: " + format(xv));
      iter++;
      if (iter > maxiter) {
        System.err.println("Max iter hit.");
        break;
      }
      // Step 2: Compute: w = E^T (f - Ex)
      CommonOps.mult(E, x, fmEx); // fmEx = Ex
      CommonOps.add(f, -1, fmEx, fmEx); // fmEx = f - Ex
      CommonOps.multTransA(E, fmEx, w); // w = E^T (f-Ex)
      // Step 3+4: Find maximum in passive set and make active.
      {
        int t = -1;
        double maxw = tolerance;
        for (int i = act; i < dof; i++) { // in active set Z only
          final double wi = wv[idx[i]];
          if (wi > maxw) {
            t = i;
            maxw = wi;
          }
        }
        // Step 3: No positive value.
        if (t < 0) {
          break;
        }
        // Step 5: Swap maximum with first inactive:
        swap(idx, act, t);
        // Step 6 part 1: Copy active column to ET (as row; so previous rows can
        // be
        // kept; EJML matrixes are stored row-major order):
        EPT.reshape(act + 1, EPT.numCols, true);
        for (int i = 0; i < EPT.numCols; i++) {
          EPT.unsafe_set(act, i, E.unsafe_get(i, idx[act]));
        }
        // System.err.println("Activating " + idx[act] + " score: " + maxv);
        // We have a new active variable.
        act++;
      }
      // Step 6 part 2: Compute z
      while (true) {
        iter++;
        if (iter > maxiter) {
          System.err.println("Max iter hit.");
          break;
        }
        // Update z:
        // Inverse EP^T EP:
        EPTEPinv.reshape(act, act); // set size
        CommonOps.multTransB(EPT, EPT, EPTEPinv); // EP^T EP
        CommonOps.invert(EPTEPinv); // ^{-1}
        // (EP^T EP)^{-1} EP^T
        EPTEPinvEPT.reshape(act, EPTEPinvEPT.numCols); // set size
        CommonOps.mult(EPTEPinv, EPT, EPTEPinvEPT); // (EP^T EP)^{-1} EP^T
        z.reshape(act, 1); // set size of s
        // Step 6: z is solution of reduced problem.
        CommonOps.mult(EPTEPinvEPT, f, z);
        // System.err.println("Find neg: " + format(sv));
        // Step 7+8+9: Find any negative value; compute minimum alpha.
        int mini = -1;
        double alpha = Double.POSITIVE_INFINITY;
        for (int i = 0; i < act; i++) {
          final double zi = zv[i];
          if (zi < -tolerance) {
            final double xi = xv[idx[i]];
            // Step 8/9: alpha
            final double alphai = xi / (xi - zi);
            // System.err.println("i: " + i + " si: " + si + " alphai: " +
            // alphai);
            if (alphai < alpha) {
              mini = i;
              alpha = alphai;
            }
          }
        }
        // Step 7: Stop if nothing to deactivate.
        if (mini < 0) {
          break;
        }
        // Step 10: Update x, in the active columns only
        // (the others will not be used anyway)
        // System.err.println("Alpha: " + alpha + " mini: " + mini + " (" +
        // idx[mini] + ")");
        for (int i = 0; i < act; i++) {
          xv[idx[i]] += alpha * (zv[i] - xv[idx[i]]);
        }
        // System.err.println("Updated x: " + format(xv));
        // Step 11: deactivate all zero indices.
        for (int i = act - 1; i >= 0; i--) {
          final double xi = xv[idx[i]];
          if (Math.abs(xi) < tolerance) {
            // System.err.println("Deactivating(" + i + ") " + idx[i] +
            // " " + xi);
            act--;
            swapRows(EPT, i, act);
            swap(idx, i, act);
            xv[idx[act]] = 0; // deactivate.
            zv[i] = zv[act];
            zv[act] = 0;
            EPT.reshape(act, EPT.numCols, true);
          }
        }
        // System.err.println("Active: " + act + " " + format(idx) + " " +
        // format(sv));
      }
      // Step 7: x = z, in active dimensions, 0 in passive
      for (int i = 0; i < act; i++) {
        if (zv[i] > tolerance) {
          xv[idx[i]] = zv[i];
        } else {
          xv[idx[i]] = 0.;
        }
      }
      for (int i = act; i < dof; i++) {
        xv[idx[i]] = 0;
      }
    }

    // System.err.println("Final x: " + format(xv));
    return x; // Step 12.
  }

  static String format(double[] d) {
    StringBuilder buf = new StringBuilder();
    for (double v : d) {
      if (buf.length() > 0) {
        buf.append(',');
      }
      buf.append(v);
    }
    return buf.toString();
  }

  static String format(int[] d) {
    StringBuilder buf = new StringBuilder();
    for (int v : d) {
      if (buf.length() > 0) {
        buf.append(',');
      }
      buf.append(v);
    }
    return buf.toString();
  }

  static void swap(int[] idx, int i, int j) {
    final int tmp = idx[i];
    idx[i] = idx[j];
    idx[j] = tmp;
  }

  static void swapRows(DenseMatrix64F A, int rowA, int rowB) {
    int indexA = rowA * A.numCols;
    int indexB = rowB * A.numCols;

    for (int i = 0; i < A.numCols; i++, indexA++, indexB++) {
      double temp = A.data[indexA];
      A.data[indexA] = A.data[indexB];
      A.data[indexB] = temp;
    }
  }

  public static void main(String[] args) {
    DenseMatrix64F C = new DenseMatrix64F(new double[][] //
    { { 0.0372, 0.2869 },//
    { 0.6861, 0.7071 },//
    { 0.6233, 0.6245 },//
    { 0.6344, 0.6170 } });

    DenseMatrix64F C1 = new DenseMatrix64F(new double[][] //
    { { 0.0372, 0.2869, 0.4 }, //
    { 0.6861, 0.7071, 0.3 }, //
    { 0.6233, 0.6245, 0.1 }, //
    { 0.6344, 0.6170, 0.5 } });

    DenseMatrix64F C2 = new DenseMatrix64F(new double[][] //
    { { 0.0372, 0.2869, 0.4 },//
    { 0.6861, 0.7071, -0.3 },//
    { 0.6233, 0.6245, -0.1 },//
    { 0.6344, 0.6170, 0.5 } });

    DenseMatrix64F d = new DenseMatrix64F(4, 1, true, new double[] { 0.8587, 0.1781, 0.0747, 0.8405 });

    {
      DenseMatrix64F x1 = nnls(C, d);
      double[] expect = new double[] { 0., 0.6929344 };
      double sum = 0.;
      for (int i = 0; i < expect.length; i++) {
        sum += Math.abs(expect[i] - x1.data[i]);
      }
      System.err.println("Difference to expectation: " + sum);
    }
    {
      DenseMatrix64F x2 = nnls(C1, d);
      double[] expect = new double[] { 0., 0., 1.61692157 };
      double sum = 0.;
      for (int i = 0; i < expect.length; i++) {
        sum += Math.abs(expect[i] - x2.data[i]);
      }
      System.err.println("Difference to expectation: " + sum);
    }
    {
      DenseMatrix64F x3 = nnls(C2, d);
      double[] expect = new double[] { 0., 0.55941697, 1.21501154 };
      double sum = 0.;
      for (int i = 0; i < expect.length; i++) {
        sum += Math.abs(expect[i] - x3.data[i]);
      }
      System.err.println("Difference to expectation: " + sum);
    }
  }
}
