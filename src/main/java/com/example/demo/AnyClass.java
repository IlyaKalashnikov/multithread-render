package com.example.demo;

import java.io.PipedOutputStream;

public class AnyClass {

    private PipedOutputStream output;

    public AnyClass(PipedOutputStream output) {
        this.output = output;
    }
}
