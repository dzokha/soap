package vn.dzokha.soap.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

// Data ranser Object
public record BasicStatsDTO(
    @JsonProperty("filename") String filename,
    @JsonProperty("fileType") String fileType,
    @JsonProperty("encoding") String encoding,
    @JsonProperty("totalSequences") long totalSequences,
    @JsonProperty("sequencesFlagged") long sequencesFlagged,
    @JsonProperty("gcContent") double gcContent
) {
    // Thêm Constructor này để sửa các lỗi "actual and formal argument lists differ in length"
    public BasicStatsDTO(String filename, String fileType) {
        this(filename, fileType, "Unknown", 0L, 0L, 0.0);
    }
}