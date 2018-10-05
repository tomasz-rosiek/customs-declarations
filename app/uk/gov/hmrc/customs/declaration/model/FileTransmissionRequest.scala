/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.customs.declaration.model

import java.net.URL

import play.api.libs.json.{Format, Json}

case class Property(name: String, value: String)

object Property {
  implicit val fmt: Format[Property] = Json.format[Property]
}

case class Interface(name: String, version: String)

object Interface {
  implicit val fmt: Format[Interface] = Json.format[Interface]
}

case class File(reference: String, name: String, mimeType: String, checksum: String, location: URL, sequenceNumber: SequenceNumber, size: Int)

object File {
  implicit val urlFormat: HttpUrlFormat.type = HttpUrlFormat
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

  val defaultCallbackUrl = "http://callback.url.com" //TODO MC hardcoded values
  val defaultDeliveryWindowDurationInSeconds = 300
  val defaultInterface = Interface("hardcodedInterface001", "0.7")

  def fromBatchFile(bf: BatchFile, bid: BatchId, fileCount: Int): FileTransmissionRequest = {
    val cf: CallbackFields = bf.maybeCallbackFields.getOrElse(throw new RuntimeException("bad things happen when the object is not complete"))
    val file = File(bf.reference.value.toString, cf.name, cf.mimeType, cf.checksum, bf.location, bf.sequenceNumber, bf.size)
    FileTransmissionRequest(Batch(bid.value.toString, fileCount), defaultCallbackUrl, defaultDeliveryWindowDurationInSeconds, file, defaultInterface, Seq.empty[Property])


  }

}
