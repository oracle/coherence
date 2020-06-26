/*
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.cache;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A local cache that implementates the Window TinyLFU eviction policy.
 *
 * @author Ben Manes
 */
@SuppressWarnings({"deprecation", "rawtypes", "unchecked"})
final class TinyLfuLocalCache extends LocalCache {
  /*
   * Maximum size is implemented using the Window TinyLfu policy [1] due to its high hit rate, O(1)
   * time complexity, and small footprint. A new entry starts in the admission window and remains
   * there as long as it has high temporal locality (recency). Eventually an entry will slip from
   * the window into the main space. If the main space is already full, then a historic frequency
   * filter determines whether to evict the newly admitted entry or the victim entry chosen by the
   * eviction policy. This process ensures that the entries in the window were very recently used
   * and entries in the main space are accessed very frequently and are moderately recent. The
   * windowing allows the policy to have a high hit rate when entries exhibit bursty access pattern
   * while the filter ensures that popular items are retained. The admission window uses LRU and
   * the main space uses Segmented LRU.
   *
   * The optimal size of the window vs main spaces is workload dependent [2]. A large admission
   * window is favored by recency-biased workloads while a small one favors frequency-biased
   * workloads. When the window is too small then recent arrivals are prematurely evicted, but when
   * too large then they pollute the cache and force the eviction of more popular entries. The
   * configuration is dynamically determined using hill climbing to walk the hit rate curve. This is
   * done by sampling the hit rate and adjusting the window size in the direction that is improving
   * (making positive or negative steps). At each interval the step size is decreased until the
   * climber converges at the optimal setting. The process is restarted when the hit rate changes
   * over a threshold, indicating that the workload altered and a new setting may be required.
   *
   * The historic usage is retained in a compact popularity sketch, which uses hashing to
   * probabilistically estimate an item's frequency. This exposes a flaw where an adversary could
   * use hash flooding [3] to artificially raise the frequency of the main space's victim and cause
   * all candidates to be rejected. In the worst case, by exploiting hash collisions an attacker
   * could cause the cache to never hit and hold only worthless items, resulting in a
   * denial-of-service attack against the underlying resource. This is protected against by
   * introducing jitter so that candidates which are at least moderately popular have a small,
   * random chance of being admitted. This causes the victim to be evicted, but in a way that
   * marginally impacts the hit rate.
   *
   * [1] TinyLFU: A Highly Efficient Cache Admission Policy
   * https://dl.acm.org/citation.cfm?id=3149371
   * [2] Adaptive Software Cache Management
   * https://dl.acm.org/citation.cfm?id=3274816
   * [3] Denial of Service via Algorithmic Complexity Attack
   * https://www.usenix.org/legacy/events/sec03/tech/full_papers/crosby/crosby.pdf
   */

  private static final long serialVersionUID = 1L;

  /** The initial percent of the maximum weighted capacity dedicated to the main space. */
  static final double PERCENT_MAIN = 0.99d;
  /** The percent of the maximum weighted capacity dedicated to the main's protected space. */
  static final double PERCENT_MAIN_PROTECTED = 0.80d;
  /** The difference in hit rates that restarts the climber. */
  static final double HILL_CLIMBER_RESTART_THRESHOLD = 0.05d;
  /** The percent of the total size to adapt the window by. */
  static final double HILL_CLIMBER_STEP_PERCENT = 0.0625d;
  /** The rate to decrease the step size to adapt by. */
  static final double HILL_CLIMBER_STEP_DECAY_RATE = 0.98d;
  /** The maximum number of entries that can be transfered between queues. */

  private final TinyLfuEntry headWindow;
  private final TinyLfuEntry headProbation;
  private final TinyLfuEntry headProtected;
  private final FrequencySketch frequencySketch;

  private int maxWindow;
  private int maxProtected;
  private double windowSize;
  private double protectedSize;

  private int hitsInSample;
  private int missesInSample;
  private double previousHitRate;

  private boolean increaseWindow;
  private double stepSize;

