# Notes for [Parallel, Concurrent, and Distributed Programming in Java Specialization](https://www.coursera.org/specializations/pcdp)

## Course 1 [Parallel Programming in Java](https://www.coursera.org/learn/parallel-programming-in-java/home/welcome)

### Week 1

Use computation graph to have a better representation of the computation progress.

Computation graph consists of (1) A set of vertices or nodes, in which each node represents a step consisting of an arbitrary sequential computation. (2) A set of directed edges that represent ordering constraints among steps.

Data race: two process in parallel visit the same data block.

Metrics to measure the performance of an algorithm:
**Work:** total time for all nodes on the computation graph.
**Span:** time of the longest path on the computation graph.
**Ideal parallelism:** result of a division: work/span. For sequential computing, ideal parallelism is 1. When this value is very large, we should consider multi-core processor or other parallel computing hardware.

**Tp:** execution time on `p` processors. `T_infinite` <= `Tp` <= `T1`.
**SpeedUp(p)** = `T1/Tp`, and it must be <= processor number `p`.

**Amdahl's law:** if q ≤ 1 is the fraction of WORK in a parallel program that must be executed *sequentially*, then the best speedup that can be obtained for that program for any number of processors, P , is Speedup(P)≤ 1/q. Amdahl’s Law reminds us to watch out for sequential bottlenecks both when designing parallel algorithms and when implementing programs on real machines.  As an example, if q = 10%, then Amdahl's Law reminds us that the best possible speedup must be ≤ 10 (which equals 1/q ), regardless of the number of processors available.

### Week 2

Asynchronous tasks could be converted to *future tasks* and *future objects* (a.k.a *promise objects*).
**Future tasks:** tasks with return values.
**Future objects:** a handle for accessing a task's return value.

There are 2 key operations could be performed on a future object `A`:
**(1) Assignment:** `A` can be assigned a reference to a future object returned by a task of the form `future{<task-with-return-value>}`. The content of the future object is constrained to be single assignement, and cannot be modified after the future task has returned (similar to a final variable in Java).
**(2) Blocking read:** the operation `A.get()` waits until the task associated with the future object `A` has completed, and then propagates the task's return value as the value returned by `A.get()`. Any statement `S` executed after `A.get()` is assured that the task associated with future object `A` must have completed before `S` starts execution.
These operations are carefully defined to avoid the possibility of a data race on a task's return value, which is why futures are well suited for functional parallelism.

In order to express future tasks in Java's fork/join (FJ) framework, we need to note three aspects:
(1) Extends the `RecursiveTask` class in the FJ framework, instead of `RecursiveAction` that was used in the previous week.
(2) The `compute()` method of a future task must have a non-void return type, whereas it has a void return type for regular tasks.
(3) A method call such as `left.join()` waits for the task referred to by object `left` in both cases, but also provides the task's return value in the case of future tasks.

The memorization used in dynamic programming (DP) could also be extended to future tasks. The lecture used the calculation of Pascal's triangle (binomial coefficients) as example to motivate memorization. The memoization pattern lends itself easily to parallelization using futures by modifying the memoized data structure to store `{(x1, y1 = future(f(x1))), (x2, y2 = future(f(x2))), ...}`. The lookup operation can then be replaced by a get() operation on the future value, if a future has already been created for the result of a given input.

