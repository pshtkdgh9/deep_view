package Master


import java.io.{File, FileWriter, PrintWriter}
import java.util
import scala.io.Source
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.broadcast.Broadcast
import Slave._

import sys.process._
import scala.collection.mutable.{ArrayBuffer, Map}

object main {
  //broadcast 변수 설정
  var broadKd : Broadcast[Master.ROOTINFO.type] = null;
  var broadHdi :Broadcast[Master.HDIClass] = null;
  val HDI = new HDIClass()

  def mapfunc(Partition: Int, iter: Iterator[((Array[Double],Array[String]), Long)]): Iterator[((Array[Double], Int), Int,Array[String])] = {
    iter.map(x => ((x._1._1, Partition.abs), x._2.toInt, x._1._2)).toIterator
  }

  var res_arr = new ArrayBuffer[(Array[(String,Double)],Double,Int)]()

  def main() = {
    val conf: SparkConf = new SparkConf()
    val sc         = SparkContext.getOrCreate(conf)
    val rootLogger = Logger.getRootLogger()
    rootLogger.setLevel(Level.ERROR)

    HDI.file_name    = "/root/spark-3.1.2-bin-hadoop3.2/data/lhb/yeon1000000.txt"
    HDI.split_number = 8
    val fname_01 = "/root/spark-3.1.2-bin-hadoop3.2/data/lhb/Query100.txt"
    val bufferdSource = Source.fromFile(fname_01)
    HDI.quant_value  = 16 // 양자화 시킬 차원의 수

    val domain = 256
    val voc    = domain / HDI.quant_value
    HDI.voc    = voc

    for (i <- 0 until HDI.dimension) HDI.mbr(i) = MBR()

    // Master KD-Tree 구축
    var RealDataRDD = sc
      .textFile("file:///" + HDI.file_name)
      .map(x => (x.split(" ")
        .take(HDI.featureSize)
        .map(_.toDouble), x.split(" ").takeRight(1)))
      .zipWithIndex()

    HDI.data_number = RealDataRDD.count().toInt
    broadHdi = sc.broadcast(HDI)

    var startmaster = System.currentTimeMillis()

    /*양자화*/
    val quantDataRDD = Quantization
      .processQuantization(
        RealDataRDD
          .map(x => x._1._1
            .map(x => x.toInt)), broadHdi)

    /*kdtree생성*/
    kdtree.generateKdtree(quantDataRDD) //root 생성

    for (i <- 0 until (HDI.split_number - 2)) //(HDI.split_number-2)) { //root노드를 만드는 과정을 위에 빼서 먼저 진행하였으므로 -2로 함. 이렇게 하면 leaf node 수가 총 16개가 된다.
      kdtree.generateKdtree(null)
    broadKd = sc.broadcast(ROOTINFO)

    val rePartition = RealDataRDD.partitionBy(new kdtreePartitioner(HDI.split_number))

    print("INFO::파티션 결과 : ")
    println(rePartition.mapPartitions(iter => Iterator(iter.size), true).collect().toList)

    var result = rePartition.mapPartitionsWithIndex(mapfunc)



    /*
       var RealDataRDD = sc.textFile("file:///" + HDI.file_name).map(
          x => (x.split(" ").take(HDI.featureSize).map(_.toDouble), x.split(" ").takeRight(1))).zipWithIndex().repartition(8)
        var startmaster = System.currentTimeMillis()
        print("INFO::파티션 결과 : ")
        println(RealDataRDD.mapPartitions(iter => Iterator(iter.size), true).collect().toList)
        var result = RealDataRDD.mapPartitionsWithIndex(mapfunc)
    */
    //Slave index 구축
    val startSlave = System.currentTimeMillis()
    var SlaveINFO  = Slave.SlaveMain.SlaveMain(result)
    val end        = System.currentTimeMillis()

    println("total time (index) : " + (end - startmaster) / 1000.0)
    println("Index Complete : " + (end - startSlave) / 1000.0)

    //Quuery 처리

//    var ArrayQuery       = new util.ArrayList[String]()
    var LimitQueryNumber = new Array[Int](2)
    var LimitRange       = new Array[Int](10)


    val ArrayQuery = bufferdSource.getLines().toArray

    //    LimitRange(0) = 28
    //    LimitRange(1) = 28
    //    LimitRange(2) = 28
    //    LimitRange(3) = 28
    //    LimitRange(4) = 28
    //    LimitRange(5) = 28
    //    LimitRange(6) = 28
    //    LimitRange(7) = 28
    //    LimitRange(8) = 28
    //    LimitRange(9) = 28


    LimitRange(0) = 231
    LimitRange(1) = 260
    LimitRange(2) = 86
    LimitRange(3) = 115
    LimitRange(4) = 144
    LimitRange(5) = 173
    LimitRange(6) = 202
    LimitRange(7) = 231
    LimitRange(8) = 260
    LimitRange(9) = 288

    LimitQueryNumber(0) = 10
    LimitQueryNumber(1) = 100


    // args(2)

    val ran = scala.util.Random
    val k_list = Seq(10, 20, 30, 50, 100)


    def test1(path:String = "test1.out" ): Unit = {
      val pw = new PrintWriter(new File(path))
      // KNN Query - 정보과학회 knn 구현 기존 IDistance 사용. dpo변경 전
        ArrayQuery.foreach{ q =>
          k_list.foreach { k =>
//            var query = ArrayQuery.get(j).substring(0, ArrayQuery.get(j).indexOf("  "))
            //var querydata = query.split(" ").map(x=>x.toInt)
            //val writer = new FileWriter("/root/ETRI/data/Result_RandomQuery_RandomK.txt",true)
            val query = q
            val querydata = q.split(" ").map(x => x.toInt)
            val knnQueryClass = new kdTreeKnnQuery()

            //        val pythonPath = "/home/lhb/anaconda3/envs/deepview/bin/python"
            //        val pythonCodePath = "/home/lhb/dv_estimator/range_estimator.py"
            //        val params = querydata.mkString(" ")+" "+k
            //        val result = (pythonPath+" "+pythonCodePath+" "+params).!!
            //        val DouResult = result.toDouble

            val (kResult, queryRange, qPartition) = knnQueryClass.knnQuery(k, querydata)
//            println(s"kResult : $kResult")
//            println(s"queryRange: $queryRange")
//            println(s"qPartition: $qPartition")

            // 기계학습 이용해서 예측된 RANGE로 탐색 수행하는 부분
            //(RootNode: InternalNode, partition: Int, ReferencepointArr: Array[(Array[ReferenceINFO],Int)], querydata:String, MapPartition: Map[Int,Int], InitRange: Int)
            //        val queryResult = QueryExec.ANNQuery(SlaveINFO._1,SlaveINFO._2,querydata.mkString(" "), kResult, DouResult,k,HDI.file_name) // 기존기법 range: 28 // 구현기법 : queryRange  - dpo 변경전
            //        print(j+1+"(No)  "+k+"   ")

            // !! 아래 질의
            val queryResult2 = QueryExec.ANNQuery(SlaveINFO._1, SlaveINFO._2, query, kResult, 28, k, HDI.file_name) // 기존기법 range: 28 // 구현기법 : queryRange  - dpo 변경전
            //val queryResult = QueryExec.KNNQuery(SlaveINFO._1,SlaveINFO._2,query,kResult,queryRange,k,HDI.file_name) // 기존기법 range: 28 // 구현기법 : queryRange  - dpo 변경전
            //print(", "+queryResult._3)
            //val q_res = (querydata.mkString(" ")+","+k+","+DouResult+","+queryResult._2+","+queryResult._1.size+"\n")
            //pw.write(q_res)
            //아래는 슬레이브 결과 받고나서.
            // val avgDist = queryResult._1.map(x => x._2).sum / queryResult._1.size
            //  val node = ROOTINFO.partitionDim(qPartition).dpo = (1 / avgDist) * queryResult._1.size

//            res_arr.append(queryResult2)
          }
        }
    }
//    def test2(n:Int, path:String = "test2.out" ): Unit = {
//      val pw = new PrintWriter(new File(path))
//      // KNN Query - 콘텐츠학회 knn 구현에서의 dpo 비교 / dpo변경 vs dpo수렴
//      k_list.foreach{ k =>
//        var count = 0
//        val kResult = Map[Int,Int]()  // 무의미함.. query에 따른 파티션마다 k개를 다르게 하기위해 썻던건데 현재 여기서는 필요없음.
//        kResult += (0->0)
//        var queryRange = 28.0
//        var threshold = 0
//        println("---------------100 Query(K:100)--------------------------------")
//        println("Query\tTotaltime\tcandidate")
//        println(s"k: [$k] range: [$queryRange]")
//        //        val k = (ran.nextInt() % 100).abs + 1 //args(3).toInt
//        var query = ArrayQuery.get(k).substring(0,ArrayQuery.get(k).indexOf("  "))
//        //(RootNode: InternalNode, partition: Int, ReferencepointArr: Array[(Array[ReferenceINFO],Int)], querydata:String, MapPartition: Map[Int,Int], InitRange: Int)
//        val queryResult = QueryExec.KNNQuery(SlaveINFO._1,SlaveINFO._2,query,kResult,queryRange,k,HDI.file_name) // 기존기법 range: 28 // 구현기법 : queryRange  - dpo 변경전
//        //print(", "+queryResult._3)
//        count = queryResult._2
//        if(k==0) threshold = queryResult._3
//        if(count == 1 && threshold > queryResult._3) queryRange *= 0.995
//        else if(count > 1) queryRange = queryRange * (1.0 + count*0.01)
//        pw.write(query+","+k+","+queryResult._4+"\n")
//      }
//      pw.close()
//    }


    val r1 = test1()
//    val r2 = test2(2)
//    val r3 = test3(3)
  }



