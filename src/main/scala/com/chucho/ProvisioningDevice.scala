package com.chucho

/**
  * Created by g100536 on 31/01/17.
  */
object ProvisioningDevice {

  def main(args: Array[String]): Unit = {

    val method = "POST"
    val headers = List(
      ("Content-Type","application/json"),
      ("Accept","application/json"),
      ("Fiware-Service","ute"),
      ("Fiware-ServicePath","/montevideo"),
      ("Cache-Control","no-cache")
    )

    val devices = (for(x <- 1 to 20)
      yield DeviceJson(
        device_id = s"thermotank$x",
        entity_name =  s"house$x",
        entity_type = "thermotank",
        attributes = List(Attr("temperature","celsius"))
      )).toList

    val body = makeBodyDevices(devices)
    val response = doRequest("http://172.17.0.2:4041/iot/devices", headers,body,method)
    println(response)

  }


  def doRequest(url: String, headers: List[(String, String)],
                body: String = "",
                requestMethod: String = "GET",
                connectTimeout: Int = 5000,
                readTimeout: Int = 5000) = {
    import java.net.{URL, HttpURLConnection}
    val connection = new URL(url).openConnection.asInstanceOf[HttpURLConnection]
    connection.setConnectTimeout(connectTimeout)
    connection.setReadTimeout(readTimeout)
    connection.setRequestMethod(requestMethod)
    connection.setDoOutput(true)
    headers.foreach(header => {
      val (h, c) = header
      connection.setRequestProperty(h, c)
    })
    val outputStream = connection.getOutputStream
    outputStream.write(body.getBytes)
    val inputStream = connection.getInputStream
    val content = io.Source.fromInputStream(inputStream).mkString
    if (inputStream != null) inputStream.close
    content
  }

  def makeBodyDevices(devices:List[DeviceJson]):String =
    s"""{"devices":[${devices.mkString(",")}]}""".stripMargin

}

case class DeviceJson(device_id:String,entity_name:String,entity_type:String, attributes:List[Attr]){
  override def toString: String =
    s"""{
        "device_id":"$device_id",
        "entity_name":"$entity_name",
        "entity_type":"$entity_type",
        "attributes":[${attributes.mkString(",")}]
      }
    """.stripMargin
}
case class Attr(name:String,_type:String){
  override def toString: String =
    s"""{
      |"name": "$name",
      |"type": "${_type}"
    }""".stripMargin

}
