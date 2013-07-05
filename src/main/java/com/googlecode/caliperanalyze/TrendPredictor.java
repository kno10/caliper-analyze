package com.googlecode.caliperanalyze;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ejml.data.DenseMatrix64F;

import com.google.caliper.model.Measurement;
import com.googlecode.caliperanalyze.util.LeastSquaresFit;
import com.googlecode.caliperanalyze.util.NonNegativeLeastSquaresFit;

public class TrendPredictor {
  /**
   * Initial size for allocations.
   */
  private static final int INITIAL_SIZE = 21;

  /** Data storage. */
  double[] values = new double[INITIAL_SIZE], //
      weights = new double[INITIAL_SIZE], //
      targets = new double[INITIAL_SIZE];

  /** Number of observations. */
  int numvalues = 0;

  /** Metadata */
  String unit = null, description = null;

  /** Merge records of the same value */
  static boolean mergeRecords = false;

  /** Use NNLS or a simpler approach **/
  static boolean useNNLS = false;

  /**
   * Add a series of measurements.
   * 
   * @param measurements Measurements to add.
   * @param target Value this was measured at.
   * @return {@code this}
   */
  public TrendPredictor add(Iterable<Measurement> measurements, double target) {
    for (Measurement m : measurements) {
      add(m, target);
    }
    return this;
  }

  /**
   * Add a single measurement to the aggregate.
   * 
   * @param measurement Measurement to add.
   * @param target Value this was measured at.
   * @return {@code this}
   */
  public TrendPredictor add(Measurement measurement, double target) {
    // Sanity check for units
    if (unit == null) {
      unit = measurement.value().unit();
    } else if (!unit.equals(measurement.value().unit())) {
      throw new RuntimeException("Inconsistent units are not supported.");
    }
    // Sanity check for descriptions
    if (description == null) {
      description = measurement.description();
    } else if (!description.equals(measurement.description())) {
      throw new RuntimeException("Multiple types of measuresments are not supported.");
    }
    if (numvalues == values.length) {
      int newsize = (values.length << 1) + 1;
      values = Arrays.copyOf(values, newsize);
      weights = Arrays.copyOf(weights, newsize);
      targets = Arrays.copyOf(targets, newsize);
    }
    final double weight = measurement.weight();
    final double value = measurement.value().magnitude() / weight;
    if (mergeRecords) {
      for (int i = 0; i <= numvalues; i++) {
        if (i < numvalues) {
          if (targets[i] == target) {
            weights[i] += weight;
            values[i] += (value - values[i]) * weight / weights[i];
            break;
          }
        } else {
          values[numvalues] = value;
          weights[numvalues] = weight;
          targets[numvalues] = target;
          ++numvalues;
          break;
        }
      }
    } else {
      values[numvalues] = value;
      weights[numvalues] = weight;
      targets[numvalues] = target;
      ++numvalues;
    }
    return this;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(description).append("[").append(unit).append("]: ");
    buf.append(String.format("%d measurements ", numvalues));

    double lambda = .1;
    findFit(targets, values, numvalues, lambda, buf);
    return buf.toString();
  }

  static final double DIV_LOG2 = 1 / Math.log(2);

  enum Function {
    CONST {
      @Override
      double map(double in) {
        return 1;
      }
    },
    LOG2N {
      @Override
      double map(double in) {
        return (in > 0) ? Math.log(in) * DIV_LOG2 : 0;
      }
    },
    LOGEN {
      @Override
      double map(double in) {
        return (in > 0) ? Math.log(in) : 0;
      }
    },
    LOG10N {
      @Override
      double map(double in) {
        return (in > 0) ? Math.log10(in) : 0;
      }
    },
    LINEAR {
      @Override
      double map(double in) {
        return in;
      }
    },
    NLOG2N {
      @Override
      double map(double in) {
        return (in > 0) ? in * Math.log(in) * DIV_LOG2 : 0;
      }
    },
    QUADRATIC {
      @Override
      double map(double in) {
        return in * in;
      }
    },
    CUBIC {
      @Override
      double map(double in) {
        return in * in * in;
      }
    },
    EXP2N {
      @Override
      double map(double in) {
        return Math.pow(2, in);
      }
    };
    abstract double map(double in);
  };

  static void findFit(double[] values, double[] targets, int numvalues, double lambda, StringBuilder buf) {
    // Note: usually we don't have many degrees of freedom, so fitting with
    // fewer functions usually works better.
    ArrayList<Function> functions = new ArrayList<Function>();
    functions.add(Function.CONST);
    functions.add(Function.LOG2N);
    // functions.add(Function.LOGEN);
    // functions.add(Function.LOG10N);
    functions.add(Function.LINEAR);
    functions.add(Function.NLOG2N);
    functions.add(Function.QUADRATIC);
    // functions.add(Function.CUBIC);
    // functions.add(Function.EXP2N);

    double[] scores;
    while (true) {
      scores = tryFit(values, targets, numvalues, lambda, functions).data;
      int worst = -1;
      double worstval = Double.POSITIVE_INFINITY;
      double avg = 0;
      for (int i = 0; i < scores.length; i++) {
        avg += scores[i];
        if (scores[i] < worstval) {
          worstval = scores[i];
          worst = i;
        }
      }
      avg /= numvalues;

      if (worstval >= avg * .1 || worst < 0 || scores.length == 1 || useNNLS) {
        break;
      }
      functions.remove(worst);
    }
    boolean first = true;
    for (int i = 0; i < scores.length; i++) {
      if (scores[i] > 0 || scores[i] < 0) {
        if (!first) {
          buf.append(' ');
        }
        first = false;
        buf.append(functions.get(i)).append(": ").append(scores[i]);
      }
    }
  }

  static DenseMatrix64F tryFit(double[] values, double[] targets, int numvalues, double lambda, List<Function> functions) {
    int dof = functions.size();
    DenseMatrix64F mat = new DenseMatrix64F(numvalues, dof);
    for (int i = 0; i < numvalues; i++) {
      for (int j = 0; j < dof; j++) {
        mat.unsafe_set(i, j, functions.get(j).map(values[i]));
      }
    }
    DenseMatrix64F vec = new DenseMatrix64F(numvalues, 1);
    System.arraycopy(targets, 0, vec.data, 0, numvalues);
    if (useNNLS) {
      return NonNegativeLeastSquaresFit.nnls(mat, vec);
    } else {
      return LeastSquaresFit.tikhonovLeastSquares(mat, vec, lambda);
    }
  }

  /**
   * Get the unit of the measurements.
   * 
   * @return Unit
   */
  public String getUnit() {
    return unit;
  }

  /**
   * Get the description of the measurements, e.g. {@code "runtime"}.
   * 
   * @return Description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the number of measurements.
   * 
   * @return Number of measurements
   */
  public int getNumMeasurements() {
    return numvalues;
  }
}
