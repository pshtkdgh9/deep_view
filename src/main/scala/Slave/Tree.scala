package Slave
import java.util
// ( keydata - (  파티션번호,value ) , Minidx - reference번호 , MIN - 거리 )
class Tree(array : Array[((Int,Double),Int,Double, Int)]) extends Serializable {
  //println("Tree start")
 // var start = System.currentTimeMillis()
  val PartitionNumber = array(0)._1._1
  var map = Map(-1-> 0.0)  // idistance를 통해 나온 값 과 porinter값을 저장하는 공간

  for(i<-0 until array.length) {
    map += (  array(i)._4 -> array(i)._1._2)
  }

  map-=(-1)

  println("Partition Number :"+PartitionNumber+"  B+tree stored data number"+map.size)

  var Size = new SizeCal2()
  var map2 = map.toSeq.sortBy(_._2)

  var leafNode = LeafNode(map2(0)._1,map2(0)._2, Size.DataSize)

  var leafnodestore = new util.ArrayList[LeafNode]  // LeafNode들을 저장하여 다음 높이 생성시 사용
  var internalnodestore = new util.ArrayList[InternalNode] // RootNode를 생성하기 위해 InternalNode들을 저장하고 사용
  leafnodestore.add(leafNode)

  new Leaf_Create(leafNode, map2,leafnodestore) // LeafNode 생성
  new Internal_Create(leafnodestore,internalnodestore) // LeafNode와 연결되 Internal Node 생성


  var height = new Height_Cal(internalnodestore.size()) // 높이 계산

  new Root_Create(internalnodestore, height.count) // 남은 InternalNode 생성 ( RootNode 생성)

  var RootNode = internalnodestore.get(0) // RootNode 지정

  leafnodestore.clear()
  internalnodestore.clear()


 //var Btreeend = System.currentTimeMillis()
  //println("btree 값생성 걸린 시간 :"+(Btreeend-start)/1000.0)

/*
  println(PartitionNumber+" 번쨰 Partition의 RootNode : "+RootNode)

  if(PartitionNumber == 0){
    var printleaf:LeafNode=null
    var printNode = RootNode
    var isleaf = false
    while(isleaf==false){
      if(printNode.entry.pointers(0).isInstanceOf[InternalNode]) printNode = printNode.entry.pointers(0).asInstanceOf[InternalNode]
      else{
        isleaf=true
        printleaf = printNode.entry.pointers(0).asInstanceOf[LeafNode]
      }
    }
    println("첫 번째 LeafNode의 저장되어있는 Data수: " + printleaf.header.numOfdata + ", NextNode: " + printleaf.header.NextNode)
    for(j<-0 until printleaf.header.numOfdata){
      println("PartitionNumber: "+PartitionNumber+", value:" + printleaf.entry.data(j) + ", pointer:" + printleaf.entry.pointers(j))
    }
  }*/



/*
     var printleaf = leafNode
    var calculate = new LeafNode_Cal(printleaf.Size, map.size)

    println(RootNode)
    for(i<-1 to calculate.leafNodeNumber) {
      println(i + "번째 LeafNode의 저장되어있는 Data수: " + printleaf.header.numOfdata + ", NextNode: " + printleaf.header.NextNode)
      for (j <- 0 until printleaf.Size)
        println("value:" + printleaf.entry.data(j) + ", pointer:" + printleaf.entry.pointers(j))
      println("")
      printleaf = printleaf.header.NextNode
    }*/





}
//높이계산
class Height_Cal(i: Int){
  var Size = new SizeCal2
  var count = 0
  var numberofnode = i
  while(numberofnode!=0){
    numberofnode /= Size.DataSize
    count+=1
  }
}
class Leaf_Create(leafNode : LeafNode, map : Seq[(Any,Any)],nodestore : util.ArrayList[LeafNode]){
  var tempNode = leafNode
  var calculate = new LeafNode_Cal(leafNode.Size, map.size)
  var index = 0

  for(i<-1 to calculate.div){
    for(j<-1 until (calculate.mok+1)) {
      index = index + 1
      NodeAdd(tempNode, map(index)_1, map(index)._2)
    }

    index = index+1
    tempNode.header.NextNode = LeafNode(map(index)._1,map(index)._2, leafNode.Size)

    tempNode = tempNode.header.NextNode
    nodestore.add(tempNode)
  }
  for(i<- 1 to calculate.leafNodeNumber - calculate.div){
    for(j<-1 until calculate.mok) {
      index = index + 1
      NodeAdd(tempNode, map(index)_1, map(index)._2)
    }

    index = index+1
    if(index < map.size){
      tempNode.header.NextNode = LeafNode(map(index)._1,map(index)._2, leafNode.Size)
      tempNode = tempNode.header.NextNode
      nodestore.add(tempNode)
    }



  }
}

