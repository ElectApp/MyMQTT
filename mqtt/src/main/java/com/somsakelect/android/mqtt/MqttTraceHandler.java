package com.somsakelect.android.mqtt;


/**
 * Interface for simple trace handling, pass the trace message to trace
 * callback.
 *
 */

public interface MqttTraceHandler {

    /**
     * Trace debugging information
     *
     * @param tag
     *            identifier for the source of the trace
     * @param message
     *            the text to be traced
     */
    void traceDebug(String tag, String message);

    /**
     * Trace error information
     *
     * @param tag
     *            identifier for the source of the trace
     * @param message
     *            the text to be traced
     */
    void traceError(String tag, String message);

    /**
     * trace exceptions
     *
     * @param tag
     *            identifier for the source of the trace
     * @param message
     *            the text to be traced
     * @param e
     *            the exception
     */
    void traceException(String tag, String message,
                        Exception e);

}
