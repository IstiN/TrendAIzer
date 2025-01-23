package com.github.istin.tradingaizer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AppTest {

    @Test
    void testMainRunsWithoutErrors() {
        // Assert that the main method runs without throwing any exceptions
        assertDoesNotThrow(() -> App.main(new String[]{}), "Main method should run without errors.");
    }
}
