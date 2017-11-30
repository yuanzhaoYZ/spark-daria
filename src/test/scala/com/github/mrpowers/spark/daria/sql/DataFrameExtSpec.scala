package com.github.mrpowers.spark.daria.sql

import org.scalatest.FunSpec
import SparkSessionExt._
import org.apache.spark.sql.types.{IntegerType, StringType}
import DataFrameExt._
import com.github.mrpowers.spark.fast.tests.DataFrameComparer
import org.apache.spark.sql.DataFrame

class DataFrameExtSpec
    extends FunSpec
    with DataFrameComparer
    with SparkSessionTestWrapper {

  describe("#printSchemaInCodeFormat") {

    it("prints the schema in a code friendly format") {

      val sourceDF = spark.createDF(
        List(
          ("jets", "football", 45),
          ("nacional", "soccer", 10)
        ), List(
          ("team", StringType, true),
          ("sport", StringType, true),
          ("goals_for", IntegerType, true)
        )
      )

      //      uncomment the next line if you want to check out the console output
      //      sourceDF.printSchemaInCodeFormat()

    }

  }

  describe("#composeTransforms") {

    it("runs a list of transforms") {

      val sourceDF = spark.createDF(
        List(
          ("jets"),
          ("nacional")
        ), List(
          ("team", StringType, true)
        )
      )

      val transforms = List(
        ExampleTransforms.withGreeting()(_),
        ExampleTransforms.withCat("sandy")(_)
      )

      val actualDF = sourceDF.composeTransforms(transforms)

      val expectedDF = spark.createDF(
        List(
          ("jets", "hello world", "sandy meow"),
          ("nacional", "hello world", "sandy meow")
        ), List(
          ("team", StringType, true),
          ("greeting", StringType, false),
          ("cats", StringType, false)
        )
      )

      assertSmallDataFrameEquality(actualDF, expectedDF)

    }

  }

  describe("#containsColumn") {

    it("returns true if a DataFrame contains a column") {

      val sourceDF = spark.createDF(
        List(
          ("jets"),
          ("nacional")
        ), List(
          ("team", StringType, true)
        )
      )

      assert(sourceDF.containsColumn("team") === true)
      assert(sourceDF.containsColumn("blah") === false)

    }

  }

  describe("#columnDiff") {

    it("returns the columns in otherDF that aren't in df") {

      val sourceDF = spark.createDF(
        List(
          ("jets", "USA"),
          ("nacional", "Colombia")
        ), List(
          ("team", StringType, true),
          ("country", StringType, true)
        )
      )

      val otherDF = spark.createDF(
        List(
          ("jets"),
          ("nacional")
        ), List(
          ("team", StringType, true)
        )
      )

      val cols = sourceDF.columnDiff(otherDF)

      assert(cols === Seq("country"))

    }

  }

  describe("#trans") {

    it("works normally when the custom transformation appends the required columns") {

      val sourceDF = spark.createDF(
        List(
          ("jets"),
          ("nacional")
        ), List(
          ("team", StringType, true)
        )
      )

      val ct = CustomTransform(
        transform = ExampleTransforms.withGreeting(),
        columnsAdded = Seq("greeting"),
        columnsRemoved = Seq(): Seq[String]
      )

      val actualDF = sourceDF.trans(ct)

      val expectedDF = spark.createDF(
        List(
          ("jets", "hello world"),
          ("nacional", "hello world")
        ), List(
          ("team", StringType, true),
          ("greeting", StringType, false)
        )
      )

      assertSmallDataFrameEquality(actualDF, expectedDF)

    }

    it("errors out if the column that's being added already exists") {

      val sourceDF = spark.createDF(
        List(
          ("jets", "hi"),
          ("nacional", "hey")
        ), List(
          ("team", StringType, true),
          ("greeting", StringType, true)
        )
      )

      val ct = CustomTransform(
        transform = ExampleTransforms.withGreeting(),
        columnsAdded = Seq("greeting"),
        columnsRemoved = Seq(): Seq[String]
      )

      intercept[DataFrameColumnsException] {
        sourceDF.trans(ct)
      }

    }

    it("errors out if the column that's being dropped doesn't exist") {

      val sourceDF = spark.createDF(
        List(
          ("jets"),
          ("nacional")
        ), List(
          ("team", StringType, true)
        )
      )

      val ct = CustomTransform(
        transform = ExampleTransforms.withGreeting(),
        columnsAdded = Seq("greeting"),
        columnsRemoved = Seq("foo")
      )

      intercept[DataFrameColumnsException] {
        sourceDF.trans(ct)
      }

    }

    it("errors out if the column isn't actually added") {

      val sourceDF = spark.createDF(
        List(
          ("jets", "hi")
        ), List(
          ("team", StringType, true)
        )
      )

      val ct = CustomTransform(
        transform = ExampleTransforms.withCat("sandy"),
        columnsAdded = Seq("greeting"),
        columnsRemoved = Seq(): Seq[String]
      )

      intercept[DataFrameColumnsException] {
        sourceDF.trans(ct)
      }

    }

    it("allows custom transformations to be chained") {

      val sourceDF = spark.createDF(
        List(
          ("jets")
        ), List(
          ("team", StringType, true)
        )
      )

      val actualDF = sourceDF
        .trans(
          CustomTransform(
            transform = ExampleTransforms.withGreeting(),
            columnsAdded = Seq("greeting"),
            columnsRemoved = Seq(): Seq[String]
          )
        )
        .trans(
          CustomTransform(
            transform = ExampleTransforms.withCat("spanky"),
            columnsAdded = Seq("cats"),
            columnsRemoved = Seq(): Seq[String]
          )
        )

      val expectedDF = spark.createDF(
        List(
          ("jets", "hello world", "spanky meow")
        ), List(
          ("team", StringType, true),
          ("greeting", StringType, false),
          ("cats", StringType, false)
        )
      )

      assertSmallDataFrameEquality(actualDF, expectedDF)

    }

    it("throws an exception if a column is not removed") {

      val sourceDF = spark.createDF(
        List(
          ("jets", "hi")
        ), List(
          ("team", StringType, true),
          ("word", StringType, true)
        )
      )

      val ct = CustomTransform(
        transform = ExampleTransforms.withGreeting(),
        columnsAdded = Seq("greeting"),
        columnsRemoved = Seq("word")
      )

      intercept[DataFrameColumnsException] {
        sourceDF.trans(ct)
      }

    }

    it("works if the columns that are removed are properly specified") {

      val sourceDF = spark.createDF(
        List(
          ("jets", "hi")
        ), List(
          ("team", StringType, true),
          ("word", StringType, true)
        )
      )

      val ct = CustomTransform(
        transform = ExampleTransforms.dropWordCol(),
        columnsAdded = Seq(): Seq[String],
        columnsRemoved = Seq("word")
      )

      val actualDF = sourceDF.trans(ct)

      val expectedDF = spark.createDF(
        List(
          ("jets")
        ), List(
          ("team", StringType, true)
        )
      )

      assertSmallDataFrameEquality(actualDF, expectedDF)

    }

  }

}
