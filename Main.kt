import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors

// Data class for task results
data class Task(val name: String, val result: String)

// NonBlockingTaskSimulator class to manage the simulation
class NonBlockingTaskSimulator(val resultsFile: File, val errorFile: File) {
    private val threadPool = Executors.newFixedThreadPool(6)  // Fixed thread pool with 6 threads
    private val scope = CoroutineScope(Dispatchers.Default)  // Coroutine scope for async tasks
    private val taskQueue = mutableListOf<Deferred<Task?>>()  // List to keep track of tasks
    private var lastResult: Task? = null  // To hold the last result
    private val taskList = listOf(
        Callable<String> { Thread.sleep(300); "Result of task 0" },
        Callable<String> { Thread.sleep(500); "Result of task 1" },
        Callable<String> { Thread.sleep(1000); "Result of task 2" },
        Callable<String> { Thread.sleep(1500); "Result of task 3" },
        Callable<String> { Thread.sleep(2000); "Result of task 4" },
        Callable<String> { Thread.sleep(2500); "Result of task 5" }
    )  // Predefined tasks

    init {
        resultsFile.createNewFile()
        errorFile.createNewFile()
    }

    // Execute task asynchronously
    fun executeTask(name: String, taskIndex: Int) {
        if (taskIndex !in taskList.indices) {
            println("Invalid task index.")
            return
        }
        val task = taskList[taskIndex]
        val deferredTask = scope.async {
            try {
                val result = task.call()  // Execute the callable task
                val taskResult = Task(name, result)  // Wrap the result in a Task object
                lastResult = taskResult  // Save the result
                writeToFile(taskResult)  // Write result to the results file
                taskResult  // Return the Task object as the result
            } catch (e: Exception) {
                writeErrorToFile(e.message ?: "Unknown error")  // Write any errors to the error file
                null  // Return null in case of an error
            }
        }
        taskQueue.add(deferredTask)  // Add the deferred task to the task queue
    }

    // Get the last result
    fun getLastResult() {
        lastResult?.let {
            println("${it.result} [${it.name}]")
        } ?: println("No result available.")
    }

    // Clean the results file
    fun cleanResultsFile() {
        resultsFile.writeText("")  // Clear the file contents
    }

    // Finish gracefully: Wait for all tasks to complete before shutting down
    fun finishGracefully() {
        println("Finishing gracefully...")
        scope.launch {
            taskQueue.awaitAll()  // Wait for all tasks to finish
            println("All tasks completed.")
            stopApplication()
        }
    }

    // Finish immediately (force)
    fun finishForce() {
        println("Forcing application to stop...")
        stopApplication()
    }

    // Print help message
    fun printHelp() {
        println("""
            Commands:
            task NAME X: Execute task X, name it NAME, and write the result to the results file.
            get: Output the last result and its name to the console.
            finish grace: Stop accepting new tasks, finish all pending tasks, and stop the application.
            finish force: Stop the application immediately.
            clean: Clean the results file.
            help: Output this help message.
        """)
    }

    // Write the result to the results file
    private fun writeToFile(task: Task) {
        resultsFile.appendText("${task.name}: ${task.result}\n")
    }

    // Write error to the error log file
    private fun writeErrorToFile(error: String) {
        errorFile.appendText("$error\n")
    }

    // Stop the application by shutting down the thread pool and canceling coroutines
    private fun stopApplication() {
        threadPool.shutdownNow()  // Stop all threads immediately
        scope.cancel()  // Cancel any pending coroutines
        println("Application stopped.")
    }

    // Handle commands from the user input
    fun handleCommand(command: String) {
        val args = command.split(" ")
        when (args[0]) {
            "task" -> {
                val name = args[1]
                val taskIndex = args[2].toInt()
                executeTask(name, taskIndex)
            }
            "get" -> getLastResult()
            "finish" -> if (args[1] == "grace") finishGracefully() else finishForce()
            "clean" -> cleanResultsFile()
            "help" -> printHelp()
            else -> println("Invalid command.")
        }
    }

    // Start reading commands from the user
    fun start() {
        val reader = System.`in`.bufferedReader()
        while (true) {
            val command = reader.readLine() ?: break  // Read input, break if EOF or null
            if (command == "exit") {
                finishGracefully()
                break
            }
            handleCommand(command)
        }
    }
}

fun main() {
    val resultsFile = File("results.txt")
    val errorFile = File("errors.txt")
    val simulator = NonBlockingTaskSimulator(resultsFile, errorFile)

    println("Application started. Type 'help' for commands.")
    simulator.start()
}
