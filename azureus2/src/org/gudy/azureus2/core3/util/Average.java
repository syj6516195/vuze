/*
 * Created on june 12th, 2003
 *
 */
package org.gudy.azureus2.core3.util;

/**
 * 
 * This class is used to compute average (mostly for speed transfer).
 *
 * @author Olivier
 * 
 */
public class Average {
  /**
  * It uses a simple array of longs to store values in a cycling way.
  * The array has 2 more elements than really needed to compute the average.
  * One is the next one to be filled, and its value is always 0,
  * and the other one is the one currently filled,
  * which value is not taken into account for the average.
  */

  //The refresh rate of the average (ms)
  private int refreshRate;

  //the period (in ms)
  private int period;

  //The number of elements in the average
  private int nbElements;

  //The time the average was last updated (divided by the refreshRate).
  private long lastUpdate;

  //The values
  private long values[];

  /**
   * Private constructor for an Average
   * @param _refreshRate the refresh rate in ms
   * @param _period the period in s
   */
  private Average(int _refreshRate, int _period) {
    this.refreshRate = _refreshRate;
    this.period = _period;

    this.nbElements = (_period * 1000) / _refreshRate + 2;
    this.lastUpdate = SystemTime.getCurrentTime() / _refreshRate;
    this.values = new long[this.nbElements];
  }

  /**
   * The way to get a new Average Object, it does some parameter checking.
   * refreshRate must be greater than 100,
   * and period*1000 must be greater than refreshRate
   * @param refreshRate in ms
   * @param period in s
   * @return the newlly created Average, or null if parameters are wrong
   */
  public static Average getInstance(int refreshRate, int period) {
    if (refreshRate < 100)
      return null;
    if ((period * 1000) < refreshRate)
      return null;
    return new Average(refreshRate, period);
  }

  /**
   * This method is used to update the buffer tha stores the values,
   * in fact it mostly does clean-up over this buffer,
   * erasing all values that have not been updated.
   * @param timeFactor which is the currentTime divided by the refresh Rate
   */
  private synchronized void update(long timeFactor) {
    //If we have a really OLD lastUpdate, we could erase the buffer a 
    //huge number of time, so if it's really old, we change it so we'll only
    //erase the buffer once.
    if (lastUpdate < timeFactor - nbElements)
      lastUpdate = timeFactor - nbElements - 1;

    //For all values between lastUpdate + 1 (next value than last updated)
    //and timeFactor (which is the new value insertion position) 
    for (long i = lastUpdate + 1; i < timeFactor; i++) {
      //We set the value to 0.
      values[(int) (i % nbElements)] = 0;
    }
    //We also clear the next value to be inserted (so on next time change...)
    values[(int) ((timeFactor + 1) % nbElements)] = 0;

    //And we update lastUpdate.
    lastUpdate = timeFactor;
  }

  /**
   * Public method to add a value to the average,
   * the time it is added is the time this method is called.
   * @param value the value to be added to the Average
   */
  public void addValue(long value) {
    //We get the current time factor.
    long timeFactor = SystemTime.getCurrentTime() / refreshRate;
    //We first update the buffer.
    update(timeFactor);
    //And then we add our value to current element
    values[(int) (timeFactor % nbElements)] += value;
  }

  /**
   * This method can be called to get the current average value.
   * @return the current Average computed.
   */
  public long getAverage() {
    //We get the current timeFactor
    long timeFactor = SystemTime.getCurrentTime() / refreshRate;
    //We first update the buffer
    update(timeFactor);

    //The sum of all elements used for the average.
    long sum = 0;

    //Starting on oldest one (the one after the next one)
    //Ending on last one fully updated (the one previous current one)
    for (long i = timeFactor + 2; i < timeFactor + nbElements; i++) {
      //Simple addition
      sum += values[(int) (i % nbElements)];
    }

    //We return the sum divided by the period
    return(sum / period);
  }
}
