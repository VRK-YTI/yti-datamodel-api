/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import java.text.SimpleDateFormat;

/**
 *
 * @author malonen
 */
public class SafeDateFormat {
    
  public static SimpleDateFormat fmt() {
      return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
  }
    
}
