/*
 * Copyright (C) 2012 Victor Nazarov <asviraspossible@gmail.com>
 */

package org.sviperll.sqlclient;

class ConnectionOptions {
    private final String connectionString;
    private final String userName;
    private final String password;

    public ConnectionOptions(String connectionString, String userName, String password) {
        this.connectionString = connectionString;
        this.userName = userName;
        this.password = password;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

}
