/*
 * Copyright (C) 2012 Victor Nazarov <asviraspossible@gmail.com>
 */

package org.sviperll.sqlclient;

import com.github.sviperll.Credentials;
import com.github.sviperll.Property;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import com.github.sviperll.cli.CLISpecification;
import com.github.sviperll.cli.CLIException;
import com.github.sviperll.cli.CLIHandlers;
import java.io.File;

/**
 *
 * @author vir
 */
public class Main {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException, IOException {
        Main main = new Main();

        CLISpecification cliSpec = new CLISpecification();
        cliSpec.add('s', "silent", "Do not output information intended for human", CLIHandlers.booleanHandler(main.silent));
        cliSpec.add("no-prompt", "Do not output command line prompt", CLIHandlers.booleanHandler(main.noprompt));
        cliSpec.add('t', "timeout", "Do not output command line prompt", CLIHandlers.integer(main.timeout));
        cliSpec.add('p', "password-file", "Do not output command line prompt", new CredentialsHandler(main.credentials));
        try {
            args = cliSpec.run(args);
            if (args.length != 2) {
                System.out.println("usage: command [OPTIONS] <driver-class-name> <jdbc-url>");
                cliSpec.usage(System.out);
            } else
                main.run(args);
        } catch (CLIException ex) {
            System.out.println("usage: command [OPTIONS] <driver-class-name> <jdbc-url>");
            cliSpec.usage(System.out);
        }
    }

    private final Property<Boolean> silent = new Property<Boolean>(false);
    private final Property<Boolean> noprompt = new Property<Boolean>(false);
    private final Property<Integer> timeout = new Property<Integer>(10);
    private final Property<Credentials> credentials = new Property<Credentials>(null);

    private void recoverFromException(Exception ex) {
        System.err.println("Found error");
        ex.printStackTrace(System.err);
    }

    private void inhibitException(Exception ex) {
        System.err.println("Inhibbited exception");
        ex.printStackTrace(System.err);
    }

    private void run(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        String driver = args[0];
        String connectionString = args[1];

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
        try {
            ConsoleWriter writer = new ConsoleWriter(bw, silent.get());
            BootProcess boot = new BootProcess(writer, connectionString);
            boot.run(driver);
        } finally {
            try {
                bw.close();
            } catch (IOException ex) {
                inhibitException(ex);
            }
        }
    }

    private class BootProcess {
        private final ConsoleWriter writer;
        private final String connectionString;

        private BootProcess(ConsoleWriter writer, String connectionString) {
            this.writer = writer;
            this.connectionString = connectionString;
        }

        private void run(String driver) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
            writer.writeForHuman("Loading: ");
            writer.writeForHuman(driver);
            writer.writeForHuman("...");
            writer.flushForHuman();
            Class.forName(driver).newInstance();
            writer.writeForHuman("done.");
            writer.newLineForHuman();
            writer.flushForHuman();

            bootUpAndRunRepl();
        }

        private void bootUpAndRunRepl() {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            try {
                ConsoleReader reader = new ConsoleReader(br, writer, !noprompt.get());
                bootUpAndRunReplInErrorRecoveryLoop(reader);
            } finally {
                try {
                    br.close();
                } catch (IOException ex) {
                    inhibitException(ex);
                }
            }
        }

        private void bootUpAndRunReplInErrorRecoveryLoop(ConsoleReader reader) {
            for (;;) {
                try {
                    bootUpAndRunRepl(reader);
                    break;
                } catch (IOException ex) {
                    recoverFromException(ex);
                } catch (SQLException ex) {
                    recoverFromException(ex);
                }
            }
        }

        private void bootUpAndRunRepl(ConsoleReader reader) throws IOException, SQLException {
            writer.writeForHuman("Connecting: ");
            writer.writeForHuman(connectionString);
            writer.writeForHuman("...");
            writer.flushForHuman();
            DriverManager.setLoginTimeout(timeout.get());
            Connection connection;
            if (credentials.get() == null)
                connection = DriverManager.getConnection(connectionString);
            else
                connection = DriverManager.getConnection(connectionString, credentials.get().userName(), credentials.get().password());

            try {
                writer.writeForHuman("done.");
                writer.newLineForHuman();
                writer.flushForHuman();

                Repl repl = new Repl(connection, reader, writer);
                repl.run();
            } finally {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    inhibitException(ex);
                }
            }
        }

    }
}
