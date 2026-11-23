package io.github.krisner94.harpia.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FlowBuilderTest {

    @Test
    void chainsUnitsInOrder() {
        FlowItem<String, String> flow = FlowBuilder.from((String input) -> input.trim())
                .step(input -> input.length())
                .step(length -> "len=" + length)
                .build();

        assertEquals("len=5", flow.execute("  abcde  "));
    }

    @Test
    void executesSingleUnitFlow() {
        FlowItem<Integer, Integer> flow = FlowBuilder.from((Integer input) -> input * 2)
                .build();

        assertEquals(42, flow.execute(21));
    }

    @Test
    void anchorsInputTypeForInlineLambdas() {
        FlowItem<String, String> flow = FlowBuilder.builder(String.class)
                .step(input -> input.trim())
                .step(input -> input.toUpperCase())
                .build();

        assertEquals("ABC", flow.execute("  abc  "));
    }

    @Test
    void startsFromNamedUnitWithoutClassTokens() {
        FlowItem<String, Integer> length = input -> input.length();

        FlowItem<String, Integer> flow = FlowBuilder.from(length)
                .build();

        assertEquals(3, flow.execute("abc"));
    }
}
