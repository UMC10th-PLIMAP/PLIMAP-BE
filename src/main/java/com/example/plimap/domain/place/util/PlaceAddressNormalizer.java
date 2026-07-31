package com.example.plimap.domain.place.util;

public final class PlaceAddressNormalizer {

    private PlaceAddressNormalizer() {
    }

    public static String normalize(String address) {
        if (address == null) {
            return null;
        }
        return address.codePoints()
                .filter(codePoint ->
                        !Character.isWhitespace(codePoint) && !Character.isSpaceChar(codePoint))
                .collect(
                        StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append
                )
                .toString();
    }
}
