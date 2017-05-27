package net.spals.appbuilder.filestore.s3

import java.util.concurrent.ExecutorService
import javax.validation.constraints.Min

import com.amazonaws.client.builder.ExecutorFactory
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.transfer.{TransferManager, TransferManagerBuilder}
import com.google.inject.{Inject, Provider}
import com.netflix.governator.annotations.Configuration
import net.spals.appbuilder.annotations.service.AutoBindProvider
import net.spals.appbuilder.executor.core.ExecutorServiceFactory

/**
  * @author tkral
  */
@AutoBindProvider
private[s3] class S3TransferManagerProvider @Inject() (
  s3Client: AmazonS3Client,
  executorServiceFactory: ExecutorServiceFactory
) extends Provider[TransferManager] {

  @Min(2)
  @Configuration("s3.fileStore.numUploadThreads")
  private var numUploadThreads: Int = 10

  override def get(): TransferManager = {
    TransferManagerBuilder.standard()
      .withS3Client(s3Client)
      .withExecutorFactory(new ExecutorFactory() {
        override def newExecutor(): ExecutorService =
          executorServiceFactory.createFixedThreadPool(numUploadThreads, getClass)
      })
      .withShutDownThreadPools(false)
      .build()
  }
}
