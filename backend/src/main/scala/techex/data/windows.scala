package techex.data

import org.joda.time.{Duration, ReadableInstant}

import scala.collection.immutable.TreeSet

object windows {

  def sized[A](n: Int) =
    SizeWindow(Vector[A](), n)


  def time[A](duration: Duration, f: A => ReadableInstant) = {
    val ord = Ordering.by((a: A) => f(a).getMillis)
    TimeWindow[A](TreeSet[A]()(ord), duration, f)
  }
}

trait Window[A] {
  def ::(a: A): Window[A]

  def :::(as: List[A]): Window[A]

  def length: Long

  def contents: Seq[A]

  def isEmpty: Boolean
}


case class SizeWindow[A](v: Vector[A], size: Int) extends Window[A] {
  override def ::(a: A): Window[A] = {
    SizeWindow((a +: v).drop(v.length - size), size)
  }

  override def length: Long =
    v.length


  override def contents: Seq[A] =
    v.toSeq


  override def isEmpty =
    v.isEmpty

  override def :::(as: List[A]): Window[A] =
    SizeWindow((as ++: v).drop(v.length - size), size)

}


case class TimeWindow[A](v: TreeSet[A], duration: Duration, f: A => ReadableInstant) extends Window[A] {

  val self: Window[A] = this

  override def ::(a: A): Window[A] = {
    val winStart = f(a).toInstant.minus(duration)
    TimeWindow((v + a).dropWhile(elem => f(elem).isBefore(winStart)), duration, f)
  }

  override def length: Long =
    v.size


  override def contents: Seq[A] =
    v.toSeq


  override def isEmpty = v.isEmpty

  override def :::(as: List[A]): Window[A] =
    as.foldLeft(self)((window, elem) => elem :: window)

}