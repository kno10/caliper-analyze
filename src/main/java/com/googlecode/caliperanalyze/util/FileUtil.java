package com.googlecode.caliperanalyze.util;

import java.io.File;
import java.io.IOException;

/**
 * File utilities.
 * 
 * @author Erich Schubert
 */
public class FileUtil {
  /**
   * Find the latest file in a given directory.
   * 
   * @param dir Directory to scan.
   * @return Latest file
   * @throws IOException When no matching file was found.
   */
  public static File findLatestFile(File dir) throws IOException {
    long lastLastModified = 0;
    File lastFile = null;
    for(File file : dir.listFiles()) {
      // Ensure it is at least supposedly a Caliper file:
      if(file.isDirectory()) {
        continue;
      }
      String name = file.getName();
      if(!(name.endsWith(".json") || name.endsWith(".json.tmp"))) {
        continue;
      }
      long modified = file.lastModified();
      if(modified > lastLastModified) {
        lastLastModified = modified;
        lastFile = file;
      }
    }
    if(lastFile == null) {
      throw new IOException("No caliper files were found.");
    }
    return lastFile;
  }
}
