package com.squarespace.dataflow;

public class DataflowLinkOptions {

    static final int Unbounded = DataflowBlockOptions.Unbounded;

    /*
     * Whether the linked target will have completion and faulting notification propagated
     * to it automatically.
     */
    private boolean propagateCompletion = false;

    private int maxNumberOfMessages = Unbounded;

    /*
     * Whether the link should be appended to the source's list
     * of links, or whether it should be prepended.
     */
    private boolean append = true;

    static final DataflowLinkOptions Default = new DataflowLinkOptions();

    static final DataflowLinkOptions UnlinkAfterOneAndPropagateCompletion = new DataflowLinkOptions(1, true);

    public DataflowLinkOptions()
    { }

    DataflowLinkOptions(int maxNumberOfMessages, boolean propagateCompletion)
    {
        setPropagateCompletion(propagateCompletion);
        setMaxNumberOfMessages(maxNumberOfMessages);
    }

    public boolean getPropagateCompletion()
    {
        return propagateCompletion;
    }

    public void setPropagateCompletion(boolean value)
    {
        if (this == Default || this == UnlinkAfterOneAndPropagateCompletion)
            throw new IllegalStateException("Default and UnlinkAfterOneAndPropagateCompletion are supposed to be immutable.");

        propagateCompletion = value;
    }

    public int getMaxNumberOfMessages()
    {
        return maxNumberOfMessages;
    }

    /*
     * Sets the maximum number of messages that may be consumed across the link.
     */
    public void setMaxNumberOfMessages(int value)
    {
        if (this == Default || this == UnlinkAfterOneAndPropagateCompletion)
            throw new IllegalStateException("Default and UnlinkAfterOneAndPropagateCompletion are supposed to be immutable.");

        if (value < 1 && value != Unbounded)
            throw new IllegalArgumentException(String.format("Value must be greater than 0 or {0}", Integer.toString(Unbounded)));

        maxNumberOfMessages = value;
    }

    public boolean isAppend()
    {
        return append;
    }

    public void setAppend(boolean value)
    {
        if (this == Default || this == UnlinkAfterOneAndPropagateCompletion)
            throw new IllegalStateException("Default and UnlinkAfterOneAndPropagateCompletion are supposed to be immutable.");

        append = value;
    }
}

