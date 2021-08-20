package ipogudin

import com.github.mrpowers.spark.fast.tests.RDDComparer
import org.scalatest.FunSpec

class RddSpec
  extends FunSpec
  with SparkSessionTestWrapper
  with RDDComparer {

  describe("rdd") {
    it("rdd counting characters") {
      val actualRDD = spark.sparkContext
        .makeRDD(Seq("one", "two", "three"))
        .flatMap(_.toList)
        .map((_, 1))
        .reduceByKey(_ + _)

      val expectedRDD = spark.sparkContext
        .makeRDD(
          Seq(('w',1), ('e', 3), ('t', 2), ('h', 1), ('o', 2), ('n', 1), ('r', 1)))

      assertSmallRDDEquality(actualRDD.sortByKey(), expectedRDD.sortByKey())
    }
  }

}
