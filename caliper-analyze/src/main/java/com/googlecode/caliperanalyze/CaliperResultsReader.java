package com.googlecode.caliperanalyze;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import com.google.caliper.json.GsonModule;
import com.google.caliper.model.Trial;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Class to load results from Calipers output directory.
 * 
 * @author Erich Schubert
 */
public class CaliperResultsReader {
  /**
   * Caliper Gson object.
   */
  Gson gson;

  /**
   * Constructor.
   */
  public CaliperResultsReader() {
    super();
    // Load Calipers Gson module:
    Injector injector = Guice.createInjector(new GsonModule());

    // The modified gson object
    gson = injector.getInstance(Gson.class);
  }

  /**
   * Read trials from JSON file into array list.
   * 
   * Note: for incomplete files, all complete trials should be read.
   * 
   * @param file File to read
   * @param output output collection
   * @throws IOException On I/O errors (e.g. incomplete files!)
   */
  public void readTrialsFromJSON(File file, Collection<Trial> output) throws IOException {
    InputStream in = new FileInputStream(file);
    JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
    reader.setLenient(true);
    reader.beginArray();
    while(reader.hasNext()) {
      output.add(gson.<Trial> fromJson(reader, Trial.class));
    }
    reader.endArray();
    reader.close();
  }
}
