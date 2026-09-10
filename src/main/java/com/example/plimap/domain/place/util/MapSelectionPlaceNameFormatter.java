package com.example.plimap.domain.place.util;

import java.util.Set;

public final class MapSelectionPlaceNameFormatter {

    private static final String COUNTRY_NAME = "대한민국";
    private static final Set<String> SIDO_NAMES = Set.of(
            "서울특별시", "부산광역시", "대구광역시", "인천광역시",
            "광주광역시", "대전광역시", "울산광역시", "세종특별자치시",
            "경기도", "강원도", "강원특별자치도", "충청북도", "충청남도",
            "전라북도", "전북특별자치도", "전라남도", "경상북도", "경상남도",
            "제주특별자치도", "서울", "부산", "대구", "인천", "광주", "대전",
            "울산", "세종", "경기", "강원", "충북", "충남", "전북", "전남",
            "경북", "경남", "제주"
    );

    private MapSelectionPlaceNameFormatter() {
    }

    public static String format(String address, String roadAddress, String sido) {
        String source = hasText(roadAddress) ? roadAddress : address;
        String normalized = normalizeWhitespace(source);
        String withoutCountry = removeLeadingToken(normalized, COUNTRY_NAME);
        String firstToken = firstToken(withoutCountry);

        if (shouldRemoveSido(firstToken, sido)) {
            return removeLeadingToken(withoutCountry, firstToken);
        }
        return withoutCountry;
    }

    private static boolean shouldRemoveSido(String firstToken, String sido) {
        return firstToken != null
                && (firstToken.equals(normalizeWhitespace(sido)) || SIDO_NAMES.contains(firstToken));
    }

    private static String removeLeadingToken(String value, String token) {
        if (!hasText(value) || !hasText(token)) {
            return value;
        }
        if (value.equals(token)) {
            return value;
        }
        String prefix = token + " ";
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static String firstToken(String value) {
        if (!hasText(value)) {
            return null;
        }
        int separatorIndex = value.indexOf(' ');
        return separatorIndex < 0 ? value : value.substring(0, separatorIndex);
    }

    private static String normalizeWhitespace(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.strip().replaceAll("\\s+", " ");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
