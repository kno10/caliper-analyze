package com.googlecode.caliperanalyze;

import com.google.caliper.model.Measurement;

/**
 * Aggregate caliper measureents
 * 
 * @author Erich Schubert
 */
public class AggregateMeasurements {
  /** Minimum and maximum */
  double min = Double.MAX_VALUE, max = Double.MIN_VALUE;

  /** Simple statistics (for online algorithm) */
  double mean = 0.0, sqdev = 0.0, weights = 0.0, sqweights = 0.0;

  /** Metadata */
  String unit = null, description = null;

  /**
   * Add a series of measurements.
   * 
   * @param measurements Measurements to add.
   * @return {@code this}
   */
  public AggregateMeasurements add(Iterable<Measurement> measurements) {
    for(Measurement m : measurements) {
      add(m);
    }
    return this;
  }

  /**
   * Add a single measurement to the aggregate.
   * 
   * @param measurement Measurement to add.
   * @return {@code this}
   */
  public AggregateMeasurements add(Measurement measurement) {
    // Sanity check for units
    if(unit == null) {
      unit = measurement.value().unit();
    }
    else if(!unit.equals(measurement.value().unit())) {
      throw new RuntimeException("Inconsistent units are not supported.");
    }
    // Sanity check for descriptions
    if(description == null) {
      description = measurement.description();
    }
    else if(!description.equals(measurement.description())) {
      throw new RuntimeException("Multiple types of measuresments are not supported.");
    }
    double weight = measurement.weight();
    double val = measurement.value().magnitude() / weight;
    if(!Double.isNaN(val)) {
      min = (min < val) ? min : val;
      max = (max > val) ? max : val;
      double delta = val - mean;
      weights += weight;
      sqweights += weight * weight;
      mean += delta * (weight / weights);
      // Online update squared deviations:
      sqdev += delta * (val - mean) * (weight / weights);
    }
    return this;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(description).append("[").append(unit).append("]: ");
    buf.append(String.format("mean: %.2f", mean));
    if(weights > 1) {
      buf.append(String.format(" +- %.2f", getStandardDeviation()));
      buf.append(String.format(" (%.2f%%)", 100. * getStandardDeviation() / mean));
    }
    buf.append(String.format(" min: %.2f max: %.2f weight: %.0f", min, max, weights));
    return buf.toString();
  }

  /**
   * Get the observed minimum.
   * 
   * @return Minimum
   */
  public double getMin() {
    return min;
  }

  /**
   * Get the observed maximum.
   * 
   * @return Maximum
   */
  public double getMax() {
    return max;
  }

  /**
   * Get the observed mean.
   * 
   * @return Mean
   */
  public double getMean() {
    return mean;
  }

  /**
   * Compute the sample standard deviation, with Bessel's correction.
   * 
   * @return Standard deviation
   */
  public double getStandardDeviation() {
    // FIXME: is this numerically stable & accurate?
    // FIXME: can we do the same update trick, and compute w*w-sqw directly?
    return Math.sqrt(sqdev * weights / (weights * weights - sqweights));
  }

  /**
   * Get the total weight of all measurements.
   * 
   * @return Weight sum
   */
  public double getWeight() {
    return weights;
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
}
