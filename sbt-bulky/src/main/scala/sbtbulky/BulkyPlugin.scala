package sbtbulky

import sbt.Keys._
import sbt._
import sbt.complete.DefaultParsers._
import sbt.internal.util.complete.Parsers.spaceDelimited

import java.io.File
import java.nio.file.Files

object BulkyPlugin extends AutoPlugin {
  //lazy val optionalUnsignedParser: Parser[Option[Int]] = NatBasic.?
  //lazy val spaceParser: Parser[Seq[String]] = spaceDelimited("<threshold>")
  //lazy val optionalUnsigned: Parser[Seq[Char]] = charClass(_.isDigit)

  object autoImport {
    val bulkyThresholdInLines = settingKey[Int](
      "The smallest number of lines to consider in source files.")
    val bulkySources = inputKey[Seq[(Int, File)]](
      "Sort by descending source files according to their number of lines.")
  }
  import autoImport._
  override lazy val globalSettings = Seq(
    bulkyThresholdInLines := 100
  )

  def convertToInt(arg: Option[String]): Option[Int] = arg match {
    case None => Some(100)
    case Some(arg) =>
      if (arg.forall(_.isDigit)) {
        val intArg = arg.toInt
        if (intArg >= 0) Some(intArg) else None
      } else None
  }

  def sortBulkyFiles(files: Seq[File], threshold: Int): Seq[(Int, File)] =
    files
      .map { file => (Files.lines(file.toPath).count.toInt, file) }
      .filter(_._1 >= threshold)
      .sortWith(_._1 > _._1)

  override lazy val projectSettings = Seq(
    Compile / bulkySources := {
      val args = spaceDelimited("<threshold>").parsed
      val thresholdOpt = convertToInt(args.headOption)
      if (thresholdOpt.nonEmpty) bulkyThresholdInLines := thresholdOpt.get else failure("You have to provide an unsigned integer")
      sortBulkyFiles((sources in Compile).value, bulkyThresholdInLines.value)
    },
    Test / bulkySources := {
      val args = spaceDelimited("<threshold>").parsed
      val thresholdOpt = convertToInt(args.headOption)
      if (thresholdOpt.nonEmpty) bulkyThresholdInLines := thresholdOpt.get else failure("You have to provide an unsigned integer")
      sortBulkyFiles((sources in Test).value, bulkyThresholdInLines.value)
    }
  )
}
