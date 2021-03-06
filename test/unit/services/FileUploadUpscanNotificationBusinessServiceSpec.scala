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

package unit.services

import java.net.URL
import java.time.Instant
import java.util.UUID

import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.customs.declaration.connectors.FileTransmissionConnector
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.HasConversationId
import uk.gov.hmrc.customs.declaration.repo.FileUploadMetadataRepo
import uk.gov.hmrc.customs.declaration.services.{DeclarationsConfigService, FileUploadUpscanNotificationBusinessService}
import uk.gov.hmrc.play.test.UnitSpec
import util.ApiSubscriptionFieldsTestData.subscriptionFieldsId
import util.TestData._

import scala.concurrent.Future

class FileUploadUpscanNotificationBusinessServiceSpec extends UnitSpec with MockitoSugar {

  private val downloadUrl = new URL("http://remotehost/bucket/123")
  private val initiateDate = Instant.parse("2018-04-24T09:30:00Z")
  private val uploadDetails = UploadedDetails("test.pdf", "application/pdf", initiateDate, "1a2b3c4d5e")
  private val readyCallbackBody = UploadedReadyCallbackBody(FileReferenceOne, downloadUrl, ReadyFileStatus, uploadDetails)
  private val callbackFields = CallbackFields(uploadDetails.fileName, uploadDetails.fileMimeType, uploadDetails.checksum)

  private val md = FileMetadataWithFilesOneAndThree
  private val mdFileOne = FileMetadataWithFilesOneAndThree.files.head
  private val mdFileOneCallback = mdFileOne.maybeCallbackFields.get
  private val fileTransmissionBatchOne = FileTransmissionBatch(md.batchId, md.fileCount)
  private val fileTransmissionCallbackUrl = "http://file_transmission_callback_url/clientSubscriptionId/"
  private val fileTransmissionServiceURL = "http://file_transmission_service_url"
  private val fileTransmissionLocation = mdFileOne.location
  private val fileTransmissionFileOne = FileTransmissionFile(mdFileOne.reference, mdFileOneCallback.name, mdFileOneCallback.mimeType, mdFileOneCallback.checksum, location = fileTransmissionLocation, mdFileOne.sequenceNumber)
  private val fileTransmissionInterfaceOne = FileTransmissionInterface("DEC64", "1.0.0")
  private val fileTransmissionProperties = Seq(
    FileTransmissionProperty("DeclarationId", md.declarationId.toString),
    FileTransmissionProperty("Eori", md.eori.toString),
    FileTransmissionProperty("DocumentType", mdFileOne.documentType.get.toString)
  )
  private val fileTransmissionRequest = FileTransmission(fileTransmissionBatchOne, new URL(fileTransmissionCallbackUrl + clientSubscriptionIdString), fileTransmissionFileOne, fileTransmissionInterfaceOne, fileTransmissionProperties)
  private implicit val implicitHasConversationId = new HasConversationId {
    override val conversationId: ConversationId = ConversationId(FileReferenceOne.value)
  }
  private val fileGroupSizeMaximum = 5
  private val fileUploadConfig = FileUploadConfig("UPSCAN_INITIATE_URL", "UPSCAN_URL_IGNORED", "UPSCAN_URL_IGNORED", fileGroupSizeMaximum, fileTransmissionCallbackUrl, fileTransmissionServiceURL)

  trait SetUp {
    val mockRepo = mock[FileUploadMetadataRepo]
    val mockConnector = mock[FileTransmissionConnector]
    val mockConfig = mock[DeclarationsConfigService]
    val mockLogger = mock[DeclarationsLogger]
    val service = new FileUploadUpscanNotificationBusinessService(mockRepo, mockConnector, mockConfig, mockLogger)

    when(mockConfig.fileUploadConfig).thenReturn(fileUploadConfig)
  }

  "FileUploadUpscanNotificationBusinessService" should {
    "update metadata and call file transmission service" in new SetUp {
      when(mockRepo.update(subscriptionFieldsId, FileReferenceOne, callbackFields)).thenReturn(Future.successful(Some(FileMetadataWithFilesOneAndThree)))
      when(mockConnector.send(any[FileTransmission])).thenReturn(Future.successful(()))

      val actual = await(service.persistAndCallFileTransmission(subscriptionFieldsId, readyCallbackBody))

      actual shouldBe (())
      verify(mockRepo).update(
        ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId],
        ameq[UUID](FileReferenceOne.value).asInstanceOf[FileReference],
        ameq(callbackFields))(any[HasConversationId]
      )
      verify(mockConnector).send(ameq(fileTransmissionRequest))
    }

    "return failed future when no metadata record found for file reference" in new SetUp {
      when(mockRepo.update(subscriptionFieldsId, FileReferenceOne, callbackFields)).thenReturn(Future.successful(None))

      val error = intercept[IllegalStateException](await(service.persistAndCallFileTransmission(subscriptionFieldsId, readyCallbackBody)))

      error.getMessage shouldBe s"database error - can't find record with file reference ${FileReferenceOne.value.toString}"
      verify(mockRepo).update(
        ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId],
        ameq[UUID](FileReferenceOne.value).asInstanceOf[FileReference],
        ameq(callbackFields))(any[HasConversationId]
      )
      verifyZeroInteractions(mockConnector)
    }

    "return failed future when file reference not found in returned metadata" in new SetUp {
      when(mockRepo.update(subscriptionFieldsId, FileReferenceOne, callbackFields)).thenReturn(Future.successful(Some(FileMetadataWithFileTwo)))

      val error = intercept[IllegalStateException](await(service.persistAndCallFileTransmission(subscriptionFieldsId, readyCallbackBody)))

      error.getMessage shouldBe s"database error - can't find file with file reference ${FileReferenceOne.value.toString}"
      verify(mockRepo).update(
        ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId],
        ameq[UUID](FileReferenceOne.value).asInstanceOf[FileReference],
        ameq(callbackFields))(any[HasConversationId]
      )
      verifyZeroInteractions(mockConnector)
    }

    "propagate exception encountered in repo" in new SetUp {
      when(mockRepo.update(subscriptionFieldsId, FileReferenceOne, callbackFields)).thenReturn(Future.failed(emulatedServiceFailure))

      val error = intercept[EmulatedServiceFailure](await(service.persistAndCallFileTransmission(subscriptionFieldsId, readyCallbackBody)))

      error shouldBe emulatedServiceFailure
      verify(mockRepo).update(
        ameq[UUID](subscriptionFieldsId.value).asInstanceOf[SubscriptionFieldsId],
        ameq[UUID](FileReferenceOne.value).asInstanceOf[FileReference],
        ameq(callbackFields))(any[HasConversationId]
      )
      verifyZeroInteractions(mockConnector)
    }

    "propagate exception encountered in connector" in new SetUp {
      when(mockRepo.update(subscriptionFieldsId, FileReferenceOne, callbackFields)).thenReturn(Future.successful(Some(FileMetadataWithFilesOneAndThree)))
      when(mockConnector.send(any[FileTransmission])).thenReturn(Future.failed(emulatedServiceFailure))

      val error = intercept[EmulatedServiceFailure](await(service.persistAndCallFileTransmission(subscriptionFieldsId, readyCallbackBody)))

      error shouldBe emulatedServiceFailure
    }
  }
}
