package io.github.krisner94.harpia.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class FlowBuilderTest {

    @Test
    void chainsUnitsInOrder() {
        FlowItem<String, TestTrace, String> flow = FlowBuilder
                .builder(String.class, TestTrace.class)
                .step((input, context) -> {
                    context.add("trim");
                    return input.trim();
                })
                .step((input, context) -> {
                    context.add("length");
                    return input.length();
                })
                .step((length, context) -> {
                    context.add("label");
                    return "len=" + length;
                })
                .build();

        TestTrace context = new TestTrace();

        assertEquals("len=5", flow.execute("  abcde  ", context));
        assertEquals(List.of("trim", "length", "label"), context.steps());
    }

    @Test
    void executesSingleUnitFlow() {
        FlowItem<Integer, TestTrace, Integer> flow = FlowBuilder
                .builder(Integer.class, TestTrace.class)
                .step((input, context) -> input * 2)
                .build();

        assertEquals(42, flow.execute(21, new TestTrace()));
    }

    @Test
    void sharesTheSameContextAcrossUnits() {
        TestTrace context = new TestTrace();

        FlowItem<String, TestTrace, String> flow = FlowBuilder
                .builder(String.class, TestTrace.class)
                .step((input, trace) -> {
                    trace.add(input);
                    return input.toUpperCase();
                })
                .step((input, trace) -> {
                    trace.add(input);
                    return String.join(",", trace.steps());
                })
                .build();

        assertEquals("abc,ABC", flow.execute("abc", context));
        assertEquals(List.of("abc", "ABC"), context.steps());
    }
}
