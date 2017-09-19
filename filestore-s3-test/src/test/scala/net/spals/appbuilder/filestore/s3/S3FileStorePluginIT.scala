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
import io.opentracing.mock.{MockSpan, MockTracer}
import net.spals.appbuilder.executor.core.ExecutorServiceFactory
import net.spals.appbuilder.filestore.core.model._
import net.spals.appbuilder.filestore.s3.S3SpanMatcher.s3Span
import org.hamcrest.MatcherAssert._
import org.hamcrest.Matchers._
import org.hamcrest.{Description, TypeSafeMatcher}
import org.mockito.ArgumentMatchers.{any => m_any, anyInt => m_anyInt}
import org.mockito.Mockito.{mock, when}
import org.slf4j.LoggerFactory
import org.testng.annotations.{BeforeClass, BeforeMethod, Test}

/**
  * Integration tests for [[S3FileStorePlugin]]
  *
  * @author tkral
  */
class S3FileStorePluginIT {

  private val LOGGER = LoggerFactory.getLogger(classOf[S3FileStorePluginIT])

  private val s3Endpoint = s"http://${System.getenv("S3_IP")}:${System.getenv("S3_PORT")}"
  private val s3Tracer = new MockTracer()
  private lazy val s3ClientProvider = {
    // Turn off MD5 checks because we're going against a Fake S3
    System.setProperty(DISABLE_GET_OBJECT_MD5_VALIDATION_PROPERTY, "true")
    System.setProperty(DISABLE_PUT_OBJECT_MD5_VALIDATION_PROPERTY, "true")

    val s3ClientProvider = new S3ClientProvider(s3Tracer)
    s3ClientProvider.awsAccessKeyId = "DUMMY"
    s3ClientProvider.awsSecretKey = "DUMMY"
    s3ClientProvider.endpoint = s3Endpoint

    LOGGER.info(s"Connecting to S3 instance at ${s3ClientProvider.endpoint}")
    s3ClientProvider
  }

  private lazy val s3Client = s3ClientProvider.get

  private lazy val s3TransferManager = {
    val executorServiceFactory = mock(classOf[ExecutorServiceFactory])
    when(executorServiceFactory.createFixedThreadPool(m_anyInt(), m_any(classOf[Class[_]])))
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

  @BeforeMethod def resetTracer() {
    s3Tracer.reset()
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
    assertThat(s3Tracer.finishedSpans(), contains[MockSpan](s3Span(s3Endpoint, "PUT")))
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
    assertThat(s3Tracer.finishedSpans(), contains[MockSpan](s3Span(s3Endpoint, "PUT"), s3Span(s3Endpoint, "PUT")))
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
    assertThat(s3Tracer.finishedSpans(), contains[MockSpan](s3Span(s3Endpoint, "PUT"), s3Span(s3Endpoint, "HEAD")))
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
    assertThat(s3Tracer.finishedSpans(), contains[MockSpan](s3Span(s3Endpoint, "PUT"),
      s3Span(s3Endpoint, "DELETE"), s3Span(s3Endpoint, "HEAD"), s3Span(s3Endpoint, "HEAD")))
  }
}

private object S3SpanMatcher {

  def s3Span(url: String, method: String): S3SpanMatcher =
    S3SpanMatcher(url, method)
}

private case class S3SpanMatcher(url: String, method: String) extends TypeSafeMatcher[MockSpan] {

  override def matchesSafely(mockSpan: MockSpan): Boolean = {
    hasEntry[String, AnyRef]("component", "java-aws-sdk").matches(mockSpan.tags()) &&
      hasEntry[String, AnyRef]("http.method", method).matches(mockSpan.tags()) &&
      hasEntry[String, AnyRef]("http.url", url).matches(mockSpan.tags()) &&
      hasEntry[String, AnyRef]("span.kind", "client").matches(mockSpan.tags()) &&
      "Amazon S3".equals(mockSpan.operationName())
  }

  override def describeTo(description: Description): Unit = {
    description.appendText("an S3 span tagged with method ")
    description.appendText(method)
    description.appendText(" and url ")
    description.appendText(url)
  }
}
