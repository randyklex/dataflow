package com.squarespace.dataflow;

/*
 * Provides options used to configure the processing performed by dataflow blocks.
 *
 * Dataflow blocks capture the state of the options at their construction. Subsequent
 * changes to the provided DataflowBlockOptions instance should not affect the behavior
 * of a dataflow block.
 */
public class DataflowBlockOptions {

    /*
     * A constant used to specify an unlimited quantity for DataflowBlockOptions members
     * that provide an upper bound. This field is constant.
     */
    public static final int Unbounded = -1;

    private int maxMessagesPerTask = Unbounded;

    private int boundedCapacity = Unbounded;

    private String nameFormat = "{0} id={1}";

    /*
     * Whether to force ordered processing of messages.
     */
    private boolean ensureOrdered = true;

    static final DataflowBlockOptions Default = new DataflowBlockOptions();

    DataflowBlockOptions DefaultOrClone()
    {
        if (this == Default) {
            return this;
        }
        else {
            DataflowBlockOptions rval = new DataflowBlockOptions();
            rval.setMaxMessagesPerTask(this.getMaxMessagesPerTask());
            rval.setBoundedCapacity(this.getBoundedCapacity());
            rval.setNameFormat(this.getNameFormat());
            rval.setEnsureOrdered(this.getEnsureOrdered());
            return rval;
        }
    }

    // TODO: not sure why we're creating an empty one.. seems like this should return Default?
    public DataflowBlockOptions()
    { }

    public int getMaxMessagesPerTask()
    {
        return maxMessagesPerTask;
    }

    public void setMaxMessagesPerTask(int value)
    {
        if (value < 1 && value != Unbounded)
            throw new IllegalArgumentException(String.format("value must be > 0 or {0}", Integer.toString(Unbounded)));

        maxMessagesPerTask = value;
    }

    /*
     * Gets a MaxMessagesPerTask value that may be used for comparison purposes.
     *
     * @return The maximum value, usable for comparison purposes.
     */
    int getActualMaxMessagesPerTask()
    {
        return (maxMessagesPerTask == Unbounded ? Integer.MAX_VALUE : maxMessagesPerTask);
    }

    public int getBoundedCapacity()
    {
        return boundedCapacity;
    }

    public void setBoundedCapacity(int value)
    {
        if (value < 1 && value != Unbounded)
            throw new IllegalArgumentException(String.format("Value must be > 0 or {0}", Integer.toString(Unbounded)));

        boundedCapacity = value;
    }

    public String getNameFormat()
    {
        return nameFormat;
    }

    public void setNameFormat(String value)
    {
        if (value == null)
            throw new IllegalArgumentException("value cannot be null.");

        nameFormat = value;
    }

    /*
     * Gets whether ordered processing should be enforced on a block's handling of messages.
     *
     * By default, dataflow blocks enforce ordering on the processing of messages. This means that a
     * block like "TransformBlock" will ensure that messages are output in the same order they
     * were input, even if parallelism is employed by the block and the processing of a message 'N' finishes
     * after the processing of a subsequence message 'N+1' (the block will reorder the results to maintain the input
     * ordering prior to making those results available to a consumer). Some blocks may allow this to be relaxed,
     * however. Setting @see EnsureOrdered to false tells a block that it may relax this ordering if it's able
     * to do so. This can be beneficial if the immediacy of a processed result being made available is more
     * important than the input-to-output ordering being maintained.
     */
    public boolean getEnsureOrdered()
    {
        return ensureOrdered;
    }

    public void setEnsureOrdered(boolean value)
    {
        ensureOrdered = value;
    }
}
