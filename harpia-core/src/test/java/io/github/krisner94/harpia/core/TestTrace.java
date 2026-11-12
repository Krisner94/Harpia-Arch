package io.github.krisner94.harpia.core;

import java.util.ArrayList;
import java.util.List;

final class TestTrace {

    private final List<String> steps = new ArrayList<>();

    void add(String step) {
        steps.add(step);
    }

    List<String> steps() {
        return steps;
    }
}
