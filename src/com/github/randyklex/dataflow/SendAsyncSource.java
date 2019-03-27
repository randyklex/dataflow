/*package com.github.randyklex.dataflow;

import java.util.function.Consumer;

public class SendAsyncSource<TOutput> implements ISourceBlock<TOutput> {

    private final ITargetBlock<TOutput> target;

    private final TOutput messageValue;

    private int cancellationState;

    private static final int CANCELLATION_STATE_NONE = 0;

    private static final int CANCELLATION_STATE_REGISTERED = 1;

    private static final int CANCELLATION_STATE_RESERVED = 2;

    private static final int CANCELLATION_STATE_COMPLETING = 3;

    private static final Consumer<Object> cancellationCallback = CancellationHandler;

    private static void CancellationHandler(Object state)
    { }

    void OfferToTarget()
    {
        try {
            boolean consumeToAccept = cancellationState != CANCELLATION_STATE_NONE;

            switch(target.offerMessage(common.SingleMessageHeader, messageValue, this, consumeToAccept))
            {
                case Accepted:
                    if (!consumeToAccept)
                    {
                        CompleteAsAccepted(false);
                    }
                    else if (cancellationState != CANCELLATION_STATE_COMPLETING)
                    {
                        throw new
                    }
                    break;
                case Declined:
                case DecliningPermanently:
                    CompleteAsDeclined(false);
                    break;
            }
        }
        catch (Exception exc)
        {
            CompleteAsFaulted(exc, false);
        }
    }

    // TODO: refactor to have a "messageConsumed" return value too.
    public TOutput consumeMessage(DataflowMessageHeader messageHeader, ITargetBlock<TOutput> target)
    {
        if (!messageHeader.isValid())
            throw new IllegalArgumentException("messageHeader is invalid.");

        if (target == null)
            throw new IllegalArgumentException("target cannot be null.");

        boolean validMessage = messageHeader.getId() == common.SINGLE_MESSAGE_ID;

        if (validMessage)
        {
            int curState = cancellationState;

            if (curState == CANCELLATION_STATE_NONE || (curState != CANCELLATION_STATE_COMPLETING))
            {
                CompleteAsAccepted(false);
                messageConsumed = true;
            }
        }

        messageConsumed = false;
        return Default(TOutput);
    }
}
*/