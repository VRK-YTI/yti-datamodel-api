package fi.vm.yti.datamodel.api.v2.opensearch.queries;


public class QueryFactoryUtils {

    private QueryFactoryUtils(){
        //Utility class
    }

    public static final int DEFAULT_PAGE_FROM = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final String DEFAULT_SORT_LANG = "fi";

    public static int pageFrom(Integer pageFrom){
        if(pageFrom == null || pageFrom <= 0){
            return DEFAULT_PAGE_FROM;
        }else{
            return  pageFrom;
        }
    }

    public static int pageSize(Integer pageSize) {
        if(pageSize == null || pageSize <= 0){
            return DEFAULT_PAGE_SIZE;
        }else{
            return pageSize;
        }
    }

}
