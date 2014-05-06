/*
 * Copyright 2014 Victor Nazarov <asviraspossible@gmail.com>.
 */
package org.sviperll.sqlclient;

import com.github.sviperll.Credentials;
import com.github.sviperll.Property;
import com.github.sviperll.cli.CLIParameterFormatException;
import com.github.sviperll.cli.CLIParameterHandler;
import com.github.sviperll.io.Charsets;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Victor Nazarov <asviraspossible@gmail.com>
 */
class CredentialsHandler implements CLIParameterHandler {
    private final Property<Credentials> credentials;

    public CredentialsHandler(Property<Credentials> credentials) {
        this.credentials = credentials;
    }

    @Override
    public void handleCLIParameter(String path) throws CLIParameterFormatException {
        try {
            File passwordFile = new File(path);
            credentials.set(Credentials.readPasswordFile(passwordFile, Charsets.UTF8));
        } catch (IOException ex) {
            throw new CLIParameterFormatException("Error reading password file: " + path, ex);
        }
    }

}
