package com.jacob.mtg.card.scryfall;

import java.util.List;

/** Entry of GET /bulk-data: where today's file is and when it was generated. */
public record BulkDataInfo(String type, String updatedAt, String downloadUri, String contentEncoding, Long size) {

    /** GET /bulk-data returns { "object": "list", "data": [ ... ] }. */
    public record BulkDataList(List<BulkDataInfo> data) {
    }
}
