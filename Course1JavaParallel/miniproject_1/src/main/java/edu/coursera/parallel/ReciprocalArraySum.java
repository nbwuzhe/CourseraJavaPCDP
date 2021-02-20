package edu.coursera.parallel;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import java.util.concurrent.ForkJoinTask;
import java.util.ArrayList;

/**
 * Class wrapping methods for implementing reciprocal array sum in parallel.
 */
public final class ReciprocalArraySum {

    /**
     * Default constructor.
     */
    private ReciprocalArraySum() {
    }

    /**
     * Sequentially compute the sum of the reciprocal values for a given array.
     *
     * @param input Input array
     * @return The sum of the reciprocals of the array input
     */
    protected static double seqArraySum(final double[] input) {
        double sum = 0;

        // Compute sum of reciprocals of array elements
        for (int i = 0; i < input.length; i++) {
            sum += 1 / input[i];
        }

        return sum;
    }

    /**
     * Computes the size of each chunk, given the number of chunks to create
     * across a given number of elements.
     *
     * @param nChunks The number of chunks to create
     * @param nElements The number of elements to chunk across
     * @return The default chunk size
     */
    private static int getChunkSize(final int nChunks, final int nElements) {
        // Integer ceil
        return (nElements + nChunks - 1) / nChunks;
    }

    /**
     * Computes the inclusive element index that the provided chunk starts at,
     * given there are a certain number of chunks.
     *
     * @param chunk The chunk to compute the start of
     * @param nChunks The number of chunks created
     * @param nElements The number of elements to chunk across
     * @return The inclusive index that this chunk starts at in the set of
     *         nElements
     */
    private static int getChunkStartInclusive(final int chunk,
            final int nChunks, final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        return chunk * chunkSize;
    }

    /**
     * Computes the exclusive element index that the provided chunk ends at,
     * given there are a certain number of chunks.
     *
     * @param chunk The chunk to compute the end of
     * @param nChunks The number of chunks created
     * @param nElements The number of elements to chunk across
     * @return The exclusive end index for this chunk
     */
    private static int getChunkEndExclusive(final int chunk, final int nChunks,
            final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        final int end = (chunk + 1) * chunkSize;
        if (end > nElements) {
            return nElements;
        } else {
            return end;
        }
    }

    /**
     * This class stub can be filled in to implement the body of each task
     * created to perform reciprocal array sum in parallel.
     */
    private static class ReciprocalArraySumTask extends RecursiveAction {
        /**
         * Starting index for traversal done by this task.
         */
        private final int startIndexInclusive;
        /**
         * Ending index for traversal done by this task.
         */
        private final int endIndexExclusive;
        /**
         * Input array to reciprocal sum.
         */
        private final double[] input;
        /**
         * Intermediate value produced by this task.
         */
        private double value;

        /**
         * Constructor.
         * @param setStartIndexInclusive Set the starting index to begin
         *        parallel traversal at.
         * @param setEndIndexExclusive Set ending index for parallel traversal.
         * @param setInput Input values
         */
        ReciprocalArraySumTask(final int setStartIndexInclusive,
                final int setEndIndexExclusive, final double[] setInput) {
            this.startIndexInclusive = setStartIndexInclusive;
            this.endIndexExclusive = setEndIndexExclusive;
            this.input = setInput;
        }

        /**
         * Getter for the value produced by this task.
         * @return Value produced by this task
         */
        public double getValue() {
            return value;
        }

        @Override
        protected void compute() {
            // TODO

            /*
            if (endIndexExclusive - startIndexInclusive <= 1000) {
                for(int m = startIndexInclusive; m < endIndexExclusive; m++) {
                    value += 1/input[m];
                }
            }
            else {
                ReciprocalArraySumTask task1 = new ReciprocalArraySumTask(startIndexInclusive, endIndexExclusive/2, input);
                ReciprocalArraySumTask task2 = new ReciprocalArraySumTask(endIndexExclusive/2, endIndexExclusive, input);
                task1.fork();
                task2.compute();
                task1.join();
                this.value = task1.getValue() + task2.getValue();
            }
            */

            // Compute() version for parArraySum only
            for(int m = startIndexInclusive; m < endIndexExclusive; m++) {
                value += 1/input[m];
            }

        }
    }

