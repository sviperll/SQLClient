/*
 * Copyright (C) 2012 Victor Nazarov <asviraspossible@gmail.com>
 */

package org.sviperll.sqlclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.sviperll.cli.CLISpecification;
import org.sviperll.cli.BooleanProperty;
import org.sviperll.cli.CLIException;

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
        cliSpec.add('s', "silent", "Do not output information intended for human", main.silent);
        cliSpec.add('p', "no-prompt", "Do not output command line prompt", main.noprompt);
        try {
            args = cliSpec.run(args);
            if (args.length < 4) {
                System.out.println("usage: command [OPTIONS] <driver-class-name> <jdbc-url> <user-name> <password>");
                cliSpec.usage(System.out);
            } else
                main.run(args);
        } catch (CLIException ex) {
            System.out.println("usage: command [OPTIONS] <driver-class-name> <jdbc-url> <user-name> <password>");
            cliSpec.usage(System.out);
        }
    }

    private final BooleanProperty silent = new BooleanProperty();
    private final BooleanProperty noprompt = new BooleanProperty();

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
        ConnectionOptions options = new ConnectionOptions(args[1], args[2], args[3]);

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
        try {
            ConsoleWriter writer = new ConsoleWriter(bw, silent.get());
            BootProcess boot = new BootProcess(writer, options);
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
        private final ConnectionOptions connectionOptions;

        private BootProcess(ConsoleWriter writer, ConnectionOptions connectionOptions) {
            this.writer = writer;
            this.connectionOptions = connectionOptions;
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
            writer.writeForHuman(connectionOptions.getConnectionString());
            writer.writeForHuman("...");
            writer.flushForHuman();
            Connection connection = DriverManager.getConnection(connectionOptions.getConnectionString(), connectionOptions.getUserName(), connectionOptions.getPassword());
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
