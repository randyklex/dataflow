package com.github.randyklex.dataflow;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

final class TargetRegistry<T> {

    private final ISourceBlock<T> owningSource;
    private final HashMap<ITargetBlock<T>, LinkedTargetInfo> targetInformation;
    private LinkedTargetInfo firstTarget;
    private LinkedTargetInfo lastTarget;
    private int linksWithRemainingMessages;

    TargetRegistry(ISourceBlock<T> owningSource)
    {
        this.owningSource = owningSource;
        this.targetInformation = new HashMap<>();
    }

    void Add(ITargetBlock<T> target, DataflowLinkOptions linkOptions)
    {
        LinkedTargetInfo targetInfo;

        if (targetInformation.containsKey(target))
            target = new NopLinkPropagator(owningSource, target);

        LinkedTargetInfo node = new LinkedTargetInfo(target, linkOptions);
        AddToList(node, linkOptions.isAppend());
        targetInformation.put(target, node);

        if (node.RemainingMessages > 0) linksWithRemainingMessages++;

        // TODO: left out the compiler directive for Feature Tracing
    }

    boolean Contains(ITargetBlock<T> target)
    {
        return targetInformation.containsKey(target);
    }

    void Remove(ITargetBlock<T> target)
    {
        Remove(target, false);
    }

    void Remove(ITargetBlock<T> target, boolean onlyIfReachedMaxMessages)
    {
        if (target == null)
            // TODO: convert this to a debug assert
            throw new IllegalArgumentException("target cannot be null.");

        if (onlyIfReachedMaxMessages && linksWithRemainingMessages == 0)
            return;

        Remove_Slow(target, onlyIfReachedMaxMessages);
    }

    private void Remove_Slow(ITargetBlock<T> target, boolean onlyIfReachedMaxMessages)
    {
        if (target == null)
            // TODO: convert this to a debug assert
            throw new IllegalArgumentException("target cannot be null.");

        LinkedTargetInfo node;
        if (targetInformation.containsKey(target)) {
            node = targetInformation.get(target);

            if (!onlyIfReachedMaxMessages || node.RemainingMessages == 1)
            {
                RemoveFromList(node);
                targetInformation.remove(target);

                if (node.RemainingMessages == 0)
                    linksWithRemainingMessages--;

                // TODO: skipped FEATURE_TRACING directive

            }
            else if (node.RemainingMessages > 0)
            {
                // TODO: skipped the debug.assert
                node.RemainingMessages--;
            }
        }
    }

    /*
     * Clears the target registry entry points while allowing subsequent traversals of the linked list.
     */
    LinkedTargetInfo ClearEntryPoints()
    {
        // save firstTarget so we can return to it.
        LinkedTargetInfo ft = firstTarget;

        firstTarget = lastTarget = null;
        targetInformation.clear();
        linksWithRemainingMessages = 0;

        return ft;
    }

    /*
     * The first node of the ordered target list.
     */
    LinkedTargetInfo getFirstTargetNode() { return firstTarget; }

    void AddToList(LinkedTargetInfo node, boolean append)
    {
        assert node != null;

        if (firstTarget == null && lastTarget == null)
            firstTarget = lastTarget = node;
        else
        {
            assert firstTarget != null && lastTarget != null;
            assert lastTarget.Next == null;
            assert firstTarget.Previous == null;

            if (append)
            {
                node.Previous = lastTarget;
                lastTarget.Next = node;
                lastTarget = node;
            }
            else
            {
                node.Next = firstTarget;
                firstTarget.Previous = node;
                firstTarget = node;
            }
        }

        // TODO: skipped the last assert.
    }

    void RemoveFromList(LinkedTargetInfo node)
    {
        assert node != null;
        assert firstTarget != null && lastTarget != null;

        LinkedTargetInfo previous = node.Previous;
        LinkedTargetInfo next = node.Next;

        if (node.Previous != null)
        {
            node.Previous.Next = next;
            node.Previous = null;
        }

        if (node.Next != null)
        {
            node.Next.Previous = previous;
            node.Next = null;
        }

        if (firstTarget == node) firstTarget = next;
        if (lastTarget == node) lastTarget = previous;
    }

    private int size() { return targetInformation.size(); }

    // TODO: left out the array of TargetsForDebugger

    final class NopLinkPropagator implements IPropagatorBlock<T, T>, ISourceBlock<T>
    {
        private final ISourceBlock<T> owningSource;
        private final ITargetBlock<T> target;

        NopLinkPropagator(ISourceBlock<T> owningSource, ITargetBlock<T> target)
        {
            assert owningSource != null;
            assert target != null;

            this.owningSource = owningSource;
            this.target = target;
        }

        public DataflowMessageStatus offerMessage(DataflowMessageHeader messageHeader, T messageValue, ISourceBlock<T> source, boolean consumeToAccept)
        {
            assert source == owningSource;
            return target.offerMessage(messageHeader, messageValue, this, consumeToAccept);
        }

        public TryResult<T> consumeMessage(DataflowMessageHeader messageHeader, ITargetBlock<T> target)
        {
            return owningSource.consumeMessage(messageHeader, this);
        }

        public boolean reserveMessage(DataflowMessageHeader messageHeader, ITargetBlock<T> target)
        {
            return owningSource.reserveMessage(messageHeader, this);
        }

        public void releaseReservation(DataflowMessageHeader messageHeader, ITargetBlock<T> target)
        {
            owningSource.releaseReservation(messageHeader, this);
        }

        public CompletableFuture<?> getCompletion()
        {
            return owningSource.getCompletion();
        }

        public void complete()
        {
            target.complete();
        }

        public void fault(Exception exception)
        {
            target.fault(exception);
        }

        public AutoCloseable linkTo(ITargetBlock<T> target, DataflowLinkOptions linkOptions) {
            // TODO: find the java equivalent of NotSupportedException
            throw new IllegalStateException("cannot call this method.");
        }
    }
    final class LinkedTargetInfo
    {
        final ITargetBlock<T> Target;
        final boolean PropagateCompletion;
        int RemainingMessages;
        LinkedTargetInfo Previous;
        LinkedTargetInfo Next;

        LinkedTargetInfo(ITargetBlock<T> target, DataflowLinkOptions linkOptions)
        {
            this.Target = target;
            this.PropagateCompletion = linkOptions.getPropagateCompletion();
            this.RemainingMessages = linkOptions.getMaxNumberOfMessages();
        }
    }
}
