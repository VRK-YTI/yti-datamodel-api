package fi.vm.yti.datamodel.api.index.model;

import java.util.List;

public class DeepSearchResourceHitListDTO extends DeepSearchHitListDTO<IndexResourceDTO> {

    public DeepSearchResourceHitListDTO(long totalCount,
                                        List<IndexResourceDTO> topHits) {
        super(totalCount, topHits);
    }

}