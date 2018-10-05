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

package uk.gov.hmrc.customs.declaration.controllers

import java.net.URL
import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.customs.declaration.connectors.GoogleAnalyticsConnector
import uk.gov.hmrc.customs.declaration.controllers.actionbuilders._
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.ActionBuilderModelHelper._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.{HasConversationId, ValidatedUploadPayloadRequest}
import uk.gov.hmrc.customs.declaration.services.FileUploadBusinessService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class FileUploadController @Inject()(
                                      val common: Common,
                                      val fileUploadBusinessService: FileUploadBusinessService,
                                      val fileUploadPayloadValidationComposedAction: FileUploadPayloadValidationComposedAction,
                                      val fileUploadAnalyticsValuesAction: FileUploadAnalyticsValuesAction,
                                      val googleAnalyticsConnector: GoogleAnalyticsConnector
                                    )
  extends BaseController {

  private def xmlOrEmptyBody: BodyParser[AnyContent] = BodyParser(rq => parse.xml(rq).map {
    case Right(xml) =>
      Right(AnyContentAsXml(xml))
    case _ =>
      Right(AnyContentAsEmpty)
  })

  def post(): Action[AnyContent] = (
    Action andThen
      fileUploadAnalyticsValuesAction andThen
      common.validateAndExtractHeadersAction andThen
      common.authAction andThen
      fileUploadPayloadValidationComposedAction
    )
    .async(bodyParser = xmlOrEmptyBody) {

      implicit vupr: ValidatedUploadPayloadRequest[AnyContent] =>
        val logger = common.logger

        logger.debug(s"Request received. Payload = ${vupr.body.toString} headers = ${vupr.headers.headers}")

        fileUploadBusinessService.send map {
          case Right(res) =>
            val referenceConversationId = ConversationId(UUID.fromString(res.reference))
            logger.debug(s"Replacing conversationId with $referenceConversationId")
            val id = new HasConversationId {
              override val conversationId: ConversationId = referenceConversationId
            }
            logger.info(s"Upload initiate request processed successfully.")(id)
            googleAnalyticsConnector.success
            Ok(res.uploadRequest.toXml).withConversationId(id)
          case Left(errorResult) =>
            errorResult
        }
    }

  def dummy(): Action[AnyContent] = Action {

    // TODO MC composed actions ^^^^^^

    val fileSubscriptionFieldsIdUuid = UUID.randomUUID()
    val batchIdUuid = UUID.randomUUID()

    val fileReferenceUuid = UUID.randomUUID()

    val fileCount = 10

    val sequenceNumberValue = 3

    val fileSize = 1024

    val batch = Batch(batchIdUuid.toString, fileCount)

    val fileLocation = "https://file-outbound-zxcvbnmkjhgfdertyuijhgt.aws.amazon.com"

    val fileName = "someFileN.ame"

    val mimeType = "application/pdf"

    val fileChecksum = "asdrfgvbhujk13579"

    val callbackFields = CallbackFields(fileName, mimeType, fileChecksum)

    val batchFile = BatchFile(FileReference(fileReferenceUuid), Option(callbackFields), new URL(fileLocation), SequenceNumber(sequenceNumberValue), fileSize, DocumentType("some document type"))

    val batchFileUploadMetadata = BatchFileUploadMetadata(DeclarationId("someId"), Eori("someEori"), SubscriptionFieldsId(fileSubscriptionFieldsIdUuid), BatchId(batchIdUuid), fileCount, Seq(batchFile))


    Ok("")
  }
}
