package fi.vm.yti.datamodel.api.index.model;

import java.util.List;

public class DeepSearchResourceHitListDTO extends DeepSearchHitListDTO<IndexResourceDTO> {

    public DeepSearchResourceHitListDTO(String type,
                                        long totalCount,
                                        List<IndexResourceDTO> topHits) {
        super(type, totalCount, topHits);
    }

}
