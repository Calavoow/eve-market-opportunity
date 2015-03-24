package eu.calavoow.app.util

import scala.util.{Success,Failure,Try}

object Util {
	/**
	 * Recover a Failure of f up to `times`.
	 * @param times The number of times to retry (thus total calls = 1 + retries)
	 * @param f The function to `recoverWith` a Failure
	 * @tparam T The Try type
	 * @return A Try of the given function f.
	 */
	def retry[T](times: Int)(f: ⇒ Try[T]) : Try[T] = {
		val firstTry = f
		// Try recovering with f, `times` times.
		Range(0,times).foldLeft(firstTry) { (cur, _) ⇒
			cur.recoverWith({case _ ⇒ f})
		}
	}
}
