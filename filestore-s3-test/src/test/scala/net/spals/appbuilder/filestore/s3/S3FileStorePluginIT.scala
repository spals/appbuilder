package net.spals.appbuilder.filestore.s3

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URI
import java.util.Optional
import java.util.concurrent.Executors

import com.amazonaws.services.s3.AmazonS3Encryption
import com.amazonaws.services.s3.internal.SkipMd5CheckStrategy._
import com.amazonaws.services.s3.transfer.TransferManager
import com.google.common.base.Charsets
import com.google.common.io.Resources
import io.opentracing.mock.MockTracer
import net.spals.appbuilder.executor.core.ExecutorServiceFactory
import net.spals.appbuilder.filestore.core.model._
import org.hamcrest.MatcherAssert._
import org.hamcrest.Matchers.{hasToString, is, not, stringContainsInOrder}
import org.mockito.ArgumentMatchers.{any => m_any, anyInt => m_anyInt}
import org.mockito.Mockito.{mock, when}
import org.slf4j.LoggerFactory
import org.testng.annotations.{BeforeClass, Test}

/**
  * Integration tests for [[S3FileStorePlugin]]
  *
  * @author tkral
  */
class S3FileStorePluginIT {

  private val LOGGER = LoggerFactory.getLogger(classOf[S3FileStorePluginIT])

  private lazy val s3ClientProvider = {
    // Turn off MD5 checks because we're going against a Fake S3
    System.setProperty(DISABLE_GET_OBJECT_MD5_VALIDATION_PROPERTY, "true")
    System.setProperty(DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY, "true")

    val s3ClientProvider = new S3ClientProvider(new MockTracer())
    s3ClientProvider.awsAccessKeyId = "DUMMY"
    s3ClientProvider.awsSecretKey = "DUMMY"
    s3ClientProvider.endpoint = s"http://${System.getenv("S3_IP")}:${System.getenv("S3_PORT")}"

    LOGGER.info(s"Connecting to S3 instance at ${s3ClientProvider.endpoint}")
    s3ClientProvider
  }

  private lazy val s3Client = s3ClientProvider.get

  private lazy val s3TransferManager = {
    val executorServiceFactory = mock(classOf[ExecutorServiceFactory])
    when(executorServiceFactory.createFixedThreadPool(m_anyInt(), m_any()))
      .thenReturn(Executors.newSingleThreadExecutor())

    val s3TransferManagerProvider = new S3TransferManagerProvider(s3Client, executorServiceFactory)
    s3TransferManagerProvider.get()
  }

  private lazy val fileStorePlugin = {
    val s3FileStorePlugin = new S3FileStorePlugin(applicationName = "S3FileStorePluginIT",
      s3Client,
      S3EncryptionHolder(Optional.empty[AmazonS3Encryption]),
      s3TransferManager,
      S3TransferEncryptionHolder(Optional.empty[TransferManager]))
    s3FileStorePlugin.endpoint = s3ClientProvider.endpoint

    s3FileStorePlugin
  }

  @BeforeClass def createBucket() {
    fileStorePlugin.createBucket()
  }

  @Test def testPutFile() {
    val fileKey = new FileStoreKey.Builder()
      .setPartition("S3FileStorePluginIT")
      .setId("testPutFile.txt")
      .build
    val fileStream = new ByteArrayInputStream("abcde".getBytes)
    val putFileRequest = new PutFileRequest.Builder()
      .setFileStream(fileStream)
      .setFileSecurityLevel(FileSecurityLevel.PUBLIC).build
    val fileMetadata = fileStorePlugin.putFile(fileKey, putFileRequest)

    assertThat(fileMetadata.getURI, hasToString[URI](s"${s3ClientProvider.endpoint}/s3filestorepluginit-filestore/S3FileStorePluginIT/testPutFile.txt"))
    assertThat(fileMetadata.getSecurityLevel, is(FileSecurityLevel.PUBLIC))
    assertThat(fileMetadata.getStoreLocation, is(FileStoreLocation.REMOTE))
  }

  @Test def testPutFileReplace() {
    val fileKey = new FileStoreKey.Builder()
      .setPartition("LocalFSFileStorePluginFTest")
      .setId("testPutFileReplace.txt")
      .build

    val fileStream1 = new ByteArrayInputStream("abcde".getBytes())
    val putFileRequest1 = new PutFileRequest.Builder().setFileStream(fileStream1)
      .setFileSecurityLevel(FileSecurityLevel.PUBLIC).build
    fileStorePlugin.putFile(fileKey, putFileRequest1)

    val fileStream2 = new ByteArrayInputStream("12345".getBytes())
    val putFileRequest2 = new PutFileRequest.Builder().setFileStream(fileStream2)
      .setFileSecurityLevel(FileSecurityLevel.PUBLIC).build
    val fileMetadata = fileStorePlugin.putFile(fileKey, putFileRequest2)

    // Verify that the contents of the file have been replaced
    assertThat(Resources.asCharSource(fileMetadata.getURI().toURL(), Charsets.UTF_8).read(), stringContainsInOrder("12345"))
  }

  @Test def testGetFile() {
    val fileKey = new FileStoreKey.Builder()
      .setPartition("S3FileStorePluginIT")
      .setId("testGetFile.txt")
      .build
    val fileStream = new ByteArrayInputStream("abcde".getBytes())
    val putFileRequest = new PutFileRequest.Builder().setFileStream(fileStream)
      .setFileSecurityLevel(FileSecurityLevel.PUBLIC).build
    val putFileMetadata = fileStorePlugin.putFile(fileKey, putFileRequest)

    val getFileMetadata = fileStorePlugin.getFileMetadata(fileKey)
    assertThat(getFileMetadata, not(Optional.empty[FileMetadata]))
    assertThat(getFileMetadata.get, is(putFileMetadata))
  }

  @Test def testDeleteFile() {
    val fileKey = new FileStoreKey.Builder()
      .setPartition("LocalFSFileStorePluginTest")
      .setId("testDeleteFile.txt")
      .build
    val fileStream = new ByteArrayInputStream("abcde".getBytes())
    val putFileRequest = new PutFileRequest.Builder().setFileStream(fileStream)
      .setFileSecurityLevel(FileSecurityLevel.PUBLIC).build
    fileStorePlugin.putFile(fileKey, putFileRequest)

    val deletedFile = fileStorePlugin.deleteFile(fileKey)
    val getFileMetadata = fileStorePlugin.getFileMetadata(fileKey)
    val getFileContent = fileStorePlugin.getFileContent(fileKey)

    assertThat(deletedFile, is(true))
    assertThat(getFileMetadata, is(Optional.empty[FileMetadata]))
    assertThat(getFileContent, is(Optional.empty[InputStream]))
  }
}
