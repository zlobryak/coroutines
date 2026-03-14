## Вопросы: Cancellation

### Вопрос №1

Отработает ли в этом коде строка <--? Поясните, почему да или нет.

```fun main() = runBlocking {
val job = CoroutineScope(EmptyCoroutineContext).launch {
launch {
delay(500)
println("ok") // <--
}
launch {
delay(500)
println("ok")
}
}
delay(100)
job.cancelAndJoin()
}
```
#### Ответ: 
Строка <-- не отработает, т.к. время жизни в главном потоке 100 до отмены, а задержка в корутине 500.

### Вопрос №2

Отработает ли в этом коде строка <--. Поясните, почему да или нет.

```fun main() = runBlocking {
val job = CoroutineScope(EmptyCoroutineContext).launch {
val child = launch {
delay(500)
println("ok") // <--
}
launch {
delay(500)
println("ok")
}
delay(100)
child.cancel()
}
delay(100)
job.join()
}
```
#### Ответ: 

Строка <-- не будет выполнена. корутина child имеет задержку в 500, тогда как child.cancel() будет выполнена с задержкой 100.

## Вопросы: Exception Handling

### Вопрос №1

Отработает ли в этом коде строка <--. Поясните, почему да или нет.

```fun main() {
with(CoroutineScope(EmptyCoroutineContext)) {
try {
launch {
throw Exception("something bad happened")
}
} catch (e: Exception) {
e.printStackTrace() // <--
}
}
Thread.sleep(1000)
}
```
#### Ответ: 

Строка <-- оработает в запущенной корутине, но блок try {} catch завершится после успешного завершения операции lunch{}.
Исклчение не будет перехвачено в блоке catch, т.к. lunch не дожидается выполнения кода, а только планирует его запуск.

### Вопрос №2

Отработает ли в этом коде строка <--. Поясните, почему да или нет.

```fun main() {
CoroutineScope(EmptyCoroutineContext).launch {
try {
coroutineScope {
throw Exception("something bad happened")
}
} catch (e: Exception) {
e.printStackTrace() // <--
}
}
Thread.sleep(1000)
}
```

#### Ответ: 

Да, строка <-- будет выполнена и обработает исключение, т.к. coroutineScope suspend функция, она пробрасывает ошибки возникшие в ее теле.
Исключение будет выброшен внутри блока try{}catch.

### Вопрос №3
Отработает ли в этом коде строка <--. Поясните, почему да или нет.

```fun main() {
CoroutineScope(EmptyCoroutineContext).launch {
try {
supervisorScope {
throw Exception("something bad happened")
}
} catch (e: Exception) {
e.printStackTrace() // <--
}
}
Thread.sleep(1000)
}
```

#### Ответ: 

Да, ошибка так же как и в предыдущем вопросе будет обработана, supervisorScope так же пробрасывает исключение возникшее внутри тела функции в блок try{}catch

### Вопрос №4
Отработает ли в этом коде строка <--. Поясните, почему да или нет.

```fun main() {
CoroutineScope(EmptyCoroutineContext).launch {
try {
coroutineScope {
launch {
delay(500)
throw Exception("something bad happened") // <--
}
launch {
throw Exception("something bad happened")
}
}
} catch (e: Exception) {
e.printStackTrace()
}
}
Thread.sleep(1000)
}
```

#### Ответ:

В данном случае строка <-- не будет выполнена, т.к. ее задержка 500, а второй блок lunch выбросит ошибка буз задержки.
coroutineScope работает так, что отменяет выполнение всех дочерних корутин, дожидается их отмены и выбрасывает первое полученное исключение.

### Вопрос №5
Отработает ли в этом коде строка <--. Поясните, почему да или нет.

```fun main() {
CoroutineScope(EmptyCoroutineContext).launch {
try {
supervisorScope {
launch {
delay(500)
throw Exception("something bad happened") // <--
}
launch {
throw Exception("something bad happened")
}
}
} catch (e: Exception) {
e.printStackTrace() // <--
}
}
Thread.sleep(1000)
}
```

#### Ответ:

supervisorScope не завершает выполнение дочерних процессов, при получении ошибки.
Оба исключения будут получены, но в printStackTrace() будет проброшено первое.

### Вопрос №6
Отработает ли в этом коде строка <--. Поясните, почему да или нет.

```fun main() {
CoroutineScope(EmptyCoroutineContext).launch {
CoroutineScope(EmptyCoroutineContext).launch {
launch {
delay(1000)
println("ok") // <--
}
launch {
delay(500)
println("ok")
}
throw Exception("something bad happened")
}
}
Thread.sleep(1000)
}
```

#### Ответ:

Строка <-- не будет выполнена. hrow Exception("something bad happened") выполнится немедленно внутри одного и тго де независимого scope.
После появления исключения, все дочерние процессы будут завершены сразу же и не дождутся завершения задержки.

### Вопрос №7
Отработает ли в этом коде строка <--. Поясните, почему да или нет.

```fun main() {
CoroutineScope(EmptyCoroutineContext).launch {
CoroutineScope(EmptyCoroutineContext + SupervisorJob()).launch {
launch {
delay(1000)
println("ok") // <--
}
launch {
delay(500)
println("ok")
}
throw Exception("something bad happened")
}
}
Thread.sleep(1000)
}
```

#### Ответ:
Строка не выполнится, т.к. исключение появляется в теле CoroutineScope(EmptyCoroutineContext + SupervisorJob()).launch {}
Если бы было launch {throw Exception("something bad happened")}, тогда сработали бы + SupervisorJob() и были бы получены результаты всех дочерних потоков.
