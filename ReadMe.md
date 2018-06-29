### How to use the library : //In Progress

#### for example :  

 - run ```nc -lk 9999```

 - Create spark Session :  
 ```
 val spark = SparkSession
 .builder
 .master("local[*]")
 .appName("StructuredStreamingApp")
 .getOrCreate()
 ```
    
 - read stream 
 
```
 val lines = spark.readStream
       .format("socket")
       .option("host", "localhost")
       .option("port", 9999)
       .load()
```
    
 - write to hbase 
```
    val query = lines
       .writeStream
       .foreach(new HBaseForeachWriter[WhatEverYourDataType] {
                        override val tableName: String = "hbase-table-name"
                        //your cluster files , i assume here it is in resources  
                        override val hbaseConfResources: Seq[String] = Seq("core-site.xml", "hbase-site.xml") 
                    
                        override def toPut(record: WhatEverYourDataType): Put = {
                          val key = .....
                          val columnFamaliyName : String = ....
                          val columnName : String = ....
                          val columnValue = ....
                          
                          val p = new Put(Bytes.toBytes(key))
                          //Add columns ...
                          p.addColumn(Bytes.toBytes(columnFamaliyName),
                           Bytes.toBytes(columnName), 
                           Bytes.toBytes(columnValue))
                           
                          p
                        }
                        
                      }
       ).start()
   
     query.awaitTermination()
     
``` 

- send messages using nc and check your hbase

- To run the test

```
sbt test
``` 

to package lib : 

```
sbt assembly
```
