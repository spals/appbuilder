package net.spals.appbuilder.filestore.s3

import java.io.{BufferedInputStream, IOException, InputStream}
import java.net.URI
import java.util.Optional
import javax.annotation.PostConstruct
import javax.validation.constraints.NotNull

import com.amazonaws.DefaultRequest
import com.amazonaws.internal.IdentityEndpointBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.internal.{Constants, S3RequestEndpointResolver, ServiceUtils}
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.transfer.TransferManager
import com.google.common.annotations.VisibleForTesting
import com.google.inject.Inject
import com.netflix.governator.annotations.Configuration
import net.spals.appbuilder.annotations.config.ApplicationName
import net.spals.appbuilder.annotations.service.AutoBindInMap
import net.spals.appbuilder.filestore.core.FileStorePlugin
import net.spals.appbuilder.filestore.core.model._

import scala.compat.java8.OptionConverters._
import scala.util.{Failure, Success, Try}

/**
  * @author tkral
  */
@AutoBindInMap(baseClass = classOf[FileStorePlugin], key = "s3")
private[s3] class S3FileStorePlugin @Inject() (
  @ApplicationName applicationName: String,
  s3Client: AmazonS3,
  s3EncryptionHolder: S3EncryptionHolder,
  s3TransferManager: TransferManager,
  s3TransferEncryptionHolder: S3TransferEncryptionHolder
) extends FileStorePlugin {

  @NotNull
  @Configuration("fileStore.s3.endpoint")
  private[s3] var endpoint: String = null

  @NotNull
  @Configuration("fileStore.s3.bucket")
  private[s3] var s3Bucket: String = s"${applicationName.toLowerCase}-filestore"

  private[s3] lazy val s3Region: Option[Region] = Try(s3Client.getRegion).toOption

  @PostConstruct private[s3] def createBucket(): Unit = {
    if (!s3Client.doesBucketExist(s3Bucket)) {
      s3Client.createBucket(s3Bucket)
    }
  }

  override def deleteFile(key: FileStoreKey): Boolean = {
    val s3ObjectId = resolveS3ObjectId(key)
    val request = new DeleteObjectRequest(s3ObjectId.getBucket, s3ObjectId.getKey)

    Try(s3Client.deleteObject(request)).isSuccess
  }

  @throws[IOException]
  override def getFileContent(key: FileStoreKey): Optional[InputStream] = {
    getFileMetadata(key).asScala.map(fileMetadata => {
      val s3ObjectId = resolveS3ObjectId(key)
      val request = new GetObjectRequest(s3ObjectId);

      val amazonS3: AmazonS3 = fileMetadata.getSecurityLevel match {
        case FileSecurityLevel.PUBLIC => s3Client
        case FileSecurityLevel.PRIVATE => s3EncryptionHolder.value.asScala
          .getOrElse(throw new UnsupportedOperationException("S3 plugin is not configured for encryption. " +
            "So private file content cannot be read. Please set the s3.fileStore.encryptionKey configuration."))
      }

      Try(amazonS3.getObject(request)) match {
        case Success(s3Object) => s3Object.getObjectContent.asInstanceOf[InputStream]
        case Failure(t) => throw new IOException().initCause(t).asInstanceOf[IOException]
      }
    }).asJava
  }

  @throws[IOException]
  override def getFileMetadata(key: FileStoreKey): Optional[FileMetadata] = {
    val s3ObjectId = resolveS3ObjectId(key)
    val request = new GetObjectMetadataRequest(s3ObjectId.getBucket, s3ObjectId.getKey)

    Try(s3Client.getObjectMetadata(request)) match {
      case Success(s3Metadata) => Option(s3Metadata).map(s3Meta => {
        val securityLevel = Option(s3Meta.getSSEAwsKmsKeyId).map(_ => FileSecurityLevel.PRIVATE)
          .getOrElse(FileSecurityLevel.PUBLIC)

        new FileMetadata.Builder()
          .setSecurityLevel(securityLevel)
          .setStoreLocation(FileStoreLocation.REMOTE)
          .setURI(resolveBrowserURI(s3ObjectId))
          .build()
      }).asJava
      case Failure(s3e: AmazonS3Exception) => s3e.getStatusCode match {
        case 404 => Optional.empty[FileMetadata]
        case _ => throw new IOException().initCause(s3e).asInstanceOf[IOException]
      }
      case Failure(t) => throw new IOException().initCause(t).asInstanceOf[IOException]

    }
  }

  @throws[IOException]
  override def putFile(key: FileStoreKey, request: PutFileRequest): FileMetadata = {
    val fileStreamForUpload: InputStream = request.getContentLength.asScala match {
      case Some(contentLength) => request.getFileStream
      // If we don't know the content-length up front, wrap the file stream in a buffered input stream
      // so that the transfer manager can chunk it.
      case None => new BufferedInputStream(request.getFileStream, 1) // TODO
        //new BufferedInputStream(filePutRequest.getFileStream(), (int)uploadChunkSizeBytes);
    }

    // We will always encrypt privately scoped files
    val localTransferManager: TransferManager = request.getFileSecurityLevel match {
      case FileSecurityLevel.PUBLIC => s3TransferManager
      case FileSecurityLevel.PRIVATE => s3TransferEncryptionHolder.value.asScala
        .getOrElse(throw new UnsupportedOperationException("S3 plugin is not configured for encryption. " +
          "So private files cannot be written. Please set the s3.fileStore.encryptionKey configuration."))
    }

    val objectMetadata = new ObjectMetadata()
    request.getContentLength.asScala.foreach(objectMetadata.setContentLength(_))
    request.getContentType.asScala.foreach(objectMetadata.setContentType(_))

    val accessControlList = request.getFileSecurityLevel match {
      case FileSecurityLevel.PUBLIC => CannedAccessControlList.PublicRead
      case FileSecurityLevel.PRIVATE => CannedAccessControlList.Private
    }

    val s3ObjectId = resolveS3ObjectId(key)
    val putObjectRequest = new PutObjectRequest(s3ObjectId.getBucket, s3ObjectId.getKey,
      fileStreamForUpload, objectMetadata)
    putObjectRequest.withCannedAcl(accessControlList)

    try {
      Try(localTransferManager.upload(putObjectRequest).waitForUploadResult()) match {
        case Success(uploadResult) => new FileMetadata.Builder()
          .setSecurityLevel(request.getFileSecurityLevel)
          .setStoreLocation(FileStoreLocation.REMOTE)
          .setURI(resolveBrowserURI(s3ObjectId))
          .build()
        case Failure(t) => throw new IOException().initCause(t).asInstanceOf[IOException]
      }
    } finally {
      fileStreamForUpload.close()
    }
  }

  @VisibleForTesting
  private[s3] def resolveBrowserURI(s3ObjectId: S3ObjectId): URI = {
    // The default URL returned from the S3Client (S3Client#getUrl) uses the REST API Endpoint.
    // But since this is likely to be used in a browser, what we really want is
    // the Website Endpoint. So here, we'll recreate the URL building logic except
    // that we'll force the Website Endpoint to be used (isPathStyleAccess)
    //
    // See http://docs.aws.amazon.com/AmazonS3/latest/dev/WebsiteEndpoints.html#WebsiteRestEndpointDiff
    //
    val s3Endpoint = s3Region.map(region => s"https://${region.toAWSRegion.getServiceEndpoint(AmazonS3.ENDPOINT_PREFIX)}")
      .getOrElse(endpoint)

    val request = new DefaultRequest(Constants.S3_SERVICE_DISPLAY_NAME)
    val serviceEndpointBuilder = new IdentityEndpointBuilder(new URI(s3Endpoint))
    val requestEndpointResolver = new S3RequestEndpointResolver(serviceEndpointBuilder, true /*isPathStyleAccess*/,
      s3ObjectId.getBucket, s3ObjectId.getKey)
    requestEndpointResolver.resolveRequestEndpoint(request)

    ServiceUtils.convertRequestToUrl(request, false, true).toURI
  }

  @VisibleForTesting
  private[s3] def resolveS3ObjectId(key: FileStoreKey) = new S3ObjectId(s3Bucket, key.toGlobalId("/"))
}
