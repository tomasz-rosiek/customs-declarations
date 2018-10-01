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

package unit.model

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.play.test.UnitSpec

class FileTransmissionRequestSpec extends UnitSpec {
  private val jsonString = """{
                             |	"batch": {
                             |		"id": "fghij67890",
                             |		"fileCount": 10
                             |	},
                             |	"callbackUrl": "https://file-transmission-callback-listener.public.mdtp/file-transmission-callback-listener/listen",
                             |	"deliveryWindowDurationInSeconds": 300,
                             |	"file": {
                             |		"reference": "abcde12345",
                             |		"name": "someFileN.ame",
                             |		"mimeType": "application/pdf",
                             |		"checksum": "asdrfgvbhujk13579",
                             |		"location": "https://file-outbound-asderfvghyujk1357690.aws.amazon.com",
                             |		"sequenceNumber": 3,
                             |		"size": 1024
                             |	},
                             |	"interface":{
                             |		"name": "interfaceName name",
                             |		"version": "1.0"
                             |	},
                             |	"properties":[
                             |		{
                             |			"name": "property1",
                             |			"value": "value1"
                             |		},
                             |		{
                             |			"name": "property2",
                             |			"value": "value2"
                             |		}
                             |	]
                             |}
                             |""".stripMargin

  private val json = Json.parse(jsonString)

  private val fileCount = 10

  private val deliveryWindowDurationInSeconds = 300

  private val sequenceNumberValue = 3

  private val fileSize = 1024

  private val fileTransmissionRequest = FileTransmissionRequest(Batch("fghij67890",fileCount),
    "https://file-transmission-callback-listener.public.mdtp/file-transmission-callback-listener/listen", deliveryWindowDurationInSeconds,
    File("abcde12345", "someFileN.ame", "application/pdf", "asdrfgvbhujk13579", "https://file-outbound-asderfvghyujk1357690.aws.amazon.com",SequenceNumber(sequenceNumberValue), fileSize), Interface("interfaceName name", "1.0"), Seq(Property("property1", "value1"), Property("property2", "value2")))

  "FileTransmissionRequest model" should {
    "serialise to Json" in {

      val actualJson = Json.toJson(fileTransmissionRequest)

      actualJson shouldBe json
    }

    "de-serialise from Json" in {

      val JsSuccess(actualFileTransmissionRequest, _) = Json.parse(jsonString).validate[FileTransmissionRequest]

      actualFileTransmissionRequest shouldBe fileTransmissionRequest
    }
  }

}