  public TinyLfuLocalCache(int cUnits) {
    super(cUnits, DEFAULT_EXPIRE);
    setEvictionPolicy(new TinyLfuPolicy());
    this.frequencySketch = new FrequencySketch();
    this.headWindow = new TinyLfuEntry().asSentinel();
    this.headProbation = new TinyLfuEntry().asSentinel();
    this.headProtected = new TinyLfuEntry().asSentinel();

    int maxMain = (int) (cUnits * PERCENT_MAIN);
    this.maxProtected = (int) (maxMain * PERCENT_MAIN_PROTECTED);
    this.maxWindow = (cUnits - maxMain);

    stepSize = (HILL_CLIMBER_STEP_PERCENT * getHighUnits());
    frequencySketch.ensureCapacity(m_cMaxUnits);
  }

  @Override
  public synchronized void setHighUnits(int cMax) {
    super.setHighUnits(cMax);
    if (frequencySketch != null) {
      frequencySketch.ensureCapacity(m_cMaxUnits);
    }
  }

  @Override
  public Object put(Object key, Object value, long millis) {
    synchronized (this) {
      frequencySketch.increment(key);
    }
    Object prior = super.put(key, value, millis);
    if (prior == null) {
      synchronized (this) {
        missesInSample++;
      }
    }
    return prior;
  }

  @Override
  protected Entry instantiateEntry() {
    return new TinyLfuEntry();
  }

  private void onHit(TinyLfuEntry entry) {
    //frequencySketch.increment(entry.getKey());
    if (entry.queue == QueueType.WINDOW) {
      onWindowHit(entry);
    } else if (entry.queue == QueueType.PROBATION) {
      onProbationHit(entry);
    } else if (entry.queue == QueueType.PROTECTED) {
      onProtectedHit(entry);
    } else {
      throw new IllegalStateException();
    }
    hitsInSample++;
    climb(entry);
  }

  /** Moves the entry to the MRU position in the admission window. */
  private void onWindowHit(TinyLfuEntry node) {
    node.moveToTail(headWindow);
  }

  /** Promotes the entry to the protected region's MRU position, demoting an entry if necessary. */
  private void onProbationHit(TinyLfuEntry node) {
    node.remove();
    node.queue = QueueType.PROTECTED;
    node.appendToTail(headProtected);

    protectedSize++;
    demoteProtected();
  }

  private void demoteProtected() {
    if (protectedSize > maxProtected) {
      TinyLfuEntry demote = headProtected.next;
      demote.remove();
      demote.queue = QueueType.PROBATION;
      demote.appendToTail(headProbation);
      protectedSize--;
    }
  }

  /** Moves the entry to the MRU position, if it falls outside of the fast-path threshold. */
  private void onProtectedHit(TinyLfuEntry node) {
    node.moveToTail(headProtected);
  }

  /**
   * Evicts from the admission window into the probation space. If the size exceeds the maximum,
   * then the admission candidate and probation's victim are evaluated and one is evicted.
   */
  @Override
  public synchronized void evict() {
    if (windowSize <= maxWindow) {
      return;
    }

    TinyLfuEntry candidate = headWindow.next;
    windowSize -= candidate.getUnits();

    candidate.remove();
    candidate.queue = QueueType.PROBATION;
    candidate.appendToTail(headProbation);

    if (getUnits() > getHighUnits()) {
      TinyLfuEntry victim = headProbation.next;
      TinyLfuEntry evict = admit(candidate, victim) ? victim : candidate;
      removeEvicted(evict);
    }
  }

  /**
   * Determines if the candidate should be accepted into the main space, as determined by its
   * frequency relative to the victim. A small amount of randomness is used to protect against hash
   * collision attacks, where the victim's frequency is artificially raised so that no new entries
   * are admitted.
   *
   * @param candidateKey the key for the entry being proposed for long term retention
   * @param victimKey the key for the entry chosen by the eviction policy for replacement
   * @return if the candidate should be admitted and the victim ejected
   */
  private boolean admit(TinyLfuEntry candidate, TinyLfuEntry victim) {
    int victimFreq = frequencySketch.frequency(victim.getKey());
    int candidateFreq = frequencySketch.frequency(candidate.getKey());
    if (candidateFreq > victimFreq) {
      return true;
    } else if (candidateFreq <= 5) {
      // The maximum frequency is 15 and halved to 7 after a reset to age the history. An attack
      // exploits that a hot candidate is rejected in favor of a hot victim. The threshold of a warm
      // candidate reduces the number of random acceptances to minimize the impact on the hit rate.
      return false;
    }
    int random = ThreadLocalRandom.current().nextInt();
    return ((random & 127) == 0);
  }

