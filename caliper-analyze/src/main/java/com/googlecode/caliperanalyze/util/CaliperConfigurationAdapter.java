package com.googlecode.caliperanalyze.util;

import java.io.File;

import com.google.caliper.options.CaliperOptions;
import com.google.caliper.options.OptionsModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Class that partially instantiates Google Caliper, in order to detect the
 * folder that it stores its data in.
 * 
 * However, this is only partially implemented as of now. The -c
 * 
 * @author Erich Schubert
 */
public class CaliperConfigurationAdapter {
  private File resultdir;

  public CaliperConfigurationAdapter(String configdir) {
    // Setup fake parameters for caliper:
    String[] params;
    if(configdir == null) {
      params = new String[] { "" };
    }
    else {
      params = new String[] { "-c", configdir, "" };
    }
    // Hack to access caliper options.
    Injector optionsInjector = Guice.createInjector(new OptionsModule(params));
    CaliperOptions options = optionsInjector.getInstance(CaliperOptions.class);

    // I couldn't find a way to get the caliper output directory.
    resultdir = new File(options.caliperDirectory(), "results");
  }

  /**
   * Get the directory that caliper stores its results in.
   * 
   * @return Caliper result directory.
   */
  public File getCaliperResultDir() {
    return resultdir;
  }
}
