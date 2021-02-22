package sbtbulky

import sbt.Keys._
import sbt._
import sbt.complete.DefaultParsers._
import sbt.internal.util.complete.Parser

import java.io.File
import java.nio.file.Files

object BulkyPlugin extends AutoPlugin {
  lazy val optionalUnsignedParser: Parser[Option[Int]] = NatBasic.?
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

  def sortBulkyFiles(files: Seq[File], threshold: Int): Seq[(Int, File)] =
    files
      .map { file => (Files.lines(file.toPath).count.toInt, file) }
      .filter(_._1 >= threshold)
      .sortWith(_._1 > _._1)

  def thresholdParser(display: String): Parser[Option[Int]] =
    (token(Space) ~> token(optionalUnsignedParser, display)) | token(optionalUnsignedParser, display)

  override lazy val projectSettings = Seq(
    Compile / bulkySources := {
      val thresholdOpt = thresholdParser("<threshold>").parsed
      val threshold = thresholdOpt.getOrElse(bulkyThresholdInLines.value)
      sortBulkyFiles((sources in Compile).value, threshold)
    },
    Test / bulkySources := {
      val thresholdOpt = thresholdParser("<threshold>").parsed
      val threshold = thresholdOpt.getOrElse(bulkyThresholdInLines.value)
      sortBulkyFiles((sources in Test).value, threshold)
    }
  )
}
