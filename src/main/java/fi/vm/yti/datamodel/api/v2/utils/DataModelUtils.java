package fi.vm.yti.datamodel.api.v2.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class DataModelUtils {

    private DataModelUtils(){
        //Util class
    }

    public static String encode(String param) {
        return URLEncoder.encode(param, StandardCharsets.UTF_8);
    }
}