  //10개의 Query * Range 5개 실행 = 50개의 결과값
  /*    println("Query\tRange\tcandidate\tTotaltime")
  for(i<-0 until LimitRange.length){
    for(j<-0 until LimitQueryNumber(0)) {
      val queryClass = new kdTreeRangeQuery()

      var querydata = ArrayQuery.get(j).split(" ").map(_.toInt)
      val resultPartitionNumbers= queryClass.retrivalKdtree(ROOTINFO.root,LimitRange(i)/10,querydata)
      QueryExec.Query(SlaveINFO._1,(ArrayQuery.get(j),LimitRange(i)),resultPartitionNumbers.toList,SlaveINFO._2,j)
    }
  }*/

  // 50개의 Query * Range:50 실행 = 90개의 결과값
  /*
      println("----------------100 Query--------------------------------")
      println("\t\t\tQuery\tRange\tcandidate\tTotaltime")
      for(rg<-0 until 2){
        for (j <- 0 until 100) {
          val rangeQueryClass = new kdTreeRangeQuery()
          var querydata = ArrayQuery.get(j).split(" ").map(_.toInt)
         val resultPartitionNumbers = rangeQueryClass.retrivalKdtree(ROOTINFO.root, LimitRange(rg), querydata)
        // val resultPartitionNumbers = List(1,2,3,4,5,6,7,8)
          //print(resultPartitionNumbers.toList)
          // kdtree.printArray(resultPartitionNumbers.toArray)
          // B+tree, (쿼리Data , Range), 파티션번호, 레퍼런스정보
          QueryExec.Query(SlaveINFO._1, (ArrayQuery.get(j), LimitRange(rg)), resultPartitionNumbers.toList, SlaveINFO._2, j,HDI.file_name)
        }
      }

  */

}

