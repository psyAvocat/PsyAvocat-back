package com.psyavocat.service.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumbersTest {

    @Test
    @DisplayName("Numéros valides : indicatif facultatif, séparateurs ignorés, forme normalisée")
    void numerosValides() {
        assertThat(PhoneNumbers.normalize("+223 76 12 34 56")).isEqualTo("+22376123456");
        assertThat(PhoneNumbers.normalize("76-12-34-56")).isEqualTo("76123456");
        assertThat(PhoneNumbers.normalize("(+33) 6.12.34.56.78")).isEqualTo("+33612345678");
    }

    @Test
    @DisplayName("Numéros invalides : trop courts, trop longs, lettres, séparateurs seuls")
    void numerosInvalides() {
        assertThat(PhoneNumbers.normalize("12")).isNull();
        assertThat(PhoneNumbers.normalize("1234567")).isNull();
        assertThat(PhoneNumbers.normalize("1234567890123456")).isNull();
        assertThat(PhoneNumbers.normalize("76 AB 34 56")).isNull();
        assertThat(PhoneNumbers.normalize("--------")).isNull();
        assertThat(PhoneNumbers.normalize(null)).isNull();
    }
}
