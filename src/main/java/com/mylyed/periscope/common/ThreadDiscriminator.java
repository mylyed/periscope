package com.mylyed.periscope.common;

import ch.qos.logback.classic.sift.ContextBasedDiscriminator;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class ThreadDiscriminator extends ContextBasedDiscriminator {
    String KEY = "threadName";

    /**
     * Return the name of the current context name as found in the logging event.
     */
    public String getDiscriminatingValue(ILoggingEvent event) {
        return event.getThreadName();
    }

    public String getDefaultValue() {
        return KEY;
    }

    public String getKey() {
        return KEY;
    }

    public void setKey(String key) {
        this.KEY = key;
    }
}