package com.alphora.cql.rest;

import io.undertow.Undertow;

class Server {
    public static Undertow build()
    {
        return Undertow.builder().addHttpListener(8080, "localhost").build();
    }
}