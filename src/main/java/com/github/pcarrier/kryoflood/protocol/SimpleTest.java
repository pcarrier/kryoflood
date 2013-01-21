package com.github.pcarrier.kryoflood.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleTest {
    @Getter
    private static final byte[] exampleBytes = "Hello!".getBytes();

    private int id;
    private String message;
}
