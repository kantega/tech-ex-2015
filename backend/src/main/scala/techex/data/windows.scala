package techex.data

class windows {

}

trait Window[A]{
  def + (a: A): Window[A]
  def ++(as:List[A]):Window[A]
  def append(a:A): Window[A] = this.+(a)
  def length:Long
  def contents:Seq[A]
  def isEmpty:Boolean
}


case class VectorWindow[A](v:Vector[A],size:Int) extends Window[A]{
  override def +(a: A): Window[A] = {
    VectorWindow((v :+ a).drop(v.length-size),size)
  }

  override def length: Long = {
    v.length
  }

  override def contents: Seq[A] = {
    v.toSeq
  }

  override def isEmpty = v.isEmpty

  override def ++(as: List[A]): Window[A] = {
    VectorWindow((v ++ as).drop(v.length-size),size)
  }
}