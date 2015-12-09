/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

final public class ErrorMessage {
    
    final public static String UNAUTHORIZED = toJs("{'errorMessage':'Unauthorized'}");
    final public static String INVALIDIRI = toJs("{'errorMessage':'Invalid ID'}");
    final public static String USEDIRI = toJs("{'errorMessage':'ID is already in use}");
    final public static String UNEXPECTED = toJs("{'errorMessage':'Unexpected error'}");
    final public static String NOTFOUND = toJs("{'errorMessage':'Not found'}");
    final public static String NOTREMOVED = toJs("{'errorMessage':'Not removed'}");
 
    private static String toJs(String jsonString) {
        return jsonString.replaceAll("'", "\"");
    } 
    
}
