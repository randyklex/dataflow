package com.github.randyklex.dataflow;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;


public class ActionBlockTest {


    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void postWithDefaultOptions()  throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ActionBlock<String> block = new ActionBlock<>((val) -> {
            System.out.println("Hello " + val);
            latch.countDown();
        });

        block.post("World");
        boolean completed = latch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(completed);
    }

    @Test
    public void postWithConstrainedAndMaxDegreeOne() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutionDataflowBlockOptions options = new ExecutionDataflowBlockOptions();
        options.setSingleProducerConstrained(true);
        options.setMaxDegreeOfParallelism(1);

        ActionBlock<String> block = new ActionBlock<>((val) -> {
            System.out.println("hello " + val);
            latch.countDown();
        }, options);

        block.post("World");
        boolean completed= latch.await(2000, TimeUnit.MILLISECONDS);
        assertTrue(completed);
    }
}