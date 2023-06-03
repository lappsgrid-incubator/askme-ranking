package org.lappsgrid.askme.scoring

/**
 *
 */
trait Tokenizer {
    String[] tokenize(String string) {
        return string.trim().split("\\s+")
    }
}