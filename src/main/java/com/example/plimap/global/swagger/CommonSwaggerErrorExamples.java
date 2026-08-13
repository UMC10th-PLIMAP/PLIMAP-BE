package com.example.plimap.global.swagger;

public final class CommonSwaggerErrorExamples {

    public static final String JSON_PREFIX = "{\r\n"
            + "  \"isSuccess\": false,\r\n"
            + "  \"code\": \"";

    public static final String JSON_MESSAGE_SEPARATOR = "\",\r\n"
            + "  \"message\": \"";

    public static final String JSON_SUFFIX = "\",\r\n"
            + "  \"result\": null\r\n"
            + "}";

    public static final String VALIDATION_FAILED = """
            {
              "isSuccess": false,
              "code": "COMMON_400_VALIDATION_FAILED",
              "message": "요청 값이 올바르지 않습니다.",
              "result": null
            }
            """;

    public static final String MISSING_PARAMETER = """
            {
              "isSuccess": false,
              "code": "COMMON_400_MISSING_PARAMETER",
              "message": "필수 요청 파라미터가 누락되었습니다.",
              "result": null
            }
            """;

    public static final String MISSING_HEADER = """
            {
              "isSuccess": false,
              "code": "COMMON_400_MISSING_HEADER",
              "message": "필수 요청 헤더가 누락되었습니다.",
              "result": null
            }
            """;

    public static final String TYPE_MISMATCH = """
            {
              "isSuccess": false,
              "code": "COMMON_400_TYPE_MISMATCH",
              "message": "요청 값의 타입이 올바르지 않습니다.",
              "result": null
            }
            """;

    public static final String MALFORMED_JSON = """
            {
              "isSuccess": false,
              "code": "COMMON_400_MALFORMED_JSON",
              "message": "요청 본문의 형식이 올바르지 않습니다.",
              "result": null
            }
            """;

    public static final String INVALID_CURSOR = """
            {
              "isSuccess": false,
              "code": "COMMON_400_INVALID_CURSOR",
              "message": "유효하지 않은 커서입니다.",
              "result": null
            }
            """;

    public static final String CONTENT_TOO_LARGE = """
            {
              "isSuccess": false,
              "code": "COMMON_413_CONTENT_TOO_LARGE",
              "message": "요청 크기가 허용된 한도를 초과했습니다.",
              "result": null
            }
            """;

    public static final String UNAUTHORIZED = """
            {
              "isSuccess": false,
              "code": "COMMON_401_UNAUTHORIZED",
              "message": "인증이 필요합니다.",
              "result": null
            }
            """;

    public static final String FORBIDDEN = """
            {
              "isSuccess": false,
              "code": "COMMON_403_FORBIDDEN",
              "message": "접근 권한이 없습니다.",
              "result": null
            }
            """;

    public static final String INTERNAL_SERVER_ERROR = """
            {
              "isSuccess": false,
              "code": "COMMON_500_INTERNAL_SERVER_ERROR",
              "message": "서버 내부 오류가 발생했습니다.",
              "result": null
            }
            """;

    private CommonSwaggerErrorExamples() {
    }
}
