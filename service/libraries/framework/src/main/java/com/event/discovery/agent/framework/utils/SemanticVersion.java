package com.event.discovery.agent.framework.utils;

import lombok.EqualsAndHashCode;

import java.util.StringJoiner;

@EqualsAndHashCode
public class SemanticVersion {
    public final int[] numbers;

    public SemanticVersion(String version) {
        final String[] split = version.replace("+", "").split("\\.");// For version like 2.14+, will be set to 2.14.0
        numbers = new int[3];
        for (int i = 0; i < split.length; i++) {
            numbers[i] = Integer.parseInt(split[i]);
        }

        if (split.length < 3) {
            for (int i = split.length; i < 3; i++) {
                numbers[i] = 0;
            }
        }
    }

    public boolean greaterThan(SemanticVersion compareTo) {
        if (numbers.length == compareTo.numbers.length) {
            for (int i = 0; i < numbers.length; i++) {
                if (numbers[i] > compareTo.numbers[i]) {
                    return true;
                } else if (numbers[i] < compareTo.numbers[i]) {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(".");
        for (int item : numbers) {
            joiner.add(Integer.toString(item));
        }
        return joiner.toString();
    }
}
