package com.googlecode.caliperanalyze;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.caliper.model.Scenario;
import com.google.caliper.model.Trial;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.gson.JsonParseException;
import com.googlecode.caliperanalyze.util.CaliperConfigurationAdapter;
import com.googlecode.caliperanalyze.util.FileUtil;

/**
 * TODO: Add logging
 *
 * @author Erich Schubert
 */
public class SimpleReporter {
  /**
   * Constructor.
   */
  public SimpleReporter() {
    super();
  }

  private void run(String[] args) {
    File[] files = getFilenames(args);
    ArrayList<Trial> trials = readFiles(files);

    // Build a multimap of all benchmark parameters:
    final SetMultimap<String, String> spec = HashMultimap.create();
    for (Iterator<Trial> iter = trials.iterator(); iter.hasNext();) {
      Trial t = iter.next();
      // TODO: also use Host and VM parameters,
      // in case someone is benchmarking VMs!
      // Any of these could be null on incomplete trials:
      try {
        spec.get("BenchmarkMethod").add(t.scenario().benchmarkSpec().methodName());
        spec.get("BenchmarkClass").add(t.scenario().benchmarkSpec().className());
        for (Map.Entry<String, String> entry : t.scenario().benchmarkSpec().parameters().entrySet()) {
          spec.get(entry.getKey()).add(entry.getValue());
        }
      } catch (NullPointerException e) {
        // We are indeed expecting this to happen sometimes.
        iter.remove(); // Remove, so this doesn't happen again below.
        continue;
      }
    }
    // Find variates:
    Set<String> keys = spec.keySet();
    ArrayList<String> variates = new ArrayList<String>(keys.size());
    Set<String> nonnumeric = new HashSet<String>(keys.size());
    for (String key : keys) {
      Set<String> values = spec.get(key);
      if (values.size() > 1) {
        variates.add(key);
        for (String v : values) {
          try {
            Double.parseDouble(v);
          } catch (NumberFormatException e) {
            nonnumeric.add(key);
            break;
          }
        }
      }
    }
    // TODO: command line parameters for sorting.
    sortHeurstically(spec, variates);
    recursivelySummarize(trials, variates, spec, new ArrayList<String>());

    // Perform estimations:
    for (String v : variates) {
      if (nonnumeric.contains(v)) {
        continue;
      }
      // Skip trend estimation for small number of samples for now
      // Until we have a better rule to estimate when it is
      // statistically sound to estimate a trend.
      if (spec.get(v).size() < 8) {
        continue;
      }
      System.out.println("Predicting trend for " + v);
      // Move target variate last:
      ArrayList<String> modv = new ArrayList<>(variates);
      modv.remove(v); // remove anywhere
      modv.add(v); // add to end
      predictTrend(trials, modv, spec, new ArrayList<String>());
    }
  }

