package com.github.randyklex.dataflow;

public class TryResult<TResult> {
    private boolean success = false;
    private TResult result = null;

    public TryResult(boolean success, TResult result)
    {
        super();
        this.success = success;
        this.result = result;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public TResult getResult()
    {
        return result;
    }
}
