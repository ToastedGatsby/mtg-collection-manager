package com.jacob.mtg.card.scryfall;

import java.util.List;

/**
 * Entry of GET /bulk-data: where today's file is and when it was generated.
 *
 * Since July 2026 Scryfall publishes bulk files only as gzipped JSON Lines (one card per line) at
 * jsonl_download_uri, with compressed_size; the old download_uri/size fields are gone. downloadUri is
 * kept as a fallback in case an older-style entry ever shows up.
 */
public record BulkDataInfo(
    String type,
    String updatedAt,
    String jsonlDownloadUri,
    String downloadUri,
    Long compressedSize) {

    public String fileUri() {
        return jsonlDownloadUri != null ? jsonlDownloadUri : downloadUri;
    }

    /** GET /bulk-data returns { "object": "list", "data": [ ... ] }. */
    public record BulkDataList(List<BulkDataInfo> data) {
    }
}
