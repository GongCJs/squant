package com.squant.cheetah.datasource

import java.io.{File, FileWriter}
import java.nio.file.Files
import java.time.LocalDateTime

import com.squant.cheetah.domain._
import com.squant.cheetah.engine.DataBase
import com.squant.cheetah.utils.Constants._
import com.squant.cheetah.utils._
import com.typesafe.scalalogging.LazyLogging

import scala.io.Source

object StockBasicsSource extends DataSource with LazyLogging {
  private val url = "http://218.244.146.57/static/all.csv"

  private val path = config.getString(CONFIG_PATH_DB_BASE)
  private val name = config.getString(CONFIG_PATH_STOCKS)
  private val tableName = "stock_tables"

  //初始化数据源
  override def init(): Unit = {
    clear()
    update()
  }

  //每个周期更新数据
  override def update(start: LocalDateTime = LocalDateTime.now(),
                      stop: LocalDateTime = LocalDateTime.now()): Unit = {
    toCSV(stop)
    toDB(tableName, DataBase.getEngine)
  }

  def toCSV(date: LocalDateTime): Unit = {
    val content = Source.fromURL(url, "gbk").mkString
    val sourceFile = new File(s"$path/$name")
    val writer = new FileWriter(sourceFile, false)
    writer.write(content)
    writer.close()

    if (!sourceFile.exists()) {
      logger.error("fail to download stock basics data.")
      return true
    }

    val dest = new File(s"$path/$name-${format(date, "yyyyMMdd")}")
    if (dest.exists()) {
      dest.delete()
    }
    Files.copy(sourceFile.toPath, dest.toPath)
  }

  def toDB(tableName: String, engine: DataBase): Unit = {
    val symbols = Symbol.csvToSymbols(s"$path/$name").map(symbol => Symbol.symbolToRow(symbol)).toList
    engine.toDB(tableName, symbols)
  }

  //清空数据源
  override def clear(): Unit = {
    rm(s"$path/$name")
    DataBase.getEngine.deleteTable(tableName)
  }
}