class Internal_Create(nodestore : util.ArrayList[LeafNode],  internalstore :util.ArrayList[InternalNode]){
  var Size = new SizeCal2
  var calculate = new InternalNode_Cal(Size.DataSize, nodestore.size())
  var index = 0

  for(i<-1 to calculate.div){
    var data = nodestore.get(index).entry.data(nodestore.get(index).header.numOfdata -1)
    var tempNode = new InternalNode(nodestore.get(index),data,Size.DataSize)
    index = index + 1
    for(j<-1 until (calculate.mok+1)){
      data = nodestore.get(index).entry.data(nodestore.get(index).header.numOfdata -1)
      NodeAdd2(tempNode,nodestore.get(index),data)
      index  = index+1
    }
    tempNode.entry.pointers(tempNode.header.numOfdata) = nodestore.get(index)
    internalstore.add(tempNode)

  }

  for(i<-1 to calculate.InternalNodeNumber - calculate.div){
    var data = nodestore.get(index).entry.data(nodestore.get(index).header.numOfdata -1)
    var tempNode = new InternalNode(nodestore.get(index),data,Size.DataSize)
    index = index + 1
    for(j<-1 until (calculate.mok)){
      data = nodestore.get(index).entry.data(nodestore.get(index).header.numOfdata -1)
      NodeAdd2(tempNode,nodestore.get(index),data)
      index  = index+1
    }
    if(index < nodestore.size()){
      tempNode.entry.pointers(tempNode.header.numOfdata) = nodestore.get(index)
    }
    internalstore.add(tempNode)
  }
}

class Root_Create(nodestore : util.ArrayList[InternalNode], height : Int){
  var Size = new SizeCal2

  var newnodestore = new util.ArrayList[InternalNode]()
  var index = 0

  for(i<-1 to height) {
    var calculate = new InternalNode_Cal(Size.DataSize, nodestore.size())
    for (j <- 1 to calculate.div) {
      var data = nodestore.get(index).entry.data(nodestore.get(index).header.numOfdata - 1)

      var tempNode = new InternalNode(nodestore.get(index),data, Size.DataSize)
      Noderemove(tempNode.entry.pointers(0).asInstanceOf[InternalNode])
      index += 1

      for (k <- 1 until (calculate.mok + 1)) {
        data = nodestore.get(index).entry.data(nodestore.get(index).header.numOfdata - 1)
        NodeAdd2(tempNode, nodestore.get(index), data)
        Noderemove(tempNode.entry.pointers(k).asInstanceOf[InternalNode])
        index = index + 1
      }
      tempNode.entry.pointers(tempNode.header.numOfdata) = nodestore.get(index)
      newnodestore.add(tempNode)
    }
    for (j <- 1 to calculate.InternalNodeNumber - calculate.div) {
      var data = nodestore.get(index).entry.data(nodestore.get(index).header.numOfdata - 1)
      var tempNode = new InternalNode(nodestore.get(index),data,  Size.DataSize)
      Noderemove(tempNode.entry.pointers(0).asInstanceOf[InternalNode])
      index += 1

      for (k <- 1 until (calculate.mok)) {
        if(calculate.InternalNodeNumber != 1){
          data = nodestore.get(index).entry.data(nodestore.get(index).header.numOfdata - 1)
          NodeAdd2(tempNode, nodestore.get(index), data)
          Noderemove(tempNode.entry.pointers(k).asInstanceOf[InternalNode])
          index = index + 1
        }
      }

      if (index < nodestore.size()) {
        tempNode.entry.pointers(tempNode.header.numOfdata) = nodestore.get(index)
      }
      newnodestore.add(tempNode)
    }
    nodestore.clear()
    nodestore.addAll(newnodestore)
    newnodestore.clear()
    index = 0
  }
}


class LeafNode_Cal(dataSize: Int, datanumber : Int){

  var leafNodeNumber = Math.ceil(datanumber.toDouble/dataSize.toDouble).toInt // 현재 Data를 삽입 시킬 공간을 확보하기 위한 leafNode 수
  var div = datanumber%leafNodeNumber // 각 노드에 Data 값을 분배하기 위해 필요한 값
  var mok = datanumber/leafNodeNumber
}

class InternalNode_Cal(dataSize: Int, datanumber : Int){
  var InternalNodeNumber = 0

  if(datanumber -(dataSize+1) < 0) InternalNodeNumber = 1
  else InternalNodeNumber= ((datanumber-(dataSize+1)) / dataSize ) + 1 + 1

  var div = datanumber % InternalNodeNumber
  var mok = datanumber / InternalNodeNumber
}
// Size 계산
class SizeCal2()extends Serializable{
  var DataSize = 30
}