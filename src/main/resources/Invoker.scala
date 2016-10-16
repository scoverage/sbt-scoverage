package scoverage

import java.io.{File, FileWriter}
import scala.collection.concurrent.TrieMap

/** @author Stephen Samuel */
object Invoker {

  private val MeasurementsPrefix = "scoverage.measurements."
  private val threadFiles = new ThreadLocal[TrieMap[String, FileWriter]]
  private val ids = TrieMap.empty[(String, Int), Any]

  /**
   * We record that the given id has been invoked by appending its id to the coverage
   * data file.
   *
   * This will happen concurrently on as many threads as the application is using,
   * so we use one file per thread, named for the thread id.
   *
   * This method is not thread-safe if the threads are in different JVMs, because
   * the thread IDs may collide.
   * You may not use `scoverage` on multiple processes in parallel without risking
   * corruption of the measurement file.
   *
   * @param id the id of the statement that was invoked
   * @param dataDir the directory where the measurement data is held
   */
  def invoked(id: Int, dataDir: String): Unit = {
    // [sam] we can do this simple check to save writing out to a file.
    // This won't work across JVMs but since there's no harm in writing out the same id multiple
    // times since for coverage we only care about 1 or more, (it just slows things down to
    // do it more than once), anything we can do to help is good. This helps especially with code
    // that is executed many times quickly, eg tight loops.
    if (!ids.contains((dataDir, id))) {
      // Each thread writes to a separate measurement file, to reduce contention
      // and because file appends via FileWriter are not atomic on Windows.
      var files = threadFiles.get()
      if (files == null) {
        files = TrieMap.empty[String, FileWriter]
        threadFiles.set(files)
      }
      val writer = files.getOrElseUpdate(dataDir, new FileWriter(measurementFile(dataDir), true))
      writer.append(id.toString + '\n').flush()

      ids.put((dataDir, id), ())
    }
  }

  def measurementFile(dataDir: File): File = measurementFile(dataDir.getAbsolutePath)
  def measurementFile(dataDir: String): File = new File(dataDir, MeasurementsPrefix + Thread.currentThread.getId)
}
