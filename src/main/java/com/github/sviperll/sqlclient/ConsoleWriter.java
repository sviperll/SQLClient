/*
 * Copyright (C) 2012 Victor Nazarov <asviraspossible@gmail.com>
 */

package com.github.sviperll.sqlclient;

import java.io.BufferedWriter;
import java.io.IOException;

class ConsoleWriter {
    private final boolean isQuiet;
    private final BufferedWriter writer;
    ConsoleWriter(BufferedWriter writer, boolean isQuiet) {
        this.writer = writer;
        this.isQuiet = isQuiet;
    }

    void writeData(String s) throws IOException {
        writer.append(s);
    }

    void flushData() throws IOException {
        writer.flush();
    }

    void newLineForData() throws IOException {
        writer.newLine();
    }

    void writeForHuman(String s) throws IOException {
        if (!isQuiet)
            writeData(s);
    }

    void flushForHuman() throws IOException {
        if (!isQuiet)
            flushData();
    }

    void newLineForHuman() throws IOException {
        if (!isQuiet)
            newLineForData();
    }

}
