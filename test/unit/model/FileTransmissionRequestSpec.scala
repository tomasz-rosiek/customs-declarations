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

///*
// * Copyright 2018 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package unit.model
//
//import java.net.URL
//import java.util.UUID
//
//import play.api.libs.json.{JsSuccess, Json}
//import uk.gov.hmrc.customs.declaration.model.FileTransmissionRequest.{defaultCallbackUrl, defaultDeliveryWindowDurationInSeconds, defaultInterface}
//import uk.gov.hmrc.customs.declaration.model._
//import uk.gov.hmrc.play.test.UnitSpec
//import util.ApiSubscriptionFieldsTestData
//
//class FileTransmissionRequestSpec extends UnitSpec {
//
//  private val batchIdUuid = UUID.randomUUID()
//
//  private val fileReferenceUuid = UUID.randomUUID()
//
//  private val fileName = "someFileN.ame"
//  private val mimeType = "application/pdf"
//
//  private val fileChecksum = "asdrfgvbhujk13579"
//
//  private val fileLocation = "https://file-outbound-zxcvbnmkjhgfdertyuijhgt.aws.amazon.com"
//
//  private val jsonString = s"""{
//                              |	"batch": {
//                              |		"id": "${batchIdUuid.toString}",
//                              |		"fileCount": 10
//                              |	},
//                              |	"callbackUrl": "$defaultCallbackUrl",
//                              |	"deliveryWindowDurationInSeconds": 300,
//                              |	"file": {
//                              |		"reference": "${fileReferenceUuid.toString}",
//                              |		"name": "$fileName",
//                              |		"mimeType": "$mimeType",
//                              |		"checksum": "$fileChecksum",
//                              |		"location": "$fileLocation",
//                              |		"sequenceNumber": 3,
//                              |		"size": 1024
//                              |	},
//                              |	"interface":{
//                              |		"name": "${defaultInterface.name}",
//                              |		"version": "${defaultInterface.version}"
//                              |	},
//                              |	"properties":[
//                              |		{
//                              |			"name": "property1",
//                              |			"value": "value1"
//                              |		},
//                              |		{
//                              |			"name": "property2",
//                              |			"value": "value2"
//                              |		}
//                              |	]
//                              |}
//                              |""".stripMargin
//
//  private val json = Json.parse(jsonString)
//
//  private val fileCount = 10
//
//  private val sequenceNumberValue = 3
//
//  private val fileSize = 1024
//
//  private val batch = Batch(batchIdUuid.toString, fileCount)
//
//
//
//  private val fileTransmissionRequest = FileTransmissionRequest(batch,
//    defaultCallbackUrl, defaultDeliveryWindowDurationInSeconds,
//    File(fileReferenceUuid.toString, fileName, mimeType, fileChecksum, new URL(fileLocation),FileSequenceNo(sequenceNumberValue), fileSize), defaultInterface, Seq(Property("property1", "value1"), Property("property2", "value2")))
//
//  private val callbackFields = CallbackFields(fileName, mimeType, fileChecksum)
//
//  private val batchFile = BatchFile(FileReference(fileReferenceUuid), Option(callbackFields), new URL(fileLocation), FileSequenceNo(sequenceNumberValue), fileSize, DocumentType("some document type"))
//
//  private val batchFileUploadMetadata = BatchFileUploadMetadata(DeclarationId("someId"), Eori("someEori"), ApiSubscriptionFieldsTestData.subscriptionFieldsId, BatchId(batchIdUuid), fileCount, Seq(batchFile))
//
//  "FileTransmissionRequest model" should {
//    "serialise to Json" in {
//
//      val actualJson = Json.toJson(fileTransmissionRequest)
//
//      actualJson shouldBe json
//    }
//
//    "de-serialise from Json" in {
//
//      val JsSuccess(actualFileTransmissionRequest, _) = Json.parse(jsonString).validate[FileTransmissionRequest]
//
//      actualFileTransmissionRequest shouldBe fileTransmissionRequest
//    }
//
//    "be created from BatchFile instance" in {
//      val batchId = batchFileUploadMetadata.batchId
//      val fileCount = batchFileUploadMetadata.fileCount
//      val ftr = FileTransmissionRequest.fromBatchFile(batchFile, batchId, fileCount)
//
//      val expectedFTR = fileTransmissionRequest.copy(properties = Seq.empty)
//
//      ftr shouldBe expectedFTR
//    }
//
//    "throw an exception when no callback data is provided" in {
//      val batchId = batchFileUploadMetadata.batchId
//      val fileCount = batchFileUploadMetadata.fileCount
//      val caught = intercept[RuntimeException]{
//        FileTransmissionRequest.fromBatchFile(batchFile.copy(maybeCallbackFields = None), batchId, fileCount)
//      }
//      caught.getMessage shouldBe "bad things happen when the object is not complete"
//    }
//
//  }
//
//}