The concept of Stream, introduced in Java 8, was also explained. An aggregate data query or data transformation can be specified by building a stream  pipeline consisting of a source (typically by invoking the `.stream()` method on a data collection, a sequence of *intermediate operations* such as `map()` and `filter()`, and an optional *terminal operation* such as `forEach()` or `average()`.
Note that: (1) all *intermeiate operations* returns a `Stream` so they could be chaned to form a pipeline. (2) no work is actually done until a *terminal operation* is invoked; it will start processing the pipeline to return a result (something that is other than a `Stream`).
***The most important difference between streams and collections is when things are computed.*** A collection is an in-memory data structure, which holds all the values that the data structure currently has—every element in the collection has to be computed before it can be added to the collection. In contrast, a stream is a conceptually fixed data structure in which elements are computed on demand.
An example of stream:
``` Java
students.stream()
    .filter(s -> s.getStatus() == Student.ACTIVE)
    .mapToInt(a -> a.getAge())
    .average();
```
To make the pipeline executed *in parallel*, we just simply replace `students.stream()` by `students.parallelStream()` or `Stream.of(students).parallel`.
To learn more about stream in Java:
[1. Article on “Processing Data with Java SE 8 Streams”](http://www.oracle.com/technetwork/articles/java/ma14-java-se-8-streams-2177646.html)
[2. Tutorial on specifying Aggregate Operations using Java streams](https://docs.oracle.com/javase/tutorial/collections/streams/)
[3. Documentation on java.util.stream.Collectors class for performing reductions on streams](http://docs.oracle.com/javase/8/docs/api/java/util/stream/Collectors.html)

#### **Data Races and Determinism**
A parallel program is said to be **functionally deterministic** if it always computes the **same answer** when given the same input, and **structurally deterministic** if it always computes the **same computation graph** when given the same input.
The presence of data races often leads to functional and/or structural nondeterminism because a parallel program with data races may exhibit different behaviors for the same input, depending on the relative scheduling and timing of memory accesses involved in a data race. **In general, the absence of data races is not sufficient to guarantee determinism.** However, all the parallel constructs introduced in this course (“Parallelism”) were carefully selected to ensure the following Determinism Property: If a parallel program is written using the constructs introduced in this course and is guaranteed to never exhibit a data race, then it must be both functionally and structurally deterministic.
Note that the determinism property states that all data-race-free parallel programs written using the constructs introduced in this course are guaranteed to be deterministic, **but it does not imply that a program with a data race must be functionally/structurally non-deterministic.** Furthermore, there may be cases of “benign” nondeterminism for programs with data races in which different executions with the same input may generate different outputs, but all the outputs may be acceptable in the context of the application, e.g., different locations for a search pattern in a target string.

### Week 3

#### **Parallel Loops**
We may regard each iteration in the *counted-for* loop as an async task, with a finish construct encompassing all iterations:

```
finish {
    for (p = head; p != null ; p = p.next) {
        async compute(p);
    }
}
```

We may also use *forall* notation for expressing parallel *counted-for* loops abovementioned:

```
forall (i : [0:n-1]) {
    a[i] = b[i] + c[i]    
} 
```

The one output array, such as the example above, could be represented as an elegant way of stream :

``` Java
a = IntStream.rangeClosed(0, N-1)
    .parallel()
    .toArray(i -> b[i] + c[i]);
```

In summary, streams are a convenient notation for parallel loops with at most one output array, but the forall notation is more convenient for loops that create/update multiple output arrays, as is the case in many scientific computations. **For generality, we will use the forall notation for parallel loops in the remainder of this module.**

#### **Parallel Matrix Multiplication**

The matrix multiplication was used as an example to illustrate how to determine which part of the program should be parallelized, and which should be kept in serialism.

The matrix multiplication program looks like:

```Java
for(i : [0:n-1]) {
  for(j : [0:n-1]) {
        c[i][j] = 0;
        for(k : [0:n-1]) {
          c[i][j] = c[i][j] + a[i][k]*b[k][j]
        }
    }
}
```

Upon a close inspection, we can see that it is safe to convert *for-i* and *for-j* into forall loops, but *for-k* must remain a sequential loop **to avoid data races**.  There are some trickier ways to also exploit parallelism in the *for-k* loop, but they rely on the observation that summation is algebraically associative even though it is computationally non-associative.

#### **Barriers in Parallel Loops**

The barrier in forall parallel loop is used to define the different phases in the loop body, such that different phase will be executed sequentially in order.

For example, consider the following code block:

```
forall (i : [0:n-1]) {
    myId = lookup(i); // convert int to a string 
    print HELLO, myId;
    print BYE, myId;
}
```
Since the loop body is run in parallel, the HELLOs and the BYEs may be printed out mixed in arbitrary order. If we want to print all BYEs after all HELLOs, we can add a **barrier** between `print HELLO, myId;` and `print BYE, myId;`.

Thus, barriers extend a parallel loop by dividing its execution into a sequence of phases. While it may be possible to write a separate *forall* loop for each phase, it is both more convenient and more efficient to instead insert barriers in a single forall loop, e.g., we would need to create an intermediate data structure to communicate the `myId` values from one *forall* to another *forall* if we split the above *forall* into two (using the notation next) loops. Barriers are a fundamental construct for parallel loops that are used in a majority of real-world parallel applications.

#### **One-Dimensional Iterative Averaging**

We discussed a simple stencil computation to solve the recurrence, X_i = (X_{i−1} + X_{i+1})/2 with boundary conditions, X_0 = 0 and X_n = 1. Though we can easily derive an analytical solution for this example, (X_i = i/n), most stencil codes in practice do not have known analytical solutions and rely on computation to obtain a solution.

The Jacobi method for solving such equations typically utilizes two arrays, oldX[] and newX[]. A naive approach to parallelizing this method would result in the following pseudocode:

```
for (iter: [0:nsteps-1]) {
  forall (i: [1:n-1]) {
    newX[i] = (oldX[i-1] + oldX[i+1]) / 2;
  }
  swap pointers newX and oldX;
}
```

Though easy to understand, this approach creates *nsteps × (n − 1)* tasks, which is too many. **Barriers** can help reduce the number of tasks created as follows:

```
forall ( i: [1:n-1]) {
  localNewX = newX; localOldX = oldX;
  for (iter: [0:nsteps-1]) {
    localNewX[i] = (localOldX[i-1] + localOldX[i+1]) / 2;
    NEXT; // Barrier
    swap pointers localNewX and localOldX;
  }
}
```

In this case, only *(n − 1)* tasks are created, and there are *n* steps barrier (next) operations, each of which involves all *(n − 1)* tasks. This is a significant improvement since creating tasks is usually more expensive than performing barrier operations.

#### **Iteration Grouping: Chunking of Parallel Loops**

Consider the following parallel loops for vector addition:

```
forall (i : [0:n-1]) {
    a[i] = b[i] + c[i]
}
```

We observed that this approach creates *n* tasks, one per *forall* iteration, which is wasteful when (as is common in practice) *n* is **much larger than the number of available processor cores**.

To  address this problem, we learned a common tactic used in practice that is referred to as loop chunking or iteration grouping, and focuses on reducing the number of tasks created to be closer to the number of processor cores, so as to reduce the overhead of parallel execution. With iteration grouping/chunking, the parallel vector addition example above can be rewritten as follows:

```
forall (g:[0:ng-1]) {
    for (i : mygroup(g, ng, [0:n-1])) {
        a[i] = b[i] + c[i]
    }
}
```

Note that we have reduced the degree of parallelism from *n* to the number of groups, *ng*, which now equals the number of  iterations/tasks in the *forall* construct.

There are two well known approaches for iteration grouping: *block* and *cyclic*. The former approach (*block*) maps consecutive iterations to the same group, whereas the latter approach (*cyclic*) maps iterations in the same congruence class (mod *ng*) to the same group. With these concepts, you should now have a better understanding of how to execute forall loops in practice with lower overhead.

For convenience, the PCDP library provides helper methods, *forallChunked()* and *forall2dChunked()*, that automatically create one-dimensional or two-dimensional parallel loops, and also perform a block-style iteration grouping/chunking on those parallel loops. An example of using the *forall2dChunked()* API for a two-dimensional parallel loop (as in the matrix multiply example) can be seen in the following Java code sketch:

```
forall2dChunked(0, N - 1, 0, N - 1, (i, j) -> {
    . . . // Statements for parallel iteration (i,j)
});
```

### Week 4 Dataflow Synchronization and Pipelining

#### **Split-phase Barriers with Java Phasers**

The barrier (in Java, the `next()` function) requires some execution time, comparatively much larger than normal execusion. However we may need to know that there are two stages inside barrier/`next()` function, which are *arrive* and *await and advance*, and are separable.

Consider the example:

```
forall (i : [0:n-1]) { 
  print HELLO, i;
  myId = lookup(i); // convert int to a string 
  print BYE, myId;
}
```

In Java’s Phaser class, the operation `ph.arriveAndAwaitAdvance()` can be used to implement a barrier through phaser object `ph`.  We  also observed that there are two possible positions for inserting a barrier between the two print statements above — before or after the call to `lookup(i)`. However, upon closer examination, we can see that the call to `lookup(i)` is local to iteration `i` and that there is no specific need to either complete it before the barrier or to complete it after the barrier.  In fact, the call to `lookup(i)` can be performed in parallel with the barrier. To facilitate this split-phase barrier (also known as a fuzzy barrier) we use two separate APIs from Java Phaser class — `ph.arrive()` and `ph.awaitAdvance()`. Together these two APIs form a barrier, but we now have the freedom to insert a computation such as `lookup(i)` between the two calls as follows:

```
// initialize phaser ph	for use by n tasks ("parties") 
Phaser ph = new Phaser(n);
// Create forall loop with n iterations that operate on ph 
forall (i : [0:n-1]) {
  print HELLO, i;
  int phase = ph.arrive();
  
  myId = lookup(i); // convert int to a string

  ph.awaitAdvance(phase);
  print BYE, myId;
}
```

Doing so enables the *barrier* processing to occur in parallel with the call to `lookup(i)`, which was our desired outcome.

#### **Point-to-Point Synchronization with Phasers**

It is possible to specify the dependence between asynchronized tasks. For example, in the following three tasks, Task 0 only depends on Task 1; Task 1 depends on both Task 0 and 2; Task 2 depends only on Task 1:

|     Task 0    |      Task 1     |     Task 2    |
|---------------|-----------------|---------------|
| X=A();//cost=1|  Y=B();//cost=2 |Z=C();//cost=3 |
|D(X,Y);//cost=3|E(X,Y,Z);//cost=2|F(Y,Z);//cost=1|


In this example, the span (critical path length) would be 6 units of time if we used a barrier (`next`), but is reduced to 5 units of time if we use individual phasers as shown in the following table:

|       Task 0       |       Task 1       |       Task 2       |
|--------------------|--------------------|--------------------|
|    X=A();//cost=1  |   Y=B();//cost=2   |   Z=C();//cost=3   |
|    ph0.arrive();   |    ph1.arrive();   |    ph2.arrive();   |
|ph1.awaitAdvance(0);|ph0.awaitAdvance(0);|ph1.awaitAdvance(0);|
|   D(X,Y);//cost=3  |ph2.awaitAdvance(0);|   F(Y,Z);//cost=1  |
|                    | E(X,Y,Z);//cost=2  |                    |

Each column in the table represents execution of a separate task, and the calls to `arrive()` and `awaitAdvance(0)` represent synchronization across different tasks via phaser objects, `ph0`, `ph1`, and `ph2`, each of which is initialized with a party count of 1 (only one signalling task). (The parameter 0 in `awaitAdvance(0)` represents a transition from phase 0 to phase 1.)

Note: **In the first step (the `arrive()` API), each thread signals to other threads that it has reached a point in its own execution where it is safe for any other thread to proceed past the barrier. In the second step (the `awaitAdvance()` API), each thread checks that all other threads have signaled that they have reached step one to be sure it can proceed past the barrier.**

#### **One-Dimensional Iterative Averaging with Phasers**

In the parallel version of the barrier-based Iterative Averaging example showed in the last week (`x[i] = (x[i-1] + x[i+1])/2`), a full barrier (before swapping the new and old arrays) is not necessary since `forall` iteration `i` only needs to wait for iterations `i − 1` and `i + 1` to complete their current phase before iteration `i` can move to its next phase. This idea can be captured by phasers, if we allocate an array of phasers as follows:

``` Java
// Allocate array of phasers
Phaser[] ph = new Phaser[n+2]; //array of phasers
for (int i = 0; i < ph.length; i++) ph[i] = new Phaser(1);

// Main computation 
forall ( i: [1:n-1]) {
  for (iter: [0:nsteps-1]) {
    newX[i] = (oldX[i-1] + oldX[i+1]) / 2;
    ph[i].arrive();
    
    if (index > 1) ph[i-1].awaitAdvance(iter);
    if (index < n-1) ph[i + 1].awaitAdvance(iter); 
    swap pointers newX and oldX;
  }
}
```

As we learned earlier, grouping/chunking of parallel iterations in a *forall* can be an important consideration for performance (due to reduced overhead). The idea of grouping of parallel iterations can be extended to *forall* loops with phasers as follows:

```Java
// Allocate array of phasers proportional to number of chunked tasks 
Phaser[] ph = new Phaser[tasks+2]; //array of phasers
for (int i = 0; i < ph.length; i++) ph[i] = new Phaser(1);

// Main computation 
forall ( i: [0:tasks-1]) {
  for (iter: [0:nsteps-1]) {
    // Compute leftmost boundary element for group
    int left = i * (n / tasks) + 1;
    myNew[left] = (myVal[left - 1] + myVal[left + 1]) / 2.0;
    
    // Compute rightmost boundary element for group 
    int right = (i + 1) * (n / tasks);
    myNew[right] = (myVal[right - 1] + myVal[right + 1]) / 2.0;
    
    // Signal arrival on phaser ph AND LEFT AND RIGHT ELEMENTS ARE AV 
    int	index = i + 1;
    ph[index].arrive();
    
    // Compute interior elements in parallel with barrier 
    for (int j = left + 1; j <= right - 1; j++)
      myNew[j] = (myVal[j - 1] + myVal[j + 1]) / 2.0;
    // Wait for previous phase to complete before advancing 
    if (index > 1) ph[index - 1].awaitAdvance(iter);
    if (index < tasks) ph[index + 1].awaitAdvance(iter);
    swap pointers newX and oldX;
  }
}
```

#### **Pipeline Parallelism**

Point-to-point synchronization could be used to build a 1D pipeline with *p* tasks T(0), T(1), ... T(p-1). We can use three stages for medical imaging processing as example: **denoising**, **registration** and **segmentation** (i.e. *p*=3). With *n* images, the `WORK = n*p` and critical path length `CPL = n + p - 1`, so the ideal parallelism `PAR = n*p/(n+p-1)`. When *n* is much larger than *p*, `PAR = p` as the limit.

The synchronization required for pipeline parallelism can be implemented using phasers by allocating an array of phasers, such that phaser `ph[i]` is "signalled" in iteration `i` by a call to `ph[i].arrive()` as follows:

```
// Code for pipeline stage i
while ( there is an input to be processed ) {
  // wait for previous stage, if any 
  if (i > 0) ph[i - 1].awaitAdvance(); 
  
  process input;
  
  // signal next stage
  ph[i].arrive();
}
```

#### **Data Flow Parallelism**

Thus far, we have studied computation graphs as structures that are derived from parallel programs. In this section, we studied a dual approach advocated in the data flow parallelism model, which is to specify parallel programs as computation graphs. The simple data flow graph studied in the lecture consisted of five nodes and four edges: A → C, A → D, B → D, B → E, or:

```
  A   B
 / \ / \
C   D   E
```

While futures can be used to generate such a computation graph, e.g., by including calls to `A.get()` and `B.get()` in task D, the computation graph edges are implicit in the get() calls when using *futures*.  Instead, we introduced the *asyncAwait* notation to specify a task along with an explicit set of preconditions (events that the task must wait for before it can start execution). With this approach, the program can be generated directly from the computation graph as follows:

```
async( () -> {/* Task A */; A.put(); } ); // Complete task and trigger event A
async( () -> {/* Task B */; B.put(); } ); // Complete task and trigger event B
asyncAwait(A, () -> {/* Task C */} );    // Only execute task after event A is triggered 
asyncAwait(A, B, () -> {/* Task D */} );  // Only execute task after events A, B are triggered 
asyncAwait(B, () -> {/* Task E */} );    // Only execute task after event B is triggered
```

Interestingly, the order of the above statements is not significant. Just as a graph can be defined by enumerating its edges in any order, the above data flow program can be rewritten as follows, without changing its meaning:

```
asyncAwait(A, () -> {/* Task C */} );    // Only execute task after event A is triggered 
asyncAwait(A, B, () -> {/* Task D */} );  // Only execute task after events A, B are triggered 
asyncAwait(B, () -> {/* Task E */} );    // Only execute task after event B is triggered 
async( () -> {/* Task A */; A.put(); } ); // Complete task and trigger event A
async( () -> {/* Task B */; B.put(); } ); // Complete task and trigger event B
```

Finally, we observed that the power and elegance of data flow parallel programming is accompanied by **the possibility of a lack of progress that can be viewed as a form of "deadlock" if the program omits a *put()* call for signalling an event**.


## Course 2 [Concurrent Programming in Java](https://www.coursera.org/learn/concurrent-programming-in-java/home/welcome)

### Week 1

#### **Java Threads**
In this lecture, we learned the concept of threads as lower-level building blocks for concurrent programs. A unique aspect of Java compared to prior mainstream programming languages is that Java included the notions of threads (as instances of the `java.lang.Thread` class) in its language definition right from the start.

When an instance of *Thread* is created (via a `new` operation), it does not start executing right away; instead, it can only start executing when its `start()` method is invoked. The statement or computation to be executed by the thread is specified as a parameter to the constructor.

The `Thread` class also includes a wait operation in the form of a `join()` method. If thread *t0* performs a `t1.join()` call, thread *t0* will be forced to wait until thread *t1* completes, after which point it can safely access any values computed by thread *t1*. Since there is no restriction on which thread can perform a `join` on which other thread, it is possible for a programmer to erroneously create a **deadlock** cycle with `join` operations. (A **deadlock** occurs when two threads wait for each other indefinitely, so that neither can make any progress.)

#### **Structured Locks**

*Structured locks* could be used to enforce *mutual exclusion* to avoid *data races*. A major benefit of structured locks is that their acquire and release operations are implicit, since these operations are automatically performed by the Java runtime environment when entering and exiting the scope of a `synchronized` statement or method, even if an exception is thrown in the middle.

We also learned about `wait()` and `notify()` operations that can be used to block and resume threads that need to wait for specific conditions. For example, a producer thread performing an `insert()` operation on a bounded buffer can call `wait()` when the buffer is full, so that it is only unblocked when a consumer thread performing a `remove()` operation calls `notify()`. Likewise, a consumer thread performing a `remove()` operation on a bounded buffer can call `wait()` when the buffer is empty, so that it is only unblocked when a producer thread performing an `insert()` operation calls `notify()`. *Structured locks* are also referred to as **intrinsic locks** or **monitors**.

#### **Unstructured Locks**

In this lecture, we introduced unstructured locks (which can be obtained in Java by creating instances of `ReentrantLock()`, and used three examples to demonstrate their generality relative to structured locks. The first example showed how explicit `lock()` and `unlock()` operations on unstructured locks can be used to support a hand-over-hand locking pattern that implements a non-nested pairing of lock/unlock operations which cannot be achieved with synchronized statements/methods. The second example showed how the `tryLock()` operations in unstructured locks can enable a thread to check the availability of a lock, and thereby acquire it if it is available or do something else if it is not. The third example illustrated the value of read-write locks (which can be obtained in Java by creating instances of `ReentrantReadWriteLock()`, whereby multiple threads are permitted to acquire a lock `L` in "read mode", `L.readLock().lock()`, but only one thread is permitted to acquire the lock in "write mode", `L.writeLock().lock()`.

However, it is also important to remember that the generality and power of unstructured locks is accompanied by an extra responsibility on the part of the programmer, e.g., ensuring that calls to `unlock()` are not forgotten, even in the presence of exceptions.

#### **Liveness and Progress Guarantees**

There are three ways in which a parallel program may enter a state in which it stops making forward progress. For sequential programs, an *infinite loop* is a common way for a program to stop making forward progress, but there are other ways to obtain an absence of progress in a parallel program. The first is **deadlock**, in which all threads are blocked indefinitely, thereby preventing any forward progress. The second is **livelock**, in which all threads repeatedly perform an interaction that prevents forward progress, e.g., an infinite "loop" of repeating lock acquire/release patterns. The third is **starvation**, in which at least one thread is prevented from making any forward progress.

#### **Dining Philosophers Problem**

In this lecture, we studied a classical concurrent programming example that is referred to as the *Dining Philosophers Problem*. In this problem, there are five threads, each of which models a "philosopher" that repeatedly performs a sequence of actions which include think, pick up chopsticks, eat, and put down chopsticks.

First, we examined a solution to this problem using *structured locks*, and demonstrated how this solution could lead to a deadlock scenario (but not livelock). Second, we examined a solution using *unstructured locks* with `tryLock()` and `unlock()` operations that never block, and demonstrated how this solution could lead to a *livelock scenario* (but not deadlock). Finally, we observed how a simple modification to the first solution with structured locks, **in which one philosopher picks up their right chopstick and their left**, while the others pick up their left chopstick first and then their right, can guarantee an absence of deadlock.


### Week 2

#### **Critical Selections**

In this lecture, we learned how critical sections and the isolated construct can help concurrent threads manage their accesses to shared resources, at a higher level than just using locks. When programming with threads, it is well known that the following situation is defined to be a data race error — when two accesses on the same shared location can potentially execute in parallel, with least one access being a write. However, there are many cases in practice when two tasks may legitimately need to perform concurrent accesses to shared locations, as in the bank transfer example.

With critical sections, two blocks of code that are marked as isolated, say `A` and `B`, are guaranteed to be executed in mutual exclusion with `A` executing before `B` or vice versa. With the use of isolated constructs, it is impossible for the bank transfer example to end up in an inconsistent state because all the reads and writes for one isolated section must complete before the start of another isolated construct. Thus, the parallel program will see the effect of one isolated section completely before another isolated section can start.

#### **Object-based Isolation**

An example of two-way linked list was used to illustrate the concept of **Object-based isolation**, which generalizes the isolated construct and relates to the classical concept of monitors. Consider deleting nodes B, C, and E on three parallel threads:

A ⇄ B ⇄ C ⇄ D ⇄ E ⇄ F

In this case, we don't have to isolate all three threads, since the threads deleting node B and E are not having any data races with each other. We only need to block deleting threads of B and C, or C and E.

The fundamental idea behind object-based isolation is that an isolated construct can be extended with a set of objects that indicate the scope of isolation, by using the following rules: **if two isolated constructs have an empty intersection in their object sets they can execute in parallel**, otherwise they must execute in mutual exclusion. We observed that implementing this capability **can be very challenging with locks** because a correct implementation must enforce the correct levels of mutual exclusion without entering into deadlock or livelock states. The linked-list example showed how the object set for a `delete()` method can be defined as consisting of three objects — the current, previous, and next objects in the list, and that this object set is sufficient to safely enable parallelism across multiple calls to `delete()`.

The Java code sketch to achieve this object-based isolation using the PCDP library to resolve the blocking problem of the previous example is as follows:

```
isolated(cur, cur.prev, cur.next, () -> {
    . . . // Body of object-based isolated construct
});
```

The relationship between object-based isolation and monitors is that all methods in a monitor object, *M1*, are executed as object-based isolated constructs with a singleton object set, *{M1}*. Similarly, all methods in a monitor object, *M2*, are executed as object-based isolated constructs with a singleton object set, *{M2}* which has an empty intersection with *{M1}*.

#### **Spanning Tree Example**

In this lecture, we learned how to use object-based isolation to create a parallel algorithm to compute spanning trees for an undirected graph. Recall that a *spanning tree* specifies a subset of edges in the graph that form a tree (no cycles), and connect all vertices in the graph (*Definition in Wikipedia:  a spanning tree T of an undirected graph G is a subgraph that is a tree which includes all of the vertices of G, with a minimum possible number of edges*). A standard recursive method for creating a spanning tree is to perform a depth-first traversal of the graph (the `Compute(v)` function in our example), making the current vertex a parent of all its neighbors that don’t already have a parent assigned in the tree (the `MakeParent(v, c)` function in the example).

The approach described in this lecture to parallelize the spanning tree computation executes recursive `Compute(c)` method calls in parallel for all neighbors, `c`, of the current vertex, `v`. Object-based isolation helps avoid a data race in the `MakeParent(v,c)` method, when two parallel threads might attempt to call `MakeParent(v1, c)` and `MakeParent(v2, c)` on the same vertex `c` at the same time. In this example, the role of object-based isolation is to ensure that all calls to `MakeParent(v,c)` with the same `c` value must execute the object-based isolated statement in *mutual exclusion*, whereas calls with different values of `c` can proceed in parallel.

#### **Atomic Variables**

In this lecture, we studied *Atomic Variables*, an important special case of object-based isolation which can be very efficiently implemented on modern computer systems. In the example given in the lecture, we have multiple threads processing an array, each using object-based isolation to safely increment a shared object, `cur`, to compute an index `j` which can then be used by the thread to access a thread-specific element of the array.

However, instead of using object-based isolation, **we can declare the index `cur` to be an Atomic Integer variable and use an atomic operation called `getAndAdd()` to atomically read the current value of cur and increment its value by 1**. Thus, `j=cur.getAndAdd(1)` has the same semantics as `isolated(cur) {j=cur;cur=cur+1;}` but is implemented much more efficiently using hardware support on today’s machines.

Another example that we studied in the lecture concerns *Atomic Reference Variables*, which are **reference variables that can be atomically read and modified** using methods such as `compareAndSet()`. If we have an atomic reference `ref`, then the call to `ref.compareAndSet(expected, new)` will compare the value of `ref` to `expected`, and if they are the same, set the value of `ref` to `new` and return `true`. This all occurs in **one atomic operation** that cannot be interrupted by any other methods invoked on the `ref` object. If `ref` and `expected` have different values, `compareAndSet()` will not modify anything and will simply return `false`.

#### **Read-Write Isolation**

In this lecture we discussed *Read-Write Isolation*, which is a refinement of object-based isolation, and is a higher-level abstraction of the *read-write locks* studied earlier as part of Unstructured Locks. The main idea behind *read-write isolation* is to **separate read accesses to shared objects from write accesses**. This approach enables two threads that only read shared objects to freely execute in parallel since they are not modifying any shared objects. **The need for mutual exclusion only arises when one or more threads attempt to enter an isolated section with write access to a shared object**.

This approach exposes more concurrency than object-based isolation since **it allows read accesses to be executed in parallel**. In the *doubly-linked list* example from our lecture, when deleting an object `cur` from the list by calling `delete(cur)`, we can replace object-based isolation on `cur` with read-only isolation, since deleting an object does not modify the object being deleted; only the previous and next objects in the list need to be modified.

### Week 3

#### **Actors**

In this lecture, we introduced the *Actor Model* as an even higher level of concurrency control than locks or isolated sections. One limitation of locks, and even isolated sections, is that, while many threads might correctly control the access to a shared object (e.g., by using object-based isolation) **it only takes one thread that accesses the object directly to create subtle and hard-to-discover concurrency errors**. The *Actor* model avoids this problem by forcing all accesses to an object to be isolated by default. **The object is part of the local state of an actor, and cannot be accessed directly by any other actor**.

An Actor consists of a *Mailbox*, a set of *Methods*, and *Local State*. The *Actor* model is reactive, in that actors can only execute methods in response to messages; these methods can read/write local state and/or send messages to other actors. Thus, the only way to modify an object in a pure actor model is to send messages to the actor that owns that object as part of its local state. In general, messages sent to actors from different actors can be arbitrarily reordered in the system. However, in many actor models, messages sent between the same pair of actors preserve the order in which they are sent.

#### **Actor Examples**

In this lecture, we further studied the *Actor Model* through two simple examples of using actors to implement well-known concurrent programming patterns. The `PrintActor` in our first example processes simple String messages by printing them. If an *EXIT* message is sent, then the `PrintActor` completes its current computation and exits. As a reminder, we assume that messages sent between the same pair of actors preserve the order in which they are sent:

``` Java
process(s) {
    if (s == "EXIT") {
        EXIT();
    } else {
        print s;
    }
}
```

In the second example, we created an *actor pipeline*, in which one actor checks the incoming messages and only forwards the ones that are in lower case. The second actor processes the lowercase messages and only forwards the ones that are of even length. This example illustrates the power of the actor model, as **this concurrent system would be much more difficult to implement using threads**, for example, since **much care would have to be taken on how to implement a shared mailbox for correct and efficient processing by parallel threads**.

#### **Sieve of Eratosthenes**

*Sieve of Eratosthenes* is an algorithms to produce all prime numbers less than a given number `N`. To implement the this algorithm, we first create an actor, *Non-Mul-2*, that receives (positive) natural numbers as input (up to some limit), and then filters out the numbers that are multiples of 2. After receiving a number that is not a multiple of 2 (in our case, the first would be 3), the *Non-Mul-2* actor creates the next actor in the pipeline, Non-Mul-3, with the goal of discarding all the numbers that are multiples of 3. The *Non-Mul-2* actor then forwards all non-multiples of 2 to the *Non-Mul-3* actor. Similarly, this new actor will create the next actor in the pipeline, *Non-Mul-5*, with the goal of discarding all the numbers that are multiples of 5. The power of the *Actor Model* is reflected in the dynamic nature of this problem, where pieces of the computation (new actors) are created dynamically as needed.

Here is an example of Java code for the `process()` method for an actor responsible for filtering out multiple of actor's "local prime" in the *Sieve of Eratosthens*:

``` Java
public void process(final Object msg) {
  int candidate = (Integer) msg;
  // Check if the candidate is a non-multiple of the "local prime".
  // For example, localPrime = 2 in the Non-Mul-2 actor
  boolean nonMul = ((candidate % localPrime) != 0);
  // nothing needs to be done if nonMul = false
  if (nonMul) {
    if (nextActor == null) { 
      . . . // create & start new actor with candidate as its local prime
    }
    else nextActor.send(msg); // forward message to next actor
  } 
} // process
```

#### **Producer-Consumer Problem with Unbounded Buffer**

In this lecture, we studied the *producer-consumer* pattern in concurrent programming which is used to solve the following classical problem: how can we safely coordinate accesses by multiple producer tasks: P1, ​P2, P3, ... and multiple consumer tasks, C1, C2, C3, ... to a shared buffer of *unbounded size* without giving up any concurrency? Part of the reason that this problem can be challenging is that **we cannot assume any a priori knowledge about the rate at which different tasks produce and consume items in the buffer**. While it is possible to solve this problem by using locks with wait-notify operations or by using object-based isolation, **both approaches will require low-level concurrent programming techniques to ensure correctness and maximum performance**. Instead, a more elegant solution can be achieved by using actors as follows.

The key idea behind any actor-based solution is to think of all objects involved in the concurrent program as actors.

1. To implement all producer tasks, consumer tasks, and the shared buffer as actors.

2. To establish the communication protocols among the actors.
(1) A producer actor can simply send a message to the buffer actor whenever it has an item to produce.
(2) A consumer actor sends a message to the buffer actor whenever it is ready to process an item. Thus, whenever the buffer actor receives a message from a producer, it knows which consumers are ready to process items and can forward the produced item to any one of them.

Thus, with the actor model, all concurrent interactions involving the buffer can be encoded in messages, instead of using locks or isolated statements.

#### **Producer-Consumer Problem with Bounded Buffer**

The previous producer-consumer problem is a simplified one since it's using a buffer with unlimited size. However, in practice, it is also important to consider the case with a bounded buffer size.

The main new challenge with bounding the size of the shared buffer is *to ensure that producer tasks are not permitted to send items to the buffer when the buffer is full*. Thus, **the buffer actor needs to play a master role in the protocol by informing producer actors when they are permitted to send data**. This is akin to the role played by the buffer/master actor with respect to consumer actors, even in the unbounded buffer case (in which the consumer actor informed the buffer actor when it is ready to consume an item). Now, *the producer actor will only send data when requested to do so by the buffer actor*. Though, this actor-based solution appears to be quite simple, it actually solves a classical problem that has been studied in advanced operating system classes for decades.

### Week 4

#### **Optimistic Concurrency**

In this lecture, we studied the optimistic concurrency pattern, which can be used to improve the performance of concurrent data structures. In practice, this pattern is most often used by experts who implement components of concurrent libraries, such as *AtomicInteger* and *ConcurrentHashMap*, but it is useful for all programmers to understand the underpinnings of this approach. As an example, we considered how the `getAndAdd()` method is typically implemented for a shared *AtomicInteger* object. The basic idea is to allow multiple threads to read the existing value of the shared object (curVal) without any synchronization, and also to compute its new value after the addition (newVal) without synchronization. These computations are performed optimistically under the assumption that no interference will occur with other threads during the period between reading `curVal` and computing `newVal`. However, it is necessary for each thread to confirm this assumption by using the `compareAndSet()` method as follows. (`compareAndSet()` is used as an important building block for optimistic concurrency because it is implemented very efficiently on many hardware platforms.)

The method call `A.compareAndSet(curVal, newVal)` invoked on *AtomicInteger* `A` checks that the value in `A` still equals `curVal`, and, if so, updates `A`’s value to `newVal` before returning true; otherwise, the method simply returns false without updating `A`. Further, the `compareAndSet()` method is guaranteed to be performed atomically, as if it was in an object-based isolated statement with respect to object A. Thus, if two threads, `T1` and `T2` call `compareAndSet()` with the same `curVal` that matches `A`’s current value, only one of them will succeed in updating `A` with their `newVal`. Furthermore, each thread will invoke an operation like `compareAndSet()` repeatedly in a loop until the operation succeeds. **This approach is guaranteed to never result in a deadlock since there are no blocking operations.** Also, since each call `compareAndSet()` is guaranteed to eventually succeed, **there cannot be a livelock either**. In general, so long as the contention on a single shared object like `A` is not high, the number of calls to `compareAndSet()` that return false will be very small, and the optimistic concurrency approach can perform much better in practice (but at the cost of more complex code logic) than using locks, isolation, or actors.

#### **Concurrent Queues**

In this lecture, we studied *concurrent queues*, an extension of the popular queue data structure to support concurrent accesses. The most common operations on a queue are `enq(x)`, which enqueues object `x` at the end (tail) of the queue, and `deq()` which removes and returns the item at the start (head) of the queue. A correct implementation of a concurrent queue must ensure that calls to `enq()` and `deq()` maintain the correct semantics, even if the calls are invoked concurrently from different threads. While it is always possible to use locks, isolation, or actors to obtain correct but less efficient implementations of a **concurrent queue**, this lecture illustrated how an expert might implement a more efficient concurrent queue using the optimistic concurrency pattern.

A common approach for such an implementation is to replace an object reference like *tail* by an *AtomicReference*. Since the `compareAndSet()` method can also be invoked on *AtomicReference* objects, we can use it to support (for example) concurrent calls to `enq()` by identifying which calls to `compareAndSet()` succeeded, and repeating the calls that failed. This provides the basic recipe for more efficient implementations of `enq()` and `deq()`, as are typically developed by concurrency experts.  A popular implementation of concurrent queues available in Java is *java.util.concurent.ConcurrentLinkedQueue*.

#### **Linearizability**

In this lecture, we studied an important correctness property of concurrent objects that is called *Linearizability*. A concurrent object is a data structure that is designed to support operations in parallel by multiple threads. The key question answered by *linearizability* is what return values are permissible when multiple threads perform these operations in parallel, taking into account what we know about the expected return values from those operations when they are performed sequentially. As an example, we considered two threads,`T1` and `T2`, performing `enq(x)` and `enq(y)` operations in parallel on a shared concurrent queue data structure, and considered what values can be returned by a `deq()` operation performed by `T2` after the call to `enq(y)`. From the viewpoint of *linearizability*, it is possible for the `deq()` operation to return item x or item y.

One way to look at the definition of *linearizability* is as though you are a lawyer attempting to "defend" a friend who implemented a concurrent data structure, and that all you need to do to prove that your friend is "not guilty" (did not write a buggy implementation) is to show one scenario in which **all the operations return values that would be consistent with a sequential execution by identifying logical moments of time at which the operations can be claimed to have taken effect**. Thus, if `deq()` returned item x or item y you can claim that either scenario is plausible because we can reasonably assume that `enq(x)` took effect before `enq(y)`, or vice versa. However, there is absolutely no plausible scenario in which the call to `deq()` can correctly return a code/exception to indicate that the queue is empty since at least `enq(y)` must have taken effect before the call to `deq()`. Thus, a goal for any implementation of a concurrent data structure is to ensure that all its executions are *linearizable* by using whatever combination of constructs (e.g., locks, isolated, actors, optimistic concurrency) is deemed appropriate to ensure correctness while giving the maximum performance.

#### **Concurrent HashMap**

In this lecture, we studied the `ConcurrentHashMap` data structure, which is available as part of the *java.util.concurrent* standard library in Java. A `ConcurrentHashMap` instance, `chm`, implements the `Map` interface, including the `get(key)` and `put(key, value)` operations. It also implements additional operations specified in the `ConcurrentMap` interface (which in turn extends the Map interface); one such operation is `putIfAbsent(key, value)`. The motivation for using `putIfAbsent()` is to ensure that only one instance of key is inserted in `chm`, even if multiple threads attempt to insert the same key in parallel. Thus, the semantics of calls to `get()`, `put()`, and `putIfAbsent()` can all be specified by the theory of *linearizability* studied earlier. **However, it is worth noting that there are also some aggregate operations**, such as `clear()` and `putAll()`, that cannot safely be performed in parallel with `put()`, `get()` and `putIfAbsent()`.

Motivated by the large number of concurrent data structures available in the *java.util.concurrent* library, this lecture advocates that, when possible, you use libraries such as `ConcurrentHashMap` rather than try to implement your own version.

#### **Minimum Spanning Tree Example**

In this lecture, we discussed how to apply concepts learned in this course to design a concurrent algorithm that solves the problem of finding a *minimum-cost spanning tree* (MST) for an undirected graph. It is well known that undirected graphs can be used to represent all kinds of networks, including roadways, train routes, and air routes. A spanning tree is a data structure that contains a subset of edges from the graph which connect all nodes in the graph without including a cycle. The cost of a spanning tree is computed as the sum of the weights of all edges in the tree.

The concurrent algorithm studied in this lecture builds on a well-known sequential algorithm that iteratively performs edge contraction operations, such that given a node `N1` in the graph, `GetMinEdge(N1)` returns an edge adjacent to `N1` with minimum cost for inclusion in the MST. If the minimum-cost edge is `(N1,N2)`, the algorithm will attempt to combine nodes `N1` and `N2` in the graph and replace the pair by a single node, `N3`. To perform edge contractions in parallel, we have to look out for the case when two threads may collide on the same vertex. For example, even if two threads started with vertices `A` and `D`, they may both end up with `C` as the neighbor with the minimum cost edge. **We must avoid a situation in which the algorithm tries to combine both `A` and `C` and `D` and `C`**. One possible approach is to use unstructured locks with calls to `tryLock()` to perform the combining safely, but without creating the possibility of *deadlock* or *livelock* situations. A key challenge with calling `tryLock()` is that some fix-up is required if the call returns false. Finally, it also helps to use a concurrent queue data structure to keep track of nodes that are available for processing.

## Course 3 [Distributed Programming in Java](https://www.coursera.org/learn/distributed-programming-in-java/home/welcome)

### Week 1

#### **Introduction to MapReduce**

In this lecture, we learned the MapReduce paradigm, which is a pattern of parallel functional programming that has been very successful in enabling "big data" computations.

The input to a MapReduce style computation is a set of *key-value pairs*. The keys are similar to keys used in hash tables, and the functional programming approach requires that both the keys and values be immutable. When a *user-specified map function*, `f`, is applied on a key-value pair, `(kA,vA)`, it results in a (possibly empty) set of output key-value pairs, `{(kA1, vA1), (kA2, vA2), ...}` This map function can be applied in parallel on all key-value pairs in the input set, to obtain a set of *intermediate* key-value pairs that is the union of all the outputs.

The next operation performed in the MapReduce workflow is referred to as `grouping`, which groups together all intermediate key-value pairs with the same key. Grouping is performed automatically by the MapReduce framework, and need not be specified by the programmer. For example, if there are two intermediate key-value pairs, `(kA1, vA1)` and `(kB1, vB1)` with the same key, `kA1 = kB1 = k`, then the output of grouping will associate the set of values `{vA1,vB1}` with key `k`.

Finally, when a user-specified reduce function, `g`, is applied on two or more grouped values (e.g., `vA1, vB1, ...`) associated with the same key `k`, it folds or reduces all those values to obtain a single output key-value pair, `(k, g(vA1, vB1, ...))`, for each key `k` in the intermediate key-value set. If needed, the set of output key-value pairs can then be used as the input for a successive MapReduce computation.

In the example discussed in the lecture, we assumed that the map function, `f`, mapped a key-value pair like `("WR", 10)` to a set of intermediate key-value pairs obtained from *factors of 10* to obtain the set, `{("WR", 2), ("WR", 5), ("WR", 10)}`, and the reduce function, `g`, calculated the sum of all the values with the same key to obtain ("WR",17) as the output key-value pair for key "WR". The same process can be performed in parallel for all keys to obtain the complete output key-value set.

#### **Apache Hadoop Project**

The *Apache Hadoop* project is a popular open-source implementation of the Map-Reduce paradigm for distributed computing. A distributed computer can be viewed as a large set of multicore computers connected by a network, such that each computer has multiple processor cores, e.g.,P0, P1, P2, P3, ... . Each individual computer also has some *persistent storage* (e.g., hard disk, flash memory), thereby making it possible to store and operate on large volumes of data when aggregating the storage available across all the computers in a data center. The main motivation for the Hadoop project is to make it easy to write large-scale parallel programs that operate on this "big data".

The *Hadoop framework* allows the programmer to specify map and reduce functions in Java, and takes care of all the details of generating a large number of map tasks and reduce tasks to perform the computation as well as scheduling them across a distributed computer. **A key property of the Hadoop framework is that it supports automatic fault-tolerance.** Since MapReduce is essentially a functional programming model, if a node in the distributed system fails, the Hadoop scheduler can reschedule the tasks that were executing on that node with the same input elsewhere, and continue computation. This is not possible with non-functional parallelism in general, because when a non-functional task modifies some state, re-executing it may result in a different answer. The ability of the Hadoop framework to process massive volumes of data has also made it a popular target for higher-level query languages that implement SQL-like semantics on top of Hadoop.

The lecture discussed the *word-count example*, which, despite its simplicity, is used very often in practice for document mining and text mining. In this example, we illustrated how a Hadoop map-reduce program can obtain word-counts for the distributed text "To be or not to be". There are several other applications that have been built on top of Hadoop and other MapReduce frameworks. The main benefit of Hadoop is that it greatly simplifies the job of writing programs to process large volumes of data available in a data center.


#### **Apache Spark Framework**

*Apache Spark* is a similar, but more general, programming model than Hadoop MapReduce. Like Hadoop, Spark also works on distributed systems, but **a key difference in Spark is that it makes better use of in-memory computing within distributed nodes compared to Hadoop MapReduce**. This difference can **have a significant impact on the performance of iterative MapReduce algorithms** since the use of memory obviates the need to write intermediate results to external storage after each map/reduce step. **However, this also implies that the size of data that can be processed in this manner is limited by the total size of memory across all nodes, which is usually much smaller than the size of external storage.** (Spark can spill excess data to external storage if needed, but doing so reduces the performance advantage over Hadoop.)

Another major difference between Spark and Hadoop MapReduce, is that the **primary data type in Spark is the Resilient Distributed Dataset (RDD)**, which can be viewed as a generalization of sets of key-value pairs. RDDs enable Spark to support more general operations than map and reduce. Spark supports intermediate operations called *Transformations* (e.g., `map`, `filter`, `join`, ...) and terminal operations called Actions (e.g., `reduce`, `count`, `collect`, ...). As in Java streams, intermediate transformations are performed lazily, i.e., their evaluation is postponed to the point when a terminal action needs to be performed.

In the lecture, we saw how the Word Count example can be implemented in Spark using Java APIs. (The Java APIs use the same underlying implementation as Scala APIs, since both APIs can be invoked in the same Java virtual machine instance.) We used the Spark `flatMap()` method to combine all the words in all the lines in an input file into a single RDD, followed by a `mapToPair()` transform method call to emit pairs of the form, `(word, 1)`, which can then be processed by a `reduceByKey()` operation to obtain the final word counts.


#### **TF-IDF (Term Frequency - Inversed Term Frequency) Example**

In this lecture, we discussed an important statistic used in information retrieval and document mining, called *Term Frequency – Inverse Document Frequency (TF-IDF)*. The motivation for computing TF-IDF statistics is to efficiently identify documents that are most similar to each other within a large corpus. 

Assume that we have a set of `N` documents `D_1, D_2, ... , D_N`, and a set of terms `TERM_1, TERM_2,...` that can appear in these documents. We can then compute total frequencies `TF_ij` for each term `TERM_i` in each document `D_j`. We can also compute the document frequencies `DF_1, DF_2, ...` for each term, indicating how many documents contain that particular term, and the inverse document frequencies (IDF): `IDF_i = N/DF_i`. The motivation for computing inverse document frequencies is to determine which terms are common and which ones are rare, and give higher weights to the rarer terms when searching for similar documents. The weights are computed as: `Weight(TERM_i,D_j) = TF_i,j × log(N/DF_i)`.

Using MapReduce, we can compute the `TF_i,j` values by using a MAP operation to find all the occurrences of `TERM_i` in document `D_j`, followed by a REDUCE operation to add up all the occurrences of `TERM_i` as key-value pairs of the form, `((D_j, TERM_i), TF_i,j)` (as in the Word Count example studied earlier). These key-value pairs can also be used to compute `DF_i` values by using a MAP operation to identify all the documents that contain `TERM_i` and a REDUCE operation to count the number of documents that `TERM_i` appears in. The final weights can then be easily computed from the `TF_i,j` and `DF_i` values. Since the TF-IDF computation uses a fixed (not iterative) number of MAP and REDUCE operations, it is a good candidate for both Hadoop and Spark frameworks.

#### **PageRank Example**

In this lecture, we discussed the `PageRank algorithm` as an example of an **iterative** algorithm that is well suited for the Spark framework. The goal of the algorithm is to determine which web pages are more important by examining links from one page to other pages. In this algorithm, the rank of a page, `B`, is defined as follows,

RANK(B) = ∑ RANK(A) / DEST_COUNT(A) for A ∈ SRC(B)

where `SRC(B)` is the set of pages that contain a link to `B`, while `DEST_COUNT(A)` is the total number of pages that `A` links to.  Intuitively, the PageRank algorithm works by splitting the weight of a page `A` (i.e., `RANK(A)`) among all of the pages that `A` links to (i.e. `DEST_COUNT(A)`). Each page that `A` links to has its own rank increased proportional to `A`'s own rank. As a result, pages that are linked to from many highly-ranked pages will also be highly ranked.

The motivation to divide the contribution of `A` in the sum by `DEST_COUNT(A)` is that if page `A` links to multiple pages, each of the successors should get a fraction of the contribution from page `A`.   Conversely, if a page has many outgoing links, then each successor page gets a relatively smaller weightage, compared to pages that have fewer outgoing links. This is a recursive definition in general, since if (say) page `X` links to page `Y`, and page `Y` links to page `X`, then `RANK(X)` depends on `RANK(Y)` and vice versa. Given the recursive nature of the problem, **we can use an iterative algorithm to compute all page ranks by repeatedly updating the rank values using the above formula, and stopping when the rank values have converged to some acceptable level of precision.** In each iteration, the new value of `RANK(B)` can be computed by accumulating the contributions from each predecessor page, `A`. A parallel implementation in Spark can be easily obtained by implementing **two steps in an iteration**, one for **computing the contributions of each page to its successor pages by using the `flatMapToPair()` method**, and the second for **computing the current rank of each page by using the `reduceByKey()` and `mapValues()` methods**. All the intermediate results between iterations will be kept in main memory, resulting in a much faster execution than a Hadoop version (which would store intermediate results in external storage).

### Week 2

#### **Introduction to Sockets**

In this lecture, we learned about client-server programming, and how two distributed Java applications can communicate with each other using sockets. Since each application in this scenario runs on a distinct Java Virtual Machine (JVM) process, we used the terms "application", "JVM" and "process" interchangeably in the lecture.  For JVM A and JVM B to communicate with each other, we assumed that JVM A plays the "client" role and JVM B the "server" role.  To establish the connection, the main thread in JVM B first creates a `ServerSocket` (called `socket`, say) which is initialized with a designated URL and port number. It then waits for client processes to connect to this socket by invoking the `socket.accept()` method, which returns an object of type `Socket` (called `s`, say) .  The `s.getInputStream()` and `s.getOutputStream()` methods can be invoked on this object to perform read and write operations via the socket, using the same APIs that you use for file I/O via streams.

Once JVM B has set up a server socket, JVM A can connect to it as a client by creating a `Socket` object with the appropriate parameters to identify JVM B's server port. As in the server case, the `getInputStream()` and `getOutputStream()` methods can be invoked on this object to perform read and write operations. With this setup, JVM A and JVM B can communicate with each other by using read and write operations, which get implemented as messages that flow across the network. Client-server communication occurs at a lower level and scale than *MapReduce*, which implicitly accomplishes communication among large numbers of processes.  Hence, client-server programming is typically used for building distributed applications with small numbers of processes.

#### **Serialization and Deserialization**

This lecture reviewed *serialization* and *deserialization*, which are essential concepts for all forms of data transfers in Java applications, including file I/O and communication in distributed applications. When communications are performed using input and output streams, as discussed in the previous lecture, **the unit of data transfer is a sequence of bytes**. Thus, it becomes important to *serialize* objects into bytes in the sender process, and to *deserialize* bytes into objects in the receiver process.

One approach to do this is via a *custom approach*, in which the  programmer provides custom code to perform the serialization and deserialization. However, writing custom serializers and deserializers can become complicated when nested objects are involved, e.g., if object `x` contains a field, `f3`, which points to object `y`. In this case, the serialization of object `x` by default also needs to include a serialization of object `y`. Another approach is to use XML, since XML was designed to serve as a data interchange standard. There are many application frameworks that support conversion of application objects into XML objects, which is convenient because typical XML implementations in Java include built-in serializers and deserializers. However, the downside is that there can be a lot of metadata created when converting Java objects into XML, and that metadata can add to the size of the serialized data being communicated.

As a result, the default approach is to use a capability that has been present since the early days of Java, namely Java Serialization and Deserialization.  This works by identifying classes that implement the `Serializable` interface, and relying on a guarantee that such classes will have built-in *serializers* and *deserializers*, analogous to classes with built-in `toString()` methods. In this situation, if object `x` is an instance of a serializable class and its field `f3` points to object `y`, then object `y` must also be an instance of a serializable class (otherwise an exception will be thrown when attempting to serialize object `x`). An important benefit of this approach is that only one copy of each object is included in the serialization, even if there may be multiple references to the object, e.g., if fields `f2` and `f3` both point to object `y`. Another benefit is that cycles in object references are handled intelligently, without getting into an infinite loop when following object references. Yet another important benefit is that this approach allows identification of fields that should be skipped during the serialization/deserialization steps because it may be unnecessary and inefficient to include them in the communication. Such fields are identified by declaring them as *transient*.

Finally, another approach that has been advocated for decades in the context of distributed object systems is to use an *Interface Definition Language (IDL)* to enable serialization and communication of objects across distributed processes.  A recent example of using the IDL approach can be found in Google's  Protocol Buffers framework.  A notable benefit of this approach relative to Java serialization is that protocol buffers can support communication of objects across processes implemented in different languages, e.g., Java, C++, Python.  The downside is that extra effort is required to enable the serialization (e.g., creating a `.proto` file as an IDL, and including an extra compile step for the IDL in the build process), which is not required when using Java serialization for communication among Java processes.

#### **Remote Method Invocation**

This lecture reviewed the concept of Remote Method Invocation (RMI), which extends the notion of method invocation in a sequential program to a distributed programming setting. As an example, let us consider a scenario in which a thread running on JVM A wants to invoke a method, `foo()`, on object `x` located on JVM B. This can be accomplished using sockets and messages, but that approach would entail writing a lot of extra code for encoding and decoding the method call, its arguments, and its return value. In contrast, Java RMI provides a very convenient way to directly address this use case.

To enable RMI, we run an RMI client on JVM A and an RMI server on JVM B. Further, JVM A is set up to contain a stub object or proxy object for remote object `x` located on JVM B. (In early Java RMI implementations, a skeleton object would also need to be allocated on the server side, JVM B, as a proxy for the shared object, but this is no longer necessary in modern implementations.) When a stub method is invoked, it transparently initiates a connection with the remote JVM containing the remote object, `x`, serializes and communicates the method parameters to the remote JVM, receives the result of the method invocation, and deserializes the result into object `y` (say) which is then passed on to the caller of method `x.foo()` as the result of the RMI call.

Thus, RMI takes care of a number of tedious details related to remote communication. However, this convenience comes with a few setup requirements as well. First, objects `x` and `y` must be serializable, because their values need to be communicated between JVMs `A` and `B`.  Second, object `x` must be included in the RMI registry, so that it can be accessed through a global name rather than a local object reference.  The registry in turn assists in mapping from global names to references to local stub objects. In summary, a key advantage of RMI is that, once this setup in place, method invocations across distributed processes can be implemented almost as simply as standard method invocations.

#### **Multicast Sockets**

In this lecture, we learned about *multicast sockets*, which are a generalization of the standard socket interface that we studied earlier. Standard sockets can be viewed as unicast communications, in which a message is sent from a source to a single destination. Broadcast communications represent a simple extension to unicast, in which a message can be sent efficiently to all nodes in the same local area network as the sender. In contrast, multicast sockets enable a sender to efficiently send the same message to a specified set of receivers on the Internet. This capability can be very useful for a number of applications, which include news feeds, video conferencing, and multi-player games. One reason why a `1:n` multicast socket is more efficient than n `1:1` sockets is because Internet routers have built-in support for the multicast capability.

In recognition of this need, the Java platform includes support for a `MulticastSocket` class, which can be used to enable a process to **join a group** associated with a given `MulticastSocket` instance. A member of a group can send a message to all other processes in the group, and can also receive messages sent by other members. This is analogous to how members of a group-chat communicate with each other. Multicast messages are restricted to datagrams, which are usually limited in size to 64KB. Membership in the group can vary dynamically, i.e., processes can decide to join or leave a group associated with a `MulticastSocket` instance as they choose. Further, just as with group-chats, a process can be a member of multiple groups.

#### **Publish-Subscribe Pattern**

In this lecture, we studied the `publish-subscribe pattern`, which represents a further generalization of the multicast concept. In this pattern, **publisher processes add messages to designated topics, and subscriber processes receive those messages by registering on the topics that they are interested in**. A key advantage of this approach is that **publishers need not be aware of which processes are the subscribers, and vice versa**. Another advantage is that it lends itself to very efficient implementations because it can enable a number of communication optimizations, which include batching and topic partitioning across broker nodes. Yet another advantage is improved reliability, because broker nodes can replicate messages in a topic, so that if one node hosting a topic fails, the entire publish-subscribe system can continue execution with another node that contains a copy of all messages in that topic.

We also studied how publish-subscribe patterns can be implemented in Java by using APIs available in the open-source *Apache Kafka project*.  To become a publisher, a process simply needs to create a `KafkaProducer` object, and use it to perform `send()` operations to designated topics. Likewise, to become a consumer, a process needs to create a `KafkaConsumer` object, which can then be used to subscribe to topics of interest.  The consumer then performs repeated `poll()` operations, each of which returns a batch of messages from the requested topics. `Kafka is commonly used as to produce input for, or receive output from, MapReduce systems such as Hadoop or Spark.` By storing Kafka messages as key-value pairs, data analytics applications written using MapReduce programming models can seamlessly interface with Kafka.  A common use case for Kafka is to structure messages in a topic to be as key-value pairs, so that they can be conveniently used as inputs to, or outputs from, data analytics applications written in Hadoop or Spark.   Each key-value message also generally includes an offset which represents the index of the message in the topic. In summary, publish-subscribe is a higher-level pattern than communicating via sockets, which is both convenient and efficient to use in situations where producers and consumers of information are set up to communicate via message groups (topics).  

### Week 3

In this lecture, we studied the Single Program Multiple Data (SPMD) model, which can enable the use of a cluster of distributed nodes as a single parallel computer. Each node in such a cluster typically consist of a multicore processor, a local memory, and a network interface card (NIC) that enables it to communicate with other nodes in the cluster.  One of the biggest challenges that arises when trying to use the distributed nodes as a single parallel computer is that of data distribution. In general, we would want to allocate large data structures that span multiple nodes in the cluster; this logical view of data structures is often referred to as a global view. However, a typical physical implementation of this global view on a cluster is obtained by distributing pieces of the global data structure across different nodes, so that each node has a local view of the piece of the data structure allocated in its local memory. In many cases in practice, the programmer has to undertake the conceptual burden of mapping back and forth between the logical global view and the physical local views.  Since there is one logical program that is executing on the individual pieces of data, this abstraction of a cluster is referred to as the Single Program Multiple Data (SPMD) model.

In this module, we will focus on a commonly used implementation of the SPMD model, that is referred to as the *Message Passing Interface (MPI)*. When using MPI, you designate a fixed set of processes that will participate for the entire lifetime of the global application. It is common for each node to execute one MPI process, but it is also possible to execute more than one MPI process per multicore node so as to improve the utilization of processor cores within the node. Each process starts executing its own copy of the MPI program, and starts by calling  the `mpi.MPI_Init()` method, where `mpi` is the instance of the MPI class used by the process. After that, each process can call the `mpi.MPI_Comm_size(mpi.MPI_COMM_WORLD)` method to determine the total number of processes participating in the MPI application, and the `MPI_Comm_rank(mpi.MPI_COMM_WORLD)` method to determine the process' own rank within the range, 0 to S−1, where `S = MPI_Comm_size()`.

In this lecture, we studied how a global view, XG, of array X can be implemented by `S` local arrays (one per process) of size `XL.length = XG.length/S`. For simplicity, assume that `XG.length` is a multiple of `S`. Then, if we logically want to set `XG[i] := i` for all logical elements of `XG`, we can instead set `XL[i] := L × R + i` in each local array, where `L = XL.length`, and `R = MPI_Comm_rank()`. Thus process 0's copy of `XL` will contain logical elements `XG[0 ... L-1]`, process 1's copy of `XL` will contain logical elements `XG[L ... 2×L - 1]`, and so on. Thus, we see that the SPMD approach is very different from client server programming, where each process can be executing a different program.

#### **Point-to-Point Communication**

In this lecture, we studied how to perform *point-to-point* communication in MPI by sending and receiving messages. In particular, we worked out the details for a simple scenario in which process 0 sends a string, "ABCD", to process 1. Since MPI programs follow the SPMD model, we have to ensure that the same program behaves differently on processes 0 and 1. This was achieved by using an *if-then-else* statement that checks the value of the rank of the process that it is executing on. If the rank is zero, we include the necessary code for calling `MPI_Send()`; otherwise, we include  the necessary code for calling `MPI_Recv()` (assuming that this simple program is only executed with two processes). Both calls include a number of parameters. The `MPI_Send()` call specifies the substring to be sent as a subarray by providing the string, offset, and data type, as well as the rank of the receiver, and a tag to assist with matching send and receive calls (we used a tag value of 99 in the lecture). The `MPI_Recv()` call (in the else part of the *if-then-else* statement) includes a buffer in which to receive the message, along with the offset and data type, as well as the rank of the sender and the tag. Each send/receive operation waits (or is blocked) until its dual operation is performed by the other process. Once a pair of parallel and compatible `MPI_Send()` and `MPI_Recv()` calls is matched, the actual communication is performed by the MPI library. This approach to matching pairs of send/receive calls in SPMD programs is referred to as two-sided communication.

As indicated in the lecture, the current implementation of MPI only supports communication of (sub)arrays of *primitive* data types. However, since we have already learned how to serialize and deserialize objects into/from bytes, the same approach can be used in MPI programs by communicating arrays of bytes.

#### **Message Ordering and Deadlock**

In this lecture, we studied some important properties of the message-passing model with send/receive operations, namely message ordering and deadlock. For message ordering, we discussed a simple example with four MPI processes, `R0`, `R1`, `R2`, `R3` (with ranks 0 to 3 respectively).  In our example, process `R1` sends message `A` to process `R0`, and process `R2` sends message `B` to process `R3`. We observed that there was no guarantee that process `R1`'s send request would complete before process `R2`'s request, even if process `R1` initiated its send request before process `R2`. Thus, **there is no guarantee of the temporal ordering of these two messages**. In MPI, **the only guarantee of message ordering is when multiple messages are sent with the same sender, receiver, data type, and tag** -- these messages will all be ordered in accordance with when their send operations were initiated.

We learned that send and receive operations can lead to an interesting parallel programming challenge called deadlock. There are many ways to create deadlocks in MPI programs. In the lecture, we studied a simple example in which process `R0` attempts to send message `X` to process R1R1, and process `R1` attempts to send message `Y` to process `R0`.  Since both sends are attempted in parallel, processes `R0` and `R1` remain blocked indefinitely as they wait for matching receive operations, thus resulting in a classical deadlock cycle.

We also learned two ways to fix such a deadlock cycle. **The first is by interchanging the two statements in one of the processes** (say process `R1`). As a result, the send operation in process `R0` will match the receive operation in process `R1`, and both processes can move forward with their next communication requests. **Another approach is to use MPI's `sendrecv()` operation** which includes all the parameters for the send and for the receive operations. By combining send and receive into a single operation, the MPI runtime ensures that deadlock is avoided because a `sendrecv()` call in process `R0` can be matched with a `sendrecv()` call in process `R1` instead of having to match individual send and receive operations.

#### **Non-Blocking Communications**

In this lecture, we studied non-blocking communications, which are implemented via the `MPI_Isend()`  and `MPI_Irecv()` API calls. The `I` in `MPI_Isend` and `MPI_Irecv` stands for "Immediate" because **these calls return immediately instead of blocking until completion**. Each such call returns an object of type `MPI_Request` which can be used as a handle to track the progress of the corresponding send/receive operation. In the example we studied, process `R0` performs an `Isend` operation with `req0` as the return value. Likewise process `R1` performs an `Irecv` operation with `req1` as the return value. Further, process `R0` can perform some local computation in statement `S2` while waiting for its `Isend`  operation to complete, and process `R1` can do the same with statement `S5` while waiting for its `Irecv` operation to complete. Finally, when process `R1` needs to use the value received from process `R0` in statement `S6`, it has to first perform an `MPI_Wait()` operation on the `req1` object to ensure that the receive operation has fully completed. Likewise, when process `R0` needs to ensure that the buffer containing the sent message can be overwritten, it has to first perform an `MPI_Wait()` operation on the `req0` object to ensure that the send operation has fully completed. For convenience, `MPI_Waitall()` can be invoked on an array of requests to wait on all of them with a single API call.

The main benefit of this approach is that the amount of idle time spent waiting for communications to complete is reduced when using non-blocking communications, since the `Isend` and `Irecv` operations can be overlapped with local computations. As a convenience, MPI also offers two additional wait operations, `MPI_Waitall()` and `MPI_Waitany()`, that can be used to wait for all and any one of a set of requests to complete. Also, while it is common for `Isend` and `Irecv` operations to be paired with each other, it is also possible for a nonblocking send/receive operation in one process to be paired with a blocking receive/send operation in another process. In fact, a blocking `Send`/`Recv` operation can also be viewed as being equivalent to a nonblocking `Isend`/`Irecv` operation that is immediately followed by a `Wait` operation.

#### **Collective Communication**

In this lecture, we studied collective communication, which can involve multiple processes, in a manner that is both similar to, and more powerful, than multicast and publish-subscribe operations. We first considered a simple broadcast operation in which rank `R0` needs to send message `X` to all other MPI processes in the application. One way to accomplish this is for `R0` to send individual (point-to-point) messages to processes `R1, R2, …` one by one, but this approach will make `R0` a sequential bottleneck when there are (say) thousands of processes in the MPI application. Further, the interconnection network for the compute nodes is capable of implementing such broadcast operations more efficiently than point-to-point messages. Collective communications help exploit this efficiency by leveraging the fact that all MPI processes execute the same program in an SPMD model. For a broadcast operation, all MPI processes execute an `MPI_Bcast()` API call **with a specified root process that is the source of the data to be broadcasted**. A key property of collective operations is that **each process must wait until all processes reach the same collective operation, before the operation can be performed.** This form of waiting is referred to as a barrier.  After the operation is completed, all processes can move past the implicit barrier in the collective call. In the case of `MPI_Bcast()`, each process will have obtained a copy of the value broadcasted by the root process.

MPI supports a wide range of collective operations, which also includes reductions. The reduction example discussed in the lecture was one in which process `R2` needs to receive the sum of the values of element `Y[0]` in all the processes, and store it in element `Z[0]` in process `R2`.  To perform this computation, all processes will need to execute a `Reduce()` operation (with an implicit barrier). The parameters to this call include the input array (`Y`), a zero offset for the input value, the output array (`Z`, which only needs to be allocated in process `R2`), a zero offset for the output value, the number of array elements (1) involved in the reduction from each process, the data type for the elements (`MPI.INT`), the operator for the reduction (`MPI_SUM`), and the root process (`R2`) which receives the reduced value. Finally, it is interesting to note that MPI's SPMD model and reduction operations can also be used to implement the MapReduce paradigm. All the local computations in each MPI process can be viewed as map operations, and they can be interspersed with reduce operations as needed.

### Week 4

#### **Combining Distribution and Multithreading**

In this lecture, we introduced processes and threads, which serve as the fundamental building blocks of distributed computing software. Though the concepts of processes and threads have been around for decades, the ability to best map them on to hardware has changed in the context of data centers with multicore nodes. **In the case of Java applications, a process corresponds to a single Java Virtual Machine (JVM) instance, and threads are created within a JVM instance.**

The advantages of creating *multiple threads* in a process include **increased sharing of memory and per-process resources by threads, improved responsiveness due to multithreading, and improved performance since threads in the same process can communicate with each other through a shared address space**.

The advantages of creating *multiple processes* in a node include **improved responsiveness (also) due to multiprocessing (e.g., when a JVM is paused during garbage collection), improved scalability (going past the scalability limitations of multithreading), and improved resilience to JVM failures within a node**. However, processes can only communicate with each other through **message-passing and other communication patterns for distributed computing**.

In summary, **processes are the basic units of distribution in a cluster of nodes** - we can distribute processes across multiple nodes in a data center, and even create multiple processes within a node. **Threads are the basic unit of parallelism and concurrency** -- we can create multiple threads in a process that can share resources like memory, and contribute to improved performance. *However, it is not possible for two threads belonging to the same process to be scheduled on different nodes*.

#### **Multi-threaded Servers**

In this lecture, we learned about multithreaded servers as an extension to the servers that we studied in client-server programming. As a motivating example, we studied the timeline for a single request sent to a standard sequential file server, which typically consists of four steps: A) accept the request, B) extract the necessary information from the request, C) read the file, and D) send the file. In practice, step C) is usually the most time-consuming step in this sequence. However, threads can be used to reduce this bottleneck. In particular, **after step A) is performed, a new thread can be created to process steps B), C) and D) for a given request.** In this way, it is possible to process multiple requests simultaneously because they are executing in different threads.

