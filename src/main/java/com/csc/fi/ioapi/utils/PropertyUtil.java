/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
 
public class PropertyUtil
{
 
  private PropertyUtil(){}
   
  private static Properties properties;
 
  static
  {
    properties = new Properties();
    
    try
    {
      PropertyUtil util = new PropertyUtil();
      properties = util.getPropertiesFromClasspath("app.properties");
    }
    
    catch (FileNotFoundException e)
     {
       e.printStackTrace();
     }
    
    catch (IOException e)
     {
       e.printStackTrace();
     }
  }
 
  public static String getProperty(String key)
   {
     return properties.getProperty(key);
   }
 
  public static Properties getProperties()
  {
    return properties;
  }
 
  private Properties getPropertiesFromClasspath(String filename) throws IOException
  {
    Properties properties = new Properties();
   
    InputStream inputStream = null;

      inputStream = this.getClass().getClassLoader().getResourceAsStream(filename);
 
      if (inputStream == null)
      {
        throw new FileNotFoundException("File '" + filename+ "' not found");
      } else {
        properties.load(inputStream);
        inputStream.close();
        return properties;
    }
  }
}