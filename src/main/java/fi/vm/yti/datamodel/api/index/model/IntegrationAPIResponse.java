package fi.vm.yti.datamodel.api.index.model;

import java.util.ArrayList;
import java.util.List;

public class IntegrationAPIResponse {

    private IntegrationMetaDTO meta;
    private List results;

    private String buildNextUri(String path, String query, Integer pageSize, Integer from, String lang, String status) {
        String returnPath = path;
        returnPath+="?pageSize="+pageSize;
        returnPath+="&from="+(from+pageSize);
        if(status!=null && !status.isEmpty()) {
            returnPath+="&status="+status;
        }
        if(query!=null && !query.isEmpty()) {
            returnPath+="&searchTerm="+query;
        }
        if(lang!=null && !lang.isEmpty()) {
            returnPath+="&language="+lang;
        }
        return returnPath;
    }

    public IntegrationAPIResponse(final ModelSearchResponse modelResponse, final ModelSearchRequest modelRequest, String path) {

        this.meta = new IntegrationMetaDTO(modelResponse.getPageSize(),modelResponse.getPageFrom(), Integer.valueOf(Math.toIntExact(modelResponse.getTotalHitCount())));
        List<IndexModelDTO> models = modelResponse.getModels();
        results = new ArrayList<>();
        models.forEach(m->{
            results.add(new IntegrationContainerDTO(m));
        });
        this.meta.setResultCount(results.size());
        if(path!=null && !path.isEmpty() && this.meta.getFrom()!=null && this.meta.getPageSize()!=null &&  (this.meta.getFrom() + this.meta.getPageSize() < this.meta.getTotalResults())) {
            this.meta.setNextPage(buildNextUri(path,modelRequest.getQuery(),modelResponse.getPageSize(), modelResponse.getPageFrom(), modelRequest.getSortLang(), modelRequest.getStatus()));
        }
    }

    public IntegrationAPIResponse(final ResourceSearchResponse resourceResponse, final ResourceSearchRequest resourceRequest, String path) {

        this.meta = new IntegrationMetaDTO(resourceResponse.getPageSize(),resourceResponse.getPageFrom(), Integer.valueOf(Math.toIntExact(resourceResponse.getTotalHitCount())));
        List<IndexResourceDTO> models = resourceResponse.getResources();
        results = new ArrayList<>();
        models.forEach(m->{
            results.add(new IntegrationResourceDTO(m));
        });
        this.meta.setResultCount(results.size());
        if(path!=null && !path.isEmpty() && this.meta.getFrom()!=null && this.meta.getPageSize()!=null &&  (this.meta.getFrom() + this.meta.getPageSize() < this.meta.getTotalResults())) {
            this.meta.setNextPage(buildNextUri(path,resourceRequest.getQuery(),resourceResponse.getPageSize(), resourceResponse.getPageFrom(), resourceRequest.getSortLang(), resourceRequest.getStatus()));
        }
    }

    public IntegrationAPIResponse(final IntegrationMetaDTO meta,
                                  final List<IntegrationResourceDTO> results) {
        this.meta = meta;
        this.results = results;
    }

    public IntegrationMetaDTO getMeta() {
        return meta;
    }

    public void setMeta(final IntegrationMetaDTO meta) {
        this.meta = meta;
    }

    public List<IntegrationResourceDTO> getResults() {
        return results;
    }

    public void setResults(final List<IntegrationResourceDTO> results) {
        this.results = results;
    }
}
