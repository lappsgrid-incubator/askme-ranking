package org.lappsgrid.askme.mining.scoring

/**
 *
 */
trait Tokenizer {
    String[] tokenize(String string) {
        return string.trim().split("\\s+")
    }
}