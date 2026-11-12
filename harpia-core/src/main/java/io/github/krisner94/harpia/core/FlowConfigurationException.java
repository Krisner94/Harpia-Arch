package io.github.krisner94.harpia.core;

/**
 * Signals an invalid flow or workflow configuration.
 */
public class FlowConfigurationException extends IllegalStateException {

    /**
     * Creates the exception with the configuration failure detail.
     *
     * @param message configuration failure detail
     */
    public FlowConfigurationException(String message) {
        super(message);
    }
}
