import org.apache.spark.{SparkConf, SparkContext}


import org.apache.spark.SparkContext._
import org.apache.spark.sql.types._
import org.apache.spark.sql.Row

import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.types.{StructType,StructField,DecimalType,IntegerType,LongType,StringType}

import org.apache.spark.sql.SparkSession

import org.apache.spark.sql.types.{StructType,StructField,DecimalType,IntegerType,LongType,StringType}

import scala.collection.mutable.ListBuffer
import com.github.nscala_time.time.Imports._




class SimilarFunc2(val theaterA: String , val theaterB: String) {
 var A: String= theaterA;
 var B: String = theaterB;
 def simi() : Float= {
  val Alist=A.toLowerCase.split(" ")
  val Blist=B.toLowerCase.split(" ")

  val inte=Alist.intersect(Blist)

  var similarity=(inte.length.toFloat/Alist.length.toFloat+inte.length.toFloat/Blist.length.toFloat)/(2.toFloat)

  return similarity
 }
}

object PlaceiqFactualTheater
{
def main(args:Array[String]){

val tempS3Dir = "s3n://redshiftspark/temp/"
 val awsAccessKey = args(0)
val awsSecretKey = args(1)
val rsDbName = args(2)
val rsUser = args(3)
val rsPassword = args(4)
val rsURL = args(5)
val jdbcURL = s"""jdbc:redshift://$rsURL/$rsDbName?user=$rsUser&password=$rsPassword"""
println(jdbcURL)


val sc = new SparkContext(new SparkConf().setAppName("SparkSQL").setMaster("local"))
sc.setLogLevel("ERROR")

val sqlContext = new SQLContext(sc)
import sqlContext.implicits._;



val spark=SparkSession.builder
    .master("local[*]")
    .config("spark.driver.cores",1)
    .appName("UnderstandingSparkSession")
    .getOrCreate()


import org.apache.spark.rdd.PairRDDFunctions;

sc.hadoopConfiguration.set("fs.s3n.awsAccessKeyId", awsAccessKey)
sc.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey", awsSecretKey)
sc.hadoopConfiguration.set("fs.s3n.awsAccessKeyId", awsAccessKey)
sc.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey", awsSecretKey)


//var zoneNYC = DateTimeZone.forID("America/New_York")
//var thisweekYear = args(6).toInt
//var thisweekMonth = args(7).toInt
//var thisweekDay = args(8).toInt

//var thisweekEnd = new DateTime(thisweekYear, thisweekMonth, thisweekDay, 0, 0, zoneNYC)
//var thisweekBegin=thisweekEnd.minusDays(8)
//var lastweekEnd=thisweekEnd.minusDays(7)
//var lastweekBegin=thisweekEnd.minusDays(15)
//var toDate=thisweekEnd.minusDays(37)



//var thisweekEnd1=thisweekEnd.toString.split("T")(0)
//var thisweekBegin1=thisweekBegin.toString.split("T")(0)
//var lastweekEnd1=lastweekEnd.toString.split("T")(0)
//var lastweekBegin1=lastweekBegin.toString.split("T")(0)
//var toDate1=toDate.toString.split("T")(0)


val outputdir="s3n://blindal-datafactory-output/blindal-segments"

val file= sc.textFile("s3n://fox-geodata-partners/factual/*.tsv.gz").persist




val split=file.map(line =>line.split("\t"))



// The schema is encoded in a string
val schemaString = "device type date daytype name city state"

// Generate the schema based on the string of schema
val fields = schemaString.split(" ").map(fieldName => StructField(fieldName, StringType, nullable = true))
val schema = StructType(fields)

// Convert records of the RDD (people) to Rows
val rowRDD = file.map(_.split("\t")).filter(_.size > 6).map(attributes => Row(attributes(0), attributes(1),attributes(2),attributes(3),attributes(4),attributes(5),attributes(6).trim))

// Apply the schema to the RDD

val factualDF = spark.createDataFrame(rowRDD, schema)

// Creates a temporary view using the DataFrame
factualDF.createOrReplaceTempView("factual")

// SQL can be run over a temporary view created using DataFrames
val theater_name = spark.sql(s""" select name, city, state  from factual group by name,city,state """)
val rows=theater_name.collect()
val theater1= sc.textFile("s3n://redshiftspark/table/Theaters.tsv").persist

val theater_split=theater1.map(_.split("\t"))


val schemaStringtheater = "seq circuit name address city state area boxoffice"

// Generate the schema based on the string of schema
val fieldsTheater = schemaStringtheater.split(" ").map(fieldName => StructField(fieldName, StringType, nullable = true))
val schemaTheater = StructType(fieldsTheater)

// Convert records of the RDD (people) to Rows
val rowRDDTheater = theater1.map(_.split("\t")).filter(_.size > 7).map(attributes => Row(attributes(0), attributes(1),attributes(2),attributes(3),attributes(4),attributes(5),attributes(6),attributes(7).trim))

// Apply the schema to the RDD

val theaterDF = spark.createDataFrame(rowRDDTheater, schemaTheater)

// Creates a temporary view using the DataFrame
theaterDF.createOrReplaceTempView("theater")


//val theater_split=theater.map(line =>line.split("\\|")).filter(_.size>7)

//val placeiq500DF = spark.sql(s""" select device from factual where  name in '${TsString500}'  """)

//val theater_split_zip = theater_split.zipWithIndex
val theater99 =  spark.sql(s""" select name,city,state from theater where seq<=99  """)


val theater249=spark.sql(s""" select name,city,state from theater where seq>99 and seq<=249  """)

val theater500=spark.sql(s""" select name,city,state from theater where seq>249  """)

val theaterall=spark.sql(s""" select name,city,state from theater  """)
 var Tm=scala.collection.mutable.Map[String,List[String]]()

//println(theater99.toDF.rdd.count)
//println(theater249.toDF.rdd.count)
//println(theater500.toDF.rdd.count)
//println(theaterall.toDF.rdd.count)

rows.foreach {  f =>
      val fName = f.getString(0)
      val fCity = f.getString(1)
      val fState=f.getString(2)
       Tm+=(fCity.toLowerCase+" "+fState.toLowerCase->List(fName))
       if(Tm contains fCity.toLowerCase+" "+fState.toLowerCase )
       {Tm(fCity.toLowerCase+" "+fState.toLowerCase)=fName::Tm(fCity.toLowerCase+" "+fState.toLowerCase)}
       else
       {Tm(fCity.toLowerCase+" "+fState.toLowerCase)=Tm(fCity.toLowerCase+" "+fState.toLowerCase)}



    }


var Lm=scala.collection.mutable.Map[String,List[String]]()


theaterall.collect().foreach {  f =>
      val fName = f.getString(0)
      val fCity = f.getString(1)
      val fState=f.getString(2)
       Lm+=(fCity.toLowerCase+" "+fState.toLowerCase->List(fName))
       if(Lm contains fCity.toLowerCase+" "+fState.toLowerCase )
       {Lm(fCity.toLowerCase+" "+fState.toLowerCase)=fName::Lm(fCity.toLowerCase+" "+fState.toLowerCase)}
       else
       {Lm(fCity.toLowerCase+" "+fState.toLowerCase)=Lm(fCity.toLowerCase+" "+fState.toLowerCase)}

}

var Lm99=scala.collection.mutable.Map[String,List[String]]()


theater99.collect().foreach {  f =>
      val fName = f.getString(0)
      val fCity = f.getString(1)
      val fState=f.getString(2)
       Lm99+=(fCity.toLowerCase+" "+fState.toLowerCase->List(fName))
       if(Lm99 contains fCity.toLowerCase+" "+fState.toLowerCase )
       {Lm99(fCity.toLowerCase+" "+fState.toLowerCase)=fName::Lm99(fCity.toLowerCase+" "+fState.toLowerCase)}
       else
       {Lm99(fCity.toLowerCase+" "+fState.toLowerCase)=Lm99(fCity.toLowerCase+" "+fState.toLowerCase)}

}


var Lm249=scala.collection.mutable.Map[String,List[String]]()


theater249.collect().foreach {  f =>
      val fName = f.getString(0)
      val fCity = f.getString(1)
      val fState=f.getString(2)
       Lm249+=(fCity.toLowerCase+" "+fState.toLowerCase->List(fName))
       if(Lm249 contains fCity.toLowerCase+" "+fState.toLowerCase )
       {Lm249(fCity.toLowerCase+" "+fState.toLowerCase)=fName::Lm249(fCity.toLowerCase+" "+fState.toLowerCase)}
       else
       {Lm249(fCity.toLowerCase+" "+fState.toLowerCase)=Lm249(fCity.toLowerCase+" "+fState.toLowerCase)}

}

var Lm500=scala.collection.mutable.Map[String,List[String]]()


theater500.collect().foreach {  f =>
      val fName = f.getString(0)
      val fCity = f.getString(1)
      val fState=f.getString(2)
       Lm500+=(fCity.toLowerCase+" "+fState.toLowerCase->List(fName))
       if(Lm500 contains fCity.toLowerCase+" "+fState.toLowerCase )
       {Lm500(fCity.toLowerCase+" "+fState.toLowerCase)=fName::Lm500(fCity.toLowerCase+" "+fState.toLowerCase)}
       else
       {Lm500(fCity.toLowerCase+" "+fState.toLowerCase)=Lm500(fCity.toLowerCase+" "+fState.toLowerCase)}

}




val fileplaceiq= sc.textFile("s3n://fox-geodata-partners/placeiq/theater_*.csv.gz").persist



val splitplaceiq=fileplaceiq.map(line =>line.split("\\|"))



// The schema is encoded in a string
val schemaStringplaceiq = "device date daytype name city state os"

// Generate the schema based on the string of schema
val fieldsplaceiq = schemaStringplaceiq.split(" ").map(fieldName => StructField(fieldName, StringType, nullable = true))
val schemaplaceiq = StructType(fieldsplaceiq)

// Convert records of the RDD (people) to Rows
val rowRDDplaceiq = fileplaceiq.map(_.split("\\|")).filter(_.size > 13).map(attributes => Row(attributes(0), attributes(1),attributes(2),attributes(6),attributes(10),attributes(11),attributes(13).trim))
val placeiqDFplaceiq = spark.createDataFrame(rowRDDplaceiq, schemaplaceiq)

// Creates a temporary view using the DataFrame
placeiqDFplaceiq.createOrReplaceTempView("placeiq")


val theater_name_placeiq = spark.sql(s""" select name,city,state from placeiq group by name,city,state""")


//println(theater99.toDF.show())



var Tm_placeiq=scala.collection.mutable.Map[String,List[String]]()


theater_name_placeiq.collect().foreach {  f =>
      val fName = f.getString(0)
      val fCity = f.getString(1)
      val fState=f.getString(2)
       Tm_placeiq+=(fCity.toLowerCase+" "+fState.toLowerCase->List(fName))
       if(Tm_placeiq contains fCity.toLowerCase+" "+fState.toLowerCase )
       {Tm_placeiq(fCity.toLowerCase+" "+fState.toLowerCase)=fName::Tm_placeiq(fCity.toLowerCase+" "+fState.toLowerCase)}
       else
       {Tm_placeiq(fCity.toLowerCase+" "+fState.toLowerCase)=Tm_placeiq(fCity.toLowerCase+" "+fState.toLowerCase)}



    }




var Ts99_placeiq = new ListBuffer[String]()





 for (key<-Tm_placeiq.keys) {



  if (Lm99.contains(key))

  { for (th1<-Tm_placeiq(key))
  {
  var Temp=scala.collection.mutable.Map[String,Float]()

  { for (th2<-Lm99(key))

  {

    val similar2 = new SimilarFunc2(th1, th2);

       Temp(th1)=similar2.simi()

  }
  }

  var Maxi=Temp.maxBy(_._2)

  if (Maxi._2>0.2)
  { Ts99_placeiq+=Maxi._1
  }
 }
 }
 }



var Ts99 = new ListBuffer[String]()





 for (key<-Tm.keys) {



  if (Lm99.contains(key))

  { for (th1<-Tm(key))
  {
  var Temp=scala.collection.mutable.Map[String,Float]()

  { for (th2<-Lm99(key))

  {

    val similar2 = new SimilarFunc2(th1, th2);

       Temp(th1)=similar2.simi()

  }
  }

  var Maxi=Temp.maxBy(_._2)

  if (Maxi._2>0.2)
  { Ts99+=Maxi._1
  }
 }
 }
 }


  val Ti99=for(e<-Ts99) yield e.replaceAll("\'" , "\\\\\'")



  val TsString99_placeiq = "('" + Ts99_placeiq.mkString("','") + "')"
  val Ti99_placeiq=for(e<-Ts99_placeiq) yield e.replaceAll("\'" , "\\\\\'")

val placeiqfactual99DF = spark.sql(s" select device from placeiq where  name in (${Ti99_placeiq.map ( x => "'" + x + "'").mkString(",") }) union select device from factual where  name in (${Ti99.map ( x => "'" + x + "'").mkString(",") })    ")
//val placeiq99DF = spark.sql(s" select device from placeiq limit 1000  ")


placeiqfactual99DF.toDF.repartition(1).write.mode("overwrite").format("com.databricks.spark.csv").option("header","True").save(outputdir+"/visitation_foxsearchlight_group1");


//println(placeiq99DF.toDF.rdd.count)

val count1 = placeiqfactual99DF.toDF.rdd.count

println(f"The count of device id of the placeiq and factual  99 segment is $count1%.0f ")

var Ts249_placeiq = new ListBuffer[String]()





 for (key<-Tm_placeiq.keys) {



  if (Lm249.contains(key))

  { for (th1<-Tm_placeiq(key))
  {
  var Temp=scala.collection.mutable.Map[String,Float]()

  { for (th2<-Lm249(key))

  {

    val similar2 = new SimilarFunc2(th1, th2);

       Temp(th1)=similar2.simi()

  }
  }

  var Maxi=Temp.maxBy(_._2)

  if (Maxi._2>0.2)
  { Ts249_placeiq+=Maxi._1
  }
 }
 }
 }


var Ts249 = new ListBuffer[String]()





 for (key<-Tm.keys) {



  if (Lm249.contains(key))

  { for (th1<-Tm(key))
  {
  var Temp=scala.collection.mutable.Map[String,Float]()

  { for (th2<-Lm249(key))

  {

    val similar2 = new SimilarFunc2(th1, th2);

       Temp(th1)=similar2.simi()

  }
  }

  var Maxi=Temp.maxBy(_._2)

  if (Maxi._2>0.2)
  { Ts249+=Maxi._1
  }
 }
 }
 }
  val TsString249 = "('" + Ts249.mkString("','") + "')"
  val Ti249=for(e<-Ts249) yield e.replaceAll("\'" , "\\\\\'")



  val Ti249_placeiq=for(e<-Ts249_placeiq) yield e.replaceAll("\'" , "\\\\\'")
val placeiqfactual249DF = spark.sql(s" select device from factual where  name in (${Ti249.map ( x => "'" + x + "'").mkString(",") }) union  select device from placeiq where  name in (${Ti249_placeiq.map ( x => "'" + x + "'").mkString(",") })  ")
val count2 = placeiqfactual249DF.toDF.rdd.count

println(f"The count of device id of the placeiq and factual 249 segment is $count2%.0f ")

placeiqfactual249DF.repartition(1).write.mode("overwrite").format("com.databricks.spark.csv").option("header","True").save(outputdir+"/visitation_foxsearchlight_group2");



var Ts500_placeiq = new ListBuffer[String]()





 for (key<-Tm_placeiq.keys) {



  if (Lm500.contains(key))

  { for (th1<-Tm_placeiq(key))
  {
  var Temp=scala.collection.mutable.Map[String,Float]()

  { for (th2<-Lm500(key))

  {

    val similar2 = new SimilarFunc2(th1, th2);

       Temp(th1)=similar2.simi()

  }
  }

  var Maxi=Temp.maxBy(_._2)

  if (Maxi._2>0.2)
  { Ts500_placeiq+=Maxi._1
  }
 }
 }
 }


var Ts500 = new ListBuffer[String]()





 for (key<-Tm.keys) {



  if (Lm500.contains(key))

  { for (th1<-Tm(key))
  {
  var Temp=scala.collection.mutable.Map[String,Float]()

  { for (th2<-Lm500(key))

  {

    val similar2 = new SimilarFunc2(th1, th2);

       Temp(th1)=similar2.simi()

  }
  }

  var Maxi=Temp.maxBy(_._2)

  if (Maxi._2>0.2)
  { Ts500+=Maxi._1
  }
 }
 }
 }

  val TsString500 = "('" + Ts500.mkString("','") + "')"
   val Ti500=for(e<-Ts500) yield e.replaceAll("\'" , "\\\\\'")



 val Ti500_placeiq=for(e<-Ts500_placeiq) yield e.replaceAll("\'" , "\\\\\'")


//println(TsString500)
val placeiqfactual500DF = spark.sql(s" select device from factual where  name in (${Ti500.map ( x => "'" + x + "'").mkString(",") }) union  select device from placeiq where  name in (${Ti500_placeiq.map ( x => "'" + x + "'").mkString(",") })  ")

val count3 = placeiqfactual500DF.toDF.rdd.count

println(f"The count of device id of the placeiq and factual 500 segment is $count3%.0f ")

placeiqfactual500DF.repartition(1).write.mode("overwrite").format("com.databricks.spark.csv").option("header","True").save(outputdir+"/visitation_foxsearchlight_group3");



var Ts_placeiq = new ListBuffer[String]()

  for (key<-Tm_placeiq.keys) {



  if (Lm.contains(key))

  { for (th1<-Tm_placeiq(key))
  {
  var Temp=scala.collection.mutable.Map[String,Float]()

  { for (th2<-Lm(key))

  {

    val similar2 = new SimilarFunc2(th1, th2);

       Temp(th1)=similar2.simi()

  }
  }

  var Maxi=Temp.maxBy(_._2)

  if (Maxi._2>0.2)
  { Ts_placeiq+=Maxi._1
  }
 }
 }
 }

  val Ti_placeiq=for(e<-Ts_placeiq) yield e.replaceAll("\'" , "\\\\\'")



  var Ts = new ListBuffer[String]()

  for (key<-Tm.keys) {



  if (Lm.contains(key))

  { for (th1<-Tm(key))
  {
  var Temp=scala.collection.mutable.Map[String,Float]()

  { for (th2<-Lm(key))

  {

    val similar2 = new SimilarFunc2(th1, th2);

       Temp(th1)=similar2.simi()

  }
  }

  var Maxi=Temp.maxBy(_._2)

  if (Maxi._2>0.2)
  { Ts+=Maxi._1
  }
 }
 }
 }

  val TsString = "('" + Ts.mkString("','") + "')"
  val Ti=for(e<-Ts) yield e.replaceAll("\'" , "\\\\\'")




val placeiqfactualDF1 = spark.sql(s" select device from factual where  name in (${Ti.map ( x => "'" + x + "'").mkString(",") }) union  select device from placeiq where  name in (${Ti_placeiq.map ( x => "'" + x + "'").mkString(",") })    ")


val count4 = placeiqfactualDF1.toDF.rdd.count

println(f"The count of device id of the placeiq and factual total segment is $count4%.0f ")
placeiqfactualDF1.repartition(1).write.mode("overwrite").format("com.databricks.spark.csv").option("header","True").save(outputdir+"/visitation_foxsearchlight_whole");







}

}
