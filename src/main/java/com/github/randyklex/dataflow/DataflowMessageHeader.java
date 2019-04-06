package com.github.randyklex.dataflow;

/*
 * Provides a container of data attributes for passing between dataflow blocks.
 */
public class DataflowMessageHeader {

    static final long DEFAULT_VALUE = 0L;

    private final long id;

    public DataflowMessageHeader(long id)
    {
        if (id == DEFAULT_VALUE)
            throw new IllegalArgumentException("invalid id");

        this.id = id;
    }

    /* Gets the validity of the message.
    *
    * @return true if the ID of the message is different from 0. False if the ID of the message is 0.
    */
    public boolean isValid()
    {
        return (id != DEFAULT_VALUE);
    }

    public long getId()
    {
        return id;
    }

    @Override
    public boolean equals(Object obj) {

        if (getClass() != obj.getClass())
            return false;

        return this.getId() == ((DataflowMessageHeader)obj).getId();
    }

    @Override
    public int hashCode() {
        return (int)id;
    }
}
