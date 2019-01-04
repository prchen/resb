# RESB

> Reactive Enterprise Service Bus

## Introduction

RESB is an extendable reactive ESB framework based on [Project Reactor](https://projectreactor.io/).

**Requirements**

- JDK 1.8 or above

**Keywords**

- Enterprise Service Bus
- Microservices
- Remote Procedure Call
- Project Reactor

**Features**

- Complete reactive API
- Custom RPC protocol supported
- Custom service discovering supported
- Custom DNS resolving supported
- Easy to be integrated with Spring 5 and / or Spring Boot 2

**About**

The project is aimed to provide a referable reactive enterprise service bus prototype base on the Reactor tech stack.
The project is decided to be released into the public domain.
Please see the announcement at the end of this document or the LICENSE file stored under the root directory of the repository.

## Getting Start

Before we start, this tutorial assumes that you have the basic idea of:

- Managing java project by maven
- Reactive programming

**Step 0 - Meet the core concepts**

- `ServiceBus`: The object which provides the integrated functionality of executing `Command`, discovering service endpoint, resolving DNS names, etc.
- `Command`: The object which carry the parameters of calling a business function.
- `Reply`: The execution result of a `Command`, which may carry a payload for result or some error information.
- `Entry`: Some meta data bound to a `Command` to tell the `ServiceBus` how to make a remote procedure call.
- `Cell`: The unit which can handle a specific `Command`.
- `Explorer`: The objects which provides the service discovering service.
- `Resolver`: The objects which provides the DNS name resolving service.
- `Protocol`: The underling implementation of making remote procedure call.
- `Interceptor`: The objects which can plugged into the execution cycle of a `Command` (like aspects).

> Currently, RESB does not have a built-in `Protocol` which means you have to implement one if you need to make remote procedure call.

**Step I - Install components locally**

Because RESB is not planed to publish to maven central repository currently, you have to build and install the jar file locally.

```bash
git clone https://github.com/prchen/resb.git && \
cd resb && \
./mvnw clean install
```

**Step II - Add dependency to your project**

```xml
<dependency>
    <groupId>pub.resb</groupId>
    <artifactId>resb-reactor</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

**Step III - Hello world!**

> You can find this sample project under the `resb-sample` directory.

Define a `Command`:

```java
// For advanced remote call, you may need an annotation here like
// @Entry("my-protocol://my-service-name/MyCellName")
// Or
// @Entry("my-protocol://my-host:port/MyCellName")
public class HelloWorldCommand implements Command<String> {

}
```

Define the `Cell` that will handle the `Command`:

```java
public class HelloWorldCell implements Cell<HelloWorldCommand, String> {
    @Override
    public Mono<Reply<String>> exchange(HelloWorldCommand command) {
        return Mono.just(Reply.of("Hello World!"));
    }
}
```

Initialize the `ServiceBus` and make a function call:

```java

public class HelloWorldMain {
    private static final ServiceBus serviceBus;

    static {
        serviceBus = ServiceBus.builder()
                .cell(new HelloWorldCell())
                .build();
    }

    public static void main(String... args) {
        serviceBus.exchange(new HelloWorldCommand())
                .map(Reply::getResult)
                .subscribe(System.out::println);
    }
}
```

Run the main method and see the output:

```
Hello World!
```

> You may also find some example from the test cases in `resb-reactor` project.

## License

```
This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.

In jurisdictions that recognize copyright laws, the author or authors
of this software dedicate any and all copyright interest in the
software to the public domain. We make this dedication for the benefit
of the public at large and to the detriment of our heirs and
successors. We intend this dedication to be an overt act of
relinquishment in perpetuity of all present and future rights to this
software under copyright law.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

For more information, please refer to <http://unlicense.org>
```
