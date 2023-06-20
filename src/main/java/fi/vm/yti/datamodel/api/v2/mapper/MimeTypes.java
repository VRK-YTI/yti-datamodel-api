package fi.vm.yti.datamodel.api.v2.mapper;

import java.util.HashMap;


@SuppressWarnings("serial")
public class MimeTypes {


	public static HashMap<String, String> mimeTypeMapping;

	static {
		mimeTypeMapping = new HashMap<String, String>() {
			private void put1(String key, String value) {
				if (put(key, value) != null) {
					throw new IllegalArgumentException("Duplicated extension: " + key);
				}
			}
			{
				put1("text/csv", "csv");
				put1("text/schema+csv", "csvs");
				put1("application/xml", "xsd");
				put1("text/xml", "xsd");
				put1("application/schema+json", "schema.json");
				put1("application/schema-instance+json", "schema.json");
			}
		};
	}
	
	public static void main(String[] args) {
		System.out.println(mimeTypeMapping.size());
	}
	
	public static void registerExtension(String mimeType, String ext) {
		mimeTypeMapping.put(mimeType, ext);
	}
	
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
