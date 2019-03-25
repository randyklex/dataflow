package com.squarespace.dataflow;

public enum DataflowMessageStatus {

    /*
     * Indicates that the TargetBlock accepted the message. Once a target has accepted
     * a message, it is wholly owned by the target.
     */
    Accepted(0x0),

    /*
     * Indicates that the TargetBlock declined the message. The SourceBlock still
     * owns the message.
     */
    Declined(0x1),

    /*
     * Indicates that the TargetBlock postponed the message for potential consumption at
     * a later time. The ISourceBlock still owns the message.
     */
    Postponed(0x2),

    NotAvailable(0x3),

    DecliningPermanently(0x4);

    private int numVal;

    DataflowMessageStatus(int numVal)
    {
        this.numVal = numVal;
    }
}
