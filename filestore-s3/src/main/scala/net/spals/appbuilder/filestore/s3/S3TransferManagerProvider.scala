package net.spals.appbuilder.filestore.s3

import java.util.concurrent.ExecutorService
import javax.validation.constraints.Min

import com.amazonaws.client.builder.ExecutorFactory
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.transfer.{TransferManager, TransferManagerBuilder}
import com.google.inject.{Inject, Provider}
import com.netflix.governator.annotations.Configuration
import net.spals.appbuilder.annotations.service.AutoBindProvider
import net.spals.appbuilder.executor.core.ExecutorServiceFactory
import net.spals.appbuilder.executor.core.ExecutorServiceFactory.Key

/**
  * @author tkral
  */
@AutoBindProvider
private[s3] class S3TransferManagerProvider @Inject() (
  s3Client: AmazonS3,
  executorServiceFactory: ExecutorServiceFactory
) extends Provider[TransferManager] {

  @Min(2)
  @Configuration("fileStore.s3.numUploadThreads")
  private[s3] var numUploadThreads: Int = 10

  override def get(): TransferManager = {
    TransferManagerBuilder.standard()
      .withS3Client(s3Client)
      .withExecutorFactory(new ExecutorFactory() {
        override def newExecutor(): ExecutorService =
          executorServiceFactory.createFixedThreadPool(numUploadThreads, new Key.Builder(getClass).build())
      })
      .withShutDownThreadPools(false)
      .build()
  }
}
