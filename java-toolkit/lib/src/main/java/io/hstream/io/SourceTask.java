/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package io.hstream.io;

import io.hstream.HRecord;

public interface SourceTask extends Task {
    void run(HRecord config, SourceTaskContext ctx);
}