package me.rayatnia.infrastructure.workers

import com.opencsv.CSVReaderBuilder
import java.io.File
import java.io.FileReader
import java.io.BufferedReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rayatnia.domain.events.FileUploadedEvent
import me.rayatnia.domain.events.ProcessingStatus
import me.rayatnia.domain.events.UserDataProcessedEvent
import me.rayatnia.domain.model.UserData
import me.rayatnia.infrastructure.messaging.EventPublisher
import me.rayatnia.infrastructure.persistence.UserDataRepository
import org.slf4j.LoggerFactory

class CsvProcessor(
    private val repository: UserDataRepository,
    private val eventPublisher: EventPublisher
) {
    private val logger = LoggerFactory.getLogger(CsvProcessor::class.java)
    
    suspend fun process(event: FileUploadedEvent) = withContext(Dispatchers.IO) {
        val file = File(event.filePath)
        if (!file.exists()) {
            logger.error("File not found: ${event.filePath}")
            return@withContext
        }
        
        try {
            BufferedReader(FileReader(file)).use { bufferedReader ->
                val headerLine = bufferedReader.readLine()
                val headers = headerLine?.split(",")?.map { it.trim() }
                    ?: throw IllegalStateException("CSV file is empty")
                
                val csvReader = CSVReaderBuilder(bufferedReader)
                    .build()
                
                csvReader.readAll().forEach { row ->
                    try {
                        val rowData = headers.zip(row.toList()).toMap()
                        val userData = UserData.fromCsvRow(rowData).getOrThrow()
                        repository.save(userData)
                        
                        eventPublisher.publish(UserDataProcessedEvent(
                            aggregateId = event.aggregateId,
                            userId = userData.id.toString(),
                            status = ProcessingStatus.SUCCESS
                        ))
                    } catch (e: Exception) {
                        logger.error("Error processing row: ${row.joinToString()}", e)
                        eventPublisher.publish(UserDataProcessedEvent(
                            aggregateId = event.aggregateId,
                            userId = "N/A",
                            status = ProcessingStatus.FAILURE,
                            errorMessage = e.message
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error processing file: ${event.filePath}", e)
        } finally {
            // Clean up the temporary file
            file.delete()
        }
    }
} 