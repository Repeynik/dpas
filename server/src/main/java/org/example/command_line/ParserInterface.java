package org.example.command_line;

public interface ParserInterface {
    ParserImpl.Config parseArgs(String[] args);

    void printUsage();
}
