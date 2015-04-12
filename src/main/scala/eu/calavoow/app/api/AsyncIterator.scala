package eu.calavoow.app.api

import concurrent.{ExecutionContext, Future}

/**
 * An asynchronous iterator is a non-blocking iterator.
 *
 * Because hasNext must have a non-blocking return type (Future[Boolean]),
 * it is not possible to implement Iterable/Iterator.
 * It is however possible to implement foreach.
 *
 * @tparam T The type to iterate over.
 * @param ec The execution context to execute the iterator in.
 *           This parameter must be given already, otherwise Traversable cannot be implemented.
 */
abstract class AsyncIterator[+T](implicit ec: ExecutionContext) extends Traversable[Future[T]] {
	def hasNext : Future[Boolean]
	def next : Future[T]

	override def foreach[U](f: (Future[T]) ⇒ U): Unit = {
		def applyOnce() {
			hasNext.foreach { hNext ⇒
				if( hNext ) {
					f(next)
					applyOnce()
				}
			}
		}
		applyOnce()
	}
}
