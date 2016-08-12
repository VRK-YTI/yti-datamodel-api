/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadExecutor {
   public static final ExecutorService pool = Executors.newCachedThreadPool();
   private static final ThreadExecutor instance = new ThreadExecutor();

   private ThreadExecutor() {
   }

   public static ThreadExecutor getInstance() {
       return instance;
   }        
}