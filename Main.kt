import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors

// Клас Task для збереження результатів
data class Task(val name: String, val result: String)

// Основний клас симулятора
class NonBlockingTaskSimulator(private val resultsFile: File, private val errorFile: File) {
    private val threadPool = Executors.newFixedThreadPool(6) // Максимум 6 потоків
    private val scope = CoroutineScope(Dispatchers.Default) // Асинхронне виконання
    private val taskQueue = mutableListOf<Deferred<Task?>>() // Черга завдань
    private var lastResult: Task? = null // Останній результат
    private val taskList = listOf(
        Callable<String> { Thread.sleep(300); "Result of task 0" },
        Callable<String> { Thread.sleep(500); "Result of task 1" },
        Callable<String> { Thread.sleep(1000); "Result of task 2" },
        Callable<String> { Thread.sleep(1500); "Result of task 3" },
        Callable<String> { Thread.sleep(2000); "Result of task 4" },
        Callable<String> { Thread.sleep(2500); "Result of task 5" }
    )

    init {
        resultsFile.createNewFile()
        errorFile.createNewFile()
    }

    // Виконання завдання асинхронно
    fun executeTask(name: String, taskIndex: Int) {
        if (taskIndex !in taskList.indices) {
            println("Invalid task index.")
            return
        }
        val task = taskList[taskIndex]
        val deferredTask = scope.async {
            try {
                val result = task.call()
                val taskResult = Task(name, result)
                lastResult = taskResult
                writeToFile(taskResult)
                taskResult
            } catch (e: Exception) {
                writeErrorToFile(e.message ?: "Unknown error")
                null
            }
        }
        taskQueue.add(deferredTask)
    }

    // Отримання останнього результату
    fun getLastResult() {
        lastResult?.let {
            println("${it.result} [${it.name}]")
        } ?: println("No result available.")
    }

    // Очистка файлу результатів
    fun cleanResultsFile() {
        resultsFile.writeText("")
    }

    // Завершення з очікуванням виконання всіх завдань
    fun finishGracefully() {
        println("Finishing gracefully...")
        runBlocking {
            taskQueue.awaitAll()
            println("All tasks completed.")
            stopApplication()
        }
    }

    // Примусове завершення
    fun finishForce() {
        println("Forcing application to stop...")
        stopApplication()
    }

    // Вивід інструкцій
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

    // Запис результату у файл
    private fun writeToFile(task: Task) {
        resultsFile.appendText("${task.name}: ${task.result}\n")
    }

    // Запис помилки у файл
    private fun writeErrorToFile(error: String) {
        errorFile.appendText("$error\n")
    }

    // Зупинка програми
    private fun stopApplication() {
        threadPool.shutdownNow()
        scope.cancel()
        println("Application stopped.")
    }

    // Обробка команд
    fun handleCommand(command: String) {
        val args = command.split(" ")
        when (args[0]) {
            "task" -> {
                val name = args.getOrNull(1) ?: run {
                    println("Task name missing.")
                    return
                }
                val taskIndex = args.getOrNull(2)?.toIntOrNull() ?: run {
                    println("Invalid task index.")
                    return
                }
                executeTask(name, taskIndex)
            }
            "get" -> getLastResult()
            "finish" -> if (args.getOrNull(1) == "grace") finishGracefully() else finishForce()
            "clean" -> cleanResultsFile()
            "help" -> printHelp()
            else -> println("Invalid command.")
        }
    }

    // Читання команд від користувача
    fun start() {
        val reader = System.`in`.bufferedReader()
        while (true) {
            val command = reader.readLine() ?: break
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
