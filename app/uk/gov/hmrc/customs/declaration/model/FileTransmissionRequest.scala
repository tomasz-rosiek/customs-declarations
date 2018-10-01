package uk.gov.hmrc.customs.declaration.model

import play.api.libs.json.{Format, Json}

case class Property(name: String, value: String)

object Property {
  implicit val fmt: Format[Property] = Json.format[Property]
}

case class Interface(name: String, version: String)

object Interface {
  implicit val fmt: Format[Interface] = Json.format[Interface]
}

case class File(reference: String, name: String, mimeType: String, checksum: String, location: String, sequenceNumber: SequenceNumber, size: Int)

object File {
  implicit val fmt: Format[File] = Json.format[File]
}

case class Batch(id: String, fileCount: Int)

object Batch {
  implicit val fmt: Format[Batch] = Json.format[Batch]
}

case class FileTransmissionRequest(batch: Batch, callbackUrl: String, deliveryWindowDurationInSeconds: Int, file: File, interface: Interface, properties: Seq[Property]) {

}

object FileTransmissionRequest {
  implicit val fmt: Format[FileTransmissionRequest] = Json.format[FileTransmissionRequest]
}