//result.map(x=>x._1._1.toList).saveAsTextFile("file:////home/etri/code/log/partition_result")

// val querydata = Source.fromFile(HDI.query_file_name).getLines.mkString.split(" ").map(_.toInt)

//queryClass.printArray(resultPartitionNumbers)

//Slave.SlaveMain.SlaveMain(resultValue)

// var a = RealDataRDD.partitionBy(2)
/*    var b= RealDataRDD.repartition(16)
    var testb = b.mapPartitionsWithIndex(mapfunc)
    println("testb PartitionNumber "+ testb.groupBy(_._1._2).count())*/
//var partitionresult = result.mapPartitionsWithIndex(mapfunc)
//println("partition Number : "+partitionresult.getNumPartitions)
//println("Group by"+ partitionresult.groupBy(_._1._2).count())

//println("kREsult"+kResult)
// println("tempRAnge"+queryRange)

//QueryExec.KNNQuery(SlaveINFO._1,)

//   for(pNumber <- 0 until 4){
//     val node = ROOTINFO.partitionDim(pNumber)
//     println("노드"+pNumber+" dpo 변경전 "+node.asInstanceOf[LeafNode].dpo)
//    }

//    for(pNumber <- 0 until 4){
//     val node = ROOTINFO.partitionDim(pNumber)
//     println("노드"+pNumber+" dpo 변경후 "+node.asInstanceOf[LeafNode].dpo)
//   }

