package com.clara.libs.fn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class UncheckedTest {

    private static String parse(String input) throws IOException {
        if (input.isBlank()) {
            throw new IOException("blank input");
        }
        return input.trim();
    }

    @Test
    void function_success_passesValueThrough() {
        var result = Stream.of(" a ", " b ")
                .map(Unchecked.function(UncheckedTest::parse))
                .toList();

        assertThat(result).containsExactly("a", "b");
    }

    @Test
    void function_checkedException_surfacesAsUncheckedWithCause() {
        var fn = Unchecked.function(UncheckedTest::parse);

        assertThatThrownBy(() -> fn.apply(" "))
                .isInstanceOf(UncheckedException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void supplier_and_runnable_wrapEquivalently() {
        assertThat(Unchecked.supplier(() -> parse("x")).get()).isEqualTo("x");
        assertThatThrownBy(Unchecked.runnable(() -> parse(" "))::run).isInstanceOf(UncheckedException.class);
    }
}