  /** Performs the hill climbing process. */
  private void climb(TinyLfuEntry entry) {
    boolean isFull = getUnits() >= getHighUnits();
    if (!isFull) {
      return;
    }

    int sampleCount = (hitsInSample + missesInSample);
    if (sampleCount < frequencySketch.sampleSize) {
      return;
    }

    double hitRate = (double) hitsInSample / sampleCount;
    double amount = adjust(hitRate);
    if (amount > 0) {
      increaseWindow(amount);
    } else if (amount < 0) {
      decreaseWindow(-amount);
    }
    resetSample(hitRate);
  }

  private double adjust(double hitRate) {
    if (hitRate < previousHitRate) {
      increaseWindow = !increaseWindow;
    }
    if (Math.abs(hitRate - previousHitRate) >= HILL_CLIMBER_RESTART_THRESHOLD) {
      stepSize = (HILL_CLIMBER_STEP_PERCENT * getHighUnits());
    }
    return increaseWindow ? stepSize : -stepSize;
  }

  /** Starts the next sample period. */
  protected void resetSample(double hitRate) {
    hitsInSample = 0;
    missesInSample = 0;
    previousHitRate = hitRate;
    stepSize *= HILL_CLIMBER_STEP_DECAY_RATE;
  }

  private void increaseWindow(double amount) {
    if (maxProtected == 0) {
      return;
    }

    double quota = Math.min(amount, maxProtected);
    int steps = (int) (windowSize + quota) - (int) windowSize;
    windowSize += quota;

    for (int i = 0; i < steps; i++) {
      maxWindow++;
      maxProtected--;

      demoteProtected();
      TinyLfuEntry candidate = headProbation.next;
      candidate.remove();
      candidate.queue = QueueType.WINDOW;
      candidate.appendToTail(headWindow);
    }
  }

  private void decreaseWindow(double amount) {
    if (maxWindow == 0) {
      return;
    }

    double quota = Math.min(amount, maxWindow);
    int steps = (int) windowSize - (int) (windowSize - quota);
    windowSize -= quota;

    for (int i = 0; i < steps; i++) {
      maxWindow--;
      maxProtected++;

      TinyLfuEntry candidate = headWindow.next;
      candidate.remove();
      candidate.queue = QueueType.PROBATION;
      candidate.appendToHead(headProbation);
    }
  }

  private enum QueueType {
    WINDOW, PROBATION, PROTECTED
  }

  public final class TinyLfuEntry extends LocalCache.Entry {
    private static final long serialVersionUID = 1L;

    private TinyLfuEntry prev;
    private TinyLfuEntry next;
    private QueueType queue;

    TinyLfuEntry asSentinel() {
      prev = this;
      next = this;
      return this;
    }

    @Override
    public void onAdd() {
      super.onAdd();

      synchronized (TinyLfuLocalCache.this) {
        if (getUnits() != -1) {
          appendToTail(headWindow);
          windowSize += getUnits();
          queue = QueueType.WINDOW;
        }
      }
    }

    @Override
    protected void discard() {
      if (!isDiscarded()) {
        remove();
      }
      super.discard();
    }

    public void moveToTail(TinyLfuEntry head) {
      remove();
      appendToTail(head);
    }

    /** Appends the entry to the tail of the list. */
    public void appendToHead(TinyLfuEntry head) {
      TinyLfuEntry first = head.next;
      head.next = this;
      first.prev = this;
      prev = head;
      next = first;
    }

    /** Appends the entry to the tail of the list. */
    public void appendToTail(TinyLfuEntry head) {
      TinyLfuEntry tail = head.prev;
      head.prev = this;
      tail.next = this;
      next = head;
      prev = tail;
    }

    /** Removes the entry from the list. */
    public void remove() {
      prev.next = next;
      next.prev = prev;
      next = prev = null;
    }
  }

  private final class TinyLfuPolicy implements ConfigurableCacheMap.EvictionPolicy {

    @Override
    public String getName() {
      return "TinyLfu";
    }

    @Override
    public void entryTouched(ConfigurableCacheMap.Entry entry) {
      onHit((TinyLfuEntry) entry);
    }

    @Override
    public void requestEviction(int cMaximum) {
      evict();
    }
  }
}
