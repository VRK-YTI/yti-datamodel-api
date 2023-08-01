package fi.vm.yti.datamodel.api.v2.mapper;

import java.util.*;


public class MimeTypes {

	
	private static HashMap<String, String> mimeTypes;
	
	static {	
		mimeTypes = new HashMap<String, String>();	
		mimeTypes.put("text/csv", "csv");
		mimeTypes.put("text/schema+csv", "csvs");
		mimeTypes.put("application/xml", "xsd");
		mimeTypes.put("text/xml", "xsd");
		mimeTypes.put("application/schema+json", "schema.json");
		mimeTypes.put("application/schema-instance+json", "schema.json");
	}
		
	public static Map<String, String> mimeTypeMapping = Collections.unmodifiableMap(mimeTypes);
	
	public static String getExtension(String mime) {
		String ext = lookupExtension(mime);
	    if (ext == null) {
	    	return "";
	    }
	    return "." + ext;
	}

	public static String lookupExtension(String mime) {
		return mimeTypeMapping.get(mime.toLowerCase());
	}
	

}