One challenge of following this approach literally is that **there is a significant overhead** in creating and starting a Java thread. However, since there is usually an upper bound on the number of threads that can be efficiently utilized within a node (often limited by the number of cores or hardware context), it is wasteful to create more threads than that number. There are two approaches that are commonly taken to address this challenge in Java applications. **One is to use a thread pool**, so that threads can be reused across multiple requests instead of creating a new thread for each request. **Another is to use lightweight tasking (e.g., as in Java's ForkJoin framework) which  execute on a thread pool with a bounded number of threads,** and offer the advantage that the overhead of task creation is significantly smaller than that of thread creation. You can learn more about tasks and threads in the companion courses on "Parallel Programming in Java" and "Concurrent Programming in Java" in this specialization.

#### **MPI and Multi-threading**

In this lecture, we learned how to extend the *Message Passing Interface (MPI)* with threads. As we learned earlier in the lecture on Processes and Threads, **it can be inefficient to create one process per processor core in a multicore node since there is a lot of unnecessary duplication of memory, resources, and overheads when doing so**. This same issue arises for MPI programs in which each rank corresponds to a single-threaded process by default. Thus, there are many motivations for creating multiple threads in an MPI process, including the fact that **threads can communicate with each other much more efficiently using shared memory, compared with the message-passing that is used to communicate among processes**.

There are three modes to create multi-threading for MPI applications.
(1) ***Funneled mode:*** One approach to enable multithreading in MPI applications is **to create one MPI process (rank) per node**, which **starts execution in a single thread that is referred to as a master thread**. This thread calls `MPI_Init()` and `MPI_Finalize()` for its rank, and creates a number of worker threads to assist in the computation to be performed within its MPI process. Further, all MPI calls are performed only by the master thread. This approach is referred to as the `MPI_THREAD_FUNNELED` mode, since, even though there are multiple threads, all MPI calls are "funneled" through the master thread.
(2) ***Serialized mode:*** A second more general mode for MPI and multithreading is referred to as `MPI_THREAD_SERIALIZED`; in this mode, multiple threads may make MPI calls but must do so **one at a time** using appropriate concurrency constructs so that the calls are "serialized".
(3) ***Multiple mode:*** The most general mode is called `MPI_THREAD_MULTIPLE` because it allows multiple threads to make MPI calls in parallel; though this mode offers more flexibility than the other modes, it puts an additional burden on the MPI implementation which usually gets reflected in larger overheads for MPI calls relative to the more restrictive modes. Further, even the `MPI_THREAD_MULTIPLE` mode has some notable restrictions, e.g., it is not permitted in this mode for two threads in the same process to wait on the same MPI request related to a nonblocking communication.

#### **Distributed Actors**

In this lecture, we studied distributed actors as another example of combining distribution and multi-threading. An actor is an object that *has a mailbox, local state, a set of methods and an active (logical) thread of control that can receive one message at a time from the mailbox, invoke a method to perform the work needed by that message, and read and update the local state as needed*. Message-passing in the actor model is nonblocking since the sender and receiver do not need to wait for each other when transmitting messages. We also studied a simple algorithm for generating prime numbers, called the *Sieve of Eratosthenes*, that can be conveniently implemented using actors. The actor paradigm is well suited to both multicore and distributed parallelism, since its message-passing model can be implemented efficiently via shared memory within a single process or in a more distributed manner across multiple processes.

Most actor implementations that support distributed execution require you to perform the following steps.
(1) First, you will need to use some kind of *configuration file* to specify the host process on which each actor will execute as well as the port that can be used to receive messages from actors on other processes.
(2) Second, you will need the ability to create actors on remote processes.
(3) Third, you will need to provide some kind of logical name to refer to a remote actor (since a reference to the actor object can only be used within the process containing that actor).
(4) Finally, messages transmitted among actors that reside in different processes need to be serialized, as in client-server programming.

Besides these steps, the essential approach to writing actor programs is the same whether the programs execute within a single process, or across multiple processes.

#### **Distributed Reactive Programming**

In this lecture, we studied the reactive programming model and its suitability for implementing distributed service oriented architectures using *asynchronous events*. A key idea behind this model is to balance the "push" and "pull" modes found in different distributed programming models. For example, actors can execute in push mode, since the receiver has no control on how many messages it receives. Likewise, Java streams and Spark RDDs operate in pull mode, since their implementations are *demand-driven (lazy)*. The adoption of distributed reactive programming is on a recent upswing, fueled in part by the availability of the  Reactive Streams specification which includes support for multiple programming languages.  In the case of Java,  the specification consists of four interfaces: `Flow.Publisher`, `Flow.Subscriber`, `Flow.Process`, and `Flow.Subscription`.

Conveniently, there is a standard Java implementation of the `Flow.Publisher` interface in the form of the `SubmissionPublisher` class. If we create an instance of this class called `pub`, a publisher can submit information by calling `pub.submit()`. Likewise, a subscriber can be registered by calling `pub.subscribe()`. Each subscriber has to implement two key methods, `onSubscribe()` and `onNext()`. Both methods allow the subscriber to specify how many elements to request at a time. Thus, a key benefit of reactive programming is that the programmer can  control the "batching" of information between the publisher and the subscriber to achieve a desired balance between the "push" and "pull" modes.
