package com.kgi.shredder.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentVectorizationServiceTest {
    @Test
    void rendersPgVectorLiteral() {
        String literal = DocumentVectorizationService.vectorLiteral(new float[] {0.1f, -0.25f, 1.0f});

        assertThat(literal).isEqualTo("[0.1,-0.25,1.0]");
    }
}
