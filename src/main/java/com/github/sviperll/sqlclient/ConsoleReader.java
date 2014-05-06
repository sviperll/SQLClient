/*
 * Copyright (C) 2012 Victor Nazarov <asviraspossible@gmail.com>
 */

package com.github.sviperll.sqlclient;

import java.io.BufferedReader;
import java.io.IOException;

class ConsoleReader {
    private final BufferedReader reader;
    private final ConsoleWriter writer;
    private final boolean isPromptEnabled;

    ConsoleReader(BufferedReader reader, ConsoleWriter writer, boolean isPromptEnabled) {
        this.reader = reader;
        this.writer = writer;
        this.isPromptEnabled = isPromptEnabled;
    }

    String readLine() throws IOException {
        if (isPromptEnabled) {
            writer.writeForHuman("> ");
            writer.flushForHuman();
        }
        return reader.readLine();
    }
}
