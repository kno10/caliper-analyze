package com.googlecode.caliperanalyze;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.caliper.model.Trial;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.gson.JsonSyntaxException;
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
    SetMultimap<String, String> spec = HashMultimap.create();
    for(Trial t : trials) {
      // TODO: also use Host and VM parameters,
      // in case someone is benchmarking VMs!
      for(Map.Entry<String, String> entry : t.scenario().benchmarkSpec().parameters().entrySet()) {
        spec.get(entry.getKey()).add(entry.getValue());
      }
    }
    // Find variates:
    Set<String> keys = spec.keySet();
    ArrayList<String> variates = new ArrayList<String>(keys.size());
    for(String key : keys) {
      if(spec.get(key).size() > 1) {
        variates.add(key);
      }
    }
    // TODO: heuristics for sorting. Number of entries?
    recursivelySummarize(trials, variates, new ArrayList<String>());
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
    for(File file : files) {
      try {
        reader.readTrialsFromJSON(file, trials);
      }
      catch(JsonSyntaxException e) {
        if(e.getCause() instanceof EOFException) {
          // Pass - probably an incomplete run.
          System.err.println("Note: truncated file: " + file);
        }
        else {
          e.printStackTrace();
          System.exit(1);
        }
      }
      catch(IOException e) {
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
    if(args.length == 0) {
      File resultdir = new CaliperConfigurationAdapter(null).getCaliperResultDir();
      files = new File[1];
      try {
        files[0] = FileUtil.findLatestFile(resultdir);
        System.out.println("Loading latest results file: " + files[0]);
      }
      catch(IOException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
    else {
      files = new File[args.length];
      for(int i = 0; i < args.length; i++) {
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
   * @param selected Current selection
   */
  private void recursivelySummarize(Collection<Trial> trials, List<String> variates, List<String> selected) {
    if(selected.size() == variates.size()) {
      AggregateMeasurements agg = new AggregateMeasurements();
      next: for(Trial t : trials) {
        Map<String, String> pars = t.scenario().benchmarkSpec().parameters();
        for(int i = 0; i < variates.size(); i++) {
          if(!selected.get(i).equals(pars.get(variates.get(i)))) {
            continue next;
          }
        }
        agg.add(t.measurements());
      }
      if(agg.getWeight() > 0) {
        for(String k : selected) {
          System.out.print(k);
          System.out.print(" ");
        }
        System.out.println(agg);
      }
    }
    else {
      String nextkey = variates.get(selected.size());
      Set<String> values = new HashSet<String>();
      for(Trial t : trials) {
        Map<String, String> pars = t.scenario().benchmarkSpec().parameters();
        values.add(pars.get(nextkey));
      }
      for(String value : values) {
        selected.add(value);
        recursivelySummarize(trials, variates, selected);
        selected.remove(selected.size() - 1);
      }
    }
  }

  public static void main(String[] args) {
    (new SimpleReporter()).run(args);
  }
}
