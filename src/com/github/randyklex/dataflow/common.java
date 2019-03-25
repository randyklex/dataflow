package com.github.randyklex.dataflow;

public class common {

    // An invalid ID to assign for reording purposes. This value is chosen to be the last
    // of the 64-bit integers that could ever be assigned as a reording ID.
    static final long INVALID_REORDERING_ID = -1;

    // A well-known message ID for code taht will send exactly one
    // mesage or where the exact message ID is not important.
    static final int SINGLE_MESSAGE_ID = 1;

    static final DataflowMessageHeader SingleMessageHeader = new DataflowMessageHeader(SINGLE_MESSAGE_ID);

    static final int KEEP_ALIVE_NUMBER_OF_MESSAGES_THRESHOLD = 1;

    static final int KEEP_ALIVE_BAN_COUNT = 1000;

}
