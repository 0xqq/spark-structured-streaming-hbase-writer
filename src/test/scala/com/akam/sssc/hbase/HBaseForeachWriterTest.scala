package com.akam.sssc.hbase

import java.util.UUID
import java.util.concurrent.{ExecutorService, Executors}

import org.apache.commons.lang.SerializationUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.security.User
import org.apache.hadoop.hbase.security.User.SecureHadoopUser
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class HBaseForeachWriterTest extends FunSuite with BeforeAndAfterAll {

  val tn: String = "hbase-writer-test-" + UUID.randomUUID()
  val hbRes: Seq[String] = Seq("core-site.xml", "hbase-site.xml")
  val cf: String = "any-columnFamily-name"
  val cn: String = "column-name"

  var connection: Connection = _
  var hTable: Table = _
  var admin: Admin = _
  var hbaseConfig: Configuration = _

  override def beforeAll(): Unit = {
    hbaseConfig = HBaseConfiguration.create()
    hbRes.foreach(hbaseConfig.addResource)
    connection = createConnection()
    Thread.sleep(1000)
    admin = connection.getAdmin
    hTable = connection.getTable(TableName.valueOf(tn))
    createTable()
  }

  override def afterAll(): Unit = {
    dropTable()
    hTable.close()
    connection.close()
  }


  test("Should be Serializable/Deserializable with Java") {
    //Given
    val exp = new HBaseForeachWriter[String]() {
      override val tableName: String = tn
      override val hbaseConfResources: Seq[String] = hbRes

      override def toPut(record: String): Put = ???

      override def pool: Option[ExecutorService] = Some(Executors.newFixedThreadPool(1))

      override def user: Option[User] = Some(new SecureHadoopUser())

    }
    //When
    val out = SerializationUtils.serialize(exp)
    val act = SerializationUtils.deserialize(out).asInstanceOf[HBaseForeachWriter[String]]
    //Then
    assert(act.tableName == exp.tableName)
    assert(act.hbaseConfResources == exp.hbaseConfResources)
    assert(act.user.isDefined)
    assert(act.pool.isDefined)
  }

  test("Should create connection to Hbase") {
    //Given
    val writer = new SimpleWriterImpl()
    //When
    val connection = writer.createConnection()
    //Then
    assert(connection != null)
    assert(!connection.isClosed)
    connection.close()
  }

  test("Should get the Hbase table") {
    //Given
    val writer = new SimpleWriterImpl()
    val connection = writer.createConnection()
    //When
    val table = writer.getHTable(connection)
    //Then
    assert(table.getName.toString === writer.tableName)
    table.close()
  }

  test("Should close Hbase connection") {
    //Given
    var connection: Connection = null
    val writer = new SimpleWriterImpl() {
      override def createConnection(): Connection = {
        val hbaseConfig = HBaseConfiguration.create()
        hbaseConfResources.foreach(hbaseConfig.addResource)
        connection = ConnectionFactory.createConnection(hbaseConfig, pool.orNull, user.orNull)
        connection
      }
    }
    //When
    writer.open(1, 1)
    writer.close(null)
    //Then
    assert(connection.isClosed)
  }

  test("Should write/read to hbase table") {
    //Given
    val writer = new SimpleWriterImpl
    val record = Record("key-1", "value-1")
    //When
    writer.open(1, 1)
    writer.process(record)
    //Then
    val result = hTable.get(new Get(Bytes.toBytes(record.key)))
    val byteValue = result.getValue(Bytes.toBytes("any-columnFamily-name"), Bytes.toBytes("column-name"))
    val value = Bytes.toString(byteValue)
    assert(value === record.value)
  }

  private def createConnection(): Connection = {
    ConnectionFactory.createConnection(hbaseConfig)
  }


  private def createTable(): Unit = {
    val tableName = TableName.valueOf(tn)
    val desc: TableDescriptor = TableDescriptorBuilder.newBuilder(tableName)
      .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(cf)).build())
      .build()
    admin.createTable(desc)
  }

  private def dropTable(): Unit = {
    val tableName = TableName.valueOf(tn)
    admin.disableTable(TableName.valueOf(tn))
    admin.deleteTable(tableName)
  }

  class SimpleWriterImpl extends HBaseForeachWriter[Record] {
    override val tableName: String = tn
    override val hbaseConfResources: Seq[String] = hbRes

    override def toPut(record: Record): Put = {
      val p = new Put(Bytes.toBytes(record.key))
      p.addColumn(Bytes.toBytes(cf), Bytes.toBytes(cn), Bytes.toBytes(record.value))
      p
    }
  }

  case class Record(key: String, value: String)

}
