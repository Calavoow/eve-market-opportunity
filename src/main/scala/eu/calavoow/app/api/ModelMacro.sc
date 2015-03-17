val x = List(2,1,3)

// 2
// 1
// res0: Int = 2
x.toStream.scanLeft(0) { (accum, y) ⇒
	println(y)
	accum + y
} takeWhile { _ < 3 } sum

// 2
// 1
// 3
// res1: Int = 2
x.view.scanLeft(0){ (accum, y) ⇒
	println(y)
	accum + y
} takeWhile { _ < 3 } sum