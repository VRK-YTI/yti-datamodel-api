package fi.vm.yti.datamodel.api.v2.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class DataModelUtils {

    private static final Logger logger = LoggerFactory.getLogger(DataModelUtils.class);

    public static String encode(String param) {
        try {
            return URLEncoder.encode(param, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
