package com.squarespace.dataflow;


public class ExecutionDataflowBlockOptions extends DataflowBlockOptions {

    /*
     * The maximum number of tasks that may be used concurrently to process messages.
     */
    private int maxDegreeOfParallelism = 1;

    /*
     * Whether the code using this block will only ever have a single producer accessing the block
     * at any given tme.
     */
    private boolean singleProducerConstrained = false;

    static final ExecutionDataflowBlockOptions Default = new ExecutionDataflowBlockOptions();

    ExecutionDataflowBlockOptions DefaultOrClone()
    {
        if (this == Default)
            return this;
        else
        {
            ExecutionDataflowBlockOptions rval = new ExecutionDataflowBlockOptions();
            rval.setMaxMessagesPerTask(this.getMaxMessagesPerTask());
            rval.setBoundedCapacity(this.getBoundedCapacity());
            rval.setNameFormat(this.getNameFormat());
            rval.setEnsureOrdered(this.getEnsureOrdered());
            rval.setMaxDegreeOfParallelism(this.getMaxDegreeOfParallelism());
            rval.setSingleProducerConstrained(this.getSingleProducerConstrained());
            return rval;
        }
    }

    public ExecutionDataflowBlockOptions()
    {
        super();
    }

    public int getMaxDegreeOfParallelism()
    {
        return maxDegreeOfParallelism;
    }

    public void setMaxDegreeOfParallelism(int value)
    {
        if (value < 1 && value != Unbounded)
            throw new IllegalArgumentException(String.format("Value must be greater than 0, or {0}", Integer.toString(Unbounded)));

        maxDegreeOfParallelism = value;
    }

    /*
     * Gets whether code using the dataflow block is constrainted to one producer at a time.
     */
    public boolean getSingleProducerConstrained()
    {
        return singleProducerConstrained;
    }

    public void setSingleProducerConstrained(boolean value)
    {
        singleProducerConstrained = value;
    }

    /*
     * Gets a MaxDegreeOfParallelism value that may be used for comparison purposes.
     */
    int getActualMaxDegreeOfParallelism()
    {
        if (maxDegreeOfParallelism == Unbounded)
            return Integer.MAX_VALUE;

        return maxDegreeOfParallelism;
    }

    boolean getSupportsParallelExecution()
    {
        if (maxDegreeOfParallelism == Unbounded || maxDegreeOfParallelism > 1)
            return true;

        return false;
    }
}
