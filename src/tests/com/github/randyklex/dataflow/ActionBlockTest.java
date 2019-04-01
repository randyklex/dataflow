package com.github.randyklex.dataflow;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ActionBlockTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void post() {
        ExecutionDataflowBlockOptions options = new ExecutionDataflowBlockOptions();
        options.setSingleProducerConstrained(true);
        options.setMaxDegreeOfParallelism(1);

        ActionBlock<String> block = new ActionBlock<>((val) -> {
            System.out.println("hello " + val);
            assertEquals("World", val);
        }, options);

        block.post("World");
    }
}