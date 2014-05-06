/*
 * Copyright (C) 2012 Victor Nazarov <asviraspossible@gmail.com>
 */

package com.github.sviperll.sqlclient;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

class Repl {
    private static final SimpleDateFormat sqlTimestampLiteralFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat sqlDateLiteralFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat sqlTimeLiteralFormat = new SimpleDateFormat("HH:mm:ss");
    private static final String quoteString = "'";
    private static final String escapedQuoteString = "''";

    private final Connection connection;
    private final ConsoleReader reader;
    private final ConsoleWriter writer;

    Repl(Connection connection, ConsoleReader reader, ConsoleWriter writer) {
        this.connection = connection;
        this.reader = reader;
        this.writer = writer;
    }

    void run() throws SQLException, IOException {
        String query;
        while((query = read()) != null) {
            Result res = eval(query);
            try {
                res.print();
                writer.flushForHuman();
            } finally {
                res.close();
            }
        }
    }

    private String read() throws IOException {
        String query = reader.readLine();
        while (query != null && query.trim().isEmpty())
            query = reader.readLine();
        return query;
    }

    private Result eval(String query) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        boolean isResultSet = statement.execute();
        if (!isResultSet) {
            return new UpdateResult(statement.getUpdateCount(), statement);
        } else {
            return new ResultSetResult(statement.getResultSet(), statement.getMetaData(), statement);
        }
    }

    private interface Result {
        void print() throws IOException, SQLException;

        void close() throws SQLException;
    }

    private class ResultSetResult implements Result {
        private final ResultSet resultSet;
        private final PreparedStatement statement;
        private final ResultSetMetaData metaData;

        private ResultSetResult(ResultSet resultSet, ResultSetMetaData metaData, PreparedStatement statement) {
            this.resultSet = resultSet;
            this.statement = statement;
            this.metaData = metaData;
        }
        @Override
        public void print() throws SQLException, IOException {
            int columnCount = metaData.getColumnCount();
            if (columnCount == 0) {
                writer.writeForHuman("No data");
                writer.newLineForHuman();
            } else {
                writer.writeForHuman(metaData.getColumnLabel(1));
                for (int i = 1; i < columnCount; i++) {
                    writer.writeForHuman(",\t");
                    writer.writeForHuman(metaData.getColumnLabel(i + 1));
                }
                writer.newLineForHuman();
                int count = 0;
                while (resultSet.next()) {
                    count++;
                    writeObject(resultSet.getObject(1));
                    for (int i = 1; i < columnCount; i++) {
                        writer.writeData(",\t");
                        writeObject(resultSet.getObject(i + 1));
                    }
                    writer.newLineForData();
                }
                writer.writeForHuman("Records selected: " + count);
                writer.newLineForHuman();
            }
        }

        @Override
        public void close() throws SQLException {
            resultSet.close();
            statement.close();
        }

        private void writeObject(Object object) throws IOException {
            if (object == null)
                writer.writeData("NULL");
            else if (object instanceof Boolean) {
                boolean bool = ((Boolean)object).booleanValue();
                if (bool)
                    writer.writeData("TRUE");
                else
                    writer.writeData("FALSE");
            } else if (object instanceof java.sql.Timestamp) {
                Date date = (Date)object;
                String literal = Repl.sqlTimestampLiteralFormat.format(date);
                String escaped = quoteSQLString(literal);
                writer.writeData("TIMESTAMP ");
                writer.writeData(escaped);
            } else if (object instanceof java.sql.Date) {
                Date date = (Date)object;
                String literal = Repl.sqlDateLiteralFormat.format(date);
                String escaped = quoteSQLString(literal);
                writer.writeData("DATE ");
                writer.writeData(escaped);
            } else if (object instanceof java.sql.Date) {
                Date date = (Date)object;
                String literal = Repl.sqlTimeLiteralFormat.format(date);
                String escaped = quoteSQLString(literal);
                writer.writeData("TIME ");
                writer.writeData(escaped);
            } else if (isTypeWithPlainLiterals(object))
                writer.writeData(object.toString());
            else
                writer.writeData(quoteSQLString(object.toString()));
        }

        private boolean isTypeWithPlainLiterals(Object object) {
            return object instanceof java.math.BigDecimal
                    || object instanceof Short
                    || object instanceof Integer
                    || object instanceof Long
                    || object instanceof Float
                    || object instanceof Double;
        }

        private String quoteSQLString(String s) {
            StringBuilder sb = new StringBuilder();
            sb.append(quoteString);
            int fromIndex = 0;
            int index;
            while ((index = s.indexOf(quoteString, fromIndex)) >= 0) {
                sb.append(s.substring(fromIndex, index));
                sb.append(escapedQuoteString);
                fromIndex = index + quoteString.length();
            }
            sb.append(s.substring(fromIndex));
            sb.append(quoteString);
            return sb.toString();
        }

    }

    private class UpdateResult implements Result {
        private final int updateCount;
        private final PreparedStatement statement;

        private UpdateResult(int updateCount, PreparedStatement statement) {
            this.updateCount = updateCount;
            this.statement = statement;
        }
        @Override
        public void print() throws IOException {
            writer.writeForHuman("Records updated: ");
            writer.writeData(Integer.toString(updateCount));
            writer.newLineForData();
        }

        @Override
        public void close() throws SQLException {
            statement.close();
        }
    }
}