    /**
     * TODO: Modify this method to compute the same reciprocal sum as
     * seqArraySum, but use two tasks running in parallel under the Java Fork
     * Join framework. You may assume that the length of the input array is
     * evenly divisible by 2.
     *
     * @param input Input array
     * @return The sum of the reciprocals of the array input
     */
    protected static double parArraySum(final double[] input) {
        assert input.length % 2 == 0;

        /*
        double sum = 0;

        // Compute sum of reciprocals of array elements
        for (int i = 0; i < input.length; i++) {
            sum += 1 / input[i];
        }

        return sum;
        */

        /*
        ForkJoinPool pool = new ForkJoinPool(2);
        ReciprocalArraySumTask t = new ReciprocalArraySumTask(0, input.length, input);
        pool.invoke(t);
        double sum = t.getValue();
        return sum;
        */

        ReciprocalArraySumTask task1 = new ReciprocalArraySumTask(0, input.length/2, input);
        ReciprocalArraySumTask task2 = new ReciprocalArraySumTask(input.length/2, input.length, input);
        task1.fork();
        task2.compute();
        task1.join();
        return task1.getValue() + task2.getValue();

        /*
        double sum1 = 0;
        double sum2 = 0;

        finish(() -> {
            async(() -> {
            for (int m = 0; m < input.length/2; m++) {
                sum1 += 1/input[m];
            }});

        for (int m = input.length/2 + 1; m < input.length; m++) {
            sum2 += 1/input[m];
        }});

        double sum = sum1 + sum2;
        return sum;
        */


    }

    /**
     * TODO: Extend the work you did to implement parArraySum to use a set
     * number of tasks to compute the reciprocal array sum. You may find the
     * above utilities getChunkStartInclusive and getChunkEndExclusive helpful
     * in computing the range of element indices that belong to each chunk.
     *
     * @param input Input array
     * @param numTasks The number of tasks to create
     * @return The sum of the reciprocals of the array input
     */
    protected static double parManyTaskArraySum(final double[] input,
            final int numTasks) {
        double sum = 0;

        /*
        // Compute sum of reciprocals of array elements
        for (int i = 0; i < input.length; i++) {
            sum += 1 / input[i];
        }
        */

        int startIndex, endIndex;

        ForkJoinPool pool = new ForkJoinPool(numTasks);
        ReciprocalArraySumTask[] taskArray = new ReciprocalArraySumTask[numTasks];

        for (int m = 0; m < numTasks; m++) {
            startIndex = getChunkStartInclusive(m, numTasks, input.length);
            endIndex = getChunkEndExclusive(m, numTasks, input.length);
            taskArray[m] = new ReciprocalArraySumTask(startIndex, endIndex, input);
            if (m < numTasks - 1) {
                taskArray[m].fork();
            }
            else {
                taskArray[m].compute();
            }
        }

        /*
        for (int m = 0; m < numTasks - 1; m++) {
            taskArray[m].fork();
        }

        taskArray[numTasks-1].compute();

        for (int m = 0; m < numTasks - 1; m++) {
            taskArray[m].join();
        }
        */

        for (int m = 0; m < numTasks; m++) {
            if (m < numTasks - 1) {
                taskArray[m].join();
            }
            sum += taskArray[m].getValue();
        }

        /*
        ArrayList<ReciprocalArraySumTask> reciprocalArraySumTaskList = new ArrayList<>();
        for(int i=0;i<numTasks;i++){
            int start = getChunkStartInclusive(i,numTasks,input.length);
            int end = getChunkEndExclusive(i,numTasks,input.length);
            reciprocalArraySumTaskList.add(new ReciprocalArraySumTask(start,end,input));
        }
        ForkJoinTask.invokeAll(reciprocalArraySumTaskList);
        for(ReciprocalArraySumTask rast : reciprocalArraySumTaskList){
            sum += rast.getValue();
        }
        */


        return sum;
    }
}
