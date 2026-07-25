package com.example.plimap.domain.track.controller.docs;

final class TrackSwaggerErrorExamples {

    static final String SEARCH_VALIDATION_FAILED = """
            {
              "isSuccess": false,
              "code": "COMMON_400_VALIDATION_FAILED",
              "message": "검색어를 입력해주세요.",
              "result": null
            }
            """;

    static final String PLAYBACK_VALIDATION_FAILED = """
            {
              "isSuccess": false,
              "code": "COMMON_400_VALIDATION_FAILED",
              "message": "iTunes 트랙 ID는 양수여야 합니다.",
              "result": null
            }
            """;

    static final String PLACE_TRACK_VALIDATION_FAILED = """
            {
              "isSuccess": false,
              "code": "COMMON_400_VALIDATION_FAILED",
              "message": "페이지 번호는 0 이상이어야 합니다.",
              "result": null
            }
            """;

    static final String PLACE_TRACK_DETAIL_TYPE_MISMATCH = """
            {
              "isSuccess": false,
              "code": "COMMON_400_TYPE_MISMATCH",
              "message": "요청 값의 타입이 올바르지 않습니다.",
              "result": null
            }
            """;

    static final String UNAUTHORIZED = """
            {
              "isSuccess": false,
              "code": "COMMON_401_UNAUTHORIZED",
              "message": "인증이 필요합니다.",
              "result": null
            }
            """;

    static final String TRACK_EXTERNAL_API_ERROR = """
            {
              "isSuccess": false,
              "code": "TRACK_EXTERNAL_API_ERROR",
              "message": "음악 검색 중 오류가 발생했습니다.",
              "result": null
            }
            """;

    static final String TRACK_METADATA_CACHE_NOT_FOUND = """
            {
              "isSuccess": false,
              "code": "TRACK_404_METADATA_CACHE_NOT_FOUND",
              "message": "곡 메타데이터가 만료되었습니다. 곡을 다시 검색해 주세요.",
              "result": null
            }
            """;

    static final String YOUTUBE_MATCH_NOT_FOUND = """
            {
              "isSuccess": false,
              "code": "TRACK_404_YOUTUBE_MATCH_NOT_FOUND",
              "message": "선택한 곡과 일치하는 YouTube 영상을 찾을 수 없습니다.",
              "result": null
            }
            """;

    static final String YOUTUBE_EXTERNAL_API_ERROR = """
            {
              "isSuccess": false,
              "code": "TRACK_500_YOUTUBE_EXTERNAL_API_ERROR",
              "message": "YouTube 영상 검색 중 오류가 발생했습니다.",
              "result": null
            }
            """;

    static final String TRACK_CACHE_ERROR = """
            {
              "isSuccess": false,
              "code": "TRACK_500_CACHE_ERROR",
              "message": "곡 캐시 처리 중 오류가 발생했습니다.",
              "result": null
            }
            """;

    static final String PLACE_NOT_FOUND = """
            {
              "isSuccess": false,
              "code": "PLACE_NOT_FOUND",
              "message": "장소를 찾을 수 없습니다.",
              "result": null
            }
            """;

    static final String PLACE_TRACK_NOT_FOUND = """
            {
              "isSuccess": false,
              "code": "PLACE_TRACK_NOT_FOUND",
              "message": "존재하지 않는 placeTrack 입니다.",
              "result": null
            }
            """;

    private TrackSwaggerErrorExamples() {
    }
}
