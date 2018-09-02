//e.g. List(1,2,3).sorted
def sorted[B >: A](implicit ord: Ordering[B])

//e.g. List(1,2,3).sum
def sum[B >: A](implicit num: Numeric[B]): B