  private void sortHeurstically(final SetMultimap<String, String> spec, ArrayList<String> variates) {
    // TODO: heuristics for sorting. Number of entries?
    Collections.sort(variates, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        int num1 = spec.get(o1).size();
        int num2 = spec.get(o2).size();
        if (num1 != num2) {
          return Integer.compare(num1, num2);
        }
        if (Character.isDigit(o1.charAt(o1.length() - 1))) {
          if (!Character.isDigit(o2.charAt(o2.length() - 1))) {
            return -1;
          }
          return Integer.compare(o1.length(), o2.length());
        }
        return 0;
      }
    });
  }

  /**
   * Read files into the array of trials.
   * 
   * @param files Files to read.
   * @return Trials
   */
  private ArrayList<Trial> readFiles(File[] files) {
    CaliperResultsReader reader = new CaliperResultsReader();
    ArrayList<Trial> trials = new ArrayList<Trial>();
    for (File file : files) {
      try {
        reader.readTrialsFromJSON(file, trials);
      } catch (JsonParseException e) {
        if (e.getCause() instanceof EOFException || e.getMessage().contains("Unterminated string")) {
          // Pass - probably an incomplete run.
          System.err.println("Note: truncated file: " + file);
        } else {
          e.printStackTrace();
          System.exit(1);
        }
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
    return trials;
  }

  /**
   * Get filenames from parameter array, or use the last file.
   * 
   * @param args Command line parameters
   * @return Files
   */
  private File[] getFilenames(String[] args) {
    File[] files;
    // TODO: allow the -c flag for caliper configuration files.
    if (args.length == 0) {
      File resultdir = new CaliperConfigurationAdapter(null).getCaliperResultDir();
      files = new File[1];
      try {
        files[0] = FileUtil.findLatestFile(resultdir);
        System.out.println("Loading latest results file: " + files[0]);
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
    } else {
      files = new File[args.length];
      for (int i = 0; i < args.length; i++) {
        files[i] = new File(args[i]);
      }
    }
    return files;
  }

  /**
   * Recursively summarize the results.
   * 
   * @param trials Trials to process
   * @param variates Variate names
   * @param spec Parameter map
   * @param selected Current selection
   */
  private void recursivelySummarize(Collection<Trial> trials, List<String> variates, SetMultimap<String, String> spec, List<String> selected) {
    final int depth = selected.size();
    String curkey = variates.get(depth);
    List<String> values = new ArrayList<String>(spec.get(curkey));
    if (depth + 1 < variates.size()) {
      Collections.sort(values); // Alphabetic.
      for (String value : values) {
        selected.add(value);
        recursivelySummarize(trials, variates, spec, selected);
        selected.remove(depth);
      }
      return;
    }
    // Initialize:
    final Map<String, AggregateMeasurements> aggs = new HashMap<String, AggregateMeasurements>();
    for (String val : values) {
      aggs.put(val, new AggregateMeasurements());
    }
    next: for (Trial t : trials) {
      for (int i = 0; i < depth; i++) {
        if (!selected.get(i).equals(getScenarioParameter(t.scenario(), variates.get(i)))) {
          continue next;
        }
      }
      aggs.get(getScenarioParameter(t.scenario(), curkey)).add(t.measurements());
    }
    Collections.sort(values, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        return Double.compare(aggs.get(o1).getMean(), aggs.get(o2).getMean());
      }
    });
    for (String val : values) {
      AggregateMeasurements agg = aggs.get(val);
      if (agg.getWeight() > 0) {
        for (String k : selected) {
          System.out.print(k);
          System.out.print(" ");
        }
        System.out.print(val);
        System.out.print(" ");
        System.out.println(agg);
      }
    }
  }

  /**
   * Recursively summarize the results.
   * 
   * @param trials Trials to process
   * @param variates Variate names
   * @param spec Parameter map
   * @param selected Current selection
   */
  private void predictTrend(Collection<Trial> trials, List<String> variates, SetMultimap<String, String> spec, List<String> selected) {
    final int depth = selected.size();
    String curkey = variates.get(depth);
    List<String> values = new ArrayList<String>(spec.get(curkey));
    if (depth + 1 < variates.size()) {
      Collections.sort(values); // Alphabetic.
      for (String value : values) {
        selected.add(value);
        predictTrend(trials, variates, spec, selected);
        selected.remove(depth);
      }
      return;
    }
    // Initialize:
    TrendPredictor trend = new TrendPredictor();
    next: for (Trial t : trials) {
      for (int i = 0; i < depth; i++) {
        if (!selected.get(i).equals(getScenarioParameter(t.scenario(), variates.get(i)))) {
          continue next;
        }
      }
      final String val = getScenarioParameter(t.scenario(), curkey);
      trend.add(t.measurements(), Double.valueOf(val));
    }
    if (trend.getNumMeasurements() > 0) {
      for (String k : selected) {
        System.out.print(k);
        System.out.print(" ");
      }
      System.out.println(trend);
    }
  }

  private String getScenarioParameter(Scenario scenario, String key) {
    String val = scenario.benchmarkSpec().parameters().get(key);
    if (val != null) {
      return val;
    }
    if ("BenchmarkMethod".equals(key)) {
      return scenario.benchmarkSpec().methodName();
    }
    if ("BenchmarkCkass".equals(key)) {
      return scenario.benchmarkSpec().className();
    }
    return null;
  }

  public static void main(String[] args) {
    (new SimpleReporter()).run(args);
  }
}
